package org.docear.plugin.services.communications.features;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.core.MultivaluedMap;

import org.docear.plugin.core.util.CoreUtils;
import org.docear.plugin.services.ServiceController;
import org.docear.plugin.services.communications.CommunicationsController;
import org.docear.plugin.services.communications.features.DocearServiceException.DocearServiceExceptionType;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.omg.CORBA.portable.UnknownException;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class AccountRegisterer {
	private static final int USER_TYPE_REGISTERED = 2;
	private static final int USER_TYPE_ANONYMOUS = 3;
	
	private class TaskState {
		public Exception ex = null;
	}
	
	public AccountRegisterer() {

	}

	public void createAnonymousUser() throws DocearServiceException, URISyntaxException, CancellationException {
		if (ServiceController.getController().getInformationRetrievalCode() > 0) {
			String name = createAnonymousUserName();
			createUser(name, null, USER_TYPE_ANONYMOUS, null, null, false, null);
			CommunicationsController.getController().tryToConnect(name, null, false, true);
		}
	}

	public void createRegisteredUser(String name, String password, String email, Integer birthYear, Boolean newsLetter, Boolean isMale)
			throws DocearServiceException, URISyntaxException, CancellationException {
		createUser(name, password, USER_TYPE_REGISTERED, email, birthYear, newsLetter, isMale);
		ResourceController.getResourceController().setProperty(CommunicationsController.DOCEAR_CONNECTION_USERNAME_PROPERTY, name);
		CommunicationsController.getController().tryToConnect(name, password, true, true);

	}


	private void createUser(final String name, final String password, final Integer type, final String email, final Integer birthYear, final Boolean newsLetter, final Boolean isMale)
			throws DocearServiceException {
				
		final TaskState state= new TaskState(); 
		ExecutorService execSrv = Executors.newSingleThreadExecutor();
		Future<TaskState> future = execSrv.submit(new Runnable() {
			public void run() {
				final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
				try {
					Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
					MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
					queryParams.add("userName", name);
					queryParams.add("password", password);
					queryParams.add("retypedPassword", password);
					queryParams.add("userType", "" + type);
					queryParams.add("eMail", email);
					queryParams.add("firstName", null);
					queryParams.add("middleName", null);
					queryParams.add("lastName", null);
					queryParams.add("birthYear", birthYear == null ? null : birthYear.toString());
					queryParams.add("generalNewsLetter", newsLetter == null ? null : newsLetter.toString());
					queryParams.add("isMale", isMale == null ? null : isMale.toString());
		
					WebResource res = CommunicationsController.getController().getServiceResource().path("/user/" + name);
					long time = System.currentTimeMillis();
					ClientResponse response = CommunicationsController.getController().post(res, queryParams);
					LogUtils.info("user creation took (ms): "+(System.currentTimeMillis()-time));
					if(Thread.interrupted()) {
						throw new DocearServiceException("request aborted");
					}
					try {
						if (response.getClientResponseStatus() != Status.OK) {
							throw new DocearServiceException(CommunicationsController.getErrorMessageString(response));
						}
					}
					finally {
						response.close();
					}
				}
				catch (DocearServiceException e) {
					LogUtils.info("DocearServiceException in AccountRegisterer.createUser().Future: "+e.getMessage());
					state.ex = e;
				}
				catch (ClientHandlerException e) {
					LogUtils.info("ClientHandlerException in AccountRegisterer.createUser().Future: "+e.getMessage());
					state.ex = new DocearServiceException(TextUtils.getText("docear.service.connect.no_connection"), DocearServiceExceptionType.NO_CONNECTION);
				}
				catch (Exception e) {
					LogUtils.info("Exception in AccountRegisterer.createUser().Future: "+e.getMessage());
					state.ex = new DocearServiceException(TextUtils.getText("docear.service.connect.unknown_error"));
				}
				finally {
					Thread.currentThread().setContextClassLoader(contextClassLoader);
				}
				
			}
		}, state);
		long time = System.currentTimeMillis();
		try {			
			future.get(CommunicationsController.CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			future.cancel(true);
			execSrv.shutdown();
			throw new DocearServiceException("registration failed because of: interrupted", DocearServiceExceptionType.SIGNUP_FAILED);
		} catch (ExecutionException e) {
			future.cancel(true);
			execSrv.shutdown();
			throw new DocearServiceException("registration failed because of: "+e.getMessage(), DocearServiceExceptionType.SIGNUP_FAILED);
		} catch (TimeoutException e) {
			future.cancel(true);
			execSrv.shutdown();
			throw new DocearServiceException("registration failed because of: timeout", DocearServiceExceptionType.SIGNUP_FAILED);
		}
		finally {
			LogUtils.info("user creation wait (ms): "+(System.currentTimeMillis()-time));
		}
		
		
		if(state.ex != null) {
			if(state.ex instanceof DocearServiceException) {
				throw (DocearServiceException)state.ex;
			}
			throw new UnknownException(state.ex);
			
		}

	}

	private String createAnonymousUserName() {
		return System.currentTimeMillis() + "_" + CoreUtils.createRandomString(5);
	}

	// public static void main(String[] args) {
	// AccountRegisterer registerer = new AccountRegisterer();
	//
	// //registerer.registerUser("stefan", "qvii-c", "", null, false);
	// response = registerer.createAnonymousUser();
	// }

}

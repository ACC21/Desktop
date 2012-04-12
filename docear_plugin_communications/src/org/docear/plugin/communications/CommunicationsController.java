package org.docear.plugin.communications;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.ws.rs.core.MultivaluedMap;

import org.docear.plugin.communications.components.WorkspaceDocearServiceConnectionBar;
import org.docear.plugin.communications.components.WorkspaceDocearServiceConnectionBar.CONNECTION_STATE;
import org.docear.plugin.communications.components.dialog.DocearServiceConnectionWaitPanel;
import org.docear.plugin.communications.components.dialog.DocearServiceLoginPanel;
import org.docear.plugin.communications.features.AccountRegisterer;
import org.docear.plugin.core.ALanguageController;
import org.docear.plugin.core.DocearController;
import org.docear.plugin.core.event.DocearEvent;
import org.docear.plugin.core.event.IDocearEventListener;
import org.freeplane.core.resources.IFreeplanePropertyListener;
import org.freeplane.core.resources.OptionPanelController.PropertyLoadListener;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.resources.components.IPropertyControl;
import org.freeplane.core.resources.components.StringProperty;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.plugin.workspace.WorkspaceController;
import org.freeplane.plugin.workspace.event.IWorkspaceEventListener;
import org.freeplane.plugin.workspace.event.WorkspaceEvent;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class CommunicationsController extends ALanguageController implements PropertyLoadListener, IWorkspaceEventListener, IFreeplanePropertyListener, IDocearEventListener {
	private final static CommunicationsController communicationsController = new CommunicationsController();

	private static final Client client;
	static {
		final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(CommunicationsController.class.getClassLoader());
		client = Client.create();
		Thread.currentThread().setContextClassLoader(contextClassLoader);
	}

	private final WorkspaceDocearServiceConnectionBar connectionBar = new WorkspaceDocearServiceConnectionBar();

	public final static String DOCEAR_CONNECTION_USERNAME_PROPERTY = "docear.service.connect.username";
	public final static String DOCEAR_CONNECTION_TOKEN_PROPERTY = "docear.service.connect.token";
	public final static String DOCEAR_CONNECTION_ANONYMOUS_USERNAME_PROPERTY = "docear.service.connect.anonyous.username";
	public final static String DOCEAR_CONNECTION_ANONYMOUS_TOKEN_PROPERTY = "docear.service.connect.anonymous.token";

	private boolean allowTransmission = true;

	public CommunicationsController() {
		super();

		addPluginDefaults();
		addPropertiesToOptionPanel();

		Controller.getCurrentController().getOptionPanelController().addPropertyLoadListener(this);
		Controller.getCurrentController().getResourceController().addPropertyChangeListener(this);
		WorkspaceController.getController().addWorkspaceListener(this);
		DocearController.getController().addDocearEventListener(this);

		WorkspaceController.getController().addToolBar(connectionBar);
		propertyChanged(DOCEAR_CONNECTION_TOKEN_PROPERTY, getRegisteredAccessToken(), null);
	}

	public static CommunicationsController getController() {
		return communicationsController;
	}

	public void showConnectionDialog() {
		JButton[] dialogButtons = new JButton[] { new JButton(TextUtils.getOptionalText("docear.service.connect.dialog.button.ok")), new JButton(TextUtils.getOptionalText("docear.service.connect.dialog.button.cancel")) };
		final DocearServiceLoginPanel loginPanel = new DocearServiceLoginPanel();
		loginPanel.ctrlOKButton(dialogButtons[0]);
		dialogButtons[0].addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Container cont = loginPanel.getParent();
				while (!(cont instanceof JOptionPane)) {
					cont = cont.getParent();
				}
				((JOptionPane) cont).setValue(e.getSource());
				closeDialogManually(cont);

			}
		});
		dialogButtons[1].addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Container cont = loginPanel.getParent();
				closeDialogManually(cont);
			}
		});

		loginPanel.setLicenseText(getLicenseText());
		int choice = JOptionPane.showOptionDialog(UITools.getFrame(), loginPanel, TextUtils.getOptionalText("docear.service.connect.title"), JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, dialogButtons, dialogButtons[0]);
		if (choice == 0) {
			tryToConnect(loginPanel.getUsername(), loginPanel.getPassword(), true, true);
		}
	}

	private String getLicenseText() {
		StringBuilder text = new StringBuilder();
		Scanner scanner = new Scanner(DocearController.class.getClassLoader().getResourceAsStream("/license.txt"));
		try {
			while (scanner.hasNextLine()) {
				text.append(scanner.nextLine() + System.getProperty("line.separator"));
			}
		} finally {
			scanner.close();
		}
		return text.toString();
	}

	private void closeDialogManually(Container container) {
		while (!(container instanceof JDialog)) {
			container = container.getParent();
		}
		((JDialog) container).dispose();
	}

	public void tryToConnect(final String username, final String password, final boolean registeredUser, final boolean silent) {
		final String propertyUserName = registeredUser ? DOCEAR_CONNECTION_USERNAME_PROPERTY : DOCEAR_CONNECTION_ANONYMOUS_USERNAME_PROPERTY;
		final String propertyAccessToken = registeredUser ? DOCEAR_CONNECTION_TOKEN_PROPERTY : DOCEAR_CONNECTION_ANONYMOUS_TOKEN_PROPERTY;

		final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
		try {
			MultivaluedMap<String, String> formParams = new MultivaluedMapImpl();
			formParams.add("password", password);

			final JButton[] dialogButtons = new JButton[] { new JButton(TextUtils.getOptionalText("docear.service.connect.dialog.button.cancel")) };
			final DocearServiceConnectionWaitPanel waitPanel = new DocearServiceConnectionWaitPanel();
			dialogButtons[0].addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Container cont = waitPanel.getParent();
					closeDialogManually(cont);
				}
			});

			Thread waitRunner = new Thread(new Runnable() {
				public void run() {
					int choice = JOptionPane.showOptionDialog(UITools.getFrame(), waitPanel, TextUtils.getOptionalText("docear.service.connect.title"), JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, dialogButtons, dialogButtons[0]);
					if (choice == 0) {
						// try to interrupt the connection process
					}
				}
			});

			if (!silent) {
				waitRunner.start();
			}

			WebResource webRes;

			try {
				webRes = client.resource(getServiceUri()).path("/authenticate/" + username);
			}
			// DOCEAR: should not happen because the URI is hard coded for now
			catch (URISyntaxException ex) {
				LogUtils.severe(ex);
				if (!silent) {
					dialogButtons[0].doClick();
				}
				return;
			}

			try {
				ClientResponse response = webRes.post(ClientResponse.class, formParams);
				Status status = response.getClientResponseStatus();
				if (!silent) {
					dialogButtons[0].doClick();
				}

				if (Status.OK.equals(status)) {
					String token = response.getHeaders().getFirst("accessToken");
					if (!silent) {
						JOptionPane.showMessageDialog(UITools.getFrame(), TextUtils.format("docear.service.connect.success", username), TextUtils.getText("docear.service.connect.success.title"), JOptionPane.PLAIN_MESSAGE);
					}

					ResourceController.getResourceController().setProperty(propertyUserName, username);
					if (registeredUser) {
						IPropertyControl ctrl = Controller.getCurrentController().getOptionPanelController().getPropertyControl(propertyUserName);
						if(ctrl != null) {
							((StringProperty) ctrl).setValue(username);
						}
					}
					ResourceController.getResourceController().setProperty(propertyAccessToken, token);
					InputStream is = response.getEntityInputStream();
					while (is.read() > -1)
						;
					is.close();
				}
				else {
					InputStream is = response.getEntityInputStream();
					int chr;
					StringBuilder message = new StringBuilder();
					while ((chr = is.read()) > -1) {
						message.append((char) chr);
					}
					is.close();
					if (!silent) {
						JOptionPane.showMessageDialog(UITools.getFrame(), TextUtils.format("docear.service.connect.failure", status, message.toString()), TextUtils.getText("docear.service.connect.failure.title"), JOptionPane.ERROR_MESSAGE);
					}

					ResourceController.getResourceController().setProperty(propertyUserName, "");
					if (registeredUser) {
						IPropertyControl ctrl = Controller.getCurrentController().getOptionPanelController().getPropertyControl(propertyUserName);
						if(ctrl != null) {
							((StringProperty) ctrl).setValue("");
						}
					}
					ResourceController.getResourceController().setProperty(propertyAccessToken, "");
				}
			} catch (Exception e) {
				
				if (!silent) {
					dialogButtons[0].doClick();
					JOptionPane.showMessageDialog(UITools.getFrame(), TextUtils.format("docear.service.connect.failure", "-1", e.getMessage()), TextUtils.getText("docear.service.connect.failure.title"), JOptionPane.ERROR_MESSAGE);
				}
				DocearController.getController().dispatchDocearEvent(new DocearEvent(FiletransferClient.class, FiletransferClient.NO_CONNECTION));
				
			}

		} finally {
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}
	}

	public boolean checkConnection() {
		client.setConnectTimeout(1000);
		try {
			MultivaluedMap<String, String> formParams = new MultivaluedMapImpl();
			formParams.add("password", "");
			ClientResponse response = client.resource(getServiceUri()).path("/authenticate/anonymous").post(ClientResponse.class, formParams);
			Status status = response.getClientResponseStatus();
			if (status != null) {
				return true;
			}
		} catch (Exception e) {
			if (e.getCause() instanceof SocketTimeoutException) {
				// no connection to server
			}
			else if (e.getCause() instanceof ConnectException) {
				// connection refused (no server running
			}
			else if (e.getCause() instanceof UnknownHostException) {
				// maybe no connection
			}
		} finally {
			client.setConnectTimeout(5000);
		}
		return false;
	}

	private void addPluginDefaults() {
		final URL defaults = this.getClass().getResource(ResourceController.PLUGIN_DEFAULTS_RESOURCE);
		if (defaults == null)
			throw new RuntimeException("cannot open " + ResourceController.PLUGIN_DEFAULTS_RESOURCE);
		Controller.getCurrentController().getResourceController().addDefaults(defaults);
	}

	private void addPropertiesToOptionPanel() {
		final URL preferences = this.getClass().getResource("preferences.xml");
		if (preferences == null)
			throw new RuntimeException("cannot open preferences");
		MModeController modeController = (MModeController) Controller
				.getCurrentModeController();

		modeController.getOptionPanelBuilder().load(preferences);
	}

	public File getCommunicationsQueuePath() {
		return new File(ResourceController.getResourceController().getFreeplaneUserDirectory(), "queue");
	}

	public void propertiesLoaded(Collection<IPropertyControl> properties) {
		Controller.getCurrentController().getOptionPanelController().getPropertyControl(DOCEAR_CONNECTION_USERNAME_PROPERTY).setEnabled(false);
	}

	public boolean postFileToDocearService(String restPath, boolean deleteIfTransferred, File... files) {
		if (!allowTransmission || files.length == 0 || isEmpty(getUserName()) || isEmpty(getAccessToken())) {
			return false;
		}
		FiletransferClient client = new FiletransferClient(restPath, files);
		return client.send(deleteIfTransferred);
	}

	public URI getServiceUri() throws URISyntaxException {
		return new URI("https://api.docear.org/");
		// return new URI("http://141.44.30.58:8080/");
	}

	public String getRegisteredUserName() {
		return ResourceController.getResourceController().getProperty(DOCEAR_CONNECTION_USERNAME_PROPERTY);
	}

	public String getUserName() {
		if (isEmpty(getRegisteredUserName()) || isEmpty(getRegisteredAccessToken())) {
			return getAnonymousUserName();
		}
		else {
			return getRegisteredUserName();
		}
	}

	private String getAnonymousUserName() {
		String userName = ResourceController.getResourceController().getProperty(DOCEAR_CONNECTION_ANONYMOUS_USERNAME_PROPERTY);
		if (isEmpty(userName)) {
			AccountRegisterer ar = new AccountRegisterer();
			ar.createAnonymousUser();
			userName = ResourceController.getResourceController().getProperty(DOCEAR_CONNECTION_ANONYMOUS_USERNAME_PROPERTY);
		}

		return userName;
	}

	public String getRegisteredAccessToken() {
		return ResourceController.getResourceController().getProperty(DOCEAR_CONNECTION_TOKEN_PROPERTY);
	}

	public String getAccessToken() {
		if (isEmpty(getRegisteredUserName()) || isEmpty(getRegisteredAccessToken())) {
			return getAnonymousAccessToken();
		}
		else {
			return getRegisteredAccessToken();
		}
	}

	private String getAnonymousAccessToken() {
		String accessToken = ResourceController.getResourceController().getProperty(DOCEAR_CONNECTION_ANONYMOUS_TOKEN_PROPERTY);
		if (isEmpty(accessToken)) {
			AccountRegisterer ar = new AccountRegisterer();
			ar.createAnonymousUser();
			accessToken = ResourceController.getResourceController().getProperty(DOCEAR_CONNECTION_ANONYMOUS_USERNAME_PROPERTY);
		}
		return accessToken;
	}

	public void propertyChanged(String propertyName, String newValue, String oldValue) {
		if (DOCEAR_CONNECTION_USERNAME_PROPERTY.equals(propertyName)) {
			connectionBar.setUsername(newValue);
		}
		else if (DOCEAR_CONNECTION_TOKEN_PROPERTY.equals(propertyName)) {
			adjustInfoBarConnectionState();

		}

	}

	private void adjustInfoBarConnectionState() {
		if (getRegisteredAccessToken() != null && getRegisteredAccessToken().trim().length() > 0) {
			connectionBar.setUsername(getRegisteredUserName());
			connectionBar.setEnabled(true);
			if (allowTransmission) {
				connectionBar.setConnectionState(CONNECTION_STATE.CONNECTED);
			}
			else {
				connectionBar.setConnectionState(CONNECTION_STATE.DISABLED);
			}
		}
		else {
			connectionBar.setUsername("");
			connectionBar.setConnectionState(CONNECTION_STATE.NO_CREDENTIALS);
			connectionBar.setEnabled(false);
		}
	}

	public void handleEvent(DocearEvent event) {
		if (event.getSource().equals(connectionBar) &&
				WorkspaceDocearServiceConnectionBar.ACTION_COMMAND_TOGGLE_CONNECTION_STATE.equals(event.getEventObject())) {
			allowTransmission = !allowTransmission;
			connectionBar.allowTransmission(allowTransmission);
			adjustInfoBarConnectionState();
			return;
		}
		if (event.getSource().equals(FiletransferClient.class)) {
			if (FiletransferClient.START_UPLOAD.equals(event.getEventObject())) {
				connectionBar.setConnectionState(CONNECTION_STATE.UPLOADING);
			}
			else if (FiletransferClient.STOP_UPLOAD.equals(event.getEventObject())) {
				adjustInfoBarConnectionState();
			}
			else if (FiletransferClient.NO_CONNECTION.equals(event.getEventObject())) {
				connectionBar.setConnectionState(CONNECTION_STATE.DISCONNECTED);
			}
			return;
		}

	}

	private boolean isEmpty(String s) {
		return s == null || s.trim().length() == 0;
	}

	public boolean allowTransmission() {
		return allowTransmission;
	}

	public void workspaceChanged(WorkspaceEvent event) {
		WorkspaceController.getController().addToolBar(connectionBar);
	}

	public void openWorkspace(WorkspaceEvent event) {
	}

	public void closeWorkspace(WorkspaceEvent event) {							
	}

	public void workspaceReady(WorkspaceEvent event) {
	}

	public void toolBarChanged(WorkspaceEvent event) {
	}

	public void configurationLoaded(WorkspaceEvent event) {
	}

	public void configurationBeforeLoading(WorkspaceEvent event) {
	}
}

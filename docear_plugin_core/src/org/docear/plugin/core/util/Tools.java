package org.docear.plugin.core.util;

import java.io.File;
import java.io.FileFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.freeplane.plugin.workspace.URIUtils;


public class Tools {
	
	//TODO: check if URI refers to a local file !!
	//WORKSPACE: temporarily disabled, test if we really need this class
	
//	public static File getFilefromUri(URI uri){		
//		if(uri == null) return null;
//		
//		try {
//			return WorkspaceUtils.resolveURI(uri);			
//		} 
//		catch (IllegalArgumentException e) {			
//			LogUtils.warn(e);
//			return null;
//		}
//	}
//	
//	public static URI getAbsoluteUri(NodeModel node){
//		URI uri = NodeLinks.getValidLink(node);	
//		
//		return WorkspaceUtils.absoluteURI(uri, node.getMap());
//	}
//	
//	public static URI getAbsoluteUri(NodeModel node, MapModel map){
//		URI uri = NodeLinks.getValidLink(node);
//		return WorkspaceUtils.absoluteURI(uri, map);
//	}
//	
//	public static URI getAbsoluteUri(URI uri){
//		return Tools.getAbsoluteUri(uri, Controller.getCurrentController().getMap());
//	}
//	
//	public static URI getAbsoluteUri(URI uri, MapModel map){
//		return WorkspaceUtils.absoluteURI(uri, map);
//	}
//	
//	
//	
//	
//    public static boolean isFile(URI uri) {
//    	final String scheme = uri.getScheme();
//		return scheme != null && scheme.equalsIgnoreCase("file");
//    }
//
//    public static boolean hasHost(URI uri) {
//        String host = uri.getHost();
//        return host != null && !"".equals(host);
//    }
//	
//	public static List<File> textURIListToFileList(String data) {
//	    List<File> list = new ArrayList<File>();
//	    StringTokenizer stringTokenizer = new StringTokenizer(data, "\r\n");
//	    while(stringTokenizer.hasMoreTokens()) {
//	    	String string = stringTokenizer.nextToken();
//	    	// the line is a comment (as per the RFC 2483)
//	    	if (string.startsWith("#")) continue;
//		    		    
//			try {
//				URI uri = new URI(string);
//				File file = new File(uri);
//			    list.add(file);
//			} catch (URISyntaxException e) {
//				LogUtils.warn("DocearNodeDropListener could not parse uri to file because an URISyntaxException occured. URI: " + string);
//			} catch (IllegalArgumentException e) {
//				LogUtils.warn("DocearNodeDropListener could not parse uri to file because an IllegalArgumentException occured. URI: " + string);
//		    }	    
//	    }	     
//	    return list;
//	}
//
//	public static String reshapeString(String s, int i) {
//		s = s.trim();
//		if(s.length() > i){
//			s = s.substring(0, i - 4);
//			s = s + "...";
//		}
//		return s;
//	}
//	
//	public static boolean exists(URI uri) {
//		if(Controller.getCurrentController() != null && Controller.getCurrentController().getMap() != null){
//			return Tools.exists(uri, Controller.getCurrentController().getMap());
//		}
//		else{
//			return Tools.exists(uri, null);
//		}
//	}
//
//	public static boolean exists(URI uri, MapModel map) {
//		uri = Tools.getAbsoluteUri(uri, map);
//		try {
//			URLConnection conn = uri.toURL().openConnection();
//			if(conn instanceof FileURLConnection) {
//				File file = new File(ParseUtil.decode(conn.getURL().getFile()));
//				if(file.exists() 
//						&& file.length() > 0) {
//					return true;
//				}
//			} else {
//				if(conn.getContentLength() > 0) {
//					return true;
//				}
//			}
//		} catch (MalformedURLException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return false;
//	}
//	
//	public static boolean FileIsLocatedInDir(URI absoluteFile, URI absoluteDir, boolean readSubDirectories){
//		if(!absoluteFile.isAbsolute() || !absoluteDir.isAbsolute()) return false;
//		
//		final File file = Tools.getFilefromUri(absoluteFile);
//		File dir = Tools.getFilefromUri(absoluteDir);
//		if(file == null || dir == null) return false;
//		File[] matchingFiles = dir.listFiles(new FilenameFilter() {
//			
//			public boolean accept(File dir, String name) {				
//				return name.equals(file.getName());
//			}
//		});
//		
//		if(matchingFiles.length > 0){
//			return true;
//		}
//		else if(readSubDirectories){
//			File[] subDirs = dir.listFiles(new DirectoryFileFilter());
//			if(subDirs != null && subDirs.length > 0){
//				for(File subDir : subDirs){
//					if(Tools.FileIsLocatedInDir(file.toURI(), subDir.toURI(), readSubDirectories)){
//						return true;
//					}
//				}
//			}
//		}
//		return false;
//	}
//	
//	
//	public static boolean setAttributeValue(NodeModel target, String attributeKey, Object value){
//		if(target == null || attributeKey == null || value == null) return false;
//		
//		NodeAttributeTableModel attributes = AttributeController.getController(MModeController.getMModeController()).createAttributeTableModel(target);
//		if(attributes != null){
//			if(attributes.getAttributeKeyList().contains(TextUtils.getText(attributeKey))){
//				//attributes.getAttribute(attributes.getAttributePosition(TextUtils.getText(attributeKey))).setValue(value);
//				AttributeController.getController(MModeController.getMModeController()).performSetValueAt(attributes, value, attributes.getAttributePosition(attributeKey), 1);
//				AttributeView attributeView = (((MapView) Controller.getCurrentController().getViewController().getMapView()).getSelected()).getAttributeView();
//	    		attributeView.setOptimalColumnWidths();
//				return true;
//			}
//			else{
//				AttributeController.getController(MModeController.getMModeController()).performInsertRow(attributes, attributes.getRowCount(), TextUtils.getText(attributeKey), value); 
//				AttributeView attributeView = (((MapView) Controller.getCurrentController().getViewController().getMapView()).getSelected()).getAttributeView();
//	    		attributeView.setOptimalColumnWidths();
//				return true;
//			}
//		}
//		return false;	
//	}
//
//	public static Object getAttributeValue(NodeModel target, String attributeKey) {
//		if(target == null || attributeKey == null) return null;
//		NodeAttributeTableModel attributes = AttributeController.getController(MModeController.getMModeController()).createAttributeTableModel(target);
//		if(attributes != null){
//			if(attributes.getAttributeKeyList().contains(TextUtils.getText(attributeKey))){
//				return attributes.getAttribute(attributes.getAttributePosition(TextUtils.getText(attributeKey))).getValue();				
//			}
//		}
//		return null;
//	}
//	
//	public static void removeAttributeValue(NodeModel target, String attributeKey) {
//		if(target == null || attributeKey == null) return;
//		NodeAttributeTableModel attributes = AttributeController.getController(MModeController.getMModeController()).createAttributeTableModel(target);
//		if(attributes != null){
//			if(attributes.getAttributeKeyList().contains(TextUtils.getText(attributeKey))){
//				AttributeController.getController(MModeController.getMModeController()).performRemoveRow(attributes, attributes.getAttributePosition(attributeKey));
//				AttributeView attributeView = (((MapView) Controller.getCurrentController().getViewController().getMapView()).getSelected()).getAttributeView();
//	    		attributeView.setOptimalColumnWidths();		
//			}
//		}		
//	}
//	
//	public static List<String> getAllAttributeKeys(NodeModel target){
//		if(target == null) return new ArrayList<String>();
//		NodeAttributeTableModel attributes = AttributeController.getController(MModeController.getMModeController()).createAttributeTableModel(target);
//		if(attributes != null){
//			return attributes.getAttributeKeyList();
//		}
//		return new ArrayList<String>();
//	}	
//	
//	public static String getStackTraceAsString(Exception exception){ 
//		StringWriter sw = new StringWriter(); 
//		PrintWriter pw = new PrintWriter(sw); 
//		pw.print(" [ "); 
//		pw.print(exception.getClass().getName()); 
//		pw.print(" ] "); 
//		pw.print(exception.getMessage()); 
//		exception.printStackTrace(pw); 
//		return sw.toString(); 
//	}

}

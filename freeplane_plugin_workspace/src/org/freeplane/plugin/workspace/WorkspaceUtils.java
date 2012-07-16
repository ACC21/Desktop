/**
 * author: Marcel Genzmehr
 * 09.08.2011
 */
package org.freeplane.plugin.workspace;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.channels.FileChannel;

import javax.swing.JOptionPane;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.Compat;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.link.LinkController;
import org.freeplane.features.link.mindmapmode.MLinkController;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.mapio.MapIO;
import org.freeplane.features.mapio.mindmapmode.MMapIO;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.url.UrlManager;
import org.freeplane.plugin.workspace.components.dialog.WorkspaceChooserDialogPanel;
import org.freeplane.plugin.workspace.model.AWorkspaceTreeNode;
import org.freeplane.plugin.workspace.model.WorkspaceIndexedTreeModel;
import org.freeplane.plugin.workspace.nodes.AFolderNode;
import org.freeplane.plugin.workspace.nodes.ALinkNode;
import org.freeplane.plugin.workspace.nodes.DefaultFileNode;
import org.freeplane.plugin.workspace.nodes.FolderLinkNode;
import org.freeplane.plugin.workspace.nodes.FolderVirtualNode;
import org.freeplane.plugin.workspace.nodes.LinkTypeFileNode;

/**
 * 
 */
public class WorkspaceUtils {
	private static final String UNC_PREFIX = "//";

	/***********************************************************************************
	 * CONSTRUCTORS
	 **********************************************************************************/

	/***********************************************************************************
	 * METHODS
	 **********************************************************************************/

	/**
	 * @param file
	 */
	public static void showFileNotFoundMessage(File file) {
		JOptionPane.showMessageDialog(UITools.getFrame(), TextUtils.format("workspace.node.link.notfound", 
				new Object[]{
					file.isDirectory()? TextUtils.getText("workspace.node.link.notfound.directory"):TextUtils.getText("workspace.node.link.notfound.file")
							,file.getName()
							,file.getParent()
				}));
	}
	
	public static void showWorkspaceChooserDialog() {
		String defaultLocation = System.getProperty("user.home")+File.separator+ResourceController.getResourceController().getProperty("ApplicationName", "freeplane").toLowerCase()+"_workspace";
		WorkspaceChooserDialogPanel dialog = new WorkspaceChooserDialogPanel(defaultLocation);
		
		JOptionPane.showMessageDialog(UITools.getFrame(), dialog, TextUtils.getRawText("no_location_set"), JOptionPane.PLAIN_MESSAGE);
	
		String location = dialog.getLocationPath();
		String profileName = dialog.getProfileName();
	
		if (location.length() == 0 || profileName.length() == 0) {			
			location = defaultLocation;
		}
	
		File f = new File(location);		
		URI newProfileBase = WorkspaceUtils.getURI(new File(f, WorkspaceController.getController().getPreferences().getWorkspaceProfilesRoot()+profileName));
		
		if(WorkspaceController.getController().getPreferences().getWorkspaceLocation() == null || !newProfileBase.equals(getProfileBaseURI())) {
			closeAllMindMaps();	
			WorkspaceController.getController().getPreferences().setNewWorkspaceLocation(WorkspaceUtils.getURI(f));			
			WorkspaceController.getController().getPreferences().setWorkspaceProfile(profileName);
			WorkspaceController.getController().loadWorkspace();
		}
	}
	
	private static void closeAllMindMaps() {
		while(Controller.getCurrentController().getMap() != null) {
			Controller.getCurrentController().close(false);
		}	
	}
	
	public static boolean createNewMindmap(final File f, String name) {
		if (!createFolderStructure(f)) {
			return false;
		}		
		Controller.getCurrentController().selectMode(MModeController.MODENAME);
		final MMapIO mapIO = (MMapIO) MModeController.getMModeController().getExtension(MapIO.class);
		
		MapModel map = mapIO.newMapFromDefaultTemplate();
		if(map == null) {
			return false;
		}
		map.getRootNode().setText(name);
		
		mapIO.save(map, f);
		Controller.getCurrentController().close(false);

		LogUtils.info("New Mindmap Created: " + f.getAbsolutePath());
		return true;
	}
	
	private static boolean createFolderStructure(final File f) {
		final File folder = f.getParentFile();
		if (folder.exists()) {
			return true;
		}
		return folder.mkdirs();
	}
	
	public static void saveCurrentConfiguration() {
		String profile = WorkspaceController.getController().getPreferences().getWorkspaceProfileHome();

		URI uri;
		File temp, config;
		try {
			uri = new URI(WorkspaceController.WORKSPACE_RESOURCE_URL_PROTOCOL + ":/" + profile + "/tmp_"
					+ WorkspaceConfiguration.CONFIG_FILE_NAME);
			temp = WorkspaceUtils.resolveURI(uri);
			uri = new URI(WorkspaceController.WORKSPACE_RESOURCE_URL_PROTOCOL + ":/" + profile + "/"
					+ WorkspaceConfiguration.CONFIG_FILE_NAME);
			config = WorkspaceUtils.resolveURI(uri);
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
			return;
		}

		try {
			WorkspaceController.getController().saveConfigurationAsXML(new FileWriter(temp));

			FileChannel from = new FileInputStream(temp).getChannel();
			FileChannel to = new FileOutputStream(config).getChannel();

			to.transferFrom(from, 0, from.size());
			to.close();
			from.close();
		}
		catch (IOException e1) {
			LogUtils.severe(e1);
		}
		temp.delete();
	}

	public static FolderLinkNode createPhysicalFolderNode(final File path, final AWorkspaceTreeNode parent) {
		if (!path.isDirectory()) {
			LogUtils.warn("the given path is no folder.");
			return null;
		}

		FolderLinkNode node = new FolderLinkNode(AFolderNode.FOLDER_TYPE_PHYSICAL);
		String name = path.getName();

		node.setName(name == null ? "directory" : name);

		if (path != null) {
			node.setPath(MLinkController.toLinkTypeDependantURI(getWorkspaceBaseFile(), path,
					LinkController.LINK_RELATIVE_TO_WORKSPACE));
		}

		addAndSave(findAllowedTargetNode(parent), node);
		return node;
	}

	public static void createLinkTypeFileNode(final File path, final AWorkspaceTreeNode parent) {
		if (!path.isFile()) {
			LogUtils.warn("the given path is no file.");
			return;
		}

		LinkTypeFileNode node = new LinkTypeFileNode(ALinkNode.LINK_TYPE_FILE);
		String name = path.getName();

		node.setName(name == null ? "fileLink" : name);

		if (path != null) {
			LogUtils.info("FilesystemPath: " + path);
			node.setLinkPath(MLinkController.toLinkTypeDependantURI(getWorkspaceBaseFile(), path,
					LinkController.LINK_RELATIVE_TO_WORKSPACE));
		}

		addAndSave(findAllowedTargetNode(parent), node);
	}

	public static void createVirtualFolderNode(String folderName, final AWorkspaceTreeNode parent) {
		if (folderName == null || folderName.trim().length() <= 0) {
			return;
		}

		AWorkspaceTreeNode targetNode = (AWorkspaceTreeNode) (parent == null ? WorkspaceController.getController()
				.getWorkspaceModel().getRoot() : parent);

		FolderVirtualNode node = new FolderVirtualNode(AFolderNode.FOLDER_TYPE_VIRTUAL);
		node.setName(folderName);

		addAndSave(targetNode, node);
	}

	public static URI getWorkspaceBaseURI() {
		URI ret = null;
		ret = getWorkspaceBaseFile().toURI();
		return ret;
	}
	
	public static URI getDataDirectoryURI() {
		URI ret = null;
		ret = getDataDirectory().toURI();
		return ret;
	}

	public static URI getProfileBaseURI() {
		URI base = getWorkspaceBaseFile().toURI();
		try {
			return normalize((new URI(base.getScheme(), base.getUserInfo(), base.getHost(), base.getPort(), base.getPath() + "/" 
					+ WorkspaceController.getController().getPreferences().getWorkspaceProfileHome()+"/", base.getQuery(),
					base.getFragment())));
		}
		catch (URISyntaxException e) {
		}
		return null;
	}

	public static File getWorkspaceBaseFile() {
		String location = WorkspaceController.getController().getPreferences().getWorkspaceLocation();
		if(location == null) {
			showWorkspaceChooserDialog();
		}
		return new File(location);
	}
	
	public static File getDataDirectory() {
		return new File(getWorkspaceBaseFile(), "_data");
	}

	public static File getProfileBaseFile() {
		return new File(getProfileBaseURI());
	}

	public static String stripIllegalChars(String string) {
		if (string == null) {
			return null;
		}
		//FIXME: DOCEAR - allow "space" in alpha 2
		return string.replaceAll("[^a-zA-Z0-9äöüÄÖÜ]+", "");
	}

	public static URI absoluteURI(final URI uri) {
		return absoluteURI(uri, null);

	}
	
	public static URI absoluteURI(final URI uri, MapModel map) {
		if(uri == null) {
			return null;
		}
		try {
			URLConnection urlConnection;
			// windows drive letters are interpreted as uri schemes -> make a file from the scheme-less uri string and use this to resolve the path
			if(Compat.isWindowsOS() && (uri.getScheme() != null && uri.getScheme().length() == 1)) { 
				urlConnection = (new File(uri.toString())).toURL().openConnection();
			} 
			else if(uri.getScheme() == null && !uri.getPath().startsWith(File.separator)) {
				if(map != null) {
					urlConnection = (new File(uri.toString())).toURL().openConnection();
				} 
				else {
					urlConnection = UrlManager.getController().getAbsoluteUri(map, uri).toURL().openConnection();
				}
			}
			else {
				urlConnection = uri.toURL().openConnection();				
			}
			
			if (urlConnection == null) {
				return null;
			}
			else {
				URI normalizedUri = normalize(urlConnection.getURL().toURI());							
				return normalizedUri;
			}
		}
		catch (URISyntaxException e) {
			LogUtils.warn(e);
		}
		catch (IOException e) {
			LogUtils.warn(e);
		}
		catch (Exception e){
			LogUtils.warn(e);
		}
		return normalize(uri);

	}
	
	private static URI normalize(URI uri){
		URI normalizedUri = uri.normalize();
		//Fix UNC paths that are incorrectly normalized by URI#resolve (see Java bug 4723726)
		String normalizedPath = normalizedUri.getPath();
		if ("file".equalsIgnoreCase(uri.getScheme()) && uri.getPath() != null && uri.getPath().startsWith(UNC_PREFIX) && (normalizedPath == null || !normalizedPath.startsWith(UNC_PREFIX))){
			try {
				normalizedUri = new URI(normalizedUri.getScheme(), ensureUNCPath(normalizedUri.getSchemeSpecificPart()), normalizedUri.getFragment());
			} catch (URISyntaxException e) {
				LogUtils.warn(e);
			}
		}				
		return normalizedUri;
	}
	
	private static String ensureUNCPath(String path) {
		int len = path.length();
		StringBuffer result = new StringBuffer(len);
		for (int i = 0; i < 4; i++) {
			//    if we have hit the first non-slash character, add another leading slash
			if (i >= len || result.length() > 0 || path.charAt(i) != '/')
				result.append('/');
		}
		result.append(path);
		return result.toString();
	}

	public static URI getWorkspaceRelativeURI(File file) {
		return LinkController.toRelativeURI(null, file, LinkController.LINK_RELATIVE_TO_WORKSPACE);
	}

	public static File resolveURI(final URI uri, final MapModel map) {
		if(uri == null || map == null) {
			return null;
		}
		try {
			return resolveURI(UrlManager.getController().getAbsoluteUri(map, uri));
		} 
		catch (Exception ex) {
			LogUtils.warn(ex);
		}
		return null;
	}
	
	public static File resolveURI(final URI uri) {
		if(uri == null) {
			return null;
		}
		try {
			if(uri.getFragment() != null) {
				return null;
			}
			URI absoluteUri = absoluteURI(uri);
			if (absoluteUri == null) {
				return null;
			}
			if("file".equalsIgnoreCase(absoluteUri.getScheme())){
				return new File(absoluteUri);
			}
		}
		catch(Exception ex) {
			LogUtils.warn(ex);
		}		
		return null;
	}

	public static URI getURI(final File f) {
		return normalize(f.toURI());
	}

	/**
	 * @param targetNode
	 * @param node
	 */
	private static void addAndSave(AWorkspaceTreeNode targetNode, AWorkspaceTreeNode node) {
		WorkspaceUtils.getModel().addNodeTo(node, targetNode);
		WorkspaceUtils.getModel().reload(targetNode);
		saveCurrentConfiguration();
	}

	public static AWorkspaceTreeNode findAllowedTargetNode(final AWorkspaceTreeNode node) {
		AWorkspaceTreeNode targetNode = node;
		// DOCEAR: drops are not allowed on physical nodes, for the moment
		while (targetNode instanceof DefaultFileNode || targetNode instanceof FolderLinkNode
				|| targetNode instanceof ALinkNode) {
			targetNode = (AWorkspaceTreeNode) targetNode.getParent();
		}
		return targetNode;
	}

	/**
	 * @return
	 */
	public static WorkspaceIndexedTreeModel getModel() {
		return WorkspaceController.getController().getWorkspaceModel();
	}
	
	public static AWorkspaceTreeNode getNodeForPath(String path) {
		if(path == null || path.length() <= 0) {
			return null;
		}
		String key = "";
		for(String token : path.split("/")) {
			key += "/"+Integer.toHexString(token.hashCode()).toUpperCase(); 
		}		
		return getModel().getNode(key);
	}

}

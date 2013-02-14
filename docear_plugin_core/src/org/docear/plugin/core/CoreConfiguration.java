package org.docear.plugin.core;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;

import javax.swing.SwingUtilities;

import org.docear.plugin.core.actions.DocearAboutAction;
import org.docear.plugin.core.actions.DocearOpenUrlAction;
import org.docear.plugin.core.actions.DocearQuitAction;
import org.docear.plugin.core.actions.DocearSetNodePrivacyAction;
import org.docear.plugin.core.actions.DocearShowDataPrivacyStatementAction;
import org.docear.plugin.core.actions.DocearShowDataProcessingTermsAction;
import org.docear.plugin.core.actions.DocearShowTermsOfUseAction;
import org.docear.plugin.core.actions.SaveAction;
import org.docear.plugin.core.actions.SaveAsAction;
import org.docear.plugin.core.features.DocearMapModelController;
import org.docear.plugin.core.features.DocearMapModelExtension;
import org.docear.plugin.core.features.DocearMapWriter;
import org.docear.plugin.core.features.DocearNodeModifiedExtensionController;
import org.docear.plugin.core.features.DocearNodePrivacyExtensionController;
import org.docear.plugin.core.listeners.DocearCoreOmniListenerAdapter;
import org.docear.plugin.core.listeners.MapLifeCycleAndViewListener;
import org.docear.plugin.core.listeners.PropertyListener;
import org.docear.plugin.core.listeners.PropertyLoadListener;
import org.docear.plugin.core.listeners.WorkspaceOpenDocumentListener;
import org.docear.plugin.core.logger.DocearLogEvent;
import org.docear.plugin.core.ui.NotificationBar;
import org.docear.plugin.core.workspace.actions.DocearChangeLibraryPathAction;
import org.docear.plugin.core.workspace.actions.DocearRenameAction;
import org.docear.plugin.core.workspace.controller.DocearProjectLoader;
import org.docear.plugin.core.workspace.model.DocearWorspaceProjectCreator;
import org.freeplane.core.resources.OptionPanelController;
import org.freeplane.core.resources.ResourceBundles;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.resources.components.IPropertyControl;
import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.ui.IMenuContributor;
import org.freeplane.core.ui.MenuBuilder;
import org.freeplane.core.util.ConfigurationUtils;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.help.OnlineDocumentationAction;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mapio.MapIO;
import org.freeplane.features.mapio.mindmapmode.MMapIO;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.ui.ViewController;
import org.freeplane.features.url.UrlManager;
import org.freeplane.features.url.mindmapmode.IMapConverter;
import org.freeplane.features.url.mindmapmode.MapConversionException;
import org.freeplane.features.url.mindmapmode.MapVersionInterpreter;
import org.freeplane.plugin.workspace.WorkspaceController;
import org.freeplane.plugin.workspace.event.WorkspaceActionEvent;
import org.freeplane.plugin.workspace.model.AWorkspaceTreeNode;
import org.freeplane.plugin.workspace.model.project.AWorkspaceProject;


public class CoreConfiguration extends ALanguageController {

	private static final String DOCEAR = "Docear";
	private static final String APPLICATION_NAME = "ApplicationName";
	private static final String DOCUMENTATION_ACTION = "DocumentationAction";
	private static final String DOCEAR_WEB_DOCU_LOCATION = "docear_webDocuLocation";
	private static final String WEB_DOCU_LOCATION = "webDocuLocation";
	private static final String REQUEST_FEATURE_ACTION = "RequestFeatureAction";
	private static final String DOCEAR_FEATURE_TRACKER_LOCATION = "docear_featureTrackerLocation";
	private static final String FEATURE_TRACKER_LOCATION = "featureTrackerLocation";
	private static final String ASK_FOR_HELP = "AskForHelp";
	private static final String HELP_FORUM_LOCATION = "helpForumLocation";
	private static final String REPORT_BUG_ACTION = "ReportBugAction";
	private static final String DOCEAR_BUG_TRACKER_LOCATION = "docear_bugTrackerLocation";
	private static final String BUG_TRACKER_LOCATION = "bugTrackerLocation";
	private static final String OPEN_FREEPLANE_SITE_ACTION = "OpenFreeplaneSiteAction";
	private static final String WEB_DOCEAR_LOCATION = "webDocearLocation";
	private static final String WEB_FREEPLANE_LOCATION = "webFreeplaneLocation";
	

	public static final String DOCUMENT_REPOSITORY_PATH = "@@literature_repository@@";
	public static final String LIBRARY_PATH = "@@library_mindmaps@@"; 
//	public static final String BIBTEX_PATH = DocearController.BIBTEX_PATH_PROPERTY;
		
	//WORKSPACE - todo: think about a solution
//	public static final NodeAttributeObserver projectPathObserver = new NodeAttributeObserver();
//	public static final NodeAttributeObserver referencePathObserver = new NodeAttributeObserver();
//	public static final NodeAttributeObserver repositoryPathObserver = new NodeAttributeObserver();	
		
	public CoreConfiguration() {			
		LogUtils.info("org.docear.plugin.core.CoreConfiguration() initializing...");
	}
	
	protected void initController(Controller controller) {
		loadAndStoreVersion(controller);
		
		adjustProperties(controller);
		
		AFreeplaneAction action = new DocearAboutAction();
		replaceAction(action.getKey(), action);
		
		action = new DocearQuitAction();
		replaceAction("QuitAction", action);
		
		AWorkspaceProject.setCurrentProjectCreator(new DocearWorspaceProjectCreator());
	}
	
	protected void initMode(ModeController modeController) {
		DocearProjectLoader docearProjectLoader = new DocearProjectLoader();
		WorkspaceController.getCurrentModeExtension().setProjectLoader(docearProjectLoader);
		
		DocearController.getController().getDocearEventLogger().appendToLog(this, DocearLogEvent.APPLICATION_STARTED);
		Toolkit.getDefaultToolkit();		
		DocearController.getController().getDocearEventLogger().appendToLog(this, DocearLogEvent.OS_OPERATING_SYSTEM, System.getProperty("os.name"));
		DocearController.getController().getDocearEventLogger().appendToLog(this, DocearLogEvent.OS_LANGUAGE_CODE, System.getProperty("user.language"));
		DocearController.getController().getDocearEventLogger().appendToLog(this, DocearLogEvent.OS_COUNTRY_CODE, System.getProperty("user.country"));
		DocearController.getController().getDocearEventLogger().appendToLog(this, DocearLogEvent.OS_TIME_ZONE, System.getProperty("user.timezone"));
		DocearController.getController().getDocearEventLogger().appendToLog(this, DocearLogEvent.OS_SCREEN_RESOLUTION, Toolkit.getDefaultToolkit().getScreenSize().toString());
		
		MapVersionInterpreter.addMapVersionInterpreter(new MapVersionInterpreter("docear", false, false, "Docear", "http://www.docear.org", null, new IMapConverter() {
			public void convert(NodeModel root) throws MapConversionException {
				final MapModel mapModel = root.getMap();				
				DocearMapModelExtension docearMapModel = mapModel.getExtension(DocearMapModelExtension.class);
				if (docearMapModel == null) {
					DocearMapModelController.setModelWithCurrentVersion(mapModel);
				}		
			}
		}));
		
		// set up context menu for workspace
		//WORKSPACE - info: test if this works without
//		WorkspaceController.getController().addWorkspaceListener(WORKSPACE_CHANGE_LISTENER);
		
		addPluginDefaults(Controller.getCurrentController());
		addMenus(modeController);
		
		registerListeners(modeController);
		//prepareWorkspace();
		
		replaceFreeplaneStringsAndActions(modeController);
		DocearMapModelController.install(new DocearMapModelController(modeController));
		
		setDocearMapWriter(modeController);
		
		registerController(modeController);
		UrlManager.getController().setLastCurrentDir(WorkspaceController.resolveFile(WorkspaceController.getCurrentModeExtension().getDefaultProjectHome()));	
	}
	
	private void loadAndStoreVersion(Controller controller) {
		//DOCEAR: has to be called before the splash is showing
		final Properties versionProperties = new Properties();
		InputStream in = null;
		try {
			in = this.getClass().getResource("/version.properties").openStream();
			versionProperties.load(in);
		}
		catch (final IOException e) {
			
		}
		
		final Properties buildProperties = new Properties();
		in = null;
		try {
			in = this.getClass().getResource("/build.number").openStream();
			buildProperties.load(in);
		}
		catch (final IOException e) {
			
		}
		final String versionNumber = versionProperties.getProperty("docear_version");
		final String versionStatus = versionProperties.getProperty("docear_version_status");
		final String versionStatusNumber = versionProperties.getProperty("docear_version_status_number");
		final int versionBuild = Integer.parseInt(buildProperties.getProperty("build.number")) -1;
		controller.getResourceController().setProperty("docear_version", versionNumber);
		controller.getResourceController().setProperty("docear_status", versionStatus+" "+versionStatusNumber+" build "+versionBuild);
		
	}
	
	private void adjustProperties(Controller controller) {
		final Properties coreProperties = new Properties();
		InputStream in = null;
		try {
			in = this.getClass().getResource("/core.properties").openStream();
			coreProperties.load(in);
		}
		catch (final IOException e) {			
		}
		
		ResourceController resController = controller.getResourceController();
		
		final URL defaults = this.getClass().getResource(ResourceController.PLUGIN_DEFAULTS_RESOURCE);
		if (defaults == null)
			throw new RuntimeException("cannot open " + ResourceController.PLUGIN_DEFAULTS_RESOURCE);
		resController.addDefaults(defaults);
		
		resController.setProperty(WEB_FREEPLANE_LOCATION, coreProperties.getProperty(WEB_DOCEAR_LOCATION));
		resController.setProperty(BUG_TRACKER_LOCATION, coreProperties.getProperty(DOCEAR_BUG_TRACKER_LOCATION));
		resController.setProperty(HELP_FORUM_LOCATION, coreProperties.getProperty("docear_helpForumLocation"));
		resController.setProperty(FEATURE_TRACKER_LOCATION, coreProperties.getProperty(DOCEAR_FEATURE_TRACKER_LOCATION));
		resController.setProperty(WEB_DOCU_LOCATION, coreProperties.getProperty(DOCEAR_WEB_DOCU_LOCATION));
		resController.setProperty("docu-online", "http://www.docear.org/wp-content/uploads/2012/04/docear-welcome.mm");
		
//		if (resController.getProperty("ApplicationName").equals("Docear")) {
//			resController.setProperty("first_start_map", "/doc/docear-welcome.mm");
//			resController.setProperty("tutorial_map", "/doc/docear-welcome.mm");
//		}
		
		
		if (!resController.getProperty(APPLICATION_NAME, "").equals(DOCEAR)) {
			return;
		}

		//replace if application name is docear
		replaceResourceBundleStrings();
	}
		
	private void addMenus(ModeController modeController) {
		modeController.addMenuContributor(new IMenuContributor() {
			public void updateMenus(ModeController modeController, MenuBuilder builder) {
				builder.addAction("/menu_bar/help", new DocearShowTermsOfUseAction(),	MenuBuilder.AS_CHILD);
				builder.addAction("/menu_bar/help", new DocearShowDataPrivacyStatementAction(),	MenuBuilder.AS_CHILD);
				builder.addAction("/menu_bar/help", new DocearShowDataProcessingTermsAction(),	MenuBuilder.AS_CHILD);
				//builder.addAction("/menu_bar/help", new DocearShowNotificationBar(),	MenuBuilder.AS_CHILD);
				if("true".equals(System.getProperty("docear.debug", "false"))) {
					modeController.addAction(new DocearSetNodePrivacyAction());
					builder.addSeparator("/menu_bar/edit", MenuBuilder.AS_CHILD);
					builder.addAction("/menu_bar/edit", new DocearSetNodePrivacyAction(),	MenuBuilder.AS_CHILD);
					builder.addSeparator("/node_popup", MenuBuilder.AS_CHILD);
					builder.addAction("/node_popup", new DocearSetNodePrivacyAction(),	MenuBuilder.AS_CHILD);
					
				}
			}
		});
		modeController.getUserInputListenerFactory().addToolBar(NotificationBar.TOOLBAR_NAME, ViewController.TOP, new NotificationBar());
		String propertyName = Controller.getCurrentController().getViewController().completeVisiblePropertyKey(NotificationBar.getNotificationBar());
		ResourceController.getResourceController().setProperty(propertyName, false);
	}



	private void setDocearMapWriter(ModeController modeController) {
		DocearMapWriter mapWriter = new DocearMapWriter(modeController.getMapController());
		mapWriter.setMapWriteHandler();		
	}

	private void registerController(ModeController modeController) {
		DocearNodeModifiedExtensionController.install(modeController);
		DocearNodePrivacyExtensionController.install(modeController);
	}

	private void replaceFreeplaneStringsAndActions(ModeController modeController) {
		disableAutoUpdater();
		
		//replace this actions if docear_core is present
		modeController.removeAction("SaveAsAction");
		modeController.addAction(new SaveAsAction());
		modeController.removeAction("SaveAction");
		modeController.addAction(new SaveAction());
		
		
		//remove sidepanel switcher
		//Controller.getCurrentModeController().removeAction("ShowFormatPanel");		
		ResourceController resourceController = ResourceController.getResourceController();		
		
		if (!resourceController.getProperty(APPLICATION_NAME, "").equals(DOCEAR)) {
			return;
		}

		//replace if application name is docear
		replaceResourceBundleStrings();

		replaceActions();
	}

	private void disableAutoUpdater() {
		final OptionPanelController optionController = Controller.getCurrentController().getOptionPanelController();		
		optionController.addPropertyLoadListener(new OptionPanelController.PropertyLoadListener() {
			
			public void propertiesLoaded(Collection<IPropertyControl> properties) {
				((IPropertyControl) optionController.getPropertyControl("check_updates_automatically")).setEnabled(false);
			}
		});
	}

	private void replaceActions() {
		
		ResourceController resourceController = ResourceController.getResourceController();
		
		
		replaceAction(REQUEST_FEATURE_ACTION, new DocearOpenUrlAction(REQUEST_FEATURE_ACTION, resourceController.getProperty(FEATURE_TRACKER_LOCATION)));
		replaceAction(ASK_FOR_HELP, new DocearOpenUrlAction(ASK_FOR_HELP, resourceController.getProperty(HELP_FORUM_LOCATION)));
		replaceAction(REPORT_BUG_ACTION, new DocearOpenUrlAction(REPORT_BUG_ACTION, resourceController.getProperty(BUG_TRACKER_LOCATION)));
		replaceAction(OPEN_FREEPLANE_SITE_ACTION, new DocearOpenUrlAction(OPEN_FREEPLANE_SITE_ACTION, resourceController.getProperty(WEB_FREEPLANE_LOCATION)));
		replaceAction(DOCUMENTATION_ACTION, new DocearOpenUrlAction(DOCUMENTATION_ACTION, resourceController.getProperty(WEB_DOCU_LOCATION)));
		replaceAction("GettingStartedAction", new GettingStartedAction());
		replaceAction("OnlineReference", new OnlineDocumentationAction("OnlineReference", "docu-online"));		
	}

	private void replaceResourceBundleStrings() {
		ResourceController resourceController = ResourceController.getResourceController();
		ResourceBundles bundles = ((ResourceBundles) resourceController.getResources());
		Controller controller = Controller.getCurrentController();

		for (Enumeration<?> i = bundles.getKeys(); i.hasMoreElements();) {
			String key = i.nextElement().toString();
			String value = bundles.getResourceString(key);
			if (value.matches(".*[Ff][Rr][Ee][Ee][Pp][Ll][Aa][Nn][Ee].*")) {
				value = value.replaceAll("[Ff][Rr][Ee][Ee][Pp][Ll][Aa][Nn][Ee]", DOCEAR);
				bundles.putResourceString(key, value);
				if (key.matches(".*[.text]")) {
					key = key.replace(".text", "");
					AFreeplaneAction action = controller.getAction(key);
					if (action != null) {
						MenuBuilder.setLabelAndMnemonic(action, value);
					}
				}
			}
		}		
	}

	private void replaceAction(String actionKey, AFreeplaneAction action) {
		Controller controller = Controller.getCurrentController();
		AFreeplaneAction previousAction = controller.getAction(actionKey);
		if(previousAction != null) {
			controller.removeAction(actionKey);
		}
		controller.addAction(action);
	}

	private void addPluginDefaults(Controller controller) {		
		ResourceController resController = controller.getResourceController();
		if (resController.getProperty("ApplicationName").equals("Docear") && DocearController.getController().isDocearFirstStart()) {			
			resController.setProperty("selection_method", "selection_method_by_click");
			resController.setProperty("links", "relative_to_workspace");
			resController.setProperty("save_folding", "always_save_folding");
			resController.setProperty("leftToolbarVisible", "false");			
			resController.setProperty("styleScrollPaneVisible", "true");
			resController.setProperty(DocearController.DOCEAR_FIRST_RUN_PROPERTY, true);			
		}		
		controller.addAction(new DocearChangeLibraryPathAction());
		controller.addAction(new DocearRenameAction());
	}
	
	private void registerListeners(ModeController modeController) {
		Controller.getCurrentController().getOptionPanelController().addPropertyLoadListener(new PropertyLoadListener());
		Controller.getCurrentController().getResourceController().addPropertyChangeListener(new PropertyListener());
		modeController.getMapController().addMapLifeCycleListener(new MapLifeCycleAndViewListener());
		DocearCoreOmniListenerAdapter adapter = new DocearCoreOmniListenerAdapter();
		modeController.getMapController().addMapLifeCycleListener(adapter);
		modeController.getMapController().addMapChangeListener(adapter);
		modeController.getMapController().addNodeChangeListener(adapter);
		modeController.getMapController().addNodeSelectionListener(adapter);
		DocearController.getController().addDocearEventListener(adapter);
		Controller.getCurrentController().getMapViewManager().addMapViewChangeListener(adapter);
		Controller.getCurrentController().getMapViewManager().addMapViewChangeListener(new MapLifeCycleAndViewListener());
		WorkspaceController.getCurrentModeExtension().getIOController().registerNodeActionListener(AWorkspaceTreeNode.class, WorkspaceActionEvent.WSNODE_OPEN_DOCUMENT, new WorkspaceOpenDocumentListener());
		//WORKSPACE - info
//		WorkspaceUtils.getModel().addTreeModelListener(new WorkspaceTreeModelListener());
	}	
	
	class GettingStartedAction extends AFreeplaneAction {
		
		public GettingStartedAction() {
			super("GettingStartedAction");			
		}

		private static final long serialVersionUID = 1L;

		public void actionPerformed(final ActionEvent e) {
			final ResourceController resourceController = ResourceController.getResourceController();
			final File baseDir = new File(resourceController.getResourceBaseDir()).getAbsoluteFile().getParentFile();
			final String languageCode = resourceController.getLanguageCode();
			final File file = ConfigurationUtils.getLocalizedFile(new File[]{baseDir}, Controller.getCurrentController().getResourceController().getProperty("tutorial_map"), languageCode);
			try {
				final URL endUrl = file.toURI().toURL();
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						try {
							if (endUrl.getFile().endsWith(".mm")) {
								 Controller.getCurrentController().selectMode(MModeController.MODENAME);
								 MMapIO mapIO = (MMapIO) MModeController.getMModeController().getExtension(MapIO.class);
								 mapIO.newDocumentationMap(endUrl);
							}
							else {
								Controller.getCurrentController().getViewController().openDocument(endUrl);
							}
						}
						catch (final Exception e1) {
							LogUtils.severe(e1);
						}
					}
				});
			}
			catch (final MalformedURLException e1) {
				LogUtils.warn(e1);
			}
			
			DocearController.getController().getDocearEventLogger().appendToLog(this, DocearLogEvent.SHOW_HELP);
		}
	}

}

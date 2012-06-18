package org.docear.plugin.core;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;

import javax.swing.SwingUtilities;

import org.docear.plugin.core.actions.DocearAboutAction;
import org.docear.plugin.core.actions.DocearOpenUrlAction;
import org.docear.plugin.core.actions.DocearQuitAction;
import org.docear.plugin.core.actions.DocearShowDataPrivacyStatementAction;
import org.docear.plugin.core.actions.DocearShowDataProcessingTermsAction;
import org.docear.plugin.core.actions.DocearShowTermsOfUseAction;
import org.docear.plugin.core.actions.SaveAction;
import org.docear.plugin.core.actions.SaveAsAction;
import org.docear.plugin.core.features.DocearMapModelController;
import org.docear.plugin.core.features.DocearMapWriter;
import org.docear.plugin.core.features.DocearNodeModelExtensionController;
import org.docear.plugin.core.listeners.MapLifeCycleAndViewListener;
import org.docear.plugin.core.listeners.PropertyListener;
import org.docear.plugin.core.listeners.PropertyLoadListener;
import org.docear.plugin.core.listeners.WorkspaceChangeListener;
import org.docear.plugin.core.listeners.WorkspaceOpenDocumentListener;
import org.docear.plugin.core.logger.DocearLogEvent;
import org.docear.plugin.core.mindmap.MapConverter;
import org.docear.plugin.core.ui.NotificationBar;
import org.docear.plugin.core.workspace.actions.DocearChangeLibraryPathAction;
import org.docear.plugin.core.workspace.actions.DocearRenameAction;
import org.docear.plugin.core.workspace.actions.WorkspaceChangeLocationsAction;
import org.docear.plugin.core.workspace.node.config.NodeAttributeObserver;
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
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.ui.ViewController;
import org.freeplane.features.url.UrlManager;
import org.freeplane.features.url.mindmapmode.MapVersionInterpreter;
import org.freeplane.plugin.workspace.WorkspaceController;
import org.freeplane.plugin.workspace.WorkspacePreferences;
import org.freeplane.plugin.workspace.WorkspaceUtils;
import org.freeplane.plugin.workspace.event.WorkspaceActionEvent;
import org.freeplane.plugin.workspace.model.AWorkspaceTreeNode;

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
	
	private static final WorkspaceChangeListener WORKSPACE_CHANGE_LISTENER = new WorkspaceChangeListener();
		
	public static final NodeAttributeObserver projectPathObserver = new NodeAttributeObserver();
	public static final NodeAttributeObserver referencePathObserver = new NodeAttributeObserver();
	public static final NodeAttributeObserver repositoryPathObserver = new NodeAttributeObserver();
	
		
	public CoreConfiguration(ModeController modeController) {			
		LogUtils.info("org.docear.plugin.core.CoreConfiguration() initializing...");
		
		init(modeController);
	}
	
	
	
	private void init(ModeController modeController) {
		DocearController.getController().getDocearEventLogger().appendToLog(this, DocearLogEvent.APPLICATION_STARTED);
		Toolkit.getDefaultToolkit();		
		DocearController.getController().getDocearEventLogger().appendToLog(this, DocearLogEvent.OS_OPERATING_SYSTEM, System.getProperty("os.name"));
		DocearController.getController().getDocearEventLogger().appendToLog(this, DocearLogEvent.OS_LANGUAGE_CODE, System.getProperty("user.language"));
		DocearController.getController().getDocearEventLogger().appendToLog(this, DocearLogEvent.OS_COUNTRY_CODE, System.getProperty("user.country"));
		DocearController.getController().getDocearEventLogger().appendToLog(this, DocearLogEvent.OS_TIME_ZONE, System.getProperty("user.timezone"));
		DocearController.getController().getDocearEventLogger().appendToLog(this, DocearLogEvent.OS_SCREEN_RESOLUTION, Toolkit.getDefaultToolkit().getScreenSize().toString());
		
		MapVersionInterpreter.addMapVersionInterpreter(new MapVersionInterpreter("0.9.0\" software_name=\"SciPlore_", false, false, "SciploreMM", "http://sciplore.org", null, new MapConverter()));
		
		// set up context menu for workspace
		WorkspaceController.getController().addWorkspaceListener(WORKSPACE_CHANGE_LISTENER);
		
		Controller.getCurrentController().addAction(new WorkspaceChangeLocationsAction());
		Controller.getCurrentController().addAction(new DocearChangeLibraryPathAction());
		Controller.getCurrentController().addAction(new DocearRenameAction());
		
		addPluginDefaults();
		addMenus(modeController);
		registerListeners(modeController);
		//prepareWorkspace();
		
		replaceFreeplaneStringsAndActions(modeController);
		DocearMapModelController.install(new DocearMapModelController(modeController));
		
		setDocearMapWriter(modeController);
		
		registerController(modeController);
		URI uri = CoreConfiguration.projectPathObserver.getUri();
		if (uri != null) {
			UrlManager.getController().setLastCurrentDir(WorkspaceUtils.resolveURI(CoreConfiguration.projectPathObserver.getUri()));
		}		
	}
		
	private void addMenus(ModeController modeController) {
		modeController.addMenuContributor(new IMenuContributor() {
			public void updateMenus(ModeController modeController, MenuBuilder builder) {
				builder.addAction("/menu_bar/help", new DocearShowTermsOfUseAction(),	MenuBuilder.AS_CHILD);
				builder.addAction("/menu_bar/help", new DocearShowDataPrivacyStatementAction(),	MenuBuilder.AS_CHILD);
				builder.addAction("/menu_bar/help", new DocearShowDataProcessingTermsAction(),	MenuBuilder.AS_CHILD);
				//builder.addAction("/menu_bar/help", new DocearShowNotificationBar(),	MenuBuilder.AS_CHILD);
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
		DocearNodeModelExtensionController.install(new DocearNodeModelExtensionController(modeController));		
	}

	private void replaceFreeplaneStringsAndActions(ModeController modeController) {
		disableAutoUpdater();
		
		ResourceController resourceController = ResourceController.getResourceController();		
		
		//replace this actions if docear_core is present
		Controller.getCurrentModeController().removeAction("SaveAsAction");
		Controller.getCurrentModeController().addAction(new SaveAsAction());
		Controller.getCurrentModeController().removeAction("SaveAction");
		Controller.getCurrentModeController().addAction(new SaveAction());
		
		
		//remove sidepanel switcher
		//Controller.getCurrentModeController().removeAction("ShowFormatPanel");
		modeController.addMenuContributor(new IMenuContributor() {
			public void updateMenus(ModeController modeController,final  MenuBuilder builder) {
				SwingUtilities.invokeLater(new Runnable() {					
					public void run() {
						builder.removeElement("$" + WorkspacePreferences.SHOW_WORKSPACE_MENUITEM + "$0");
					}
				});
													
			}
		});
		
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
		AFreeplaneAction action;
		resourceController.setProperty(WEB_FREEPLANE_LOCATION, resourceController.getProperty(WEB_DOCEAR_LOCATION));
		resourceController.setProperty(BUG_TRACKER_LOCATION, resourceController.getProperty(DOCEAR_BUG_TRACKER_LOCATION));
		resourceController.setProperty(HELP_FORUM_LOCATION, resourceController.getProperty("docear_helpForumLocation"));
		resourceController.setProperty(FEATURE_TRACKER_LOCATION, resourceController.getProperty(DOCEAR_FEATURE_TRACKER_LOCATION));
		resourceController.setProperty(WEB_DOCU_LOCATION, resourceController.getProperty(DOCEAR_WEB_DOCU_LOCATION));
		resourceController.setProperty("docu-online", "http://www.docear.org/wp-content/uploads/2012/04/docear-welcome.mm");
		
		replaceAction(REQUEST_FEATURE_ACTION, new DocearOpenUrlAction(REQUEST_FEATURE_ACTION, resourceController.getProperty(FEATURE_TRACKER_LOCATION)));
		replaceAction(ASK_FOR_HELP, new DocearOpenUrlAction(ASK_FOR_HELP, resourceController.getProperty(HELP_FORUM_LOCATION)));
		replaceAction(REPORT_BUG_ACTION, new DocearOpenUrlAction(REPORT_BUG_ACTION, resourceController.getProperty(BUG_TRACKER_LOCATION)));
		replaceAction(OPEN_FREEPLANE_SITE_ACTION, new DocearOpenUrlAction(OPEN_FREEPLANE_SITE_ACTION, resourceController.getProperty(WEB_FREEPLANE_LOCATION)));
		replaceAction(DOCUMENTATION_ACTION, new DocearOpenUrlAction(DOCUMENTATION_ACTION, resourceController.getProperty(WEB_DOCU_LOCATION)));
		replaceAction("GettingStartedAction", new GettingStartedAction());
		replaceAction("OnlineReference", new OnlineDocumentationAction("OnlineReference", "docu-online"));
		
		action = new DocearAboutAction();
		replaceAction(action.getKey(), action);
		
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

		controller.removeAction(actionKey);
		controller.addAction(action);
	}

	private void addPluginDefaults() {
		
		ResourceController resController = Controller.getCurrentController().getResourceController();
		if (resController.getProperty("ApplicationName").equals("Docear")) {
			resController.setProperty("first_start_map", "/doc/docear-welcome.mm");
			resController.setProperty("tutorial_map", "/doc/docear-welcome.mm");
		}
		final URL defaults = this.getClass().getResource(ResourceController.PLUGIN_DEFAULTS_RESOURCE);
		if (defaults == null)
			throw new RuntimeException("cannot open " + ResourceController.PLUGIN_DEFAULTS_RESOURCE);
		Controller.getCurrentController().getResourceController().addDefaults(defaults);
		if (resController.getProperty("ApplicationName").equals("Docear") && DocearController.getController().isDocearFirstStart()) {
			Controller.getCurrentController().getResourceController().setProperty("selection_method", "selection_method_by_click");
			Controller.getCurrentController().getResourceController().setProperty("links", "relative_to_workspace");
			Controller.getCurrentController().getResourceController().setProperty("save_folding", "always_save_folding");
			Controller.getCurrentController().getResourceController().setProperty("leftToolbarVisible", "false");			
			Controller.getCurrentController().getResourceController().setProperty("styleScrollPaneVisible", "true");
			Controller.getCurrentController().getResourceController().setProperty(DocearController.DOCEAR_FIRST_RUN_PROPERTY, true);			
		}
		AFreeplaneAction previousAction = Controller.getCurrentController().getAction("QuitAction");
		if(previousAction != null) {
			Controller.getCurrentController().removeAction("QuitAction");
		}
		Controller.getCurrentController().addAction(new DocearQuitAction());
	}
	
	private void registerListeners(ModeController modeController) {
		Controller.getCurrentController().getOptionPanelController().addPropertyLoadListener(new PropertyLoadListener());
		Controller.getCurrentController().getResourceController().addPropertyChangeListener(new PropertyListener());
		modeController.getMapController().addMapLifeCycleListener(new MapLifeCycleAndViewListener());
		Controller.getCurrentController().getMapViewManager().addMapViewChangeListener(new MapLifeCycleAndViewListener());
		WorkspaceController.getIOController().registerNodeActionListener(AWorkspaceTreeNode.class, WorkspaceActionEvent.WSNODE_OPEN_DOCUMENT, new WorkspaceOpenDocumentListener());
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
				final URL endUrl = file.toURL();
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						try {
							if (endUrl.getFile().endsWith(".mm")) {
								 Controller.getCurrentController().selectMode(MModeController.MODENAME);
								 ((MMapController)Controller.getCurrentModeController().getMapController()).newDocumentationMap(endUrl);
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

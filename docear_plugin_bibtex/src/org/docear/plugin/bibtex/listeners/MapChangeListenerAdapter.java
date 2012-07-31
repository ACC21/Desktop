package org.docear.plugin.bibtex.listeners;

import java.net.URI;

import javax.swing.SwingUtilities;

import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.export.DocearReferenceUpdateController;

import org.docear.plugin.bibtex.ReferenceUpdater;
import org.docear.plugin.bibtex.ReferencesController;
import org.docear.plugin.bibtex.jabref.JabRefAttributes;
import org.docear.plugin.bibtex.jabref.ResolveDuplicateEntryAbortedException;
import org.docear.plugin.core.features.AnnotationID;
import org.docear.plugin.core.features.AnnotationModel;
import org.docear.plugin.core.features.IAnnotation.AnnotationType;
import org.docear.plugin.core.mindmap.AnnotationController;
import org.docear.plugin.core.mindmap.MindmapUpdateController;
import org.docear.plugin.core.util.Tools;
import org.docear.plugin.pdfutilities.pdf.PdfFileFilter;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.map.AMapChangeListenerAdapter;
import org.freeplane.features.map.MapChangeEvent;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeChangeEvent;
import org.freeplane.features.map.NodeModel;

public class MapChangeListenerAdapter extends AMapChangeListenerAdapter {

	public void mapChanged(MapChangeEvent event) {
	}

	public void onNodeDeleted(NodeModel parent, NodeModel child, int index) {
	}

	public void onNodeInserted(NodeModel parent, NodeModel child, int newIndex) {	
	}

	public void onNodeMoved(NodeModel oldParent, int oldIndex, NodeModel newParent, NodeModel child, int newIndex) {
	}

	public void onPreNodeDelete(NodeModel oldParent, NodeModel selectedNode, int index) {
	}

	public void onPreNodeMoved(NodeModel oldParent, int oldIndex, NodeModel newParent, NodeModel child, int newIndex) {
	}

	public void nodeChanged(NodeChangeEvent event) {
		if (event.getProperty().equals(NodeModel.HYPERLINK_CHANGED)) {
			URI newUri = (URI) event.getNewValue();
			if (newUri != null) {
				try{
					if(new PdfFileFilter().accept(Tools.getFilefromUri(Tools.getAbsoluteUri(newUri)))){
						if(AnnotationController.getModel(event.getNode(), false) == null){
							AnnotationModel model = new AnnotationModel();
							model.setAnnotationID(new AnnotationID(newUri, 0));
							model.setAnnotationType(AnnotationType.PDF_FILE);
							AnnotationController.setModel(event.getNode(), model);
						}
					}
				}
				catch(Exception e){
					LogUtils.warn(e);
				}
				JabRefAttributes jabRefAttributes = ReferencesController.getController().getJabRefAttributes();				
				try {					
					BibtexEntry entry = jabRefAttributes.findBibtexEntryForPDF(newUri, event.getNode().getMap());
					if (entry == null) {
						entry = jabRefAttributes.findBibtexEntryForURL(newUri, event.getNode().getMap(), false);
					}
					if (entry != null) {					
						jabRefAttributes.setReferenceToNode(entry, event.getNode());
						if (jabRefAttributes.isNodeDirty()) {
							jabRefAttributes.setNodeDirty(false);
    						SwingUtilities.invokeLater(new Runnable() {					
    							@Override
    							public void run() {
    								if (DocearReferenceUpdateController.isLocked()) {
    									return;
    								}
    								DocearReferenceUpdateController.lock();
    								try {
    									MindmapUpdateController mindmapUpdateController = new MindmapUpdateController(true);
    									mindmapUpdateController.addMindmapUpdater(new ReferenceUpdater(TextUtils.getText("update_references_open_mindmaps")));
    									mindmapUpdateController.updateCurrentMindmap(true);
    								}
    								finally {
    									DocearReferenceUpdateController.unlock();
    								}
    							}
    						});
						}
					}
				}
				catch (ResolveDuplicateEntryAbortedException e) {
					System.out.println("MapChangeListenerAdapter.nodeChanged interrupted");
					jabRefAttributes.removeLinkFromNode(event.getNode());
					return;
				}
			}
			if(newUri == null && AnnotationController.getModel(event.getNode(), false) != null){
				AnnotationController.setModel(event.getNode(), null);
			}
		}
	}

	public void onCreate(MapModel map) {
	}

	public void onRemove(MapModel map) {
	}

	public void onSavedAs(MapModel map) {
		ReferencesController.getController().getJabrefWrapper().getJabrefFrame();
		try {
			saveJabrefDatabase();
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}

	}

	public void onSaved(MapModel map) {
		ReferencesController.getController().getJabrefWrapper().getJabrefFrame();
		try {
			saveJabrefDatabase();
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}
	}

	private void saveJabrefDatabase() {
		BasePanel basePanel = ReferencesController.getController().getJabrefWrapper().getBasePanel();
		if(basePanel != null && basePanel.isBaseChanged()) {
			basePanel.runCommand("save");
		}

	}
}

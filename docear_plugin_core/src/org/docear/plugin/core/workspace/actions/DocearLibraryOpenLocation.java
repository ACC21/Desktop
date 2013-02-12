package org.docear.plugin.core.workspace.actions;

import java.awt.event.ActionEvent;

import org.docear.plugin.core.IDocearLibrary;
import org.freeplane.plugin.workspace.WorkspaceController;
import org.freeplane.plugin.workspace.model.AWorkspaceTreeNode;

public class DocearLibraryOpenLocation extends NodeOpenLocationAction {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DocearLibraryOpenLocation() {
		super();
	}
	
	public void actionPerformed(ActionEvent event) {
		AWorkspaceTreeNode targetNode = getNodeFromActionEvent(event);
		if(targetNode instanceof IDocearLibrary) {
			this.openFolder(WorkspaceController.resolveURI(((IDocearLibrary) targetNode).getLibraryPath()));
		}
		else {
			super.actionPerformed(event);
		}
	}

}

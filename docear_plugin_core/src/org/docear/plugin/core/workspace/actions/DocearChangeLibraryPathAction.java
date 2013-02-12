/**
 * author: Marcel Genzmehr
 * 21.11.2011
 */
package org.docear.plugin.core.workspace.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.net.URI;

import javax.swing.filechooser.FileFilter;

import org.freeplane.core.util.TextUtils;
import org.freeplane.plugin.workspace.actions.AWorkspaceAction;

public class DocearChangeLibraryPathAction extends AWorkspaceAction {

	private static final long serialVersionUID = 1L;
	private final String bibFilterDescription = "*.bib ("+TextUtils.getText("locationdialog.filefilter.bib")+")";
	
	/***********************************************************************************
	 * CONSTRUCTORS
	 **********************************************************************************/

	public DocearChangeLibraryPathAction() {
		super("workspace.action.docear.uri.change");
	}
	/***********************************************************************************
	 * METHODS
	 **********************************************************************************/

	/**
	 * @param oldLocation 
	 * @param directory 
	 * @return
	 */
	private URI requestUri(URI oldLocation, boolean directory, Component explanation, FileFilter... fileFilters) {
//		LocationDialogPanel locDialog = new LocationDialogPanel(oldLocation, directory, fileFilters);
//		if (explanation != null) {
//			locDialog.setExplanation(explanation);
//		}
//		int option = JOptionPane.showConfirmDialog(UITools.getFrame(), locDialog, TextUtils.getRawText(getKey()+".title"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
//		if(option == JOptionPane.OK_OPTION) {
//			return locDialog.getLocationUri();
//		}
		return null;
	}
	
	/***********************************************************************************
	 * REQUIRED METHODS FOR INTERFACES
	 **********************************************************************************/
	
	public void actionPerformed(ActionEvent e) {
//		AWorkspaceTreeNode targetNode = getNodeFromActionEvent(e);
//		if(targetNode instanceof FolderTypeProjectsNode ) {
//			URI uri = requestUri(((FolderTypeProjectsNode) targetNode).getPath(), true, null);
//			if(uri != null) {
//				((FolderTypeProjectsNode)targetNode).setPath(uri);
//			}
//		} 
//		else if(targetNode instanceof FolderTypeLiteratureRepositoryNode) {
//			URI uri = requestUri(((FolderTypeLiteratureRepositoryNode) targetNode).getPath(), true, null);
//			if(uri != null) {
//				((FolderTypeLiteratureRepositoryNode)targetNode).setPath(uri);
//			}
//		}
//		else if(targetNode instanceof LinkTypeReferencesNode) {
//			FileFilter bibFilter = new FileFilter() {
//				
//				public String getDescription() {
//					return bibFilterDescription;
//				}
//				
//				public boolean accept(File f) {
//					return (f.isDirectory() || f.getName().endsWith(".bib"));
//				}
//			};
//			JHyperlink hyperlink = new JHyperlink(TextUtils.getText("bibtex_mendeley_help"), TextUtils.getText("bibtex_mendeley_help_uri"));
//			URI uri = requestUri(((LinkTypeReferencesNode) targetNode).getLinkPath(), false, hyperlink, bibFilter);
//			if(uri != null) {
//				((LinkTypeReferencesNode)targetNode).setLinkPath(uri);
//			}
//		}
//		targetNode.refresh();

	}

	
}

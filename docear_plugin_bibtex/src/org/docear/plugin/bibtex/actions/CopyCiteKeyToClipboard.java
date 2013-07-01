package org.docear.plugin.bibtex.actions;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Collection;


import org.docear.plugin.bibtex.ReferencesController;
import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.ui.EnabledAction;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;

@EnabledAction(checkOnNodeChange=true)
public class CopyCiteKeyToClipboard extends AFreeplaneAction{

	public final static String KEY = "CopyCiteKeyToClipboardAction";
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public CopyCiteKeyToClipboard() {
		super(KEY);
	}

	public void actionPerformed(ActionEvent e) {
		Collection<NodeModel> nodes = Controller.getCurrentModeController().getMapController().getSelectedNodes();
		String strBuffer = "";
		for(NodeModel node : nodes) {
			String bibKey = ReferencesController.getController().getJabRefAttributes().getBibtexKey(node);			
			if(bibKey != null && strBuffer.indexOf(bibKey) == -1) {
				if(!"".equals(strBuffer)) {
					strBuffer += ",";
				}
				strBuffer += bibKey;
			}
		}
		final String citeKeys = "\\cite{"+strBuffer+"}";
		Transferable content = new Transferable() {
			public boolean isDataFlavorSupported(DataFlavor flavor) {
				return DataFlavor.stringFlavor.equals(flavor);
			}
			
			public DataFlavor[] getTransferDataFlavors() {
				return new DataFlavor[]{DataFlavor.stringFlavor};
			}
			
			public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
				if(DataFlavor.stringFlavor.equals(flavor)) {
					return citeKeys;
				}
				throw new UnsupportedFlavorException(flavor);
			}
		};
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(content, null);
		
	}
	
	public void setEnabled() {
		NodeModel node = Controller.getCurrentModeController().getMapController().getSelectedNode();
		if (node == null) {
			setEnabled(false);
			return;
		}
		final String bibtexKey = ReferencesController.getController().getJabRefAttributes().getBibtexKey(node);
		
		if (bibtexKey != null && bibtexKey.length()>0) {
			setEnabled(true);
		}
		else {
			setEnabled(false);
		}
		
	}
}

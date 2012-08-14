package org.docear.plugin.services.communications.components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JToolBar;
import javax.swing.border.EmptyBorder;

import org.docear.plugin.core.DocearController;
import org.docear.plugin.core.event.DocearEvent;
import org.docear.plugin.services.communications.CommunicationsController;
import org.freeplane.core.util.TextUtils;

public class WorkspaceDocearServiceConnectionBar extends JToolBar {
	
	public static enum CONNECTION_STATE {
		CONNECTED,
		DISCONNECTED,
		UPLOADING,
		DISABLED,
		NO_CREDENTIALS
	}

	private static final long serialVersionUID = 1L;
	public static final String ACTION_COMMAND_TOGGLE_CONNECTION_STATE = "toggle_connection_state";
	
	protected static Insets nullInsets = new Insets(0, 0, 0, 0);
	protected static Insets marginInsets = new Insets(2, 2, 2, 2);
	
	private static Icon onIcon = new ImageIcon(WorkspaceDocearServiceConnectionBar.class.getResource("/icons/arrow-refresh-on.png"));
	private static Icon offIcon = new ImageIcon(WorkspaceDocearServiceConnectionBar.class.getResource("/icons/arrow-refresh-off.png"));
	
	
	
	private final JButton button;
	private final JLabel lblUsername;
	private final JLabel lblConnectionState;
	private final MouseListener mouseClickDispatcher = new MouseListener() {
		
		public void mouseReleased(MouseEvent e) {}		
		public void mousePressed(MouseEvent e) {}		
		public void mouseExited(MouseEvent e) {}		
		public void mouseEntered(MouseEvent e) {}
		
		public void mouseClicked(MouseEvent e) {
			DocearController.getController().dispatchDocearEvent(new DocearEvent(CommunicationsController.CONNECTION_BAR_CLICKED, e));
		}
	}; 
	
	
	private CONNECTION_STATE connectionState;
	
	public WorkspaceDocearServiceConnectionBar() {
		setMargin(nullInsets);
		setFloatable(false);
		setRollover(true);
		
		button = add(new AbstractAction("Connection", onIcon) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				DocearController.getController().dispatchDocearEvent(new DocearEvent(WorkspaceDocearServiceConnectionBar.this, ACTION_COMMAND_TOGGLE_CONNECTION_STATE));				
			}
		});
		configureComponent(button);
		button.setDisabledIcon(new ImageIcon(this.getClass().getResource("/icons/arrow-refresh-disabled.png")));
		
		lblUsername = new JLabel("username");
		lblUsername.setBorder(new EmptyBorder(marginInsets));	
		lblUsername.addMouseListener(mouseClickDispatcher);
		add(lblUsername);
		
		lblConnectionState = new JLabel(TextUtils.getText("docear.service.connect.bar.status.1")+":");
		lblConnectionState.setBorder(new EmptyBorder(marginInsets));
		lblConnectionState.addMouseListener(mouseClickDispatcher);
		add(lblConnectionState);
		
		
	}
	
	
	/***********************************************************************************
	 * METHODS
	 **********************************************************************************/
	public void setUsername(String name) {
		this.lblUsername.setText(name);
	}
	
	public void setConnectionState(CONNECTION_STATE state) {
		connectionState = state;
		this.lblConnectionState.setText(TextUtils.getText("docear.service.connect.bar.status."+state.ordinal()));
	}
	
	public CONNECTION_STATE getConnectionState() {
		return connectionState;
	}
	
	public void setEnabled(boolean enabled) {
		this.button.setEnabled(enabled);
		lblUsername.setEnabled(enabled);
		lblConnectionState.setEnabled(enabled);
		super.setEnabled(enabled);
	}
	
	public void allowTransmission(boolean enabled) {
		if(enabled) {
			button.setIcon(onIcon);
		}
		else {
			button.setIcon(offIcon);
		}
		button.repaint();
	}
	
 	
	protected void configureComponent(final Component comp) {
		if (!(comp instanceof AbstractButton)) {
			return;
		}
		final AbstractButton abstractButton = (AbstractButton) comp;
		final String actionName = (String) abstractButton.getAction().getValue(Action.NAME);
		abstractButton.setName(actionName);
		if (null != abstractButton.getIcon()) {
			final String text = abstractButton.getText();
			final String toolTipText = abstractButton.getToolTipText();
			if (text != null) {
				if (toolTipText == null) {
					abstractButton.setToolTipText(text);
				}
				abstractButton.setText(null);
			}
		}
		if (System.getProperty("os.name").equals("Mac OS X")) {
			abstractButton.putClientProperty("JButton.buttonType", "segmented");
			abstractButton.putClientProperty("JButton.segmentPosition", "middle");
			final Dimension buttonSize = new Dimension(22, 22);
			abstractButton.setPreferredSize(buttonSize);
			abstractButton.setFocusPainted(false);
		}
		abstractButton.setFocusable(false);
		abstractButton.setMargin(nullInsets);
	}
}

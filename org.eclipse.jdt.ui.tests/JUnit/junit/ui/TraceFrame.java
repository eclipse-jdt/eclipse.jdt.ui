package junit.ui;

import java.io.*;
import java.awt.*;
import java.awt.event.*;

import junit.framework.*;
import junit.util.*;

class TraceFrame extends Frame {
	private Button fButton= null;
	private TextArea fTextArea= null;
	public TraceFrame() {
		setLayout(new GridBagLayout());
		setBackground(SystemColor.control);
		setSize(403, 236);
		setTitle("Stack Trace");

		fTextArea= new TextArea();
		fTextArea.setRows(10);
		fTextArea.setColumns(60);

		fButton= new Button("Close");
		fButton.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dispose();
				}
			}
		);

		GridBagConstraints constraintsStackTextArea = new GridBagConstraints();
		constraintsStackTextArea.gridx = 0; constraintsStackTextArea.gridy = 0;
		constraintsStackTextArea.gridwidth = 1; constraintsStackTextArea.gridheight = 1;
		constraintsStackTextArea.fill = GridBagConstraints.BOTH;
		constraintsStackTextArea.anchor = GridBagConstraints.CENTER;
		constraintsStackTextArea.weightx = 1.0;
		constraintsStackTextArea.weighty = 1.0;
		constraintsStackTextArea.insets = new Insets(8, 8, 8, 8);
		add(fTextArea, constraintsStackTextArea);

		GridBagConstraints constraintsCloseButton = new GridBagConstraints();
		constraintsCloseButton.gridx = 0; constraintsCloseButton.gridy = 1;
		constraintsCloseButton.gridwidth = 1; constraintsCloseButton.gridheight = 1;
		constraintsCloseButton.anchor = java.awt.GridBagConstraints.EAST;
		constraintsCloseButton.weightx = 0.0;
		constraintsCloseButton.weighty = 0.0;
		constraintsCloseButton.insets = new Insets(0, 8, 8, 8);
		add(fButton, constraintsCloseButton);
		
		addWindowListener(
			new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					dispose();
				}
			}
		);
	
		//{{INIT_CONTROLS
		setLayout(new BorderLayout(0,0));
		setBackground(java.awt.Color.orange);
		setSize(0,0);
		setVisible(false);
		//}}
	}
	/**
	 * Shows the stack trace of the passed in throwable
	 */
	public void showTrace(Throwable t) {
		StringWriter stringWriter= new StringWriter();
		PrintWriter writer= new PrintWriter(stringWriter);
		t.printStackTrace(writer);
		StringBuffer buffer= stringWriter.getBuffer();
		fTextArea.setText(StringUtil.truncate(buffer.toString(), 5000));
	}
	//{{DECLARE_CONTROLS
	//}}
	//{{DECLARE_MENUS
	//}}
}
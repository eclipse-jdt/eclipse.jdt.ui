package org.eclipse.jdt.internal.junit.launcher;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class NameValuePairDialog extends Dialog {

	private String fName;
	private String fValue;

	private String fTitle;
	private String[] fFieldLabels;
	private String[] fInitialValues;
	
	private Label fNameLabel;
	private Text fNameText;
	private Label fValueLabel;
	private Text fValueText;

	public NameValuePairDialog(Shell shell, String title, String[] fieldLabels, String[] initialValues) {
		super(shell);
		fTitle = title;
		fFieldLabels = fieldLabels;
		fInitialValues = initialValues;
	}

	/**
	 * @see Dialog#createDialogArea(Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite comp = new Composite(parent, SWT.NULL);
		GridLayout topLayout = new GridLayout();
		topLayout.numColumns = 2;
		comp.setLayout(topLayout);
		GridData gd;
		
		fNameLabel = new Label(comp, SWT.NONE);
		fNameLabel.setText(fFieldLabels[0]);
		
		fNameText = new Text(comp, SWT.BORDER | SWT.SINGLE);
		fNameText.setText(fInitialValues[0]);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 300;
		fNameText.setLayoutData(gd);
		
		fValueLabel = new Label(comp, SWT.NONE);
		fValueLabel.setText(fFieldLabels[1]);
		
		fValueText = new Text(comp, SWT.BORDER | SWT.SINGLE);
		fValueText.setText(fInitialValues[1]);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 300;
		fValueText.setLayoutData(gd);
		
		return comp;
	}
	
	/**
	 * Return the name/value pair entered in this dialog.  If the cancel button was hit,
	 * both will be <code>null</code>.
	 */
	public String[] getNameValuePair() {
		return new String[] {fName, fValue};
	}
	
	/**
	 * @see Dialog#buttonPressed(int)
	 */
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			fName= fNameText.getText();
			fValue = fValueText.getText();
		} else {
			fName = null;
			fValue = null;
		}
		super.buttonPressed(buttonId);
	}
	
	/**
	 * @see Window#configureShell(Shell)
	 */
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		if (fTitle != null) {
			shell.setText(fTitle);
		}
	}
}

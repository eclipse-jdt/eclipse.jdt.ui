/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IInputValidator;

import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;

/**
 * A replacement for the JFace.InputDialog (improved layout)
 */
public class StringInputDialog extends StatusDialog {
	
	private StringDialogField fNameDialogField;
	private StatusInfo fNameStatus;	
	private IInputValidator fValidator;
		
	public StringInputDialog(Shell parent, String title, Image image, String message, String input, IInputValidator validator) {
		super(parent);
		setTitle(title);
		setImage(image);
		fValidator= validator;
		fNameStatus= new StatusInfo();
		fNameDialogField= new StringDialogField();
		fNameDialogField.setLabelText(message);
		fNameDialogField.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				doValidation();
			}		
		});
		fNameDialogField.setText(input);
	}
	
	public String getValue() {
		return fNameDialogField.getText();
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite)super.createDialogArea(parent);
		MGridLayout layout= new MGridLayout();
		layout.minimumWidth= 300;
		layout.numColumns= 1;
		layout.marginWidth= 0;
		layout.marginHeight= 0;	
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		// will create two controls on two lines: message and input field
		fNameDialogField.doFillIntoGrid(composite, 2);
				
		fNameDialogField.postSetFocusOnDialogField(parent.getDisplay());
		return composite;
	}
	
	protected void doValidation() {
		String val= fValidator.isValid(fNameDialogField.getText());
		if (val == null) {
			fNameStatus.setOK();
		} else {
			fNameStatus.setError(val);
		}
		updateStatus(fNameStatus);
	}	

}
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IInputValidator;

import java.text.MessageFormat;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.Separator;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridData;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridUtil;


public class NameClashDialog extends StatusDialog {
	
	private SelectionButtonDialogField fReplaceRadioButton;
	private SelectionButtonDialogField fRenameRadioButton;
	
	private StringDialogField fNameDialogField;
	private StatusInfo fDialogStatus;		
	private IInputValidator fValidator;
	
	private String fMessage;
	
	public NameClashDialog(Shell parent, IInputValidator validator, String initialInput, boolean allowReplace) {
		super(parent);
		setTitle(ReorgMessages.getString("nameClashDialog.title")); //$NON-NLS-1$
		fMessage= MessageFormat.format(ReorgMessages.getString("nameClashDialog.message"), new String[] { initialInput }); //$NON-NLS-1$
		
		fValidator= validator;
		fDialogStatus= new StatusInfo();
		
		IDialogFieldListener listener= new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				doFieldChanged();
			}		
		};
		
		fReplaceRadioButton= new SelectionButtonDialogField(SWT.RADIO);
		fReplaceRadioButton.setDialogFieldListener(listener);
		fReplaceRadioButton.setLabelText(ReorgMessages.getString("nameClashDialog.replace")); //$NON-NLS-1$
		
		fRenameRadioButton= new SelectionButtonDialogField(SWT.RADIO);
		fRenameRadioButton.setDialogFieldListener(listener);
		fRenameRadioButton.setLabelText(ReorgMessages.getString("nameClashDialog.rename")); //$NON-NLS-1$
		
		fNameDialogField= new StringDialogField();
		fNameDialogField.setDialogFieldListener(listener);
		
		fRenameRadioButton.attachDialogField(fNameDialogField);
		
		if (allowReplace) {
			fReplaceRadioButton.setSelection(true);
			fRenameRadioButton.setSelection(false);
		} else {
			fReplaceRadioButton.setSelection(false);
			fReplaceRadioButton.setEnabled(false);
			fRenameRadioButton.setSelection(true);
		}
		fNameDialogField.setText(initialInput);
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		MGridLayout layout= new MGridLayout();
		layout.minimumWidth= convertWidthInCharsToPixels(80);
		layout.numColumns= 1;
		composite.setLayout(layout);

		DialogField label= new DialogField();
		label.setLabelText(fMessage);
		label.doFillIntoGrid(composite, 1);
		
		(new Separator()).doFillIntoGrid(composite, 1, 1);
		
		fReplaceRadioButton.doFillIntoGrid(composite, 1);
		fRenameRadioButton.doFillIntoGrid(composite, 1);
		Control textControl= fNameDialogField.getTextControl(composite);
		
		MGridData gd= MGridUtil.createHorizontalFill();
		gd.horizontalIndent= convertHorizontalDLUsToPixels(15);
		textControl.setLayoutData(gd);
		return composite;
	}
	
	protected void doFieldChanged() {
		if (fRenameRadioButton.isSelected()) {
			String val= fValidator.isValid(fNameDialogField.getText());
			if (val == null) {
				fDialogStatus.setOK();
			} else {
				fDialogStatus.setError(val);
			}
		} else {
			fDialogStatus.setOK();
		}
		updateStatus(fDialogStatus);
	}
	
	public boolean isReplace() {
		return fReplaceRadioButton.isSelected();
	}
	
	public String getNewName() {
		return fNameDialogField.getText();
	}
}
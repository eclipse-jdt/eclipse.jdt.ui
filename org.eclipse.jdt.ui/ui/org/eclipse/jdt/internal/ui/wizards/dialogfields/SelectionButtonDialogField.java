/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.wizards.dialogfields;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jdt.internal.ui.wizards.swt.MGridData;

/**
 * Radio or checkbox button
 */

public class SelectionButtonDialogField extends DialogField {
	
	private Button fButton;
	private boolean fIsSelected;
	private DialogField[] fAttachedDialogFields;
	private int fButtonStyle;

	/**
	 * styles: SWT.RADIO, SWT.CHECK, SWT.TOGGLE, SWT.PUSH
	 */
	public SelectionButtonDialogField(int buttonStyle) {
		super();
		fIsSelected= false;
		fAttachedDialogFields= null;
		fButtonStyle= buttonStyle;
	}
	
	public void attachDialogField(DialogField dialogField) {
		attachDialogFields(new DialogField[] { dialogField });
	}
	
	public void attachDialogFields(DialogField[] dialogFields) {
		fAttachedDialogFields= dialogFields;
		for (int i= 0; i < dialogFields.length; i++) {
			dialogFields[i].setEnabled(fIsSelected);
		}
	}	
	
	public boolean isAttached(DialogField editor) {
		if (fAttachedDialogFields != null) {
			for (int i=0; i < fAttachedDialogFields.length; i++) {
				if (fAttachedDialogFields[i] == editor) {
					return true;
				}
			}
		}
		return false;
	}
	
	// ------- layout helpers
		
	public Control[] doFillIntoGrid(Composite parent, int nColumns) {
		assertEnoughColumns(nColumns);
		
		Button rbutton= getSelectionButton(parent);
		MGridData gd= new MGridData();
		gd.horizontalSpan= nColumns;
		gd.horizontalAlignment= MGridData.FILL;
		rbutton.setLayoutData(gd);
		
		return new Control[] { rbutton };
	}	
	
	public int getNumberOfControls() {
		return 1;	
	}	
	
	// ------- ui creation			
		
	public Button getSelectionButton(Composite group) {
		if (fButton == null) {
			assertCompositeNotNull(group);
			
			fButton= new Button(group, fButtonStyle);
			fButton.setFont(group.getFont());			
			fButton.setText(fLabelText);
			fButton.setEnabled(isEnabled());
			fButton.setSelection(fIsSelected);
			fButton.addSelectionListener(new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {
					doWidgetSelected(e);
				}
				public void widgetSelected(SelectionEvent e) {
					doWidgetSelected(e);
				}
			});				
		}
		return fButton;
	}
	
	private void doWidgetSelected(SelectionEvent e) {
		if (isOkToUse(fButton)) {
			changeValue(fButton.getSelection());
		}
	}	
	
	private void changeValue(boolean newState) {
		if (fIsSelected != newState) {
			fIsSelected= newState;			
			if (fAttachedDialogFields != null) {
				boolean focusSet= false;
				for (int i= 0; i < fAttachedDialogFields.length; i++) {		
					fAttachedDialogFields[i].setEnabled(fIsSelected);
					if (fIsSelected && !focusSet) {
						focusSet= fAttachedDialogFields[i].setFocus();
					}
				}
			}
			dialogFieldChanged();
		} else if (fButtonStyle == SWT.PUSH) {
			dialogFieldChanged();
		}
	}		

	// ------ model access	
		
	public boolean isSelected() {
		return fIsSelected;
	}
	
	public void setSelection(boolean selected) {
		changeValue(selected);
		if (isOkToUse(fButton)) {
			fButton.setSelection(selected);
		}
	}

	// ------ enable / disable management
	
	protected void updateEnableState() {
		super.updateEnableState();
		if (isOkToUse(fButton)) {
			fButton.setEnabled(isEnabled());
		}		
	}
	
	
	
		
}
/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;import org.eclipse.jface.util.Assert;

public abstract class TextInputWizardPage extends EditorSavingWizardPage {

	private StringDialogField fStringInput;
	private String fInitialSetting;
	
	/**
	 * Creates a new text input page.
	 * @param isLastUserPage <code>true</code> if this page is the wizard's last
	 *  user input page. Otherwise <code>false</code>.
	 */
	public TextInputWizardPage(boolean isLastUserPage) {
		this(isLastUserPage, "");
	}
	
	/**
	 * Creates a new text input page.
	 * @param isLastUserPage <code>true</code> if this page is the wizard's last
	 *  user input page. Otherwise <code>false</code>.
	 * @param initialSetting the initialSetting.
	 */
	public TextInputWizardPage(boolean isLastUserPage, String initialSetting) {
		super(isLastUserPage);
		Assert.isNotNull(initialSetting);
		fInitialSetting= initialSetting;
	}
	
	/**
	 * Checks the page's state and issues a corresponding error message. The page validation
	 * is computed by calling <code>validatePage</code>.
	 */
	protected void checkState() {
		RefactoringStatus status= validatePage();
		getRefactoringWizard().setStatus(status);
		if (status != null && status.hasFatalError()) {
			setPageComplete(false);
			setErrorMessage(status.getFirstMessage(RefactoringStatus.ERROR));
		} else {
			setPageComplete(true);	
			setErrorMessage(null);
		}	
	}
	
	/**
	 * Performs input validation. Returns a <code>RefactoringStatus</code> which
	 * describes the result of input validation. <code>Null<code> is interpreted
	 * as no error.
	 */
	protected RefactoringStatus validatePage(){
		return null;
	}
	
	/**
	 * Adds a text field.
	 * On each keystroke in this field <code>checkState</code> is called.
	 * No need to override.
	 * @see #checkState
	 */
	protected StringDialogField createStringDialogField(){
		fStringInput= new StringDialogField();
		fStringInput.setLabelText(getLabelText());
		fStringInput.setText(getInitialValue());
		fStringInput.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				checkState();
			};
		});
		return fStringInput;
	}

	public void setVisible(boolean visible) {
		if (visible) {
			checkState();
		}
		super.setVisible(visible);
		if (visible) {
			fStringInput.setFocus();
		}
	}

	public String getNewName() {
		return fStringInput.getText();
	}
	
	protected String getLabelText(){
		return RefactoringResources.getResourceString("TextInputPage.labelmessage");
	}
	
	protected String getInitialValue() {
		return fInitialSetting;
	}

	/* (non-JavaDoc)
	 * Method declared in EditorSavingWizardPage.
	 */
	protected DialogField[] createDialogFields() {
		return new DialogField[] { createStringDialogField(), getEditorList() };
	}
	
	/**
	 * Sets the stringInput
	 * @param stringInput The stringInput to set
	 */
	protected void setStringInput(StringDialogField stringInput) {
		fStringInput= stringInput;
	}
	/**
	 * Gets the stringInput
	 * @return Returns a StringDialogField
	 */
	protected StringDialogField getStringInput() {
		return fStringInput;
	}

}


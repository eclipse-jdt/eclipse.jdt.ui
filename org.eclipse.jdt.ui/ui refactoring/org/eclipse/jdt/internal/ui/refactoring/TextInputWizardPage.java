/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.internal.core.refactoring.DebugUtils;import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;import org.eclipse.jface.util.Assert;import org.eclipse.swt.SWT;import org.eclipse.swt.widgets.Composite;

public abstract class TextInputWizardPage extends UserInputWizardPage{

	private StringDialogField fStringInput;
	private String fInitialSetting;
	
	
	public static final String PAGE_NAME= "TextInputPage";
	private static final String PREFIX= PAGE_NAME + ".";
	
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
		super(PAGE_NAME, isLastUserPage);
		Assert.isNotNull(initialSetting);
		fInitialSetting= initialSetting;
	}
	
	/**
	 * @return <code>true</code> iff the input provided at initialization is valid.
	 * Typically it is not, because the user is required to provide some information e.g. a new type name etc.
	 */
	protected boolean isInitialInputValid(){
		return false;
	}
	
	/**
	 * @return <code>true</code> iff an empty string is valid.
	 * Typically it is not, because the user is required to provide some information e.g. a new type name etc.
	 */
	protected boolean isEmptyInputValid(){
		return false;
	}
	
	/**
	 * Checks the page's state and issues a corresponding error message. The page validation
	 * is computed by calling <code>validatePage</code>.
	 */
	protected void checkState() {		
		if (! isEmptyInputValid() && fStringInput.getText().equals("")){
			setPageComplete(false);
			setErrorMessage(null);
			return;
		}
		if ((! isInitialInputValid()) && fStringInput.getText().equals(fInitialSetting)){
			setPageComplete(false);
			setErrorMessage(null);
			return;
		}
		
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
	
	/* (non-JavaDoc)
	 * Method defined in IWizardPage
	 */
	public void createControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		DialogField[] fields= createDialogFields();	
		LayoutUtil.doDefaultLayout(composite, fields, true);
		setControl(composite);
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
		return new DialogField[] { createStringDialogField()};
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


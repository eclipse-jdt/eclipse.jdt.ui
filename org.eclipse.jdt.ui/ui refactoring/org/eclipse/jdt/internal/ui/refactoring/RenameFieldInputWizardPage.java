/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameFieldRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;

class RenameFieldInputWizardPage extends RenameInputWizardPage {

	private Button fRenameGetter;
	private Button fRenameSetter;
	private String fGetterRenamingErrorMessage;
	private String fSetterRenamingErrorMessage;
	
	public RenameFieldInputWizardPage(String message, String contextHelpId, String initialValue) {
		super(message, contextHelpId, true, initialValue);
	}

	/* non java-doc
	 * @see DialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		Composite parentComposite= (Composite)getControl();
				
		Composite composite= new Composite(parentComposite, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label separator= new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		getGetterSetterRenamingEnablement();
				
		fRenameGetter= new Button(composite, SWT.CHECK);
		fRenameGetter.setEnabled(fGetterRenamingErrorMessage == null);
		fRenameGetter.setSelection(getRenameFieldRefactoring().getRenameGetter());
		fRenameGetter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fRenameGetter.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				getRenameFieldRefactoring().setRenameGetter(fRenameGetter.getSelection());
			}
		});
		
		fRenameSetter= new Button(composite, SWT.CHECK);
		fRenameSetter.setEnabled(fSetterRenamingErrorMessage == null);
		fRenameSetter.setSelection(getRenameFieldRefactoring().getRenameSetter());
		fRenameSetter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fRenameSetter.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				getRenameFieldRefactoring().setRenameSetter(fRenameSetter.getSelection());
			}
		});
		
		updateGetterSetterLabels();
	}
	private void getGetterSetterRenamingEnablement() {
		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable(){
			public void run() {
				checkGetterRenamingEnablement();
				checkSetterRenamingEnablement();
			}
		});
	}
	
	protected void updateGetterSetterLabels(){
		fRenameGetter.setText(getRenameGetterLabel());
		fRenameSetter.setText(getRenameSetterLabel());
	}
	
	private String getRenameGetterLabel(){
		String defaultLabel= RefactoringMessages.getString("RenameFieldInputWizardPage.rename_getter"); //$NON-NLS-1$
		if (fGetterRenamingErrorMessage != null)
			return constructDisabledGetterRenamingLabel(defaultLabel);
		try {
			IMethod	getter= getRenameFieldRefactoring().getGetter();
			if (getter == null || ! getter.exists())
				return defaultLabel;
			String getterSig= JavaElementUtil.createMethodSignature(getter);
			return RefactoringMessages.getFormattedString("RenameFieldInputWizardPage.rename_getter_to", new String[]{getterSig, createNewGetterName()}); //$NON-NLS-1$
		} catch(JavaModelException e) {
			JavaPlugin.log(e);
			return defaultLabel;			
		}
	}
		
	private String getRenameSetterLabel(){
		String defaultLabel= RefactoringMessages.getString("RenameFieldInputWizardPage.rename_setter"); //$NON-NLS-1$
		if (fSetterRenamingErrorMessage != null)
			return constructDisabledSetterRenamingLabel(defaultLabel);
		try {
			IMethod	setter= getRenameFieldRefactoring().getSetter();
			if (setter == null || ! setter.exists())
				return defaultLabel;
			String setterSig= JavaElementUtil.createMethodSignature(setter);
			return RefactoringMessages.getFormattedString("RenameFieldInputWizardPage.rename_setter_to", new String[]{setterSig, createNewSetterName()});//$NON-NLS-1$
		} catch(JavaModelException e) {
			JavaPlugin.log(e);
			return defaultLabel;			
		}
	}
	private String constructDisabledSetterRenamingLabel(String defaultLabel) {
		if (fSetterRenamingErrorMessage.equals("")) //$NON-NLS-1$
			return defaultLabel;
		String[] keys= {defaultLabel, fSetterRenamingErrorMessage};
		return RefactoringMessages.getFormattedString("RenameFieldInputWizardPage.setter_label", keys); //$NON-NLS-1$
	}
	
	private String constructDisabledGetterRenamingLabel(String defaultLabel) {
		if (fGetterRenamingErrorMessage.equals("")) //$NON-NLS-1$
			return defaultLabel;
		String[] keys= {defaultLabel, fGetterRenamingErrorMessage};
		return RefactoringMessages.getFormattedString("RenameFieldInputWizardPage.getter_label", keys);			 //$NON-NLS-1$
	}
	
	private String createNewGetterName() throws JavaModelException {
		return getRenameFieldRefactoring().getNewGetterName();
	}
	
	private String createNewSetterName() throws JavaModelException {
		return getRenameFieldRefactoring().getNewSetterName();
	}
	
	private String checkGetterRenamingEnablement(){
		if (fGetterRenamingErrorMessage != null)
			return  fGetterRenamingErrorMessage;
		try {
			fGetterRenamingErrorMessage= getRenameFieldRefactoring().canEnableGetterRenaming();
			return fGetterRenamingErrorMessage;
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return ""; //$NON-NLS-1$
		} 
	}

	private String checkSetterRenamingEnablement(){
		if (fSetterRenamingErrorMessage != null)
			return  fSetterRenamingErrorMessage;
		try {
			fSetterRenamingErrorMessage= getRenameFieldRefactoring().canEnableSetterRenaming();
			return fSetterRenamingErrorMessage;
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return ""; //$NON-NLS-1$
		} 
	}
	
	private RenameFieldRefactoring getRenameFieldRefactoring(){
		return (RenameFieldRefactoring)getRefactoring();
	}
}

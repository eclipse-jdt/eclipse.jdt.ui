/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.TypeSelectionDialog;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveStaticMembersRefactoring;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.TypeInfo;

class MoveMembersInputPage extends UserInputWizardPage {

	public static final String PAGE_NAME= "MoveMembersInputPage"; //$NON-NLS-1$
	
	private Text fTextField;
	
	public MoveMembersInputPage() {
		super(PAGE_NAME, true);
	}
	
	public void setVisible(boolean visible){
		if (visible){
			String message= RefactoringMessages.getFormattedString("MoveMembersInputPage.descriptionKey", //$NON-NLS-1$
				new String[]{new Integer(getMoveRefactoring().getMovedMembers().length).toString(),
						 	 JavaModelUtil.getFullyQualifiedName(getMoveRefactoring().getDeclaringType())});
			setDescription(message);
		}	
		super.setVisible(visible);	
	}
	
	public void createControl(Composite parent) {		
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout gl= new GridLayout();
		gl.numColumns= 3;
		gl.makeColumnsEqualWidth= false;
		composite.setLayout(gl);
		
		Label label= new Label(composite, SWT.NONE);
		label.setText(RefactoringMessages.getString("MoveMembersInputPage.destination")); //$NON-NLS-1$
		label.setLayoutData(new GridData());
		
		addTextField(composite);
		
		Button button= new Button(composite, SWT.PUSH);
		button.setText(RefactoringMessages.getString("MoveMembersInputPage.browse")); //$NON-NLS-1$
		button.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				openTypeSelectionDialog();
			}
		});
		
		setPageComplete(false);
		setControl(composite);
		Dialog.applyDialogFont(composite);
		WorkbenchHelp.setHelp(composite, IJavaHelpContextIds.MOVE_MEMBERS_WIZARD_PAGE);
	}

	public void addTextField(Composite composite) {
		fTextField= new Text(composite, SWT.SINGLE | SWT.BORDER);
		fTextField.setFocus();
		fTextField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fTextField.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				IStatus status= JavaConventions.validateJavaTypeName(fTextField.getText());
				if (status.getSeverity() == IStatus.ERROR){
					error(status.getMessage());
				} else {
					try {
						IType resolvedType= getMoveRefactoring().getDeclaringType().getJavaProject().findType(fTextField.getText());
						IStatus validationStatus= validateDestinationType(resolvedType, fTextField.getText());
						if (validationStatus.isOK()){
							setErrorMessage(null);
							setPageComplete(true);
						} else {
							error(validationStatus.getMessage());
						}
					} catch(JavaModelException ex) {
						JavaPlugin.log(ex); //no ui here
						error(RefactoringMessages.getString("MoveMembersInputPage.invalid_name")); //$NON-NLS-1$
					}
				}	
			}
			private void error(String message){
				setErrorMessage(message);
				setPageComplete(false);
			}
		});
	}
	
	protected boolean performFinish() {
		initializeRefactoring();
		return super.performFinish();
	}
	
	public IWizardPage getNextPage() {
		initializeRefactoring();
		return super.getNextPage();
	}

	private void initializeRefactoring() {
		try {
			getMoveRefactoring().setDestinationTypeFullyQualifiedName(fTextField.getText());
		} catch(JavaModelException e) {
			ExceptionHandler.handle(e, getShell(), RefactoringMessages.getString("MoveMembersInputPage.move_Member"), RefactoringMessages.getString("MoveMembersInputPage.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	private IJavaSearchScope createWorkspaceSourceScope(){
		try {
			return RefactoringScopeFactory.create(getMoveRefactoring().getDeclaringType().getJavaProject());
		} catch (JavaModelException e) {
			//fallback is the whole workspace scope
			String title= RefactoringMessages.getString("MoveMembersInputPage.move"); //$NON-NLS-1$
			String message= RefactoringMessages.getString("MoveMembersInputPage.internal_error"); //$NON-NLS-1$
			ExceptionHandler.handle(e, getShell(), title, message);
			return SearchEngine.createJavaSearchScope(new IJavaElement[]{getMoveRefactoring().getDeclaringType().getJavaProject()}, true);
		}
	}
	
	private void openTypeSelectionDialog(){
		int elementKinds= IJavaSearchConstants.TYPE;
		final IJavaSearchScope scope= createWorkspaceSourceScope();
		TypeSelectionDialog dialog= new TypeSelectionDialog(getShell(), getWizard().getContainer(), elementKinds, scope);
		dialog.setTitle(RefactoringMessages.getString("MoveMembersInputPage.choose_Type")); //$NON-NLS-1$
		dialog.setMessage(RefactoringMessages.getString("MoveMembersInputPage.dialogMessage")); //$NON-NLS-1$
		dialog.setUpperListLabel(RefactoringMessages.getString("MoveMembersInputPage.upperListLabel")); //$NON-NLS-1$
		dialog.setLowerListLabel(RefactoringMessages.getString("MoveMembersInputPage.lowerListLabel")); //$NON-NLS-1$
		dialog.setValidator(new ISelectionStatusValidator(){
			public IStatus validate(Object[] selection) {
				Assert.isTrue(selection.length <= 1);
				if (selection.length == 0)
					return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, RefactoringMessages.getString("MoveMembersInputPage.Invalid_selection"), null); //$NON-NLS-1$
				Object element= selection[0];
				if (! (element instanceof TypeInfo))
					return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, RefactoringMessages.getString("MoveMembersInputPage.Invalid_selection"), null); //$NON-NLS-1$
				try {
					TypeInfo info= (TypeInfo)element;
					return validateDestinationType(info.resolveType(scope), info.getTypeName());
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
					return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, RefactoringMessages.getString("MoveMembersInputPage.internal_error"), null); //$NON-NLS-1$
				}
			}
		});
		dialog.setMatchEmptyString(false);
		dialog.setFilter(createInitialFilter());
		if (dialog.open() == Dialog.CANCEL)
			return;
		IType firstResult= (IType)dialog.getFirstResult();		
		fTextField.setText(JavaModelUtil.getFullyQualifiedName(firstResult));	
	}

	private String createInitialFilter() {
		if (! fTextField.getText().trim().equals("")) //$NON-NLS-1$
			return fTextField.getText();
		else
			return RefactoringMessages.getString("MoveMembersInputPage.initialFiler");	 //$NON-NLS-1$
	}
	
	private static IStatus validateDestinationType(IType type, String typeName){
		if (type == null || ! type.exists())
			return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, RefactoringMessages.getFormattedString("MoveMembersInputPage.not_found", typeName), null); //$NON-NLS-1$
		if (type.isBinary())
			return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, RefactoringMessages.getString("MoveMembersInputPage.no_binary"), null); //$NON-NLS-1$
		return new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
	}
	
	private MoveStaticMembersRefactoring getMoveRefactoring(){
		return (MoveStaticMembersRefactoring)getRefactoring();
	}
}

/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveStaticMembersProcessor;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.TypeInfo;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.TypeSelectionDialog;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.ControlContentAssistHelper;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.JavaTypeCompletionProcessor;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

public class MoveMembersWizard extends RefactoringWizard {

	public MoveMembersWizard(MoveRefactoring ref) {
		super(ref, DIALOG_BASED_UESR_INTERFACE);
		setDefaultPageTitle(RefactoringMessages.getString("MoveMembersWizard.page_title")); //$NON-NLS-1$
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new MoveMembersInputPage());
	}
	
	private static class MoveMembersInputPage extends UserInputWizardPage {

		public static final String PAGE_NAME= "MoveMembersInputPage"; //$NON-NLS-1$
		private static final long LABEL_FLAGS= JavaElementLabels.ALL_DEFAULT;

		private Combo fDestinationField;
		private static final int MRU_COUNT= 10;
		private static List fgMruDestinations= new ArrayList(MRU_COUNT);

		public MoveMembersInputPage() {
			super(PAGE_NAME);
		}
	
		public void setVisible(boolean visible){
			if (visible){
				String message= RefactoringMessages.getFormattedString("MoveMembersInputPage.descriptionKey", //$NON-NLS-1$
					new String[]{new Integer(getMoveProcessor().getMembersToMove().length).toString(),
								 JavaModelUtil.getFullyQualifiedName(getMoveProcessor().getDeclaringType())});
				setDescription(message);
			}	
			super.setVisible(visible);	
		}
	
		public void createControl(Composite parent) {		
			Composite composite= new Composite(parent, SWT.NONE);
			GridLayout gl= new GridLayout();
			gl.numColumns= 2;
			composite.setLayout(gl);
		
			addLabel(composite);
			addDestinationControls(composite);
		
			setControl(composite);
			Dialog.applyDialogFont(composite);
			WorkbenchHelp.setHelp(composite, IJavaHelpContextIds.MOVE_MEMBERS_WIZARD_PAGE);
		}

		private void addLabel(Composite parent) {
			Label label= new Label(parent, SWT.NONE);
			IMember[] members= getMoveProcessor().getMembersToMove();
			if (members.length == 1) {
				label.setText(RefactoringMessages.getFormattedString(
						"MoveMembersInputPage.destination_single", //$NON-NLS-1$
						JavaElementLabels.getElementLabel(members[0], LABEL_FLAGS)));
			} else {
				label.setText(RefactoringMessages.getFormattedString(
						"MoveMembersInputPage.destination_multi", //$NON-NLS-1$
						String.valueOf(members.length)));
			}
			GridData gd= new GridData();
			gd.horizontalSpan= 2;
			label.setLayoutData(gd);
		}

		private void addDestinationControls(Composite composite) {
			fDestinationField= new Combo(composite, SWT.SINGLE | SWT.BORDER);
			fDestinationField.setFocus();
			fDestinationField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fDestinationField.setItems((String[]) fgMruDestinations.toArray(new String[fgMruDestinations.size()]));
			fDestinationField.addModifyListener(new ModifyListener(){
				public void modifyText(ModifyEvent e) {
					handleDestinationChanged();
				}
				private void handleDestinationChanged() {
					IStatus status= JavaConventions.validateJavaTypeName(fDestinationField.getText());
					if (status.getSeverity() == IStatus.ERROR){
						error(status.getMessage());
					} else {
						try {
							IType resolvedType= getMoveProcessor().getDeclaringType().getJavaProject().findType(fDestinationField.getText());
							IStatus validationStatus= validateDestinationType(resolvedType, fDestinationField.getText());
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
			if (fgMruDestinations.size() > 0) {
				fDestinationField.select(0);
			} else {
				setPageComplete(false);
			}
			JavaTypeCompletionProcessor processor= new JavaTypeCompletionProcessor(false, false);
			IPackageFragment context= (IPackageFragment) getMoveProcessor().getDeclaringType().getAncestor(IJavaElement.PACKAGE_FRAGMENT);
			processor.setPackageFragment(context);
			ControlContentAssistHelper.createComboContentAssistant(fDestinationField, processor);
			
			Button button= new Button(composite, SWT.PUSH);
			button.setText(RefactoringMessages.getString("MoveMembersInputPage.browse")); //$NON-NLS-1$
			button.setLayoutData(new GridData());
			SWTUtil.setButtonDimensionHint(button);
			button.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					openTypeSelectionDialog();
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
				String destination= fDestinationField.getText();
				if (!fgMruDestinations.remove(destination) && fgMruDestinations.size() >= MRU_COUNT)
					fgMruDestinations.remove(fgMruDestinations.size() - 1);
				fgMruDestinations.add(0, destination);
				
				getMoveProcessor().setDestinationTypeFullyQualifiedName(destination);
			} catch(JavaModelException e) {
				ExceptionHandler.handle(e, getShell(), RefactoringMessages.getString("MoveMembersInputPage.move_Member"), RefactoringMessages.getString("MoveMembersInputPage.exception")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	
		private IJavaSearchScope createWorkspaceSourceScope(){
			IJavaElement[] project= new IJavaElement[] { getMoveProcessor().getDeclaringType().getJavaProject() };
			return SearchEngine.createJavaSearchScope(project, IJavaSearchScope.REFERENCED_PROJECTS | IJavaSearchScope.SOURCES);
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
			if (dialog.open() == Window.CANCEL)
				return;
			IType firstResult= (IType)dialog.getFirstResult();		
			fDestinationField.setText(JavaModelUtil.getFullyQualifiedName(firstResult));	
		}

		private String createInitialFilter() {
			if (! fDestinationField.getText().trim().equals("")) //$NON-NLS-1$
				return fDestinationField.getText();
			else
				return getMoveProcessor().getDeclaringType().getElementName();
		}
	
		private static IStatus validateDestinationType(IType type, String typeName){
			if (type == null || ! type.exists())
				return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, RefactoringMessages.getFormattedString("MoveMembersInputPage.not_found", typeName), null); //$NON-NLS-1$
			if (type.isBinary())
				return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, RefactoringMessages.getString("MoveMembersInputPage.no_binary"), null); //$NON-NLS-1$
			return new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
		}
	
		private MoveStaticMembersProcessor getMoveProcessor() {
			return (MoveStaticMembersProcessor)getRefactoring().getAdapter(MoveStaticMembersProcessor.class);
		}
	}
}

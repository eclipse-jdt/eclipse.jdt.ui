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
package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaStatusConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenamePackageRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.INewNameQueries;
import org.eclipse.jdt.internal.corext.refactoring.reorg.INewNameQuery;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.util.Resources;

public class NewNameQueries implements INewNameQueries {

	private static final String INVALID_NAME_NO_MESSAGE= "";//$NON-NLS-1$
	private final Wizard fWizard;
	private final Shell fShell;

	public NewNameQueries() {
		fShell= null;
		fWizard= null;
	}
	
	public NewNameQueries(Wizard wizard) {
		fWizard= wizard;
		fShell= null;
	}
	
	public NewNameQueries(Shell shell) {
		fShell = shell;
		fWizard= null;
	}

	private Shell getShell() {
		Assert.isTrue(fWizard == null || fShell == null);
		if (fWizard != null)
			return fWizard.getContainer().getShell();
			
		if (fShell != null)
			return fShell;
		return JavaPlugin.getActiveWorkbenchShell();
	}

	private static String removeTrailingJava(String name) {
		Assert.isTrue(name.endsWith(".java")); //$NON-NLS-1$
		return name.substring(0, name.length() - ".java".length()); //$NON-NLS-1$
	}

	public INewNameQuery createNewCompilationUnitNameQuery(ICompilationUnit cu) {
		String[] keys= {removeTrailingJava(cu.getElementName())};
		String message= ReorgMessages.getFormattedString("ReorgQueries.enterNewNameQuestion", keys); //$NON-NLS-1$
		return createStaticQuery(createCompilationUnitNameValidator(cu), message, removeTrailingJava(cu.getElementName()), getShell());
	}


	public INewNameQuery createNewResourceNameQuery(IResource res) {
		String[] keys= {res.getName()};
		String message= ReorgMessages.getFormattedString("ReorgQueries.enterNewNameQuestion", keys); //$NON-NLS-1$
		return createStaticQuery(createResourceNameValidator(res), message, res.getName(), getShell());
	}


	public INewNameQuery createNewPackageNameQuery(IPackageFragment pack) {
		String[] keys= {pack.getElementName()};
		String message= ReorgMessages.getFormattedString("ReorgQueries.enterNewNameQuestion", keys); //$NON-NLS-1$
		return createStaticQuery(createPackageNameValidator(pack), message, pack.getElementName(), getShell());
	}


	public INewNameQuery createNullQuery(){
		return createStaticQuery(null);
	}


	public INewNameQuery createStaticQuery(final String newName){
		return new INewNameQuery(){
			public String getNewName() {
				return newName;
			}
		};
	}

	private static INewNameQuery createStaticQuery(final IInputValidator validator, final String message, final String initial, final Shell shell){
		return new INewNameQuery(){
			public String getNewName() {
				InputDialog dialog= new InputDialog(shell, ReorgMessages.getString("ReorgQueries.nameConflictMessage"), message, initial, validator); //$NON-NLS-1$
				if (dialog.open() == Window.CANCEL)
					throw new OperationCanceledException();
				return dialog.getValue();
			}
		};
	}

	private static IInputValidator createResourceNameValidator(final IResource res){
		IInputValidator validator= new IInputValidator(){
			public String isValid(String newText) {
				if (newText == null || "".equals(newText) || res.getParent() == null) //$NON-NLS-1$
					return INVALID_NAME_NO_MESSAGE;
				if (res.getParent().findMember(newText) != null)
					return ReorgMessages.getString("ReorgQueries.resourceWithThisNameAlreadyExists"); //$NON-NLS-1$
				if (! res.getParent().getFullPath().isValidSegment(newText))
					return ReorgMessages.getString("ReorgQueries.invalidNameMessage"); //$NON-NLS-1$
				IStatus status= res.getParent().getWorkspace().validateName(newText, res.getType());
				if (status.getSeverity() == IStatus.ERROR)
					return status.getMessage();
					
				if (res.getName().equalsIgnoreCase(newText))
					return ReorgMessages.getString("ReorgQueries.resourceExistsWithDifferentCaseMassage"); //$NON-NLS-1$
					
				return null;
			}
		};
		return validator;
	}

	private static IInputValidator createCompilationUnitNameValidator(final ICompilationUnit cu) {
		IInputValidator validator= new IInputValidator(){
			public String isValid(String newText) {
				if (newText == null || "".equals(newText)) //$NON-NLS-1$
					return INVALID_NAME_NO_MESSAGE;
				String newCuName= newText + ".java"; //$NON-NLS-1$
				IStatus status= JavaConventions.validateCompilationUnitName(newCuName);	
				if (status.getSeverity() == IStatus.ERROR)
					return status.getMessage();
				RefactoringStatus refStatus;
				try {
					refStatus= Checks.checkCompilationUnitNewName(cu, newText);
				} catch (JavaModelException e) {
					return INVALID_NAME_NO_MESSAGE;
				}
				if (refStatus.hasFatalError())
					return refStatus.getFirstMessage(RefactoringStatus.FATAL);

				if (cu.getElementName().equalsIgnoreCase(newCuName))
					return ReorgMessages.getString("ReorgQueries.resourceExistsWithDifferentCaseMassage"); //$NON-NLS-1$
				
				return null;	
			}
		};
		return validator;
	}


	private static IInputValidator createPackageNameValidator(final IPackageFragment pack) {
		IInputValidator validator= new IInputValidator(){
			public String isValid(String newText) {
				if (newText == null || "".equals(newText)) //$NON-NLS-1$
					return INVALID_NAME_NO_MESSAGE;
				IStatus status= JavaConventions.validatePackageName(newText);
				if (status.getSeverity() == IStatus.ERROR)
					return status.getMessage();
				
				IJavaElement parent= pack.getParent();
				try {
					if (parent instanceof IPackageFragmentRoot){ 
						if (! RenamePackageRefactoring.isPackageNameOkInRoot(newText, (IPackageFragmentRoot)parent))
							return ReorgMessages.getString("ReorgQueries.packagewithThatNameexistsMassage");	 //$NON-NLS-1$
					}	
				} catch (JavaModelException e) {
					return INVALID_NAME_NO_MESSAGE;
				}
				if (pack.getElementName().equalsIgnoreCase(newText))
					return ReorgMessages.getString("ReorgQueries.resourceExistsWithDifferentCaseMassage"); //$NON-NLS-1$
					
				return null;
			}
		};	
		return validator;
	}			

	static class OverwriteQuery {

		private boolean alwaysOverwriteNonReadOnly= false;
		private boolean alwaysOverwrite= false;
		private boolean fCanceled= false;
		
		public boolean overwrite(final Object element) {
		
			IResource resource= ResourceUtil.getResource(element);

			if (resource != null){
				IPath location = resource.getLocation();
				if (location == null) {
					//undefined path variable
					return false;
				}

				if (location.toFile().exists() == false) {
					//link target does not exist
					return false;
				}
			}

			if (fCanceled)
				return false;
			
			if (alwaysOverwrite) 
				return true;

			final boolean isReadOnly= isReadOnly(element);
		
			if (alwaysOverwriteNonReadOnly && ! isReadOnly) 
				return true;

			final Shell parentShell= JavaPlugin.getActiveWorkbenchShell();
			final int[] result = new int[1];
			// Dialogs need to be created and opened in the UI thread
			Runnable query = new Runnable() {
				public void run() {
					int resultId[]= {
						IDialogConstants.YES_ID,
						IDialogConstants.YES_TO_ALL_ID,
						IDialogConstants.NO_ID,
						IDialogConstants.CANCEL_ID};
 
					String message= createMessage(element, isReadOnly);
					MessageDialog dialog= new MessageDialog(
						parentShell, 
						ReorgMessages.getString("ReorgQueries.Confirm_Overwritting"), //$NON-NLS-1$
						null,
						message,
						MessageDialog.QUESTION,
						new String[] {
							IDialogConstants.YES_LABEL,
							IDialogConstants.YES_TO_ALL_LABEL,
							IDialogConstants.NO_LABEL,
							IDialogConstants.CANCEL_LABEL },
						0);
					dialog.open();
					result[0]= resultId[dialog.getReturnCode()];
				}
			};
			parentShell.getDisplay().syncExec(query);
			if (result[0] == IDialogConstants.YES_TO_ALL_ID) {
				alwaysOverwriteNonReadOnly= true;
				if (isReadOnly)
					alwaysOverwrite= true;
				return true;
			} else if (result[0] == IDialogConstants.YES_ID) {
				return true;
			} else if (result[0] == IDialogConstants.CANCEL_ID) {
				fCanceled= true;
				return false;
			} else if (result[0] == IDialogConstants.NO_ID) {
				return false;
			} 
			Assert.isTrue(false);
			return false;
		}
	
		private String createMessage(Object element, boolean isReadOnly) {
			String[] keys= {ReorgUtils.getName(element)};
			if (isReadOnly)
				return ReorgMessages.getFormattedString("ReorgQueries.exists_read-only", keys); //$NON-NLS-1$
		 	else
				return ReorgMessages.getFormattedString("ReorgQueries.exists", keys); //$NON-NLS-1$
		}

		public boolean isCanceled(){
			return fCanceled;
		}
	
		private static boolean isReadOnly(Object element) {
			if (element instanceof IResource)
				return isReadOnlyResource((IResource)element);
			else if (element instanceof IJavaElement){
				IResource resource= ResourceUtil.getResource(element);
				if (resource == null)
					return false;
				if (isReadOnlyResource(resource))
					return true;
				return ((IJavaElement)element).isReadOnly();
			} else
				return false;
		}
	
		private static boolean isReadOnlyResource(IResource resource) {
			IStatus status= Resources.makeCommittable(resource, null);
			if (status.isOK())
				return false;
			if (status.getCode() == IJavaStatusConstants.VALIDATE_EDIT_CHANGED_CONTENT)
				return false;
			if (status.getCode() == IResourceStatus.OUT_OF_SYNC_LOCAL)
				return false;
			return true;	
		}
	}
}

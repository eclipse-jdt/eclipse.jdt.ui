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
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenamePackageProcessor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.INewNameQueries;
import org.eclipse.jdt.internal.corext.refactoring.reorg.INewNameQuery;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

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

	public INewNameQuery createNewCompilationUnitNameQuery(ICompilationUnit cu, String initialSuggestedName) {
		String[] keys= {removeTrailingJava(cu.getElementName())};
		String message= ReorgMessages.getFormattedString("ReorgQueries.enterNewNameQuestion", keys); //$NON-NLS-1$
		return createStaticQuery(createCompilationUnitNameValidator(cu), message, initialSuggestedName, getShell());
	}


	public INewNameQuery createNewResourceNameQuery(IResource res, String initialSuggestedName) {
		String[] keys= {res.getName()};
		String message= ReorgMessages.getFormattedString("ReorgQueries.enterNewNameQuestion", keys); //$NON-NLS-1$
		return createStaticQuery(createResourceNameValidator(res), message, initialSuggestedName, getShell());
	}


	public INewNameQuery createNewPackageNameQuery(IPackageFragment pack, String initialSuggestedName) {
		String[] keys= {pack.getElementName()};
		String message= ReorgMessages.getFormattedString("ReorgQueries.enterNewNameQuestion", keys); //$NON-NLS-1$
		return createStaticQuery(createPackageNameValidator(pack), message, initialSuggestedName, getShell());
	}

	public INewNameQuery createNewPackageFragmentRootNameQuery(IPackageFragmentRoot root, String initialSuggestedName) {
		String[] keys= {root.getElementName()};
		String message= ReorgMessages.getFormattedString("ReorgQueries.enterNewNameQuestion", keys); //$NON-NLS-1$
		return createStaticQuery(createPackageFragmentRootNameValidator(root), message, initialSuggestedName, getShell());
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
					return refStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);

				if (cu.getElementName().equalsIgnoreCase(newCuName))
					return ReorgMessages.getString("ReorgQueries.resourceExistsWithDifferentCaseMassage"); //$NON-NLS-1$
				
				return null;	
			}
		};
		return validator;
	}


	private static IInputValidator createPackageFragmentRootNameValidator(final IPackageFragmentRoot root) {
		return new IInputValidator() {
			IInputValidator resourceNameValidator= createResourceNameValidator(root.getResource());
			public String isValid(String newText) {
				return resourceNameValidator.isValid(newText);
			}
		};
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
						if (! RenamePackageProcessor.isPackageNameOkInRoot(newText, (IPackageFragmentRoot)parent))
							return ReorgMessages.getString("ReorgQueries.packagewithThatNameexistsMassage");	 //$NON-NLS-1$
					}	
				} catch (CoreException e) {
					return INVALID_NAME_NO_MESSAGE;
				}
				if (pack.getElementName().equalsIgnoreCase(newText))
					return ReorgMessages.getString("ReorgQueries.resourceExistsWithDifferentCaseMassage"); //$NON-NLS-1$
					
				return null;
			}
		};	
		return validator;
	}			
}

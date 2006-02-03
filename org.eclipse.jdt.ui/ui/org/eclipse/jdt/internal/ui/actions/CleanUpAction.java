/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.browsing.LogicalPackage;
import org.eclipse.jdt.internal.ui.dialogs.ProblemDialog;
import org.eclipse.jdt.internal.ui.fix.CleanUpRefactoringWizard;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.util.ElementValidator;

public class CleanUpAction extends SelectionDispatchAction {

	private JavaEditor fEditor;

	public CleanUpAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.CleanUpAction_label); 
		setToolTipText(ActionMessages.CleanUpAction_tooltip); 
		setDescription(ActionMessages.CleanUpAction_description); 

//		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.ORGANIZE_IMPORTS_ACTION);
	}
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the Java editor
	 */
	public CleanUpAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(getCompilationUnit(fEditor) != null);
	}
		
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(ITextSelection selection) {
		ICompilationUnit cu= getCompilationUnit(fEditor);
		if (cu != null) {
			run(cu);
		}
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(IStructuredSelection selection) {
		ICompilationUnit[] cus= getCompilationUnits(selection);
		if (cus.length == 0)
			return;
		if (cus.length == 1) {
			run(cus[0]);
		} else {
			runOnMultiple(cus);
		}
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void selectionChanged(ITextSelection selection) {
		setEnabled(getCompilationUnit(fEditor) != null);
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(isEnabled(selection));
	}
	
	private boolean isEnabled(IStructuredSelection selection) {
		Object[] selected= selection.toArray();
		for (int i= 0; i < selected.length; i++) {
			try {
				if (selected[i] instanceof IJavaElement) {
					IJavaElement elem= (IJavaElement) selected[i];
					if (elem.exists()) {
						switch (elem.getElementType()) {
							case IJavaElement.TYPE:
								return elem.getParent().getElementType() == IJavaElement.COMPILATION_UNIT; // for browsing perspective
							case IJavaElement.COMPILATION_UNIT:
								return true;
							case IJavaElement.IMPORT_CONTAINER:
								return true;
							case IJavaElement.PACKAGE_FRAGMENT:
							case IJavaElement.PACKAGE_FRAGMENT_ROOT:
								IPackageFragmentRoot root= (IPackageFragmentRoot) elem.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
								return (root.getKind() == IPackageFragmentRoot.K_SOURCE);
							case IJavaElement.JAVA_PROJECT:
								// https://bugs.eclipse.org/bugs/show_bug.cgi?id=65638
								return true;
						}
					}
				} else if (selected[i] instanceof LogicalPackage) {
					return true;
				}
			} catch (JavaModelException e) {
				if (!e.isDoesNotExist()) {
					JavaPlugin.log(e);
				}
			}
		}
		return false;
	}
	
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 * @param cu The compilation unit to process
	 */
	public void run(ICompilationUnit cu) {
		if (!ElementValidator.check(cu, getShell(), ActionMessages.CleanUpAction_ErrorDialogTitle, fEditor != null)) 
			return;
		if (!ActionUtil.isProcessable(getShell(), cu))
			return;
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu);
		CleanUpRefactoringWizard refactoringWizard= new CleanUpRefactoringWizard(refactoring, RefactoringWizard.WIZARD_BASED_USER_INTERFACE, false, true);
		
		RefactoringStarter starter= new RefactoringStarter();
		try {
			starter.activate(refactoring, refactoringWizard, JavaPlugin.getActiveWorkbenchShell(), "Clean ups", true); //$NON-NLS-1$
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			showUnexpectedError(e);
		}
		return;
	}
	
//Bug 120528 [clean up] inside editor behaves unexpected
//	private void runLight(ICompilationUnit cu) {
//		if (!ElementValidator.check(cu, getShell(), ActionMessages.CleanUpAction_ErrorDialogTitle, fEditor != null)) 
//			return;
//		if (!ActionUtil.isProcessable(getShell(), cu))
//			return;
//		
//		CleanUpRefactoring refactoring= new CleanUpRefactoring();
//		refactoring.addCompilationUnit(cu);
//		
//		ICleanUp[] cleanUps= CleanUpRefactoringWizard.createAllCleanUps();
//		for (int i= 0; i < cleanUps.length; i++) {
//			refactoring.addCleanUp(cleanUps[i]);
//		}
//		
//		int stopSeverity= RefactoringCore.getConditionCheckingFailedSeverity();
//		Shell shell= JavaPlugin.getActiveWorkbenchShell();
//		BusyIndicatorRunnableContext context= new BusyIndicatorRunnableContext();
//		RefactoringExecutionHelper executer= new RefactoringExecutionHelper(refactoring, stopSeverity, true, shell, context);
//		try {
//			executer.perform();
//		} catch (InterruptedException e) {
//		} catch (InvocationTargetException e) {
//			JavaPlugin.log(e);
//		}
//	}

	/**
	 * Perform on multiple compilation units. No editors are opened.
	 * @param cus The compilation units to run on
	 */
	public void runOnMultiple(final ICompilationUnit[] cus) {
		String message= ActionMessages.CleanUpAction_MultiStateErrorTitle; 
		final MultiStatus status= new MultiStatus(JavaUI.ID_PLUGIN, IStatus.OK, message, null);
		
		if (!ElementValidator.check(cus, getShell(), ActionMessages.CleanUpAction_ErrorDialogTitle, true))
			return;
		
		for (int i= 0; i < cus.length; i++) {
			testOnBuildPath(cus[i], status);
		}
		
		if (!status.isOK()) {
			String title= ActionMessages.CleanUpAction_ErrorDialogTitle; 
			ProblemDialog.open(getShell(), title, null, status);
			return;
		}
			
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		for (int i= 0; i < cus.length; i++) {
			refactoring.addCompilationUnit(cus[i]);
		}
		
		CleanUpRefactoringWizard refactoringWizard= new CleanUpRefactoringWizard(refactoring, RefactoringWizard.WIZARD_BASED_USER_INTERFACE, false, true);
		
		RefactoringStarter starter= new RefactoringStarter();
		try {
			starter.activate(refactoring, refactoringWizard, JavaPlugin.getActiveWorkbenchShell(), "Clean ups", true); //$NON-NLS-1$
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			showUnexpectedError(e);
		}
		return;
	}

	private void showUnexpectedError(CoreException e) {
		String message2= Messages.format(ActionMessages.CleanUpAction_UnexpectedErrorMessage, e.getStatus().getMessage()); 
		IStatus status= new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, message2, null);
		String title= ActionMessages.CleanUpAction_ErrorDialogTitle; 
		ProblemDialog.open(getShell(), title, null, status);
	}
	
	private boolean testOnBuildPath(ICompilationUnit cu, MultiStatus status) {
		IJavaProject project= cu.getJavaProject();
		if (!project.isOnClasspath(cu)) {
			String cuLocation= cu.getPath().makeRelative().toString();
			String message= Messages.format(ActionMessages.CleanUpAction_CUNotOnBuildpathMessage, cuLocation); 
			status.add(new Status(IStatus.INFO, JavaUI.ID_PLUGIN, IStatus.ERROR, message, null));
			return false;
		}
		return true;
	}
	
	private ICompilationUnit[] getCompilationUnits(IStructuredSelection selection) {
		HashSet result= new HashSet();
		Object[] selected= selection.toArray();
		for (int i= 0; i < selected.length; i++) {
			try {
				if (selected[i] instanceof IJavaElement) {
					IJavaElement elem= (IJavaElement) selected[i];
					if (elem.exists()) {
					
						switch (elem.getElementType()) {
							case IJavaElement.TYPE:
								if (elem.getParent().getElementType() == IJavaElement.COMPILATION_UNIT) {
									result.add(elem.getParent());
								}
								break;						
							case IJavaElement.COMPILATION_UNIT:
								result.add(elem);
								break;
							case IJavaElement.IMPORT_CONTAINER:
								result.add(elem.getParent());
								break;							
							case IJavaElement.PACKAGE_FRAGMENT:
								collectCompilationUnits((IPackageFragment) elem, result);
								break;
							case IJavaElement.PACKAGE_FRAGMENT_ROOT:
								collectCompilationUnits((IPackageFragmentRoot) elem, result);
								break;
							case IJavaElement.JAVA_PROJECT:
								IPackageFragmentRoot[] roots= ((IJavaProject) elem).getPackageFragmentRoots();
								for (int k= 0; k < roots.length; k++) {
									collectCompilationUnits(roots[k], result);
								}
								break;			
						}
					}
				} else if (selected[i] instanceof LogicalPackage) {
					IPackageFragment[] packageFragments= ((LogicalPackage)selected[i]).getFragments();
					for (int k= 0; k < packageFragments.length; k++) {
						IPackageFragment pack= packageFragments[k];
						if (pack.exists()) {
							collectCompilationUnits(pack, result);
						}
					}
				}
			} catch (JavaModelException e) {
				if (JavaModelUtil.isExceptionToBeLogged(e))
					JavaPlugin.log(e);
			}
		}
		return (ICompilationUnit[]) result.toArray(new ICompilationUnit[result.size()]);
	}
	
	private void collectCompilationUnits(IPackageFragment pack, Collection result) throws JavaModelException {
		result.addAll(Arrays.asList(pack.getCompilationUnits()));
	}
	
	private void collectCompilationUnits(IPackageFragmentRoot root, Collection result) throws JavaModelException {
		if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
			IJavaElement[] children= root.getChildren();
			for (int i= 0; i < children.length; i++) {
				collectCompilationUnits((IPackageFragment) children[i], result);
			}
		}
	}	
	
	private static ICompilationUnit getCompilationUnit(JavaEditor editor) {
		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
		ICompilationUnit cu= manager.getWorkingCopy(editor.getEditorInput());
		return cu;
	}

}

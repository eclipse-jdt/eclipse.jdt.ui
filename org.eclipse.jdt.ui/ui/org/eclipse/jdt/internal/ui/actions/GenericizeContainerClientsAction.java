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
package org.eclipse.jdt.internal.ui.actions;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.internal.corext.refactoring.genericize.GenericizeContainerClientsRefactoring;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.refactoring.GenericizeContainerClientsWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

/**
 * Changes clients of J2SE 1.4 container classes (collections, maps, enumerations, iterators)
 * to use generics (type parameters).
 *  
 * @since 3.1
 */
public class GenericizeContainerClientsAction extends SelectionDispatchAction {

	public static final boolean DEBUG= "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.jdt.ui/debug/GenericizeContainerClientsAction")); //$NON-NLS-1$//$NON-NLS-2$
	
	private static final String DIALOG_MESSAGE_TITLE= RefactoringMessages.getString("GenericizeContainerClientsAction.dialog_title");//$NON-NLS-1$
	private CompilationUnitEditor fEditor;

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public GenericizeContainerClientsAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}
	
	/**
	 * Creates a new {@link GenericizeContainerClientsAction}. The action requires
	 * that the selection provided by the site's selection provider is of type 
	 * {@link org.eclipse.jface.viewers.IStructuredSelection}.
	 * 
	 * @param site the site providing context information for this action
	 */
	public GenericizeContainerClientsAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.getString("GenericizeContainerClientsAction.label")); //$NON-NLS-1$
	}
	
	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.text.ITextSelection)
	 */
	public void selectionChanged(ITextSelection selection) {
		// do nothing
	}
	
	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(canEnable(getSelectedElements(selection)));
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e);
			setEnabled(false);//no ui
		}
	}
	
	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		try {
			IJavaElement[] elements= getSelectedElements(selection);
			if (canEnable(elements)) {
				startRefactoring(elements);
			} else {
				String unavailable= RefactoringMessages.getString("GenericizeContainerClientsAction.unavailable"); //$NON-NLS-1$;
				MessageDialog.openInformation(getShell(), RefactoringMessages.getString("OpenRefactoringWizardAction.unavailable"), unavailable); //$NON-NLS-1$
			}
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), RefactoringMessages.getString("OpenRefactoringWizardAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.text.ITextSelection)
	 */
	public void run(ITextSelection selection) {
		try {
			if (!ActionUtil.isProcessable(getShell(), fEditor))
				return;
			IJavaElement element= SelectionConverter.getInput(fEditor);
			IJavaElement[] array= new IJavaElement[] {element};
			if (element != null && canEnable(array)){
				startRefactoring(array);	
			} else {
				String unavailable= RefactoringMessages.getString("GenericizeContainerClientsAction.unavailable"); //$NON-NLS-1$;
				MessageDialog.openInformation(getShell(), RefactoringMessages.getString("OpenRefactoringWizardAction.unavailable"), unavailable); //$NON-NLS-1$
			}
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), RefactoringMessages.getString("OpenRefactoringWizardAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	private static IJavaElement[] getSelectedElements(IStructuredSelection selection){
		List list= selection.toList();
		IJavaElement[] elements= new IJavaElement[list.size()];
		for (int i= 0; i < list.size(); i++) {
			Object object= list.get(i);
			if (object instanceof IJavaElement)
				elements[i]= (IJavaElement) object;
			else
				return new IJavaElement[0];
		}
		return elements;
	}

	private static boolean canEnable(IJavaElement[] elements) throws JavaModelException {
		//TODO: LogicalPackages?
		return GenericizeContainerClientsRefactoring.isAvailable(elements);
	}
	
	private void startRefactoring(IJavaElement[] elements) {
		try {
			GenericizeContainerClientsRefactoring refactoring= GenericizeContainerClientsRefactoring.create(elements);
			if (refactoring == null)
				return;
			new RefactoringStarter().activate(refactoring, createWizard(refactoring), getShell(), DIALOG_MESSAGE_TITLE, true);
		} catch (CoreException e){
			ExceptionHandler.handle(e, DIALOG_MESSAGE_TITLE, RefactoringMessages.getString("OpenRefactoringWizardAction.exception")); //$NON-NLS-1$
		}
	}

	private static RefactoringWizard createWizard(GenericizeContainerClientsRefactoring refactoring) {
		return new GenericizeContainerClientsWizard(refactoring);
	}
}

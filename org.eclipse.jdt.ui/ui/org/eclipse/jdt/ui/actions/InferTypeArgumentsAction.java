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
package org.eclipse.jdt.ui.actions;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.generics.InferTypeArgumentsRefactoring;

import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.refactoring.InferTypeArgumentsWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Infers type argumnets for raw references to generic types.
 *  
 * @since 3.1
 */
public class InferTypeArgumentsAction extends SelectionDispatchAction {

	private static final String DIALOG_MESSAGE_TITLE= RefactoringMessages.getString("InferTypeArgumentsAction.dialog_title");//$NON-NLS-1$
	private CompilationUnitEditor fEditor;

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public InferTypeArgumentsAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}
	
	/**
	 * Creates a new {@link InferTypeArgumentsAction}. The action requires
	 * that the selection provided by the site's selection provider is of type 
	 * {@link org.eclipse.jface.viewers.IStructuredSelection}.
	 * 
	 * @param site the site providing context information for this action
	 */
	public InferTypeArgumentsAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.getString("InferTypeArgumentsAction.label")); //$NON-NLS-1$
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
		setEnabled(RefactoringAvailabilityTester.isInferTypeArgumentsAvailable(selection));
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		IJavaElement[] elements= getSelectedElements(selection);
		if (RefactoringAvailabilityTester.isInferTypeArgumentsAvailable(elements)) {
			startRefactoring(elements);
		} else {
			String unavailable= RefactoringMessages.getString("InferTypeArgumentsAction.unavailable"); //$NON-NLS-1$;
			MessageDialog.openInformation(getShell(), RefactoringMessages.getString("OpenRefactoringWizardAction.unavailable"), unavailable); //$NON-NLS-1$
		}
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.text.ITextSelection)
	 */
	public void run(ITextSelection selection) {
		if (!ActionUtil.isProcessable(getShell(), fEditor))
			return;
		IJavaElement element= SelectionConverter.getInput(fEditor);
		IJavaElement[] array= new IJavaElement[] {element};
		if (element != null && RefactoringAvailabilityTester.isInferTypeArgumentsAvailable(array)){
			startRefactoring(array);	
		} else {
			String unavailable= RefactoringMessages.getString("InferTypeArgumentsAction.unavailable"); //$NON-NLS-1$;
			MessageDialog.openInformation(getShell(), RefactoringMessages.getString("OpenRefactoringWizardAction.unavailable"), unavailable); //$NON-NLS-1$
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

	private void startRefactoring(IJavaElement[] elements) {
		try {
			InferTypeArgumentsRefactoring refactoring= InferTypeArgumentsRefactoring.create(elements);
			if (refactoring == null)
				return;
			new RefactoringStarter().activate(refactoring, new InferTypeArgumentsWizard(refactoring), getShell(), DIALOG_MESSAGE_TITLE, true);
		} catch (CoreException e){
			ExceptionHandler.handle(e, DIALOG_MESSAGE_TITLE, RefactoringMessages.getString("OpenRefactoringWizardAction.exception")); //$NON-NLS-1$
		}
	}
}
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
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.MoveInstanceMethodWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodRefactoring;

/**
 * <p> This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 */
public class MoveInstanceMethodAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;
	
	private static final String DIALOG_TITLE= RefactoringMessages.getString("MoveInstanceMethodAction.dialog_title"); //$NON-NLS-1$

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public MoveInstanceMethodAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}

	public MoveInstanceMethodAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.getString("MoveInstanceMethodAction.Move_Method")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.MOVE_ACTION);		
	}

	/*
	 * @see SelectionDispatchAction#selectionChanged(IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(canEnable(selection));
		} catch (JavaModelException e) {
			setEnabled(false);//no ui
		}
	}

    /*
     * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.text.ITextSelection)
     */
	public void selectionChanged(ITextSelection selection) {
       //do nothing
    }

	private static boolean canEnable(IStructuredSelection selection) throws JavaModelException{
		IMethod method= getSingleSelectedMethod(selection);
		if (method == null)
			return false;
		return MoveInstanceMethodRefactoring.isAvailable(method, JavaPreferencesSettings.getCodeGenerationSettings());
	}

	private static IMethod getSingleSelectedMethod(IStructuredSelection selection) {
		if (selection.isEmpty() || selection.size() != 1) 
			return null;
		
		Object first= selection.getFirstElement();
		if (! (first instanceof IMethod))
			return null;
		return (IMethod) first;
	}
	/*
	 * @see SelectionDispatchAction#run(IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {		
		try {
			Assert.isTrue(canEnable(selection));
			
			IMethod method= getSingleSelectedMethod(selection);
			Assert.isNotNull(method);	
			run(method);
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, getShell(), DIALOG_TITLE, RefactoringMessages.getString("MoveInstanceMethodAction.unexpected_exception"));	 //$NON-NLS-1$
		}
 	}	
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	public void run(ITextSelection selection) {
		try {
			run(selection.getOffset(), selection.getLength(), getCompilationUnitForTextSelection());
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, getShell(), DIALOG_TITLE, RefactoringMessages.getString("MoveInstanceMethodAction.unexpected_exception"));	 //$NON-NLS-1$
		}
	}

	private void run(IMethod method) throws JavaModelException {
		MoveInstanceMethodRefactoring refactoring= MoveInstanceMethodRefactoring.create(method, JavaPreferencesSettings.getCodeGenerationSettings());
		if (refactoring == null) {
			MessageDialog.openInformation(getShell(), DIALOG_TITLE, RefactoringMessages.getString("MoveInstanceMethodAction.No_reference_or_declaration")); //$NON-NLS-1$
			return;
		}
		new RefactoringStarter().activate(refactoring, createWizard(refactoring), getShell(), DIALOG_TITLE, true);
	}
		
	private void run(int selectionOffset, int selectionLength, ICompilationUnit cu) throws JavaModelException{
		Assert.isNotNull(cu);
		Assert.isTrue(selectionOffset >= 0);
		Assert.isTrue(selectionLength >= 0);
		
		if (!ActionUtil.isProcessable(getShell(), cu))
			return;

		IMethod method= getMethod();

		if (method != null)
			run(method);
	}
	
	private IMethod getMethod() {
		IJavaElement[] elements= SelectionConverter.codeResolveHandled(fEditor, getShell(), RefactoringMessages.getString("RenameJavaElementAction.name")); //$NON-NLS-1$
		if (elements == null || elements.length != 1 || ! (elements[0] instanceof IMethod))
			return null;
		return (IMethod) elements[0];
	}

	private ICompilationUnit getCompilationUnitForTextSelection() {
		Assert.isNotNull(fEditor);
		return SelectionConverter.getInputAsCompilationUnit(fEditor);
	}
	
	private static RefactoringWizard createWizard(MoveInstanceMethodRefactoring refactoring) {
		return new MoveInstanceMethodWizard(refactoring);
	}
}

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
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodRefactoring;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.MoveInstanceMethodWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

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
	 * @param editor the compilation unit editor
	 */
	public MoveInstanceMethodAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}

	public MoveInstanceMethodAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.getString("MoveInstanceMethodAction.Move_Method")); //$NON-NLS-1$
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.MOVE_ACTION);		
	}

	/*
	 * @see SelectionDispatchAction#selectionChanged(IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(canEnable(selection));
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e);
			setEnabled(false);//no ui
		}
	}

	public void selectionChanged(ITextSelection selection) {
		setEnabled(true);
    }

	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	public void selectionChanged(JavaTextSelection selection) {
		try {
			setEnabled(canEnabled(selection));
		} catch (CoreException e) {
			setEnabled(false);
		}
	}
	
	private static boolean canEnabled(JavaTextSelection selection) throws JavaModelException {
		IJavaElement method= selection.resolveEnclosingElement();
		if (!(method instanceof IMethod))
			return false;
		return MoveInstanceMethodRefactoring.isAvailable((IMethod)method);
	}

	private static boolean canEnable(IStructuredSelection selection) throws JavaModelException{
		IMethod method= getSingleSelectedMethod(selection);
		if (method == null)
			return false;
		return MoveInstanceMethodRefactoring.isAvailable(method);
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
			run(selection, getCompilationUnitForTextSelection());
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, getShell(), DIALOG_TITLE, RefactoringMessages.getString("MoveInstanceMethodAction.unexpected_exception"));	 //$NON-NLS-1$
		}
	}

	private void run(IMethod method) throws JavaModelException {
		MoveInstanceMethodRefactoring refactoring= MoveInstanceMethodRefactoring.create(method, JavaPreferencesSettings.getCodeGenerationSettings(method.getJavaProject()));
		if (refactoring == null) {
			MessageDialog.openInformation(getShell(), DIALOG_TITLE, RefactoringMessages.getString("MoveInstanceMethodAction.No_reference_or_declaration")); //$NON-NLS-1$
			return;
		}
		new RefactoringStarter().activate(refactoring, createWizard(refactoring), getShell(), DIALOG_TITLE, true);
	}
		
	private void run(ITextSelection selection, ICompilationUnit cu) throws JavaModelException{
		Assert.isNotNull(cu);
		Assert.isTrue(selection.getOffset() >= 0);
		Assert.isTrue(selection.getLength() >= 0);
		
		if (!ActionUtil.isProcessable(getShell(), cu))
			return;

		IMethod method= getMethod(cu, selection);

		if (method != null) {
			run(method);
		} else {
			MessageDialog.openInformation(getShell(), DIALOG_TITLE, 
				RefactoringMessages.getString("MoveInstanceMethodAction.No_reference_or_declaration")); //$NON-NLS-1$
		}
	}
	
	private static IMethod getMethod(ICompilationUnit cu, ITextSelection selection) throws JavaModelException {
		IJavaElement element= SelectionConverter.getElementAtOffset(cu, selection);
		if (element instanceof IMethod)
			return (IMethod) element;
		return null;
	}

	private ICompilationUnit getCompilationUnitForTextSelection() {
		Assert.isNotNull(fEditor);
		return SelectionConverter.getInputAsCompilationUnit(fEditor);
	}
	
	private static RefactoringWizard createWizard(MoveInstanceMethodRefactoring refactoring) {
		return new MoveInstanceMethodWizard(refactoring);
	}
}

/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.code.InlineMethodWizard;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring;

/**
 * Inlines a method.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
public class InlineMethodAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;
	
	private static final String DIALOG_TITLE= "Inline Method";

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public InlineMethodAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}

	public InlineMethodAction(IWorkbenchSite site) {
		super(site);
		setText("I&nline Method...");
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.INLINE_ACTION);
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.text.ITextSelection)
	 */
	protected void selectionChanged(ITextSelection selection) {
	   //do nothing
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	protected void run(ITextSelection selection) {
		ICompilationUnit cu= getCompilationUnit();
		if (cu == null)
			return;
		run(selection.getOffset(), selection.getLength(), cu);
	}
		
	private ICompilationUnit getCompilationUnit() {
		return SelectionConverter.getInputAsCompilationUnit(fEditor);
	}
	
	private RefactoringWizard createWizard(InlineMethodRefactoring refactoring) {
		return new InlineMethodWizard(refactoring);
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void run(IStructuredSelection selection) {
		try {
			Assert.isTrue(canEnable(selection));

			Object first= selection.getFirstElement();
			Assert.isTrue(first instanceof IMethod);

			IMethod method= (IMethod) first;
			run(method.getNameRange().getOffset(), method.getNameRange().getLength(), method.getCompilationUnit());
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, getShell(), DIALOG_TITLE, "Unexpected exception during operation");
		}
	}

	private void run(int selectionOffset, int selectionLength, ICompilationUnit cu) {
		InlineMethodRefactoring refactoring= InlineMethodRefactoring.create(
			cu, selectionOffset, selectionLength,
			JavaPreferencesSettings.getCodeGenerationSettings());
		if (refactoring == null) {
			MessageDialog.openInformation(getShell(), DIALOG_TITLE, "No method invocation or declaration selected.");
			return;
		}
		try {
			if (fEditor == null){
				activate(refactoring);
			} else {
				IRewriteTarget target= (IRewriteTarget) fEditor.getAdapter(IRewriteTarget.class);
				try {
					target.beginCompoundChange();
					activate(refactoring);
				} finally {
					if (target != null)
						target.endCompoundChange();
				}
			}
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, getShell(), DIALOG_TITLE, "Unexpected exception during operation");
		}
	}

	private void activate(InlineMethodRefactoring refactoring) throws JavaModelException {
		new RefactoringStarter().activate(refactoring, createWizard(refactoring), DIALOG_TITLE, true);
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(canEnable(selection));
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
	}

	private boolean canEnable(IStructuredSelection selection) throws JavaModelException{
		if (selection.isEmpty() || selection.size() != 1)
			return false;

		Object first= selection.getFirstElement();
		return (first instanceof IMethod) && shouldAcceptElement((IMethod)first);
	}

	private boolean shouldAcceptElement(IMethod method) throws JavaModelException {
		return ! method.isBinary();
	}
}

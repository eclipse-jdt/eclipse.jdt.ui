/*******************************************************************************
 * Copyright (c) 2023 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ui.actions;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringExecutionStarter;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Extracts an expression into a constant field and replaces all occurrences of the expression with
 * the new constant.
 *
 * Modifies a method with the static keyword and changes all method invocations to properly invoke
 * the refactored method.
 *
 * @since 3.30
 *
 */
public final class MakeStaticAction extends SelectionDispatchAction {

	private JavaEditor fEditor;

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 *
	 * @param editor the java editor. Must not be null.
	 *
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	public MakeStaticAction(JavaEditor editor) {
		super(editor.getEditorSite());
		setText(RefactoringMessages.MakeStaticAction_title);
		setToolTipText(RefactoringMessages.MakeStaticAction_tooltip);
		setDescription(RefactoringMessages.MakeStaticAction_description);
		fEditor= editor;
		setEnabled(SelectionConverter.getInputAsCompilationUnit(fEditor) != null);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.MAKE_STATIC_ACTION);
	}

	public MakeStaticAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.MakeStaticAction_title);
		setToolTipText(RefactoringMessages.MakeStaticAction_tooltip);
		setDescription(RefactoringMessages.MakeStaticAction_description);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.MAKE_STATIC_ACTION);
	}

	// Selected in Editor window
	@Override
	public void selectionChanged(ITextSelection selection) {
		setText(RefactoringMessages.MakeStaticAction_title);
		setToolTipText(RefactoringMessages.MakeStaticAction_tooltip);
		setDescription(RefactoringMessages.MakeStaticAction_description);
		setEnabled(SelectionConverter.getInputAsCompilationUnit(fEditor) != null);
	}

	// Selected in outline window
	@Override
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isMakeStaticAvailable(selection));
		} catch (JavaModelException e) {
			if (JavaModelUtil.isExceptionToBeLogged(e)) {
				JavaPlugin.log(e);
			}
		}
	}

	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 *
	 * @param selection the Java text selection (internal type)
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 */
	@Override
	public void selectionChanged(JavaTextSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isMakeStaticAvailable(selection));
		} catch (JavaModelException e) {
			setEnabled(false);
		}
	}

	@Override
	public void run(ITextSelection selection) {
		if (!ActionUtil.isProcessable(fEditor)) {
			return;
		}
		ITypeRoot editorInput= SelectionConverter.getInput(fEditor);
		if (!ActionUtil.isEditable(getShell(), editorInput)) {
			return;
		}
		run(selection.getOffset(), selection.getLength(), (ICompilationUnit) editorInput);
	}

	@Override
	public void run(IStructuredSelection selection) {
		try {
			Assert.isTrue(RefactoringAvailabilityTester.isMakeStaticAvailable(selection));
			Object first= selection.getFirstElement();
			Assert.isTrue(first instanceof IMethod);
			if (!ActionUtil.isEditable(getShell(), (IMethod) first)) {
				return;
			}
			run((IMethod) first);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, RefactoringMessages.MakeStaticAction_dialog_title, RefactoringMessages.MakeStaticAction_unknown_exception);
		}
	}

	private void run(int offset, int length, ICompilationUnit unit) {
		RefactoringExecutionStarter.startMakeStaticRefactoring(unit, offset, length, getShell());
	}

	private void run(IMethod method) {
		RefactoringExecutionStarter.startMakeStaticRefactoring(method, getShell());
	}
}

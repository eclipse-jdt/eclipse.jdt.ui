/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.actions;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
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
 * Action that replaces method invocations. This action may be invoked
 * on source or binary methods or method invocations with or without attached source.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 3.2
 */
public class ReplaceInvocationsAction extends SelectionDispatchAction {

	private JavaEditor fEditor;

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the java editor
	 */
	public ReplaceInvocationsAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(true);
	}

	/**
	 * Creates a new <code>ReplaceInvocationsAction</code>. 
	 * 
	 * @param site the site providing context information for this action
	 */
	public ReplaceInvocationsAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.ReplaceInvocationsAction_label);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.REPLACE_INVOCATIONS_ACTION);
	}

	//---- structured selection --------------------------------------------------

	/*
	 * @see SelectionDispatchAction#selectionChanged(IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isReplaceInvocationsAvailable(selection));
		} catch (JavaModelException e) {
			if (JavaModelUtil.isExceptionToBeLogged(e))
				JavaPlugin.log(e);
		}
	}

	/*
	 * @see SelectionDispatchAction#selectionChanged(ITextSelection)
	 */
	public void selectionChanged(ITextSelection selection) {
		setEnabled(true);
	}

	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	public void selectionChanged(JavaTextSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isReplaceInvocationsAvailable(selection));
		} catch (JavaModelException e) {
			setEnabled(false);
		}
	}

	/*
	 * @see SelectionDispatchAction#run(IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		try {
			Assert.isTrue(RefactoringAvailabilityTester.isReplaceInvocationsAvailable(selection));
			Object first= selection.getFirstElement();
			Assert.isTrue(first instanceof IMethod);
			IMethod method= (IMethod) first;
			if (ActionUtil.isProcessable(getShell(), method))
				RefactoringExecutionStarter.startReplaceInvocationsRefactoring(method, getShell());
		} catch (CoreException e) {
			handleException(e);
		}
	}

	private void handleException(CoreException e) {
		ExceptionHandler.handle(e, RefactoringMessages.ReplaceInvocationsAction_dialog_title, RefactoringMessages.ReplaceInvocationsAction_unavailable);
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	public void run(ITextSelection selection) {
		try {
			Object editorInput= SelectionConverter.getInput(fEditor);
			if ((editorInput instanceof ICompilationUnit || editorInput instanceof IClassFile)
					&& ActionUtil.isProcessable(getShell(), (IJavaElement) editorInput)) {
				IJavaElement unit= (IJavaElement) editorInput;
				RefactoringExecutionStarter.startReplaceInvocationsRefactoring(unit, selection.getOffset(), selection.getLength(), getShell());
			}
		} catch (JavaModelException e) {
			handleException(e);
		}
	}
}

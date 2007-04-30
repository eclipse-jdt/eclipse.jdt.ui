/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringExecutionStarter;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class IntroduceParameterObjectAction extends SelectionDispatchAction {
	public static final String ACTION_ID= "org.eclipse.jdt.ui.actions.IntroduceParameterObject"; //TODO Place in JdtActionConstants //$NON-NLS-1$

	public static final String ACTION_DEFINITION_ID= "org.eclipse.jdt.ui.refactoring.introduceparamobject"; //TODO Place in IJavaEditorActionDefinitionIds //$NON-NLS-1$
	
	private JavaEditor fEditor;

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the compilation unit editor
	 */
	public IntroduceParameterObjectAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(true);
	}

	/**
	 * Creates a new <code>IntroduceIndirectionAction</code>. 
	 * 
	 * @param site the site providing context information for this action
	 */
	public IntroduceParameterObjectAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.IntroduceParameterObjectAction_action_text);
		//setToolTipText(RefactoringMessages.IntroduceIndirectionAction_tooltip);
		//setDescription(RefactoringMessages.IntroduceIndirectionAction_description);
		//PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.INTRODUCE_INDIRECTION_ACTION);
	}

	//---- structured selection --------------------------------------------------

	/*
	 * @see SelectionDispatchAction#selectionChanged(IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isIntroduceParameterObjectAvailable(selection));
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

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jdt.internal.ui.javaeditor.JavaTextSelection)
	 */
	public void selectionChanged(JavaTextSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isIntroduceParameterObjectAvailable(selection));
		} catch (Exception e) {
			setEnabled(false);
		}
	}

	/*
	 * @see SelectionDispatchAction#run(IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		try {
			Assert.isTrue(RefactoringAvailabilityTester.isIntroduceParameterObjectAvailable(selection));
			Object first= selection.getFirstElement();
			Assert.isTrue(first instanceof IMethod);
			run((IMethod) first);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, ActionMessages.IntroduceParameterObjectAction_exceptiondialog_title, ActionMessages.IntroduceParameterObjectAction_unexpected_exception);
		}
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	public void run(ITextSelection selection) {
		try {
			Object editorInput= SelectionConverter.getInput(fEditor);
			if (editorInput instanceof ICompilationUnit)
				run(selection.getOffset(), selection.getLength(), (ICompilationUnit) editorInput);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), ActionMessages.IntroduceParameterObjectAction_exceptiondialog_title, ActionMessages.IntroduceParameterObjectAction_unexpected_exception);
		}
	}

	private void run(int offset, int length, ICompilationUnit unit) throws CoreException {
		if (!ActionUtil.isEditable(fEditor, getShell(), unit))
			return;
		RefactoringExecutionStarter.startIntroduceParameterObject(unit, offset, length, getShell());
	}

	private void run(IMethod method) throws CoreException {
		if (!ActionUtil.isEditable(fEditor, getShell(), method))
			return;
		RefactoringExecutionStarter.startIntroduceParameterObject(method, getShell());
	}
}

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
package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

/**
 * Abstract Action for opening a Java editor.
 */
public abstract class OpenEditorAction extends Action {
	protected String fClassName;
	protected TestRunnerViewPart fTestRunner;
	private final boolean fActivate;
	
	/**
	 * Constructor for OpenEditorAction.
	 */
	protected OpenEditorAction(TestRunnerViewPart testRunner, String testClassName) {
		this(testRunner, testClassName, true);
	}

	public OpenEditorAction(TestRunnerViewPart testRunner, String className, boolean activate) {
		super(JUnitMessages.OpenEditorAction_action_label); 
		fClassName= className;
		fTestRunner= testRunner;
		fActivate= activate;
	}

	/*
	 * @see IAction#run()
	 */
	public void run() {
		ITextEditor textEditor= null;
		try {
			IJavaElement element= findElement(getLaunchedProject(), fClassName);
			if (element == null) {
				MessageDialog.openError(getShell(), 
					JUnitMessages.OpenEditorAction_error_cannotopen_title, JUnitMessages.OpenEditorAction_error_cannotopen_message); 
				return;
			} 
			textEditor= (ITextEditor)EditorUtility.openInEditor(element, fActivate);			
		} catch (CoreException e) {
			ErrorDialog.openError(getShell(), JUnitMessages.OpenEditorAction_error_dialog_title, JUnitMessages.OpenEditorAction_error_dialog_message, e.getStatus()); 
			return;
		}
		if (textEditor == null) {
			fTestRunner.setInfoMessage(JUnitMessages.OpenEditorAction_message_cannotopen); 
			return;
		}
		reveal(textEditor);
	}
	
	protected Shell getShell() {
		return fTestRunner.getSite().getShell();
	}

	protected IJavaProject getLaunchedProject() {
		return fTestRunner.getLaunchedProject();
	}
	
	protected String getClassName() {
		return fClassName;
	}

	protected abstract IJavaElement findElement(IJavaProject project, String className) throws CoreException;
	
	protected abstract void reveal(ITextEditor editor);
	
}

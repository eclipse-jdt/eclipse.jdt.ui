/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

/**
 * Abstract Action for opening a Java editor.
 */
public abstract class OpenEditorAction extends Action {
	protected String fClassName;
	protected TestRunnerViewPart fTestRunner;
	
	/**
	 * Constructor for OpenEditorAction.
	 */
	protected OpenEditorAction(TestRunnerViewPart testRunner, String testClassName) {
		super("&Goto File");
		fClassName= testClassName;
		fTestRunner= testRunner;
	}

	/*
	 * @see IAction#run()
	 */
	public void run() {
		ITextEditor textEditor= null;
		try {
			IJavaElement element= findElement(fTestRunner.getLaunchedProject(), fClassName);
			if (element == null) {
				MessageDialog.openError(fTestRunner.getSite().getShell(), 
					"Cannot Open Editor", "Test class not found in project");
				return;
			}
			// use of internal API for backward compatibility with 1.0
			textEditor= (ITextEditor)EditorUtility.openInEditor(element, false);			
		} catch (CoreException e) {
			ErrorDialog.openError(fTestRunner.getSite().getShell(), "Error", "Could not open editor", e.getStatus());
			return;
		}
		if (textEditor == null) {
			fTestRunner.postInfo("Could not open editor");
			return;
		}
		reveal(textEditor);
	}
	
	protected abstract IJavaElement findElement(IJavaProject project, String className) throws JavaModelException;
	
	protected abstract void reveal(ITextEditor editor);
}

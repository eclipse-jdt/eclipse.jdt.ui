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
		super(JUnitMessages.getString("OpenEditorAction.action.label")); //$NON-NLS-1$
		fClassName= testClassName;
		fTestRunner= testRunner;
	}

	/*
	 * @see IAction#run()
	 */
	public void run() {
		ITextEditor textEditor= null;
		try {
			IJavaElement element= findElement(getLaunchedProject(), fClassName);
			if (element == null) {
				MessageDialog.openError(fTestRunner.getSite().getShell(), 
					JUnitMessages.getString("OpenEditorAction.error.cannotopen.title"), JUnitMessages.getString("OpenEditorAction.error.cannotopen.message")); //$NON-NLS-1$ //$NON-NLS-2$
				return;
			}
			// use of internal API for backward compatibility with 1.0
			textEditor= (ITextEditor)EditorUtility.openInEditor(element, false);			
		} catch (CoreException e) {
			ErrorDialog.openError(fTestRunner.getSite().getShell(), JUnitMessages.getString("OpenEditorAction.error.dialog.title"), JUnitMessages.getString("OpenEditorAction.error.dialog.message"), e.getStatus()); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		if (textEditor == null) {
			fTestRunner.postInfo(JUnitMessages.getString("OpenEditorAction.message.cannotopen")); //$NON-NLS-1$
			return;
		}
		reveal(textEditor);
	}
	
	protected IJavaProject getLaunchedProject() {
		return fTestRunner.getLaunchedProject();
	}
	
	protected String getClassName() {
		return fClassName;
	}

	protected abstract IJavaElement findElement(IJavaProject project, String className) throws JavaModelException;
	
	protected abstract void reveal(ITextEditor editor);
	
}

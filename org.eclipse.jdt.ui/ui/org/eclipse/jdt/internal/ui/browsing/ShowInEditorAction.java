/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.browsing;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

class ShowInEditorAction extends Action {

	private IEditorPart fEditor;

	public void run() {
		IWorkbenchPage wbPage= JavaPlugin.getDefault().getActivePage();
		if (wbPage == null)
			return;
		run(wbPage.getSelection(), wbPage);
	}
	
	void run(ISelection selection, IWorkbenchPage wbPage) {
		if (selection.isEmpty() || !(selection instanceof IStructuredSelection))
			return;

		Object target= ((IStructuredSelection)selection).getFirstElement();
		if (!(target instanceof IJavaElement))
			return;

		IJavaElement javaElement= (IJavaElement)target;
		if (!javaElement.exists()) {
			beep();
			return;
		}
		if (javaElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
			// Don't open
		} else
			try  {
				IEditorPart editor= EditorUtility.openInEditor(javaElement, false);
				EditorUtility.revealInEditor(editor, javaElement);
			} catch (JavaModelException ex) {
				ExceptionHandler.handle(ex, JavaBrowsingMessages.getString("ShowInEditorAction.Error.openEditor.title"), JavaBrowsingMessages.getString("ShowInEditorAction.Error.openEditor.message")); //$NON-NLS-2$ //$NON-NLS-1$
				return;
			} catch (PartInitException ex) {
				ExceptionHandler.handle(ex, JavaBrowsingMessages.getString("ShowInEditorAction.Error.openEditor.title"), JavaBrowsingMessages.getString("ShowInEditorAction.Error.openEditor.message")); //$NON-NLS-2$ //$NON-NLS-1$
				return;
			}
	}
	
	private IClassFile getClassFile(IJavaElement jElement) {
		if (jElement instanceof IMember)
			return ((IMember)jElement).getClassFile();
		return null;
	}


	private boolean isBinary(IJavaElement jElement) {
		if (jElement instanceof IMember)
			return ((IMember)jElement).isBinary();
		return false;
	}

	private void beep() {
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		if (shell != null && shell.getDisplay() != null)
			shell.getDisplay().beep();
	}	
}
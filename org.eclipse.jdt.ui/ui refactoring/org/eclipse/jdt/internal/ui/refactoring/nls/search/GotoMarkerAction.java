/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.nls.search;

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

import org.eclipse.search.internal.ui.SearchPlugin;
import org.eclipse.search.ui.ISearchResultView;
import org.eclipse.search.ui.ISearchResultViewEntry;
import org.eclipse.search.ui.SearchUI;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;

class GotoMarkerAction extends Action {

	private IEditorPart fEditor;

	public void run() {
		ISearchResultView view= SearchUI.getSearchResultView();		
		view.getSelection();
		ISelection selection= view.getSelection();
		Object element= null;
		if (selection instanceof IStructuredSelection)
			element= ((IStructuredSelection)selection).getFirstElement();
		if (element instanceof ISearchResultViewEntry) {
			ISearchResultViewEntry entry= (ISearchResultViewEntry)element;
			show(entry.getSelectedMarker());
		}
	}
	
	public void show(IMarker marker) {
		IResource resource= marker.getResource();
		if (resource instanceof IFile)
			showInEditor(marker, new FileEditorInput((IFile)resource), JavaUI.ID_CU_EDITOR);
	}

	public void showInEditor(IMarker marker, IEditorInput input, String editorId) {
		IWorkbenchPage page= SearchPlugin.getActivePage();
		if (page == null)
			return;
		
		IEditorPart editor= null;
		IEditorPart[] editorParts= page.getEditors();
		for (int i= 0; i < editorParts.length; i++) {
			IEditorPart part= editorParts[i];
			if (input.equals(part.getEditorInput())) {
				editor= part;
				break;
			}
		}
		if (editor == null) {
			if (fEditor != null && !fEditor.isDirty())
					page.closeEditor(fEditor, false);
			try {
				editor= page.openEditor(input, editorId, false);
			} catch (PartInitException ex) {
				ExceptionHandler.handle(ex, NLSSearchMessages.getString("Search.Error.openEditor.title"), NLSSearchMessages.getString("Search.Error.openEditor.message")); //$NON-NLS-2$ //$NON-NLS-1$
				return;
			}
		} else {
			page.bringToTop(editor);
		}
		if (editor != null) {
			editor.gotoMarker(marker);
			fEditor = editor;
		}
	}
	
	private IClassFile getClassFile(IJavaElement jElement) {
		if (jElement instanceof IMember)
			return ((IMember)jElement).getClassFile();
		return null;
	}

	private IJavaElement getJavaElement(IMarker marker) {
		try {
			return JavaCore.create((String)marker.getAttribute(INLSSearchUIConstants.ATT_JE_HANDLE_ID));
		} catch (CoreException ex) {
			ExceptionHandler.handle(ex, NLSSearchMessages.getString("Search.Error.createJavaElement.title"), NLSSearchMessages.getString("Search.Error.createJavaElement.message")); //$NON-NLS-2$ //$NON-NLS-1$
			return null;
		}
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
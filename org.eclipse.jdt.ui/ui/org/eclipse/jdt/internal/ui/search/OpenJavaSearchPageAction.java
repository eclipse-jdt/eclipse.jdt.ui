/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import org.eclipse.search.ui.SearchUI;

import org.eclipse.search.internal.ui.SearchPlugin;

/**
 * Opens the Search Dialog.
 */
public class OpenJavaSearchPageAction extends Action implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow fWindow;

	public OpenJavaSearchPageAction() {
	}

	public void init(IWorkbenchWindow window) {
		fWindow= window;
	}

	public void run(IAction action) {
		run();
	}

	public void run() {
		if (getWindow().getActivePage() == null) {
			SearchPlugin.beep();
			return;
		}
		SearchUI.openSearchDialog(getWindow(), "org.eclipse.jdt.ui.JavaSearchPage"); //$NON-NLS-1$
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// do nothing since the action isn't selection dependent.
	}

	private ISelection getSelection() {
		return getWindow().getSelectionService().getSelection();
	}
	
	private IEditorPart getEditorPart() {
		return getWindow().getActivePage().getActiveEditor();
	}

	private IWorkbenchWindow getWindow() {
		if (fWindow == null)
			fWindow= SearchPlugin.getActiveWorkbenchWindow();
		return fWindow;
	}

	public void dispose() {
		fWindow= null;
	}
}

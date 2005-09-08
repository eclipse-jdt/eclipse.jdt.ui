/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.actions;

import java.util.Arrays;
import java.util.Comparator;
import java.util.ResourceBundle;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.internal.core.refactoring.history.IRefactoringHistory;
import org.eclipse.ltk.internal.core.refactoring.history.RefactoringDescriptorHandle;
import org.eclipse.ltk.internal.ui.refactoring.history.RefactoringHistoryDialog;

import org.eclipse.jdt.internal.corext.Assert;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Action to show the workspace refactoring history.
 * 
 * @since 3.2
 */
public class ShowWorkspaceHistoryActionDelegate implements IWorkbenchWindowActionDelegate {

	/** The bundle name */
	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringHistoryDialog"; //$NON-NLS-1$

	/** The workbench window */
	private IWorkbenchWindow fWindow= null;

	/**
	 * Creates a new show workspace history action delegate.
	 */
	public ShowWorkspaceHistoryActionDelegate() {
		// Do nothing
	}

	/**
	 * @inheritDoc
	 */
	public final void dispose() {
		// Do nothing
	}

	/**
	 * Returns the desired refactoring descriptors from the history.
	 * 
	 * @param history
	 *            the refactoring history
	 * @return the handles
	 */
	protected RefactoringDescriptorHandle[] getDescriptors(final IRefactoringHistory history) {
		Assert.isNotNull(history);
		return history.getWorkspaceHistory();
	}

	/**
	 * @inheritDoc
	 */
	public final void init(final IWorkbenchWindow window) {
		fWindow= window;
	}

	/**
	 * Initializes the refactoring history dialog.
	 * 
	 * @param dialog
	 *            the history dialog to initialize
	 */
	protected void initializeHistoryDialog(final RefactoringHistoryDialog dialog) {
		Assert.isNotNull(dialog);
		// Do nothing
	}

	/**
	 * @inheritDoc
	 */
	public final void run(final IAction action) {
		Shell shell= null;
		if (fWindow != null)
			shell= fWindow.getShell();
		if (shell == null)
			shell= JavaPlugin.getActiveWorkbenchShell();
		final IRefactoringHistory history= RefactoringCore.getRefactoringHistory();
		RefactoringDescriptorHandle[] handles= {};
		try {
			history.connect();
			handles= getDescriptors(history);
		} finally {
			history.disconnect();
		}
		Arrays.sort(handles, new Comparator() {

			public final int compare(final Object first, final Object second) {
				final RefactoringDescriptorHandle predecessor= (RefactoringDescriptorHandle) first;
				final RefactoringDescriptorHandle successor= (RefactoringDescriptorHandle) second;
				return (int) (predecessor.getTimeStamp() - successor.getTimeStamp());
			}
		});
		final RefactoringHistoryDialog dialog= new RefactoringHistoryDialog(shell, ResourceBundle.getBundle(BUNDLE_NAME), handles);
		initializeHistoryDialog(dialog);
		dialog.open();
	}

	/**
	 * @inheritDoc
	 */
	public void selectionChanged(final IAction action, final ISelection selection) {
		// Do nothing
	}
}

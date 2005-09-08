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

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IProject;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ltk.internal.core.refactoring.history.IRefactoringHistory;
import org.eclipse.ltk.internal.core.refactoring.history.RefactoringDescriptorHandle;
import org.eclipse.ltk.internal.ui.refactoring.history.RefactoringHistoryDialog;

/**
 * Action to show a project refactoring history.
 * 
 * @since 3.2
 */
public final class ShowProjectHistoryActionDelegate extends ShowWorkspaceHistoryActionDelegate {

	/** The selected project */
	private IProject fProject= null;

	/**
	 * Creates a new show project history action delegate.
	 */
	public ShowProjectHistoryActionDelegate() {
		// Do nothing
	}

	/**
	 * @inheritDoc
	 */
	protected final RefactoringDescriptorHandle[] getDescriptors(final IRefactoringHistory history) {
		return history.getProjectHistory(fProject);
	}

	/**
	 * @inheritDoc
	 */
	protected final void initializeHistoryDialog(final RefactoringHistoryDialog dialog) {
		super.initializeHistoryDialog(dialog);
		dialog.setProject(fProject);
	}

	/**
	 * @inheritDoc
	 */
	public final void selectionChanged(final IAction action, final ISelection selection) {
		super.selectionChanged(action, selection);
		if (selection instanceof IStructuredSelection) {
			final IStructuredSelection structured= (IStructuredSelection) selection;
			if (structured.size() == 1) {
				final Object element= structured.getFirstElement();
				if (element instanceof IAdaptable) {
					final IAdaptable adaptable= (IAdaptable) element;
					final IProject project= (IProject) adaptable.getAdapter(IProject.class);
					if (project != null) {
						fProject= project;
						action.setEnabled(true);
						return;
					}
				}
			}
		}
		fProject= null;
		action.setEnabled(false);
	}
}

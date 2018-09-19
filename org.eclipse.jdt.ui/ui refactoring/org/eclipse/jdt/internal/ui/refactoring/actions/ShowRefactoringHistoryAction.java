/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * Action to show the global refactoring history.
 *
 * TODO: remove once bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=131746 is fixed
 */
public final class ShowRefactoringHistoryAction implements IWorkbenchWindowActionDelegate {

	/** The workbench window, or <code>null</code> */
	private IWorkbenchWindow fWindow= null;

	@Override
	public void dispose() {
		// Do nothing
	}

	@Override
	public void init(final IWorkbenchWindow window) {
		fWindow= window;
	}

	@Override
	public void run(final IAction a) {
		if (fWindow != null) {
			org.eclipse.ltk.ui.refactoring.actions.ShowRefactoringHistoryAction action= new org.eclipse.ltk.ui.refactoring.actions.ShowRefactoringHistoryAction();
			action.init(fWindow);
			action.run(a);
		}
	}

	@Override
	public void selectionChanged(final IAction action, final ISelection selection) {
		// Do nothing
	}
}

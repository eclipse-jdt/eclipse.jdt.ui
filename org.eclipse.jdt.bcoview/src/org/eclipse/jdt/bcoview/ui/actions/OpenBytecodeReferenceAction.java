/*******************************************************************************
 * Copyright (c) 2023 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.bcoview.ui.actions;

import org.eclipse.jdt.bcoview.BytecodeOutlinePlugin;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Opens a bytecode reference from view
 */
public class OpenBytecodeReferenceAction implements IViewActionDelegate {

	public OpenBytecodeReferenceAction() {
		super();
	}

	@Override
	public void run(IAction action) {
		try {
			IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			activePage.showView("org.eclipse.jdt.bcoview.views.BytecodeReferenceView"); //$NON-NLS-1$
		} catch (PartInitException e) {
			BytecodeOutlinePlugin.error("Could not open Bytecode Reference View: " + e.getMessage(), e); //$NON-NLS-1$
		}

	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// no op
	}

	@Override
	public void init(IViewPart view) {
		// no op
	}

}

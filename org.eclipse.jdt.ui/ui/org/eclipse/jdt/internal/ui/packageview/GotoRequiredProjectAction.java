/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Goto to the referenced required project
 */
class GotoRequiredProjectAction extends Action {

	private PackageExplorerPart fPackageExplorer;

	GotoRequiredProjectAction(PackageExplorerPart part) {
		super(PackagesMessages.GotoRequiredProjectAction_label);
		setDescription(PackagesMessages.GotoRequiredProjectAction_description);
		setToolTipText(PackagesMessages.GotoRequiredProjectAction_tooltip);
		fPackageExplorer= part;
	}

	@Override
	public void run() {
		IStructuredSelection selection= (IStructuredSelection)fPackageExplorer.getSite().getSelectionProvider().getSelection();
		Object element= selection.getFirstElement();
		if (element instanceof ClassPathContainer.RequiredProjectWrapper) {
			ClassPathContainer.RequiredProjectWrapper wrapper= (ClassPathContainer.RequiredProjectWrapper) element;
			fPackageExplorer.tryToReveal(wrapper.getProject());
		}
	}
}

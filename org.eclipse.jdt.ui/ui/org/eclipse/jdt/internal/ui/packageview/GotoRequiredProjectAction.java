/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
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
		super(PackagesMessages.getString("GotoRequiredProjectAction.label"));  //$NON-NLS-1$
		setDescription(PackagesMessages.getString("GotoRequiredProjectAction.description"));  //$NON-NLS-1$
		setToolTipText(PackagesMessages.getString("GotoRequiredProjectAction.tooltip"));  //$NON-NLS-1$
		fPackageExplorer= part;
	}
 
	public void run() { 
		IStructuredSelection selection= (IStructuredSelection)fPackageExplorer.getSite().getSelectionProvider().getSelection();
		Object element= selection.getFirstElement();
		if (element instanceof ClassPathContainer.RequiredProjectWrapper) {
			ClassPathContainer.RequiredProjectWrapper wrapper= (ClassPathContainer.RequiredProjectWrapper) element;
			fPackageExplorer.tryToReveal(wrapper.getProject());
		}
	}
}

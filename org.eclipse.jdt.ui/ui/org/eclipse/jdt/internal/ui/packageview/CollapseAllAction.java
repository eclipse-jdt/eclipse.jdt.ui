/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;

/**
 * Collapse all nodes.
 */
class CollapseAllAction extends Action {
	
	private PackageExplorerPart fPackageExplorer;
	
	CollapseAllAction(PackageExplorerPart part) {
		super(PackagesMessages.getString("CollapseAllAction.label")); //$NON-NLS-1$
		setDescription(PackagesMessages.getString("CollapseAllAction.description")); //$NON-NLS-1$
		setToolTipText(PackagesMessages.getString("CollapseAllAction.tooltip")); //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(this, "collapseall.gif"); //$NON-NLS-1$
		
		fPackageExplorer= part;
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.COLLAPSE_ALL_ACTION);
	}
 
	public void run() { 
		fPackageExplorer.collapseAll();
	}
}

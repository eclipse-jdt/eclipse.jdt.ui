/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.help.WorkbenchHelp;

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
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.COLLAPSE_ALL_ACTION);
	}
 
	public void run() { 
		fPackageExplorer.collapseAll();
	}
}
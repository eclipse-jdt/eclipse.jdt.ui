/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation 
 *          (report 36180: Callers/Callees view)
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jface.action.Action;

import org.eclipse.ui.PlatformUI;

/**
 * This class is copied from the org.eclipse.search2.internal.ui.CancelSearchAction class. 
 */
public class CancelSearchAction extends Action {

	private CallHierarchyViewPart fView;

	public CancelSearchAction(CallHierarchyViewPart view) {
		super(CallHierarchyMessages.getString("CancelSearchAction.label")); //$NON-NLS-1$
		fView= view;
		setToolTipText(CallHierarchyMessages.getString("CancelSearchAction.tooltip")); //$NON-NLS-1$
        JavaPluginImages.setLocalImageDescriptors(this, "ch_cancel.gif"); //$NON-NLS-1$

        PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_CANCEL_SEARCH_ACTION);
}
	
	public void run() {
		fView.cancelJobs();
	}
}

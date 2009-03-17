/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation
 * 			(report 36180: Callers/Callees view)
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import org.eclipse.jface.action.Action;

import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

class RefreshAction extends Action {
    private CallHierarchyViewPart fPart;

	public RefreshAction(CallHierarchyViewPart part) {
		fPart= part;
		setText(CallHierarchyMessages.RefreshAction_text);
		setToolTipText(CallHierarchyMessages.RefreshAction_tooltip);
		JavaPluginImages.setLocalImageDescriptors(this, "refresh_nav.gif");//$NON-NLS-1$
		setActionDefinitionId(IWorkbenchCommandConstants.FILE_REFRESH);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_REFRESH_ACTION);
	}

    /**
     * @see org.eclipse.jface.action.Action#run()
     */
    public void run() {
        fPart.refresh();
    }
}

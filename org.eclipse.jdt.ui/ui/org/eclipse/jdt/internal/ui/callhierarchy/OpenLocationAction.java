/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation 
 * 			(report 36180: Callers/Callees view)
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import java.util.Iterator;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.corext.callhierarchy.CallLocation;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

class OpenLocationAction extends SelectionDispatchAction {
    public OpenLocationAction(IWorkbenchSite site) {
        super(site);
        setText(CallHierarchyMessages.getString("OpenLocationAction.label")); //$NON-NLS-1$
        setToolTipText(CallHierarchyMessages.getString("OpenLocationAction.tooltip")); //$NON-NLS-1$
    }

    private boolean checkEnabled(IStructuredSelection selection) {
        if (selection.isEmpty()) {
            return false;
        }

        for (Iterator iter = selection.iterator(); iter.hasNext();) {
            Object element = (Object) iter.next();

            if (element instanceof MethodWrapper) {
                continue;
            } else if (element instanceof CallLocation) {
                continue;
            }

            return false;
        }

        return true;
    }

    /* (non-Javadoc)
 * Method declared on SelectionDispatchAction.
 */
    public void run(IStructuredSelection selection) {
        if (!checkEnabled(selection)) {
            return;
        }

        run(selection.getFirstElement());
    }

    public void run(Object element) {
        CallHierarchyUI.openInEditor(element, getShell(), getDialogTitle());
    }

    private String getDialogTitle() {
        return CallHierarchyMessages.getString("OpenLocationAction.error.title"); //$NON-NLS-1$
    }
}

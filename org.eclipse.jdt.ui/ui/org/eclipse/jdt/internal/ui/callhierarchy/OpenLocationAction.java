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

class OpenLocationAction extends SelectionDispatchAction {
    public OpenLocationAction(IWorkbenchSite site) {
        super(site);
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
    protected void run(IStructuredSelection selection) {
        if (!checkEnabled(selection)) {
            return;
        }

        run(selection.getFirstElement());
    }

    private void run(Object element) {
        CallHierarchy.openInEditor(element, getShell(), getDialogTitle());
    }

    private String getDialogTitle() {
        return CallHierarchyMessages.getString("OpenLocationAction.error.title"); //$NON-NLS-1$
    }
}

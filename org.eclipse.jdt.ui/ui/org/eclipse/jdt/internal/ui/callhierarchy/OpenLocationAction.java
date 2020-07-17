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
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation
 * 			(report 36180: Callers/Callees view)
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.internal.corext.callhierarchy.CallLocation;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

class OpenLocationAction extends SelectionDispatchAction {
    private CallHierarchyViewPart fPart;

    public OpenLocationAction(CallHierarchyViewPart part, IWorkbenchSite site) {
        super(site);
        fPart= part;
		LocationViewer viewer= fPart.getLocationViewer();
        setText(CallHierarchyMessages.OpenLocationAction_label);
        setToolTipText(CallHierarchyMessages.OpenLocationAction_tooltip);
		setEnabled(!fPart.getSelection().isEmpty());

		viewer.addSelectionChangedListener(event -> setEnabled(!event.getSelection().isEmpty()));
	}

    private boolean checkEnabled(IStructuredSelection selection) {
        if (selection.isEmpty()) {
            return false;
        }

        for (Object element : selection) {
            if (element instanceof MethodWrapper
                    || element instanceof CallLocation) {
                continue;
            }

            return false;
        }

        return true;
    }

    @Override
	public ISelection getSelection() {
        return fPart.getSelection();
    }

    @Override
	public void run(IStructuredSelection selection) {
        if (!checkEnabled(selection))
            return;

        for (Object name : selection) {
	        boolean noError= CallHierarchyUI.openInEditor(name, getShell(), OpenStrategy.activateOnOpen());
	        if (! noError)
	        	return;
		}
    }
}

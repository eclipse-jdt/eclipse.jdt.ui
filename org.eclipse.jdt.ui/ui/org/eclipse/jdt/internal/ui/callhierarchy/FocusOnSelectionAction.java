/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;

import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

class FocusOnSelectionAction extends Action {
    private CallHierarchyViewPart fPart;

    public FocusOnSelectionAction(CallHierarchyViewPart part) {
        super(CallHierarchyMessages.getString("FocusOnSelectionAction.focusOnSelection.text")); //$NON-NLS-1$
        fPart= part;
        setDescription(CallHierarchyMessages.getString("FocusOnSelectionAction.focusOnSelection.description")); //$NON-NLS-1$
        setToolTipText(CallHierarchyMessages.getString("FocusOnSelectionAction.focusOnSelection.tooltip")); //$NON-NLS-1$
        WorkbenchHelp.setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_FOCUS_ON_SELECTION_ACTION);
    }

    public boolean canActionBeAdded() {
        Object element = SelectionUtil.getSingleElement(getSelection());

        IMethod method = getSelectedMethod(element);
        
        if (method != null) {
            setText(CallHierarchyMessages.getFormattedString("FocusOnSelectionAction.focusOn.text", method.getElementName())); //$NON-NLS-1$

            return true;
        }

        return false;
    }

    private IMethod getSelectedMethod(Object element) {
		IMethod method = null;
        
        if (element instanceof IMethod) {
            method= (IMethod) element;
        } else if (element instanceof MethodWrapper) {
            IMember member= ((MethodWrapper) element).getMember();
            if (member.getElementType() == IJavaElement.METHOD) {
                method= (IMethod) member;
            }
        }
		return method;
	}

	/*
     * @see Action#run
     */
    public void run() {
        Object element = SelectionUtil.getSingleElement(getSelection());

        IMethod method= getSelectedMethod(element);
        if (method != null) {
                fPart.setMethod(method);
        }
    }

    private ISelection getSelection() {
        ISelectionProvider provider = fPart.getSite().getSelectionProvider();

        if (provider != null) {
            return provider.getSelection();
        }

        return null;
    }
}
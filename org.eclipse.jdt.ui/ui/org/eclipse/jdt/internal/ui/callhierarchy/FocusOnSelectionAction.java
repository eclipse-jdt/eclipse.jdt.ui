/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation 
 * 			(report 36180: Callers/Callees view)
 *   Stephan Herrmann (stephan@cs.tu-berlin.de):
 *          - bug 75800: [call hierarchy] should allow searches for fields
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IMember;

import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;

class FocusOnSelectionAction extends Action {
    private CallHierarchyViewPart fPart;

    public FocusOnSelectionAction(CallHierarchyViewPart part) {
        super(CallHierarchyMessages.FocusOnSelectionAction_focusOnSelection_text); 
        fPart= part;
        setDescription(CallHierarchyMessages.FocusOnSelectionAction_focusOnSelection_description); 
        setToolTipText(CallHierarchyMessages.FocusOnSelectionAction_focusOnSelection_tooltip); 
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_FOCUS_ON_SELECTION_ACTION);
    }

    public boolean canActionBeAdded() {
        Object element = SelectionUtil.getSingleElement(getSelection());

        IMember member = getSelectedMember(element);
        
        if (member != null) {
            setText(Messages.format(CallHierarchyMessages.FocusOnSelectionAction_focusOn_text, member.getElementName())); 

            return true;
        }

        return false;
    }

    private IMember getSelectedMember(Object element) {
		if (CallHierarchy.isPossibleParent(element)) {
			return (IMember) element;
		} else if (element instanceof MethodWrapper) {
			IMember wrapped= ((MethodWrapper) element).getMember();
			if (CallHierarchy.isPossibleParent(wrapped)) {
				return wrapped;
			}
		}
		return null;
	}

	/*
     * @see Action#run
     */
    public void run() {
        Object element = SelectionUtil.getSingleElement(getSelection());

        IMember member= getSelectedMember(element);
		if (member != null) {
			fPart.setRootElement(member);
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

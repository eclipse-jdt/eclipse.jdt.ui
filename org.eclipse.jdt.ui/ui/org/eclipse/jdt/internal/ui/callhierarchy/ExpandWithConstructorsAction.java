/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.callhierarchy.CallerMethodWrapper;
import org.eclipse.jdt.internal.corext.callhierarchy.RealCallers;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

/**
 * The action to expand the selected member hierarchy with constructor calls.
 * 
 * @since 3.5
 */
class ExpandWithConstructorsAction extends Action {

	/**
	 * The call hierarchy view part.
	 */
	private CallHierarchyViewPart fPart;

	/**
	 * The call hierarchy viewer.
	 */
	private CallHierarchyViewer fCallHierarchyViewer;

	/**
	 * Creates the action for expanding the hierarchy with constructor calls.
	 * 
	 * @param callHierarchyViewPart the call hierarchy view part
	 * @param callHierarchyViewer the call hierarchy viewer
	 */
	public ExpandWithConstructorsAction(CallHierarchyViewPart callHierarchyViewPart, CallHierarchyViewer callHierarchyViewer) {
		super(CallHierarchyMessages.ExpandWithConstructorsAction_expandWithConstructors_text, AS_CHECK_BOX);
		fPart= callHierarchyViewPart;
		fCallHierarchyViewer= callHierarchyViewer;
		setDescription(CallHierarchyMessages.ExpandWithConstructorsAction_expandWithConstructors_description);
		setToolTipText(CallHierarchyMessages.ExpandWithConstructorsAction_expandWithConstructors_tooltip);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_EXPAND_WITH_CONSTRUCTORS_ACTION);

	}


	/*
	 * @see Action#run
	 */
	public void run() {
		boolean isChecked= isChecked();

		CallerMethodWrapper[] members= getSelectedInputElements();
		for (int i= 0; i < members.length; i++) {
			members[i].setExpandWithConstructors(isChecked);
			fCallHierarchyViewer.refresh(members[i]);
			fCallHierarchyViewer.setExpandedState(members[i], isChecked);
		}
	}

	/**
	 * Return array of selected elements as caller method wrappers.
	 * 
	 * @return array of selected elements as caller method wrapper elements, <code>null</code> in
	 *         case of invalid selection
	 */
	private CallerMethodWrapper[] getSelectedInputElements() {
		ISelection selection= getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection= (IStructuredSelection)selection;
			CallerMethodWrapper[] wrappers= new CallerMethodWrapper[((IStructuredSelection)selection).size()];
			int i= 0;
			for (Iterator iter= structuredSelection.iterator(); iter.hasNext();) {
				Object elem= iter.next();
				if (elem instanceof CallerMethodWrapper) {
					wrappers[i++]= (CallerMethodWrapper)elem;
				} else {
					return null;
				}
			}
			return wrappers;
		}
		return null;
	}

	/**
	 * Gets the selection from the call hierarchy view part.
	 * 
	 * @return the current selection
	 */
	private ISelection getSelection() {
		return fPart.getSelection();
	}

	/**
	 * Checks whether this action can be added for the selected element in the call hierarchy.
	 * 
	 * @return <code> true</code> if the action can be added, <code>false</code> otherwise
	 */
	public boolean canActionBeAdded() {
		if (fPart.getCallMode() == CallHierarchyViewPart.CALL_MODE_CALLEES)
			return false;
		ISelection selection= getSelection();
		if (selection.isEmpty())
			return false;
		
		boolean allElementsChecked= true;
		for (Iterator iter= ((IStructuredSelection)selection).iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (!(element instanceof CallerMethodWrapper) || element instanceof RealCallers)
				return false;
			
			CallerMethodWrapper wrapper= (CallerMethodWrapper)element;
			IMember member= wrapper.getMember();
			if (!(member instanceof IMethod))
				return false;
			IMethod method= (IMethod)member;
			try {
				if (JdtFlags.isStatic(method) || method.isConstructor())
					return false;
			} catch (JavaModelException e) {
				return false; // don't try to work with inexistent elements
			}
			if (wrapper.getExpandWithConstructors() != true) {
				allElementsChecked= false;
			}
		}
		if (allElementsChecked) {
			setChecked(true);
		} else {
			setChecked(false);
		}
		return true;
	}
}

/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TransferData;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.jdt.core.IMember;

import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;

import org.eclipse.jdt.internal.ui.dnd.ViewerInputDropAdapter;

class CallHierarchyTransferDropAdapter extends ViewerInputDropAdapter {

	private CallHierarchyViewPart fCallHierarchyViewPart;

	public CallHierarchyTransferDropAdapter(CallHierarchyViewPart viewPart, StructuredViewer viewer) {
		super(viewer);
		fCallHierarchyViewPart= viewPart;
	}

	/**
	 * {@inheritDoc}
	 */
	protected void doInputView(Object inputElements) {
		IMember[] newElements= (IMember[])inputElements;
		IMember[] members= fCallHierarchyViewPart.getInputElements();
		boolean noInput= members != null && members.length > 0;
		if (getCurrentOperation() == DND.DROP_LINK || !noInput) {
			fCallHierarchyViewPart.setInputElements(newElements);
			return;
		} 
		List/*<IMember>*/oldElements= Arrays.asList(members);
		List/*<IMember>*/elements= new ArrayList/*<IMember>*/();
		for (int i= 0; i < newElements.length; i++) {
			if (!oldElements.contains(newElements[i]))
				elements.add(newElements[i]);
		}
		if (elements.size() == 0)
			return;
		newElements= (IMember[])elements.toArray(new IMember[elements.size()]);
		fCallHierarchyViewPart.addInputElements(newElements);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.dnd.ViewerInputDropAdapter#performDrop(java.lang.Object)
	 * @since 3.7
	 */
	public boolean performDrop(Object data) {
		setSelectionFeedbackEnabled(false);
		setExpandEnabled(false);

		if (getCurrentTarget() != null)
			return super.performDrop(data);

		Object input= getInputElement(getSelection());
		if (input != null) {
			doInputView(input);
			return true;
		}
		return super.performDrop(data);
	}

	/**
	 * {@inheritDoc}
	 */
	protected Object getInputElement(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			List elements= ((IStructuredSelection) selection).toList();
			if (CallHierarchy.arePossibleInputElements(elements)) {
				return elements.toArray(new IMember[elements.size()]);
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.dnd.ViewerInputDropAdapter#determineOperation(java.lang.Object, int, org.eclipse.swt.dnd.TransferData, int)
	 * @since 3.7
	 */
	protected int determineOperation(Object target, int operation, TransferData transferType, int operations) {
		setSelectionFeedbackEnabled(false);
		setExpandEnabled(false);

		initializeSelection();

		if (target != null) {
			return super.determineOperation(target, operation, transferType, operations);
		} else if (getInputElement(getSelection()) != null) {
			setSelectionFeedbackEnabled(false);
			setExpandEnabled(false);
			return operation == DND.DROP_DEFAULT || operation == DND.DROP_MOVE ? DND.DROP_LINK : operation;
		} else {
			return DND.DROP_NONE;
		}
	}
}

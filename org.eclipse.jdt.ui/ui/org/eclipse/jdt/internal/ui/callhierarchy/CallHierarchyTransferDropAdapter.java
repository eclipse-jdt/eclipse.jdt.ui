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

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.jdt.core.IMember;

import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;

import org.eclipse.jdt.internal.ui.dnd.ViewerInputDropAdapter;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;

class CallHierarchyTransferDropAdapter extends ViewerInputDropAdapter {
	
	private CallHierarchyViewPart fCallHierarchyViewPart;

	public CallHierarchyTransferDropAdapter(CallHierarchyViewPart viewPart, StructuredViewer viewer) {
		super(viewer);
		fCallHierarchyViewPart= viewPart;
	}	
	
	/**
	 * {@inheritDoc}
	 */
	protected void doInputView(Object inputElement) {
		fCallHierarchyViewPart.setRootElement((IMember) inputElement);
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected Object getInputElement(ISelection selection) {
		Object single= SelectionUtil.getSingleElement(selection);
		if (single == null)
			return null;
		
		return getCandidate(single);
	}
    
    private static IMember getCandidate(Object input) {
        if (! CallHierarchy.isPossibleParent(input))
            return null;
        
        return (IMember) input;
    }
}

/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.jdt.core.IMethod;

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
		fCallHierarchyViewPart.setMethod((IMethod) inputElement);
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
    
    private static IMethod getCandidate(Object input) {
        if (!(input instanceof IMethod))
            return null;
        
        return (IMethod) input;
    }
}

/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;

import org.eclipse.jdt.internal.ui.packageview.SelectionTransferDropAdapter;
import org.eclipse.jdt.internal.ui.util.OpenTypeHierarchyUtil;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;

public class TypeHierarchyTransferDropAdapter extends SelectionTransferDropAdapter {

	private static final int OPERATION = DND.DROP_LINK;
	private TypeHierarchyViewPart fTypeHierarchyViewPart;

	public TypeHierarchyTransferDropAdapter(TypeHierarchyViewPart viewPart, AbstractTreeViewer viewer) {
		super(viewer);
		fTypeHierarchyViewPart= viewPart;
	}

	public void validateDrop(Object target, DropTargetEvent event, int operation) {
		event.detail= DND.DROP_NONE;
		initializeSelection();
		if (target != null){
			super.validateDrop(target, event, operation);
			return;
		}	
		if (getInputElement(getSelection()) != null) 
			event.detail= TypeHierarchyTransferDropAdapter.OPERATION;
	}

	public void drop(Object target, DropTargetEvent event) {
		if (target != null || event.detail != TypeHierarchyTransferDropAdapter.OPERATION){
			super.drop(target, event);
			return;
		}	
		IJavaElement input= getInputElement(getSelection());
		fTypeHierarchyViewPart.setInputElement(input);
		if (input instanceof IMember) 
			fTypeHierarchyViewPart.selectMember((IMember) input);
	}
	
	private static IJavaElement getInputElement(ISelection selection) {
		Object single= SelectionUtil.getSingleElement(selection);
		if (single == null)
			return null;
		IJavaElement[] candidates= OpenTypeHierarchyUtil.getCandidates(single);
		if (candidates != null && candidates.length > 0) 
			return candidates[0];
		return null;
	}
}

/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;

import org.eclipse.jdt.internal.ui.dnd.JdtViewerDropAdapter;
import org.eclipse.jdt.internal.ui.dnd.LocalSelectionTransfer;
import org.eclipse.jdt.internal.ui.dnd.TransferDropTargetListener;
import org.eclipse.jdt.internal.ui.util.OpenTypeHierarchyUtil;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;

class TypeHierarchyTransferDropAdapter extends JdtViewerDropAdapter implements TransferDropTargetListener {

	private ISelection fSelection;
	private TypeHierarchyViewPart fTypeHierarchyViewPart;
	
	public TypeHierarchyTransferDropAdapter(TypeHierarchyViewPart viewPart, StructuredViewer viewer) {
		super(viewer,  DND.FEEDBACK_SCROLL | DND.FEEDBACK_EXPAND);
		fTypeHierarchyViewPart= viewPart;
	}

	//---- TransferDropTargetListener interface ---------------------------------------
	
	public Transfer getTransfer() {
		return LocalSelectionTransfer.getInstance();
	}

	//---- Actual DND -----------------------------------------------------------------
	
	public void dragEnter(DropTargetEvent event) {
		clear();
		super.dragEnter(event);
	}
	
	public void dragLeave(DropTargetEvent event) {
		clear();
		super.dragLeave(event);
	}
	
	private void clear() {
		fSelection= null;
	}
	
	public void validateDrop(Object target, DropTargetEvent event, int operation) {
		event.detail= DND.DROP_NONE;	
		
		if (fSelection == null) {
			ISelection s= LocalSelectionTransfer.getInstance().getSelection();
			if (!(s instanceof IStructuredSelection))
				return;
			fSelection= s;	
		}	
		
		if (getInputElement(fSelection) != null) 
			event.detail= DND.DROP_COPY;	
	}

	public void drop(Object target, DropTargetEvent event) {
		try{
			if (event.detail == DND.DROP_NONE)
				return;
	
			IJavaElement input= getInputElement(fSelection);
			fTypeHierarchyViewPart.setInputElement(input);
			if (input instanceof IMember) 
				fTypeHierarchyViewPart.selectMember((IMember) input);
		} finally{
			// The drag source listener must not perform any operation
			// since this drop adapter did the remove of the source even
			// if we moved something.
			event.detail= DND.DROP_NONE;
		}
	}
	
	private static IJavaElement getInputElement(ISelection selection) {
		Object single= SelectionUtil.getSingleElement(selection);
		if (single != null) {
			IJavaElement[] candidates= OpenTypeHierarchyUtil.getCandidates(single);
			if (candidates != null && candidates.length > 0) {
				return candidates[0];
			}
		}
		return null;
	}
	
}
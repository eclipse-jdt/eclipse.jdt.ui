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
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jdt.internal.ui.dnd.TransferDragSourceListener;

/**
 * Drag support class to allow dragging of working set elements
 * into a new working set
 */
class WorkingSetRearangeDragAdapter extends DragSourceAdapter implements TransferDragSourceListener {
	
	private ISelectionProvider fProvider;
	
	WorkingSetRearangeDragAdapter(ISelectionProvider provider) {
		Assert.isNotNull(provider);
		fProvider= provider;
	}

	public Transfer getTransfer() {
		return WorkingSetTransfer.getInstance();
	}
	
	public void dragStart(DragSourceEvent event) {
		ISelection selection= fProvider.getSelection();
		event.doit= isDragable(selection);
		if (event.doit) {
			WorkingSetTransfer.getInstance().setSelection((MultiElementSelection)selection);
		}
	}
	
	private boolean isDragable(ISelection s) {
		if (!(s instanceof MultiElementSelection))
			return false;
		MultiElementSelection selection= (MultiElementSelection)s;
		return selection.size() == 1 && selection.getFirstElement() instanceof IAdaptable;
	}
	
	public void dragSetData(DragSourceEvent event){
		ISelection selection= fProvider.getSelection();
		if (!(selection instanceof MultiElementSelection)) {
			event.data= null;
			return;
		}
		event.data= selection;
	}

	public void dragFinished(DragSourceEvent event) {
		if (!event.doit)
			return;
		
		if (event.detail == DND.DROP_MOVE) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=30543
			// handleDropMove(event);
		}	
		else if (event.detail == DND.DROP_NONE || event.detail == DND.DROP_TARGET_MOVE) {
		}
	}	
}

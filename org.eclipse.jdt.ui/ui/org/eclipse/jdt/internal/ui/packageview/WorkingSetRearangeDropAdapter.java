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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;

import org.eclipse.jface.viewers.AbstractTreeViewer;

import org.eclipse.ui.IWorkingSet;

import org.eclipse.jdt.internal.ui.dnd.JdtViewerDropAdapter;
import org.eclipse.jdt.internal.ui.dnd.TransferDropTargetListener;
import org.eclipse.jdt.internal.ui.workingsets.HistoryWorkingSetUpdater;

class WorkingSetRearangeDropAdapter extends JdtViewerDropAdapter implements TransferDropTargetListener {
	
	WorkingSetRearangeDropAdapter(AbstractTreeViewer viewer) {
		super(viewer, DND.FEEDBACK_SCROLL | DND.FEEDBACK_EXPAND);
	}

	//---- TransferDropTargetListener interface ---------------------------------------
	
	public Transfer getTransfer() {
		return WorkingSetTransfer.getInstance();
	}
	
	public boolean isEnabled(DropTargetEvent event) {
		if (true)
			return false;
		Object target= event.item != null ? event.item.getData() : null;
		if (target == null)
			return false;
		return target instanceof IWorkingSet && !((IWorkingSet)target).getId().equals(HistoryWorkingSetUpdater.ID);
	}

	//---- Actual DND -----------------------------------------------------------------
	
	public void validateDrop(Object target, DropTargetEvent event, int operation) {
		event.detail= operation;
	}

	public void drop(Object target, final DropTargetEvent event) {
		IWorkingSet workingSet= (IWorkingSet)target;
		MultiElementSelection selection= (MultiElementSelection)event.data;
		Object element= selection.getFirstElement();
		if (!(element instanceof IAdaptable)) {
			event.detail= DND.DROP_NONE;
			return;
		}
		List elements= new ArrayList(Arrays.asList(workingSet.getElements()));
		elements.add(element);
		workingSet.setElements((IAdaptable[])elements.toArray(new IAdaptable[elements.size()]));
	}	
}

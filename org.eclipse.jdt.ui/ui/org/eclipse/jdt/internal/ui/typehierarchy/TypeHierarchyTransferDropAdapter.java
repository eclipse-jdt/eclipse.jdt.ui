/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.ui.actions.AddMethodStubAction;
import org.eclipse.jdt.internal.ui.dnd.JdtViewerDropAdapter;
import org.eclipse.jdt.internal.ui.dnd.LocalSelectionTransfer;
import org.eclipse.jdt.internal.ui.dnd.TransferDropTargetListener;

public class TypeHierarchyTransferDropAdapter extends JdtViewerDropAdapter implements TransferDropTargetListener {

	private AddMethodStubAction fAddMethodStubAction;

	public TypeHierarchyTransferDropAdapter(AbstractTreeViewer viewer) {
		super(viewer, DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL);
		
		fAddMethodStubAction= new AddMethodStubAction();
	}

	//---- TransferDropTargetListener interface ---------------------------------------
	
	public Transfer getTransfer() {
		return LocalSelectionTransfer.getInstance();
	}

	//---- Actual DND -----------------------------------------------------------------
		
	public void validateDrop(Object target, DropTargetEvent event, int operation) {
		event.detail= DND.DROP_NONE;
		
		ISelection selection= LocalSelectionTransfer.getInstance().getSelection();
		
		if (target instanceof IType) {
			if (AddMethodStubAction.canActionBeAdded((IType)target, selection)) {
				if (operation == DND.DROP_NONE) {
					operation= DND.DROP_COPY; // use copy as default operation
				}
				event.detail= operation;
			}
		}
		return;	
	}	

	public void drop(Object target, DropTargetEvent event) {
		ISelection selection= LocalSelectionTransfer.getInstance().getSelection();
		
		if (target instanceof IType) {
			if (fAddMethodStubAction.init((IType)target, selection)) {
				fAddMethodStubAction.run();
			}
		}
		return;
	}
}

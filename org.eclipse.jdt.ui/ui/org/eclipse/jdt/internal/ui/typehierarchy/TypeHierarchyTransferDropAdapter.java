/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.ui.actions.AddMethodStubAction;
import org.eclipse.jdt.internal.ui.dnd.JdtViewerDropAdapter;
import org.eclipse.jdt.internal.ui.dnd.LocalSelectionTransfer;
import org.eclipse.jdt.internal.ui.dnd.TransferDropTargetListener;
import org.eclipse.jdt.internal.ui.util.OpenTypeHierarchyUtil;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;

public class TypeHierarchyTransferDropAdapter extends JdtViewerDropAdapter implements TransferDropTargetListener {
	private AddMethodStubAction fAddMethodStubAction;
	private TypeHierarchyViewPart fTypeHierarchyViewPart;
	public TypeHierarchyTransferDropAdapter(TypeHierarchyViewPart viewPart, AbstractTreeViewer viewer) {
		super(viewer, DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL);
		
		fAddMethodStubAction= new AddMethodStubAction();
		fTypeHierarchyViewPart= viewPart;
	}
	//---- TransferDropTargetListener interface ---------------------------------------
	
	public Transfer getTransfer() {
		return LocalSelectionTransfer.getInstance();
	}
	//---- Actual DND -----------------------------------------------------------------
		
	public void validateDrop(Object target, DropTargetEvent event, int operation) {
		event.detail= DND.DROP_NONE;
		
		ISelection selection= LocalSelectionTransfer.getInstance().getSelection();
		if (target == null) {
			if (getInputElement(selection) != null) {
				event.detail= DND.DROP_COPY;
			}
		} else if (target instanceof IType) {
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
		
		if (target == null) {
			IJavaElement input= getInputElement(selection);
			fTypeHierarchyViewPart.setInputElement(input);
			if (input instanceof IMember) {
				fTypeHierarchyViewPart.selectMember((IMember) input);
			}
		} else if (target instanceof IType) {
			if (fAddMethodStubAction.init((IType)target, selection)) {
				fAddMethodStubAction.run();
			}
		}
		return;
	}
	
	private IJavaElement getInputElement(ISelection selection) {
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

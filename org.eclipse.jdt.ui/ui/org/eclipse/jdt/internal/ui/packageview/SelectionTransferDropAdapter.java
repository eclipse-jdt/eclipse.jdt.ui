package org.eclipse.jdt.internal.ui.packageview;
/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.dnd.JdtTreeViewerDropAdapter;
import org.eclipse.jdt.internal.ui.dnd.LocalSelectionTransfer;
import org.eclipse.jdt.internal.ui.dnd.TransferDropTargetListener;
import org.eclipse.jdt.internal.ui.reorg.CopyAction;
import org.eclipse.jdt.internal.ui.reorg.ICopySupport;
import org.eclipse.jdt.internal.ui.reorg.IMoveSupport;
import org.eclipse.jdt.internal.ui.reorg.MoveAction;
import org.eclipse.jdt.internal.ui.reorg.ReorgSupportFactory;
import org.eclipse.jdt.internal.ui.util.JdtHackFinder;

public class SelectionTransferDropAdapter extends JdtTreeViewerDropAdapter implements TransferDropTargetListener {

	private List fElements;
	private IMoveSupport fMoveSupport;
	private int fCanMoveElements;
	private ICopySupport fCopySupport;
	private int fCanCopyElements;

	public SelectionTransferDropAdapter(AbstractTreeViewer viewer) {
		super(viewer, SWT.NONE);
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
		fElements= null;
		fMoveSupport= null;
		fCanMoveElements= 0;
		fCopySupport= null;
		fCanCopyElements= 0;
	}
	
	public void validateDrop(Object target, DropTargetEvent event, int operation) {
		event.detail= DND.DROP_NONE;
		if (fElements == null) {
			ISelection s= LocalSelectionTransfer.getInstance().getSelection();
			if (!(s instanceof IStructuredSelection))
				return;
			fElements= ((IStructuredSelection)s).toList();
		}	
		
		boolean success= false;
		if (operation == DND.DROP_COPY) {
			success= handleValidateCopy(target, event);
		} else if (operation == DND.DROP_MOVE) {
			success= handleValidateMove(target, event);
		}
		if (success)
			event.detail= operation;
			
		return;	
	}	

	public void drop(Object target, DropTargetEvent event) {
		if (event.detail == DND.DROP_MOVE) {
			handleDropMove(target, event);
		} else if (event.detail == DND.DROP_COPY) {
			handleDropCopy(target, event);
		}
		
		// The drag source listener must not perform any operation
		// since this drop adapter did the remove of the source even
		// if we moved something.
		event.detail= DND.DROP_NONE;
		return;
	}
	
	private boolean handleValidateMove(Object target, DropTargetEvent event) {
		if (fMoveSupport == null)
			fMoveSupport= ReorgSupportFactory.createMoveSupport(fElements);
		
		if (!canMoveElements())
			return false;	

		return fMoveSupport.canMove(fElements, target);
	}
	
	private boolean canMoveElements() {
		if (fCanMoveElements == 0) {
			fCanMoveElements= 2;
			Iterator iter= fElements.iterator();
			while (iter.hasNext()) {
				if (!fMoveSupport.isMovable(iter.next())) {
					fCanMoveElements= 1;
					break;
				}
			}
		}
		return fCanMoveElements == 2;
	}
	
	private void handleDropMove(final Object target, DropTargetEvent event) {
		MoveAction action= new MoveAction(getViewer(), "#MOVE") {
			protected Object selectDestination(IJavaElement root, List elements, Shell shell) {
				return target;
			}
			
		};
		action.run();
		// Remove the action from the selection provider.
		org.eclipse.jdt.internal.ui.util.JdtHackFinder.fixme("Workaround for 1GAUFEK: ITPUI:WIN2000 - Leaking context menus");
		action.getSelectionProvider().removeSelectionChangedListener(action);
	}
	
	private boolean handleValidateCopy(Object target, DropTargetEvent event) {
		if (fCopySupport == null)
			fCopySupport= ReorgSupportFactory.createCopySupport(fElements);
		
		if (!canCopyElements())
			return false;	

		return fCopySupport.canCopy(fElements, target);
	}
			
	private boolean canCopyElements() {
		if (fCanCopyElements == 0) {
			fCanCopyElements= 2;
			Iterator iter= fElements.iterator();
			while (iter.hasNext()) {
				if (!fCopySupport.isCopyable(iter.next())) {
					fCanCopyElements= 1;
					break;
				}
			}
		}
		return fCanCopyElements == 2;
	}		
	
	private void handleDropCopy(final Object target, DropTargetEvent event) {
		CopyAction action= new CopyAction(getViewer(), "#COPY") {
			protected Object selectDestination(IJavaElement root, List elements, Shell shell) {
				return target;
			}
			
		};
		action.run();
		// Remove the action from the selection provider.
		org.eclipse.jdt.internal.ui.util.JdtHackFinder.fixme("Workaround for 1GAUFEK: ITPUI:WIN2000 - Leaking context menus");
		action.getSelectionProvider().removeSelectionChangedListener(action);
	}
}
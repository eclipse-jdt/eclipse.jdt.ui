/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dnd;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
public class BasicSelectionTransferDragAdapter extends DragSourceAdapter implements TransferDragSourceListener {
	
	private ISelectionProvider fProvider;
	
	public BasicSelectionTransferDragAdapter(ISelectionProvider provider) {
		fProvider= provider;
		Assert.isNotNull(fProvider);
	}

	/**
	 * @see TransferDragSourceListener#getTransfer
	 */
	public Transfer getTransfer() {
		return LocalSelectionTransfer.getInstance();
	}
	
	/**
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragStart
	 */
	public void dragStart(DragSourceEvent event) {
		ISelection selection= fProvider.getSelection();
		LocalSelectionTransfer.getInstance().setSelection(selection);
		event.doit= isDragable(selection);
	}
	
	/**
	 * Overwrite to validate selection
	 */
	protected boolean isDragable(ISelection selection) {
		return true;
	}

	/**
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragSetData
	 */		
	public void dragSetData(DragSourceEvent event) {
		// For consistency set the data to the selection even though
		// the selection is provided by the LocalSelectionTransfer
		// to the drop target adapter.
		event.data= LocalSelectionTransfer.getInstance().getSelection();
	}

	/**
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragFinished
	 */	
	public void dragFinished(DragSourceEvent event) {
		// We assume that the drop target listener has done all
		// the work.
		Assert.isTrue(event.detail == DND.DROP_NONE);
		LocalSelectionTransfer.getInstance().setSelection(null);
	}	
}
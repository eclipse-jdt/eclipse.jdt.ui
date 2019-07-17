/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.dnd;

import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.util.TransferDragSourceListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;


public class BasicSelectionTransferDragAdapter extends DragSourceAdapter implements TransferDragSourceListener {

	private ISelectionProvider fProvider;

	public BasicSelectionTransferDragAdapter(ISelectionProvider provider) {
		Assert.isNotNull(provider);
		fProvider= provider;
	}

	/**
	 * @see TransferDragSourceListener#getTransfer
	 */
	@Override
	public Transfer getTransfer() {
		return LocalSelectionTransfer.getTransfer();
	}

	/* non Java-doc
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragStart
	 */
	@Override
	public void dragStart(DragSourceEvent event) {
		ISelection selection= fProvider.getSelection();
		LocalSelectionTransfer.getTransfer().setSelection(selection);
		LocalSelectionTransfer.getTransfer().setSelectionSetTime(event.time & 0xFFFFFFFFL);
		event.doit= isDragable(selection);
	}

	/**
	 * Checks if the elements contained in the given selection can
	 * be dragged.
	 * <p>
	 * Subclasses may override.
	 *
	 * @param selection containing the elements to be dragged
	 */
	protected boolean isDragable(ISelection selection) {
		return true;
	}


	/* non Java-doc
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragSetData
	 */
	@Override
	public void dragSetData(DragSourceEvent event) {
		// For consistency set the data to the selection even though
		// the selection is provided by the LocalSelectionTransfer
		// to the drop target adapter.
		event.data= LocalSelectionTransfer.getTransfer().getSelection();
	}


	/* non Java-doc
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragFinished
	 */
	@Override
	public void dragFinished(DragSourceEvent event) {
		LocalSelectionTransfer.getTransfer().setSelection(null);
		LocalSelectionTransfer.getTransfer().setSelectionSetTime(0);
	}
}

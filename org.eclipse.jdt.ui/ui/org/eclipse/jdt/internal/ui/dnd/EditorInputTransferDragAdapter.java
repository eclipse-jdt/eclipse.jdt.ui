/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.dnd;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.runtime.Assert;

import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;

import org.eclipse.jface.util.TransferDragSourceListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorInputTransfer;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

public class EditorInputTransferDragAdapter extends DragSourceAdapter implements TransferDragSourceListener {

	private ISelectionProvider fProvider;

	public EditorInputTransferDragAdapter(ISelectionProvider provider) {
		Assert.isNotNull(provider);
		fProvider= provider;
	}

	/*
	 * @see TransferDragSourceListener#getTransfer
	 */
	public Transfer getTransfer() {
		return EditorInputTransfer.getInstance();
	}

	/*
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragStart
	 */
	public void dragStart(DragSourceEvent event) {
		ISelection selection= fProvider.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection= (IStructuredSelection) selection;
			if (structuredSelection.size() > 0) {
				event.doit= true;
				return;
			}
		}
		event.doit= false;
	}

	/*
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragSetData
	 */
	public void dragSetData(DragSourceEvent event) {
		if (EditorInputTransfer.getInstance().isSupportedType(event.dataType)) {
			ISelection selection= fProvider.getSelection();
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection structuredSelection= (IStructuredSelection) selection;
				ArrayList transferData= new ArrayList();
				for (Iterator iter= structuredSelection.iterator(); iter.hasNext();) {
					Object element= iter.next();
					IEditorInput editorInput= EditorUtility.getEditorInput(element);
					if (editorInput != null) {
						try {
							String editorId= EditorUtility.getEditorID(editorInput);
							transferData.add(EditorInputTransfer.createEditorInputData(editorId, editorInput));
						} catch (PartInitException e) {
							JavaPlugin.log(e);
						}
					}
				}
				event.data= transferData.toArray(new EditorInputTransfer.EditorInputData[transferData.size()]);
			}
		}
	}


	/*
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragFinished
	 */
	public void dragFinished(DragSourceEvent event) {
		Assert.isTrue(event.detail != DND.DROP_MOVE);
	}
}

package org.eclipse.jdt.internal.ui.packageview;
/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

import java.util.Iterator;

import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.ui.dnd.LocalSelectionTransfer;
import org.eclipse.jdt.internal.ui.dnd.TransferDragSourceListener;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;

public class SelectionTransferDragAdapter extends DragSourceAdapter implements TransferDragSourceListener {
	
	private ISelectionProvider fProvider;
	
	public SelectionTransferDragAdapter(ISelectionProvider provider) {
		fProvider= provider;
		Assert.isNotNull(fProvider);
	}

	public Transfer getTransfer() {
		return LocalSelectionTransfer.getInstance();
	}
	
	public void dragStart(DragSourceEvent event) {
		LocalSelectionTransfer.getInstance().setSelection(fProvider.getSelection());
		event.doit= isDragable((StructuredSelection)event.data);
	}
	
	private boolean isDragable(StructuredSelection selection) {
		for (Iterator iter= selection.iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (element instanceof IJavaElement) {
				IPackageFragmentRoot root= (IPackageFragmentRoot)JavaModelUtility.findElementOfKind((IJavaElement)element, 
					IJavaElement.PACKAGE_FRAGMENT_ROOT);
				if (root != null && root.isArchive())
					return false;
			}
		}
		return true;
	}
		
	public void dragSetData(DragSourceEvent event) {
		// For consistency set the data to the selection even though
		// the selection is provided by the LocalSelectionTransfer
		// to the drop target adapter.
		event.data= LocalSelectionTransfer.getInstance().getSelection();
	}
	
	public void dragFinished(DragSourceEvent event) {
		// We assume that the drop target listener has done all
		// the work.
		Assert.isTrue(event.detail == DND.DROP_NONE);
		LocalSelectionTransfer.getInstance().setSelection(null);
	}	
}
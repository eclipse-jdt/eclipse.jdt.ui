/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;

import java.util.Iterator;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.ui.dnd.BasicSelectionTransferDragAdapter;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;

public class SelectionTransferDragAdapter extends BasicSelectionTransferDragAdapter {
		
	public SelectionTransferDragAdapter(ISelectionProvider provider) {
		super(provider);
	}

	protected boolean isDragable(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			for (Iterator iter= ((IStructuredSelection)selection).iterator(); iter.hasNext();) {
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
		return false;
	}
}
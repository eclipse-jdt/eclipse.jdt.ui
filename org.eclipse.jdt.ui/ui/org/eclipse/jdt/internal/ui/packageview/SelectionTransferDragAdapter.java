/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.packageview;

import java.util.Iterator;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.ui.dnd.BasicSelectionTransferDragAdapter;

public class SelectionTransferDragAdapter extends BasicSelectionTransferDragAdapter {
		
	public SelectionTransferDragAdapter(ISelectionProvider provider) {
		super(provider);
	}

	protected boolean isDragable(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			for (Iterator iter= ((IStructuredSelection)selection).iterator(); iter.hasNext();) {
				Object element= iter.next();
				if (element instanceof IJavaElement) {
					IPackageFragmentRoot root= (IPackageFragmentRoot)((IJavaElement)element).getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
					if (root != null && root.isArchive() && root.isExternal())
						return false;
				}
			}
			return true;
		}
		return false;
	}
}

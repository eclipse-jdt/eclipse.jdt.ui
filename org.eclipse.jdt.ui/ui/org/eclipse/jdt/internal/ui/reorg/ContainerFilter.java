/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
package org.eclipse.jdt.internal.ui.reorg;

import java.util.List;

import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.internal.ui.packageview.PackageFilter;

public class ContainerFilter extends PackageFilter {
	private List fElements;
	
	ContainerFilter(List elements) {
		fElements= elements;
	}

	public boolean select(Viewer viewer, Object parent, Object o) {
		ICopySupport support= ReorgSupportFactory.createCopySupport(fElements);
		for (int i= 0; i < fElements.size(); i++) {
			if (o.equals(fElements.get(i)))
				return false;
		}
		return support.canBeAncestor(o);
	}

}
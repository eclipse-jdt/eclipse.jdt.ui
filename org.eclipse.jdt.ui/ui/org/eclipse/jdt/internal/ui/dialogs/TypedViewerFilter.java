package org.eclipse.jdt.internal.ui.dialogs;
/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

import java.util.List;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class TypedViewerFilter extends ViewerFilter {

	private Class[] fAcceptedTypes;
	private List fRejectedElements;

	public TypedViewerFilter(Class[] acceptedTypes) {
		this(acceptedTypes, null);
	}
	
	public TypedViewerFilter(Class[] acceptedTypes, List rejectedElements) {
		fAcceptedTypes= acceptedTypes;
		fRejectedElements= rejectedElements;
	}	

	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (fRejectedElements != null) {
			for (int i= fRejectedElements.size()-1; i >= 0; i--) {
				if (element.equals(fRejectedElements.get(i))) {
					return false;
				}
			}
		}
		for (int i= 0; i < fAcceptedTypes.length; i++) {
			if (fAcceptedTypes[i].isInstance(element)) {
				return true;
			}
		}
		return false;
	}

	public boolean isFilterProperty(Object element, Object property) {
		return false;
	}
}
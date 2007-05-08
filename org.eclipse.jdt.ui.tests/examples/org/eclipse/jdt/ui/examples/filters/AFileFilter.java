package org.eclipse.jdt.ui.examples.filters;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.jdt.core.ICompilationUnit;

public class AFileFilter extends ViewerFilter {

	public AFileFilter() {
		// TODO Auto-generated constructor stub
	}

	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof ICompilationUnit) {
			return !((ICompilationUnit) element).getElementName().equals("A.java");
		}
		return true;
	}

}

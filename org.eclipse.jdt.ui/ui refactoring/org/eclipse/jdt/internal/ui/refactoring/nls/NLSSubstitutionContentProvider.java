/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.nls;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.nls.NLSHolder;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSLine;

class NLSSubstitutionContentProvider implements IStructuredContentProvider {

 	private NLSHolder fHolder;
 	
	/*
	 * @see IStructuredContentProvider#getElements(Object)
	 */
	public Object[] getElements(Object inputElement) {
		return fHolder.getSubstitutions();
	}
	
	public NLSLine[] getLines(Object inputElement) {
		return fHolder.getLines();
	}
	
	/*
	 * @see IContentProvider#dispose()
	 */
	public void dispose() {
		fHolder= null;
	}

	/*
	 * @see IContentProvider#inputChanged(Viewer, Object, Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput == null)
			return;
		if (fHolder != null && fHolder.getCu().equals(newInput))
			return;
		fHolder= NLSHolder.create((ICompilationUnit)newInput);	
	}
}
/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.jface.viewers.IElementComparer;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;

/* package */ class PackageExplorerElementComparer implements IElementComparer {

	public boolean equals(Object o1, Object o2) {
		if (o1 == o2)	// this handles also the case that both are null
			return true;
		if (o1 == null)  
			return false; // o2 != null if we reach this point 
		if (o1.equals(o2))
			return true;
		IJavaElement j1= (o1 instanceof IJavaElement) ? (IJavaElement)o1 : null;
		IJavaElement j2= (o2 instanceof IJavaElement) ? (IJavaElement)o2 : null;
		if (j1 == null || j2 == null)
			return false;
		ICompilationUnit c1= (ICompilationUnit)j1.getAncestor(IJavaElement.COMPILATION_UNIT);
		ICompilationUnit c2= (ICompilationUnit)j2.getAncestor(IJavaElement.COMPILATION_UNIT);
		if (c1 == null || c2 == null)
			return false;
		if (c1.isWorkingCopy() && c2.isWorkingCopy() || !c1.isWorkingCopy() && !c2.isWorkingCopy())
			return false;
		// From here on either c1 or c2 is a working copy.
		if (c1.isWorkingCopy()) {
			j1= c1.getOriginal(j1);
		} else if (c2.isWorkingCopy()) {
			j2= c2.getOriginal(j2); 
		}
		if (j1 == null || j2 == null)
			return false;
		return j1.equals(j2);
	}

	public int hashCode(Object o1) {
		IJavaElement j1= (o1 instanceof IJavaElement) ? (IJavaElement)o1 : null;
		if (j1 == null)
			return o1.hashCode();
		ICompilationUnit c1= (ICompilationUnit)j1.getAncestor(IJavaElement.COMPILATION_UNIT);
		if (c1 == null || !c1.isWorkingCopy())
			return o1.hashCode();
		// From here on c1 is a working copy.
		j1= c1.getOriginal(j1);
		if (j1 == null)
			return o1.hashCode();
		return j1.hashCode();
	}
}

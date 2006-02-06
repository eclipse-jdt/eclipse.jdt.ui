/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;


/**
 *  Viewer sorter which sorts the Java elements like
 *  they appear in the source.
 * 
 * @since 3.2
 */
public class SourcePositionSorter extends ViewerSorter {

	/*
	 * @see org.eclipse.jface.viewers.ViewerSorter#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public int compare(Viewer viewer, Object e1, Object e2) {
		if (!(e1 instanceof ISourceReference))
			return 0;
		if (!(e2 instanceof ISourceReference))
			return 0;
		
		if (((IJavaElement)e1).getParent() != ((IJavaElement)e2).getParent())
			return 0;
		
		try {
			ISourceRange sr1= ((ISourceReference)e1).getSourceRange();
			ISourceRange sr2= ((ISourceReference)e2).getSourceRange();
			if (sr1 == null || sr2 == null)
				return 0;
			
			return sr1.getOffset() - sr2.getOffset();
			
		} catch (JavaModelException e) {
			return 0;
		}
	}
}

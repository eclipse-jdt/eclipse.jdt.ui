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
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.viewers.Viewer;

public class ParentSorter extends NameSorter {
	public int compare(Viewer viewer, Object e1, Object e2) {
		String leftParent= getParentName(e1);
		String rightParent= getParentName(e2);
		int result= collator.compare(leftParent, rightParent);
		if (result == 0)
			return super.compare(viewer, e1, e2);
		else 
			return result;
	}
	
	private String getParentName(Object element) {
		if (element instanceof IJavaElement) {
			IJavaElement parent= ((IJavaElement)element).getParent();
			if (parent instanceof IType)
				return ((IType)parent).getFullyQualifiedName();
			if (parent != null)
				return parent.getElementName();
		}
		if (element instanceof IResource) {
			IResource parent= ((IResource)element).getParent();
			if (parent != null)
				return parent.getName();
		}
		return ""; //$NON-NLS-1$
	}
}

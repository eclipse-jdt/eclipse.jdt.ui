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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.viewers.Viewer;

public class PathSorter extends NameSorter {
	public int compare(Viewer viewer, Object e1, Object e2) {
		IPath path1= getPath(e1);
		IPath path2=getPath(e2);
		int result= compare(path1, path2);
		if (result != 0)
			return result;
		else
			return super.compare(viewer, e1, e2);
	}
	
	/**
	 * @param e1
	 * @return
	 */
	private IPath getPath(Object element) {
		if (element instanceof IJavaElement)
			return ((IJavaElement)element).getPath();
		if (element instanceof IResource)
			return ((IResource)element).getFullPath();
		return new Path(""); //$NON-NLS-1$
	}

	protected int compare(IPath path1, IPath path2) {
		int segmentCount= Math.min(path1.segmentCount(), path2.segmentCount());
		for (int i= 0; i < segmentCount; i++) {
			int value= collator.compare(path1.segment(i), path2.segment(i));
			if (value != 0)
				return value;
		}
		return path1.segmentCount() - path2.segmentCount();
	}
}

/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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

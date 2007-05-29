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

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IProject;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.jdt.core.IJavaProject;

public class PureProjectFilter extends ViewerFilter {

	public PureProjectFilter() {
		// TODO Auto-generated constructor stub
	}

	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof IJavaProject) {
			element= ((IJavaProject) element).getResource();
		}
		if (element instanceof IProject) {
			IProject proj= (IProject) element;
			if (!proj.isOpen())
				return false;
			try {
				String[] natureIds= proj.getDescription().getNatureIds();
				return natureIds.length == 1;
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return true;
	}

}

/*
 * Copyright (c) 2000, 2003 IBM Corp. and others..
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package org.eclipse.jdt.internal.ui.filters;


import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.team.core.RepositoryProvider;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;

/**
 * Filters non-shared resources and Java elements
 * i.e. those controlled by a team provider.
 * 
 * @since 2.1
 */
public class NonSharedFilter extends ViewerFilter {

	/*
	 * @see ViewerFilter
	 */
	public boolean select(Viewer viewer, Object parent, Object element) {
		if (element instanceof IResource)
			return isSharedProject(((IResource)element).getProject());
		
		IJavaProject jp= null;
		if (element instanceof IJavaElement) {
			jp= ((IJavaElement)element).getJavaProject();
	
		} else if (element instanceof ClassPathContainer) {
			ClassPathContainer container= (ClassPathContainer) element;
			jp= container.getJavaProject();
		}
		if (jp == null)
			return false;
		return isSharedProject(jp.getProject());
	}
	
	private boolean isSharedProject(IProject project) {
		return RepositoryProvider.getProvider(project) != null;
	}
}
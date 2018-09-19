/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.filters;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;


/**
 * Filters out all non-Java elements.
 */
public class NonJavaElementFilter extends ViewerFilter {

	@Override
	public boolean select(Viewer viewer, Object parent, Object element) {
		if (element instanceof IJavaElement)
			return true;

		if (element instanceof IProject) {
			IProject project= (IProject) element;
			try {
				return project.hasNature(JavaCore.NATURE_ID);
			} catch (CoreException e) {
				return true;
			}
		}

		if (element instanceof IResource) {
			IProject project= ((IResource)element).getProject();
			return project == null || !project.isOpen();
		}

		// Exclude all IStorage elements which are neither Java elements nor resources
		if (element instanceof IStorage)
			return false;

		return true;
	}
}

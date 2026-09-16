/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;


/**
 * The LibraryFilter is a filter used to determine whether
 * a Java internal library is shown
 */
public class ContainedLibraryFilter extends ViewerFilter {

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof IPackageFragmentRoot root) {
			// only filter out libraries contained in the project itself
			if (!root.isExternal()) {
				IResource resource= root.getResource();
				if (resource != null) {
					IProject project= resource.getProject();
					IProject container= root.getJavaProject().getProject();
					if (container.equals(project)) {
						try {
							IClasspathEntry entry = root.getResolvedClasspathEntry();
							if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
								return true;
							}
						} catch (JavaModelException e) {
							return true;
						}
						return false;
					}
				}
			}
		}
		return true;
	}
}

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
package org.eclipse.jdt.internal.ui.refactoring.nls;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.internal.ui.wizards.TypedViewerFilter;

/**
 * A TypedViewerFilter that accepts only PackageFragments and JavaProjects.
 * PackageFragments are only accepted if they are of the kind K_SOURCE.
 */
public class JavaTypedViewerFilter extends TypedViewerFilter {

	public JavaTypedViewerFilter() {
		super(new Class[]{IPackageFragmentRoot.class, IJavaProject.class});
	}

	public boolean select(Viewer viewer, Object parent, Object element) {
		if (element instanceof IPackageFragmentRoot) {
			IPackageFragmentRoot fragmentRoot= (IPackageFragmentRoot)element;
			try {
				return (fragmentRoot.getKind() == IPackageFragmentRoot.K_SOURCE);
			} catch (JavaModelException e) {
				return false;
			}
		}
		return super.select(viewer, parent, element);
	}
}
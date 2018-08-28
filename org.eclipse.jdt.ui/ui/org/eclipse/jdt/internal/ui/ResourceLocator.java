/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

/**
 * This class locates different resources
 * which are related to an object
 */
public class ResourceLocator implements IResourceLocator {

	@Override
	public IResource getUnderlyingResource(Object element) throws JavaModelException {
		if (element instanceof IJavaElement)
			return ((IJavaElement) element).getUnderlyingResource();
		else
			return null;
	}

	@Override
	public IResource getCorrespondingResource(Object element) throws JavaModelException {
		if (element instanceof IJavaElement)
			return ((IJavaElement) element).getCorrespondingResource();
		else
			return null;
	}

	@Override
	public IResource getContainingResource(Object element) throws JavaModelException {
		IResource resource= null;
		if (element instanceof IResource)
			resource= (IResource) element;
		if (element instanceof IJavaElement) {
			resource= ((IJavaElement) element).getResource();
			if (resource == null)
				resource= ((IJavaElement) element).getJavaProject().getProject();
		}
		return resource;
	}
}

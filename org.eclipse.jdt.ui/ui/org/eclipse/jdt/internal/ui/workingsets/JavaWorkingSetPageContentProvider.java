/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.workingsets;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

class JavaWorkingSetPageContentProvider extends StandardJavaElementContentProvider {
	
	public boolean hasChildren(Object element) {

		if (element instanceof IProject && !((IProject)element).isAccessible())
			return false;

		if (element instanceof IPackageFragment) {
			IPackageFragment pkg= (IPackageFragment)element;
			try {
				if (pkg.getKind() == IPackageFragmentRoot.K_BINARY)
					return pkg.getChildren().length > 0;
			} catch (JavaModelException ex) {
				// use super behavior
			}
		}
		return super.hasChildren(element);
	}

	public Object[] getChildren(Object parentElement) {
		try {
			if (parentElement instanceof IJavaModel) 
				return concatenate(super.getChildren(parentElement), getClosedProjects((IJavaModel)parentElement));
			else
				return super.getChildren(parentElement);
		} catch (CoreException e) {
			return NO_CHILDREN;
		}
	}

	private IProject[] getClosedProjects(IJavaModel model) throws JavaModelException {
		Object[] resources= model.getNonJavaResources();
		ArrayList closedProjects= new ArrayList(resources.length);

		for (int i= 0; i < resources.length; i++) {
			Object resource= resources[i];
			if (resource instanceof IProject) {
				if (!((IProject)resource).isOpen())
					closedProjects.add(resource);
			}
		}
		
		return (IProject[])closedProjects.toArray(new IProject[closedProjects.size()]);
	}
}

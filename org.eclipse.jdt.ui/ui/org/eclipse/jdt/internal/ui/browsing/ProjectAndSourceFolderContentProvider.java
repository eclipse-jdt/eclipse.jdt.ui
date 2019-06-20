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
package org.eclipse.jdt.internal.ui.browsing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

class ProjectAndSourceFolderContentProvider extends JavaBrowsingContentProvider {

	ProjectAndSourceFolderContentProvider(JavaBrowsingPart browsingPart) {
		super(false, browsingPart);
	}

	@Override
	public Object[] getChildren(Object element) {
		if (!exists(element))
			return NO_CHILDREN;

		try {
			startReadInDisplayThread();
			if (element instanceof IStructuredSelection) {
				Assert.isLegal(false);
				Object[] result= new Object[0];
				Class<?> clazz= null;
				Iterator<?> iter= ((IStructuredSelection)element).iterator();
				while (iter.hasNext()) {
					Object item=  iter.next();
					if (clazz == null)
						clazz= item.getClass();
					if (clazz == item.getClass())
						result= concatenate(result, getChildren(item));
					else
						return NO_CHILDREN;
				}
				return result;
			}
			if (element instanceof IStructuredSelection) {
				Assert.isLegal(false);
				Object[] result= new Object[0];
				Iterator<?> iter= ((IStructuredSelection)element).iterator();
				while (iter.hasNext())
					result= concatenate(result, getChildren(iter.next()));
				return result;
			}
			if (element instanceof IJavaProject)
				return getPackageFragmentRoots((IJavaProject)element);
			if (element instanceof IPackageFragmentRoot)
				return NO_CHILDREN;

			return super.getChildren(element);

		} catch (JavaModelException e) {
			return NO_CHILDREN;
		} finally {
			finishedReadInDisplayThread();
		}
	}

	@Override
	protected Object[] getPackageFragmentRoots(IJavaProject project) throws JavaModelException {
		if (!project.getProject().isOpen())
			return NO_CHILDREN;

		IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
		List<IPackageFragmentRoot> list= new ArrayList<>(roots.length);
		// filter out package fragments that correspond to projects and
		// replace them with the package fragments directly
		for (IPackageFragmentRoot root : roots) {
			if (!isProjectPackageFragmentRoot(root))
				list.add(root);
		}
		return list.toArray();
	}

	/*
	 *
	 * @see ITreeContentProvider
	 */
	@Override
	public boolean hasChildren(Object element) {
		return element instanceof IJavaProject && super.hasChildren(element);
	}
}

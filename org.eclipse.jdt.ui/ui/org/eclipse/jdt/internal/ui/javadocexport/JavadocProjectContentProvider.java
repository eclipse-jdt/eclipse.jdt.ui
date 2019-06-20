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
package org.eclipse.jdt.internal.ui.javadocexport;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class JavadocProjectContentProvider implements ITreeContentProvider {

	/*
	 * @see ITreeContentProvider#getChildren(Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		try {
			if (parentElement instanceof IJavaProject) {
				IJavaProject project= (IJavaProject) parentElement;
				return getPackageFragmentRoots(project);
			} else if (parentElement instanceof IPackageFragmentRoot) {
				return getPackageFragments((IPackageFragmentRoot) parentElement);
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return new Object[0];
	}
	/*
	 * @see IStructuredContentProvider#getElements(Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		try {
			return JavaCore.create(root).getJavaProjects();
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return new Object[0];
	}

	/*
	 * @see ITreeContentProvider#getParent(Object)
	 */
	@Override
	public Object getParent(Object element) {

		IJavaElement parent= ((IJavaElement)element).getParent();
		if (parent instanceof IPackageFragmentRoot) {
			IPackageFragmentRoot root= (IPackageFragmentRoot) parent;
			if (root.getPath().equals(root.getJavaProject().getProject().getFullPath())) {
				return root.getJavaProject();
			}
		}
		return parent;
	}

	/*
	 * @see ITreeContentProvider#hasChildren(Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		return (getChildren(element).length > 0);
	}

	/*
	 * @see IContentProvider#dispose()
	 */
	@Override
	public void dispose() {
	}

	/*
	 * @see IContentProvider#inputChanged(Viewer, Object, Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	private Object[] getPackageFragmentRoots(IJavaProject project) throws JavaModelException {
		ArrayList<Object> result= new ArrayList<>();

		for (IPackageFragmentRoot root : project.getPackageFragmentRoots()) {
			if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
				if (root.getPath().equals(root.getJavaProject().getPath())) {
					Object[] packageFragments= getPackageFragments(root);
					result.addAll(Arrays.asList(packageFragments));
				} else {
					result.add(root);
				}
			}
		}
		return result.toArray();
	}

	private Object[] getPackageFragments(IPackageFragmentRoot root) throws JavaModelException {
		ArrayList<IJavaElement> packageFragments= new ArrayList<>();

		for (IJavaElement child : root.getChildren()) {
			if (((IPackageFragment) child).containsJavaResources()) {
				packageFragments.add(child);
			}
		}
		return packageFragments.toArray();
	}
}

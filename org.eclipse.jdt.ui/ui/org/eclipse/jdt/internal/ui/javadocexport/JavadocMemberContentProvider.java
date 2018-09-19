/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class JavadocMemberContentProvider implements ITreeContentProvider {

	/*
	 * @see ITreeContentProvider#getChildren(Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		return new Object[0];
	}

	/*
	 * @see ITreeContentProvider#getParent(Object)
	 */
	@Override
	public Object getParent(Object element) {
		//@test
		//System.out.println(element.getClass().toString());
		if (element instanceof IPackageFragment)
			return ((IPackageFragment) element).getParent();
		return null;
	}

	/*
	 * @see ITreeContentProvider#hasChildren(Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		try {
			if (element instanceof IPackageFragment) {
				IPackageFragment iPackageFragment= (IPackageFragment) element;
				return (iPackageFragment.getChildren().length > 0);
			}
		} catch (JavaModelException e) {
		}
		return false;
	}

	/*
	 * @see IStructuredContentProvider#getElements(Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		try {
			if (inputElement instanceof IPackageFragment) {
				ICompilationUnit[] cu= ((IPackageFragment) inputElement).getCompilationUnits();
				return cu;
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return new Object[0];
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

}

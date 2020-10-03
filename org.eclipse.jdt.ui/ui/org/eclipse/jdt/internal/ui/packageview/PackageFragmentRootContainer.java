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
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.ui.model.IWorkbenchAdapter;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

public abstract class PackageFragmentRootContainer implements IAdaptable {

	private static WorkbenchAdapterImpl fgAdapterInstance= new WorkbenchAdapterImpl();

	private static class WorkbenchAdapterImpl implements IWorkbenchAdapter {

		@Override
		public Object[] getChildren(Object o) {
			if (o instanceof PackageFragmentRootContainer)
				return ((PackageFragmentRootContainer) o).getChildren();
			return new Object[0];
		}

		@Override
		public ImageDescriptor getImageDescriptor(Object o) {
			if (o instanceof PackageFragmentRootContainer)
				return ((PackageFragmentRootContainer) o).getImageDescriptor();
			return null;
		}

		@Override
		public String getLabel(Object o) {
			if (o instanceof PackageFragmentRootContainer)
				return ((PackageFragmentRootContainer) o).getLabel();
			return ""; //$NON-NLS-1$
		}

		@Override
		public Object getParent(Object o) {
			if (o instanceof PackageFragmentRootContainer)
				return ((PackageFragmentRootContainer) o).getJavaProject();
			return null;
		}
	}

	private IJavaProject fProject;

	public PackageFragmentRootContainer(IJavaProject project) {
		Assert.isNotNull(project);
		fProject= project;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IWorkbenchAdapter.class)
			return (T) fgAdapterInstance;
		return null;
	}

	public abstract IAdaptable[] getChildren();

	public abstract IPackageFragmentRoot[] getPackageFragmentRoots();

	public abstract String getLabel();

	public abstract ImageDescriptor getImageDescriptor();

	public IJavaProject getJavaProject() {
		return fProject;
	}
}

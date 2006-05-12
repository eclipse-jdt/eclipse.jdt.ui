/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;

import org.eclipse.jface.viewers.IDecorationContext;

import org.eclipse.team.ui.mapping.SynchronizationStateTester;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.util.JavaElementResourceMapping;

public class HierarchicalDecorationContext implements IDecorationContext {
	
	private HierarchicalSynchronizationStateTester fStateTester;
	public static final HierarchicalDecorationContext CONTEXT= new HierarchicalDecorationContext();
	
	private HierarchicalDecorationContext() {
		fStateTester= new HierarchicalSynchronizationStateTester();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IDecorationContext#getProperties()
	 */
	public String[] getProperties() {
		return new String[] { SynchronizationStateTester.PROP_TESTER };
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IDecorationContext#getProperty(java.lang.String)
	 */
	public Object getProperty(String property) {
		if (property == SynchronizationStateTester.PROP_TESTER) {
			return fStateTester;
		}
		return null;
	}
		
	private static final class HierarchicalSynchronizationStateTester extends SynchronizationStateTester {

		public HierarchicalSynchronizationStateTester() {
		}

		/* (non-Javadoc)
		 * @see org.eclipse.team.ui.mapping.SynchronizationStateTester#getState(java.lang.Object, int, org.eclipse.core.runtime.IProgressMonitor)
		 */
		public int getState(Object element, int stateMask, IProgressMonitor monitor) throws CoreException {
			if (element instanceof JavaElementResourceMapping) {
				JavaElementResourceMapping mapping= (JavaElementResourceMapping) element;
				IJavaElement javaElement= mapping.getJavaElement();
				if (javaElement instanceof IPackageFragment) {
					IPackageFragment packageFragment= (IPackageFragment) javaElement;
					if (!packageFragment.isDefaultPackage()) {
						element= new HierarchicalPackageFragementResourceMapping(packageFragment);
					}
				}
			}
			return super.getState(element, stateMask, monitor);
		}
	}
	
	private static final class HierarchicalPackageFragementResourceMapping extends JavaElementResourceMapping {
		private final IPackageFragment fPack;
		private HierarchicalPackageFragementResourceMapping(IPackageFragment fragment) {
			Assert.isNotNull(fragment);
			fPack= fragment;
		}
		public Object getModelObject() {
			return fPack;
		}
		public IProject[] getProjects() {
			return new IProject[] {fPack.getJavaProject().getProject() };
		}
		public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
			return new ResourceTraversal[] {
				new ResourceTraversal(new IResource[] {fPack.getResource()}, IResource.DEPTH_INFINITE, 0)
			};
		}
	}
}

/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.IWorkbenchAdapter;

import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;

/**
 * Representation of class path containers in Java UI.
 */
public class ClassPathContainer extends PackageFragmentRootContainer {

	private IClasspathEntry fClassPathEntry;
	private IClasspathContainer fContainer;
	private boolean fDecorateTestCodeContainerIcons;

	public static class RequiredProjectWrapper implements IAdaptable, IWorkbenchAdapter {

		private final ClassPathContainer fParent;
		private final IJavaProject fProject;
		private final IClasspathEntry fClasspathEntry;

		public RequiredProjectWrapper(ClassPathContainer parent, IJavaProject project, IClasspathEntry classpathEntry) {
			fParent= parent;
			fProject= project;
			fClasspathEntry= classpathEntry;
		}

		public IJavaProject getProject() {
			return fProject;
		}

		public ClassPathContainer getParentClassPathContainer() {
			return fParent;
		}

		public IClasspathEntry getClasspathEntry() {
			return fClasspathEntry;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T getAdapter(Class<T> adapter) {
			if (adapter == IWorkbenchAdapter.class)
				return (T) this;
			return null;
		}

		@Override
		public Object[] getChildren(Object o) {
			return new Object[0];
		}

		@Override
		public ImageDescriptor getImageDescriptor(Object object) {
			if (fClasspathEntry.isTest())
				return JavaPluginImages.DESC_OBJS_PROJECT_TEST;
			else
				return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(IDE.SharedImages.IMG_OBJ_PROJECT);
		}

		@Override
		public String getLabel(Object o) {
			return fProject.getElementName();
		}

		@Override
		public Object getParent(Object o) {
			return fParent;
		}

		@Override
		public int hashCode() {
			return Objects.hash(fClasspathEntry, fParent, fProject);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RequiredProjectWrapper other= (RequiredProjectWrapper) obj;
			if (!Objects.equals(fClasspathEntry, other.fClasspathEntry)) {
				return false;
			}
			if (!Objects.equals(fParent, other.fParent)) {
				return false;
			}
			if (!Objects.equals(fProject, other.fProject)) {
				return false;
			}
			return true;
		}

	}

	public ClassPathContainer(IJavaProject parent, IClasspathEntry entry) {
		super(parent);
		fClassPathEntry= entry;
		try {
			fContainer= JavaCore.getClasspathContainer(entry.getPath(), parent);
		} catch (JavaModelException e) {
			fContainer= null;
		}
		fDecorateTestCodeContainerIcons= PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.DECORATE_TEST_CODE_CONTAINER_ICONS);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ClassPathContainer) {
			ClassPathContainer other = (ClassPathContainer)obj;
			if (getJavaProject().equals(other.getJavaProject()) &&
				fClassPathEntry.equals(other.fClassPathEntry)) {
				return true;
			}

		}
		return false;
	}

	@Override
	public int hashCode() {
		return getJavaProject().hashCode()*17+fClassPathEntry.hashCode();
	}

	@Override
	public IPackageFragmentRoot[] getPackageFragmentRoots() {
		return getJavaProject().findPackageFragmentRoots(fClassPathEntry);
	}

	@Override
	public IAdaptable[] getChildren() {
		List<IAdaptable> list= new ArrayList<>();
		IPackageFragmentRoot[] roots= getPackageFragmentRoots();
		list.addAll(Arrays.asList(roots));
		if (fContainer != null) {
			IClasspathEntry[] classpathEntries= fContainer.getClasspathEntries();
			if (classpathEntries == null) {
				// invalid implementation of a classpath container
				JavaPlugin.log(new IllegalArgumentException("Invalid classpath container implementation: getClasspathEntries() returns null. " + fContainer.getPath())); //$NON-NLS-1$
			} else {
				IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
				for (IClasspathEntry entry : classpathEntries) {
					if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
						IResource resource= root.findMember(entry.getPath());
						if (resource instanceof IProject)
							list.add(new RequiredProjectWrapper(this, JavaCore.create((IProject) resource), entry));
					}
				}
			}
		}
		return list.toArray(new IAdaptable[list.size()]);
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return fDecorateTestCodeContainerIcons && fClassPathEntry.isTest() ? JavaPluginImages.DESC_OBJS_LIBRARY_TEST : JavaPluginImages.DESC_OBJS_LIBRARY;
	}

	@Override
	public String getLabel() {
		if (fContainer != null)
			return fContainer.getDescription();

		IPath path= fClassPathEntry.getPath();
		String containerId= path.segment(0);
		ClasspathContainerInitializer initializer= JavaCore.getClasspathContainerInitializer(containerId);
		if (initializer != null) {
			String description= initializer.getDescription(path, getJavaProject());
			return Messages.format(PackagesMessages.ClassPathContainer_unbound_label, description);
		}
		return Messages.format(PackagesMessages.ClassPathContainer_unknown_label, BasicElementLabels.getPathLabel(path, false));
	}

	public IClasspathEntry getClasspathEntry() {
		return fClassPathEntry;
	}

	static boolean contains(IJavaProject project, IClasspathEntry entry, IPackageFragmentRoot root) {
		for (IPackageFragmentRoot r : project.findPackageFragmentRoots(entry)) {
			if (r.equals(root)) {
				return true;
			}
		}
		return false;
	}

}

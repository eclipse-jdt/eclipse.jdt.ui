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
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgDestination;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgDestinationValidator;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgDestinationFactory;

import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;


public final class DestinationContentProvider extends StandardJavaElementContentProvider {

	private IReorgDestinationValidator fValidator;

	private String searchString;

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }


	public DestinationContentProvider(IReorgDestinationValidator validator) {
		super(true);
		fValidator= validator;
	}

	@Override
	public boolean hasChildren(Object element) {
		IReorgDestination destination= ReorgDestinationFactory.createDestination(element);
		if (!fValidator.canChildrenBeDestinations(destination))
				return false;

		if (element instanceof IJavaElement){
			IJavaElement javaElement= (IJavaElement) element;
			if (javaElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT_ROOT) {
				IPackageFragmentRoot root= (IPackageFragmentRoot) javaElement;
				if (root.isArchive() || root.isExternal())
					return false;
			}
		}

		return super.hasChildren(element);
	}

	@Override
	public Object[] getChildren(Object element) {
		try {
			if (element instanceof IJavaModel) {
				Object [] javaProjects = getJavaProjects((IJavaModel)element);

				List<Object> filteredJavaProjectList= getFilteredProjectList(javaProjects);

				return concatenate(filteredJavaProjectList.toArray(), getOpenNonJavaProjects((IJavaModel)element));
			} else {
				Object[] children= doGetChildren(element);
				ArrayList<Object> result= new ArrayList<>(children.length);
				for (Object child : children) {
					IReorgDestination destination= ReorgDestinationFactory.createDestination(child);
					if (fValidator.canElementBeDestination(destination) || fValidator.canChildrenBeDestinations(destination)) {

						if(searchString != null && !searchString.isEmpty()) {
							if(matchesSearchCriteria(child.toString())) {
								result.add(child);
							}
						} else {
							result.add(child);
						}
					}
				}
				return result.toArray();
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return new Object[0];
		}
	}


	private List<Object> getFilteredProjectList(Object[] javaProjects) throws JavaModelException {
		boolean packageNameExistInProject= false;
		List<Object> filteredJavaProjectList = new ArrayList<>();
		for(Object javaProject : javaProjects) {
			IJavaProject javaModel = (IJavaProject) javaProject;
			if(searchString != null && !searchString.isEmpty()) {
				boolean matchesCriteria = matchesSearchCriteria(javaModel.getElementName());

				if(matchesCriteria) {
					filteredJavaProjectList.add(javaProject);
				} else {
					for(Object children : javaModel.getChildren()){
						packageNameExistInProject = isPackageNameExists(children);
						if(packageNameExistInProject) {
							filteredJavaProjectList.add(javaProject);
							break;
						}
					}
				}
			} else {
				filteredJavaProjectList = Arrays.asList(javaProjects);
			}
		}
		return filteredJavaProjectList;
	}

	private boolean isPackageNameExists(Object element) {
		Object[] children= doGetChildren(element);
		for (Object child : children) {
			IReorgDestination destination= ReorgDestinationFactory.createDestination(child);
			if (child.getClass().equals(PackageFragment.class) && (fValidator.canElementBeDestination(destination) || fValidator.canChildrenBeDestinations(destination))) {
				PackageFragment packageFrag = (PackageFragment) child;
				if(matchesSearchCriteria(packageFrag.getElementName())) {
					return true;
				}
			}
		}
		return false;
	}
	private boolean matchesSearchCriteria(String text) {
		return text.toLowerCase().contains(searchString.toLowerCase());
	}

	private Object[] doGetChildren(Object parentElement) {
		if (parentElement instanceof IContainer) {
			final IContainer container= (IContainer) parentElement;
			return getResources(container);
		}
		return super.getChildren(parentElement);
	}

	// Copied from supertype
	private Object[] getResources(IContainer container) {
		try {
			IResource[] members= container.members();
			IJavaProject javaProject= JavaCore.create(container.getProject());
			if (javaProject == null || !javaProject.exists())
				return members;
			boolean isFolderOnClasspath = javaProject.isOnClasspath(container);
			List<IResource> nonJavaResources= new ArrayList<>();
			Set<IPath> classRootPaths= new HashSet<>();
			for (IPackageFragmentRoot classpathRoot : javaProject.getAllPackageFragmentRoots()) {
				IPath classRootPath= classpathRoot.getPath();
				if (classRootPath != null) {
					classRootPaths.add(classRootPath);
				}
			}
			// Can be on classpath but as a member of non-java resource folder
			for (IResource member : members) {
				// A resource can also be a java element
				// in the case of exclusion and inclusion filters.
				// We therefore exclude Java elements from the list
				// of non-Java resources.
				if (isFolderOnClasspath) {
					if (!classRootPaths.contains(member.getFullPath())) {
						nonJavaResources.add(member);
					}
				} else if (!javaProject.isOnClasspath(member)) {
					nonJavaResources.add(member);
				}
			}
			return nonJavaResources.toArray();
		} catch(CoreException e) {
			return NO_CHILDREN;
		}
	}

	private static Object[] getOpenNonJavaProjects(IJavaModel model) throws JavaModelException {
		Object[] nonJavaProjects= model.getNonJavaResources();
		ArrayList<IProject> result= new ArrayList<>(nonJavaProjects.length);
		for (Object nonJavaProject : nonJavaProjects) {
			IProject project = (IProject) nonJavaProject;
			if (project.isOpen())
				result.add(project);
		}
		return result.toArray();
	}

}

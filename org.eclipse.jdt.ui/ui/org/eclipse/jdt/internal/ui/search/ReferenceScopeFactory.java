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
package org.eclipse.jdt.internal.ui.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.internal.corext.util.JdtFlags;

public class ReferenceScopeFactory {

	private ReferenceScopeFactory() {
		// no instances, please
	}

	public static IJavaSearchScope create(IJavaElement element) throws JavaModelException {
		return SearchEngine.createJavaSearchScope(getScopeElements(element), false);
	}

	/*
	 * This method is public for testing purposes.
	 */
	public static IJavaElement[] getScopeElements(IJavaElement javaElement) throws JavaModelException {
		if (javaElement instanceof IMember) {
			IMember member= (IMember) javaElement;
			IJavaElement container= member.getCompilationUnit();
			if (container == null)
				container= member.getClassFile();
			if (JdtFlags.isPrivate(member)) {
				if (container != null)
					return new IJavaElement[] { container };
				else
					return new IJavaElement[] { member };
			}
			if (!JdtFlags.isPublic(member) && !JdtFlags.isProtected(member) && container != null) {
				IPackageFragment pack= (IPackageFragment) container.getParent();
				return new IJavaElement[] { pack };
			}
		}
		IPackageFragmentRoot root= (IPackageFragmentRoot) javaElement.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
		return getAllScopeElements(javaElement.getJavaProject(), root != null && isInJRELibrary(root));
	}

	private static IJavaElement[] getAllScopeElements(IJavaProject project, boolean includeJRE) throws JavaModelException {
		Collection sourceRoots= getNonSystemRoots(getReferencingProjects(project), false);
		sourceRoots.addAll(getNonSystemRoots(project, includeJRE));
		return (IPackageFragmentRoot[]) sourceRoots.toArray(new IPackageFragmentRoot[sourceRoots.size()]);
	}

	/**
	 * @param focus
	 * @return Collection		containing IJavaProject objects
	 * @throws JavaModelException
	 */
	private static Collection getReferencingProjects(IJavaProject focus) throws JavaModelException {
		Set projects= new HashSet();

		addReferencingProjects(focus, projects);
		return projects;
	}

	/**
	 * Adds to <code>projects</code> IJavaProject objects for all projects
	 * directly or indirectly referencing focus.
	 * 
	 * @param focus
	 * @param projects		IJavaProjects will be added to this set
	 * @throws JavaModelException
	 */
	private static void addReferencingProjects(IJavaProject focus, Set projects) throws JavaModelException {
		IProject[] referencingProjects= focus.getProject().getReferencingProjects();
		for (int i= 0; i < referencingProjects.length; i++) {
			IJavaProject candidate= JavaCore.create(referencingProjects[i]);
			if (candidate == null || projects.contains(candidate) || !candidate.exists())
				continue; // break cycle
			IClasspathEntry entry= getReferencingClassPathEntry(candidate, focus);
			if (entry != null) {
				projects.add(candidate);
				if (entry.isExported())
					addReferencingProjects(candidate, projects);
			}
		}
	}

	/**
	 * Finds, if possible, a classpathEntry in one given project such that this
	 * classpath entry references another given project.  If more than one entry
	 * exists for the referenced project and at least one is exported, then an
	 * exported entry will be returned.
	 * 
	 * @param referencingProject
	 * @param referencedProject
	 * @return IClasspathEntry
	 * @throws JavaModelException
	 */
	private static IClasspathEntry getReferencingClassPathEntry(IJavaProject referencingProject, IJavaProject referencedProject) throws JavaModelException {
		IClasspathEntry result= null;
		IPath path= referencedProject.getProject().getFullPath();
		IClasspathEntry[] classpath= referencingProject.getResolvedClasspath(true);
		for (int i= 0; i < classpath.length; i++) {
			IClasspathEntry entry= classpath[i];
			if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT && path.equals(entry.getPath())) {
				if (entry.isExported())
					return entry;
				// Consider it as a candidate. May be there is another entry that is
				// exported.
				result= entry;
			}
		}
		return result;
	}

	/**
	 * @param projects		a collection of IJavaProject
	 * @return Collection	a collection of IPackageFragmentRoot, one element
	 * for each packageFragmentRoot which lies within a project in
	 * <code>projects</code>.
	 */
	private static Collection getNonSystemRoots(Collection projects, boolean includeJRE) throws JavaModelException {
		List result= new ArrayList();
		for (Iterator it= projects.iterator(); it.hasNext();)
			result.addAll(getNonSystemRoots((IJavaProject) it.next(), includeJRE));
		return result;
	}

	private static List getNonSystemRoots(IJavaProject javaProject, boolean includeJRE) throws JavaModelException {
		List elements= new ArrayList();
		IPackageFragmentRoot[] roots= javaProject.getPackageFragmentRoots();
		// Add all package fragment roots except archives
		for (int i= 0; i < roots.length; i++) {
			IPackageFragmentRoot root= roots[i];
			if (includeJRE || !isInJRELibrary(root))
				elements.add(root);
		}
		return elements;
	}

	/**
	 * @param root
	 * @return
	 */
	private static boolean isInJRELibrary(IPackageFragmentRoot root) throws JavaModelException {
		IClasspathEntry entry= root.getRawClasspathEntry();
		if (!(entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER))
			return false;
		IPath path= entry.getPath();
		if (path.segmentCount() > 0 && path.segment(0).equals(JavaRuntime.JRE_CONTAINER))
			return true;
		return false;
	}
}

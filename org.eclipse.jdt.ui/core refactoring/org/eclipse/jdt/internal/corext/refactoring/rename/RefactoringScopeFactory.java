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
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IProject;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

public class RefactoringScopeFactory {

	private RefactoringScopeFactory() {
		//no instances
	}

	public static IJavaSearchScope create(IMember[] members) throws JavaModelException {
		Assert.isTrue(members != null && members.length > 0);
		IMember candidate= members[0];
		int visibility= getVisibility(candidate); 
		for (int i= 1; i < members.length; i++) {
			int mv= getVisibility(members[i]);
			if (mv > visibility) {
				visibility= mv;
				candidate= members[i];
			}
		}
		return create(candidate);
	}
	
	private static int getVisibility(IMember member) throws JavaModelException {
		if (JdtFlags.isPrivate(member))
			return 0;
		if (JdtFlags.isPackageVisible(member))
			return 1;
		if (JdtFlags.isProtected(member))
			return 2;
		return 4;
	}

	/**
	 * @return scope with all CUs or projects possibly referencing <code>javaElement</code>.
	 */ 
	public static IJavaSearchScope create(IJavaElement javaElement) throws JavaModelException {
		if (javaElement instanceof IMember) {
			IMember member= (IMember) javaElement;
			if (JdtFlags.isPrivate(member)) {
				if (member.getCompilationUnit() != null)
					return SearchEngine.createJavaSearchScope(new IJavaElement[] { member.getCompilationUnit()});
				else
					return SearchEngine.createJavaSearchScope(new IJavaElement[] { member });
			}
			// Removed code that does some optimizations regarding package visible members. The problem is that
			// there can be a package fragment with the same name in a different source folder or project. So we
			// have to treat package visible members like public or protected members.
		}
		return create(javaElement.getJavaProject());
	}

	private static IJavaSearchScope create(IJavaProject javaProject) throws JavaModelException {
		return SearchEngine.createJavaSearchScope(getAllScopeElements(javaProject), false);
	}

	private static IJavaElement[] getAllScopeElements(IJavaProject project) throws JavaModelException {
		Collection sourceRoots= getAllSourceRootsInProjects(getReferencingProjects(project));
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
		projects.add(focus);
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
	private static Collection getAllSourceRootsInProjects(Collection projects) throws JavaModelException {
		List result= new ArrayList();
		for (Iterator it= projects.iterator(); it.hasNext();)
			result.addAll(getSourceRoots((IJavaProject) it.next()));
		return result;
	}

	private static List getSourceRoots(IJavaProject javaProject) throws JavaModelException {
		List elements= new ArrayList();
		IPackageFragmentRoot[] roots= javaProject.getPackageFragmentRoots();
		// Add all package fragment roots except archives
		for (int i= 0; i < roots.length; i++) {
			IPackageFragmentRoot root= roots[i];
			if (!root.isArchive())
				elements.add(root);
		}
		return elements;
	}

	/**
	 * @return scope with all projects possibly referenced from within <code>javaElements</code>.
	 */ 
	public static IJavaSearchScope createReferencedScope(IJavaElement[] javaElements) {
		Set projects= new HashSet();
		for (int i= 0; i < javaElements.length; i++) {
			projects.add(javaElements[i].getJavaProject());
		}
		IJavaProject[] prj= (IJavaProject[]) projects.toArray(new IJavaProject[projects.size()]);
		return SearchEngine.createJavaSearchScope(prj, true);
	}
}

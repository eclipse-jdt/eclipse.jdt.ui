/*******************************************************************************
 * Copyright (c) 2000, 2001 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.ArrayList;
import java.util.List;

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

import org.eclipse.jdt.internal.corext.refactoring.util.JdtFlags;

public class RefactoringScopeFactory {
	
	private RefactoringScopeFactory(){
	}
	
	private static IJavaSearchScope create(IJavaProject javaProject) throws JavaModelException {
		List elements= getSourceRoots(javaProject);
		addReferencingProjects(javaProject.getProject(), elements);
		return SearchEngine.createJavaSearchScope((IJavaElement[]) elements.toArray(new IJavaElement[elements.size()]), false);
	}

	public static IJavaSearchScope create(IJavaElement javaElement) throws JavaModelException {
		if (javaElement instanceof IMember) {
			IMember member= (IMember)javaElement;
			if (JdtFlags.isPrivate(member)) {
				if (member.getCompilationUnit() != null)
					return SearchEngine.createJavaSearchScope(new IJavaElement[]{member.getCompilationUnit()});
				else 	
					return SearchEngine.createJavaSearchScope(new IJavaElement[]{member});
			}	
			if (! JdtFlags.isPublic(member) && !JdtFlags.isProtected(member) && member.getCompilationUnit() != null) {
				IPackageFragment pack= (IPackageFragment)member.getCompilationUnit().getParent();
				return SearchEngine.createJavaSearchScope(new IJavaElement[]{pack});
			}	
		}
		return create(javaElement.getJavaProject());
	}
	
	private static void addReferencingProjects(IProject focus, List list) throws JavaModelException {
		IPath path= focus.getProject().getFullPath();
		IProject[] projects= focus.getProject().getReferencingProjects();
		for (int i= 0, length= projects.length; i < length; i++) {
			IProject project= projects[i];
			IJavaProject javaProject= JavaCore.create(project);
			IClasspathEntry[] classpath= javaProject.getRawClasspath();
			for (int j= 0, length2= classpath.length; j < length2; j++) {
				IClasspathEntry entry= classpath[j];
				if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT && path.equals(entry.getPath())) {
					list.add(getSourceRoots(javaProject));
					if (entry.isExported())
						addReferencingProjects(javaProject.getProject(), list);
					break;
				}
			}
		}
	}
	
	private static List getSourceRoots(IJavaProject javaProject) throws JavaModelException {
		List elements= new  ArrayList();
		IPackageFragmentRoot[] roots= javaProject.getPackageFragmentRoots();
		// Add all package fragment roots except archives
		for (int i= 0; i < roots.length; i++) {
			IPackageFragmentRoot root= roots[i];
			if (!root.isArchive())
				elements.add(root);
		}
		return elements;
	}		
}


/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
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
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.refactoring.util.JdtFlags;

public class RefactoringScopeFactory {
	
	private RefactoringScopeFactory(){
	}
	
	private static IJavaSearchScope create(IJavaProject javaProject) throws JavaModelException {
		List projects= new  ArrayList();
		projects.add(javaProject);
		addReferencingProjects(javaProject.getProject(), projects);
		IJavaProject[] javaProjects= (IJavaProject[]) projects.toArray(new IJavaProject[projects.size()]);
		return SearchEngine.createJavaSearchScope(javaProjects, false);
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
			if (list.contains(javaProject)) continue; // break cycle
			
			IClasspathEntry[] classpath= javaProject.getResolvedClasspath(true);
			for (int j= 0, length2= classpath.length; j < length2; j++) {
				IClasspathEntry entry= classpath[j];
				if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT && path.equals(entry.getPath())) {
					list.add(javaProject);
					if (entry.isExported())
						addReferencingProjects(javaProject.getProject(), list);
					break;
				}
			}
		}
	}	
}


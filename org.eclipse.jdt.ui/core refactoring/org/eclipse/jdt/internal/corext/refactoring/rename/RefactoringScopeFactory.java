/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

class RefactoringScopeFactory {
	private static IJavaSearchScope create(IJavaProject javaProject) {
		List projects= new  ArrayList();
		projects.add(javaProject);
		projects.addAll(getReferencingJavaProjects(javaProject));
		IJavaProject[] javaProjects= (IJavaProject[]) projects.toArray(new IJavaProject[projects.size()]);
		return SearchEngine.createJavaSearchScope(javaProjects, false);
	}
	
	private static List getReferencingJavaProjects(IJavaProject javaProject){
		IProject[] refProjects= javaProject.getProject().getReferencingProjects();
		List result= new ArrayList(refProjects.length);
		for (int i= 0; i < refProjects.length; i++) {
			IProject refProject= refProjects[i];
			IJavaProject refJavaProject= JavaCore.create(refProject);
			if (refJavaProject != null)
				result.add(refJavaProject);
		}
		return result;
	}
	
	static IJavaSearchScope create(IJavaElement javaElement) throws JavaModelException {
		if (javaElement instanceof IMember) {
			IMember member= (IMember)javaElement;
			if (Flags.isPrivate(member.getFlags())) {
				if (member.getCompilationUnit() != null)
					return SearchEngine.createJavaSearchScope(new IJavaElement[]{member.getCompilationUnit()});
				else 	
					return SearchEngine.createJavaSearchScope(new IJavaElement[]{member});
			}	
			if (! Flags.isPublic(member.getFlags()) && !Flags.isProtected(member.getFlags()) && member.getCompilationUnit() != null) {
				IPackageFragment pack= (IPackageFragment)member.getCompilationUnit().getParent();
				return SearchEngine.createJavaSearchScope(new IJavaElement[]{pack});
			}	
		}
		return create(javaElement.getJavaProject());
	}
}


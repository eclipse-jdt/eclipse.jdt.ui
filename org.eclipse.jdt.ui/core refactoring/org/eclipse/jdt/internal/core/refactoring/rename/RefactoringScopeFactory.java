/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.rename;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;

class RefactoringScopeFactory {
	private static IJavaSearchScope create(IJavaProject javaProject){
		IProject project = javaProject.getProject();
		Set projects= new  HashSet();
		projects.add(project);
		projects.addAll(Arrays.asList(project.getReferencingProjects()));
		return SearchEngine.createJavaSearchScope((IResource[])projects.toArray(new IResource[projects.size()]));
	}
	
	static IJavaSearchScope create(IJavaElement javaElement) throws JavaModelException{
		if (javaElement instanceof IMember){
			IMember member= (IMember)javaElement;
			if (Flags.isPrivate(member.getFlags()))
				return SearchEngine.createJavaSearchScope(new IJavaElement[]{member.getCompilationUnit()});
			if (! Flags.isPublic(member.getFlags()) && !Flags.isProtected(member.getFlags())){
				IPackageFragment pack= (IPackageFragment)member.getCompilationUnit().getParent();
				return SearchEngine.createJavaSearchScope(new IJavaElement[]{pack});
			}	
		}
		return create(javaElement.getJavaProject());
	}
}


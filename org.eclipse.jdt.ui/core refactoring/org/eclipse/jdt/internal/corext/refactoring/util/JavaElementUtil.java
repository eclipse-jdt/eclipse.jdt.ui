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
package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class JavaElementUtil {
	
	//no instances
	private JavaElementUtil(){
	}
	
	public static String createMethodSignature(IMethod method){
		try {
			return Signature.toString(method.getSignature(), method.getElementName(), method.getParameterNames(), false, ! method.isConstructor());
		} catch(JavaModelException e) {
			return method.getElementName(); //fallback
		}
	}
	
	public static String createFieldSignature(IField field){
		return JavaModelUtil.getFullyQualifiedName(field.getDeclaringType()) + "." + field.getElementName(); //$NON-NLS-1$
	}
	
	public static String createSignature(IMember member){
		switch (member.getElementType()){
			case IJavaElement.FIELD:
				return createFieldSignature((IField)member);
			case IJavaElement.TYPE:
				return JavaModelUtil.getFullyQualifiedName(((IType)member));
			case IJavaElement.INITIALIZER:
				return RefactoringCoreMessages.getString("JavaElementUtil.initializer"); //$NON-NLS-1$
			case IJavaElement.METHOD:
				return createMethodSignature((IMethod)member);				
			default:
				Assert.isTrue(false);
				return null;	
		}
	}
	
	public static IJavaElement[] getElementsOfType(IJavaElement[] elements, int type){
		Set result= new HashSet(elements.length);
		for (int i= 0; i < elements.length; i++) {
			IJavaElement element= elements[i];
			if (element.getElementType() == type)
				result.add(element);
		}
		return (IJavaElement[]) result.toArray(new IJavaElement[result.size()]);
	}

	public static IType getMainType(ICompilationUnit cu) throws JavaModelException{
		IType[] types= cu.getTypes();
		for (int i = 0; i < types.length; i++) {
			if (isMainType(types[i]))
				return types[i];
		}
		return null;
	}
	
	public static boolean isMainType(IType type) throws JavaModelException{
		if (! type.exists())	
			return false;

		if (type.isBinary())
			return false;
			
		if (type.getCompilationUnit() == null)
			return false;
		
		if (type.getDeclaringType() != null)
			return false;
		
		return isPrimaryType(type) || isCuOnlyType(type);
	}


	private static boolean isPrimaryType(IType type){
		return type.getElementName().equals(Signature.getQualifier(type.getCompilationUnit().getElementName()));
	}


	private static boolean isCuOnlyType(IType type) throws JavaModelException{
		return type.getCompilationUnit().getTypes().length == 1;
	}

	/** @see org.eclipse.jdt.internal.core.JavaElement#isAncestorOf(org.eclipse.jdt.core.IJavaElement) */
	public static boolean isAncestorOf(IJavaElement ancestor, IJavaElement child) {
		IJavaElement parent= child.getParent();
		while (parent != null && !parent.equals(ancestor)) {
			parent= parent.getParent();
		}
		return parent != null;
	}
	
	public static IMethod[] getAllConstructors(IType type) throws JavaModelException {
		if (type.isInterface())
			return new IMethod[0];
		List result= new ArrayList();
		IMethod[] methods= type.getMethods();
		for (int i= 0; i < methods.length; i++) {
			IMethod iMethod= methods[i];
			if (iMethod.isConstructor())
				result.add(iMethod);
		}
		return (IMethod[]) result.toArray(new IMethod[result.size()]);
	}

	/**
	 * Returns an array of projects that have the specified root on their
	 * classpaths.
	 */
	public static IJavaProject[] getReferencingProjects(IPackageFragmentRoot root) throws JavaModelException {
		IClasspathEntry cpe= root.getRawClasspathEntry();
		IJavaProject myProject= root.getJavaProject();
		IJavaProject[] allJavaProjects= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
		List result= new ArrayList(allJavaProjects.length);
		for (int i= 0; i < allJavaProjects.length; i++) {
			IJavaProject project= allJavaProjects[i];
			if (project.equals(myProject))
				continue;
			IPackageFragmentRoot[] roots= project.findPackageFragmentRoots(cpe);
			if (roots.length > 0)
				result.add(project);
		}
		return (IJavaProject[]) result.toArray(new IJavaProject[result.size()]);
	}	
	
	public static IMember[] merge(IMember[] a1, IMember[] a2) {
		// Don't use hash sets since ordering is important for some refactorings.
		List result= new ArrayList(a1.length + a2.length);
		for (int i= 0; i < a1.length; i++) {
			IMember member= a1[i];
			if (!result.contains(member))
				result.add(member);
		}
		for (int i= 0; i < a2.length; i++) {
			IMember member= a2[i];
			if (!result.contains(member))
				result.add(member);
		}
		return (IMember[]) result.toArray(new IMember[result.size()]);
	}

	public static boolean isDefaultPackage(Object element) {
		return (element instanceof IPackageFragment) && ((IPackageFragment)element).isDefaultPackage();
	}
	
	public static IMember[] sortByOffset(IMember[] members){
		Comparator comparator= new Comparator(){
			public int compare(Object o1, Object o2){
				try{
					return ((IMember) o1).getNameRange().getOffset() - ((IMember) o2).getNameRange().getOffset();
				} catch (JavaModelException e){
					return 0;
				}	
			}
		};
		Arrays.sort(members, comparator);
		return members;
	}
}

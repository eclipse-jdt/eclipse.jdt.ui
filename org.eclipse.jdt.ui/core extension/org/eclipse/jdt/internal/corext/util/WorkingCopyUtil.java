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
package org.eclipse.jdt.internal.corext.util;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;


public class WorkingCopyUtil {

	//no instances
	private WorkingCopyUtil(){
	}
	
	public static IJavaElement getWorkingCopyIfExists(IJavaElement element) {
		if (element == null) return null;
		switch(element.getElementType()) {
			case IJavaElement.COMPILATION_UNIT: return JavaModelUtil.toWorkingCopy((ICompilationUnit)element);
			case IJavaElement.TYPE:
			case IJavaElement.FIELD:
			case IJavaElement.METHOD:
			case IJavaElement.INITIALIZER: return JavaModelUtil.toWorkingCopy((IMember)element);
			case IJavaElement.IMPORT_CONTAINER: return JavaModelUtil.toWorkingCopy((IImportContainer)element);
			case IJavaElement.IMPORT_DECLARATION: return JavaModelUtil.toWorkingCopy((IImportDeclaration)element);
			case IJavaElement.PACKAGE_DECLARATION: return JavaModelUtil.toWorkingCopy((IPackageDeclaration)element);
			default: return element;
		}
	}

	public static ICompilationUnit getWorkingCopyIfExists(ICompilationUnit cu){
		if (cu == null)
			return null;
		return JavaModelUtil.toWorkingCopy(cu);
	}
	
	public static IMember getWorkingCopyIfExists(IMember member) throws JavaModelException {
		if (member == null)
			return null;
		return JavaModelUtil.toWorkingCopy(member);
	}
	
	public static IJavaElement getOriginal(IMember member){
		return JavaModelUtil.toOriginal(member);
	}

	public static ICompilationUnit getOriginal(ICompilationUnit cu){
		return JavaModelUtil.toOriginal(cu);
	}
	
	public static IMember[] getOriginals(IMember[] members){
		IMember[] result= new IMember[members.length];
		for (int i= 0; i < members.length; i++) {
			result[i]= (IMember)WorkingCopyUtil.getOriginal(members[i]);
		}
		return result;
	}

	/**
	 * Creates a <em>new</em> working copy and the caller is responsible for destroying it.
	 * @see org.eclipse.jdt.core.IWorkingCopy#destroy()
	 */
	public static ICompilationUnit getNewWorkingCopy(ICompilationUnit cu) throws JavaModelException{
		/*
		 * Explicitly create a new working copy.
		 */
		return (ICompilationUnit)(getOriginal(cu).getWorkingCopy());
	}

	/**
	 * Creates a <em>new</em> working copy and the caller is responsible for destroying it.
	 * A cu with the specified name may or may not exist in the package.
	 * @see org.eclipse.jdt.core.IWorkingCopy#destroy()
	 */
	public static ICompilationUnit getNewWorkingCopy(IPackageFragment pack, String cuName) throws JavaModelException{
		return (ICompilationUnit)pack.getCompilationUnit(cuName).getWorkingCopy();
	}
}


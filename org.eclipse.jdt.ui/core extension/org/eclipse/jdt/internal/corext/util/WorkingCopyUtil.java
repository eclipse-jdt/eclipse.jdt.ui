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
package org.eclipse.jdt.internal.corext.util;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;


public class WorkingCopyUtil {

	//no instances
	private WorkingCopyUtil(){
	}
	
	/**
	 * @deprecated Inline this method.
	 */
	public static IJavaElement getWorkingCopyIfExists(IJavaElement element) {
		return element;
	}
	
	/**
	 * @deprecated Inline this method.
	 */
	public static ICompilationUnit getWorkingCopyIfExists(ICompilationUnit element) {
		return element;
	}
	
	/**
	 * @deprecated Inline this method.
	 */
	public static IMember getWorkingCopyIfExists(IMember member) {
		return member;
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
		return (getOriginal(cu).getWorkingCopy(null));
	}

	/**
	 * Creates a <em>new</em> working copy and the caller is responsible for destroying it.
	 * @see org.eclipse.jdt.core.IWorkingCopy#destroy()
	 */
	public static ICompilationUnit getNewWorkingCopy(ICompilationUnit cu, WorkingCopyOwner owner, IProgressMonitor pm) throws JavaModelException{
		/*
		 * Explicitly create a new working copy.
		 */
		return getOriginal(cu).getWorkingCopy(owner, null, pm);
	}

	/**
	 * Creates a <em>new</em> working copy and the caller is responsible for destroying it.
	 * A cu with the specified name may or may not exist in the package.
	 * @see org.eclipse.jdt.core.IWorkingCopy#destroy()
	 */
	public static ICompilationUnit getNewWorkingCopy(IPackageFragment pack, String cuName) throws JavaModelException{
		return pack.getCompilationUnit(cuName).getWorkingCopy(null);
	}

	/**
	 * Creates a <em>new</em> working copy and the caller is responsible for destroying it.
	 * A cu with the specified name may or may not exist in the package.
	 * @see org.eclipse.jdt.core.IWorkingCopy#destroy()
	 */
	public static ICompilationUnit getNewWorkingCopy(IPackageFragment pack, String cuName, WorkingCopyOwner owner, IProgressMonitor pm) throws JavaModelException{
		return pack.getCompilationUnit(cuName).getWorkingCopy(owner, null, pm);
	}
}


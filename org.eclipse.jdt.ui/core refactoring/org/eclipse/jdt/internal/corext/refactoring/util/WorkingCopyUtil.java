/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.util;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IWorkingCopy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class WorkingCopyUtil {

	//no instances
	private WorkingCopyUtil(){
	}
	
	public static ICompilationUnit getWorkingCopyIfExists(ICompilationUnit cu){
		if (cu == null)
			return null;
		if (cu.isWorkingCopy())
			return cu;
		// XXX: This is a layer breaker - should not access jdt.ui			
		IWorkingCopy[] wcs= JavaUI.getSharedWorkingCopies();
		for(int i= 0; i < wcs.length; i++){
			if (cu.equals(wcs[i].getOriginalElement()) && wcs[i] instanceof ICompilationUnit)
				return (ICompilationUnit)wcs[i];
		}
		return cu;
	}
	
	public static IMember getWorkingCopyIfExists(IMember member) throws JavaModelException {
		if (member == null)
			return null;
		if (member.getCompilationUnit().isWorkingCopy())
			return member;
			
		ICompilationUnit workingCopy= getWorkingCopyIfExists(member.getCompilationUnit());
		if (workingCopy == null)
			return null;
		if (!workingCopy.isWorkingCopy())
			return member;
			
		return JavaModelUtil.findMemberInCompilationUnit(workingCopy, member);
	}
	
	public static IJavaElement getOriginal(IMember member){
		if (! member.getCompilationUnit().isWorkingCopy())
			return member;
		return member.getCompilationUnit().getOriginal(member);	
	}

	public static ICompilationUnit getOriginal(ICompilationUnit cu){
		if (! cu.isWorkingCopy())
			return cu;
		else
			return (ICompilationUnit)cu.getOriginalElement();	
	}

	/**
	 * Creates a <em>new</em> working copy and the caller is responsible for destroying it.
	 * @see IWorkingCopy#destroy()
	 */
	public static ICompilationUnit getNewWorkingCopy(ICompilationUnit cu) throws JavaModelException{
		/*
		 * Explicitly create a new working copy.
		 */
		return (ICompilationUnit)(getOriginal(cu).getWorkingCopy());
	}

}


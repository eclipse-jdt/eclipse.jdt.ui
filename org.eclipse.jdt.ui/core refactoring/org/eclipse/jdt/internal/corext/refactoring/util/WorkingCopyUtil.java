/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.util;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;

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
		ICompilationUnit[] wcs= ResourceManager.getWorkingCopies();
		for(int i= 0; i < wcs.length; i++){
			if (cu.equals(wcs[i].getOriginalElement()))
				return wcs[i];
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
	
}


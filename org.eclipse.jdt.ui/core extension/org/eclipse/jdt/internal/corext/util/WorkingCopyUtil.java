/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.util;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;


public class WorkingCopyUtil {

	//no instances
	private WorkingCopyUtil(){
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
	 * @see IWorkingCopy#destroy()
	 */
	public static ICompilationUnit getNewWorkingCopy(ICompilationUnit cu) throws JavaModelException{
		/*
		 * Explicitly create a new working copy.
		 */
		return (ICompilationUnit)(getOriginal(cu).getWorkingCopy());
	}

}


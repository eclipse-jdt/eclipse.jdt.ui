package org.eclipse.jdt.internal.core.refactoring.rename;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;

import org.eclipse.jdt.internal.core.refactoring.util.ResourceManager;

class WorkingCopyUtil {

	private WorkingCopyUtil(){
	}
	
	static ICompilationUnit getWorkingCopyIfExists(ICompilationUnit cu){
		if (cu.isWorkingCopy())
			return cu;
		ICompilationUnit[] wcs= ResourceManager.getWorkingCopies();
		for(int i= 0; i < wcs.length; i++){
			if (cu.equals(wcs[i].getOriginalElement()))
				return wcs[i];
		}
		return cu;
	}
	
	static IJavaElement getOriginal(IMember member){
		if (! member.getCompilationUnit().isWorkingCopy())
			return member;
		return member.getCompilationUnit().getOriginal(member);	
	}	
	
}


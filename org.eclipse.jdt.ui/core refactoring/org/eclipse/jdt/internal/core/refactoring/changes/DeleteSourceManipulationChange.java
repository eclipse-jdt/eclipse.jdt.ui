package org.eclipse.jdt.internal.core.refactoring.changes;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceManipulation;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.base.Change;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;

class DeleteSourceManipulationChange extends DeleteChange {

	private String fHandle;;
	
	DeleteSourceManipulationChange(ISourceManipulation sm){
		Assert.isNotNull(sm);
		fHandle= getJavaElement(sm).getHandleIdentifier();
	}

	/**
	 * @see IChange#getName()
	 */
	public String getName() {
		return "Delete";
	}

	/**
	 * @see IChange#getCorrespondingJavaElement()
	 */
	public IJavaElement getCorrespondingJavaElement() {
		return (IJavaElement)JavaCore.create(fHandle);
	}
	
	/**
	 * @see DeleteChange#doDelete(IProgressMonitor)
	 */
	protected void doDelete(IProgressMonitor pm) throws JavaModelException{
		//cast safe
		((ISourceManipulation)getCorrespondingJavaElement()).delete(true, pm);	
	}
		
	private static IJavaElement getJavaElement(ISourceManipulation sm){
		//XXX all known ISourceManipulations are IJavaElements
		return (IJavaElement)sm;
	}
}


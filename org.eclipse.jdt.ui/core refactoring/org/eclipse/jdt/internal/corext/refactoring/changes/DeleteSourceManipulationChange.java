/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceManipulation;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IReorgExceptionHandler;

public class DeleteSourceManipulationChange extends AbstractDeleteChange {

	private String fHandle;
	
	public DeleteSourceManipulationChange(ISourceManipulation sm){
		Assert.isNotNull(sm);
		fHandle= getJavaElement(sm).getHandleIdentifier();
	}

	/*
	 * @see IChange#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("DeleteSourceManipulationChange.delete"); //$NON-NLS-1$
	}

	/*
	 * @see IChange#getModifiedLanguageElement()
	 */
	public Object getModifiedLanguageElement() {
		return JavaCore.create(fHandle);
	}
	
	/*
	 * @see DeleteChange#doDelete(IProgressMonitor)
	 */
	protected void doDelete(ChangeContext context, IProgressMonitor pm) throws JavaModelException{
		try{
			//cast safe
			((ISourceManipulation)getModifiedLanguageElement()).delete(false, pm);	
		} catch (JavaModelException jme) {
			if (! (getModifiedLanguageElement() instanceof ICompilationUnit))
				throw jme;
			if (! (context.getExceptionHandler() instanceof IReorgExceptionHandler))
				throw jme;
			if (! (jme.getException() instanceof CoreException))
				throw jme;
			ICompilationUnit cu= (ICompilationUnit)getModifiedLanguageElement();
			CoreException ce= (CoreException)jme.getException();
			IReorgExceptionHandler handler= (IReorgExceptionHandler)context.getExceptionHandler();
			IStatus[] children= ce.getStatus().getChildren();
			if (children.length == 1 && children[0].getCode() == IResourceStatus.OUT_OF_SYNC_LOCAL){
				if (handler.forceDeletingResourceOutOfSynch(cu.getElementName(), ce)){
					cu.delete(true, pm);
					return;
				}	else
						return; //do not rethrow in this case
			} else
				throw jme;
		}
		
	}
		
	private static IJavaElement getJavaElement(ISourceManipulation sm){
		//all known ISourceManipulations are IJavaElements
		return (IJavaElement)sm;
	}
}


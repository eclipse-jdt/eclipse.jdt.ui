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
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceManipulation;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;

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
		return RefactoringCoreMessages.getFormattedString("DeleteSourceManipulationChange.0", getElementName()); //$NON-NLS-1$
	}
	
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		return super.isValid(pm, false, true);
	}

	private String getElementName() {
		IJavaElement javaElement= getJavaElement(getSourceModification());
		if (JavaElementUtil.isDefaultPackage(javaElement))
			return RefactoringCoreMessages.getString("DeleteSourceManipulationChange.1"); //$NON-NLS-1$
		return javaElement.getElementName();
	}

	/*
	 * @see IChange#getModifiedLanguageElement()
	 */
	public Object getModifiedElement() {
		return JavaCore.create(fHandle);
	}
	
	/*
	 * @see DeleteChange#doDelete(IProgressMonitor)
	 */
	protected void doDelete(IProgressMonitor pm) throws JavaModelException {
		try{
			getSourceModification().delete(false, pm);	
		} catch (JavaModelException jme) {
			// the problem is that there isn't any implementation of IReorgExceptionHandler.
			// So the code got never executed. Have to check what to do here.
			
//			if (! (getModifiedLanguageElement() instanceof ICompilationUnit))
//				throw jme;
//			if (! (context.getExceptionHandler() instanceof IReorgExceptionHandler))
//				throw jme;
//			if (! (jme.getException() instanceof CoreException))
//				throw jme;
//			ICompilationUnit cu= (ICompilationUnit)getModifiedLanguageElement();
//			CoreException ce= (CoreException)jme.getException();
//			IReorgExceptionHandler handler= (IReorgExceptionHandler)context.getExceptionHandler();
//			IStatus[] children= ce.getStatus().getChildren();
//			if (children.length == 1 && children[0].getCode() == IResourceStatus.OUT_OF_SYNC_LOCAL){
//				if (handler.forceDeletingResourceOutOfSynch(cu.getElementName(), ce)){
//					cu.delete(true, pm);
//					return;
//				}	else
//						return; //do not rethrow in this case
//			} else
//				throw jme;
			throw jme;
		}
		
	}
		
	private ISourceManipulation getSourceModification() {
		return (ISourceManipulation)getModifiedElement();
	}

	private static IJavaElement getJavaElement(ISourceManipulation sm){
		//all known ISourceManipulations are IJavaElements
		return (IJavaElement)sm;
	}
}


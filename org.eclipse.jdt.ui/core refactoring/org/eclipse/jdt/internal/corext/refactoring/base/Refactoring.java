/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.base;

import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.UndoManager;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;

/**
 * Superclass for all refactorings.
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public abstract class Refactoring implements IRefactoring {

	private static IUndoManager fgUndoManager= new UndoManager();
	
	private IFile[] fUnsavedFiles= new IFile[0];
		
	public static IUndoManager getUndoManager() {
		return fgUndoManager;
	}
	
	public void setUnsavedFiles(IFile[] files){
		Assert.isNotNull(files);
		fUnsavedFiles= files;
	}
	
	protected IFile[] getUnsavedFiles(){
		return fUnsavedFiles;
	}
	
	/* non java-doc
	 * for debugging only
	 */
	public String toString(){
		return getName();
	}
	
	//---- Conditions ---------------------------
	
	/**
	 * Checks if this refactoring can be activated.
	 * Typically, this is used in the ui to check if a corresponding menu entry should be shown.
	 * Must not return <code>null</code>.
	 */ 
	public abstract RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException;
	
	/**
	 * After <code>checkActivation</code> has been performed and the user has provided all input
	 * necessary to perform the refactoring this method is called to check the remaining preconditions.
	 * Typically, this is used in the ui after the user has pressed 'next' on the last user input page.
	 * This method is always called after <code>checkActivation</code> and only if the status returned by
	 * <code>checkActivation</code> <code>isOK</code>.
	 * Must not return <code>null</code>.
	 * @see #checkActivation
	 * @see RefactoringStatus#isOK
	 */ 		
	public abstract RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException;
	
	/**
	 * @see IRefactoring#checkPreconditions
	 * This implementation performs <code>checkActivation</code>
	 * and <code>checkInput</code> and merges the results.
	 * 
	 * @see #checkActivation
	 * @see #checkInput
	 * @see RefactoringStatus#merge
	 */
	public RefactoringStatus checkPreconditions(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 11); //$NON-NLS-1$
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkActivation(new SubProgressMonitor(pm, 1, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK)));
		if (!result.hasFatalError())
			result.merge(checkInput(new SubProgressMonitor(pm, 10, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK)));	
		pm.done();
		return result;
	}
		
	/**
	 * Checks whether it is possible to modify the given <code>IJavaElement</code>.
	 * The <code>IJavaElement</code> must exist and be non read-only to be modifiable.
	 * Moreover, if it is a <code>IMember</code> it must not be binary.
	 * The returned <code>RefactoringStatus</code> has <code>ERROR</code> severity if
	 * it is not possible to modify the element.
	 *
	 * @see IJavaElement#exists
	 * @see IJavaElement#isReadOnly
	 * @see IMember#isBinary
	 * @see RefactoringStatus
	 *
	 */ 
	protected static RefactoringStatus checkAvailability(IJavaElement javaElement) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		if (! javaElement.exists())
			result.addFatalError(RefactoringCoreMessages.getFormattedString("Refactoring.not_in_model", javaElement.getElementName())); //$NON-NLS-1$
		if (javaElement.isReadOnly())
			result.addFatalError(RefactoringCoreMessages.getFormattedString("Refactoring.read_only", javaElement.getElementName()));	 //$NON-NLS-1$
		if (javaElement.exists() && !javaElement.isStructureKnown())
			result.addFatalError(RefactoringCoreMessages.getFormattedString("Refactoring.unknown_structure", javaElement.getElementName()));	 //$NON-NLS-1$
		if (javaElement instanceof IMember && ((IMember)javaElement).isBinary())
			result.addFatalError(RefactoringCoreMessages.getFormattedString("Refactoring.binary", javaElement.getElementName())); //$NON-NLS-1$
		return result;
	}
	
	//----- other ------------------------------
			
	/**
	 * Finds an <code>IResource</code> for a given <code>ICompilationUnit</code>.
	 * If the parameter is a working copy then the <code>IResource</code> for
	 * the original element is returned.
	 * @see ICompilationUnit#isWorkingCopy
	 * @see ICompilationUnit#getUnderlyingResource
	 */
	public static IResource getResource(ICompilationUnit cu) throws JavaModelException{
		if (cu.isWorkingCopy()) 
			return cu.getOriginalElement().getUnderlyingResource();
		else 
			return cu.getUnderlyingResource();
	}
	
	/**
	 * Returns the <code>IResource</code> that the given <code>IMember</code> is defined in.
	 * @see #getResource
	 */
	public static IResource getResource(IMember member) throws JavaModelException{
		Assert.isTrue(!member.isBinary());
		return getResource(member.getCompilationUnit());
	}
}
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
package org.eclipse.jdt.internal.corext.refactoring.base;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.internal.corext.refactoring.UndoManager;

/**
 * Abstract super class for all refactorings.
 * <ul>
 *   <li>
 * </u> 
 */
public abstract class Refactoring implements IAdaptable {

	private static IUndoManager fgUndoManager= null;
	
	public static IUndoManager getUndoManager() {
		if (fgUndoManager == null)
			fgUndoManager= new UndoManager();
		return fgUndoManager;
	}
	
	/**
	 * Returns the refactoring's name.
	 * 
	 * @return the refactoring's name. Mainly used in the UI.
	 */ 
	public abstract String getName();
	
	//---- Conditions ------------------------------------------------------------
	
	/**
	 * This implementation performs <code>checkActivation</code>
	 * and <code>checkInput</code> and merges the results.
	 * 
	 * @see #checkActivation
	 * @see #checkInput
	 * @see RefactoringStatus#merge
	 */
	public RefactoringStatus checkPreconditions(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 11); //$NON-NLS-1$
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkActivation(new SubProgressMonitor(pm, 1)));
		if (!result.hasFatalError())
			result.merge(checkInput(new SubProgressMonitor(pm, 10)));	
		pm.done();
		return result;
	}
	
	/**
	 * Checks if the refactoring can be executed at all. The method is typically
	 * called by the UI to perform a <em>fast</em> check after an action got 
	 * executed.
	 * <p>
	 * The refactoring is considered as not being executable if the returned status
	 * has the severity <code>RefactoringStatus#FATAL</code>.
	 * 
	 * @param pm a progress monitor to report progress. Although availability checks 
	 *  are supposed to execute fast, there can be certain situations where progress
	 *  reporting is necessary. For example rebuilding a corrupted index may report
	 *  progress.
	 * @return a refactoring status. If the status is <code>RefactoringStatus#FATAL</code>
	 *  the refactoring is considered as not being executable.
	 * @throws CoreException if an execption occurs during activation checking.
	 */ 
	public abstract RefactoringStatus checkActivation(IProgressMonitor pm) throws CoreException;
	
	/**
	 * After <code>checkActivation</code> has been performed and the user has provided all input
	 * necessary to perform the refactoring this method is called to check the remaining preconditions.
	 * Typically, this is used in the ui after the user has pressed 'next' on the last user input page.
	 * This method is always called after <code>checkActivation</code> and only if the status returned by
	 * <code>checkActivation</code> is not <code>RefactoringStatus#FATAL</code>.
	 * May be called more than once.
	 * Must not return <code>null</code>.
	 * 
	 * @see #checkActivation
	 * @see RefactoringStatus#FATAL
	 */ 		
	public abstract RefactoringStatus checkInput(IProgressMonitor pm) throws CoreException;
	
	//---- change creation ------------------------------------------------------
		
	/**
	 * Creates an <code>IChange</code> object that performs the actual refactoring.
	 * It is guaranteed that <code>createChange</code> is not called before <code>
	 * checkPreconditions</code> or that it isn't called at all if <code>checkPreconditions
	 * </code> returns an <code>RefactoringStatus</code> object with a severity of <code>
	 * RefactoringStatus.ERROR</code>.
	 */
	public abstract Change createChange(IProgressMonitor pm) throws CoreException;
	
	/* (non-Javadoc)
	 * Method declared in IAdaptabe 
	 */
	public Object getAdapter(Class adapter) {
		if (adapter.isInstance(this))
			return this;
		return null;
	}
	
	/* (non-Javadoc)
	 * for debugging only
	 */
	public String toString() {
		return getName();
	}
}

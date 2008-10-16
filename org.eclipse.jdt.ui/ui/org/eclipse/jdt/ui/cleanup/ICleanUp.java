/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.cleanup;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;


/**
 * A clean up solves problems in a compilation unit.
 * <p>
 * The clean up is asked for its requirements through a call to {@link #getRequirements()}. The
 * clean up can request an AST and define how to build this AST. It can base its requirements on the
 * options passed through {@link #setOptions(CleanUpOptions)}.
 * </p>
 * <p>
 * A context containing the information requested by the requirements are passed to
 * {@link #createFix(CleanUpContext)}. A fix capable of fixing the problems is returned by this
 * function if {@link #checkPreConditions(IJavaProject, ICompilationUnit[], IProgressMonitor)} has
 * returned a non fatal error status.
 * </p>
 * <p>
 * At the end {@link #checkPostConditions(IProgressMonitor)} is called.
 * </p>
 * 
 * @since 3.5
 */
public interface ICleanUp {

	/**
	 * Sets the options that will be used.
	 * 
	 * @param options the options to use
	 */
	public void setOptions(CleanUpOptions options);

	/**
	 * Human readable description for each step this clean up will execute.
	 * 
	 * @return descriptions an array of {@linkplain String strings} or <code>null</code>
	 */
	public String[] getStepDescriptions();

	/**
	 * The requirements of this clean up.
	 * 
	 * @return the requirements used for {@link #createFix(CleanUpContext)} to work
	 */
	public CleanUpRequirements getRequirements();

	/**
	 * After call to checkPreConditions clients will start creating fixes for
	 * <code>compilationUnits</code> in <code>project</code> unless the result of checkPreConditions
	 * contains a fatal error
	 * 
	 * @param project The project to clean up
	 * @param compilationUnits The compilation Units to clean up, all member of project
	 * @param monitor the monitor to show progress
	 * @return the result of the precondition check, not null
	 * @throws CoreException if an unexpected error occurred
	 */
	public abstract RefactoringStatus checkPreConditions(IJavaProject project, ICompilationUnit[] compilationUnits, IProgressMonitor monitor) throws CoreException;

	/**
	 * Create an <code>ICleanUpFix</code> which fixes all problems in <code>context</code> or
	 * <code>null</code> if nothing to fix.
	 * 
	 * @param context a context containing all information requested by {@link #getRequirements()}
	 * @return the fix for the problems or <code>null</code> if nothing to fix
	 * @throws CoreException if an unexpected error occurred
	 */
	public abstract ICleanUpFix createFix(CleanUpContext context) throws CoreException;

	/**
	 * Called when done cleaning up.
	 * 
	 * @param monitor the monitor to show progress
	 * @return the result of the postcondition check, not null
	 * @throws CoreException if an unexpected error occurred
	 */
	public abstract RefactoringStatus checkPostConditions(IProgressMonitor monitor) throws CoreException;

}

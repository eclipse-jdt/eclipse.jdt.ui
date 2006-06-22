/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.IFix;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A clean up can solve several different problems in
 * a given <code>CompilationUnit</code>. The <code>CompilationUnit</code>
 * is compiled by using the compiler options returned by
 * <code>getRequiredOptions</code>. A <code>ICleanUp</code> can have
 * different options which can be set by using the <code>Control</code>
 * returned by <code>createConfigurationControl</code>
 *
 * @since 3.2
 */
public interface ICleanUp {
	
	/**
	 * Create a <code>IFix</code> which fixes all problems which this 
	 * multi-fix can fix in <code>CompilationUnit</code>.
	 * 
	 * @param compilationUnit The compilation unit to fix, may be null
	 * @return The fix or null if no fixes possible
	 * @throws CoreException
	 */
	public abstract IFix createFix(CompilationUnit compilationUnit) throws CoreException;
	
	/**
	 * Create a <code>IFix</code> which fixes all <code>problems</code> in
	 * <code>CompilationUnit</code>
	 * 
	 * @param compilationUnit The compilation unit to fix, may be null
	 * @param problems The locations of the problems to fix
	 * @return The fix or null if no fixes possible
	 * @throws CoreException
	 */
	public abstract IFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException;

	/**
	 * Required compiler options to allow <code>createFix</code> to work
	 * correct.
	 *
	 * @return The options as map or null
	 */
	public abstract Map getRequiredOptions();

	/**
	 * Persist current settings of this in <code>settings</code>
	 * 
	 * @param settings The settings to store to, not null
	 */
	public abstract void saveSettings(IDialogSettings settings);
	
	/**
	 * Description for each operation this clean up will execute
	 * 
	 * @return descriptions or null
	 */
	public String[] getDescriptions();
	
	/**
	 * After call to checkPreConditions clients will start creating fixes for <code>compilationUnits</code>
	 * int <code>project</code> unless the result of checkPreConditions contains a fatal error
	 * 
	 * @param project The project to clean up
	 * @param compilationUnits The compilation Units to clean up, all member of project
	 * @param monitor the monitor to show progress
	 * @return the result of the precondition check, not null
	 */
	public abstract RefactoringStatus checkPreConditions(IJavaProject project, ICompilationUnit[] compilationUnits, IProgressMonitor monitor) throws CoreException;
	
	/**
	 * Called when done cleaning up.
	 * 
	 * @param monitor the monitor to show progress
	 * @return the result of the postcondition check, not null
	 */
	public abstract RefactoringStatus checkPostConditions(IProgressMonitor monitor) throws CoreException;
	
	/**
	 * True if <code>problem</code> in <code>CompilationUnit</code> can be fixed
	 * by this CleanUp. If true
	 * <code>createFix(compilationUnit, new IProblemLocation[] {problem})</code>
	 * does not return null.
	 * 
	 * @param compilationUnit The compilation unit to fix not null
	 * @param problem The location of the problem to fix
	 * @return True if problem can be fixed
	 * @throws CoreException
	 */
	public boolean canFix(CompilationUnit compilationUnit, IProblemLocation problem) throws CoreException;

	/**
	 * Maximal number of problems this clean up will fix in
	 * compilation unit. There may be less then the returned number
	 * but never more.
	 * 
	 * @param compilationUnit The compilation unit to fix, not null
	 * @return The maximal number of fixes or -1 if unknown.
	 */
	public abstract int maximalNumberOfFixes(CompilationUnit compilationUnit);

	/**
	 * Default flags for this clean up.
	 * 
	 * @return Default flags for this clean up >= 0
	 */
	public abstract int getDefaultFlag();

	/**
	 * Set flag with id to b.
	 * 
	 * @param id The id of the flag to set
	 * @param b The value for the flag
	 */
	public void setFlag(int id, boolean b);

	/**
	 * Is flag with id enabled?
	 * 
	 * @return True if flag with id is enabled
	 */
	public boolean isFlag(int id);

	/**
	 * A code snippet which complies to the
	 * current settings.
	 * 
	 * @return A code snippet, not null.
	 */
	public abstract String getPreview();

}

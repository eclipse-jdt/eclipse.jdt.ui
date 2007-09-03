/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.IFix;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

public abstract class AbstractCleanUp implements ICleanUp {
	
	private CleanUpOptions fOptions;
	
	protected AbstractCleanUp() {
	}

	protected AbstractCleanUp(Map settings) {
		if (settings != null)
			setOptions(new CleanUpOptions(settings));
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setOptions(CleanUpOptions options) {
		fOptions= options;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public CleanUpOptions getOptions() {
		return fOptions;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String[] getDescriptions() {
		return new String[0];
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getPreview() {
		return ""; //$NON-NLS-1$
	}

	/**
	 * {@inheritDoc}
	 */
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(false, false, null);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus checkPreConditions(IJavaProject project, ICompilationUnit[] compilationUnits, IProgressMonitor monitor) throws CoreException {
		return new RefactoringStatus();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IFix createFix(CleanUpContext context) throws CoreException {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus checkPostConditions(IProgressMonitor monitor) throws CoreException {
		return new RefactoringStatus();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean canFix(CompilationUnit compilationUnit, IProblemLocation problem) throws CoreException {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public int computeNumberOfFixes(CompilationUnit compilationUnit) {
		return -1;
	}
	
	/**
	 * Utility method to: count number of problems in <code>problems</code> with <code>problemId</code>
	 * @param problems the set of problems
	 * @param problemId the problem id to look for
	 * @return number of problems with problem id
	 */
	protected static int getNumberOfProblems(IProblem[] problems, int problemId) {
		int result= 0;
		for (int i= 0; i < problems.length; i++) {
			if (problems[i].getID() == problemId)
				result++;
		}
		return result;
	}
	
	protected boolean isEnabled(String key) {
		Assert.isNotNull(fOptions);
		
		return fOptions.isEnabled(key);
	}
	
}

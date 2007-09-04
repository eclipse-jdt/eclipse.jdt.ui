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

import java.util.ArrayList;
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

import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

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
	 * @param key the name of the option
	 * @return true if option with <code>key</code> is enabled
	 */
	protected boolean isEnabled(String key) {
		Assert.isNotNull(fOptions);
		Assert.isNotNull(key);

		return fOptions.isEnabled(key);
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

	/**
	 * Convert set of IProblems to IProblemLocations
	 * @param problems the problems to convert
	 * @return the converted set
	 */
	protected static IProblemLocation[] convertProblems(IProblem[] problems) {
		IProblemLocation[] result= new IProblemLocation[problems.length];

		for (int i= 0; i < problems.length; i++) {
			result[i]= new ProblemLocation(problems[i]);
		}

		return result;
	}

	/**
	 * Returns unique problem locations. All locations in result 
	 * have an id element <code>problemIds</code>.
	 * 
	 * @param problems the problems to filter
	 * @param problemIds the ids of the resulting problem locations
	 * @return problem locations
	 */
	protected static IProblemLocation[] filter(IProblemLocation[] problems, int[] problemIds) {
		ArrayList result= new ArrayList();

		for (int i= 0; i < problems.length; i++) {
			IProblemLocation problem= problems[i];
			if (contains(problemIds, problem.getProblemId()) && !contains(result, problem)) {
				result.add(problem);
			}
		}

		return (IProblemLocation[]) result.toArray(new IProblemLocation[result.size()]);
	}

	private static boolean contains(ArrayList problems, IProblemLocation problem) {
		for (int i= 0; i < problems.size(); i++) {
			IProblemLocation existing= (IProblemLocation) problems.get(i);
			if (existing.getProblemId() == problem.getProblemId() && existing.getOffset() == problem.getOffset() && existing.getLength() == problem.getLength()) {
				return true;
			}
		}

		return false;
	}

	private static boolean contains(int[] ids, int id) {
		for (int i= 0; i < ids.length; i++) {
			if (ids[i] == id)
				return true;
		}
		return false;
	}

}

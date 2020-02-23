/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;


public abstract class AbstractMultiFix extends AbstractCleanUp implements IMultiFix {

	protected AbstractMultiFix() {
	}

	protected AbstractMultiFix(Map<String, String> settings) {
		super(settings);
	}

	@Override
	public final ICleanUpFix createFix(CleanUpContext context) throws CoreException {
		CompilationUnit unit= context.getAST();
		if (unit == null)
			return null;

		if (context instanceof MultiFixContext) {
			return createFix(unit, ((MultiFixContext)context).getProblemLocations());
		} else {
			return createFix(unit);
		}
	}

	protected abstract ICleanUpFix createFix(CompilationUnit unit) throws CoreException;

	protected abstract ICleanUpFix createFix(CompilationUnit unit, IProblemLocation[] problems) throws CoreException;

	@Override
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
		for (IProblem problem : problems) {
			if (problem.getID() == problemId) {
				result++;
			}
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
		ArrayList<IProblemLocation> result= new ArrayList<>();

		for (IProblemLocation problem : problems) {
			if (contains(problemIds, problem.getProblemId()) && !contains(result, problem)) {
				result.add(problem);
			}
		}

		return result.toArray(new IProblemLocation[result.size()]);
	}

	private static boolean contains(ArrayList<IProblemLocation> problems, IProblemLocation problem) {
		for (IProblemLocation existing : problems) {
			if (existing.getProblemId() == problem.getProblemId() && existing.getOffset() == problem.getOffset() && existing.getLength() == problem.getLength()) {
				return true;
			}
		}

		return false;
	}

	private static boolean contains(int[] ids, int id) {
		for (int id2 : ids) {
			if (id2 == id)
				return true;
		}
		return false;
	}
}

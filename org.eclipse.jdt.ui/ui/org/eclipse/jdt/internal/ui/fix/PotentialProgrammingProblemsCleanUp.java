/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Red Hat Inc. - modified to use PotentialProgrammingProblemsCleanUpCore
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.IProblemLocationCore;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocationCore;

public class PotentialProgrammingProblemsCleanUp extends AbstractMultiFix {

	private PotentialProgrammingProblemsCleanUpCore coreCleanUp= new PotentialProgrammingProblemsCleanUpCore();

	public PotentialProgrammingProblemsCleanUp(Map<String, String> options) {
		super();
		setOptions(options);
	}

	public PotentialProgrammingProblemsCleanUp() {
		super();
	}

	@Override
	public void setOptions(CleanUpOptions options) {
		coreCleanUp.setOptions(options);
	}

	/**A
	 * {@inheritDoc}
	 */
	@Override
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(coreCleanUp.getRequirementsCore());
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit compilationUnit) throws CoreException {
		ICleanUpFixCore fixCore= coreCleanUp.createFix(compilationUnit);
		return fixCore == null ? null : new CleanUpFixWrapper(fixCore);
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		List<IProblemLocationCore> coreProblemList= new ArrayList<>();
		for (IProblemLocation location : problems) {
			coreProblemList.add((ProblemLocationCore)location);
		}
		ICleanUpFixCore fixCore= coreCleanUp.createFix(compilationUnit, coreProblemList.toArray(new IProblemLocationCore[0]));
		return fixCore == null ? null : new CleanUpFixWrapper(fixCore);
	}

	@Override
	public String[] getStepDescriptions() {
		return coreCleanUp.getStepDescriptions();
	}

	@Override
	public String getPreview() {
		return coreCleanUp.getPreview();
	}

	@Override
	public boolean canFix(ICompilationUnit compilationUnit, IProblemLocation problem) {
		return coreCleanUp.canFix(compilationUnit, (ProblemLocationCore)problem);
	}

	@Override
	public RefactoringStatus checkPreConditions(IJavaProject project, ICompilationUnit[] compilationUnits, IProgressMonitor monitor) throws CoreException {
		return coreCleanUp.checkPreConditions(project, compilationUnits, monitor);
	}

	@Override
	public RefactoringStatus checkPostConditions(IProgressMonitor monitor) throws CoreException {
		return coreCleanUp.checkPostConditions(monitor);
	}

	@Override
	public int computeNumberOfFixes(CompilationUnit compilationUnit) {
		return coreCleanUp.computeNumberOfFixes(compilationUnit);
	}
}

/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
 *     Red Hat Inc. - core class based on PotentialProgrammingProblemsCleanUp
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.CleanUpRequirementsCore;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.PotentialProgrammingProblemsFixCore;

import org.eclipse.jdt.internal.ui.text.correction.IProblemLocationCore;

public class PotentialProgrammingProblemsCleanUpCore extends AbstractMultiFixCore {

	public PotentialProgrammingProblemsCleanUpCore(Map<String, String> options) {
		super(options);
	}

	public PotentialProgrammingProblemsCleanUpCore() {
		super();
	}

	/**A
	 * {@inheritDoc}
	 */
	@Override
	public CleanUpRequirementsCore getRequirementsCore() {
		boolean requireAST= requireAST();
		Map<String, String> requiredOptions= requireAST ? getRequiredOptions() : null;
		return new CleanUpRequirementsCore(requireAST, false, false, requiredOptions);
	}

	private boolean requireAST() {
		boolean addSUID= isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID);
		if (!addSUID)
			return false;

		return isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED) ||
		       isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_DEFAULT);
	}

	@Override
	public ICleanUpFixCore createFix(CompilationUnit compilationUnit) throws CoreException {

		boolean addSUID= isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID);
		if (!addSUID)
			return null;

		return PotentialProgrammingProblemsFixCore.createCleanUp(compilationUnit,
				isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED) ||
				isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_DEFAULT));
	}

	@Override
	public ICleanUpFixCore createFix(CompilationUnit compilationUnit, IProblemLocationCore[] problems) throws CoreException {
		if (compilationUnit == null)
			return null;

		return PotentialProgrammingProblemsFixCore.createCleanUp(compilationUnit, problems,
				(isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED)) ||
				(isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_DEFAULT)));
	}

	private Map<String, String> getRequiredOptions() {
		Map<String, String> result= new Hashtable<>();
		if ((isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED)) ||
				(isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_DEFAULT)))
			result.put(JavaCore.COMPILER_PB_MISSING_SERIAL_VERSION, JavaCore.WARNING);
		return result;
	}

	@Override
	public String[] getStepDescriptions() {
		List<String> result= new ArrayList<>();

		if ((isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED)))
			result.add(MultiFixMessages.SerialVersionCleanUp_Generated_description);
		if ((isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_DEFAULT)))
			result.add(MultiFixMessages.CodeStyleCleanUp_addDefaultSerialVersionId_description);

		return result.toArray(new String[result.size()]);
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();

		bld.append("class E implements java.io.Serializable {\n"); //$NON-NLS-1$
		if ((isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED))) {
			bld.append("    private static final long serialVersionUID = -391484377137870342L;\n"); //$NON-NLS-1$
			bld.append("}\n"); //$NON-NLS-1$
		} else if ((isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_DEFAULT))) {
			bld.append("    private static final long serialVersionUID = 1L;\n"); //$NON-NLS-1$
			bld.append("}\n"); //$NON-NLS-1$
		} else {
			bld.append("}\n\n"); //$NON-NLS-1$
		}

		return bld.toString();
	}

	@Override
	public boolean canFix(ICompilationUnit compilationUnit, IProblemLocationCore problem) {
		if (problem.getProblemId() == IProblem.MissingSerialVersion)
			return (isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED))
				|| (isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_DEFAULT));

		return false;
	}

	@Override
	public RefactoringStatus checkPreConditions(IJavaProject project, ICompilationUnit[] compilationUnits, IProgressMonitor monitor) throws CoreException {
		RefactoringStatus superStatus= super.checkPreConditions(project, compilationUnits, monitor);
		if (superStatus.hasFatalError())
			return superStatus;

		return PotentialProgrammingProblemsFixCore.checkPreConditions(project, compilationUnits, monitor,
				isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED),
				isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_DEFAULT),
				false);
	}

	@Override
	public RefactoringStatus checkPostConditions(IProgressMonitor monitor) throws CoreException {
		return PotentialProgrammingProblemsFixCore.checkPostConditions(monitor);
	}

	@Override
	public int computeNumberOfFixes(CompilationUnit compilationUnit) {
		if ((isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED)) ||
				(isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID) && isEnabled(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_DEFAULT)))
			return getNumberOfProblems(compilationUnit.getProblems(), IProblem.MissingSerialVersion);

		return 0;
	}
}

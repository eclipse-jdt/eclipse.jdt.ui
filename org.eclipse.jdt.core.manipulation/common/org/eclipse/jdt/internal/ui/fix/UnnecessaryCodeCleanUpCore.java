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
 *     Red Hat Inc. - created core class from UnnecessaryCodeCleanUp
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.CleanUpRequirementsCore;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.UnusedCodeFixCore;

import org.eclipse.jdt.internal.ui.text.correction.IProblemLocationCore;

public class UnnecessaryCodeCleanUpCore extends AbstractMultiFixCore {

	public UnnecessaryCodeCleanUpCore(Map<String, String> options) {
		super(options);
	}

	public UnnecessaryCodeCleanUpCore() {
		super();
	}

	@Override
	public CleanUpRequirementsCore getRequirementsCore() {
		boolean requireAST= isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);
		Map<String, String> requiredOptions= requireAST ? getRequiredOptions() : null;
		return new CleanUpRequirementsCore(requireAST, false, false, requiredOptions);
	}

	@Override
	public ICleanUpFixCore createFix(CompilationUnit compilationUnit) throws CoreException {
		return UnusedCodeFixCore.createCleanUp(compilationUnit,
				false,
				false,
				false,
				false,
				false,
				false,
				isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_CASTS),
				false);
	}

	@Override
	public ICleanUpFixCore createFix(CompilationUnit compilationUnit, IProblemLocationCore[] problems) throws CoreException {
		return UnusedCodeFixCore.createCleanUp(compilationUnit, problems,
				false,
				false,
				false,
				false,
				false,
				false,
				isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_CASTS),
				false);
	}

	private Map<String, String> getRequiredOptions() {
		Map<String, String> result= new Hashtable<>();

		if (isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_CASTS))
			result.put(JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK, JavaCore.WARNING);

		if (isEnabled(CleanUpConstants.REMOVE_UNUSED_CODE_METHOD_PARAMETERS)) {
			result.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER, JavaCore.WARNING);
		}

		return result;
	}

	@Override
	public String[] getStepDescriptions() {
		List<String> result= new ArrayList<>();
		if (isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_CASTS))
			result.add(MultiFixMessages.UnusedCodeCleanUp_RemoveUnusedCasts_description);
		return result.toArray(new String[result.size()]);
	}

	@Override
	public String getPreview() {
		StringBuilder buf= new StringBuilder();

		if (isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_CASTS)) {
			buf.append("Boolean b= Boolean.TRUE;\n"); //$NON-NLS-1$
		} else {
			buf.append("Boolean b= (Boolean) Boolean.TRUE;\n"); //$NON-NLS-1$
		}

		return buf.toString();
	}

	@Override
	public boolean canFix(ICompilationUnit compilationUnit, IProblemLocationCore problem) {
		if (problem.getProblemId() == IProblem.UnnecessaryCast)
			return isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		return false;
	}

	@Override
	public int computeNumberOfFixes(CompilationUnit compilationUnit) {
		int result= 0;
		IProblem[] problems= compilationUnit.getProblems();
		if (isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_CASTS))
			result+= getNumberOfProblems(problems, IProblem.UnnecessaryCast);
		return result;
	}
}

/*******************************************************************************
 * Copyright (c) 2014, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
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

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.TypeParametersFix;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

public class TypeParametersCleanUp extends AbstractMultiFix {

	private Map<String, String> fOptions;

	public TypeParametersCleanUp(Map<String, String> options) {
		super(options);
		fOptions= options;
	}

	public TypeParametersCleanUp() {
		super();
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.INSERT_INFERRED_TYPE_ARGUMENTS) || isEnabled(CleanUpConstants.REMOVE_REDUNDANT_TYPE_ARGUMENTS);
		Map<String, String> requiredOptions= requireAST ? getRequiredOptions() : null;
		return new CleanUpRequirements(requireAST, false, false, requiredOptions);
	}

	private Map<String, String> getRequiredOptions() {
		Map<String, String> result= new Hashtable<>();

		if (isEnabled(CleanUpConstants.REMOVE_REDUNDANT_TYPE_ARGUMENTS))
			result.put(JavaCore.COMPILER_PB_REDUNDANT_TYPE_ARGUMENTS, JavaCore.WARNING);

		return result;
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;

		return TypeParametersFix.createCleanUp(compilationUnit,
				isEnabled(CleanUpConstants.INSERT_INFERRED_TYPE_ARGUMENTS),
				isEnabled(CleanUpConstants.REMOVE_REDUNDANT_TYPE_ARGUMENTS));
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		if (compilationUnit == null)
			return null;

		return TypeParametersFix.createCleanUp(compilationUnit, problems,
				isEnabled(CleanUpConstants.INSERT_INFERRED_TYPE_ARGUMENTS),
				isEnabled(CleanUpConstants.REMOVE_REDUNDANT_TYPE_ARGUMENTS));
	}

	@Override
	public String[] getStepDescriptions() {
		List<String> result= new ArrayList<>();
		if (isEnabled(CleanUpConstants.INSERT_INFERRED_TYPE_ARGUMENTS)) {
			result.add(MultiFixMessages.TypeParametersCleanUp_InsertInferredTypeArguments_description);
		} else if (isEnabled(CleanUpConstants.REMOVE_REDUNDANT_TYPE_ARGUMENTS)) {
			result.add(MultiFixMessages.TypeParametersCleanUp_RemoveUnnecessaryTypeArguments_description);
		}

		return result.toArray(new String[result.size()]);
	}

	@Override
	public boolean canFix(ICompilationUnit compilationUnit, IProblemLocation problem) {
		int problemId= problem.getProblemId();

		if (problemId == IProblem.RedundantSpecificationOfTypeArguments)
			return isEnabled(CleanUpConstants.REMOVE_REDUNDANT_TYPE_ARGUMENTS);
		if (problemId == IProblem.DiamondNotBelow17)
			return isEnabled(CleanUpConstants.INSERT_INFERRED_TYPE_ARGUMENTS);


		return false;
	}

	@Override
	public int computeNumberOfFixes(CompilationUnit compilationUnit) {
		if (fOptions == null)
			return 0;
		int result= 0;
		IProblem[] problems= compilationUnit.getProblems();
		if (isEnabled(CleanUpConstants.REMOVE_REDUNDANT_TYPE_ARGUMENTS))
			result= getNumberOfProblems(problems, IProblem.RedundantSpecificationOfTypeArguments);
		else if (isEnabled(CleanUpConstants.INSERT_INFERRED_TYPE_ARGUMENTS))
			result= getNumberOfProblems(problems, IProblem.DiamondNotBelow17);
		return result;

	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.REMOVE_REDUNDANT_TYPE_ARGUMENTS)) {
			return "Map<Integer, String> map= new HashMap<>();\n"; //$NON-NLS-1$
		}

		return "Map<Integer, String> map= new HashMap<Integer, String>();\n"; //$NON-NLS-1$
	}
}

/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.ExpensiveTypeInferenceFixCore;
import org.eclipse.jdt.internal.corext.fix.FixMessages;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

public class ExpensiveTypeInferenceCleanUpCore extends AbstractMultiFix {
	public ExpensiveTypeInferenceCleanUpCore() {
	}

	public ExpensiveTypeInferenceCleanUpCore(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean enabled= isEnabled(CleanUpConstants.EXPLICIT_TYPE_ARGUMENTS_FOR_EXPENSIVE_INFERENCE);
		Map<String, String> options= new HashMap<>();
		options.put(JavaCore.COMPILER_PB_POTENTIALLY_EXPENSIVE_TYPE_INFERENCE, JavaCore.WARNING);
		return new CleanUpRequirements(enabled, false, false, enabled ? options : null);
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit root) {
		return createFix(root, convertProblems(root.getProblems()));
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit root, IProblemLocation[] problems) {
		return isEnabled(CleanUpConstants.EXPLICIT_TYPE_ARGUMENTS_FOR_EXPENSIVE_INFERENCE) ? ExpensiveTypeInferenceFixCore.createFix(root, problems) : null;
	}

	@Override
	public boolean canFix(ICompilationUnit cu, IProblemLocation problem) {
		return problem.getProblemId() == IProblem.PotentiallyExpensiveTypeInference && isEnabled(CleanUpConstants.EXPLICIT_TYPE_ARGUMENTS_FOR_EXPENSIVE_INFERENCE);
	}

	@Override
	public int computeNumberOfFixes(CompilationUnit root) {
		return isEnabled(CleanUpConstants.EXPLICIT_TYPE_ARGUMENTS_FOR_EXPENSIVE_INFERENCE)
				? ExpensiveTypeInferenceFixCore.countFixes(root, convertProblems(root.getProblems())) : 0;
	}

	@Override
	public String[] getStepDescriptions() {
		return isEnabled(CleanUpConstants.EXPLICIT_TYPE_ARGUMENTS_FOR_EXPENSIVE_INFERENCE) ? new String[] { FixMessages.ExpensiveTypeInferenceFix_description } : new String[0];
	}

	@Override
	public String getPreview() {
		String witness= isEnabled(CleanUpConstants.EXPLICIT_TYPE_ARGUMENTS_FOR_EXPENSIVE_INFERENCE) ? "<List<String>>" : ""; //$NON-NLS-1$ //$NON-NLS-2$
		return "List<List<String>> data = Arrays." + witness + "asList(Arrays.asList(\"a\"), /* ... */ Arrays.asList(\"z\"));\n"; //$NON-NLS-1$ //$NON-NLS-2$
	}
}

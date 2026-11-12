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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.CorrectionEngine;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2Core;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.ExpensiveTypeInferenceFixCore;
import org.eclipse.jdt.internal.corext.fix.FixMessages;
import org.eclipse.jdt.internal.corext.fix.SuppressWarningsFixCore;

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
		return isEnabled(CleanUpConstants.EXPLICIT_TYPE_ARGUMENTS_FOR_EXPENSIVE_INFERENCE)
				? ExpensiveTypeInferenceFixCore.createFix(root, getUnsuppressedProblems(root, problems)) : null;
	}

	private static IProblemLocation[] getUnsuppressedProblems(CompilationUnit root, IProblemLocation[] problems) {
		// The shared clean-up AST exposes suppressed warnings for other marker-based clean ups.
		String token= CorrectionEngine.getWarningToken(IProblem.PotentiallyExpensiveTypeInference);
		return Arrays.stream(problems).filter(problem -> problem.getProblemId() == IProblem.PotentiallyExpensiveTypeInference)
				.filter(problem -> !isSuppressed(problem.getCoveredNode(root), token)).toArray(IProblemLocation[]::new);
	}

	private static boolean isSuppressed(ASTNode location, String token) {
		for (ASTNode node= location; node != null; node= node.getParent()) {
			ChildListPropertyDescriptor property= SuppressWarningsFixCore.getChildListPropertyDescriptor(node, token);
			if (property == null) {
				continue;
			}
			@SuppressWarnings("unchecked")
			List<IExtendedModifier> modifiers= (List<IExtendedModifier>) node.getStructuralProperty(property);
			Annotation annotation= StubUtility2Core.findAnnotation("java.lang.SuppressWarnings", modifiers); //$NON-NLS-1$
			IAnnotationBinding binding= annotation == null ? null : annotation.resolveAnnotationBinding();
			if (binding != null) {
				for (IMemberValuePairBinding pair : binding.getDeclaredMemberValuePairs()) {
					if (pair.getValue() instanceof Object[] values
							&& Arrays.stream(values).anyMatch(value -> token.equals(value) || "all".equals(value))) { //$NON-NLS-1$
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean canFix(ICompilationUnit cu, IProblemLocation problem) {
		return problem.getProblemId() == IProblem.PotentiallyExpensiveTypeInference && isEnabled(CleanUpConstants.EXPLICIT_TYPE_ARGUMENTS_FOR_EXPENSIVE_INFERENCE);
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

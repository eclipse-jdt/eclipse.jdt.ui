/*******************************************************************************
 * Copyright (c) 2020, 2022 Fabrice TIERCELIN and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fabrice TIERCELIN - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Collections;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.LambdaExpressionAndMethodRefFixCore;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that simplifies the lambda expression and the method reference syntax:
 * <ul>
 * <li>Parenthesis are not needed for a single untyped parameter,</li>
 * <li>Return statement is not needed for a single expression,</li>
 * <li>Brackets are not needed for a single statement,</li>
 * <li>A lambda expression can be replaced by a creation or a method reference in some cases.</li>
 * </ul>
 */
public class LambdaExpressionAndMethodRefCleanUp extends AbstractMultiFix {
	public LambdaExpressionAndMethodRefCleanUp() {
		this(Collections.emptyMap());
	}

	public LambdaExpressionAndMethodRefCleanUp(final Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF);
		Map<String, String> requiredOptions= null;
		return new CleanUpRequirements(requireAST, false, false, requiredOptions);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF)) {
			return new String[] { MultiFixMessages.LambdaExpressionAndMethodRefCleanUp_description };
		}
		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF)) {
			return """
				someString -> someString.trim().toLowerCase();
				someString -> someString.trim().toLowerCase();
				someString -> (someString.trim().toLowerCase() + "bar");
				ArrayList::new;
				Date::getTime;
				"""; //$NON-NLS-1$
		}

		return """
			(someString) -> someString.trim().toLowerCase();
			someString -> {return someString.trim().toLowerCase();};
			someString -> {return someString.trim().toLowerCase() + "bar";};
			() -> new ArrayList<>();
			date -> date.getTime();
			"""; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF) || !JavaModelUtil.is1d8OrHigher(unit.getJavaElement().getJavaProject())) {
			return null;
		}

		ICleanUpFix cleanUpFixCore= LambdaExpressionAndMethodRefFixCore.createCleanUp(unit);
		return cleanUpFixCore;
	}

	@Override
	public boolean canFix(final ICompilationUnit compilationUnit, final IProblemLocation problem) {
		return false;
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit, IProblemLocation[] problems) throws CoreException {
		return null;
	}
}

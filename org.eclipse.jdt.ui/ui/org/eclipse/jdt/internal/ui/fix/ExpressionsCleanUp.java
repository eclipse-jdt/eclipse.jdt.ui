/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.ExpressionsFix;

import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public class ExpressionsCleanUp extends AbstractCleanUp {

	public ExpressionsCleanUp(Map<String, String> options) {
		super(options);
	}

	public ExpressionsCleanUp() {
		super();
	}

	@Override
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(requireAST(), false, false, null);
	}

	private boolean requireAST() {
		boolean usePrentheses= isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		if (!usePrentheses)
			return false;

		return isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS) ||
		       isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);
	}

	@Override
	public ICleanUpFix createFix(CleanUpContext context) throws CoreException {
		CompilationUnit compilationUnit= context.getAST();
		if (compilationUnit == null)
			return null;

		boolean usePrentheses= isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		if (!usePrentheses)
			return null;

		return ExpressionsFix.createCleanUp(compilationUnit,
				isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS),
				isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER));
	}

	@Override
	public String[] getStepDescriptions() {
		List<String> result= new ArrayList<>();
		if (isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES) && isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS))
			result.add(MultiFixMessages.ExpressionsCleanUp_addParanoiac_description);

		if (isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES) && isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER))
			result.add(MultiFixMessages.ExpressionsCleanUp_removeUnnecessary_description);

		return result.toArray(new String[result.size()]);
	}

	@Override
	public String getPreview() {
		StringBuilder buf= new StringBuilder();

		if (isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES) && isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS)) {
			buf.append("boolean b= (((i > 0) && (i < 10)) || (i == 50));\n"); //$NON-NLS-1$
		} else if (isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES) && isEnabled(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER)) {
			buf.append("boolean b= i > 0 && i < 10 || i == 50;\n"); //$NON-NLS-1$
		} else {
			buf.append("boolean b= (i > 0 && i < 10 || i == 50);\n"); //$NON-NLS-1$
		}

		return buf.toString();
	}

}

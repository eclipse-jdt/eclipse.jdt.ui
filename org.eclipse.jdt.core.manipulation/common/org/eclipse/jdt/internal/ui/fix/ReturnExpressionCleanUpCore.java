/*******************************************************************************
 * Copyright (c) 2021 Fabrice TIERCELIN and others.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.CleanUpContextCore;
import org.eclipse.jdt.core.manipulation.CleanUpRequirementsCore;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.ReturnExpressionFixCore;

public class ReturnExpressionCleanUpCore extends AbstractCleanUpCore {
	public ReturnExpressionCleanUpCore(final Map<String, String> options) {
		super(options);
	}

	public ReturnExpressionCleanUpCore() {
	}

	@Override
	public CleanUpRequirementsCore getRequirementsCore() {
		return new CleanUpRequirementsCore(requireAST(), false, false, null);
	}

	public boolean requireAST() {
		return isEnabled(CleanUpConstants.RETURN_EXPRESSION);
	}

	@Override
	public ICleanUpFixCore createFixCore(final CleanUpContextCore context) throws CoreException {
		CompilationUnit compilationUnit= context.getAST();

		if (compilationUnit == null || !isEnabled(CleanUpConstants.RETURN_EXPRESSION)) {
			return null;
		}

		return ReturnExpressionFixCore.createCleanUp(compilationUnit);
	}

	@Override
	public String[] getStepDescriptions() {
		List<String> result= new ArrayList<>();

		if (isEnabled(CleanUpConstants.RETURN_EXPRESSION)) {
			result.add(MultiFixMessages.ReturnExpressionCleanUp_description);
		}

		return result.toArray(new String[0]);
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();
		bld.append("public int getNumber() {\n"); //$NON-NLS-1$

		if (isEnabled(CleanUpConstants.RETURN_EXPRESSION)) {
			bld.append("    return 0;\n"); //$NON-NLS-1$
		} else {
			bld.append("    int i = 0;\n"); //$NON-NLS-1$
			bld.append("    return i;\n"); //$NON-NLS-1$
		}

		bld.append("}\n"); //$NON-NLS-1$

		if (isEnabled(CleanUpConstants.RETURN_EXPRESSION)) {
			bld.append("\n"); //$NON-NLS-1$
		}

		return bld.toString();
	}
}

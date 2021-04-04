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

import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.CleanUpContextCore;
import org.eclipse.jdt.core.manipulation.CleanUpRequirementsCore;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.DoWhileRatherThanWhileFixCore;

public class DoWhileRatherThanWhileCleanUpCore extends AbstractCleanUpCore {
	public DoWhileRatherThanWhileCleanUpCore(final Map<String, String> options) {
		super(options);
	}

	public DoWhileRatherThanWhileCleanUpCore() {
	}

	@Override
	public CleanUpRequirementsCore getRequirementsCore() {
		return new CleanUpRequirementsCore(requireAST(), false, false, null);
	}

	public boolean requireAST() {
		return isEnabled(CleanUpConstants.DO_WHILE_RATHER_THAN_WHILE);
	}

	@Override
	public ICleanUpFixCore createFixCore(final CleanUpContextCore context) throws CoreException {
		CompilationUnit compilationUnit= context.getAST();

		if (compilationUnit == null || !isEnabled(CleanUpConstants.DO_WHILE_RATHER_THAN_WHILE)) {
			return null;
		}

		return DoWhileRatherThanWhileFixCore.createCleanUp(compilationUnit);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.DO_WHILE_RATHER_THAN_WHILE)) {
			return new String[] {MultiFixMessages.DoWhileRatherThanWhileCleanUp_description};
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();

		if (isEnabled(CleanUpConstants.DO_WHILE_RATHER_THAN_WHILE)) {
			bld.append("do {\n"); //$NON-NLS-1$
		} else {
			bld.append("while (true) {\n"); //$NON-NLS-1$
		}

		bld.append("    if (i > 100) {\n"); //$NON-NLS-1$
		bld.append("        return;\n"); //$NON-NLS-1$
		bld.append("    }\n"); //$NON-NLS-1$
		bld.append("    i *= 2;\n"); //$NON-NLS-1$

		if (isEnabled(CleanUpConstants.DO_WHILE_RATHER_THAN_WHILE)) {
			bld.append("} while (true);\n"); //$NON-NLS-1$
		} else {
			bld.append("}\n"); //$NON-NLS-1$
		}

		return bld.toString();
	}
}

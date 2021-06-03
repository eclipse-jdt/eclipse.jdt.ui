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
import org.eclipse.jdt.internal.corext.fix.PullOutIfFromIfElseFixCore;

public class PullOutIfFromIfElseCleanUpCore extends AbstractCleanUpCore {
	public PullOutIfFromIfElseCleanUpCore(final Map<String, String> options) {
		super(options);
	}

	public PullOutIfFromIfElseCleanUpCore() {
	}

	@Override
	public CleanUpRequirementsCore getRequirementsCore() {
		return new CleanUpRequirementsCore(requireAST(), false, false, null);
	}

	public boolean requireAST() {
		return isEnabled(CleanUpConstants.PULL_OUT_IF_FROM_IF_ELSE);
	}

	@Override
	public ICleanUpFixCore createFixCore(final CleanUpContextCore context) throws CoreException {
		CompilationUnit compilationUnit= context.getAST();

		if (compilationUnit == null || !isEnabled(CleanUpConstants.PULL_OUT_IF_FROM_IF_ELSE)) {
			return null;
		}

		return PullOutIfFromIfElseFixCore.createCleanUp(compilationUnit);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.PULL_OUT_IF_FROM_IF_ELSE)) {
			return new String[] {MultiFixMessages.PullOutIfFromIfElseCleanUp_description};
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.PULL_OUT_IF_FROM_IF_ELSE)) {
			return "" //$NON-NLS-1$
					+ "if (isActive) {\n" //$NON-NLS-1$
					+ "    if (isFound) {\n" //$NON-NLS-1$
					+ "        System.out.println(\"foo\");\n" //$NON-NLS-1$
					+ "    } else {\n" //$NON-NLS-1$
					+ "        System.out.println(\"bar\");\n" //$NON-NLS-1$
					+ "    }\n" //$NON-NLS-1$
					+ "}\n\n\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "if (isFound) {\n" //$NON-NLS-1$
				+ "    if (isActive) {\n" //$NON-NLS-1$
				+ "        System.out.println(\"foo\");\n" //$NON-NLS-1$
				+ "    }\n" //$NON-NLS-1$
				+ "} else {\n" //$NON-NLS-1$
				+ "    if (isActive) {\n" //$NON-NLS-1$
				+ "        System.out.println(\"bar\");\n" //$NON-NLS-1$
				+ "    }\n" //$NON-NLS-1$
				+ "}\n"; //$NON-NLS-1$
	}
}

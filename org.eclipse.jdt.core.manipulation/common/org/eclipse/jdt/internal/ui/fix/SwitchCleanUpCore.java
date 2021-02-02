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
import org.eclipse.jdt.internal.corext.fix.SwitchFixCore;

public class SwitchCleanUpCore extends AbstractCleanUpCore {
	public SwitchCleanUpCore(final Map<String, String> options) {
		super(options);
	}

	public SwitchCleanUpCore() {
	}

	@Override
	public CleanUpRequirementsCore getRequirementsCore() {
		return new CleanUpRequirementsCore(requireAST(), false, false, null);
	}

	public boolean requireAST() {
		return isEnabled(CleanUpConstants.USE_SWITCH);
	}

	@Override
	public ICleanUpFixCore createFixCore(final CleanUpContextCore context) throws CoreException {
		CompilationUnit compilationUnit= context.getAST();

		if (compilationUnit == null) {
			return null;
		}

		boolean convertIfElseChainToSwitch= isEnabled(CleanUpConstants.USE_SWITCH);

		if (!convertIfElseChainToSwitch) {
			return null;
		}

		return SwitchFixCore.createCleanUp(compilationUnit);
	}

	@Override
	public String[] getStepDescriptions() {
		List<String> result= new ArrayList<>();

		if (isEnabled(CleanUpConstants.USE_SWITCH)) {
			result.add(MultiFixMessages.CodeStyleCleanUp_Switch_description);
		}

		return result.toArray(new String[0]);
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.USE_SWITCH)) {
			return "" //$NON-NLS-1$
					+ "switch (number) {\n" //$NON-NLS-1$
					+ "case 0:\n" //$NON-NLS-1$
					+ "  i = 0;\n" //$NON-NLS-1$
					+ "  break;\n" //$NON-NLS-1$
					+ "case 1:\n" //$NON-NLS-1$
					+ "  j = 10;\n" //$NON-NLS-1$
					+ "  break;\n" //$NON-NLS-1$
					+ "case 2:\n" //$NON-NLS-1$
					+ "  k = 20;\n" //$NON-NLS-1$
					+ "  break;\n" //$NON-NLS-1$
					+ "default:\n" //$NON-NLS-1$
					+ "  m = -1;\n" //$NON-NLS-1$
					+ "  break;\n" //$NON-NLS-1$
					+ "}\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "if (number == 0) {\n" //$NON-NLS-1$
				+ "    i = 0;\n" //$NON-NLS-1$
				+ "} else if (number == 1) {\n" //$NON-NLS-1$
				+ "    j = 10;\n" //$NON-NLS-1$
				+ "} else if (2 == number) {\n" //$NON-NLS-1$
				+ "    k = 20;\n" //$NON-NLS-1$
				+ "} else {\n" //$NON-NLS-1$
				+ "    m = -1;\n" //$NON-NLS-1$
				+ "}\n\n\n\n\n\n"; //$NON-NLS-1$
	}
}

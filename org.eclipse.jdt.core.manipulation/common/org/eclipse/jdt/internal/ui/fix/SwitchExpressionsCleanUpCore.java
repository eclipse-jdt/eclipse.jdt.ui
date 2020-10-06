/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
 *     Red Hat Inc. - created by modifying LambdaExpresionsCleanUpCore
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
import org.eclipse.jdt.internal.corext.fix.SwitchExpressionsFixCore;

public class SwitchExpressionsCleanUpCore extends AbstractCleanUpCore {
	public SwitchExpressionsCleanUpCore(Map<String, String> options) {
		super(options);
	}

	public SwitchExpressionsCleanUpCore() {
	}

	@Override
	public CleanUpRequirementsCore getRequirementsCore() {
		return new CleanUpRequirementsCore(requireAST(), false, false, null);
	}

	public boolean requireAST() {
		return isEnabled(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);
	}

	@Override
	public ICleanUpFixCore createFixCore(CleanUpContextCore context) throws CoreException {
		CompilationUnit compilationUnit= context.getAST();
		if (compilationUnit == null)
			return null;

		boolean convertToSwitchExpressions= isEnabled(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);
		if (!convertToSwitchExpressions)
			return null;

		return SwitchExpressionsFixCore.createCleanUp(compilationUnit);
	}

	@Override
	public String[] getStepDescriptions() {
		List<String> result= new ArrayList<>();
		if (isEnabled(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS))
			result.add(MultiFixMessages.SwitchExpressionsCleanUp_ConvertToSwitchExpressions_description);

		return result.toArray(new String[0]);
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS)) {
			return "" //$NON-NLS-1$
					+ "int i = switch(j) {\n" //$NON-NLS-1$
					+ "    case 1 -> 3;\n" //$NON-NLS-1$
					+ "    case 2 -> 4;\n" //$NON-NLS-1$
					+ "    default -> 0;\n" //$NON-NLS-1$
					+ "};\n\n\n\n\n\n\n\n\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "int i;\n" //$NON-NLS-1$
				+ "switch(j) {\n" //$NON-NLS-1$
				+ "    case 1:\n" //$NON-NLS-1$
				+ "        i = 3;\n" //$NON-NLS-1$
				+ "        break;\n" //$NON-NLS-1$
				+ "    case 2:\n" //$NON-NLS-1$
				+ "        i = 4;\n" //$NON-NLS-1$
				+ "        break;\n" //$NON-NLS-1$
				+ "    default:\n" //$NON-NLS-1$
				+ "        i = 0;\n" //$NON-NLS-1$
				+ "        break;\n" //$NON-NLS-1$
				+ "}\n" //$NON-NLS-1$
				+ "\n"; //$NON-NLS-1$
	}
}

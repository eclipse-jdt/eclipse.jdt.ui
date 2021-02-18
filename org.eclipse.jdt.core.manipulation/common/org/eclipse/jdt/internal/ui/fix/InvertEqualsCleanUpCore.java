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
import org.eclipse.jdt.internal.corext.fix.InvertEqualsFixCore;

public class InvertEqualsCleanUpCore extends AbstractCleanUpCore {
	public InvertEqualsCleanUpCore(final Map<String, String> options) {
		super(options);
	}

	public InvertEqualsCleanUpCore() {
	}

	@Override
	public CleanUpRequirementsCore getRequirementsCore() {
		return new CleanUpRequirementsCore(requireAST(), false, false, null);
	}

	public boolean requireAST() {
		return isEnabled(CleanUpConstants.INVERT_EQUALS);
	}

	@Override
	public ICleanUpFixCore createFixCore(final CleanUpContextCore context) throws CoreException {
		CompilationUnit compilationUnit= context.getAST();

		if (compilationUnit == null || !isEnabled(CleanUpConstants.INVERT_EQUALS)) {
			return null;
		}

		return InvertEqualsFixCore.createCleanUp(compilationUnit);
	}

	@Override
	public String[] getStepDescriptions() {
		List<String> result= new ArrayList<>();

		if (isEnabled(CleanUpConstants.INVERT_EQUALS)) {
			result.add(MultiFixMessages.InvertEqualsCleanUp_description);
		}

		return result.toArray(new String[0]);
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.INVERT_EQUALS)) {
			return "" //$NON-NLS-1$
					+ "boolean result = \"foo\".equals(text);\n" //$NON-NLS-1$
					+ "boolean result2 = (text1 + text2).equals(text);\n" //$NON-NLS-1$
					+ "boolean result3 = DayOfWeek.MONDAY.equals(object);\n" //$NON-NLS-1$
					+ "boolean result4 = \"foo\".equalsIgnoreCase(text);\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "boolean result = text.equals(\"foo\");\n" //$NON-NLS-1$
				+ "boolean result2 = text.equals(text1 + text2);\n" //$NON-NLS-1$
				+ "boolean result3 = object.equals(DayOfWeek.MONDAY);\n" //$NON-NLS-1$
				+ "boolean result4 = text.equalsIgnoreCase(\"foo\");\n"; //$NON-NLS-1$
	}
}

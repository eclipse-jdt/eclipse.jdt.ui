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

import org.eclipse.jdt.internal.corext.fix.BooleanValueRatherThanComparisonFixCore;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public class BooleanValueRatherThanComparisonCleanUpCore extends AbstractCleanUp {
	public BooleanValueRatherThanComparisonCleanUpCore(final Map<String, String> options) {
		super(options);
	}

	public BooleanValueRatherThanComparisonCleanUpCore() {
	}

	@Override
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(requireAST(), false, false, null);
	}

	public boolean requireAST() {
		return isEnabled(CleanUpConstants.BOOLEAN_VALUE_RATHER_THAN_COMPARISON);
	}

	@Override
	public ICleanUpFix createFix(final CleanUpContext context) throws CoreException {
		CompilationUnit compilationUnit= context.getAST();

		if (compilationUnit == null || !isEnabled(CleanUpConstants.BOOLEAN_VALUE_RATHER_THAN_COMPARISON)) {
			return null;
		}

		return BooleanValueRatherThanComparisonFixCore.createCleanUp(compilationUnit);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.BOOLEAN_VALUE_RATHER_THAN_COMPARISON)) {
			return new String[] {MultiFixMessages.BooleanValueRatherThanComparisonCleanUp_description};
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.BOOLEAN_VALUE_RATHER_THAN_COMPARISON)) {
			return """
				boolean booleanValue = isValid;
				boolean booleanValue2 = !isValid;
				boolean booleanValue3 = i <= 0;
				boolean booleanValue4 = !booleanObject;
				"""; //$NON-NLS-1$
		}

		return """
			boolean booleanValue = isValid == true;
			boolean booleanValue2 = isValid == false;
			boolean booleanValue3 = Boolean.FALSE.equals(i > 0);
			boolean booleanValue4 = booleanObject.equals(Boolean.FALSE);
			"""; //$NON-NLS-1$
	}
}

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
import org.eclipse.jdt.internal.corext.fix.PrimitiveShortRatherThanWrapperFixCore;

public class PrimitiveShortRatherThanWrapperCleanUpCore extends AbstractCleanUpCore {
	public PrimitiveShortRatherThanWrapperCleanUpCore(final Map<String, String> options) {
		super(options);
	}

	public PrimitiveShortRatherThanWrapperCleanUpCore() {
	}

	@Override
	public CleanUpRequirementsCore getRequirementsCore() {
		return new CleanUpRequirementsCore(requireAST(), false, false, null);
	}

	public boolean requireAST() {
		return isEnabled(CleanUpConstants.PRIMITIVE_RATHER_THAN_WRAPPER);
	}

	@Override
	public ICleanUpFixCore createFixCore(final CleanUpContextCore context) throws CoreException {
		CompilationUnit compilationUnit= context.getAST();

		if (compilationUnit == null || !isEnabled(CleanUpConstants.PRIMITIVE_RATHER_THAN_WRAPPER)) {
			return null;
		}

		return PrimitiveShortRatherThanWrapperFixCore.createCleanUp(compilationUnit);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.PRIMITIVE_RATHER_THAN_WRAPPER)) {
			return new String[] {MultiFixMessages.PrimitiveShortRatherThanWrapperCleanUp_description};
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();

		if (isEnabled(CleanUpConstants.PRIMITIVE_RATHER_THAN_WRAPPER)) {
			bld.append("short aShort = Short.MIN_VALUE;\n"); //$NON-NLS-1$
		} else {
			bld.append("Short aShort = Short.MIN_VALUE;\n"); //$NON-NLS-1$
		}

		bld.append("if (aShort > i) {\n"); //$NON-NLS-1$
		bld.append("    System.out.println(\"True!\");\n"); //$NON-NLS-1$
		bld.append("}\n"); //$NON-NLS-1$

		return bld.toString();
	}
}

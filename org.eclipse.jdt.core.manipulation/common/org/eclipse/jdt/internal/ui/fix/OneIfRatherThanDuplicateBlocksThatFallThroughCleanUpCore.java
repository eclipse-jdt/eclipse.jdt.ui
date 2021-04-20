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
import org.eclipse.jdt.internal.corext.fix.OneIfRatherThanDuplicateBlocksThatFallThroughFixCore;

public class OneIfRatherThanDuplicateBlocksThatFallThroughCleanUpCore extends AbstractCleanUpCore {
	public OneIfRatherThanDuplicateBlocksThatFallThroughCleanUpCore(final Map<String, String> options) {
		super(options);
	}

	public OneIfRatherThanDuplicateBlocksThatFallThroughCleanUpCore() {
	}

	@Override
	public CleanUpRequirementsCore getRequirementsCore() {
		return new CleanUpRequirementsCore(requireAST(), false, false, null);
	}

	public boolean requireAST() {
		return isEnabled(CleanUpConstants.ONE_IF_RATHER_THAN_DUPLICATE_BLOCKS_THAT_FALL_THROUGH);
	}

	@Override
	public ICleanUpFixCore createFixCore(final CleanUpContextCore context) throws CoreException {
		CompilationUnit compilationUnit= context.getAST();

		if (compilationUnit == null || !isEnabled(CleanUpConstants.ONE_IF_RATHER_THAN_DUPLICATE_BLOCKS_THAT_FALL_THROUGH)) {
			return null;
		}

		return OneIfRatherThanDuplicateBlocksThatFallThroughFixCore.createCleanUp(compilationUnit);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.ONE_IF_RATHER_THAN_DUPLICATE_BLOCKS_THAT_FALL_THROUGH)) {
			return new String[] {MultiFixMessages.OneIfRatherThanDuplicateBlocksThatFallThroughCleanUp_description};
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();

		if (isEnabled(CleanUpConstants.ONE_IF_RATHER_THAN_DUPLICATE_BLOCKS_THAT_FALL_THROUGH)) {
			bld.append("if (isActive || isValid) {\n"); //$NON-NLS-1$
		} else {
			bld.append("if (isActive) {\n"); //$NON-NLS-1$
		}

		bld.append("    System.out.println(\"The same code\");\n"); //$NON-NLS-1$
		bld.append("    throw new NullPointerException();\n"); //$NON-NLS-1$
		bld.append("}\n"); //$NON-NLS-1$

		if (!isEnabled(CleanUpConstants.ONE_IF_RATHER_THAN_DUPLICATE_BLOCKS_THAT_FALL_THROUGH)) {
			bld.append("if (isValid) {\n"); //$NON-NLS-1$
			bld.append("    System.out.println(\"The same code\");\n"); //$NON-NLS-1$
			bld.append("    throw new NullPointerException();\n"); //$NON-NLS-1$
			bld.append("}\n"); //$NON-NLS-1$
		}

		bld.append("System.out.println(\"Next code\");\n"); //$NON-NLS-1$

		if (isEnabled(CleanUpConstants.ONE_IF_RATHER_THAN_DUPLICATE_BLOCKS_THAT_FALL_THROUGH)) {
			bld.append("\n\n\n\n"); //$NON-NLS-1$
		}

		return bld.toString();
	}
}

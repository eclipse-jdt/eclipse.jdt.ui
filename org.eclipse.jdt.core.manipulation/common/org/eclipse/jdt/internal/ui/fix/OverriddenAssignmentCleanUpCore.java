/*******************************************************************************
 * Copyright (c) 2020, 2024 Fabrice TIERCELIN and others.
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
 *     Red Hat Inc. - refactor to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.OverriddenAssignmentFixCore;

import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

/**
 * A fix that removes passive assignment when the variable is reassigned before being read.
 */
public class OverriddenAssignmentCleanUpCore extends AbstractCleanUp {

	public OverriddenAssignmentCleanUpCore() {
	}

	public OverriddenAssignmentCleanUpCore(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(requireAST(), false, false, null);
	}

	public boolean requireAST() {
		return isEnabled(CleanUpConstants.OVERRIDDEN_ASSIGNMENT);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.OVERRIDDEN_ASSIGNMENT)) {
			return new String[] { MultiFixMessages.OverriddenAssignmentCleanUp_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();

		if (isEnabled(CleanUpConstants.OVERRIDDEN_ASSIGNMENT)) {
			if (isEnabled(CleanUpConstants.OVERRIDDEN_ASSIGNMENT_MOVE_DECL)) {
				bld.append("String separator = System.lineSeparator();\n"); //$NON-NLS-1$
				bld.append("long time = System.currentTimeMillis();\n"); //$NON-NLS-1$
			} else {
				bld.append("long time;\n"); //$NON-NLS-1$
				bld.append("String separator = System.lineSeparator();\n"); //$NON-NLS-1$
				bld.append("time = System.currentTimeMillis();\n"); //$NON-NLS-1$
			}
		} else {
			bld.append("long time = 0;\n"); //$NON-NLS-1$
			bld.append("String separator = \"\";\n"); //$NON-NLS-1$
			bld.append("separator = System.lineSeparator();\n"); //$NON-NLS-1$
			bld.append("time = System.currentTimeMillis();\n"); //$NON-NLS-1$
		}


		return bld.toString();
	}

	@Override
	public ICleanUpFix createFix(final CleanUpContext context) throws CoreException {
		CompilationUnit compilationUnit= context.getAST();

		if (compilationUnit == null || !isEnabled(CleanUpConstants.OVERRIDDEN_ASSIGNMENT)) {
			return null;
		}

		return OverriddenAssignmentFixCore.createCleanUp(compilationUnit, isEnabled(CleanUpConstants.OVERRIDDEN_ASSIGNMENT_MOVE_DECL));
	}

}

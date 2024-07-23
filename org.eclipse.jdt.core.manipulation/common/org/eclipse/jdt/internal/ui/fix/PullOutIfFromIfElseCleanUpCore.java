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

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.PullOutIfFromIfElseFixCore;

import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public class PullOutIfFromIfElseCleanUpCore extends AbstractCleanUp {
	public PullOutIfFromIfElseCleanUpCore(final Map<String, String> options) {
		super(options);
	}

	public PullOutIfFromIfElseCleanUpCore() {
	}

	@Override
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(requireAST(), false, false, null);
	}

	public boolean requireAST() {
		return isEnabled(CleanUpConstants.PULL_OUT_IF_FROM_IF_ELSE);
	}

	@Override
	public ICleanUpFix createFix(final CleanUpContext context) throws CoreException {
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
			return """
				if (isActive) {
				    if (isFound) {
				        System.out.println("foo");
				    } else {
				        System.out.println("bar");
				    }
				}
				
				
				"""; //$NON-NLS-1$
		}

		return """
			if (isFound) {
			    if (isActive) {
			        System.out.println("foo");
			    }
			} else {
			    if (isActive) {
			        System.out.println("bar");
			    }
			}
			"""; //$NON-NLS-1$
	}
}

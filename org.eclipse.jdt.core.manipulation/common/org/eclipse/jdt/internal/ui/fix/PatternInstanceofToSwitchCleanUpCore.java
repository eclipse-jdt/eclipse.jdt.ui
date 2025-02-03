/*******************************************************************************
 * Copyright (c) 2025 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.PatternInstanceofToSwitchFixCore;

import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public class PatternInstanceofToSwitchCleanUpCore extends AbstractCleanUp {
	public PatternInstanceofToSwitchCleanUpCore(final Map<String, String> options) {
		super(options);
	}

	public PatternInstanceofToSwitchCleanUpCore() {
	}

	@Override
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(requireAST(), false, false, null);
	}

	public boolean requireAST() {
		return isEnabled(CleanUpConstants.USE_SWITCH_FOR_INSTANCEOF_PATTERN);
	}

	@Override
	public ICleanUpFix createFix(final CleanUpContext context) throws CoreException {
		CompilationUnit compilationUnit= context.getAST();

		if (compilationUnit == null) {
			return null;
		}

		boolean convertIfElseInstanceOfChainToSwitch= isEnabled(CleanUpConstants.USE_SWITCH_FOR_INSTANCEOF_PATTERN);

		if (!convertIfElseInstanceOfChainToSwitch) {
			return null;
		}

		return PatternInstanceofToSwitchFixCore.createCleanUp(compilationUnit);
	}

	@Override
	public String[] getStepDescriptions() {
		List<String> result= new ArrayList<>();

		if (isEnabled(CleanUpConstants.USE_SWITCH_FOR_INSTANCEOF_PATTERN)) {
			result.add(MultiFixMessages.CodeStyleCleanUp_PatternInstanceOfToSwitch_description);
		}

		return result.toArray(new String[0]);
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.USE_SWITCH_FOR_INSTANCEOF_PATTERN)) {
			return """
				switch (x) {
				case Integer xInt -> i = xInt.intValue();
				case Double xDouble -> d = xDouble.doubleValue():
				case Boolean xBoolean -> b = xBoolean.booleanValue();
				case null, default -> {
				  i = 0;
				  d = 0.0D;
				  b = false;
				}
				}

				"""; //$NON-NLS-1$
		}

		return """
			if (x instanceof Integer xInt) {
			    i = xInt.intValue();
			} else if (x instanceof Double xDouble) {
			    d = xDouble.doubleValue();
			} else if (x instancoef Boolean) {
			    b = xBoolean.booleanValue();
			} else {
			    i = 0;
			    d = 0.0D;
			    b = false;
			}
			"""; //$NON-NLS-1$
	}
}

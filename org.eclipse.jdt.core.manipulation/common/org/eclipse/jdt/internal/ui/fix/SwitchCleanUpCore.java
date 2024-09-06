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

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.SwitchFixCore;

import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public class SwitchCleanUpCore extends AbstractCleanUp {
	public SwitchCleanUpCore(final Map<String, String> options) {
		super(options);
	}

	public SwitchCleanUpCore() {
	}

	@Override
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(requireAST(), false, false, null);
	}

	public boolean requireAST() {
		return isEnabled(CleanUpConstants.USE_SWITCH);
	}

	@Override
	public ICleanUpFix createFix(final CleanUpContext context) throws CoreException {
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
			return """
				switch (number) {
				case 0:
				  i = 0;
				  break;
				case 1:
				  j = 10;
				  break;
				case 2:
				  k = 20;
				  break;
				default:
				  m = -1;
				  break;
				}
				"""; //$NON-NLS-1$
		}

		return """
			if (number == 0) {
			    i = 0;
			} else if (number == 1) {
			    j = 10;
			} else if (2 == number) {
			    k = 20;
			} else {
			    m = -1;
			}
			
			
			
			
			
			"""; //$NON-NLS-1$
	}
}

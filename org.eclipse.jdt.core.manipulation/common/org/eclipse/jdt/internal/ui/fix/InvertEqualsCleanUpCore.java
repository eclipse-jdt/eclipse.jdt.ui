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
import org.eclipse.jdt.internal.corext.fix.InvertEqualsFixCore;

import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public class InvertEqualsCleanUpCore extends AbstractCleanUp {
	public InvertEqualsCleanUpCore(final Map<String, String> options) {
		super(options);
	}

	public InvertEqualsCleanUpCore() {
	}

	@Override
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(requireAST(), false, false, null);
	}

	public boolean requireAST() {
		return isEnabled(CleanUpConstants.INVERT_EQUALS);
	}

	@Override
	public ICleanUpFix createFix(final CleanUpContext context) throws CoreException {
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
			return """
				boolean result = "foo".equals(text);
				boolean result2 = (text1 + text2).equals(text);
				boolean result3 = DayOfWeek.MONDAY.equals(object);
				boolean result4 = "foo".equalsIgnoreCase(text);
				"""; //$NON-NLS-1$
		}

		return """
			boolean result = text.equals("foo");
			boolean result2 = text.equals(text1 + text2);
			boolean result3 = object.equals(DayOfWeek.MONDAY);
			boolean result4 = text.equalsIgnoreCase("foo");
			"""; //$NON-NLS-1$
	}
}

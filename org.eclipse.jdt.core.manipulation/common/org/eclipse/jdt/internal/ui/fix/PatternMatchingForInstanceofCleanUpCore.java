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
import org.eclipse.jdt.internal.corext.fix.PatternMatchingForInstanceofFixCore;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public class PatternMatchingForInstanceofCleanUpCore extends AbstractCleanUp {
	public PatternMatchingForInstanceofCleanUpCore(final Map<String, String> options) {
		super(options);
	}

	public PatternMatchingForInstanceofCleanUpCore() {
	}

	@Override
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(requireAST(), false, false, null);
	}

	public boolean requireAST() {
		return isEnabled(CleanUpConstants.USE_PATTERN_MATCHING_FOR_INSTANCEOF);
	}

	@Override
	public ICleanUpFix createFix(final CleanUpContext context) throws CoreException {
		CompilationUnit compilationUnit= context.getAST();

		if (compilationUnit == null
				|| !isEnabled(CleanUpConstants.USE_PATTERN_MATCHING_FOR_INSTANCEOF)
				|| !JavaModelUtil.is16OrHigher(compilationUnit.getJavaElement().getJavaProject())) {
			return null;
		}

		return PatternMatchingForInstanceofFixCore.createCleanUp(compilationUnit);
	}

	@Override
	public String[] getStepDescriptions() {
		List<String> result= new ArrayList<>();

		if (isEnabled(CleanUpConstants.USE_PATTERN_MATCHING_FOR_INSTANCEOF)) {
			result.add(MultiFixMessages.PatternMatchingForInstanceofCleanup_description);
		}

		return result.toArray(new String[0]);
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.USE_PATTERN_MATCHING_FOR_INSTANCEOF)) {
			return """
				if (object instanceof Integer i) {
				    return i.intValue();
				}
				
				"""; //$NON-NLS-1$
		}

		return """
			if (object instanceof Integer) {
			    Integer i = (Integer) object;
			    return i.intValue();
			}
			"""; //$NON-NLS-1$
	}
}

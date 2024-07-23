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
import org.eclipse.jdt.internal.corext.fix.PlainReplacementFixCore;

import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public class PlainReplacementCleanUpCore extends AbstractCleanUp {
	public PlainReplacementCleanUpCore(final Map<String, String> options) {
		super(options);
	}

	public PlainReplacementCleanUpCore() {
	}

	@Override
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(requireAST(), false, false, null);
	}

	public boolean requireAST() {
		return isEnabled(CleanUpConstants.PLAIN_REPLACEMENT);
	}

	@Override
	public ICleanUpFix createFix(final CleanUpContext context) throws CoreException {
		CompilationUnit compilationUnit= context.getAST();

		if (compilationUnit == null || !isEnabled(CleanUpConstants.PLAIN_REPLACEMENT)) {
			return null;
		}

		return PlainReplacementFixCore.createCleanUp(compilationUnit);
	}

	@Override
	public String[] getStepDescriptions() {
		List<String> result= new ArrayList<>();

		if (isEnabled(CleanUpConstants.PLAIN_REPLACEMENT)) {
			result.add(MultiFixMessages.PlainReplacementCleanUp_description);
		}

		return result.toArray(new String[0]);
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.PLAIN_REPLACEMENT)) {
			return """
				String result = text.replace("foo", "bar");
				String result2 = text.replace("$0.02", "$0.50");
				String result3 = text.replace('.', '/');
				String result4 = text.replace(placeholder, value);
				"""; //$NON-NLS-1$
		}

		return """
			String result = text.replaceAll("foo", "bar");
			String result2 = text.replaceAll("\\\\$0\\\\.02", "\\\\$0.50");
			String result3 = text.replaceAll("\\\\.", "/");
			String result4 = text.replaceAll(Pattern.quote(placeholder), Matcher.quoteReplacement(value));
			"""; //$NON-NLS-1$
	}
}

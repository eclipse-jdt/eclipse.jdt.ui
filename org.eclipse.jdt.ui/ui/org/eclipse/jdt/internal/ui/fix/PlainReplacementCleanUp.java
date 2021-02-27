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

import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

/**
 * A fix that replaces <code>String.replaceAll()</code> by <code>String.replace()</code> when the pattern and the
 * replacement is a plain text not containing surrogate characters:
 * <ul>
 * <li>If the pattern is a constant, it is unescaped if needed,</li>
 * <li>If the pattern is quoted, it is unquoted,</li>
 * <li>If the replacement is quoted, it is unquoted,</li>
 * <li>If the pattern and the replacement are 1-character-long, they are replaced by chars.</li>
 * </ul>
 */
public class PlainReplacementCleanUp extends AbstractCleanUp {
	private PlainReplacementCleanUpCore coreCleanUp= new PlainReplacementCleanUpCore();

	public PlainReplacementCleanUp(final Map<String, String> options) {
		setOptions(options);
	}

	public PlainReplacementCleanUp() {
	}

	@Override
	public void setOptions(final CleanUpOptions options) {
		coreCleanUp.setOptions(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(coreCleanUp.getRequirementsCore());
	}

	@Override
	public ICleanUpFix createFix(final CleanUpContext context) throws CoreException {
		ICleanUpFixCore fixCore= coreCleanUp.createFixCore(context);
		return fixCore != null ? new CleanUpFixWrapper(fixCore) : null;
	}

	@Override
	public String[] getStepDescriptions() {
		return coreCleanUp.getStepDescriptions();
	}

	@Override
	public String getPreview() {
		return coreCleanUp.getPreview();
	}
}

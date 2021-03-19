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
 * A fix that replaces the new instance syntax by curly brackets to create an array when possible:
 * <ul>
 * <li>It must be an initialization of a declaration,</li>
 * <li>The declaration must have the same type.</li>
 * </ul>
 */
public class ArrayWithCurlyCleanUp extends AbstractCleanUp {
	private ArrayWithCurlyCleanUpCore coreCleanUp= new ArrayWithCurlyCleanUpCore();

	public ArrayWithCurlyCleanUp(final Map<String, String> options) {
		setOptions(options);
	}

	public ArrayWithCurlyCleanUp() {
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

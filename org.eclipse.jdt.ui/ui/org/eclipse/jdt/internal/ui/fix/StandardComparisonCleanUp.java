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
 * A fix that fixes <code>Comparable.compareTo()</code> usage:
 * <ul>
 * <li>The developer is not supposed to predict the <code>1</code> and <code>-1</code> values; them is supposed to get zero or a value lesser or greater than zero,</li>
 * <li>Beware! The behavior may change if you implement a custom comparator!</li>
 * </ul>
 */
public class StandardComparisonCleanUp extends AbstractCleanUp {
	private StandardComparisonCleanUpCore coreCleanUp= new StandardComparisonCleanUpCore();

	public StandardComparisonCleanUp(final Map<String, String> options) {
		setOptions(options);
	}

	public StandardComparisonCleanUp() {
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

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
 * A fix that removes the comparator declaration if it is the default one:
 * <ul>
 * <li>The declared comparator should be an equivalent to the natural order,</li>
 * <li>Apply on anonymous class, lambda, <code>Comparator.comparing()</code>, <code>Comparator.naturalOrder()</code> and null,</li>
 * <li>Apply on <code>List.sort(Comparator)</code>, <code>Collections.sort(List, Comparator)</code>, <code>Collections.max(Collection, Comparator)</code> and <code>Collections.min(Collection, Comparator)</code>,</li>
 * <li>If the comparator is used in the method <code>List.sort(Comparator)</code>, the method is converted into <code>Collections.sort(List)</code>.</li>
 * </ul>
 */
public class RedundantComparatorCleanUp extends AbstractCleanUp {
	private RedundantComparatorCleanUpCore coreCleanUp= new RedundantComparatorCleanUpCore();

	public RedundantComparatorCleanUp(final Map<String, String> options) {
		setOptions(options);
	}

	public RedundantComparatorCleanUp() {
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

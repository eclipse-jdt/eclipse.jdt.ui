/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer
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
 * A fix that replaces <code>System.getProperty(xxx)</code> by Java methods:
 * <ul>
 * <li>Beware! The code doesn't any longer take the system property into account!</li>
 *
 * System.getProperties() can be overridden by calls to System.setProperty(String key, String value)
 * or with command line parameters -Dfile.separator=/
 *
 * File.separator gets the separator for the default filesystem.
 *
 * FileSystems.getDefault() gets you the default filesystem.
 * </ul>
 */
public class ConstantsForSystemPropertyCleanUp extends AbstractCleanUp {
	private final ConstantsForSystemPropertiesCleanUpCore coreCleanUp= new ConstantsForSystemPropertiesCleanUpCore();

	public ConstantsForSystemPropertyCleanUp(final Map<String, String> options) {
		setOptions(options);
	}

	public ConstantsForSystemPropertyCleanUp() {
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
		return fixCore == null ? null : new CleanUpFixWrapper(fixCore);
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

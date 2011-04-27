/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.refactoring;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;

import org.eclipse.ltk.core.refactoring.Change;

import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;

public class LaunchConfigRenameChange extends LaunchConfigChange {

	private final String fNewName;

	private final ILaunchManager fLaunchManager;

	public LaunchConfigRenameChange(LaunchConfigurationContainer config,
			String newName, ILaunchManager lm, boolean shouldFlagWarning) {
		super(config, shouldFlagWarning);
		fNewName = newName;
		fLaunchManager = lm;
	}

	@Override
	protected void alterLaunchConfiguration(ILaunchConfigurationWorkingCopy copy)
			throws CoreException {
		if (!fLaunchManager.isExistingLaunchConfigurationName(fNewName))
			copy.rename(fNewName);
	}

	@Override
	protected String getOldValue(ILaunchConfiguration config) {
		return fConfig.getName();
	}

	@Override
	public Change getUndo(String oldValue) throws CoreException {
		return new LaunchConfigRenameChange(fConfig, oldValue, fLaunchManager,
				shouldFlagWarning());
	}

	@Override
	public String getName() {
		return Messages.format(JUnitMessages.LaunchConfigRenameChange_name,
				new Object[] { fConfig.getName(), fNewName });
	}
}
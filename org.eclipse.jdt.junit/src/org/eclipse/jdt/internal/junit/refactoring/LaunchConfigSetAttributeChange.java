/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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

import org.eclipse.ltk.core.refactoring.Change;

import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;

public class LaunchConfigSetAttributeChange extends LaunchConfigChange {

	private String fNewValue;

	private final String fAttributeName;

	public LaunchConfigSetAttributeChange(LaunchConfigurationContainer config, String attributeName, String newValue, boolean shouldFlagWarning) {
		super(config, shouldFlagWarning);
		fNewValue= newValue;
		fAttributeName= attributeName;
	}

	protected Change getUndo(String oldValue) throws CoreException {
		return new LaunchConfigSetAttributeChange(fConfig, fAttributeName, oldValue, shouldFlagWarning());
	}

	public String getChangedAttributeName() {
		return fAttributeName;
	}

	protected void alterLaunchConfiguration(ILaunchConfigurationWorkingCopy copy) throws CoreException {
		copy.setAttribute(fAttributeName, fNewValue);
	}

	protected String getOldValue(ILaunchConfiguration config) throws CoreException {
		return config.getAttribute(fAttributeName, (String) null);
	}

	public String getName() {
		return Messages.format(JUnitMessages.LaunchConfigSetAttributeChange_name, new Object[] {fAttributeName, fConfig.getName()});
	}
}
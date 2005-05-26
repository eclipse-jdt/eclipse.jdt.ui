/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

public class LaunchConfigurationContainer {

	public LaunchConfigurationContainer(ILaunchConfiguration configuration) {
		fConfiguration= configuration;
	}

	private ILaunchConfiguration fConfiguration;

	public String getName() {
		return fConfiguration.getName();
	}

	public ILaunchConfiguration getConfiguration() {
		return fConfiguration;
	}

	public void setConfiguration(ILaunchConfiguration configuration) {
		fConfiguration= configuration;
	}

	public String getAttribute(String attribute, String defaultValue) throws CoreException {
		return fConfiguration.getAttribute(attribute, defaultValue);
	}
}
/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     David Saff (saff@mit.edu) - bug 102632: [JUnit] Support for JUnit 4.
 *******************************************************************************/
package org.eclipse.jdt.ui.unittest.junit.internal.launcher;

import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaDependenciesTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;

import org.eclipse.jdt.internal.junit.launcher.AssertionVMArg;

import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.ui.unittest.junit.launcher.JUnitLaunchConfigurationTab;

public class JUnitTabGroup extends AbstractLaunchConfigurationTabGroup {

	@Override
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfiguration configuration = DebugUITools.getLaunchConfiguration(dialog);
		boolean isModularConfiguration = configuration != null && JavaRuntime.isModularConfiguration(configuration);
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] { new JUnitLaunchConfigurationTab(),
				new JavaArgumentsTab(), new JavaJRETab(true),
				isModularConfiguration ? new JavaDependenciesTab() : new JavaClasspathTab(), new SourceLookupTab(),
				new EnvironmentTab(), new CommonTab() };
		setTabs(tabs);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		super.setDefaults(config);
		AssertionVMArg.setArgDefault(config);
	}
}

/*******************************************************************************
 * Copyright (c) 2017, 2026 Angelo Zerr and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation,
 *         originally in https://github.com/angelozerr/jdt-codemining (EPL)
 *     Kai Winter <kaiwinter@gmx.de> - JUnit 5 ParameterizedTest support in jdt-codemining
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui.codemining;

import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;

import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;

/**
 * The JUnit launch shortcut, launching with a given test kind instead of the one of the project: a
 * project can contain tests of several JUnit versions. It reuses an existing launch configuration
 * only if it has the same test kind.
 */
class KindLaunchShortcut extends JUnitLaunchShortcut {

	private final String fTestKindId;

	KindLaunchShortcut(String testKindId) {
		fTestKindId= testKindId;
	}

	@Override
	protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(IJavaElement element, String testName) throws CoreException {
		ILaunchConfigurationWorkingCopy configuration= super.createLaunchConfiguration(element, testName);
		configuration.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND, fTestKindId);
		return configuration;
	}

	@Override
	protected String[] getAttributeNamesToCompare() {
		String[] inherited= super.getAttributeNamesToCompare();
		// Do not reuse the configuration of the same test launched with another kind. The
		// configurations created by the shortcut always have the attribute, the ones created by hand
		// without it are not reused.
		String[] names= Arrays.copyOf(inherited, inherited.length + 1);
		names[names.length - 1]= JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND;
		return names;
	}
}

/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.launcher;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.debug.core.ILaunchConfiguration;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

/**
 * Attribute keys used by the IJUnitLaunchConfiguration. Note that these constants are not API and
 * might change in the future.
 */
public class JUnitLaunchConfigurationConstants {

	public static final String MODE_RUN_QUIETLY_MODE = "runQuietly"; //$NON-NLS-1$
	public static final String ID_JUNIT_APPLICATION= "org.eclipse.jdt.junit.launchconfig"; //$NON-NLS-1$

	public static final String ATTR_NO_DISPLAY = JUnitCorePlugin.PLUGIN_ID + ".NO_DISPLAY"; //$NON-NLS-1$



	public static final String ATTR_PORT= JUnitCorePlugin.PLUGIN_ID+".PORT"; //$NON-NLS-1$

	public static final String ATTR_DONT_ADD_MISSING_JUNIT5_DEPENDENCY= JUnitCorePlugin.PLUGIN_ID + ".DONT_ADD_MISSING_JUNIT5_DEPENDENCY"; //$NON-NLS-1$

	/**
	 * If the element is a test class annotated with <code>@RunWith(JUnitPlatform.class)</code>,
	 * this attribute is set to true.
	 */
	public static final String ATTR_RUN_WITH_JUNIT_PLATFORM_ANNOTATION= JUnitCorePlugin.PLUGIN_ID + ".IS_RUN_WITH_JUNIT_PLATFORM"; //$NON-NLS-1$

	/**
	 * The test method name (followed by a comma-separated list of fully qualified parameter type
	 * names in parentheses, if exists), or "" iff running the whole test type.
	 */
	public static final String ATTR_TEST_NAME= JUnitCorePlugin.PLUGIN_ID + ".TESTNAME"; //$NON-NLS-1$

	/**
	 * @Deprecated use {@link #ATTR_TEST_NAME}
	 **/
	public static final String ATTR_TEST_METHOD_NAME= ATTR_TEST_NAME;

	public static final String ATTR_KEEPRUNNING = JUnitCorePlugin.PLUGIN_ID+ ".KEEPRUNNING_ATTR"; //$NON-NLS-1$
	/**
	 * The launch container, or "" iff running a single test type.
	 */
	public static final String ATTR_TEST_CONTAINER= JUnitCorePlugin.PLUGIN_ID+".CONTAINER"; //$NON-NLS-1$

	public static final String ATTR_FAILURES_NAMES= JUnitCorePlugin.PLUGIN_ID+".FAILURENAMES"; //$NON-NLS-1$

	public static final String ATTR_TEST_RUNNER_KIND= JUnitCorePlugin.PLUGIN_ID+".TEST_KIND"; //$NON-NLS-1$

	public static final String ATTR_TEST_HAS_INCLUDE_TAGS= JUnitCorePlugin.PLUGIN_ID + ".HAS_INCLUDE_TAGS"; //$NON-NLS-1$

	public static final String ATTR_TEST_HAS_EXCLUDE_TAGS= JUnitCorePlugin.PLUGIN_ID + ".HAS_EXCLUDE_TAGS"; //$NON-NLS-1$

	public static final String ATTR_TEST_INCLUDE_TAGS= JUnitCorePlugin.PLUGIN_ID + ".INCLUDE_TAGS"; //$NON-NLS-1$

	public static final String ATTR_TEST_EXCLUDE_TAGS= JUnitCorePlugin.PLUGIN_ID + ".EXCLUDE_TAGS"; //$NON-NLS-1$

	/**
	 * The unique ID of test to run or "" if not available (applicable to JUnit 5 and above).
	 */
	public static final String ATTR_TEST_UNIQUE_ID= JUnitCorePlugin.PLUGIN_ID + ".TEST_UNIQUE_ID"; //$NON-NLS-1$

	public static ITestKind getTestRunnerKind(ILaunchConfiguration launchConfiguration) {
		try {
			String loaderId = launchConfiguration.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND, (String) null);
			if (loaderId != null) {
				return TestKindRegistry.getDefault().getKind(loaderId);
			}
		} catch (CoreException e) {
		}
		return  ITestKind.NULL;
	}

	public static IJavaProject getJavaProject(ILaunchConfiguration configuration) {
		try {
			String projectName= configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
			if (projectName != null && projectName.length() > 0) {
				return JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getProject(projectName));
			}
		} catch (CoreException e) {
		}
		return null;
	}

	private JUnitLaunchConfigurationConstants() {
	}


}

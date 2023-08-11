/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
package org.eclipse.jdt.internal.junit;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;

/**
 * Defines constants which are used to refer to values in the plugin's preference store.
 */
public class JUnitPreferencesConstants {
	/**
	 * Boolean preference controlling whether the failure stack should be
	 * filtered.
	 */
	public static final String DO_FILTER_STACK= JUnitCorePlugin.PLUGIN_ID + ".do_filter_stack"; //$NON-NLS-1$

	/**
	 * Boolean preference controlling whether the JUnit view should be shown on
	 * errors only.
	 */
	public static final String SHOW_ON_ERROR_ONLY= JUnitCorePlugin.PLUGIN_ID + ".show_on_error"; //$NON-NLS-1$

	/**
	 * Boolean preference controlling whether '-ea' should be added to VM arguments when creating a
	 * new JUnit launch configuration.
	 */
	public static final String ENABLE_ASSERTIONS= JUnitCorePlugin.PLUGIN_ID + ".enable_assertions"; //$NON-NLS-1$

	public static final boolean ENABLE_ASSERTIONS_DEFAULT= true;

	/**
	 * List of active stack filters. A String containing a comma separated list
	 * of fully qualified type names/patterns.
	 */
	public static final String PREF_ACTIVE_FILTERS_LIST = JUnitCorePlugin.PLUGIN_ID + ".active_filters"; //$NON-NLS-1$

	/**
	 * List of inactive stack filters. A String containing a comma separated
	 * list of fully qualified type names/patterns.
	 */
	public static final String PREF_INACTIVE_FILTERS_LIST = JUnitCorePlugin.PLUGIN_ID + ".inactive_filters"; //$NON-NLS-1$

	/**
	 * Maximum number of remembered test runs.
	 */
	public static final String MAX_TEST_RUNS= JUnitCorePlugin.PLUGIN_ID + ".max_test_runs"; //$NON-NLS-1$

	/**
	 * Javadoc location for JUnit 3
	 */
	public static final String JUNIT3_JAVADOC= JUnitCorePlugin.PLUGIN_ID + ".junit3.javadoclocation"; //$NON-NLS-1$


	/**
	 * Javadoc location for JUnit 4
	 */
	public static final String JUNIT4_JAVADOC= JUnitCorePlugin.PLUGIN_ID + ".junit4.javadoclocation"; //$NON-NLS-1$

	/**
	 * Javadoc location for org.hamcrest.core (JUnit 4)
	 */
	public static final String HAMCREST_CORE_JAVADOC= JUnitCorePlugin.PLUGIN_ID + ".junit4.hamcrest.core.javadoclocation"; //$NON-NLS-1$

	/**
	 * Javadoc location for org.hamcrest (JUnit 4)
	 */
	public static final String HAMCREST_JAVADOC= JUnitCorePlugin.PLUGIN_ID + ".junit4.hamcrest.javadoclocation"; //$NON-NLS-1$

	/**
	 * Javadoc location for org.junit.jupiter.api (JUnit 5)
	 */
	public static final String JUNIT_JUPITER_API_JAVADOC= JUnitCorePlugin.PLUGIN_ID + ".junit5.jupiter.api.javadoclocation"; //$NON-NLS-1$

	/**
	 * Javadoc location for org.junit.jupiter.engine (JUnit 5)
	 */
	public static final String JUNIT_JUPITER_ENGINE_JAVADOC= JUnitCorePlugin.PLUGIN_ID + ".junit5.jupiter.engine.javadoclocation"; //$NON-NLS-1$

	/**
	 * Javadoc location for org.junit.jupiter.migrationsupport (JUnit 5)
	 */
	public static final String JUNIT_JUPITER_MIGRATIONSUPPORT_JAVADOC= JUnitCorePlugin.PLUGIN_ID + ".junit5.jupiter.migrationsupport.javadoclocation"; //$NON-NLS-1$

	/**
	 * Javadoc location for org.junit.jupiter.params (JUnit 5)
	 */
	public static final String JUNIT_JUPITER_PARAMS_JAVADOC= JUnitCorePlugin.PLUGIN_ID + ".junit5.jupiter.params.javadoclocation"; //$NON-NLS-1$

	/**
	 * Javadoc location for org.junit.platform.commons (JUnit 5)
	 */
	public static final String JUNIT_PLATFORM_COMMONS_JAVADOC= JUnitCorePlugin.PLUGIN_ID + ".junit5.platform.commons.javadoclocation"; //$NON-NLS-1$

	/**
	 * Javadoc location for org.junit.platform.engine (JUnit 5)
	 */
	public static final String JUNIT_PLATFORM_ENGINE_JAVADOC= JUnitCorePlugin.PLUGIN_ID + ".junit5.platform.engine.javadoclocation"; //$NON-NLS-1$

	/**
	 * Javadoc location for org.junit.platform.launcher (JUnit 5)
	 */
	public static final String JUNIT_PLATFORM_LAUNCHER_JAVADOC= JUnitCorePlugin.PLUGIN_ID + ".junit5.platform.launcher.javadoclocation"; //$NON-NLS-1$

	/**
	 * Javadoc location for org.junit.platform.runner (JUnit 5)
	 */
	public static final String JUNIT_PLATFORM_RUNNER_JAVADOC= JUnitCorePlugin.PLUGIN_ID + ".junit5.platform.runner.javadoclocation"; //$NON-NLS-1$

	/**
	 * Javadoc location for org.junit.platform.suite.api (JUnit 5)
	 */
	public static final String JUNIT_PLATFORM_SUITE_API_JAVADOC= JUnitCorePlugin.PLUGIN_ID + ".junit5.platform.suite.api.javadoclocation"; //$NON-NLS-1$

	/**
	 * Javadoc location for org.junit.platform.suite.engine (JUnit 5)
	 */
	public static final String JUNIT_PLATFORM_SUITE_ENGINE_JAVADOC= JUnitCorePlugin.PLUGIN_ID + ".junit5.platform.suite.engine.javadoclocation"; //$NON-NLS-1$

	/**
	 * Javadoc location for org.junit.platform.suite.commons (JUnit 5)
	 */
	public static final String JUNIT_PLATFORM_SUITE_COMMONS_JAVADOC= JUnitCorePlugin.PLUGIN_ID + ".junit5.platform.suite.commons.javadoclocation"; //$NON-NLS-1$

	/**
	 * Javadoc location for org.junit.vintage.engine (JUnit 5)
	 */
	public static final String JUNIT_VINTAGE_ENGINE_JAVADOC= JUnitCorePlugin.PLUGIN_ID + ".junit5.vintage.engine.javadoclocation"; //$NON-NLS-1$

	/**
	 * Javadoc location for org.opentest4j (JUnit 5)
	 */
	public static final String JUNIT_OPENTEST4J_JAVADOC= JUnitCorePlugin.PLUGIN_ID + ".junit5.opentest4j.javadoclocation"; //$NON-NLS-1$

	/**
	 * Javadoc location for org.apiguardian (JUnit 5)
	 */
	public static final String JUNIT_APIGUARDIAN_JAVADOC= JUnitCorePlugin.PLUGIN_ID + ".junit5.apiguardian.javadoclocation"; //$NON-NLS-1$

	private static final String[] fgDefaultFilterPatterns= new String[] {
		"org.eclipse.jdt.internal.junit.runner.*", //$NON-NLS-1$
		"org.eclipse.jdt.internal.junit4.runner.*", //$NON-NLS-1$
		"org.eclipse.jdt.internal.junit5.runner.*", //$NON-NLS-1$
		"org.eclipse.jdt.internal.junit.ui.*", //$NON-NLS-1$
		"junit.framework.TestCase", //$NON-NLS-1$
		"junit.framework.TestResult", //$NON-NLS-1$
		"junit.framework.TestResult$1", //$NON-NLS-1$
		"junit.framework.TestSuite", //$NON-NLS-1$
		"junit.framework.Assert", //$NON-NLS-1$
		"org.junit.*", //$NON-NLS-1$ //TODO: filter all these?
		"java.lang.reflect.Method.invoke", //$NON-NLS-1$
		"sun.reflect.*", //$NON-NLS-1$
		"jdk.internal.reflect.*", //$NON-NLS-1$
	};

	private JUnitPreferencesConstants() {
		// no instance
	}

	/**
	 * Returns the default list of active stack filters.
	 *
	 * @return list
	 */
	public static String[] createDefaultStackFiltersList() {
		return fgDefaultFilterPatterns;
	}

	/**
	 * Serializes the array of strings into one comma-separated string.
	 *
	 * @param list array of strings
	 * @return a single string composed of the given list
	 */
	public static String serializeList(String[] list) {
		if (list == null)
			return ""; //$NON-NLS-1$

		return String.join(String.valueOf(','), list);
	}

	/**
	 * Parses the comma-separated string into an array of strings.
	 *
	 * @param listString a comma-separated string
	 * @return an array of strings
	 */
	public static String[] parseList(String listString) {
		List<String> list= new ArrayList<>(10);
		StringTokenizer tokenizer= new StringTokenizer(listString, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens())
			list.add(tokenizer.nextToken());
		return list.toArray(new String[list.size()]);
	}

	public static String[] getFilterPatterns() {
		return JUnitPreferencesConstants.parseList(Platform.getPreferencesService().getString(JUnitCorePlugin.CORE_PLUGIN_ID, PREF_ACTIVE_FILTERS_LIST, null, null));
	}

	public static boolean getFilterStack() {
		return Platform.getPreferencesService().getBoolean(JUnitCorePlugin.CORE_PLUGIN_ID, DO_FILTER_STACK, true, null);
	}

	public static void setFilterStack(boolean filter) {
		InstanceScope.INSTANCE.getNode(JUnitCorePlugin.CORE_PLUGIN_ID).putBoolean(DO_FILTER_STACK, filter);
	}
}

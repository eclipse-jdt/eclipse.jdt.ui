/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
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
 *     David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.runner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface ITestLoader {
	/**
	 * @param testClasses classes to be run
	 * @param testName individual method to be run
	 * @param failureNames may want to run these first, since they failed
	 * @param packages packages containing tests to run
	 * @param includeExcludeTags tags to be included and excluded in the test run
	 * @param uniqueId unique ID of the test to run
	 * @param listener to be notified if tests could not be loaded
	 * @return the loaded test references
	 */
	ITestReference[] loadTests(Class<?>[] testClasses, String testName, String[] failureNames, String[] packages, String[][] includeExcludeTags, String uniqueId, RemoteTestRunner listener);

	/**
	 * Loads tests for an arbitrary collection of (class, method) pairs in a single launch.
	 * <p>
	 * The default implementation delegates to {@link #loadTests(Class[], String, String[], String[], String[][], String, RemoteTestRunner)}
	 * for each pair, preserving the legacy behavior of one filtered request per method.
	 * Loaders that can express multi-method discovery natively (e.g. JUnit Platform via multiple
	 * {@code selectMethod} selectors, JUnit 4 via a multi-method filter) are encouraged to override
	 * this method so that the entire selection runs inside a single launch.
	 * </p>
	 *
	 * @param classToMethods ordered map from test class to the set of methods to run within that class
	 * @param failureNames may want to run these first, since they failed
	 * @param packages packages containing tests to run
	 * @param includeExcludeTags tags to be included and excluded in the test run
	 * @param uniqueId unique ID of the test to run
	 * @param listener to be notified if tests could not be loaded
	 * @return the loaded test references
	 */
	default ITestReference[] loadTests(LinkedHashMap<Class<?>, List<String>> classToMethods, String[] failureNames, String[] packages, String[][] includeExcludeTags, String uniqueId, RemoteTestRunner listener) {
		List<ITestReference> refs= new ArrayList<>();
		for (Map.Entry<Class<?>, List<String>> entry : classToMethods.entrySet()) {
			Class<?>[] singleClass= { entry.getKey() };
			List<String> methods= entry.getValue();
			if (methods == null || methods.isEmpty()) {
				// An empty / null method list represents an unfiltered class run, which
				// the legacy single-method overload models by passing testName == null.
				// This lets callers (e.g. RemoteTestRunner) mix class-only and
				// Class:method entries from the same -testNameFile without losing the
				// class-only selections.
				ITestReference[] wholeClass= loadTests(singleClass, null, failureNames, packages, includeExcludeTags, uniqueId, listener);
				if (wholeClass != null) {
					for (ITestReference r : wholeClass) {
						if (r != null) {
							refs.add(r);
						}
					}
				}
				continue;
			}
			for (String methodName : methods) {
				ITestReference[] perMethod= loadTests(singleClass, methodName, failureNames, packages, includeExcludeTags, uniqueId, listener);
				if (perMethod != null) {
					for (ITestReference r : perMethod) {
						if (r != null) {
							refs.add(r);
						}
					}
				}
			}
		}
		return refs.toArray(new ITestReference[0]);
	}
}


/*******************************************************************************
 * Copyright (c) 2025 Carsten Hammer and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer using github copilot - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.model.TestSuiteElement;

/**
 * Utility class for finding IMethod from TestSuiteElement.
 * 
 * @since 3.15
 */
public class TestMethodFinder {

	private static final char PARAM_START = '(';

	/**
	 * Find the IMethod for a TestSuiteElement representing a parameterized test.
	 * Extracts method name from test name pattern "methodName(ParameterType)" and
	 * looks up the method in the test class.
	 * 
	 * @param testSuiteElement the test suite element
	 * @return the IMethod, or null if not found
	 */
	public static IMethod findMethodForParameterizedTest(TestSuiteElement testSuiteElement) {
		String testName = testSuiteElement.getTestName();
		int index = testName.indexOf(PARAM_START);
		if (index < 0) {
			return null; // Not a parameterized test method signature
		}

		String methodName = testName.substring(0, index);
		String className = testSuiteElement.getSuiteTypeName();

		if (className == null || className.isEmpty()) {
			return null;
		}

		IJavaProject javaProject = testSuiteElement.getTestRunSession().getLaunchedProject();
		if (javaProject == null) {
			return null;
		}

		try {
			IType type = javaProject.findType(className);
			if (type == null) {
				return null;
			}

			// Find the method - for parameterized tests, method name is without parameters
			IMethod[] methods = type.getMethods();
			for (IMethod method : methods) {
				if (method.getElementName().equals(methodName)) {
					return method;
				}
			}
		} catch (JavaModelException e) {
			JUnitPlugin.log(e);
		}

		return null;
	}

	private TestMethodFinder() {
		// Utility class - no instances
	}
}

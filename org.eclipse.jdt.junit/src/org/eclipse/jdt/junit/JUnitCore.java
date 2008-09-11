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

package org.eclipse.jdt.junit;


import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;

/**
 * Class for accessing JUnit support; all functionality is provided by
 * static methods.
 * <p>
 * This class is not intended to be subclassed or instantiated by clients.
 * </p>
 *
 * @since 2.1
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class JUnitCore {

	/**
	 * Adds a listener for test runs.
	 *
	 * @param listener listener to be added
	 * @deprecated As of 3.3, replaced by {@link #addTestRunListener(TestRunListener)}
	 */
	public static void addTestRunListener(ITestRunListener listener) {
		JUnitPlugin.getDefault().addTestRunListener(listener);
	}

	/**
	 * Removes a listener for test runs.
	 *
	 * @param listener listener to be removed
	 * @deprecated As of 3.3, replaced by {@link #removeTestRunListener(TestRunListener)}
	 */
	public static void removeTestRunListener(ITestRunListener listener) {
		JUnitPlugin.getDefault().removeTestRunListener(listener);
	}

	/**
	 * Adds a listener for test runs.
	 *
	 * @param listener the listener to be added
	 * @since 3.3
	 */
	public static void addTestRunListener(TestRunListener listener) {
		JUnitPlugin.getDefault().getNewTestRunListeners().add(listener);
	}

	/**
	 * Removes a listener for test runs.
	 *
	 * @param listener the listener to be removed
	 * @since 3.3
	 */
	public static void removeTestRunListener(TestRunListener listener) {
		JUnitPlugin.getDefault().getNewTestRunListeners().remove(listener);
	}
}

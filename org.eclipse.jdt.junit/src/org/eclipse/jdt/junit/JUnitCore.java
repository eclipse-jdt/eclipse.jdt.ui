/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.junit;

import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;

/**
 * Class for accessing JUnit support; all functionality is provided by 
 * static methods. 
 * </p>
 * 
 * @since 2.1
 */
public class JUnitCore {

	/**
	 * Adds a listener for test runs.
	 * 
	 * @param listener listener to be added
	 */
	public static void addTestRunListener(ITestRunListener listener) {
		JUnitPlugin.getDefault().addTestRunListener(listener);
	}

	/**
	 * Removes a listener for test runs.
	 * 
	 * @param listener listener to be removed 
	 */
	public static void removeTestRunListener(ITestRunListener listener) {
		JUnitPlugin.getDefault().removeTestRunListener(listener);
	}
}

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

/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.junit;


import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.junit.launcher.JUnit4TestFinder;
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

	/**
	 * Finds types that contain JUnit tests in the given container.
	 * 
	 * @param container the container
	 * @param monitor the progress monitor used to report progress and request cancelation,
	 *   or <code>null</code> if none
	 * @return test types inside the given container
	 * @throws CoreException when a problem occurs while accessing <code>container</code> or its children
	 * @throws OperationCanceledException if the operation has been canceled
	 * 
	 * @since 3.5
	 */
	public static IType[] findTestTypes(IJavaElement container, IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		final Set result= new HashSet();
		JUnit4TestFinder finder= new JUnit4TestFinder();
		finder.findTestsInContainer(container, result, monitor);

		return (IType[])result.toArray(new IType[result.size()]);
	}

}

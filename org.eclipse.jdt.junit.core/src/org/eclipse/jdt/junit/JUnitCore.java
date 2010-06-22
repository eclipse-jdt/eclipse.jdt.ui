/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Mirko Raner <mirko@raner.ws> - Expose JUnitModel.exportTestRunSession(...) as API - https://bugs.eclipse.org/316199
 *******************************************************************************/

package org.eclipse.jdt.junit;


import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.transform.TransformerException;

import org.eclipse.jdt.junit.model.ITestRunSession;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.launcher.JUnit4TestFinder;
import org.eclipse.jdt.internal.junit.model.JUnitModel;
import org.eclipse.jdt.internal.junit.model.ModelMessages;
import org.eclipse.jdt.internal.junit.model.TestRunSession;

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
	 * ID of the JUnit {@linkplain IClasspathContainer classpath container}.
	 * The general format of classpath entries using this container is unspecified.
	 * 
	 * @see #JUNIT3_CONTAINER_PATH
	 * @see #JUNIT4_CONTAINER_PATH
	 * @since 3.6
	 */
	public static final String JUNIT_CONTAINER_ID= "org.eclipse.jdt.junit.JUNIT_CONTAINER"; //$NON-NLS-1$
	
	/**
	 * Path of the JUnit 3 {@linkplain IClasspathContainer classpath container}.
	 * 
	 * @since 3.6
	 */
	public final static IPath JUNIT3_CONTAINER_PATH= new Path(JUNIT_CONTAINER_ID).append("3"); //$NON-NLS-1$
	
	/**
	 * Path of the JUnit 4 {@linkplain IClasspathContainer classpath container}.
	 * 
	 * @since 3.6
	 */
	public final static IPath JUNIT4_CONTAINER_PATH= new Path(JUNIT_CONTAINER_ID).append("4"); //$NON-NLS-1$

	/**
	 * Adds a listener for test runs.
	 *
	 * @param listener listener to be added
	 * @deprecated As of 3.3, replaced by {@link #addTestRunListener(TestRunListener)}
	 */
	public static void addTestRunListener(ITestRunListener listener) {
		JUnitCorePlugin.getDefault().addTestRunListener(listener);
	}

	/**
	 * Removes a listener for test runs.
	 *
	 * @param listener listener to be removed
	 * @deprecated As of 3.3, replaced by {@link #removeTestRunListener(TestRunListener)}
	 */
	public static void removeTestRunListener(ITestRunListener listener) {
		JUnitCorePlugin.getDefault().removeTestRunListener(listener);
	}

	/**
	 * Adds a listener for test runs.
	 * <p>
	 * <strong>Note:</strong> If your plug-in should be loaded when a test run starts,
	 * please contribute to the <code>org.eclipse.jdt.junit.testRunListeners</code> extension point instead.
	 * </p>
	 *
	 * @param listener the listener to be added
	 * @since 3.3
	 */
	public static void addTestRunListener(TestRunListener listener) {
		JUnitCorePlugin.getDefault().getNewTestRunListeners().add(listener);
	}

	/**
	 * Removes a listener for test runs.
	 *
	 * @param listener the listener to be removed
	 * @since 3.3
	 */
	public static void removeTestRunListener(TestRunListener listener) {
		JUnitCorePlugin.getDefault().getNewTestRunListeners().remove(listener);
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

	/**
	 * Exports the given test run session into an XML report file.
	 * 
	 * @param testRunSession the test run session
	 * @param file the destination
	 * @throws CoreException if an error occurred
	 * 
	 * @since 3.7
	 */
	public static void exportTestRunSession(ITestRunSession testRunSession, File file) throws CoreException {
		JUnitModel.exportTestRunSession((TestRunSession)testRunSession, file);
	}

	/**
	 * Exports the given test run session to an output stream.
	 * 
	 * @param testRunSession the test run session
	 * @param output the output stream
	 * @throws CoreException if an error occurred
	 * 
	 * @since 3.7
	 */
	public static void exportTestRunSession(ITestRunSession testRunSession, OutputStream output) throws CoreException {
		try {
			JUnitModel.exportTestRunSession((TestRunSession)testRunSession, output);
			
		} catch (TransformerException exception) {
			String pluginID= JUnitCorePlugin.getPluginId();
			String message= ModelMessages.JUnitModel_could_not_export;
			throw new CoreException(new Status(IStatus.ERROR, pluginID, message, exception));
		}
	}

	/**
	 * Imports a test run session from the given file.
	 * 
	 * @param file a file containing a test run session transcript
	 * @return the imported test run session
	 * @throws CoreException if the import failed
	 * 
	 * @since 3.7
	 */
	public static ITestRunSession importTestRunSession(File file) throws CoreException {
		return JUnitModel.importTestRunSession(file);
	}

	/**
	 * Imports a test run session from the given URL.
	 * 
	 * @param url an URL to a test run session transcript
	 * @param monitor a progress monitor for cancellation
	 * @return the imported test run session, or <code>null</code> if the import has been cancelled
	 * @throws CoreException if the import failed
	 * 
	 * @since 3.7
	 */
	public static ITestRunSession importTestRunSession(final String url, IProgressMonitor monitor) throws CoreException {
		try {
			return JUnitModel.importTestRunSession(url, monitor);
			
		} catch (InvocationTargetException exception) {
			String pluginID= JUnitCorePlugin.getPluginId();
			String message= ModelMessages.JUnitModel_could_not_import;
			Throwable throwable= exception.getCause() != null ? exception.getCause() : exception;
			throw new CoreException(new Status(IStatus.ERROR, pluginID, message, throwable));
			
		} catch (InterruptedException interrupt) {
			return null;
		}
	}
}

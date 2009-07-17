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
package org.eclipse.jdt.junit.model;

import org.eclipse.jdt.core.IJavaProject;

/**
 * Represents a test run session.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *
 * @since 3.3
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ITestRunSession extends ITestElementContainer {

	/**
	 * Returns the name of the test run. The name is the name of the launch configuration use to run this test.
	 *
	 * @return returns the test run name
	 */
	public String getTestRunName();

	/**
	 * Returns the Java project from which this test run session has been launched, or <code>null</code> if not available.
	 * 
	 * @return the launched project, or <code>null</code> is not available.
	 * @since 3.6
	 */
	public abstract IJavaProject getLaunchedProject();

}

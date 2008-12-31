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
package org.eclipse.jdt.text.tests;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.IJavaProject;


/**
 * Test setup which creates a Java project with JUnit source.
 * <p>
 * - the project name is "JUnit_" + current time
 * - the source folder is called "src"
 * - the output folder is called "bin"
 * </p>
 *
 * @since 3.1
 */
public class JUnitProjectTestSetup extends TestSetup {

	private static IJavaProject fgProject;

	public static IJavaProject getProject() {
		assertNotNull(fgProject);
		return fgProject;
	}

	public JUnitProjectTestSetup(Test test) {
		super(test);
	}

	/*
	 * @see junit.framework.TestCase#setUp()
	 * @since 3.1
	 */
	protected void setUp() throws Exception {
		String projectName= "JUnit_" + System.currentTimeMillis();
		fgProject= JavaProjectHelper.createJavaProjectWithJUnitSource(projectName, "src", "bin");
	}

	/*
	 * @see junit.framework.TestCase#tearDown()
	 * @since 3.1
	 */
	protected void tearDown() throws Exception {
		if (fgProject != null) {
			JavaProjectHelper.delete(fgProject);
			fgProject= null;
		}
	}
}

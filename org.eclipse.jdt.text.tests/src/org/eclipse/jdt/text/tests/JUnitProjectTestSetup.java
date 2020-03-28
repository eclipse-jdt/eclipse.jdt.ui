/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import static org.junit.Assert.assertNotNull;

import org.junit.rules.ExternalResource;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

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
public class JUnitProjectTestSetup extends ExternalResource {

	private static IJavaProject fgProject;

	public static IJavaProject getProject() {
		assertNotNull(fgProject);
		return fgProject;
	}

	@Override
	public void before() throws Exception {
		String projectName= "JUnit_" + System.currentTimeMillis();
		fgProject= JavaProjectHelper.createJavaProjectWithJUnitSource(projectName, "src", "bin");
	}

	@Override
	public void after() {
		if (fgProject != null) {
			try {
				JavaProjectHelper.delete(fgProject);
			} catch (CoreException e) {
				e.printStackTrace();
			}
			fgProject= null;
		}
	}
}

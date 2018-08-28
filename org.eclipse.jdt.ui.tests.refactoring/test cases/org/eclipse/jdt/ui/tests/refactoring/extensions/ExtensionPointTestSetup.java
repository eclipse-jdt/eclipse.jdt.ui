/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.refactoring.extensions;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.ui.util.CoreUtility;


public class ExtensionPointTestSetup extends TestSetup {

	private IJavaProject fJavaProject;
	private IPackageFragmentRoot fRoot;
	private static final String CONTAINER= "src";

	public ExtensionPointTestSetup(Test test) {
		super(test);
	}

	public IPackageFragmentRoot getRoot() {
		return fRoot;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		fJavaProject= JavaProjectHelper.createJavaProject("TestProject", "bin");
		JavaProjectHelper.addRTJar(fJavaProject);
		fRoot= JavaProjectHelper.addSourceContainer(fJavaProject, CONTAINER);

		CoreUtility.setAutoBuilding(false);

		getRoot().createPackageFragment("test", true, null);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		JavaProjectHelper.delete(fJavaProject);
	}
}

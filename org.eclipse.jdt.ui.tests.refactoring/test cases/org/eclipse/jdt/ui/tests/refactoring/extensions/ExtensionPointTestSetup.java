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
package org.eclipse.jdt.ui.tests.refactoring.extensions;

import org.junit.rules.ExternalResource;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.ui.util.CoreUtility;

public class ExtensionPointTestSetup extends ExternalResource {
	private IJavaProject fJavaProject;
	private IPackageFragmentRoot fRoot;
	private static final String CONTAINER= "src";

	public IPackageFragmentRoot getRoot() {
		return fRoot;
	}

	@Override
	protected void before() throws Exception {
		fJavaProject= JavaProjectHelper.createJavaProject("TestProject", "bin");
		JavaProjectHelper.addRTJar(fJavaProject);
		fRoot= JavaProjectHelper.addSourceContainer(fJavaProject, CONTAINER);

		CoreUtility.setAutoBuilding(false);

		getRoot().createPackageFragment("test", true, null);
	}

	@Override
	protected void after() {
		try {
			JavaProjectHelper.delete(fJavaProject);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
}

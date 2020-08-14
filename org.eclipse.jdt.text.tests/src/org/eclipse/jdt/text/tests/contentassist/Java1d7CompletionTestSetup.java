/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package org.eclipse.jdt.text.tests.contentassist;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.tests.core.rules.Java1d7ProjectTestSetup;

/**
 * This class is used to run tests on Java Dolphin 1.7 .
 */
class Java1d7CompletionTestSetup extends Java1d7ProjectTestSetup {
	public IPackageFragment getTestPackage() throws CoreException {
		IJavaProject project= getProject();
		IPackageFragmentRoot root= project.getPackageFragmentRoot("src");
		if (!root.exists())
			root= JavaProjectHelper.addSourceContainer(project, "src");

		IPackageFragment fragment= root.getPackageFragment("test1");
		if (!fragment.exists())
			fragment= root.createPackageFragment("test1", false, null);

		return fragment;
	}

	private static int fAnonymousSoureFolderCounter= 0;
	public IPackageFragment getAnonymousTestPackage() throws CoreException {
		IJavaProject project= getProject();
		String sourceFolder= "src" + fAnonymousSoureFolderCounter++;
		IPackageFragmentRoot root= project.getPackageFragmentRoot(sourceFolder);
		if (!root.exists())
			root= JavaProjectHelper.addSourceContainer(project, sourceFolder);

		IPackageFragment fragment= root.getPackageFragment("test1");
		if (!fragment.exists())
			fragment= root.createPackageFragment("test1", false, null);

		return fragment;
	}
}
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
package org.eclipse.jdt.ui.tests.refactoring.reorg;


import org.junit.Assert;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;


public class TestProject {

	private IJavaProject fTestProject;
	private IPackageFragmentRoot fSourceFolder;

	public TestProject() throws Exception {
		this("TestProject");
	}

	public TestProject(String name) throws Exception {
		fTestProject= JavaProjectHelper.createJavaProject(name, "bin");
		// was: we must make sure that the performance test are compatible to 2.1.3 & 3.0 so use rt13
		// rt13 is deprecated, use rt15
		Assert.assertNotNull("rt not found", JavaProjectHelper.addRTJar15(fTestProject));
		fSourceFolder= JavaProjectHelper.addSourceContainer(fTestProject, "src");
	}

	public IJavaProject getProject() {
		return fTestProject;
	}

	public IPackageFragmentRoot getSourceFolder() {
		return fSourceFolder;
	}

	public void delete() throws Exception {
		if (fTestProject != null && fTestProject.exists())
			JavaProjectHelper.delete(fTestProject);
	}
}

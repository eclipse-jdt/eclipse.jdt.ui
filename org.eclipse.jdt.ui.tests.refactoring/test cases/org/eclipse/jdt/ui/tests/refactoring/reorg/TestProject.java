/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.reorg;

import junit.framework.Assert;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.testplugin.JavaProjectHelper;


public class TestProject {
	
	private IJavaProject fTestProject;
	private IPackageFragmentRoot fSourceFolder;

	public TestProject() throws Exception {
		this("TestProject");
	}
	
	public TestProject(String name) throws Exception {
		fTestProject= JavaProjectHelper.createJavaProject(name, "bin");
		Assert.assertTrue("rt not found", JavaProjectHelper.addRTJar(fTestProject) != null);
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

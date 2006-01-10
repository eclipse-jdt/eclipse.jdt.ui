/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.contentassist;

import junit.framework.Test;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.testplugin.JavaProjectHelper;

class CompletionTestSetup extends ProjectTestSetup {

	public static IPackageFragment getTestPackage() throws CoreException {
		IJavaProject project= ProjectTestSetup.getProject();
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(project, "src");
		return sourceFolder.createPackageFragment("test1", false, null);
	}
	
	public CompletionTestSetup(Test test) {
		super(test);
	}
	
	/*
	 * @see org.eclipse.jdt.ui.tests.core.ProjectTestSetup#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
	}
	
}
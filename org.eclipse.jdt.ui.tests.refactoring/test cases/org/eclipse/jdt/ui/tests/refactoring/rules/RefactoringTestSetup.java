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
package org.eclipse.jdt.ui.tests.refactoring.rules;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Sets up a project with rtstubs*.jar and compiler, code formatting, code generation, and template options.
 */
public class RefactoringTestSetup extends AbstractRefactoringTestSetup {

	public static final String CONTAINER= "src";
	private IPackageFragmentRoot fgRoot;
	private IPackageFragment fgPackageP;
	private IPackageFragment fgPackageQ;
	private IJavaProject fgJavaTestProject;
	private IPackageFragmentRoot fgJRELibrary;

	public IPackageFragmentRoot getDefaultSourceFolder() throws Exception {
		if (fgRoot != null)
			return fgRoot;
		throw new Exception(RefactoringTestSetup.class.getName() + " not initialized");
	}

	public IPackageFragmentRoot getJRELibrary() throws Exception {
		if (fgJRELibrary != null)
			return fgJRELibrary;
		throw new Exception(RefactoringTestSetup.class.getName() + " not initialized");
	}

	public IJavaProject getProject()throws Exception {
		if (fgJavaTestProject != null)
			return fgJavaTestProject;
		throw new Exception(RefactoringTestSetup.class.getName() + " not initialized");
	}

	public IPackageFragment getPackageP()throws Exception {
		if (fgPackageP != null)
			return fgPackageP;
		throw new Exception(RefactoringTestSetup.class.getName() + " not initialized");
	}

	public IPackageFragment getPackageQ()throws Exception {
		if (fgPackageQ != null)
			return fgPackageQ;
		throw new Exception(RefactoringTestSetup.class.getName() + " not initialized");
	}

	@Override
	public void before() throws Exception {
		super.before();
		if (JavaPlugin.getActivePage() != null)
			JavaPlugin.getActivePage().close(); // Closed perspective is NOT restored in tearDown()!

		fgJavaTestProject= JavaProjectHelper.createJavaProject("TestProject"+System.currentTimeMillis(), "bin");
		fgJRELibrary= addRTJar(fgJavaTestProject);
		fgRoot= JavaProjectHelper.addSourceContainer(fgJavaTestProject, CONTAINER);
		fgPackageP= fgRoot.createPackageFragment("p", true, null);
		fgPackageQ= fgRoot.createPackageFragment("q", true, null);
	}

	protected IPackageFragmentRoot addRTJar(IJavaProject project) throws CoreException {
		return JavaProjectHelper.addRTJar(project);
	}

	@Override
	public void after() {
		try {
			JavaProjectHelper.delete(fgJavaTestProject);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		super.after();
	}
}


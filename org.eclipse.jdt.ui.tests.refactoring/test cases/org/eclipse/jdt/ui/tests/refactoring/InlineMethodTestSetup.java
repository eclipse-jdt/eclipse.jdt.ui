/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

public class InlineMethodTestSetup extends TestSetup {

	private IJavaProject fJavaProject;
	private IPackageFragmentRoot fRoot;
	private static final String CONTAINER= "src";

	private IPackageFragment fInvalid;
	private IPackageFragment fSimple;
	private IPackageFragment fArgument;
	private IPackageFragment fNameConflict;

	public InlineMethodTestSetup(Test test) {
		super(test);
	}

	public IPackageFragmentRoot getRoot() {
		return fRoot;
	}
		
	protected void setUp() throws Exception {
		super.setUp();
		
		fJavaProject= JavaProjectHelper.createJavaProject("TestProject", "bin");
		JavaProjectHelper.addRTJar(fJavaProject);
		fRoot= JavaProjectHelper.addSourceContainer(fJavaProject, CONTAINER);
		
		Refactoring.getUndoManager().flush();
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		IWorkspaceDescription description= workspace.getDescription();
		description.setAutoBuilding(false);
		workspace.setDescription(description);
		
		fInvalid= fRoot.createPackageFragment("invalid", true, null);
		fSimple= fRoot.createPackageFragment("simple_in", true, null);		
		fArgument= fRoot.createPackageFragment("argument_in", true, null);
		fNameConflict= fRoot.createPackageFragment("nameconflict_in", true, null);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		RefactoringTest.performDummySearch(fJavaProject);
		JavaProjectHelper.delete(fJavaProject);
	}
	
	public IPackageFragment getInvalidPackage() {
		return fInvalid;
	}

	public IPackageFragment getSimplePackage() {
		return fSimple;
	}

	public IPackageFragment getArgumentPackage() {
		return fArgument;
	}

	public IPackageFragment getNameConflictPackage() {
		return fNameConflict;
	}
}

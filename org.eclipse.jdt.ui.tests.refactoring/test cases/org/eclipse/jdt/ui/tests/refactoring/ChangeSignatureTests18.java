/**
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.ltk.core.refactoring.RefactoringCore;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;

import org.eclipse.jdt.ui.tests.core.Java18ProjectTestSetup;

public class ChangeSignatureTests18 extends ChangeSignatureTests {

	private static final Class THIS= ChangeSignatureTests18 .class;

	private IJavaProject fJProject1;

	public ChangeSignatureTests18 (String name) {
		super(name);
	}

	@Override
	protected String getRefactoringPath() {
		return "ChangeSignature18/";
	}

	public static Test suite() {
		return new Java18ProjectTestSetup(new NoSuperTestsSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new Java18ProjectTestSetup(new RefactoringTestSetup(test));
	}

	@Override
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, Java18ProjectTestSetup.getDefaultClasspath());
	}

	@Override
	protected void setUp() throws Exception {
		fJProject1= Java18ProjectTestSetup.getProject();
		fRoot= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		fPackageP= fRoot.createPackageFragment("p", true, null);
		fIsPreDeltaTest= false;
		RefactoringCore.getUndoManager().flush();
	}

	// Exchange the method parameters
	public void testLambda0() throws Exception {
		helper1(new String[]{"j", "i"}, new String[]{"I", "I"});
	}

	// Add an extra method parameter
	public void testLambda1() throws Exception {
		String[] signature= { "I" };
		String[] newNames= { "j" };
		String[] newTypes= { "int" };
		String[] newDefaultValues= { "0" };
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= { 1 };
		helperAdd(signature, newParamInfo, newIndices);
	}

	// Add a new method parameter to an empty parameter method
	public void testLambda2() throws Exception {
		String[] signature= {};
		String[] newNames= { "x" };
		String[] newTypes= { "int" };
		String[] newDefaultValues= { "0" };
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= { 0 };
		helperAdd(signature, newParamInfo, newIndices);
	}

	// Rename method
	public void testLambda3() throws Exception {
		String[] signature= { "QString;" };
		helperRenameMethod(signature, "newName", false, true);
	}

	// Rename method involving method reference
	public void testMethodReference0() throws Exception {
		String[] signature= {};
		helperRenameMethod(signature, "newName", false, true);
	}

	// TODO Remove a method parameter
}
/**
 * Copyright (c) 2014 IBM Corporation and others.
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

package org.eclipse.jdt.ui.tests.refactoring;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.ltk.core.refactoring.RefactoringCore;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;

import org.eclipse.jdt.ui.tests.CustomBaseRunner;
import org.eclipse.jdt.ui.tests.IgnoreInheritedTests;
import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class ChangeSignatureTests1d8 extends ChangeSignatureTests {
	@Rule
	public Java1d8ProjectTestSetup jps= new Java1d8ProjectTestSetup();

	private IJavaProject fJProject1;

	@Override
	protected String getRefactoringPath() {
		return "ChangeSignature18/";
	}

	@Override
	public void genericafter() throws Exception {
		JavaProjectHelper.clear(fJProject1, jps.getDefaultClasspath());
	}

	@Override
	public void genericbefore() throws Exception {
		fJProject1= jps.getProject();
		fRoot= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		fPackageP= fRoot.createPackageFragment("p", true, null);
		fIsPreDeltaTest= false;
		RefactoringCore.getUndoManager().flush();
	}

	// Exchange the method parameters
	@Test
	public void testLambda0() throws Exception {
		helper1(new String[]{"j", "i"}, new String[]{"I", "I"});
	}

	// Add an extra method parameter
	@Test
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
	@Test
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
	@Test
	public void testLambda3() throws Exception {
		String[] signature= { "QString;" };
		helperRenameMethod(signature, "newName", false, true);
	}

	// Rename method involving method reference
	@Test
	public void testMethodReference0() throws Exception {
		String[] signature= {};
		helperRenameMethod(signature, "newName", false, true);
	}

	// TODO Remove a method parameter
}
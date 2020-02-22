/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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

package org.eclipse.jdt.junit.tests;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.internal.junit.model.JUnitModel;

import org.eclipse.jdt.ui.tests.core.rules.JUnitWorkspaceTestSetup;

public class TestRunSessionSerializationTests3 extends AbstractTestRunSessionSerializationTests {

	@Rule
	public JUnitWorkspaceTestSetup jwts= new JUnitWorkspaceTestSetup(false);

	@Test
	public void testATestCase() throws Exception {
		String test= "ATestCase";
		runCUTest(test);
	}

	@Test
	public void testATestCase_testSucceed() throws Exception {
		String testType= "ATestCase";
		String method= "testSucceed";
		runMethodTest(testType, method);
	}

	@Test
	public void testATestSuite() throws Exception {
		String test= "ATestSuite";
		runCUTest(test);
	}

	@Test
	public void testFailures() throws Exception {
		String test= "Failures";
		runCUTest(test);
	}

	@Test
	public void testAllTests() throws Exception {
		String test= "AllTests";
		runCUTest(test);
	}

	@Test
	public void testFailingSuite() throws Exception {
		String test= "FailingSuite";
		runCUTest(test);
	}

	@Test
	public void testImportAntSuite() throws Exception {
		Path testsPath= new Path(JUnitWorkspaceTestSetup.getProjectPath() + "ant/result/TESTS-TestSuites.xml");
		File testsFile= JavaTestPlugin.getDefault().getFileInPlugin(testsPath);
		JUnitModel.importTestRunSession(testsFile); // no contents check for now...
	}

}

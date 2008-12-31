/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.junit.tests;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.internal.junit.model.JUnitModel;

public class TestRunSessionSerializationTests3 extends AbstractTestRunSessionSerializationTests {

	public static Test setUpTest(Test test) {
		return new JUnitWorkspaceTestSetup(test, false);
	}

	public static Test suite() {
		return new JUnitWorkspaceTestSetup(new TestSuite(TestRunSessionSerializationTests3.class), false);
	}

	public void testATestCase() throws Exception {
		String test= "ATestCase";
		runCUTest(test);
	}

	public void testATestCase_testSucceed() throws Exception {
		String testType= "ATestCase";
		String method= "testSucceed";
		runMethodTest(testType, method);
	}

	public void testATestSuite() throws Exception {
		String test= "ATestSuite";
		runCUTest(test);
	}

	public void testFailures() throws Exception {
		String test= "Failures";
		runCUTest(test);
	}

	public void testAllTests() throws Exception {
		String test= "AllTests";
		runCUTest(test);
	}

	public void testFailingSuite() throws Exception {
		String test= "FailingSuite";
		runCUTest(test);
	}

	public void testImportAntSuite() throws Exception {
		Path testsPath= new Path(JUnitWorkspaceTestSetup.getProjectPath() + "ant/result/TESTS-TestSuites.xml");
		File testsFile= JavaTestPlugin.getDefault().getFileInPlugin(testsPath);
		JUnitModel.importTestRunSession(testsFile); // no contents check for now...
	}

}

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
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.junit.model.JUnitModel;

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.JavaRuntime;

public class TestRunSessionSerializationTests4 extends AbstractTestRunSessionSerializationTests {

	public static Test setUpTest(Test test) {
		IVMInstall defaultVMInstall= JavaRuntime.getDefaultVMInstall();
		if (defaultVMInstall instanceof IVMInstall2) {
			IVMInstall2 install2= (IVMInstall2) defaultVMInstall;
			String version= JavaModelUtil.getCompilerCompliance(install2, JavaCore.VERSION_1_4);
			if (JavaModelUtil.is50OrHigher(version))
				return new JUnitWorkspaceTestSetup(test, true);
		}

		return new TestCase("TestRunSessionSerializationTests4 disabled because VM < 5.0") {
			public void run(TestResult result) {
				result.startTest(this);
				result.endTest(this);
			}
		};
	}

	public static Test suite() {
		return setUpTest(new TestSuite(TestRunSessionSerializationTests4.class));
	}

	public void testATestCase() throws Exception {
		String test= "ATestCase";
		runCUTest(test);
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

	public void testImportAntSuite() throws Exception {
		Path testsPath= new Path(JUnitWorkspaceTestSetup.getProjectPath() + "ant/result/TESTS-TestSuites.xml");
		File testsFile= JavaTestPlugin.getDefault().getFileInPlugin(testsPath);
		JUnitModel.importTestRunSession(testsFile); // no contents check for now...
	}

}

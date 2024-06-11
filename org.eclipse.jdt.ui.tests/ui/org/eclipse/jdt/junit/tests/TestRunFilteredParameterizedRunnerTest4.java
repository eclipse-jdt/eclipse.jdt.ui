/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
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

import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestElement.ProgressState;
import org.eclipse.jdt.junit.model.ITestElement.Result;
import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;

public class TestRunFilteredParameterizedRunnerTest4 extends AbstractTestRunListenerTest {

	private IType fATestCase;

	private String[] runTreeTest(IType typeToLaunch, String testName, int step) throws Exception {
		TestRunLog log= new TestRunLog();
		final TestRunListener testRunListener= new TestRunListeners.TreeTest(log, step);
		JUnitCore.addTestRunListener(testRunListener);
		try {
			return launchJUnit(typeToLaunch, TestKindRegistry.JUNIT4_TEST_KIND_ID, testName, log);
		} finally {
			JUnitCore.removeTestRunListener(testRunListener);
		}
	}

	@Override
	@Before
	public void setUp() throws Exception {
		fProject= JavaProjectHelper.createJavaProject("TestRunListenerTest", "bin");
		JavaProjectHelper.addToClasspath(fProject, JavaCore.newContainerEntry(JUnitCore.JUNIT4_CONTAINER_PATH));
		JavaProjectHelper.addRTJar15(fProject);
		String source=
				"""
			package pack;
			
			import java.util.Arrays;
			
			import org.junit.Assert;
			import org.junit.Test;
			import org.junit.runner.RunWith;
			import org.junit.runners.Parameterized;
			import org.junit.runners.Parameterized.Parameter;
			import org.junit.runners.Parameterized.Parameters;
			
			@RunWith(Parameterized.class)
			public class ATestCase {
			
				@Parameters
				public static Iterable<Object[]> data() {
					return Arrays.asList(new Object[][] { { 6 }, { 12 } });
				}
			
				@Parameter
				public int param;
			
				@Test
				public void testDiv() {
					Assert.assertEquals(0, param % 2);
				}
			
				@Test
				public void testDiv2() {
					Assert.assertEquals(0, param % 3);
				}
			}
			;""";
		fATestCase= createType(source, "pack", "ATestCase.java");
	}

	@Test
	public void testMatchRoot() throws Exception {
		String[] expectedSequence= new String[] {
				TestRunListeners.sessionAsString("ATestCase pack.ATestCase", ProgressState.COMPLETED, Result.OK, 0),
				TestRunListeners.suiteAsString("pack.ATestCase", ProgressState.COMPLETED, Result.OK, null, 1),
				TestRunListeners.suiteAsString("[0]", ProgressState.COMPLETED, Result.OK, null, 2),
				TestRunListeners.testCaseAsString("testDiv[0]", "pack.ATestCase", ProgressState.COMPLETED, Result.OK, null, 3),
				TestRunListeners.testCaseAsString("testDiv2[0]", "pack.ATestCase", ProgressState.COMPLETED, Result.OK, null, 3),
				TestRunListeners.suiteAsString("[1]", ProgressState.COMPLETED, Result.OK, null, 2),
				TestRunListeners.testCaseAsString("testDiv[1]", "pack.ATestCase", ProgressState.COMPLETED, Result.OK, null, 3),
				TestRunListeners.testCaseAsString("testDiv2[1]", "pack.ATestCase", ProgressState.COMPLETED, Result.OK, null, 3),
		};
		String[] actual= runTreeTest(fATestCase, "pack.ATestCase", 10);
		assertEqualLog(expectedSequence, actual);
	}
	@Test
	public void testMatchSubtree1ByName() throws Exception {
		String[] expectedSequence= new String[] {
				TestRunListeners.sessionAsString("ATestCase [0]", ProgressState.COMPLETED, Result.OK, 0),
				TestRunListeners.suiteAsString("[0]", ProgressState.COMPLETED, Result.OK, null, 1),
				TestRunListeners.testCaseAsString("testDiv[0]", "pack.ATestCase", ProgressState.COMPLETED, Result.OK, null, 2),
				TestRunListeners.testCaseAsString("testDiv2[0]", "pack.ATestCase", ProgressState.COMPLETED, Result.OK, null, 2),
		};
		String[] actual= runTreeTest(fATestCase, "[0]", 6);
		assertEqualLog(expectedSequence, actual);
	}
	@Test
	public void testMatchSubtree1Leaf1ByName() throws Exception {
		String[] expectedSequence= new String[] {
				TestRunListeners.sessionAsString("ATestCase testDiv[0]", ProgressState.COMPLETED, Result.OK, 0),
				TestRunListeners.testCaseAsString("testDiv[0]", "pack.ATestCase", ProgressState.COMPLETED, Result.OK, null, 1),
		};
		String[] actual= runTreeTest(fATestCase, "testDiv[0]", 4);
		assertEqualLog(expectedSequence, actual);
	}
	@Test
	public void testMatchSubtree1Leaf1ByNameAndClass() throws Exception {
		String[] expectedSequence= new String[] {
				TestRunListeners.sessionAsString("ATestCase testDiv[0]", ProgressState.COMPLETED, Result.OK, 0),
				TestRunListeners.testCaseAsString("testDiv[0]", "pack.ATestCase", ProgressState.COMPLETED, Result.OK, null, 1),
		};
		String[] actual2= runTreeTest(fATestCase, "testDiv[0](pack.ATestCase)", 4);
		assertEqualLog(expectedSequence, actual2);
	}
	@Test
	public void testMatchSubtree1Leaf2ByName() throws Exception {
		String[] expectedSequence= new String[] {
				TestRunListeners.sessionAsString("ATestCase testDiv[1]", ProgressState.COMPLETED, Result.OK, 0),
				TestRunListeners.testCaseAsString("testDiv[1]", "pack.ATestCase", ProgressState.COMPLETED, Result.OK, null, 1),
		};
		String[] actual= runTreeTest(fATestCase, "testDiv[1]", 4);
		assertEqualLog(expectedSequence, actual);
	}
	@Test
	public void testMatchSubtree2ByName() throws Exception {
		String[] expectedSequence= new String[] {
				TestRunListeners.sessionAsString("ATestCase [1]", ProgressState.COMPLETED, Result.OK, 0),
				TestRunListeners.suiteAsString("[1]", ProgressState.COMPLETED, Result.OK, null, 1),
				TestRunListeners.testCaseAsString("testDiv[1]", "pack.ATestCase", ProgressState.COMPLETED, Result.OK, null, 2),
				TestRunListeners.testCaseAsString("testDiv2[1]", "pack.ATestCase", ProgressState.COMPLETED, Result.OK, null, 2),
		};
		String[] actual= runTreeTest(fATestCase, "[1]", 6);
		assertEqualLog(expectedSequence, actual);
	}
	@Test
	public void testMatchAllFirstLeafs() throws Exception {
		String[] expectedSequence= new String[] {
				TestRunListeners.sessionAsString("ATestCase testDiv", ProgressState.COMPLETED, Result.OK, 0),
				TestRunListeners.suiteAsString("pack.ATestCase", ProgressState.COMPLETED, Result.OK, null, 1),
				TestRunListeners.suiteAsString("[0]", ProgressState.COMPLETED, Result.OK, null, 2),
				TestRunListeners.testCaseAsString("testDiv[0]", "pack.ATestCase", ProgressState.COMPLETED, Result.OK, null, 3),
				TestRunListeners.suiteAsString("[1]", ProgressState.COMPLETED, Result.OK, null, 2),
				TestRunListeners.testCaseAsString("testDiv[1]", "pack.ATestCase", ProgressState.COMPLETED, Result.OK, null, 3),
		};
		String[] actual= runTreeTest(fATestCase, "testDiv", 6);
		assertEqualLog(expectedSequence, actual);
	}

}

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
import org.eclipse.jdt.junit.model.ITestElement.FailureTrace;
import org.eclipse.jdt.junit.model.ITestElement.ProgressState;
import org.eclipse.jdt.junit.model.ITestElement.Result;
import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;

public class TestRunListenerTest4 extends AbstractTestRunListenerTest {

	private String[] runSequenceTest(IType typeToLaunch) throws Exception {
		TestRunLog log= new TestRunLog();
		final TestRunListener testRunListener= new TestRunListeners.SequenceTest(log);
		JUnitCore.addTestRunListener(testRunListener);
		try {
			return launchJUnit(typeToLaunch, log);
		} finally {
			JUnitCore.removeTestRunListener(testRunListener);
		}
	}

	private String[] runTreeTest(IType typeToLaunch, int step) throws Exception {
		TestRunLog log= new TestRunLog();
		final TestRunListener testRunListener= new TestRunListeners.TreeTest(log, step);
		JUnitCore.addTestRunListener(testRunListener);
		try {
			return launchJUnit(typeToLaunch, TestKindRegistry.JUNIT4_TEST_KIND_ID, log);
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
	}

	@Test
	public void testOK() throws Exception {
		String source=
				"""
			package pack;
			import org.junit.Test;
			public class ATestCase {
			    @Test public void testSucceed() { }
			}""";
		IType aTestCase= createType(source, "pack", "ATestCase.java");

		String[] expectedSequence= new String[] {
			"sessionStarted-" + TestRunListeners.sessionAsString("ATestCase", ProgressState.RUNNING, Result.UNDEFINED, 0),
			"testCaseStarted-" + TestRunListeners.testCaseAsString("testSucceed", "pack.ATestCase", ProgressState.RUNNING, Result.UNDEFINED, null, 0),
			"testCaseFinished-" + TestRunListeners.testCaseAsString("testSucceed", "pack.ATestCase", ProgressState.COMPLETED, Result.OK, null, 0),
			"sessionFinished-" + TestRunListeners.sessionAsString("ATestCase", ProgressState.COMPLETED, Result.OK, 0)
		};
		String[] actual= runSequenceTest(aTestCase);
		assertEqualLog(expectedSequence, actual);
	}
	@Test
	public void testTreeOK() throws Exception {
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
					return Arrays.asList(new Object[][] { { 2 }, { 4 } });
				}
			
				@Parameter
				public int param;
			
				@Test
				public void testEven() {
					Assert.assertEquals(0, param % 2);
				}
			}
			""";
		IType aTestCase= createType(source, "pack", "ATestCase.java");

		String[] expectedSequence= new String[] {
				TestRunListeners.sessionAsString("ATestCase", ProgressState.COMPLETED, Result.OK, 0),
				TestRunListeners.suiteAsString("pack.ATestCase", ProgressState.COMPLETED, Result.OK, null, 1),
				TestRunListeners.suiteAsString("[0]", ProgressState.COMPLETED, Result.OK, null, 2),
				TestRunListeners.testCaseAsString("testEven[0]", "pack.ATestCase", ProgressState.COMPLETED, Result.OK, null, 3),
				TestRunListeners.suiteAsString("[1]", ProgressState.COMPLETED, Result.OK, null, 2),
				TestRunListeners.testCaseAsString("testEven[1]", "pack.ATestCase", ProgressState.COMPLETED, Result.OK, null, 3),
		};
		String[] actual= runTreeTest(aTestCase, 6);
		assertEqualLog(expectedSequence, actual);
	}

	@Test
	public void testFail() throws Exception {
		String source=
			"""
			package pack;
			import org.junit.Test;
			import static org.junit.Assert.*;
			public class ATestCase {
			    @Test public void testFail() { fail(); }
			}""";
		IType aTestCase= createType(source, "pack", "ATestCase.java");

		String[] expectedSequence= new String[] {
			"sessionStarted-" + TestRunListeners.sessionAsString("ATestCase", ProgressState.RUNNING, Result.UNDEFINED, 0),
			"testCaseStarted-" + TestRunListeners.testCaseAsString("testFail", "pack.ATestCase", ProgressState.RUNNING, Result.UNDEFINED, null, 0),
			"testCaseFinished-" + TestRunListeners.testCaseAsString("testFail", "pack.ATestCase", ProgressState.COMPLETED, Result.FAILURE, new FailureTrace("java.lang.AssertionError", null, null), 0),
			"sessionFinished-" + TestRunListeners.sessionAsString("ATestCase", ProgressState.COMPLETED, Result.FAILURE, 0)
		};
		String[] actual= runSequenceTest(aTestCase);
		assertEqualLog(expectedSequence, actual);
	}

	@Test
	public void testSimpleTest() throws Exception {
		String source=
			"""
			package pack;
			import org.junit.Test;
			import org.junit.FixMethodOrder;
			import org.junit.runners.MethodSorters;
			import static org.junit.Assert.*;
			
			@FixMethodOrder(MethodSorters.NAME_ASCENDING)
			public class ATestCase {
				protected int fValue1;
				protected int fValue2;
			
				protected void setUp() {
					fValue1= 2;
					fValue2= 3;
				}
				@Test public void testAdd() {
					double result= fValue1 + fValue2;
					// forced failure result == 5
					assertTrue(result == 6);
				}
				@Test public void testDivideByZero() {
					int zero= 0;
					int result= 8/zero;
				}
				@Test public void testEquals() {
					assertEquals(12, 12);
					assertEquals(12L, 12L);
					assertEquals(Long.valueOf(12), Long.valueOf(12));
			
					assertEquals("Size", String.valueOf(12), String.valueOf(13));
				}
			}""";
		IType aTestCase= createType(source, "pack", "ATestCase.java");

		String[] expectedSequence= new String[] {
			"sessionStarted-" + TestRunListeners.sessionAsString("ATestCase", ProgressState.RUNNING, Result.UNDEFINED, 0),
			"testCaseStarted-" + TestRunListeners.testCaseAsString("testAdd", "pack.ATestCase", ProgressState.RUNNING, Result.UNDEFINED, null, 0),
			"testCaseFinished-" + TestRunListeners.testCaseAsString("testAdd", "pack.ATestCase", ProgressState.COMPLETED, Result.FAILURE, new FailureTrace("java.lang.AssertionError", null, null), 0),
			"testCaseStarted-" + TestRunListeners.testCaseAsString("testDivideByZero", "pack.ATestCase", ProgressState.RUNNING, Result.UNDEFINED, null, 0),
			"testCaseFinished-" + TestRunListeners.testCaseAsString("testDivideByZero", "pack.ATestCase", ProgressState.COMPLETED, Result.ERROR, new FailureTrace("java.lang.ArithmeticException", null, null), 0),
			"testCaseStarted-" + TestRunListeners.testCaseAsString("testEquals", "pack.ATestCase", ProgressState.RUNNING, Result.UNDEFINED, null, 0),
			"testCaseFinished-" + TestRunListeners.testCaseAsString("testEquals", "pack.ATestCase", ProgressState.COMPLETED, Result.FAILURE, new FailureTrace("org.junit.ComparisonFailure", "12", "13"), 0),
			"sessionFinished-" + TestRunListeners.sessionAsString("ATestCase", ProgressState.COMPLETED, Result.ERROR, 0)
		};
		String[] actual= runSequenceTest(aTestCase);
		assertEqualLog(expectedSequence, actual);
	}


	@Test
	public void testTreeOnSessionStarted() throws Exception {
		String source=
				"""
			package pack;
			import org.junit.Test;
			public class ATestCase {
			    @Test public void testSucceed() { }
			}""";
		IType aTestCase= createType(source, "pack", "ATestCase.java");

		String[] expectedTree= new String[] {
			TestRunListeners.sessionAsString("ATestCase", ProgressState.RUNNING, Result.UNDEFINED, 0),
			TestRunListeners.suiteAsString("pack.ATestCase", ProgressState.NOT_STARTED, Result.UNDEFINED, null, 1),
			TestRunListeners.testCaseAsString("testSucceed", "pack.ATestCase", ProgressState.NOT_STARTED, Result.UNDEFINED, null, 2),
		};
		String[] actual= runTreeTest(aTestCase, 1);
		assertEqualLog(expectedTree, actual);
	}

	@Test
	public void testTreeOnSessionEnded() throws Exception {
		String source=
				"""
			package pack;
			import org.junit.Test;
			import static org.junit.Assert.*;
			public class ATestCase {
			    @Test public void testFail() { fail(); }
			}""";
		IType aTestCase= createType(source, "pack", "ATestCase.java");

		String[] expectedTree= new String[] {
			TestRunListeners.sessionAsString("ATestCase", ProgressState.COMPLETED, Result.FAILURE, 0),
			TestRunListeners.suiteAsString("pack.ATestCase", ProgressState.COMPLETED, Result.FAILURE, null, 1),
			TestRunListeners.testCaseAsString("testFail", "pack.ATestCase", ProgressState.COMPLETED, Result.FAILURE, new FailureTrace("java.lang.AssertionError", null, null), 2),
		};
		String[] actual= runTreeTest(aTestCase, 4);
		assertEqualLog(expectedTree, actual);
	}

	@Test
	public void testTreeOnSecondTestStarted() throws Exception {
		String source=
				"""
			package pack;
			import org.junit.Test;
			import org.junit.FixMethodOrder;
			import org.junit.runners.MethodSorters;
			import static org.junit.Assert.*;
			
			@FixMethodOrder(MethodSorters.NAME_ASCENDING)
			public class ATestCase {
			    @Test public void test1Succeed() { }
			    @Test public void test2Fail() { fail(); }
			}""";
		IType aTestCase= createType(source, "pack", "ATestCase.java");

		String[] expectedTree= new String[] {
			TestRunListeners.sessionAsString("ATestCase", ProgressState.RUNNING, Result.UNDEFINED, 0),
			TestRunListeners.suiteAsString("pack.ATestCase", ProgressState.RUNNING, Result.UNDEFINED, null, 1),
			TestRunListeners.testCaseAsString("test1Succeed", "pack.ATestCase", ProgressState.COMPLETED, Result.OK, null, 2),
			TestRunListeners.testCaseAsString("test2Fail", "pack.ATestCase", ProgressState.RUNNING, Result.UNDEFINED, null, 2),
		};
		String[] actual= runTreeTest(aTestCase, 4);
		assertEqualLog(expectedTree, actual);
	}

	@Test
	public void testTreeOnSecondTestStarted2() throws Exception {
		String source=
				"""
			package pack;
			import org.junit.Test;
			import org.junit.FixMethodOrder;
			import org.junit.runners.MethodSorters;
			import static org.junit.Assert.*;
			
			@FixMethodOrder(MethodSorters.NAME_ASCENDING)
			public class ATestCase {
			    @Test public void test2Succeed() { }
			    @Test public void test1Fail() { fail(); }
			}""";

		IType aTestCase= createType(source, "pack", "ATestCase.java");

		String[] expectedTree= new String[] {
			TestRunListeners.sessionAsString("ATestCase", ProgressState.RUNNING, Result.FAILURE, 0),
			TestRunListeners.suiteAsString("pack.ATestCase", ProgressState.RUNNING, Result.FAILURE, null, 1),
			TestRunListeners.testCaseAsString("test1Fail", "pack.ATestCase", ProgressState.COMPLETED, Result.FAILURE, new FailureTrace("java.lang.AssertionError", null, null), 2),
			TestRunListeners.testCaseAsString("test2Succeed", "pack.ATestCase", ProgressState.RUNNING, Result.UNDEFINED, null, 2),
		};
		String[] actual= runTreeTest(aTestCase, 4);
		assertEqualLog(expectedTree, actual);
	}

	@Test
	public void testParametrizedWithEvilChars() throws Exception {
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
			
				@Parameters(name = "{index}: testEven({0})")
				public static Iterable<String[]> data() {
					return Arrays.asList(new String[][] { { "2" }, { "4\\n" }, { "6\\r" }, { "8\\r\\n" }, { "0\\\\," } });
				}
			
				@Parameter
				public String param;
			
				@Test
				public void testEven() {
					Assert.assertEquals(0, Integer.parseInt(param.trim()) % 2);
				}
			}
			""";
		IType aTestCase= createType(source, "pack", "ATestCase.java");

		String[] expectedSequence= new String[] {
				TestRunListeners.sessionAsString("ATestCase", ProgressState.COMPLETED, Result.ERROR, 0),
				TestRunListeners.suiteAsString("pack.ATestCase", ProgressState.COMPLETED, Result.ERROR, null, 1),
				TestRunListeners.suiteAsString("[0: testEven(2)]", ProgressState.COMPLETED, Result.OK, null, 2),
				TestRunListeners.testCaseAsString("testEven[0: testEven(2)]", "pack.ATestCase", ProgressState.COMPLETED, Result.OK, null, 3),
				TestRunListeners.suiteAsString("[1: testEven(4 )]", ProgressState.COMPLETED, Result.OK, null, 2),
				TestRunListeners.testCaseAsString("testEven[1: testEven(4 )]", "pack.ATestCase", ProgressState.COMPLETED, Result.OK, null, 3),
				TestRunListeners.suiteAsString("[2: testEven(6 )]", ProgressState.COMPLETED, Result.OK, null, 2),
				TestRunListeners.testCaseAsString("testEven[2: testEven(6 )]", "pack.ATestCase", ProgressState.COMPLETED, Result.OK, null, 3),
				TestRunListeners.suiteAsString("[3: testEven(8 )]", ProgressState.COMPLETED, Result.OK, null, 2),
				TestRunListeners.testCaseAsString("testEven[3: testEven(8 )]", "pack.ATestCase", ProgressState.COMPLETED, Result.OK, null, 3),
				TestRunListeners.suiteAsString("[4: testEven(0\\,)]", ProgressState.COMPLETED, Result.ERROR, null, 2),
				TestRunListeners.testCaseAsString("testEven[4: testEven(0\\,)]", "pack.ATestCase", ProgressState.COMPLETED, Result.ERROR, new FailureTrace("java.lang.NumberFormatException", null, null), 3),
		};
		String[] actual= runTreeTest(aTestCase, 12);
		assertEqualLog(expectedSequence, actual);
	}
}

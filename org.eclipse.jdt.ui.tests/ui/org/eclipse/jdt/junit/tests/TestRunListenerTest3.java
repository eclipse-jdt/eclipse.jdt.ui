/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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

import org.junit.Test;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestElement.FailureTrace;
import org.eclipse.jdt.junit.model.ITestElement.ProgressState;
import org.eclipse.jdt.junit.model.ITestElement.Result;
import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.JUnitMessages;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;

public class TestRunListenerTest3 extends AbstractTestRunListenerTest {

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
			return launchJUnit(typeToLaunch, TestKindRegistry.JUNIT3_TEST_KIND_ID, log);
		} finally {
			JUnitCore.removeTestRunListener(testRunListener);
		}
	}
	@Test
	public void testOK() throws Exception {
		String source=
				"""
			package pack;
			import junit.framework.TestCase;
			public class ATestCase extends TestCase {
			    public void testSucceed() { }
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
	public void testFail() throws Exception {
		String source=
			"""
			package pack;
			import junit.framework.TestCase;
			public class ATestCase extends TestCase {
			    public void testFail() { fail(); }
			}""";
		IType aTestCase= createType(source, "pack", "ATestCase.java");

		String[] expectedSequence= new String[] {
			"sessionStarted-" + TestRunListeners.sessionAsString("ATestCase", ProgressState.RUNNING, Result.UNDEFINED, 0),
			"testCaseStarted-" + TestRunListeners.testCaseAsString("testFail", "pack.ATestCase", ProgressState.RUNNING, Result.UNDEFINED, null, 0),
			"testCaseFinished-" + TestRunListeners.testCaseAsString("testFail", "pack.ATestCase", ProgressState.COMPLETED, Result.FAILURE, new FailureTrace("junit.framework.AssertionFailedError", null, null), 0),
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
			import junit.framework.*;
			
			public class ATestCase extends TestCase {
				protected int fValue1;
				protected int fValue2;
			
				public ATestCase(String name) {
					super(name);
				}
				protected void setUp() {
					fValue1= 2;
					fValue2= 3;
				}
				public static Test suite() {
					// ensure ordering:
					TestSuite result= new TestSuite("ATestCase");
					result.addTest(new ATestCase("testAdd"));
					result.addTest(new ATestCase("testDivideByZero"));
					result.addTest(new ATestCase("testEquals"));
					return result;
				}
				public void testAdd() {
					double result= fValue1 + fValue2;
					// forced failure result == 5
					assertTrue(result == 6);
				}
				public void testDivideByZero() {
					int zero= 0;
					int result= 8/zero;
				}
				public void testEquals() {
					assertEquals(12, 12);
					assertEquals(12L, 12L);
					assertEquals(new Long(12), new Long(12));
			
					assertEquals("Size", String.valueOf(12), String.valueOf(13));
				}
				public static void main (String[] args) {
					junit.textui.TestRunner.run(suite());
				}
			}""";
		IType aTestCase= createType(source, "pack", "ATestCase.java");

		String[] expectedSequence= new String[] {
			"sessionStarted-" + TestRunListeners.sessionAsString("ATestCase", ProgressState.RUNNING, Result.UNDEFINED, 0),
			"testCaseStarted-" + TestRunListeners.testCaseAsString("testAdd", "pack.ATestCase", ProgressState.RUNNING, Result.UNDEFINED, null, 0),
			"testCaseFinished-" + TestRunListeners.testCaseAsString("testAdd", "pack.ATestCase", ProgressState.COMPLETED, Result.FAILURE, new FailureTrace("junit.framework.AssertionFailedError", null, null), 0),
			"testCaseStarted-" + TestRunListeners.testCaseAsString("testDivideByZero", "pack.ATestCase", ProgressState.RUNNING, Result.UNDEFINED, null, 0),
			"testCaseFinished-" + TestRunListeners.testCaseAsString("testDivideByZero", "pack.ATestCase", ProgressState.COMPLETED, Result.ERROR, new FailureTrace("java.lang.ArithmeticException", null, null), 0),
			"testCaseStarted-" + TestRunListeners.testCaseAsString("testEquals", "pack.ATestCase", ProgressState.RUNNING, Result.UNDEFINED, null, 0),
			"testCaseFinished-" + TestRunListeners.testCaseAsString("testEquals", "pack.ATestCase", ProgressState.COMPLETED, Result.FAILURE, new FailureTrace("junit.framework.ComparisonFailure", "12", "13"), 0),
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
			import junit.framework.TestCase;
			public class ATestCase extends TestCase {
			    public void testSucceed() { }
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
			import junit.framework.TestCase;
			public class ATestCase extends TestCase {
			    public void testFail() { fail(); }
			}""";
		IType aTestCase= createType(source, "pack", "ATestCase.java");

		String[] expectedTree= new String[] {
			TestRunListeners.sessionAsString("ATestCase", ProgressState.COMPLETED, Result.FAILURE, 0),
			TestRunListeners.suiteAsString("pack.ATestCase", ProgressState.COMPLETED, Result.FAILURE, null, 1),
			TestRunListeners.testCaseAsString("testFail", "pack.ATestCase", ProgressState.COMPLETED, Result.FAILURE, new FailureTrace("junit.framework.AssertionFailedError", null, null), 2),
		};
		String[] actual= runTreeTest(aTestCase, 4);
		assertEqualLog(expectedTree, actual);
	}
	@Test
	public void testTreeOnSecondTestStarted() throws Exception {
		String source=
				"""
			package pack;
			import junit.framework.*;
			public class ATestCase extends TestCase {
			    public static Test suite() {
			        // ensure ordering:
			        TestSuite result= new TestSuite("pack.ATestCase");
			        result.addTest(new ATestCase("testSucceed"));
			        result.addTest(new ATestCase("testFail"));
			        return result;
			    }
			    public ATestCase(String name) {
			        super(name);
			    }
			    public void testSucceed() { }
			    public void testFail() { fail(); }
			}""";
		IType aTestCase= createType(source, "pack", "ATestCase.java");

		String[] expectedTree= new String[] {
			TestRunListeners.sessionAsString("ATestCase", ProgressState.RUNNING, Result.UNDEFINED, 0),
			TestRunListeners.suiteAsString("pack.ATestCase", ProgressState.RUNNING, Result.UNDEFINED, null, 1),
			TestRunListeners.testCaseAsString("testSucceed", "pack.ATestCase", ProgressState.COMPLETED, Result.OK, null, 2),
			TestRunListeners.testCaseAsString("testFail", "pack.ATestCase", ProgressState.RUNNING, Result.UNDEFINED, null, 2),
		};
		String[] actual= runTreeTest(aTestCase, 4);
		assertEqualLog(expectedTree, actual);
	}
	@Test
	public void testTreeOnSecondTestStarted2() throws Exception {
		String source=
				"""
			package pack;
			import junit.framework.*;
			public class ATestCase extends TestCase {
			    public static Test suite() {
			        // ensure ordering:
			        TestSuite result= new TestSuite("pack.ATestCase");
			        result.addTest(new ATestCase("testFail"));
			        result.addTest(new ATestCase("testSucceed"));
			        return result;
			    }
			    public ATestCase(String name) {
			        super(name);
			    }
			    public void testFail() { fail(); }
			    public void testSucceed() { }
			}""";
		IType aTestCase= createType(source, "pack", "ATestCase.java");

		String[] expectedTree= new String[] {
			TestRunListeners.sessionAsString("ATestCase", ProgressState.RUNNING, Result.FAILURE, 0),
			TestRunListeners.suiteAsString("pack.ATestCase", ProgressState.RUNNING, Result.FAILURE, null, 1),
			TestRunListeners.testCaseAsString("testFail", "pack.ATestCase", ProgressState.COMPLETED, Result.FAILURE, new FailureTrace("junit.framework.AssertionFailedError", null, null), 2),
			TestRunListeners.testCaseAsString("testSucceed", "pack.ATestCase", ProgressState.RUNNING, Result.UNDEFINED, null, 2),
		};
		String[] actual= runTreeTest(aTestCase, 4);
		assertEqualLog(expectedTree, actual);
	}
	@Test
	public void testTreeUnrootedEnded() throws Exception {
		// regression test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=153807
		String source=
				"""
			package pack;
			
			import junit.framework.TestCase;
			import junit.framework.TestResult;
			import junit.framework.TestSuite;
			
			public class ATestCase extends TestCase {
			    public static class RealTest extends TestCase {
			        public RealTest(String name) {
			            super(name);
			        }
			
			        public void myTest1() throws Exception { }
			
			        public void myTest2() throws Exception {
			            fail();
			        }
			    }
			
			    public void testAllTests() { }
			
			    public void run(TestResult result) {
			        TestSuite suite = new TestSuite("MySuite");
			        suite.addTest(new RealTest("myTest1"));
			        suite.addTest(new RealTest("myTest2"));
			        suite.run(result);
			    }
			}""";
		IType aTestCase= createType(source, "pack", "ATestCase.java");

		String[] expectedTree= new String[] {
			TestRunListeners.sessionAsString("ATestCase", ProgressState.COMPLETED, Result.FAILURE, 0),
			TestRunListeners.suiteAsString("pack.ATestCase", ProgressState.NOT_STARTED, Result.UNDEFINED, null, 1),
			TestRunListeners.testCaseAsString("testAllTests", "pack.ATestCase", ProgressState.NOT_STARTED, Result.UNDEFINED, null, 2),
			TestRunListeners.suiteAsString(JUnitMessages.TestRunSession_unrootedTests, ProgressState.COMPLETED, Result.FAILURE, null, 1),
			TestRunListeners.testCaseAsString("myTest1", "pack.ATestCase.RealTest", ProgressState.COMPLETED, Result.OK, null, 2),
			TestRunListeners.testCaseAsString("myTest2", "pack.ATestCase.RealTest", ProgressState.COMPLETED, Result.FAILURE, new FailureTrace("junit.framework.AssertionFailedError", null, null), 2),
		};
		String[] actual= runTreeTest(aTestCase, 6);
		assertEqualLog(expectedTree, actual);
	}
	@Test
	public void testTreeJUnit4TestAdapter() throws Exception {
		// regression test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=397747
		IClasspathEntry cpe= JavaCore.newContainerEntry(JUnitCore.JUNIT4_CONTAINER_PATH);
		JavaProjectHelper.clear(fProject, new IClasspathEntry[] { cpe });
		JavaProjectHelper.addRTJar15(fProject);

		String source=
				"""
			package test;
			
			import junit.framework.JUnit4TestAdapter;
			import junit.framework.TestCase;
			import junit.framework.TestSuite;
			
			import org.junit.Test;
			import org.junit.runner.RunWith;
			import org.junit.runners.Suite;
			import org.junit.runners.Suite.SuiteClasses;
			
			public class MyTestSuite {
				public static junit.framework.Test suite() {
					TestSuite suite = new TestSuite();
					suite.addTest(new JUnit4TestAdapter(JUnit4TestSuite.class));
					suite.addTestSuite(JUnit3TestCase.class);
					return suite;
				}
			\t
				@RunWith(Suite.class)
				@SuiteClasses({JUnit4TestCase.class})
				public static class JUnit4TestSuite {}
			\t
				public static class JUnit4TestCase {
					@Test public void testA() {}
					@Test public void testB() {}
				}
			\t
				public static class JUnit3TestCase extends TestCase {
					public void testC() {}
					public void testD() {}
					public void testE() {}
				}
			}
			""";
		IType aTestCase= createType(source, "test", "MyTestSuite.java");

		String[] expectedTree= new String[] {
				TestRunListeners.sessionAsString("MyTestSuite", ProgressState.COMPLETED, Result.OK, 0),
				TestRunListeners.suiteAsString("junit.framework.TestSuite", ProgressState.COMPLETED, Result.OK, null, 1),

				TestRunListeners.suiteAsString("test.MyTestSuite.JUnit4TestSuite", ProgressState.COMPLETED, Result.OK, null, 2),
				TestRunListeners.suiteAsString("test.MyTestSuite.JUnit4TestCase", ProgressState.COMPLETED, Result.OK, null, 3),
				TestRunListeners.testCaseAsString("testA", "test.MyTestSuite.JUnit4TestCase", ProgressState.COMPLETED, Result.OK, null, 4),
				TestRunListeners.testCaseAsString("testB", "test.MyTestSuite.JUnit4TestCase", ProgressState.COMPLETED, Result.OK, null, 4),

				TestRunListeners.suiteAsString("test.MyTestSuite.JUnit3TestCase", ProgressState.COMPLETED, Result.OK, null, 2),
				TestRunListeners.testCaseAsString("testC", "test.MyTestSuite.JUnit3TestCase", ProgressState.COMPLETED, Result.OK, null, 3),
				TestRunListeners.testCaseAsString("testD", "test.MyTestSuite.JUnit3TestCase", ProgressState.COMPLETED, Result.OK, null, 3),
				TestRunListeners.testCaseAsString("testE", "test.MyTestSuite.JUnit3TestCase", ProgressState.COMPLETED, Result.OK, null, 3),
		};
		String[] actual= runTreeTest(aTestCase, 12);
		assertEqualLog(expectedTree, actual);
	}
}

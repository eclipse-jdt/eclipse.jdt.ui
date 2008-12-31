/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.junit.tests;

import org.eclipse.jdt.junit.ITestRunListener;
import org.eclipse.jdt.junit.JUnitCore;

import org.eclipse.jdt.core.IType;

/**
 * @deprecated
 */
public class LegacyTestRunListenerTest extends AbstractTestRunListenerTest {

	private String[] runTest(String source) throws Exception {
		IType aTestCase= createType(source, "pack", "ATestCase.java");

		TestRunLog log= new TestRunLog();

		final LegacyTestRunListener testRunListener= new LegacyTestRunListener(log);
		JUnitCore.addTestRunListener(testRunListener);
		try {
			return launchJUnit(aTestCase, log);
		} finally {
			JUnitCore.removeTestRunListener(testRunListener);
		}
	}

	public void testOK() throws Exception {
		String source=
				"package pack;\n" +
				"import junit.framework.TestCase;\n" +
				"public class ATestCase extends TestCase {\n" +
				"    public void testSucceed() { }\n" +
				"}";
		String[] expectedSequence= new String[] {
			LegacyTestRunListener.testRunStartedMessage(1),
			LegacyTestRunListener.testStartedMessage("2", "testSucceed(pack.ATestCase)"),
			LegacyTestRunListener.testEndedMessage("2", "testSucceed(pack.ATestCase)"),
			LegacyTestRunListener.testRunEndedMessage()
		};
		String[] actual= runTest(source);
		assertEqualLog(expectedSequence, actual);
	}

	public void testFail() throws Exception {
		String source=
			"package pack;\n" +
			"import junit.framework.TestCase;\n" +
			"public class ATestCase extends TestCase {\n" +
			"    public void testFail() { fail(); }\n" +
			"}";
		String[] expectedSequence= new String[] {
				LegacyTestRunListener.testRunStartedMessage(1),
				LegacyTestRunListener.testStartedMessage("2", "testFail(pack.ATestCase)"),
				LegacyTestRunListener.testFailedMessage(ITestRunListener.STATUS_FAILURE, "2", "testFail(pack.ATestCase)"),
				LegacyTestRunListener.testEndedMessage("2", "testFail(pack.ATestCase)"),
				LegacyTestRunListener.testRunEndedMessage()
		};
		String[] actual= runTest(source);
		assertEqualLog(expectedSequence, actual);
	}

	public void testSimpleTest() throws Exception {
		String source=
			"package pack;\n" +
			"import junit.framework.*;\n" +
			"\n" +
			"public class ATestCase extends TestCase {\n" +
			"	protected int fValue1;\n" +
			"	protected int fValue2;\n" +
			"\n" +
			"	public ATestCase(String name) {\n" +
			"		super(name);\n" +
			"	}\n" +
			"	protected void setUp() {\n" +
			"		fValue1= 2;\n" +
			"		fValue2= 3;\n" +
			"	}\n" +
			"	public static Test suite() {\n" +
			"		// ensure ordering:\n" +
			"		TestSuite result= new TestSuite(\"ATestCase\");\n" +
			"		result.addTest(new ATestCase(\"testAdd\"));\n" +
			"		result.addTest(new ATestCase(\"testDivideByZero\"));\n" +
			"		result.addTest(new ATestCase(\"testEquals\"));\n" +
			"		return result;\n" +
			"	}\n" +
			"	public void testAdd() {\n" +
			"		double result= fValue1 + fValue2;\n" +
			"		// forced failure result == 5\n" +
			"		assertTrue(result == 6);\n" +
			"	}\n" +
			"	public void testDivideByZero() {\n" +
			"		int zero= 0;\n" +
			"		int result= 8/zero;\n" +
			"	}\n" +
			"	public void testEquals() {\n" +
			"		assertEquals(12, 12);\n" +
			"		assertEquals(12L, 12L);\n" +
			"		assertEquals(new Long(12), new Long(12));\n" +
			"\n" +
			"		assertEquals(\"Size\", 12, 13);\n" +
			"		assertEquals(\"Capacity\", 12.0, 11.99, 0.0);\n" +
			"	}\n" +
			"	public static void main (String[] args) {\n" +
			"		junit.textui.TestRunner.run(suite());\n" +
			"	}\n" +
			"}";
		String[] expectedSequence= new String[] {
				LegacyTestRunListener.testRunStartedMessage(3),

				LegacyTestRunListener.testStartedMessage("2", "testAdd(pack.ATestCase)"),
				LegacyTestRunListener.testFailedMessage(ITestRunListener.STATUS_FAILURE, "2", "testAdd(pack.ATestCase)"),
				LegacyTestRunListener.testEndedMessage("2", "testAdd(pack.ATestCase)"),

				LegacyTestRunListener.testStartedMessage("3", "testDivideByZero(pack.ATestCase)"),
				LegacyTestRunListener.testFailedMessage(ITestRunListener.STATUS_ERROR, "3", "testDivideByZero(pack.ATestCase)"),
				LegacyTestRunListener.testEndedMessage("3", "testDivideByZero(pack.ATestCase)"),

				LegacyTestRunListener.testStartedMessage("4", "testEquals(pack.ATestCase)"),
				LegacyTestRunListener.testFailedMessage(ITestRunListener.STATUS_FAILURE, "4", "testEquals(pack.ATestCase)"),
				LegacyTestRunListener.testEndedMessage("4", "testEquals(pack.ATestCase)"),

				LegacyTestRunListener.testRunEndedMessage()
		};
		String[] actual= runTest(source);
		assertEqualLog(expectedSequence, actual);
	}

}

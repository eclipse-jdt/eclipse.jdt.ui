/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.junit.tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;

import org.eclipse.swt.widgets.Display;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.junit.model.JUnitModel;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;

import org.eclipse.jdt.testplugin.JavaTestPlugin;
import org.eclipse.jdt.testplugin.util.DisplayHelper;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestCaseElement;
import org.eclipse.jdt.junit.model.ITestElement;
import org.eclipse.jdt.junit.model.ITestRunSession;
import org.eclipse.jdt.junit.model.ITestSuiteElement;
import org.eclipse.jdt.junit.model.ITestElement.FailureTrace;

public class TestRunSessionSerializationTests extends TestCase {
	
	public static Test setUpTest(Test test) {
		return new JUnitWorkspaceTestSetup(test);
	}
	
	public static Test suite() {
		return new JUnitWorkspaceTestSetup(new TestSuite(TestRunSessionSerializationTests.class));
	}
	
	private static class SerializationResult {
		TestRunSession fTestRunSession;
		String fSerialized;
	}
	
	private SerializationResult launchTest(IType typeToLaunch) throws Exception {
		final SerializationResult result= new SerializationResult();

		TestRunListener testRunListener= new TestRunListener() {
			public void sessionFinished(ITestRunSession session) {
				assertNotNull(session);
				result.fTestRunSession= (TestRunSession) session;
			}
		};
		
		JUnitCore.addTestRunListener(testRunListener); 
		try {
			new AbstractTestRunListenerTest().launchJUnit(typeToLaunch);
			assertTrue(new DisplayHelper(){
				protected boolean condition() {
					return result.fTestRunSession != null;
				}
			}.waitForCondition(Display.getCurrent(), 10 * 1000, 100));
		} finally {
			JUnitCore.removeTestRunListener(testRunListener);
		}
		
		ByteArrayOutputStream out= new ByteArrayOutputStream();
		JUnitModel.exportTestRunSession(result.fTestRunSession, out);
		
		result.fSerialized= out.toString("UTF-8");
		return result;
	}

	private void runExportImport(IType test, String expectedXML) throws Exception {
		SerializationResult serializationResult= launchTest(test);
		assertEqualXML(expectedXML, serializationResult.fSerialized);
		
		IFile resultFile= JUnitWorkspaceTestSetup.getJavaProject().getProject().getFile("testresult.xml");
		try {
			resultFile.create(new ByteArrayInputStream(serializationResult.fSerialized.getBytes()), true, null);
			TestRunSession imported= JUnitModel.importTestRunSession(resultFile.getLocation().toFile());
			assertEqualSessions(serializationResult.fTestRunSession, imported);
		} finally {
			if (resultFile.exists())
				try {
					resultFile.delete(true, null);
				} catch (CoreException e) {
					e.printStackTrace();
				}
		}
	}
	
	private void assertEqualXML(String expected, String actual) {
		/*
		 * Just strips all whitespace and avoids comparing stack traces (which are VM-dependent). 
		 */
		
		String regex= "(?m)^\\s*at\\s+[\\w\\.\\:\\;\\$\\(\\)\\[ \\t]+$\r?\n?";
		String replacement= "";
		expected= expected.replaceAll(regex, replacement);
		actual= actual.replaceAll(regex, replacement);
		
		StringTokenizer expTok= new StringTokenizer(expected);
		StringTokenizer actTok= new StringTokenizer(actual);
		if (expTok.countTokens() != actTok.countTokens())
			assertEquals(expected, actual);
		while (expTok.hasMoreElements()) {
			String e= expTok.nextToken();
			String a= actTok.nextToken();
			if (! e.equals(a)) {
				assertEquals(expected, actual);
				fail(a);
			}
		}
	}

	private void assertEqualSessions(TestRunSession expected, TestRunSession actual) {
		assertEquals(expected.getTestRunName(), actual.getTestRunName());
		assertEquals(expected.getStartedCount(), actual.getStartedCount());
		assertEquals(expected.getTotalCount(), actual.getTotalCount());
		assertEquals(expected.getErrorCount(), actual.getErrorCount());
		assertEquals(expected.getFailureCount(), actual.getFailureCount());
		assertEquals(expected.getIgnoredCount(), actual.getIgnoredCount());
		
//		assertEquals(expected.getLaunchedProject(), actual.getLaunchedProject()); //TODO
		
		assertEqualSuite(expected.getTestRoot(), actual.getTestRoot());
	}
	
	private void assertEqualSuite(ITestSuiteElement expected, ITestSuiteElement actual) {
		assertEquals(expected.getProgressState(), actual.getProgressState());
		assertEquals(expected.getTestResult(false), actual.getTestResult(false));
		if (actual instanceof TestSuiteElement) {
			TestSuiteElement act= (TestSuiteElement) actual;
			TestSuiteElement exp= (TestSuiteElement) expected;
			assertEquals(exp.getTestName(), act.getTestName());
			assertEquals(exp.getTrace(), act.getTrace());
			assertEquals(exp.getTestName(), act.getTestName());
			ITestElement[] expChildren= exp.getChildren();
			ITestElement[] actChildren= act.getChildren();
			assertEquals(expChildren.length, actChildren.length);
			for (int i= 0; i < expChildren.length; i++) {
				ITestElement expChild= expChildren[i];
				ITestElement actChild= actChildren[i];
				if (expChild instanceof ITestCaseElement) {
					assertEqualTestCase((ITestCaseElement) expChild, (ITestCaseElement) actChild);
				} else if (expChild instanceof ITestSuiteElement) {
					assertEqualSuite((ITestSuiteElement) expChild, (ITestSuiteElement) actChild);
				} else {
					fail(expChild.getClass().getName());
				}
			}
		}
	}

	private void assertEqualTestCase(ITestCaseElement expected, ITestCaseElement actual) {
		assertEquals(expected.getTestClassName(), actual.getTestClassName());
		assertEquals(expected.getTestMethodName(), actual.getTestMethodName());
		assertEquals(expected.getTestResult(false), actual.getTestResult(false));
		FailureTrace expFailure= expected.getFailureTrace();
		FailureTrace actFailure= actual.getFailureTrace();
		if (expFailure == null) {
			assertNull(actFailure);
		} else if (actFailure == null) {
			assertNull(expFailure);
		} else {
			assertEquals(expFailure.getActual(), actFailure.getActual());
			assertEquals(expFailure.getExpected(), actFailure.getExpected());
			// FailureTrace#getTrace() is VM-dependent; could only compare first line
		}
	}
	
	public static String getContents(InputStream in) throws IOException {
		InputStreamReader reader= new InputStreamReader(in);
		StringBuffer sb= new StringBuffer(8192);
		char[] cbuf= new char[8192];
		try {
			int read= 0;
			while ((read= reader.read(cbuf)) != -1)
				sb.append(cbuf, 0, read);
		} finally {
			reader.close();
		}
		return sb.toString();
	}
	
	private void runCUTest(String test) throws CoreException, IOException, FileNotFoundException, Exception {
		IPackageFragmentRoot root= JUnitWorkspaceTestSetup.getRoot();
		IPackageFragment pack= root.getPackageFragment("pack");
		ICompilationUnit cu= pack.getCompilationUnit(test + ".java");
		IType aTestCase= cu.findPrimaryType();
		
		Path expectedPath= new Path(JUnitWorkspaceTestSetup.PROJECT_PATH + "xml/" + test + ".xml");
		File expectedFile= JavaTestPlugin.getDefault().getFileInPlugin(expectedPath);
		String expected= getContents(new FileInputStream(expectedFile));
		runExportImport(aTestCase, expected);
		
		runImportAntResult(test);
	}
	
	private void runImportAntResult(String test) throws CoreException {
		Path testPath= new Path(JUnitWorkspaceTestSetup.PROJECT_PATH + "ant/result/TEST-pack." + test + ".xml");
		File testFile= JavaTestPlugin.getDefault().getFileInPlugin(testPath);
		JUnitModel.importTestRunSession(testFile); // no contents check for now...
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
		Path testsPath= new Path(JUnitWorkspaceTestSetup.PROJECT_PATH + "ant/result/TESTS-TestSuites.xml");
		File testsFile= JavaTestPlugin.getDefault().getFileInPlugin(testsPath);
		JUnitModel.importTestRunSession(testsFile); // no contents check for now...
	}
	
}

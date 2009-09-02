/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Brock Janiczak (brockj@tpg.com.au)
 *         - https://bugs.eclipse.org/bugs/show_bug.cgi?id=102236: [JUnit] display execution time next to each test
 *     Achim Demelt <a.demelt@exxcellent.de> - [junit] Separate UI from non-UI code - https://bugs.eclipse.org/bugs/show_bug.cgi?id=278844
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

import junit.framework.TestCase;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestCaseElement;
import org.eclipse.jdt.junit.model.ITestElement;
import org.eclipse.jdt.junit.model.ITestRunSession;
import org.eclipse.jdt.junit.model.ITestSuiteElement;
import org.eclipse.jdt.junit.model.ITestElement.FailureTrace;
import org.eclipse.jdt.testplugin.JavaTestPlugin;
import org.eclipse.jdt.testplugin.util.DisplayHelper;

import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.junit.model.JUnitModel;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;

public class AbstractTestRunSessionSerializationTests extends TestCase {

	private static final int TIMEOUT= 10 * 1000;


	private static class SerializationResult {
		TestRunSession fTestRunSession;
		String fSerialized;
	}

	private SerializationResult launchTest(IJavaElement elementToLaunch) throws Exception {
		final SerializationResult result= new SerializationResult();

		TestRunListener testRunListener= new TestRunListener() {
			public void sessionFinished(ITestRunSession session) {
				assertNotNull(session);
				result.fTestRunSession= (TestRunSession) session;
			}
		};

		JUnitCore.addTestRunListener(testRunListener);
		try {
			new AbstractTestRunListenerTest().launchJUnit(elementToLaunch);
			assertTrue(new DisplayHelper(){
				protected boolean condition() {
					return result.fTestRunSession != null;
				}
			}.waitForCondition(Display.getCurrent(), TIMEOUT, 100));
		} finally {
			JUnitCore.removeTestRunListener(testRunListener);
		}

		ByteArrayOutputStream out= new ByteArrayOutputStream();
		JUnitModel.exportTestRunSession(result.fTestRunSession, out);

		result.fSerialized= out.toString("UTF-8");
		return result;
	}

	private void runExportImport(IJavaElement test, String expectedXML) throws Exception {
		SerializationResult serializationResult= launchTest(test);
		assertEqualXML(expectedXML, serializationResult.fSerialized);

		IFile resultFile= JUnitWorkspaceTestSetup.getJavaProject().getProject().getFile("testresult.xml");
		try {
			resultFile.create(new ByteArrayInputStream(serializationResult.fSerialized.getBytes()), true, null);
			TestRunSession imported= JUnitModel.importTestRunSession(resultFile.getLocation().toFile());
			// swap out the test run session because it may not have been done earlier
			// due to lingering TestRunnerViewPart$TestSessionListeners
			serializationResult.fTestRunSession.swapOut();
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
		 * Avoid comparing stack traces (which are VM-dependent)
		 */
		String regex= "(?m)^\\s*at\\s+[\\w\\.\\:\\;\\$\\(\\)\\[ \\t]+$\r?\n?";
		/*
		 * Strips lines like " ... 18 more"
		 */
		String regex2= "(?m)^\\s*\\.{3}\\s+\\d+\\s+more\\s+$\r?\n?";
		/*
		 * Strips running times
		 */
		String regex3= "(?<=time=\\\")\\d+\\.\\d+(?=\\\")";
		String replacement= "";
		expected= expected.replaceAll(regex, replacement).replaceAll(regex2, replacement).replaceAll(regex3, replacement);
		actual= actual.replaceAll(regex, replacement).replaceAll(regex2, replacement).replaceAll(regex3, replacement);
		int ibmJava6BugOffset= actual.indexOf("><");
		if (ibmJava6BugOffset > 0) // https://bugs.eclipse.org/bugs/show_bug.cgi?id=197842
			actual= new StringBuffer(actual).insert(ibmJava6BugOffset + 1, " ").toString();

		/*
		 * Strip all whitespace
		 */
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

	protected void runCUTest(String test) throws CoreException, IOException, FileNotFoundException, Exception {
		IPackageFragmentRoot root= JUnitWorkspaceTestSetup.getRoot();
		IPackageFragment pack= root.getPackageFragment("pack");
		ICompilationUnit cu= pack.getCompilationUnit(test + ".java");
		IType aTestCase= cu.findPrimaryType();

		Path expectedPath= new Path(JUnitWorkspaceTestSetup.getProjectPath() + "xml/" + test + ".xml");
		File expectedFile= JavaTestPlugin.getDefault().getFileInPlugin(expectedPath);
		String expected= getContents(new FileInputStream(expectedFile));
		runExportImport(aTestCase, expected);

		runImportAntResult(test);
	}

	private void runImportAntResult(String test) throws CoreException {
		Path testPath= new Path(JUnitWorkspaceTestSetup.getProjectPath() + "ant/result/TEST-pack." + test + ".xml");
		File testFile= JavaTestPlugin.getDefault().getFileInPlugin(testPath);
		JUnitModel.importTestRunSession(testFile); // no contents check for now...
	}

	protected void runMethodTest(String testType, String method) throws Exception {
		IPackageFragmentRoot root= JUnitWorkspaceTestSetup.getRoot();
		IPackageFragment pack= root.getPackageFragment("pack");
		ICompilationUnit cu= pack.getCompilationUnit(testType + ".java");
		IType testCase= cu.findPrimaryType();
		IMethod testMethod= testCase.getMethod(method, new String[0]);

		Path expectedPath= new Path(JUnitWorkspaceTestSetup.getProjectPath() + "xml/" + testType + "_" + method + ".xml");
		File expectedFile= JavaTestPlugin.getDefault().getFileInPlugin(expectedPath);
		String expected= getContents(new FileInputStream(expectedFile));
		runExportImport(testMethod, expected);

		//ant cannot run single test methods
	}

}

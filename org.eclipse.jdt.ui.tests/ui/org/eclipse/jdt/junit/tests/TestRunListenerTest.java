/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.junit.tests;

import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.swt.widgets.Display;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener2;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.junit.launcher.JUnitBaseLaunchConfiguration;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.util.DisplayHelper;

import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;

import org.eclipse.jdt.junit.ITestRunListener;

public class TestRunListenerTest extends TestCase {

	private IJavaProject fProject;
	private boolean fLaunchHasTerminated= false;

	protected void setUp() throws Exception {
		fProject= JavaProjectHelper.createJavaProject("TestRunListenerTest", "bin");
		// have to set up an 1.3 project to avoid requiring a 5.0 VM
		JavaProjectHelper.addRTJar13(fProject);
		JavaProjectHelper.addVariableEntry(fProject, new Path("JUNIT_HOME/junit.jar"), null, null);
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fProject);
	}
	
	private static class TestJUnitLaunchShortcut extends JUnitLaunchShortcut {
		public static ILaunchConfiguration createConfiguration(IJavaElement element) throws CoreException {
			ILaunchConfigurationWorkingCopy copy= new TestJUnitLaunchShortcut().createLaunchConfiguration(element);
			return copy.doSave();
		}
	}
	
	private void runTest(String source, final String[] expectedSequence) throws Exception {
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fProject, "src");
		IPackageFragment pack= root.createPackageFragment("pack", true, null);
		ICompilationUnit aTestCaseCU= pack.createCompilationUnit("ATestCase.java", source, true, null);
		IType aTestCase= aTestCaseCU.findPrimaryType();
		
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
		
		TestRunListener.startListening();
		
		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		lm.removeLaunches(lm.getLaunches());
		ILaunchesListener2 launchesListener= new ILaunchesListener2() {
			public void launchesTerminated(ILaunch[] launches) {
				for (int i= 0; i < launches.length; i++) {
					if (isJUnitLaunch(launches[i]))
						fLaunchHasTerminated= true;
					logLaunch("terminated", launches[i]);
				}
			}
			public void launchesRemoved(ILaunch[] launches) {
				for (int i= 0; i < launches.length; i++) {
					if (isJUnitLaunch(launches[i]))
						fLaunchHasTerminated= true;
					logLaunch("removed   ", launches[i]);
				}
			}
			public void launchesAdded(ILaunch[] launches) {
				for (int i= 0; i < launches.length; i++)
					logLaunch("added     ", launches[i]);
			}
			public void launchesChanged(ILaunch[] launches) {
				for (int i= 0; i < launches.length; i++)
					logLaunch("changed   ", launches[i]);
			}
			private void logLaunch(String action, ILaunch launch) {
				StringBuffer buf= new StringBuffer();
				buf.append(System.currentTimeMillis()).append(" ");
				buf.append("launch ").append(action).append(": ");
				ILaunchConfiguration launchConfiguration= launch.getLaunchConfiguration();
				if (launchConfiguration != null) {
					buf.append(launchConfiguration.getName()).append(": ");
				}
				buf.append(launch);
				if (isJUnitLaunch(launch)) {
					buf.append(" [JUnit]");
				}
				System.out.println(buf);				
			}
		};
		lm.addLaunchListener(launchesListener);
		
		ILaunchConfiguration configuration= TestJUnitLaunchShortcut.createConfiguration(aTestCase);
		try {
			configuration.launch(ILaunchManager.RUN_MODE, null);
			new DisplayHelper() {
				protected boolean condition() {
					return fLaunchHasTerminated;
				}
			}.waitForCondition(Display.getCurrent(), 30 * 1000, 1000);
		} finally {
			lm.removeLaunchListener(launchesListener);
			lm.removeLaunches(lm.getLaunches());
			configuration.delete();
		}
		if (! fLaunchHasTerminated)
			fail("Launch has not terminated");
		
		new DisplayHelper(){
			protected boolean condition() {
				return TestRunListener.getMessageCount() >= expectedSequence.length;
			}
		}.waitForCondition(Display.getCurrent(), 5*1000, 100);
		
		List messages= TestRunListener.endListening();
		StringBuffer actual= new StringBuffer();
		for (Iterator iter= messages.iterator(); iter.hasNext();) {
			actual.append(iter.next()).append('\n');
		}
		StringBuffer expected= new StringBuffer();
		for (int i= 0; i < expectedSequence.length; i++) {
			expected.append(expectedSequence[i]).append('\n');
		}
		assertEquals(expected.toString(), actual.toString());
	}
	
	private boolean isJUnitLaunch(ILaunch launch) {
		ILaunchConfiguration config= launch.getLaunchConfiguration();
		if (config == null)
			return false;
		
		// test whether the launch defines the JUnit attributes
		String portStr= launch.getAttribute(JUnitBaseLaunchConfiguration.PORT_ATTR);
		String typeStr= launch.getAttribute(JUnitBaseLaunchConfiguration.TESTTYPE_ATTR);
		if (portStr == null || typeStr == null)
			return false;
		
		return true;

	}
	
	public void testOK() throws Exception {
		String source=
				"package pack;\n" +
				"import junit.framework.TestCase;\n" +
				"public class ATestCase extends TestCase {\n" +
				"    public void testSucceed() { }\n" +
				"}";
		String[] expectedSequence= new String[] {
			TestRunListener.testRunStartedMessage(1),
			TestRunListener.testStartedMessage("2", "testSucceed(pack.ATestCase)"),
			TestRunListener.testEndedMessage("2", "testSucceed(pack.ATestCase)"),
			TestRunListener.testRunEndedMessage()
		};
		runTest(source, expectedSequence);
	}

	public void testFail() throws Exception {
		String source=
			"package pack;\n" +
			"import junit.framework.TestCase;\n" +
			"public class ATestCase extends TestCase {\n" +
			"    public void testFail() { fail(); }\n" +
			"}";
		String[] expectedSequence= new String[] {
				TestRunListener.testRunStartedMessage(1),
				TestRunListener.testStartedMessage("2", "testFail(pack.ATestCase)"),
				TestRunListener.testFailedMessage(ITestRunListener.STATUS_FAILURE, "2", "testFail(pack.ATestCase)"),
				TestRunListener.testEndedMessage("2", "testFail(pack.ATestCase)"),
				TestRunListener.testRunEndedMessage()
		};
		runTest(source, expectedSequence);
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
				TestRunListener.testRunStartedMessage(3),
				
				TestRunListener.testStartedMessage("2", "testAdd(pack.ATestCase)"),
				TestRunListener.testFailedMessage(ITestRunListener.STATUS_FAILURE, "2", "testAdd(pack.ATestCase)"),
				TestRunListener.testEndedMessage("2", "testAdd(pack.ATestCase)"),
				
				TestRunListener.testStartedMessage("3", "testDivideByZero(pack.ATestCase)"),
				TestRunListener.testFailedMessage(ITestRunListener.STATUS_ERROR, "3", "testDivideByZero(pack.ATestCase)"),
				TestRunListener.testEndedMessage("3", "testDivideByZero(pack.ATestCase)"),
				
				TestRunListener.testStartedMessage("4", "testEquals(pack.ATestCase)"),
				TestRunListener.testFailedMessage(ITestRunListener.STATUS_FAILURE, "4", "testEquals(pack.ATestCase)"),
				TestRunListener.testEndedMessage("4", "testEquals(pack.ATestCase)"),
				
				TestRunListener.testRunEndedMessage()
		};
		runTest(source, expectedSequence);
	}
	
}

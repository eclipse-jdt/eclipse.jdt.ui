/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.util.DisplayHelper;

import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;

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
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;


public class AbstractTestRunListenerTest {

	public static class TestRunLog {
		private ArrayList<String> fLog;
		private boolean fIsDone;

		public TestRunLog() {
			fLog= new ArrayList<>();
			fIsDone= false;
		}

		public synchronized int getMessageCount() {
			return fLog.size();
		}

		public synchronized String[] getLog() {
			return fLog.toArray(new String[fLog.size()]);
		}

		public synchronized void add(String entry) {
			fLog.add(entry);
		}

		public void setDone() {
			fIsDone= true;
		}

		public boolean isDone() {
			return fIsDone;
		}
	}


	IJavaProject fProject;
	private boolean fLaunchHasTerminated= false;

	@Before
	public void setUp() throws Exception {
		fProject= JavaProjectHelper.createJavaProject("TestRunListenerTest", "bin");
		// have to set up an 1.3 project to avoid requiring a 5.0 VM
		JavaProjectHelper.addRTJar13(fProject);
		JavaProjectHelper.addToClasspath(fProject, JavaCore.newContainerEntry(JUnitCore.JUNIT4_CONTAINER_PATH));
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.delete(fProject);
	}

	private static class TestJUnitLaunchShortcut extends JUnitLaunchShortcut {
		public static ILaunchConfigurationWorkingCopy createConfiguration(IJavaElement element, String testName) throws CoreException {
			ILaunchConfigurationWorkingCopy copy= new TestJUnitLaunchShortcut().createLaunchConfiguration(element, testName);
			return copy;
		}
	}

	protected IType createType(String source, String packageName, String typeName) throws CoreException, JavaModelException {
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fProject, "src");
		IPackageFragment pack= root.createPackageFragment(packageName, true, null);
		ICompilationUnit aTestCaseCU= pack.createCompilationUnit(typeName, source, true, null);
		IType aTestCase= aTestCaseCU.findPrimaryType();
		return aTestCase;
	}

	protected void launchJUnit(IJavaElement aTest, String testKindID) throws CoreException {
		launchJUnit(aTest, testKindID, (String)null);
	}

	protected void launchJUnit(IJavaElement aTest, String testKindID, String testName) throws CoreException {
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
		IMarker[] markers= aTest.getJavaProject().getProject().findMarkers(null, true, IResource.DEPTH_INFINITE);
		for (IMarker marker : markers) {
			if(marker.getAttribute(IMarker.SEVERITY, 0) >= IMarker.SEVERITY_ERROR) {
				fail("unexpected errors, e.g. :" + marker.toString());
			}
		}

		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		lm.removeLaunches(lm.getLaunches());
		ILaunchesListener2 launchesListener= new ILaunchesListener2() {
			@Override
			public void launchesTerminated(ILaunch[] launches) {
				for (ILaunch launch : launches) {
					if (isJUnitLaunch(launch)) {
						fLaunchHasTerminated= true;
					}
					logLaunch("terminated", launch);
				}
			}
			@Override
			public void launchesRemoved(ILaunch[] launches) {
				for (ILaunch launch : launches) {
					if (isJUnitLaunch(launch)) {
						fLaunchHasTerminated= true;
					}
					logLaunch("removed   ", launch);
				}
			}
			@Override
			public void launchesAdded(ILaunch[] launches) {
				for (ILaunch launch : launches) {
					logLaunch("added     ", launch);
				}
			}
			@Override
			public void launchesChanged(ILaunch[] launches) {
				for (ILaunch launch : launches) {
					logLaunch("changed   ", launch);
				}
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

		ILaunchConfigurationWorkingCopy configuration= TestJUnitLaunchShortcut.createConfiguration(aTest, testName);
		if (testKindID != null) {
			configuration.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND, testKindID);
		}
		try {
			configuration.launch(ILaunchManager.RUN_MODE, null);
			new DisplayHelper() {
				@Override
				protected boolean condition() {
					return fLaunchHasTerminated;
				}
			}.waitForCondition(Display.getCurrent(), 30 * 1000, 1000);
		} finally {
			lm.removeLaunchListener(launchesListener);
			lm.removeLaunches(lm.getLaunches());
			configuration.delete();
		}
		assertTrue("Launch has not terminated", fLaunchHasTerminated);
	}

	protected String[] launchJUnit(IJavaElement aTest, final TestRunLog log) throws CoreException {
		return launchJUnit(aTest, null, log);
	}

	protected String[] launchJUnit(IJavaElement aTest, String testKindID, final TestRunLog log) throws CoreException {
		return launchJUnit(aTest, testKindID, null, log);
	}

	protected String[] launchJUnit(IJavaElement aTest, String testKindID, String testName, final TestRunLog log) throws CoreException {
		launchJUnit(aTest, testKindID, testName);

		boolean success= new DisplayHelper(){
			@Override
			protected boolean condition() {
				return log.isDone();
			}
		}.waitForCondition(Display.getCurrent(), 15*1000, 100);
		if (! success)
			log.add("AbstractTestRunListenerTest#launchJUnit(IJavaElement, TestRunLog) timed out");
		return log.getLog();
	}

	private boolean isJUnitLaunch(ILaunch launch) {
		ILaunchConfiguration config= launch.getLaunchConfiguration();
		if (config == null)
			return false;

		// test whether the launch defines the JUnit attributes
		String portStr= launch.getAttribute(JUnitLaunchConfigurationConstants.ATTR_PORT);
		if (portStr == null)
			return false;

		return true;
	}

	public static void assertEqualLog(final String[] expectedSequence, String[] logMessages) {
		StringBuilder actual= new StringBuilder();
		for (String logMessage : logMessages) {
			actual.append(logMessage).append('\n');
		}
		StringBuilder expected= new StringBuilder();
		for (String sequence : expectedSequence) {
			expected.append(sequence).append('\n');
		}
		assertEquals(expected.toString(), actual.toString());
	}


}

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
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.launcher.AdvancedJUnitLaunchConfigurationDelegate;
import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.util.DisplayHelper;

import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

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
import org.eclipse.debug.core.Launch;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;


public class AbstractTestRunListenerTest {

	public static class TestRunLog {
		private final ArrayList<String> fLog;
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

	protected IType createType(String source, String packageName, String typeName) throws CoreException {
		return createType(fProject, source, packageName, typeName);
	}

	protected static IType createType(IJavaProject project, String source, String packageName, String typeName) throws CoreException {
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(project, "src");
		IPackageFragment pack= root.createPackageFragment(packageName, true, null);
		ICompilationUnit aTestCaseCU= pack.createCompilationUnit(typeName, source, true, null);
		IType aTestCase= aTestCaseCU.findPrimaryType();
		return aTestCase;
	}

	protected void launchJUnit(IJavaElement aTest, String testKindID) throws CoreException {
		launchJUnit(aTest, testKindID, (String)null);
	}

	protected void launchJUnit(IJavaElement aTest, String testKindID, String testName) throws CoreException {
		buildTestCase(aTest);

		LaunchesListener listener = new LaunchesListener();
		ILaunchConfigurationWorkingCopy configuration= createLaunchConfiguration(aTest, testKindID, testName, listener);
		try {
			configuration.launch(ILaunchManager.RUN_MODE, null);
			waitForCondition(listener.fLaunchHasTerminated::get, 30 * 1000, 1000);
		} finally {
			cleanUp(configuration, listener);
		}
		assertTrue("Launch has not terminated", listener.fLaunchHasTerminated.get());
	}

	protected static ILaunchConfigurationWorkingCopy createLaunchConfiguration(IJavaElement aTest, String testKindID, String testName, LaunchesListener launchesListener) throws CoreException {
		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		lm.removeLaunches(lm.getLaunches());
		lm.addLaunchListener(launchesListener);
		ILaunchConfigurationWorkingCopy configuration= TestJUnitLaunchShortcut.createConfiguration(aTest, testName);
		if (testKindID != null) {
			configuration.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND, testKindID);
		}
		return configuration;
	}

	protected static void cleanUp(ILaunchConfigurationWorkingCopy configuration, LaunchesListener launchesListener) throws CoreException {
		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		lm.removeLaunchListener(launchesListener);
		lm.removeLaunches(lm.getLaunches());
		configuration.delete();
	}

	protected static void buildTestCase(IJavaElement aTest) throws CoreException {
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
		IMarker[] markers= aTest.getJavaProject().getProject().findMarkers(null, true, IResource.DEPTH_INFINITE);
		for (IMarker marker : markers) {
			if(marker.getAttribute(IMarker.SEVERITY, 0) >= IMarker.SEVERITY_ERROR) {
				fail("unexpected errors, e.g. :" + marker.toString());
			}
		}
	}

	protected String[] launchJUnit(IJavaElement aTest, final TestRunLog log) throws CoreException {
		return launchJUnit(aTest, null, log);
	}

	protected String[] launchJUnit(IJavaElement aTest, String testKindID, final TestRunLog log) throws CoreException {
		return launchJUnit(aTest, testKindID, null, log);
	}

	protected String[] launchJUnit(IJavaElement aTest, String testKindID, String testName, final TestRunLog log) throws CoreException {
		launchJUnit(aTest, testKindID, testName);

		boolean success= waitForCondition(log::isDone, 15*1000, 100);
		if (! success)
			log.add("AbstractTestRunListenerTest#launchJUnit(IJavaElement, TestRunLog) timed out");
		return log.getLog();
	}

	/**
	 * Launches a JUnit test session with an arbitrary set of {@link IMember} test elements,
	 * possibly spanning multiple types. Used to verify the multi-method extension to the
	 * {@code -testNameFile} dispatch (see {@code JUnitLaunchConfigurationDelegate#createTestNamesFile(IJavaElement[])}).
	 *
	 * <p>The default {@code evaluateTests} in the production delegate never returns multiple
	 * {@code IMethod} entries, so this helper installs an inline subclass of
	 * {@link AdvancedJUnitLaunchConfigurationDelegate} that returns the supplied members,
	 * then bypasses the launch registry and directly invokes {@code delegate.launch(...)}.
	 * The launch is registered with the launch manager beforehand so that {@code JUnitModel}
	 * picks up the test events through the standard {@code ATTR_PORT} change notification.</p>
	 *
	 * @param testMembers the test members (types or methods) to run in a single VM
	 * @param anchor      anchor type used to materialize a launch configuration; typically
	 *                    the declaring type of the first member
	 * @param testKindId  one of the {@code TestKindRegistry.JUNIT*_TEST_KIND_ID} constants
	 * @param log         log that the caller's {@link org.eclipse.jdt.junit.TestRunListener}
	 *                    writes into; used to wait for {@code sessionFinished}
	 */
	protected String[] launchJUnitMultiMethod(final IMember[] testMembers, IType anchor, String testKindId, final TestRunLog log) throws CoreException {
		buildTestCase(anchor);

		ILaunchManager lm= DebugPlugin.getDefault().getLaunchManager();
		lm.removeLaunches(lm.getLaunches());
		LaunchesListener listener= new LaunchesListener();
		lm.addLaunchListener(listener);

		ILaunchConfigurationWorkingCopy configuration= TestJUnitLaunchShortcut.createConfiguration(anchor, null);
		configuration.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND, testKindId);
		configuration.removeAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_NAME);

		AdvancedJUnitLaunchConfigurationDelegate delegate= new AdvancedJUnitLaunchConfigurationDelegate() {
			@Override
			protected IMember[] evaluateTests(ILaunchConfiguration cfg, IProgressMonitor monitor) {
				return testMembers;
			}
		};

		Launch launch= new Launch(configuration, ILaunchManager.RUN_MODE, null);
		try {
			lm.addLaunch(launch);
			delegate.launch(configuration, ILaunchManager.RUN_MODE, launch, new NullProgressMonitor());
			waitForCondition(listener.fLaunchHasTerminated::get, 60 * 1000, 1000);
			boolean success= waitForCondition(log::isDone, 15 * 1000, 100);
			if (!success) {
				log.add("AbstractTestRunListenerTest#launchJUnitMultiMethod timed out");
			}
		} finally {
			try {
				if (!launch.isTerminated()) {
					launch.terminate();
				}
				// Wait briefly so the listener can observe the termination event
				// before we unregister it; otherwise the launchesTerminated callback
				// may fire on a removed listener and the next run can mistake any
				// stale launch state for the current one.
				waitForCondition(listener.fLaunchHasTerminated::get, 5 * 1000, 200);
			} catch (Exception ignore) {
				// best-effort cleanup
			}
			lm.removeLaunchListener(listener);
			lm.removeLaunches(lm.getLaunches());
			try {
				configuration.delete();
			} catch (CoreException ignore) {
				// best-effort cleanup
			}
		}
		return log.getLog();
	}

	protected static boolean waitForCondition(Supplier<Boolean> condition, long timeout, long interval) {
		DisplayHelper displayHelper= new DisplayHelper() {
			@Override
			protected boolean condition() {
				return condition.get();
			}
		};
		return displayHelper.waitForCondition(Display.getCurrent(), timeout, interval);
	}

	protected static boolean isJUnitLaunch(ILaunch launch) {
		ILaunchConfiguration config= launch.getLaunchConfiguration();
		if (config == null)
			return false;

		// test whether the launch defines the JUnit attributes
		String portStr= launch.getAttribute(JUnitLaunchConfigurationConstants.ATTR_PORT);
		if (portStr == null)
			return false;

		return true;
	}

	private static void logLaunch(String action, ILaunch launch) {
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

	/**
	 * Asserts that the {@link TestRunListeners.SequenceTest} log records exactly one
	 * {@code sessionStarted}/{@code sessionFinished} pair plus a {@code testCaseStarted}
	 * and {@code testCaseFinished} event for each of the supplied {@code class#method}
	 * identifiers (in any order). Used by the multi-method launch tests because the
	 * dispatch order across classes is implementation-defined.
	 */
	protected static void assertMultiMethodRun(String[] log, String... expectedClassHashMethod) {
		Pattern p= Pattern.compile("testCaseMethod: (.+?)\\s*[\\r\\n]+\\s*class: (\\S+)");
		int sessionStartedCount= 0;
		int sessionFinishedCount= 0;
		Set<String> started= new TreeSet<>();
		Set<String> finished= new TreeSet<>();
		for (String entry : log) {
			if (entry.startsWith("sessionStarted-")) {
				sessionStartedCount++;
			} else if (entry.startsWith("sessionFinished-")) {
				sessionFinishedCount++;
			} else if (entry.startsWith("testCaseStarted-")) {
				Matcher m= p.matcher(entry);
				if (m.find()) {
					started.add(m.group(2) + "#" + m.group(1));
				}
			} else if (entry.startsWith("testCaseFinished-")) {
				Matcher m= p.matcher(entry);
				if (m.find()) {
					finished.add(m.group(2) + "#" + m.group(1));
				}
			}
		}
		Set<String> expected= new TreeSet<>(Arrays.asList(expectedClassHashMethod));
		assertEquals("Expected exactly one sessionStarted event in log: " + Arrays.toString(log), 1, sessionStartedCount);
		assertEquals("Expected exactly one sessionFinished event in log: " + Arrays.toString(log), 1, sessionFinishedCount);
		assertEquals("Started test cases differ", expected, started);
		assertEquals("Finished test cases differ", expected, finished);
	}

	protected static class LaunchesListener implements ILaunchesListener2 {

		protected final AtomicBoolean fLaunchChanged= new AtomicBoolean(false);
		protected final AtomicBoolean fLaunchHasTerminated= new AtomicBoolean(false);

		@Override
		public void launchesTerminated(ILaunch[] launches) {
			for (ILaunch launch : launches) {
				if (isJUnitLaunch(launch)) {
					fLaunchHasTerminated.set(true);
				}
				logLaunch("terminated", launch);
			}
		}
		@Override
		public void launchesRemoved(ILaunch[] launches) {
			for (ILaunch launch : launches) {
				if (isJUnitLaunch(launch)) {
					fLaunchHasTerminated.set(true);
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
				if (isJUnitLaunch(launch)) {
					fLaunchChanged.set(true);
				}
				logLaunch("changed   ", launch);
			}
		}
	}
}

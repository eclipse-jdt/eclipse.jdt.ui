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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestElement.FailureTrace;
import org.eclipse.jdt.junit.model.ITestElement.ProgressState;
import org.eclipse.jdt.junit.model.ITestElement.Result;
import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.buildpath.BuildPathSupport;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.internal.junit.model.ITestRunSessionListener;
import org.eclipse.jdt.internal.junit.model.ITestSessionListener;
import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.junit.model.TestElement.Status;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;

public class TestRunListenerTest5 extends AbstractTestRunListenerTest {

	private String[] runSequenceTest(IType typeToLaunch) throws Exception {
		TestRunLog log= new TestRunLog();
		final TestRunListener testRunListener= new TestRunListeners.SequenceTest(log);
		JUnitCore.addTestRunListener(testRunListener);
		try {
			return launchJUnit(typeToLaunch, TestKindRegistry.JUNIT5_TEST_KIND_ID, log);
		} finally {
			JUnitCore.removeTestRunListener(testRunListener);
		}
	}

	private String[] runTreeTest(IType typeToLaunch, int step) throws Exception {
		TestRunLog log= new TestRunLog();
		final TestRunListener testRunListener= new TestRunListeners.TreeTest(log, step);
		JUnitCore.addTestRunListener(testRunListener);
		try {
			return launchJUnit(typeToLaunch, TestKindRegistry.JUNIT5_TEST_KIND_ID, log);
		} finally {
			JUnitCore.removeTestRunListener(testRunListener);
		}
	}

	@Override
	@Before
	public void setUp() throws Exception {
		fProject= JavaProjectHelper.createJavaProject("TestRunListenerTest", "bin");
		JavaProjectHelper.addToClasspath(fProject, JavaCore.newContainerEntry(JUnitCore.JUNIT5_CONTAINER_PATH));
		JavaProjectHelper.addRTJar18(fProject);
	}

	@Test
	public void testOK() throws Exception {
		String source=
				"""
			package pack;
			import org.junit.jupiter.api.Test;
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
	public void testFail() throws Exception {
		String source=
			"""
			package pack;
			import org.junit.jupiter.api.Test;
			import static org.junit.jupiter.api.Assertions.*;
			public class ATestCase {
			    @Test public void testFail() { fail("reason"); }
			}""";
		IType aTestCase= createType(source, "pack", "ATestCase.java");

		String[] expectedSequence= new String[] {
			"sessionStarted-" + TestRunListeners.sessionAsString("ATestCase", ProgressState.RUNNING, Result.UNDEFINED, 0),
			"testCaseStarted-" + TestRunListeners.testCaseAsString("testFail", "pack.ATestCase", ProgressState.RUNNING, Result.UNDEFINED, null, 0),
			"testCaseFinished-" + TestRunListeners.testCaseAsString("testFail", "pack.ATestCase", ProgressState.COMPLETED, Result.FAILURE, new FailureTrace("org.opentest4j.AssertionFailedError", null, null), 0),
			"sessionFinished-" + TestRunListeners.sessionAsString("ATestCase", ProgressState.COMPLETED, Result.FAILURE, 0)
		};
		String[] actual= runSequenceTest(aTestCase);
		assertEqualLog(expectedSequence, actual);
	}

	@Test
	public void testTreeOnSessionStarted() throws Exception {
		String source=
				"""
			package pack;
			import org.junit.jupiter.api.Test;
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
			import org.junit.jupiter.api.Test;
			import static org.junit.jupiter.api.Assertions.*;
			public class ATestCase {
			    @Test public void testFail() { fail("reason"); }
			}""";
		IType aTestCase= createType(source, "pack", "ATestCase.java");

		String[] expectedTree= new String[] {
			TestRunListeners.sessionAsString("ATestCase", ProgressState.COMPLETED, Result.FAILURE, 0),
			TestRunListeners.suiteAsString("pack.ATestCase", ProgressState.COMPLETED, Result.FAILURE, null, 1),
			TestRunListeners.testCaseAsString("testFail", "pack.ATestCase", ProgressState.COMPLETED, Result.FAILURE, new FailureTrace("org.opentest4j.AssertionFailedError", null, null), 2),
		};
		String[] actual= runTreeTest(aTestCase, 4);
		assertEqualLog(expectedTree, actual);
	}

	@Test
	public void testThatLauncherLibGetsAdded() throws Exception {
		JavaProjectHelper.removeFromClasspath(fProject, JUnitCore.JUNIT5_CONTAINER_PATH);
		JavaProjectHelper.addToClasspath(fProject, BuildPathSupport.getJUnitJupiterApiLibraryEntry());
		JavaProjectHelper.addToClasspath(fProject, BuildPathSupport.getJUnitPlatformCommonsLibraryEntry());
		JavaProjectHelper.addToClasspath(fProject, BuildPathSupport.getJUnitOpentest4jLibraryEntry());
		JavaProjectHelper.addToClasspath(fProject, BuildPathSupport.getJUnitApiGuardianLibraryEntry());
		JavaProjectHelper.addToClasspath(fProject, BuildPathSupport.getJUnitPlatformEngineLibraryEntry());
		JavaProjectHelper.addToClasspath(fProject, BuildPathSupport.getJUnitJupiterEngineLibraryEntry());
		String source=
				"""
			package pack;
			import org.junit.jupiter.api.Test;
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

	// Test for: https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/2667
	@Test
	public void testCorrectLaunchTypeWithoutVersionInJUnitLibraryFileNames() throws Exception {
		JavaProjectHelper.removeFromClasspath(fProject, JUnitCore.JUNIT5_CONTAINER_PATH);
		IClasspathEntry[] entries = {
				BuildPathSupport.getJUnitJupiterApiLibraryEntry(),
				BuildPathSupport.getJUnitPlatformCommonsLibraryEntry(),
				BuildPathSupport.getJUnitPlatformEngineLibraryEntry(),
				BuildPathSupport.getJUnitJupiterEngineLibraryEntry(),
				BuildPathSupport.getJUnitOpentest4jLibraryEntry(),
				BuildPathSupport.getJUnitApiGuardianLibraryEntry(),
		};
		IProject project= fProject.getProject();
		for (int i = 0; i < entries.length; ++i) {
			IClasspathEntry entry = entries[i];
			String name = "lib" + i + ".jar";
			IPath entryPath= entry.getPath();
			IFile copy= project.getFile(name);
			Files.copy(entryPath.toPath(), copy.getLocation().toPath());
			copy.refreshLocal(IResource.DEPTH_INFINITE, null);
			JavaProjectHelper.addToClasspath(fProject, JavaCore.newLibraryEntry(project.getLocation().append(name), null, null));
		}
		List<String> lines = Files.readAllLines(fProject.getProject().getLocation().append(".classpath").toPath());
		lines.stream().forEach(System.out::println);
		String source=
				"""
			package pack;
			import org.junit.jupiter.api.Test;
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
		TestRunLog log= new TestRunLog();
		final TestRunListener testRunListener= new TestRunListeners.SequenceTest(log);
		JUnitCore.addTestRunListener(testRunListener);
		String[] actual = {};
		try {
			actual = launchJUnit(aTestCase, null, log);
		} finally {
			JUnitCore.removeTestRunListener(testRunListener);
		}
		assertEqualLog(expectedSequence, actual);
	}

	@Test
	public void testTerminateLaunch() throws Exception {
		doTestTerminateLaunch(fProject, TestKindRegistry.JUNIT5_TEST_KIND_ID);
	}

	protected static void doTestTerminateLaunch(IJavaProject project, String testKindId) throws CoreException {
		String source=
				"""
				package pack;
				import org.junit.jupiter.api.Test;
				public class ATestCaseTerminate {
				    @Test public void testSleep() throws Exception { Thread.sleep(30_000); }
				}""";
		IType aTestCase= createType(project, source, "pack", "ATestCaseTerminate.java");
		buildTestCase(aTestCase);

		LaunchesListener launchesListener= new LaunchesListener();
		ILaunchConfigurationWorkingCopy configuration= createLaunchConfiguration(aTestCase, testKindId, null, launchesListener);

		IJobManager jm= Job.getJobManager();
		ScheduledJobsListener jobListener= new ScheduledJobsListener(JUnitMessages.TestRunnerViewPart_jobName);
		jm.addJobChangeListener(jobListener);

		TestSessionListener sessionListener = new TestSessionListener();
		TestRunSessionListener runSessionListener = new TestRunSessionListener();
		JUnitCorePlugin.getModel().addTestRunSessionListener(runSessionListener);
		try {
			configuration.launch(ILaunchManager.RUN_MODE, null);
			waitForCondition(launchesListener.fLaunchChanged::get, 30 * 1000, 1000);

			long scheduledJobsCount = jobListener.scheduledCount.get();
			boolean jobCountIncrease= waitForCondition(() -> jobListener.scheduledCount.get() > scheduledJobsCount, 5 * 1000, 100);
			assertTrue("Expected JUnit update jobs to be scheduled", jobCountIncrease);

			// register the session listener here, so that its hopefully the last listener to be notified of stopping
			runSessionListener.fTestRunSession.addTestSessionListener(sessionListener);
			terminateLaunches();
			boolean terminatedLaunch= waitForCondition(launchesListener.fLaunchHasTerminated::get, 30 * 1000, 1000);
			assertTrue("Unexpected timeout on JUnit launch terminate", terminatedLaunch);
			boolean stoppedSession= waitForCondition(sessionListener.fSessionStopped::get, 30 * 1000, 1000);
			assertTrue("Unexpected timeout on JUnit session stop", stoppedSession);
			long scheduledJobsCountAfterTermination = jobListener.scheduledCount.get();

			jobCountIncrease= waitForCondition(() -> jobListener.scheduledCount.get() > scheduledJobsCountAfterTermination, 1 * 1000, 100);
			assertFalse("Expected no new JUnit update jobs to be scheduled", jobCountIncrease);
		} finally {
			jm.removeJobChangeListener(jobListener);
			JUnitCorePlugin.getModel().removeTestRunSessionListener(runSessionListener);
			terminateLaunches();
			cleanUp(configuration, launchesListener);
		}
	}

	private static void terminateLaunches() throws DebugException {
		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		ILaunch[] launches= lm.getLaunches();
		for (ILaunch launch : launches) {
			if (isJUnitLaunch(launch)) {
				launch.terminate();
			}
		}
	}

	private static class ScheduledJobsListener extends JobChangeAdapter {

		private final String jobName;
		final AtomicLong scheduledCount;

		ScheduledJobsListener(String jobName) {
			this.jobName = jobName;
			scheduledCount = new AtomicLong(0L);
		}

		@Override
		public void scheduled(IJobChangeEvent event) {
			String name= event.getJob().getName();
			if (jobName.equals(name)) {
				scheduledCount.incrementAndGet();
			}
		}
	}

	private static class TestRunSessionListener implements ITestRunSessionListener  {

		private TestRunSession fTestRunSession;

		public TestRunSessionListener() {
		}

		@Override
		public void sessionAdded(TestRunSession testRunSession) {
			fTestRunSession= testRunSession;
		}

		@Override
		public void sessionRemoved(TestRunSession testRunSession) {
		}
	}

	static class TestSessionListener implements ITestSessionListener {

		final AtomicBoolean fSessionStopped = new AtomicBoolean(false);

		@Override
		public void sessionStarted() {
		}

		@Override
		public void sessionEnded(long elapsedTime) {
		}

		@Override
		public void sessionStopped(long elapsedTime) {
			fSessionStopped.set(true);
		}

		@Override
		public void sessionTerminated() {
		}

		@Override
		public void testAdded(TestElement testElement) {
		}

		@Override
		public void runningBegins() {
		}

		@Override
		public void testStarted(TestCaseElement testCaseElement) {
		}

		@Override
		public void testEnded(TestCaseElement testCaseElement) {
		}

		@Override
		public void testFailed(TestElement testElement, Status status, String trace, String expected, String actual) {
		}

		@Override
		public void testReran(TestCaseElement testCaseElement, Status status, String trace, String expectedResult, String actualResult) {
		}

		@Override
		public boolean acceptsSwapToDisk() {
			return false;
		}
	}
}

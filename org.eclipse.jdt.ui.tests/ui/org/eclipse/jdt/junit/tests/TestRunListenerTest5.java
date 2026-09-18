/*******************************************************************************
 * Copyright (c) 2006, 2026 IBM Corporation and others.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestElement.FailureTrace;
import org.eclipse.jdt.junit.model.ITestElement.ProgressState;
import org.eclipse.jdt.junit.model.ITestElement.Result;
import org.eclipse.jdt.junit.model.ITestRunSession;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.util.DisplayHelper;

import org.eclipse.swt.widgets.Display;

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
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.buildpath.BuildPathSupport;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.internal.junit.model.ITestSessionListener;
import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.junit.model.TestElement.Status;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.ui.TestRunnerViewPart;

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

	/**
	 * Verifies that selecting individual methods across multiple JUnit Jupiter test classes
	 * runs all of them in a single VM via the multi-method {@code -testNameFile} extension.
	 */
	@Test
	public void testMultiMethodAcrossClasses() throws Exception {
		String fooSource=
				"""
			package pack;
			import org.junit.jupiter.api.Test;
			public class FooTest {
			    @Test public void a() { }
			    @Test public void unused() { }
			}""";
		String barSource=
				"""
			package pack;
			import org.junit.jupiter.api.Test;
			public class BarTest {
			    @Test public void b() { }
			    @Test public void c() { }
			}""";
		IType fooType= createType(fooSource, "pack", "FooTest.java");
		IType barType= createType(barSource, "pack", "BarTest.java");

		IMember[] members= {
				fooType.getMethod("a", new String[0]),
				barType.getMethod("b", new String[0]),
				barType.getMethod("c", new String[0]),
		};

		TestRunLog log= new TestRunLog();
		final TestRunListener testRunListener= new TestRunListeners.SequenceTest(log);
		JUnitCore.addTestRunListener(testRunListener);
		String[] actual;
		try {
			actual= launchJUnitMultiMethod(members, fooType, TestKindRegistry.JUNIT5_TEST_KIND_ID, log);
		} finally {
			JUnitCore.removeTestRunListener(testRunListener);
		}

		assertMultiMethodRun(actual, "pack.FooTest#a", "pack.BarTest#b", "pack.BarTest#c");
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
		AtomicReference<TestRunSession> startedSession= new AtomicReference<>();
		TestRunListener runSessionListener= new TestRunListener() {
			@Override
			public void sessionStarted(ITestRunSession session) {
				if (session instanceof TestRunSession testRunSession && testRunSession.getLaunch() != null
						&& configuration.equals(testRunSession.getLaunch().getLaunchConfiguration())) {
					startedSession.set(testRunSession);
				}
			}
		};
		JUnitCore.addTestRunListener(runSessionListener);
		try {
			// This test needs a view, independently of preceding tests and show-on-error preferences.
			assertNotNull(JUnitPlugin.showTestRunnerViewPartInActivePage());
			ILaunch launch= configuration.launch(ILaunchManager.RUN_MODE, null);
			// A launch change only announces the port; the remote VM may not have started JUnit yet.
			// Keep the job-scheduling timeout separate from this session-start prerequisite.
			assertTrue("Unexpected timeout on JUnit session start",
					waitForCondition(() -> startedSession.get() != null, 30 * 1000, 100));
			assertSame("Expected the session belonging to this launch", launch, startedSession.get().getLaunch());

			long scheduledJobsCount = jobListener.scheduledCount.get();
			boolean jobCountIncrease= waitForCondition(() -> jobListener.scheduledCount.get() > scheduledJobsCount, 5 * 1000, 100);
			assertTrue("Expected JUnit update jobs to be scheduled", jobCountIncrease);

			// register the session listener here, so that its hopefully the last listener to be notified of stopping
			startedSession.get().addTestSessionListener(sessionListener);
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
			JUnitCore.removeTestRunListener(runSessionListener);
			TestRunSession session= startedSession.get();
			if (session != null)
				session.removeTestSessionListener(sessionListener);
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

	@Test
	public void testRetiredSessionTerminationDoesNotStopActiveSession() throws Exception {
		assertSessionNotification(ITestSessionListener::sessionTerminated, true);
	}

	@Test
	public void testRetiredSessionStopDoesNotStopActiveSession() throws Exception {
		assertSessionNotification(listener -> listener.sessionStopped(0), true);
	}

	@Test
	public void testRetiredSessionEndDoesNotStopActiveSession() throws Exception {
		assertSessionNotification(listener -> listener.sessionEnded(0), true);
	}

	@Test
	public void testActiveSessionTerminationStopsUpdateJobs() throws Exception {
		assertSessionNotification(ITestSessionListener::sessionTerminated, false);
	}

	@Test
	public void testActiveSessionStopStopsUpdateJobs() throws Exception {
		assertSessionNotification(listener -> listener.sessionStopped(0), false);
	}

	@Test
	public void testActiveSessionEndStopsUpdateJobs() throws Exception {
		assertSessionNotification(listener -> listener.sessionEnded(0), false);
	}

	private void assertSessionNotification(Consumer<ITestSessionListener> notification, boolean retired) throws Exception {
		TestRunnerViewPart view= JUnitPlugin.showTestRunnerViewPartInActivePage();
		assertNotNull(view);
		DisplayHelper.driveEventQueue(Display.getCurrent());
		Field activeSessionField= accessibleField(TestRunnerViewPart.class, "fTestRunSession");
		Field listenerField= accessibleField(TestRunnerViewPart.class, "fTestSessionListener");
		Field jobField= accessibleField(TestRunnerViewPart.class, "fUpdateJob");
		Field runningField= accessibleField(TestRunSession.class, "fIsRunning");
		Field dirtyListenerField= accessibleField(TestRunnerViewPart.class, "fDirtyListener");
		Object previousDirtyListener= dirtyListenerField.get(view);
		Method activate= TestRunnerViewPart.class.getDeclaredMethod("setActiveTestRunSession", TestRunSession.class);
		activate.setAccessible(true);
		Object previous= activeSessionField.get(view);
		TestRunSession oldSession= new TestRunSession("retired-session", fProject);
		TestRunSession currentSession= new TestRunSession("active-session", fProject);
		runningField.setBoolean(oldSession, true);
		runningField.setBoolean(currentSession, true);
		try {
			activate.invoke(view, oldSession);
			ITestSessionListener oldListener= (ITestSessionListener) listenerField.get(view);
			assertNotNull(oldListener);
			activate.invoke(view, currentSession);
			ITestSessionListener currentListener= (ITestSessionListener) listenerField.get(view);
			Object currentJob= jobField.get(view);
			assertNotNull(currentListener);
			assertNotNull(currentJob);
			assertNotSame(oldListener, currentListener);

			// A notifier may retain an old ListenerList snapshot while the UI
			// switches sessions. Deliver that event on a notification thread.
			ITestSessionListener recipient= retired ? oldListener : currentListener;
			deliverSessionNotification(notification, recipient);
			assertSame(currentSession, activeSessionField.get(view));
			if (retired) {
				assertSame("A retired session must not detach the active session listener", currentListener, listenerField.get(view));
				assertSame("A retired session must not stop the active session's update job", currentJob, jobField.get(view));
				assertTrue("The active update job must remain schedulable", ((Job) currentJob).shouldSchedule());
			} else {
				assertNull("The active session must still detach its own listener", listenerField.get(view));
				assertNull("The active session must still stop its own update job", jobField.get(view));
				assertFalse("The stopped update job must not reschedule", ((Job) currentJob).shouldSchedule());
			}
		} finally {
			Object dirtyListener= dirtyListenerField.get(view);
			if (dirtyListener != previousDirtyListener) {
				if (dirtyListener != null)
					JavaCore.removeElementChangedListener((IElementChangedListener) dirtyListener);
				dirtyListenerField.set(view, previousDirtyListener);
				if (previousDirtyListener != null)
					JavaCore.addElementChangedListener((IElementChangedListener) previousDirtyListener);
			}
			runningField.setBoolean(oldSession, false);
			runningField.setBoolean(currentSession, false);
			activate.invoke(view, previous);
		}
	}

	private static void deliverSessionNotification(Consumer<ITestSessionListener> notification, ITestSessionListener recipient) throws Exception {
		FutureTask<Void> delivered= new FutureTask<>(() -> {
			notification.accept(recipient);
			return null;
		});
		Thread notifier= new Thread(delivered, "JUnit view session notification");
		notifier.setDaemon(true);
		try {
			notifier.start();
			assertTrue("The session notification must complete", waitForCondition(delivered::isDone, 10000, 10));
			delivered.get();
		} finally {
			// Keep dispatching pending syncExec work even after an assertion fails.
			// The worker must finish before the previous view session is restored.
			assertTrue("The notification thread must finish before restoring the view",
					waitForCondition(() -> !notifier.isAlive(), 10000, 10));
		}
	}

	private static Field accessibleField(Class<?> declaringClass, String name) throws ReflectiveOperationException {
		Field field= declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}
}

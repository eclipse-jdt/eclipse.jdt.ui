/*******************************************************************************
 * Copyright (c) 2026 Carsten Hammer and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer - initial tests
 *******************************************************************************/
package org.eclipse.jdt.junit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestCaseElement;
import org.eclipse.jdt.junit.model.ITestElement.ProgressState;
import org.eclipse.jdt.junit.model.ITestElement.Result;
import org.eclipse.jdt.junit.model.ITestRunSession;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.util.DisplayHelper;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import org.eclipse.core.commands.Command;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;

import org.eclipse.debug.core.ILaunch;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.internal.junit.model.JUnitModel;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.ui.TestRunnerViewPart;

import org.eclipse.jdt.internal.ui.viewsupport.ViewHistory;

public class ImportedTestRunReloadUITest extends AbstractTestRunListenerTest {

	private static final String RELOAD_COMMAND= "org.eclipse.jdt.junit.reloadImportedTestRun"; //$NON-NLS-1$

	@Rule
	public TemporaryFolder fTemporaryFolder= new TemporaryFolder();

	private final List<TestRunSession> fSessionsToRemove= new ArrayList<>();
	private IWorkbenchPage fPage;
	private IWorkbenchPart fPreviousPart;
	private TestRunnerViewPart fView;
	private ViewHistory<TestRunSession> fHistory;
	private TestRunSession fPreviousSession;
	private boolean fViewWasOpen;
	private Command fReloadCommand;

	@Override
	@Before
	public void setUp() throws Exception {
		fProject= JavaProjectHelper.createJavaProject("ImportedTestRunReloadUITest", "bin"); //$NON-NLS-1$ //$NON-NLS-2$
		JavaProjectHelper.addToClasspath(fProject, JavaCore.newContainerEntry(JUnitCore.JUNIT4_CONTAINER_PATH));
		JavaProjectHelper.addRTJar15(fProject);
		fPage= JUnitPlugin.getActivePage();
		assertNotNull(fPage);
		fPreviousPart= fPage.getActivePart();
		fViewWasOpen= fPage.findView(TestRunnerViewPart.NAME) != null;
		fView= (TestRunnerViewPart) fPage.showView(TestRunnerViewPart.NAME);
		fPreviousSession= fView.getTestRunSession();
		Field historyField= TestRunnerViewPart.class.getDeclaredField("fViewHistory"); //$NON-NLS-1$
		historyField.setAccessible(true);
		@SuppressWarnings("unchecked")
		ViewHistory<TestRunSession> history= (ViewHistory<TestRunSession>) historyField.get(fView);
		fHistory= history;
		fPage.activate(fView);
		DisplayHelper.driveEventQueue(Display.getCurrent());
		ICommandService commandService= fView.getSite().getService(ICommandService.class);
		fReloadCommand= commandService.getCommand(RELOAD_COMMAND);
		assertTrue(fReloadCommand.isDefined());
		assertTrue(fReloadCommand.isHandled());
	}

	@After
	public void tearDownView() {
		JUnitModel model= JUnitCorePlugin.getModel();
		try {
			if (fHistory != null) {
				fHistory.setActiveEntry(model.getTestRunSessions().contains(fPreviousSession) ? fPreviousSession : null);
			}
		} finally {
			for (TestRunSession session : fSessionsToRemove) {
				if (session != null && model.getTestRunSessions().contains(session)) {
					model.removeTestRunSession(session);
				}
			}
			if (fView != null && !fViewWasOpen) {
				fPage.hideView(fView);
			}
			if (fPreviousPart != null) {
				fPage.activate(fPreviousPart);
			}
			DisplayHelper.driveEventQueue(Display.getCurrent());
		}
	}

	@Test
	public void testReloadEnablementFollowsHistorySelection() throws Exception {
		JUnitModel model= JUnitCorePlugin.getModel();
		Path resultFile= fTemporaryFolder.newFile("results.xml").toPath(); //$NON-NLS-1$
		writeTestRun(resultFile, "original run"); //$NON-NLS-1$
		TestRunSession imported= JUnitModel.importTestRunSession(resultFile.toFile());
		fSessionsToRemove.add(imported);
		DisplayHelper.driveEventQueue(Display.getCurrent());

		TestRunSession other= new TestRunSession("non-file session", null); //$NON-NLS-1$
		fSessionsToRemove.add(other);
		model.addTestRunSession(other);
		DisplayHelper.driveEventQueue(Display.getCurrent());

		// Use the same history entry point as the drop-down without refreshing the handler in the test.
		fHistory.setActiveEntry(imported);
		assertReloadState(imported, true);
		fHistory.setActiveEntry(other);
		assertReloadState(other, false);
		fHistory.setActiveEntry(imported);
		assertReloadState(imported, true);

		int historySize= model.getTestRunSessions().size();
		writeTestRun(resultFile, "reloaded run"); //$NON-NLS-1$
		IHandlerService handlerService= fView.getSite().getService(IHandlerService.class);
		handlerService.executeCommand(RELOAD_COMMAND, null);
		DisplayHelper.driveEventQueue(Display.getCurrent());
		TestRunSession reloaded= fView.getTestRunSession();
		fSessionsToRemove.add(reloaded);
		assertNotNull(reloaded);
		assertNotSame(imported, reloaded);
		assertEquals("reloaded run", reloaded.getTestRunName()); //$NON-NLS-1$
		assertEquals(historySize, model.getTestRunSessions().size());
		assertReloadState(reloaded, true);

		fHistory.setActiveEntry(null);
		assertReloadState(null, false);
		fHistory.setActiveEntry(reloaded);
		assertReloadState(reloaded, true);
		model.removeTestRunSession(reloaded);
		DisplayHelper.driveEventQueue(Display.getCurrent());
		assertFalse(model.getTestRunSessions().contains(reloaded));
		assertReloadState(other, false);
	}

	@Test
	public void testReloadEnablementAfterJUnitLaunch() throws Exception {
		JUnitModel model= JUnitCorePlugin.getModel();
		Path resultFile= fTemporaryFolder.newFile("launched-results.xml").toPath(); //$NON-NLS-1$
		writeTestRun(resultFile, "imported before launch"); //$NON-NLS-1$
		TestRunSession imported= JUnitModel.importTestRunSession(resultFile.toFile());
		fSessionsToRemove.add(imported);
		String source= """
				package reload;
				import org.junit.Test;
				public class LaunchedTest {
				    @Test public void testPass() { }
				}
				"""; //$NON-NLS-1$
		IType testType= createType(source, "reload", "LaunchedTest.java"); //$NON-NLS-1$ //$NON-NLS-2$
		AtomicReference<ILaunch> launched= new AtomicReference<>();
		AtomicBoolean finished= new AtomicBoolean();
		AtomicInteger finishedTests= new AtomicInteger();
		AtomicReference<String> finishedTest= new AtomicReference<>();
		AtomicReference<Result> testResult= new AtomicReference<>();
		AtomicReference<Result> sessionResult= new AtomicReference<>();
		TestRunListener listener= new TestRunListener() {
			@Override
			public void sessionLaunched(ITestRunSession session) {
				if (fProject.equals(session.getLaunchedProject())) {
					launched.compareAndSet(null, ((TestRunSession) session).getLaunch());
				}
			}

			private boolean isLaunchedSession(ITestRunSession session) {
				return launched.get() != null && ((TestRunSession) session).getLaunch() == launched.get();
			}

			@Override
			public void testCaseFinished(ITestCaseElement testCase) {
				if (isLaunchedSession(testCase.getTestRunSession())) {
					finishedTest.set(testCase.getTestClassName() + "#" + testCase.getTestMethodName()); //$NON-NLS-1$
					testResult.set(testCase.getTestResult(false));
					finishedTests.incrementAndGet();
				}
			}

			@Override
			public void sessionFinished(ITestRunSession session) {
				if (isLaunchedSession(session)) {
					sessionResult.set(session.getTestResult(true));
					finished.set(true);
				}
			}
		};
		JUnitCore.addTestRunListener(listener);
		try {
			launchJUnit(testType, TestKindRegistry.JUNIT4_TEST_KIND_ID);
			assertTrue("The launched JUnit session must finish", waitForCondition(finished::get, 15000, 100)); //$NON-NLS-1$
			assertEquals(1, finishedTests.get());
			assertEquals("reload.LaunchedTest#testPass", finishedTest.get()); //$NON-NLS-1$
			assertEquals(Result.OK, testResult.get());
			assertEquals(Result.OK, sessionResult.get());

			// Identify the session by the actual launch, not by its position in history.
			List<TestRunSession> sessions= model.getTestRunSessions().stream()
					.filter(session -> session.getLaunch() == launched.get()).toList();
			assertEquals(1, sessions.size());
			TestRunSession executed= sessions.get(0);
			assertEquals(ProgressState.COMPLETED, executed.getProgressState());
			assertNull(model.getImportedTestRunSource(executed));
			// A launch may activate a different part. Set focus once, not between history selections.
			fPage.activate(fView);
			for (int i= 0; i < 2; i++) {
				fHistory.setActiveEntry(imported);
				assertReloadState(imported, true);
				fHistory.setActiveEntry(executed);
				assertReloadState(executed, false);
			}
			fHistory.setActiveEntry(imported);
			assertReloadState(imported, true);

			int historySize= model.getTestRunSessions().size();
			writeTestRun(resultFile, "reloaded after launch"); //$NON-NLS-1$
			fView.getSite().getService(IHandlerService.class).executeCommand(RELOAD_COMMAND, null);
			DisplayHelper.driveEventQueue(Display.getCurrent());
			TestRunSession reloaded= fView.getTestRunSession();
			fSessionsToRemove.add(reloaded);
			assertNotNull(reloaded);
			assertNotSame(imported, reloaded);
			assertNotSame(executed, reloaded);
			assertEquals("reloaded after launch", reloaded.getTestRunName()); //$NON-NLS-1$
			assertEquals(historySize, model.getTestRunSessions().size());
			assertTrue(model.getTestRunSessions().contains(executed));
			assertEquals(Result.OK, executed.getTestResult(true));
			assertNull(model.getImportedTestRunSource(executed));
			assertReloadState(reloaded, true);
			fHistory.setActiveEntry(executed);
			assertReloadState(executed, false);
			fHistory.setActiveEntry(reloaded);
			assertReloadState(reloaded, true);
		} finally {
			JUnitCore.removeTestRunListener(listener);
			ILaunch launch= launched.get();
			if (launch != null) {
				try {
					if (!launch.isTerminated()) {
						launch.terminate();
					}
				} finally {
					model.getTestRunSessions().stream().filter(session -> session.getLaunch() == launch)
							.forEach(fSessionsToRemove::add);
				}
			}
		}
	}

	private void assertReloadState(TestRunSession session, boolean enabled) throws Exception {
		DisplayHelper.driveEventQueue(Display.getCurrent());
		assertSame(session, fView.getTestRunSession());
		assertSame(fView, fPage.getActivePart());
		ToolItem item= findToolItem(fView.getSite().getShell(), fReloadCommand.getDescription());
		assertNotNull("Reload toolbar item must be present", item); //$NON-NLS-1$
		// Toolbar enablement is updated asynchronously. Observe the widget before
		// querying the command, since isEnabled() can itself re-evaluate the handler.
		assertTrue("Reload toolbar must follow the selected history entry (enabled=" + enabled + ")", new DisplayHelper() { //$NON-NLS-1$ //$NON-NLS-2$
			@Override
			protected boolean condition() {
				return !item.isDisposed() && item.getEnabled() == enabled;
			}
		}.waitForCondition(Display.getCurrent(), 5000));
		assertEquals("Reload command must follow the selected history entry", enabled, fReloadCommand.isEnabled()); //$NON-NLS-1$
		assertNotNull("Reload toolbar item must have an image", item.getImage()); //$NON-NLS-1$
	}

	private static ToolItem findToolItem(Composite parent, String tooltip) {
		for (Control child : parent.getChildren()) {
			if (child instanceof ToolBar toolbar) {
				for (ToolItem item : toolbar.getItems()) {
					if (tooltip.equals(item.getToolTipText())) {
						return item;
					}
				}
			}
			if (child instanceof Composite composite) {
				ToolItem item= findToolItem(composite, tooltip);
				if (item != null) {
					return item;
				}
			}
		}
		return null;
	}

	private static void writeTestRun(Path file, String name) throws Exception {
		String xml= """
				<?xml version="1.0" encoding="UTF-8"?>
				<testrun name="%s" tests="1" started="1" failures="0" errors="0" ignored="0">
				  <testsuite name="example.Tests" time="0.0">
				    <testcase name="test" classname="example.Tests" time="0.0"/>
				  </testsuite>
				</testrun>
				""".formatted(name); //$NON-NLS-1$
		Files.writeString(file, xml, StandardCharsets.UTF_8);
	}
}

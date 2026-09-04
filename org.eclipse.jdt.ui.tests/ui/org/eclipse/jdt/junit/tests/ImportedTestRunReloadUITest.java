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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.model.JUnitModel;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.ui.TestRunnerViewPart;

import org.eclipse.jdt.internal.ui.viewsupport.ViewHistory;

public class ImportedTestRunReloadUITest {

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

	@Before
	public void setUp() throws Exception {
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
	public void tearDown() {
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

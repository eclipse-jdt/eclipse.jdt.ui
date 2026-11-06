/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.dialogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.util.DisplayHelper;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.ui.dialogs.FilteredList;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.ui.propertiesfileeditor.KeyReferenceSelectionDialog;
import org.eclipse.jdt.internal.ui.propertiesfileeditor.KeyReferenceSelectionDialog.KeyLocation;

/**
 * Tests that an outdated asynchronous preview of the {@link KeyReferenceSelectionDialog} is
 * ignored.
 * <p>
 * Each test lets the preview job of the first entry finish without dispatching UI events, so its
 * result is queued with {@link Display#asyncExec(Runnable)} but not yet delivered. The test then
 * changes the dialog and delivers the queued result.
 * </p>
 */
public class KeyReferenceSelectionDialogTests {

	private static final String CONTENT_A= "first.key=Alpha\n";

	private static final String CONTENT_B= "second.key=Beta\n";

	private static IJavaProject testProject;

	private static Map<Object, KeyLocation> locations;

	private Shell shell;

	private KeyReferenceSelectionDialog dialog;

	@BeforeAll
	public static void setUpAll() throws CoreException {
		testProject= JavaProjectHelper.createJavaProject("KeyReferenceSelectionDialogTests", "bin");
		IFile fileA= createFile("a.properties", CONTENT_A);
		IFile fileB= createFile("b.properties", CONTENT_B);
		locations= Map.of(
				"A", new KeyLocation(fileA, 0, "first.key".length()),
				"B", new KeyLocation(fileB, 0, "second.key".length()));
	}

	@AfterAll
	public static void tearDownAll() throws CoreException {
		if (testProject != null) {
			JavaProjectHelper.delete(testProject);
		}
	}

	@BeforeEach
	public void setUp() throws InterruptedException {
		shell= new Shell();
		dialog= new KeyReferenceSelectionDialog(shell, new LabelProvider(), new LabelProvider(), locations::get);
		dialog.setBlockOnOpen(false);
		dialog.setElements(new Object[] { "A", "B" });
		dialog.open();
		// The upper list selects its first entry when its table is filled, which fills the lower list
		// and starts the preview job of "A".
		Table lowerTable= findTable(false);
		dispatchUntil(() -> lowerTable.getItemCount() > 0, "The lower list should be filled");
	}

	@AfterEach
	public void tearDown() {
		shell.dispose();
	}

	/**
	 * Guards the other tests: without it, "the preview is empty" would also hold if no preview was
	 * ever shown.
	 */
	@Test
	public void testPreviewIsShownWhenDelivered() throws InterruptedException {
		joinPreviewJob();
		assertEquals("", previewText(), "The result should stay queued until events are dispatched");

		DisplayHelper.driveEventQueue(Display.getCurrent());
		assertEquals(CONTENT_A, previewText());
	}

	@Test
	public void testOutdatedPreviewDoesNotReplaceNewSelection() throws InterruptedException {
		joinPreviewJob();

		selectUpperRow(1);
		DisplayHelper.driveEventQueue(Display.getCurrent());
		assertNotEquals(CONTENT_A, previewText(), "The outdated preview of the first entry must be ignored");

		dispatchUntil(() -> CONTENT_B.equals(previewText()), "The preview of the second entry should be shown");
	}

	@Test
	public void testOutdatedPreviewIsNotRestoredAfterClear() throws InterruptedException {
		joinPreviewJob();

		// Clearing the selection clears the preview and makes the queued result outdated.
		Table upperTable= findTable(true);
		upperTable.deselectAll();
		upperTable.notifyListeners(SWT.Selection, new Event());
		DisplayHelper.driveEventQueue(Display.getCurrent());
		assertEquals("", previewText(), "An outdated preview must not be shown after the list was cleared");
	}

	@Test
	public void testOutdatedPreviewIsIgnoredWhenShellIsDisposed() throws InterruptedException {
		joinPreviewJob();
		Shell dialogShell= dialog.getShell();

		// Dispose the shell directly, bypassing Dialog.close(). The text widget of the viewer is then
		// gone (getTextWidget() returns null) when the queued result is delivered.
		dialogShell.dispose();

		// An exception thrown while delivering the queued result fails the test.
		DisplayHelper.driveEventQueue(Display.getCurrent());
		assertTrue(dialogShell.isDisposed(), "The dialog shell should be disposed");
	}

	private static IFile createFile(String name, String content) throws CoreException {
		IFile file= testProject.getProject().getFile(name);
		file.create(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), true, null);
		return file;
	}

	private void selectUpperRow(int index) {
		Table upperTable= findTable(true);
		upperTable.setSelection(index);
		upperTable.notifyListeners(SWT.Selection, new Event());
	}

	/**
	 * Returns the table of the upper (filtered) or the lower list of the dialog.
	 */
	private Table findTable(boolean upper) {
		List<Table> tables= new ArrayList<>();
		collectTables(dialog.getShell(), tables);
		for (Table table : tables) {
			if ((table.getParent() instanceof FilteredList) == upper) {
				return table;
			}
		}
		return fail("The " + (upper ? "upper" : "lower") + " list should exist");
	}

	private static void collectTables(Control control, List<Table> tables) {
		if (control instanceof Table table) {
			tables.add(table);
		} else if (control instanceof Composite composite) {
			for (Control child : composite.getChildren()) {
				collectTables(child, tables);
			}
		}
	}

	private String previewText() {
		return findStyledText(dialog.getShell()).getText();
	}

	private static StyledText findStyledText(Control control) {
		if (control instanceof StyledText styledText) {
			return styledText;
		}
		if (control instanceof Composite composite) {
			for (Control child : composite.getChildren()) {
				StyledText styledText= findStyledText(child);
				if (styledText != null) {
					return styledText;
				}
			}
		}
		return null;
	}

	/**
	 * Waits until all preview jobs of the dialog have finished, without dispatching UI events.
	 * Blocking the UI thread is safe here because the job only calls
	 * {@link Display#asyncExec(Runnable)}, so its result stays queued until the event loop runs.
	 */
	private static void joinPreviewJob() throws InterruptedException {
		long deadline= System.currentTimeMillis() + 5000;
		while (Job.getJobManager().find(KeyReferenceSelectionDialog.PREVIEW_JOB_FAMILY).length > 0) {
			assertTrue(System.currentTimeMillis() < deadline, "The preview job did not finish in time");
			Thread.sleep(10);
		}
	}

	/**
	 * Dispatches one event at a time until the condition holds. {@link DisplayHelper} is not used
	 * because it dispatches all pending events at once, which could also deliver the queued result
	 * of a preview job before the test is ready for it.
	 */
	private static void dispatchUntil(BooleanSupplier condition, String message) throws InterruptedException {
		Display display= Display.getCurrent();
		long deadline= System.currentTimeMillis() + 5000;
		while (!condition.getAsBoolean()) {
			assertTrue(System.currentTimeMillis() < deadline, message);
			if (!display.readAndDispatch()) {
				Thread.sleep(10);
			}
		}
	}
}

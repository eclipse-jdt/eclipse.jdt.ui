/*******************************************************************************
 * Copyright (c) 2025, 2026 Patrick Ziegler and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Patrick Ziegler - initial API and implementation
 *     IBM Corporation - Added tests for preview section for open type.
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.dialogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.util.DisplayHelper;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.jface.dialogs.IDialogConstants;

import org.eclipse.ui.dialogs.SelectionDialog;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.TypeNameMatch;

import org.eclipse.jdt.internal.core.search.JavaSearchTypeNameMatch;
import org.eclipse.jdt.internal.corext.util.OpenTypeHistory;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.dialogs.FilteredTypesSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.OpenTypeSelectionDialog;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

/**
 * Test case for the {@link FilteredTypesSelectionDialog} to make sure that the
 * suggested types fit to the search string.
 */
public class FilteredTypesSelectionDialogTests {
	private static IJavaProject testProject;
	private static IPackageFragment testPackage;
	private static String CU_NAME = "Test.java";
	private static String CU_CONTENT = """
			package test;
			public class Test {
			}
			""";

	private FilteredTypesSelectionDialog dialog;
	private SelectionDisplayHelper displayHelper;
	private Shell shell;

	@BeforeAll
	public static void setUpAll() throws CoreException {
		testProject = JavaProjectHelper.createJavaProject("TestProject", "bin");
		JavaProjectHelper.addRTJar(testProject);

		IPackageFragmentRoot packageRoot = JavaProjectHelper.addSourceContainer(testProject, "src");
		testPackage = packageRoot.createPackageFragment("test", true, null);
		ICompilationUnit cu = testPackage.createCompilationUnit(CU_NAME, CU_CONTENT, true, null);
		EditorUtility.openInSpecificEditor(cu, JavaUI.ID_CU_EDITOR, true);
	}

	@AfterAll
	public static void tearDownAll() throws CoreException {
		if (testProject != null) {
			JavaProjectHelper.delete(testProject);
		}
	}

	@BeforeEach
	public void setUp() {
		shell = new Shell();
		dialog = new FilteredTypesSelectionDialog(shell, false, null, null, IJavaSearchConstants.TYPE);
		dialog.setBlockOnOpen(false);
		displayHelper = new SelectionDisplayHelper(dialog);
	}

	@AfterEach
	public void tearDown() {
		shell.dispose();
	}

	@ParameterizedTest
	@CsvSource({
		// https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/2387
		"java.lang.AbstractBuilder, java.lang.AbstractStringBuilder",
		// https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/2505
		"java.lang.String, java.lang.String",
		// https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/2538
		"OOME, java.lang.OutOfMemoryError",
		"OOfM, java.lang.OutOfMemoryError"
	})
	public void testWithSearchString(String pattern, String expectedType) {
		dialog.setInitialPattern(pattern);
		dialog.setTitle("Search for type");
		dialog.open();
		displayHelper.waitForCondition(Display.getCurrent(), 5000);

		JavaSearchTypeNameMatch firstMatch = displayHelper.getFirstMatch();
		assertEquals(expectedType, firstMatch.getFullyQualifiedName());
	}

	@Test
	public void testOpenTypeSelectionDialogPreview() {
		OpenTypeSelectionDialog openTypeDialog = new OpenTypeSelectionDialog(shell, false, null, null, IJavaSearchConstants.TYPE);
		openTypeDialog.setBlockOnOpen(false);
		SelectionDisplayHelper helper = new SelectionDisplayHelper(openTypeDialog);

		openTypeDialog.setInitialPattern("Test");
		openTypeDialog.setTitle("Open Type");
		openTypeDialog.open();
		helper.waitForCondition(Display.getCurrent(), 5000);

		Table table = helper.getTable();
		assertNotNull(table);
		assertTrue(table.getItemCount() > 0);

		StyledText styledText = findStyledText(openTypeDialog.getShell());
		assertNotNull(styledText, "Preview StyledText should be created in OpenTypeSelectionDialog");

		table.setSelection(0);
		table.notifyListeners(SWT.Selection, new Event());

		// the preview is computed in a background job
		assertTrue(waitFor(() -> styledText.getText().contains("public class Test")), "Preview should display source of Test class");

		// removing the selection clears the preview
		table.deselectAll();
		table.notifyListeners(SWT.Selection, new Event());
		DisplayHelper.driveEventQueue(Display.getCurrent());
		assertTrue(styledText.getText().isEmpty(), "Preview should be cleared when the selection is removed");

		openTypeDialog.close();
	}

	@Test
	public void testOpenTypeSelectionDialogPreviewMultiSelection() {
		OpenTypeSelectionDialog openTypeDialog = new OpenTypeSelectionDialog(shell, true, null, null, IJavaSearchConstants.TYPE);
		openTypeDialog.setBlockOnOpen(false);
		SelectionDisplayHelper helper = new SelectionDisplayHelper(openTypeDialog);

		openTypeDialog.setInitialPattern("T*");
		openTypeDialog.setTitle("Open Type");
		openTypeDialog.open();
		helper.waitForCondition(Display.getCurrent(), 5000);

		Table table = helper.getTable();
		assertNotNull(table);
		assertTrue(table.getItemCount() > 1, "Two matches are required to test multi-selection");

		StyledText styledText = findStyledText(openTypeDialog.getShell());
		assertNotNull(styledText, "Preview StyledText should be created in OpenTypeSelectionDialog");

		table.setSelection(0);
		table.notifyListeners(SWT.Selection, new Event());
		assertTrue(waitFor(() -> !styledText.getText().isEmpty()), "Single selection should populate the preview");

		table.setSelection(new int[] { 0, 1 });
		table.notifyListeners(SWT.Selection, new Event());
		DisplayHelper.driveEventQueue(Display.getCurrent());
		assertTrue(styledText.getText().isEmpty(), "Preview should be cleared when multiple items are selected");

		openTypeDialog.close();
	}

	@Test
	public void testOpenTypeSelectionDialogPreviewNotRestoredAfterClear() throws InterruptedException {
		OpenTypeSelectionDialog openTypeDialog = new OpenTypeSelectionDialog(shell, false, null, null, IJavaSearchConstants.TYPE);
		openTypeDialog.setBlockOnOpen(false);
		SelectionDisplayHelper helper = new SelectionDisplayHelper(openTypeDialog);

		openTypeDialog.setInitialPattern("Test");
		openTypeDialog.setTitle("Open Type");
		openTypeDialog.open();
		helper.waitForCondition(Display.getCurrent(), 5000);

		Table table = helper.getTable();
		assertNotNull(table);
		assertTrue(table.getItemCount() > 0);
		StyledText styledText = findStyledText(openTypeDialog.getShell());
		assertNotNull(styledText, "Preview StyledText should be created in OpenTypeSelectionDialog");

		table.setSelection(0);
		table.notifyListeners(SWT.Selection, new Event());

		// Block the UI thread without dispatching events: the background job finishes and its
		// result is queued with asyncExec, but not yet delivered.
		joinPreviewJob();

		// Clearing now makes the queued result outdated. It must be ignored when delivered.
		table.deselectAll();
		table.notifyListeners(SWT.Selection, new Event());
		DisplayHelper.driveEventQueue(Display.getCurrent());
		assertTrue(styledText.getText().isEmpty(), "An outdated preview must not be shown after the preview was cleared");

		openTypeDialog.close();
	}

	@Test
	public void testOpenTypeSelectionDialogCloseWhilePreviewPending() throws InterruptedException {
		OpenTypeSelectionDialog openTypeDialog = new OpenTypeSelectionDialog(shell, false, null, null, IJavaSearchConstants.TYPE);
		openTypeDialog.setBlockOnOpen(false);
		SelectionDisplayHelper helper = new SelectionDisplayHelper(openTypeDialog);

		openTypeDialog.setInitialPattern("Test");
		openTypeDialog.setTitle("Open Type");
		openTypeDialog.open();
		helper.waitForCondition(Display.getCurrent(), 5000);

		Table table = helper.getTable();
		assertNotNull(table);
		assertTrue(table.getItemCount() > 0);

		table.setSelection(0);
		table.notifyListeners(SWT.Selection, new Event());

		// Block the UI thread without dispatching events, so the job's result is queued but not delivered.
		joinPreviewJob();
		openTypeDialog.close();

		// Delivering the queued result after the dialog is gone must neither throw nor touch disposed widgets.
		// An exception thrown here fails the test.
		DisplayHelper.driveEventQueue(Display.getCurrent());
		assertTrue(openTypeDialog.getShell() == null || openTypeDialog.getShell().isDisposed(), "Dialog should be closed");
	}

	@Test
	public void testOpenTypeSelectionDialogShellDisposedWhilePreviewPending() throws InterruptedException {
		OpenTypeSelectionDialog openTypeDialog = new OpenTypeSelectionDialog(shell, false, null, null, IJavaSearchConstants.TYPE);
		openTypeDialog.setBlockOnOpen(false);
		SelectionDisplayHelper helper = new SelectionDisplayHelper(openTypeDialog);

		openTypeDialog.setInitialPattern("Test");
		openTypeDialog.setTitle("Open Type");
		openTypeDialog.open();
		helper.waitForCondition(Display.getCurrent(), 5000);

		Table table = helper.getTable();
		assertNotNull(table);
		assertTrue(table.getItemCount() > 0);
		Shell dialogShell = openTypeDialog.getShell();
		assertNotNull(dialogShell);

		table.setSelection(0);
		table.notifyListeners(SWT.Selection, new Event());

		// Block the UI thread without dispatching events, so the job's result is queued but not delivered.
		joinPreviewJob();

		// Dispose the shell directly, bypassing Dialog.close(). The viewer's text widget is then gone
		// (getTextWidget() returns null) when the queued result is delivered.
		dialogShell.dispose();

		// An exception thrown while delivering the queued result fails the test.
		DisplayHelper.driveEventQueue(Display.getCurrent());
		assertTrue(dialogShell.isDisposed(), "Dialog shell should be disposed");
	}

	@Test
	public void testOpenTypeSelectionDialogPreviewUsableAfterClear() {
		OpenTypeSelectionDialog openTypeDialog = new OpenTypeSelectionDialog(shell, true, null, null, IJavaSearchConstants.TYPE);
		openTypeDialog.setBlockOnOpen(false);
		SelectionDisplayHelper helper = new SelectionDisplayHelper(openTypeDialog);

		openTypeDialog.setInitialPattern("T*");
		openTypeDialog.setTitle("Open Type");
		openTypeDialog.open();
		helper.waitForCondition(Display.getCurrent(), 5000);

		Table table = helper.getTable();
		assertNotNull(table);
		assertTrue(table.getItemCount() > 1, "Two matches are required to clear the preview by multi-selection");
		StyledText styledText = findStyledText(openTypeDialog.getShell());
		assertNotNull(styledText, "Preview StyledText should be created in OpenTypeSelectionDialog");

		table.setSelection(0);
		table.notifyListeners(SWT.Selection, new Event());
		assertTrue(waitFor(() -> !styledText.getText().isEmpty()), "Single selection should populate the preview");

		table.setSelection(new int[] { 0, 1 });
		table.notifyListeners(SWT.Selection, new Event());
		DisplayHelper.driveEventQueue(Display.getCurrent());
		assertTrue(styledText.getText().isEmpty(), "Preview should be cleared when multiple items are selected");

		// The viewer must still work after it was cleared
		table.setSelection(0);
		table.notifyListeners(SWT.Selection, new Event());
		assertTrue(waitFor(() -> !styledText.getText().isEmpty()), "Preview should be shown again after it was cleared");

		openTypeDialog.close();
	}

	@Test
	public void testOpenTypeSelectionDialogPreviewFollowsHistorySelection() throws CoreException {
		IType firstType = testPackage.getCompilationUnit(CU_NAME).getType("Test");
		ICompilationUnit secondUnit = testPackage.createCompilationUnit("HistorySecond.java", """
				package test;
				public class HistorySecond {
				}
				""", true, null);
		OpenTypeHistory history = OpenTypeHistory.getInstance();
		try {
			// Fill the history the way a user does: open two types with the dialog.
			openTypeWithDialog("Test");
			openTypeWithDialog("HistorySecond");

			// no initial pattern: the list shows the history
			OpenTypeSelectionDialog openTypeDialog = new OpenTypeSelectionDialog(shell, false, null, null, IJavaSearchConstants.TYPE);
			openTypeDialog.setBlockOnOpen(false);
			SelectionDisplayHelper helper = new SelectionDisplayHelper(openTypeDialog);
			openTypeDialog.setTitle("Open Type");
			openTypeDialog.open();
			helper.waitForCondition(Display.getCurrent(), 5000);

			Table table = helper.getTable();
			assertNotNull(table);
			StyledText styledText = findStyledText(openTypeDialog.getShell());
			assertNotNull(styledText, "Preview StyledText should be created in OpenTypeSelectionDialog");
			assertTrue(waitFor(() -> indexOfType(table, "Test") >= 0 && indexOfType(table, "HistorySecond") >= 0),
					"Both history entries should be listed");

			selectRow(table, indexOfType(table, "Test"));
			assertTrue(waitFor(() -> styledText.getText().contains("public class Test")), "Preview should follow the selected history entry");

			selectRow(table, indexOfType(table, "HistorySecond"));
			assertTrue(waitFor(() -> styledText.getText().contains("public class HistorySecond")), "Preview should switch to the next history entry");

			selectRow(table, indexOfType(table, "Test"));
			assertTrue(waitFor(() -> styledText.getText().contains("public class Test")), "Preview should switch back to the previous history entry");

			openTypeDialog.close();
		} finally {
			for (TypeNameMatch match : history.getTypeInfos()) {
				if (firstType.equals(match.getType()) || secondUnit.getType("HistorySecond").equals(match.getType())) {
					history.remove(match);
				}
			}
			secondUnit.delete(true, null);
		}
	}

	/**
	 * Opens the type with the given name using the dialog, so that it is added to the history.
	 */
	private void openTypeWithDialog(String typeName) {
		OpenTypeSelectionDialog openTypeDialog = new OpenTypeSelectionDialog(shell, false, null, null, IJavaSearchConstants.TYPE);
		openTypeDialog.setBlockOnOpen(false);
		SelectionDisplayHelper helper = new SelectionDisplayHelper(openTypeDialog);
		openTypeDialog.setInitialPattern(typeName);
		openTypeDialog.setTitle("Open Type");
		openTypeDialog.open();
		helper.waitForCondition(Display.getCurrent(), 5000);

		Table table = helper.getTable();
		assertNotNull(table);
		selectRow(table, indexOfType(table, typeName));
		Button openButton = findButton(openTypeDialog.getShell(), IDialogConstants.OK_ID);
		assertNotNull(openButton, "Open button should exist");
		openButton.notifyListeners(SWT.Selection, new Event());
		assertEquals(IDialogConstants.OK_ID, openTypeDialog.getReturnCode(), "Dialog should be closed with Open");
	}

	private static Button findButton(Control control, int id) {
		if (control instanceof Button button && Integer.valueOf(id).equals(button.getData())) {
			return button;
		}
		if (control instanceof Composite composite) {
			for (Control child : composite.getChildren()) {
				Button button = findButton(child, id);
				if (button != null) {
					return button;
				}
			}
		}
		return null;
	}

	private static int indexOfType(Table table, String typeName) {
		for (int i = 0; i < table.getItemCount(); i++) {
			if (table.getItem(i).getData() instanceof TypeNameMatch match && typeName.equals(match.getSimpleTypeName())) {
				return i;
			}
		}
		return -1;
	}

	private static void selectRow(Table table, int index) {
		assertTrue(index >= 0, "Row to select should exist");
		table.setSelection(index);
		table.notifyListeners(SWT.Selection, new Event());
	}

	/**
	 * Waits until all preview jobs of the {@link OpenTypeSelectionDialog} have finished, without
	 * dispatching UI events. Blocking the UI thread is safe here because the job only calls
	 * {@link Display#asyncExec(Runnable)}, so its result stays queued until the event loop runs.
	 */
	private static void joinPreviewJob() throws InterruptedException {
		long deadline = System.currentTimeMillis() + 5000;
		while (Job.getJobManager().find(OpenTypeSelectionDialog.PREVIEW_JOB_FAMILY).length > 0) {
			assertTrue(System.currentTimeMillis() < deadline, "Preview job did not finish in time");
			Thread.sleep(10);
		}
	}

	/**
	 * Runs the event loop until the condition holds or a timeout elapsed.
	 *
	 * @return <code>true</code> if the condition holds
	 */
	private static boolean waitFor(BooleanSupplier condition) {
		return new DisplayHelper() {
			@Override
			protected boolean condition() {
				return condition.getAsBoolean();
			}
		}.waitForCondition(Display.getCurrent(), 5000);
	}

	private static StyledText findStyledText(Control control) {
		if (control instanceof StyledText st) {
			return st;
		}
		if (control instanceof Composite composite) {
			for (Control child : composite.getChildren()) {
				StyledText st = findStyledText(child);
				if (st != null) {
					return st;
				}
			}
		}
		return null;
	}

	/**
	 * Utility class that exposes the table of the selection dialog. When the
	 * search string is updated, a job is notified that calculates the content
	 * of the table.
	 *
	 * This helper waits until this job is done by checking whether the table
	 * contains at least one element.
	 *
	 * Elements in this table are of type {@link JavaSearchTypeNameMatch}.
	 */
	private static class SelectionDisplayHelper extends DisplayHelper {
		private final SelectionDialog dialog;
		private Table table;

		public SelectionDisplayHelper(SelectionDialog dialog) {
			this.dialog = dialog;
		}

		@Override
		protected boolean condition() {
			// wait until search has finished
			return getTable().getItemCount() != 0;
		}

		public JavaSearchTypeNameMatch getFirstMatch() {
			return (JavaSearchTypeNameMatch) getTable().getItem(0).getData();
		}

		public Table getTable() {
			if (table == null) {
				table = findTable(dialog.getShell());
			}
			return table;
		}

		private static Table findTable(Control control) {
			if (control instanceof Table table) {
				return table;
			}
			if (control instanceof Composite composite) {
				for (Control child : composite.getChildren()) {
					Table table = findTable(child);
					if (table != null) {
						return table;
					}
				}
			}
			return null;
		}
	}
}

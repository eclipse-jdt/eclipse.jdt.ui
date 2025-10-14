/*******************************************************************************
 * Copyright (c) 2025 Patrick Ziegler others.
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
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.dialogs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.util.DisplayHelper;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.dialogs.SelectionDialog;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

import org.eclipse.jdt.internal.core.search.JavaSearchTypeNameMatch;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.dialogs.FilteredTypesSelectionDialog;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

/**
 * Test case for the {@link FilteredTypesSelectionDialog} to make sure that the
 * suggested types fit to the search string.
 */
public class FilteredTypesSelectionDialogTests {
	private static IJavaProject testProject;
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
		IPackageFragment packageFragment = packageRoot.createPackageFragment("test", true, null);
		ICompilationUnit cu = packageFragment.createCompilationUnit(CU_NAME, CU_CONTENT, true, null);
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
		"OOME, java.lang.OutOfMemoryError"
	})
	public void testWithSearchString(String pattern, String expectedType) {
		dialog.setInitialPattern(pattern);
		dialog.setTitle("Search for type");
		dialog.open();
		displayHelper.waitForCondition(Display.getCurrent(), 5000);

		JavaSearchTypeNameMatch firstMatch = displayHelper.getFirstMatch();
		assertEquals(expectedType, firstMatch.getFullyQualifiedName());
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

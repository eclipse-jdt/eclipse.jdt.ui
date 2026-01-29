/*******************************************************************************
 * Copyright (c) 2025, 2026 Daniel Schmid and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Daniel Schmid - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.folding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

public class FoldingWithShowSelectedElementTests {
	@Rule
	public ProjectTestSetup projectSetup= new ProjectTestSetup();

	private IJavaProject jProject;

	private IPackageFragmentRoot sourceFolder;

	private IPackageFragment packageFragment;

	@Before
	public void setUp() throws CoreException {
		jProject= projectSetup.getProject();
		sourceFolder= jProject.findPackageFragmentRoot(jProject.getResource().getFullPath().append("src"));
		if (sourceFolder == null) {
			sourceFolder= JavaProjectHelper.addSourceContainer(jProject, "src");
		}
		packageFragment= sourceFolder.createPackageFragment("org.example.test", false, null);
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_SHOW_SEGMENTS, true);
	}

	@After
	public void tearDown() {
		JavaPlugin.getDefault().getPreferenceStore().setToDefault(PreferenceConstants.EDITOR_SHOW_SEGMENTS);
		JavaPlugin.getDefault().getPreferenceStore().setToDefault(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGIONS_ENABLED);
	}

	@Test
	public void testFoldingActive() throws Exception {
		String str= """
				package org.example.test;
				public class A {
					void someMethod() {
						// this method should be folded
					}
				}
				""";
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "B.java", str);
		assertEquals(1, regions.size());
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 4);
	}

	@Test
	public void testInsertText() throws Exception {
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGIONS_ENABLED, true);
		String str= """
				package org.example.test;
				public class A {
					// region
					void someMethod() {
						// content here
					}
					// endregion
				}
				""";

		ICompilationUnit cu= packageFragment.createCompilationUnit("A.java", str, true, null);
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(cu);

		try {
			editor.setSelection(cu.getElementAt(str.indexOf("someMethod")));

			ProjectionAnnotationModel model= editor.getAdapter(ProjectionAnnotationModel.class);

			assertEquals("""
						// region
						void someMethod() {
							// content here
						}
					""", editor.getViewer().getTextWidget().getText());

			List<IRegion> regions= FoldingTestUtils.extractRegions(model);

			assertEquals(2, regions.size());

			IDocument document= editor.getViewer().getDocument();
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 6);
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 5);

			document.replace(str.indexOf("content"), 0, "method ");

			regions = FoldingTestUtils.extractRegions(model);
			str = document.get();

			assertEquals(2, regions.size());

			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 6);
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 5);
		} finally {
			editor.close(false);
		}
	}

	@Test
	public void testWithTrailingWhitespaceAfterClosingBrace() throws Exception {
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGIONS_ENABLED, true);
		String str= """
				package org.example.test;
				public class A {
					void someMethod() {
						// content here
					}\t\t\t\t
				}
				""";

		ICompilationUnit cu= packageFragment.createCompilationUnit("A.java", str, true, null);
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(cu);

		try {
			editor.setSelection(cu.getElementAt(str.indexOf("someMethod")));

			assertEquals("""
						void someMethod() {
							// content here
						}\t\t\t\t
					""", editor.getViewer().getTextWidget().getText());

		} finally {
			editor.close(false);
		}
	}
}

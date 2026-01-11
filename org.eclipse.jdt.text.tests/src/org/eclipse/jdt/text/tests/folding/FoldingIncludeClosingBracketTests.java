/*******************************************************************************
 * Copyright (c) 2026 Daniel Schmid and others.
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

import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;

// https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/2439
public class FoldingIncludeClosingBracketTests {
	@Rule
	public ProjectTestSetup projectSetup= new ProjectTestSetup();

	private IJavaProject jProject;

	private IPackageFragmentRoot sourceFolder;

	private IPackageFragment packageFragment;

	private IPreferenceStore preferenceStore;

	@Before
	public void setUp() throws CoreException {
		jProject= projectSetup.getProject();
		sourceFolder= jProject.findPackageFragmentRoot(jProject.getResource().getFullPath().append("src"));
		if (sourceFolder == null) {
			sourceFolder= JavaProjectHelper.addSourceContainer(jProject, "src");
		}
		packageFragment= sourceFolder.createPackageFragment("org.example.test", false, null);
		preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		configureEnhancedFolding(true);
	}

	private void configureEnhancedFolding(boolean useEnhancedFolding) {
		preferenceStore.setValue(PreferenceConstants.EDITOR_NEW_FOLDING_ENABLED, useEnhancedFolding);
	}

	/*
	 * Folding of control structures (e.g. if, for, while) does not include the closing bracket in the folding regions for consistency.
	 * To include the closing bracket there, a consistent decision is needed for all control structures.
	 * If such a decision is made, this test can be replaced by tests for the relevant situations.
	 */
	@Test
	public void testControlStructures_closingBracketShouldNotBeIncluded() throws Exception {
		String str= """
				package org.example.test;
				public class ControlStructures {
				    void x() {
				        if (true) {

				        }
				        for(int i=0;i<1;i++){

				        }
				        while(true) {

				        }
				        do {

				        } while(false);
				    };
				}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 5);
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 15); // x()
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 4); // if
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 6, 7); // for
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 9, 10); // while
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 12, 13); // do
	}

	@Test
	public void testOtherBlocks_closingBracketsShouldBeIncluded() throws Exception {
		String str= """
				package org.example.test;
				import java.util.function.Supplier;
				public class Suppliers {
				    void x() {
				        Supplier<String> s = () -> {
				            return "";
				        };

				        Supplier<String> s2 = () -> {
				            return "";
				        }; // this line should not be folded
				    }
				}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 3);
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 11); // x()
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 4, 6); // s
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 8, 9); // s2
	}

	@Test
	public void testAnnotatedEnums() throws Exception {
		configureEnhancedFolding(false);
		String str= """
				package org.example.test;
				enum TestEnum {
					@SomeAnnotation
					A,
					@SomeAnnotation
					@OtherAnnotation
					B,
					C,
					@SomeAnnotation
					D;

					@SomeAnnotation
					void someMethod() {

					}
				}
				@interface SomeAnnotation {}
				@interface OtherAnnotation {}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 4);
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 3); // A
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 4, 6); // B
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 8, 9); // C
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 11, 14); // someMethod
	}
}

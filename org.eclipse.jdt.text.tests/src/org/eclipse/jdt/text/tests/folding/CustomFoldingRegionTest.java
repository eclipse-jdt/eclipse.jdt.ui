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

import static org.junit.Assume.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

@RunWith(Parameterized.class)
public class CustomFoldingRegionTest {
	@Rule
	public ProjectTestSetup projectSetup= new ProjectTestSetup();

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	private IPackageFragment fPackageFragment;

	@Parameter
	public boolean extendedFoldingActive;

	@Parameters(name = "Extended folding active: {0}")
	public static Object[] extendedFoldingElements() {
		return new Object[] { true, false };
	}

	@Before
	public void setUp() throws CoreException {
		fJProject1= projectSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		fPackageFragment= fSourceFolder.createPackageFragment("org.example.test", false, null);
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGIONS_ENABLED, true);
		store.setValue(PreferenceConstants.EDITOR_NEW_FOLDING_ENABLED, extendedFoldingActive);
	}


	@After
	public void tearDown() throws CoreException {
		JavaProjectHelper.delete(fJProject1);
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setToDefault(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_START);
		store.setToDefault(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_END);
		store.setToDefault(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGIONS_ENABLED);
	}

	@Test
	public void testNoCustomFoldingRegions() throws Exception {
		String str= """
				package org.example.test;
				public class Test { }
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(0, projectionRanges.size());
	}

	@Test
	public void testCustomFoldingRegionInsideAndOutsideClass() throws Exception {
		String str= """
				package org.example.test;
				// region 1
				// something else
				// endregion
				class Test {
					// region 2
					// something else
					// endregion
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 1, 3); // region 1
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 5, 7); // region 2
	}

	@Test
	public void testNestedCustomRegions() throws Exception {
		String str= """
				package org.example.test;

				class Test {
					// region outer
					// region inner

					// endregion outer
					// endregion inner
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(3, projectionRanges.size());
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 2, 8);//class Test
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 7);//outer
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 6);//inner
	}

	@Test
	public void testCustomRegionsDisabled() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGIONS_ENABLED, false);

		String str= """
				package org.example.test;

				class Test {
					// region outer

					// endregion inner
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertDoesNotContainRegionUsingStartLine(projectionRanges, str, 3);//outer
	}

	@Test
	public void testCustomFoldingRegionsInMethod() throws Exception {
		String str= """
				package org.example.test;
				class Test {
					void a(){
						// region 1

						// endregion
					}
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 5); // region 1
	}

	@Test
	public void testNoCustomFoldingRegionsSingleImport() throws Exception {
		String str= """
				package org.example.test;

				import java.util.List;
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(0, projectionRanges.size());
	}

	@Test
	public void testCustomFoldingRegionAroundSingleImport() throws Exception {
		String str= """
				package org.example.test;

				// region imports
				import java.util.List;
				// endregion
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 2, 4); // imports
	}

	@Test
	public void testCustomFoldingRegionAroundClasses() throws Exception {
		String str= """
				package org.example.test;

				class A {

				}

				// region 1

				class B {

				}

				class C {

				}
				// endregion

				class D {

				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 6, 15); // region 1
	}

	@Test
	public void testCustomFoldingRegionsMultipleLevels() throws Exception {
		String str= """
				package org.example.test;
				// region outside class
				class Test {
					// endregion should be ignored with old folding
					// region outside method
					void a(){
						// endregion should be ignored
						// region inside method
						System.out.println("Hello World");
						// endregion inside method
					}
					// endregion outside method
				}
				// endregion outside class
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(5, projectionRanges.size());
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 2, 12);//class Test
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 1, 13);//outside class
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 11);//outside method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 7, 9);//inside method
		FoldingTestUtils.assertDoesNotContainRegionUsingStartAndEndLine(projectionRanges, str, 1, 3);//endregion should be ignored
	}

	@Test
	public void testCustomFoldingRegionsNotEndingTooEarly() throws Exception {
		String str= """
				package org.example.test;

				class Test {
					void a(){
						// region inside method
					}
					// endregion outside method
				}
				// endregion outside class
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertDoesNotContainRegionUsingStartLine(projectionRanges, str, 4);//region inside method
	}

	@Test
	public void testCustomFoldingRegionsUsingSpecialCommentTypes() throws Exception {
		String str= """
				package org.example.test;

				class SpecialCommentTypes {
					void a(){
						/* region multiline
						*/
						/** region javadoc */
						/** endregion javadoc */
						/* endregion multiline
						*/
					}
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		if (extendedFoldingActive) {
			assertEquals(5, projectionRanges.size());
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 2, 11);//class SpecialCommentTypes
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 10);//void a()
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 9);// multiline (folding region)
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 6, 7);// javadoc
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 8, 9);// multiline (last comment)
		} else {
			assertEquals(4, projectionRanges.size());
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 2, 11);//class SpecialCommentTypes
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 10);//void a()
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 9);// multiline
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 6, 7);// javadoc
		}
	}

	@Test
	public void testCustomRegionsWithLocalClass() throws Exception {
		String str= """
				package org.example.test;

				class Test {
					void a(){
						// region 1
						int i;

						// endregion
						class Inner{

						}
					}
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 7);//region 1
	}

	@Test
	public void testNoCustomRegionAtDifferentLevelsWithOtherClass() throws Exception {
		String str= """
				package org.example.test;

				class Test{
					// region outside
					class A {
						public void helloWorld() {

						}
						// endregion inside
					}

					class B {


				    }

				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertDoesNotContainRegionUsingStartLine(projectionRanges, str, 3);// region outside
	}

	@Test
	public void testCustomRegionsAroundFieldAndMethod() throws Exception {
		String str= """
				package org.example.test;

				class Test {
					// region 1
					int a;

					void b(){

					}
					// endregion
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 9);//region 1
	}

	@Test
	public void testDifferentConfiguration() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_START, "#regstart");
		store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_END, "#regend");


		String str= """
				package org.example.test;
				class Test {
					// region should be ignored
					// #regstart this is the region
					// #regend should end here
					// endregion should be ignored
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 4); // this is the region
	}

	@Test
	public void testCommentsInEmptyBlocks() throws Exception {
		String str= """
				package org.example.test;
				class Test {
					void a(){
						{/* region 1*/}
						System.out.println("Hello World");
						{/* endregion 1*/}
					}
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 5);//region 1
	}

	@Test
	public void testNoFoldingRegionWithAdditionalTokensAfterComment() throws Exception {
		String str= """
				package org.example.test;
				class Test {
					void a(){
						{/* region 1*/}
						System.out.println("Hello World");
						{/* endregion 1*/;}
					}
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertDoesNotContainRegionUsingStartLine(projectionRanges, str, 3);
	}

	@Test
	public void testNoFoldingRegionWithAdditionalTokensBeforeComment() throws Exception {
		String str= """
				package org.example.test;
				class Test {
					void a(){
						{;/* region 1*/}
						System.out.println("Hello World");
						{/* endregion 1*/}
					}
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertDoesNotContainRegionUsingStartLine(projectionRanges, str, 3);
	}

	@Test
	public void testBlockFoldingRegionAllowSemicolonBeforeBlock() throws Exception {
		String str= """
				package org.example.test;
				class Test {
					void a(){
						;{/* region 1*/}
						System.out.println("Hello World");
						{/* endregion 1*/}
					}
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 5);//region 1
	}

	@Test
	public void testBlockFoldingRegionAllowOtherBlockBeforeBlock() throws Exception {
		String str= """
				package org.example.test;
				class Test {
					void a(){
						{ }
						{/* region 1*/}
						System.out.println("Hello World");
						{/* endregion 1*/}
					}
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 6);//region 1
	}

	@Test
	public void testBlockFoldingRegionAllowCommentBeforeBlock() throws Exception {
		String str= """
				package org.example.test;
				class Test {
					void a(){
						// Hello
						{/* region 1*/}
						// World
						{/* endregion 1*/}
					}
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 6);//region 1
	}

	@Test
	public void testBlockFoldingRegionWithCodeInSameLineAsEnd() throws Exception {
		String str= """
				package org.example.test;
				class Test {
					void a(){
						{/* region 1*/}
						System.out.println("Hello");
						{/* endregion 1*/}System.out.println("World");
					}
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 4);//region 1
	}

	@Test
	public void testMethodWithSingleCommentIsNotUsedForCustomRegions() throws Exception {
		String str= """
				package org.example.test;
				class Test {
					void a(){/* region 1*/}

					/* endregion */
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertDoesNotContainRegionUsingStartLine(projectionRanges, str, 2);
	}

	@Test
	public void testMethodWithSingleCommentIsNotUsedForEndregionComment() throws Exception {
		String str= """
				package org.example.test;
				class Test {
					// region a

					void a(){/* endregion*/}
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertDoesNotContainRegionUsingStartLine(projectionRanges, str, 2);
	}

	@Test
	public void testSameStartAndEndMarkersWithinBlock() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_START, "----");
		store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_END, "----");
		String str= """
				package org.example.test;
				class Test {
					void a(){
						{/* --------- */}
						System.out.println("Hello");
						{/* --------- */}
						System.out.println("World");
					}
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 4);//region 1
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 5, 6);//region 1
	}

	@Test
	public void testNotFoldedWithinDifferentControlFlowStatements() throws Exception {
		assumeTrue("Only enabled with extended folding", extendedFoldingActive);
		String str= """
				package org.example.test;
				class Test {
					void a() {
						// region 1
						for (int i = 0; i < 10; i++) {
							// endregion
							// region 2
						}
						boolean b=false;
						// region 3
						while (b) {
							// endregion
						}
					}
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertDoesNotContainRegionUsingStartLine(projectionRanges, str, 3);// region 1
		FoldingTestUtils.assertDoesNotContainRegionUsingStartLine(projectionRanges, str, 6);// region 2
		FoldingTestUtils.assertDoesNotContainRegionUsingStartLine(projectionRanges, str, 9);// region 3
	}

	@Test
	public void testCustomRegionsWithElementAfterwards() throws Exception {
		String str= """
				package org.example.test;

				class Test {
					// region 1

					/* endregion */ void test(){}
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 4); // region 1
	}

	@Test
	public void testSameStartAndEndMarkers() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_START, "region");
		store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_END, "region");

		String str= """
				package org.example.test;

				class Test {
					// region first start

					// region second start

				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 4);//first start
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 5, 6);//second start
	}


	@Test
	public void testSameStartAndEndMarkersWithElementsInBetween() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_START, "----");
		store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_END, "----");

		String str= """
				package org.example.test;

				class Test {
					// ---- variables

					private int i = 0;
					private String s = "Hello World";

					// ---- methods

					void test() {
						System.out.println("a");
					}

					// ----
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 7);//variables
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 8, 13);//methods
	}

	@Test
	public void testSameStartAndEndMarkersNested() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_START, "----");
		store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_END, "----");

		String str= """
				package org.example.test;

				class Test {
					// ---- outer ----

					void test() {
						// ---- inner ----

						System.out.println("text");

						// ---- inner 2 ----

						System.out.println("more text");
					}

					// ---- outer 2 ----

					void otherMethod() {

					}
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 6, 9);//inner
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 10, 12);//inner 2
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 15, 19);//outer 2
	}

	@Test
	public void testStartMarkerStartsWithEndMarker() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_START, "reg");
		store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_END, "regend");

		String str= """
				package org.example.test;

				class Test {
					// reg no end marker

					// reg first

					// regend second

				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 8);//no end marker
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 5, 6);//first
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 7, 8);//second
	}

	@Test
	public void testStartMarkerStartsWithEndMarkerWithoutTopLevelType() throws Exception {
		assumeTrue("Only enabled with extended folding", extendedFoldingActive);
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_START, "reg");
		store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_END, "regend");

		String str= """
				package org.example.test;

				// reg no end marker

				// reg first

				// regend second
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertContainsRegionWithOffsetAndLength(projectionRanges, 2, 7, //no end marker
		FoldingTestUtils.findLineStartIndex(str, 2), str.length() - 1);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 5);//first
	}

	@Test
	public void testEndMarkerStartsWithStartMarker() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_START, "regstart");
		store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_END, "reg");

		String str= """
				package org.example.test;

				class Test {

					// reg no region

					// regstart first

					// reg first end

					// regstart second

					// regstart third

					// reg end
				}
		""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 6, 7);//first
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 10, 11);//second
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 12, 13);//third
	}

	@Test
	public void testSameStartAndEndMarkerTerminatesAtEOF() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_START, "reg");
		store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_END, "reg");

		String str= """
		// reg my region

		// some comment without line break""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 0, 2);
	}

	@Test
	public void testSameStartAndEndMarkerTerminatesAtEOFEmptyLine() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_START, "reg");
		store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_END, "reg");

		String str= """
		// reg my region


		""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 0, 3);
	}

	@Test
	public void testSameStartAndEndMarkerWithNestedFoldingRegionsAfterwards() throws Exception {
		assumeTrue(extendedFoldingActive);
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_START, "reg");
		store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_END, "reg");

		String str= """
				package org.example.test;

				class Repro {

					// reg a folding region should start here

					public String test(boolean b) {
						if (b)

							return "a";
						return "b";
					}

				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 12);//custom region
	}

	@Test
	public void testFoldingUpdateWithMultipleCustomRegionsDoesNotSwitchRegions() throws Exception {
		String code= """
				package org.example.test;

				// region outer

				class Test {
					// region middle

					// region inner
					void someMethod() {
						// content
					}
					// endregion
					// endregion
				}

				// endregion outer
				""";
		ICompilationUnit cu= fPackageFragment.createCompilationUnit("Test.java", code, true, null);
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(cu);
		try {
			ProjectionAnnotationModel model= editor.getAdapter(ProjectionAnnotationModel.class);

			List<IRegion> initialRegions= FoldingTestUtils.extractRegions(model);

			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(initialRegions, code, 2, 15);//outer
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(initialRegions, code, 5, 12);//middle
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(initialRegions, code, 7, 11);//inner
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(initialRegions, code, 8, 10);//someMethod

			// get the mutable Positions which are in the same order as the extracted regions (copies)
			List<Position> positions= getFoldingPositionsFromModel(model);

			assertEquals(initialRegions.size(), positions.size());

			String additionalText= "more ";
			editor.getViewer().getDocument().replace(code.indexOf("content"), 0, additionalText);
			cu.reconcile(ICompilationUnit.NO_AST, false, null, null);

			// check that regions are in the same order as before and not modified in another way
			for(int i= 0; i < positions.size(); i++) {
				assertEquals(initialRegions.get(i).getOffset(), positions.get(i).getOffset());
				assertEquals(initialRegions.get(i).getLength() + additionalText.length(), positions.get(i).getLength());
			}
		} finally {
			editor.close(false);
		}
	}

	@Test
	public void testCustomFoldingRegionsAreAssociatedWithCorrectIJavaElements() throws Exception {
		String code= """
				package org.example.test;

				// region outside of class

				class Test {
					// region within class

					// region nested within class

					int i;

					// region 3rd nested within class

					void someMethod() {
						// region within method

						// endregion within method
					}
					// endregion 3rd nested within class
					// endregion nested within class

					void otherMethod() {}

					// endregion within class
				}

				// endregion outside of class

				// region at the EOF
				// endregion
				""";
		ICompilationUnit cu= fPackageFragment.createCompilationUnit("Test.java", code, true, null);
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(cu);
		try {
			ProjectionAnnotationModel model= editor.getAdapter(ProjectionAnnotationModel.class);

			assertContainsCustomFoldingPositionWithElement(code, model, "// region outside of class", "Test.java", IJavaElement.COMPILATION_UNIT);
			assertContainsCustomFoldingPositionWithElement(code, model, "// region within class", "Test", IJavaElement.TYPE);
			assertContainsCustomFoldingPositionWithElement(code, model, "// region nested within class", "Test", IJavaElement.TYPE);
			assertContainsCustomFoldingPositionWithElement(code, model, "// region 3rd nested within class", "Test", IJavaElement.TYPE);
			assertContainsCustomFoldingPositionWithElement(code, model, "// region within method", "someMethod", IJavaElement.METHOD);
			assertContainsCustomFoldingPositionWithElement(code, model, "// region at the EOF", "Test.java", IJavaElement.COMPILATION_UNIT);
		} finally {
			editor.close(false);
		}
	}

	private void assertContainsCustomFoldingPositionWithElement(String code, ProjectionAnnotationModel model, String startComment, String elementName, int elementType) throws Exception {
		Iterator<Annotation> it= model.getAnnotationIterator();
		while (it.hasNext()) {
			Annotation annotation= it.next();
			Position position= model.getPosition(annotation);
			String regionPart= code.substring(position.getOffset(), position.getOffset() + position.getLength());
			if (regionPart.trim().startsWith(startComment)) {
				assertEquals("org.eclipse.jdt.ui.text.folding.DefaultJavaFoldingStructureProvider$JavaProjectionAnnotation", annotation.getClass().getName());
				Method getter= annotation.getClass().getDeclaredMethod("getElement");
				getter.setAccessible(true);
				IJavaElement element= (IJavaElement) getter.invoke(annotation);
				assertEquals(elementName, element.getElementName(), "incorrect IJavaElement associated with the following folding region:" + regionPart);
				assertEquals(elementType, element.getElementType(), "incorrect IJavaElement type associated with the following folding region:" + regionPart);
				return;
			}
		}
		fail("No folding region starting with the following comment was found: " + startComment);
	}

	private List<Position> getFoldingPositionsFromModel(ProjectionAnnotationModel model) {
		List<Position> positions= new ArrayList<>();
		Iterator<Annotation> it= model.getAnnotationIterator();
		while (it.hasNext()) {
			Annotation a= it.next();
			if (a instanceof ProjectionAnnotation) {
				Position p= model.getPosition(a);
				positions.add(p);
			}
		}
		return positions;
	}

	private List<IRegion> getProjectionRangesOfFile(String str) throws Exception {
		return FoldingTestUtils.getProjectionRangesOfPackage(fPackageFragment, str);
	}

}

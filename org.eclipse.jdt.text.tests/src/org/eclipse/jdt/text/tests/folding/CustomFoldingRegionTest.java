/*******************************************************************************
 * Copyright (c) 2025 Daniel Schmid and others.
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

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;

@RunWith(Parameterized.class)
public class CustomFoldingRegionTest {
	@Rule
	public ProjectTestSetup projectSetup= new ProjectTestSetup();

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	private IPackageFragment fPackageFragment;

	@Parameter
	public boolean extendedFoldingActive;

	@Parameters(name = "Experimental folding active: {0}")
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
				public class Test {
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(0, projectionRanges.size());
	}

	@Test
	public void testCustomFoldingRegionInsideAndOutsideClass() throws Exception {
		String str= """
				package org.example.test;
				// region
				// something else
				// endregion
				public class Test {
					// region
					// something else
					// endregion
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(2, projectionRanges.size());
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 1, 3);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 5, 7);
	}

	@Test
	public void testNestedCustomRegions() throws Exception {
		String str= """
				package org.example.test;

				public class Test {
					// region outer
					// region inner

					// endregion outer
					// endregion inner
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(2, projectionRanges.size());
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 7);//outer
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 6);//inner
	}

	@Test
	public void testCustomRegionsDisabled() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGIONS_ENABLED, false);

		String str= """
				package org.example.test;

				public class Test {
					// region outer

					// endregion inner
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(0, projectionRanges.size());
	}

	@Test
	public void testNoCustomFoldingRegionsInMethod() throws Exception {
		String str= """
				package org.example.test;
				public class Test {
					void a(){

					}
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(1, projectionRanges.size());
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 2, 3);
	}

	@Test
	public void testCustomFoldingRegionsInMethod() throws Exception {
		String str= """
				package org.example.test;
				public class Test {
					void a(){
						// region

						// endregion
					}
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(2, projectionRanges.size());
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 2, 5);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 5);
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
		assertEquals(1, projectionRanges.size());
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 2, 4);
	}

	@Test
	public void testCustomFoldingRegionAroundClasses() throws Exception {
		String str= """
				package org.example.test;

				class A {

				}

				// region

				class B {

				}

				class C {

				}
				// endregion

				class D {

				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(1, projectionRanges.size());
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 6, 15);
	}

	@Test
	public void testCustomFoldingRegionsMultipleLevels() throws Exception {
		String str= """
				package org.example.test;
				// region outside class
				public class Test {
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
		assertEquals(4, projectionRanges.size());
		if (extendedFoldingActive) {
			// folding with control structures does not consider top level classes
			// hence the first region ends with the endregion comment inside the Test class
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 1, 3);
		}else {
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 1, 13);//outside class
		}
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 11);//outside method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 7, 9);//inside method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 5, 9);//void a()
	}

	@Test
	public void testCustomFoldingRegionsNotEndingTooEarly() throws Exception {
		String str= """
				package org.example.test;

				public class Test {
					void a(){
						// region inside method
					}
					// endregion outside method
				}
				// endregion outside class
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(1, projectionRanges.size());
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 4);//void a()
	}

	@Test
	public void testCustomFoldingRegionsUsingSpecialCommentTypes() throws Exception {
		String str= """
				package org.example.test;

				public class Test {
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
		if(extendedFoldingActive) {
			assertEquals(5, projectionRanges.size());
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 9);//void a()
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 5);// multiline (comment)
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 9);// multiline (folding region)
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 6, 7);// javadoc
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 8, 9);// multiline (last comment)
		} else {
			assertEquals(3, projectionRanges.size());
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 9);//void a()
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 9);// multiline
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 6, 7);// javadoc
		}
	}

	@Test
	public void testCustomRegionsWithLocalClass() throws Exception {
		String str= """
				package org.example.test;

				public class Test {
					void a(){
						// region
						int i;

						// endregion
						class Inner{

						}
					}
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(3, projectionRanges.size());
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 10);//void a()
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 7);//region
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 8, 9);//class Inner
	}

	@Test
	public void testNoCustomRegionAtDifferentLevelsWithOtherClass() throws Exception {
		String str= """
				package org.example.test;

				public class Test{
					// region outside
					public class A {
						public void helloWorld() {

						}
						// endregion inside
					}

					public class B {


				    }

				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(3, projectionRanges.size());
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 8);//class A
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 5, 6);//void helloWorld()
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 11, 13);//class B
	}

	@Test
	public void testCustomRegionsAroundFieldAndMethod() throws Exception {
		String str= """
				package org.example.test;

				public class Test {
					// region
					int a;

					void b(){

					}
					// endregion
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(2, projectionRanges.size());
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 9);//region
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 6, 7);//void b()
	}

	@Test
	public void testDifferentConfiguration() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_START, "#regstart");
		store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_END, "#regend");


		String str= """
				package org.example.test;
				public class Test {
					// region should be ignored
					// #regstart this is the region
					// #regend should end here
					// endregion should be ignored
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(1, projectionRanges.size());
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 4);
	}

	@Test
	public void testCommentsInEmptyBlocks() throws Exception {
		String str= """
				package org.example.test;
				public class Test {
					void a(){
						{/* region 1*/}
						System.out.println("Hello World");
						{/* endregion 1*/}
					}
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(2, projectionRanges.size());
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 2, 5);//void a()
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 5);//region 1
	}

	@Test
	public void testNotFoldedWithinDifferentControlFlowStatements() throws Exception {
		assumeTrue("Only enabled with extended folding", extendedFoldingActive);
		String str= """
				package org.example.test;
				public class Test {
					void a() {
						// region
						for (int i = 0; i < 10; i++) {
							// endregion
							// region
						}
						boolean b=false;
						// region
						while (b) {
							// endregion
						}
					}
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(3, projectionRanges.size());
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 2, 12);//void a()
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 6);//void a()
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 10, 11);//void a()
	}

	private List<IRegion> getProjectionRangesOfFile(String str) throws Exception {
		return FoldingTestUtils.getProjectionRangesOfFile(fPackageFragment, "Test.java", str);
	}

}

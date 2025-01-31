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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.junit.After;
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

public class CustomFoldingRegionNewFoldingTest {
	@Rule
	public ProjectTestSetup projectSetup= new ProjectTestSetup();

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	private IPackageFragment fPackageFragment;

	@Before
	public void setUp() throws CoreException {
		fJProject1= projectSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		fPackageFragment= fSourceFolder.createPackageFragment("org.example.test", false, null);
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_NEW_FOLDING_ENABLED, true);
	}


	@After
	public void tearDown() throws CoreException {
		JavaProjectHelper.delete(fJProject1);
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setToDefault(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_START);
		store.setToDefault(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_END);
		store.setToDefault(PreferenceConstants.EDITOR_NEW_FOLDING_ENABLED);
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
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 1, 2);
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 5, 6);
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
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 6);//outer
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 5);//inner
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
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 2, 3);
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
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 2, 5);
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 4);
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
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 2, 3);
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
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 6, 14);
	}

	@Test
	public void testCustomFoldingRegionsMultipleLevels() throws Exception {
		String str= """
				package org.example.test;
				// region outside class
				public class Test {
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
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 1, 11);//outside class
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 9);//outside method
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 8);//void a()
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 6, 7);//inside method
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
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 4);//void a()
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
		assertEquals(5, projectionRanges.size());
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 9);//void a()
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 5);// multiline (comment)
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 7);// multiline (folding region)
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 6, 6);// javadoc
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 8, 9);// multiline (last comment)
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
		assertEquals(2, projectionRanges.size());
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 10);//void a()
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 6);//region
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
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 8);//class A
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 5, 6);//void helloWorld()
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 11, 13);//class B
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
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 8);//region
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 6, 7);//void b()
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
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 3);
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
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 2, 5);//void a()
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 4);//region 1
	}

	private void assertContainsRegionUsingStartAndEndLine(List<IRegion> projectionRanges, String input, int startLine, int endLine) {
		assertTrue(startLine <= endLine, "start line must be smaller or equal to end line");
		int startLineBegin= findLineStartIndex(input, startLine);

		int endLineBegin= findLineStartIndex(input, endLine);
		int endLineEnd= findNextLineStart(input, endLineBegin);
		endLineEnd= getLengthIfNotFound(input, endLineEnd);

		for (IRegion region : projectionRanges) {
			if (region.getOffset() == startLineBegin + 1 && region.getOffset() + region.getLength() == endLineEnd + 1) {
				return;
			}
		}

		fail(
				"missing region from line " + startLine + " (index " + (startLineBegin + 1) + ") " +
						"to line " + endLine + " (index " + (endLineEnd + 1) + ")" +
						", actual regions: " + projectionRanges
		);
	}


	private int getLengthIfNotFound(String input, int startLineEnd) {
		if (startLineEnd == -1) {
			startLineEnd= input.length();
		}
		return startLineEnd;
	}


	private int findLineStartIndex(String input, int lineNumber) {
		int currentInputIndex= 0;
		for (int i= 0; i < lineNumber; i++) {
			currentInputIndex= findNextLineStart(input, currentInputIndex);
			if (currentInputIndex == -1) {
				fail("line number is greater than the total number of lines");
			}
		}
		return currentInputIndex;
	}


	private int findNextLineStart(String input, int currentInputIndex) {
		return input.indexOf('\n', currentInputIndex + 1);
	}

	private List<IRegion> getProjectionRangesOfFile(String str) throws Exception {
		return FoldingTestUtils.getProjectionRangesOfFile(fPackageFragment, "Test.java", str);
	}

}

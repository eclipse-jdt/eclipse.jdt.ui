package org.eclipse.jdt.ui.tests.folding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;

import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

public class CustomFoldingRegionTest {
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
	}


	@After
	public void tearDown() throws CoreException {
		JavaProjectHelper.delete(fJProject1);
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setToDefault(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_START);
		store.setToDefault(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_END);
	}

	@Test
	public void testNoCustomFoldingRegions() throws JavaModelException, PartInitException {
		String str= """
				package org.example.test;
				public class Test {
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(0, projectionRanges.size());
	}

	@Test
	public void testCustomFoldingRegionInsideAndOutsideClass() throws JavaModelException, PartInitException {
		String str= """
				package org.example.test;
				// #region
				// something else
				// #endregion
				public class Test {
					// #region
					// something else
					// #endregion
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(2, projectionRanges.size());
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 1, 3);
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 5, 7);
	}

	@Test
	public void testNestedCustomRegions() throws JavaModelException, PartInitException {
		String str= """
				package org.example.test;

				public class Test {
					// #region outer
					// #region inner

					// #endregion outer
					// #endregion inner
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(2, projectionRanges.size());
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 7);//outer
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 6);//inner
	}

	@Test
	public void testNoCustomFoldingRegionsInMethod() throws JavaModelException, PartInitException {
		String str= """
				package org.example.test;
				public class Test {
					void a(){

					}
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(1, projectionRanges.size());
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 2, 4);
	}

	@Test
	public void testCustomFoldingRegionsInMethod() throws JavaModelException, PartInitException {
		String str= """
				package org.example.test;
				public class Test {
					void a(){
						// #region

						// #endregion
					}
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(2, projectionRanges.size());
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 2, 6);
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 5);
	}

	@Test
	public void testNoCustomFoldingRegionsSingleImport() throws JavaModelException, PartInitException {
		String str= """
				package org.example.test;

				import java.util.List;
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(0, projectionRanges.size());
	}

	@Test
	public void testCustomFoldingRegionAroundSingleImport() throws JavaModelException, PartInitException {
		String str= """
				package org.example.test;

				// #region imports
				import java.util.List;
				// #endregion
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(1, projectionRanges.size());
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 2, 4);
	}

	@Test
	public void testCustomFoldingRegionAroundClasses() throws JavaModelException, PartInitException {
		String str= """
				package org.example.test;

				class A {

				}

				// #region

				class B {

				}

				class C {

				}
				// #endregion

				class D {

				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(1, projectionRanges.size());
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 6, 15);
	}

	@Test
	public void testCustomFoldingRegionsMultipleLevels() throws JavaModelException, PartInitException {
		String str= """
				package org.example.test;
				// #region outside class
				public class Test {
					// #endregion should be ignored
					// #region outside method
					void a(){
						// #endregion should be ignored
						// #region inside method
						System.out.println("Hello World");
						// #endregion inside method
					}
					// #endregion outside method
				}
				// #endregion outside class
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(4, projectionRanges.size());
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 1, 13);//outside class
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 11);//outside method
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 5, 10);//void a()
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 7, 9);//inside method
	}

	@Test
	public void testCustomFoldingRegionsNotEndingTooEarly() throws JavaModelException, PartInitException {
		String str= """
				package org.example.test;

				public class Test {
					void a(){
						// #region inside method
					}
					// #endregion outside method
				}
				// #endregion outside class
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(1, projectionRanges.size());
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 5);//void a()
	}

	@Test
	public void testCustomFoldingRegionsUsingSpecialCommentTypes() throws JavaModelException, PartInitException {
		String str= """
				package org.example.test;

				public class Test {
					void a(){
						/* #region multiline
						*/
						/** #region javadoc */
						/** #endregion javadoc */
						/* #endregion multiline
						*/
					}
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(3, projectionRanges.size());
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 10);//void a()
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 8);// multiline
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 6, 7);// javadoc
	}

	@Test
	public void testCustomRegionsWithLocalClass() throws JavaModelException, PartInitException {
		String str= """
				package org.example.test;

				public class Test {
					void a(){
						// #region
						int i;

						// #endregion
						class Inner{

						}
					}
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(3, projectionRanges.size());
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 11);//void a()
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 7);//region
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 8, 10);//class Inner
	}

	@Test
	public void testNoCustomRegionAtDifferentLevelsWithOtherClass() throws JavaModelException, PartInitException {
		String str= """
				package org.example.test;

				public class Test{
					// #region outside
					public class A {
						public void helloWorld() {
						}
						// #endregion inside
					}

					public class B {
				    }

				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(3, projectionRanges.size());
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 4, 8);//class A
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 5, 6);//void helloWorld()
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 10, 11);//class B
	}

	@Test
	public void testCustomRegionsAroundFieldAndMethod() throws JavaModelException, PartInitException {
		String str= """
				package org.example.test;

				public class Test {
					// #region
					int a;

					void b(){

					}
					// #endregion
				}
				""";
		List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
		assertEquals(2, projectionRanges.size());
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 9);//region
		assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 6, 8);//void b()
	}

	@Test
	public void testDifferentConfiguration() throws PartInitException, JavaModelException {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		try {
			store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_START, "#regstart");
			store.setValue(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_END, "#regend");


			String str= """
					package org.example.test;
					public class Test {
						// #region should be ignored
						// #regstart this is the region
						// #regend should end here
						// #endregion should be ignored
					}
					""";
			List<IRegion> projectionRanges= getProjectionRangesOfFile(str);
			assertEquals(1, projectionRanges.size());
			assertContainsRegionUsingStartAndEndLine(projectionRanges, str, 3, 4);
		} finally {

		}
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
				"missing region from line " + startLine + "(index " + (startLineBegin + 1) + ") " +
						"to line " + endLine + "(index " + (endLineEnd + 1) + ")" +
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

	private List<IRegion> getProjectionRangesOfFile(String str) throws JavaModelException, PartInitException {

		ICompilationUnit compilationUnit= fPackageFragment.createCompilationUnit("Test.java", str, false, null);
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(compilationUnit);
		ProjectionAnnotationModel model= editor.getAdapter(ProjectionAnnotationModel.class);
		List<IRegion> regions= new ArrayList<>();
		for (Iterator<Annotation> it= model.getAnnotationIterator(); it.hasNext();) {
			Annotation annotation= it.next();
			if (annotation instanceof ProjectionAnnotation projectionAnnotation) {
				Position position= model.getPosition(projectionAnnotation);
				regions.add(new Region(position.getOffset(), position.getLength()));
			}
		}
		return regions;
	}

}

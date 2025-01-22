/*******************************************************************************
 * Copyright (c) 2025 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.folding;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

public class FoldingTest {
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
	}

	@After
	public void tearDown() throws CoreException {
		JavaProjectHelper.delete(jProject);
	}

	@Test
	public void testCompilationUnitFolding() throws Exception {
		String str= """
				package org.example.test;
				public class A {		//here should not be an annotation
				}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 0);
	}

	@Test
	public void testClassWithJavadocAsHeaderComment() throws Exception {
		String str= """
				package org.example.test;
				/**									//here should be an annotation
				 * Javadoc
				 */
				public class HeaderCommentTest {
				}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 1);

		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 1, 3); // Javadoc
	}

	@Test
	public void testImportsFolding() throws Exception {
		String str= """
				package org.example.test;

				import java.util.List;				//here should be an annotation
				import java.util.ArrayList;

				public class ImportsTest {
				}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 1);

		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 3); // Imports
	}

	@Test
	public void testSingleMethodWithJavadoc() throws Exception {
		String str= """
				package org.example.test;
				public class SingleMethodTest {
				    /**									//here should be an annotation
				     * Javadoc
				     */
				    public void foo() {					//here should be an annotation
				        System.out.println("Hello");
				    }
				}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 2);

		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 4); // Javadoc
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 5, 7); // foo Methode
	}

	@Test
	public void testMultipleMethodsWithoutComments() throws Exception {
		String str= """
				package org.example.test;
				public class MultipleMethodTest {
				    public void foo() {					//here should be an annotation

				    }
				    public void bar() {					//here should be an annotation

				    }
				}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 2);

		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 4); // foo Methode
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 5, 7); // bar Methode
	}

	@Test
	public void testInnerClassFolding() throws Exception {
		String str= """
				package org.example.test;
				public class OuterClass {
				    class InnerClass {				//here should be an annotation
				        void bar() {				//here should be an annotation

				        }
				    }
				}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 2);

		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 6); // InnerClass
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 5); // bar Methode
	}

	@Test
	public void testInnerClassWithJavadoc() throws Exception {
		String str= """
				package org.example.test;
				public class OuterWithDocs {
				    /**										//here should be an annotation
				     * Javadoc
				     */
				    class InnerWithDocs {					//here should be an annotation
				        /**									//here should be an annotation
				         * Javadoc
				         */
				        void bar() {						//here should be an annotation

				        }
				    }
				}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 4);

		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 4); // OuterWithDocs Javadoc
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 5, 12); // InnerWithDocs Klasse
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 6, 8); // InnerWithDocs Javadoc
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 9, 11); // bar Methode
	}

	@Test
	public void testJavadocs() throws Exception {
		String str= """
				package org.example.test;
				   /**										//here should be an annotation
				    * Javadoc
				    */
				    /**										//here should be an annotation
				    * Another Javadoc
				    */
				    /**										//here should be an annotation
				    * Yet another Javadoc
				    */
				    public class Example {}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 3);

		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 1, 3); // 1. Javadoc
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 4, 6); // 2. Javadoc
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 7, 9); // 3. Javadoc
	}

	@Test
	public void testCommentBlocks() throws Exception {
		String str= """
				package org.example.test;
				   /* 						//here should be an annotation
				     *
				     */
				/* 							//here should be an annotation
				     *
				     */
				/* 							//here should be an annotation
				     *
				     */
				class h {

				    /* 						//here should be an annotation
				     *
				     */
				    void b() { 				//here should be an annotation
				        /* 					//here should NOT be an annotation
				         *
				         */
				        int a;
				    }
				}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 5);

		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 1, 3); // 1. Javadoc
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 4, 6); // 2. Javadoc
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 7, 9); // 3. Javadoc
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 12, 14); // 4. Javadoc
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 15, 20); // Methode b()
	}

	@Test
	public void testCopyrightHeader() throws Exception {
		String str= """
				/**							//here should be an annotation
				* This is some copyright header
				*/
				package org.example.test;

				class SomeClass {}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 1);
	}
}

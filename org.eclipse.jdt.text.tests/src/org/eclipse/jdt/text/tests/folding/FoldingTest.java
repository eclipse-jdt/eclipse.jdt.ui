/*******************************************************************************
 * Copyright (c) 2025, 2026 Vector Informatik GmbH and others.
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

import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

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
public class FoldingTest {
	@Rule
	public ProjectTestSetup projectSetup= new ProjectTestSetup();

	private IJavaProject jProject;

	private IPackageFragmentRoot sourceFolder;

	private IPackageFragment packageFragment;

	@Parameters(name = "Extended folding active: {0}")
	public static Object[] data() {
		return new Object[] { true, false };
	}

	@Parameter
	public boolean newFoldingActive;

	@Before
	public void setUp() throws CoreException {
		jProject= projectSetup.getProject();
		sourceFolder= jProject.findPackageFragmentRoot(jProject.getResource().getFullPath().append("src"));
		if (sourceFolder == null) {
			sourceFolder= JavaProjectHelper.addSourceContainer(jProject, "src");
		}
		packageFragment= sourceFolder.createPackageFragment("org.example.test", false, null);
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_NEW_FOLDING_ENABLED, newFoldingActive);
	}

	@After
	public void tearDown() throws CoreException {
		JavaProjectHelper.delete(jProject);
		JavaPlugin.getDefault().getPreferenceStore().setToDefault(PreferenceConstants.EDITOR_NEW_FOLDING_ENABLED);
	}

	@Test
	public void testCompilationUnitFolding() throws Exception {
		String str= """
				package org.example.test;
				class A {		//here should be an annotation
				}
				""";
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 1, 2); // class
	}

	@Test
	public void testDoNotFoldOneLinerEmptyClass() throws Exception {
		String str= """
				package org.example.test;
				class A {		//here should not be an annotation
				} // This comment prevents this line to be folded
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, str, 0); //
	}

	@Test
	public void testDoNotFoldInSameLine() throws Exception {
		String str= """
				package org.example.test;
				class A { }
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, str, 0); //
	}

	@Test
	public void testFoldOneLinersEmptyClass() throws Exception {
		String str= """
				package org.example.test;
				class A {		//here should be an annotation
				}
				""";
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 1, 2); // class
	}

	@Test
	public void testClassWithJavadocAsHeaderComment() throws Exception {
		String str= """
				package org.example.test;
				/**									//here should be an annotation
				 * Javadoc
				 */
				class HeaderCommentTest {
				}
				""";

		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 1, 3); // Javadoc
	}

	@Test
	public void testImportsFolding() throws Exception {
		String str= """
				package org.example.test;

				import java.util.List;				//here should be an annotation
				import java.util.ArrayList;

				class ImportsTest {
				}
				""";

		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 3); // Imports
	}

	@Test
	public void testSingleMethodWithJavadoc() throws Exception {
		String str= """
				package org.example.test;
				class SingleMethodTest {
				    /**									//here should be an annotation
				     * Javadoc
				     */
				    public void foo() {					//here should be an annotation
				        System.out.println("Hello");
				    }
				}
				""";

		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 4); // Javadoc
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 5, 7); // foo Methode
	}

	@Test
	public void testSingleMethodWithAnnotation() throws Exception {
		String str= """
				package org.example.test;
				class DeprecatedMethodTest {
				    @Deprecated						//here should be an annotation
				    public void foo() {				//here should not be an annotation
				        System.out.println("Hello");
				    }
				}
				""";

		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 5); // method
		FoldingTestUtils.assertDoesNotContainRegionUsingStartLine(regions, str, 3);
	}

	@Test
	public void testMultipleMethodsWithoutComments() throws Exception {
		String str= """
				package org.example.test;
				class MultipleMethodTest {
				    public void foo() {					//here should be an annotation

				    }
				    public void bar() {					//here should be an annotation

				    }
				}
				""";

		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 4); // foo Methode
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 5, 7); // bar Methode
	}

	@Test
	public void testInnerClassFolding() throws Exception {
		String str= """
				package org.example.test;
				class OuterClass {
				    class InnerClass {				//here should be an annotation
				        void bar() {				//here should be an annotation

				        }
				    }
				}
				""";

		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 6); // InnerClass
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 5); // bar Methode
	}

	@Test
	public void testInnerClassWithJavadoc() throws Exception {
		String str= """
				package org.example.test;
				class OuterWithDocs {
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

		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
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
				    class Example {}
				""";

		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
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
				        /* 					//here should be an annotation
				         *
				         */
				        int a;
				    }
				}
				""";

		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 1, 3); // 1. Javadoc
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 4, 6); // 2. Javadoc
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 7, 9); // 3. Javadoc
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 12, 14); // 4. Javadoc
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 15, 20); // Methode b()
		if (newFoldingActive) {
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 16, 18); // 5. Javadoc
		}
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
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 0, 2);
	}

	@Test
	public void testMethodDeclarationFoldingWithSameLineStart() throws Exception {
		String str= """
				package org.example.test;
				class X {
					/*				//here should be an annotation
					 * a b
					 */
					void a() {		//here should be an annotation

					} void b() {	//here should be an annotation

					}
				}
				""";
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 4); // JavaDoc
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 5, 6); // 1. Method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 7, 9); // 2. Method
	}

	@Test
	public void testIfStatementFolding() throws Exception {
		assumeTrue("Only doable with the new folding", newFoldingActive);
		String str= """
				package org.example.test;
				class D {
				    void x() {			//here should be an annotation
				        if (true) {		//here should be an annotation

				        }
				    }
				}
				""";
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 6); // 1. Method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 5); // if
	}

	@Test
	public void testTryStatementFolding() throws Exception {
		assumeTrue("Only doable with the new folding", newFoldingActive);
		String str= """
				package org.example.test;
				class E {
				    void x() {						//here should be an annotation
				        try {						//here should be an annotation

				        } catch (Exception e) {		//here should be an annotation

				        }
				    }
				}
				""";
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 8); // 1. Method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 4); // try
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 5, 7); // catch
	}

	@Test
	public void testWhileStatementFolding() throws Exception {
		assumeTrue("Only doable with the new folding", newFoldingActive);
		String str= """
				package org.example.test;
				class F {
				    void x() {				//here should be an annotation
				        while (true) {		//here should be an annotation

				        }
				    }
				}
				""";
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 6); // 1. Method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 5); // while
	}

	@Test
	public void testForStatementFolding() throws Exception {
		assumeTrue("Only doable with the new folding", newFoldingActive);
		String str= """
				package org.example.test;
				class G {
				    void x() {					//here should be an annotation
				        for(int i=0;i<1;i++){	//here should be an annotation

				        }
				    }
				}
				""";
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 6); // 1. Method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 5); // for
	}

	@Test
	public void testEnhancedForStatementFolding() throws Exception {
		assumeTrue("Only doable with the new folding", newFoldingActive);
		String str= """
				package org.example.test;
				class H {
				    void x() {							//here should be an annotation
				        for(String s: new String[0]){	//here should be an annotation

				        }
				    }
				}
				""";
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 6); // 1. Method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 5); // for
	}

	@Test
	public void testDoStatementFolding() throws Exception {
		assumeTrue("Only doable with the new folding", newFoldingActive);
		String str= """
				package org.example.test;
				class I {
				    void x() {				//here should be an annotation
				        do {				//here should be an annotation

				        } while(false);
				    }
				}
				""";
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 6); // 1. Method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 4); // do
	}

	@Test
	public void testSynchronizedStatementFolding() throws Exception {
		assumeTrue("Only doable with the new folding", newFoldingActive);
		String str= """
				package org.example.test;
				class K {
				    void x() {					//here should be an annotation
				        synchronized(this) {	//here should be an annotation

				        }
				    }
				}
				""";
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 6); // 1. Method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 5); // synchronized
	}

	@Test
	public void testLambdaExpressionFolding() throws Exception {
		assumeTrue("Only doable with the new folding", newFoldingActive);
		String str= """
				package org.example.test;
				import java.util.function.Supplier;
				class L {
				    void x() {							//here should be an annotation
				        Supplier<String> s = () -> {	//here should be an annotation
				            return "";
				        };
				    }
				}
				""";
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 7); // 1. Method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 4, 6); // Supplier
	}

	@Test
	public void testAnonymousClassDeclarationFolding() throws Exception {
		String str= """
				package org.example.test;
				class M {
				    Object o = new Object(){		//here should be an annotation
				        void y() {					//here should be an annotation

				        }
				    };
				}
				""";
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 6); // Object
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 5); // Method
	}

	/**
	 * See <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=130472#c16">Bug 130472 -
	 * [projection] Bizarre folding behavior</a>
	 */
	@Test
	public void testAnonymousClassAsParameterFolding() throws Exception {
		String str= """
				package org.example.test;
				class Snippet15 {
					void method() {
						add(new Runnable() {
							public void run() {

							}
						});
					}
				}
							""";
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 8); // method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 7); // Runnable (anonymous class)
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 4, 6); // run
	}

	/**
	 * See <a href="https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/2596">GitHub Issue #2596</a>
	 */
	@Test
	public void testEnumDeclarationFolding() throws Exception {
		String str= """
				package org.example.test;
				enum N {					//here should be an annotation
				    A,
				    B
				}
				""";
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 1, 4); // enum
	}

	@Test
	public void testInitializerFolding() throws Exception {
		String str= """
				package org.example.test;
				class O {
				    static {					//here should be an annotation

				    }
				}
				""";
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 4); // static
	}

	@Test
	public void testNestedFolding() throws Exception {
		assumeTrue("Only doable with the new folding", newFoldingActive);
		String str= """
				package org.example.test;
				class P {
				    void x() {							//here should be an annotation
				        if (true) {						//here should be an annotation
				            for(int i=0;i<1;i++){		//here should be an annotation
				                while(true) {			//here should be an annotation
				                    do {				//here should be an annotation

				                    } while(false);
				                }
				            }
				        }
				    }
				}
				""";
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 12); // method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 11); // if
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 4, 10); // for
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 5, 9); // while
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 6, 7); // do
	}

	@Test
	public void testLocalClass() throws Exception {
		String str= """
				package org.example.test;
				class Outer {
					void a() {						//here should be an annotation
						class Inner2{				//here should be an annotation

						}
					}
				}
				""";
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 6); // method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 5); // inner class
	}


	@Test
	public void testSingleLiner() throws Exception {
		assumeTrue("Only doable with the new folding", newFoldingActive);
		String str= """
				package org.example.test;
				class I {
				    void x() {				//here should be an annotation
				        do {				//here should not be an annotation
				        } while(false);
						for(int i=0;i<1;i++){	//here should be an annotation
				        }
				        synchronized(this) {	//here should be an annotation
				        }
				        if (true) {		//here should be an annotation
				        }
				        try {						//here should not be an annotation
				        } catch (Exception e) {		//here should be an annotation
				        }
				        int zaehler = 0;
						switch (zaehler) {			//here should be an annotation
				        case 0:						//here should not be an annotation
				            break;
				        default:					//here should not be an annotation
				            break;
				    	}
				    }
				    public void bar() {					//here should be an annotation
				    }
				}
				""";
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 21); // x()
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 22, 23); // bar()
		FoldingTestUtils.assertDoesNotContainRegionUsingStartLine(regions, str, 3); // do-while
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 5, 6); // for
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 7,8); // synchronized
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 9, 10); // if
		FoldingTestUtils.assertDoesNotContainRegionUsingStartLine(regions, str, 11); // try
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 12, 13); // catch
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 15, 20); // switch
		FoldingTestUtils.assertDoesNotContainRegionUsingStartLine(regions, str, 17); // case 0
		FoldingTestUtils.assertDoesNotContainRegionUsingStartLine(regions, str, 19); // default
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 22, 23); // bar
	}

	@Test
	public void testSwitchExpression() throws Exception {
		assumeTrue("Only doable with the new folding", newFoldingActive);
		String str= """
				package org.example.test;
				class Outer {
					void a() {
						int b = 0;
						int c = switch (b) {		//here should be an annotation
							case 1 ->

							1;
							case 2 ->
							1;
							case 3 ->

							break;
							1;
							case 4 -> {
								b = 2;
								yield 3;
							}
							case 5 -> {
								yield 3;
							}
							default ->

							0;
						};
					}
				}
								""";
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 4, 24); // switch expression
	}

	@Test
	public void testSwitchStatment() throws Exception {
		assumeTrue("Only doable with the new folding", newFoldingActive);
		String str= """
				package org.example.test;
				class Outer {
					void a() {
						int b = 0;
						switch (b) {				//here should be an annotation
					        case 0:					//here should be an annotation

					            break;
					            b = 1;
					        case 1:
					            break;

					        default:

					          	break;
						}
					}
				}
				""";
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 4, 15); // switch
	}

	@Test
	public void testInnerClassWithAnnotations() throws Exception {
		assumeFalse("Only doable with the old folding", newFoldingActive);
		String str= """
				package org.example.test;
				class Outer4 {
					@Deprecated // here should be an annotation
					@SuppressWarnings("")

					class Inner {}

				}
				""";
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 5); // @Deprecated
	}

	/**
	 * See <a href="https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/2571">GitHub Issue #2571</a>
	 */
	@Test
	public void testArrayInitializers() throws Exception {
		String str= """
				package org.example.test;
				class RecordTest {
					private int[] arr = { // here should be an annotation
							1,
							2,
							3
					};
				}

								""";
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 6); // array
	}

	@Test
	public void testAdvancedFoldingAfterChildren() throws Exception {
		assumeTrue("Only doable with the extended folding", newFoldingActive);
		String str = """
				public class Tst {
					public static void main(String[] args) {
						Predicate<Object> a = o -> {
							return true;
						};
						List<String> b = new ArrayList<>() {

						};
						boolean c = true;
						if (c) {
							System.out.println("bbb");
						} else {
							System.out.println("aaa");
						}
					}

				}
				""";
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 4); // predicate
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 5, 7); // ArrayList
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 9, 10); // if
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 11, 13); // else
	}
}

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

	@Parameters(name = "New folding active: {0}")
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
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 5, 6); // foo Methode
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
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 3); // foo Methode
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 5, 6); // bar Methode
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
		if (newFoldingActive) {
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 5); // InnerClass
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 4); // bar Methode
		} else {
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 6); // InnerClass
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 4); // bar Methode
		}
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
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 5, 11); // InnerWithDocs Klasse
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 6, 8); // InnerWithDocs Javadoc
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 9, 10); // bar Methode
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
				        /* 					//here should be an annotation
				         *
				         */
				        int a;
				    }
				}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, newFoldingActive ? 6 : 5);

		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 1, 3); // 1. Javadoc
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 4, 6); // 2. Javadoc
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 7, 9); // 3. Javadoc
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 12, 14); // 4. Javadoc
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 15, 19); // Methode b()
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
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 1);
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
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 3);
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 4); // JavaDoc
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 5, 6); // 1. Method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 7, 8); // 2. Method
	}

	@Test
	public void testIfStatementFolding() throws Exception {
		assumeTrue("Only doable with the new folding", newFoldingActive);
		String str= """
				package org.example.test;
				public class D {
				    void x() {			//here should be an annotation
				        if (true) {		//here should be an annotation

				        }
				    }
				}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 2);
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 5); // 1. Method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 4); // if
	}

	@Test
	public void testTryStatementFolding() throws Exception {
		assumeTrue("Only doable with the new folding", newFoldingActive);
		String str= """
				package org.example.test;
				public class E {
				    void x() {						//here should be an annotation
				        try {						//here should be an annotation

				        } catch (Exception e) {		//here should be an annotation

				        }
				    }
				}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 3);
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 7); // 1. Method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 4); // try
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 5, 6); // catch
	}

	@Test
	public void testWhileStatementFolding() throws Exception {
		assumeTrue("Only doable with the new folding", newFoldingActive);
		String str= """
				package org.example.test;
				public class F {
				    void x() {				//here should be an annotation
				        while (true) {		//here should be an annotation

				        }
				    }
				}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 2);
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 5); // 1. Method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 4); // while
	}

	@Test
	public void testForStatementFolding() throws Exception {
		assumeTrue("Only doable with the new folding", newFoldingActive);
		String str= """
				package org.example.test;
				public class G {
				    void x() {					//here should be an annotation
				        for(int i=0;i<1;i++){	//here should be an annotation

				        }
				    }
				}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 2);
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 5); // 1. Method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 4); // for
	}

	@Test
	public void testEnhancedForStatementFolding() throws Exception {
		assumeTrue("Only doable with the new folding", newFoldingActive);
		String str= """
				package org.example.test;
				public class H {
				    void x() {							//here should be an annotation
				        for(String s: new String[0]){	//here should be an annotation

				        }
				    }
				}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 2);
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 5); // 1. Method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 4); // for
	}

	@Test
	public void testDoStatementFolding() throws Exception {
		assumeTrue("Only doable with the new folding", newFoldingActive);
		String str= """
				package org.example.test;
				public class I {
				    void x() {				//here should be an annotation
				        do {				//here should be an annotation

				        } while(false);
				    }
				}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 2);
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 5); // 1. Method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 4); // do
	}

	@Test
	public void testSynchronizedStatementFolding() throws Exception {
		assumeTrue("Only doable with the new folding", newFoldingActive);
		String str= """
				package org.example.test;
				public class K {
				    void x() {					//here should be an annotation
				        synchronized(this) {	//here should be an annotation

				        }
				    }
				}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 2);
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 5); // 1. Method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 4); // synchronized
	}

	@Test
	public void testLambdaExpressionFolding() throws Exception {
		assumeTrue("Only doable with the new folding", newFoldingActive);
		String str= """
				package org.example.test;
				import java.util.function.Supplier;
				public class L {
				    void x() {							//here should be an annotation
				        Supplier<String> s = () -> {	//here should be an annotation
				            return "";
				        };
				    }
				}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 2);
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 6); // 1. Method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 4, 5); // Supplier
	}

	@Test
	public void testAnonymousClassDeclarationFolding() throws Exception {
		String str= """
				package org.example.test;
				public class M {
				    Object o = new Object(){		//here should be an annotation
				        void y() {					//here should be an annotation

				        }
				    };
				}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 2);
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		if (newFoldingActive) {
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 5); // Object
		} else {
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 6); // Object
		}
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 4); // Method
	}

	@Test
	public void testEnumDeclarationFolding() throws Exception {
		assumeTrue("Only doable with the new folding", newFoldingActive);
		String str= """
				package org.example.test;
				public enum N {					//here should be an annotation
				    A,
				    B
				}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 1);
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 1, 3); // enum
	}

	@Test
	public void testInitializerFolding() throws Exception {
		String str= """
				package org.example.test;
				public class O {
				    static {					//here should be an annotation

				    }
				}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 1);
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 3); // static
	}

	@Test
	public void testNestedFolding() throws Exception {
		assumeTrue("Only doable with the new folding", newFoldingActive);
		String str= """
				package org.example.test;
				public class P {
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
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 5);
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 11); // method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 10); // if
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 4, 9); // for
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 5, 8); // while
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
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 2);
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 5); // method
		if (newFoldingActive) {
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 4); // inner class
		} else {
			FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 3, 5); // inner class
		}
	}


	@Test
	public void testSingleLiner() throws Exception {
		assumeTrue("Only doable with the new folding", newFoldingActive);
		String str= """
				package org.example.test;
				public class I {
				    void x() {				//here should be an annotation
				        do {				//here should not be an annotation
				        } while(false);
						for(int i=0;i<1;i++){	//here should not be an annotation
				        }
				        synchronized(this) {	//here should not be an annotation
				        }
				        if (true) {		//here should not be an annotation
				        }
				        try {						//here should not be an annotation
				        } catch (Exception e) {		//here should not be an annotation
				        }
				        int zaehler = 0;
						switch (zaehler) {			//here should be an annotation
				        case 0:						//here should not be an annotation
				            break;
				        default:					//here should not be an annotation
				            break;
				    	}
				    }
				    public void bar() {					//here should not be an annotation
				    }
				}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 2);
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 20); // Method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 15, 19); // switch
	}

	@Test
	public void testSwitchExpression() throws Exception {
		assumeTrue("Only doable with the new folding", newFoldingActive);
		String str= """
				package org.example.test;
				class Outer {
					void a() {						//here should be an annotation
						int b = 0;
						int c = switch (b) {		//here should be an annotation
							case 1 -> 				//here should be an annotation

							1;
							case 2 -> 				//here should not be an annotation
							1;
							case 3 -> 				//here should be an annotation

							break;
							1;
							case 4 -> {				//here should be an annotation
								b = 2;
								yield 3;
							}
							case 5 -> {				//here should not be an annotation
								yield 3;
							}
							default -> 				//here should be an annotation

							0;
						};
					}
				}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 6);
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 24); // method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 4, 23); // switch
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 5, 6); // 1. case
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 10, 11); // 3. case
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 14, 15); // 4. case
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 21, 22); // default
	}

	@Test
	public void testSwitchStatment() throws Exception {
		assumeTrue("Only doable with the new folding", newFoldingActive);
		String str= """
				package org.example.test;
				class Outer {
					void a() {						//here should be an annotation
						int b = 0;
						switch (b) {				//here should be an annotation
					        case 0:					//here should be an annotation

					            break;
					            b = 1;
					        case 1:					//here should not be an annotation
					            break;

					        default:				//here should be an annotation

					          	break;
						}
					}
				}
				""";
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 4);
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 15); // method
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 4, 14); // switch
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 5, 6); // case
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 12, 13); // default
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
		FoldingTestUtils.assertCodeHasRegions(packageFragment, "TestFolding.java", str, 1);
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, "TestFolding.java", str);
		FoldingTestUtils.assertContainsRegionUsingStartAndEndLine(regions, str, 2, 5); // @Deprecated
	}
}

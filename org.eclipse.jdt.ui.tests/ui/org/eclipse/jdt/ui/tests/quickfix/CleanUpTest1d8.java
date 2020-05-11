/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fabrice TIERCELIN - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

@RunWith(JUnit4.class)
public class CleanUpTest1d8 extends CleanUpTestCase {
	@Rule
    public ProjectTestSetup projectsetup= new Java1d8ProjectTestSetup();

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		StubUtility.setCodeTemplate(CodeTemplateContextType.OVERRIDECOMMENT_ID, "", null);
	}

	@Override
	protected IJavaProject getProject() {
		return Java1d8ProjectTestSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return Java1d8ProjectTestSetup.getDefaultClasspath();
	}

	@Test
	public void testConvertToLambda01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    void foo(){\n" //
				+ "        Runnable r = new Runnable() {\n" //
				+ "            @Override\n" //
				+ "            public void run() {\n" //
				+ "                System.out.println(\"do something\");\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "    };\n" //
				+ "}\n";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    void foo(){\n" //
				+ "        Runnable r = () -> System.out.println(\"do something\");\n" //
				+ "    };\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });

		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original });
	}

	@Test
	public void testConvertToLambda02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    void foo(){\n" //
				+ "        Runnable r1 = new Runnable() {\n" //
				+ "            @Override\n" //
				+ "            public void run() {\n" //
				+ "                System.out.println(\"do something\");\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "        Runnable r2 = new Runnable() {\n" //
				+ "            @Override\n" //
				+ "            public void run() {\n" //
				+ "                System.out.println(\"do one thing\");\n" //
				+ "                System.out.println(\"do another thing\");\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "    };\n" //
				+ "}\n";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    void foo(){\n" //
				+ "        Runnable r1 = () -> System.out.println(\"do something\");\n" //
				+ "        Runnable r2 = () -> {\n" //
				+ "            System.out.println(\"do one thing\");\n" //
				+ "            System.out.println(\"do another thing\");\n" //
				+ "        };\n" //
				+ "    };\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });

		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original });
	}

	@Test
	public void testConvertToLambda03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.function.Supplier;\n" //
				+ "class E {\n" //
				+ "    Supplier<Supplier<String>> s= new Supplier<Supplier<String>>() {\n" //
				+ "        @Override\n" //
				+ "        public Supplier<String> get() {\n" //
				+ "            return new Supplier<String>() {\n" //
				+ "                @Override\n" //
				+ "                public String get() {\n" //
				+ "                    return \"a\";\n" //
				+ "                }\n" //
				+ "            };\n" //
				+ "        }\n" //
				+ "    };\n" //
				+ "}\n";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.util.function.Supplier;\n" //
				+ "class E {\n" //
				+ "    Supplier<Supplier<String>> s= () -> () -> \"a\";\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });

		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original });
	}

	@Test
	public void testConvertToLambdaNestedWithImports() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "import java.util.concurrent.Callable;\n" //
				+ "import java.util.concurrent.Executors;\n" //
				+ "public class E {\n" //
				+ "    void foo() {\n" //
				+ "        new Thread(new Runnable() {\n" //
				+ "            @Override\n" //
				+ "            public void run() {\n" //
				+ "                Executors.newSingleThreadExecutor().submit(new Callable<String>() {\n" //
				+ "                    @Override\n" //
				+ "                    public String call() throws Exception {\n" //
				+ "                        return \"hi\";\n" //
				+ "                    }\n" //
				+ "                });\n" //
				+ "            }\n" //
				+ "        });\n" //
				+ "    }\n" //
				+ "}\n";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		sample= "" //
				+ "package test;\n" //
				+ "import java.util.concurrent.Executors;\n" //
				+ "public class E {\n" //
				+ "    void foo() {\n" //
				+ "        new Thread(() -> Executors.newSingleThreadExecutor().submit(() -> \"hi\"));\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });

		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original });
	}

	// fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=434507#c5
	@Test
	public void testConvertToLambdaAmbiguous01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "interface ISuper {\n" //
				+ "    void foo(FI1 fi1);\n" //
				+ "}\n" //
				+ "\n" //
				+ "interface ISub extends ISuper {\n" //
				+ "    void foo(FI2 fi2);\n" //
				+ "}\n" //
				+ "\n" //
				+ "@FunctionalInterface\n" //
				+ "interface FI1 {\n" //
				+ "    void abc();\n" //
				+ "}\n" //
				+ "\n" //
				+ "@FunctionalInterface\n" //
				+ "interface FI2 {\n" //
				+ "    void xyz();\n" //
				+ "}\n" //
				+ "\n" //
				+ "class Test1 {\n" //
				+ "    private void test1() {\n" //
				+ "        f1().foo(new FI1() {\n" //
				+ "            @Override\n" //
				+ "            public void abc() {\n" //
				+ "                System.out.println();\n" //
				+ "            }\n" //
				+ "        });\n" //
				+ "\n" //
				+ "    }\n" //
				+ "    \n" //
				+ "    private ISub f1() {\n" //
				+ "        return null;\n" //
				+ "    }\n" //
				+ "}\n" //
				+ "\n" //
				+ "abstract class Test2 implements ISub {\n" //
				+ "    private void test2() {\n" //
				+ "        foo(new FI1() {\n" //
				+ "            @Override\n" //
				+ "            public void abc() {\n" //
				+ "                System.out.println();\n" //
				+ "            }\n" //
				+ "        });\n" //
				+ "    }\n" //
				+ "}\n" //
				+ "\n" //
				+ "class Test3 {\n" //
				+ "    void foo(FI1 fi1) {}\n" //
				+ "    void foo(FI2 fi2) {}\n" //
				+ "    private void test3() {\n" //
				+ "        foo(new FI1() {\n" //
				+ "            @Override\n" //
				+ "            public void abc() {\n" //
				+ "                System.out.println();\n" //
				+ "            }\n" //
				+ "        });\n" //
				+ "    }\n" //
				+ "}\n" //
				+ "\n" //
				+ "class Outer {\n" //
				+ "    class Test4 {\n" //
				+ "        {\n" //
				+ "            bar(0, new FI1() {\n" //
				+ "                @Override\n" //
				+ "                public void abc() {\n" //
				+ "                }\n" //
				+ "            });\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "    void bar(int i, FI1 fi1) {}\n" //
				+ "    void bar(int s, FI2 fi2) {}\n" //
				+ "}\n";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		sample= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "interface ISuper {\n" //
				+ "    void foo(FI1 fi1);\n" //
				+ "}\n" //
				+ "\n" //
				+ "interface ISub extends ISuper {\n" //
				+ "    void foo(FI2 fi2);\n" //
				+ "}\n" //
				+ "\n" //
				+ "@FunctionalInterface\n" //
				+ "interface FI1 {\n" //
				+ "    void abc();\n" //
				+ "}\n" //
				+ "\n" //
				+ "@FunctionalInterface\n" //
				+ "interface FI2 {\n" //
				+ "    void xyz();\n" //
				+ "}\n" //
				+ "\n" //
				+ "class Test1 {\n" //
				+ "    private void test1() {\n" //
				+ "        f1().foo((FI1) () -> System.out.println());\n" //
				+ "\n" //
				+ "    }\n" //
				+ "    \n" //
				+ "    private ISub f1() {\n" //
				+ "        return null;\n" //
				+ "    }\n" //
				+ "}\n" //
				+ "\n" //
				+ "abstract class Test2 implements ISub {\n" //
				+ "    private void test2() {\n" //
				+ "        foo((FI1) () -> System.out.println());\n" //
				+ "    }\n" //
				+ "}\n" //
				+ "\n" //
				+ "class Test3 {\n" //
				+ "    void foo(FI1 fi1) {}\n" //
				+ "    void foo(FI2 fi2) {}\n" //
				+ "    private void test3() {\n" //
				+ "        foo((FI1) () -> System.out.println());\n" //
				+ "    }\n" //
				+ "}\n" //
				+ "\n" //
				+ "class Outer {\n" //
				+ "    class Test4 {\n" //
				+ "        {\n" //
				+ "            bar(0, (FI1) () -> {\n" //
				+ "            });\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "    void bar(int i, FI1 fi1) {}\n" //
				+ "    void bar(int s, FI2 fi2) {}\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });

		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original });
	}

	// fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=434507#c5
	@Test
	public void testConvertToLambdaAmbiguous02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "@FunctionalInterface\n" //
				+ "interface FI1 {\n" //
				+ "    void abc();\n" //
				+ "}\n" //
				+ "\n" //
				+ "@FunctionalInterface\n" //
				+ "interface FI2 {\n" //
				+ "    void xyz();\n" //
				+ "}\n" //
				+ "\n" //
				+ "class Outer {\n" //
				+ "    void outer(FI1 fi1) {}\n" //
				+ "}\n" //
				+ "class OuterSub extends Outer {\n" //
				+ "    OuterSub() {\n" //
				+ "        super.outer(new FI1() {\n" //
				+ "            @Override\n" //
				+ "            public void abc() {\n" //
				+ "                System.out.println();\n" //
				+ "            }\n" //
				+ "        });\n" //
				+ "    }\n" //
				+ "    class Test1 {\n" //
				+ "        private void test1() {\n" //
				+ "            OuterSub.super.outer(new FI1() {\n" //
				+ "                @Override\n" //
				+ "                public void abc() {\n" //
				+ "                    System.out.println();\n" //
				+ "                }\n" //
				+ "            });\n" //
				+ "            OuterSub.this.outer(new FI1() {\n" //
				+ "                @Override\n" //
				+ "                public void abc() {\n" //
				+ "                    System.out.println();\n" //
				+ "                }\n" //
				+ "            });\n" //
				+ "            outer(new FI1() {\n" //
				+ "                @Override\n" //
				+ "                public void abc() {\n" //
				+ "                    System.out.println();\n" //
				+ "                }\n" //
				+ "            });\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "    @Override\n" //
				+ "    void outer(FI1 fi1) {}\n" //
				+ "    void outer(FI2 fi2) {}\n" //
				+ "}\n" //
				+ "\n" //
				+ "class OuterSub2 extends OuterSub {\n" //
				+ "    OuterSub2() {\n" //
				+ "        super.outer(new FI1() {\n" //
				+ "            @Override\n" //
				+ "            public void abc() {\n" //
				+ "                System.out.println();\n" //
				+ "            }\n" //
				+ "        });\n" //
				+ "    }\n" //
				+ "    class Test2 {\n" //
				+ "        private void test2() {\n" //
				+ "            OuterSub2.super.outer(new FI1() {\n" //
				+ "                @Override\n" //
				+ "                public void abc() {\n" //
				+ "                    System.out.println();\n" //
				+ "                }\n" //
				+ "            });\n" //
				+ "            OuterSub2.this.outer(new FI1() {\n" //
				+ "                @Override\n" //
				+ "                public void abc() {\n" //
				+ "                    System.out.println();\n" //
				+ "                }\n" //
				+ "            });\n" //
				+ "            outer(new FI1() {\n" //
				+ "                @Override\n" //
				+ "                public void abc() {\n" //
				+ "                    System.out.println();\n" //
				+ "                }\n" //
				+ "            });\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		sample= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "@FunctionalInterface\n" //
				+ "interface FI1 {\n" //
				+ "    void abc();\n" //
				+ "}\n" //
				+ "\n" //
				+ "@FunctionalInterface\n" //
				+ "interface FI2 {\n" //
				+ "    void xyz();\n" //
				+ "}\n" //
				+ "\n" //
				+ "class Outer {\n" //
				+ "    void outer(FI1 fi1) {}\n" //
				+ "}\n" //
				+ "class OuterSub extends Outer {\n" //
				+ "    OuterSub() {\n" //
				+ "        super.outer(() -> System.out.println());\n" //
				+ "    }\n" //
				+ "    class Test1 {\n" //
				+ "        private void test1() {\n" //
				+ "            OuterSub.super.outer(() -> System.out.println());\n" //
				+ "            OuterSub.this.outer((FI1) () -> System.out.println());\n" //
				+ "            outer((FI1) () -> System.out.println());\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "    @Override\n" //
				+ "    void outer(FI1 fi1) {}\n" //
				+ "    void outer(FI2 fi2) {}\n" //
				+ "}\n" //
				+ "\n" //
				+ "class OuterSub2 extends OuterSub {\n" //
				+ "    OuterSub2() {\n" //
				+ "        super.outer((FI1) () -> System.out.println());\n" //
				+ "    }\n" //
				+ "    class Test2 {\n" //
				+ "        private void test2() {\n" //
				+ "            OuterSub2.super.outer((FI1) () -> System.out.println());\n" //
				+ "            OuterSub2.this.outer((FI1) () -> System.out.println());\n" //
				+ "            outer((FI1) () -> System.out.println());\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });

		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original });
	}

	// fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=434507#c2
	@Test
	public void testConvertToLambdaAmbiguous03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public interface E {\n" //
				+ "    default void m() {\n" //
				+ "        bar(0, new FI() {\n" //
				+ "            @Override\n" //
				+ "            public int foo(int x) {\n" //
				+ "                return x++;\n" //
				+ "            }\n" //
				+ "        });\n" //
				+ "        baz(0, new ZI() {\n" //
				+ "            @Override\n" //
				+ "            public int zoo() {\n" //
				+ "                return 1;\n" //
				+ "            }\n" //
				+ "        });\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    void bar(int i, FI fi);\n" //
				+ "    void bar(int i, FV fv);\n" //
				+ "\n" //
				+ "    void baz(int i, ZI zi);\n" //
				+ "    void baz(int i, ZV zv);\n" //
				+ "}\n" //
				+ "\n" //
				+ "@FunctionalInterface interface FI { int  foo(int a); }\n" //
				+ "@FunctionalInterface interface FV { void foo(int a); }\n" //
				+ "\n" //
				+ "@FunctionalInterface interface ZI { int  zoo(); }\n" //
				+ "@FunctionalInterface interface ZV { void zoo(); }\n";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		sample= "" //
				+ "package test;\n" //
				+ "public interface E {\n" //
				+ "    default void m() {\n" //
				+ "        bar(0, (FI) x -> x++);\n" //
				+ "        baz(0, () -> 1);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    void bar(int i, FI fi);\n" //
				+ "    void bar(int i, FV fv);\n" //
				+ "\n" //
				+ "    void baz(int i, ZI zi);\n" //
				+ "    void baz(int i, ZV zv);\n" //
				+ "}\n" //
				+ "\n" //
				+ "@FunctionalInterface interface FI { int  foo(int a); }\n" //
				+ "@FunctionalInterface interface FV { void foo(int a); }\n" //
				+ "\n" //
				+ "@FunctionalInterface interface ZI { int  zoo(); }\n" //
				+ "@FunctionalInterface interface ZV { void zoo(); }\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });

		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original });
	}

	@Test
	public void testConvertToLambdaConflictingNames() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "interface FI {\n" //
				+ "    void run(int x);\n" //
				+ "}\n" //
				+ "\n" //
				+ "public class Test {\n" //
				+ "    {\n" //
				+ "        int e;\n" //
				+ "        FI fi = new FI() {\n" //
				+ "            @Override\n" //
				+ "            public void run(int e) {\n" //
				+ "                class C1 {\n" //
				+ "                    void init1() {\n" //
				+ "                        m(new FI() {\n" //
				+ "                            @Override\n" //
				+ "                            public void run(int e) {\n" //
				+ "                                FI fi = new FI() {\n" //
				+ "                                    @Override\n" //
				+ "                                    public void run(int e) {\n" //
				+ "                                        FI fi = new FI() {\n" //
				+ "                                            @Override\n" //
				+ "                                            public void run(int e) {\n" //
				+ "                                                return;\n" //
				+ "                                            }\n" //
				+ "                                        };\n" //
				+ "                                    }\n" //
				+ "                                };\n" //
				+ "                            }\n" //
				+ "                        });\n" //
				+ "                    }\n" //
				+ "\n" //
				+ "                    void init2() {\n" //
				+ "                        m(new FI() {\n" //
				+ "                            @Override\n" //
				+ "                            public void run(int e) {\n" //
				+ "                                new FI() {\n" //
				+ "                                    @Override\n" //
				+ "                                    public void run(int e3) {\n" //
				+ "                                        FI fi = new FI() {\n" //
				+ "                                            @Override\n" //
				+ "                                            public void run(int e) {\n" //
				+ "                                                return;\n" //
				+ "                                            }\n" //
				+ "                                        };\n" //
				+ "                                    }\n" //
				+ "                                };\n" //
				+ "                            }\n" //
				+ "                        });\n" //
				+ "                    }\n" //
				+ "                }\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    void m(FI fi) {\n" //
				+ "    };\n" //
				+ "}\n";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("Test.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "interface FI {\n" //
				+ "    void run(int x);\n" //
				+ "}\n" //
				+ "\n" //
				+ "public class Test {\n" //
				+ "    {\n" //
				+ "        int e;\n" //
				+ "        FI fi = e4 -> {\n" //
				+ "            class C1 {\n" //
				+ "                void init1() {\n" //
				+ "                    m(e3 -> {\n" //
				+ "                        FI fi2 = e2 -> {\n" //
				+ "                            FI fi1 = e1 -> {\n" //
				+ "                                return;\n" //
				+ "                            };\n" //
				+ "                        };\n" //
				+ "                    });\n" //
				+ "                }\n" //
				+ "\n" //
				+ "                void init2() {\n" //
				+ "                    m(e2 -> new FI() {\n" //
				+ "                        @Override\n" //
				+ "                        public void run(int e3) {\n" //
				+ "                            FI fi = e1 -> {\n" //
				+ "                                return;\n" //
				+ "                            };\n" //
				+ "                        }\n" //
				+ "                    });\n" //
				+ "                }\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    void m(FI fi) {\n" //
				+ "    };\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambdaWithMethodAnnotations() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class C1 {\n" //
				+ "    Runnable r1 = new Runnable() {\n" //
				+ "        @Override @A @Deprecated\n" //
				+ "        public void run() {\n" //
				+ "        }\n" //
				+ "    };\n" //
				+ "    Runnable r2 = new Runnable() {\n" //
				+ "        @Override @Deprecated\n" //
				+ "        public void run() {\n" //
				+ "        }\n" //
				+ "    };\n" //
				+ "}\n" //
				+ "@interface A {}\n";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("C1.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		sample= "" //
				+ "package test;\n" //
				+ "public class C1 {\n" //
				+ "    Runnable r1 = new Runnable() {\n" //
				+ "        @Override @A @Deprecated\n" //
				+ "        public void run() {\n" //
				+ "        }\n" //
				+ "    };\n" //
				+ "    Runnable r2 = () -> {\n" //
				+ "    };\n" //
				+ "}\n" //
				+ "@interface A {}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testConvertToAnonymousWithWildcards() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "import java.util.*;\n" //
				+ "public class E {\n" //
				+ "    void foo(Integer[] ints){\n" //
				+ "        Arrays.sort(ints, (i1, i2) -> i1 - i2);\n" //
				+ "        Comparator<?> cw = (w1, w2) -> 0;\n" //
				+ "        Comparator cr = (r1, r2) -> 0;\n" //
				+ "        Comparator<? extends Number> ce = (n1, n2) -> -0;\n" //
				+ "    };\n" //
				+ "}\n";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		sample= "" //
				+ "package test;\n" //
				+ "import java.util.*;\n" //
				+ "public class E {\n" //
				+ "    void foo(Integer[] ints){\n" //
				+ "        Arrays.sort(ints, new Comparator<Integer>() {\n" //
				+ "            @Override\n" //
				+ "            public int compare(Integer i1, Integer i2) {\n" //
				+ "                return i1 - i2;\n" //
				+ "            }\n" //
				+ "        });\n" //
				+ "        Comparator<?> cw = new Comparator<Object>() {\n" //
				+ "            @Override\n" //
				+ "            public int compare(Object w1, Object w2) {\n" //
				+ "                return 0;\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "        Comparator cr = new Comparator() {\n" //
				+ "            @Override\n" //
				+ "            public int compare(Object r1, Object r2) {\n" //
				+ "                return 0;\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "        Comparator<? extends Number> ce = new Comparator<Number>() {\n" //
				+ "            @Override\n" //
				+ "            public int compare(Number n1, Number n2) {\n" //
				+ "                return -0;\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "    };\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });

		disable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);
		enable(CleanUpConstants.USE_LAMBDA);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original });
	}

	@Test
	public void testConvertToAnonymousWithWildcards1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "interface I<M> {\n" //
				+ "    M run(M x);\n" //
				+ "}\n" //
				+ "\n" //
				+ "class Test {\n" //
				+ "    I<?> li = s -> null;\n" //
				+ "}\n";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("Test.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		sample= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "interface I<M> {\n" //
				+ "    M run(M x);\n" //
				+ "}\n" //
				+ "\n" //
				+ "class Test {\n" //
				+ "    I<?> li = new I<Object>() {\n" //
				+ "        @Override\n" //
				+ "        public Object run(Object s) {\n" //
				+ "            return null;\n" //
				+ "        }\n" //
				+ "    };\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });

		disable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);
		enable(CleanUpConstants.USE_LAMBDA);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original });
	}

	@Test
	public void testConvertToAnonymousWithJoinedSAM() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=428526#c1 and #c6
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "interface Foo<T, N extends Number> {\n" //
				+ "    void m(T t);\n" //
				+ "    void m(N n);\n" //
				+ "}\n" //
				+ "interface Baz extends Foo<Integer, Integer> {}\n" //
				+ "class Test {\n" //
				+ "    Baz baz = x -> { return; };\n" //
				+ "}\n";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("Test.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		sample= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "interface Foo<T, N extends Number> {\n" //
				+ "    void m(T t);\n" //
				+ "    void m(N n);\n" //
				+ "}\n" //
				+ "interface Baz extends Foo<Integer, Integer> {}\n" //
				+ "class Test {\n" //
				+ "    Baz baz = new Baz() {\n" //
				+ "        @Override\n" //
				+ "        public void m(Integer x) { return; }\n" //
				+ "    };\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });

		disable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);
		enable(CleanUpConstants.USE_LAMBDA);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original });
	}

	@Test
	public void testConvertToLambdaWithNonFunctionalTargetType() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=468457
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "public class Snippet {\n" //
				+ "    void test(Interface context) {\n" //
				+ "        context.set(\"bar\", new Runnable() {\n" //
				+ "            @Override\n" //
				+ "            public void run() {}\n" //
				+ "        });\n" //
				+ "        \n" //
				+ "    }    \n" //
				+ "}\n" //
				+ "\n" //
				+ "interface Interface {\n" //
				+ "    public void set(String name, Object value);\n" //
				+ "}\n";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("Snippet.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		sample= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "public class Snippet {\n" //
				+ "    void test(Interface context) {\n" //
				+ "        context.set(\"bar\", (Runnable) () -> {});\n" //
				+ "        \n" //
				+ "    }    \n" //
				+ "}\n" //
				+ "\n" //
				+ "interface Interface {\n" //
				+ "    public void set(String name, Object value);\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });

		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);
		disable(CleanUpConstants.USE_LAMBDA);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original });
	}

	@Test
	public void testConvertToLambdaWithSynchronizedOrStrictfp() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class C1 {\n" //
				+ "    Runnable run1 = new Runnable() {\n" //
				+ "        @Override\n" //
				+ "        public synchronized void run() {\n" //
				+ "        }\n" //
				+ "    };\n" //
				+ "    Runnable run2 = new Runnable() {\n" //
				+ "        @Override\n" //
				+ "        public strictfp void run() {\n" //
				+ "        }\n" //
				+ "    };\n" //
				+ "}\n";
		String original= sample;
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	// fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=560018
	@Test
	public void testConvertToLambdaInFieldInitializerWithFinalFieldReference() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= ""
				+ "package test;\n"
				+ "public class C1 {\n"
				+ "    final String s;\n"
				+ "    Runnable run1 = new Runnable() {\n"
				+ "        @Override\n"
				+ "        public void run() {\n"
				+ "            System.out.println(s\n"
				+ "        }\n"
				+ "    };\n"
				+ "    public C1() {\n"
				+ "        s = \"abc\";\n"
				+ "    };\n"
				+ "}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", sample, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	// fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=560018
	@Test
	public void testConvertToLambdaInFieldInitializerWithFinalFieldReference2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= ""
				+ "package test;\n"
				+ "public class C1 {\n"
				+ "    final String s = \"abc\";\n"
				+ "    Runnable run1 = new Runnable() {\n"
				+ "        @Override\n"
				+ "        public void run() {\n"
				+ "            System.out.println(s);\n"
				+ "        }\n"
				+ "    };\n"
				+ "}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", sample, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		String expected1= ""
				+ "package test;\n"
				+ "public class C1 {\n"
				+ "    final String s = \"abc\";\n"
				+ "    Runnable run1 = () -> System.out.println(s);\n"
				+ "}\n";
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambdaAndQualifyNextField() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= ""
				+ "package test;\n"
				+ "\n"
				+ "public class C1 {\n"
				+ "    static final String previousField = \"abc\";\n"
				+ "\n"
				+ "    Runnable run1 = new Runnable() {\n"
				+ "        @Override\n"
				+ "        public void run() {\n"
				+ "            System.out.println(previousField + instanceField + classField + getString());\n"
				+ "        }\n"
				+ "    };\n"
				+ "\n"
				+ "    static final String classField = \"abc\";\n"
				+ "    final String instanceField = \"abc\";\n"
				+ "    public String getString() {\n"
				+ "        return \"\";\n"
				+ "    }\n"
				+ "}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", sample, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		String expected= ""
				+ "package test;\n"
				+ "\n"
				+ "public class C1 {\n"
				+ "    static final String previousField = \"abc\";\n"
				+ "\n"
				+ "    Runnable run1 = () -> System.out.println(previousField + this.instanceField + C1.classField + getString());\n"
				+ "\n"
				+ "    static final String classField = \"abc\";\n"
				+ "    final String instanceField = \"abc\";\n"
				+ "    public String getString() {\n"
				+ "        return \"\";\n"
				+ "    }\n"
				+ "}\n";
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected });
	}

	@Test
	public void testConvertToLambdaWithQualifiedField() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= ""
				+ "package test;\n"
				+ "\n"
				+ "public class C1 {\n"
				+ "    static final String previousField = \"abc\";\n"
				+ "\n"
				+ "    Runnable run1 = new Runnable() {\n"
				+ "        @Override\n"
				+ "        public void run() {\n"
				+ "            System.out.println(C1.previousField + C1.this.instanceField + C1.classField + C1.this.getString());\n"
				+ "        }\n"
				+ "    };\n"
				+ "\n"
				+ "    static final String classField = \"def\";\n"
				+ "    final String instanceField = \"abc\";\n"
				+ "    public String getString() {\n"
				+ "        return \"\";\n"
				+ "    }\n"
				+ "}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", sample, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		String expected= ""
				+ "package test;\n"
				+ "\n"
				+ "public class C1 {\n"
				+ "    static final String previousField = \"abc\";\n"
				+ "\n"
				+ "    Runnable run1 = () -> System.out.println(C1.previousField + C1.this.instanceField + C1.classField + C1.this.getString());\n"
				+ "\n"
				+ "    static final String classField = \"def\";\n"
				+ "    final String instanceField = \"abc\";\n"
				+ "    public String getString() {\n"
				+ "        return \"\";\n"
				+ "    }\n"
				+ "}\n";
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected });
	}

	@Test
	public void testDoNotRefactorWithExpressions() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Date;\n" //
				+ "import java.util.function.Supplier;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public Supplier<Date> doNotRefactorWithAnonymousBody() {\n" //
				+ "        return () -> new Date() {\n" //
				+ "            @Override\n" //
				+ "            public String toString() {\n" //
				+ "                return \"foo\";\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testSimplifyLambdaExpression() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import static java.util.Calendar.getInstance;\n" //
				+ "import static java.util.Calendar.getAvailableLocales;\n" //
				+ "\n" //
				+ "import java.time.Instant;\n" //
				+ "import java.util.ArrayList;\n" //
				+ "import java.util.Calendar;\n" //
				+ "import java.util.Date;\n" //
				+ "import java.util.Locale;\n" //
				+ "import java.util.Vector;\n" //
				+ "import java.util.function.BiFunction;\n" //
				+ "import java.util.function.Function;\n" //
				+ "import java.util.function.Supplier;\n" //
				+ "\n" //
				+ "public class E extends Date {\n" //
				+ "    public String changeableText = \"foo\";\n" //
				+ "\n" //
				+ "    public Function<String, String> removeParentheses() {\n" //
				+ "        return (someString) -> someString.trim().toLowerCase();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, String> doNotRemoveParenthesesWithSingleVariableDeclaration() {\n" //
				+ "        return (String someString) -> someString.trim().toLowerCase();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public BiFunction<String, String, Integer> doNotRemoveParenthesesWithTwoParameters() {\n" //
				+ "        return (someString, anotherString) -> someString.trim().compareTo(anotherString.trim());\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Supplier<Boolean> doNotRemoveParenthesesWithNoParameter() {\n" //
				+ "        return () -> {System.out.println(\"foo\");return true;};\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, String> removeReturnAndBrackets() {\n" //
				+ "        return someString -> {return someString.trim().toLowerCase();};\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, String> removeReturnAndBracketsWithParentheses() {\n" //
				+ "        return someString -> {return someString.trim().toLowerCase() + \"bar\";};\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, String> doNotRemoveReturnWithSeveralStatements() {\n" //
				+ "        return someString -> {String trimmed = someString.trim();\n" //
				+ "        return trimmed.toLowerCase();};\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Supplier<ArrayList<String>> useCreationReference() {\n" //
				+ "        return () -> new ArrayList<>();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Integer, ArrayList<String>> useCreationReferenceWithParameter() {\n" //
				+ "        return capacity -> new ArrayList<>(capacity);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Integer, ArrayList<String>> useCreationReferenceWithParameterAndType() {\n" //
				+ "        // TODO this can be refactored like useCreationReferenceWithParameter\n" //
				+ "        return (Integer capacity) -> new ArrayList<>(capacity);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Integer, ArrayList<String>> doNotRefactorWithExpressions() {\n" //
				+ "        return capacity -> new ArrayList<>(capacity + 1);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public BiFunction<Integer, Integer, Vector<String>> useCreationReferenceWithParameters() {\n" //
				+ "        return (initialCapacity, capacityIncrement) -> new Vector<>(initialCapacity, capacityIncrement);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public BiFunction<Integer, Integer, Vector<String>> doNotRefactorShuffledParams() {\n" //
				+ "        return (initialCapacity, capacityIncrement) -> new Vector<>(capacityIncrement, initialCapacity);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Date, Long> useMethodReference() {\n" //
				+ "        return date -> date.getTime();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public BiFunction<Date, Date, Integer> useMethodReferenceWithParameter() {\n" //
				+ "        return (date, anotherDate) -> date.compareTo(anotherDate);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, Long> useTypeReference() {\n" //
				+ "        return numberInText -> Long.getLong(numberInText);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Function<Instant, Date> useTypeReferenceOnClassMethod() {\n" //
				+ "        return instant -> Date.from(instant);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Function<Locale, Calendar> useTypeReferenceOnImportedMethod() {\n" //
				+ "        return locale -> Calendar.getInstance(locale);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Supplier<Locale[]> useTypeReferenceAsSupplier() {\n" //
				+ "        return () -> Calendar.getAvailableLocales();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, Integer> useExpressionMethodReferenceOnLiteral() {\n" //
				+ "        return textToSearch -> \"AutoRefactor\".indexOf(textToSearch);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, Integer> doNotUseExpressionMethodReferenceOnVariable() {\n" //
				+ "        return textToSearch -> this.changeableText.indexOf(textToSearch);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Date, Integer> useThisMethodReference() {\n" //
				+ "        return anotherDate -> compareTo(anotherDate);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public class InnerClass {\n" //
				+ "        public Function<Date, Integer> doNotUseThisMethodReferenceOnTopLevelClassMethod() {\n" //
				+ "            return anotherDate -> compareTo(anotherDate);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Date, Integer> useThisMethodReferenceAddThis() {\n" //
				+ "        return anotherDate -> this.compareTo(anotherDate);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Date, Integer> useSuperMethodReference() {\n" //
				+ "        return anotherDate -> super.compareTo(anotherDate);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Integer, String> doNotUseConflictingMethodReference() {\n" //
				+ "        return numberToPrint -> numberToPrint.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Integer, String> doNotUseConflictingStaticMethodReference() {\n" //
				+ "        return numberToPrint -> Integer.toString(numberToPrint);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import static java.util.Calendar.getInstance;\n" //
				+ "import static java.util.Calendar.getAvailableLocales;\n" //
				+ "\n" //
				+ "import java.time.Instant;\n" //
				+ "import java.util.ArrayList;\n" //
				+ "import java.util.Calendar;\n" //
				+ "import java.util.Date;\n" //
				+ "import java.util.Locale;\n" //
				+ "import java.util.Vector;\n" //
				+ "import java.util.function.BiFunction;\n" //
				+ "import java.util.function.Function;\n" //
				+ "import java.util.function.Supplier;\n" //
				+ "\n" //
				+ "public class E extends Date {\n" //
				+ "    public String changeableText = \"foo\";\n" //
				+ "\n" //
				+ "    public Function<String, String> removeParentheses() {\n" //
				+ "        return someString -> someString.trim().toLowerCase();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, String> doNotRemoveParenthesesWithSingleVariableDeclaration() {\n" //
				+ "        return (String someString) -> someString.trim().toLowerCase();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public BiFunction<String, String, Integer> doNotRemoveParenthesesWithTwoParameters() {\n" //
				+ "        return (someString, anotherString) -> someString.trim().compareTo(anotherString.trim());\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Supplier<Boolean> doNotRemoveParenthesesWithNoParameter() {\n" //
				+ "        return () -> {System.out.println(\"foo\");return true;};\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, String> removeReturnAndBrackets() {\n" //
				+ "        return someString -> someString.trim().toLowerCase();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, String> removeReturnAndBracketsWithParentheses() {\n" //
				+ "        return someString -> (someString.trim().toLowerCase() + \"bar\");\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, String> doNotRemoveReturnWithSeveralStatements() {\n" //
				+ "        return someString -> {String trimmed = someString.trim();\n" //
				+ "        return trimmed.toLowerCase();};\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Supplier<ArrayList<String>> useCreationReference() {\n" //
				+ "        return ArrayList::new;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Integer, ArrayList<String>> useCreationReferenceWithParameter() {\n" //
				+ "        return ArrayList::new;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Integer, ArrayList<String>> useCreationReferenceWithParameterAndType() {\n" //
				+ "        // TODO this can be refactored like useCreationReferenceWithParameter\n" //
				+ "        return (Integer capacity) -> new ArrayList<>(capacity);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Integer, ArrayList<String>> doNotRefactorWithExpressions() {\n" //
				+ "        return capacity -> new ArrayList<>(capacity + 1);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public BiFunction<Integer, Integer, Vector<String>> useCreationReferenceWithParameters() {\n" //
				+ "        return Vector::new;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public BiFunction<Integer, Integer, Vector<String>> doNotRefactorShuffledParams() {\n" //
				+ "        return (initialCapacity, capacityIncrement) -> new Vector<>(capacityIncrement, initialCapacity);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Date, Long> useMethodReference() {\n" //
				+ "        return Date::getTime;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public BiFunction<Date, Date, Integer> useMethodReferenceWithParameter() {\n" //
				+ "        return Date::compareTo;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, Long> useTypeReference() {\n" //
				+ "        return Long::getLong;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Function<Instant, Date> useTypeReferenceOnClassMethod() {\n" //
				+ "        return instant -> Date.from(instant);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Function<Locale, Calendar> useTypeReferenceOnImportedMethod() {\n" //
				+ "        return Calendar::getInstance;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Supplier<Locale[]> useTypeReferenceAsSupplier() {\n" //
				+ "        return Calendar::getAvailableLocales;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, Integer> useExpressionMethodReferenceOnLiteral() {\n" //
				+ "        return \"AutoRefactor\"::indexOf;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, Integer> doNotUseExpressionMethodReferenceOnVariable() {\n" //
				+ "        return textToSearch -> this.changeableText.indexOf(textToSearch);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Date, Integer> useThisMethodReference() {\n" //
				+ "        return this::compareTo;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public class InnerClass {\n" //
				+ "        public Function<Date, Integer> doNotUseThisMethodReferenceOnTopLevelClassMethod() {\n" //
				+ "            return anotherDate -> compareTo(anotherDate);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Date, Integer> useThisMethodReferenceAddThis() {\n" //
				+ "        return this::compareTo;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Date, Integer> useSuperMethodReference() {\n" //
				+ "        return super::compareTo;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Integer, String> doNotUseConflictingMethodReference() {\n" //
				+ "        return numberToPrint -> numberToPrint.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Integer, String> doNotUseConflictingStaticMethodReference() {\n" //
				+ "        return numberToPrint -> Integer.toString(numberToPrint);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambdaWithRecursion() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= ""
				+ "import java.util.function.Function;\n" //
				+ "public class C1 {\n" //
				+ "\n" //
				+ "    public interface I1 {\n" //
				+ "        public int add(int a);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    I1 k = new I1() {\n" //
				+ "        @Override\n" //
				+ "        public int add(int a) {\n" //
				+ "            if (a == 2) {\n" //
				+ "                return add(3);\n" //
				+ "            }\n" //
				+ "            return a + 7;\n" //
				+ "        }\n" //
				+ "    };\n" //
				+ "\n" //
				+ "    public static I1 j = new I1() {\n" //
				+ "        @Override\n" //
				+ "        public int add(int a) {\n" //
				+ "            if (a == 2) {\n" //
				+ "                return add(4);\n" //
				+ "            }\n" //
				+ "            return a + 8;\n" //
				+ "        }\n" //
				+ "    };\n" //
				+ "}\n"; //
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", sample, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		String expected= ""
				+ "import java.util.function.Function;\n" //
				+ "public class C1 {\n" //
				+ "\n" //
				+ "    public interface I1 {\n" //
				+ "        public int add(int a);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    I1 k = a -> {\n" //
				+ "        if (a == 2) {\n" //
				+ "            return this.k.add(3);\n" //
				+ "        }\n" //
				+ "        return a + 7;\n" //
				+ "    };\n" //
				+ "\n" //
				+ "    public static I1 j = a -> {\n" //
				+ "        if (a == 2) {\n" //
				+ "            return C1.j.add(4);\n" //
				+ "        }\n" //
				+ "        return a + 8;\n" //
				+ "    };\n" //
				+ "}\n"; //
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected });
	}

	@Test
	public void testDoNotConvertLocalRecursiveClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "import java.util.function.Function;\n" //
				+ "\n" //
				+ "public class C2 {\n" //
				+ "\n" //
				+ "    public interface I1 {\n" //
				+ "        public int add(int a);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int foo() {\n" //
				+ "        I1 doNotConvert = new I1() {\n" //
				+ "            @Override\n" //
				+ "            public int add(int a) {\n" //
				+ "                if (a == 2) {\n" //
				+ "                    return add(5);\n" //
				+ "                }\n" //
				+ "                return a + 9;\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "        return n.add(9);\n" //
				+ "    }\n" //
				+ "}\n"; //
		ICompilationUnit cu= pack1.createCompilationUnit("C2.java", sample, false, null);

		enable(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}
}

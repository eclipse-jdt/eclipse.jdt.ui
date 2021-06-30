/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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

import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.FixMessages;

import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public class CleanUpTest1d8 extends CleanUpTestCase {
	@Rule
	public ProjectTestSetup projectSetup= new Java1d8ProjectTestSetup();

	@Override
	public void setUp() throws Exception {
		super.setUp();
		StubUtility.setCodeTemplate(CodeTemplateContextType.OVERRIDECOMMENT_ID, "", null);
	}

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	@Test
	public void testConvertToLambda01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    void foo(){\n" //
				+ "        // Keep this comment\n" //
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
				+ "        // Keep this comment\n" //
				+ "        Runnable r = () -> System.out.println(\"do something\");\n" //
				+ "    };\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);

		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original },
				new HashSet<>(Arrays.asList(FixMessages.LambdaExpressionsFix_convert_to_anonymous_class_creation)));
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

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 },
				new HashSet<>(Arrays.asList(FixMessages.LambdaExpressionsFix_convert_to_lambda_expression)));

		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original },
				new HashSet<>(Arrays.asList(FixMessages.LambdaExpressionsFix_convert_to_anonymous_class_creation)));
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

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);

		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original }, null);
	}

	@Test
	public void testConvertToLambdaWithConstant() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    @FunctionalInterface\n" //
				+ "    interface FI1 extends Runnable {\n" //
				+ "        int CONSTANT_VALUE = 123;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    void foo() {\n" //
				+ "        Runnable r = new FI1() {\n" //
				+ "            @Override\n" //
				+ "            public void run() {\n" //
				+ "                System.out.println(CONSTANT_VALUE);\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "    };\n" //
				+ "}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		sample= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    @FunctionalInterface\n" //
				+ "    interface FI1 extends Runnable {\n" //
				+ "        int CONSTANT_VALUE = 123;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    void foo() {\n" //
				+ "        Runnable r = () -> System.out.println(FI1.CONSTANT_VALUE);\n" //
				+ "    };\n" //
				+ "}\n";
		String expected= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(FixMessages.LambdaExpressionsFix_convert_to_lambda_expression)));
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

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);

		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original }, null);
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

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);

		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original }, null);
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

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);

		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original }, null);
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

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);

		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original }, null);
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

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
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

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
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

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);

		disable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);
		enable(CleanUpConstants.USE_LAMBDA);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original }, null);
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

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);

		disable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);
		enable(CleanUpConstants.USE_LAMBDA);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original }, null);
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

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);

		disable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);
		enable(CleanUpConstants.USE_LAMBDA);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original }, null);
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

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);

		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);
		disable(CleanUpConstants.USE_LAMBDA);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original }, null);
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
				+ "\n"
				+ "public class C1 {\n"
				+ "    final String s;\n"
				+ "\n"
				+ "    Runnable run1 = new Runnable() {\n"
				+ "        @Override\n"
				+ "        public void run() {\n"
				+ "            System.out.println(s);\n"
				+ "        }\n"
				+ "    };\n"
				+ "\n"
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
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected1 }, null);
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
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected }, null);
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
				+ "    Runnable run1 = () -> System.out.println(C1.previousField + this.instanceField + C1.classField + this.getString());\n"
				+ "\n"
				+ "    static final String classField = \"def\";\n"
				+ "    final String instanceField = \"abc\";\n"
				+ "    public String getString() {\n"
				+ "        return \"\";\n"
				+ "    }\n"
				+ "}\n";
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected }, null);
	}

	@Test
	public void testSimplifyLambdaExpression() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= "" //
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
				+ "    public Function<String, String> removeReturnAndBrackets() {\n" //
				+ "        return someString -> {return someString.trim().toLowerCase();};\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, String> removeReturnAndBracketsWithParentheses() {\n" //
				+ "        return (someString) -> {return someString.trim().toLowerCase() + \"bar\";};\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Supplier<ArrayList<String>> useCreationReference() {\n" //
				+ "        return () -> { return new ArrayList<>(); };\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Integer, ArrayList<String>> useCreationReferenceWithParameter() {\n" //
				+ "        return (capacity) -> new ArrayList<>(capacity);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Integer, ArrayList<String>> useCreationReferenceWithParameterAndType() {\n" //
				+ "        // TODO this can be refactored like useCreationReferenceWithParameter\n" //
				+ "        return (Integer capacity) -> new ArrayList<>(capacity);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public BiFunction<Integer, Integer, Vector<String>> useCreationReferenceWithParameters() {\n" //
				+ "        return (initialCapacity, capacityIncrement) -> new Vector<>(initialCapacity, capacityIncrement);\n" //
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
				+ "    public static Function<String, Long> useTypeReference() {\n" //
				+ "        return (numberInText) -> { return Long.getLong(numberInText); };\n" //
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
				+ "    public Function<Date, Integer> useThisMethodReference() {\n" //
				+ "        return anotherDate -> compareTo(anotherDate);\n" //
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
				+ "    public static Integer dummy(String arg) {\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Function<String, Integer> useTypeReferenceQualifyingLocalType() {\n" //
				+ "        return numberInText -> E.dummy(numberInText);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Function<String, Integer> useTypeReferenceFullyQualifyingLocalType() {\n" //
				+ "        return numberInText -> test1.E.dummy(numberInText);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Function<String, Integer> useTypeReferenceOnLocalType() {\n" //
				+ "        return numberInText -> dummy(numberInText);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Function<Instant, java.sql.Date> useTypeReferenceQualifyingInheritedType() {\n" //
				+ "        return instant -> java.sql.Date.from(instant);\n" //
				+ "    }\n" //
				+ "}\n";

		String expected= "" //
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
				+ "    public Function<String, String> removeReturnAndBrackets() {\n" //
				+ "        return someString -> someString.trim().toLowerCase();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, String> removeReturnAndBracketsWithParentheses() {\n" //
				+ "        return someString -> (someString.trim().toLowerCase() + \"bar\");\n" //
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
				+ "    public BiFunction<Integer, Integer, Vector<String>> useCreationReferenceWithParameters() {\n" //
				+ "        return Vector::new;\n" //
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
				+ "    public static Function<String, Long> useTypeReference() {\n" //
				+ "        return Long::getLong;\n" //
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
				+ "    public Function<Date, Integer> useThisMethodReference() {\n" //
				+ "        return this::compareTo;\n" //
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
				+ "    public static Integer dummy(String arg) {\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Function<String, Integer> useTypeReferenceQualifyingLocalType() {\n" //
				+ "        return E::dummy;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Function<String, Integer> useTypeReferenceFullyQualifyingLocalType() {\n" //
				+ "        return E::dummy;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Function<String, Integer> useTypeReferenceOnLocalType() {\n" //
				+ "        return E::dummy;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Function<Instant, java.sql.Date> useTypeReferenceQualifyingInheritedType() {\n" //
				+ "        return java.sql.Date::from;\n" //
				+ "    }\n" //
				+ "}\n";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF);

		// Then
		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.LambdaExpressionAndMethodRefCleanUp_description)));
	}

	@Test
	public void testDoNotSimplifyLambdaExpression() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.ArrayList;\n" //
				+ "import java.util.Date;\n" //
				+ "import java.util.Vector;\n" //
				+ "import java.util.function.BiFunction;\n" //
				+ "import java.util.function.Function;\n" //
				+ "import java.util.function.Supplier;\n" //
				+ "\n" //
				+ "public class E extends Date {\n" //
				+ "    public String changeableText = \"foo\";\n" //
				+ "\n" //
				+ "    public Supplier<Date> doNotRefactorWithAnonymousBody() {\n" //
				+ "        return () -> new Date() {\n" //
				+ "            @Override\n" //
				+ "            public String toString() {\n" //
				+ "                return \"foo\";\n" //
				+ "            }\n" //
				+ "        };\n" //
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
				+ "    public Function<String, String> doNotRemoveReturnWithSeveralStatements() {\n" //
				+ "        return someString -> {String trimmed = someString.trim();\n" //
				+ "        return trimmed.toLowerCase();};\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Integer, ArrayList<String>> doNotRefactorWithExpressions() {\n" //
				+ "        return capacity -> new ArrayList<>(capacity + 1);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public BiFunction<Integer, Integer, Vector<String>> doNotRefactorShuffledParams() {\n" //
				+ "        return (initialCapacity, capacityIncrement) -> new Vector<>(capacityIncrement, initialCapacity);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, Integer> doNotUseExpressionMethodReferenceOnVariable() {\n" //
				+ "        return textToSearch -> this.changeableText.indexOf(textToSearch);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public class InnerClass {\n" //
				+ "        public Function<Date, Integer> doNotUseThisMethodReferenceOnTopLevelClassMethod() {\n" //
				+ "            return anotherDate -> compareTo(anotherDate);\n" //
				+ "        }\n" //
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
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testConvertToLambdaWithRecursion() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= ""
				+ "package test;\n" //
				+ "\n" //
				+ "import java.util.function.Function;\n" //
				+ "\n" //
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
				+ "package test;\n" //
				+ "\n" //
				+ "import java.util.function.Function;\n" //
				+ "\n" //
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
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected }, null);
	}

	@Test
	public void testDoNotConvertLocalRecursiveClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
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
				+ "        return doNotConvert.add(9);\n" //
				+ "    }\n" //
				+ "}\n"; //
		ICompilationUnit cu= pack1.createCompilationUnit("C2.java", sample, false, null);

		enable(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testComparingOnCriteria() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.io.File;\n" //
				+ "import java.util.Collections;\n" //
				+ "import java.util.Comparator;\n" //
				+ "import java.util.Date;\n" //
				+ "import java.util.List;\n" //
				+ "import java.util.Locale;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    private Comparator<Date> refactorField = new Comparator<Date>() {\n" //
				+ "        @Override\n" //
				+ "        public int compare(Date o1, Date o2) {\n" //
				+ "            return o1.toString().compareTo(o2.toString());\n" //
				+ "        }\n" //
				+ "    };\n" //
				+ "\n" //
				+ "    public List<Date> useMethodRef(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = new Comparator<Date>() {\n" //
				+ "\n" //
				+ "            @Override\n" //
				+ "            public int compare(Date o1, Date o2) {\n" //
				+ "                return o1.toString().compareTo(o2.toString());\n" //
				+ "            }\n" //
				+ "\n" //
				+ "        };\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> useReversedMethodRef(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = new Comparator<Date>() {\n" //
				+ "\n" //
				+ "            @Override\n" //
				+ "            public int compare(Date o1, Date o2) {\n" //
				+ "                return o2.toString().compareTo(o1.toString());\n" //
				+ "            }\n" //
				+ "\n" //
				+ "        };\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> useNegatedMethodRef(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = new Comparator<Date>() {\n" //
				+ "\n" //
				+ "            @Override\n" //
				+ "            public int compare(Date o1, Date o2) {\n" //
				+ "                return -o1.toString().compareTo(o2.toString());\n" //
				+ "            }\n" //
				+ "\n" //
				+ "        };\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<File> useTypedLambdaExpression(List<File> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<File> comparator = new Comparator<File>() {\n" //
				+ "\n" //
				+ "            @Override\n" //
				+ "            public int compare(File f1, File f2) {\n" //
				+ "                return f1.separator.compareTo(f2.separator);\n" //
				+ "            }\n" //
				+ "\n" //
				+ "        };\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<File> useUntypedLambdaExpression(List<File> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator comparator = new Comparator<File>() {\n" //
				+ "\n" //
				+ "            @Override\n" //
				+ "            public int compare(File f1, File f2) {\n" //
				+ "                return f1.separator.compareTo(f2.separator);\n" //
				+ "            }\n" //
				+ "\n" //
				+ "        };\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<File> useReversedLambdaExpression(List<File> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<File> comparator = new Comparator<File>() {\n" //
				+ "\n" //
				+ "            @Override\n" //
				+ "            public int compare(File f1, File f2) {\n" //
				+ "                return f2.separator.compareTo(f1.separator);\n" //
				+ "            }\n" //
				+ "\n" //
				+ "        };\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> replaceLambdaByMethodRef(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = (o1, o2) -> o1.toString().compareTo(o2.toString());\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> replaceLambdaByReversedMethodRef(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = (o1, o2) -> o2.toString().compareTo(o1.toString());\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> replaceLambdaByNegatedMethodRef(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = (o1, o2) -> -o1.toString().compareTo(o2.toString());\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<File> replaceLambdaByTypedLambdaExpression(List<File> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<File> comparator = (f1, f2) -> f1.separator.compareTo(f2.separator);\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<File> replaceLambdaByReversedLambdaExpression(List<File> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<File> comparator = (f1, f2) -> f2.separator.compareTo(f1.separator);\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> replaceLambdaUsingRightType(List<Date> initialPackagesToDelete) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Collections.sort(initialPackagesToDelete, (Date one, Date two) -> one.toString().compareTo(two.toString()));\n" //
				+ "\n" //
				+ "        return initialPackagesToDelete;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> useMethodRefNullFirst(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = new Comparator<Date>() {\n" //
				+ "\n" //
				+ "            @Override\n" //
				+ "            public int compare(Date o1, Date o2) {\n" //
				+ "                if (o1 != null) {\n" //
				+ "                    if (o2 != null) {\n" //
				+ "                        return o1.toString().compareTo(o2.toString());\n" //
				+ "                    }\n" //
				+ "\n" //
				+ "                    return 1;\n" //
				+ "                } else if (o2 != null) {\n" //
				+ "                    return -1;\n" //
				+ "                } else {\n" //
				+ "                    return 0;\n" //
				+ "                }\n" //
				+ "            }\n" //
				+ "\n" //
				+ "        };\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> useMethodRefNullLast(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = new Comparator<Date>() {\n" //
				+ "\n" //
				+ "            @Override\n" //
				+ "            public int compare(Date o1, Date o2) {\n" //
				+ "                if (o1 != null) {\n" //
				+ "                    if (null != o2) {\n" //
				+ "                        return o1.toString().compareTo(o2.toString());\n" //
				+ "                    } else {\n" //
				+ "                        return -10;\n" //
				+ "                    }\n" //
				+ "                } else {\n" //
				+ "                    return (o2 == null) ? 0 : 20;\n" //
				+ "                }\n" //
				+ "            }\n" //
				+ "\n" //
				+ "        };\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> useReversedMethodRefNullFirst(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = new Comparator<Date>() {\n" //
				+ "\n" //
				+ "            @Override\n" //
				+ "            public int compare(Date o1, Date o2) {\n" //
				+ "                if (o1 != null)\n" //
				+ "                    if (null == o2)\n" //
				+ "                        return 123;\n" //
				+ "                     else\n" //
				+ "                        return o2.toString().compareTo(o1.toString());\n" //
				+ "\n" //
				+ "                return (o2 == null) ? 0 : -123;\n" //
				+ "            }\n" //
				+ "\n" //
				+ "        };\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> useReversedMethodRefNullLast(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Collections.sort(listToSort, new Comparator<Date>() {\n" //
				+ "\n" //
				+ "            @Override\n" //
				+ "            public int compare(Date o1, Date o2) {\n" //
				+ "                if (o1 != null) {\n" //
				+ "                    if (null == o2) {\n" //
				+ "                        return -10;\n" //
				+ "                    } else {\n" //
				+ "                        return Long.compare(o2.getTime(), o1.getTime());\n" //
				+ "                    }\n" //
				+ "                }\n" //
				+ "\n" //
				+ "                return (o2 == null) ? 0 : 20;\n" //
				+ "            }\n" //
				+ "\n" //
				+ "        });\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> useMethodRefWithNegation(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = new Comparator<Date>() {\n" //
				+ "            @Override\n" //
				+ "            public int compare(Date o1, Date o2) {\n" //
				+ "                if (!(o1 != null)) {\n" //
				+ "                    if (o2 != null) {\n" //
				+ "                        return -1;\n" //
				+ "                    } else {\n" //
				+ "                        return 0;\n" //
				+ "                    }\n" //
				+ "                } else {\n" //
				+ "                    if (o2 != null) {\n" //
				+ "                        return -o1.toString().compareTo(o2.toString());\n" //
				+ "                    }\n" //
				+ "\n" //
				+ "                    return 1;\n" //
				+ "                }\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "        listToSort.sort(comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> useMethodRefUnorderedCondition(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = new Comparator<Date>() {\n" //
				+ "\n" //
				+ "            @Override\n" //
				+ "            public int compare(Date o1, Date o2) {\n" //
				+ "                if (o2 != null) {\n" //
				+ "                    if (o1 != null) {\n" //
				+ "                        return o1.toString().compareTo(o2.toString());\n" //
				+ "                    }\n" //
				+ "\n" //
				+ "                    return -1;\n" //
				+ "                } else if (o1 != null) {\n" //
				+ "                    return 1;\n" //
				+ "                } else {\n" //
				+ "                    return 0;\n" //
				+ "                }\n" //
				+ "            }\n" //
				+ "\n" //
				+ "        };\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> replaceLambdaByMethodRefNullFirst(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = (o1, o2) -> {\n" //
				+ "            if (o1 != null) {\n" //
				+ "                if (o2 != null) {\n" //
				+ "                    return o1.toString().compareTo(o2.toString());\n" //
				+ "                }\n" //
				+ "\n" //
				+ "                return 1;\n" //
				+ "            } else if (o2 != null) {\n" //
				+ "                return -1;\n" //
				+ "            } else {\n" //
				+ "                return 0;\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> replaceLambdaByMethodRefNullLast(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = (o1, o2) -> {\n" //
				+ "            if (o1 != null) {\n" //
				+ "                if (null != o2) {\n" //
				+ "                    return o1.toString().compareTo(o2.toString());\n" //
				+ "                } else {\n" //
				+ "                    return -10;\n" //
				+ "                }\n" //
				+ "            } else {\n" //
				+ "                return (o2 == null) ? 0 : 20;\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> replaceLambdaByReversedMethodRefNullFirst(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = (o1, o2) -> {\n" //
				+ "            if (o1 != null)\n" //
				+ "                if (null == o2)\n" //
				+ "                    return 123;\n" //
				+ "                 else\n" //
				+ "                    return o2.toString().compareTo(o1.toString());\n" //
				+ "\n" //
				+ "            return (o2 == null) ? 0 : -123;\n" //
				+ "        };\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> replaceLambdaByReversedMethodRefNullLast(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator= (o1, o2) -> {\n" //
				+ "            if (o1 != null) {\n" //
				+ "                if (null == o2) {\n" //
				+ "                    return -10;\n" //
				+ "                } else {\n" //
				+ "                    return Long.compare(o2.getTime(), o1.getTime());\n" //
				+ "                }\n" //
				+ "            }\n" //
				+ "\n" //
				+ "            return (o2 == null) ? 0 : 20;\n" //
				+ "        };\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> replaceLambdaByMethodRefWithNegation(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = (o1, o2) -> {\n" //
				+ "            if (!(o1 != null)) {\n" //
				+ "                if (o2 != null) {\n" //
				+ "                    return -1;\n" //
				+ "                } else {\n" //
				+ "                    return 0;\n" //
				+ "                }\n" //
				+ "            } else {\n" //
				+ "                if (o2 != null) {\n" //
				+ "                    return -o1.toString().compareTo(o2.toString());\n" //
				+ "                }\n" //
				+ "\n" //
				+ "                return 1;\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "        listToSort.sort(comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> replaceLambdaByMethodRefUnorderedCondition(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = (o1, o2) -> {\n" //
				+ "            if (o2 != null) {\n" //
				+ "                if (o1 != null) {\n" //
				+ "                    return o1.toString().compareTo(o2.toString());\n" //
				+ "                }\n" //
				+ "\n" //
				+ "                return -1;\n" //
				+ "            } else if (o1 != null) {\n" //
				+ "                return 1;\n" //
				+ "            } else {\n" //
				+ "                return 0;\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "}\n";

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.io.File;\n" //
				+ "import java.util.Collections;\n" //
				+ "import java.util.Comparator;\n" //
				+ "import java.util.Date;\n" //
				+ "import java.util.List;\n" //
				+ "import java.util.Locale;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    private Comparator<Date> refactorField = Comparator.comparing(Date::toString);\n" //
				+ "\n" //
				+ "    public List<Date> useMethodRef(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = Comparator.comparing(Date::toString);\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> useReversedMethodRef(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = Comparator.comparing(Date::toString).reversed();\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> useNegatedMethodRef(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = Comparator.comparing(Date::toString).reversed();\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<File> useTypedLambdaExpression(List<File> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<File> comparator = Comparator.comparing(f1 -> f1.separator);\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<File> useUntypedLambdaExpression(List<File> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator comparator = Comparator.comparing((File f1) -> f1.separator);\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<File> useReversedLambdaExpression(List<File> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<File> comparator = Comparator.comparing((File f1) -> f1.separator).reversed();\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> replaceLambdaByMethodRef(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = Comparator.comparing(Date::toString);\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> replaceLambdaByReversedMethodRef(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = Comparator.comparing(Date::toString).reversed();\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> replaceLambdaByNegatedMethodRef(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = Comparator.comparing(Date::toString).reversed();\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<File> replaceLambdaByTypedLambdaExpression(List<File> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<File> comparator = Comparator.comparing(f1 -> f1.separator);\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<File> replaceLambdaByReversedLambdaExpression(List<File> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<File> comparator = Comparator.comparing((File f1) -> f1.separator).reversed();\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> replaceLambdaUsingRightType(List<Date> initialPackagesToDelete) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Collections.sort(initialPackagesToDelete, Comparator.comparing(Date::toString));\n" //
				+ "\n" //
				+ "        return initialPackagesToDelete;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> useMethodRefNullFirst(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = Comparator.nullsFirst(Comparator.comparing(Date::toString));\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> useMethodRefNullLast(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = Comparator.nullsLast(Comparator.comparing(Date::toString));\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> useReversedMethodRefNullFirst(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = Comparator.nullsFirst(Comparator.comparing(Date::toString).reversed());\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> useReversedMethodRefNullLast(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Collections.sort(listToSort, Comparator.nullsLast(Comparator.comparing(Date::getTime)));\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> useMethodRefWithNegation(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = Comparator.nullsFirst(Comparator.comparing(Date::toString).reversed());\n" //
				+ "        listToSort.sort(comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> useMethodRefUnorderedCondition(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = Comparator.nullsFirst(Comparator.comparing(Date::toString));\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> replaceLambdaByMethodRefNullFirst(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = Comparator.nullsFirst(Comparator.comparing(Date::toString));\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> replaceLambdaByMethodRefNullLast(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = Comparator.nullsLast(Comparator.comparing(Date::toString));\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> replaceLambdaByReversedMethodRefNullFirst(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = Comparator.nullsFirst(Comparator.comparing(Date::toString).reversed());\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> replaceLambdaByReversedMethodRefNullLast(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator= Comparator.nullsLast(Comparator.comparing(Date::getTime));\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> replaceLambdaByMethodRefWithNegation(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = Comparator.nullsFirst(Comparator.comparing(Date::toString).reversed());\n" //
				+ "        listToSort.sort(comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> replaceLambdaByMethodRefUnorderedCondition(List<Date> listToSort) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Comparator<Date> comparator = Comparator.nullsFirst(Comparator.comparing(Date::toString));\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "}\n";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.COMPARING_ON_CRITERIA);

		// Then
		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.ComparingOnCriteriaCleanUp_description)));
	}

	@Test
	public void testDoNotUseComparingOnCriteria() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Collections;\n" //
				+ "import java.util.Comparator;\n" //
				+ "import java.util.Date;\n" //
				+ "import java.util.List;\n" //
				+ "import java.util.Locale;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public List<Date> doNotUseMethodRefWithWeirdBehavior(List<Date> listToSort) {\n" //
				+ "        Comparator<Date> comparator = new Comparator<Date>() {\n" //
				+ "\n" //
				+ "            @Override\n" //
				+ "            public int compare(Date o1, Date o2) {\n" //
				+ "                if (o1 != null) {\n" //
				+ "                    if (o2 != null) {\n" //
				+ "                        return o1.toString().compareTo(o2.toString());\n" //
				+ "                    } else {\n" //
				+ "                        return 1;\n" //
				+ "                    }\n" //
				+ "                } else if (o2 != null) {\n" //
				+ "                    return -1;\n" //
				+ "                } else {\n" //
				+ "                    return 100;\n" //
				+ "                }\n" //
				+ "            }\n" //
				+ "\n" //
				+ "        };\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<String> doNotUseMethodRef(List<String> listToSort) {\n" //
				+ "        Comparator<String> comparator = new Comparator<String>() {\n" //
				+ "            @Override\n" //
				+ "            public int compare(String o1, String o2) {\n" //
				+ "                return o1.toLowerCase(Locale.ENGLISH).compareTo(o2.toLowerCase(Locale.ENGLISH));\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Comparator<Date> doNotRefactorComparisonWithoutCompareToMethod(List<Date> listToSort) {\n" //
				+ "        Comparator<Date> comparator = new Comparator<Date>() {\n" //
				+ "\n" //
				+ "            @Override\n" //
				+ "            public int compare(Date o1, Date o2) {\n" //
				+ "                return (int) (o1.getTime() - o2.getTime());\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "\n" //
				+ "        return comparator;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int compareTo(E anc) {\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public E getNewInstance() {\n" //
				+ "        return new E();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private Comparator<E> doNotRefactorNotComparableObjects = new Comparator<E>() {\n" //
				+ "        @Override\n" //
				+ "        public int compare(E o1, E o2) {\n" //
				+ "            return o1.getNewInstance().compareTo(o2.getNewInstance());\n" //
				+ "        }\n" //
				+ "    };\n" //
				+ "\n" //
				+ "    public Comparator<Date> doNotRemoveSecondaryMethod(List<Date> listToSort) {\n" //
				+ "        Comparator<Date> comparator = new Comparator<Date>() {\n" //
				+ "\n" //
				+ "            @Override\n" //
				+ "            public int compare(Date o1, Date o2) {\n" //
				+ "                return o1.toString().compareTo(o2.toString());\n" //
				+ "            }\n" //
				+ "\n" //
				+ "            @Override\n" //
				+ "            public String toString() {\n" //
				+ "                return \"Compare formatted dates\";\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "\n" //
				+ "        return comparator;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> doNotReplaceLambdaByUseMethodRefWithWeirdBehavior(List<Date> listToSort) {\n" //
				+ "        Comparator<Date> comparator = (o1, o2) -> {\n" //
				+ "            if (o1 != null) {\n" //
				+ "                if (o2 != null) {\n" //
				+ "                    return o1.toString().compareTo(o2.toString());\n" //
				+ "                } else {\n" //
				+ "                    return 1;\n" //
				+ "                }\n" //
				+ "            } else if (o2 != null) {\n" //
				+ "                return -1;\n" //
				+ "            } else {\n" //
				+ "                return 100;\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<String> doNotReplaceLambdaByUseMethodRef(List<String> listToSort) {\n" //
				+ "        Comparator<String> comparator = (o1, o2) -> o1.toLowerCase(Locale.ENGLISH).compareTo(o2.toLowerCase(Locale.ENGLISH));\n" //
				+ "        Collections.sort(listToSort, comparator);\n" //
				+ "\n" //
				+ "        return listToSort;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Comparator<Date> doNotReplaceLambdaByRefactorComparisonWithoutCompareToMethod(List<Date> listToSort) {\n" //
				+ "        Comparator<Date> comparator = (o1, o2) -> (int) (o1.getTime() - o2.getTime());\n" //
				+ "\n" //
				+ "        return comparator;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Comparator<Date> doNotReplaceLambdaByRemoveSecondaryMethod(List<Date> listToSort) {\n" //
				+ "        Comparator<Date> comparator = new Comparator<Date>() {\n" //
				+ "\n" //
				+ "            @Override\n" //
				+ "            public int compare(Date o1, Date o2) {\n" //
				+ "                return o1.toString().compareTo(o2.toString());\n" //
				+ "            }\n" //
				+ "\n" //
				+ "            @Override\n" //
				+ "            public String toString() {\n" //
				+ "                return \"Compare formatted dates\";\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "\n" //
				+ "        return comparator;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.COMPARING_ON_CRITERIA);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testJoin() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String input= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public String refactorConcatenation(String[] texts) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        // Keep this comment too\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        // Keep this comment also\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                isFirst = false;\n" //
				+ "            } else {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorReassignment(String[] texts) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (String text : texts) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                isFirst = false;\n" //
				+ "            } else {\n" //
				+ "                concatenation = concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation = concatenation.append(text);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Runnable refactorFinalConcatenation(String[] names) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        final StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (int i = 0; i < names.length; i++) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                isFirst = false;\n" //
				+ "            } else {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(names[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        Runnable supplier= new Runnable() {\n" //
				+ "            @Override\n" //
				+ "            public void run() {\n" //
				+ "                System.out.println(concatenation.toString());\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "        return supplier;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithChar(String[] titles) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (String title : titles) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                isFirst = false;\n" //
				+ "            } else {\n" //
				+ "                concatenation.append(',');\n" //
				+ "            }\n" //
				+ "            concatenation.append(title);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithHardCodedDelimiter(String[] texts) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                isFirst = false;\n" //
				+ "            } else {\n" //
				+ "                concatenation = concatenation.append(\" \" + 1);\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithBuilderFirst(String[] texts) {\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "        boolean isFirst = true;\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                isFirst = false;\n" //
				+ "            } else {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithStringBuffer(String[] texts) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuffer concatenation = new StringBuffer();\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                isFirst = false;\n" //
				+ "            } else {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithBooleanObject(String[] texts) {\n" //
				+ "        Boolean isFirst = Boolean.TRUE;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                isFirst = Boolean.FALSE;\n" //
				+ "            } else {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation = concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithNegatedBoolean(String[] texts) {\n" //
				+ "        Boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (!isFirst) {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            } else {\n" //
				+ "                isFirst = false;\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithReversedBoolean(String[] texts) {\n" //
				+ "        boolean isVisited = Boolean.FALSE;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (isVisited) {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            } else {\n" //
				+ "                isVisited = Boolean.TRUE;\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithLotsOfMethods(String[] texts) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                isFirst = false;\n" //
				+ "            } else {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        System.out.println(concatenation.charAt(0));\n" //
				+ "        System.out.println(concatenation.chars());\n" //
				+ "        System.out.println(concatenation.codePoints());\n" //
				+ "        System.out.println(concatenation.indexOf(\"foo\", 0));\n" //
				+ "        System.out.println(concatenation.lastIndexOf(\"foo\"));\n" //
				+ "        System.out.println(concatenation.lastIndexOf(\"foo\", 0));\n" //
				+ "        System.out.println(concatenation.length());\n" //
				+ "        System.out.println(concatenation.subSequence(0, 0));\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationOnForeach(String[] texts) {\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "        boolean isFirst = true;\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (String text : texts) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                isFirst = false;\n" //
				+ "            } else {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(text);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithConditionOnIndex(String[] texts) {\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (i > 0) {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithInequalityOnIndex(String[] texts) {\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (i != 0) {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithReversedConditionOnIndex(String[] texts) {\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (0 < i) {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithGreaterOrEqualsOnIndex(String[] texts) {\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (i >= 1) {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithDelimiterAtTheEnd(String[] texts) {\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "            if (i < texts.length - 1) {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithMirroredCondition(String[] texts) {\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "            if (texts.length - 1 > i) {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithNotEqualsCondition(String[] texts) {\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "            if (i < texts.length - 1) {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithLessOrEqualsCondition(String[] texts) {\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "            if (i <= texts.length - 2) {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationTestingLength(String[] texts) {\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (concatenation.length() > 0) {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationTestingNotEmpty(String[] texts) {\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (concatenation.length() != 0) {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationTestingGreaterOrEqualsOne(String[] texts) {\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (concatenation.length() >= 1) {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationTestingLengthMirrored(String[] texts) {\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (0 < concatenation.length()) {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationTestingNotEmptyMirrored(String[] texts) {\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (0 != concatenation.length()) {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationTestingGreaterOrEqualsOneMirrored(String[] texts) {\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (1 <= concatenation.length()) {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConstantBooleanShift(String[] texts) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (String text : texts) {\n" //
				+ "            if (!isFirst) {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            isFirst = false;\n" //
				+ "            concatenation.append(text);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorWithBooleanShiftAtTheEnd(String[] texts) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (String text : texts) {\n" //
				+ "            if (!isFirst) {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(text);\n" //
				+ "            isFirst = false;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorWithReversedBooleanShift(String[] texts) {\n" //
				+ "        boolean isNotFirst = false;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (String text : texts) {\n" //
				+ "            if (isNotFirst) {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(text);\n" //
				+ "            isNotFirst = true;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", input, false, null);

		enable(CleanUpConstants.JOIN);

		String output= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public String refactorConcatenation(String[] texts) {\n" //
				+ "        // Keep this comment\n" //
				+ "        \n" //
				+ "        // Keep this comment too\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment also\n" //
				+ "        String concatenation = String.join(\", \", texts);\n" //
				+ "\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorReassignment(String[] texts) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        String concatenation = String.join(\", \", texts);\n" //
				+ "\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Runnable refactorFinalConcatenation(String[] names) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        final String concatenation = String.join(\", \", names);\n" //
				+ "\n" //
				+ "        Runnable supplier= new Runnable() {\n" //
				+ "            @Override\n" //
				+ "            public void run() {\n" //
				+ "                System.out.println(concatenation);\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "        return supplier;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithChar(String[] titles) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        String concatenation = String.join(String.valueOf(','), titles);\n" //
				+ "\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithHardCodedDelimiter(String[] texts) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        String concatenation = String.join(\" \" + 1, texts);\n" //
				+ "\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithBuilderFirst(String[] texts) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        String concatenation = String.join(\", \", texts);\n" //
				+ "\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithStringBuffer(String[] texts) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        String concatenation = String.join(\", \", texts);\n" //
				+ "\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithBooleanObject(String[] texts) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        String concatenation = String.join(\", \", texts);\n" //
				+ "\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithNegatedBoolean(String[] texts) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        String concatenation = String.join(\", \", texts);\n" //
				+ "\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithReversedBoolean(String[] texts) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        String concatenation = String.join(\", \", texts);\n" //
				+ "\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithLotsOfMethods(String[] texts) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        String concatenation = String.join(\", \", texts);\n" //
				+ "\n" //
				+ "        System.out.println(concatenation.charAt(0));\n" //
				+ "        System.out.println(concatenation.chars());\n" //
				+ "        System.out.println(concatenation.codePoints());\n" //
				+ "        System.out.println(concatenation.indexOf(\"foo\", 0));\n" //
				+ "        System.out.println(concatenation.lastIndexOf(\"foo\"));\n" //
				+ "        System.out.println(concatenation.lastIndexOf(\"foo\", 0));\n" //
				+ "        System.out.println(concatenation.length());\n" //
				+ "        System.out.println(concatenation.subSequence(0, 0));\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationOnForeach(String[] texts) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        String concatenation = String.join(\", \", texts);\n" //
				+ "\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithConditionOnIndex(String[] texts) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        String concatenation = String.join(\", \", texts);\n" //
				+ "\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithInequalityOnIndex(String[] texts) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        String concatenation = String.join(\", \", texts);\n" //
				+ "\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithReversedConditionOnIndex(String[] texts) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        String concatenation = String.join(\", \", texts);\n" //
				+ "\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithGreaterOrEqualsOnIndex(String[] texts) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        String concatenation = String.join(\", \", texts);\n" //
				+ "\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithDelimiterAtTheEnd(String[] texts) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        String concatenation = String.join(\", \", texts);\n" //
				+ "\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithMirroredCondition(String[] texts) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        String concatenation = String.join(\", \", texts);\n" //
				+ "\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithNotEqualsCondition(String[] texts) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        String concatenation = String.join(\", \", texts);\n" //
				+ "\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationWithLessOrEqualsCondition(String[] texts) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        String concatenation = String.join(\", \", texts);\n" //
				+ "\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationTestingLength(String[] texts) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        String concatenation = String.join(\", \", texts);\n" //
				+ "\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationTestingNotEmpty(String[] texts) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        String concatenation = String.join(\", \", texts);\n" //
				+ "\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationTestingGreaterOrEqualsOne(String[] texts) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        String concatenation = String.join(\", \", texts);\n" //
				+ "\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationTestingLengthMirrored(String[] texts) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        String concatenation = String.join(\", \", texts);\n" //
				+ "\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationTestingNotEmptyMirrored(String[] texts) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        String concatenation = String.join(\", \", texts);\n" //
				+ "\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConcatenationTestingGreaterOrEqualsOneMirrored(String[] texts) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        String concatenation = String.join(\", \", texts);\n" //
				+ "\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConstantBooleanShift(String[] texts) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        String concatenation = String.join(\", \", texts);\n" //
				+ "\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorWithBooleanShiftAtTheEnd(String[] texts) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        String concatenation = String.join(\", \", texts);\n" //
				+ "\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorWithReversedBooleanShift(String[] texts) {\n" //
				+ "        \n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        String concatenation = String.join(\", \", texts);\n" //
				+ "\n" //
				+ "        return concatenation;\n" //
				+ "    }\n" //
				+ "}\n";

		assertNotEquals("The class must be changed", input, output);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { output },
				new HashSet<>(Arrays.asList(MultiFixMessages.JoinCleanup_description)));
	}

	@Test
	public void testDoNotJoin() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public boolean doNotRefactorUsedBoolean(String[] texts) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                isFirst = false;\n" //
				+ "            } else {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        System.out.println(concatenation.toString());\n" //
				+ "        return isFirst;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotRefactorUnhandledMethod(String[] texts) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                isFirst = false;\n" //
				+ "            } else {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        System.out.println(concatenation.codePointAt(0));\n" //
				+ "        System.out.println(concatenation.codePointBefore(0));\n" //
				+ "        System.out.println(concatenation.codePointCount(0, 0));\n" //
				+ "        concatenation.getChars(0, 0, new char[0], 0);\n" //
				+ "        System.out.println(concatenation.indexOf(\"foo\"));\n" //
				+ "        System.out.println(concatenation.offsetByCodePoints(0, 0));\n" //
				+ "        System.out.println(concatenation.substring(0));\n" //
				+ "        System.out.println(concatenation.substring(0, 0));\n" //
				+ "        System.out.println(concatenation.capacity());\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotRefactorPartialConcatenation(String[] texts) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        for (int i = 1; i < texts.length; i++) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                isFirst = false;\n" //
				+ "            } else {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotRefactorUnfinishedConcatenation(String[] texts) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        for (int i = 0; i < texts.length - 1; i++) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                isFirst = false;\n" //
				+ "            } else {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotRefactorReversedConcatenation(String[] texts) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        for (int i = texts.length - 1; i >= 0; i--) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                isFirst = false;\n" //
				+ "            } else {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotRefactorWithOppositeBoolean(String[] texts) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        for (int i = 1; i < texts.length; i++) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            } else {\n" //
				+ "                isFirst = false;\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotRefactorOnObjects(Object[] texts) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                isFirst = false;\n" //
				+ "            } else {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotRefactorWithOtherAppending(String[] texts) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                isFirst = false;\n" //
				+ "            } else {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        concatenation.append(\"foo\");\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotRefactorWithInitialization(String[] texts) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder(\"foo\");\n" //
				+ "\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                isFirst = false;\n" //
				+ "            } else {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotRefactorWithWrongIndex(String[] texts) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                isFirst = false;\n" //
				+ "            } else {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[0]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotRefactorWithWrongBoolean(String[] texts, boolean isSecond) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (isSecond) {\n" //
				+ "                isFirst = false;\n" //
				+ "            } else {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotRefactorWithWrongBoolean(String[] texts) {\n" //
				+ "        boolean isSecond = false;\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                isSecond = false;\n" //
				+ "            } else {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotRefactorWithWrongArray(String[] texts, String[] names) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                isFirst = false;\n" //
				+ "            } else {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(names[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotRefactorWithWrongBuilder(String[] texts, StringBuilder otherBuilder) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                isFirst = false;\n" //
				+ "            } else {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            otherBuilder.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotRefactorWithAnotherBuilder(String[] texts, StringBuilder otherBuilder) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                isFirst = false;\n" //
				+ "            } else {\n" //
				+ "                otherBuilder.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotRefactorWithAdditionalStatement(String[] texts) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                isFirst = false;\n" //
				+ "            } else {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i]);\n" //
				+ "            System.out.println(\"Hi!\");\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotRefactorWithWrongMethod(String[] texts) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        for (int i = 0; i < texts.length; i++) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                isFirst = false;\n" //
				+ "            } else {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(texts[i], 0, 2);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotRefactorWrongVariable(String[] texts, String test) {\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "        boolean isFirst = true;\n" //
				+ "\n" //
				+ "        for (String text : texts) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                isFirst = false;\n" //
				+ "            } else {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(test);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotRefactorWithBooleanShiftFirst(String[] texts) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        for (String text : texts) {\n" //
				+ "            isFirst = false;\n" //
				+ "            if (!isFirst) {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            concatenation.append(text);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotRefactorWithAppendingFirst(String[] texts) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        for (String text : texts) {\n" //
				+ "            concatenation.append(text);\n" //
				+ "            if (!isFirst) {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            isFirst = false;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotRefactorWithConditionAtTheEnd(String[] texts) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        for (String text : texts) {\n" //
				+ "            concatenation.append(text);\n" //
				+ "            isFirst = false;\n" //
				+ "            if (!isFirst) {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotRefactorWithNonsense(String[] texts) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        for (String text : texts) {\n" //
				+ "            isFirst = false;\n" //
				+ "            concatenation.append(text);\n" //
				+ "            if (!isFirst) {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotRefactorUnshiftedBoolean(String[] texts) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        for (String text : texts) {\n" //
				+ "            if (!isFirst) {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            isFirst = true;\n" //
				+ "            concatenation.append(text);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotRefactorWrongCondition(String[] texts) {\n" //
				+ "        boolean isFirst = true;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        for (String text : texts) {\n" //
				+ "            if (isFirst) {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            isFirst = false;\n" //
				+ "            concatenation.append(text);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotRefactorWrongInit(String[] texts) {\n" //
				+ "        boolean isFirst = false;\n" //
				+ "        StringBuilder concatenation = new StringBuilder();\n" //
				+ "\n" //
				+ "        for (String text : texts) {\n" //
				+ "            if (!isFirst) {\n" //
				+ "                concatenation.append(\", \");\n" //
				+ "            }\n" //
				+ "            isFirst = false;\n" //
				+ "            concatenation.append(text);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return concatenation.toString();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.JOIN);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testStringBufferToStringBuilderLocalsOnly() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample0= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class SuperClass {\n" //
				+ "    public StringBuffer field0;\n" //
				+ "    public void method0(StringBuffer parm) {\n" //
				+ "        System.out.println(parm.toString());\n" //
				+ "    }\n" //
				+ "}";
		ICompilationUnit cu0= pack1.createCompilationUnit("SuperClass.java", sample0, false, null);

		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class TestStringBuilderCleanup extends SuperClass {\n" //
				+ "    StringBuffer field1;\n" //
				+ "    StringBuffer field2;\n" //
				+ "    public void changeLambda(StringBuffer parm) {\n" //
				+ "        Runnable r = () -> {\n" //
				+ "            StringBuffer a = new StringBuffer();\n" //
				+ "            field1 = field2;\n" //
				+ "            super.field0 = parm;\n" //
				+ "            super.method0(parm);\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("TestStringBuilderCleanup.java", sample, false, null);

		enable(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER);
		enable(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER_FOR_LOCALS);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class TestStringBuilderCleanup extends SuperClass {\n" //
				+ "    StringBuffer field1;\n" //
				+ "    StringBuffer field2;\n" //
				+ "    public void changeLambda(StringBuffer parm) {\n" //
				+ "        Runnable r = () -> {\n" //
				+ "            StringBuilder a = new StringBuilder();\n" //
				+ "            field1 = field2;\n" //
				+ "            super.field0 = parm;\n" //
				+ "            super.method0(parm);\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu0, cu1 }, new String[] { sample0, expected1 }, null);
	}

	@Test
	public void testDoNotChangeStringBufferToStringBuilderLocalsOnly() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample0= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class SuperClass {\n" //
				+ "    public StringBuffer field0;\n" //
				+ "    public void method0(StringBuffer parm) {\n" //
				+ "        System.out.println(parm.toString());\n" //
				+ "    }\n" //
				+ "}";
		ICompilationUnit cu0= pack1.createCompilationUnit("SuperClass.java", sample0, false, null);

		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.io.StringWriter;\n" //
				+ "\n" //
				+ "public class TestStringBuilderCleanup extends SuperClass {\n" //
				+ "    StringBuffer field1;\n" //
				+ "    StringBuffer field2;\n" //
				+ "    public void doNotChangeLambdaWithFieldAssignment() {\n" //
				+ "        Runnable r = () -> {\n" //
				+ "            StringBuffer a = new StringBuffer();\n" //
				+ "            a = field1;\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "    public void doNotChangeLambdaWithParmAssignment(StringBuffer parm) {\n" //
				+ "        Runnable r = () -> {\n" //
				+ "            StringBuffer a = new StringBuffer();\n" //
				+ "            a = parm;\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "    public void doNotChangeLambdaWithSuperFieldAssignment(StringBuffer parm) {\n" //
				+ "        Runnable r = () -> {\n" //
				+ "            StringBuffer a = new StringBuffer();\n" //
				+ "            a = super.field0;\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "    public void doNotChangeLambdaWithMethodCall(StringBuffer parm) {\n" //
				+ "        Runnable r = () -> {\n" //
				+ "            StringBuffer a = new StringBuffer();\n" //
				+ "            doNotChangeLambdaWithSuperFieldAssignment(a);\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "    public void doNotChangeLambdaWithSuperMethodCall(StringBuffer parm) {\n" //
				+ "        Runnable r = () -> {\n" //
				+ "            StringBuffer a = new StringBuffer();\n" //
				+ "            super.method0(a);\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "    public void doNotChangeConstructorCall() {\n" //
				+ "        StringBuffer a = new StringBuffer();\n" //
				+ "        new Helper(a);\n" //
				+ "    }\n"
				+ "    private class Helper {\n" //
				+ "    	   public Helper(StringBuffer b) {\n" //
				+ "	           System.out.println(b.toString()); \n" //
				+ "   	   }\n"
				+ "    }\n"
				+ "    public void doNotChangeIfBufferIsAssigned() {\n" //
				+ "        StringWriter stringWriter = new StringWriter();\n"
				+ "	       StringBuffer buffer = stringWriter.getBuffer();"
				+ "    }\n"
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("TestStringBuilderCleanup.java", sample, false, null);

		enable(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER);
		enable(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER_FOR_LOCALS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu0, cu1 });
	}

	@Test
	public void testChangeStringBufferToStringBuilderAll() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample0= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class SuperClass {\n" //
				+ "    public StringBuffer field0;\n" //
				+ "    public void method0(StringBuffer parm) {\n" //
				+ "        System.out.println(parm.toString());\n" //
				+ "    }\n" //
				+ "}";
		ICompilationUnit cu0= pack1.createCompilationUnit("SuperClass.java", sample0, false, null);

		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class TestStringBuilderCleanup extends SuperClass {\n" //
				+ "    StringBuffer field1;\n" //
				+ "    StringBuffer field2;\n" //
				+ "    public void changeLambdaWithFieldAssignment() {\n" //
				+ "        Runnable r = () -> {\n" //
				+ "            StringBuffer a = new StringBuffer();\n" //
				+ "            a = field1;\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "    public void changeLambdaWithParmAssignment(StringBuffer parm) {\n" //
				+ "        Runnable r = () -> {\n" //
				+ "            StringBuffer a = new StringBuffer();\n" //
				+ "            a = parm;\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "    public void changeLambdaWithSuperFieldAssignment(StringBuffer parm) {\n" //
				+ "        Runnable r = () -> {\n" //
				+ "            StringBuffer a = new StringBuffer();\n" //
				+ "            a = super.field0;\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "    public void changeLambdaWithMethodCall(StringBuffer parm) {\n" //
				+ "        Runnable r = () -> {\n" //
				+ "            StringBuffer a = new StringBuffer();\n" //
				+ "            changeLambdaWithSuperFieldAssignment(a);\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "    public void changeLambdaWithSuperMethodCall(StringBuffer parm) {\n" //
				+ "        Runnable r = () -> {\n" //
				+ "            StringBuffer a = new StringBuffer();\n" //
				+ "            super.method0(a);\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("TestStringBuilderCleanup.java", sample, false, null);

		enable(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER);
		disable(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER_FOR_LOCALS);

		String expected0= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class SuperClass {\n" //
				+ "    public StringBuilder field0;\n" //
				+ "    public void method0(StringBuilder parm) {\n" //
				+ "        System.out.println(parm.toString());\n" //
				+ "    }\n" //
				+ "}";

		String expected1= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class TestStringBuilderCleanup extends SuperClass {\n" //
				+ "    StringBuilder field1;\n" //
				+ "    StringBuilder field2;\n" //
				+ "    public void changeLambdaWithFieldAssignment() {\n" //
				+ "        Runnable r = () -> {\n" //
				+ "            StringBuilder a = new StringBuilder();\n" //
				+ "            a = field1;\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "    public void changeLambdaWithParmAssignment(StringBuilder parm) {\n" //
				+ "        Runnable r = () -> {\n" //
				+ "            StringBuilder a = new StringBuilder();\n" //
				+ "            a = parm;\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "    public void changeLambdaWithSuperFieldAssignment(StringBuilder parm) {\n" //
				+ "        Runnable r = () -> {\n" //
				+ "            StringBuilder a = new StringBuilder();\n" //
				+ "            a = super.field0;\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "    public void changeLambdaWithMethodCall(StringBuilder parm) {\n" //
				+ "        Runnable r = () -> {\n" //
				+ "            StringBuilder a = new StringBuilder();\n" //
				+ "            changeLambdaWithSuperFieldAssignment(a);\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "    public void changeLambdaWithSuperMethodCall(StringBuilder parm) {\n" //
				+ "        Runnable r = () -> {\n" //
				+ "            StringBuilder a = new StringBuilder();\n" //
				+ "            super.method0(a);\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu0, cu1 }, new String[] { expected0, expected1 },
				new HashSet<>(Arrays.asList(MultiFixMessages.StringBufferToStringBuilderCleanUp_description)));
	}

}

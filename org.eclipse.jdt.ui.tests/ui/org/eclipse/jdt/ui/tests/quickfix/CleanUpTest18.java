/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.tests.core.Java18ProjectTestSetup;

import junit.framework.Test;
import junit.framework.TestSuite;

public class CleanUpTest18 extends CleanUpTestCase {

	private static final Class<CleanUpTest18> THIS= CleanUpTest18.class;

	public CleanUpTest18(String name) {
		super(name);
	}

	public static Test suite() {
		return setUpTest(new TestSuite(THIS));
	}
	
	public static Test setUpTest(Test test) {
		return new Java18ProjectTestSetup(test);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		StubUtility.setCodeTemplate(CodeTemplateContextType.OVERRIDECOMMENT_ID, "", null);
	}

	@Override
	protected IJavaProject getProject() {
		return Java18ProjectTestSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return Java18ProjectTestSetup.getDefaultClasspath();
	}

	public void testConvertToLambda01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(){\n");
		buf.append("        Runnable r = new Runnable() {\n");
		buf.append("            @Override\n");
		buf.append("            public void run() {\n");
		buf.append("                System.out.println(\"do something\");\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    };\n");
		buf.append("}\n");
		String original= buf.toString();
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(){\n");
		buf.append("        Runnable r = () -> System.out.println(\"do something\");\n");
		buf.append("    };\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
		
		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);
		
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original });
	}

	public void testConvertToLambda02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(){\n");
		buf.append("        Runnable r1 = new Runnable() {\n");
		buf.append("            @Override\n");
		buf.append("            public void run() {\n");
		buf.append("                System.out.println(\"do something\");\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("        Runnable r2 = new Runnable() {\n");
		buf.append("            @Override\n");
		buf.append("            public void run() {\n");
		buf.append("                System.out.println(\"do one thing\");\n");
		buf.append("                System.out.println(\"do another thing\");\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    };\n");
		buf.append("}\n");
		String original= buf.toString();
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(){\n");
		buf.append("        Runnable r1 = () -> System.out.println(\"do something\");\n");
		buf.append("        Runnable r2 = () -> {\n");
		buf.append("            System.out.println(\"do one thing\");\n");
		buf.append("            System.out.println(\"do another thing\");\n");
		buf.append("        };\n");
		buf.append("    };\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
		
		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);
		
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original });
	}

	public void testConvertToLambda03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.function.Supplier;\n");
		buf.append("class E {\n");
		buf.append("    Supplier<Supplier<String>> s= new Supplier<Supplier<String>>() {\n");
		buf.append("        @Override\n");
		buf.append("        public Supplier<String> get() {\n");
		buf.append("            return new Supplier<String>() {\n");
		buf.append("                @Override\n");
		buf.append("                public String get() {\n");
		buf.append("                    return \"a\";\n");
		buf.append("                }\n");
		buf.append("            };\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("}\n");
		String original= buf.toString();
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.function.Supplier;\n");
		buf.append("class E {\n");
		buf.append("    Supplier<Supplier<String>> s= () -> () -> \"a\";\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });

		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original });
	}

	public void testConvertToLambdaNestedWithImports() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("import java.util.concurrent.Callable;\n");
		buf.append("import java.util.concurrent.Executors;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        new Thread(new Runnable() {\n");
		buf.append("            @Override\n");
		buf.append("            public void run() {\n");
		buf.append("                Executors.newSingleThreadExecutor().submit(new Callable<String>() {\n");
		buf.append("                    @Override\n");
		buf.append("                    public String call() throws Exception {\n");
		buf.append("                        return \"hi\";\n");
		buf.append("                    }\n");
		buf.append("                });\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("}\n");
		String original= buf.toString();
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);
		
		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);
		
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("import java.util.concurrent.Executors;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        new Thread(() -> Executors.newSingleThreadExecutor().submit(() -> \"hi\"));\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
		
		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);
		
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original });
	}

	// fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=434507#c5
	public void testConvertToLambdaAmbiguous01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("\n");
		buf.append("interface ISuper {\n");
		buf.append("    void foo(FI1 fi1);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface ISub extends ISuper {\n");
		buf.append("    void foo(FI2 fi2);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI1 {\n");
		buf.append("    void abc();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI2 {\n");
		buf.append("    void xyz();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Test1 {\n");
		buf.append("    private void test1() {\n");
		buf.append("        f1().foo(new FI1() {\n");
		buf.append("            @Override\n");
		buf.append("            public void abc() {\n");
		buf.append("                System.out.println();\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    private ISub f1() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("abstract class Test2 implements ISub {\n");
		buf.append("    private void test2() {\n");
		buf.append("        foo(new FI1() {\n");
		buf.append("            @Override\n");
		buf.append("            public void abc() {\n");
		buf.append("                System.out.println();\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Test3 {\n");
		buf.append("    void foo(FI1 fi1) {}\n");
		buf.append("    void foo(FI2 fi2) {}\n");
		buf.append("    private void test3() {\n");
		buf.append("        foo(new FI1() {\n");
		buf.append("            @Override\n");
		buf.append("            public void abc() {\n");
		buf.append("                System.out.println();\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Outer {\n");
		buf.append("    class Test4 {\n");
		buf.append("        {\n");
		buf.append("            bar(0, new FI1() {\n");
		buf.append("                @Override\n");
		buf.append("                public void abc() {\n");
		buf.append("                }\n");
		buf.append("            });\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    void bar(int i, FI1 fi1) {}\n");
		buf.append("    void bar(int s, FI2 fi2) {}\n");
		buf.append("}\n");
		String original= buf.toString();
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("\n");
		buf.append("interface ISuper {\n");
		buf.append("    void foo(FI1 fi1);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface ISub extends ISuper {\n");
		buf.append("    void foo(FI2 fi2);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI1 {\n");
		buf.append("    void abc();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI2 {\n");
		buf.append("    void xyz();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Test1 {\n");
		buf.append("    private void test1() {\n");
		buf.append("        f1().foo((FI1) () -> System.out.println());\n");
		buf.append("\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    private ISub f1() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("abstract class Test2 implements ISub {\n");
		buf.append("    private void test2() {\n");
		buf.append("        foo((FI1) () -> System.out.println());\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Test3 {\n");
		buf.append("    void foo(FI1 fi1) {}\n");
		buf.append("    void foo(FI2 fi2) {}\n");
		buf.append("    private void test3() {\n");
		buf.append("        foo((FI1) () -> System.out.println());\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Outer {\n");
		buf.append("    class Test4 {\n");
		buf.append("        {\n");
		buf.append("            bar(0, (FI1) () -> {\n");
		buf.append("            });\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    void bar(int i, FI1 fi1) {}\n");
		buf.append("    void bar(int s, FI2 fi2) {}\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
		
		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);
		
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original });
	}

	// fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=434507#c5
	public void testConvertToLambdaAmbiguous02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI1 {\n");
		buf.append("    void abc();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI2 {\n");
		buf.append("    void xyz();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Outer {\n");
		buf.append("    void outer(FI1 fi1) {}\n");
		buf.append("}\n");
		buf.append("class OuterSub extends Outer {\n");
		buf.append("    OuterSub() {\n");
		buf.append("        super.outer(new FI1() {\n");
		buf.append("            @Override\n");
		buf.append("            public void abc() {\n");
		buf.append("                System.out.println();\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("    class Test1 {\n");
		buf.append("        private void test1() {\n");
		buf.append("            OuterSub.super.outer(new FI1() {\n");
		buf.append("                @Override\n");
		buf.append("                public void abc() {\n");
		buf.append("                    System.out.println();\n");
		buf.append("                }\n");
		buf.append("            });\n");
		buf.append("            OuterSub.this.outer(new FI1() {\n");
		buf.append("                @Override\n");
		buf.append("                public void abc() {\n");
		buf.append("                    System.out.println();\n");
		buf.append("                }\n");
		buf.append("            });\n");
		buf.append("            outer(new FI1() {\n");
		buf.append("                @Override\n");
		buf.append("                public void abc() {\n");
		buf.append("                    System.out.println();\n");
		buf.append("                }\n");
		buf.append("            });\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    @Override\n");
		buf.append("    void outer(FI1 fi1) {}\n");
		buf.append("    void outer(FI2 fi2) {}\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class OuterSub2 extends OuterSub {\n");
		buf.append("    OuterSub2() {\n");
		buf.append("        super.outer(new FI1() {\n");
		buf.append("            @Override\n");
		buf.append("            public void abc() {\n");
		buf.append("                System.out.println();\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("    class Test2 {\n");
		buf.append("        private void test2() {\n");
		buf.append("            OuterSub2.super.outer(new FI1() {\n");
		buf.append("                @Override\n");
		buf.append("                public void abc() {\n");
		buf.append("                    System.out.println();\n");
		buf.append("                }\n");
		buf.append("            });\n");
		buf.append("            OuterSub2.this.outer(new FI1() {\n");
		buf.append("                @Override\n");
		buf.append("                public void abc() {\n");
		buf.append("                    System.out.println();\n");
		buf.append("                }\n");
		buf.append("            });\n");
		buf.append("            outer(new FI1() {\n");
		buf.append("                @Override\n");
		buf.append("                public void abc() {\n");
		buf.append("                    System.out.println();\n");
		buf.append("                }\n");
		buf.append("            });\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String original= buf.toString();
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);
	
		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);
	
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI1 {\n");
		buf.append("    void abc();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI2 {\n");
		buf.append("    void xyz();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Outer {\n");
		buf.append("    void outer(FI1 fi1) {}\n");
		buf.append("}\n");
		buf.append("class OuterSub extends Outer {\n");
		buf.append("    OuterSub() {\n");
		buf.append("        super.outer(() -> System.out.println());\n");
		buf.append("    }\n");
		buf.append("    class Test1 {\n");
		buf.append("        private void test1() {\n");
		buf.append("            OuterSub.super.outer(() -> System.out.println());\n");
		buf.append("            OuterSub.this.outer((FI1) () -> System.out.println());\n");
		buf.append("            outer((FI1) () -> System.out.println());\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    @Override\n");
		buf.append("    void outer(FI1 fi1) {}\n");
		buf.append("    void outer(FI2 fi2) {}\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class OuterSub2 extends OuterSub {\n");
		buf.append("    OuterSub2() {\n");
		buf.append("        super.outer((FI1) () -> System.out.println());\n");
		buf.append("    }\n");
		buf.append("    class Test2 {\n");
		buf.append("        private void test2() {\n");
		buf.append("            OuterSub2.super.outer((FI1) () -> System.out.println());\n");
		buf.append("            OuterSub2.this.outer((FI1) () -> System.out.println());\n");
		buf.append("            outer((FI1) () -> System.out.println());\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
	
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
		
		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);
		
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original });
	}

	// fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=434507#c2
	public void testConvertToLambdaAmbiguous03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public interface E {\n");
		buf.append("    default void m() {\n");
		buf.append("        bar(0, new FI() {\n");
		buf.append("            @Override\n");
		buf.append("            public int foo(int x) {\n");
		buf.append("                return x++;\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("        baz(0, new ZI() {\n");
		buf.append("            @Override\n");
		buf.append("            public int zoo() {\n");
		buf.append("                return 1;\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    void bar(int i, FI fi);\n");
		buf.append("    void bar(int i, FV fv);\n");
		buf.append("\n");
		buf.append("    void baz(int i, ZI zi);\n");
		buf.append("    void baz(int i, ZV zv);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface interface FI { int  foo(int a); }\n");
		buf.append("@FunctionalInterface interface FV { void foo(int a); }\n");
		buf.append("\n");
		buf.append("@FunctionalInterface interface ZI { int  zoo(); }\n");
		buf.append("@FunctionalInterface interface ZV { void zoo(); }\n");
		String original= buf.toString();
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);
		
		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);
		
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public interface E {\n");
		buf.append("    default void m() {\n");
		buf.append("        bar(0, (FI) x -> x++);\n");
		buf.append("        baz(0, () -> 1);\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    void bar(int i, FI fi);\n");
		buf.append("    void bar(int i, FV fv);\n");
		buf.append("\n");
		buf.append("    void baz(int i, ZI zi);\n");
		buf.append("    void baz(int i, ZV zv);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface interface FI { int  foo(int a); }\n");
		buf.append("@FunctionalInterface interface FV { void foo(int a); }\n");
		buf.append("\n");
		buf.append("@FunctionalInterface interface ZI { int  zoo(); }\n");
		buf.append("@FunctionalInterface interface ZV { void zoo(); }\n");
		String expected1= buf.toString();
		
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
		
		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);
		
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original });
	}

	public void testConvertToLambdaConflictingNames() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface FI {\n");
		buf.append("    void run(int x);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class Test {\n");
		buf.append("    {\n");
		buf.append("        int e;\n");
		buf.append("        FI fi = new FI() {\n");
		buf.append("            @Override\n");
		buf.append("            public void run(int e) {\n");
		buf.append("                class C1 {\n");
		buf.append("                    void init1() {\n");
		buf.append("                        m(new FI() {\n");
		buf.append("                            @Override\n");
		buf.append("                            public void run(int e) {\n");
		buf.append("                                FI fi = new FI() {\n");
		buf.append("                                    @Override\n");
		buf.append("                                    public void run(int e) {\n");
		buf.append("                                        FI fi = new FI() {\n");
		buf.append("                                            @Override\n");
		buf.append("                                            public void run(int e) {\n");
		buf.append("                                                return;\n");
		buf.append("                                            }\n");
		buf.append("                                        };\n");
		buf.append("                                    }\n");
		buf.append("                                };\n");
		buf.append("                            }\n");
		buf.append("                        });\n");
		buf.append("                    }\n");
		buf.append("\n");
		buf.append("                    void init2() {\n");
		buf.append("                        m(new FI() {\n");
		buf.append("                            @Override\n");
		buf.append("                            public void run(int e) {\n");
		buf.append("                                new FI() {\n");
		buf.append("                                    @Override\n");
		buf.append("                                    public void run(int e3) {\n");
		buf.append("                                        FI fi = new FI() {\n");
		buf.append("                                            @Override\n");
		buf.append("                                            public void run(int e) {\n");
		buf.append("                                                return;\n");
		buf.append("                                            }\n");
		buf.append("                                        };\n");
		buf.append("                                    }\n");
		buf.append("                                };\n");
		buf.append("                            }\n");
		buf.append("                        });\n");
		buf.append("                    }\n");
		buf.append("                }\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    void m(FI fi) {\n");
		buf.append("    };\n");
		buf.append("}\n");
		String original= buf.toString();
		ICompilationUnit cu1= pack1.createCompilationUnit("Test.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface FI {\n");
		buf.append("    void run(int x);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class Test {\n");
		buf.append("    {\n");
		buf.append("        int e;\n");
		buf.append("        FI fi = e4 -> {\n");
		buf.append("            class C1 {\n");
		buf.append("                void init1() {\n");
		buf.append("                    m(e3 -> {\n");
		buf.append("                        FI fi2 = e2 -> {\n");
		buf.append("                            FI fi1 = e1 -> {\n");
		buf.append("                                return;\n");
		buf.append("                            };\n");
		buf.append("                        };\n");
		buf.append("                    });\n");
		buf.append("                }\n");
		buf.append("\n");
		buf.append("                void init2() {\n");
		buf.append("                    m(e2 -> new FI() {\n");
		buf.append("                        @Override\n");
		buf.append("                        public void run(int e3) {\n");
		buf.append("                            FI fi = e1 -> {\n");
		buf.append("                                return;\n");
		buf.append("                            };\n");
		buf.append("                        }\n");
		buf.append("                    });\n");
		buf.append("                }\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    void m(FI fi) {\n");
		buf.append("    };\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testConvertToLambdaWithMethodAnnotations() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class C1 {\n");
		buf.append("    Runnable r1 = new Runnable() {\n");
		buf.append("        @Override @A @Deprecated\n");
		buf.append("        public void run() {\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("    Runnable r2 = new Runnable() {\n");
		buf.append("        @Override @Deprecated\n");
		buf.append("        public void run() {\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("}\n");
		buf.append("@interface A {}\n");
		String original= buf.toString();
		ICompilationUnit cu1= pack1.createCompilationUnit("C1.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class C1 {\n");
		buf.append("    Runnable r1 = new Runnable() {\n");
		buf.append("        @Override @A @Deprecated\n");
		buf.append("        public void run() {\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("    Runnable r2 = () -> {\n");
		buf.append("    };\n");
		buf.append("}\n");
		buf.append("@interface A {}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testConvertToAnonymousWithWildcards() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("import java.util.*;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Integer[] ints){\n");
		buf.append("        Arrays.sort(ints, (i1, i2) -> i1 - i2);\n");
		buf.append("        Comparator<?> cw = (w1, w2) -> 0;\n");
		buf.append("        Comparator cr = (r1, r2) -> 0;\n");
		buf.append("        Comparator<? extends Number> ce = (n1, n2) -> -0;\n");
		buf.append("    };\n");
		buf.append("}\n");
		String original= buf.toString();
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);
	
		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);
	
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("import java.util.*;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Integer[] ints){\n");
		buf.append("        Arrays.sort(ints, new Comparator<Integer>() {\n");
		buf.append("            @Override\n");
		buf.append("            public int compare(Integer i1, Integer i2) {\n");
		buf.append("                return i1 - i2;\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("        Comparator<?> cw = new Comparator<Object>() {\n");
		buf.append("            @Override\n");
		buf.append("            public int compare(Object w1, Object w2) {\n");
		buf.append("                return 0;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("        Comparator cr = new Comparator() {\n");
		buf.append("            @Override\n");
		buf.append("            public int compare(Object r1, Object r2) {\n");
		buf.append("                return 0;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("        Comparator<? extends Number> ce = new Comparator<Number>() {\n");
		buf.append("            @Override\n");
		buf.append("            public int compare(Number n1, Number n2) {\n");
		buf.append("                return -0;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    };\n");
		buf.append("}\n");
		String expected1= buf.toString();
	
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
		
		disable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);
		enable(CleanUpConstants.USE_LAMBDA);
		
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original });
	}

	public void testConvertToAnonymousWithWildcards1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("\n");
		buf.append("interface I<M> {\n");
		buf.append("    M run(M x);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Test {\n");
		buf.append("    I<?> li = s -> null;\n");
		buf.append("}\n");
		String original= buf.toString();
		ICompilationUnit cu1= pack1.createCompilationUnit("Test.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("\n");
		buf.append("interface I<M> {\n");
		buf.append("    M run(M x);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Test {\n");
		buf.append("    I<?> li = new I<Object>() {\n");
		buf.append("        @Override\n");
		buf.append("        public Object run(Object s) {\n");
		buf.append("            return null;\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });

		disable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);
		enable(CleanUpConstants.USE_LAMBDA);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original });
	}

	public void testConvertToAnonymousWithJoinedSAM() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=428526#c1 and #c6
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("\n");
		buf.append("interface Foo<T, N extends Number> {\n");
		buf.append("    void m(T t);\n");
		buf.append("    void m(N n);\n");
		buf.append("}\n");
		buf.append("interface Baz extends Foo<Integer, Integer> {}\n");
		buf.append("class Test {\n");
		buf.append("    Baz baz = x -> { return; };\n");
		buf.append("}\n");
		String original= buf.toString();
		ICompilationUnit cu1= pack1.createCompilationUnit("Test.java", original, false, null);
	
		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);
	
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("\n");
		buf.append("interface Foo<T, N extends Number> {\n");
		buf.append("    void m(T t);\n");
		buf.append("    void m(N n);\n");
		buf.append("}\n");
		buf.append("interface Baz extends Foo<Integer, Integer> {}\n");
		buf.append("class Test {\n");
		buf.append("    Baz baz = new Baz() {\n");
		buf.append("        @Override\n");
		buf.append("        public void m(Integer x) { return; }\n");
		buf.append("    };\n");
		buf.append("}\n");
		String expected1= buf.toString();
	
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	
		disable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);
		enable(CleanUpConstants.USE_LAMBDA);
	
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original });
	}

	public void testConvertToLambdaWithNonFunctionalTargetType() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=468457
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("\n");
		buf.append("public class Snippet {\n");
		buf.append("    void test(Interface context) {\n");
		buf.append("        context.set(\"bar\", new Runnable() {\n");
		buf.append("            @Override\n");
		buf.append("            public void run() {}\n");
		buf.append("        });\n");
		buf.append("        \n");
		buf.append("    }    \n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface Interface {\n");
		buf.append("    public void set(String name, Object value);\n");
		buf.append("}\n");
		String original= buf.toString();
		ICompilationUnit cu1= pack1.createCompilationUnit("Snippet.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("\n");
		buf.append("public class Snippet {\n");
		buf.append("    void test(Interface context) {\n");
		buf.append("        context.set(\"bar\", (Runnable) () -> {});\n");
		buf.append("        \n");
		buf.append("    }    \n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface Interface {\n");
		buf.append("    public void set(String name, Object value);\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });

		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);
		disable(CleanUpConstants.USE_LAMBDA);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original });
	}

	public void testConvertToLambdaWithSynchronizedOrStrictfp() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class C1 {\n");
		buf.append("    Runnable run1 = new Runnable() {\n");
		buf.append("        @Override\n");
		buf.append("        public synchronized void run() {\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("    Runnable run2 = new Runnable() {\n");
		buf.append("        @Override\n");
		buf.append("        public strictfp void run() {\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("}\n");
		String original= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}
}

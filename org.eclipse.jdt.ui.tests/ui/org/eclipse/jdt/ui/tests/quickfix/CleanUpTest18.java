/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.tests.core.Java18ProjectTestSetup;

public class CleanUpTest18 extends CleanUpTestCase {

	private static final Class THIS= CleanUpTest18.class;

	public CleanUpTest18(String name) {
		super(name);
	}

	public static Test suite() {
		return setUpTest(new TestSuite(THIS));
	}
	
	public static Test setUpTest(Test test) {
		return new Java18ProjectTestSetup(test);
	}

	protected IJavaProject getProject() {
		return Java18ProjectTestSetup.getProject();
	}

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
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

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
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

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
	}
	
	public void testConvertToAnonymous_andBack_WithWildcards() throws Exception {
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
		buf.append("            /* comment */\n");
		buf.append("            @Override\n");
		buf.append("            public int compare(Integer i1, Integer i2) {\n");
		buf.append("                return i1 - i2;\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("        Comparator<?> cw = new Comparator<Object>() {\n");
		buf.append("            /* comment */\n");
		buf.append("            @Override\n");
		buf.append("            public int compare(Object w1, Object w2) {\n");
		buf.append("                return 0;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("        Comparator cr = new Comparator() {\n");
		buf.append("            /* comment */\n");
		buf.append("            @Override\n");
		buf.append("            public int compare(Object r1, Object r2) {\n");
		buf.append("                return 0;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("        Comparator<? extends Number> ce = new Comparator<Number>() {\n");
		buf.append("            /* comment */\n");
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

	public void testConvertToAnonymous_andBack_WithWildcards1() throws Exception {
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
		buf.append("        /* comment */\n");
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

	public void testConvertToAnonymous_andBack_WithJoinedSAM() throws Exception {
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
		buf.append("        /* comment */\n");
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
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
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
	}

}

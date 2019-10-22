/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fabrice TIERCELIN - Split the tests
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.tests.core.Java1d5ProjectTestSetup;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests the cleanup features related to Java 5.
 */
public class CleanUpTest1d5 extends CleanUpTestCase {
	private static final Class<CleanUpTest1d5> THIS= CleanUpTest1d5.class;

	public CleanUpTest1d5(String name) {
		super(name);
	}

	public static Test suite() {
		return setUpTest(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new Java1d5ProjectTestSetup(test);
	}

	@Override
	protected IJavaProject getProject() {
		return Java1d5ProjectTestSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return Java1d5ProjectTestSetup.getDefaultClasspath();
	}

	public void testJava50ForLoop01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.ArrayList;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        List<E1> list= new ArrayList<E1>();\n" //
				+ "        for (Iterator<E1> iter = list.iterator(); iter.hasNext();) {\n" //
				+ "            E1 e = iter.next();\n" //
				+ "            System.out.println(e);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.util.ArrayList;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        List<E1> list= new ArrayList<E1>();\n" //
				+ "        for (E1 e : list) {\n" //
				+ "            System.out.println(e);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testJava50ForLoop02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.ArrayList;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        List<E1> list1= new ArrayList<E1>();\n" //
				+ "        List<E1> list2= new ArrayList<E1>();\n" //
				+ "        for (Iterator<E1> iter = list1.iterator(); iter.hasNext();) {\n" //
				+ "            E1 e1 = iter.next();\n" //
				+ "            for (Iterator iterator = list2.iterator(); iterator.hasNext();) {\n" //
				+ "                E1 e2 = (E1) iterator.next();\n" //
				+ "                System.out.println(e2);\n" //
				+ "            }\n" //
				+ "            System.out.println(e1);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.util.ArrayList;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        List<E1> list1= new ArrayList<E1>();\n" //
				+ "        List<E1> list2= new ArrayList<E1>();\n" //
				+ "        for (E1 e1 : list1) {\n" //
				+ "            for (Object element : list2) {\n" //
				+ "                E1 e2 = (E1) element;\n" //
				+ "                System.out.println(e2);\n" //
				+ "            }\n" //
				+ "            System.out.println(e1);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testJava50ForLoop03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        int[] array={1,2,3,4};\n" //
				+ "        for (int i=0;i<array.length;i++) {\n" //
				+ "            String[] strs={\"1\",\"2\"};\n" //
				+ "            for (int j = 0; j < strs.length; j++) {\n" //
				+ "                System.out.println(array[i]+strs[j]);\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        int[] array={1,2,3,4};\n" //
				+ "        for (int element : array) {\n" //
				+ "            String[] strs={\"1\",\"2\"};\n" //
				+ "            for (String str : strs) {\n" //
				+ "                System.out.println(element+str);\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testJava50ForLoop04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        int[] array= new int[10];\n" //
				+ "        for (int i = 0; i < array.length; i++) {\n" //
				+ "            for (int j = 0; j < array.length; j++) {\n" //
				+ "                for (int k = 0; k < array.length; k++) {\n" //
				+ "                }\n" //
				+ "                for (int k = 0; k < array.length; k++) {\n" //
				+ "                }\n" //
				+ "            }\n" //
				+ "            for (int j = 0; j < array.length; j++) {\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        int[] array= new int[10];\n" //
				+ "        for (int element : array) {\n" //
				+ "            for (int element2 : array) {\n" //
				+ "                for (int element3 : array) {\n" //
				+ "                }\n" //
				+ "                for (int element3 : array) {\n" //
				+ "                }\n" //
				+ "            }\n" //
				+ "            for (int element2 : array) {\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testJava50ForLoop05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        int[] array= null;\n" //
				+ "        for (int i = 0; --i < array.length;) {}\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	public void testJava50ForLoop06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        int a= 0;\n" //
				+ "        for (a=0;a>0;a++) {}\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	public void testJava50ForLoop07() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        int a= 0;\n" //
				+ "        for (a=0;;a++) {}\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	public void testJava50ForLoop08() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        int[] array= null;\n" //
				+ "        int a= 0;\n" //
				+ "        for (;a<array.length;a++) {}\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	public void testJava50ForLoop09() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        int[] array= null;\n" //
				+ "        for (int i = 0; i < array.length; i++) {\n" //
				+ "            final int element= array[i];\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        int[] array= null;\n" //
				+ "        for (final int element : array) {\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testJava50ForLoop10() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        int[] array= null;\n" //
				+ "        int i;\n" //
				+ "        for (i = 0; i < array.length; i++) {}\n" //
				+ "        System.out.println(i);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	public void testJava50ForLoop11() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private class E1Sub {\n" //
				+ "        public int[] array;\n" //
				+ "    }\n" //
				+ "    private E1Sub e1sub;\n" //
				+ "    public void foo() {\n" //
				+ "        for (int i = 0; i < this.e1sub.array.length; i++) {\n" //
				+ "            System.out.println(this.e1sub.array[i]);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private class E1Sub {\n" //
				+ "        public int[] array;\n" //
				+ "    }\n" //
				+ "    private E1Sub e1sub;\n" //
				+ "    public void foo() {\n" //
				+ "        for (int element : this.e1sub.array) {\n" //
				+ "            System.out.println(element);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testJava50ForLoop12() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public int[] array;\n" //
				+ "    public void foo() {\n" //
				+ "        for (int i = 0; i < this.array.length; i++) {\n" //
				+ "            System.out.println(this.array[i]);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public int[] array;\n" //
				+ "    public void foo() {\n" //
				+ "        for (int element : this.array) {\n" //
				+ "            System.out.println(element);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testJava50ForLoop13() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public int[] array1, array2;\n" //
				+ "    public void foo() {\n" //
				+ "        for (int i = array1.length - array2.length; i < 1; i++) {\n" //
				+ "            System.out.println(1);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	public void testJava50ForLoop14() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import test2.E3;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        E2 e2= new E2();\n" //
				+ "        e2.foo();\n" //
				+ "        E3 e3= new E3();\n" //
				+ "        for (int i = 0; i < e3.array.length;i++) {\n" //
				+ "            System.out.println(e3.array[i]);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "    public void foo() {};\n" //
				+ "}\n";
		pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= "" //
				+ "package test2;\n" //
				+ "public class E2 {}\n";
		pack2.createCompilationUnit("E2.java", sample, false, null);

		sample= "" //
				+ "package test2;\n" //
				+ "public class E3 {\n" //
				+ "    public E2[] array;\n" //
				+ "}\n";
		pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "import test2.E3;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        E2 e2= new E2();\n" //
				+ "        e2.foo();\n" //
				+ "        E3 e3= new E3();\n" //
				+ "        for (test2.E2 element : e3.array) {\n" //
				+ "            System.out.println(element);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testJava50ForLoop15() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "public class ForeachTest {\n" //
				+ "    void foo(Object list) {\n" //
				+ "        for (Iterator<String> iter= ((List<String>) list).iterator(); iter.hasNext(); ) {\n" //
				+ "            String element = iter.next();\n" //
				+ "            System.out.println(element);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.util.List;\n" //
				+ "public class ForeachTest {\n" //
				+ "    void foo(Object list) {\n" //
				+ "        for (String element : ((List<String>) list)) {\n" //
				+ "            System.out.println(element);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testJava50ForLoopBug548002() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.List;\n" //
				+ "public class ForeachTest {\n" //
				+ "    void foo(List list) {\n" //
				+ "        for (int i= 0; i < list.size(); ++i);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.util.List;\n" //
				+ "public class ForeachTest {\n" //
				+ "    void foo(List list) {\n" //
				+ "        for (Object element : list);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testJava50ForLoopBug550334() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.List;\n" //
				+ "public class ForeachTest {\n" //
				+ "    void foo(List list) {\n" //
				+ "        String[] a= new String[]{\"a\", \"b\", \"c\"});\n" //
				+ "        for (int i= 0; i < list.size(); ++i) {\n" //
				+ "            list.get(i).append(a[i]);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.util.List;\n" //
				+ "public class ForeachTest {\n" //
				+ "    void foo(List list) {\n" //
				+ "        String[] a= new String[]{\"a\", \"b\", \"c\"});\n" //
				+ "        for (int i= 0; i < list.size(); ++i) {\n" //
				+ "            list.get(i).append(a[i]);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testJava50ForLoopBug550672() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.List;\n" //
				+ "import java.util.ArrayList;\n" //
				+ "public class ForeachTest {\n" //
				+ "    void foo(List list) {\n" //
				+ "        List<File> a = new ArrayList<>();\n" //
				+ "        List<File> b = new ArrayList<>();\n" //
				+ "        for (int i = 0; i < a.size(); i++) {\n" //
				+ "            System.out.print(a.get(i) + \" \" + b.get(i));\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.util.List;\n" //
				+ "import java.util.ArrayList;\n" //
				+ "public class ForeachTest {\n" //
				+ "    void foo(List list) {\n" //
				+ "        List<File> a = new ArrayList<>();\n" //
				+ "        List<File> b = new ArrayList<>();\n" //
				+ "        for (int i = 0; i < a.size(); i++) {\n" //
				+ "            System.out.print(a.get(i) + \" \" + b.get(i));\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testBug550726() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.io.File;\n" //
				+ "import java.util.ArrayList;\n" //
				+ "import java.util.List;\n" //
				+ "public class A {\n" //
				+ "    public static void main(String[] args) {\n" //
				+ "        List<File> a = new ArrayList<>();\n" //
				+ "        for (int i = 0; i < a.size(); i++) {\n" //
				+ "            System.out.print(a);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.io.File;\n" //
				+ "import java.util.ArrayList;\n" //
				+ "import java.util.List;\n" //
				+ "public class A {\n" //
				+ "    public static void main(String[] args) {\n" //
				+ "        List<File> a = new ArrayList<>();\n" //
				+ "        for (File element : a) {\n" //
				+ "            System.out.print(a);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testJava50ForLoopBug154939() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(List<Integer> list) {\n" //
				+ "       for (Iterator<Integer> iter = list.iterator(); iter.hasNext() && false;) {\n" //
				+ "            Integer id = iter.next();\n" //
				+ "       } \n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	public void testJava50ForLoop160218() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    void bar(List<Number> x) {\n" //
				+ "        if (true) {\n" //
				+ "            for (Iterator<Number> i = x.iterator(); i.hasNext();)\n" //
				+ "                System.out.println(i.next());\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_NEVER);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    void bar(List<Number> x) {\n" //
				+ "        if (true)\n" //
				+ "            for (Number number : x)\n" //
				+ "                System.out.println(number);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testJava50ForLoop159449() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(Object[] objs) {\n" //
				+ "        if (objs != null)\n" //
				+ "            for (int i = 0; i < objs.length; i++) {\n" //
				+ "                System.out.println(objs[i]);\n" //
				+ "            }\n" //
				+ "    }\n" //
				+ "    public void bar(List<Object> objs) {\n" //
				+ "        if (objs != null)\n" //
				+ "            for (Iterator<Object> i = objs.iterator(); i.hasNext();) {\n" //
				+ "                System.out.println(i.next());\n" //
				+ "            }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(Object[] objs) {\n" //
				+ "        if (objs != null) {\n" //
				+ "            for (Object obj : objs) {\n" //
				+ "                System.out.println(obj);\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "    public void bar(List<Object> objs) {\n" //
				+ "        if (objs != null) {\n" //
				+ "            for (Object object : objs) {\n" //
				+ "                System.out.println(object);\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testJava50ForLoop160283_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    void foo(Object[] x) {\n" //
				+ "        for (int i = 0; i < x.length; i++) {\n" //
				+ "            System.out.println(x[i]);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "    void bar(List<Object> x) {\n" //
				+ "        for (Iterator<Object> i = x.iterator(); i.hasNext();) {\n" //
				+ "            System.out.println(i.next());\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_NEVER);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    void foo(Object[] x) {\n" //
				+ "        for (Object element : x)\n" //
				+ "            System.out.println(element);\n" //
				+ "    }\n" //
				+ "    void bar(List<Object> x) {\n" //
				+ "        for (Object object : x)\n" //
				+ "            System.out.println(object);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testJava50ForLoop160283_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    void foo(Object[] x) {\n" //
				+ "        for (int i = 0; i < x.length; i++)\n" //
				+ "            System.out.println(x[i]);\n" //
				+ "    }\n" //
				+ "    void bar(List<Object> x) {\n" //
				+ "        for (Iterator<Object> i = x.iterator(); i.hasNext();)\n" //
				+ "            System.out.println(i.next());\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    void foo(Object[] x) {\n" //
				+ "        for (Object element : x) {\n" //
				+ "            System.out.println(element);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "    void bar(List<Object> x) {\n" //
				+ "        for (Object object : x) {\n" //
				+ "            System.out.println(object);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testJava50ForLoop160312() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    void foo(Object[] x, Object[] y) {\n" //
				+ "        for (int i = 0; i < y.length; i++)\n" //
				+ "            for (int j = 0; j < x.length; j++)\n" //
				+ "                System.out.println(x[j]);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    void foo(Object[] x, Object[] y) {\n" //
				+ "        for (Object element : y) {\n" //
				+ "            for (Object element2 : x) {\n" //
				+ "                System.out.println(element2);\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testJava50ForLoop160270() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    void foo(List<Object> y) {\n" //
				+ "        for (Iterator<Object> it = y.iterator(); it.hasNext();) {\n" //
				+ "            System.out.println(it.next());\n" //
				+ "        }\n" //
				+ "        \n" //
				+ "        int j= 0;\n" //
				+ "        for (Iterator<Object> it = y.iterator(); it.hasNext(); j++) {\n" //
				+ "            System.out.println(it.next());\n" //
				+ "        }\n" //
				+ "        \n" //
				+ "        for (Iterator<Object> it = y.iterator(); it.hasNext(); bar()) {\n" //
				+ "            System.out.println(it.next());\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private void bar() {}\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    void foo(List<Object> y) {\n" //
				+ "        for (Object object : y) {\n" //
				+ "            System.out.println(object);\n" //
				+ "        }\n" //
				+ "        \n" //
				+ "        int j= 0;\n" //
				+ "        for (Iterator<Object> it = y.iterator(); it.hasNext(); j++) {\n" //
				+ "            System.out.println(it.next());\n" //
				+ "        }\n" //
				+ "        \n" //
				+ "        for (Iterator<Object> it = y.iterator(); it.hasNext(); bar()) {\n" //
				+ "            System.out.println(it.next());\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private void bar() {}\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}
}

/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.tests.core.rules.Java1d5ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;


/**
 * Tests the cleanup features related to Java 5.
 */
@RunWith(JUnit4.class)
public class CleanUpTest1d5 extends CleanUpTestCase {

	@Rule
    public ProjectTestSetup projectsetup = new Java1d5ProjectTestSetup();

	@Override
	protected IJavaProject getProject() {
		return Java1d5ProjectTestSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return Java1d5ProjectTestSetup.getDefaultClasspath();
	}

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
	public void testJava50ForBug560431_1() throws Exception {
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
				+ "            System.out.println(\"here\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_ONLY_IF_LOOP_VAR_USED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testJava50ForBug560431_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        int[] array={1,2,3,4};\n" //
				+ "        for (int i=0;i<array.length;i++) {\n" //
				+ "            String[] strs={\"1\",\"2\"};\n" //
				+ "            for (int j = 0; j < strs.length; j++) {\n" //
				+ "                System.out.println(\"here\");\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_ONLY_IF_LOOP_VAR_USED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
	public void testJava50ForLoop163122_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    void foo(Object[] x, Object[] y) {\n" //
				+ "        for (int i = 0; i < y.length; i++)\n" //
				+ "            for (int j = 0; j < x.length; j++)\n" //
				+ "                System.out.println(y[i]);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    void foo(Object[] x, Object[] y) {\n" //
				+ "        for (Object element : y) {\n" //
				+ "            for (Object element2 : x) {\n" //
				+ "                System.out.println(element);\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testJava50ForLoop163122_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    void foo(Object[] x, Object[] y) {\n" //
				+ "        for (int i = 0; i < y.length; i++)\n" //
				+ "            for (int j = 0; j < x.length; j++)\n" //
				+ "                System.out.println(y[i]);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    void foo(Object[] x, Object[] y) {\n" //
				+ "        for (Object element : y)\n" //
				+ "            for (Object element2 : x)\n" //
				+ "                System.out.println(element);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testJava50ForLoop163122_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    void foo(Object[] x, Object[] y) {\n" //
				+ "        for (int i = 0; i < y.length; i++)\n" //
				+ "            for (int j = 0; j < x.length; j++)\n" //
				+ "                System.out.println(x[i]);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    void foo(Object[] x, Object[] y) {\n" //
				+ "        for (int i = 0; i < y.length; i++) {\n" //
				+ "            for (Object element : x) {\n" //
				+ "                System.out.println(x[i]);\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testJava50ForLoop163122_4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    void foo(Object[] x, Object[] y) {\n" //
				+ "        for (int i = 0; i < y.length; i++)\n" //
				+ "            for (int j = 0; j < x.length; j++)\n" //
				+ "                System.out.println(x[i]);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    void foo(Object[] x, Object[] y) {\n" //
				+ "        for (int i = 0; i < y.length; i++)\n" //
				+ "            for (Object element : x)\n" //
				+ "                System.out.println(x[i]);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testJava50ForLoop163122_5() throws Exception {
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

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

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

	@Test
	public void testJava50ForLoop110599() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    public void a(int[] i, List<String> l) {\n" //
				+ "        //Comment\n" //
				+ "        for (int j = 0; j < i.length; j++) {\n" //
				+ "            System.out.println(i[j]);\n" //
				+ "        }\n" //
				+ "        //Comment\n" //
				+ "        for (Iterator<String> iterator = l.iterator(); iterator.hasNext();) {\n" //
				+ "            String str = iterator.next();\n" //
				+ "            System.out.println(str);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    public void a(int[] i, List<String> l) {\n" //
				+ "        //Comment\n" //
				+ "        for (int element : i) {\n" //
				+ "            System.out.println(element);\n" //
				+ "        }\n" //
				+ "        //Comment\n" //
				+ "        for (String str : l) {\n" //
				+ "            System.out.println(str);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testJava50ForLoop269595() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void a(int[] array) {\n" //
				+ "        for (int i = 0; i < array.length; i++) {\n" //
				+ "            final int value = array[i];\n" //
				+ "            System.out.println(value);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void a(final int[] array) {\n" //
				+ "        for (final int value : array) {\n" //
				+ "            System.out.println(value);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testJava50ForLoop264421() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(String[] src) {\n" //
				+ "        for (int i = 0; i < src.length; i++) {\n" //
				+ "            String path = src[i];\n" //
				+ "            String output = path;\n" //
				+ "            if (output.length() == 1) {\n" //
				+ "                output = output + \"-XXX\";\n" //
				+ "            }\n" //
				+ "            System.err.println(\"path=\"+ path + \",output=\"+output);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(String[] src) {\n" //
				+ "        for (String path : src) {\n" //
				+ "            String output = path;\n" //
				+ "            if (output.length() == 1) {\n" //
				+ "                output = output + \"-XXX\";\n" //
				+ "            }\n" //
				+ "            System.err.println(\"path=\"+ path + \",output=\"+output);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testJava50ForLoop274199() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public static void main(String[] args) {\n" //
				+ "        for (int i = 0; i < args.length; i++) {\n" //
				+ "            String output = args[i];\n" //
				+ "            if (output.length() == 1) {\n" //
				+ "                output = output + \"-XXX\";\n" //
				+ "            }\n" //
				+ "\n" //
				+ "            String s = \"path=\" + args[i] + \",output=\" + output;\n" //
				+ "        }\n" //
				+ "        \n" //
				+ "        for (int i = 0; i < args.length; i++) {\n" //
				+ "            String output = args[i];\n" //
				+ "            String output1 = output;\n" //
				+ "            if (output1.length() == 1) {\n" //
				+ "                output1 = output1 + \"-XXX\";\n" //
				+ "            }\n" //
				+ "\n" //
				+ "            String s = \"path=\" + args[i] + \",output=\" + output1;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public static void main(String[] args) {\n" //
				+ "        for (String arg : args) {\n" //
				+ "            String output = arg;\n" //
				+ "            if (output.length() == 1) {\n" //
				+ "                output = output + \"-XXX\";\n" //
				+ "            }\n" //
				+ "\n" //
				+ "            String s = \"path=\" + arg + \",output=\" + output;\n" //
				+ "        }\n" //
				+ "        \n" //
				+ "        for (String output : args) {\n" //
				+ "            String output1 = output;\n" //
				+ "            if (output1.length() == 1) {\n" //
				+ "                output1 = output1 + \"-XXX\";\n" //
				+ "            }\n" //
				+ "\n" //
				+ "            String s = \"path=\" + output + \",output=\" + output1;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testJava50ForLoop349782() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=349782
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public int[] array;\n" //
				+ "    public void foo() {\n" //
				+ "        for (int i = 0; i < this.array.length; ++i) {\n" //
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

	@Test
	public void testJava50ForLoop344674() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=344674
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public int[] array;\n" //
				+ "    public void foo(Object obj) {\n" //
				+ "        for (int i = 0; i < ((E1) obj).array.length; i++) {\n" //
				+ "            System.out.println(((E1) obj).array[i]);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public int[] array;\n" //
				+ "    public void foo(Object obj) {\n" //
				+ "        for (int element : ((E1) obj).array) {\n" //
				+ "            System.out.println(element);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testJava50ForLoop374264() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=374264
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(List<String> list) {\n" //
				+ "        for (Iterator<String> iterator = list.iterator(); iterator.hasNext();) {\n" //
				+ "            removeSecond(iterator);\n" //
				+ "        }\n" //
				+ "        System.out.println(list);\n" //
				+ "    }\n" //
				+ "    private static void removeSecond(Iterator<String> iterator) {\n" //
				+ "        if (\"second\".equals(iterator.next())) {\n" //
				+ "            iterator.remove();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testAutoboxing() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    public static void bar() {\n" //
				+ "        Character c = Character.valueOf('*');\n" //
				+ "        Byte by = Byte.valueOf((byte) 0);\n" //
				+ "        Boolean bo = Boolean.valueOf(true);\n" //
				+ "        Integer i = Integer.valueOf(42);\n" //
				+ "        Long l1 = Long.valueOf(42L);\n" //
				+ "        Long l2 = Long.valueOf(42);\n" //
				+ "        Short s = Short.valueOf((short) 42);\n" //
				+ "        Float f = Float.valueOf(42.42F);\n" //
				+ "        Double d = Double.valueOf(42.42);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_AUTOBOXING);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    public static void bar() {\n" //
				+ "        Character c = '*';\n" //
				+ "        Byte by = (byte) 0;\n" //
				+ "        Boolean bo = true;\n" //
				+ "        Integer i = 42;\n" //
				+ "        Long l1 = 42L;\n" //
				+ "        Long l2 = (long) 42;\n" //
				+ "        Short s = (short) 42;\n" //
				+ "        Float f = 42.42F;\n" //
				+ "        Double d = 42.42;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testAutoboxingSpecialCases() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    public static void removeUnnecessaryValueOfCallsInPrimitiveDeclaration() {\n" //
				+ "        char c = Character.valueOf('*');\n" //
				+ "        byte by = Byte.valueOf((byte) 0);\n" //
				+ "        boolean bo = Boolean.valueOf(true);\n" //
				+ "        int i = Integer.valueOf(42);\n" //
				+ "        long l1 = Long.valueOf(42L);\n" //
				+ "        long l2 = Long.valueOf(42);\n" //
				+ "        short s = Short.valueOf((short) 42);\n" //
				+ "        float f = Float.valueOf(42.42F);\n" //
				+ "        double d = Double.valueOf(42.42);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static void doNotUseAutoboxingWithObjectDeclaration() {\n" //
				+ "        Object c = Character.valueOf('*');\n" //
				+ "        Object by = Byte.valueOf((byte) 0);\n" //
				+ "        Object bo = Boolean.valueOf(true);\n" //
				+ "        Object i = Integer.valueOf(42);\n" //
				+ "        Object l1 = Long.valueOf(42L);\n" //
				+ "        Object l2 = Long.valueOf(42);\n" //
				+ "        Object s = Short.valueOf((short) 42);\n" //
				+ "        Object f = Float.valueOf(42.42F);\n" //
				+ "        Object d = Double.valueOf(42.42);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static void directlyReturnWrapperParameter(Character c, Byte by, Boolean bo, Integer i, Long l, Short s,\n" //
				+ "            Float f, Double d) {\n" //
				+ "        Object myObject = null;\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        myObject = Character.valueOf(c);\n" //
				+ "        myObject = Byte.valueOf(by);\n" //
				+ "        myObject = Boolean.valueOf(bo);\n" //
				+ "        myObject = Integer.valueOf(i);\n" //
				+ "        myObject = Long.valueOf(l);\n" //
				+ "        myObject = Short.valueOf(s);\n" //
				+ "        myObject = Float.valueOf(f);\n" //
				+ "        myObject = Double.valueOf(d);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static void useAutoboxingOnAssignment() {\n" //
				+ "        // Keep this comment\n" //
				+ "        Character c;\n" //
				+ "        c = Character.valueOf('*');\n" //
				+ "        Byte by;\n" //
				+ "        by = Byte.valueOf((byte) 0);\n" //
				+ "        Boolean bo1;\n" //
				+ "        bo1 = Boolean.valueOf(true);\n" //
				+ "        Integer i;\n" //
				+ "        i = Integer.valueOf(42);\n" //
				+ "        Long l1;\n" //
				+ "        l1 = Long.valueOf(42L);\n" //
				+ "        Long l2;\n" //
				+ "        l2 = Long.valueOf(42);\n" //
				+ "        Short s;\n" //
				+ "        s = Short.valueOf((short) 42);\n" //
				+ "        Float f;\n" //
				+ "        f = Float.valueOf(42.42F);\n" //
				+ "        Double d;\n" //
				+ "        d = Double.valueOf(42.42);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static void removeUnnecessaryValueOfCallsInPrimitiveAssignment() {\n" //
				+ "        // Keep this comment\n" //
				+ "        char c;\n" //
				+ "        c = Character.valueOf('*');\n" //
				+ "        byte by;\n" //
				+ "        by = Byte.valueOf((byte) 0);\n" //
				+ "        boolean bo1;\n" //
				+ "        bo1 = Boolean.valueOf(true);\n" //
				+ "        int i;\n" //
				+ "        i = Integer.valueOf(42);\n" //
				+ "        long l1;\n" //
				+ "        l1 = Long.valueOf(42L);\n" //
				+ "        long l2;\n" //
				+ "        l2 = Long.valueOf(42);\n" //
				+ "        short s;\n" //
				+ "        s = Short.valueOf((short) 42);\n" //
				+ "        float f;\n" //
				+ "        f = Float.valueOf(42.42F);\n" //
				+ "        double d;\n" //
				+ "        d = Double.valueOf(42.42);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static void doNotUseAutoboxingWithObjectAssignment() {\n" //
				+ "        Object c;\n" //
				+ "        c = Character.valueOf('*');\n" //
				+ "        Object by;\n" //
				+ "        by = Byte.valueOf((byte) 0);\n" //
				+ "        Object bo1;\n" //
				+ "        bo1 = Boolean.valueOf(true);\n" //
				+ "        Object i;\n" //
				+ "        i = Integer.valueOf(42);\n" //
				+ "        Object l1;\n" //
				+ "        l1 = Long.valueOf(42L);\n" //
				+ "        Object l2;\n" //
				+ "        l2 = Long.valueOf(42);\n" //
				+ "        Object s;\n" //
				+ "        s = Short.valueOf((short) 42);\n" //
				+ "        Object f;\n" //
				+ "        f = Float.valueOf(42.42F);\n" //
				+ "        Object d;\n" //
				+ "        d = Double.valueOf(42.42);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Character removeUnnecessaryValueOfCallsInCharacterWrapper() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return Character.valueOf('*');\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Byte removeUnnecessaryValueOfCallsInByteWrapper() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return Byte.valueOf((byte) 0);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Boolean removeUnnecessaryValueOfCallsInBooleanWrapper() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return Boolean.valueOf(true);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Integer removeUnnecessaryValueOfCallsInIntegerWrapper() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return Integer.valueOf(42);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Long removeUnnecessaryValueOfCallsInLongWrapper() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return Long.valueOf(42L);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Short removeUnnecessaryValueOfCallsInShortWrapper() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return Short.valueOf((short) 42);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Float removeUnnecessaryValueOfCallsInFloatWrapper() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return Float.valueOf(42.42F);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Double removeUnnecessaryValueOfCallsInDoubleWrapper() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return Double.valueOf(42.42);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static char removeUnnecessaryValueOfCallsInCharacterPrimitive() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return Character.valueOf('*');\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static byte removeUnnecessaryValueOfCallsInBytePrimitive() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return Byte.valueOf((byte) 0);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static boolean removeUnnecessaryValueOfCallsInBooleanPrimitive() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return Boolean.valueOf(true);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static int removeUnnecessaryValueOfCallsInIntegerPrimitive() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return Integer.valueOf(42);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static long removeUnnecessaryValueOfCallsInLongPrimitive() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return Long.valueOf(42L);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static short removeUnnecessaryValueOfCallsInShortPrimitive() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return Short.valueOf((short) 42);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static float removeUnnecessaryValueOfCallsInFloatPrimitive() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return Float.valueOf(42.42F);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static double removeUnnecessaryValueOfCallsInDoublePrimitive() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return Double.valueOf(42.42);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Object doNotUseAutoboxingReturningObject() {\n" //
				+ "        return Character.valueOf('a');\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_AUTOBOXING);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    public static void removeUnnecessaryValueOfCallsInPrimitiveDeclaration() {\n" //
				+ "        char c = '*';\n" //
				+ "        byte by = (byte) 0;\n" //
				+ "        boolean bo = true;\n" //
				+ "        int i = 42;\n" //
				+ "        long l1 = 42L;\n" //
				+ "        long l2 = 42;\n" //
				+ "        short s = (short) 42;\n" //
				+ "        float f = 42.42F;\n" //
				+ "        double d = 42.42;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static void doNotUseAutoboxingWithObjectDeclaration() {\n" //
				+ "        Object c = Character.valueOf('*');\n" //
				+ "        Object by = Byte.valueOf((byte) 0);\n" //
				+ "        Object bo = Boolean.valueOf(true);\n" //
				+ "        Object i = Integer.valueOf(42);\n" //
				+ "        Object l1 = Long.valueOf(42L);\n" //
				+ "        Object l2 = Long.valueOf(42);\n" //
				+ "        Object s = Short.valueOf((short) 42);\n" //
				+ "        Object f = Float.valueOf(42.42F);\n" //
				+ "        Object d = Double.valueOf(42.42);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static void directlyReturnWrapperParameter(Character c, Byte by, Boolean bo, Integer i, Long l, Short s,\n" //
				+ "            Float f, Double d) {\n" //
				+ "        Object myObject = null;\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        myObject = c;\n" //
				+ "        myObject = by;\n" //
				+ "        myObject = bo;\n" //
				+ "        myObject = i;\n" //
				+ "        myObject = l;\n" //
				+ "        myObject = s;\n" //
				+ "        myObject = f;\n" //
				+ "        myObject = d;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static void useAutoboxingOnAssignment() {\n" //
				+ "        // Keep this comment\n" //
				+ "        Character c;\n" //
				+ "        c = '*';\n" //
				+ "        Byte by;\n" //
				+ "        by = (byte) 0;\n" //
				+ "        Boolean bo1;\n" //
				+ "        bo1 = true;\n" //
				+ "        Integer i;\n" //
				+ "        i = 42;\n" //
				+ "        Long l1;\n" //
				+ "        l1 = 42L;\n" //
				+ "        Long l2;\n" //
				+ "        l2 = (long) 42;\n" //
				+ "        Short s;\n" //
				+ "        s = (short) 42;\n" //
				+ "        Float f;\n" //
				+ "        f = 42.42F;\n" //
				+ "        Double d;\n" //
				+ "        d = 42.42;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static void removeUnnecessaryValueOfCallsInPrimitiveAssignment() {\n" //
				+ "        // Keep this comment\n" //
				+ "        char c;\n" //
				+ "        c = '*';\n" //
				+ "        byte by;\n" //
				+ "        by = (byte) 0;\n" //
				+ "        boolean bo1;\n" //
				+ "        bo1 = true;\n" //
				+ "        int i;\n" //
				+ "        i = 42;\n" //
				+ "        long l1;\n" //
				+ "        l1 = 42L;\n" //
				+ "        long l2;\n" //
				+ "        l2 = 42;\n" //
				+ "        short s;\n" //
				+ "        s = (short) 42;\n" //
				+ "        float f;\n" //
				+ "        f = 42.42F;\n" //
				+ "        double d;\n" //
				+ "        d = 42.42;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static void doNotUseAutoboxingWithObjectAssignment() {\n" //
				+ "        Object c;\n" //
				+ "        c = Character.valueOf('*');\n" //
				+ "        Object by;\n" //
				+ "        by = Byte.valueOf((byte) 0);\n" //
				+ "        Object bo1;\n" //
				+ "        bo1 = Boolean.valueOf(true);\n" //
				+ "        Object i;\n" //
				+ "        i = Integer.valueOf(42);\n" //
				+ "        Object l1;\n" //
				+ "        l1 = Long.valueOf(42L);\n" //
				+ "        Object l2;\n" //
				+ "        l2 = Long.valueOf(42);\n" //
				+ "        Object s;\n" //
				+ "        s = Short.valueOf((short) 42);\n" //
				+ "        Object f;\n" //
				+ "        f = Float.valueOf(42.42F);\n" //
				+ "        Object d;\n" //
				+ "        d = Double.valueOf(42.42);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Character removeUnnecessaryValueOfCallsInCharacterWrapper() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return '*';\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Byte removeUnnecessaryValueOfCallsInByteWrapper() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return (byte) 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Boolean removeUnnecessaryValueOfCallsInBooleanWrapper() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return true;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Integer removeUnnecessaryValueOfCallsInIntegerWrapper() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return 42;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Long removeUnnecessaryValueOfCallsInLongWrapper() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return 42L;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Short removeUnnecessaryValueOfCallsInShortWrapper() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return (short) 42;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Float removeUnnecessaryValueOfCallsInFloatWrapper() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return 42.42F;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Double removeUnnecessaryValueOfCallsInDoubleWrapper() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return 42.42;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static char removeUnnecessaryValueOfCallsInCharacterPrimitive() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return '*';\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static byte removeUnnecessaryValueOfCallsInBytePrimitive() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return (byte) 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static boolean removeUnnecessaryValueOfCallsInBooleanPrimitive() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return true;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static int removeUnnecessaryValueOfCallsInIntegerPrimitive() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return 42;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static long removeUnnecessaryValueOfCallsInLongPrimitive() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return 42L;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static short removeUnnecessaryValueOfCallsInShortPrimitive() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return (short) 42;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static float removeUnnecessaryValueOfCallsInFloatPrimitive() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return 42.42F;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static double removeUnnecessaryValueOfCallsInDoublePrimitive() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return 42.42;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Object doNotUseAutoboxingReturningObject() {\n" //
				+ "        return Character.valueOf('a');\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testDoNotUseAutoboxingOnString() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public static void doNotUseAutoboxingOnString() {\n" //
				+ "        Integer i = Integer.valueOf(\"1\");\n" //
				+ "        Long l = Long.valueOf(\"1\");\n" //
				+ "        Short s = Short.valueOf(\"1\");\n" //
				+ "        Float f = Float.valueOf(\"1\");\n" //
				+ "        Double d = Double.valueOf(\"1\");\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_AUTOBOXING);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testUseUnboxingOnPrimitiveDeclaration() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    public static void useUnboxingOnPrimitiveDeclaration(Character cObject, Byte byObject, Boolean boObject,\n" //
				+ "            Integer iObject, Short sObject, Long lObject, Float fObject, Double dObject) {\n" //
				+ "        // Keep this comment\n" //
				+ "        char c = cObject.charValue();\n" //
				+ "        byte by = byObject.byteValue();\n" //
				+ "        boolean bo = boObject.booleanValue();\n" //
				+ "        int i = iObject.intValue();\n" //
				+ "        short s = sObject.shortValue();\n" //
				+ "        long l = lObject.longValue();\n" //
				+ "        float f = fObject.floatValue();\n" //
				+ "        double d = dObject.doubleValue();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_UNBOXING);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    public static void useUnboxingOnPrimitiveDeclaration(Character cObject, Byte byObject, Boolean boObject,\n" //
				+ "            Integer iObject, Short sObject, Long lObject, Float fObject, Double dObject) {\n" //
				+ "        // Keep this comment\n" //
				+ "        char c = cObject;\n" //
				+ "        byte by = byObject;\n" //
				+ "        boolean bo = boObject;\n" //
				+ "        int i = iObject;\n" //
				+ "        short s = sObject;\n" //
				+ "        long l = lObject;\n" //
				+ "        float f = fObject;\n" //
				+ "        double d = dObject;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testDoNotUseUnboxingOnNarrowingType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public static void doNotUseUnboxingOnNarrowingType(Character cObject, Byte byObject,\n" //
				+ "            Integer iObject, Short sObject, Float fObject) {\n" //
				+ "        int c = cObject.charValue();\n" //
				+ "        int by = byObject.byteValue();\n" //
				+ "        long i = iObject.intValue();\n" //
				+ "        int s = sObject.shortValue();\n" //
				+ "        double f = fObject.floatValue();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_UNBOXING);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotUseUnboxingWhenTypesDontMatch() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public static void doNotUseUnboxingWhenTypesDontMatch(Byte byObject,\n" //
				+ "            Integer iObject, Short sObject, Long lObject, Float fObject, Double dObject) {\n" //
				+ "        short by = byObject.shortValue();\n" //
				+ "        short i = iObject.shortValue();\n" //
				+ "        byte s = sObject.byteValue();\n" //
				+ "        short l = lObject.shortValue();\n" //
				+ "        short f = fObject.shortValue();\n" //
				+ "        short d = dObject.shortValue();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_UNBOXING);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testUnboxing2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    public static void reuseWrapper(Character cObject, Byte byObject, Boolean boObject,\n" //
				+ "            Integer iObject, Short sObject, Long lObject, Float fObject, Double dObject) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Character c = cObject.charValue();\n" //
				+ "        Byte by = byObject.byteValue();\n" //
				+ "        Boolean bo = boObject.booleanValue();\n" //
				+ "        Integer i = iObject.intValue();\n" //
				+ "        Short s = sObject.shortValue();\n" //
				+ "        Long l = lObject.longValue();\n" //
				+ "        Float f = fObject.floatValue();\n" //
				+ "        Double d = dObject.doubleValue();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_UNBOXING);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    public static void reuseWrapper(Character cObject, Byte byObject, Boolean boObject,\n" //
				+ "            Integer iObject, Short sObject, Long lObject, Float fObject, Double dObject) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Character c = cObject;\n" //
				+ "        Byte by = byObject;\n" //
				+ "        Boolean bo = boObject;\n" //
				+ "        Integer i = iObject;\n" //
				+ "        Short s = sObject;\n" //
				+ "        Long l = lObject;\n" //
				+ "        Float f = fObject;\n" //
				+ "        Double d = dObject;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testUnboxing3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public static void useUnboxingOnPrimitiveAssignment(Character cObject, Byte byObject, Boolean boObject,\n" //
				+ "            Integer iObject, Short sObject, Long lObject, Float fObject, Double dObject) {\n" //
				+ "        // Keep this comment\n" //
				+ "        char c;\n" //
				+ "        c = cObject.charValue();\n" //
				+ "        byte by;\n" //
				+ "        by = byObject.byteValue();\n" //
				+ "        boolean bo;\n" //
				+ "        bo = boObject.booleanValue();\n" //
				+ "        int i;\n" //
				+ "        i = iObject.intValue();\n" //
				+ "        short s;\n" //
				+ "        s = sObject.shortValue();\n" //
				+ "        long l;\n" //
				+ "        l = lObject.longValue();\n" //
				+ "        float f;\n" //
				+ "        f = fObject.floatValue();\n" //
				+ "        double d;\n" //
				+ "        d = dObject.doubleValue();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_UNBOXING);
		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public static void useUnboxingOnPrimitiveAssignment(Character cObject, Byte byObject, Boolean boObject,\n" //
				+ "            Integer iObject, Short sObject, Long lObject, Float fObject, Double dObject) {\n" //
				+ "        // Keep this comment\n" //
				+ "        char c;\n" //
				+ "        c = cObject;\n" //
				+ "        byte by;\n" //
				+ "        by = byObject;\n" //
				+ "        boolean bo;\n" //
				+ "        bo = boObject;\n" //
				+ "        int i;\n" //
				+ "        i = iObject;\n" //
				+ "        short s;\n" //
				+ "        s = sObject;\n" //
				+ "        long l;\n" //
				+ "        l = lObject;\n" //
				+ "        float f;\n" //
				+ "        f = fObject;\n" //
				+ "        double d;\n" //
				+ "        d = dObject;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testUnboxing4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public static char useUnboxingOnPrimitiveReturn(Character cObject) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return cObject.charValue();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_UNBOXING);
		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public static char useUnboxingOnPrimitiveReturn(Character cObject) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return cObject;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testUnboxing5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public static byte useUnboxingOnPrimitiveReturn(Byte byObject) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return byObject.byteValue();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_UNBOXING);
		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public static byte useUnboxingOnPrimitiveReturn(Byte byObject) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return byObject;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testUnboxing6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public static boolean useUnboxingOnPrimitiveReturn(Boolean boObject) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return boObject.booleanValue();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_UNBOXING);
		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public static boolean useUnboxingOnPrimitiveReturn(Boolean boObject) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return boObject;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testUnboxing7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public static int useUnboxingOnPrimitiveReturn(Integer iObject) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return iObject.intValue();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_UNBOXING);
		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public static int useUnboxingOnPrimitiveReturn(Integer iObject) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return iObject;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testUnboxing8() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public static short useUnboxingOnPrimitiveReturn(Short sObject) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return sObject.shortValue();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_UNBOXING);
		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public static short useUnboxingOnPrimitiveReturn(Short sObject) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return sObject;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testUnboxing9() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public static long useUnboxingOnPrimitiveReturn(Long lObject) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return lObject.longValue();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_UNBOXING);
		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public static long useUnboxingOnPrimitiveReturn(Long lObject) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return lObject;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testUnboxing10() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public static float useUnboxingOnPrimitiveReturn(Float fObject) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return fObject.floatValue();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_UNBOXING);
		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public static float useUnboxingOnPrimitiveReturn(Float fObject) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return fObject;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testUnboxing11() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public static double useUnboxingOnPrimitiveReturn(Double dObject) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return dObject.doubleValue();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_UNBOXING);
		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public static double useUnboxingOnPrimitiveReturn(Double dObject) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return dObject;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testUnboxingInArrayAccess() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public static String useUnboxingOnArrayAccess(String[] strings, Integer i) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return strings[i.intValue()];\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_UNBOXING);
		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public static String useUnboxingOnArrayAccess(String[] strings, Integer i) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return strings[i];\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testUnnecessaryArrayBug550129() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "public class X {\n" //
				+ "  public int foo(String x, String ...y) { return y.length + 1; }\n" //
				+ "  public int bar() {\n" //
				+ "      return foo(\"a\", new String[] {\"b\", \"c\", \"d\"});\n" //
				+ "  };\n" //
				+ "  public int bar2() {\n" //
				+ "      return foo(\"a\", \"b\", new String[] {\"c\", \"d\"});\n" //
				+ "  };\n" //
				+ "  public int foo2(String[] ...x) { return x.length; }\n" //
				+ "  public int bar3() {\n" //
				+ "      return foo2(new String[][] { new String[] {\"a\", \"b\"}});\n" //
				+ "  };\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("X.java", sample, false, null);

		sample= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "public class X {\n" //
				+ "  public int foo(String x, String ...y) { return y.length + 1; }\n" //
				+ "  public int bar() {\n" //
				+ "      return foo(\"a\", \"b\", \"c\", \"d\");\n" //
				+ "  };\n" //
				+ "  public int bar2() {\n" //
				+ "      return foo(\"a\", \"b\", new String[] {\"c\", \"d\"});\n" //
				+ "  };\n" //
				+ "  public int foo2(String[] ...x) { return x.length; }\n" //
				+ "  public int bar3() {\n" //
				+ "      return foo2(new String[][] { new String[] {\"a\", \"b\"}});\n" //
				+ "  };\n" //
				+ "}\n";
		String expected1= sample;

		sample= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "import java.util.Arrays;\n" //
				+ "\n" //
				+ "public final class X2 {\n" //
				+ "  public static class Y {\n" //
				+ "      public int foo(String x, String ...y) { return y.length + 1; }\n" //
				+ "  }\n" //
				+ "  public static class Z extends Y {\n" //
				+ "      public int foo2() {\n" //
				+ "          List<String> list= Arrays.asList(new String[] {\"one\"/* 1 */\n" //
				+ "              + \"one\", \"two\"/* 2 */\n" //
				+ "              + \"two\", \"three\"/* 3 */\n" //
				+ "              + \"three\"});\n" //
				+ "          return super.foo(\"x\", new String[] {\"y\", \"z\"});\n" //
				+ "      }\n" //
				+ "}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("X2.java", sample, false, null);

		sample= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "import java.util.Arrays;\n" //
				+ "\n" //
				+ "public final class X2 {\n" //
				+ "  public static class Y {\n" //
				+ "      public int foo(String x, String ...y) { return y.length + 1; }\n" //
				+ "  }\n" //
				+ "  public static class Z extends Y {\n" //
				+ "      public int foo2() {\n" //
				+ "          List<String> list= Arrays.asList(\"one\"/* 1 */\n" //
				+ "              + \"one\", \"two\"/* 2 */\n" //
				+ "          + \"two\", \"three\"/* 3 */\n" //
				+ "          + \"three\");\n" //
				+ "          return super.foo(\"x\", \"y\", \"z\");\n" //
				+ "      }\n" //
				+ "}\n";
		String expected2= sample;

		enable(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1, cu2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testUnnecessaryArrayBug562091() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.lang.reflect.Method;\n" //
				+ "\n" //
				+ "public class A {\n" //
				+ "  public void foo() throws Throwable {\n" //
				+ "    Method method= A.class.getMethod(\"bah\", A.class);\n" //
				+ "    method.invoke(this, new Object[] {null});\n" //
				+ "  }\n" //
				+ "}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}
}

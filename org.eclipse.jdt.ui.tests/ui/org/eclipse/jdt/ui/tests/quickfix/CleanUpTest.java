/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alex Blewitt - https://bugs.eclipse.org/bugs/show_bug.cgi?id=168954
 *     Chris West (Faux) <eclipse@goeswhere.com> - [clean up] "Use modifier 'final' where possible" can introduce compile errors - https://bugs.eclipse.org/bugs/show_bug.cgi?id=272532
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IncrementalProjectBuilder;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.fix.Java50CleanUp;
import org.eclipse.jdt.internal.ui.fix.UnimplementedCodeCleanUp;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

public class CleanUpTest extends CleanUpTestCase {

	private static final Class THIS= CleanUpTest.class;

	public CleanUpTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}
	
	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}

	public void testAddNLSTag01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String s= \"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public String s1 = \"\";\n");
		buf.append("    public void foo() {\n");
		buf.append("        String s2 = \"\";\n");
		buf.append("        String s3 = s2 + \"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("    public static final String s= \"\";\n");
		buf.append("    public static String bar(String s1, String s2) {\n");
		buf.append("        bar(\"\", \"\");\n");
		buf.append("        return \"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);

		enable(CleanUpConstants.ADD_MISSING_NLS_TAGS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String s= \"\"; //$NON-NLS-1$\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public String s1 = \"\"; //$NON-NLS-1$\n");
		buf.append("    public void foo() {\n");
		buf.append("        String s2 = \"\"; //$NON-NLS-1$\n");
		buf.append("        String s3 = s2 + \"\"; //$NON-NLS-1$\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("    public static final String s= \"\"; //$NON-NLS-1$\n");
		buf.append("    public static String bar(String s1, String s2) {\n");
		buf.append("        bar(\"\", \"\"); //$NON-NLS-1$ //$NON-NLS-2$\n");
		buf.append("        return \"\"; //$NON-NLS-1$\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3});
	}

	public void testRemoveNLSTag01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String s= null; //$NON-NLS-1$\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public String s1 = null; //$NON-NLS-1$\n");
		buf.append("    public void foo() {\n");
		buf.append("        String s2 = null; //$NON-NLS-1$\n");
		buf.append("        String s3 = s2 + s2; //$NON-NLS-1$\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("    public static final String s= null; //$NON-NLS-1$\n");
		buf.append("    public static String bar(String s1, String s2) {\n");
		buf.append("        bar(s2, s1); //$NON-NLS-1$ //$NON-NLS-2$\n");
		buf.append("        return s1; //$NON-NLS-1$\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_NLS_TAGS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String s= null; \n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public String s1 = null; \n");
		buf.append("    public void foo() {\n");
		buf.append("        String s2 = null; \n");
		buf.append("        String s3 = s2 + s2; \n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("    public static final String s= null; \n");
		buf.append("    public static String bar(String s1, String s2) {\n");
		buf.append("        bar(s2, s1); \n");
		buf.append("        return s1; \n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3});
	}

	public void testUnusedCode01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.*;\n");
		buf.append("public class E2 {\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import java.util.HashMap;\n");
		buf.append("import test1.E2;\n");
		buf.append("import java.io.StringReader;\n");
		buf.append("import java.util.HashMap;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("}\n");
		String expected2= buf.toString();

		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("}\n");
		String expected3= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3});
	}

	public void testUnusedCode02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private void foo() {}\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    private void foo() {}\n");
		buf.append("    private void bar() {}\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class E3 {\n");
		buf.append("    private class E3Inner {\n");
		buf.append("        private void foo() {}\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        Runnable r= new Runnable() {\n");
		buf.append("            public void run() {}\n");
		buf.append("            private void foo() {};\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_METHODS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("}\n");
		String expected2= buf.toString();

		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class E3 {\n");
		buf.append("    private class E3Inner {\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        Runnable r= new Runnable() {\n");
		buf.append("            public void run() {};\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3});
	}

	public void testUnusedCode03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private E1(int i) {}\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public E2() {}\n");
		buf.append("    private E2(int i) {}\n");
		buf.append("    private E2(String s) {}\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class E3 {\n");
		buf.append("    public class E3Inner {\n");
		buf.append("        private E3Inner(int i) {}\n");
		buf.append("    }\n");
		buf.append("    private void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private E1(int i) {}\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public E2() {}\n");
		buf.append("}\n");
		String expected2= buf.toString();

		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class E3 {\n");
		buf.append("    public class E3Inner {\n");
		buf.append("        private E3Inner(int i) {}\n");
		buf.append("    }\n");
		buf.append("    private void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3});
	}

	public void testUnusedCode04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int i;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    private int i= 10;\n");
		buf.append("    private int j= 10;\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class E3 {\n");
		buf.append("    private int i;\n");
		buf.append("    private int j;\n");
		buf.append("    private void foo() {\n");
		buf.append("        i= 10;\n");
		buf.append("        i= 20;\n");
		buf.append("        i= j;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("}\n");
		String expected2= buf.toString();

		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class E3 {\n");
		buf.append("    private int j;\n");
		buf.append("    private void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3});
	}

	public void testUnusedCode05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private class E1Inner{}\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    private class E2Inner1 {}\n");
		buf.append("    private class E2Inner2 {}\n");
		buf.append("    public class E2Inner3 {}\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class E3 {\n");
		buf.append("    public class E3Inner {\n");
		buf.append("        private class E3InnerInner {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public class E2Inner3 {}\n");
		buf.append("}\n");
		String expected2= buf.toString();

		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class E3 {\n");
		buf.append("    public class E3Inner {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3});
	}

	public void testUnusedCode06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= 10;\n");
		buf.append("        int j= 10;\n");
		buf.append("    }\n");
		buf.append("    private void bar() {\n");
		buf.append("        int i= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class E3 {\n");
		buf.append("    public class E3Inner {\n");
		buf.append("        public void foo() {\n");
		buf.append("            int i= 10;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= 10;\n");
		buf.append("        int j= i;\n");
		buf.append("        j= 10;\n");
		buf.append("        j= 20;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("    private void bar() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class E3 {\n");
		buf.append("    public class E3Inner {\n");
		buf.append("        public void foo() {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3});
	}

	public void testUnusedCode07() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= bar();\n");
		buf.append("        int j= 1;\n");
		buf.append("    }\n");
		buf.append("    public int bar() {return 1;}\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        bar();\n");
		buf.append("    }\n");
		buf.append("    public int bar() {return 1;}\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testUnusedCode08() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int i= bar();\n");
		buf.append("    private int j= 1;\n");
		buf.append("    public int bar() {return 1;}\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int i= bar();\n");
		buf.append("    public int bar() {return 1;}\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testUnusedCode09() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= 1;\n");
		buf.append("        i= bar();\n");
		buf.append("        int j= 1;\n");
		buf.append("        j= 1;\n");
		buf.append("    }\n");
		buf.append("    public int bar() {return 1;}\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        bar();\n");
		buf.append("    }\n");
		buf.append("    public int bar() {return 1;}\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testUnusedCode10() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int i= 1;\n");
		buf.append("    private int j= 1;\n");
		buf.append("    public void foo() {\n");
		buf.append("        i= bar();\n");
		buf.append("        j= 1;\n");
		buf.append("    }\n");
		buf.append("    public int bar() {return 1;}\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int i= 1;\n");
		buf.append("    public void foo() {\n");
		buf.append("        i= bar();\n");
		buf.append("    }\n");
		buf.append("    public int bar() {return 1;}\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testUnusedCode11() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1  {\n");
		buf.append("    private void foo(String s) {\n");
		buf.append("        String s1= (String)s;\n");
		buf.append("        Object o= (Object)new Object();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    String s1;\n");
		buf.append("    String s2= (String)s1;\n");
		buf.append("    public void foo(Integer i) {\n");
		buf.append("        Number n= (Number)i;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1  {\n");
		buf.append("    private void foo(String s) {\n");
		buf.append("        String s1= s;\n");
		buf.append("        Object o= new Object();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    String s1;\n");
		buf.append("    String s2= s1;\n");
		buf.append("    public void foo(Integer i) {\n");
		buf.append("        Number n= i;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2}, new String[] {expected1, expected2});
	}

	public void testUnusedCodeBug123766() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1  {\n");
		buf.append("    private int i,j;\n");
		buf.append("    public void foo() {\n");
		buf.append("        String s1,s2;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1  {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testUnusedCodeBug150853() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import foo.Bar;\n");
		buf.append("public class E1 {}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testUnusedCodeBug173014_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("        void foo() {\n");
		buf.append("                class Local {}\n");
		buf.append("        }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("        void foo() {\n");
		buf.append("        }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testUnusedCodeBug173014_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static void main(String[] args) {\n");
		buf.append("        class Local {}\n");
		buf.append("        class Local2 {\n");
		buf.append("            class LMember {}\n");
		buf.append("            class LMember2 extends Local2 {}\n");
		buf.append("            LMember m;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static void main(String[] args) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testUnusedCodeBug189394() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Random;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Random ran = new Random();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Random;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        new Random();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testJava5001() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    private int field= 1;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1{\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    private int field1= 1;\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    private int field2= 2;\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    private int field= 1;\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1{\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    private int field1= 1;\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    private int field2= 2;\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2}, new String[] {expected1, expected2});
	}

	public void testJava5002() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    private int f() {return 1;}\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1{\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    private int f1() {return 1;}\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    private int f2() {return 2;}\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    private int f() {return 1;}\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1{\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    private int f1() {return 1;}\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    private int f2() {return 2;}\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2}, new String[] {expected1, expected2});
	}

	public void testJava5003() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" * @deprecated\n");
		buf.append(" */\n");
		buf.append("public class E1 {\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1{\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    private class E2Sub1 {}\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    private class E2Sub2 {}\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" * @deprecated\n");
		buf.append(" */\n");
		buf.append("@Deprecated\n");
		buf.append("public class E1 {\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1{\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    private class E2Sub1 {}\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    private class E2Sub2 {}\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2}, new String[] {expected1, expected2});
	}

	public void testJava5004() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo1() {}\n");
		buf.append("    protected void foo2() {}\n");
		buf.append("    private void foo3() {}\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1 {\n");
		buf.append("    public void foo1() {}\n");
		buf.append("    protected void foo2() {}\n");
		buf.append("    protected void foo3() {}\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("    public void foo1() {}\n");
		buf.append("    protected void foo2() {}\n");
		buf.append("    public void foo3() {}\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);

		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1 {\n");
		buf.append("    @Override\n");
		buf.append("    public void foo1() {}\n");
		buf.append("    @Override\n");
		buf.append("    protected void foo2() {}\n");
		buf.append("    protected void foo3() {}\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("    @Override\n");
		buf.append("    public void foo1() {}\n");
		buf.append("    @Override\n");
		buf.append("    protected void foo2() {}\n");
		buf.append("    @Override\n");
		buf.append("    public void foo3() {}\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {cu1.getBuffer().getContents(), expected1, expected2});
	}

	public void testJava5005() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    public void foo1() {}\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    protected void foo2() {}\n");
		buf.append("    private void foo3() {}\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    public int i;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1 {\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    public void foo1() {}\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    protected void foo2() {}\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    protected void foo3() {}\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    public void foo1() {}\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    protected void foo2() {}\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    public void foo3() {}\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);

		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    public void foo1() {}\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    protected void foo2() {}\n");
		buf.append("    private void foo3() {}\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    public int i;\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1 {\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    @Override\n");
		buf.append("    public void foo1() {}\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    @Override\n");
		buf.append("    protected void foo2() {}\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    protected void foo3() {}\n");
		buf.append("}\n");
		String expected2= buf.toString();

		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    @Override\n");
		buf.append("    public void foo1() {}\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    @Override\n");
		buf.append("    protected void foo2() {}\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    @Override\n");
		buf.append("    public void foo3() {}\n");
		buf.append("}\n");
		String expected3= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3});
	}

	public void testJava50Bug222257() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        ArrayList list= new ArrayList<String>();\n");
		buf.append("        ArrayList list2= new ArrayList<String>();\n");
		buf.append("        \n");
		buf.append("        System.out.println(list);\n");
		buf.append("        System.out.println(list2);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		HashMap map= new HashMap();
		map.put(CleanUpConstants.VARIABLE_DECLARATION_USE_TYPE_ARGUMENTS_FOR_RAW_TYPE_REFERENCES, CleanUpOptions.TRUE);
		Java50CleanUp cleanUp= new Java50CleanUp(map);

		ASTParser parser= ASTParser.newParser(ASTProvider.SHARED_AST_LEVEL);
		parser.setResolveBindings(true);
		parser.setProject(fJProject1);

		Map options= RefactoringASTParser.getCompilerOptions(fJProject1);
		options.putAll(cleanUp.getRequirements().getCompilerOptions());
		parser.setCompilerOptions(options);

		final CompilationUnit[] roots= new CompilationUnit[1];
		parser.createASTs(new ICompilationUnit[] { cu1 }, new String[0], new ASTRequestor() {
			/* (non-Javadoc)
			 * @see org.eclipse.jdt.core.dom.ASTRequestor#acceptAST(org.eclipse.jdt.core.ICompilationUnit, org.eclipse.jdt.core.dom.CompilationUnit)
			 */
			public void acceptAST(ICompilationUnit source, CompilationUnit ast) {
				roots[0]= ast;
			}
		}, null);

		IProblem[] problems= roots[0].getProblems();
		assertTrue(problems.length == 2);
		for (int i= 0; i < problems.length; i++) {
			ProblemLocation location= new ProblemLocation(problems[i]);
			assertTrue(cleanUp.canFix(cu1, location));
		}
	}
	
	public void testAddOverride15() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    void m();\n");
		buf.append("    boolean equals(Object obj);\n");
		buf.append("}\n");
		buf.append("interface J extends I {\n");
		buf.append("    void m(); // @Override error in 1.5, not in 1.6\n");
		buf.append("}\n");
		buf.append("class X implements J {\n");
		buf.append("    public void m() {} // @Override error in 1.5, not in 1.6\n");
		buf.append("    public int hashCode() { return 0; }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("I.java", buf.toString(), false, null);

		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    void m();\n");
		buf.append("    boolean equals(Object obj);\n");
		buf.append("}\n");
		buf.append("interface J extends I {\n");
		buf.append("    void m(); // @Override error in 1.5, not in 1.6\n");
		buf.append("}\n");
		buf.append("class X implements J {\n");
		buf.append("    public void m() {} // @Override error in 1.5, not in 1.6\n");
		buf.append("    @Override\n");
		buf.append("    public int hashCode() { return 0; }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testAddOverride16() throws Exception {
		IJavaProject project= JavaProjectHelper.createJavaProject("CleanUpTestProject", "bin");
		try {
			JavaProjectHelper.addRTJar16(project);
			IPackageFragmentRoot src= JavaProjectHelper.addSourceContainer(project, "src");
			
			IPackageFragment pack1= src.createPackageFragment("test1", false, null);
			StringBuffer buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("interface I {\n");
			buf.append("    void m();\n");
			buf.append("    boolean equals(Object obj);\n");
			buf.append("}\n");
			buf.append("interface J extends I {\n");
			buf.append("    void m(); // @Override error in 1.5, not in 1.6\n");
			buf.append("}\n");
			buf.append("class X implements J {\n");
			buf.append("    public void m() {} // @Override error in 1.5, not in 1.6\n");
			buf.append("    public int hashCode() { return 0; }\n");
			buf.append("}\n");
			ICompilationUnit cu1= pack1.createCompilationUnit("I.java", buf.toString(), false, null);

			enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
			enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);
			enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("interface I {\n");
			buf.append("    void m();\n");
			buf.append("    @Override\n");
			buf.append("    boolean equals(Object obj);\n");
			buf.append("}\n");
			buf.append("interface J extends I {\n");
			buf.append("    @Override\n");
			buf.append("    void m(); // @Override error in 1.5, not in 1.6\n");
			buf.append("}\n");
			buf.append("class X implements J {\n");
			buf.append("    @Override\n");
			buf.append("    public void m() {} // @Override error in 1.5, not in 1.6\n");
			buf.append("    @Override\n");
			buf.append("    public int hashCode() { return 0; }\n");
			buf.append("}\n");
			String expected1= buf.toString();

			assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
		} finally {
			JavaProjectHelper.delete(project);
		}
	}
	
	public void testAddOverride16_no_interface_methods() throws Exception {
		IJavaProject project= JavaProjectHelper.createJavaProject("CleanUpTestProject", "bin");
		try {
			JavaProjectHelper.addRTJar16(project);
			IPackageFragmentRoot src= JavaProjectHelper.addSourceContainer(project, "src");
			
			IPackageFragment pack1= src.createPackageFragment("test1", false, null);
			StringBuffer buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("interface I {\n");
			buf.append("    void m();\n");
			buf.append("    boolean equals(Object obj);\n");
			buf.append("}\n");
			buf.append("interface J extends I {\n");
			buf.append("    void m(); // @Override error in 1.5, not in 1.6\n");
			buf.append("}\n");
			buf.append("class X implements J {\n");
			buf.append("    public void m() {} // @Override error in 1.5, not in 1.6\n");
			buf.append("    public int hashCode() { return 0; }\n");
			buf.append("}\n");
			ICompilationUnit cu1= pack1.createCompilationUnit("I.java", buf.toString(), false, null);
			
			enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
			enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);
			
			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("interface I {\n");
			buf.append("    void m();\n");
			buf.append("    boolean equals(Object obj);\n");
			buf.append("}\n");
			buf.append("interface J extends I {\n");
			buf.append("    void m(); // @Override error in 1.5, not in 1.6\n");
			buf.append("}\n");
			buf.append("class X implements J {\n");
			buf.append("    public void m() {} // @Override error in 1.5, not in 1.6\n");
			buf.append("    @Override\n");
			buf.append("    public int hashCode() { return 0; }\n");
			buf.append("}\n");
			String expected1= buf.toString();
			
			assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
		} finally {
			JavaProjectHelper.delete(project);
		}
	}
	
	public void testCodeStyle01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    String s = \"\"; //$NON-NLS-1$\n");
		buf.append("    String t = \"\";  //$NON-NLS-1$\n");
		buf.append("    \n");
		buf.append("    public void foo() {\n");
		buf.append("        s = \"\"; //$NON-NLS-1$\n");
		buf.append("        s = s + s;\n");
		buf.append("        s = t + s;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1{\n");
		buf.append("    int i = 10;\n");
		buf.append("    \n");
		buf.append("    public class E2Inner {\n");
		buf.append("        public void bar() {\n");
		buf.append("            int j = i;\n");
		buf.append("            String k = s + t;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    public void fooBar() {\n");
		buf.append("        String k = s;\n");
		buf.append("        int j = i;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    String s = \"\"; //$NON-NLS-1$\n");
		buf.append("    String t = \"\";  //$NON-NLS-1$\n");
		buf.append("    \n");
		buf.append("    public void foo() {\n");
		buf.append("        this.s = \"\"; //$NON-NLS-1$\n");
		buf.append("        this.s = this.s + this.s;\n");
		buf.append("        this.s = this.t + this.s;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1{\n");
		buf.append("    int i = 10;\n");
		buf.append("    \n");
		buf.append("    public class E2Inner {\n");
		buf.append("        public void bar() {\n");
		buf.append("            int j = E2.this.i;\n");
		buf.append("            String k = E2.this.s + E2.this.t;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    public void fooBar() {\n");
		buf.append("        String k = this.s;\n");
		buf.append("        int j = this.i;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2}, new String[] {expected1, expected2});
	}

	public void testCodeStyle02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static int i= 0;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    private E1 e1;\n");
		buf.append("    \n");
		buf.append("    public void foo() {\n");
		buf.append("        e1= new E1();\n");
		buf.append("        int j= e1.i;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    private E1 e1;\n");
		buf.append("    \n");
		buf.append("    public void foo() {\n");
		buf.append("        this.e1= new E1();\n");
		buf.append("        int j= E1.i;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2}, new String[] {cu1.getBuffer().getContents(), expected1});
	}

	public void testCodeStyle03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static int f;\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= this.f;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public static String s = \"\";\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(this.s);\n");
		buf.append("        E1 e1= new E1();\n");
		buf.append("        int i= e1.f;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E1;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("    public static int g;\n");
		buf.append("    {\n");
		buf.append("        this.g= (new E1()).f;\n");
		buf.append("    }\n");
		buf.append("    public static int f= E1.f;\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static int f;\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= E1.f;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public static String s = \"\";\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(E2.s);\n");
		buf.append("        E1 e1= new E1();\n");
		buf.append("        int i= E1.f;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E1;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("    public static int g;\n");
		buf.append("    {\n");
		buf.append("        E3.g= E1.f;\n");
		buf.append("    }\n");
		buf.append("    public static int f= E1.f;\n");
		buf.append("}\n");
		String expected3= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3});
	}

	public void testCodeStyle04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static int f() {return 1;}\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= this.f();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public static String s() {return \"\";}\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(this.s());\n");
		buf.append("        E1 e1= new E1();\n");
		buf.append("        int i= e1.f();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E1;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("    public static int g;\n");
		buf.append("    {\n");
		buf.append("        this.g= (new E1()).f();\n");
		buf.append("    }\n");
		buf.append("    public static int f= E1.f();\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static int f() {return 1;}\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= E1.f();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public static String s() {return \"\";}\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(E2.s());\n");
		buf.append("        E1 e1= new E1();\n");
		buf.append("        int i= E1.f();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E1;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("    public static int g;\n");
		buf.append("    {\n");
		buf.append("        E3.g= E1.f();\n");
		buf.append("    }\n");
		buf.append("    public static int f= E1.f();\n");
		buf.append("}\n");
		String expected3= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3});

	}

	public void testCodeStyle05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public String s= \"\";\n");
		buf.append("    public E2 e2;\n");
		buf.append("    public static int i= 10;\n");
		buf.append("    public void foo() {}\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1 {\n");
		buf.append("    public int i = 10;\n");
		buf.append("    public E1 e1;\n");
		buf.append("    public void fooBar() {}\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E1;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 {\n");
		buf.append("    private E1 e1;    \n");
		buf.append("    public void foo() {\n");
		buf.append("        e1= new E1();\n");
		buf.append("        int j= e1.i;\n");
		buf.append("        String s= e1.s;\n");
		buf.append("        e1.foo();\n");
		buf.append("        e1.e2.fooBar();\n");
		buf.append("        int k= e1.e2.e2.e2.i;\n");
		buf.append("        int h= e1.e2.e2.e1.e2.e1.i;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E1;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 {\n");
		buf.append("    private E1 e1;    \n");
		buf.append("    public void foo() {\n");
		buf.append("        this.e1= new E1();\n");
		buf.append("        int j= E1.i;\n");
		buf.append("        String s= this.e1.s;\n");
		buf.append("        this.e1.foo();\n");
		buf.append("        this.e1.e2.fooBar();\n");
		buf.append("        int k= this.e1.e2.e2.e2.i;\n");
		buf.append("        int h= E1.i;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {cu1.getBuffer().getContents(), cu2.getBuffer().getContents(), expected1});
	}

	public void testCodeStyle06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public String s= \"\";\n");
		buf.append("    public E1 create() {\n");
		buf.append("        return new E1();\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        create().s= \"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	public void testCodeStyle07() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static int i = 10;\n");
		buf.append("    private static int j = i + 10 * i;\n");
		buf.append("    public void foo() {\n");
		buf.append("        String s= i + \"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	public void testCodeStyle08() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public final static int i = 1;\n");
		buf.append("    public final int j = 2;\n");
		buf.append("    private final int k = 3;\n");
		buf.append("    public void foo() {\n");
		buf.append("        switch (3) {\n");
		buf.append("        case i: break;\n");
		buf.append("        case j: break;\n");
		buf.append("        case k: break;\n");
		buf.append("        default: break;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	public void testCodeStyle09() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public abstract class E1Inner1 {\n");
		buf.append("        protected int n;\n");
		buf.append("        public abstract void foo();\n");
		buf.append("    }\n");
		buf.append("    public abstract class E1Inner2 {\n");
		buf.append("        public abstract void run();\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        E1Inner1 inner= new E1Inner1() {\n");
		buf.append("            public void foo() {\n");
		buf.append("                E1Inner2 inner2= new E1Inner2() {\n");
		buf.append("                    public void run() {\n");
		buf.append("                        System.out.println(n);\n");
		buf.append("                    }\n");
		buf.append("                };\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	public void testCodeStyle10() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private static final int N;\n");
		buf.append("    static {N= 10;}\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	public void testCodeStyle11() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {    \n");
		buf.append("    public static int E1N = 10;\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(E1N);\n");
		buf.append("        E1N = 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1 {\n");
		buf.append("    public static int E2N = 10;\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(E1N);\n");
		buf.append("        E1N = 10;\n");
		buf.append("        System.out.println(E2N);\n");
		buf.append("        E2N = 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("    private static int E3N = 10;\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(E1N);\n");
		buf.append("        E1N = 10;\n");
		buf.append("        System.out.println(E2N);\n");
		buf.append("        E2N = 10;\n");
		buf.append("        System.out.println(E3N);\n");
		buf.append("        E3N = 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {    \n");
		buf.append("    public static int E1N = 10;\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(E1.E1N);\n");
		buf.append("        E1.E1N = 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1 {\n");
		buf.append("    public static int E2N = 10;\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(E1.E1N);\n");
		buf.append("        E1.E1N = 10;\n");
		buf.append("        System.out.println(E2.E2N);\n");
		buf.append("        E2.E2N = 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E1;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("    private static int E3N = 10;\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(E1.E1N);\n");
		buf.append("        E1.E1N = 10;\n");
		buf.append("        System.out.println(E2.E2N);\n");
		buf.append("        E2.E2N = 10;\n");
		buf.append("        System.out.println(E3.E3N);\n");
		buf.append("        E3.E3N = 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3});

	}

	public void testCodeStyle12() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public final static int N1 = 10;\n");
		buf.append("    public static int N2 = N1;\n");
		buf.append("    {\n");
		buf.append("        System.out.println(N1);\n");
		buf.append("        N2 = 10;\n");
		buf.append("        System.out.println(N2);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public final static int N1 = 10;\n");
		buf.append("    public static int N2 = E1.N1;\n");
		buf.append("    {\n");
		buf.append("        System.out.println(E1.N1);\n");
		buf.append("        E1.N2 = 10;\n");
		buf.append("        System.out.println(E1.N2);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCodeStyle13() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private static class E1Inner {\n");
		buf.append("        private static class E1InnerInner {\n");
		buf.append("            public static int N = 10;\n");
		buf.append("            static {\n");
		buf.append("                System.out.println(N);\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private static class E1Inner {\n");
		buf.append("        private static class E1InnerInner {\n");
		buf.append("            public static int N = 10;\n");
		buf.append("            static {\n");
		buf.append("                System.out.println(E1InnerInner.N);\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCodeStyle14() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    static class E1Inner {\n");
		buf.append("        public static class E1InnerInner {\n");
		buf.append("            public static int N = 10;\n");
		buf.append("            public void foo() {\n");
		buf.append("                System.out.println(N);\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    static class E1Inner {\n");
		buf.append("        public static class E1InnerInner {\n");
		buf.append("            public static int N = 10;\n");
		buf.append("            public void foo() {\n");
		buf.append("                System.out.println(E1InnerInner.N);\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCodeStyle15() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    static class E1Inner {\n");
		buf.append("        public static class E1InnerInner {\n");
		buf.append("            public static int N = 10;\n");
		buf.append("            public void foo() {\n");
		buf.append("                System.out.println((new E1InnerInner()).N);\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    static class E1Inner {\n");
		buf.append("        public static class E1InnerInner {\n");
		buf.append("            public static int N = 10;\n");
		buf.append("            public void foo() {\n");
		buf.append("                System.out.println(E1InnerInner.N);\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCodeStyle16() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static int E1N;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1 {\n");
		buf.append("    public static int E2N = 10;\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(E1.E1N);\n");
		buf.append("        System.out.println(E2.E1N);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E1;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(E1.E1N);\n");
		buf.append("        System.out.println(E2.E1N);\n");
		buf.append("        System.out.println(E3.E1N);\n");
		buf.append("        System.out.println(E2.E2N);\n");
		buf.append("        System.out.println(E3.E2N);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1 {\n");
		buf.append("    public static int E2N = 10;\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(E1.E1N);\n");
		buf.append("        System.out.println(E1.E1N);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E1;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(E1.E1N);\n");
		buf.append("        System.out.println(E1.E1N);\n");
		buf.append("        System.out.println(E1.E1N);\n");
		buf.append("        System.out.println(E2.E2N);\n");
		buf.append("        System.out.println(E2.E2N);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {cu1.getBuffer().getContents(), expected1, expected2});

	}

	public void testCodeStyle17() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public boolean b= true;\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (b)\n");
		buf.append("            System.out.println(10);\n");
		buf.append("        if (b) {\n");
		buf.append("            System.out.println(10);\n");
		buf.append("        } else\n");
		buf.append("            System.out.println(10);\n");
		buf.append("        if (b)\n");
		buf.append("            System.out.println(10);\n");
		buf.append("        else\n");
		buf.append("            System.out.println(10);\n");
		buf.append("        while (b)\n");
		buf.append("            System.out.println(10);\n");
		buf.append("        do\n");
		buf.append("            System.out.println(10);\n");
		buf.append("        while (b);\n");
		buf.append("        for(;;)\n");
		buf.append("            System.out.println(10);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public boolean b= true;\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (b) {\n");
		buf.append("            System.out.println(10);\n");
		buf.append("        }\n");
		buf.append("        if (b) {\n");
		buf.append("            System.out.println(10);\n");
		buf.append("        } else {\n");
		buf.append("            System.out.println(10);\n");
		buf.append("        }\n");
		buf.append("        if (b) {\n");
		buf.append("            System.out.println(10);\n");
		buf.append("        } else {\n");
		buf.append("            System.out.println(10);\n");
		buf.append("        }\n");
		buf.append("        while (b) {\n");
		buf.append("            System.out.println(10);\n");
		buf.append("        }\n");
		buf.append("        do {\n");
		buf.append("            System.out.println(10);\n");
		buf.append("        } while (b);\n");
		buf.append("        for(;;) {\n");
		buf.append("            System.out.println(10);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCodeStyle18() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public boolean b, q;\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (b)\n");
		buf.append("            System.out.println(1);\n");
		buf.append("        else if (q)\n");
		buf.append("            System.out.println(1);\n");
		buf.append("        else\n");
		buf.append("            if (b && q)\n");
		buf.append("                System.out.println(1);\n");
		buf.append("            else\n");
		buf.append("                System.out.println(2);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public boolean b, q;\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (b) {\n");
		buf.append("            System.out.println(1);\n");
		buf.append("        } else if (q) {\n");
		buf.append("            System.out.println(1);\n");
		buf.append("        } else\n");
		buf.append("            if (b && q) {\n");
		buf.append("                System.out.println(1);\n");
		buf.append("            } else {\n");
		buf.append("                System.out.println(2);\n");
		buf.append("            }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCodeStyle19() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public boolean b, q;\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (;b;)\n");
		buf.append("            for (;q;)\n");
		buf.append("                if (b)\n");
		buf.append("                    System.out.println(1);\n");
		buf.append("                else if (q)\n");
		buf.append("                    System.out.println(2);\n");
		buf.append("                else\n");
		buf.append("                    System.out.println(3);\n");
		buf.append("        for (;b;)\n");
		buf.append("            for (;q;) {\n");
		buf.append("                \n");
		buf.append("            }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public boolean b, q;\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (;b;) {\n");
		buf.append("            for (;q;) {\n");
		buf.append("                if (b) {\n");
		buf.append("                    System.out.println(1);\n");
		buf.append("                } else if (q) {\n");
		buf.append("                    System.out.println(2);\n");
		buf.append("                } else {\n");
		buf.append("                    System.out.println(3);\n");
		buf.append("                }\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("        for (;b;) {\n");
		buf.append("            for (;q;) {\n");
		buf.append("                \n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCodeStyle20() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public boolean b, q;\n");
		buf.append("    public void foo() {\n");
		buf.append("        while (b)\n");
		buf.append("            while (q)\n");
		buf.append("                if (b)\n");
		buf.append("                    System.out.println(1);\n");
		buf.append("                else if (q)\n");
		buf.append("                    System.out.println(2);\n");
		buf.append("                else\n");
		buf.append("                    System.out.println(3);\n");
		buf.append("        while (b)\n");
		buf.append("            while (q) {\n");
		buf.append("                \n");
		buf.append("            }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public boolean b, q;\n");
		buf.append("    public void foo() {\n");
		buf.append("        while (b) {\n");
		buf.append("            while (q) {\n");
		buf.append("                if (b) {\n");
		buf.append("                    System.out.println(1);\n");
		buf.append("                } else if (q) {\n");
		buf.append("                    System.out.println(2);\n");
		buf.append("                } else {\n");
		buf.append("                    System.out.println(3);\n");
		buf.append("                }\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("        while (b) {\n");
		buf.append("            while (q) {\n");
		buf.append("                \n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCodeStyle21() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public boolean b, q;\n");
		buf.append("    public void foo() {\n");
		buf.append("        do\n");
		buf.append("            do\n");
		buf.append("                if (b)\n");
		buf.append("                    System.out.println(1);\n");
		buf.append("                else if (q)\n");
		buf.append("                    System.out.println(2);\n");
		buf.append("                else\n");
		buf.append("                    System.out.println(3);\n");
		buf.append("            while (q);\n");
		buf.append("        while (b);\n");
		buf.append("        do\n");
		buf.append("            do {\n");
		buf.append("                \n");
		buf.append("            } while (q);\n");
		buf.append("        while (b);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public boolean b, q;\n");
		buf.append("    public void foo() {\n");
		buf.append("        do {\n");
		buf.append("            do {\n");
		buf.append("                if (b) {\n");
		buf.append("                    System.out.println(1);\n");
		buf.append("                } else if (q) {\n");
		buf.append("                    System.out.println(2);\n");
		buf.append("                } else {\n");
		buf.append("                    System.out.println(3);\n");
		buf.append("                }\n");
		buf.append("            } while (q);\n");
		buf.append("        } while (b);\n");
		buf.append("        do {\n");
		buf.append("            do {\n");
		buf.append("                \n");
		buf.append("            } while (q);\n");
		buf.append("        } while (b);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCodeStyle22() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.I1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        I1 i1= new I1() {\n");
		buf.append("            private static final int N= 10;\n");
		buf.append("            public void foo() {\n");
		buf.append("                System.out.println(N);\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public interface I1 {}\n");
		pack2.createCompilationUnit("I1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	public void testCodeStyle23() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int fNb= 0;\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true)\n");
		buf.append("            fNb++;\n");
		buf.append("        String s; //$NON-NLS-1$\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.REMOVE_UNNECESSARY_NLS_TAGS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int fNb= 0;\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("            this.fNb++;\n");
		buf.append("        }\n");
		buf.append("        String s; \n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCodeStyle24() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true)\n");
		buf.append("            System.out.println(\"\");\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.REMOVE_UNNECESSARY_NLS_TAGS);
		enable(CleanUpConstants.ADD_MISSING_NLS_TAGS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("            System.out.println(\"\"); //$NON-NLS-1$\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCodeStyle25() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(I1Impl.N);\n");
		buf.append("        I1 i1= new I1();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.I1;\n");
		buf.append("public class I1Impl implements I1 {}\n");
		pack1.createCompilationUnit("I1Impl.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class I1 {}\n");
		pack1.createCompilationUnit("I1.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public interface I1 {\n");
		buf.append("    public static int N= 10;\n");
		buf.append("}\n");
		pack2.createCompilationUnit("I1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(test2.I1.N);\n");
		buf.append("        I1 i1= new I1();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCodeStyle26() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {}\n");
		buf.append("    private void bar() {\n");
		buf.append("        foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {}\n");
		buf.append("    private void bar() {\n");
		buf.append("        this.foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCodeStyle27() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static void foo() {}\n");
		buf.append("    private void bar() {\n");
		buf.append("        foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static void foo() {}\n");
		buf.append("    private void bar() {\n");
		buf.append("        E1.foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCodeStyleBug118204() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    static String s;\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(s);\n");
		buf.append("    }\n");
		buf.append("    E1(){}\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    static String s;\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(E1.s);\n");
		buf.append("    }\n");
		buf.append("    E1(){}\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCodeStyleBug114544() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(new E1().i);\n");
		buf.append("    }\n");
		buf.append("    public static int i= 10;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        new E1();\n");
		buf.append("        System.out.println(E1.i);\n");
		buf.append("    }\n");
		buf.append("    public static int i= 10;\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCodeStyleBug119170_01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static void foo() {}\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    private static class E1 {}\n");
		buf.append("    public void bar() {\n");
		buf.append("        test1.E1 e1= new test1.E1();\n");
		buf.append("        e1.foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    private static class E1 {}\n");
		buf.append("    public void bar() {\n");
		buf.append("        test1.E1 e1= new test1.E1();\n");
		buf.append("        test1.E1.foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCodeStyleBug119170_02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static void foo() {}\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    private static String E1= \"\";\n");
		buf.append("    public void foo() {\n");
		buf.append("        test1.E1 e1= new test1.E1();\n");
		buf.append("        e1.foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    private static String E1= \"\";\n");
		buf.append("    public void foo() {\n");
		buf.append("        test1.E1 e1= new test1.E1();\n");
		buf.append("        test1.E1.foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCodeStyleBug123468() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    protected int field;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1 {\n");
		buf.append("    private int field;\n");
		buf.append("    public void foo() {\n");
		buf.append("        super.field= 10;\n");
		buf.append("        field= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1 {\n");
		buf.append("    private int field;\n");
		buf.append("    public void foo() {\n");
		buf.append("        super.field= 10;\n");
		buf.append("        this.field= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCodeStyleBug129115() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private static int NUMBER;\n");
		buf.append("    public void reset() {\n");
		buf.append("        NUMBER= 0;\n");
		buf.append("    }\n");
		buf.append("    enum MyEnum {\n");
		buf.append("        STATE_1, STATE_2, STATE_3\n");
		buf.append("      };\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private static int NUMBER;\n");
		buf.append("    public void reset() {\n");
		buf.append("        E1.NUMBER= 0;\n");
		buf.append("    }\n");
		buf.append("    enum MyEnum {\n");
		buf.append("        STATE_1, STATE_2, STATE_3\n");
		buf.append("      };\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCodeStyleBug135219() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E { \n");
		buf.append("    public int i;\n");
		buf.append("    public void print(int j) {}\n");
		buf.append("    public void foo() {\n");
		buf.append("        print(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E { \n");
		buf.append("    public int i;\n");
		buf.append("    public void print(int j) {}\n");
		buf.append("    public void foo() {\n");
		buf.append("        this.print(this.i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCodeStyleBug_138318() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E<I> {\n");
		buf.append("    private static int I;\n");
		buf.append("    private static String STR() {return \"\";}\n");
		buf.append("    public void foo() {\n");
		buf.append("        Runnable runnable = new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                System.out.println(I);\n");
		buf.append("                System.out.println(STR());\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E<I> {\n");
		buf.append("    private static int I;\n");
		buf.append("    private static String STR() {return \"\";}\n");
		buf.append("    public void foo() {\n");
		buf.append("        Runnable runnable = new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                System.out.println(E.I);\n");
		buf.append("                System.out.println(E.STR());\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCodeStyleBug138325_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E<I> {\n");
		buf.append("    private int i;\n");
		buf.append("    private String str() {return \"\";}\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(str());\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E<I> {\n");
		buf.append("    private int i;\n");
		buf.append("    private String str() {return \"\";}\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(this.i);\n");
		buf.append("        System.out.println(this.str());\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCodeStyleBug138325_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E<I> {\n");
		buf.append("    private int i;\n");
		buf.append("    private String str() {return \"\";}\n");
		buf.append("    public void foo() {\n");
		buf.append("        Runnable runnable = new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                System.out.println(i);\n");
		buf.append("                System.out.println(str());\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E<I> {\n");
		buf.append("    private int i;\n");
		buf.append("    private String str() {return \"\";}\n");
		buf.append("    public void foo() {\n");
		buf.append("        Runnable runnable = new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                System.out.println(E.this.i);\n");
		buf.append("                System.out.println(E.this.str());\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCodeStyle_Bug140565() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.*;\n");
		buf.append("public class E1 {\n");
		buf.append("        static class ClassA {static ClassB B;}\n");
		buf.append("        static class ClassB {static ClassC C;}\n");
		buf.append("        static class ClassC {static ClassD D;}\n");
		buf.append("        static class ClassD {}\n");
		buf.append("\n");
		buf.append("        public void foo() {\n");
		buf.append("                ClassA.B.C.D.toString();\n");
		buf.append("        }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("        static class ClassA {static ClassB B;}\n");
		buf.append("        static class ClassB {static ClassC C;}\n");
		buf.append("        static class ClassC {static ClassD D;}\n");
		buf.append("        static class ClassD {}\n");
		buf.append("\n");
		buf.append("        public void foo() {\n");
		buf.append("                ClassC.D.toString();\n");
		buf.append("        }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCodeStyleBug157480() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 extends ETop {\n");
		buf.append("    public void bar(boolean b) {\n");
		buf.append("        if (b == true && b || b) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class ETop {\n");
		buf.append("    public void bar(boolean b) {}\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 extends ETop {\n");
		buf.append("    @Override\n");
		buf.append("    public void bar(boolean b) {\n");
		buf.append("        if (((b == true) && b) || b) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class ETop {\n");
		buf.append("    public void bar(boolean b) {}\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCodeStyleBug154787() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface E1 {String FOO = \"FOO\";}\n");
		pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 implements E1 {}\n");
		pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 {\n");
		buf.append("    public String foo() {\n");
		buf.append("        return E2.FOO;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	public void testCodeStyleBug189398() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(Object o) {\n");
		buf.append("        if (o != null)\n");
		buf.append("            System.out.println(o);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(Object o) {\n");
		buf.append("        if (o != null) {\n");
		buf.append("            System.out.println(o);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testCodeStyleBug238828_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int field;\n");
		buf.append("\n");
		buf.append("    public String foo() {\n");
		buf.append("        return \"Foo\" + field //MyComment\n");
		buf.append("                    + field;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int field;\n");
		buf.append("\n");
		buf.append("    public String foo() {\n");
		buf.append("        return \"Foo\" + this.field //MyComment\n");
		buf.append("                    + this.field;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testCodeStyleBug238828_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private static int FIELD;\n");
		buf.append("\n");
		buf.append("    public String foo() {\n");
		buf.append("        return \"Foo\" + FIELD //MyComment\n");
		buf.append("                    + FIELD;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private static int FIELD;\n");
		buf.append("\n");
		buf.append("    public String foo() {\n");
		buf.append("        return \"Foo\" + E1.FIELD //MyComment\n");
		buf.append("                    + E1.FIELD;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testRemoveNonStaticQualifier_Bug219204_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void test() {\n");
		buf.append("        E1 t = E1.bar().g().g().foo(E1.foo(null).bar()).bar();\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private static E1 foo(E1 t) {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private static E1 bar() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private E1 g() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void test() {\n");
		buf.append("        E1.bar().g().g();\n");
		buf.append("        E1.foo(E1.foo(null).bar());\n");
		buf.append("        E1 t = E1.bar();\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private static E1 foo(E1 t) {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private static E1 bar() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private E1 g() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testRemoveNonStaticQualifier_Bug219204_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void test() {\n");
		buf.append("        while (true)\n");
		buf.append("            new E1().bar1().bar2().bar3();\n");
		buf.append("    }\n");
		buf.append("    private static E1 bar1() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("    private static E1 bar2() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("    private static E1 bar3() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void test() {\n");
		buf.append("        while (true) {\n");
		buf.append("            new E1();\n");
		buf.append("            E1.bar1();\n");
		buf.append("            E1.bar2();\n");
		buf.append("            E1.bar3();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private static E1 bar1() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("    private static E1 bar2() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("    private static E1 bar3() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testJava50ForLoop01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        List<E1> list= new ArrayList<E1>();\n");
		buf.append("        for (Iterator<E1> iter = list.iterator(); iter.hasNext();) {\n");
		buf.append("            E1 e = iter.next();\n");
		buf.append("            System.out.println(e);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        List<E1> list= new ArrayList<E1>();\n");
		buf.append("        for (E1 e : list) {\n");
		buf.append("            System.out.println(e);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testJava50ForLoop02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        List<E1> list1= new ArrayList<E1>();\n");
		buf.append("        List<E1> list2= new ArrayList<E1>();\n");
		buf.append("        for (Iterator<E1> iter = list1.iterator(); iter.hasNext();) {\n");
		buf.append("            E1 e1 = iter.next();\n");
		buf.append("            for (Iterator iterator = list2.iterator(); iterator.hasNext();) {\n");
		buf.append("                E1 e2 = (E1) iterator.next();\n");
		buf.append("                System.out.println(e2);\n");
		buf.append("            }\n");
		buf.append("            System.out.println(e1);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        List<E1> list1= new ArrayList<E1>();\n");
		buf.append("        List<E1> list2= new ArrayList<E1>();\n");
		buf.append("        for (E1 e1 : list1) {\n");
		buf.append("            for (Object element : list2) {\n");
		buf.append("                E1 e2 = (E1) element;\n");
		buf.append("                System.out.println(e2);\n");
		buf.append("            }\n");
		buf.append("            System.out.println(e1);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testJava50ForLoop03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int[] array={1,2,3,4};\n");
		buf.append("        for (int i=0;i<array.length;i++) {\n");
		buf.append("            String[] strs={\"1\",\"2\"};\n");
		buf.append("            for (int j = 0; j < strs.length; j++) {\n");
		buf.append("                System.out.println(array[i]+strs[j]);\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int[] array={1,2,3,4};\n");
		buf.append("        for (int element : array) {\n");
		buf.append("            String[] strs={\"1\",\"2\"};\n");
		buf.append("            for (String str : strs) {\n");
		buf.append("                System.out.println(element+str);\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testJava50ForLoop04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int[] array= new int[10];\n");
		buf.append("        for (int i = 0; i < array.length; i++) {\n");
		buf.append("            for (int j = 0; j < array.length; j++) {\n");
		buf.append("                for (int k = 0; k < array.length; k++) {\n");
		buf.append("                }\n");
		buf.append("                for (int k = 0; k < array.length; k++) {\n");
		buf.append("                }\n");
		buf.append("            }\n");
		buf.append("            for (int j = 0; j < array.length; j++) {\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int[] array= new int[10];\n");
		buf.append("        for (int element : array) {\n");
		buf.append("            for (int element2 : array) {\n");
		buf.append("                for (int element3 : array) {\n");
		buf.append("                }\n");
		buf.append("                for (int element3 : array) {\n");
		buf.append("                }\n");
		buf.append("            }\n");
		buf.append("            for (int element2 : array) {\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testJava50ForLoop05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int[] array= null;\n");
		buf.append("        for (int i = 0; --i < array.length;) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	public void testJava50ForLoop06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int a= 0;\n");
		buf.append("        for (a=0;a>0;a++) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	public void testJava50ForLoop07() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int a= 0;\n");
		buf.append("        for (a=0;;a++) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	public void testJava50ForLoop08() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int[] array= null;\n");
		buf.append("        int a= 0;\n");
		buf.append("        for (;a<array.length;a++) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	public void testJava50ForLoop09() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int[] array= null;\n");
		buf.append("        for (int i = 0; i < array.length; i++) {\n");
		buf.append("            final int element= array[i];\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int[] array= null;\n");
		buf.append("        for (final int element : array) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testJava50ForLoop10() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int[] array= null;\n");
		buf.append("        int i;\n");
		buf.append("        for (i = 0; i < array.length; i++) {}\n");
		buf.append("        System.out.println(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	public void testJava50ForLoop11() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private class E1Sub {\n");
		buf.append("        public int[] array;\n");
		buf.append("    }\n");
		buf.append("    private E1Sub e1sub;\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int i = 0; i < this.e1sub.array.length; i++) {\n");
		buf.append("            System.out.println(this.e1sub.array[i]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private class E1Sub {\n");
		buf.append("        public int[] array;\n");
		buf.append("    }\n");
		buf.append("    private E1Sub e1sub;\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int element : this.e1sub.array) {\n");
		buf.append("            System.out.println(element);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testJava50ForLoop12() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public int[] array;\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int i = 0; i < this.array.length; i++) {\n");
		buf.append("            System.out.println(this.array[i]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public int[] array;\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int element : this.array) {\n");
		buf.append("            System.out.println(element);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testJava50ForLoop13() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public int[] array1, array2;\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int i = array1.length - array2.length; i < 1; i++) {\n");
		buf.append("            System.out.println(1);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	public void testJava50ForLoop14() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.E3;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        E2 e2= new E2();\n");
		buf.append("        e2.foo();\n");
		buf.append("        E3 e3= new E3();\n");
		buf.append("        for (int i = 0; i < e3.array.length;i++) {\n");
		buf.append("            System.out.println(e3.array[i]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public void foo() {};\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class E2 {}\n");
		pack2.createCompilationUnit("E2.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class E3 {\n");
		buf.append("    public E2[] array;\n");
		buf.append("}\n");
		pack2.createCompilationUnit("E3.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.E3;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        E2 e2= new E2();\n");
		buf.append("        e2.foo();\n");
		buf.append("        E3 e3= new E3();\n");
		buf.append("        for (test2.E2 element : e3.array) {\n");
		buf.append("            System.out.println(element);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testJava50ForLoopBug154939() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(List<Integer> list) {\n");
		buf.append("       for (Iterator<Integer> iter = list.iterator(); iter.hasNext() && false;) {\n");
		buf.append("            Integer id = iter.next();\n");
		buf.append("       } \n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	public void testJava50ForLoop160218() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    void bar(List<Number> x) {\n");
		buf.append("        if (true) {\n");
		buf.append("            for (Iterator<Number> i = x.iterator(); i.hasNext();)\n");
		buf.append("                System.out.println(i.next());\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_NEVER);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    void bar(List<Number> x) {\n");
		buf.append("        if (true)\n");
		buf.append("            for (Number number : x)\n");
		buf.append("                System.out.println(number);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testJava50ForLoop159449() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(Object[] objs) {\n");
		buf.append("        if (objs != null)\n");
		buf.append("            for (int i = 0; i < objs.length; i++) {\n");
		buf.append("                System.out.println(objs[i]);\n");
		buf.append("            }\n");
		buf.append("    }\n");
		buf.append("    public void bar(List<Object> objs) {\n");
		buf.append("        if (objs != null)\n");
		buf.append("            for (Iterator<Object> i = objs.iterator(); i.hasNext();) {\n");
		buf.append("                System.out.println(i.next());\n");
		buf.append("            }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(Object[] objs) {\n");
		buf.append("        if (objs != null) {\n");
		buf.append("            for (Object obj : objs) {\n");
		buf.append("                System.out.println(obj);\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void bar(List<Object> objs) {\n");
		buf.append("        if (objs != null) {\n");
		buf.append("            for (Object object : objs) {\n");
		buf.append("                System.out.println(object);\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testJava50ForLoop160283_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    void foo(Object[] x) {\n");
		buf.append("        for (int i = 0; i < x.length; i++) {\n");
		buf.append("            System.out.println(x[i]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    void bar(List<Object> x) {\n");
		buf.append("        for (Iterator<Object> i = x.iterator(); i.hasNext();) {\n");
		buf.append("            System.out.println(i.next());\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_NEVER);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    void foo(Object[] x) {\n");
		buf.append("        for (Object element : x)\n");
		buf.append("            System.out.println(element);\n");
		buf.append("    }\n");
		buf.append("    void bar(List<Object> x) {\n");
		buf.append("        for (Object object : x)\n");
		buf.append("            System.out.println(object);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testJava50ForLoop160283_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    void foo(Object[] x) {\n");
		buf.append("        for (int i = 0; i < x.length; i++)\n");
		buf.append("            System.out.println(x[i]);\n");
		buf.append("    }\n");
		buf.append("    void bar(List<Object> x) {\n");
		buf.append("        for (Iterator<Object> i = x.iterator(); i.hasNext();)\n");
		buf.append("            System.out.println(i.next());\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    void foo(Object[] x) {\n");
		buf.append("        for (Object element : x) {\n");
		buf.append("            System.out.println(element);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    void bar(List<Object> x) {\n");
		buf.append("        for (Object object : x) {\n");
		buf.append("            System.out.println(object);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testJava50ForLoop160312() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    void foo(Object[] x, Object[] y) {\n");
		buf.append("        for (int i = 0; i < y.length; i++)\n");
		buf.append("            for (int j = 0; j < x.length; j++)\n");
		buf.append("                System.out.println(x[j]);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    void foo(Object[] x, Object[] y) {\n");
		buf.append("        for (Object element : y) {\n");
		buf.append("            for (Object element2 : x) {\n");
		buf.append("                System.out.println(element2);\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testJava50ForLoop160270() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    void foo(List<Object> y) {\n");
		buf.append("        for (Iterator<Object> it = y.iterator(); it.hasNext();) {\n");
		buf.append("            System.out.println(it.next());\n");
		buf.append("        }\n");
		buf.append("        \n");
		buf.append("        int j= 0;\n");
		buf.append("        for (Iterator<Object> it = y.iterator(); it.hasNext(); j++) {\n");
		buf.append("            System.out.println(it.next());\n");
		buf.append("        }\n");
		buf.append("        \n");
		buf.append("        for (Iterator<Object> it = y.iterator(); it.hasNext(); bar()) {\n");
		buf.append("            System.out.println(it.next());\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void bar() {}\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    void foo(List<Object> y) {\n");
		buf.append("        for (Object object : y) {\n");
		buf.append("            System.out.println(object);\n");
		buf.append("        }\n");
		buf.append("        \n");
		buf.append("        int j= 0;\n");
		buf.append("        for (Iterator<Object> it = y.iterator(); it.hasNext(); j++) {\n");
		buf.append("            System.out.println(it.next());\n");
		buf.append("        }\n");
		buf.append("        \n");
		buf.append("        for (Iterator<Object> it = y.iterator(); it.hasNext(); bar()) {\n");
		buf.append("            System.out.println(it.next());\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void bar() {}\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testJava50ForLoop163122_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    void foo(Object[] x, Object[] y) {\n");
		buf.append("        for (int i = 0; i < y.length; i++)\n");
		buf.append("            for (int j = 0; j < x.length; j++)\n");
		buf.append("                System.out.println(y[i]);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    void foo(Object[] x, Object[] y) {\n");
		buf.append("        for (Object element : y) {\n");
		buf.append("            for (Object element2 : x) {\n");
		buf.append("                System.out.println(element);\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testJava50ForLoop163122_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    void foo(Object[] x, Object[] y) {\n");
		buf.append("        for (int i = 0; i < y.length; i++)\n");
		buf.append("            for (int j = 0; j < x.length; j++)\n");
		buf.append("                System.out.println(y[i]);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    void foo(Object[] x, Object[] y) {\n");
		buf.append("        for (Object element : y)\n");
		buf.append("            for (Object element2 : x)\n");
		buf.append("                System.out.println(element);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testJava50ForLoop163122_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    void foo(Object[] x, Object[] y) {\n");
		buf.append("        for (int i = 0; i < y.length; i++)\n");
		buf.append("            for (int j = 0; j < x.length; j++)\n");
		buf.append("                System.out.println(x[i]);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    void foo(Object[] x, Object[] y) {\n");
		buf.append("        for (int i = 0; i < y.length; i++) {\n");
		buf.append("            for (Object element : x) {\n");
		buf.append("                System.out.println(x[i]);\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testJava50ForLoop163122_4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    void foo(Object[] x, Object[] y) {\n");
		buf.append("        for (int i = 0; i < y.length; i++)\n");
		buf.append("            for (int j = 0; j < x.length; j++)\n");
		buf.append("                System.out.println(x[i]);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    void foo(Object[] x, Object[] y) {\n");
		buf.append("        for (int i = 0; i < y.length; i++)\n");
		buf.append("            for (Object element : x)\n");
		buf.append("                System.out.println(x[i]);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testJava50ForLoop163122_5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    void foo(Object[] x, Object[] y) {\n");
		buf.append("        for (int i = 0; i < y.length; i++)\n");
		buf.append("            for (int j = 0; j < x.length; j++)\n");
		buf.append("                System.out.println(x[j]);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    void foo(Object[] x, Object[] y) {\n");
		buf.append("        for (Object element : y) {\n");
		buf.append("            for (Object element2 : x) {\n");
		buf.append("                System.out.println(element2);\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testJava50ForLoop110599() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void a(int[] i, List<String> l) {\n");
		buf.append("        //Comment\n");
		buf.append("        for (int j = 0; j < i.length; j++) {\n");
		buf.append("            System.out.println(i[j]);\n");
		buf.append("        }\n");
		buf.append("        //Comment\n");
		buf.append("        for (Iterator<String> iterator = l.iterator(); iterator.hasNext();) {\n");
		buf.append("            String str = iterator.next();\n");
		buf.append("            System.out.println(str);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void a(int[] i, List<String> l) {\n");
		buf.append("        //Comment\n");
		buf.append("        for (int element : i) {\n");
		buf.append("            System.out.println(element);\n");
		buf.append("        }\n");
		buf.append("        //Comment\n");
		buf.append("        for (String str : l) {\n");
		buf.append("            System.out.println(str);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testJava50ForLoop269595() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void a(int[] array) {\n");
		buf.append("        for (int i = 0; i < array.length; i++) {\n");
		buf.append("            final int value = array[i];\n");
		buf.append("            System.out.println(value);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void a(final int[] array) {\n");
		buf.append("        for (final int value : array) {\n");
		buf.append("            System.out.println(value);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testJava50ForLoop264421() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(String[] src) {\n");
		buf.append("        for (int i = 0; i < src.length; i++) {\n");
		buf.append("            String path = src[i];\n");
		buf.append("            String output = path;\n");
		buf.append("            if (output.length() == 1) {\n");
		buf.append("                output = output + \"-XXX\";\n");
		buf.append("            }\n");
		buf.append("            System.err.println(\"path=\"+ path + \",output=\"+output);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(String[] src) {\n");
		buf.append("        for (String path : src) {\n");
		buf.append("            String output = path;\n");
		buf.append("            if (output.length() == 1) {\n");
		buf.append("                output = output + \"-XXX\";\n");
		buf.append("            }\n");
		buf.append("            System.err.println(\"path=\"+ path + \",output=\"+output);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testJava50ForLoop274199() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static void main(String[] args) {\n");
		buf.append("        for (int i = 0; i < args.length; i++) {\n");
		buf.append("            String output = args[i];\n");
		buf.append("            if (output.length() == 1) {\n");
		buf.append("                output = output + \"-XXX\";\n");
		buf.append("            }\n");
		buf.append("\n");
		buf.append("            String s = \"path=\" + args[i] + \",output=\" + output;\n");
		buf.append("        }\n");
		buf.append("        \n");
		buf.append("        for (int i = 0; i < args.length; i++) {\n");
		buf.append("            String output = args[i];\n");
		buf.append("            String output1 = output;\n");
		buf.append("            if (output1.length() == 1) {\n");
		buf.append("                output1 = output1 + \"-XXX\";\n");
		buf.append("            }\n");
		buf.append("\n");
		buf.append("            String s = \"path=\" + args[i] + \",output=\" + output1;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static void main(String[] args) {\n");
		buf.append("        for (String arg : args) {\n");
		buf.append("            String output = arg;\n");
		buf.append("            if (output.length() == 1) {\n");
		buf.append("                output = output + \"-XXX\";\n");
		buf.append("            }\n");
		buf.append("\n");
		buf.append("            String s = \"path=\" + arg + \",output=\" + output;\n");
		buf.append("        }\n");
		buf.append("        \n");
		buf.append("        for (String output : args) {\n");
		buf.append("            String output1 = output;\n");
		buf.append("            if (output1.length() == 1) {\n");
		buf.append("                output1 = output1 + \"-XXX\";\n");
		buf.append("            }\n");
		buf.append("\n");
		buf.append("            String s = \"path=\" + output + \",output=\" + output1;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testCombination01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class E1 {\n");
		buf.append("    private int i= 10;\n");
		buf.append("    private int j= 20;\n");
		buf.append("    \n");
		buf.append("    public void foo() {\n");
		buf.append("        i= j;\n");
		buf.append("        i= 20;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class E1 {\n");
		buf.append("    private int j= 20;\n");
		buf.append("    \n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCombination02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true)\n");
		buf.append("            System.out.println(\"\");\n");
		buf.append("        if (true)\n");
		buf.append("            System.out.println(\"\");\n");
		buf.append("        if (true)\n");
		buf.append("            System.out.println(\"\");\n");
		buf.append("        System.out.println(\"\");\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.ADD_MISSING_NLS_TAGS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("            System.out.println(\"\"); //$NON-NLS-1$\n");
		buf.append("        }\n");
		buf.append("        if (true) {\n");
		buf.append("            System.out.println(\"\"); //$NON-NLS-1$\n");
		buf.append("        }\n");
		buf.append("        if (true) {\n");
		buf.append("            System.out.println(\"\"); //$NON-NLS-1$\n");
		buf.append("        }\n");
		buf.append("        System.out.println(\"\"); //$NON-NLS-1$\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCombination03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;  \n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1  {\n");
		buf.append("    private List<String> fList;\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (Iterator<String> iter = fList.iterator(); iter.hasNext();) {\n");
		buf.append("            String element = (String) iter.next();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		buf= new StringBuffer();
		buf.append("package test1;  \n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1  {\n");
		buf.append("    private List<String> fList;\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (String string : this.fList) {\n");
		buf.append("            String element = (String) string;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testBug245254() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int i= 0;\n");
		buf.append("    void method() {\n");
		buf.append("        if (true\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int i= 0;\n");
		buf.append("    void method() {\n");
		buf.append("        if (true\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected });
	}

	public void testCombinationBug120585() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int i= 0;\n");
		buf.append("    void method() {\n");
		buf.append("        int[] array= null;\n");
		buf.append("        for (int i= 0; i < array.length; i++)\n");
		buf.append("            System.out.println(array[i]);\n");
		buf.append("        i= 12;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    void method() {\n");
		buf.append("        int[] array= null;\n");
		buf.append("        for (int element : array) {\n");
		buf.append("            System.out.println(element);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCombinationBug125455() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1  {\n");
		buf.append("    private void bar(boolean wait) {\n");
		buf.append("        if (!wait) \n");
		buf.append("            return;\n");
		buf.append("    }\n");
		buf.append("    private void foo(String s) {\n");
		buf.append("        String s1= \"\";\n");
		buf.append("        if (s.equals(\"\"))\n");
		buf.append("            System.out.println();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.ADD_MISSING_NLS_TAGS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1  {\n");
		buf.append("    private void bar(boolean wait) {\n");
		buf.append("        if (!wait) {\n");
		buf.append("            return;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private void foo(String s) {\n");
		buf.append("        String s1= \"\"; //$NON-NLS-1$\n");
		buf.append("        if (s.equals(\"\")) { //$NON-NLS-1$\n");
		buf.append("            System.out.println();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCombinationBug157468() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private void bar(boolean bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb) {\n");
		buf.append("        if (bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb) { // a b c d e f g h i j k\n");
		buf.append("            final String s = \"\";\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);

		Hashtable options= TestOptions.getDefaultOptions();

		Map eclipse21Settings= DefaultCodeFormatterConstants.getEclipseDefaultSettings();
		for (Iterator iterator= eclipse21Settings.keySet().iterator(); iterator.hasNext();) {
			String key= (String)iterator.next();
			options.put(key, eclipse21Settings.get(key));
		}

		JavaCore.setOptions(options);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class E1 {\n");
		buf.append("	private void bar(boolean bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb) {\n");
		buf.append("		if (bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb) { // a b c d e f g\n");
		buf.append("																// h i j k\n");
		buf.append("			final String s = \"\";\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCombinationBug234984_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void method(String[] arr) {\n");
		buf.append("        for (int i = 0; i < arr.length; i++) {\n");
		buf.append("            String item = arr[i];\n");
		buf.append("            String item2 = item + \"a\";\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void method(String[] arr) {\n");
		buf.append("        for (final String item : arr) {\n");
		buf.append("            final String item2 = item + \"a\";\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testCombinationBug234984_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void method(List<E1> es) {\n");
		buf.append("        for (Iterator<E1> iterator = es.iterator(); iterator.hasNext();) {\n");
		buf.append("            E1 next = iterator.next();\n");
		buf.append("            next= new E1();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void method(List<E1> es) {\n");
		buf.append("        for (E1 next : es) {\n");
		buf.append("            next= new E1();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testSerialVersion01() throws Exception {

		JavaProjectHelper.set14CompilerOptions(fJProject1);

		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			StringBuffer buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import java.io.Serializable;\n");
			buf.append("public class E1 implements Serializable {\n");
			buf.append("}\n");
			ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);
			fJProject1.getProject().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

			enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID);
			enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import java.io.Serializable;\n");
			buf.append("public class E1 implements Serializable {\n");
			buf.append("\n");
			buf.append("    " + FIELD_COMMENT + "\n");
			buf.append("    private static final long serialVersionUID = 1L;\n");
			buf.append("}\n");
			String expected1= buf.toString();
			assertRefactoringResultAsExpectedIgnoreHashValue(new ICompilationUnit[] {cu1}, new String[] {expected1});
		} finally {
			JavaProjectHelper.set15CompilerOptions(fJProject1);
		}
	}

	public void testSerialVersion02() throws Exception {

		JavaProjectHelper.set14CompilerOptions(fJProject1);

		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			StringBuffer buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import java.io.Serializable;\n");
			buf.append("public class E1 implements Serializable {\n");
			buf.append("    public class B1 implements Serializable {\n");
			buf.append("    }\n");
			buf.append("    public class B2 extends B1 {\n");
			buf.append("    }\n");
			buf.append("}\n");
			ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

			enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID);
			enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import java.io.Serializable;\n");
			buf.append("public class E1 implements Serializable {\n");
			buf.append("    " + FIELD_COMMENT + "\n");
			buf.append("    private static final long serialVersionUID = 1L;\n");
			buf.append("    public class B1 implements Serializable {\n");
			buf.append("\n");
			buf.append("        " + FIELD_COMMENT + "\n");
			buf.append("        private static final long serialVersionUID = 1L;\n");
			buf.append("    }\n");
			buf.append("    public class B2 extends B1 {\n");
			buf.append("\n");
			buf.append("        " + FIELD_COMMENT + "\n");
			buf.append("        private static final long serialVersionUID = 1L;\n");
			buf.append("    }\n");
			buf.append("}\n");
			String expected1= buf.toString();
			assertRefactoringResultAsExpectedIgnoreHashValue(new ICompilationUnit[] {cu1}, new String[] {expected1});
		} finally {
			JavaProjectHelper.set15CompilerOptions(fJProject1);
		}
	}

	public void testSerialVersion03() throws Exception {

		JavaProjectHelper.set14CompilerOptions(fJProject1);

		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			StringBuffer buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import java.io.Serializable;\n");
			buf.append("public class E1 implements Serializable {\n");
			buf.append("}\n");
			ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);
			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import java.io.Externalizable;\n");
			buf.append("public class E2 implements Externalizable {\n");
			buf.append("}\n");
			ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

			enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID);
			enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import java.io.Serializable;\n");
			buf.append("public class E1 implements Serializable {\n");
			buf.append("\n");
			buf.append("    " + FIELD_COMMENT + "\n");
			buf.append("    private static final long serialVersionUID = 1L;\n");
			buf.append("}\n");
			String expected2= buf.toString();
			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import java.io.Externalizable;\n");
			buf.append("public class E2 implements Externalizable {\n");
			buf.append("}\n");
			String expected1= buf.toString();

			assertRefactoringResultAsExpectedIgnoreHashValue(new ICompilationUnit[] {cu1, cu2}, new String[] {expected1, expected2});
		} finally {
			JavaProjectHelper.set15CompilerOptions(fJProject1);
		}
	}

	public void testSerialVersion04() throws Exception {

		JavaProjectHelper.set14CompilerOptions(fJProject1);

		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			StringBuffer buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import java.io.Serializable;\n");
			buf.append("public class E1 implements Serializable {\n");
			buf.append("    public void foo() {\n");
			buf.append("        Serializable s= new Serializable() {\n");
			buf.append("        };\n");
			buf.append("    }\n");
			buf.append("}\n");
			ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

			enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID);
			enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import java.io.Serializable;\n");
			buf.append("public class E1 implements Serializable {\n");
			buf.append("    " + FIELD_COMMENT + "\n");
			buf.append("    private static final long serialVersionUID = 1L;\n");
			buf.append("\n");
			buf.append("    public void foo() {\n");
			buf.append("        Serializable s= new Serializable() {\n");
			buf.append("\n");
			buf.append("            " + FIELD_COMMENT + "\n");
			buf.append("            private static final long serialVersionUID = 1L;\n");
			buf.append("        };\n");
			buf.append("    }\n");
			buf.append("}\n");
			String expected1= buf.toString();
			assertRefactoringResultAsExpectedIgnoreHashValue(new ICompilationUnit[] {cu1}, new String[] {expected1});
		} finally {
			JavaProjectHelper.set15CompilerOptions(fJProject1);
		}
	}

	public void testSerialVersion05() throws Exception {

		JavaProjectHelper.set14CompilerOptions(fJProject1);

		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			StringBuffer buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import java.io.Serializable;\n");
			buf.append("public class E1 implements Serializable {\n");
			buf.append("\n");
			buf.append("    private Serializable s= new Serializable() {\n");
			buf.append("        \n");
			buf.append("    };\n");
			buf.append("}\n");
			ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

			enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID);
			enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import java.io.Serializable;\n");
			buf.append("public class E1 implements Serializable {\n");
			buf.append("\n");
			buf.append("    " + FIELD_COMMENT + "\n");
			buf.append("    private static final long serialVersionUID = 1L;\n");
			buf.append("    private Serializable s= new Serializable() {\n");
			buf.append("\n");
			buf.append("        " + FIELD_COMMENT + "\n");
			buf.append("        private static final long serialVersionUID = 1L;\n");
			buf.append("        \n");
			buf.append("    };\n");
			buf.append("}\n");
			String expected1= buf.toString();
			assertRefactoringResultAsExpectedIgnoreHashValue(new ICompilationUnit[] {cu1}, new String[] {expected1});
		} finally {
			JavaProjectHelper.set15CompilerOptions(fJProject1);
		}
	}

	public void testSerialVersionBug139381() throws Exception {

		JavaProjectHelper.set14CompilerOptions(fJProject1);

		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			StringBuffer buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import java.io.Serializable;\n");
			buf.append("public class E1 {\n");
			buf.append("    void foo1() {\n");
			buf.append("        new Serializable() {\n");
			buf.append("        };\n");
			buf.append("    }\n");
			buf.append("    void foo2() {\n");
			buf.append("        new Object() {\n");
			buf.append("        };\n");
			buf.append("        new Serializable() {\n");
			buf.append("        };\n");
			buf.append("    }\n");
			buf.append("}\n");
			ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

			enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID);
			enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import java.io.Serializable;\n");
			buf.append("public class E1 {\n");
			buf.append("    void foo1() {\n");
			buf.append("        new Serializable() {\n");
			buf.append("\n");
			buf.append("            " + FIELD_COMMENT + "\n");
			buf.append("            private static final long serialVersionUID = 1L;\n");
			buf.append("        };\n");
			buf.append("    }\n");
			buf.append("    void foo2() {\n");
			buf.append("        new Object() {\n");
			buf.append("        };\n");
			buf.append("        new Serializable() {\n");
			buf.append("\n");
			buf.append("            " + FIELD_COMMENT + "\n");
			buf.append("            private static final long serialVersionUID = 1L;\n");
			buf.append("        };\n");
			buf.append("    }\n");
			buf.append("}\n");
			String expected1= buf.toString();
			assertRefactoringResultAsExpectedIgnoreHashValue(new ICompilationUnit[] {cu1}, new String[] {expected1});
		} finally {
			JavaProjectHelper.set15CompilerOptions(fJProject1);
		}
	}

	public void testAddBlockBug149110_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true)\n");
		buf.append("            throw new IllegalAccessError();\n");
		buf.append("        if (true) {\n");
		buf.append("            throw new IllegalAccessError();\n");
		buf.append("        }\n");
		buf.append("        if (false)\n");
		buf.append("            System.out.println();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true)\n");
		buf.append("            throw new IllegalAccessError();\n");
		buf.append("        if (true)\n");
		buf.append("            throw new IllegalAccessError();\n");
		buf.append("        if (false) {\n");
		buf.append("            System.out.println();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { buf.toString() });
	}

	public void testAddBlockBug149110_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true)\n");
		buf.append("            return;\n");
		buf.append("        if (true) {\n");
		buf.append("            return;\n");
		buf.append("        }\n");
		buf.append("        if (false)\n");
		buf.append("            System.out.println();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true)\n");
		buf.append("            return;\n");
		buf.append("        if (true)\n");
		buf.append("            return;\n");
		buf.append("        if (false) {\n");
		buf.append("            System.out.println();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { buf.toString() });
	}

	public void testRemoveBlock01() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void if_() {\n");
		buf.append("        if (true) {\n");
		buf.append("            ;\n");
		buf.append("        } else if (false) {\n");
		buf.append("            ;\n");
		buf.append("        } else {\n");
		buf.append("            ;\n");
		buf.append("        }\n");
		buf.append("        \n");
		buf.append("        if (true) {\n");
		buf.append("            ;;\n");
		buf.append("        } else if (false) {\n");
		buf.append("            ;;\n");
		buf.append("        } else {\n");
		buf.append("            ;;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_NEVER);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void if_() {\n");
		buf.append("        if (true)\n");
		buf.append("            ;\n");
		buf.append("        else if (false)\n");
		buf.append("            ;\n");
		buf.append("        else\n");
		buf.append("            ;\n");
		buf.append("        \n");
		buf.append("        if (true) {\n");
		buf.append("            ;;\n");
		buf.append("        } else if (false) {\n");
		buf.append("            ;;\n");
		buf.append("        } else {\n");
		buf.append("            ;;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testRemoveBlock02() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (;;) {\n");
		buf.append("            ; \n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void bar() {\n");
		buf.append("        for (;;) {\n");
		buf.append("            ;; \n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_NEVER);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (;;);\n");
		buf.append("    }\n");
		buf.append("    public void bar() {\n");
		buf.append("        for (;;) {\n");
		buf.append("            ;; \n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testRemoveBlock03() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        while (true) {\n");
		buf.append("            ;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void bar() {\n");
		buf.append("        while (true) {\n");
		buf.append("            ;;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_NEVER);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        while (true);\n");
		buf.append("    }\n");
		buf.append("    public void bar() {\n");
		buf.append("        while (true) {\n");
		buf.append("            ;;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testRemoveBlock04() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        do {\n");
		buf.append("            ;\n");
		buf.append("        } while (true);\n");
		buf.append("    }\n");
		buf.append("    public void bar() {\n");
		buf.append("        do {\n");
		buf.append("            ;;\n");
		buf.append("        } while (true);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_NEVER);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        do; while (true);\n");
		buf.append("    }\n");
		buf.append("    public void bar() {\n");
		buf.append("        do {\n");
		buf.append("            ;;\n");
		buf.append("        } while (true);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testRemoveBlock05() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int[] is= null;\n");
		buf.append("        for (int i= 0;i < is.length;i++) {\n");
		buf.append("            ;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_NEVER);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int[] is= null;\n");
		buf.append("        for (int element : is);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testRemoveBlockBug138628() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("            if (true)\n");
		buf.append("                ;\n");
		buf.append("        } else if (true) {\n");
		buf.append("            if (false) {\n");
		buf.append("                ;\n");
		buf.append("            } else\n");
		buf.append("                ;\n");
		buf.append("        } else if (false) {\n");
		buf.append("            if (true) {\n");
		buf.append("                ;\n");
		buf.append("            }\n");
		buf.append("        } else {\n");
		buf.append("            if (true)\n");
		buf.append("                ;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_NEVER);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("            if (true)\n");
		buf.append("                ;\n");
		buf.append("        } else if (true) {\n");
		buf.append("            if (false)\n");
		buf.append("                ;\n");
		buf.append("            else\n");
		buf.append("                ;\n");
		buf.append("        } else if (false) {\n");
		buf.append("            if (true)\n");
		buf.append("                ;\n");
		buf.append("        } else if (true)\n");
		buf.append("            ;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testRemoveBlockBug149990() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (false) {\n");
		buf.append("            while (true) {\n");
		buf.append("                if (false) {\n");
		buf.append("                    ;\n");
		buf.append("                }\n");
		buf.append("            }\n");
		buf.append("        } else\n");
		buf.append("            ;\n");
		buf.append("    }\n");
		buf.append("}");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (false) {\n");
		buf.append("            while (true)\n");
		buf.append("                if (false)\n");
		buf.append("                    ;\n");
		buf.append("        } else\n");
		buf.append("            ;\n");
		buf.append("    }\n");
		buf.append("}");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testRemoveBlockBug156513_1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(boolean b, int[] ints) {\n");
		buf.append("        if (b) {\n");
		buf.append("            for (int i = 0; i < ints.length; i++) {\n");
		buf.append("                System.out.println(ints[i]);\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_NEVER);
		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(boolean b, int[] ints) {\n");
		buf.append("        if (b)\n");
		buf.append("            for (int j : ints)\n");
		buf.append("                System.out.println(j);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testRemoveBlockBug156513_2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(boolean b, int[] ints) {\n");
		buf.append("        for (int i = 0; i < ints.length; i++) {\n");
		buf.append("            for (int j = 0; j < ints.length; j++) {\n");
		buf.append("                System.out.println(ints[j]);\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_NEVER);
		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(boolean b, int[] ints) {\n");
		buf.append("        for (int k : ints)\n");
		buf.append("            for (int l : ints)\n");
		buf.append("                System.out.println(l);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testUnnecessaryCodeBug127704_1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private boolean foo() {\n");
		buf.append("        return (boolean) (Boolean) Boolean.TRUE;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private boolean foo() {\n");
		buf.append("        return Boolean.TRUE;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testUnnecessaryCodeBug127704_2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private Integer foo() {\n");
		buf.append("        return (Integer) (Number) getNumber();\n");
		buf.append("    }\n");
		buf.append("    private Number getNumber() {return null;}\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private Integer foo() {\n");
		buf.append("        return (Integer) getNumber();\n");
		buf.append("    }\n");
		buf.append("    private Number getNumber() {return null;}\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testAddParenthesis01() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(int i) {\n");
		buf.append("        if (i == 0 || i == 1)\n");
		buf.append("            System.out.println(i);\n");
		buf.append("        \n");
		buf.append("        while (i > 0 && i < 10)\n");
		buf.append("            System.out.println(1);\n");
		buf.append("        \n");
		buf.append("        boolean b= i != -1 && i > 10 && i < 100 || i > 20;\n");
		buf.append("        \n");
		buf.append("        do ; while (i > 5 && b || i < 100 && i > 90);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(int i) {\n");
		buf.append("        if ((i == 0) || (i == 1)) {\n");
		buf.append("            System.out.println(i);\n");
		buf.append("        }\n");
		buf.append("        \n");
		buf.append("        while ((i > 0) && (i < 10)) {\n");
		buf.append("            System.out.println(1);\n");
		buf.append("        }\n");
		buf.append("        \n");
		buf.append("        boolean b= ((i != -1) && (i > 10) && (i < 100)) || (i > 20);\n");
		buf.append("        \n");
		buf.append("        do {\n");
		buf.append("            ;\n");
		buf.append("        } while (((i > 5) && b) || ((i < 100) && (i > 90)));\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testRemoveParenthesis01() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(int i) {\n");
		buf.append("        if ((i == 0) || (i == 1)) {\n");
		buf.append("            System.out.println(i);\n");
		buf.append("        }\n");
		buf.append("        \n");
		buf.append("        while ((i > 0) && (i < 10)) {\n");
		buf.append("            System.out.println(1);\n");
		buf.append("        }\n");
		buf.append("        \n");
		buf.append("        boolean b= ((i != -1) && (i > 10) && (i < 100)) || (i > 20);\n");
		buf.append("        \n");
		buf.append("        do {\n");
		buf.append("            ;\n");
		buf.append("        } while (((i > 5) && b) || ((i < 100) && (i > 90)));\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_NEVER);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(int i) {\n");
		buf.append("        if (i == 0 || i == 1)\n");
		buf.append("            System.out.println(i);\n");
		buf.append("        \n");
		buf.append("        while (i > 0 && i < 10)\n");
		buf.append("            System.out.println(1);\n");
		buf.append("        \n");
		buf.append("        boolean b= i != -1 && i > 10 && i < 100 || i > 20;\n");
		buf.append("        \n");
		buf.append("        do; while (i > 5 && b || i < 100 && i > 90);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testRemoveParenthesisBug134739() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(boolean a) {\n");
		buf.append("        if (((a)))\n");
		buf.append("            return;\n");
		buf.append("    }\n");
		buf.append("    public void bar(boolean a, boolean b) {\n");
		buf.append("        if (((a)) || ((b)))\n");
		buf.append("            return;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_NEVER);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(boolean a) {\n");
		buf.append("        if (a)\n");
		buf.append("            return;\n");
		buf.append("    }\n");
		buf.append("    public void bar(boolean a, boolean b) {\n");
		buf.append("        if (a || b)\n");
		buf.append("            return;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testRemoveParenthesisBug134741_1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public boolean foo(Object o) {\n");
		buf.append("        if ((((String)o)).equals(\"\"))\n");
		buf.append("            return true;\n");
		buf.append("        return false;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public boolean foo(Object o) {\n");
		buf.append("        if (((String)o).equals(\"\"))\n");
		buf.append("            return true;\n");
		buf.append("        return false;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testRemoveParenthesisBug134741_2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(boolean a) {\n");
		buf.append("        if ((\"\" + \"b\").equals(\"a\"))\n");
		buf.append("            return;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	public void testRemoveParenthesisBug134741_3() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public String foo2(String s) {\n");
		buf.append("        return (s != null ? s : \"\").toString();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	public void testRemoveParenthesisBug134985_1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public boolean foo(String s1, String s2, boolean a, boolean b) {\n");
		buf.append("        return (a == b) == (s1 == s2);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public boolean foo(String s1, String s2, boolean a, boolean b) {\n");
		buf.append("        return a == b == (s1 == s2);\n");
		buf.append("    }\n");
		buf.append("}\n");

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {buf.toString()});
	}

	public void testRemoveParenthesisBug134985_2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public String foo() {\n");
		buf.append("        return (\"\" + 3) + (3 + 3);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public String foo() {\n");
		buf.append("        return \"\" + 3 + (3 + 3);\n");
		buf.append("    }\n");
		buf.append("}\n");

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {buf.toString()});
	}

	public void testRemoveParenthesisBug188207() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public int foo() {\n");
		buf.append("        boolean b= (true ? true : (true ? false : true));\n");
		buf.append("        return ((b ? true : true) ? 0 : 1);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public int foo() {\n");
		buf.append("        boolean b= true ? true : true ? false : true;\n");
		buf.append("        return (b ? true : true) ? 0 : 1;\n");
		buf.append("    }\n");
		buf.append("}\n");

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {buf.toString()});
	}

	public void testRemoveParenthesisBug208752() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        double d = 2.0 * (0.5 / 4.0);\n");
		buf.append("        int spaceCount = (3);\n");
		buf.append("        spaceCount = 2 * (spaceCount / 2);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        double d = 2.0 * 0.5 / 4.0;\n");
		buf.append("        int spaceCount = 3;\n");
		buf.append("        spaceCount = 2 * (spaceCount / 2);\n");
		buf.append("    }\n");
		buf.append("}\n");

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { buf.toString() });
	}

	public void testRemoveParenthesisBug190188() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        (new Object()).toString();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        new Object().toString();\n");
		buf.append("    }\n");
		buf.append("}\n");

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { buf.toString() });
	}

	public void testRemoveParenthesisBug212856() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public int foo() {\n");
		buf.append("        int n= 1 + (2 - 3);\n");
		buf.append("        n= 1 - (2 + 3);\n");
		buf.append("        n= 1 - (2 - 3);\n");
		buf.append("        n= 1 * (2 * 3);\n");
		buf.append("        return 2 * (n % 10);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public int foo() {\n");
		buf.append("        int n= 1 + 2 - 3;\n");
		buf.append("        n= 1 - (2 + 3);\n");
		buf.append("        n= 1 - (2 - 3);\n");
		buf.append("        n= 1 * 2 * 3;\n");
		buf.append("        return 2 * (n % 10);\n");
		buf.append("    }\n");
		buf.append("}\n");

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { buf.toString() });
	}

	public void testRemoveQualifier01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public int foo;\n");
		buf.append("    public void setFoo(int foo) {\n");
		buf.append("        this.foo= foo;\n");
		buf.append("    }\n");
		buf.append("    public int getFoo() {\n");
		buf.append("        return this.foo;\n");
		buf.append("    }   \n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public int foo;\n");
		buf.append("    public void setFoo(int foo) {\n");
		buf.append("        this.foo= foo;\n");
		buf.append("    }\n");
		buf.append("    public int getFoo() {\n");
		buf.append("        return foo;\n");
		buf.append("    }   \n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testRemoveQualifier02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public int foo() {return 0;}\n");
		buf.append("    public int getFoo() {\n");
		buf.append("        return this.foo();\n");
		buf.append("    }   \n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public int foo() {return 0;}\n");
		buf.append("    public int getFoo() {\n");
		buf.append("        return foo();\n");
		buf.append("    }   \n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testRemoveQualifier03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public int foo;\n");
		buf.append("    public int bar;\n");
		buf.append("    public class E1Inner {\n");
		buf.append("        private int bar;\n");
		buf.append("        public int getFoo() {\n");
		buf.append("            E1.this.bar= this.bar;\n");
		buf.append("            return E1.this.foo;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public int foo;\n");
		buf.append("    public int bar;\n");
		buf.append("    public class E1Inner {\n");
		buf.append("        private int bar;\n");
		buf.append("        public int getFoo() {\n");
		buf.append("            E1.this.bar= bar;\n");
		buf.append("            return foo;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testRemoveQualifier04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public int foo() {return 0;}\n");
		buf.append("    public int bar() {return 0;}\n");
		buf.append("    public class E1Inner {\n");
		buf.append("        private int bar() {return 1;}\n");
		buf.append("        public int getFoo() {\n");
		buf.append("            E1.this.bar(); \n");
		buf.append("            this.bar();\n");
		buf.append("            return E1.this.foo();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public int foo() {return 0;}\n");
		buf.append("    public int bar() {return 0;}\n");
		buf.append("    public class E1Inner {\n");
		buf.append("        private int bar() {return 1;}\n");
		buf.append("        public int getFoo() {\n");
		buf.append("            E1.this.bar(); \n");
		buf.append("            bar();\n");
		buf.append("            return foo();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testRemoveQualifierBug134720() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        this.setEnabled(true);\n");
		buf.append("    }\n");
		buf.append("    private void setEnabled(boolean b) {}\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        setEnabled(true);\n");
		buf.append("    }\n");
		buf.append("    private void setEnabled(boolean b) {}\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testRemoveQualifierBug150481_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public class Inner extends E {\n");
		buf.append("        public void test() {\n");
		buf.append("            E.this.foo();\n");
		buf.append("            this.foo();\n");
		buf.append("            foo();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void foo() {}\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public class Inner extends E {\n");
		buf.append("        public void test() {\n");
		buf.append("            E.this.foo();\n");
		buf.append("            foo();\n");
		buf.append("            foo();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void foo() {}\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testRemoveQualifierBug150481_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public class Inner {\n");
		buf.append("        public void test() {\n");
		buf.append("            E.this.foo();\n");
		buf.append("            foo();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void foo() {}\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public class Inner {\n");
		buf.append("        public void test() {\n");
		buf.append("            foo();\n");
		buf.append("            foo();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void foo() {}\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testRemoveQualifierBug219478() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 extends E2 {\n");
		buf.append("    private class E1Inner extends E2 {\n");
		buf.append("        public E1Inner() {\n");
		buf.append("            i = 2;\n");
		buf.append("            System.out.println(i + E1.this.i);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class E2 {\n");
		buf.append("    protected int i = 1;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 extends E2 {\n");
		buf.append("    private class E1Inner extends E2 {\n");
		buf.append("        public E1Inner() {\n");
		buf.append("            i = 2;\n");
		buf.append("            System.out.println(i + E1.this.i);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class E2 {\n");
		buf.append("    protected int i = 1;\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testRemoveQualifierBug219608() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 extends E2 {\n");
		buf.append("    private int i = 1;\n");
		buf.append("    private class E1Inner extends E2 {\n");
		buf.append("        public E1Inner() {\n");
		buf.append("            System.out.println(i + E1.this.i);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class E2 {\n");
		buf.append("    private int i = 1;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 extends E2 {\n");
		buf.append("    private int i = 1;\n");
		buf.append("    private class E1Inner extends E2 {\n");
		buf.append("        public E1Inner() {\n");
		buf.append("            System.out.println(i + i);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class E2 {\n");
		buf.append("    private int i = 1;\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testAddFinal01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private int i= 0;\n");
		buf.append("    public void foo(int j, int k) {\n");
		buf.append("        int h, v;\n");
		buf.append("        v= 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private final int i= 0;\n");
		buf.append("    public void foo(final int j, final int k) {\n");
		buf.append("        final int h;\n");
		buf.append("        int v;\n");
		buf.append("        v= 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testAddFinal02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private Object obj1= new Object();\n");
		buf.append("    protected Object obj2;\n");
		buf.append("    Object obj3;\n");
		buf.append("    public Object obj4;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private final Object obj1= new Object();\n");
		buf.append("    protected Object obj2;\n");
		buf.append("    Object obj3;\n");
		buf.append("    public Object obj4;\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testAddFinal03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private int i = 0;\n");
		buf.append("    public void foo() throws Exception {\n");
		buf.append("    }\n");
		buf.append("    public void bar(int j) {\n");
		buf.append("        int k;\n");
		buf.append("        try {\n");
		buf.append("            foo();\n");
		buf.append("        } catch (Exception e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private int i = 0;\n");
		buf.append("    public void foo() throws Exception {\n");
		buf.append("    }\n");
		buf.append("    public void bar(int j) {\n");
		buf.append("        final int k;\n");
		buf.append("        try {\n");
		buf.append("            foo();\n");
		buf.append("        } catch (final Exception e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testAddFinal04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private int i = 0;\n");
		buf.append("    public void foo() throws Exception {\n");
		buf.append("    }\n");
		buf.append("    public void bar(int j) {\n");
		buf.append("        int k;\n");
		buf.append("        try {\n");
		buf.append("            foo();\n");
		buf.append("        } catch (Exception e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private int i = 0;\n");
		buf.append("    public void foo() throws Exception {\n");
		buf.append("    }\n");
		buf.append("    public void bar(final int j) {\n");
		buf.append("        int k;\n");
		buf.append("        try {\n");
		buf.append("            foo();\n");
		buf.append("        } catch (Exception e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testAddFinal05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= 0;\n");
		buf.append("        if (i > 1 || i == 1 && i > 1)\n");
		buf.append("            ;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        final int i= 0;\n");
		buf.append("        if ((i > 1) || ((i == 1) && (i > 1)))\n");
		buf.append("            ;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testAddFinalBug129807() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public interface I {\n");
		buf.append("        void foo(int i);\n");
		buf.append("    }\n");
		buf.append("    public class IImpl implements I {\n");
		buf.append("        public void foo(int i) {}\n");
		buf.append("    }\n");
		buf.append("    public abstract void bar(int i, String s);\n");
		buf.append("    public void foobar(int i, int j) {}\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public interface I {\n");
		buf.append("        void foo(int i);\n");
		buf.append("    }\n");
		buf.append("    public class IImpl implements I {\n");
		buf.append("        public void foo(final int i) {}\n");
		buf.append("    }\n");
		buf.append("    public abstract void bar(int i, String s);\n");
		buf.append("    public void foobar(final int i, final int j) {}\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testAddFinalBug134676_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E<T> { \n");
		buf.append("    private String s;\n");
		buf.append("    void setS(String s) {\n");
		buf.append("        this.s = s;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	public void testAddFinalBug134676_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E<T> { \n");
		buf.append("    private String s= \"\";\n");
		buf.append("    private T t;\n");
		buf.append("    private T t2;\n");
		buf.append("    public E(T t) {t2= t;}\n");
		buf.append("    void setT(T t) {\n");
		buf.append("        this.t = t;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E<T> { \n");
		buf.append("    private final String s= \"\";\n");
		buf.append("    private T t;\n");
		buf.append("    private final T t2;\n");
		buf.append("    public E(T t) {t2= t;}\n");
		buf.append("    void setT(T t) {\n");
		buf.append("        this.t = t;\n");
		buf.append("    }\n");
		buf.append("}\n");

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {buf.toString()});
	}

	//Changed test due to https://bugs.eclipse.org/bugs/show_bug.cgi?id=220124
	public void testAddFinalBug145028() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private volatile int field= 0;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	//https://bugs.eclipse.org/bugs/show_bug.cgi?id=294768
	public void testAddFinalBug294768() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private transient int field= 0;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);
		
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);
		
		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}
	
	public void testAddFinalBug157276_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int field;\n");
		buf.append("    public E1() {\n");
		buf.append("        field= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private final int field;\n");
		buf.append("    public E1() {\n");
		buf.append("        field= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {buf.toString()});
	}

	public void testAddFinalBug157276_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int field;\n");
		buf.append("    public E1() {\n");
		buf.append("        field= 10;\n");
		buf.append("    }\n");
		buf.append("    public E1(int f) {\n");
		buf.append("        field= f;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private final int field;\n");
		buf.append("    public E1() {\n");
		buf.append("        field= 10;\n");
		buf.append("    }\n");
		buf.append("    public E1(int f) {\n");
		buf.append("        field= f;\n");
		buf.append("    }\n");
		buf.append("}\n");

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {buf.toString()});
	}

	public void testAddFinalBug157276_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int field;\n");
		buf.append("    public E1() {\n");
		buf.append("        field= 10;\n");
		buf.append("    }\n");
		buf.append("    public E1(final int f) {\n");
		buf.append("        field= f;\n");
		buf.append("        field= 5;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	public void testAddFinalBug157276_4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int field;\n");
		buf.append("    public E1() {\n");
		buf.append("    }\n");
		buf.append("    public E1(final int f) {\n");
		buf.append("        field= f;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	public void testAddFinalBug157276_5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int field= 0;\n");
		buf.append("    public E1() {\n");
		buf.append("        field= 5;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	public void testAddFinalBug157276_6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int field;\n");
		buf.append("    public E1() {\n");
		buf.append("        if (false) field= 5;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	public void testAddFinalBug157276_7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private static int field;\n");
		buf.append("    public E1() {\n");
		buf.append("        field= 5;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	public void testAddFinalBug156842() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int f0;\n");
		buf.append("    private int f1= 0;\n");
		buf.append("    private int f3;\n");
		buf.append("    public E1() {\n");
		buf.append("        f3= 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int f0;\n");
		buf.append("    private final int f1= 0;\n");
		buf.append("    private final int f3;\n");
		buf.append("    public E1() {\n");
		buf.append("        f3= 0;\n");
		buf.append("    }\n");
		buf.append("}\n");

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {buf.toString()});
	}

	public void testAddFinalBug158041_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(int[] ints) {\n");
		buf.append("        for (int j = 0; j < ints.length; j++) {\n");
		buf.append("            System.out.println(ints[j]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);
		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(int[] ints) {\n");
		buf.append("        for (final int i : ints) {\n");
		buf.append("            System.out.println(i);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { buf.toString() });
	}

	public void testAddFinalBug158041_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(int[] ints) {\n");
		buf.append("        for (int j = 0; j < ints.length; j++) {\n");
		buf.append("            int i = ints[j];\n");
		buf.append("            System.out.println(i);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);
		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(int[] ints) {\n");
		buf.append("        for (final int i : ints) {\n");
		buf.append("            System.out.println(i);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { buf.toString() });
	}

	public void testAddFinalBug158041_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(List<E1> es) {\n");
		buf.append("        for (Iterator<E1> iterator = es.iterator(); iterator.hasNext();) {\n");
		buf.append("            System.out.println(iterator.next());\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);
		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(List<E1> es) {\n");
		buf.append("        for (final E1 e1 : es) {\n");
		buf.append("            System.out.println(e1);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {buf.toString()});
	}

	public void testAddFinalBug158041_4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(List<E1> es) {\n");
		buf.append("        for (Iterator<E1> iterator = es.iterator(); iterator.hasNext();) {\n");
		buf.append("            E1 e1 = iterator.next();\n");
		buf.append("            System.out.println(e1);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);
		enable(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(List<E1> es) {\n");
		buf.append("        for (final E1 e1 : es) {\n");
		buf.append("            System.out.println(e1);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { buf.toString() });
	}

	public void testAddFinalBug163789() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int i;\n");
		buf.append("    public E1() {\n");
		buf.append("        this(10);\n");
		buf.append("        i = 10;\n");
		buf.append("    }\n");
		buf.append("    public E1(int j) {\n");
		buf.append("        i = j;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int i;\n");
		buf.append("    public E1() {\n");
		buf.append("        this(10);\n");
		buf.append("        i = 10;\n");
		buf.append("    }\n");
		buf.append("    public E1(final int j) {\n");
		buf.append("        i = j;\n");
		buf.append("    }\n");
		buf.append("}\n");

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {buf.toString()});
	}

	public void testAddFinalBug191862() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E01 {\n");
		buf.append("    @SuppressWarnings(\"unused\")\n");
		buf.append("    @Deprecated\n");
		buf.append("    private int x = 5, y= 10;\n");
		buf.append("    \n");
		buf.append("    private void foo() {\n");
		buf.append("        @SuppressWarnings(\"unused\")\n");
		buf.append("        @Deprecated\n");
		buf.append("        int i= 10, j;\n");
		buf.append("        j= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E01 {\n");
		buf.append("    @SuppressWarnings(\"unused\")\n");
		buf.append("    @Deprecated\n");
		buf.append("    private final int x = 5, y= 10;\n");
		buf.append("    \n");
		buf.append("    private void foo() {\n");
		buf.append("        @SuppressWarnings(\"unused\")\n");
		buf.append("        @Deprecated\n");
		buf.append("        final\n");
		buf.append("        int i= 10;\n");
		buf.append("        @SuppressWarnings(\"unused\")\n");
		buf.append("        @Deprecated\n");
		buf.append("        int j;\n");
		buf.append("        j= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {buf.toString()});
	}

	public void testAddFinalBug213995() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private Object foo = new Object() {\n");
		buf.append("        public boolean equals(Object obj) {\n");
		buf.append("            return super.equals(obj);\n");
		buf.append("        }\n");
		buf.append("    }; \n");
		buf.append("    public void foo() {\n");
		buf.append("        Object foo = new Object() {\n");
		buf.append("            public boolean equals(Object obj) {\n");
		buf.append("                return super.equals(obj);\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private Object foo = new Object() {\n");
		buf.append("        public boolean equals(final Object obj) {\n");
		buf.append("            return super.equals(obj);\n");
		buf.append("        }\n");
		buf.append("    }; \n");
		buf.append("    public void foo() {\n");
		buf.append("        Object foo = new Object() {\n");
		buf.append("            public boolean equals(final Object obj) {\n");
		buf.append("                return super.equals(obj);\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { buf.toString() });
	}

	public void testAddFinalBug272532() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int field;\n");
		buf.append("    public E1() {\n");
		buf.append("        if (true)\n");
		buf.append("            return;\n");
		buf.append("        field= 5;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	public void testRemoveBlockReturnThrows01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public void foo(Object obj) {\n");
		buf.append("        if (obj == null) {\n");
		buf.append("            throw new IllegalArgumentException();\n");
		buf.append("        }\n");
		buf.append("        \n");
		buf.append("        if (obj.hashCode() > 0) {\n");
		buf.append("            return;\n");
		buf.append("        }\n");
		buf.append("        \n");
		buf.append("        if (obj.hashCode() < 0) {\n");
		buf.append("            System.out.println(\"\");\n");
		buf.append("            return;\n");
		buf.append("        }\n");
		buf.append("        \n");
		buf.append("        if (obj.toString() != null) {\n");
		buf.append("            System.out.println(obj.toString());\n");
		buf.append("        } else {\n");
		buf.append("            System.out.println(\"\");\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public void foo(Object obj) {\n");
		buf.append("        if (obj == null)\n");
		buf.append("            throw new IllegalArgumentException();\n");
		buf.append("        \n");
		buf.append("        if (obj.hashCode() > 0)\n");
		buf.append("            return;\n");
		buf.append("        \n");
		buf.append("        if (obj.hashCode() < 0) {\n");
		buf.append("            System.out.println(\"\");\n");
		buf.append("            return;\n");
		buf.append("        }\n");
		buf.append("        \n");
		buf.append("        if (obj.toString() != null) {\n");
		buf.append("            System.out.println(obj.toString());\n");
		buf.append("        } else {\n");
		buf.append("            System.out.println(\"\");\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testRemoveTrailingWhitespace01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package    test1;     \n");
		buf.append("   public class E1 {  \n");
		buf.append("                   \n");
		buf.append("}                  \n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		buf= new StringBuffer();
		buf.append("package    test1;\n");
		buf.append("public class E1 {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testRemoveTrailingWhitespace02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package    test1;     \n");
		buf.append("   public class E1 {  \n");
		buf.append("                   \n");
		buf.append("}                  \n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_IGNORE_EMPTY);

		buf= new StringBuffer();
		buf.append("package    test1;\n");
		buf.append("   public class E1 {\n");
		buf.append("                   \n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testRemoveTrailingWhitespaceBug173081() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" * \n");
		buf.append(" *     \n");
		buf.append(" */\n");
		buf.append("public class E1 { \n");
		buf.append("    /**\n");
		buf.append("     * \n");
		buf.append("	 *     \n");
		buf.append("     */\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_IGNORE_EMPTY);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" * \n");
		buf.append(" * \n");
		buf.append(" */\n");
		buf.append("public class E1 {\n");
		buf.append("    /**\n");
		buf.append("     * \n");
		buf.append("     * \n");
		buf.append("     */\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testSortMembers01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("   public class SM01 {\n");
		buf.append("   int b;\n");
		buf.append("   int a;\n");
		buf.append("   void d() {};\n");
		buf.append("   void c() {};\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("SM01.java", buf.toString(), false, null);

		enable(CleanUpConstants.SORT_MEMBERS);
		enable(CleanUpConstants.SORT_MEMBERS_ALL);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("   public class SM01 {\n");
		buf.append("   int a;\n");
		buf.append("   int b;\n");
		buf.append("   void c() {};\n");
		buf.append("   void d() {};\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testSortMembers02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("   public class SM02 {\n");
		buf.append("   int b;\n");
		buf.append("   int a;\n");
		buf.append("   void d() {};\n");
		buf.append("   void c() {};\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("SM02.java", buf.toString(), false, null);

		enable(CleanUpConstants.SORT_MEMBERS);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("   public class SM02 {\n");
		buf.append("   int b;\n");
		buf.append("   int a;\n");
		buf.append("   void c() {};\n");
		buf.append("   void d() {};\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testSortMembersBug218542() throws Exception {
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.APPEARANCE_ENABLE_VISIBILITY_SORT_ORDER, true);
		assertTrue(JavaPlugin.getDefault().getMemberOrderPreferenceCache().isSortByVisibility());

		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
			StringBuffer buf= new StringBuffer();
			buf.append("package test;\n");
			buf.append("   public class SM02 {\n");
			buf.append("   private int b;\n");
			buf.append("   public int a;\n");
			buf.append("   void d() {};\n");
			buf.append("   void c() {};\n");
			buf.append("}\n");
			ICompilationUnit cu1= pack1.createCompilationUnit("SM02.java", buf.toString(), false, null);

			enable(CleanUpConstants.SORT_MEMBERS);

			buf= new StringBuffer();
			buf.append("package test;\n");
			buf.append("   public class SM02 {\n");
			buf.append("   private int b;\n");
			buf.append("   public int a;\n");
			buf.append("   void c() {};\n");
			buf.append("   void d() {};\n");
			buf.append("}\n");
			String expected1= buf.toString();

			assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
		} finally {
			JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.APPEARANCE_ENABLE_VISIBILITY_SORT_ORDER, false);
		}
	}

	public void testSortMembersBug223997() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class SM02 {\n");
		buf.append("    public String s2;\n");
		buf.append("    public static String s1;\n");
		buf.append("   void d() {};\n");
		buf.append("   void c() {};\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("SM02.java", buf.toString(), false, null);

		enable(CleanUpConstants.SORT_MEMBERS);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class SM02 {\n");
		buf.append("    public String s2;\n");
		buf.append("    public static String s1;\n");
		buf.append("   void c() {};\n");
		buf.append("   void d() {};\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testSortMembersBug263173() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class SM263173 {\n");
		buf.append("    static int someInt;\n");
		buf.append("    static {\n");
		buf.append("        someInt = 1;\n");
		buf.append("    };\n");
		buf.append("    static int anotherInt = someInt;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("SM263173.java", buf.toString(), false, null);
		
		enable(CleanUpConstants.SORT_MEMBERS);
		
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class SM263173 {\n");
		buf.append("    static int someInt;\n");
		buf.append("    static {\n");
		buf.append("        someInt = 1;\n");
		buf.append("    };\n");
		buf.append("    static int anotherInt = someInt;\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}
	
	public void testOrganizeImports01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    A a;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {}\n");
		pack2.createCompilationUnit("A.java", buf.toString(), false, null);

		IPackageFragment pack3= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class A {}\n");
		pack3.createCompilationUnit("A.java", buf.toString(), false, null);

		enable(CleanUpConstants.ORGANIZE_IMPORTS);

		RefactoringStatus status= assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
		RefactoringStatusEntry[] entries= status.getEntries();
		assertTrue(entries.length == 1);
		String message= entries[0].getMessage();
		assertTrue(message, entries[0].isInfo());
		assertTrue(message, message.indexOf("ambiguous") != -1);
	}

	public void testOrganizeImports02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    Vect or v;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.ORGANIZE_IMPORTS);

		RefactoringStatus status= assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
		RefactoringStatusEntry[] entries= status.getEntries();
		assertTrue(entries.length == 1);
		String message= entries[0].getMessage();
		assertTrue(message, entries[0].isInfo());
		assertTrue(message, message.indexOf("parse") != -1);
	}

	public void testOrganizeImportsBug202266() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test2", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class E2 {\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test3", false, null);
		buf= new StringBuffer();
		buf.append("package test3;\n");
		buf.append("public class E2 {\n");
		buf.append("}\n");
		pack2.createCompilationUnit("E2.java", buf.toString(), false, null);

		IPackageFragment pack3= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    ArrayList foo;\n");
		buf.append("    E2 foo2;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack3.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.ORGANIZE_IMPORTS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("\n");
		buf.append("public class E1 {\n");
		buf.append("    ArrayList foo;\n");
		buf.append("    E2 foo2;\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testOrganizeImportsBug229570() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface E1 {\n");
		buf.append("  List<IEntity> getChildEntities();\n");
		buf.append("  ArrayList<String> test;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.ORGANIZE_IMPORTS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.List;\n");
		buf.append("\n");
		buf.append("public interface E1 {\n");
		buf.append("  List<IEntity> getChildEntities();\n");
		buf.append("  ArrayList<String> test;\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testCorrectIndetation01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("/**\n");
		buf.append("* \n");
		buf.append("*/\n");
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" * \n");
		buf.append("* \n");
		buf.append(" */\n");
		buf.append("        public class E1 {\n");
		buf.append("    /**\n");
		buf.append("         * \n");
		buf.append(" * \n");
		buf.append("     */\n");
		buf.append("            public void foo() {\n");
		buf.append("            //a\n");
		buf.append("        //b\n");
		buf.append("            }\n");
		buf.append("    /*\n");
		buf.append("     *\n");
		buf.append("           *\n");
		buf.append("* \n");
		buf.append("     */\n");
		buf.append("        }\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		buf= new StringBuffer();
		buf.append("/**\n");
		buf.append(" * \n");
		buf.append(" */\n");
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" * \n");
		buf.append(" * \n");
		buf.append(" */\n");
		buf.append("public class E1 {\n");
		buf.append("    /**\n");
		buf.append("     * \n");
		buf.append("     * \n");
		buf.append("     */\n");
		buf.append("    public void foo() {\n");
		buf.append("        //a\n");
		buf.append("        //b\n");
		buf.append("    }\n");
		buf.append("    /*\n");
		buf.append("     *\n");
		buf.append("     *\n");
		buf.append("     * \n");
		buf.append("     */\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	public void testCorrectIndetationBug202145_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("//  \n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);
		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        //\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testCorrectIndetationBug202145_2() throws Exception {
		IJavaProject project= ProjectTestSetup.getProject();
		project.setOption(DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_LINE_COMMENTS_ON_FIRST_COLUMN, DefaultCodeFormatterConstants.TRUE);
		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			StringBuffer buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("public class E1 {\n");
			buf.append("    public void foo() {\n");
			buf.append("//  \n");
			buf.append("    }\n");
			buf.append("}\n");
			ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

			enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);
			enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
			enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("public class E1 {\n");
			buf.append("    public void foo() {\n");
			buf.append("//\n");
			buf.append("    }\n");
			buf.append("}\n");
			String expected1= buf.toString();

			assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
		} finally {
			project.setOption(DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_LINE_COMMENTS_ON_FIRST_COLUMN, DefaultCodeFormatterConstants.FALSE);
		}
	}

	public void testUnimplementedCode01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public interface IFace {\n");
		buf.append("    void foo();\n");
		buf.append("    void bar();\n");
		buf.append("}\n");
		pack1.createCompilationUnit("IFace.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E01 implements IFace {\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E01.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E02 implements IFace {\n");
		buf.append("    public class Inner implements IFace {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E02.java", buf.toString(), false, null);

		enable(CleanUpConstants.ADD_MISSING_METHODES);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E01 implements IFace {\n");
		buf.append("\n");
		buf.append("    /* comment */\n");
		buf.append("    public void foo() {\n");
		buf.append("        //TODO\n");
		buf.append("        \n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    /* comment */\n");
		buf.append("    public void bar() {\n");
		buf.append("        //TODO\n");
		buf.append("        \n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E02 implements IFace {\n");
		buf.append("    public class Inner implements IFace {\n");
		buf.append("\n");
		buf.append("        /* comment */\n");
		buf.append("        public void foo() {\n");
		buf.append("            //TODO\n");
		buf.append("            \n");
		buf.append("        }\n");
		buf.append("\n");
		buf.append("        /* comment */\n");
		buf.append("        public void bar() {\n");
		buf.append("            //TODO\n");
		buf.append("            \n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    /* comment */\n");
		buf.append("    public void foo() {\n");
		buf.append("        //TODO\n");
		buf.append("        \n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    /* comment */\n");
		buf.append("    public void bar() {\n");
		buf.append("        //TODO\n");
		buf.append("        \n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1, cu2 }, new String[] { expected1, expected2 });
	}

	public void testUnimplementedCode02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public interface IFace {\n");
		buf.append("    void foo();\n");
		buf.append("}\n");
		pack1.createCompilationUnit("IFace.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E01 implements IFace {\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E01.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E02 implements IFace {\n");
		buf.append("    \n");
		buf.append("    public class Inner implements IFace {\n");
		buf.append("        \n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E02.java", buf.toString(), false, null);

		enable(UnimplementedCodeCleanUp.MAKE_TYPE_ABSTRACT);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public abstract class E01 implements IFace {\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public abstract class E02 implements IFace {\n");
		buf.append("    \n");
		buf.append("    public abstract class Inner implements IFace {\n");
		buf.append("        \n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1, cu2 }, new String[] { expected1, expected2 });
	}

}

/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Alex Blewitt - https://bugs.eclipse.org/bugs/show_bug.cgi?id=168954
 *     Chris West (Faux) <eclipse@goeswhere.com> - [clean up] "Use modifier 'final' where possible" can introduce compile errors - https://bugs.eclipse.org/bugs/show_bug.cgi?id=272532
 *     Red Hat Inc. - redundant semicolons test
 *     Fabrice TIERCELIN - Autoboxing and Unboxing test
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IncrementalProjectBuilder;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.core.manipulation.CleanUpOptionsCore;

import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.FixMessages;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.fix.Java50CleanUp;
import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;
import org.eclipse.jdt.internal.ui.fix.RedundantModifiersCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnimplementedCodeCleanUp;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

public class CleanUpTest extends CleanUpTestCase {
	@Rule
	public ProjectTestSetup projectSetup= new ProjectTestSetup();

	IJavaProject fJProject1= getProject();

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	private static class NoChangeRedundantModifiersCleanUp extends RedundantModifiersCleanUp {
		private NoChangeRedundantModifiersCleanUp(Map<String, String> options) {
			super(options);
		}

		@Override
		protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
			return super.createFix(unit);
		}
	}

	@Test
	public void testAddNLSTag01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        String s= \"\";\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "    public String s1 = \"\";\n" //
				+ "    public void foo() {\n" //
				+ "        String s2 = \"\";\n" //
				+ "        String s3 = s2 + \"\";\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= "" //
				+ "package test2;\n" //
				+ "import test1.E2;\n" //
				+ "public class E3 extends E2 {\n" //
				+ "    public static final String s= \"\";\n" //
				+ "    public static String bar(String s1, String s2) {\n" //
				+ "        bar(\"\", \"\");\n" //
				+ "        return \"\";\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.ADD_MISSING_NLS_TAGS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        String s= \"\"; //$NON-NLS-1$\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "    public String s1 = \"\"; //$NON-NLS-1$\n" //
				+ "    public void foo() {\n" //
				+ "        String s2 = \"\"; //$NON-NLS-1$\n" //
				+ "        String s3 = s2 + \"\"; //$NON-NLS-1$\n" //
				+ "    }\n" //
				+ "}\n";
		String expected2= sample;

		sample= "" //
				+ "package test2;\n" //
				+ "import test1.E2;\n" //
				+ "public class E3 extends E2 {\n" //
				+ "    public static final String s= \"\"; //$NON-NLS-1$\n" //
				+ "    public static String bar(String s1, String s2) {\n" //
				+ "        bar(\"\", \"\"); //$NON-NLS-1$ //$NON-NLS-2$\n" //
				+ "        return \"\"; //$NON-NLS-1$\n" //
				+ "    }\n" //
				+ "}\n";
		String expected3= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3});
	}

	@Test
	public void testRemoveNLSTag01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        String s= null; //$NON-NLS-1$\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "    public String s1 = null; //$NON-NLS-1$\n" //
				+ "    public void foo() {\n" //
				+ "        String s2 = null; //$NON-NLS-1$\n" //
				+ "        String s3 = s2 + s2; //$NON-NLS-1$\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= "" //
				+ "package test2;\n" //
				+ "import test1.E2;\n" //
				+ "public class E3 extends E2 {\n" //
				+ "    public static final String s= null; //$NON-NLS-1$\n" //
				+ "    public static String bar(String s1, String s2) {\n" //
				+ "        bar(s2, s1); //$NON-NLS-1$ //$NON-NLS-2$\n" //
				+ "        return s1; //$NON-NLS-1$\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_NLS_TAGS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        String s= null; \n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "    public String s1 = null; \n" //
				+ "    public void foo() {\n" //
				+ "        String s2 = null; \n" //
				+ "        String s3 = s2 + s2; \n" //
				+ "    }\n" //
				+ "}\n";
		String expected2= sample;

		sample= "" //
				+ "package test2;\n" //
				+ "import test1.E2;\n" //
				+ "public class E3 extends E2 {\n" //
				+ "    public static final String s= null; \n" //
				+ "    public static String bar(String s1, String s2) {\n" //
				+ "        bar(s2, s1); \n" //
				+ "        return s1; \n" //
				+ "    }\n" //
				+ "}\n";
		String expected3= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3});
	}

	@Test
	public void testUnusedCode01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.util.ArrayList;\n" //
				+ "import java.util.*;\n" //
				+ "public class E2 {\n" //
				+ "}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= "" //
				+ "package test2;\n" //
				+ "import java.util.HashMap;\n" //
				+ "import test1.E2;\n" //
				+ "import java.io.StringReader;\n" //
				+ "import java.util.HashMap;\n" //
				+ "public class E3 extends E2 {\n" //
				+ "}\n";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "}\n";
		String expected2= sample;

		sample= "" //
				+ "package test2;\n" //
				+ "import test1.E2;\n" //
				+ "public class E3 extends E2 {\n" //
				+ "}\n";
		String expected3= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3});
	}

	@Test
	public void testUnusedCode02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private void foo() {}\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "    private void foo() {}\n" //
				+ "    private void bar() {}\n" //
				+ "}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= "" //
				+ "package test2;\n" //
				+ "public class E3 {\n" //
				+ "    private class E3Inner {\n" //
				+ "        private void foo() {}\n" //
				+ "    }\n" //
				+ "    public void foo() {\n" //
				+ "        Runnable r= new Runnable() {\n" //
				+ "            public void run() {}\n" //
				+ "            private void foo() {};\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_METHODS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "}\n";
		String expected1= sample;

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "}\n";
		String expected2= sample;

		sample= "" //
				+ "package test2;\n" //
				+ "public class E3 {\n" //
				+ "    private class E3Inner {\n" //
				+ "    }\n" //
				+ "    public void foo() {\n" //
				+ "        Runnable r= new Runnable() {\n" //
				+ "            public void run() {};\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";
		String expected3= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3});
	}

	@Test
	public void testUnusedCode03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private E1(int i) {}\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "    public E2() {}\n" //
				+ "    private E2(int i) {}\n" //
				+ "    private E2(String s) {}\n" //
				+ "}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= "" //
				+ "package test2;\n" //
				+ "public class E3 {\n" //
				+ "    public class E3Inner {\n" //
				+ "        private E3Inner(int i) {}\n" //
				+ "    }\n" //
				+ "    private void foo() {\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private E1(int i) {}\n" //
				+ "}\n";
		String expected1= sample;

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "    public E2() {}\n" //
				+ "}\n";
		String expected2= sample;

		sample= "" //
				+ "package test2;\n" //
				+ "public class E3 {\n" //
				+ "    public class E3Inner {\n" //
				+ "        private E3Inner(int i) {}\n" //
				+ "    }\n" //
				+ "    private void foo() {\n" //
				+ "    }\n" //
				+ "}\n";
		String expected3= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3});
	}

	@Test
	public void testUnusedCode04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private int i;\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "    private int i= 10;\n" //
				+ "    private int j= 10;\n" //
				+ "}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= "" //
				+ "package test2;\n" //
				+ "public class E3 {\n" //
				+ "    private int i;\n" //
				+ "    private int j;\n" //
				+ "    private void foo() {\n" //
				+ "        i= 10;\n" //
				+ "        i= 20;\n" //
				+ "        i= j;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "}\n";
		String expected1= sample;

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "}\n";
		String expected2= sample;

		sample= "" //
				+ "package test2;\n" //
				+ "public class E3 {\n" //
				+ "    private int j;\n" //
				+ "    private void foo() {\n" //
				+ "    }\n" //
				+ "}\n";
		String expected3= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3});
	}

	@Test
	public void testUnusedCode05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private class E1Inner{}\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "    private class E2Inner1 {}\n" //
				+ "    private class E2Inner2 {}\n" //
				+ "    public class E2Inner3 {}\n" //
				+ "}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= "" //
				+ "package test2;\n" //
				+ "public class E3 {\n" //
				+ "    public class E3Inner {\n" //
				+ "        private class E3InnerInner {}\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "}\n";
		String expected1= sample;

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "    public class E2Inner3 {}\n" //
				+ "}\n";
		String expected2= sample;

		sample= "" //
				+ "package test2;\n" //
				+ "public class E3 {\n" //
				+ "    public class E3Inner {\n" //
				+ "    }\n" //
				+ "}\n";
		String expected3= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3});
	}

	@Test
	public void testUnusedCode06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        int i= 10;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "    public void foo() {\n" //
				+ "        int i= 10;\n" //
				+ "        int j= 10;\n" //
				+ "    }\n" //
				+ "    private void bar() {\n" //
				+ "        int i= 10;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= "" //
				+ "package test2;\n" //
				+ "public class E3 {\n" //
				+ "    public class E3Inner {\n" //
				+ "        public void foo() {\n" //
				+ "            int i= 10;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "    public void foo() {\n" //
				+ "        int i= 10;\n" //
				+ "        int j= i;\n" //
				+ "        j= 10;\n" //
				+ "        j= 20;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "    public void foo() {\n" //
				+ "    }\n" //
				+ "    private void bar() {\n" //
				+ "    }\n" //
				+ "}\n";
		String expected2= sample;

		sample= "" //
				+ "package test2;\n" //
				+ "public class E3 {\n" //
				+ "    public class E3Inner {\n" //
				+ "        public void foo() {\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "    public void foo() {\n" //
				+ "        int i= 10;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected3= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3});
	}

	@Test
	public void testUnusedCode07() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        int i= bar();\n" //
				+ "        int j= 1;\n" //
				+ "    }\n" //
				+ "    public int bar() {return 1;}\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        bar();\n" //
				+ "    }\n" //
				+ "    public int bar() {return 1;}\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testUnusedCode08() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private int i= bar();\n" //
				+ "    private int j= 1;\n" //
				+ "    public int bar() {return 1;}\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private int i= bar();\n" //
				+ "    public int bar() {return 1;}\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testUnusedCode09() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        int i= 1;\n" //
				+ "        i= bar();\n" //
				+ "        int j= 1;\n" //
				+ "        j= 1;\n" //
				+ "    }\n" //
				+ "    public int bar() {return 1;}\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        bar();\n" //
				+ "    }\n" //
				+ "    public int bar() {return 1;}\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testUnusedCode10() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private int i= 1;\n" //
				+ "    private int j= 1;\n" //
				+ "    public void foo() {\n" //
				+ "        i= bar();\n" //
				+ "        j= 1;\n" //
				+ "    }\n" //
				+ "    public int bar() {return 1;}\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private int i= 1;\n" //
				+ "    public void foo() {\n" //
				+ "        i= bar();\n" //
				+ "    }\n" //
				+ "    public int bar() {return 1;}\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testUnusedCode11() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1  {\n" //
				+ "    private void foo(String s) {\n" //
				+ "        String s1= (String)s;\n" //
				+ "        Object o= (Object)new Object();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "    String s1;\n" //
				+ "    String s2= (String)s1;\n" //
				+ "    public void foo(Integer i) {\n" //
				+ "        Number n= (Number)i;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1  {\n" //
				+ "    private void foo(String s) {\n" //
				+ "        String s1= s;\n" //
				+ "        Object o= new Object();\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "    String s1;\n" //
				+ "    String s2= s1;\n" //
				+ "    public void foo(Integer i) {\n" //
				+ "        Number n= i;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected2= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2}, new String[] {expected1, expected2});
	}

	@Test
	public void testUnusedCodeBug123766() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1  {\n" //
				+ "    private int i,j;\n" //
				+ "    public void foo() {\n" //
				+ "        String s1,s2;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1  {\n" //
				+ "    public void foo() {\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testUnusedCodeBug150853() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import foo.Bar;\n" //
				+ "public class E1 {}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testUnusedCodeBug173014_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "        void foo() {\n" //
				+ "                class Local {}\n" //
				+ "        }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "        void foo() {\n" //
				+ "        }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testUnusedCodeBug173014_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public static void main(String[] args) {\n" //
				+ "        class Local {}\n" //
				+ "        class Local2 {\n" //
				+ "            class LMember {}\n" //
				+ "            class LMember2 extends Local2 {}\n" //
				+ "            LMember m;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public static void main(String[] args) {\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testUnusedCodeBug189394() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.Random;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        Random ran = new Random();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.util.Random;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        new Random();\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testUnusedCodeBug335173_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //

				+ "package test1;\n" //
				+ "import java.util.Comparator;\n" //
				+ "\n" //
				+ "class IntComp implements Comparator<Integer> {\n" //
				+ "    public int compare(Integer o1, Integer o2) {\n" //
				+ "        return ((Integer) o1).intValue() - ((Integer) o2).intValue();\n" //
				+ "    }\n" //
				+ "}\n";

		ICompilationUnit cu1= pack1.createCompilationUnit("IntComp.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.util.Comparator;\n" //
				+ "\n" //
				+ "class IntComp implements Comparator<Integer> {\n" //
				+ "    public int compare(Integer o1, Integer o2) {\n" //
				+ "        return o1.intValue() - o2.intValue();\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testUnusedCodeBug335173_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(Integer n) {\n" //
				+ "        int i = (((Integer) n)).intValue();\n" //
				+ "        foo(((Integer) n));\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(Integer n) {\n" //
				+ "        int i = ((n)).intValue();\n" //
				+ "        foo((n));\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testUnusedCodeBug335173_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(Integer n) {\n" //
				+ "        int i = ((Integer) (n)).intValue();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(Integer n) {\n" //
				+ "        int i = (n).intValue();\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testUnusedCodeBug371078_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //

				+ "package test1;\n" //
				+ "class E1 {\n" //
				+ "    public static Object create(final int a, final int b) {\n" //
				+ "        return (Double) ((double) (a * Math.pow(10, -b)));\n" //
				+ "    }\n" //
				+ "}\n";

		ICompilationUnit cu1= pack1.createCompilationUnit("IntComp.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		sample= "" //
				+ "package test1;\n" //
				+ "class E1 {\n" //
				+ "    public static Object create(final int a, final int b) {\n" //
				+ "        return (a * Math.pow(10, -b));\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testUnusedCodeBug371078_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //

				+ "package test1;\n" //
				+ "public class NestedCasts {\n" //
				+ "	void foo(Integer i) {\n" //
				+ "		Object o= ((((Number) (((Integer) i)))));\n" //
				+ "	}\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("NestedCasts.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class NestedCasts {\n" //
				+ "	void foo(Integer i) {\n" //
				+ "		Object o= (((((i)))));\n" //
				+ "	}\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testJava5001() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    private int field= 1;\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 extends E1{\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    private int field1= 1;\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    private int field2= 2;\n" //
				+ "}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    @Deprecated\n" //
				+ "    private int field= 1;\n" //
				+ "}\n";
		String expected1= sample;

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 extends E1{\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    @Deprecated\n" //
				+ "    private int field1= 1;\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    @Deprecated\n" //
				+ "    private int field2= 2;\n" //
				+ "}\n";
		String expected2= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2}, new String[] {expected1, expected2});
	}

	@Test
	public void testJava5002() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    private int f() {return 1;}\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 extends E1{\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    private int f1() {return 1;}\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    private int f2() {return 2;}\n" //
				+ "}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    @Deprecated\n" //
				+ "    private int f() {return 1;}\n" //
				+ "}\n";
		String expected1= sample;

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 extends E1{\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    @Deprecated\n" //
				+ "    private int f1() {return 1;}\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    @Deprecated\n" //
				+ "    private int f2() {return 2;}\n" //
				+ "}\n";
		String expected2= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2}, new String[] {expected1, expected2});
	}

	@Test
	public void testJava5003() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "/**\n" //
				+ " * @deprecated\n" //
				+ " */\n" //
				+ "public class E1 {\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 extends E1{\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    private class E2Sub1 {}\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    private class E2Sub2 {}\n" //
				+ "}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED);

		sample= "" //
				+ "package test1;\n" //
				+ "/**\n" //
				+ " * @deprecated\n" //
				+ " */\n" //
				+ "@Deprecated\n" //
				+ "public class E1 {\n" //
				+ "}\n";
		String expected1= sample;

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 extends E1{\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    @Deprecated\n" //
				+ "    private class E2Sub1 {}\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    @Deprecated\n" //
				+ "    private class E2Sub2 {}\n" //
				+ "}\n";
		String expected2= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2}, new String[] {expected1, expected2});
	}

	@Test
	public void testJava5004() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo1() {}\n" //
				+ "    protected void foo2() {}\n" //
				+ "    private void foo3() {}\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 extends E1 {\n" //
				+ "    public void foo1() {}\n" //
				+ "    protected void foo2() {}\n" //
				+ "    protected void foo3() {}\n" //
				+ "}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= "" //
				+ "package test2;\n" //
				+ "import test1.E2;\n" //
				+ "public class E3 extends E2 {\n" //
				+ "    public void foo1() {}\n" //
				+ "    protected void foo2() {}\n" //
				+ "    public void foo3() {}\n" //
				+ "}\n";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 extends E1 {\n" //
				+ "    @Override\n" //
				+ "    public void foo1() {}\n" //
				+ "    @Override\n" //
				+ "    protected void foo2() {}\n" //
				+ "    protected void foo3() {}\n" //
				+ "}\n";
		String expected1= sample;

		sample= "" //
				+ "package test2;\n" //
				+ "import test1.E2;\n" //
				+ "public class E3 extends E2 {\n" //
				+ "    @Override\n" //
				+ "    public void foo1() {}\n" //
				+ "    @Override\n" //
				+ "    protected void foo2() {}\n" //
				+ "    @Override\n" //
				+ "    public void foo3() {}\n" //
				+ "}\n";
		String expected2= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {cu1.getBuffer().getContents(), expected1, expected2});
	}

	@Test
	public void testJava5005() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    public void foo1() {}\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    protected void foo2() {}\n" //
				+ "    private void foo3() {}\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    public int i;\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 extends E1 {\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    public void foo1() {}\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    protected void foo2() {}\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    protected void foo3() {}\n" //
				+ "}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= "" //
				+ "package test2;\n" //
				+ "import test1.E2;\n" //
				+ "public class E3 extends E2 {\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    public void foo1() {}\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    protected void foo2() {}\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    public void foo3() {}\n" //
				+ "}\n";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    @Deprecated\n" //
				+ "    public void foo1() {}\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    @Deprecated\n" //
				+ "    protected void foo2() {}\n" //
				+ "    private void foo3() {}\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    @Deprecated\n" //
				+ "    public int i;\n" //
				+ "}\n";
		String expected1= sample;

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 extends E1 {\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    @Deprecated\n" //
				+ "    @Override\n" //
				+ "    public void foo1() {}\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    @Deprecated\n" //
				+ "    @Override\n" //
				+ "    protected void foo2() {}\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    @Deprecated\n" //
				+ "    protected void foo3() {}\n" //
				+ "}\n";
		String expected2= sample;

		sample= "" //
				+ "package test2;\n" //
				+ "import test1.E2;\n" //
				+ "public class E3 extends E2 {\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    @Deprecated\n" //
				+ "    @Override\n" //
				+ "    public void foo1() {}\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    @Deprecated\n" //
				+ "    @Override\n" //
				+ "    protected void foo2() {}\n" //
				+ "    /**\n" //
				+ "     * @deprecated\n" //
				+ "     */\n" //
				+ "    @Deprecated\n" //
				+ "    @Override\n" //
				+ "    public void foo3() {}\n" //
				+ "}\n";
		String expected3= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3});
	}

	@Test
	public void testJava50Bug222257() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.ArrayList;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        ArrayList list= new ArrayList<String>();\n" //
				+ "        ArrayList list2= new ArrayList<String>();\n" //
				+ "        \n" //
				+ "        System.out.println(list);\n" //
				+ "        System.out.println(list2);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		HashMap<String, String> map= new HashMap<>();
		map.put(CleanUpConstants.VARIABLE_DECLARATION_USE_TYPE_ARGUMENTS_FOR_RAW_TYPE_REFERENCES, CleanUpOptions.TRUE);
		Java50CleanUp cleanUp= new Java50CleanUp(map);

		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setResolveBindings(true);
		parser.setProject(getProject());

		Map<String, String> options= RefactoringASTParser.getCompilerOptions(getProject());
		options.putAll(cleanUp.getRequirements().getCompilerOptions());
		parser.setCompilerOptions(options);

		final CompilationUnit[] roots= new CompilationUnit[1];
		parser.createASTs(new ICompilationUnit[] { cu1 }, new String[0], new ASTRequestor() {
			@Override
			public void acceptAST(ICompilationUnit source, CompilationUnit ast) {
				roots[0]= ast;
			}
		}, null);

		IProblem[] problems= roots[0].getProblems();
		assertEquals(2, problems.length);
		for (IProblem problem : problems) {
			ProblemLocation location= new ProblemLocation(problem);
			assertTrue(cleanUp.canFix(cu1, location));
		}
	}

	@Test
	public void testAddOverride15() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "interface I {\n" //
				+ "    void m();\n" //
				+ "    boolean equals(Object obj);\n" //
				+ "}\n" //
				+ "interface J extends I {\n" //
				+ "    void m(); // @Override error in 1.5, not in 1.6\n" //
				+ "}\n" //
				+ "class X implements J {\n" //
				+ "    public void m() {} // @Override error in 1.5, not in 1.6\n" //
				+ "    public int hashCode() { return 0; }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("I.java", sample, false, null);

		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION);

		sample= "" //
				+ "package test1;\n" //
				+ "interface I {\n" //
				+ "    void m();\n" //
				+ "    boolean equals(Object obj);\n" //
				+ "}\n" //
				+ "interface J extends I {\n" //
				+ "    void m(); // @Override error in 1.5, not in 1.6\n" //
				+ "}\n" //
				+ "class X implements J {\n" //
				+ "    public void m() {} // @Override error in 1.5, not in 1.6\n" //
				+ "    @Override\n" //
				+ "    public int hashCode() { return 0; }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testAddOverride16() throws Exception {
		IJavaProject project= JavaProjectHelper.createJavaProject("CleanUpTestProject", "bin");
		try {
			JavaProjectHelper.addRTJar16(project);
			IPackageFragmentRoot src= JavaProjectHelper.addSourceContainer(project, "src");

			IPackageFragment pack1= src.createPackageFragment("test1", false, null);
			String sample= "" //
					+ "package test1;\n" //
					+ "interface I {\n" //
					+ "    void m();\n" //
					+ "    boolean equals(Object obj);\n" //
					+ "}\n" //
					+ "interface J extends I {\n" //
					+ "    void m(); // @Override error in 1.5, not in 1.6\n" //
					+ "}\n" //
					+ "class X implements J {\n" //
					+ "    public void m() {} // @Override error in 1.5, not in 1.6\n" //
					+ "    public int hashCode() { return 0; }\n" //
					+ "}\n";
			ICompilationUnit cu1= pack1.createCompilationUnit("I.java", sample, false, null);

			enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
			enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);
			enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION);

			sample= "" //
					+ "package test1;\n" //
					+ "interface I {\n" //
					+ "    void m();\n" //
					+ "    @Override\n" //
					+ "    boolean equals(Object obj);\n" //
					+ "}\n" //
					+ "interface J extends I {\n" //
					+ "    @Override\n" //
					+ "    void m(); // @Override error in 1.5, not in 1.6\n" //
					+ "}\n" //
					+ "class X implements J {\n" //
					+ "    @Override\n" //
					+ "    public void m() {} // @Override error in 1.5, not in 1.6\n" //
					+ "    @Override\n" //
					+ "    public int hashCode() { return 0; }\n" //
					+ "}\n";
			String expected1= sample;

			assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
		} finally {
			JavaProjectHelper.delete(project);
		}
	}

	@Test
	public void testAddOverride16_no_interface_methods() throws Exception {
		IJavaProject project= JavaProjectHelper.createJavaProject("CleanUpTestProject", "bin");
		try {
			JavaProjectHelper.addRTJar16(project);
			IPackageFragmentRoot src= JavaProjectHelper.addSourceContainer(project, "src");

			IPackageFragment pack1= src.createPackageFragment("test1", false, null);
			String sample= "" //
					+ "package test1;\n" //
					+ "interface I {\n" //
					+ "    void m();\n" //
					+ "    boolean equals(Object obj);\n" //
					+ "}\n" //
					+ "interface J extends I {\n" //
					+ "    void m(); // @Override error in 1.5, not in 1.6\n" //
					+ "}\n" //
					+ "class X implements J {\n" //
					+ "    public void m() {} // @Override error in 1.5, not in 1.6\n" //
					+ "    public int hashCode() { return 0; }\n" //
					+ "}\n";
			ICompilationUnit cu1= pack1.createCompilationUnit("I.java", sample, false, null);

			enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
			enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);

			sample= "" //
					+ "package test1;\n" //
					+ "interface I {\n" //
					+ "    void m();\n" //
					+ "    boolean equals(Object obj);\n" //
					+ "}\n" //
					+ "interface J extends I {\n" //
					+ "    void m(); // @Override error in 1.5, not in 1.6\n" //
					+ "}\n" //
					+ "class X implements J {\n" //
					+ "    public void m() {} // @Override error in 1.5, not in 1.6\n" //
					+ "    @Override\n" //
					+ "    public int hashCode() { return 0; }\n" //
					+ "}\n";
			String expected1= sample;

			assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
		} finally {
			JavaProjectHelper.delete(project);
		}
	}

	/**
	 * Tests if CleanUp works when the number of problems in a single CU is greater than the
	 * Compiler option {@link JavaCore#COMPILER_PB_MAX_PER_UNIT} which has a default value of 100,
	 * see http://bugs.eclipse.org/322543 for details.
	 *
	 * @throws Exception if the something fails while executing this test
	 * @since 3.7
	 */
	@Test
	public void testCleanUpWithCUProblemsGreaterThanMaxProblemsPerCUPreference() throws Exception {
		IJavaProject project= JavaProjectHelper.createJavaProject("CleanUpTestProject", "bin");
		try {
			int count;
			final int PROBLEMS_COUNT= 101;
			JavaProjectHelper.addRTJar16(project);
			IPackageFragmentRoot src= JavaProjectHelper.addSourceContainer(project, "src");
			IPackageFragment pack1= src.createPackageFragment("test1", false, null);
			StringBuilder bld= new StringBuilder();
			bld.append("package test1;\n");
			bld.append("interface I {\n");
			for (count= 0; count < PROBLEMS_COUNT; count++)
				bld.append("    void m" + count + "();\n");
			bld.append("}\n");
			bld.append("class X implements I {\n");
			for (count= 0; count < PROBLEMS_COUNT; count++)
				bld.append("    public void m" + count + "() {} // @Override error in 1.5, not in 1.6\n");
			bld.append("}\n");
			ICompilationUnit cu1= pack1.createCompilationUnit("I.java", bld.toString(), false, null);

			enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
			enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);
			enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION);

			bld= new StringBuilder();
			bld.append("package test1;\n");
			bld.append("interface I {\n");
			for (count= 0; count < PROBLEMS_COUNT; count++)
				bld.append("    void m" + count + "();\n");
			bld.append("}\n");
			bld.append("class X implements I {\n");
			for (count= 0; count < PROBLEMS_COUNT; count++) {
				bld.append("    @Override\n");
				bld.append("    public void m" + count + "() {} // @Override error in 1.5, not in 1.6\n");
			}
			bld.append("}\n");
			String expected1= bld.toString();

			assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
		} finally {
			JavaProjectHelper.delete(project);
		}
	}

	@Test
	public void testCodeStyle01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    String s = \"\"; //$NON-NLS-1$\n" //
				+ "    String t = \"\";  //$NON-NLS-1$\n" //
				+ "    \n" //
				+ "    public void foo() {\n" //
				+ "        s = \"\"; //$NON-NLS-1$\n" //
				+ "        s = s + s;\n" //
				+ "        s = t + s;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 extends E1{\n" //
				+ "    int i = 10;\n" //
				+ "    \n" //
				+ "    public class E2Inner {\n" //
				+ "        public void bar() {\n" //
				+ "            int j = i;\n" //
				+ "            String k = s + t;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "    \n" //
				+ "    public void fooBar() {\n" //
				+ "        String k = s;\n" //
				+ "        int j = i;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    String s = \"\"; //$NON-NLS-1$\n" //
				+ "    String t = \"\";  //$NON-NLS-1$\n" //
				+ "    \n" //
				+ "    public void foo() {\n" //
				+ "        this.s = \"\"; //$NON-NLS-1$\n" //
				+ "        this.s = this.s + this.s;\n" //
				+ "        this.s = this.t + this.s;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 extends E1{\n" //
				+ "    int i = 10;\n" //
				+ "    \n" //
				+ "    public class E2Inner {\n" //
				+ "        public void bar() {\n" //
				+ "            int j = E2.this.i;\n" //
				+ "            String k = E2.this.s + E2.this.t;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "    \n" //
				+ "    public void fooBar() {\n" //
				+ "        String k = this.s;\n" //
				+ "        int j = this.i;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected2= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2}, new String[] {expected1, expected2});
	}

	@Test
	public void testCodeStyle02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public static int i= 0;\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "    private E1 e1;\n" //
				+ "    \n" //
				+ "    public void foo() {\n" //
				+ "        e1= new E1();\n" //
				+ "        int j= e1.i;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "    private E1 e1;\n" //
				+ "    \n" //
				+ "    public void foo() {\n" //
				+ "        this.e1= new E1();\n" //
				+ "        int j= E1.i;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2}, new String[] {cu1.getBuffer().getContents(), expected1});
	}

	@Test
	public void testCodeStyle03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public static int f;\n" //
				+ "    public void foo() {\n" //
				+ "        int i= this.f;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "    public static String s = \"\";\n" //
				+ "    public void foo() {\n" //
				+ "        System.out.println(this.s);\n" //
				+ "        E1 e1= new E1();\n" //
				+ "        int i= e1.f;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= "" //
				+ "package test2;\n" //
				+ "import test1.E1;\n" //
				+ "import test1.E2;\n" //
				+ "public class E3 extends E2 {\n" //
				+ "    public static int g;\n" //
				+ "    {\n" //
				+ "        this.g= (new E1()).f;\n" //
				+ "    }\n" //
				+ "    public static int f= E1.f;\n" //
				+ "}\n";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public static int f;\n" //
				+ "    public void foo() {\n" //
				+ "        int i= E1.f;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "    public static String s = \"\";\n" //
				+ "    public void foo() {\n" //
				+ "        System.out.println(E2.s);\n" //
				+ "        E1 e1= new E1();\n" //
				+ "        int i= E1.f;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected2= sample;

		sample= "" //
				+ "package test2;\n" //
				+ "import test1.E1;\n" //
				+ "import test1.E2;\n" //
				+ "public class E3 extends E2 {\n" //
				+ "    public static int g;\n" //
				+ "    {\n" //
				+ "        E3.g= E1.f;\n" //
				+ "    }\n" //
				+ "    public static int f= E1.f;\n" //
				+ "}\n";
		String expected3= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3});
	}

	@Test
	public void testCodeStyle04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public static int f() {return 1;}\n" //
				+ "    public void foo() {\n" //
				+ "        int i= this.f();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "    public static String s() {return \"\";}\n" //
				+ "    public void foo() {\n" //
				+ "        System.out.println(this.s());\n" //
				+ "        E1 e1= new E1();\n" //
				+ "        int i= e1.f();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= "" //
				+ "package test2;\n" //
				+ "import test1.E1;\n" //
				+ "import test1.E2;\n" //
				+ "public class E3 extends E2 {\n" //
				+ "    public static int g;\n" //
				+ "    {\n" //
				+ "        this.g= (new E1()).f();\n" //
				+ "    }\n" //
				+ "    public static int f= E1.f();\n" //
				+ "}\n";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public static int f() {return 1;}\n" //
				+ "    public void foo() {\n" //
				+ "        int i= E1.f();\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "    public static String s() {return \"\";}\n" //
				+ "    public void foo() {\n" //
				+ "        System.out.println(E2.s());\n" //
				+ "        E1 e1= new E1();\n" //
				+ "        int i= E1.f();\n" //
				+ "    }\n" //
				+ "}\n";
		String expected2= sample;

		sample= "" //
				+ "package test2;\n" //
				+ "import test1.E1;\n" //
				+ "import test1.E2;\n" //
				+ "public class E3 extends E2 {\n" //
				+ "    public static int g;\n" //
				+ "    {\n" //
				+ "        E3.g= E1.f();\n" //
				+ "    }\n" //
				+ "    public static int f= E1.f();\n" //
				+ "}\n";
		String expected3= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3});

	}

	@Test
	public void testCodeStyle05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public String s= \"\";\n" //
				+ "    public E2 e2;\n" //
				+ "    public static int i= 10;\n" //
				+ "    public void foo() {}\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 extends E1 {\n" //
				+ "    public int i = 10;\n" //
				+ "    public E1 e1;\n" //
				+ "    public void fooBar() {}\n" //
				+ "}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= "" //
				+ "package test2;\n" //
				+ "import test1.E1;\n" //
				+ "import test1.E2;\n" //
				+ "public class E3 {\n" //
				+ "    private E1 e1;    \n" //
				+ "    public void foo() {\n" //
				+ "        e1= new E1();\n" //
				+ "        int j= e1.i;\n" //
				+ "        String s= e1.s;\n" //
				+ "        e1.foo();\n" //
				+ "        e1.e2.fooBar();\n" //
				+ "        int k= e1.e2.e2.e2.i;\n" //
				+ "        int h= e1.e2.e2.e1.e2.e1.i;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		sample= "" //
				+ "package test2;\n" //
				+ "import test1.E1;\n" //
				+ "import test1.E2;\n" //
				+ "public class E3 {\n" //
				+ "    private E1 e1;    \n" //
				+ "    public void foo() {\n" //
				+ "        this.e1= new E1();\n" //
				+ "        int j= E1.i;\n" //
				+ "        String s= this.e1.s;\n" //
				+ "        this.e1.foo();\n" //
				+ "        this.e1.e2.fooBar();\n" //
				+ "        int k= this.e1.e2.e2.e2.i;\n" //
				+ "        int h= E1.i;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {cu1.getBuffer().getContents(), cu2.getBuffer().getContents(), expected1});
	}

	@Test
	public void testCodeStyle06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public String s= \"\";\n" //
				+ "    public E1 create() {\n" //
				+ "        return new E1();\n" //
				+ "    }\n" //
				+ "    public void foo() {\n" //
				+ "        create().s= \"\";\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testCodeStyle07() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public static int i = 10;\n" //
				+ "    private static int j = i + 10 * i;\n" //
				+ "    public void foo() {\n" //
				+ "        String s= i + \"\";\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testCodeStyle08() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public final static int i = 1;\n" //
				+ "    public final int j = 2;\n" //
				+ "    private final int k = 3;\n" //
				+ "    public void foo() {\n" //
				+ "        switch (3) {\n" //
				+ "        case i: break;\n" //
				+ "        case j: break;\n" //
				+ "        case k: break;\n" //
				+ "        default: break;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testCodeStyle09() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public abstract class E1Inner1 {\n" //
				+ "        protected int n;\n" //
				+ "        public abstract void foo();\n" //
				+ "    }\n" //
				+ "    public abstract class E1Inner2 {\n" //
				+ "        public abstract void run();\n" //
				+ "    }\n" //
				+ "    public void foo() {\n" //
				+ "        E1Inner1 inner= new E1Inner1() {\n" //
				+ "            public void foo() {\n" //
				+ "                E1Inner2 inner2= new E1Inner2() {\n" //
				+ "                    public void run() {\n" //
				+ "                        System.out.println(n);\n" //
				+ "                    }\n" //
				+ "                };\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testCodeStyle10() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private static final int N;\n" //
				+ "    static {N= 10;}\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testCodeStyle11() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {    \n" //
				+ "    public static int E1N = 10;\n" //
				+ "    public void foo() {\n" //
				+ "        System.out.println(E1N);\n" //
				+ "        E1N = 10;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 extends E1 {\n" //
				+ "    public static int E2N = 10;\n" //
				+ "    public void foo() {\n" //
				+ "        System.out.println(E1N);\n" //
				+ "        E1N = 10;\n" //
				+ "        System.out.println(E2N);\n" //
				+ "        E2N = 10;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= "" //
				+ "package test2;\n" //
				+ "import test1.E2;\n" //
				+ "public class E3 extends E2 {\n" //
				+ "    private static int E3N = 10;\n" //
				+ "    public void foo() {\n" //
				+ "        System.out.println(E1N);\n" //
				+ "        E1N = 10;\n" //
				+ "        System.out.println(E2N);\n" //
				+ "        E2N = 10;\n" //
				+ "        System.out.println(E3N);\n" //
				+ "        E3N = 10;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {    \n" //
				+ "    public static int E1N = 10;\n" //
				+ "    public void foo() {\n" //
				+ "        System.out.println(E1.E1N);\n" //
				+ "        E1.E1N = 10;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 extends E1 {\n" //
				+ "    public static int E2N = 10;\n" //
				+ "    public void foo() {\n" //
				+ "        System.out.println(E1.E1N);\n" //
				+ "        E1.E1N = 10;\n" //
				+ "        System.out.println(E2.E2N);\n" //
				+ "        E2.E2N = 10;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected2= sample;

		sample= "" //
				+ "package test2;\n" //
				+ "import test1.E1;\n" //
				+ "import test1.E2;\n" //
				+ "public class E3 extends E2 {\n" //
				+ "    private static int E3N = 10;\n" //
				+ "    public void foo() {\n" //
				+ "        System.out.println(E1.E1N);\n" //
				+ "        E1.E1N = 10;\n" //
				+ "        System.out.println(E2.E2N);\n" //
				+ "        E2.E2N = 10;\n" //
				+ "        System.out.println(E3.E3N);\n" //
				+ "        E3.E3N = 10;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected3= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {expected1, expected2, expected3});

	}

	@Test
	public void testCodeStyle12() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public final static int N1 = 10;\n" //
				+ "    public static int N2 = N1;\n" //
				+ "    {\n" //
				+ "        System.out.println(N1);\n" //
				+ "        N2 = 10;\n" //
				+ "        System.out.println(N2);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public final static int N1 = 10;\n" //
				+ "    public static int N2 = E1.N1;\n" //
				+ "    {\n" //
				+ "        System.out.println(E1.N1);\n" //
				+ "        E1.N2 = 10;\n" //
				+ "        System.out.println(E1.N2);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCodeStyle13() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private static class E1Inner {\n" //
				+ "        private static class E1InnerInner {\n" //
				+ "            public static int N = 10;\n" //
				+ "            static {\n" //
				+ "                System.out.println(N);\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private static class E1Inner {\n" //
				+ "        private static class E1InnerInner {\n" //
				+ "            public static int N = 10;\n" //
				+ "            static {\n" //
				+ "                System.out.println(E1InnerInner.N);\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCodeStyle14() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    static class E1Inner {\n" //
				+ "        public static class E1InnerInner {\n" //
				+ "            public static int N = 10;\n" //
				+ "            public void foo() {\n" //
				+ "                System.out.println(N);\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    static class E1Inner {\n" //
				+ "        public static class E1InnerInner {\n" //
				+ "            public static int N = 10;\n" //
				+ "            public void foo() {\n" //
				+ "                System.out.println(E1InnerInner.N);\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCodeStyle15() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    static class E1Inner {\n" //
				+ "        public static class E1InnerInner {\n" //
				+ "            public static int N = 10;\n" //
				+ "            public void foo() {\n" //
				+ "                System.out.println((new E1InnerInner()).N);\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    static class E1Inner {\n" //
				+ "        public static class E1InnerInner {\n" //
				+ "            public static int N = 10;\n" //
				+ "            public void foo() {\n" //
				+ "                System.out.println(E1InnerInner.N);\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCodeStyle16() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public static int E1N;\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 extends E1 {\n" //
				+ "    public static int E2N = 10;\n" //
				+ "    public void foo() {\n" //
				+ "        System.out.println(E1.E1N);\n" //
				+ "        System.out.println(E2.E1N);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= "" //
				+ "package test2;\n" //
				+ "import test1.E1;\n" //
				+ "import test1.E2;\n" //
				+ "public class E3 extends E2 {\n" //
				+ "    public void foo() {\n" //
				+ "        System.out.println(E1.E1N);\n" //
				+ "        System.out.println(E2.E1N);\n" //
				+ "        System.out.println(E3.E1N);\n" //
				+ "        System.out.println(E2.E2N);\n" //
				+ "        System.out.println(E3.E2N);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 extends E1 {\n" //
				+ "    public static int E2N = 10;\n" //
				+ "    public void foo() {\n" //
				+ "        System.out.println(E1.E1N);\n" //
				+ "        System.out.println(E1.E1N);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		sample= "" //
				+ "package test2;\n" //
				+ "import test1.E1;\n" //
				+ "import test1.E2;\n" //
				+ "public class E3 extends E2 {\n" //
				+ "    public void foo() {\n" //
				+ "        System.out.println(E1.E1N);\n" //
				+ "        System.out.println(E1.E1N);\n" //
				+ "        System.out.println(E1.E1N);\n" //
				+ "        System.out.println(E2.E2N);\n" //
				+ "        System.out.println(E2.E2N);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected2= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2, cu3}, new String[] {cu1.getBuffer().getContents(), expected1, expected2});

	}

	@Test
	public void testCodeStyle17() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public boolean b= true;\n" //
				+ "    public void foo() {\n" //
				+ "        if (b)\n" //
				+ "            System.out.println(10);\n" //
				+ "        if (b) {\n" //
				+ "            System.out.println(10);\n" //
				+ "        } else\n" //
				+ "            System.out.println(10);\n" //
				+ "        if (b)\n" //
				+ "            System.out.println(10);\n" //
				+ "        else\n" //
				+ "            System.out.println(10);\n" //
				+ "        while (b)\n" //
				+ "            System.out.println(10);\n" //
				+ "        do\n" //
				+ "            System.out.println(10);\n" //
				+ "        while (b);\n" //
				+ "        for(;;)\n" //
				+ "            System.out.println(10);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public boolean b= true;\n" //
				+ "    public void foo() {\n" //
				+ "        if (b) {\n" //
				+ "            System.out.println(10);\n" //
				+ "        }\n" //
				+ "        if (b) {\n" //
				+ "            System.out.println(10);\n" //
				+ "        } else {\n" //
				+ "            System.out.println(10);\n" //
				+ "        }\n" //
				+ "        if (b) {\n" //
				+ "            System.out.println(10);\n" //
				+ "        } else {\n" //
				+ "            System.out.println(10);\n" //
				+ "        }\n" //
				+ "        while (b) {\n" //
				+ "            System.out.println(10);\n" //
				+ "        }\n" //
				+ "        do {\n" //
				+ "            System.out.println(10);\n" //
				+ "        } while (b);\n" //
				+ "        for(;;) {\n" //
				+ "            System.out.println(10);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCodeStyle18() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public boolean b, q;\n" //
				+ "    public void foo() {\n" //
				+ "        if (b)\n" //
				+ "            System.out.println(1);\n" //
				+ "        else if (q)\n" //
				+ "            System.out.println(1);\n" //
				+ "        else\n" //
				+ "            if (b && q)\n" //
				+ "                System.out.println(1);\n" //
				+ "            else\n" //
				+ "                System.out.println(2);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public boolean b, q;\n" //
				+ "    public void foo() {\n" //
				+ "        if (b) {\n" //
				+ "            System.out.println(1);\n" //
				+ "        } else if (q) {\n" //
				+ "            System.out.println(1);\n" //
				+ "        } else\n" //
				+ "            if (b && q) {\n" //
				+ "                System.out.println(1);\n" //
				+ "            } else {\n" //
				+ "                System.out.println(2);\n" //
				+ "            }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCodeStyle19() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public boolean b, q;\n" //
				+ "    public void foo() {\n" //
				+ "        for (;b;)\n" //
				+ "            for (;q;)\n" //
				+ "                if (b)\n" //
				+ "                    System.out.println(1);\n" //
				+ "                else if (q)\n" //
				+ "                    System.out.println(2);\n" //
				+ "                else\n" //
				+ "                    System.out.println(3);\n" //
				+ "        for (;b;)\n" //
				+ "            for (;q;) {\n" //
				+ "                \n" //
				+ "            }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public boolean b, q;\n" //
				+ "    public void foo() {\n" //
				+ "        for (;b;) {\n" //
				+ "            for (;q;) {\n" //
				+ "                if (b) {\n" //
				+ "                    System.out.println(1);\n" //
				+ "                } else if (q) {\n" //
				+ "                    System.out.println(2);\n" //
				+ "                } else {\n" //
				+ "                    System.out.println(3);\n" //
				+ "                }\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "        for (;b;) {\n" //
				+ "            for (;q;) {\n" //
				+ "                \n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCodeStyle20() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public boolean b, q;\n" //
				+ "    public void foo() {\n" //
				+ "        while (b)\n" //
				+ "            while (q)\n" //
				+ "                if (b)\n" //
				+ "                    System.out.println(1);\n" //
				+ "                else if (q)\n" //
				+ "                    System.out.println(2);\n" //
				+ "                else\n" //
				+ "                    System.out.println(3);\n" //
				+ "        while (b)\n" //
				+ "            while (q) {\n" //
				+ "                \n" //
				+ "            }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public boolean b, q;\n" //
				+ "    public void foo() {\n" //
				+ "        while (b) {\n" //
				+ "            while (q) {\n" //
				+ "                if (b) {\n" //
				+ "                    System.out.println(1);\n" //
				+ "                } else if (q) {\n" //
				+ "                    System.out.println(2);\n" //
				+ "                } else {\n" //
				+ "                    System.out.println(3);\n" //
				+ "                }\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "        while (b) {\n" //
				+ "            while (q) {\n" //
				+ "                \n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCodeStyle21() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public boolean b, q;\n" //
				+ "    public void foo() {\n" //
				+ "        do\n" //
				+ "            do\n" //
				+ "                if (b)\n" //
				+ "                    System.out.println(1);\n" //
				+ "                else if (q)\n" //
				+ "                    System.out.println(2);\n" //
				+ "                else\n" //
				+ "                    System.out.println(3);\n" //
				+ "            while (q);\n" //
				+ "        while (b);\n" //
				+ "        do\n" //
				+ "            do {\n" //
				+ "                \n" //
				+ "            } while (q);\n" //
				+ "        while (b);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public boolean b, q;\n" //
				+ "    public void foo() {\n" //
				+ "        do {\n" //
				+ "            do {\n" //
				+ "                if (b) {\n" //
				+ "                    System.out.println(1);\n" //
				+ "                } else if (q) {\n" //
				+ "                    System.out.println(2);\n" //
				+ "                } else {\n" //
				+ "                    System.out.println(3);\n" //
				+ "                }\n" //
				+ "            } while (q);\n" //
				+ "        } while (b);\n" //
				+ "        do {\n" //
				+ "            do {\n" //
				+ "                \n" //
				+ "            } while (q);\n" //
				+ "        } while (b);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCodeStyle22() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import test2.I1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        I1 i1= new I1() {\n" //
				+ "            private static final int N= 10;\n" //
				+ "            public void foo() {\n" //
				+ "                System.out.println(N);\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= "" //
				+ "package test2;\n" //
				+ "public interface I1 {}\n";
		pack2.createCompilationUnit("I1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testCodeStyle23() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private int fNb= 0;\n" //
				+ "    public void foo() {\n" //
				+ "        if (true)\n" //
				+ "            fNb++;\n" //
				+ "        String s; //$NON-NLS-1$\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.REMOVE_UNNECESSARY_NLS_TAGS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private int fNb= 0;\n" //
				+ "    public void foo() {\n" //
				+ "        if (true) {\n" //
				+ "            this.fNb++;\n" //
				+ "        }\n" //
				+ "        String s; \n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCodeStyle24() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        if (true)\n" //
				+ "            System.out.println(\"\");\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.REMOVE_UNNECESSARY_NLS_TAGS);
		enable(CleanUpConstants.ADD_MISSING_NLS_TAGS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        if (true) {\n" //
				+ "            System.out.println(\"\"); //$NON-NLS-1$\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCodeStyle25() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        System.out.println(I1Impl.N);\n" //
				+ "        I1 i1= new I1();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "import test2.I1;\n" //
				+ "public class I1Impl implements I1 {}\n";
		pack1.createCompilationUnit("I1Impl.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "public class I1 {}\n";
		pack1.createCompilationUnit("I1.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= "" //
				+ "package test2;\n" //
				+ "public interface I1 {\n" //
				+ "    public static int N= 10;\n" //
				+ "}\n";
		pack2.createCompilationUnit("I1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        System.out.println(test2.I1.N);\n" //
				+ "        I1 i1= new I1();\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCodeStyle26() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {}\n" //
				+ "    private void bar() {\n" //
				+ "        foo();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {}\n" //
				+ "    private void bar() {\n" //
				+ "        this.foo();\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCodeStyle27() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public static void foo() {}\n" //
				+ "    private void bar() {\n" //
				+ "        foo();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public static void foo() {}\n" //
				+ "    private void bar() {\n" //
				+ "        E1.foo();\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCodeStyleBug118204() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    static String s;\n" //
				+ "    public void foo() {\n" //
				+ "        System.out.println(s);\n" //
				+ "    }\n" //
				+ "    E1(){}\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    static String s;\n" //
				+ "    public void foo() {\n" //
				+ "        System.out.println(E1.s);\n" //
				+ "    }\n" //
				+ "    E1(){}\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCodeStyleBug114544() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        System.out.println(new E1().i);\n" //
				+ "    }\n" //
				+ "    public static int i= 10;\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        new E1();\n" //
				+ "        System.out.println(E1.i);\n" //
				+ "    }\n" //
				+ "    public static int i= 10;\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCodeStyleBug119170_01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public static void foo() {}\n" //
				+ "}\n";
		pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "    private static class E1 {}\n" //
				+ "    public void bar() {\n" //
				+ "        test1.E1 e1= new test1.E1();\n" //
				+ "        e1.foo();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E2.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "    private static class E1 {}\n" //
				+ "    public void bar() {\n" //
				+ "        test1.E1 e1= new test1.E1();\n" //
				+ "        test1.E1.foo();\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCodeStyleBug119170_02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public static void foo() {}\n" //
				+ "}\n";
		pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "    private static String E1= \"\";\n" //
				+ "    public void foo() {\n" //
				+ "        test1.E1 e1= new test1.E1();\n" //
				+ "        e1.foo();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E2.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 {\n" //
				+ "    private static String E1= \"\";\n" //
				+ "    public void foo() {\n" //
				+ "        test1.E1 e1= new test1.E1();\n" //
				+ "        test1.E1.foo();\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCodeStyleBug123468() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    protected int field;\n" //
				+ "}\n";
		pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 extends E1 {\n" //
				+ "    private int field;\n" //
				+ "    public void foo() {\n" //
				+ "        super.field= 10;\n" //
				+ "        field= 10;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E2.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 extends E1 {\n" //
				+ "    private int field;\n" //
				+ "    public void foo() {\n" //
				+ "        super.field= 10;\n" //
				+ "        this.field= 10;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCodeStyleBug129115() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private static int NUMBER;\n" //
				+ "    public void reset() {\n" //
				+ "        NUMBER= 0;\n" //
				+ "    }\n" //
				+ "    enum MyEnum {\n" //
				+ "        STATE_1, STATE_2, STATE_3\n" //
				+ "      };\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private static int NUMBER;\n" //
				+ "    public void reset() {\n" //
				+ "        E1.NUMBER= 0;\n" //
				+ "    }\n" //
				+ "    enum MyEnum {\n" //
				+ "        STATE_1, STATE_2, STATE_3\n" //
				+ "      };\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCodeStyleBug135219() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E { \n" //
				+ "    public int i;\n" //
				+ "    public void print(int j) {}\n" //
				+ "    public void foo() {\n" //
				+ "        print(i);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS);

		sample= "" //
				+ "package test;\n" //
				+ "public class E { \n" //
				+ "    public int i;\n" //
				+ "    public void print(int j) {}\n" //
				+ "    public void foo() {\n" //
				+ "        this.print(this.i);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCodeStyleBug_138318() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E<I> {\n" //
				+ "    private static int I;\n" //
				+ "    private static String STR() {return \"\";}\n" //
				+ "    public void foo() {\n" //
				+ "        Runnable runnable = new Runnable() {\n" //
				+ "            public void run() {\n" //
				+ "                System.out.println(I);\n" //
				+ "                System.out.println(STR());\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD);

		sample= "" //
				+ "package test;\n" //
				+ "public class E<I> {\n" //
				+ "    private static int I;\n" //
				+ "    private static String STR() {return \"\";}\n" //
				+ "    public void foo() {\n" //
				+ "        Runnable runnable = new Runnable() {\n" //
				+ "            public void run() {\n" //
				+ "                System.out.println(E.I);\n" //
				+ "                System.out.println(E.STR());\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCodeStyleBug138325_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E<I> {\n" //
				+ "    private int i;\n" //
				+ "    private String str() {return \"\";}\n" //
				+ "    public void foo() {\n" //
				+ "        System.out.println(i);\n" //
				+ "        System.out.println(str());\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS);

		sample= "" //
				+ "package test;\n" //
				+ "public class E<I> {\n" //
				+ "    private int i;\n" //
				+ "    private String str() {return \"\";}\n" //
				+ "    public void foo() {\n" //
				+ "        System.out.println(this.i);\n" //
				+ "        System.out.println(this.str());\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCodeStyleBug138325_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E<I> {\n" //
				+ "    private int i;\n" //
				+ "    private String str() {return \"\";}\n" //
				+ "    public void foo() {\n" //
				+ "        Runnable runnable = new Runnable() {\n" //
				+ "            public void run() {\n" //
				+ "                System.out.println(i);\n" //
				+ "                System.out.println(str());\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS);

		sample= "" //
				+ "package test;\n" //
				+ "public class E<I> {\n" //
				+ "    private int i;\n" //
				+ "    private String str() {return \"\";}\n" //
				+ "    public void foo() {\n" //
				+ "        Runnable runnable = new Runnable() {\n" //
				+ "            public void run() {\n" //
				+ "                System.out.println(E.this.i);\n" //
				+ "                System.out.println(E.this.str());\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCodeStyleQualifyMethodAccessesImportConflictBug_552461() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "import static java.util.Date.parse;\n" //
				+ "\n" //
				+ "import java.sql.Date;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public Object addFullyQualifiedName(String dateText, Date sqlDate) {\n" //
				+ "        return parse(dateText);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD);

		sample= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "import static java.util.Date.parse;\n" //
				+ "\n" //
				+ "import java.sql.Date;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public Object addFullyQualifiedName(String dateText, Date sqlDate) {\n" //
				+ "        return java.util.Date.parse(dateText);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testCodeStyleQualifyMethodAccessesAlreadyImportedBug_552461() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "import static java.util.Date.parse;\n" //
				+ "\n" //
				+ "import java.util.Date;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public Object addFullyQualifiedName(String dateText, Date sqlDate) {\n" //
				+ "        return parse(dateText);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD);

		sample= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "import static java.util.Date.parse;\n" //
				+ "\n" //
				+ "import java.util.Date;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public Object addFullyQualifiedName(String dateText, Date sqlDate) {\n" //
				+ "        return Date.parse(dateText);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testCodeStyle_Bug140565() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.io.*;\n" //
				+ "public class E1 {\n" //
				+ "        static class ClassA {static ClassB B;}\n" //
				+ "        static class ClassB {static ClassC C;}\n" //
				+ "        static class ClassC {static ClassD D;}\n" //
				+ "        static class ClassD {}\n" //
				+ "\n" //
				+ "        public void foo() {\n" //
				+ "                ClassA.B.C.D.toString();\n" //
				+ "        }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "        static class ClassA {static ClassB B;}\n" //
				+ "        static class ClassB {static ClassC C;}\n" //
				+ "        static class ClassC {static ClassD D;}\n" //
				+ "        static class ClassD {}\n" //
				+ "\n" //
				+ "        public void foo() {\n" //
				+ "                ClassC.D.toString();\n" //
				+ "        }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCodeStyleBug157480() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 extends ETop {\n" //
				+ "    public void bar(boolean b) {\n" //
				+ "        if (b == true && b || b) {}\n" //
				+ "    }\n" //
				+ "}\n" //
				+ "class ETop {\n" //
				+ "    public void bar(boolean b) {}\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 extends ETop {\n" //
				+ "    @Override\n" //
				+ "    public void bar(boolean b) {\n" //
				+ "        if (((b == true) && b) || b) {}\n" //
				+ "    }\n" //
				+ "}\n" //
				+ "class ETop {\n" //
				+ "    public void bar(boolean b) {}\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCodeStyleBug154787() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "interface E1 {String FOO = \"FOO\";}\n";
		pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E2 implements E1 {}\n";
		pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= "" //
				+ "package test2;\n" //
				+ "import test1.E2;\n" //
				+ "public class E3 {\n" //
				+ "    public String foo() {\n" //
				+ "        return E2.FOO;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testCodeStyleBug189398() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(Object o) {\n" //
				+ "        if (o != null)\n" //
				+ "            System.out.println(o);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(Object o) {\n" //
				+ "        if (o != null) {\n" //
				+ "            System.out.println(o);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testCodeStyleBug238828_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private int field;\n" //
				+ "\n" //
				+ "    public String foo() {\n" //
				+ "        return \"Foo\" + field //MyComment\n" //
				+ "                    + field;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private int field;\n" //
				+ "\n" //
				+ "    public String foo() {\n" //
				+ "        return \"Foo\" + this.field //MyComment\n" //
				+ "                    + this.field;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testCodeStyleBug238828_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private static int FIELD;\n" //
				+ "\n" //
				+ "    public String foo() {\n" //
				+ "        return \"Foo\" + FIELD //MyComment\n" //
				+ "                    + FIELD;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private static int FIELD;\n" //
				+ "\n" //
				+ "    public String foo() {\n" //
				+ "        return \"Foo\" + E1.FIELD //MyComment\n" //
				+ "                    + E1.FIELD;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testCodeStyleBug346230() throws Exception {
		IJavaProject project= JavaProjectHelper.createJavaProject("CleanUpTestProject", "bin");
		try {
			JavaProjectHelper.addRTJar16(project);
			IPackageFragmentRoot src= JavaProjectHelper.addSourceContainer(project, "src");
			IPackageFragment pack1= src.createPackageFragment("test1", false, null);

			String sample= "" //
					+ "package test1;\n" //
					+ "interface CinematicEvent {\n" //
					+ "    public void stop();\n" //
					+ "    public boolean internalUpdate();\n" //
					+ "}\n";
			ICompilationUnit cu1= pack1.createCompilationUnit("CinematicEvent.java", sample, false, null);

			sample= "" //
					+ "package test1;\n" //
					+ "abstract class E1 implements CinematicEvent {\n" //
					+ "\n" //
					+ "    protected PlayState playState = PlayState.Stopped;\n" //
					+ "    protected LoopMode loopMode = LoopMode.DontLoop;\n" //
					+ "\n" //
					+ "    public boolean internalUpdate() {\n" //
					+ "        return loopMode == loopMode.DontLoop;\n" //
					+ "    }\n" //
					+ "\n" //
					+ "    public void stop() {\n" //
					+ "    }\n" //
					+ "\n" //
					+ "    public void read() {\n" //
					+ "        Object ic= new Object();\n" //
					+ "        playState.toString();\n" //
					+ "    }\n" //
					+ "\n" //
					+ "    enum PlayState {\n" //
					+ "        Stopped\n" //
					+ "    }\n" //
					+ "    enum LoopMode {\n" //
					+ "        DontLoop\n" //
					+ "    }\n" //
					+ "}\n";
			ICompilationUnit cu2= pack1.createCompilationUnit("E1.java", sample, false, null);

			enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
			enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
			enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
			enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
			enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
			enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);
			enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
			enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);
			enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION);

			sample= "" //
					+ "package test1;\n" //
					+ "abstract class E1 implements CinematicEvent {\n" //
					+ "\n" //
					+ "    protected PlayState playState = PlayState.Stopped;\n" //
					+ "    protected LoopMode loopMode = LoopMode.DontLoop;\n" //
					+ "\n" //
					+ "    @Override\n" //
					+ "    public boolean internalUpdate() {\n" //
					+ "        return this.loopMode == LoopMode.DontLoop;\n" //
					+ "    }\n" //
					+ "\n" //
					+ "    @Override\n" //
					+ "    public void stop() {\n" //
					+ "    }\n" //
					+ "\n" //
					+ "    public void read() {\n" //
					+ "        final Object ic= new Object();\n" //
					+ "        this.playState.toString();\n" //
					+ "    }\n" //
					+ "\n" //
					+ "    enum PlayState {\n" //
					+ "        Stopped\n" //
					+ "    }\n" //
					+ "    enum LoopMode {\n" //
					+ "        DontLoop\n" //
					+ "    }\n" //
					+ "}\n";
			String expected1= sample;

			assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1, cu2 }, new String[] { cu1.getBuffer().getContents(), expected1 });
		} finally {
			JavaProjectHelper.delete(project);
		}

	}

	@Test
	public void testCodeStyle_StaticAccessThroughInstance_Bug307407() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private final String localString = new MyClass().getMyString();\n" //
				+ "    public static class MyClass {\n" //
				+ "        public static String getMyString() {\n" //
				+ "            return \"a\";\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testRemoveNonStaticQualifier_Bug219204_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void test() {\n" //
				+ "        E1 t = E1.bar().g().g().foo(E1.foo(null).bar()).bar();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private static E1 foo(E1 t) {\n" //
				+ "        return null;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private static E1 bar() {\n" //
				+ "        return null;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private E1 g() {\n" //
				+ "        return null;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void test() {\n" //
				+ "        E1.bar().g().g();\n" //
				+ "        E1.foo(null);\n" //
				+ "        E1.foo(E1.bar());\n" //
				+ "        E1 t = E1.bar();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private static E1 foo(E1 t) {\n" //
				+ "        return null;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private static E1 bar() {\n" //
				+ "        return null;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private E1 g() {\n" //
				+ "        return null;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveNonStaticQualifier_Bug219204_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void test() {\n" //
				+ "        while (true)\n" //
				+ "            new E1().bar1().bar2().bar3();\n" //
				+ "    }\n" //
				+ "    private static E1 bar1() {\n" //
				+ "        return null;\n" //
				+ "    }\n" //
				+ "    private static E1 bar2() {\n" //
				+ "        return null;\n" //
				+ "    }\n" //
				+ "    private static E1 bar3() {\n" //
				+ "        return null;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void test() {\n" //
				+ "        while (true) {\n" //
				+ "            new E1();\n" //
				+ "            E1.bar1();\n" //
				+ "            E1.bar2();\n" //
				+ "            E1.bar3();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "    private static E1 bar1() {\n" //
				+ "        return null;\n" //
				+ "    }\n" //
				+ "    private static E1 bar2() {\n" //
				+ "        return null;\n" //
				+ "    }\n" //
				+ "    private static E1 bar3() {\n" //
				+ "        return null;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testChangeNonstaticAccessToStatic_Bug439733() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "class Singleton {\n" //
				+ "    public static String name = \"The Singleton\";\n" //
				+ "    public static Singleton instance = new Singleton();\n" //
				+ "    public static Singleton getInstance() {\n" //
				+ "        return instance;\n" //
				+ "    }\n" //
				+ "}\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public static void main(String[] args) {\n" //
				+ "        System.out.println(Singleton.instance.name);\n" //
				+ "        System.out.println(Singleton.getInstance().name);\n" //
				+ "        System.out.println(Singleton.getInstance().getInstance().name);\n" //
				+ "        System.out.println(new Singleton().name);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		sample= "" //
				+ "package test1;\n" //
				+ "class Singleton {\n" //
				+ "    public static String name = \"The Singleton\";\n" //
				+ "    public static Singleton instance = new Singleton();\n" //
				+ "    public static Singleton getInstance() {\n" //
				+ "        return instance;\n" //
				+ "    }\n" //
				+ "}\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public static void main(String[] args) {\n" //
				+ "        System.out.println(Singleton.name);\n" //
				+ "        Singleton.getInstance();\n" //
				+ "        System.out.println(Singleton.name);\n" //
				+ "        Singleton.getInstance();\n" //
				+ "        Singleton.getInstance();\n" //
				+ "        System.out.println(Singleton.name);\n" //
				+ "        new Singleton();\n" //
				+ "        System.out.println(Singleton.name);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testCombination01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    private int i= 10;\n" //
				+ "    private int j= 20;\n" //
				+ "    \n" //
				+ "    public void foo() {\n" //
				+ "        i= j;\n" //
				+ "        i= 20;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    private int j= 20;\n" //
				+ "    \n" //
				+ "    public void foo() {\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCombination02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        if (true)\n" //
				+ "            System.out.println(\"\");\n" //
				+ "        if (true)\n" //
				+ "            System.out.println(\"\");\n" //
				+ "        if (true)\n" //
				+ "            System.out.println(\"\");\n" //
				+ "        System.out.println(\"\");\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.ADD_MISSING_NLS_TAGS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        if (true) {\n" //
				+ "            System.out.println(\"\"); //$NON-NLS-1$\n" //
				+ "        }\n" //
				+ "        if (true) {\n" //
				+ "            System.out.println(\"\"); //$NON-NLS-1$\n" //
				+ "        }\n" //
				+ "        if (true) {\n" //
				+ "            System.out.println(\"\"); //$NON-NLS-1$\n" //
				+ "        }\n" //
				+ "        System.out.println(\"\"); //$NON-NLS-1$\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCombination03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;  \n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1  {\n" //
				+ "    private List<String> fList;\n" //
				+ "    public void foo() {\n" //
				+ "        for (Iterator<String> iter = fList.iterator(); iter.hasNext();) {\n" //
				+ "            String element = (String) iter.next();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;  \n" //
				+ "import java.util.List;\n" //
				+ "public class E1  {\n" //
				+ "    private List<String> fList;\n" //
				+ "    public void foo() {\n" //
				+ "        for (String string : this.fList) {\n" //
				+ "            String element = (String) string;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testBug245254() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private int i= 0;\n" //
				+ "    void method() {\n" //
				+ "        if (true\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private int i= 0;\n" //
				+ "    void method() {\n" //
				+ "        if (true\n" //
				+ "    }\n" //
				+ "}\n";
		String expected= sample;
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected });
	}

	@Test
	public void testCombinationBug120585() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private int i= 0;\n" //
				+ "    void method() {\n" //
				+ "        int[] array= null;\n" //
				+ "        for (int i= 0; i < array.length; i++)\n" //
				+ "            System.out.println(array[i]);\n" //
				+ "        i= 12;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS);
		enable(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    void method() {\n" //
				+ "        int[] array= null;\n" //
				+ "        for (int element : array) {\n" //
				+ "            System.out.println(element);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCombinationBug125455() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1  {\n" //
				+ "    private void bar(boolean wait) {\n" //
				+ "        if (!wait) \n" //
				+ "            return;\n" //
				+ "    }\n" //
				+ "    private void foo(String s) {\n" //
				+ "        String s1= \"\";\n" //
				+ "        if (s.equals(\"\"))\n" //
				+ "            System.out.println();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.ADD_MISSING_NLS_TAGS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1  {\n" //
				+ "    private void bar(boolean wait) {\n" //
				+ "        if (!wait) {\n" //
				+ "            return;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "    private void foo(String s) {\n" //
				+ "        String s1= \"\"; //$NON-NLS-1$\n" //
				+ "        if (s.equals(\"\")) { //$NON-NLS-1$\n" //
				+ "            System.out.println();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCombinationBug157468() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private void bar(boolean bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb) {\n" //
				+ "        if (bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb) { // a b c d e f g h i j k\n" //
				+ "            final String s = \"\";\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);

		Hashtable<String, String> options= TestOptions.getDefaultOptions();

		Map<String, String> formatterSettings= DefaultCodeFormatterConstants.getEclipseDefaultSettings();
		formatterSettings.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_COUNT_LINE_LENGTH_FROM_STARTING_POSITION,
				DefaultCodeFormatterConstants.FALSE);
		options.putAll(formatterSettings);

		JavaCore.setOptions(options);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "	private void bar(boolean bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb) {\n" //
				+ "		if (bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb) { // a b c d e f g\n" //
				+ "																// h i j k\n" //
				+ "			final String s = \"\";\n" //
				+ "		}\n" //
				+ "	}\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCombinationBug234984_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void method(String[] arr) {\n" //
				+ "        for (int i = 0; i < arr.length; i++) {\n" //
				+ "            String item = arr[i];\n" //
				+ "            String item2 = item + \"a\";\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void method(String[] arr) {\n" //
				+ "        for (final String item : arr) {\n" //
				+ "            final String item2 = item + \"a\";\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testCombinationBug234984_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    public void method(List<E1> es) {\n" //
				+ "        for (Iterator<E1> iterator = es.iterator(); iterator.hasNext();) {\n" //
				+ "            E1 next = iterator.next();\n" //
				+ "            next= new E1();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    public void method(List<E1> es) {\n" //
				+ "        for (E1 next : es) {\n" //
				+ "            next= new E1();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testSerialVersion01() throws Exception {

		JavaProjectHelper.set14CompilerOptions(getProject());

		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String sample= "" //
					+ "package test1;\n" //
					+ "import java.io.Serializable;\n" //
					+ "public class E1 implements Serializable {\n" //
					+ "}\n";
			ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);
			getProject().getProject().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

			enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID);
			enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED);

			sample= "" //
					+ "package test1;\n" //
					+ "import java.io.Serializable;\n" //
					+ "public class E1 implements Serializable {\n" //
					+ "\n" //
					+ "    " + FIELD_COMMENT + "\n" //
					+ "    private static final long serialVersionUID = 1L;\n" //
					+ "}\n";
			String expected1= sample;
			assertRefactoringResultAsExpectedIgnoreHashValue(new ICompilationUnit[] {cu1}, new String[] {expected1});
		} finally {
			JavaProjectHelper.set15CompilerOptions(fJProject1);
		}
	}

	@Test
	public void testSerialVersion02() throws Exception {

		JavaProjectHelper.set14CompilerOptions(fJProject1);

		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String sample= "" //
					+ "package test1;\n" //
					+ "import java.io.Serializable;\n" //
					+ "public class E1 implements Serializable {\n" //
					+ "    public class B1 implements Serializable {\n" //
					+ "    }\n" //
					+ "    public class B2 extends B1 {\n" //
					+ "    }\n" //
					+ "}\n";
			ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

			enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID);
			enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED);

			sample= "" //
					+ "package test1;\n" //
					+ "import java.io.Serializable;\n" //
					+ "public class E1 implements Serializable {\n" //
					+ "    " + FIELD_COMMENT + "\n" //
					+ "    private static final long serialVersionUID = 1L;\n" //
					+ "    public class B1 implements Serializable {\n" //
					+ "\n" //
					+ "        " + FIELD_COMMENT + "\n" //
					+ "        private static final long serialVersionUID = 1L;\n" //
					+ "    }\n" //
					+ "    public class B2 extends B1 {\n" //
					+ "\n" //
					+ "        " + FIELD_COMMENT + "\n" //
					+ "        private static final long serialVersionUID = 1L;\n" //
					+ "    }\n" //
					+ "}\n";
			String expected1= sample;
			assertRefactoringResultAsExpectedIgnoreHashValue(new ICompilationUnit[] {cu1}, new String[] {expected1});
		} finally {
			JavaProjectHelper.set15CompilerOptions(getProject());
		}
	}

	@Test
	public void testSerialVersion03() throws Exception {

		JavaProjectHelper.set14CompilerOptions(getProject());

		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String sample= "" //
					+ "package test1;\n" //
					+ "import java.io.Serializable;\n" //
					+ "public class E1 implements Serializable {\n" //
					+ "}\n";
			ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);
			sample= "" //
					+ "package test1;\n" //
					+ "import java.io.Externalizable;\n" //
					+ "public class E2 implements Externalizable {\n" //
					+ "}\n";
			ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample, false, null);

			enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID);
			enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED);

			sample= "" //
					+ "package test1;\n" //
					+ "import java.io.Serializable;\n" //
					+ "public class E1 implements Serializable {\n" //
					+ "\n" //
					+ "    " + FIELD_COMMENT + "\n" //
					+ "    private static final long serialVersionUID = 1L;\n" //
					+ "}\n";
			String expected2= sample;
			sample= "" //
					+ "package test1;\n" //
					+ "import java.io.Externalizable;\n" //
					+ "public class E2 implements Externalizable {\n" //
					+ "}\n";
			String expected1= sample;

			assertRefactoringResultAsExpectedIgnoreHashValue(new ICompilationUnit[] {cu1, cu2}, new String[] {expected1, expected2});
		} finally {
			JavaProjectHelper.set15CompilerOptions(getProject());
		}
	}

	@Test
	public void testSerialVersion04() throws Exception {

		JavaProjectHelper.set14CompilerOptions(getProject());

		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String sample= "" //
					+ "package test1;\n" //
					+ "import java.io.Serializable;\n" //
					+ "public class E1 implements Serializable {\n" //
					+ "    public void foo() {\n" //
					+ "        Serializable s= new Serializable() {\n" //
					+ "        };\n" //
					+ "    }\n" //
					+ "}\n";
			ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

			enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID);
			enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED);

			sample= "" //
					+ "package test1;\n" //
					+ "import java.io.Serializable;\n" //
					+ "public class E1 implements Serializable {\n" //
					+ "    " + FIELD_COMMENT + "\n" //
					+ "    private static final long serialVersionUID = 1L;\n" //
					+ "\n" //
					+ "    public void foo() {\n" //
					+ "        Serializable s= new Serializable() {\n" //
					+ "\n" //
					+ "            " + FIELD_COMMENT + "\n" //
					+ "            private static final long serialVersionUID = 1L;\n" //
					+ "        };\n" //
					+ "    }\n" //
					+ "}\n";
			String expected1= sample;
			assertRefactoringResultAsExpectedIgnoreHashValue(new ICompilationUnit[] {cu1}, new String[] {expected1});
		} finally {
			JavaProjectHelper.set15CompilerOptions(getProject());
		}
	}

	@Test
	public void testSerialVersion05() throws Exception {

		JavaProjectHelper.set14CompilerOptions(getProject());

		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String sample= "" //
					+ "package test1;\n" //
					+ "import java.io.Serializable;\n" //
					+ "public class E1 implements Serializable {\n" //
					+ "\n" //
					+ "    private Serializable s= new Serializable() {\n" //
					+ "        \n" //
					+ "    };\n" //
					+ "}\n";
			ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

			enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID);
			enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED);

			sample= "" //
					+ "package test1;\n" //
					+ "import java.io.Serializable;\n" //
					+ "public class E1 implements Serializable {\n" //
					+ "\n" //
					+ "    " + FIELD_COMMENT + "\n" //
					+ "    private static final long serialVersionUID = 1L;\n" //
					+ "    private Serializable s= new Serializable() {\n" //
					+ "\n" //
					+ "        " + FIELD_COMMENT + "\n" //
					+ "        private static final long serialVersionUID = 1L;\n" //
					+ "        \n" //
					+ "    };\n" //
					+ "}\n";
			String expected1= sample;
			assertRefactoringResultAsExpectedIgnoreHashValue(new ICompilationUnit[] {cu1}, new String[] {expected1});
		} finally {
			JavaProjectHelper.set15CompilerOptions(getProject());
		}
	}

	@Test
	public void testKeepCommentOnReplacement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean[] refactorBooleanArray() {\n" //
				+ "        boolean[] array = new boolean[10];\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        for (int i = 0; i < array.length; i++) {\n" //
				+ "            array[i] = true;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return array;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.ARRAYS_FILL);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Arrays;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean[] refactorBooleanArray() {\n" //
				+ "        boolean[] array = new boolean[10];\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        Arrays.fill(array, true);\n" //
				+ "\n" //
				+ "        return array;\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testKeepCommentOnRemoval() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    class A {\n" //
				+ "        A(int a) {}\n" //
				+ "\n" //
				+ "        A() {\n" //
				+ "            // Keep this comment\n" //
				+ "            super();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REDUNDANT_SUPER_CALL);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    class A {\n" //
				+ "        A(int a) {}\n" //
				+ "\n" //
				+ "        A() {\n" //
				+ "            // Keep this comment\n" //
				+ "            \n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		assertGroupCategoryUsed(new ICompilationUnit[] { cu1 }, new String[] { MultiFixMessages.RedundantSuperCallCleanup_description });
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testUseArraysFill() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    private static final boolean CONSTANT = true;\n" //
				+ "    private boolean[] booleanArray = new boolean[10];\n" //
				+ "\n" //
				+ "    public boolean[] refactorBooleanArray() {\n" //
				+ "        boolean[] array = new boolean[10];\n" //
				+ "\n" //
				+ "        for (int i = 0; i < array.length; i++) {\n" //
				+ "            array[i] = true;\n" //
				+ "        }\n" //
				+ "        for (int i = 0; i < array.length; ++i) {\n" //
				+ "            array[i] = false;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return array;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int[] refactorNumberArray() {\n" //
				+ "        int[] array = new int[10];\n" //
				+ "\n" //
				+ "        for (int i = 0; i < array.length; i++) {\n" //
				+ "            array[i] = 123;\n" //
				+ "        }\n" //
				+ "        for (int i = 0; i < array.length; i++) {\n" //
				+ "            array[i] = Integer.MAX_VALUE;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return array;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public char[] refactorCharacterArray() {\n" //
				+ "        char[] array = new char[10];\n" //
				+ "\n" //
				+ "        for (int i = 0; i < array.length; i++) {\n" //
				+ "            array[i] = '!';\n" //
				+ "        }\n" //
				+ "        for (int j = 0; array.length > j; j++) {\n" //
				+ "            array[j] = 'z';\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return array;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String[] refactorStringArray() {\n" //
				+ "        String[] array = new String[10];\n" //
				+ "\n" //
				+ "        for (int i = 0; i < array.length; i++) {\n" //
				+ "            array[i] = \"foo\";\n" //
				+ "        }\n" //
				+ "        for (int j = 0; array.length > j; j++) {\n" //
				+ "            array[j] = null;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return array;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String[] refactorStringArrayWithLocalVar(String s) {\n" //
				+ "        String[] array = new String[10];\n" //
				+ "\n" //
				+ "        String var = \"foo\";\n" //
				+ "        for (int i = 0; i < array.length; i++) {\n" //
				+ "            array[i] = var;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        for (int i = 0; i < array.length; i++) {\n" //
				+ "            array[i] = s;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return array;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String[] refactorArrayWithFinalField() {\n" //
				+ "        Boolean[] array = new Boolean[10];\n" //
				+ "\n" //
				+ "        for (int i = 0; i < array.length; i++) {\n" //
				+ "            array[i] = CONSTANT;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return array;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String[] refactorBackwardLoopOnArrary() {\n" //
				+ "        String[] array = new String[10];\n" //
				+ "\n" //
				+ "        for (int i = array.length - 1; i >= 0; i--) {\n" //
				+ "            array[i] = \"foo\";\n" //
				+ "        }\n" //
				+ "        for (int i = array.length - 1; 0 <= i; --i) {\n" //
				+ "            array[i] = \"foo\";\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return array;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void refactorExternalArray() {\n" //
				+ "        for (int i = 0; i < booleanArray.length; i++) {\n" //
				+ "            booleanArray[i] = true;\n" //
				+ "        }\n" //
				+ "        for (int i = 0; i < this.booleanArray.length; i++) {\n" //
				+ "            this.booleanArray[i] = false;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.ARRAYS_FILL);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Arrays;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    private static final boolean CONSTANT = true;\n" //
				+ "    private boolean[] booleanArray = new boolean[10];\n" //
				+ "\n" //
				+ "    public boolean[] refactorBooleanArray() {\n" //
				+ "        boolean[] array = new boolean[10];\n" //
				+ "\n" //
				+ "        Arrays.fill(array, true);\n" //
				+ "        Arrays.fill(array, false);\n" //
				+ "\n" //
				+ "        return array;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int[] refactorNumberArray() {\n" //
				+ "        int[] array = new int[10];\n" //
				+ "\n" //
				+ "        Arrays.fill(array, 123);\n" //
				+ "        Arrays.fill(array, Integer.MAX_VALUE);\n" //
				+ "\n" //
				+ "        return array;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public char[] refactorCharacterArray() {\n" //
				+ "        char[] array = new char[10];\n" //
				+ "\n" //
				+ "        Arrays.fill(array, '!');\n" //
				+ "        Arrays.fill(array, 'z');\n" //
				+ "\n" //
				+ "        return array;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String[] refactorStringArray() {\n" //
				+ "        String[] array = new String[10];\n" //
				+ "\n" //
				+ "        Arrays.fill(array, \"foo\");\n" //
				+ "        Arrays.fill(array, null);\n" //
				+ "\n" //
				+ "        return array;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String[] refactorStringArrayWithLocalVar(String s) {\n" //
				+ "        String[] array = new String[10];\n" //
				+ "\n" //
				+ "        String var = \"foo\";\n" //
				+ "        Arrays.fill(array, var);\n" //
				+ "\n" //
				+ "        Arrays.fill(array, s);\n" //
				+ "\n" //
				+ "        return array;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String[] refactorArrayWithFinalField() {\n" //
				+ "        Boolean[] array = new Boolean[10];\n" //
				+ "\n" //
				+ "        Arrays.fill(array, CONSTANT);\n" //
				+ "\n" //
				+ "        return array;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String[] refactorBackwardLoopOnArrary() {\n" //
				+ "        String[] array = new String[10];\n" //
				+ "\n" //
				+ "        Arrays.fill(array, \"foo\");\n" //
				+ "        Arrays.fill(array, \"foo\");\n" //
				+ "\n" //
				+ "        return array;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void refactorExternalArray() {\n" //
				+ "        Arrays.fill(booleanArray, true);\n" //
				+ "        Arrays.fill(this.booleanArray, false);\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testDoNotUseArraysFill() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public int field = 4;\n" //
				+ "    private static int changingValue = 0;\n" //
				+ "    private final int CONSTANT = changingValue++;\n" //
				+ "    private E1[] arrayOfE1 = null;\n" //
				+ "\n" //
				+ "    public boolean[] doNotReplaceNonForEachLoop() {\n" //
				+ "        boolean[] array = new boolean[10];\n" //
				+ "\n" //
				+ "        for (int i = 1; i < array.length; i++) {\n" //
				+ "            array[i] = true;\n" //
				+ "        }\n" //
				+ "        for (int i = 0; i < array.length - 1; i++) {\n" //
				+ "            array[i] = false;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return array;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public boolean[] doNotReplaceWierdLoop(int k) {\n" //
				+ "        boolean[] array = new boolean[10];\n" //
				+ "\n" //
				+ "        for (int m = 0; k++ < array.length; m++) {\n" //
				+ "            array[m] = true;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return array;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int[] doNotRefactorInitWithoutConstant(int n) {\n" //
				+ "        int[] array = new int[10];\n" //
				+ "\n" //
				+ "        for (int p = 0; p < array.length; p++) {\n" //
				+ "            array[p] = p*p;\n" //
				+ "        }\n" //
				+ "        for (int p = array.length - 1; p >= 0; p--) {\n" //
				+ "            array[p] = n++;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return array;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int[] doNotRefactorInitWithIndexVarOrNonFinalField(int q) {\n" //
				+ "        int[] array = new int[10];\n" //
				+ "\n" //
				+ "        for (int r = 0; r < array.length; r++) {\n" //
				+ "            array[r] = r;\n" //
				+ "        }\n" //
				+ "        for (int r = 0; r < array.length; r++) {\n" //
				+ "            array[r] = field;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return array;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int[] doNotRefactorCodeThatUsesIndex() {\n" //
				+ "        int[] array = new int[10];\n" //
				+ "\n" //
				+ "        for (int s = 0; s < array.length; s++) {\n" //
				+ "            array[s] = arrayOfE1[s].CONSTANT;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return array;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int[] doNotRefactorWithAnotherStatement() {\n" //
				+ "        int[] array = new int[10];\n" //
				+ "\n" //
				+ "        for (int i = 0; i < array.length; i++) {\n" //
				+ "            array[i] = 123;\n" //
				+ "            System.out.println(\"Do not forget me!\");\n" //
				+ "        }\n" //
				+ "        for (int i = array.length - 1; i >= 0; i--) {\n" //
				+ "            System.out.println(\"Do not forget me!\");\n" //
				+ "            array[i] = 123;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return array;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int[] doNotRefactorWithSpecificIndex() {\n" //
				+ "        int[] array = new int[10];\n" //
				+ "\n" //
				+ "        for (int i = 0; i < array.length; i++) {\n" //
				+ "            array[0] = 123;\n" //
				+ "        }\n" //
				+ "        for (int i = 0; i < array.length; i++) {\n" //
				+ "            array[array.length - i] = 123;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return array;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int[] doNotRefactorAnotherArray(int[] array3) {\n" //
				+ "        int[] array = new int[10];\n" //
				+ "        int[] array2 = new int[10];\n" //
				+ "\n" //
				+ "        for (int i = 0; i < array.length; i++) {\n" //
				+ "            array2[i] = 123;\n" //
				+ "        }\n" //
				+ "        for (int i = 0; i < array.length; i++) {\n" //
				+ "            array3[i] = 123;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return array;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int[] doNotRefactorSpecialAssignment(int[] array) {\n" //
				+ "        for (int i = 0; i < array.length; i++) {\n" //
				+ "            array[i] += 123;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return array;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.ARRAYS_FILL);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testUseLazyLogicalOperator() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.List;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    private static int staticField = 0;\n" //
				+ "\n" //
				+ "    public void replaceOperatorWithPrimitiveTypes(boolean b1, boolean b2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean newBoolean1 = b1 & b2;\n" //
				+ "        boolean newBoolean2 = b1 | b2;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceOperatorWithExtendedOperands(boolean b1, boolean b2, boolean b3) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean newBoolean1 = b1 & b2 & b3;\n" //
				+ "        boolean newBoolean2 = b1 | b2 | b3;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceOperatorWithWrappers(Boolean b1, Boolean b2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean newBoolean1 = b1 & b2;\n" //
				+ "        boolean newBoolean2 = b1 | b2;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotReplaceOperatorWithIntegers(int i1, int i2) {\n" //
				+ "        int newInteger1 = i1 & i2;\n" //
				+ "        int newInteger2 = i1 | i2;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceOperatorWithExpressions(int i1, int i2, int i3, int i4) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean newBoolean1 = (i1 == i2) & (i3 != i4);\n" //
				+ "        boolean newBoolean2 = (i1 == i2) | (i3 != i4);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceOperatorWithUnparentherizedExpressions(int i1, int i2, int i3, int i4, int i5, int i6) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean newBoolean1 = i1 == i2 & i3 != i4 & i5 != i6;\n" //
				+ "        boolean newBoolean2 = i1 == i2 | i3 != i4 | i5 != i6;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotReplaceOperatorWithMethods(List<String> myList) {\n" //
				+ "        boolean newBoolean1 = myList.remove(\"lorem\") & myList.remove(\"ipsum\");\n" //
				+ "        boolean newBoolean2 = myList.remove(\"lorem\") | myList.remove(\"ipsum\");\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotReplaceOperatorWithArrayAccess() {\n" //
				+ "        boolean[] booleans = new boolean[] {true, true};\n" //
				+ "        boolean newBoolean1 = booleans[0] & booleans[1] & booleans[2];\n" //
				+ "        boolean newBoolean2 = booleans[0] | booleans[1] | booleans[2];\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotReplaceOperatorWithDivision(int i1, int i2) {\n" //
				+ "        boolean newBoolean1 = (i1 == 123) & ((10 / i1) == i2);\n" //
				+ "        boolean newBoolean2 = (i1 == 123) | ((10 / i1) == i2);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceOperatorWithMethodOnLeftOperand(List<String> myList, boolean b1, boolean b2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean newBoolean1 = myList.remove(\"lorem\") & b1 & b2;\n" //
				+ "        boolean newBoolean2 = myList.remove(\"lorem\") | b1 | b2;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotReplaceOperatorWithIncrements(int i1, int i2, int i3, int i4) {\n" //
				+ "        boolean newBoolean1 = (i1 == i2) & (i3 != i4++);\n" //
				+ "        boolean newBoolean2 = (i1 == i2) & (i3 != ++i4);\n" //
				+ "        boolean newBoolean3 = (i1 == i2) & (i3 != i4--);\n" //
				+ "        boolean newBoolean4 = (i1 == i2) & (i3 != --i4);\n" //
				+ "\n" //
				+ "        boolean newBoolean5 = (i1 == i2) | (i3 != i4++);\n" //
				+ "        boolean newBoolean6 = (i1 == i2) | (i3 != ++i4);\n" //
				+ "        boolean newBoolean7 = (i1 == i2) | (i3 != i4--);\n" //
				+ "        boolean newBoolean8 = (i1 == i2) | (i3 != --i4);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotReplaceOperatorWithAssignments(int i1, int i2, boolean b1, boolean b2) {\n" //
				+ "        boolean newBoolean1 = (i1 == i2) & (b1 = b2);\n" //
				+ "        boolean newBoolean2 = (i1 == i2) | (b1 = b2);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private class SideEffect {\n" //
				+ "        private SideEffect() {\n" //
				+ "            staticField++;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotReplaceOperatorWithInstanciations(Boolean b1) {\n" //
				+ "        boolean newBoolean1 = b1 & new SideEffect() instanceof SideEffect;\n" //
				+ "        boolean newBoolean2 = b1 | new SideEffect() instanceof SideEffect;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_LAZY_LOGICAL_OPERATOR);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.List;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    private static int staticField = 0;\n" //
				+ "\n" //
				+ "    public void replaceOperatorWithPrimitiveTypes(boolean b1, boolean b2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean newBoolean1 = b1 && b2;\n" //
				+ "        boolean newBoolean2 = b1 || b2;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceOperatorWithExtendedOperands(boolean b1, boolean b2, boolean b3) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean newBoolean1 = b1 && b2 && b3;\n" //
				+ "        boolean newBoolean2 = b1 || b2 || b3;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceOperatorWithWrappers(Boolean b1, Boolean b2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean newBoolean1 = b1 && b2;\n" //
				+ "        boolean newBoolean2 = b1 || b2;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotReplaceOperatorWithIntegers(int i1, int i2) {\n" //
				+ "        int newInteger1 = i1 & i2;\n" //
				+ "        int newInteger2 = i1 | i2;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceOperatorWithExpressions(int i1, int i2, int i3, int i4) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean newBoolean1 = (i1 == i2) && (i3 != i4);\n" //
				+ "        boolean newBoolean2 = (i1 == i2) || (i3 != i4);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceOperatorWithUnparentherizedExpressions(int i1, int i2, int i3, int i4, int i5, int i6) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean newBoolean1 = i1 == i2 && i3 != i4 && i5 != i6;\n" //
				+ "        boolean newBoolean2 = i1 == i2 || i3 != i4 || i5 != i6;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotReplaceOperatorWithMethods(List<String> myList) {\n" //
				+ "        boolean newBoolean1 = myList.remove(\"lorem\") & myList.remove(\"ipsum\");\n" //
				+ "        boolean newBoolean2 = myList.remove(\"lorem\") | myList.remove(\"ipsum\");\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotReplaceOperatorWithArrayAccess() {\n" //
				+ "        boolean[] booleans = new boolean[] {true, true};\n" //
				+ "        boolean newBoolean1 = booleans[0] & booleans[1] & booleans[2];\n" //
				+ "        boolean newBoolean2 = booleans[0] | booleans[1] | booleans[2];\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotReplaceOperatorWithDivision(int i1, int i2) {\n" //
				+ "        boolean newBoolean1 = (i1 == 123) & ((10 / i1) == i2);\n" //
				+ "        boolean newBoolean2 = (i1 == 123) | ((10 / i1) == i2);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceOperatorWithMethodOnLeftOperand(List<String> myList, boolean b1, boolean b2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        boolean newBoolean1 = myList.remove(\"lorem\") && b1 && b2;\n" //
				+ "        boolean newBoolean2 = myList.remove(\"lorem\") || b1 || b2;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotReplaceOperatorWithIncrements(int i1, int i2, int i3, int i4) {\n" //
				+ "        boolean newBoolean1 = (i1 == i2) & (i3 != i4++);\n" //
				+ "        boolean newBoolean2 = (i1 == i2) & (i3 != ++i4);\n" //
				+ "        boolean newBoolean3 = (i1 == i2) & (i3 != i4--);\n" //
				+ "        boolean newBoolean4 = (i1 == i2) & (i3 != --i4);\n" //
				+ "\n" //
				+ "        boolean newBoolean5 = (i1 == i2) | (i3 != i4++);\n" //
				+ "        boolean newBoolean6 = (i1 == i2) | (i3 != ++i4);\n" //
				+ "        boolean newBoolean7 = (i1 == i2) | (i3 != i4--);\n" //
				+ "        boolean newBoolean8 = (i1 == i2) | (i3 != --i4);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotReplaceOperatorWithAssignments(int i1, int i2, boolean b1, boolean b2) {\n" //
				+ "        boolean newBoolean1 = (i1 == i2) & (b1 = b2);\n" //
				+ "        boolean newBoolean2 = (i1 == i2) | (b1 = b2);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private class SideEffect {\n" //
				+ "        private SideEffect() {\n" //
				+ "            staticField++;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotReplaceOperatorWithInstanciations(Boolean b1) {\n" //
				+ "        boolean newBoolean1 = b1 & new SideEffect() instanceof SideEffect;\n" //
				+ "        boolean newBoolean2 = b1 | new SideEffect() instanceof SideEffect;\n" //
				+ "    }\n" //
				+ "}\n";
		assertGroupCategoryUsed(new ICompilationUnit[] { cu1 }, new String[] { MultiFixMessages.CodeStyleCleanUp_LazyLogical_description });
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testPushDownNegationReplaceDoubleNegation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void replaceDoubleNegation(boolean b) {\n" //
				+ "        boolean b1 = !!b;\n" //
				+ "        boolean b2 = !Boolean.TRUE;\n" //
				+ "        boolean b3 = !Boolean.FALSE;\n" //
				+ "        boolean b4 = !true;\n" //
				+ "        boolean b5 = !false;\n" //
				+ "        boolean b6 = !!!!b;\n" //
				+ "        boolean b7 = !!!!!Boolean.TRUE;\n" //
				+ "        boolean b8 = !!!!!!!Boolean.FALSE;\n" //
				+ "        boolean b9 = !!!!!!!!!true;\n" //
				+ "        boolean b10 = !!!false;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void replaceDoubleNegation(boolean b) {\n" //
				+ "        boolean b1 = b;\n" //
				+ "        boolean b2 = false;\n" //
				+ "        boolean b3 = true;\n" //
				+ "        boolean b4 = false;\n" //
				+ "        boolean b5 = true;\n" //
				+ "        boolean b6 = b;\n" //
				+ "        boolean b7 = false;\n" //
				+ "        boolean b8 = true;\n" //
				+ "        boolean b9 = false;\n" //
				+ "        boolean b10 = true;\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testPushDownNegationReplaceDoubleNegationWithParentheses() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceDoubleNegationWithParentheses(boolean b) {\n" //
				+ "        return !!!(!(b /* another refactoring removes the parentheses */));\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceDoubleNegationWithParentheses(boolean b) {\n" //
				+ "        return (b /* another refactoring removes the parentheses */);\n" //
				+ "    }\n" //
				+ "}\n";

		assertGroupCategoryUsed(new ICompilationUnit[] { cu1 }, new String[] { MultiFixMessages.PushDownNegationCleanup_description });
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testPushDownNegationReplaceNegationWithInfixAndOperator() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationWithInfixAndOperator(boolean b1, boolean b2, boolean b3) {\n" //
				+ "        return !(b1 && b2 && b3); // another refactoring removes the parentheses\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationWithInfixAndOperator(boolean b1, boolean b2, boolean b3) {\n" //
				+ "        return (!b1 || !b2 || !b3); // another refactoring removes the parentheses\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testPushDownNegationReplaceNegationWithInfixOrOperator() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationWithInfixOrOperator(boolean b1, boolean b2, boolean b3) {\n" //
				+ "        return !(b1 || b2 || b3); // another refactoring removes the parentheses\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationWithInfixOrOperator(boolean b1, boolean b2, boolean b3) {\n" //
				+ "        return (!b1 && !b2 && !b3); // another refactoring removes the parentheses\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testPushDownNegationReplaceInstanceofNegationWithInfixAndOperator() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationWithInfixAndOperator(boolean b1, boolean b2) {\n" //
				+ "        return !(b1 && b2 instanceof String); // another refactoring removes the parentheses\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationWithInfixAndOperator(boolean b1, boolean b2) {\n" //
				+ "        return (!b1 || !(b2 instanceof String)); // another refactoring removes the parentheses\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testPushDownNegationReplaceInstanceofNegationWithInfixOrOperator() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationWithInfixOrOperator(boolean b1, boolean b2) {\n" //
				+ "        return !(b1 instanceof String || b2); // another refactoring removes the parentheses\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationWithInfixOrOperator(boolean b1, boolean b2) {\n" //
				+ "        return (!(b1 instanceof String) && !b2); // another refactoring removes the parentheses\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testPushDownNegationReplaceNegationWithEqualOperator() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationWithEqualOperator(boolean b1, boolean b2) {\n" //
				+ "        return !(b1 == b2); // another refactoring removes the parentheses\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationWithEqualOperator(boolean b1, boolean b2) {\n" //
				+ "        return (b1 != b2); // another refactoring removes the parentheses\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testPushDownNegationReplaceNegationWithNotEqualOperator() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationWithNotEqualOperator(boolean b1, boolean b2) {\n" //
				+ "        return !(b1 != b2); // another refactoring removes the parentheses\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationWithNotEqualOperator(boolean b1, boolean b2) {\n" //
				+ "        return (b1 == b2); // another refactoring removes the parentheses\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testPushDownNegationReplaceNegationRevertInnerExpressions() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationRevertInnerExpressions(boolean b1, boolean b2) {\n" //
				+ "        return !(!b1 && !b2 /* another refactoring removes the parentheses */);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationRevertInnerExpressions(boolean b1, boolean b2) {\n" //
				+ "        return (b1 || b2 /* another refactoring removes the parentheses */);\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testPushDownNegationReplaceNegationLeaveParentheses() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationLeaveParentheses(boolean b1, boolean b2) {\n" //
				+ "        return !(!(b1 && b2 /* another refactoring removes the parentheses */));\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationLeaveParentheses(boolean b1, boolean b2) {\n" //
				+ "        return (b1 && b2 /* another refactoring removes the parentheses */);\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testPushDownNegationReplaceNegationRemoveParentheses() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationRemoveParentheses(boolean b1, boolean b2) {\n" //
				+ "        return !((!b1) && (!b2));\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationRemoveParentheses(boolean b1, boolean b2) {\n" //
				+ "        return (b1 || b2);\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testPushDownNegationReplaceNegateNonBooleanExprs() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegateNonBooleanExprs(Object o) {\n" //
				+ "        return !(o != null /* another refactoring removes the parentheses */);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegateNonBooleanExprs(Object o) {\n" //
				+ "        return (o == null /* another refactoring removes the parentheses */);\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testPushDownNegationReplaceNegateNonBooleanPrimitiveExprs() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegateNonBooleanPrimitiveExprs(Boolean b) {\n" //
				+ "        return !(b != null /* another refactoring removes the parentheses */);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegateNonBooleanPrimitiveExprs(Boolean b) {\n" //
				+ "        return (b == null /* another refactoring removes the parentheses */);\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testPushDownNegationReplaceNegationAndLessOperator() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationAndLessOperator(int i1, int i2) {\n" //
				+ "        return !(i1 < i2 /* another refactoring removes the parentheses */);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationAndLessOperator(int i1, int i2) {\n" //
				+ "        return (i1 >= i2 /* another refactoring removes the parentheses */);\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testPushDownNegationReplaceNegationAndLessEqualOperator() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationAndLessEqualOperator(int i1, int i2) {\n" //
				+ "        return !(i1 <= i2 /* another refactoring removes the parentheses */);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationAndLessEqualOperator(int i1, int i2) {\n" //
				+ "        return (i1 > i2 /* another refactoring removes the parentheses */);\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testPushDownNegationReplaceNegationAndGreaterOperator() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationAndGreaterOperator(int i1, int i2) {\n" //
				+ "        return !(i1 > i2 /* another refactoring removes the parentheses */);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationAndGreaterOperator(int i1, int i2) {\n" //
				+ "        return (i1 <= i2 /* another refactoring removes the parentheses */);\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testPushDownNegationReplaceNegationAndGreaterEqualOperator() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationAndGreaterEqualOperator(int i1, int i2) {\n" //
				+ "        return !(i1 >= i2 /* another refactoring removes the parentheses */);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationAndGreaterEqualOperator(int i1, int i2) {\n" //
				+ "        return (i1 < i2 /* another refactoring removes the parentheses */);\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testPushDownNegationReplaceNegationAndEqualOperator() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationAndEqualOperator(int i1, int i2) {\n" //
				+ "        return !(i1 == i2 /* another refactoring removes the parentheses */);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationAndEqualOperator(int i1, int i2) {\n" //
				+ "        return (i1 != i2 /* another refactoring removes the parentheses */);\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testPushDownNegationReplaceNegationAndNotEqualOperator() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationAndNotEqualOperator(int i1, int i2) {\n" //
				+ "        return !(i1 != i2 /* another refactoring removes the parentheses */);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PUSH_DOWN_NEGATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean replaceNegationAndNotEqualOperator(int i1, int i2) {\n" //
				+ "        return (i1 == i2 /* another refactoring removes the parentheses */);\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testRedundantComparisonStatement() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String input= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Date;\n" //
				+ "import java.util.List;\n" //
				+ "import java.util.Map;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    private static final String DEFAULT = \"\";\n" //
				+ "    private String input;\n" //
				+ "\n" //
				+ "    public String refactorLocalVariable1(String input) {\n" //
				+ "        String output;\n" //
				+ "        // Keep this comment\n" //
				+ "        if (input == null) {\n" //
				+ "            output = null;\n" //
				+ "        } else {\n" //
				+ "            output = /* Keep this comment too */ input;\n" //
				+ "        }\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorLocalVariable2(String input) {\n" //
				+ "        String output;\n" //
				+ "        // Keep this comment\n" //
				+ "        if (null == input) {\n" //
				+ "            output = null;\n" //
				+ "        } else {\n" //
				+ "            output = input;\n" //
				+ "        }\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorLocalVariable3(String input) {\n" //
				+ "        String output;\n" //
				+ "        // Keep this comment\n" //
				+ "        if (input != null) {\n" //
				+ "            output = input;\n" //
				+ "        } else {\n" //
				+ "            output = null;\n" //
				+ "        }\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorLocalVariable4(String input) {\n" //
				+ "        String output;\n" //
				+ "        // Keep this comment\n" //
				+ "        if (null != input) {\n" //
				+ "            output = input;\n" //
				+ "        } else {\n" //
				+ "            output = null;\n" //
				+ "        }\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int removeHardCodedNumber(int input) {\n" //
				+ "        int output;\n" //
				+ "        // Keep this comment\n" //
				+ "        if (123 != input) {\n" //
				+ "            output = input;\n" //
				+ "        } else {\n" //
				+ "            output = 123;\n" //
				+ "        }\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public char removeHardCodedCharacter(char input) {\n" //
				+ "        char output;\n" //
				+ "        // Keep this comment\n" //
				+ "        if (input == 'a') {\n" //
				+ "            output = 'a';\n" //
				+ "        } else {\n" //
				+ "            output = input;\n" //
				+ "        }\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int removeHardCodedExpression(int input) {\n" //
				+ "        int output;\n" //
				+ "        // Keep this comment\n" //
				+ "        if (input != 1 + 2 + 3) {\n" //
				+ "            output = input;\n" //
				+ "        } else {\n" //
				+ "            output = 3 + 2 + 1;\n" //
				+ "        }\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorLocalVariable5(String input, boolean isValid) {\n" //
				+ "        String output = null;\n" //
				+ "        if (isValid)\n" //
				+ "            if (input != null) {\n" //
				+ "                output = input;\n" //
				+ "            } else {\n" //
				+ "                output = null;\n" //
				+ "            }\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void refactorFieldAssign1(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (input == null) {\n" //
				+ "            this.input = null;\n" //
				+ "        } else {\n" //
				+ "            this.input = input;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void refactorFieldAssign2(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (null == input) {\n" //
				+ "            this.input = null;\n" //
				+ "        } else {\n" //
				+ "            this.input = input;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void refactorFieldAssign3(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (input != null) {\n" //
				+ "            this.input = input;\n" //
				+ "        } else {\n" //
				+ "            this.input = null;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void refactorFieldAssign4(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (null != input) {\n" //
				+ "            this.input = input;\n" //
				+ "        } else {\n" //
				+ "            this.input = null;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorReturn1(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (input == null) {\n" //
				+ "            return null;\n" //
				+ "        } else {\n" //
				+ "            return /* Keep this comment too */ input;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorReturn2(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (null == input) {\n" //
				+ "            return null;\n" //
				+ "        } else {\n" //
				+ "            return input;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorReturn3(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (input != null) {\n" //
				+ "            return input;\n" //
				+ "        } else {\n" //
				+ "            return null;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorReturn4(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (null != input) {\n" //
				+ "            return input;\n" //
				+ "        } else {\n" //
				+ "            return null;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorReturnNoElse1(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (input == null) {\n" //
				+ "            return null;\n" //
				+ "        }\n" //
				+ "        return input;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorReturnNoElse2(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (null == input) {\n" //
				+ "            return null;\n" //
				+ "        }\n" //
				+ "        return input;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorReturnNoElse3(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (input != null) {\n" //
				+ "            return input;\n" //
				+ "        }\n" //
				+ "        return null;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorReturnNoElse4(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (null != input) {\n" //
				+ "            return input;\n" //
				+ "        }\n" //
				+ "        return null;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConstant1(String input) {\n" //
				+ "        String output;\n" //
				+ "        // Keep this comment\n" //
				+ "        if (input != null) {\n" //
				+ "            output = /* Keep this comment too */ null;\n" //
				+ "        } else {\n" //
				+ "            output = input;\n" //
				+ "        }\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConstant2(String input) {\n" //
				+ "        String output;\n" //
				+ "        // Keep this comment\n" //
				+ "        if (null != input) {\n" //
				+ "            output = null;\n" //
				+ "        } else {\n" //
				+ "            output = input;\n" //
				+ "        }\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConstant3(String input) {\n" //
				+ "        String output;\n" //
				+ "        // Keep this comment\n" //
				+ "        if (input == null) {\n" //
				+ "            output = input;\n" //
				+ "        } else {\n" //
				+ "            output = null;\n" //
				+ "        }\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConstant4(String input) {\n" //
				+ "        String output;\n" //
				+ "        // Keep this comment\n" //
				+ "        if (null == input) {\n" //
				+ "            output = input;\n" //
				+ "        } else {\n" //
				+ "            output = null;\n" //
				+ "        }\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConstantReturn1(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (input != null) {\n" //
				+ "            return /* Keep this comment too */ null;\n" //
				+ "        } else {\n" //
				+ "            return input;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConstantReturn2(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (null != input) {\n" //
				+ "            return null;\n" //
				+ "        } else {\n" //
				+ "            return input;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConstantReturn3(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (input == null) {\n" //
				+ "            return input;\n" //
				+ "        } else {\n" //
				+ "            return null;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConstantReturn4(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (null == input) {\n" //
				+ "            return input;\n" //
				+ "        } else {\n" //
				+ "            return null;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConstantReturnNoElse1(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (input != null) {\n" //
				+ "            return null;\n" //
				+ "        }\n" //
				+ "        return input;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConstantReturnNoElse2(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (null != input) {\n" //
				+ "            return null;\n" //
				+ "        }\n" //
				+ "        return input;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConstantReturnNoElse3(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (input == null) {\n" //
				+ "            return input;\n" //
				+ "        }\n" //
				+ "        return null;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConstantReturnNoElse4(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (null == input) {\n" //
				+ "            return input;\n" //
				+ "        }\n" //
				+ "        return null;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", input, false, null);

		enable(CleanUpConstants.REMOVE_REDUNDANT_COMPARISON_STATEMENT);

		String output= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Date;\n" //
				+ "import java.util.List;\n" //
				+ "import java.util.Map;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    private static final String DEFAULT = \"\";\n" //
				+ "    private String input;\n" //
				+ "\n" //
				+ "    public String refactorLocalVariable1(String input) {\n" //
				+ "        String output;\n" //
				+ "        // Keep this comment\n" //
				+ "        output = /* Keep this comment too */ input;\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorLocalVariable2(String input) {\n" //
				+ "        String output;\n" //
				+ "        // Keep this comment\n" //
				+ "        output = input;\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorLocalVariable3(String input) {\n" //
				+ "        String output;\n" //
				+ "        // Keep this comment\n" //
				+ "        output = input;\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorLocalVariable4(String input) {\n" //
				+ "        String output;\n" //
				+ "        // Keep this comment\n" //
				+ "        output = input;\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int removeHardCodedNumber(int input) {\n" //
				+ "        int output;\n" //
				+ "        // Keep this comment\n" //
				+ "        output = input;\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public char removeHardCodedCharacter(char input) {\n" //
				+ "        char output;\n" //
				+ "        // Keep this comment\n" //
				+ "        output = input;\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int removeHardCodedExpression(int input) {\n" //
				+ "        int output;\n" //
				+ "        // Keep this comment\n" //
				+ "        output = input;\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorLocalVariable5(String input, boolean isValid) {\n" //
				+ "        String output = null;\n" //
				+ "        if (isValid)\n" //
				+ "            output = input;\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void refactorFieldAssign1(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        this.input = input;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void refactorFieldAssign2(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        this.input = input;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void refactorFieldAssign3(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        this.input = input;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void refactorFieldAssign4(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        this.input = input;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorReturn1(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return /* Keep this comment too */ input;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorReturn2(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return input;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorReturn3(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return input;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorReturn4(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return input;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorReturnNoElse1(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return input;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorReturnNoElse2(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return input;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorReturnNoElse3(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return input;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorReturnNoElse4(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return input;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConstant1(String input) {\n" //
				+ "        String output;\n" //
				+ "        // Keep this comment\n" //
				+ "        output = /* Keep this comment too */ null;\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConstant2(String input) {\n" //
				+ "        String output;\n" //
				+ "        // Keep this comment\n" //
				+ "        output = null;\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConstant3(String input) {\n" //
				+ "        String output;\n" //
				+ "        // Keep this comment\n" //
				+ "        output = null;\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConstant4(String input) {\n" //
				+ "        String output;\n" //
				+ "        // Keep this comment\n" //
				+ "        output = null;\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConstantReturn1(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return /* Keep this comment too */ null;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConstantReturn2(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return null;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConstantReturn3(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return null;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConstantReturn4(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return null;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConstantReturnNoElse1(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return null;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConstantReturnNoElse2(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return null;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConstantReturnNoElse3(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return null;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String refactorConstantReturnNoElse4(String input) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return null;\n" //
				+ "    }\n" //
				+ "}\n";
		assertGroupCategoryUsed(new ICompilationUnit[] { cu }, new String[] { MultiFixMessages.RedundantComparisonStatementCleanup_description });
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { output });
	}

	@Test
	public void testKeepComparisonStatement() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Collection;\n" //
				+ "import java.util.Collections;\n" //
				+ "import java.util.Date;\n" //
				+ "import java.util.List;\n" //
				+ "import java.util.Map;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    private static final String DEFAULT = \"\";\n" //
				+ "    private String input;\n" //
				+ "\n" //
				+ "    public String doNotRefactorLocalVariable(String input) {\n" //
				+ "        String output;\n" //
				+ "        if (input == null) {\n" //
				+ "            output = DEFAULT;\n" //
				+ "        } else {\n" //
				+ "            output = input;\n" //
				+ "        }\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotRefactorConstant(String input) {\n" //
				+ "        String output;\n" //
				+ "        if (input != null) {\n" //
				+ "            output = DEFAULT;\n" //
				+ "        } else {\n" //
				+ "            output = input;\n" //
				+ "        }\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotRefactorActiveExpression(List<String> input) {\n" //
				+ "        String result;\n" //
				+ "        if (input.remove(0) == null) {\n" //
				+ "            result = null;\n" //
				+ "        } else {\n" //
				+ "            result = input.remove(0);\n" //
				+ "        }\n" //
				+ "        return result;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotUseConstantWithoutActiveExpression(List<String> input) {\n" //
				+ "        String result;\n" //
				+ "        if (input.remove(0) == null) {\n" //
				+ "            result = null;\n" //
				+ "        } else {\n" //
				+ "            result = input.remove(0);\n" //
				+ "        }\n" //
				+ "        return result;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotRefactorFieldAssignXXX(String input, E other) {\n" //
				+ "        if (input == null) {\n" //
				+ "            this.input = null;\n" //
				+ "        } else {\n" //
				+ "            other.input = input;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotRefactorFieldAssign(String input) {\n" //
				+ "        if (input == null) {\n" //
				+ "            this.input = DEFAULT;\n" //
				+ "        } else {\n" //
				+ "            this.input = input;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotRefactorConstantReturn(String input) {\n" //
				+ "        if (null != input) {\n" //
				+ "            return input;\n" //
				+ "        } else {\n" //
				+ "            return DEFAULT;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Collection<?> doNotRefactorDifferentReturn(Collection<?> c) {\n" //
				+ "        if (c == null) {\n" //
				+ "            return Collections.emptySet();\n" //
				+ "        } else {\n" //
				+ "            return c;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Date doNotRefactorActiveAssignment(List<Date> input) {\n" //
				+ "        Date date;\n" //
				+ "        if (input.remove(0) != null) {\n" //
				+ "            date = input.remove(0);\n" //
				+ "        } else {\n" //
				+ "            date = null;\n" //
				+ "        }\n" //
				+ "        return date;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Date doNotRefactorActiveReturn(List<Date> input) {\n" //
				+ "        if (input.remove(0) != null) {\n" //
				+ "            return input.remove(0);\n" //
				+ "        }\n" //
				+ "        return null;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_REDUNDANT_COMPARISON_STATEMENT);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testUselessSuperCall() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    class A {\n" //
				+ "        A(int a) {}\n" //
				+ "\n" //
				+ "        A() {\n" //
				+ "            super();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    class B extends A {\n" //
				+ "        B(int b) {\n" //
				+ "            super(b);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        B() {\n" //
				+ "            super();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REDUNDANT_SUPER_CALL);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    class A {\n" //
				+ "        A(int a) {}\n" //
				+ "\n" //
				+ "        A() {\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    class B extends A {\n" //
				+ "        B(int b) {\n" //
				+ "            super(b);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        B() {\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		assertGroupCategoryUsed(new ICompilationUnit[] { cu1 }, new String[] { MultiFixMessages.RedundantSuperCallCleanup_description });
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testKeepSuperCall() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    class A {\n" //
				+ "        A(int a) {}\n" //
				+ "\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    class B extends A {\n" //
				+ "        B(int b) {\n" //
				+ "            super(b);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REDUNDANT_SUPER_CALL);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testMergeConditionalBlocks() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    /** Duplicate if and else if code, merge it */\n" //
				+ "    public void duplicateIfAndElseIf(int i) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (i == 0) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "        } else if (i == 123) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "        } else {\n" //
				+ "            // Keep this comment also\n" //
				+ "            System.out.println(\"Different\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    /** Duplicate if and else if code, merge it */\n" //
				+ "    public void mergeTwoStructures(int a, int b) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (a == 0) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\" + (a + 1));\n" //
				+ "        } else if (a == 1) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\" + (1 + a));\n" //
				+ "        } else {\n" //
				+ "            // Keep this comment also\n" //
				+ "            System.out.println(\"Different\");\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        if (b == 0) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\" + (a + 123 + 0));\n" //
				+ "        } else if (b == 1) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\" + (a + 123));\n" //
				+ "        } else {\n" //
				+ "            // Keep this comment also\n" //
				+ "            System.out.println(\"Different\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    /** Duplicate if and else code, merge it */\n" //
				+ "    public void duplicateIfAndElse(int j) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (j == 0) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            if (j > 0) {\n" //
				+ "                System.out.println(\"Duplicate\");\n" //
				+ "            } else {\n" //
				+ "                System.out.println(\"Duplicate too\");\n" //
				+ "            }\n" //
				+ "        } else if (j == 1) {\n" //
				+ "            // Keep this comment also\n" //
				+ "            System.out.println(\"Different\");\n" //
				+ "        } else {\n" //
				+ "            // Keep this comment too\n" //
				+ "            if (j <= 0) {\n" //
				+ "                System.out.println(\"Duplicate too\");\n" //
				+ "            } else {\n" //
				+ "                System.out.println(\"Duplicate\");\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    /** Duplicate if and else if code, merge it */\n" //
				+ "    public void duplicateIfAndElseIfWithoutElse(int k) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (k == 0) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\" + !(k == 0));\n" //
				+ "        } else if (k == 1) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\" + (k != 0));\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    /** Duplicate else if codes, merge it */\n" //
				+ "    public void duplicateIfAndElseIfAmongOther(int m) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (m == 0) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"A given code\");\n" //
				+ "        } if (m == 1) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\" + (m > 0));\n" //
				+ "        } else if (m == 2) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\" + (0 < m));\n" //
				+ "        } else {\n" //
				+ "            // Keep this comment also\n" //
				+ "            System.out.println(\"Different\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    /** Duplicate if and else if code, merge it */\n" //
				+ "    public void duplicateSingleStatement(int n) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (n == 0)\n" //
				+ "            // Keep this comment too\n" //
				+ "            if (n > 0) {\n" //
				+ "                System.out.println(\"Duplicate\");\n" //
				+ "            } else {\n" //
				+ "                System.out.println(\"Duplicate too\");\n" //
				+ "            }\n" //
				+ "        else if (n == 1)\n" //
				+ "            // Keep this comment too\n" //
				+ "            if (n > 0)\n" //
				+ "                System.out.println(\"Duplicate\");\n" //
				+ "            else\n" //
				+ "                System.out.println(\"Duplicate too\");\n" //
				+ "        else\n" //
				+ "            // Keep this comment also\n" //
				+ "            System.out.println(\"Different\");\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    /** Duplicate if and else if code, merge it */\n" //
				+ "    public void numerousDuplicateIfAndElseIf(int o) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (o == 0) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "        } else if (o == 1) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "        } else if (o == 2)\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "        else if (o == 3) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "        } else {\n" //
				+ "            // Keep this comment also\n" //
				+ "            System.out.println(\"Different\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    /** Duplicate if and else if code, merge it */\n" //
				+ "    public void complexIfAndElseIf(int p) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (p == 0) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate \");\n" //
				+ "        } else if (p == 1 || p == 2) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate \");\n" //
				+ "        } else if (p > 10) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate \");\n" //
				+ "        } else {\n" //
				+ "            // Keep this comment also\n" //
				+ "            System.out.println(\"Different\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    /** Duplicate if and else if code, merge it */\n" //
				+ "    public void longIfAndElseIf(int q, boolean isValid) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (q == 0) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "            q = q + 1;\n" //
				+ "        } else if (isValid ? (q == 1) : (q > 1)) {\n" //
				+ "            // Keep this comment also\n" //
				+ "            System.out.println(\"Different\");\n" //
				+ "        } else {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "            q++;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MERGE_CONDITIONAL_BLOCKS);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    /** Duplicate if and else if code, merge it */\n" //
				+ "    public void duplicateIfAndElseIf(int i) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if ((i == 0) || (i == 123)) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "        } else {\n" //
				+ "            // Keep this comment also\n" //
				+ "            System.out.println(\"Different\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    /** Duplicate if and else if code, merge it */\n" //
				+ "    public void mergeTwoStructures(int a, int b) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if ((a == 0) || (a == 1)) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\" + (a + 1));\n" //
				+ "        } else {\n" //
				+ "            // Keep this comment also\n" //
				+ "            System.out.println(\"Different\");\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        if ((b == 0) || (b == 1)) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\" + (a + 123 + 0));\n" //
				+ "        } else {\n" //
				+ "            // Keep this comment also\n" //
				+ "            System.out.println(\"Different\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    /** Duplicate if and else code, merge it */\n" //
				+ "    public void duplicateIfAndElse(int j) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if ((j == 0) || (j != 1)) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            if (j > 0) {\n" //
				+ "                System.out.println(\"Duplicate\");\n" //
				+ "            } else {\n" //
				+ "                System.out.println(\"Duplicate too\");\n" //
				+ "            }\n" //
				+ "        } else {\n" //
				+ "            // Keep this comment also\n" //
				+ "            System.out.println(\"Different\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    /** Duplicate if and else if code, merge it */\n" //
				+ "    public void duplicateIfAndElseIfWithoutElse(int k) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if ((k == 0) || (k == 1)) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\" + !(k == 0));\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    /** Duplicate else if codes, merge it */\n" //
				+ "    public void duplicateIfAndElseIfAmongOther(int m) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (m == 0) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"A given code\");\n" //
				+ "        } if ((m == 1) || (m == 2)) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\" + (m > 0));\n" //
				+ "        } else {\n" //
				+ "            // Keep this comment also\n" //
				+ "            System.out.println(\"Different\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    /** Duplicate if and else if code, merge it */\n" //
				+ "    public void duplicateSingleStatement(int n) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if ((n == 0) || (n == 1))\n" //
				+ "            // Keep this comment too\n" //
				+ "            if (n > 0) {\n" //
				+ "                System.out.println(\"Duplicate\");\n" //
				+ "            } else {\n" //
				+ "                System.out.println(\"Duplicate too\");\n" //
				+ "            }\n" //
				+ "        else\n" //
				+ "            // Keep this comment also\n" //
				+ "            System.out.println(\"Different\");\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    /** Duplicate if and else if code, merge it */\n" //
				+ "    public void numerousDuplicateIfAndElseIf(int o) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if ((o == 0) || (o == 1) || (o == 2)\n" //
				+ "                || (o == 3)) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "        } else {\n" //
				+ "            // Keep this comment also\n" //
				+ "            System.out.println(\"Different\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    /** Duplicate if and else if code, merge it */\n" //
				+ "    public void complexIfAndElseIf(int p) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if ((p == 0) || (p == 1 || p == 2) || (p > 10)) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate \");\n" //
				+ "        } else {\n" //
				+ "            // Keep this comment also\n" //
				+ "            System.out.println(\"Different\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    /** Duplicate if and else if code, merge it */\n" //
				+ "    public void longIfAndElseIf(int q, boolean isValid) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if ((q == 0) || (isValid ? (q != 1) : (q <= 1))) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "            q = q + 1;\n" //
				+ "        } else {\n" //
				+ "            // Keep this comment also\n" //
				+ "            System.out.println(\"Different\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		assertGroupCategoryUsed(new ICompilationUnit[] { cu1 }, new String[] { MultiFixMessages.MergeConditionalBlocksCleanup_description });
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testDoNotMergeConditionalBlocks() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    /** 5 operands, not easily readable */\n" //
				+ "    public void doNotMergeMoreThanFourOperands(int i) {\n" //
				+ "        if ((i == 0) || (i == 1 || i == 2)) {\n" //
				+ "            System.out.println(\"Duplicate \");\n" //
				+ "        } else if (i > 10 && i < 100) {\n" //
				+ "            System.out.println(\"Duplicate \");\n" //
				+ "        } else {\n" //
				+ "            System.out.println(\"Different\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    /** Different if and else if code, leave it */\n" //
				+ "    public void doNotMergeAdditionalCode(int i) {\n" //
				+ "        if (i == 0) {\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "        } else if (i == 1) {\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "            System.out.println(\"but not only\");\n" //
				+ "        } else {\n" //
				+ "            System.out.println(\"Different\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    /** Different code in the middle, leave it */\n" //
				+ "    public void doNotMergeIntruderCode(int i) {\n" //
				+ "        if (i == 0) {\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "        } else if (i == 1) {\n" //
				+ "            System.out.println(\"Intruder\");\n" //
				+ "        } else if (i == 2) {\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "        } else {\n" //
				+ "            System.out.println(\"Different\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MERGE_CONDITIONAL_BLOCKS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testRedundantFallingThroughBlockEnd() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String input= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.List;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    private boolean b= true;\n" //
				+ "\n" //
				+ "    public void mergeIfBlocksIntoFollowingCode(int i) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (i <= 0) {\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "            return;\n" //
				+ "        } else if (i == 10) {\n" //
				+ "            System.out.println(\"Doing another thing\");\n" //
				+ "            return;\n" //
				+ "        } else if (i == 20) {\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "            return;\n" //
				+ "        }\n" //
				+ "        System.out.println(\"Doing something\");\n" //
				+ "        return;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int mergeUselessElse(int i) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (i <= 0) {\n" //
				+ "            System.out.println(\"Doing another thing\");\n" //
				+ "        } else return i + 123;\n" //
				+ "        return i + 123;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public char mergeIfStatementIntoFollowingCode(int j) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (j <= 0) return 'a';\n" //
				+ "        else if (j == 10) return 'b';\n" //
				+ "        else if (j == 20) return 'a';\n" //
				+ "        return 'a';\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void mergeEndOfIfIntoFollowingCode(int k) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (k <= 0) {\n" //
				+ "            System.out.println(\"Doing another thing\");\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "            return;\n" //
				+ "        } else if (k == 10) {\n" //
				+ "            System.out.println(\"Doing another thing\");\n" //
				+ "            return;\n" //
				+ "        } else if (k == 20) {\n" //
				+ "            System.out.println(\"Doing another thing\");\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "            return;\n" //
				+ "        }\n" //
				+ "        System.out.println(\"Doing something\");\n" //
				+ "        return;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void mergeWithoutContinue(int i, int j) {\n" //
				+ "        while (j-- > 0) {\n" //
				+ "            // Keep this comment\n" //
				+ "            if (i <= 0) {\n" //
				+ "                System.out.println(\"Doing another thing\");\n" //
				+ "                System.out.println(\"Doing something\");\n" //
				+ "                continue;\n" //
				+ "            } else if (i == 10) {\n" //
				+ "                System.out.println(\"Doing another thing\");\n" //
				+ "                continue;\n" //
				+ "            } else if (i == 20) {\n" //
				+ "                System.out.println(\"Doing another thing\");\n" //
				+ "                System.out.println(\"Doing something\");\n" //
				+ "                continue;\n" //
				+ "            }\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void mergeWithoutReturn(int n) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (n <= 0) {\n" //
				+ "            System.out.println(\"Doing another thing\");\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "            return;\n" //
				+ "        } else if (n == 10) {\n" //
				+ "            System.out.println(\"Doing another thing\");\n" //
				+ "            return;\n" //
				+ "        } else if (n == 20) {\n" //
				+ "            System.out.println(\"Doing another thing\");\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "            return;\n" //
				+ "        }\n" //
				+ "        System.out.println(\"Doing something\");\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void mergeIfThrowingException(int i) throws Exception {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (i <= 0) {\n" //
				+ "            i += 42;\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "            throw new Exception();\n" //
				+ "        } else if (i == 10) {\n" //
				+ "            i += 42;\n" //
				+ "            System.out.println(\"Doing another thing\");\n" //
				+ "            throw new Exception();\n" //
				+ "        } else if (i == 20) {\n" //
				+ "            i += 42;\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "            throw new Exception();\n" //
				+ "        }\n" //
				+ "        i = i + 42;\n" //
				+ "        System.out.println(\"Doing something\");\n" //
				+ "        throw new Exception();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void mergeDeepStatements(String number, int i) {\n" //
				+ "        // Keep this comment\n" //
				+ "        try {\n" //
				+ "            Integer.valueOf(number);\n" //
				+ "        } catch (NumberFormatException nfe) {\n" //
				+ "            if (i <= 0) {\n" //
				+ "                i += 42;\n" //
				+ "                System.out.println(\"Doing something\");\n" //
				+ "                return;\n" //
				+ "            } else if (i == 10) {\n" //
				+ "                i += 42;\n" //
				+ "                System.out.println(\"Doing another thing\");\n" //
				+ "                return;\n" //
				+ "            } else if (i == 20) {\n" //
				+ "                i += 42;\n" //
				+ "                System.out.println(\"Doing something\");\n" //
				+ "                return;\n" //
				+ "            }\n" //
				+ "        } catch (IllegalArgumentException iae) {\n" //
				+ "            System.out.println(\"Doing another thing\");\n" //
				+ "            return;\n" //
				+ "        } catch (NullPointerException npe) {\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "            return;\n" //
				+ "        }\n" //
				+ "        System.out.println(\"Doing something\");\n" //
				+ "        return;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void mergeIfWithContinue(int[] numbers) {\n" //
				+ "        for (int i : numbers) {\n" //
				+ "            // Keep this comment\n" //
				+ "            if (i <= 0) {\n" //
				+ "                System.out.println(\"Doing something\");\n" //
				+ "                continue;\n" //
				+ "            } else if (i == 10) {\n" //
				+ "                System.out.println(\"Doing another thing\");\n" //
				+ "                continue;\n" //
				+ "            } else if (i == 20) {\n" //
				+ "                System.out.println(\"Doing something\");\n" //
				+ "                continue;\n" //
				+ "            }\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "            continue;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void mergeIfWithBreak(int[] numbers) {\n" //
				+ "        for (int i : numbers) {\n" //
				+ "            // Keep this comment\n" //
				+ "            if (i <= 0) {\n" //
				+ "                System.out.println(\"Doing something\");\n" //
				+ "                break;\n" //
				+ "            } else if (i == 10) {\n" //
				+ "                System.out.println(\"Doing another thing\");\n" //
				+ "                break;\n" //
				+ "            } else if (i == 20) {\n" //
				+ "                System.out.println(\"Doing something\");\n" //
				+ "                break;\n" //
				+ "            }\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "            break;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void mergeIfThatAlwaysFallThrough(int i, boolean interruptCode) throws Exception {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (i <= 0) {\n" //
				+ "            i++;\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "            if (interruptCode) {\n" //
				+ "                throw new Exception(\"Stop!\");\n" //
				+ "            } else {\n" //
				+ "                return;\n" //
				+ "            }\n" //
				+ "        } else if (i == 10) {\n" //
				+ "            i += 1;\n" //
				+ "            System.out.println(\"Doing another thing\");\n" //
				+ "            if (interruptCode) {\n" //
				+ "                throw new Exception(\"Stop!\");\n" //
				+ "            } else {\n" //
				+ "                return;\n" //
				+ "            }\n" //
				+ "        } else if (i == 20) {\n" //
				+ "            i = 1 + i;\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "            if (interruptCode) {\n" //
				+ "                throw new Exception(\"Stop!\");\n" //
				+ "            } else {\n" //
				+ "                return;\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "        i = i + 1;\n" //
				+ "        System.out.println(\"Doing something\");\n" //
				+ "        if (interruptCode) {\n" //
				+ "            throw new Exception(\"Stop!\");\n" //
				+ "        } else {\n" //
				+ "            return;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", input, false, null);

		enable(CleanUpConstants.REDUNDANT_FALLING_THROUGH_BLOCK_END);

		String output= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.List;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    private boolean b= true;\n" //
				+ "\n" //
				+ "    public void mergeIfBlocksIntoFollowingCode(int i) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (i <= 0) {\n" //
				+ "        } else if (i == 10) {\n" //
				+ "            System.out.println(\"Doing another thing\");\n" //
				+ "            return;\n" //
				+ "        } else if (i == 20) {\n" //
				+ "        }\n" //
				+ "        System.out.println(\"Doing something\");\n" //
				+ "        return;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int mergeUselessElse(int i) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (i <= 0) {\n" //
				+ "            System.out.println(\"Doing another thing\");\n" //
				+ "        }\n" //
				+ "        return i + 123;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public char mergeIfStatementIntoFollowingCode(int j) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (j <= 0) {\n" //
				+ "        } else if (j == 10) return 'b';\n" //
				+ "        else if (j == 20) {\n" //
				+ "        }\n" //
				+ "        return 'a';\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void mergeEndOfIfIntoFollowingCode(int k) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (k <= 0) {\n" //
				+ "            System.out.println(\"Doing another thing\");\n" //
				+ "        } else if (k == 10) {\n" //
				+ "            System.out.println(\"Doing another thing\");\n" //
				+ "            return;\n" //
				+ "        } else if (k == 20) {\n" //
				+ "            System.out.println(\"Doing another thing\");\n" //
				+ "        }\n" //
				+ "        System.out.println(\"Doing something\");\n" //
				+ "        return;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void mergeWithoutContinue(int i, int j) {\n" //
				+ "        while (j-- > 0) {\n" //
				+ "            // Keep this comment\n" //
				+ "            if (i <= 0) {\n" //
				+ "                System.out.println(\"Doing another thing\");\n" //
				+ "            } else if (i == 10) {\n" //
				+ "                System.out.println(\"Doing another thing\");\n" //
				+ "                continue;\n" //
				+ "            } else if (i == 20) {\n" //
				+ "                System.out.println(\"Doing another thing\");\n" //
				+ "            }\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void mergeWithoutReturn(int n) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (n <= 0) {\n" //
				+ "            System.out.println(\"Doing another thing\");\n" //
				+ "        } else if (n == 10) {\n" //
				+ "            System.out.println(\"Doing another thing\");\n" //
				+ "            return;\n" //
				+ "        } else if (n == 20) {\n" //
				+ "            System.out.println(\"Doing another thing\");\n" //
				+ "        }\n" //
				+ "        System.out.println(\"Doing something\");\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void mergeIfThrowingException(int i) throws Exception {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (i <= 0) {\n" //
				+ "        } else if (i == 10) {\n" //
				+ "            i += 42;\n" //
				+ "            System.out.println(\"Doing another thing\");\n" //
				+ "            throw new Exception();\n" //
				+ "        } else if (i == 20) {\n" //
				+ "        }\n" //
				+ "        i = i + 42;\n" //
				+ "        System.out.println(\"Doing something\");\n" //
				+ "        throw new Exception();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void mergeDeepStatements(String number, int i) {\n" //
				+ "        // Keep this comment\n" //
				+ "        try {\n" //
				+ "            Integer.valueOf(number);\n" //
				+ "        } catch (NumberFormatException nfe) {\n" //
				+ "            if (i <= 0) {\n" //
				+ "                i += 42;\n" //
				+ "            } else if (i == 10) {\n" //
				+ "                i += 42;\n" //
				+ "                System.out.println(\"Doing another thing\");\n" //
				+ "                return;\n" //
				+ "            } else if (i == 20) {\n" //
				+ "                i += 42;\n" //
				+ "            }\n" //
				+ "        } catch (IllegalArgumentException iae) {\n" //
				+ "            System.out.println(\"Doing another thing\");\n" //
				+ "            return;\n" //
				+ "        } catch (NullPointerException npe) {\n" //
				+ "        }\n" //
				+ "        System.out.println(\"Doing something\");\n" //
				+ "        return;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void mergeIfWithContinue(int[] numbers) {\n" //
				+ "        for (int i : numbers) {\n" //
				+ "            // Keep this comment\n" //
				+ "            if (i <= 0) {\n" //
				+ "            } else if (i == 10) {\n" //
				+ "                System.out.println(\"Doing another thing\");\n" //
				+ "                continue;\n" //
				+ "            } else if (i == 20) {\n" //
				+ "            }\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "            continue;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void mergeIfWithBreak(int[] numbers) {\n" //
				+ "        for (int i : numbers) {\n" //
				+ "            // Keep this comment\n" //
				+ "            if (i <= 0) {\n" //
				+ "            } else if (i == 10) {\n" //
				+ "                System.out.println(\"Doing another thing\");\n" //
				+ "                break;\n" //
				+ "            } else if (i == 20) {\n" //
				+ "            }\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "            break;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void mergeIfThatAlwaysFallThrough(int i, boolean interruptCode) throws Exception {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (i <= 0) {\n" //
				+ "        } else if (i == 10) {\n" //
				+ "            i += 1;\n" //
				+ "            System.out.println(\"Doing another thing\");\n" //
				+ "            if (interruptCode) {\n" //
				+ "                throw new Exception(\"Stop!\");\n" //
				+ "            } else {\n" //
				+ "                return;\n" //
				+ "            }\n" //
				+ "        } else if (i == 20) {\n" //
				+ "        }\n" //
				+ "        i = i + 1;\n" //
				+ "        System.out.println(\"Doing something\");\n" //
				+ "        if (interruptCode) {\n" //
				+ "            throw new Exception(\"Stop!\");\n" //
				+ "        } else {\n" //
				+ "            return;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		assertGroupCategoryUsed(new ICompilationUnit[] { cu }, new String[] { MultiFixMessages.RedundantFallingThroughBlockEndCleanup_description });
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { output });
	}

	@Test
	public void testDoNotMergeRedundantFallingThroughBlockEnd() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    private boolean b= true;\n" //
				+ "\n" //
				+ "    public void doNotMergeDifferentVariable(int i) {\n" //
				+ "        if (i <= 0) {\n" //
				+ "            boolean b= false;\n" //
				+ "            System.out.println(\"Display a varaible: \" + b);\n" //
				+ "            return;\n" //
				+ "        } else if (i == 10) {\n" //
				+ "            System.out.println(\"Doing another thing\");\n" //
				+ "            return;\n" //
				+ "        } else if (i == 20) {\n" //
				+ "            int b= 123;\n" //
				+ "            System.out.println(\"Display a varaible: \" + b);\n" //
				+ "            return;\n" //
				+ "        }\n" //
				+ "        System.out.println(\"Display a varaible: \" + b);\n" //
				+ "        return;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotMergeWithoutLabeledContinue(int i, int j, int k) {\n" //
				+ "        loop: while (k-- > 0) {\n" //
				+ "            while (j-- > 0) {\n" //
				+ "                if (i <= 0) {\n" //
				+ "                    System.out.println(\"Doing another thing\");\n" //
				+ "                    System.out.println(\"Doing something\");\n" //
				+ "                    continue loop;\n" //
				+ "                } else if (i == 10) {\n" //
				+ "                    System.out.println(\"Doing another thing\");\n" //
				+ "                    continue loop;\n" //
				+ "                } else if (i == 20) {\n" //
				+ "                    System.out.println(\"Doing another thing\");\n" //
				+ "                    System.out.println(\"Doing something\");\n" //
				+ "                    continue loop;\n" //
				+ "                }\n" //
				+ "                System.out.println(\"Doing something\");\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotMergeWithoutBreak(int i, int j) {\n" //
				+ "        while (j-- > 0) {\n" //
				+ "            if (i <= 0) {\n" //
				+ "                System.out.println(\"Doing another thing\");\n" //
				+ "                System.out.println(\"Doing something\");\n" //
				+ "                break;\n" //
				+ "            } else if (i == 10) {\n" //
				+ "                System.out.println(\"Doing another thing\");\n" //
				+ "                break;\n" //
				+ "            } else if (i == 20) {\n" //
				+ "                System.out.println(\"Doing another thing\");\n" //
				+ "                System.out.println(\"Doing something\");\n" //
				+ "                break;\n" //
				+ "            }\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotRefactorCodeThatDoesntFallThrough(int i) {\n" //
				+ "        if (i <= 0) {\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "        } else if (i == 20) {\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "        }\n" //
				+ "        System.out.println(\"Doing something\");\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotRefactorNotLastStatements(String number, int i) {\n" //
				+ "        if (i > 0) {\n" //
				+ "            try {\n" //
				+ "                Integer.valueOf(number);\n" //
				+ "            } catch (NumberFormatException nfe) {\n" //
				+ "                if (i == 5) {\n" //
				+ "                    i += 42;\n" //
				+ "                    System.out.println(\"Doing something\");\n" //
				+ "                    return;\n" //
				+ "                } else if (i == 10) {\n" //
				+ "                    i += 42;\n" //
				+ "                    System.out.println(\"Doing another thing\");\n" //
				+ "                    return;\n" //
				+ "                } else if (i == 20) {\n" //
				+ "                    i += 42;\n" //
				+ "                    System.out.println(\"Doing something\");\n" //
				+ "                    return;\n" //
				+ "                }\n" //
				+ "            } catch (IllegalArgumentException iae) {\n" //
				+ "                System.out.println(\"Doing another thing\");\n" //
				+ "                return;\n" //
				+ "            } catch (NullPointerException npe) {\n" //
				+ "                System.out.println(\"Doing something\");\n" //
				+ "                return;\n" //
				+ "            }\n" //
				+ "            System.out.println(\"Insidious code...\");\n" //
				+ "        }\n" //
				+ "        System.out.println(\"Doing something\");\n" //
				+ "        return;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotMergeIfThatNotAlwaysFallThrough(int i, boolean interruptCode) throws Exception {\n" //
				+ "        if (i <= 0) {\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "            if (interruptCode) {\n" //
				+ "                throw new Exception(\"Stop!\");\n" //
				+ "            }\n" //
				+ "        } else if (i == 10) {\n" //
				+ "            System.out.println(\"Doing another thing\");\n" //
				+ "            if (interruptCode) {\n" //
				+ "                throw new Exception(\"Stop!\");\n" //
				+ "            }\n" //
				+ "        } else if (i == 20) {\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "            if (interruptCode) {\n" //
				+ "                throw new Exception(\"Stop!\");\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "        System.out.println(\"Doing something\");\n" //
				+ "        if (interruptCode) {\n" //
				+ "            throw new Exception(\"Stop!\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotRefactorWithFinally(String number) {\n" //
				+ "        try {\n" //
				+ "            Integer.valueOf(number);\n" //
				+ "        } catch (NumberFormatException nfe) {\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "            return;\n" //
				+ "        } catch (NullPointerException npe) {\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "            return;\n" //
				+ "        } finally {\n" //
				+ "            System.out.println(\"Beware of finally!\");\n" //
				+ "        }\n" //
				+ "        System.out.println(\"Doing something\");\n" //
				+ "        return;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotRefactorCodeThatDoesntFallThrough(String number) {\n" //
				+ "        try {\n" //
				+ "            Integer.valueOf(number);\n" //
				+ "        } catch (NumberFormatException nfe) {\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "        } catch (NullPointerException npe) {\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "        }\n" //
				+ "        System.out.println(\"Doing something\");\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotMergeCatchThatNotAlwaysFallThrough(String number, boolean interruptCode) throws Exception {\n" //
				+ "        try {\n" //
				+ "            Integer.valueOf(number);\n" //
				+ "        } catch (NumberFormatException nfe) {\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "            if (interruptCode) {\n" //
				+ "                throw new Exception(\"Stop!\");\n" //
				+ "            }\n" //
				+ "        } catch (IllegalArgumentException iae) {\n" //
				+ "            System.out.println(\"Doing another thing\");\n" //
				+ "            if (interruptCode) {\n" //
				+ "                throw new Exception(\"Stop!\");\n" //
				+ "            }\n" //
				+ "        } catch (NullPointerException npe) {\n" //
				+ "            System.out.println(\"Doing something\");\n" //
				+ "            if (interruptCode) {\n" //
				+ "                throw new Exception(\"Stop!\");\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "        System.out.println(\"Doing something\");\n" //
				+ "        if (interruptCode) {\n" //
				+ "            throw new Exception(\"Stop!\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.REDUNDANT_FALLING_THROUGH_BLOCK_END);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testRedundantIfCondition() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String input= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.io.IOException;\n" //
				+ "import java.util.List;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public int removeOppositeCondition(boolean b1, boolean b2) {\n" //
				+ "        int i = -1;\n" //
				+ "        // Keep this comment\n" //
				+ "        if (b1 && b2) {\n" //
				+ "            i = 0;\n" //
				+ "        } else if (!b2 || !b1) {\n" //
				+ "            i = 1;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return i;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int removeOppositeConditionWithElse(int i1, int i2) {\n" //
				+ "        int i = -1;\n" //
				+ "        // Keep this comment\n" //
				+ "        if (i1 < i2) {\n" //
				+ "            i = 0;\n" //
				+ "        } else if (i2 <= i1) {\n" //
				+ "            i = 1;\n" //
				+ "        } else {\n" //
				+ "            i = 2;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return i;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int removeOppositeConditionAmongOthers(int i1, int i2) {\n" //
				+ "        int i = -1;\n" //
				+ "        // Keep this comment\n" //
				+ "        if (i1 == 0) {\n" //
				+ "            i = -1;\n" //
				+ "        } else if (i1 < i2 + 1) {\n" //
				+ "            i = 0;\n" //
				+ "        } else if (1 + i2 <= i1) {\n" //
				+ "            i = 1;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return i;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int refactorCaughtCode(boolean b1, boolean b2) {\n" //
				+ "        int i = -1;\n" //
				+ "        try {\n" //
				+ "            // Keep this comment\n" //
				+ "            if (b1 && b2) {\n" //
				+ "                i = 0;\n" //
				+ "            } else if (!b2 || !b1) {\n" //
				+ "                throw new IOException();\n" //
				+ "            }\n" //
				+ "        } catch (IOException e) {\n" //
				+ "            System.out.println(\"I should be reachable\");\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return i;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int removeUncaughtCode(boolean b1, boolean b2) {\n" //
				+ "        int i = -1;\n" //
				+ "        try {\n" //
				+ "            // Keep this comment\n" //
				+ "            if (b1 && b2) {\n" //
				+ "                i = 0;\n" //
				+ "            } else if (!b2 || !b1) {\n" //
				+ "                i = 1;\n" //
				+ "            } else {\n" //
				+ "                throw new NullPointerException();\n" //
				+ "            }\n" //
				+ "        } finally {\n" //
				+ "            System.out.println(\"I should be reachable\");\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return i;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", input, false, null);

		enable(CleanUpConstants.REDUNDANT_IF_CONDITION);

		String output= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.io.IOException;\n" //
				+ "import java.util.List;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public int removeOppositeCondition(boolean b1, boolean b2) {\n" //
				+ "        int i = -1;\n" //
				+ "        // Keep this comment\n" //
				+ "        if (b1 && b2) {\n" //
				+ "            i = 0;\n" //
				+ "        } else {\n" //
				+ "            i = 1;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return i;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int removeOppositeConditionWithElse(int i1, int i2) {\n" //
				+ "        int i = -1;\n" //
				+ "        // Keep this comment\n" //
				+ "        if (i1 < i2) {\n" //
				+ "            i = 0;\n" //
				+ "        } else {\n" //
				+ "            i = 1;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return i;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int removeOppositeConditionAmongOthers(int i1, int i2) {\n" //
				+ "        int i = -1;\n" //
				+ "        // Keep this comment\n" //
				+ "        if (i1 == 0) {\n" //
				+ "            i = -1;\n" //
				+ "        } else if (i1 < i2 + 1) {\n" //
				+ "            i = 0;\n" //
				+ "        } else {\n" //
				+ "            i = 1;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return i;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int refactorCaughtCode(boolean b1, boolean b2) {\n" //
				+ "        int i = -1;\n" //
				+ "        try {\n" //
				+ "            // Keep this comment\n" //
				+ "            if (b1 && b2) {\n" //
				+ "                i = 0;\n" //
				+ "            } else {\n" //
				+ "                throw new IOException();\n" //
				+ "            }\n" //
				+ "        } catch (IOException e) {\n" //
				+ "            System.out.println(\"I should be reachable\");\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return i;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int removeUncaughtCode(boolean b1, boolean b2) {\n" //
				+ "        int i = -1;\n" //
				+ "        try {\n" //
				+ "            // Keep this comment\n" //
				+ "            if (b1 && b2) {\n" //
				+ "                i = 0;\n" //
				+ "            } else {\n" //
				+ "                i = 1;\n" //
				+ "            }\n" //
				+ "        } finally {\n" //
				+ "            System.out.println(\"I should be reachable\");\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return i;\n" //
				+ "    }\n" //
				+ "}\n";
		assertGroupCategoryUsed(new ICompilationUnit[] { cu }, new String[] { MultiFixMessages.RedundantIfConditionCleanup_description });
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { output });
	}

	@Test
	public void testKeepIfCondition() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.io.IOException;\n" //
				+ "import java.util.List;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public int doNotRemoveDifferentCondition(boolean b1, boolean b2) {\n" //
				+ "        int i = -1;\n" //
				+ "        if (b1 && b2) {\n" //
				+ "            i = 0;\n" //
				+ "        } else if (b2 || b1) {\n" //
				+ "            i = 1;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return i;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int doNotRemoveMovedOperands(int number1, int number2) {\n" //
				+ "        int i = -1;\n" //
				+ "        if (number1 < number2) {\n" //
				+ "            i = 0;\n" //
				+ "        } else if (number2 < number1) {\n" //
				+ "            i = 1;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return i;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int doNotRemoveActiveCondition(List<String> myList) {\n" //
				+ "        int i = -1;\n" //
				+ "        if (myList.remove(\"I will be removed\")) {\n" //
				+ "            i = 0;\n" //
				+ "        } else if (myList.remove(\"I will be removed\")) {\n" //
				+ "            i = 1;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return i;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int doNotRemoveCaughtCode(boolean b1, boolean b2) {\n" //
				+ "        int i = -1;\n" //
				+ "        try {\n" //
				+ "            if (b1 && b2) {\n" //
				+ "                i = 0;\n" //
				+ "            } else if (!b2 || !b1) {\n" //
				+ "                i = 1;\n" //
				+ "            } else {\n" //
				+ "                throw new IOException();\n" //
				+ "            }\n" //
				+ "        } catch (IOException e) {\n" //
				+ "            System.out.println(\"I should be reachable\");\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return i;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int doNotRefactorFallThroughBlocks(boolean b1, boolean b2) {\n" //
				+ "        if (b1 && b2) {\n" //
				+ "            return 0;\n" //
				+ "        } else if (!b2 || !b1) {\n" //
				+ "            return 1;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 2;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.REDUNDANT_IF_CONDITION);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testRemoveUselessReturn() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void removeUselessReturn() {\n" //
				+ "        return;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void removeUselessReturnWithPreviousCode() {\n" //
				+ "        System.out.println(\"Keep this line\");\n" //
				+ "        return;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void removeUselessReturnWithIf(boolean isValid) {\n" //
				+ "        if (isValid) {\n" //
				+ "            System.out.println(\"Keep this line\");\n" //
				+ "            return;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceByBlock(boolean isEnabled) {\n" //
				+ "        System.out.println(\"Keep this line\");\n" //
				+ "        if (isEnabled)\n" //
				+ "            return;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void removeElseStatement(boolean isValid) {\n" //
				+ "        System.out.println(\"Keep this line\");\n" //
				+ "        if (isValid)\n" //
				+ "            System.out.println(\"isValid is true\");\n" //
				+ "        else\n" //
				+ "            return;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void removeElseBlock(boolean isValid) {\n" //
				+ "        System.out.println(\"Keep this line\");\n" //
				+ "        if (isValid) {\n" //
				+ "            System.out.println(\"isValid is true\");\n" //
				+ "        } else {\n" //
				+ "            return;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void removeUselessReturnWithSwitch(int myNumber) {\n" //
				+ "        switch (myNumber) {\n" //
				+ "        case 0:\n" //
				+ "            System.out.println(\"Keep this line\");\n" //
				+ "            return;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void removeUselessReturnWithIfElse(boolean isValid) {\n" //
				+ "        if (isValid) {\n" //
				+ "            System.out.println(\"Keep this line\");\n" //
				+ "            return;\n" //
				+ "        } else {\n" //
				+ "            System.out.println(\"Remove anyway\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void removeUselessReturnInLambda() {\n" //
				+ "        Runnable r = () -> {return;};\n" //
				+ "        r.run();\n" //
				+ "        System.out.println(\"Remove anyway\");\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_USELESS_RETURN);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void removeUselessReturn() {\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void removeUselessReturnWithPreviousCode() {\n" //
				+ "        System.out.println(\"Keep this line\");\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void removeUselessReturnWithIf(boolean isValid) {\n" //
				+ "        if (isValid) {\n" //
				+ "            System.out.println(\"Keep this line\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceByBlock(boolean isEnabled) {\n" //
				+ "        System.out.println(\"Keep this line\");\n" //
				+ "        if (isEnabled) {\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void removeElseStatement(boolean isValid) {\n" //
				+ "        System.out.println(\"Keep this line\");\n" //
				+ "        if (isValid)\n" //
				+ "            System.out.println(\"isValid is true\");\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void removeElseBlock(boolean isValid) {\n" //
				+ "        System.out.println(\"Keep this line\");\n" //
				+ "        if (isValid) {\n" //
				+ "            System.out.println(\"isValid is true\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void removeUselessReturnWithSwitch(int myNumber) {\n" //
				+ "        switch (myNumber) {\n" //
				+ "        case 0:\n" //
				+ "            System.out.println(\"Keep this line\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void removeUselessReturnWithIfElse(boolean isValid) {\n" //
				+ "        if (isValid) {\n" //
				+ "            System.out.println(\"Keep this line\");\n" //
				+ "        } else {\n" //
				+ "            System.out.println(\"Remove anyway\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void removeUselessReturnInLambda() {\n" //
				+ "        Runnable r = () -> {};\n" //
				+ "        r.run();\n" //
				+ "        System.out.println(\"Remove anyway\");\n" //
				+ "    }\n" //
				+ "}\n";

		assertGroupCategoryUsed(new ICompilationUnit[] { cu1 }, new String[] { MultiFixMessages.UselessReturnCleanUp_description });
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testDoNotRemoveReturn() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public int doNotRemoveReturnWithValue() {\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotRemoveUselessReturnInMiddleOfSwitch(int myNumber) {\n" //
				+ "        switch (myNumber) {\n" //
				+ "        case 0:\n" //
				+ "            System.out.println(\"I'm not the last statement\");\n" //
				+ "            return;\n" //
				+ "        case 1:\n" //
				+ "            System.out.println(\"Do some stuff\");\n" //
				+ "            break;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotRemoveReturnWithFollowingCode(boolean isValid) {\n" //
				+ "        if (isValid) {\n" //
				+ "            System.out.println(\"Keep this line\");\n" //
				+ "            return;\n" //
				+ "        }\n" //
				+ "        System.out.println(\"Do not forget me\");\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotRemoveReturnInWhile(int myNumber) {\n" //
				+ "        while (myNumber-- > 0) {\n" //
				+ "            System.out.println(\"Keep this line\");\n" //
				+ "            return;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotRemoveReturnInDoWhile(int myNumber) {\n" //
				+ "        do {\n" //
				+ "            System.out.println(\"Keep this line\");\n" //
				+ "            return;\n" //
				+ "        } while (myNumber-- > 0);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotRemoveReturnInFor() {\n" //
				+ "        for (int myNumber = 0; myNumber < 10; myNumber++) {\n" //
				+ "            System.out.println(\"Keep this line\");\n" //
				+ "            return;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotRemoveReturnInForEach(int[] integers) {\n" //
				+ "        for (int myNumber : integers) {\n" //
				+ "            System.out.println(\"Only the first value: \" + myNumber);\n" //
				+ "            return;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_USELESS_RETURN);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testRemoveUselessContinue() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String input= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.List;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void removeUselessContinue(List<String> texts) {\n" //
				+ "        for (String text : texts) {\n" //
				+ "            continue;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void removeUselessContinueWithPreviousCode(List<String> texts) {\n" //
				+ "        for (String text : texts) {\n" //
				+ "            System.out.println(\"Keep this line\");\n" //
				+ "            continue;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void removeUselessContinueWithIf(List<String> texts, boolean isValid) {\n" //
				+ "        for (String text : texts) {\n" //
				+ "            if (isValid) {\n" //
				+ "                System.out.println(\"Keep this line\");\n" //
				+ "                continue;\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceByBlock(List<String> texts, boolean isValid) {\n" //
				+ "        for (String text : texts) {\n" //
				+ "            System.out.println(\"Keep this line\");\n" //
				+ "            if (isValid)\n" //
				+ "                continue;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void removeElseStatement(List<String> texts, boolean isValid) {\n" //
				+ "        for (String text : texts) {\n" //
				+ "            System.out.println(\"Keep this line\");\n" //
				+ "            if (isValid)\n" //
				+ "                System.out.println(\"isValid is true\");\n" //
				+ "            else\n" //
				+ "                continue;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void removeElseBlock(List<String> texts, boolean isValid) {\n" //
				+ "        for (String text : texts) {\n" //
				+ "            System.out.println(\"Keep this line\");\n" //
				+ "            if (isValid) {\n" //
				+ "                System.out.println(\"isValid is true\");\n" //
				+ "            } else {\n" //
				+ "                continue;\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void removeUselessContinueWithSwitch(List<String> texts, int myNumber) {\n" //
				+ "        for (String text : texts) {\n" //
				+ "            switch (myNumber) {\n" //
				+ "            case 0:\n" //
				+ "                System.out.println(\"Keep this line\");\n" //
				+ "                continue;\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void removeUselessContinueWithIfElse(List<String> texts, boolean isValid) {\n" //
				+ "        for (String text : texts) {\n" //
				+ "            if (isValid) {\n" //
				+ "                System.out.println(\"Keep this line\");\n" //
				+ "                continue;\n" //
				+ "            } else {\n" //
				+ "                System.out.println(\"Remove anyway\");\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", input, false, null);

		enable(CleanUpConstants.REMOVE_USELESS_CONTINUE);

		String output= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.List;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void removeUselessContinue(List<String> texts) {\n" //
				+ "        for (String text : texts) {\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void removeUselessContinueWithPreviousCode(List<String> texts) {\n" //
				+ "        for (String text : texts) {\n" //
				+ "            System.out.println(\"Keep this line\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void removeUselessContinueWithIf(List<String> texts, boolean isValid) {\n" //
				+ "        for (String text : texts) {\n" //
				+ "            if (isValid) {\n" //
				+ "                System.out.println(\"Keep this line\");\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceByBlock(List<String> texts, boolean isValid) {\n" //
				+ "        for (String text : texts) {\n" //
				+ "            System.out.println(\"Keep this line\");\n" //
				+ "            if (isValid) {\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void removeElseStatement(List<String> texts, boolean isValid) {\n" //
				+ "        for (String text : texts) {\n" //
				+ "            System.out.println(\"Keep this line\");\n" //
				+ "            if (isValid)\n" //
				+ "                System.out.println(\"isValid is true\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void removeElseBlock(List<String> texts, boolean isValid) {\n" //
				+ "        for (String text : texts) {\n" //
				+ "            System.out.println(\"Keep this line\");\n" //
				+ "            if (isValid) {\n" //
				+ "                System.out.println(\"isValid is true\");\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void removeUselessContinueWithSwitch(List<String> texts, int myNumber) {\n" //
				+ "        for (String text : texts) {\n" //
				+ "            switch (myNumber) {\n" //
				+ "            case 0:\n" //
				+ "                System.out.println(\"Keep this line\");\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void removeUselessContinueWithIfElse(List<String> texts, boolean isValid) {\n" //
				+ "        for (String text : texts) {\n" //
				+ "            if (isValid) {\n" //
				+ "                System.out.println(\"Keep this line\");\n" //
				+ "            } else {\n" //
				+ "                System.out.println(\"Remove anyway\");\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";

		assertGroupCategoryUsed(new ICompilationUnit[] { cu }, new String[] { MultiFixMessages.UselessContinueCleanUp_description });
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { output });
	}

	@Test
	public void testDoNotRemoveContinue() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.List;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void doNotRemoveBreak(List<String> texts) {\n" //
				+ "        for (String text : texts) {\n" //
				+ "            break;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotRemoveReturn(List<String> texts) {\n" //
				+ "        for (String text : texts) {\n" //
				+ "            return;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotRemoveThrow(List<String> texts) {\n" //
				+ "        for (String text : texts) {\n" //
				+ "            throw new NullPointerException();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotRemoveContinueWithLabel(List<String> texts, List<String> otherTexts) {\n" //
				+ "        begin: for (String text : texts) {\n" //
				+ "            for (String otherText : otherTexts) {\n" //
				+ "                System.out.println(\"Keep this line\");\n" //
				+ "                continue begin;\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotRemoveUselessContinueInMiddleOfSwitch(List<String> texts, int myNumber) {\n" //
				+ "        for (String text : texts) {\n" //
				+ "            switch (myNumber) {\n" //
				+ "            case 0:\n" //
				+ "                System.out.println(\"I'm not the last statement\");\n" //
				+ "                continue;\n" //
				+ "            case 1:\n" //
				+ "                System.out.println(\"Do some stuff\");\n" //
				+ "                break;\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotRemoveContinueWithFollowingCode(List<String> texts, boolean isValid) {\n" //
				+ "        for (String text : texts) {\n" //
				+ "            if (isValid) {\n" //
				+ "                System.out.println(\"Keep this line\");\n" //
				+ "                continue;\n" //
				+ "            }\n" //
				+ "            System.out.println(\"Keep this line\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_USELESS_CONTINUE);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testMapMethodRatherThanKeySetMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Map;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public int replaceUnnecesaryCallsToMapKeySet(Map<String, String> map) {\n" //
				+ "        // Keep this comment\n" //
				+ "        int x = map.keySet().size();\n" //
				+ "\n" //
				+ "        if (map.keySet().contains(\"hello\")) {\n" //
				+ "            map.keySet().remove(\"hello\");\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        if (map.keySet().remove(\"world\")) {\n" //
				+ "            // Cannot replace, because `map.removeKey(\"world\") != null` is not strictly equivalent\n" //
				+ "            System.out.println(map);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        // Keep this comment also\n" //
				+ "        map.keySet().clear();\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        if (map.keySet().isEmpty()) {\n" //
				+ "            x++;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return x;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_DIRECTLY_MAP_METHOD);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Map;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public int replaceUnnecesaryCallsToMapKeySet(Map<String, String> map) {\n" //
				+ "        // Keep this comment\n" //
				+ "        int x = map.size();\n" //
				+ "\n" //
				+ "        if (map.containsKey(\"hello\")) {\n" //
				+ "            map.remove(\"hello\");\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        if (map.keySet().remove(\"world\")) {\n" //
				+ "            // Cannot replace, because `map.removeKey(\"world\") != null` is not strictly equivalent\n" //
				+ "            System.out.println(map);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        // Keep this comment also\n" //
				+ "        map.clear();\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        if (map.isEmpty()) {\n" //
				+ "            x++;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return x;\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testMapMethodRatherThanValuesMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Map;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public int replaceUnnecesaryCallsToMapValues(Map<String, String> map) {\n" //
				+ "        // Keep this comment\n" //
				+ "        int x = map.values().size();\n" //
				+ "\n" //
				+ "        if (map.values().contains(\"hello\")) {\n" //
				+ "            map.values().remove(\"hello\");\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        if (map.values().remove(\"world\")) {\n" //
				+ "            System.out.println(map);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        // Keep this comment also\n" //
				+ "        map.values().clear();\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        if (map.values().contains(\"foo\")) {\n" //
				+ "            x++;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return x;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_DIRECTLY_MAP_METHOD);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Map;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public int replaceUnnecesaryCallsToMapValues(Map<String, String> map) {\n" //
				+ "        // Keep this comment\n" //
				+ "        int x = map.size();\n" //
				+ "\n" //
				+ "        if (map.containsValue(\"hello\")) {\n" //
				+ "            map.values().remove(\"hello\");\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        if (map.values().remove(\"world\")) {\n" //
				+ "            System.out.println(map);\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        // Keep this comment also\n" //
				+ "        map.clear();\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        if (map.containsValue(\"foo\")) {\n" //
				+ "            x++;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return x;\n" //
				+ "    }\n" //
				+ "}\n";

		assertGroupCategoryUsed(new ICompilationUnit[] { cu1 }, new String[] { MultiFixMessages.UseDirectlyMapMethodCleanup_description });
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testDoNotUseMapMethodInsideMapImplementation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.HashMap;\n" //
				+ "import java.util.Map;\n" //
				+ "\n" //
				+ "public class E1<K,V> extends HashMap<K,V> {\n" //
				+ "    @Override\n" //
				+ "    public boolean containsKey(Object key) {\n" //
				+ "        return keySet().contains(key);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_DIRECTLY_MAP_METHOD);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotUseMapMethodInsideThisMapImplementation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.HashMap;\n" //
				+ "import java.util.Map;\n" //
				+ "\n" //
				+ "public class E1<K,V> extends HashMap<K,V> {\n" //
				+ "    @Override\n" //
				+ "    public boolean containsKey(Object key) {\n" //
				+ "        return this.keySet().contains(key);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_DIRECTLY_MAP_METHOD);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testCloneCollection() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.ArrayList;\n" //
				+ "import java.util.Date;\n" //
				+ "import java.util.List;\n" //
				+ "import java.util.Map;\n" //
				+ "import java.util.Map.Entry;\n" //
				+ "import java.util.Stack;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void replaceNewNoArgsAssignmentThenAddAll(List<String> col, List<String> output) {\n" //
				+ "        // Keep this comment\n" //
				+ "        output = new ArrayList<String>();\n" //
				+ "        output.addAll(col);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<String> replaceNewNoArgsThenAddAll(List<String> col) {\n" //
				+ "        // Keep this comment\n" //
				+ "        final List<String> output = new ArrayList<String>();\n" //
				+ "        output.addAll(col);\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> replaceNewOneArgThenAddAll(List<Date> col) {\n" //
				+ "        // Keep this comment\n" //
				+ "        final List<Date> output = new ArrayList<Date>(0);\n" //
				+ "        output.addAll(col);\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Integer> replaceNewCollectionSizeThenAddAll(List<Integer> col, List<List<Integer>> listOfCol) {\n" //
				+ "        // Keep this comment\n" //
				+ "        final List<Integer> output = new ArrayList<Integer>(col.size());\n" //
				+ "        output.addAll(col);\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Object replaceNewThenAddAllParameterizedType(Map<String, String> map) {\n" //
				+ "        // Keep this comment\n" //
				+ "        List<Entry<String, String>> output = new ArrayList<Entry<String, String>>();\n" //
				+ "        output.addAll(map.entrySet());\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.COLLECTION_CLONING);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.ArrayList;\n" //
				+ "import java.util.Date;\n" //
				+ "import java.util.List;\n" //
				+ "import java.util.Map;\n" //
				+ "import java.util.Map.Entry;\n" //
				+ "import java.util.Stack;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void replaceNewNoArgsAssignmentThenAddAll(List<String> col, List<String> output) {\n" //
				+ "        // Keep this comment\n" //
				+ "        output = new ArrayList<String>(col);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<String> replaceNewNoArgsThenAddAll(List<String> col) {\n" //
				+ "        // Keep this comment\n" //
				+ "        final List<String> output = new ArrayList<String>(col);\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Date> replaceNewOneArgThenAddAll(List<Date> col) {\n" //
				+ "        // Keep this comment\n" //
				+ "        final List<Date> output = new ArrayList<Date>(col);\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Integer> replaceNewCollectionSizeThenAddAll(List<Integer> col, List<List<Integer>> listOfCol) {\n" //
				+ "        // Keep this comment\n" //
				+ "        final List<Integer> output = new ArrayList<Integer>(col);\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Object replaceNewThenAddAllParameterizedType(Map<String, String> map) {\n" //
				+ "        // Keep this comment\n" //
				+ "        List<Entry<String, String>> output = new ArrayList<Entry<String, String>>(map.entrySet());\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "}\n";

		assertGroupCategoryUsed(new ICompilationUnit[] { cu1 }, new String[] { MultiFixMessages.CollectionCloningCleanUp_description });
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testDoNotCloneCollection() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.ArrayList;\n" //
				+ "import java.util.List;\n" //
				+ "import java.util.Stack;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void doNotReplaceStackCtor(List<String> col, List<String> output) {\n" //
				+ "        output = new Stack<String>();\n" //
				+ "        output.addAll(col);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<String> doNotReplaceAlreadyInitedCol(List<String> col1, List<String> col2) {\n" //
				+ "        final List<String> output = new ArrayList<String>(col1);\n" //
				+ "        output.addAll(col2);\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<String> doNotReplaceWithSpecificSize(List<String> col) {\n" //
				+ "        final List<String> output = new ArrayList<String>(10);\n" //
				+ "        output.addAll(col);\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public List<Object> doNotReplaceNewThenAddAllIncompatibleTypes(List<String> col) {\n" //
				+ "        final List<Object> output = new ArrayList<Object>();\n" //
				+ "        output.addAll(col);\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.COLLECTION_CLONING);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testCloneMap() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.HashMap;\n" //
				+ "import java.util.List;\n" //
				+ "import java.util.Map;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void replaceNewNoArgsAssignmentThenPutAll(Map<String, String> map, Map<String, String> output) {\n" //
				+ "        // Keep this comment\n" //
				+ "        output = new HashMap<String, String>();\n" //
				+ "        output.putAll(map);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Map<String, String> replaceNewNoArgsThenPutAll(Map<String, String> map) {\n" //
				+ "        // Keep this comment\n" //
				+ "        final Map<String, String> output = new HashMap<String, String>();\n" //
				+ "        output.putAll(map);\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Map<String, String> replaceNew0ArgThenPutAll(Map<String, String> map) {\n" //
				+ "        // Keep this comment\n" //
				+ "        final Map<String, String> output = new HashMap<String, String>(0);\n" //
				+ "        output.putAll(map);\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Map<String, String> replaceNew1ArgThenPutAll(Map<String, String> map) {\n" //
				+ "        // Keep this comment\n" //
				+ "        final Map<String, String> output = new HashMap<String, String>(0);\n" //
				+ "        output.putAll(map);\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Map<String, String> replaceNewMapSizeThenPutAll(Map<String, String> map) {\n" //
				+ "        // Keep this comment\n" //
				+ "        final Map<String, String> output = new HashMap<String, String>(map.size());\n" //
				+ "        output.putAll(map);\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Map<String, String> replaceWithSizeOfSubMap(List<Map<String, String>> listOfMap) {\n" //
				+ "        // Keep this comment\n" //
				+ "        final Map<String, String> output = new HashMap<String, String>(listOfMap.get(0).size());\n" //
				+ "        output.putAll(listOfMap.get(0));\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MAP_CLONING);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.HashMap;\n" //
				+ "import java.util.List;\n" //
				+ "import java.util.Map;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void replaceNewNoArgsAssignmentThenPutAll(Map<String, String> map, Map<String, String> output) {\n" //
				+ "        // Keep this comment\n" //
				+ "        output = new HashMap<String, String>(map);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Map<String, String> replaceNewNoArgsThenPutAll(Map<String, String> map) {\n" //
				+ "        // Keep this comment\n" //
				+ "        final Map<String, String> output = new HashMap<String, String>(map);\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Map<String, String> replaceNew0ArgThenPutAll(Map<String, String> map) {\n" //
				+ "        // Keep this comment\n" //
				+ "        final Map<String, String> output = new HashMap<String, String>(map);\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Map<String, String> replaceNew1ArgThenPutAll(Map<String, String> map) {\n" //
				+ "        // Keep this comment\n" //
				+ "        final Map<String, String> output = new HashMap<String, String>(map);\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Map<String, String> replaceNewMapSizeThenPutAll(Map<String, String> map) {\n" //
				+ "        // Keep this comment\n" //
				+ "        final Map<String, String> output = new HashMap<String, String>(map);\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Map<String, String> replaceWithSizeOfSubMap(List<Map<String, String>> listOfMap) {\n" //
				+ "        // Keep this comment\n" //
				+ "        final Map<String, String> output = new HashMap<String, String>(listOfMap.get(0));\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "}\n";

		assertGroupCategoryUsed(new ICompilationUnit[] { cu1 }, new String[] { MultiFixMessages.MapCloningCleanUp_description });
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testDoNotCloneMap() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.HashMap;\n" //
				+ "import java.util.Map;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public Map<String, String> doNotReplaceAlreadyInitedMap(Map<String, String> map1, Map<String, String> map2) {\n" //
				+ "        final Map<String, String> output = new HashMap<String, String>(map1);\n" //
				+ "        output.putAll(map2);\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Map<String, String> doNotReplaceWithSpecificSize(Map<String, String> map) {\n" //
				+ "        Map<String, String> output = new HashMap<String, String>(10);\n" //
				+ "        output.putAll(map);\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Map<Object, Object> doNotReplaceNewThenAddAllIncompatibleTypes(Map<String, String> map) {\n" //
				+ "        final Map<Object, Object> output = new HashMap<Object, Object>();\n" //
				+ "        output.putAll(map);\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Map<String, String> doNotReplaceAnonymousMap(Map<String, String> map) {\n" //
				+ "        final Map<String, String> output = new HashMap<String, String>() {\n" //
				+ "            private static final long serialVersionUID= 1L;\n" //
				+ "\n" //
				+ "            @Override\n" //
				+ "            public void putAll(Map<? extends String, ? extends String> map) {\n" //
				+ "                // Drop the map\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "        output.putAll(map);\n" //
				+ "        return output;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MAP_CLONING);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testSerialVersionBug139381() throws Exception {

		JavaProjectHelper.set14CompilerOptions(getProject());

		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String sample= "" //
					+ "package test1;\n" //
					+ "import java.io.Serializable;\n" //
					+ "public class E1 {\n" //
					+ "    void foo1() {\n" //
					+ "        new Serializable() {\n" //
					+ "        };\n" //
					+ "    }\n" //
					+ "    void foo2() {\n" //
					+ "        new Object() {\n" //
					+ "        };\n" //
					+ "        new Serializable() {\n" //
					+ "        };\n" //
					+ "    }\n" //
					+ "}\n";
			ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

			enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID);
			enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED);

			sample= "" //
					+ "package test1;\n" //
					+ "import java.io.Serializable;\n" //
					+ "public class E1 {\n" //
					+ "    void foo1() {\n" //
					+ "        new Serializable() {\n" //
					+ "\n" //
					+ "            " + FIELD_COMMENT + "\n" //
					+ "            private static final long serialVersionUID = 1L;\n" //
					+ "        };\n" //
					+ "    }\n" //
					+ "    void foo2() {\n" //
					+ "        new Object() {\n" //
					+ "        };\n" //
					+ "        new Serializable() {\n" //
					+ "\n" //
					+ "            " + FIELD_COMMENT + "\n" //
					+ "            private static final long serialVersionUID = 1L;\n" //
					+ "        };\n" //
					+ "    }\n" //
					+ "}\n";
			String expected1= sample;
			assertRefactoringResultAsExpectedIgnoreHashValue(new ICompilationUnit[] {cu1}, new String[] {expected1});
		} finally {
			JavaProjectHelper.set15CompilerOptions(fJProject1);
		}
	}

	@Test
	public void testAddBlockBug149110_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        if (true)\n" //
				+ "            throw new IllegalAccessError();\n" //
				+ "        if (true) {\n" //
				+ "            throw new IllegalAccessError();\n" //
				+ "        }\n" //
				+ "        if (false)\n" //
				+ "            System.out.println();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        if (true)\n" //
				+ "            throw new IllegalAccessError();\n" //
				+ "        if (true)\n" //
				+ "            throw new IllegalAccessError();\n" //
				+ "        if (false) {\n" //
				+ "            System.out.println();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testAddBlockBug149110_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        if (true)\n" //
				+ "            return;\n" //
				+ "        if (true) {\n" //
				+ "            return;\n" //
				+ "        }\n" //
				+ "        if (false)\n" //
				+ "            System.out.println();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        if (true)\n" //
				+ "            return;\n" //
				+ "        if (true)\n" //
				+ "            return;\n" //
				+ "        if (false) {\n" //
				+ "            System.out.println();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testRemoveBlock01() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    public void if_() {\n" //
				+ "        if (true) {\n" //
				+ "            ;\n" //
				+ "        } else if (false) {\n" //
				+ "            ;\n" //
				+ "        } else {\n" //
				+ "            ;\n" //
				+ "        }\n" //
				+ "        \n" //
				+ "        if (true) {\n" //
				+ "            ;;\n" //
				+ "        } else if (false) {\n" //
				+ "            ;;\n" //
				+ "        } else {\n" //
				+ "            ;;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    public void if_() {\n" //
				+ "        if (true)\n" //
				+ "            ;\n" //
				+ "        else if (false)\n" //
				+ "            ;\n" //
				+ "        else\n" //
				+ "            ;\n" //
				+ "        \n" //
				+ "        if (true) {\n" //
				+ "            ;;\n" //
				+ "        } else if (false) {\n" //
				+ "            ;;\n" //
				+ "        } else {\n" //
				+ "            ;;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testRemoveBlock02() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        for (;;) {\n" //
				+ "            ; \n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "    public void bar() {\n" //
				+ "        for (;;) {\n" //
				+ "            ;; \n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        for (;;);\n" //
				+ "    }\n" //
				+ "    public void bar() {\n" //
				+ "        for (;;) {\n" //
				+ "            ;; \n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testRemoveBlock03() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        while (true) {\n" //
				+ "            ;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "    public void bar() {\n" //
				+ "        while (true) {\n" //
				+ "            ;;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        while (true);\n" //
				+ "    }\n" //
				+ "    public void bar() {\n" //
				+ "        while (true) {\n" //
				+ "            ;;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testRemoveBlock04() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        do {\n" //
				+ "            ;\n" //
				+ "        } while (true);\n" //
				+ "    }\n" //
				+ "    public void bar() {\n" //
				+ "        do {\n" //
				+ "            ;;\n" //
				+ "        } while (true);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        do; while (true);\n" //
				+ "    }\n" //
				+ "    public void bar() {\n" //
				+ "        do {\n" //
				+ "            ;;\n" //
				+ "        } while (true);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testRemoveBlock05() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        int[] is= null;\n" //
				+ "        for (int i= 0;i < is.length;i++) {\n" //
				+ "            ;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        int[] is= null;\n" //
				+ "        for (int element : is);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testRemoveBlockBug138628() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        if (true) {\n" //
				+ "            if (true)\n" //
				+ "                ;\n" //
				+ "        } else if (true) {\n" //
				+ "            if (false) {\n" //
				+ "                ;\n" //
				+ "            } else\n" //
				+ "                ;\n" //
				+ "        } else if (false) {\n" //
				+ "            if (true) {\n" //
				+ "                ;\n" //
				+ "            }\n" //
				+ "        } else {\n" //
				+ "            if (true)\n" //
				+ "                ;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        if (true) {\n" //
				+ "            if (true)\n" //
				+ "                ;\n" //
				+ "        } else if (true) {\n" //
				+ "            if (false)\n" //
				+ "                ;\n" //
				+ "            else\n" //
				+ "                ;\n" //
				+ "        } else if (false) {\n" //
				+ "            if (true)\n" //
				+ "                ;\n" //
				+ "        } else if (true)\n" //
				+ "            ;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testRemoveBlockBug149990() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        if (false) {\n" //
				+ "            while (true) {\n" //
				+ "                if (false) {\n" //
				+ "                    ;\n" //
				+ "                }\n" //
				+ "            }\n" //
				+ "        } else\n" //
				+ "            ;\n" //
				+ "    }\n" //
				+ "}";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        if (false) {\n" //
				+ "            while (true)\n" //
				+ "                if (false)\n" //
				+ "                    ;\n" //
				+ "        } else\n" //
				+ "            ;\n" //
				+ "    }\n" //
				+ "}";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testRemoveBlockBug156513_1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(boolean b, int[] ints) {\n" //
				+ "        if (b) {\n" //
				+ "            for (int i = 0; i < ints.length; i++) {\n" //
				+ "                System.out.println(ints[i]);\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER);
		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(boolean b, int[] ints) {\n" //
				+ "        if (b)\n" //
				+ "            for (int j : ints)\n" //
				+ "                System.out.println(j);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testRemoveBlockBug156513_2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(boolean b, int[] ints) {\n" //
				+ "        for (int i = 0; i < ints.length; i++) {\n" //
				+ "            for (int j = 0; j < ints.length; j++) {\n" //
				+ "                System.out.println(ints[j]);\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER);
		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(boolean b, int[] ints) {\n" //
				+ "        for (int k : ints)\n" //
				+ "            for (int l : ints)\n" //
				+ "                System.out.println(l);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testUnnecessaryCodeBug127704_1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    private boolean foo() {\n" //
				+ "        return (boolean) (Boolean) Boolean.TRUE;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    private boolean foo() {\n" //
				+ "        return Boolean.TRUE;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testUnnecessaryCodeBug127704_2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    private Integer foo() {\n" //
				+ "        return (Integer) (Number) getNumber();\n" //
				+ "    }\n" //
				+ "    private Number getNumber() {return null;}\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    private Integer foo() {\n" //
				+ "        return (Integer) getNumber();\n" //
				+ "    }\n" //
				+ "    private Number getNumber() {return null;}\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testAddParentheses01() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    void foo(int i) {\n" //
				+ "        if (i == 0 || i == 1)\n" //
				+ "            System.out.println(i);\n" //
				+ "        \n" //
				+ "        while (i > 0 && i < 10)\n" //
				+ "            System.out.println(1);\n" //
				+ "        \n" //
				+ "        boolean b= i != -1 && i > 10 && i < 100 || i > 20;\n" //
				+ "        \n" //
				+ "        do ; while (i > 5 && b || i < 100 && i > 90);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    void foo(int i) {\n" //
				+ "        if ((i == 0) || (i == 1)) {\n" //
				+ "            System.out.println(i);\n" //
				+ "        }\n" //
				+ "        \n" //
				+ "        while ((i > 0) && (i < 10)) {\n" //
				+ "            System.out.println(1);\n" //
				+ "        }\n" //
				+ "        \n" //
				+ "        boolean b= ((i != -1) && (i > 10) && (i < 100)) || (i > 20);\n" //
				+ "        \n" //
				+ "        do {\n" //
				+ "            ;\n" //
				+ "        } while (((i > 5) && b) || ((i < 100) && (i > 90)));\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testAddParentheses02() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=331845
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    void foo(int i, int j) {\n" //
				+ "        if (i + 10 != j - 5)\n" //
				+ "            System.out.println(i - j + 10 - i * j);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    void foo(int i, int j) {\n" //
				+ "        if ((i + 10) != (j - 5)) {\n" //
				+ "            System.out.println(((i - j) + 10) - (i * j));\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParentheses01() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    void foo(int i) {\n" //
				+ "        if ((i == 0) || (i == 1)) {\n" //
				+ "            System.out.println(i);\n" //
				+ "        }\n" //
				+ "        \n" //
				+ "        while ((i > 0) && (i < 10)) {\n" //
				+ "            System.out.println(1);\n" //
				+ "        }\n" //
				+ "        \n" //
				+ "        boolean b= ((i != -1) && (i > 10) && (i < 100)) || (i > 20);\n" //
				+ "        \n" //
				+ "        do {\n" //
				+ "            ;\n" //
				+ "        } while (((i > 5) && b) || ((i < 100) && (i > 90)));\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    void foo(int i) {\n" //
				+ "        if (i == 0 || i == 1)\n" //
				+ "            System.out.println(i);\n" //
				+ "        \n" //
				+ "        while (i > 0 && i < 10)\n" //
				+ "            System.out.println(1);\n" //
				+ "        \n" //
				+ "        boolean b= i != -1 && i > 10 && i < 100 || i > 20;\n" //
				+ "        \n" //
				+ "        do; while (i > 5 && b || i < 100 && i > 90);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testRemoveParenthesesBug134739() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(boolean a) {\n" //
				+ "        if (((a)))\n" //
				+ "            return;\n" //
				+ "    }\n" //
				+ "    public void bar(boolean a, boolean b) {\n" //
				+ "        if (((a)) || ((b)))\n" //
				+ "            return;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(boolean a) {\n" //
				+ "        if (a)\n" //
				+ "            return;\n" //
				+ "    }\n" //
				+ "    public void bar(boolean a, boolean b) {\n" //
				+ "        if (a || b)\n" //
				+ "            return;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testRemoveParenthesesBug134741_1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public boolean foo(Object o) {\n" //
				+ "        if ((((String)o)).equals(\"\"))\n" //
				+ "            return true;\n" //
				+ "        return false;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public boolean foo(Object o) {\n" //
				+ "        if (((String)o).equals(\"\"))\n" //
				+ "            return true;\n" //
				+ "        return false;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testRemoveParenthesesBug134741_2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(boolean a) {\n" //
				+ "        if ((\"\" + \"b\").equals(\"a\"))\n" //
				+ "            return;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testRemoveParenthesesBug134741_3() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public String foo2(String s) {\n" //
				+ "        return (s != null ? s : \"\").toString();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testRemoveParenthesesBug134985_1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public boolean foo(String s1, String s2, boolean a, boolean b) {\n" //
				+ "        return (a == b) == (s1 == s2);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public boolean foo(String s1, String s2, boolean a, boolean b) {\n" //
				+ "        return a == b == (s1 == s2);\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {sample});
	}

	@Test
	public void testRemoveParenthesesBug134985_2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public String foo() {\n" //
				+ "        return (\"\" + 3) + (3 + 3);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public String foo() {\n" //
				+ "        return \"\" + 3 + (3 + 3);\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {sample});
	}

	@Test
	public void testRemoveParenthesesBug188207() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public int foo() {\n" //
				+ "        boolean b= (true ? true : (true ? false : true));\n" //
				+ "        return ((b ? true : true) ? 0 : 1);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public int foo() {\n" //
				+ "        boolean b= true ? true : true ? false : true;\n" //
				+ "        return (b ? true : true) ? 0 : 1;\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {sample});
	}

	@Test
	public void testRemoveParenthesesBug208752() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        double d = 2.0 * (0.5 / 4.0);\n" //
				+ "        int spaceCount = (3);\n" //
				+ "        spaceCount = 2 * (spaceCount / 2);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        double d = 2.0 * (0.5 / 4.0);\n" //
				+ "        int spaceCount = 3;\n" //
				+ "        spaceCount = 2 * (spaceCount / 2);\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testRemoveParenthesesBug190188() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        (new Object()).toString();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        new Object().toString();\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testRemoveParenthesesBug212856() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public int foo() {\n" //
				+ "        int n= 1 + (2 - 3);\n" //
				+ "        n= 1 - (2 + 3);\n" //
				+ "        n= 1 - (2 - 3);\n" //
				+ "        n= 1 * (2 * 3);\n" //
				+ "        return 2 * (n % 10);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public int foo() {\n" //
				+ "        int n= 1 + 2 - 3;\n" //
				+ "        n= 1 - (2 + 3);\n" //
				+ "        n= 1 - (2 - 3);\n" //
				+ "        n= 1 * 2 * 3;\n" //
				+ "        return 2 * (n % 10);\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testRemoveParenthesesBug335173_1() throws Exception {
		//while loop's expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(boolean a) {\n" //
				+ "        while (((a))) {\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "    public void bar(int x) {\n" //
				+ "        while ((x > 2)) {\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(boolean a) {\n" //
				+ "        while (a) {\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "    public void bar(int x) {\n" //
				+ "        while (x > 2) {\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_2() throws Exception {
		//do while loop's expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int x) {\n" //
				+ "        do {\n" //
				+ "        } while ((x > 2));\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int x) {\n" //
				+ "        do {\n" //
				+ "        } while (x > 2);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_3() throws Exception {
		//for loop's expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int x) {\n" //
				+ "        for (int x = 0; (x > 2); x++) {\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int x) {\n" //
				+ "        for (int x = 0; x > 2; x++) {\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_4() throws Exception {
		//switch statement expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int x) {\n" //
				+ "        switch ((x - 2)) {\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int x) {\n" //
				+ "        switch (x - 2) {\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_5() throws Exception {
		//switch case expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int x) {\n" //
				+ "        switch (x) {\n" //
				+ "        case (1 + 2):\n" //
				+ "            break;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int x) {\n" //
				+ "        switch (x) {\n" //
				+ "        case 1 + 2:\n" //
				+ "            break;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_6() throws Exception {
		//throw statement expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int type) throws Exception {\n" //
				+ "        throw (type == 1 ? new IllegalArgumentException() : new Exception());\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int type) throws Exception {\n" //
				+ "        throw type == 1 ? new IllegalArgumentException() : new Exception();\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_7() throws Exception {
		//synchronized statement expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    private static final Object OBJECT = new Object();\n" //
				+ "    private static final String STRING = new String();\n" //
				+ "    \n" //
				+ "    public void foo(int x) {\n" //
				+ "        synchronized ((x == 1 ? STRING : OBJECT)) {\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    private static final Object OBJECT = new Object();\n" //
				+ "    private static final String STRING = new String();\n" //
				+ "    \n" //
				+ "    public void foo(int x) {\n" //
				+ "        synchronized (x == 1 ? STRING : OBJECT) {\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_8() throws Exception {
		//assert statement expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int x) {\n" //
				+ "        assert (x > 2);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int x) {\n" //
				+ "        assert x > 2;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_9() throws Exception {
		//assert statement message expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int x) {\n" //
				+ "        assert x > 2 : (x - 2);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int x) {\n" //
				+ "        assert x > 2 : x - 2;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_10() throws Exception {
		//array access index expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int a[], int x) {\n" //
				+ "        int i = a[(x + 2)];\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int a[], int x) {\n" //
				+ "        int i = a[x + 2];\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_11() throws Exception {
		//conditional expression's then expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int x) {\n" //
				+ "        int i = x > 10 ? (x > 5 ? x - 1 : x - 2): x;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int x) {\n" //
				+ "        int i = x > 10 ? x > 5 ? x - 1 : x - 2: x;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_12() throws Exception {
		//conditional expression's else expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int x) {\n" //
				+ "        int i = x > 10 ? x: (x > 5 ? x - 1 : x - 2);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int x) {\n" //
				+ "        int i = x > 10 ? x: x > 5 ? x - 1 : x - 2;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_13() throws Exception {
		//conditional expression's then expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int x) {\n" //
				+ "        int i = x > 10 ? (x = x - 2): x;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int x) {\n" //
				+ "        int i = x > 10 ? (x = x - 2): x;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_14() throws Exception {
		//conditional expression's else expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int x) {\n" //
				+ "        int i = x > 10 ? x: (x = x - 2);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int x) {\n" //
				+ "        int i = x > 10 ? x: (x = x - 2);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_15() throws Exception {
		//shift operators
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int x) {\n" //
				+ "        int m= (x >> 2) >> 1;\n" //
				+ "        m= x >> (2 >> 1);\n" //
				+ "        int n= (x << 2) << 1;\n" //
				+ "        n= x << (2 << 1);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int x) {\n" //
				+ "        int m= x >> 2 >> 1;\n" //
				+ "        m= x >> (2 >> 1);\n" //
				+ "        int n= x << 2 << 1;\n" //
				+ "        n= x << (2 << 1);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_16() throws Exception {
		//integer multiplication
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int x, long y) {\n" //
				+ "        int m= (4 * x) * 2;\n" //
				+ "        int n= 4 * (x * 2);\n" //
				+ "        int p= 4 * (x % 3);\n" //
				+ "        int q= 4 * (x / 3);\n" //
				+ "        int r= 4 * (x * y);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int x, long y) {\n" //
				+ "        int m= 4 * x * 2;\n" //
				+ "        int n= 4 * x * 2;\n" //
				+ "        int p= 4 * (x % 3);\n" //
				+ "        int q= 4 * (x / 3);\n" //
				+ "        int r= 4 * (x * y);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_17() throws Exception {
		//floating point multiplication
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(double x) {\n" //
				+ "        int m= (4.0 * x) * 0.5;\n" //
				+ "        int n= 4.0 * (x * 0.5);\n" //
				+ "        int p= 4.0 * (x / 100);\n" //
				+ "        int q= 4.0 * (x % 3);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(double x) {\n" //
				+ "        int m= 4.0 * x * 0.5;\n" //
				+ "        int n= 4.0 * (x * 0.5);\n" //
				+ "        int p= 4.0 * (x / 100);\n" //
				+ "        int q= 4.0 * (x % 3);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_18() throws Exception {
		//integer addition
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int x, long y) {\n" //
				+ "        int m= (4 + x) + 2;\n" //
				+ "        int n= 4 + (x + 2);\n" //
				+ "        int p= 4 + (x + y);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(int x, long y) {\n" //
				+ "        int m= 4 + x + 2;\n" //
				+ "        int n= 4 + x + 2;\n" //
				+ "        int p= 4 + (x + y);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_19() throws Exception {
		//floating point addition
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(double x) {\n" //
				+ "        int m= (4.0 + x) + 100.0;\n" //
				+ "        int n= 4.0 + (x + 100.0);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(double x) {\n" //
				+ "        int m= 4.0 + x + 100.0;\n" //
				+ "        int n= 4.0 + (x + 100.0);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_20() throws Exception {
		//string concatenation
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(String s, String t, String u) {\n" //
				+ "        String a= (s + t) + u;\n" //
				+ "        String b= s + (t + u);\n" //
				+ "        String c= (1 + 2) + s;\n" //
				+ "        String d= 1 + (2 + s);\n" //
				+ "        String e= s + (1 + 2);\n" //
				+ "        String f= (s + 1) + 2;\n" //
				+ "        String g= (1 + s) + 2;\n" //
				+ "        String h= 1 + (s + 2);\n" //
				+ "        String i= s + (1 + t);\n" //
				+ "        String j= s + (t + 1);\n" //
				+ "        String k= s + (1 - 2);\n" //
				+ "        String l= s + (1 * 2);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo(String s, String t, String u) {\n" //
				+ "        String a= s + t + u;\n" //
				+ "        String b= s + t + u;\n" //
				+ "        String c= 1 + 2 + s;\n" //
				+ "        String d= 1 + (2 + s);\n" //
				+ "        String e= s + (1 + 2);\n" //
				+ "        String f= s + 1 + 2;\n" //
				+ "        String g= 1 + s + 2;\n" //
				+ "        String h= 1 + s + 2;\n" //
				+ "        String i= s + 1 + t;\n" //
				+ "        String j= s + t + 1;\n" //
				+ "        String k= s + (1 - 2);\n" //
				+ "        String l= s + 1 * 2;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug405096_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    final Short cache[] = new Short[-(-128) + 1];\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testRemoveParenthesesBug405096_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    int a= 10;\n" //
				+ "    final Short cache[] = new Short[-(-a) + 1];\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testRemoveParenthesesBug405096_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    int a= 10;\n" //
				+ "    final Short cache[] = new Short[-(--a) + 1];\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testRemoveParenthesesBug405096_4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    int a= 10;\n" //
				+ "    final Short cache[] = new Short[+(+a) + 1];\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testRemoveParenthesesBug405096_5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    int a= 10\n" //
				+ "    final Short cache[] = new Short[+(++a) + +(-127)];\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    int a= 10\n" //
				+ "    final Short cache[] = new Short[+(++a) + +-127];\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug405096_6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    final Short cache[] = new Short[+(+128) + ~(-127)];\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    final Short cache[] = new Short[+(+128) + ~-127];\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug405096_7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    String a= \"\";\n" //
				+ "    int n= 0;\n" //
				+ "    \n" //
				+ "    int i1 = 1+(1+(+128));\n" //
				+ "    int j1 = 1+(1+(+n));\n" //
				+ "    int i2 = 1-(-128);\n" //
				+ "    int j2 = 1-(-n);\n" //
				+ "    int i3 = 1+(++n);\n" //
				+ "    int j3 = 1-(--n);\n" //
				+ "    String s1 = a+(++n);\n" //
				+ "    String s2 = a+(+128);\n" //
				+ "    int i5 = 1+(--n);\n" //
				+ "    int j5 = 1-(++n);\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    String a= \"\";\n" //
				+ "    int n= 0;\n" //
				+ "    \n" //
				+ "    int i1 = 1+1+(+128);\n" //
				+ "    int j1 = 1+1+(+n);\n" //
				+ "    int i2 = 1-(-128);\n" //
				+ "    int j2 = 1-(-n);\n" //
				+ "    int i3 = 1+(++n);\n" //
				+ "    int j3 = 1-(--n);\n" //
				+ "    String s1 = a+(++n);\n" //
				+ "    String s2 = a+(+128);\n" //
				+ "    int i5 = 1+--n;\n" //
				+ "    int j5 = 1-++n;\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveQualifier01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public int foo;\n" //
				+ "    public void setFoo(int foo) {\n" //
				+ "        this.foo= foo;\n" //
				+ "    }\n" //
				+ "    public int getFoo() {\n" //
				+ "        return this.foo;\n" //
				+ "    }   \n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public int foo;\n" //
				+ "    public void setFoo(int foo) {\n" //
				+ "        this.foo= foo;\n" //
				+ "    }\n" //
				+ "    public int getFoo() {\n" //
				+ "        return foo;\n" //
				+ "    }   \n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testNumberSuffix() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    private long usual = 101l;\n" //
				+ "    private long octal = 0121l;\n" //
				+ "    private long hex = 0xdafdafdafl;\n" //
				+ "\n" //
				+ "    private float usualFloat = 101f;\n" //
				+ "    private float octalFloat = 0121f;\n" //
				+ "\n" //
				+ "    private double usualDouble = 101d;\n" //
				+ "\n" //
				+ "    public long refactorIt() {\n" //
				+ "        long localVar = 11l;\n" //
				+ "        return localVar + 333l;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public double doNotRefactor() {\n" //
				+ "        long l = 11L;\n" //
				+ "        float f = 11F;\n" //
				+ "        double d = 11D;\n" //
				+ "        float localFloat = 11f;\n" //
				+ "        double localDouble = 11d;\n" //
				+ "        return l + 101L + f + 11F + d + 11D + localFloat + 11f + localDouble + 11d;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.NUMBER_SUFFIX);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    private long usual = 101L;\n" //
				+ "    private long octal = 0121L;\n" //
				+ "    private long hex = 0xdafdafdafL;\n" //
				+ "\n" //
				+ "    private float usualFloat = 101f;\n" //
				+ "    private float octalFloat = 0121f;\n" //
				+ "\n" //
				+ "    private double usualDouble = 101d;\n" //
				+ "\n" //
				+ "    public long refactorIt() {\n" //
				+ "        long localVar = 11L;\n" //
				+ "        return localVar + 333L;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public double doNotRefactor() {\n" //
				+ "        long l = 11L;\n" //
				+ "        float f = 11F;\n" //
				+ "        double d = 11D;\n" //
				+ "        float localFloat = 11f;\n" //
				+ "        double localDouble = 11d;\n" //
				+ "        return l + 101L + f + 11F + d + 11D + localFloat + 11f + localDouble + 11d;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRegExPrecompilation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Arrays;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    private String dateValidation= \".*\";\n" //
				+ "    private static boolean valid;\n" //
				+ "\n" //
				+ "    static {\n" //
				+ "        // Keep this comment\n" //
				+ "        String dateValidation= \"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\";\n" //
				+ "        String dateValidation2= \"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\";\n" //
				+ "        String date1= \"1962-12-18\";\n" //
				+ "        String date2= \"2000-03-15\";\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        valid= date1.matches(dateValidation) && date2.matches(dateValidation)\n" //
				+ "                && date1.matches(dateValidation2) && date2.matches(dateValidation2);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public boolean usePattern(String date1, String date2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        String dateValidation= \"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\";\n" //
				+ "        String dateValidation2= \"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\";\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        return date1.matches(dateValidation) && date2.matches(dateValidation)\n" //
				+ "                && date1.matches(dateValidation2) && date2.matches(dateValidation2);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public boolean usePatternAmongStatements(String date1, String date2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        String dateValidation= \"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\";\n" //
				+ "        System.out.println(\"Do other things\");\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        return date1.matches(dateValidation) && date2.matches(dateValidation);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String usePatternForReplace(String date1, String date2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        String dateValidation= \"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\";\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        String dateText1= date1.replaceFirst(dateValidation, \"0000-00-00\");\n" //
				+ "        // Keep this comment also\n" //
				+ "        String dateText2= date2.replaceAll(dateValidation, \"0000-00-00\");\n" //
				+ "\n" //
				+ "        return dateText1 + dateText2;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String usePatternForSplit1(String speech1, String speech2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        String line= \"\\\\r?\\\\n\";\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        String[] phrases1= speech1.split(line);\n" //
				+ "        // Keep this comment also\n" //
				+ "        String[] phrases2= speech2.split(line, 123);\n" //
				+ "\n" //
				+ "        return Arrays.toString(phrases1) + Arrays.toString(phrases2);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String usePatternForSplit2(String speech1, String speech2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        String line= \".\";\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        String[] phrases1= speech1.split(line);\n" //
				+ "        // Keep this comment also\n" //
				+ "        String[] phrases2= speech2.split(line, 123);\n" //
				+ "\n" //
				+ "        return Arrays.toString(phrases1) + Arrays.toString(phrases2);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String usePatternForSplit3(String speech1, String speech2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        String line= \"\\\\a\";\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        String[] phrases1= speech1.split(line);\n" //
				+ "        // Keep this comment also\n" //
				+ "        String[] phrases2= speech2.split(line, 123);\n" //
				+ "\n" //
				+ "        return Arrays.toString(phrases1) + Arrays.toString(phrases2);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String usePatternForLocalVariableOnly(String date1, String date2, String date3) {\n" //
				+ "        String dateText1= date1.replaceFirst(dateValidation, \"0000-00-00\");\n" //
				+ "        // Keep this comment\n" //
				+ "        String dateValidation= \"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\";\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        String dateText2= date2.replaceFirst(dateValidation, \"0000-00-00\");\n" //
				+ "        // Keep this comment also\n" //
				+ "        String dateText3= date3.replaceAll(dateValidation, \"0000-00-00\");\n" //
				+ "\n" //
				+ "        return dateText1 + dateText2 + dateText3;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "   public boolean usePatternFromVariable(String regex, String date1, String date2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        String dateValidation= regex;\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        return date1.matches(dateValidation) && \"\".equals(date2.replaceFirst(dateValidation, \"\"));\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PRECOMPILE_REGEX);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Arrays;\n" //
				+ "import java.util.regex.Pattern;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    private String dateValidation= \".*\";\n" //
				+ "    private static boolean valid;\n" //
				+ "\n" //
				+ "    static {\n" //
				+ "        // Keep this comment\n" //
				+ "        Pattern dateValidation= Pattern.compile(\"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\");\n" //
				+ "        Pattern dateValidation2= Pattern.compile(\"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\");\n" //
				+ "        String date1= \"1962-12-18\";\n" //
				+ "        String date2= \"2000-03-15\";\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        valid= dateValidation.matcher(date1).matches() && dateValidation.matcher(date2).matches()\n" //
				+ "                && dateValidation2.matcher(date1).matches() && dateValidation2.matcher(date2).matches();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private static final Pattern dateValidation_pattern = Pattern.compile(\"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\");\n" //
				+ "    private static final Pattern dateValidation2_pattern = Pattern.compile(\"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\");\n" //
				+ "\n" //
				+ "    public boolean usePattern(String date1, String date2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Pattern dateValidation= dateValidation_pattern;\n" //
				+ "        Pattern dateValidation2= dateValidation2_pattern;\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        return dateValidation.matcher(date1).matches() && dateValidation.matcher(date2).matches()\n" //
				+ "                && dateValidation2.matcher(date1).matches() && dateValidation2.matcher(date2).matches();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private static final Pattern dateValidation_pattern2 = Pattern.compile(\"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\");\n" //
				+ "\n" //
				+ "    public boolean usePatternAmongStatements(String date1, String date2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Pattern dateValidation= dateValidation_pattern2;\n" //
				+ "        System.out.println(\"Do other things\");\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        return dateValidation.matcher(date1).matches() && dateValidation.matcher(date2).matches();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private static final Pattern dateValidation_pattern3 = Pattern.compile(\"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\");\n" //
				+ "\n" //
				+ "    public String usePatternForReplace(String date1, String date2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Pattern dateValidation= dateValidation_pattern3;\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        String dateText1= dateValidation.matcher(date1).replaceFirst(\"0000-00-00\");\n" //
				+ "        // Keep this comment also\n" //
				+ "        String dateText2= dateValidation.matcher(date2).replaceAll(\"0000-00-00\");\n" //
				+ "\n" //
				+ "        return dateText1 + dateText2;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private static final Pattern line_pattern = Pattern.compile(\"\\\\r?\\\\n\");\n" //
				+ "\n" //
				+ "    public String usePatternForSplit1(String speech1, String speech2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Pattern line= line_pattern;\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        String[] phrases1= line.split(speech1);\n" //
				+ "        // Keep this comment also\n" //
				+ "        String[] phrases2= line.split(speech2, 123);\n" //
				+ "\n" //
				+ "        return Arrays.toString(phrases1) + Arrays.toString(phrases2);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private static final Pattern line_pattern2 = Pattern.compile(\".\");\n" //
				+ "\n" //
				+ "    public String usePatternForSplit2(String speech1, String speech2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Pattern line= line_pattern2;\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        String[] phrases1= line.split(speech1);\n" //
				+ "        // Keep this comment also\n" //
				+ "        String[] phrases2= line.split(speech2, 123);\n" //
				+ "\n" //
				+ "        return Arrays.toString(phrases1) + Arrays.toString(phrases2);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private static final Pattern line_pattern3 = Pattern.compile(\"\\\\a\");\n" //
				+ "\n" //
				+ "    public String usePatternForSplit3(String speech1, String speech2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Pattern line= line_pattern3;\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        String[] phrases1= line.split(speech1);\n" //
				+ "        // Keep this comment also\n" //
				+ "        String[] phrases2= line.split(speech2, 123);\n" //
				+ "\n" //
				+ "        return Arrays.toString(phrases1) + Arrays.toString(phrases2);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private static final Pattern dateValidation_pattern4 = Pattern.compile(\"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\");\n" //
				+ "\n" //
				+ "    public String usePatternForLocalVariableOnly(String date1, String date2, String date3) {\n" //
				+ "        String dateText1= date1.replaceFirst(dateValidation, \"0000-00-00\");\n" //
				+ "        // Keep this comment\n" //
				+ "        Pattern dateValidation= dateValidation_pattern4;\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        String dateText2= dateValidation.matcher(date2).replaceFirst(\"0000-00-00\");\n" //
				+ "        // Keep this comment also\n" //
				+ "        String dateText3= dateValidation.matcher(date3).replaceAll(\"0000-00-00\");\n" //
				+ "\n" //
				+ "        return dateText1 + dateText2 + dateText3;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public boolean usePatternFromVariable(String regex, String date1, String date2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Pattern dateValidation= Pattern.compile(regex);\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        return dateValidation.matcher(date1).matches() && \"\".equals(dateValidation.matcher(date2).replaceFirst(\"\"));\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRegExPrecompilationInDefaultMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Arrays;\n" //
				+ "\n" //
				+ "public interface I1 {\n" //
				+ "    private String dateValidation= \".*\";\n" //
				+ "\n" //
				+ "    public default boolean usePattern(String date1, String date2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        String dateValidation= \"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\";\n" //
				+ "        String dateValidation2= \"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\";\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        return date1.matches(dateValidation) && date2.matches(dateValidation)\n" //
				+ "                && date1.matches(dateValidation2) && date2.matches(dateValidation2);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("I1.java", sample, false, null);

		enable(CleanUpConstants.PRECOMPILE_REGEX);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Arrays;\n" //
				+ "import java.util.regex.Pattern;\n" //
				+ "\n" //
				+ "public interface I1 {\n" //
				+ "    private String dateValidation= \".*\";\n" //
				+ "\n" //
				+ "    public default boolean usePattern(String date1, String date2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        Pattern dateValidation= Pattern.compile(\"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\");\n" //
				+ "        Pattern dateValidation2= Pattern.compile(\"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\");\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        return dateValidation.matcher(date1).matches() && dateValidation.matcher(date2).matches()\n" //
				+ "                && dateValidation2.matcher(date1).matches() && dateValidation2.matcher(date2).matches();\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRegExPrecompilationInInnerClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Arrays;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    private String dateValidation= \".*\";\n" //
				+ "\n" //
				+ "    private class Inner1 {\n" //
				+ "        public default boolean usePattern(String date1, String date2) {\n" //
				+ "            // Keep this comment\n" //
				+ "            String dateValidation= \"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\";\n" //
				+ "\n" //
				+ "            // Keep this comment too\n" //
				+ "            return date1.matches(dateValidation) && date2.matches(dateValidation);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private static class Inner2 {\n" //
				+ "        public default boolean usePattern(String date1, String date2) {\n" //
				+ "            // Keep this comment\n" //
				+ "            String dateValidation= \"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\";\n" //
				+ "\n" //
				+ "            // Keep this comment too\n" //
				+ "            return date1.matches(dateValidation) && date2.matches(dateValidation);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void foo() {\n" //
				+ "        public default boolean usePattern(String date1, String date2) {\n" //
				+ "            // Keep this comment\n" //
				+ "            String dateValidation= \"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\";\n" //
				+ "\n" //
				+ "            // Keep this comment too\n" //
				+ "            return date1.matches(dateValidation) && date2.matches(dateValidation);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PRECOMPILE_REGEX);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Arrays;\n" //
				+ "import java.util.regex.Pattern;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    private String dateValidation= \".*\";\n" //
				+ "\n" //
				+ "    private class Inner1 {\n" //
				+ "        public default boolean usePattern(String date1, String date2) {\n" //
				+ "            // Keep this comment\n" //
				+ "            Pattern dateValidation= Pattern.compile(\"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\");\n" //
				+ "\n" //
				+ "            // Keep this comment too\n" //
				+ "            return dateValidation.matcher(date1).matches() && dateValidation.matcher(date2).matches();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private static class Inner2 {\n" //
				+ "        private static final Pattern dateValidation_pattern = Pattern\n" //
				+ "                .compile(\"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\");\n" //
				+ "\n" //
				+ "        public default boolean usePattern(String date1, String date2) {\n" //
				+ "            // Keep this comment\n" //
				+ "            Pattern dateValidation= dateValidation_pattern;\n" //
				+ "\n" //
				+ "            // Keep this comment too\n" //
				+ "            return dateValidation.matcher(date1).matches() && dateValidation.matcher(date2).matches();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private static final Pattern dateValidation_pattern = Pattern.compile(\"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\");\n" //
				+ "\n" //
				+ "    public void foo() {\n" //
				+ "        public default boolean usePattern(String date1, String date2) {\n" //
				+ "            // Keep this comment\n" //
				+ "            Pattern dateValidation= dateValidation_pattern;\n" //
				+ "\n" //
				+ "            // Keep this comment too\n" //
				+ "            return dateValidation.matcher(date1).matches() && dateValidation.matcher(date2).matches();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRegExPrecompilationInLocalClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Arrays;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    private String dateValidation= \".*\";\n" //
				+ "\n" //
				+ "    public void foo() {\n" //
				+ "        class Inner {\n" //
				+ "            public default boolean usePattern(String date1, String date2) {\n" //
				+ "                // Keep this comment\n" //
				+ "                String dateValidation= \"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\";\n" //
				+ "                String dateValidation2= \"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\";\n" //
				+ "\n" //
				+ "                // Keep this comment too\n" //
				+ "                return date1.matches(dateValidation) && date2.matches(dateValidation)\n" //
				+ "                    && date1.matches(dateValidation2) && date2.matches(dateValidation2);\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PRECOMPILE_REGEX);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Arrays;\n" //
				+ "import java.util.regex.Pattern;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    private String dateValidation= \".*\";\n" //
				+ "\n" //
				+ "    public void foo() {\n" //
				+ "        class Inner {\n" //
				+ "            public default boolean usePattern(String date1, String date2) {\n" //
				+ "                // Keep this comment\n" //
				+ "                Pattern dateValidation= Pattern.compile(\"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\");\n" //
				+ "                Pattern dateValidation2= Pattern.compile(\"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\");\n" //
				+ "\n" //
				+ "                // Keep this comment too\n" //
				+ "                return dateValidation.matcher(date1).matches() && dateValidation.matcher(date2).matches()\n" //
				+ "                        && dateValidation2.matcher(date1).matches() && dateValidation2.matcher(date2).matches();\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRegExPrecompilationInAnonymousClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Arrays;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    abstract class I1 {\n" //
				+ "        public abstract boolean validate(String date1, String date2);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void foo() {\n" //
				+ "        I1 i1= new I1() {\n" //
				+ "            @Override\n" //
				+ "            public boolean validate(String date1, String date2) {\n" //
				+ "                // Keep this comment\n" //
				+ "                String dateValidation= \"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\";\n" //
				+ "                String dateValidation2= \"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\";\n" //
				+ "\n" //
				+ "                // Keep this comment too\n" //
				+ "                return date1.matches(dateValidation) && date2.matches(dateValidation)\n" //
				+ "                        && date1.matches(dateValidation2) && date2.matches(dateValidation2);\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PRECOMPILE_REGEX);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Arrays;\n" //
				+ "import java.util.regex.Pattern;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    abstract class I1 {\n" //
				+ "        public abstract boolean validate(String date1, String date2);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private static final Pattern dateValidation_pattern = Pattern.compile(\"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\");\n" //
				+ "    private static final Pattern dateValidation2_pattern = Pattern.compile(\"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\");\n" //
				+ "\n" //
				+ "    public void foo() {\n" //
				+ "        I1 i1= new I1() {\n" //
				+ "            @Override\n" //
				+ "            public boolean validate(String date1, String date2) {\n" //
				+ "                // Keep this comment\n" //
				+ "                Pattern dateValidation= dateValidation_pattern;\n" //
				+ "                Pattern dateValidation2= dateValidation2_pattern;\n" //
				+ "\n" //
				+ "                // Keep this comment too\n" //
				+ "                return dateValidation.matcher(date1).matches() && dateValidation.matcher(date2).matches()\n" //
				+ "                        && dateValidation2.matcher(date1).matches() && dateValidation2.matcher(date2).matches();\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRegExPrecompilationInLambda() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Arrays;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    I1 i1 = () -> {\n" //
				+ "        String p = \"abcd\";\n" //
				+ "        String x = \"abcdef\";\n" //
				+ "        String y = \"bcdefg\";\n" //
				+ "        String[] a = x.split(p);\n" //
				+ "        String[] b = y.split(p);\n" //
				+ "    };\n" //
				+ "\n" //
				+ "    interface I1 {\n" //
				+ "        public void m();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void foo() {\n" //
				+ "        I1 i1= () -> {\n" //
				+ "            String p = \"abcd\";\n" //
				+ "            String x = \"abcdef\";\n" //
				+ "            String y = \"bcdefg\";\n" //
				+ "            String[] a = x.split(p);\n" //
				+ "            String[] b = y.split(p);\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PRECOMPILE_REGEX);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Arrays;\n" //
				+ "import java.util.regex.Pattern;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    private static final Pattern p_pattern = Pattern.compile(\"abcd\");\n" //
				+ "    I1 i1 = () -> {\n" //
				+ "        Pattern p = p_pattern;\n" //
				+ "        String x = \"abcdef\";\n" //
				+ "        String y = \"bcdefg\";\n" //
				+ "        String[] a = p.split(x);\n" //
				+ "        String[] b = p.split(y);\n" //
				+ "    };\n" //
				+ "\n" //
				+ "    interface I1 {\n" //
				+ "        public void m();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private static final Pattern p_pattern2 = Pattern.compile(\"abcd\");\n" //
				+ "\n" //
				+ "    public void foo() {\n" //
				+ "        I1 i1= () -> {\n" //
				+ "            Pattern p = p_pattern2;\n" //
				+ "            String x = \"abcdef\";\n" //
				+ "            String y = \"bcdefg\";\n" //
				+ "            String[] a = p.split(x);\n" //
				+ "            String[] b = p.split(y);\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testDoNotRefactorRegExWithPrecompilation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Arrays;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public boolean doNotUsePatternForOneUse(String date) {\n" //
				+ "       String dateValidation= \"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\";\n" //
				+ "\n" //
				+ "       return date.matches(dateValidation);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public boolean doNotUsePatternWithOtherUse(String date1, String date2) {\n" //
				+ "       String dateValidation= \"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\";\n" //
				+ "       System.out.println(\"The pattern is: \" + dateValidation);\n" //
				+ "\n" //
				+ "       return date1.matches(dateValidation) && date2.matches(dateValidation);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public boolean doNotUsePatternWithOtherMethod(String date1, String date2) {\n" //
				+ "       String dateValidation= \"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\";\n" //
				+ "\n" //
				+ "       return date1.matches(dateValidation) && \"\".equals(date2.replace(dateValidation, \"\"));\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public boolean doNotUsePatternInMultiDeclaration(String date1, String date2) {\n" //
				+ "       String dateValidation= \"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\", foo= \"bar\";\n" //
				+ "\n" //
				+ "       return date1.matches(dateValidation) && date2.matches(dateValidation);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public boolean doNotUsePatternOnMisplacedUse(String date1, String date2) {\n" //
				+ "       String dateValidation= \"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\";\n" //
				+ "\n" //
				+ "       return dateValidation.matches(date1) && dateValidation.matches(date2);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String doNotUsePatternOnMisplacedParameter(String date1, String date2) {\n" //
				+ "       String dateValidation= \"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\";\n" //
				+ "\n" //
				+ "       String dateText1= date1.replaceFirst(\"0000-00-00\", dateValidation);\n" //
				+ "       String dateText2= date2.replaceAll(\"0000-00-00\", dateValidation);\n" //
				+ "\n" //
				+ "       return dateText1 + dateText2;\n" //
				+ "    }\n" //
				+ "    public String doNotUsePatternOnSimpleSplit1(String speech1, String speech2) {\n" //
				+ "       String line= \"a\";\n" //
				+ "\n" //
				+ "       String[] phrases1= speech1.split(line);\n" //
				+ "       String[] phrases2= speech2.split(line, 1);\n" //
				+ "       return phrases1[0] + phrases2[0];\n" //
				+ "    }\n" //
				+ "    public String doNotUsePatternOnSimpleSplit2(String speech1, String speech2) {\n" //
				+ "       String line= \"\\\\;\";\n" //
				+ "\n" //
				+ "       String[] phrases1= speech1.split(line);\n" //
				+ "       String[] phrases2= speech2.split(line, 1);\n" //
				+ "       return phrases1[0] + phrases2[0];\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PRECOMPILE_REGEX);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testRegExPrecompilationWithExistingImport() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import javax.validation.constraints.Pattern;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    private String code;\n" //
				+ "    private String dateValidation= \".*\";\n" //
				+ "\n" //
				+ "   public boolean usePattern(String date1, String date2) {\n" //
				+ "       // Keep this comment\n" //
				+ "       String dateValidation= \"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\";\n" //
				+ "\n" //
				+ "       // Keep this comment too\n" //
				+ "       return date1.matches(dateValidation) && date2.matches(dateValidation);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    @Pattern(regexp=\"\\\\d{4}\",\n" //
				+ "        message=\"The code should contain exactly four numbers.\")\n" //
				+ "    public String getCode() {\n" //
				+ "        return code;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void setCode(String code) {\n" //
				+ "        this.code= code;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.PRECOMPILE_REGEX);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import javax.validation.constraints.Pattern;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    private String code;\n" //
				+ "    private String dateValidation= \".*\";\n" //
				+ "    private static final java.util.regex.Pattern dateValidation_pattern = java.util.regex.Pattern\n"
				+ "            .compile(\"\\\\d{4}\\\\-\\\\d{2}\\\\-\\\\d{2}\");\n" //
				+ "\n" //
				+ "    public boolean usePattern(String date1, String date2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        java.util.regex.Pattern dateValidation= dateValidation_pattern;\n" //
				+ "\n" //
				+ "        // Keep this comment too\n" //
				+ "        return dateValidation.matcher(date1).matches() && dateValidation.matcher(date2).matches();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    @Pattern(regexp=\"\\\\d{4}\",\n" //
				+ "            message=\"The code should contain exactly four numbers.\")\n" //
				+ "    public String getCode() {\n" //
				+ "        return code;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void setCode(String code) {\n" //
				+ "        this.code= code;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testEmbeddedIf() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    public void collapseIfStatements(boolean isActive, boolean isValid) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (isActive) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            if (isValid) {\n" //
				+ "                // Keep this comment also\n" //
				+ "                int i = 0;\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void collapseWithFourOperands(int i1, int i2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (0 < i1 && i1 < 10) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            if (0 < i2 && i2 < 10) {\n" //
				+ "                // Keep this comment also\n" //
				+ "                int i = 0;\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void collapseIfStatementsAddParenthesesIfDifferentConditionalOperator(boolean isActive, boolean isValid, boolean isEditMode) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (isActive) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            if (isValid || isEditMode) {\n" //
				+ "                // Keep this comment also\n" //
				+ "                int i = 0;\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void collapseIfWithOROperator(boolean isActive, boolean isValid, boolean isEditMode) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (isActive) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            if (isValid | isEditMode) {\n" //
				+ "                // Keep this comment also\n" //
				+ "                int i = 0;\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.RAISE_EMBEDDED_IF);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "    public void collapseIfStatements(boolean isActive, boolean isValid) {\n" //
				+ "        // Keep this comment\n" //
				+ "        // Keep this comment too\n" //
				+ "        if (isActive && isValid) {\n" //
				+ "            // Keep this comment also\n" //
				+ "            int i = 0;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void collapseWithFourOperands(int i1, int i2) {\n" //
				+ "        // Keep this comment\n" //
				+ "        // Keep this comment too\n" //
				+ "        if ((0 < i1 && i1 < 10) && (0 < i2 && i2 < 10)) {\n" //
				+ "            // Keep this comment also\n" //
				+ "            int i = 0;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void collapseIfStatementsAddParenthesesIfDifferentConditionalOperator(boolean isActive, boolean isValid, boolean isEditMode) {\n" //
				+ "        // Keep this comment\n" //
				+ "        // Keep this comment too\n" //
				+ "        if (isActive && (isValid || isEditMode)) {\n" //
				+ "            // Keep this comment also\n" //
				+ "            int i = 0;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void collapseIfWithOROperator(boolean isActive, boolean isValid, boolean isEditMode) {\n" //
				+ "        // Keep this comment\n" //
				+ "        // Keep this comment too\n" //
				+ "        if (isActive && (isValid | isEditMode)) {\n" //
				+ "            // Keep this comment also\n" //
				+ "            int i = 0;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		assertGroupCategoryUsed(new ICompilationUnit[] { cu1 }, new String[] { MultiFixMessages.EmbeddedIfCleanup_description });
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testDoNotRaiseEmbeddedIf() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void doNotCollapseWithFiveOperands(int i1, int i2) {\n" //
				+ "        if (0 < i1 && i1 < 10) {\n" //
				+ "            if (100 < i2 && i2 < 200 || i2 < 0) {\n" //
				+ "                int i = 0;\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotCollapseOuterIfWithElseStatement(boolean isActive, boolean isValid) {\n" //
				+ "        if (isActive) {\n" //
				+ "            if (isValid) {\n" //
				+ "                int i = 0;\n" //
				+ "            }\n" //
				+ "        } else {\n" //
				+ "            int i = 0;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotCollapseIfWithElseStatement2(boolean isActive, boolean isValid) {\n" //
				+ "        if (isActive) {\n" //
				+ "            if (isValid) {\n" //
				+ "                int i = 0;\n" //
				+ "            } else {\n" //
				+ "                int i = 0;\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.RAISE_EMBEDDED_IF);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testRemoveQualifier02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public int foo() {return 0;}\n" //
				+ "    public int getFoo() {\n" //
				+ "        return this.foo();\n" //
				+ "    }   \n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public int foo() {return 0;}\n" //
				+ "    public int getFoo() {\n" //
				+ "        return foo();\n" //
				+ "    }   \n" //
				+ "}\n";
		String expected1= sample;

		assertGroupCategoryUsed(new ICompilationUnit[] { cu1 }, new String[] { FixMessages.CodeStyleFix_removeThis_groupDescription });
		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testRemoveQualifier03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public int foo;\n" //
				+ "    public int bar;\n" //
				+ "    public class E1Inner {\n" //
				+ "        private int bar;\n" //
				+ "        public int getFoo() {\n" //
				+ "            E1.this.bar= this.bar;\n" //
				+ "            return E1.this.foo;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public int foo;\n" //
				+ "    public int bar;\n" //
				+ "    public class E1Inner {\n" //
				+ "        private int bar;\n" //
				+ "        public int getFoo() {\n" //
				+ "            E1.this.bar= bar;\n" //
				+ "            return foo;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testRemoveQualifier04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public int foo() {return 0;}\n" //
				+ "    public int bar() {return 0;}\n" //
				+ "    public class E1Inner {\n" //
				+ "        private int bar() {return 1;}\n" //
				+ "        public int getFoo() {\n" //
				+ "            E1.this.bar(); \n" //
				+ "            this.bar();\n" //
				+ "            return E1.this.foo();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public int foo() {return 0;}\n" //
				+ "    public int bar() {return 0;}\n" //
				+ "    public class E1Inner {\n" //
				+ "        private int bar() {return 1;}\n" //
				+ "        public int getFoo() {\n" //
				+ "            E1.this.bar(); \n" //
				+ "            bar();\n" //
				+ "            return foo();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testRemoveQualifierBug134720() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        this.setEnabled(true);\n" //
				+ "    }\n" //
				+ "    private void setEnabled(boolean b) {}\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        setEnabled(true);\n" //
				+ "    }\n" //
				+ "    private void setEnabled(boolean b) {}\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testRemoveQualifierBug150481_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public class Inner extends E {\n" //
				+ "        public void test() {\n" //
				+ "            E.this.foo();\n" //
				+ "            this.foo();\n" //
				+ "            foo();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "    public void foo() {}\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public class Inner extends E {\n" //
				+ "        public void test() {\n" //
				+ "            E.this.foo();\n" //
				+ "            foo();\n" //
				+ "            foo();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "    public void foo() {}\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testRemoveQualifierBug150481_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public class Inner {\n" //
				+ "        public void test() {\n" //
				+ "            E.this.foo();\n" //
				+ "            foo();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "    public void foo() {}\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public class Inner {\n" //
				+ "        public void test() {\n" //
				+ "            foo();\n" //
				+ "            foo();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "    public void foo() {}\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testRemoveQualifierBug219478() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 extends E2 {\n" //
				+ "    private class E1Inner extends E2 {\n" //
				+ "        public E1Inner() {\n" //
				+ "            i = 2;\n" //
				+ "            System.out.println(i + E1.this.i);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n" //
				+ "class E2 {\n" //
				+ "    protected int i = 1;\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 extends E2 {\n" //
				+ "    private class E1Inner extends E2 {\n" //
				+ "        public E1Inner() {\n" //
				+ "            i = 2;\n" //
				+ "            System.out.println(i + E1.this.i);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n" //
				+ "class E2 {\n" //
				+ "    protected int i = 1;\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveQualifierBug219608() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 extends E2 {\n" //
				+ "    private int i = 1;\n" //
				+ "    private class E1Inner extends E2 {\n" //
				+ "        public E1Inner() {\n" //
				+ "            System.out.println(i + E1.this.i);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n" //
				+ "class E2 {\n" //
				+ "    private int i = 1;\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 extends E2 {\n" //
				+ "    private int i = 1;\n" //
				+ "    private class E1Inner extends E2 {\n" //
				+ "        public E1Inner() {\n" //
				+ "            System.out.println(i + i);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n" //
				+ "class E2 {\n" //
				+ "    private int i = 1;\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveQualifierBug330754() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class Test {\n" //
				+ "    String label = \"works\";\n" //
				+ "    class Nested extends Test {\n" //
				+ "        Nested() {\n" //
				+ "            label = \"broken\";\n" //
				+ "        }\n" //
				+ "        @Override\n" //
				+ "        public String toString() {\n" //
				+ "            return Test.this.label;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY);

		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testAddFinal01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    private int i= 0;\n" //
				+ "    public void foo(int j, int k) {\n" //
				+ "        int h, v;\n" //
				+ "        v= 0;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    private final int i= 0;\n" //
				+ "    public void foo(final int j, final int k) {\n" //
				+ "        final int h;\n" //
				+ "        int v;\n" //
				+ "        v= 0;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertGroupCategoryUsed(new ICompilationUnit[] { cu1 }, new String[] {
				FixMessages.VariableDeclarationFix_changeModifierOfUnknownToFinal_description
				});
		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testAddFinal02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    private Object obj1= new Object();\n" //
				+ "    protected Object obj2;\n" //
				+ "    Object obj3;\n" //
				+ "    public Object obj4;\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    private final Object obj1= new Object();\n" //
				+ "    protected Object obj2;\n" //
				+ "    Object obj3;\n" //
				+ "    public Object obj4;\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testAddFinal03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    private int i = 0;\n" //
				+ "    public void foo() throws Exception {\n" //
				+ "    }\n" //
				+ "    public void bar(int j) {\n" //
				+ "        int k;\n" //
				+ "        try {\n" //
				+ "            foo();\n" //
				+ "        } catch (Exception e) {\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    private int i = 0;\n" //
				+ "    public void foo() throws Exception {\n" //
				+ "    }\n" //
				+ "    public void bar(int j) {\n" //
				+ "        final int k;\n" //
				+ "        try {\n" //
				+ "            foo();\n" //
				+ "        } catch (final Exception e) {\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testAddFinal04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    private int i = 0;\n" //
				+ "    public void foo() throws Exception {\n" //
				+ "    }\n" //
				+ "    public void bar(int j) {\n" //
				+ "        int k;\n" //
				+ "        try {\n" //
				+ "            foo();\n" //
				+ "        } catch (Exception e) {\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    private int i = 0;\n" //
				+ "    public void foo() throws Exception {\n" //
				+ "    }\n" //
				+ "    public void bar(final int j) {\n" //
				+ "        int k;\n" //
				+ "        try {\n" //
				+ "            foo();\n" //
				+ "        } catch (Exception e) {\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testAddFinal05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        int i= 0;\n" //
				+ "        if (i > 1 || i == 1 && i > 1)\n" //
				+ "            ;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS);

		sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        final int i= 0;\n" //
				+ "        if ((i > 1) || ((i == 1) && (i > 1)))\n" //
				+ "            ;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testAddFinalBug129807() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public abstract class E {\n" //
				+ "    public interface I {\n" //
				+ "        void foo(int i);\n" //
				+ "    }\n" //
				+ "    public class IImpl implements I {\n" //
				+ "        public void foo(int i) {}\n" //
				+ "    }\n" //
				+ "    public abstract void bar(int i, String s);\n" //
				+ "    public void foobar(int i, int j) {}\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS);

		sample= "" //
				+ "package test;\n" //
				+ "public abstract class E {\n" //
				+ "    public interface I {\n" //
				+ "        void foo(int i);\n" //
				+ "    }\n" //
				+ "    public class IImpl implements I {\n" //
				+ "        public void foo(final int i) {}\n" //
				+ "    }\n" //
				+ "    public abstract void bar(int i, String s);\n" //
				+ "    public void foobar(final int i, final int j) {}\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testAddFinalBug134676_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E<T> { \n" //
				+ "    private String s;\n" //
				+ "    void setS(String s) {\n" //
				+ "        this.s = s;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testAddFinalBug134676_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E<T> { \n" //
				+ "    private String s= \"\";\n" //
				+ "    private T t;\n" //
				+ "    private T t2;\n" //
				+ "    public E(T t) {t2= t;}\n" //
				+ "    void setT(T t) {\n" //
				+ "        this.t = t;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		sample= "" //
				+ "package test;\n" //
				+ "public class E<T> { \n" //
				+ "    private final String s= \"\";\n" //
				+ "    private T t;\n" //
				+ "    private final T t2;\n" //
				+ "    public E(T t) {t2= t;}\n" //
				+ "    void setT(T t) {\n" //
				+ "        this.t = t;\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {sample});
	}

	//Changed test due to https://bugs.eclipse.org/bugs/show_bug.cgi?id=220124
	@Test
	public void testAddFinalBug145028() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private volatile int field= 0;\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	//https://bugs.eclipse.org/bugs/show_bug.cgi?id=294768
	@Test
	public void testAddFinalBug294768() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private transient int field= 0;\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testAddFinalBug157276_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private int field;\n" //
				+ "    public E1() {\n" //
				+ "        field= 10;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private final int field;\n" //
				+ "    public E1() {\n" //
				+ "        field= 10;\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {sample});
	}

	@Test
	public void testAddFinalBug157276_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private int field;\n" //
				+ "    public E1() {\n" //
				+ "        field= 10;\n" //
				+ "    }\n" //
				+ "    public E1(int f) {\n" //
				+ "        field= f;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private final int field;\n" //
				+ "    public E1() {\n" //
				+ "        field= 10;\n" //
				+ "    }\n" //
				+ "    public E1(int f) {\n" //
				+ "        field= f;\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {sample});
	}

	@Test
	public void testAddFinalBug157276_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private int field;\n" //
				+ "    public E1() {\n" //
				+ "        field= 10;\n" //
				+ "    }\n" //
				+ "    public E1(final int f) {\n" //
				+ "        field= f;\n" //
				+ "        field= 5;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testAddFinalBug157276_4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private int field;\n" //
				+ "    public E1() {\n" //
				+ "    }\n" //
				+ "    public E1(final int f) {\n" //
				+ "        field= f;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testAddFinalBug157276_5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private int field= 0;\n" //
				+ "    public E1() {\n" //
				+ "        field= 5;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testAddFinalBug157276_6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private int field;\n" //
				+ "    public E1() {\n" //
				+ "        if (false) field= 5;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testAddFinalBug157276_7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private static int field;\n" //
				+ "    public E1() {\n" //
				+ "        field= 5;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testAddFinalBug156842() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private int f0;\n" //
				+ "    private int f1= 0;\n" //
				+ "    private int f3;\n" //
				+ "    public E1() {\n" //
				+ "        f3= 0;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private int f0;\n" //
				+ "    private final int f1= 0;\n" //
				+ "    private final int f3;\n" //
				+ "    public E1() {\n" //
				+ "        f3= 0;\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {sample});
	}

	@Test
	public void testAddFinalBug158041_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(int[] ints) {\n" //
				+ "        for (int j = 0; j < ints.length; j++) {\n" //
				+ "            System.out.println(ints[j]);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);
		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(int[] ints) {\n" //
				+ "        for (final int i : ints) {\n" //
				+ "            System.out.println(i);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testAddFinalBug158041_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(int[] ints) {\n" //
				+ "        for (int j = 0; j < ints.length; j++) {\n" //
				+ "            int i = ints[j];\n" //
				+ "            System.out.println(i);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);
		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(int[] ints) {\n" //
				+ "        for (final int i : ints) {\n" //
				+ "            System.out.println(i);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testAddFinalBug158041_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(List<E1> es) {\n" //
				+ "        for (Iterator<E1> iterator = es.iterator(); iterator.hasNext();) {\n" //
				+ "            System.out.println(iterator.next());\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);
		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(List<E1> es) {\n" //
				+ "        for (final E1 e1 : es) {\n" //
				+ "            System.out.println(e1);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {sample});
	}

	@Test
	public void testAddFinalBug158041_4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(List<E1> es) {\n" //
				+ "        for (Iterator<E1> iterator = es.iterator(); iterator.hasNext();) {\n" //
				+ "            E1 e1 = iterator.next();\n" //
				+ "            System.out.println(e1);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);
		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(List<E1> es) {\n" //
				+ "        for (final E1 e1 : es) {\n" //
				+ "            System.out.println(e1);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testAddFinalBug163789() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private int i;\n" //
				+ "    public E1() {\n" //
				+ "        this(10);\n" //
				+ "        i = 10;\n" //
				+ "    }\n" //
				+ "    public E1(int j) {\n" //
				+ "        i = j;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private int i;\n" //
				+ "    public E1() {\n" //
				+ "        this(10);\n" //
				+ "        i = 10;\n" //
				+ "    }\n" //
				+ "    public E1(final int j) {\n" //
				+ "        i = j;\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {sample});
	}

	@Test
	public void testAddFinalBug191862() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E01 {\n" //
				+ "    @SuppressWarnings(\"unused\")\n" //
				+ "    @Deprecated\n" //
				+ "    private int x = 5, y= 10;\n" //
				+ "    \n" //
				+ "    private void foo() {\n" //
				+ "        @SuppressWarnings(\"unused\")\n" //
				+ "        @Deprecated\n" //
				+ "        int i= 10, j;\n" //
				+ "        j= 10;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E01 {\n" //
				+ "    @SuppressWarnings(\"unused\")\n" //
				+ "    @Deprecated\n" //
				+ "    private final int x = 5, y= 10;\n" //
				+ "    \n" //
				+ "    private void foo() {\n" //
				+ "        @SuppressWarnings(\"unused\")\n" //
				+ "        @Deprecated\n" //
				+ "        final\n" //
				+ "        int i= 10;\n" //
				+ "        @SuppressWarnings(\"unused\")\n" //
				+ "        @Deprecated\n" //
				+ "        int j;\n" //
				+ "        j= 10;\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {sample});
	}

	@Test
	public void testAddFinalBug213995() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private Object foo = new Object() {\n" //
				+ "        public boolean equals(Object obj) {\n" //
				+ "            return super.equals(obj);\n" //
				+ "        }\n" //
				+ "    }; \n" //
				+ "    public void foo() {\n" //
				+ "        Object foo = new Object() {\n" //
				+ "            public boolean equals(Object obj) {\n" //
				+ "                return super.equals(obj);\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private Object foo = new Object() {\n" //
				+ "        public boolean equals(final Object obj) {\n" //
				+ "            return super.equals(obj);\n" //
				+ "        }\n" //
				+ "    }; \n" //
				+ "    public void foo() {\n" //
				+ "        Object foo = new Object() {\n" //
				+ "            public boolean equals(final Object obj) {\n" //
				+ "                return super.equals(obj);\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testAddFinalBug272532() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private int field;\n" //
				+ "    public E1() {\n" //
				+ "        if (true)\n" //
				+ "            return;\n" //
				+ "        field= 5;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testAddFinalBug297566() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    private int x;\n" //
				+ "    public E1() {\n" //
				+ "        this();\n" //
				+ "    }\n" //
				+ "    public E1(int a) {\n" //
				+ "        x = a;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChangeEventWithError(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testAddFinalBug297566_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    private int x;\n" //
				+ "\n" //
				+ "    public E1() {\n" //
				+ "        this(10);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public E1(int a) {\n" //
				+ "        this();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public E1(int f, int y) {\n" //
				+ "        x = a;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChangeEventWithError(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testRemoveStringCreation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public String replaceNewString() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return new String(\"\");\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String replaceNewStringInMethodInvocation(String s, int i) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return new String(s + i).toLowerCase();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.NO_STRING_CREATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public String replaceNewString() {\n" //
				+ "        // Keep this comment\n" //
				+ "        return \"\";\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public String replaceNewStringInMethodInvocation(String s, int i) {\n" //
				+ "        // Keep this comment\n" //
				+ "        return (s + i).toLowerCase();\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { sample });
	}

	@Test
	public void testDoNotRemoveStringCreation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public String doNotReplaceNullableString(String s) {\n" //
				+ "        return new String(s);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.NO_STRING_CREATION);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testCheckSignOfBitwiseOperation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "public class Foo {\n" //
				+ "  private static final int CONSTANT = -1;\n" //
				+ "\n" //
				+ "  public int foo () {\n" //
				+ "    int i = 0;\n" //
				+ "    if (i & (CONSTANT | C2) > 0) {}\n" //
				+ "    if (0 < (i & (CONSTANT | C2))) {}\n" //
				+ "    return (1>>4 & CONSTANT) > 0;\n" //
				+ "  };\n" //
				+ "};\n";
		ICompilationUnit cu= pack1.createCompilationUnit("Foo.java", sample, false, null);
		sample= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "public class Foo {\n" //
				+ "  private static final int CONSTANT = -1;\n" //
				+ "\n" //
				+ "  public int foo () {\n" //
				+ "    int i = 0;\n" //
				+ "    if (i & (CONSTANT | C2) != 0) {}\n" //
				+ "    if (0 != (i & (CONSTANT | C2))) {}\n" //
				+ "    return (1>>4 & CONSTANT) != 0;\n" //
				+ "  };\n" //
				+ "};\n";
		String expected = sample;

		enable(CleanUpConstants.CHECK_SIGN_OF_BITWISE_OPERATION);

		assertGroupCategoryUsed(new ICompilationUnit[] { cu }, new String[] { MultiFixMessages.CheckSignOfBitwiseOperation_description });
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected });
	}

	@Test
	public void testKeepCheckSignOfBitwiseOperation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "public class Foo {\n" //
				+ "  private static final int CONSTANT = -1;\n" //
				+ "\n" //
				+ "  public void bar() {\n" //
				+ "    int i = 0;\n" //
				+ "    if (i > 0) {}\n" //
				+ "    if (i > 0 && (CONSTANT +1) > 0) {}\n" //
				+ "  };\n" //
				+ "};\n";
		String original= sample;
		ICompilationUnit cu= pack1.createCompilationUnit("Foo.java", original, false, null);

		enable(CleanUpConstants.CHECK_SIGN_OF_BITWISE_OPERATION);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testRemoveBlockReturnThrows01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public abstract class E {\n" //
				+ "    public void foo(Object obj) {\n" //
				+ "        if (obj == null) {\n" //
				+ "            throw new IllegalArgumentException();\n" //
				+ "        }\n" //
				+ "        \n" //
				+ "        if (obj.hashCode() > 0) {\n" //
				+ "            return;\n" //
				+ "        }\n" //
				+ "        \n" //
				+ "        if (obj.hashCode() < 0) {\n" //
				+ "            System.out.println(\"\");\n" //
				+ "            return;\n" //
				+ "        }\n" //
				+ "        \n" //
				+ "        if (obj.toString() != null) {\n" //
				+ "            System.out.println(obj.toString());\n" //
				+ "        } else {\n" //
				+ "            System.out.println(\"\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW);

		sample= "" //
				+ "package test;\n" //
				+ "public abstract class E {\n" //
				+ "    public void foo(Object obj) {\n" //
				+ "        if (obj == null)\n" //
				+ "            throw new IllegalArgumentException();\n" //
				+ "        \n" //
				+ "        if (obj.hashCode() > 0)\n" //
				+ "            return;\n" //
				+ "        \n" //
				+ "        if (obj.hashCode() < 0) {\n" //
				+ "            System.out.println(\"\");\n" //
				+ "            return;\n" //
				+ "        }\n" //
				+ "        \n" //
				+ "        if (obj.toString() != null) {\n" //
				+ "            System.out.println(obj.toString());\n" //
				+ "        } else {\n" //
				+ "            System.out.println(\"\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testRemoveTrailingWhitespace01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package    test1;     \n" //
				+ "   public class E1 {  \n" //
				+ "                   \n" //
				+ "}                  \n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		sample= "" //
				+ "package    test1;\n" //
				+ "public class E1 {\n" //
				+ "\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testRemoveTrailingWhitespace02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package    test1;     \n" //
				+ "   public class E1 {  \n" //
				+ "                   \n" //
				+ "}                  \n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_IGNORE_EMPTY);

		sample= "" //
				+ "package    test1;\n" //
				+ "   public class E1 {\n" //
				+ "                   \n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testRemoveTrailingWhitespaceBug173081() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "/**\n" //
				+ " * \n" //
				+ " *     \n" //
				+ " */\n" //
				+ "public class E1 { \n" //
				+ "    /**\n" //
				+ "     * \n" //
				+ "	 *     \n" //
				+ "     */\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_IGNORE_EMPTY);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		sample= "" //
				+ "package test1;\n" //
				+ "/**\n" //
				+ " * \n" //
				+ " * \n" //
				+ " */\n" //
				+ "public class E1 {\n" //
				+ "    /**\n" //
				+ "     * \n" //
				+ "     * \n" //
				+ "     */\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testSortMembers01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "   public class SM01 {\n" //
				+ "   int b;\n" //
				+ "   int a;\n" //
				+ "   void d() {};\n" //
				+ "   void c() {};\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("SM01.java", sample, false, null);

		enable(CleanUpConstants.SORT_MEMBERS);
		enable(CleanUpConstants.SORT_MEMBERS_ALL);

		sample= "" //
				+ "package test;\n" //
				+ "   public class SM01 {\n" //
				+ "   int a;\n" //
				+ "   int b;\n" //
				+ "   void c() {};\n" //
				+ "   void d() {};\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testSortMembers02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "   public class SM02 {\n" //
				+ "   int b;\n" //
				+ "   int a;\n" //
				+ "   void d() {};\n" //
				+ "   void c() {};\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("SM02.java", sample, false, null);

		enable(CleanUpConstants.SORT_MEMBERS);

		sample= "" //
				+ "package test;\n" //
				+ "   public class SM02 {\n" //
				+ "   int b;\n" //
				+ "   int a;\n" //
				+ "   void c() {};\n" //
				+ "   void d() {};\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testSortMembersBug218542() throws Exception {
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.APPEARANCE_ENABLE_VISIBILITY_SORT_ORDER, true);
		assertTrue(JavaPlugin.getDefault().getMemberOrderPreferenceCache().isSortByVisibility());

		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
			String sample= "" //
					+ "package test;\n" //
					+ "   public class SM02 {\n" //
					+ "   private int b;\n" //
					+ "   public int a;\n" //
					+ "   void d() {};\n" //
					+ "   void c() {};\n" //
					+ "}\n";
			ICompilationUnit cu1= pack1.createCompilationUnit("SM02.java", sample, false, null);

			enable(CleanUpConstants.SORT_MEMBERS);

			sample= "" //
					+ "package test;\n" //
					+ "   public class SM02 {\n" //
					+ "   private int b;\n" //
					+ "   public int a;\n" //
					+ "   void c() {};\n" //
					+ "   void d() {};\n" //
					+ "}\n";
			String expected1= sample;

			assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
		} finally {
			JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.APPEARANCE_ENABLE_VISIBILITY_SORT_ORDER, false);
		}
	}

	@Test
	public void testSortMembersBug223997() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class SM02 {\n" //
				+ "    public String s2;\n" //
				+ "    public static String s1;\n" //
				+ "   void d() {};\n" //
				+ "   void c() {};\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("SM02.java", sample, false, null);

		enable(CleanUpConstants.SORT_MEMBERS);

		sample= "" //
				+ "package test;\n" //
				+ "public class SM02 {\n" //
				+ "    public static String s1;\n" //
				+ "    public String s2;\n" //
				+ "   void c() {};\n" //
				+ "   void d() {};\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testSortMembersBug263173() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class SM263173 {\n" //
				+ "    static int someInt;\n" //
				+ "    static {\n" //
				+ "        someInt = 1;\n" //
				+ "    };\n" //
				+ "    static int anotherInt = someInt;\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("SM263173.java", sample, false, null);

		enable(CleanUpConstants.SORT_MEMBERS);

		sample= "" //
				+ "package test;\n" //
				+ "public class SM263173 {\n" //
				+ "    static int someInt;\n" //
				+ "    static {\n" //
				+ "        someInt = 1;\n" //
				+ "    };\n" //
				+ "    static int anotherInt = someInt;\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testSortMembersBug434941() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class A {\n" //
				+ "    public static final int CONSTANT = 5;\n" //
				+ "    public static void main(final String[] args) { }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.SORT_MEMBERS);
		enable(CleanUpConstants.SORT_MEMBERS_ALL);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });

		enable(CleanUpConstants.SORT_MEMBERS);
		disable(CleanUpConstants.SORT_MEMBERS_ALL);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testSortMembersMixedFields() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class A {\n" //
				+ "    public static final int B = 1;\n" //
				+ "    public final int A = 2;\n" //
				+ "    public static final int C = 3;\n" //
				+ "    public final int D = 4;\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.SORT_MEMBERS);
		disable(CleanUpConstants.SORT_MEMBERS_ALL);

		sample= "" //
				+ "package test;\n" //
				+ "public class A {\n" //
				+ "    public static final int B = 1;\n" //
				+ "    public static final int C = 3;\n" //
				+ "    public final int A = 2;\n" //
				+ "    public final int D = 4;\n" //
				+ "}\n";

		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testSortMembersMixedFieldsInterface() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public interface A {\n" //
				+ "    public static final int B = 1;\n" //
				+ "    public final int A = 2;\n" //
				+ "    public static final int C = 3;\n" //
				+ "    public final int D = 4;\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.SORT_MEMBERS);
		disable(CleanUpConstants.SORT_MEMBERS_ALL);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });

		enable(CleanUpConstants.SORT_MEMBERS_ALL);

		sample= "" //
				+ "package test;\n" //
				+ "public interface A {\n" //
				+ "    public final int A = 2;\n" //
				+ "    public static final int B = 1;\n" //
				+ "    public static final int C = 3;\n" //
				+ "    public final int D = 4;\n" //
				+ "}\n";

		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testSortMembersBug407759() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class A {\n" //
				+ "    void foo2() {}\n" //
				+ "    void foo1() {}\n" //
				+ "    static int someInt;\n" //
				+ "    static void fooStatic() {}\n" //
				+ "    static {\n" //
				+ "    	someInt = 1;\n" //
				+ "    }\n" //
				+ "    void foo3() {}\n" //
				+ "    static int anotherInt = someInt;\n" //
				+ "    void foo() {}\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.SORT_MEMBERS);
		disable(CleanUpConstants.SORT_MEMBERS_ALL);

		sample= "" //
				+ "package test;\n" //
				+ "public class A {\n" //
				+ "    static int someInt;\n" //
				+ "    static {\n" //
				+ "    	someInt = 1;\n" //
				+ "    }\n" //
				+ "    static int anotherInt = someInt;\n" //
				+ "    static void fooStatic() {}\n" //
				+ "    void foo() {}\n" //
				+ "    void foo1() {}\n" //
				+ "    void foo2() {}\n" //
				+ "    void foo3() {}\n" //
				+ "}\n";

		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });

		enable(CleanUpConstants.SORT_MEMBERS);
		enable(CleanUpConstants.SORT_MEMBERS_ALL);

		sample= "" //
				+ "package test;\n" //
				+ "public class A {\n" //
				+ "    static int anotherInt = someInt;\n" //
				+ "    static int someInt;\n" //
				+ "    static {\n" //
				+ "    	someInt = 1;\n" //
				+ "    }\n" //
				+ "    static void fooStatic() {}\n" //
				+ "    void foo() {}\n" //
				+ "    void foo1() {}\n" //
				+ "    void foo2() {}\n" //
				+ "    void foo3() {}\n" //
				+ "}\n";

		expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testSortMembersVisibility() throws Exception {
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.APPEARANCE_ENABLE_VISIBILITY_SORT_ORDER, true);
		JavaPlugin.getDefault().getPreferenceStore().setToDefault(PreferenceConstants.APPEARANCE_VISIBILITY_SORT_ORDER);

		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
			String sample= "" //
					+ "package test;\n" //
					+ "public class A {\n" //
					+ "    public final int B = 1;\n" //
					+ "    private static final int AA = 1;\n" //
					+ "    public static final int BB = 2;\n" //
					+ "    private final int A = 2;\n" //
					+ "    final int C = 3;\n" //
					+ "    protected static final int DD = 3;\n" //
					+ "    final static int CC = 4;\n" //
					+ "    protected final int D = 4;\n" //
					+ "}\n";
			ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

			enable(CleanUpConstants.SORT_MEMBERS);
			disable(CleanUpConstants.SORT_MEMBERS_ALL);

			sample= "" //
					+ "package test;\n" //
					+ "public class A {\n" //
					+ "    private static final int AA = 1;\n" //
					+ "    public static final int BB = 2;\n" //
					+ "    protected static final int DD = 3;\n" //
					+ "    final static int CC = 4;\n" //
					+ "    public final int B = 1;\n" //
					+ "    private final int A = 2;\n" //
					+ "    final int C = 3;\n" //
					+ "    protected final int D = 4;\n" //
					+ "}\n";

			String expected1= sample;

			assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });

			enable(CleanUpConstants.SORT_MEMBERS);
			enable(CleanUpConstants.SORT_MEMBERS_ALL);

			sample= "" //
					+ "package test;\n" //
					+ "public class A {\n" //
					+ "    public static final int BB = 2;\n" //
					+ "    private static final int AA = 1;\n" //
					+ "    protected static final int DD = 3;\n" //
					+ "    final static int CC = 4;\n" //
					+ "    public final int B = 1;\n" //
					+ "    private final int A = 2;\n" //
					+ "    protected final int D = 4;\n" //
					+ "    final int C = 3;\n" //
					+ "}\n";

			expected1= sample;

			assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
		} finally {
			JavaPlugin.getDefault().getPreferenceStore().setToDefault(PreferenceConstants.APPEARANCE_ENABLE_VISIBILITY_SORT_ORDER);
		}
	}

	@Test
	public void testOrganizeImports01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    A a;\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		sample= "" //
				+ "package test1;\n" //
				+ "public class A {}\n";
		pack2.createCompilationUnit("A.java", sample, false, null);

		IPackageFragment pack3= fSourceFolder.createPackageFragment("test2", false, null);
		sample= "" //
				+ "package test2;\n" //
				+ "public class A {}\n";
		pack3.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.ORGANIZE_IMPORTS);

		RefactoringStatus status= assertRefactoringHasNoChangeEventWithError(new ICompilationUnit[] {cu1});
		RefactoringStatusEntry[] entries= status.getEntries();
		assertEquals(1, entries.length);
		String message= entries[0].getMessage();
		assertTrue(message, entries[0].isInfo());
		assertTrue(message, message.contains("ambiguous"));
	}

	@Test
	public void testOrganizeImports02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public class E {\n" //
				+ "    Vect or v;\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.ORGANIZE_IMPORTS);

		RefactoringStatus status= assertRefactoringHasNoChangeEventWithError(new ICompilationUnit[] {cu1});
		RefactoringStatusEntry[] entries= status.getEntries();
		assertEquals(1, entries.length);
		String message= entries[0].getMessage();
		assertTrue(message, entries[0].isInfo());
		assertTrue(message, message.contains("parse"));
	}

	@Test
	public void testOrganizeImportsBug202266() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test2", false, null);
		String sample= "" //
				+ "package test2;\n" //
				+ "public class E2 {\n" //
				+ "}\n";
		pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test3", false, null);
		sample= "" //
				+ "package test3;\n" //
				+ "public class E2 {\n" //
				+ "}\n";
		pack2.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack3= fSourceFolder.createPackageFragment("test1", false, null);
		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    ArrayList foo;\n" //
				+ "    E2 foo2;\n" //
				+ "}\n";
		ICompilationUnit cu1= pack3.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.ORGANIZE_IMPORTS);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.ArrayList;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    ArrayList foo;\n" //
				+ "    E2 foo2;\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testOrganizeImportsBug229570() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public interface E1 {\n" //
				+ "  List<IEntity> getChildEntities();\n" //
				+ "  ArrayList<String> test;\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.ORGANIZE_IMPORTS);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.ArrayList;\n" //
				+ "import java.util.List;\n" //
				+ "\n" //
				+ "public interface E1 {\n" //
				+ "  List<IEntity> getChildEntities();\n" //
				+ "  ArrayList<String> test;\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testCorrectIndetation01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "/**\n" //
				+ "* \n" //
				+ "*/\n" //
				+ "package test1;\n" //
				+ "/**\n" //
				+ " * \n" //
				+ "* \n" //
				+ " */\n" //
				+ "        public class E1 {\n" //
				+ "    /**\n" //
				+ "         * \n" //
				+ " * \n" //
				+ "     */\n" //
				+ "            public void foo() {\n" //
				+ "            //a\n" //
				+ "        //b\n" //
				+ "            }\n" //
				+ "    /*\n" //
				+ "     *\n" //
				+ "           *\n" //
				+ "* \n" //
				+ "     */\n" //
				+ "        }\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		sample= "" //
				+ "/**\n" //
				+ " * \n" //
				+ " */\n" //
				+ "package test1;\n" //
				+ "/**\n" //
				+ " * \n" //
				+ " * \n" //
				+ " */\n" //
				+ "public class E1 {\n" //
				+ "    /**\n" //
				+ "     * \n" //
				+ "     * \n" //
				+ "     */\n" //
				+ "    public void foo() {\n" //
				+ "        //a\n" //
				+ "        //b\n" //
				+ "    }\n" //
				+ "    /*\n" //
				+ "     *\n" //
				+ "     *\n" //
				+ "     * \n" //
				+ "     */\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}

	@Test
	public void testCorrectIndetationBug202145_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "//  \n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);
		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL);

		sample= "" //
				+ "package test1;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        //\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testCorrectIndetationBug202145_2() throws Exception {
		IJavaProject project= getProject();
		project.setOption(DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_LINE_COMMENTS_ON_FIRST_COLUMN, DefaultCodeFormatterConstants.TRUE);
		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String sample= "" //
					+ "package test1;\n" //
					+ "public class E1 {\n" //
					+ "    public void foo() {\n" //
					+ "//  \n" //
					+ "    }\n" //
					+ "}\n";
			ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

			enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);
			enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
			enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL);

			sample= "" //
					+ "package test1;\n" //
					+ "public class E1 {\n" //
					+ "    public void foo() {\n" //
					+ "//\n" //
					+ "    }\n" //
					+ "}\n";
			String expected1= sample;

			assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
		} finally {
			project.setOption(DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_LINE_COMMENTS_ON_FIRST_COLUMN, DefaultCodeFormatterConstants.FALSE);
		}
	}

	@Test
	public void testUnimplementedCode01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public interface IFace {\n" //
				+ "    void foo();\n" //
				+ "    void bar();\n" //
				+ "}\n";
		pack1.createCompilationUnit("IFace.java", sample, false, null);

		sample= "" //
				+ "package test;\n" //
				+ "public class E01 implements IFace {\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E01.java", sample, false, null);

		sample= "" //
				+ "package test;\n" //
				+ "public class E02 implements IFace {\n" //
				+ "    public class Inner implements IFace {\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("E02.java", sample, false, null);

		enable(CleanUpConstants.ADD_MISSING_METHODES);

		sample= "" //
				+ "package test;\n" //
				+ "public class E01 implements IFace {\n" //
				+ "\n" //
				+ "    /* comment */\n" //
				+ "    public void foo() {\n" //
				+ "        //TODO\n" //
				+ "        \n" //
				+ "    }\n" //
				+ "\n" //
				+ "    /* comment */\n" //
				+ "    public void bar() {\n" //
				+ "        //TODO\n" //
				+ "        \n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		sample= "" //
				+ "package test;\n" //
				+ "public class E02 implements IFace {\n" //
				+ "    public class Inner implements IFace {\n" //
				+ "\n" //
				+ "        /* comment */\n" //
				+ "        public void foo() {\n" //
				+ "            //TODO\n" //
				+ "            \n" //
				+ "        }\n" //
				+ "\n" //
				+ "        /* comment */\n" //
				+ "        public void bar() {\n" //
				+ "            //TODO\n" //
				+ "            \n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    /* comment */\n" //
				+ "    public void foo() {\n" //
				+ "        //TODO\n" //
				+ "        \n" //
				+ "    }\n" //
				+ "\n" //
				+ "    /* comment */\n" //
				+ "    public void bar() {\n" //
				+ "        //TODO\n" //
				+ "        \n" //
				+ "    }\n" //
				+ "}\n";
		String expected2= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1, cu2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testUnimplementedCode02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "public interface IFace {\n" //
				+ "    void foo();\n" //
				+ "}\n";
		pack1.createCompilationUnit("IFace.java", sample, false, null);

		sample= "" //
				+ "package test;\n" //
				+ "public class E01 implements IFace {\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E01.java", sample, false, null);

		sample= "" //
				+ "package test;\n" //
				+ "public class E02 implements IFace {\n" //
				+ "    \n" //
				+ "    public class Inner implements IFace {\n" //
				+ "        \n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("E02.java", sample, false, null);

		enable(UnimplementedCodeCleanUp.MAKE_TYPE_ABSTRACT);

		sample= "" //
				+ "package test;\n" //
				+ "public abstract class E01 implements IFace {\n" //
				+ "}\n";
		String expected1= sample;

		sample= "" //
				+ "package test;\n" //
				+ "public abstract class E02 implements IFace {\n" //
				+ "    \n" //
				+ "    public abstract class Inner implements IFace {\n" //
				+ "        \n" //
				+ "    }\n" //
				+ "}\n";
		String expected2= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1, cu2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testRemoveRedundantModifiers () throws Exception {
		StringBuffer buf;
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public abstract interface IFoo {\n");
		buf.append("  public static final int MAGIC_NUMBER = 646;\n");
		buf.append("  public abstract int foo ();\n");
		buf.append("  abstract void func ();\n");
		buf.append("  public int bar (int bazz);\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("IFoo.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public interface IFoo {\n");
		buf.append("  int MAGIC_NUMBER = 646;\n");
		buf.append("  int foo ();\n");
		buf.append("  void func ();\n");
		buf.append("  int bar (int bazz);\n");
		buf.append("}\n");
		String expected1 = buf.toString();

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public final class Sealed {\n");
		buf.append("  public final void foo () {};\n");
		buf.append("  \n");
		buf.append("  abstract static interface INested {\n");
		buf.append("  }\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("Sealed.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public final class Sealed {\n");
		buf.append("  public void foo () {};\n");
		buf.append("  \n");
		buf.append("  interface INested {\n");
		buf.append("  }\n");
		buf.append("}\n");
		String expected2 = buf.toString();

		// Anonymous class within an interface:
		// public keyword must not be removed (see bug#536612)
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public interface X {\n");
		buf.append("  void B();\n");
		buf.append("  void A();\n");
		buf.append("  default X y() {\n");
		buf.append("    return new X() {\n");
		buf.append("      @Override public void A() {}\n");
		buf.append("      @Override public void B() {}\n");
		buf.append("    };\n");
		buf.append("  }\n");
		buf.append("}\n");
		String expected3 = buf.toString();
		ICompilationUnit cu3= pack1.createCompilationUnit("AnonymousNestedInInterface.java", buf.toString(), false, null);

		String input4= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "public enum SampleEnum {\n" //
				+ "  VALUE1(\"1\"), VALUE2(\"2\");\n" //
				+ "\n" //
				+ "  private SampleEnum(String string) {}\n" //
				+ "}\n";
		ICompilationUnit cu4= pack1.createCompilationUnit("SampleEnum.java", input4, false, null);

		String expected4= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "public enum SampleEnum {\n" //
				+ "  VALUE1(\"1\"), VALUE2(\"2\");\n" //
				+ "\n" //
				+ "  SampleEnum(String string) {}\n" //
				+ "}\n";

		// public modifier must not be removed from enum methods
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public interface A {\n");
		buf.append("  public static enum B {\n");
		buf.append("    public static void method () { }\n");
		buf.append("  }\n");
		buf.append("}\n");
		ICompilationUnit cu5= pack1.createCompilationUnit("NestedEnum.java", buf.toString(), false, null);
		// https://docs.oracle.com/javase/specs/jls/se8/html/jls-8.html#jls-8.9
		// nested enum type is implicitly static
		// Bug#538459 'public' modified must not be removed from static method in nested enum
		String expected5 = buf.toString().replace("static enum", "enum");

		// Bug#551038: final keyword must not be removed from method with varargs
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public final class SafeVarargsExample {\n");
		buf.append("  @SafeVarargs\n");
		buf.append("  public final void errorRemoveRedundantModifiers(final String... input) {\n");
		buf.append("  }\n");
		buf.append("}\n");
		String expected6 = buf.toString();
		ICompilationUnit cu6= pack1.createCompilationUnit("SafeVarargsExample.java", buf.toString(), false, null);

		// Bug#553608: modifiers public static final must not be removed from inner enum within interface
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public interface Foo {\n");
		buf.append("  enum Bar {\n");
		buf.append("    A;\n");
		buf.append("    public static final int B = 0;\n");
		buf.append("  }\n");
		buf.append("}\n");
		String expected7 = buf.toString();
		ICompilationUnit cu7= pack1.createCompilationUnit("NestedEnumExample.java", buf.toString(), false, null);

		enable(CleanUpConstants.REMOVE_REDUNDANT_MODIFIERS);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1, cu2, cu3, cu4, cu5, cu6, cu7 }, new String[] { expected1, expected2, expected3, expected4, expected5, expected6, expected7 });

	}

	@Test
	public void testDoNotRemoveModifiers() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public enum SampleEnum {\n" //$NON-NLS-1$
				+ "  VALUE1, VALUE2;\n" //$NON-NLS-1$
				+ "\n" //
				+ "  private void notAConstructor(String string) {}\n" //$NON-NLS-1$
				+ "}\n"; //$NON-NLS-1$
		ICompilationUnit cu= pack.createCompilationUnit("SampleEnum.java", sample, false, null);

		// When
		enable(CleanUpConstants.REMOVE_REDUNDANT_MODIFIERS);

		// Then
		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testDoNotTouchCleanedModifiers() throws Exception {
		// Given
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public interface ICleanInterface {\n" //
				+ "  int MAGIC_NUMBER = 646;\n" //
				+ "  int foo();\n" //
				+ "  void func();\n" //
				+ "  int bar(int bazz);\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("ICleanInterface.java", sample, false, null);

		// When
		enable(CleanUpConstants.REMOVE_REDUNDANT_MODIFIERS);

		// Then
		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });

		// When
		ASTParser parser= ASTParser.newParser(AST.JLS15);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(cu1);
		parser.setResolveBindings(true);
		CompilationUnit unit= (CompilationUnit) parser.createAST(null);
		Map<String, String> options= new HashMap<>();
		options.put(CleanUpConstants.REMOVE_REDUNDANT_MODIFIERS, CleanUpOptionsCore.TRUE);
		NoChangeRedundantModifiersCleanUp cleanup= new NoChangeRedundantModifiersCleanUp(options);
		ICleanUpFix fix= cleanup.createFix(unit);

		// Then
		assertNull("ICleanInterface should not be cleaned up", fix);
	}

	@Test
	public void testRemoveRedundantSemicolons () throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //

		// Ensure various extra semi-colons are removed and required ones are left intact.
		// This includes a lambda expression.
				+ "package test; ;\n" //
				+ "enum cars { sedan, coupe };\n" //
				+ "public class Foo {\n" //
				+ "  int add(int a, int b) {return a+b;};\n" //
				+ "  int a= 3;; ;\n" //
				+ "  int b= 7; // leave this ; alone\n" //
				+ "  int c= 10; /* and this ; too */\n" //
				+ "  public int foo () {\n" //
				+ "    ;\n" //
				+ "    Runnable r = () -> {\n" //
				+ "      System.out.println(\"running\");\n" //
				+ "    };;\n" //
				+ "    for (;;)\n" //
				+ "      ;;\n" //
				+ "      ;\n" //
				+ "    while (a++ < 1000) ;\n" //
				+ "  };\n" //
				+ "};\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("Foo.java", sample, false, null);

		// Ensure semi-colon after lambda expression remains intact.
		sample= "" //
				+ "package test;\n" //
				+ "enum cars { sedan, coupe }\n" //
				+ "public class Foo {\n" //
				+ "  int add(int a, int b) {return a+b;}\n" //
				+ "  int a= 3;\n" //
				+ "  int b= 7; // leave this ; alone\n" //
				+ "  int c= 10; /* and this ; too */\n" //
				+ "  public int foo () {\n" //
				+ "    \n" //
				+ "    Runnable r = () -> {\n" //
				+ "      System.out.println(\"running\");\n" //
				+ "    };\n" //
				+ "    for (;;)\n" //
				+ "      ;\n" //
				+ "      \n" //
				+ "    while (a++ < 1000) ;\n" //
				+ "  }\n" //
				+ "}\n";
		String expected1 = sample;

		enable(CleanUpConstants.REMOVE_REDUNDANT_SEMICOLONS);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });

	}

	@Test
	public void testBug491087() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "interface A {\n" //
				+ "    class B {\n" //
				+ "        String field;\n" //
				+ "       B() { field = \"foo\"; }\n" //
				+ "    }\n" //
				+ "}\n" //
				+ "class C {\n" //
				+ "    class D {\n" //
				+ "       String field;\n" //
				+ "       D() { field = \"bar\"; }\n" //
				+ "    }\n" //
				+ "}\n";

		ICompilationUnit cu1= pack1.createCompilationUnit("C.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);

		sample= "" //
				+ "package test1;\n" //
				+ "interface A {\n" //
				+ "    class B {\n" //
				+ "        String field;\n" //
				+ "       B() { this.field = \"foo\"; }\n" //
				+ "    }\n" //
				+ "}\n" //
				+ "class C {\n" //
				+ "    class D {\n" //
				+ "       String field;\n" //
				+ "       D() { this.field = \"bar\"; }\n" //
				+ "    }\n" //
				+ "}\n";

		String expected1= sample;

		assertGroupCategoryUsed(new ICompilationUnit[] { cu1 }, new String[] {
				Messages.format(FixMessages.CodeStyleFix_QualifyWithThis_description, new Object[] {"field", "this"})
		});
		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}
}

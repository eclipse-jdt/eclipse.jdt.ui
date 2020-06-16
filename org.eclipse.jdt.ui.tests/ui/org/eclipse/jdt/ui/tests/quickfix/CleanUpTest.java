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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.CoreException;
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
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.core.manipulation.CleanUpOptionsCore;

import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.fix.Java50CleanUp;
import org.eclipse.jdt.internal.ui.fix.RedundantModifiersCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnimplementedCodeCleanUp;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

@RunWith(JUnit4.class)
public class CleanUpTest extends CleanUpTestCase {

	@Rule
    public ProjectTestSetup projectsetup = new ProjectTestSetup();

	private class NoChangeRedundantModifiersCleanUp extends RedundantModifiersCleanUp {
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
	public void testUnusedCodeBug335173_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();

		buf.append("package test1;\n");
		buf.append("import java.util.Comparator;\n");
		buf.append("\n");
		buf.append("class IntComp implements Comparator<Integer> {\n");
		buf.append("    public int compare(Integer o1, Integer o2) {\n");
		buf.append("        return ((Integer) o1).intValue() - ((Integer) o2).intValue();\n");
		buf.append("    }\n");
		buf.append("}\n");

		ICompilationUnit cu1= pack1.createCompilationUnit("IntComp.java", buf.toString(), false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Comparator;\n");
		buf.append("\n");
		buf.append("class IntComp implements Comparator<Integer> {\n");
		buf.append("    public int compare(Integer o1, Integer o2) {\n");
		buf.append("        return o1.intValue() - o2.intValue();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testUnusedCodeBug335173_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(Integer n) {\n");
		buf.append("        int i = (((Integer) n)).intValue();\n");
		buf.append("        foo(((Integer) n));\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(Integer n) {\n");
		buf.append("        int i = ((n)).intValue();\n");
		buf.append("        foo((n));\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testUnusedCodeBug335173_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(Integer n) {\n");
		buf.append("        int i = ((Integer) (n)).intValue();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(Integer n) {\n");
		buf.append("        int i = (n).intValue();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testUnusedCodeBug371078_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();

		buf.append("package test1;\n");
		buf.append("class E1 {\n");
		buf.append("    public static Object create(final int a, final int b) {\n");
		buf.append("        return (Double) ((double) (a * Math.pow(10, -b)));\n");
		buf.append("    }\n");
		buf.append("}\n");

		ICompilationUnit cu1= pack1.createCompilationUnit("IntComp.java", buf.toString(), false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("class E1 {\n");
		buf.append("    public static Object create(final int a, final int b) {\n");
		buf.append("        return (a * Math.pow(10, -b));\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testUnusedCodeBug371078_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();

		buf.append("package test1;\n");
		buf.append("public class NestedCasts {\n");
		buf.append("	void foo(Integer i) {\n");
		buf.append("		Object o= ((((Number) (((Integer) i)))));\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("NestedCasts.java", buf.toString(), false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class NestedCasts {\n");
		buf.append("	void foo(Integer i) {\n");
		buf.append("		Object o= (((((i)))));\n");
		buf.append("	}\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
	public void testJava50Bug222257() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
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

		HashMap<String, String> map= new HashMap<>();
		map.put(CleanUpConstants.VARIABLE_DECLARATION_USE_TYPE_ARGUMENTS_FOR_RAW_TYPE_REFERENCES, CleanUpOptions.TRUE);
		Java50CleanUp cleanUp= new Java50CleanUp(map);

		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setResolveBindings(true);
		parser.setProject(fJProject1);

		Map<String, String> options= RefactoringASTParser.getCompilerOptions(fJProject1);
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
		assertTrue(problems.length == 2);
		for (IProblem problem : problems) {
			ProblemLocation location= new ProblemLocation(problem);
			assertTrue(cleanUp.canFix(cu1, location));
		}
	}

	@Test
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

	@Test
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

	@Test
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
			StringBuffer buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("interface I {\n");
			for (count= 0; count < PROBLEMS_COUNT; count++)
				buf.append("    void m" + count + "();\n");
			buf.append("}\n");
			buf.append("class X implements I {\n");
			for (count= 0; count < PROBLEMS_COUNT; count++)
				buf.append("    public void m" + count + "() {} // @Override error in 1.5, not in 1.6\n");
			buf.append("}\n");
			ICompilationUnit cu1= pack1.createCompilationUnit("I.java", buf.toString(), false, null);

			enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
			enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);
			enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("interface I {\n");
			for (count= 0; count < PROBLEMS_COUNT; count++)
				buf.append("    void m" + count + "();\n");
			buf.append("}\n");
			buf.append("class X implements I {\n");
			for (count= 0; count < PROBLEMS_COUNT; count++) {
				buf.append("    @Override\n");
				buf.append("    public void m" + count + "() {} // @Override error in 1.5, not in 1.6\n");
			}
			buf.append("}\n");
			String expected1= buf.toString();

			assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
		} finally {
			JavaProjectHelper.delete(project);
		}
	}

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
	public void testCodeStyle06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
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

	@Test
	public void testCodeStyle07() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
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

	@Test
	public void testCodeStyle08() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
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

	@Test
	public void testCodeStyle09() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
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

	@Test
	public void testCodeStyle10() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
	public void testCodeStyleBug346230() throws Exception {
		IJavaProject project= JavaProjectHelper.createJavaProject("CleanUpTestProject", "bin");
		try {
			JavaProjectHelper.addRTJar16(project);
			IPackageFragmentRoot src= JavaProjectHelper.addSourceContainer(project, "src");
			IPackageFragment pack1= src.createPackageFragment("test1", false, null);

			StringBuffer buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("interface CinematicEvent {\n");
			buf.append("    public void stop();\n");
			buf.append("    public boolean internalUpdate();\n");
			buf.append("}\n");
			ICompilationUnit cu1= pack1.createCompilationUnit("CinematicEvent.java", buf.toString(), false, null);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("abstract class E1 implements CinematicEvent {\n");
			buf.append("\n");
			buf.append("    protected PlayState playState = PlayState.Stopped;\n");
			buf.append("    protected LoopMode loopMode = LoopMode.DontLoop;\n");
			buf.append("\n");
			buf.append("    public boolean internalUpdate() {\n");
			buf.append("        return loopMode == loopMode.DontLoop;\n");
			buf.append("    }\n");
			buf.append("\n");
			buf.append("    public void stop() {\n");
			buf.append("    }\n");
			buf.append("\n");
			buf.append("    public void read() {\n");
			buf.append("        Object ic= new Object();\n");
			buf.append("        playState.toString();\n");
			buf.append("    }\n");
			buf.append("\n");
			buf.append("    enum PlayState {\n");
			buf.append("        Stopped\n");
			buf.append("    }\n");
			buf.append("    enum LoopMode {\n");
			buf.append("        DontLoop\n");
			buf.append("    }\n");
			buf.append("}\n");
			ICompilationUnit cu2= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

			enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
			enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);
			enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
			enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);
			enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
			enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);
			enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
			enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);
			enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("abstract class E1 implements CinematicEvent {\n");
			buf.append("\n");
			buf.append("    protected PlayState playState = PlayState.Stopped;\n");
			buf.append("    protected LoopMode loopMode = LoopMode.DontLoop;\n");
			buf.append("\n");
			buf.append("    @Override\n");
			buf.append("    public boolean internalUpdate() {\n");
			buf.append("        return this.loopMode == LoopMode.DontLoop;\n");
			buf.append("    }\n");
			buf.append("\n");
			buf.append("    @Override\n");
			buf.append("    public void stop() {\n");
			buf.append("    }\n");
			buf.append("\n");
			buf.append("    public void read() {\n");
			buf.append("        final Object ic= new Object();\n");
			buf.append("        this.playState.toString();\n");
			buf.append("    }\n");
			buf.append("\n");
			buf.append("    enum PlayState {\n");
			buf.append("        Stopped\n");
			buf.append("    }\n");
			buf.append("    enum LoopMode {\n");
			buf.append("        DontLoop\n");
			buf.append("    }\n");
			buf.append("}\n");
			String expected1= buf.toString();

			assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1, cu2 }, new String[] { cu1.getBuffer().getContents(), expected1 });
		} finally {
			JavaProjectHelper.delete(project);
		}

	}

	@Test
	public void testCodeStyle_StaticAccessThroughInstance_Bug307407() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private final String localString = new MyClass().getMyString();\n");
		buf.append("    public static class MyClass {\n");
		buf.append("        public static String getMyString() {\n");
		buf.append("            return \"a\";\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
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
		buf.append("        E1.foo(null);\n");
		buf.append("        E1.foo(E1.bar());\n");
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

	@Test
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

	@Test
	public void testChangeNonstaticAccessToStatic_Bug439733() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("class Singleton {\n");
		buf.append("    public static String name = \"The Singleton\";\n");
		buf.append("    public static Singleton instance = new Singleton();\n");
		buf.append("    public static Singleton getInstance() {\n");
		buf.append("        return instance;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class E1 {\n");
		buf.append("    public static void main(String[] args) {\n");
		buf.append("        System.out.println(Singleton.instance.name);\n");
		buf.append("        System.out.println(Singleton.getInstance().name);\n");
		buf.append("        System.out.println(Singleton.getInstance().getInstance().name);\n");
		buf.append("        System.out.println(new Singleton().name);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS);
		enable(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("class Singleton {\n");
		buf.append("    public static String name = \"The Singleton\";\n");
		buf.append("    public static Singleton instance = new Singleton();\n");
		buf.append("    public static Singleton getInstance() {\n");
		buf.append("        return instance;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class E1 {\n");
		buf.append("    public static void main(String[] args) {\n");
		buf.append("        System.out.println(Singleton.name);\n");
		buf.append("        Singleton.getInstance();\n");
		buf.append("        System.out.println(Singleton.name);\n");
		buf.append("        Singleton.getInstance();\n");
		buf.append("        Singleton.getInstance();\n");
		buf.append("        System.out.println(Singleton.name);\n");
		buf.append("        new Singleton();\n");
		buf.append("        System.out.println(Singleton.name);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

		Hashtable<String, String> options= TestOptions.getDefaultOptions();

		Map<String, String> formatterSettings= DefaultCodeFormatterConstants.getEclipseDefaultSettings();
		formatterSettings.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_COUNT_LINE_LENGTH_FROM_STARTING_POSITION,
				DefaultCodeFormatterConstants.FALSE);
		options.putAll(formatterSettings);

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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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
				+ "        } else if (i == 1) {\n" //
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
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "        } else if (a == 1) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "        } else {\n" //
				+ "            // Keep this comment also\n" //
				+ "            System.out.println(\"Different\");\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        if (b == 0) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "        } else if (b == 1) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
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
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "        } else if (j == 1) {\n" //
				+ "            // Keep this comment also\n" //
				+ "            System.out.println(\"Different\");\n" //
				+ "        } else {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    /** Duplicate if and else if code, merge it */\n" //
				+ "    public void duplicateIfAndElseIfWithoutElse(int k) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (k == 0) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "        } else if (k == 1) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
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
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "        } else if (m == 2) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
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
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "        else if (n == 1)\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
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
				+ "    public void longIfAndElseIf(int q) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (q == 0) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "            System.out.println(\"code\");\n" //
				+ "        } else if (q == 1) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "            System.out.println(\"code\");\n" //
				+ "        } else {\n" //
				+ "            // Keep this comment also\n" //
				+ "            System.out.println(\"Different\");\n" //
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
				+ "        if ((i == 0) || (i == 1)) {\n" //
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
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "        } else {\n" //
				+ "            // Keep this comment also\n" //
				+ "            System.out.println(\"Different\");\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        // Keep this comment\n" //
				+ "        if ((b == 0) || (b == 1)) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "        } else {\n" //
				+ "            // Keep this comment also\n" //
				+ "            System.out.println(\"Different\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    /** Duplicate if and else code, merge it */\n" //
				+ "    public void duplicateIfAndElse(int j) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if ((j == 0) || !(j == 1)) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
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
				+ "            System.out.println(\"Duplicate\");\n" //
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
				+ "            System.out.println(\"Duplicate\");\n" //
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
				+ "            System.out.println(\"Duplicate\");\n" //
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
				+ "    public void longIfAndElseIf(int q) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if ((q == 0) || (q == 1)) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            System.out.println(\"Duplicate\");\n" //
				+ "            System.out.println(\"code\");\n" //
				+ "        } else {\n" //
				+ "            // Keep this comment also\n" //
				+ "            System.out.println(\"Different\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";

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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
	public void testAddParentheses01() throws Exception {

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

	@Test
	public void testAddParentheses02() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=331845
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(int i, int j) {\n");
		buf.append("        if (i + 10 != j - 5)\n");
		buf.append("            System.out.println(i - j + 10 - i * j);\n");
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
		buf.append("    void foo(int i, int j) {\n");
		buf.append("        if ((i + 10) != (j - 5)) {\n");
		buf.append("            System.out.println(((i - j) + 10) - (i * j));\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParentheses01() throws Exception {

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

	@Test
	public void testRemoveParenthesesBug134739() throws Exception {

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

	@Test
	public void testRemoveParenthesesBug134741_1() throws Exception {

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

	@Test
	public void testRemoveParenthesesBug134741_2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
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

	@Test
	public void testRemoveParenthesesBug134741_3() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
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

	@Test
	public void testRemoveParenthesesBug134985_1() throws Exception {

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

	@Test
	public void testRemoveParenthesesBug134985_2() throws Exception {

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

	@Test
	public void testRemoveParenthesesBug188207() throws Exception {

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

	@Test
	public void testRemoveParenthesesBug208752() throws Exception {

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
		buf.append("        double d = 2.0 * (0.5 / 4.0);\n");
		buf.append("        int spaceCount = 3;\n");
		buf.append("        spaceCount = 2 * (spaceCount / 2);\n");
		buf.append("    }\n");
		buf.append("}\n");

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { buf.toString() });
	}

	@Test
	public void testRemoveParenthesesBug190188() throws Exception {

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

	@Test
	public void testRemoveParenthesesBug212856() throws Exception {

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

	@Test
	public void testRemoveParenthesesBug335173_1() throws Exception {
		//while loop's expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(boolean a) {\n");
		buf.append("        while (((a))) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void bar(int x) {\n");
		buf.append("        while ((x > 2)) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(boolean a) {\n");
		buf.append("        while (a) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void bar(int x) {\n");
		buf.append("        while (x > 2) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_2() throws Exception {
		//do while loop's expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        do {\n");
		buf.append("        } while ((x > 2));\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        do {\n");
		buf.append("        } while (x > 2);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_3() throws Exception {
		//for loop's expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        for (int x = 0; (x > 2); x++) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        for (int x = 0; x > 2; x++) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_4() throws Exception {
		//switch statement expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        switch ((x - 2)) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        switch (x - 2) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_5() throws Exception {
		//switch case expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        switch (x) {\n");
		buf.append("        case (1 + 2):\n");
		buf.append("            break;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        switch (x) {\n");
		buf.append("        case 1 + 2:\n");
		buf.append("            break;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_6() throws Exception {
		//throw statement expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int type) throws Exception {\n");
		buf.append("        throw (type == 1 ? new IllegalArgumentException() : new Exception());\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int type) throws Exception {\n");
		buf.append("        throw type == 1 ? new IllegalArgumentException() : new Exception();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_7() throws Exception {
		//synchronized statement expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private static final Object OBJECT = new Object();\n");
		buf.append("    private static final String STRING = new String();\n");
		buf.append("    \n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        synchronized ((x == 1 ? STRING : OBJECT)) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private static final Object OBJECT = new Object();\n");
		buf.append("    private static final String STRING = new String();\n");
		buf.append("    \n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        synchronized (x == 1 ? STRING : OBJECT) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_8() throws Exception {
		//assert statement expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        assert (x > 2);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        assert x > 2;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_9() throws Exception {
		//assert statement message expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        assert x > 2 : (x - 2);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        assert x > 2 : x - 2;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_10() throws Exception {
		//array access index expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int a[], int x) {\n");
		buf.append("        int i = a[(x + 2)];\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int a[], int x) {\n");
		buf.append("        int i = a[x + 2];\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_11() throws Exception {
		//conditional expression's then expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        int i = x > 10 ? (x > 5 ? x - 1 : x - 2): x;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        int i = x > 10 ? x > 5 ? x - 1 : x - 2: x;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_12() throws Exception {
		//conditional expression's else expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        int i = x > 10 ? x: (x > 5 ? x - 1 : x - 2);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        int i = x > 10 ? x: x > 5 ? x - 1 : x - 2;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_13() throws Exception {
		//conditional expression's then expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        int i = x > 10 ? (x = x - 2): x;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        int i = x > 10 ? (x = x - 2): x;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_14() throws Exception {
		//conditional expression's else expression
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        int i = x > 10 ? x: (x = x - 2);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        int i = x > 10 ? x: (x = x - 2);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_15() throws Exception {
		//shift operators
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        int m= (x >> 2) >> 1;\n");
		buf.append("        m= x >> (2 >> 1);\n");
		buf.append("        int n= (x << 2) << 1;\n");
		buf.append("        n= x << (2 << 1);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        int m= x >> 2 >> 1;\n");
		buf.append("        m= x >> (2 >> 1);\n");
		buf.append("        int n= x << 2 << 1;\n");
		buf.append("        n= x << (2 << 1);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_16() throws Exception {
		//integer multiplication
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int x, long y) {\n");
		buf.append("        int m= (4 * x) * 2;\n");
		buf.append("        int n= 4 * (x * 2);\n");
		buf.append("        int p= 4 * (x % 3);\n");
		buf.append("        int q= 4 * (x / 3);\n");
		buf.append("        int r= 4 * (x * y);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int x, long y) {\n");
		buf.append("        int m= 4 * x * 2;\n");
		buf.append("        int n= 4 * x * 2;\n");
		buf.append("        int p= 4 * (x % 3);\n");
		buf.append("        int q= 4 * (x / 3);\n");
		buf.append("        int r= 4 * (x * y);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_17() throws Exception {
		//floating point multiplication
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(double x) {\n");
		buf.append("        int m= (4.0 * x) * 0.5;\n");
		buf.append("        int n= 4.0 * (x * 0.5);\n");
		buf.append("        int p= 4.0 * (x / 100);\n");
		buf.append("        int q= 4.0 * (x % 3);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(double x) {\n");
		buf.append("        int m= 4.0 * x * 0.5;\n");
		buf.append("        int n= 4.0 * (x * 0.5);\n");
		buf.append("        int p= 4.0 * (x / 100);\n");
		buf.append("        int q= 4.0 * (x % 3);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_18() throws Exception {
		//integer addition
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int x, long y) {\n");
		buf.append("        int m= (4 + x) + 2;\n");
		buf.append("        int n= 4 + (x + 2);\n");
		buf.append("        int p= 4 + (x + y);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int x, long y) {\n");
		buf.append("        int m= 4 + x + 2;\n");
		buf.append("        int n= 4 + x + 2;\n");
		buf.append("        int p= 4 + (x + y);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_19() throws Exception {
		//floating point addition
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(double x) {\n");
		buf.append("        int m= (4.0 + x) + 100.0;\n");
		buf.append("        int n= 4.0 + (x + 100.0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(double x) {\n");
		buf.append("        int m= 4.0 + x + 100.0;\n");
		buf.append("        int n= 4.0 + (x + 100.0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug335173_20() throws Exception {
		//string concatenation
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String s, String t, String u) {\n");
		buf.append("        String a= (s + t) + u;\n");
		buf.append("        String b= s + (t + u);\n");
		buf.append("        String c= (1 + 2) + s;\n");
		buf.append("        String d= 1 + (2 + s);\n");
		buf.append("        String e= s + (1 + 2);\n");
		buf.append("        String f= (s + 1) + 2;\n");
		buf.append("        String g= (1 + s) + 2;\n");
		buf.append("        String h= 1 + (s + 2);\n");
		buf.append("        String i= s + (1 + t);\n");
		buf.append("        String j= s + (t + 1);\n");
		buf.append("        String k= s + (1 - 2);\n");
		buf.append("        String l= s + (1 * 2);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String s, String t, String u) {\n");
		buf.append("        String a= s + t + u;\n");
		buf.append("        String b= s + t + u;\n");
		buf.append("        String c= 1 + 2 + s;\n");
		buf.append("        String d= 1 + (2 + s);\n");
		buf.append("        String e= s + (1 + 2);\n");
		buf.append("        String f= s + 1 + 2;\n");
		buf.append("        String g= 1 + s + 2;\n");
		buf.append("        String h= 1 + s + 2;\n");
		buf.append("        String i= s + 1 + t;\n");
		buf.append("        String j= s + t + 1;\n");
		buf.append("        String k= s + (1 - 2);\n");
		buf.append("        String l= s + 1 * 2;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug405096_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    final Short cache[] = new Short[-(-128) + 1];\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testRemoveParenthesesBug405096_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    int a= 10\n");
		buf.append("    final Short cache[] = new Short[-(-a) + 1];\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testRemoveParenthesesBug405096_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    int a= 10\n");
		buf.append("    final Short cache[] = new Short[-(--a) + 1];\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testRemoveParenthesesBug405096_4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    int a= 10\n");
		buf.append("    final Short cache[] = new Short[+(+a) + 1];\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testRemoveParenthesesBug405096_5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    int a= 10\n");
		buf.append("    final Short cache[] = new Short[+(++a) + +(-127)];\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    int a= 10\n");
		buf.append("    final Short cache[] = new Short[+(++a) + +-127];\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug405096_6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    final Short cache[] = new Short[+(+128) + ~(-127)];\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    final Short cache[] = new Short[+(+128) + ~-127];\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testRemoveParenthesesBug405096_7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    String a= \"\";\n");
		buf.append("    int n= 0;\n");
		buf.append("    \n");
		buf.append("    int i1 = 1+(1+(+128));\n");
		buf.append("    int j1 = 1+(1+(+n));\n");
		buf.append("    int i2 = 1-(-128);\n");
		buf.append("    int j2 = 1-(-n);\n");
		buf.append("    int i3 = 1+(++n);\n");
		buf.append("    int j3 = 1-(--n);\n");
		buf.append("    String s1 = a+(++n);\n");
		buf.append("    String s2 = a+(+128);\n");
		buf.append("    int i5 = 1+(--n);\n");
		buf.append("    int j5 = 1-(++n);\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES);
		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    String a= \"\";\n");
		buf.append("    int n= 0;\n");
		buf.append("    \n");
		buf.append("    int i1 = 1+1+(+128);\n");
		buf.append("    int j1 = 1+1+(+n);\n");
		buf.append("    int i2 = 1-(-128);\n");
		buf.append("    int j2 = 1-(-n);\n");
		buf.append("    int i3 = 1+(++n);\n");
		buf.append("    int j3 = 1-(--n);\n");
		buf.append("    String s1 = a+(++n);\n");
		buf.append("    String s2 = a+(+128);\n");
		buf.append("    int i5 = 1+--n;\n");
		buf.append("    int j5 = 1-++n;\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
	public void testRemoveQualifierBug330754() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class Test {\n");
		buf.append("    String label = \"works\";\n");
		buf.append("    class Nested extends Test {\n");
		buf.append("        Nested() {\n");
		buf.append("            label = \"broken\";\n");
		buf.append("        }\n");
		buf.append("        @Override\n");
		buf.append("        public String toString() {\n");
		buf.append("            return Test.this.label;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY);

		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
	public void testAddFinalBug134676_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
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

	@Test
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
	@Test
	public void testAddFinalBug145028() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
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
	@Test
	public void testAddFinalBug294768() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private transient int field= 0;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
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

	@Test
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

	@Test
	public void testAddFinalBug157276_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
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

	@Test
	public void testAddFinalBug157276_4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
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

	@Test
	public void testAddFinalBug157276_5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
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

	@Test
	public void testAddFinalBug157276_6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
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

	@Test
	public void testAddFinalBug157276_7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
	public void testAddFinalBug272532() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
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

	@Test
	public void testAddFinalBug297566() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int x;\n");
		buf.append("    public E1() {\n");
		buf.append("        this();\n");
		buf.append("    }\n");
		buf.append("    public E1(int a) {\n");
		buf.append("        x = a;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
	public void testAddFinalBug297566_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int x;\n");
		buf.append("    public E1() {\n");
		buf.append("        this(10);\n");
		buf.append("    }\n");
		buf.append("    public E1(int a) {\n");
		buf.append("        this();\n");
		buf.append("    }\n");
		buf.append("    public E1(int f, int y) {\n");
		buf.append("        x = a;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);

		assertRefactoringHasNoChange(new ICompilationUnit[] {cu1});
	}

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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
		buf.append("    public static String s1;\n");
		buf.append("    public String s2;\n");
		buf.append("   void c() {};\n");
		buf.append("   void d() {};\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
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

	@Test
	public void testSortMembersBug434941() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class A {\n");
		buf.append("    public static final int CONSTANT = 5;\n");
		buf.append("    public static void main(final String[] args) { }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

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
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class A {\n");
		buf.append("    public static final int B = 1;\n");
		buf.append("    public final int A = 2;\n");
		buf.append("    public static final int C = 3;\n");
		buf.append("    public final int D = 4;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		enable(CleanUpConstants.SORT_MEMBERS);
		disable(CleanUpConstants.SORT_MEMBERS_ALL);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class A {\n");
		buf.append("    public static final int B = 1;\n");
		buf.append("    public static final int C = 3;\n");
		buf.append("    public final int A = 2;\n");
		buf.append("    public final int D = 4;\n");
		buf.append("}\n");

		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testSortMembersMixedFieldsInterface() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public interface A {\n");
		buf.append("    public static final int B = 1;\n");
		buf.append("    public final int A = 2;\n");
		buf.append("    public static final int C = 3;\n");
		buf.append("    public final int D = 4;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		enable(CleanUpConstants.SORT_MEMBERS);
		disable(CleanUpConstants.SORT_MEMBERS_ALL);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });

		enable(CleanUpConstants.SORT_MEMBERS_ALL);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public interface A {\n");
		buf.append("    public final int A = 2;\n");
		buf.append("    public static final int B = 1;\n");
		buf.append("    public static final int C = 3;\n");
		buf.append("    public final int D = 4;\n");
		buf.append("}\n");

		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testSortMembersBug407759() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class A {\n");
		buf.append("    void foo2() {}\n");
		buf.append("    void foo1() {}\n");
		buf.append("    static int someInt;\n");
		buf.append("    static void fooStatic() {}\n");
		buf.append("    static {\n");
		buf.append("    	someInt = 1;\n");
		buf.append("    }\n");
		buf.append("    void foo3() {}\n");
		buf.append("    static int anotherInt = someInt;\n");
		buf.append("    void foo() {}\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		enable(CleanUpConstants.SORT_MEMBERS);
		disable(CleanUpConstants.SORT_MEMBERS_ALL);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class A {\n");
		buf.append("    static int someInt;\n");
		buf.append("    static {\n");
		buf.append("    	someInt = 1;\n");
		buf.append("    }\n");
		buf.append("    static int anotherInt = someInt;\n");
		buf.append("    static void fooStatic() {}\n");
		buf.append("    void foo() {}\n");
		buf.append("    void foo1() {}\n");
		buf.append("    void foo2() {}\n");
		buf.append("    void foo3() {}\n");
		buf.append("}\n");

		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });

		enable(CleanUpConstants.SORT_MEMBERS);
		enable(CleanUpConstants.SORT_MEMBERS_ALL);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class A {\n");
		buf.append("    static int anotherInt = someInt;\n");
		buf.append("    static int someInt;\n");
		buf.append("    static {\n");
		buf.append("    	someInt = 1;\n");
		buf.append("    }\n");
		buf.append("    static void fooStatic() {}\n");
		buf.append("    void foo() {}\n");
		buf.append("    void foo1() {}\n");
		buf.append("    void foo2() {}\n");
		buf.append("    void foo3() {}\n");
		buf.append("}\n");

		expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testSortMembersVisibility() throws Exception {
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.APPEARANCE_ENABLE_VISIBILITY_SORT_ORDER, true);
		JavaPlugin.getDefault().getPreferenceStore().setToDefault(PreferenceConstants.APPEARANCE_VISIBILITY_SORT_ORDER);

		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
			StringBuffer buf= new StringBuffer();
			buf.append("package test;\n");
			buf.append("public class A {\n");
			buf.append("    public final int B = 1;\n");
			buf.append("    private static final int AA = 1;\n");
			buf.append("    public static final int BB = 2;\n");
			buf.append("    private final int A = 2;\n");
			buf.append("    final int C = 3;\n");
			buf.append("    protected static final int DD = 3;\n");
			buf.append("    final static int CC = 4;\n");
			buf.append("    protected final int D = 4;\n");
			buf.append("}\n");
			ICompilationUnit cu1= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

			enable(CleanUpConstants.SORT_MEMBERS);
			disable(CleanUpConstants.SORT_MEMBERS_ALL);

			buf= new StringBuffer();
			buf.append("package test;\n");
			buf.append("public class A {\n");
			buf.append("    private static final int AA = 1;\n");
			buf.append("    public static final int BB = 2;\n");
			buf.append("    protected static final int DD = 3;\n");
			buf.append("    final static int CC = 4;\n");
			buf.append("    public final int B = 1;\n");
			buf.append("    private final int A = 2;\n");
			buf.append("    final int C = 3;\n");
			buf.append("    protected final int D = 4;\n");
			buf.append("}\n");

			String expected1= buf.toString();

			assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });

			enable(CleanUpConstants.SORT_MEMBERS);
			enable(CleanUpConstants.SORT_MEMBERS_ALL);

			buf= new StringBuffer();
			buf.append("package test;\n");
			buf.append("public class A {\n");
			buf.append("    public static final int BB = 2;\n");
			buf.append("    private static final int AA = 1;\n");
			buf.append("    protected static final int DD = 3;\n");
			buf.append("    final static int CC = 4;\n");
			buf.append("    public final int B = 1;\n");
			buf.append("    private final int A = 2;\n");
			buf.append("    protected final int D = 4;\n");
			buf.append("    final int C = 3;\n");
			buf.append("}\n");

			expected1= buf.toString();

			assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
		} finally {
			JavaPlugin.getDefault().getPreferenceStore().setToDefault(PreferenceConstants.APPEARANCE_ENABLE_VISIBILITY_SORT_ORDER);
		}
	}

	@Test
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
		assertTrue(message, message.contains("ambiguous"));
	}

	@Test
	public void testOrganizeImports02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
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
		assertTrue(message, message.contains("parse"));
	}

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

		// public modifier must not be removed from enum methods
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public interface A {\n");
		buf.append("  public static enum B {\n");
		buf.append("    public static void method () { }\n");
		buf.append("  }\n");
		buf.append("}\n");
		ICompilationUnit cu4= pack1.createCompilationUnit("NestedEnum.java", buf.toString(), false, null);
		// https://docs.oracle.com/javase/specs/jls/se8/html/jls-8.html#jls-8.9
		// nested enum type is implicitly static
		// Bug#538459 'public' modified must not be removed from static method in nested enum
		String expected4 = buf.toString().replace("static enum", "enum");

		// Bug#551038: final keyword must not be removed from method with varargs
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public final class SafeVarargsExample {\n");
		buf.append("  @SafeVarargs\n");
		buf.append("  public final void errorRemoveRedundantModifiers(final String... input) {\n");
		buf.append("  }\n");
		buf.append("}\n");
		String expected5 = buf.toString();
		ICompilationUnit cu5= pack1.createCompilationUnit("SafeVarargsExample.java", buf.toString(), false, null);

		// Bug#553608: modifiers public static final must not be removed from inner enum within interface
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public interface Foo {\n");
		buf.append("  enum Bar {\n");
		buf.append("    A;\n");
		buf.append("    public static final int B = 0;\n");
		buf.append("  }\n");
		buf.append("}\n");
		String expected6 = buf.toString();
		ICompilationUnit cu6= pack1.createCompilationUnit("NestedEnumExample.java", buf.toString(), false, null);

		enable(CleanUpConstants.REMOVE_REDUNDANT_MODIFIERS);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1, cu2, cu3, cu4, cu5, cu6 }, new String[] { expected1, expected2, expected3, expected4, expected5, expected6 });

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
		ASTParser parser= ASTParser.newParser(AST.JLS14);
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
		StringBuffer buf= new StringBuffer();

		// Ensure various extra semi-colons are removed and required ones are left intact.
		// This includes a lambda expression.
		buf.append("package test; ;\n");
		buf.append("enum cars { sedan, coupe };\n");
		buf.append("public class Foo {\n");
		buf.append("  int add(int a, int b) {return a+b;};\n");
		buf.append("  int a= 3;; ;\n");
		buf.append("  int b= 7; // leave this ; alone\n");
		buf.append("  int c= 10; /* and this ; too */\n");
		buf.append("  public int foo () {\n");
		buf.append("    ;\n");
		buf.append("    Runnable r = () -> {\n");
		buf.append("      System.out.println(\"running\");\n");
		buf.append("    };;\n");
		buf.append("    for (;;)\n");
		buf.append("      ;;\n");
		buf.append("      ;\n");
		buf.append("    while (a++ < 1000) ;\n");
		buf.append("  };\n");
		buf.append("};\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("Foo.java", buf.toString(), false, null);

		// Ensure semi-colon after lambda expression remains intact.
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("enum cars { sedan, coupe }\n");
		buf.append("public class Foo {\n");
		buf.append("  int add(int a, int b) {return a+b;}\n");
		buf.append("  int a= 3;\n");
		buf.append("  int b= 7; // leave this ; alone\n");
		buf.append("  int c= 10; /* and this ; too */\n");
		buf.append("  public int foo () {\n");
		buf.append("    \n");
		buf.append("    Runnable r = () -> {\n");
		buf.append("      System.out.println(\"running\");\n");
		buf.append("    };\n");
		buf.append("    for (;;)\n");
		buf.append("      ;\n");
		buf.append("      \n");
		buf.append("    while (a++ < 1000) ;\n");
		buf.append("  }\n");
		buf.append("}\n");
		String expected1 = buf.toString();

		enable(CleanUpConstants.REMOVE_REDUNDANT_SEMICOLONS);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });

	}

	@Test
	public void testBug491087() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface A {\n");
		buf.append("    class B {\n");
		buf.append("        String field;\n");
		buf.append("       B() { field = \"foo\"; }\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class C {\n");
		buf.append("    class D {\n");
		buf.append("       String field;\n");
		buf.append("       D() { field = \"bar\"; }\n");
		buf.append("    }\n");
		buf.append("}\n");

		ICompilationUnit cu1= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface A {\n");
		buf.append("    class B {\n");
		buf.append("        String field;\n");
		buf.append("       B() { this.field = \"foo\"; }\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class C {\n");
		buf.append("    class D {\n");
		buf.append("       String field;\n");
		buf.append("       D() { this.field = \"bar\"; }\n");
		buf.append("    }\n");
		buf.append("}\n");

		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1});
	}
}

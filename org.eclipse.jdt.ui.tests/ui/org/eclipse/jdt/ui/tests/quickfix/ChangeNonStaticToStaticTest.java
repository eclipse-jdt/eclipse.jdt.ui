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
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.Hashtable;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.tests.harness.FussyProgressMonitor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Preferences;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextEditBasedChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.fix.CodeStyleCleanUp;

public class ChangeNonStaticToStaticTest extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup = new ProjectTestSetup();

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");


		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
		store.setValue(PreferenceConstants.CODEGEN_KEYWORD_THIS, false);

		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "//TODO\n${body_statement}", null);

		Preferences corePrefs= JavaPlugin.getJavaCorePluginPreferences();
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_PREFIXES, "");
		corePrefs.setValue(JavaCore.CODEASSIST_STATIC_FIELD_PREFIXES, "");
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_SUFFIXES, "");
		corePrefs.setValue(JavaCore.CODEASSIST_STATIC_FIELD_SUFFIXES, "");

		fJProject1= projectSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	private void assertRefactoringResultAsExpected(CleanUpRefactoring refactoring, String[] expected) throws CoreException {
		FussyProgressMonitor testMonitor= new FussyProgressMonitor();
		refactoring.checkAllConditions(testMonitor);
		testMonitor.assertUsedUp();
		CompositeChange change= (CompositeChange)refactoring.createChange(null);
		Change[] children= change.getChildren();
		String[] previews= new String[children.length];
		for (int i= 0; i < children.length; i++) {
			FussyProgressMonitor testMonitor2= new FussyProgressMonitor();
			previews[i]= ((TextEditBasedChange)children[i]).getPreviewContent(testMonitor2);
			testMonitor2.assertUsedUp();
		}

		assertEqualStringsIgnoreOrder(previews, expected);
	}

	private CodeStyleCleanUp createCleanUp() {
		Map<String, String> options= new Hashtable<>();
		options.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS, CleanUpOptions.TRUE);
		options.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS, CleanUpOptions.TRUE);
	    return new CodeStyleCleanUp(options);
    }

	@Test
	public void testNonStaticAccessTest01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static int I;\n");
		buf.append("    public void foo() {\n");
		buf.append("        (new E1()).I= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu);

		ICleanUp cleanUp= createCleanUp();
		refactoring.addCleanUp(cleanUp);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static int I;\n");
		buf.append("    public void foo() {\n");
		buf.append("        E1.I= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
	}

	@Test
	public void testNonStaticAccessTest02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1<T> {\n");
		buf.append("    public static int I;\n");
		buf.append("    public void foo() {\n");
		buf.append("        (new E1<String>()).I= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu);

		ICleanUp cleanUp= createCleanUp();
		refactoring.addCleanUp(cleanUp);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1<T> {\n");
		buf.append("    public static int I;\n");
		buf.append("    public void foo() {\n");
		buf.append("        E1.I= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
	}

	@Test
	public void testNonStaticAccessTest03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1<T extends String> {\n");
		buf.append("    public static int I;\n");
		buf.append("    public void foo() {\n");
		buf.append("        (new E1<String>()).I= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu);

		ICleanUp cleanUp= createCleanUp();
		refactoring.addCleanUp(cleanUp);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1<T extends String> {\n");
		buf.append("    public static int I;\n");
		buf.append("    public void foo() {\n");
		buf.append("        E1.I= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
	}

	@Test
	public void testNonStaticAccessTest04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static int I;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    private static class E1 {\n");
		buf.append("        public static int N;\n");
		buf.append("    }\n");
		buf.append("    public void bar() {\n");
		buf.append("        test1.E1 e1= new test1.E1();\n");
		buf.append("        e1.I= 10;\n");
		buf.append("        \n");
		buf.append("        E1 e12= new E1();\n");
		buf.append("        e12.N= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu);

		ICleanUp cleanUp= createCleanUp();
		refactoring.addCleanUp(cleanUp);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    private static class E1 {\n");
		buf.append("        public static int N;\n");
		buf.append("    }\n");
		buf.append("    public void bar() {\n");
		buf.append("        test1.E1 e1= new test1.E1();\n");
		buf.append("        test1.E1.I= 10;\n");
		buf.append("        \n");
		buf.append("        E1 e12= new E1();\n");
		buf.append("        E1.N= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
	}

	@Test
	public void testNonStaticAccessTest05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static int I;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1 {}\n");
		pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuilder();
		buf.append("package test2;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3  {\n");
		buf.append("    private E2 e2;\n");
		buf.append("    public void foo() {\n");
		buf.append("        e2.I= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);

		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu);

		ICleanUp cleanUp= createCleanUp();
		refactoring.addCleanUp(cleanUp);

		buf= new StringBuilder();
		buf.append("package test2;\n");
		buf.append("import test1.E1;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3  {\n");
		buf.append("    private E2 e2;\n");
		buf.append("    public void foo() {\n");
		buf.append("        E1.I= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
	}

	@Test
	public void testNonStaticAccessTest06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1<T> {\n");
		buf.append("    public static int I;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E2<T, G> extends E1<T> {}\n");
		pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuilder();
		buf.append("package test2;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3<G>  {\n");
		buf.append("    private E2<String, G> e2;\n");
		buf.append("    public void foo() {\n");
		buf.append("        e2.I= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);

		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu);

		ICleanUp cleanUp= createCleanUp();
		refactoring.addCleanUp(cleanUp);

		buf= new StringBuilder();
		buf.append("package test2;\n");
		buf.append("import test1.E1;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3<G>  {\n");
		buf.append("    private E2<String, G> e2;\n");
		buf.append("    public void foo() {\n");
		buf.append("        E1.I= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
	}

	@Test
	public void testNonStaticAccessTest07() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1<T> {\n");
		buf.append("    public static int I;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E2<T, G> extends E1<T> {}\n");
		pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuilder();
		buf.append("package test2;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3<G>  {\n");
		buf.append("    private E2<String, G> e2;\n");
		buf.append("    private static class E1<T, G> {}\n");
		buf.append("    public void foo() {\n");
		buf.append("        e2.I= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);

		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu);

		ICleanUp cleanUp= createCleanUp();
		refactoring.addCleanUp(cleanUp);

		buf= new StringBuilder();
		buf.append("package test2;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3<G>  {\n");
		buf.append("    private E2<String, G> e2;\n");
		buf.append("    private static class E1<T, G> {}\n");
		buf.append("    public void foo() {\n");
		buf.append("        test1.E1.I= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
	}

	@Test
	public void testNonStaticAccessTest08() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1<T> {\n");
		buf.append("    public static int I;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E2<T, G> extends E1<T> {}\n");
		pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuilder();
		buf.append("package test2;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3<G>  {\n");
		buf.append("    private E2<String, G> e2;\n");
		buf.append("    private class E1<T, G> {\n");
		buf.append("        private class C {\n");
		buf.append("            public void foo() {\n");
		buf.append("                e2.I= 10;\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);

		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu);

		ICleanUp cleanUp= createCleanUp();
		refactoring.addCleanUp(cleanUp);

		buf= new StringBuilder();
		buf.append("package test2;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3<G>  {\n");
		buf.append("    private E2<String, G> e2;\n");
		buf.append("    private class E1<T, G> {\n");
		buf.append("        private class C {\n");
		buf.append("            public void foo() {\n");
		buf.append("                test1.E1.I= 10;\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
	}

	@Test
	public void testNonStaticAccessTest09() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1<T> {\n");
		buf.append("    public static int I;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E2<T, G> extends E1<T> {}\n");
		pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuilder();
		buf.append("package test2;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3<G>  {\n");
		buf.append("    private E2<String, G> e2;\n");
		buf.append("    private class C {\n");
		buf.append("        private class E1<T, G> {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        e2.I= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);

		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu);

		ICleanUp cleanUp= createCleanUp();
		refactoring.addCleanUp(cleanUp);

		buf= new StringBuilder();
		buf.append("package test2;\n");
		buf.append("import test1.E1;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3<G>  {\n");
		buf.append("    private E2<String, G> e2;\n");
		buf.append("    private class C {\n");
		buf.append("        private class E1<T, G> {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        E1.I= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
	}

	@Test
	public void testNonStaticAccessTest10() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static void foo() {};\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    private static String E1= \"\";\n");
		buf.append("    public void foo() {\n");
		buf.append("        test1.E1 e1= new test1.E1();\n");
		buf.append("        e1.foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu);

		ICleanUp cleanUp= createCleanUp();
		refactoring.addCleanUp(cleanUp);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    private static String E1= \"\";\n");
		buf.append("    public void foo() {\n");
		buf.append("        test1.E1 e1= new test1.E1();\n");
		buf.append("        test1.E1.foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
	}

	@Test
	public void testNonStaticAccessTest11() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1<T> {\n");
		buf.append("    public static void foo() {};\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    private static String E1= \"\";\n");
		buf.append("    public void foo() {\n");
		buf.append("        test1.E1<String> e1= new test1.E1<String>();\n");
		buf.append("        e1.foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu);

		ICleanUp cleanUp= createCleanUp();
		refactoring.addCleanUp(cleanUp);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    private static String E1= \"\";\n");
		buf.append("    public void foo() {\n");
		buf.append("        test1.E1<String> e1= new test1.E1<String>();\n");
		buf.append("        test1.E1.foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
	}

	@Test
	public void testNonStaticAccessTest12() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1<G> {\n");
		buf.append("    public static int I;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.E1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        test1.E1<E1<test1.E1<E2>>> e1= new test1.E1<E1<test1.E1<E2>>>();\n");
		buf.append("        e1.I= 10;\n");
		buf.append("        E1<E1<E2>> f=null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuilder();
		buf.append("package test2;\n");
		buf.append("public class E1<G>  {}\n");
		pack2.createCompilationUnit("E1.java", buf.toString(), false, null);

		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu);

		ICleanUp cleanUp= createCleanUp();
		refactoring.addCleanUp(cleanUp);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.E1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        test1.E1<E1<test1.E1<E2>>> e1= new test1.E1<E1<test1.E1<E2>>>();\n");
		buf.append("        test1.E1.I= 10;\n");
		buf.append("        E1<E1<E2>> f=null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
	}
}

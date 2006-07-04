/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.Hashtable;
import java.util.regex.Pattern;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Preferences;

import org.eclipse.core.resources.IncrementalProjectBuilder;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextEditBasedChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.fix.CodeStyleCleanUp;
import org.eclipse.jdt.internal.ui.fix.ControlStatementsCleanUp;
import org.eclipse.jdt.internal.ui.fix.ExpressionsCleanUp;
import org.eclipse.jdt.internal.ui.fix.ICleanUp;
import org.eclipse.jdt.internal.ui.fix.Java50CleanUp;
import org.eclipse.jdt.internal.ui.fix.PotentialProgrammingProblemsCleanUp;
import org.eclipse.jdt.internal.ui.fix.StringCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnnecessaryCodeCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnusedCodeCleanUp;
import org.eclipse.jdt.internal.ui.fix.VariableDeclarationCleanUp;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

public class CleanUpTest extends QuickFixTest {
	
	private static final String FIELD_COMMENT= "/* Test */";
	
	private static final Class THIS= CleanUpTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public CleanUpTest(String name) {
		super(name);
	}
	
	public static Test allTests() {
		TestSuite suite= new TestSuite();
		
		suite.addTest(CleanUpStressTest.suite());
		suite.addTest(new ProjectTestSetup(new TestSuite(THIS)));
		
		return suite;
	}
	
	public static Test suite() {
		if (true)
			return allTests();
		
		return setUpTest(new CleanUpTest("testRemoveBlock05"));
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}
	
	protected void setUp() throws Exception {
		Hashtable options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		
		
		JavaCore.setOptions(options);			

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
		store.setValue(PreferenceConstants.CODEGEN_KEYWORD_THIS, false);
		
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "//TODO\n${body_statement}", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.FIELDCOMMENT_ID, FIELD_COMMENT, null);
		
		Preferences corePrefs= JavaCore.getPlugin().getPluginPreferences();
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_PREFIXES, "");
		corePrefs.setValue(JavaCore.CODEASSIST_STATIC_FIELD_PREFIXES, "");
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_SUFFIXES, "");
		corePrefs.setValue(JavaCore.CODEASSIST_STATIC_FIELD_SUFFIXES, "");	
		
		fJProject1= ProjectTestSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}
	
	private void assertRefactoringResultAsExpected(CleanUpRefactoring refactoring, String[] expected) throws CoreException {
		refactoring.checkAllConditions(new NullProgressMonitor());
		CompositeChange change= (CompositeChange)refactoring.createChange(null);
		Change[] children= change.getChildren();
		String[] previews= new String[children.length]; 
		for (int i= 0; i < children.length; i++) {
			previews[i]= ((TextEditBasedChange)children[i]).getPreviewContent(null);
		}

		assertEqualStringsIgnoreOrder(previews, expected);
	}
	
	private void assertRefactoringResultAsExpectedIgnoreHashValue(CleanUpRefactoring refactoring, String[] expected) throws CoreException {
		refactoring.checkAllConditions(new NullProgressMonitor());
		CompositeChange change= (CompositeChange)refactoring.createChange(null);
		Change[] children= change.getChildren();
		String[] previews= new String[children.length]; 
		Pattern regex= Pattern.compile("long serialVersionUID = .*L;");
		for (int i= 0; i < children.length; i++) {
			String previewContent= ((TextEditBasedChange)children[i]).getPreviewContent(null);
			previews[i]= previewContent.replaceAll(regex.pattern(), "long serialVersionUID = 1L;");
		}

		assertEqualStringsIgnoreOrder(previews, expected);
	}
	
	private void assertRefactoringHasNoChange(CleanUpRefactoring refactoring) throws CoreException {
		refactoring.checkAllConditions(new NullProgressMonitor());
		CompositeChange change= (CompositeChange)refactoring.createChange(null);
		Change[] children= change.getChildren();
		StringBuffer buf= new StringBuffer();
		buf.append("Refactoring should generate no changes but does change:\n");
		for (int i= 0; i < children.length; i++) {
			buf.append(((TextChange)children[i]).getPreviewContent(null));
		}
		
		assertTrue(buf.toString(), change.getChildren().length == 0);
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		refactoring.addCompilationUnit(cu2);
		refactoring.addCompilationUnit(cu3);
		
		ICleanUp cleanUp= new StringCleanUp(StringCleanUp.ADD_MISSING_NLS_TAG);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1, expected2, expected3});	
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		refactoring.addCompilationUnit(cu2);
		refactoring.addCompilationUnit(cu3);

		ICleanUp cleanUp= new StringCleanUp(StringCleanUp.REMOVE_UNNECESSARY_NLS_TAG);
		refactoring.addCleanUp(cleanUp);		
		
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

		assertRefactoringResultAsExpected(refactoring, new String[] {expected1, expected2, expected3});	
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		refactoring.addCompilationUnit(cu2);
		refactoring.addCompilationUnit(cu3);
		
		ICleanUp cleanUp= new UnusedCodeCleanUp(UnusedCodeCleanUp.REMOVE_UNUSED_IMPORTS);
		refactoring.addCleanUp(cleanUp);
		
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

		assertRefactoringResultAsExpected(refactoring, new String[] {expected1, expected2, expected3});	
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		refactoring.addCompilationUnit(cu2);
		refactoring.addCompilationUnit(cu3);
		
		ICleanUp cleanUp= new UnusedCodeCleanUp(UnusedCodeCleanUp.REMOVE_UNUSED_PRIVATE_METHODS);
		refactoring.addCleanUp(cleanUp);
		
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

		assertRefactoringResultAsExpected(refactoring, new String[] {expected1, expected2, expected3});	
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		refactoring.addCompilationUnit(cu2);
		refactoring.addCompilationUnit(cu3);
		
		ICleanUp cleanUp= new UnusedCodeCleanUp(UnusedCodeCleanUp.REMOVE_UNUSED_PRIVATE_CONSTRUCTORS);
		refactoring.addCleanUp(cleanUp);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
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
		buf.append("    }\n");
		buf.append("    private void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		assertRefactoringResultAsExpected(refactoring, new String[] {expected1, expected2, expected3});	
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		refactoring.addCompilationUnit(cu2);
		refactoring.addCompilationUnit(cu3);
		
		ICleanUp cleanUp= new UnusedCodeCleanUp(UnusedCodeCleanUp.REMOVE_UNUSED_PRIVATE_FIELDS);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1, expected2, expected3});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		refactoring.addCompilationUnit(cu2);
		refactoring.addCompilationUnit(cu3);
		
		ICleanUp cleanUp= new UnusedCodeCleanUp(UnusedCodeCleanUp.REMOVE_UNUSED_PRIVATE_TYPES);
		refactoring.addCleanUp(cleanUp);
		
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

		assertRefactoringResultAsExpected(refactoring, new String[] {expected1, expected2, expected3});	
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		refactoring.addCompilationUnit(cu2);
		refactoring.addCompilationUnit(cu3);
		
		ICleanUp cleanUp= new UnusedCodeCleanUp(UnusedCodeCleanUp.REMOVE_UNUSED_LOCAL_VARIABLES);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1, expected2, expected3});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new UnusedCodeCleanUp(UnusedCodeCleanUp.REMOVE_UNUSED_LOCAL_VARIABLES);
		refactoring.addCleanUp(cleanUp);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= bar();\n");
		buf.append("    }\n");
		buf.append("    public int bar() {return 1;}\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new UnusedCodeCleanUp(UnusedCodeCleanUp.REMOVE_UNUSED_PRIVATE_FIELDS);
		refactoring.addCleanUp(cleanUp);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int i= bar();\n");
		buf.append("    public int bar() {return 1;}\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new UnusedCodeCleanUp(UnusedCodeCleanUp.REMOVE_UNUSED_LOCAL_VARIABLES);
		refactoring.addCleanUp(cleanUp);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= 1;\n");
		buf.append("        i= bar();\n");
		buf.append("    }\n");
		buf.append("    public int bar() {return 1;}\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new UnusedCodeCleanUp(UnusedCodeCleanUp.REMOVE_UNUSED_PRIVATE_FIELDS);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		refactoring.addCompilationUnit(cu2);
		
		ICleanUp cleanUp= new UnnecessaryCodeCleanUp(UnnecessaryCodeCleanUp.REMOVE_UNUSED_CAST);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1, expected2});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new UnusedCodeCleanUp(UnusedCodeCleanUp.REMOVE_UNUSED_PRIVATE_FIELDS | UnusedCodeCleanUp.REMOVE_UNUSED_LOCAL_VARIABLES);
		refactoring.addCleanUp(cleanUp);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1  {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		refactoring.addCompilationUnit(cu2);
		
		ICleanUp cleanUp= new Java50CleanUp(Java50CleanUp.ADD_DEPRECATED_ANNOTATION);
		refactoring.addCleanUp(cleanUp);
		
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

		assertRefactoringResultAsExpected(refactoring, new String[] {expected1, expected2});	
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		refactoring.addCompilationUnit(cu2);
		
		ICleanUp cleanUp= new Java50CleanUp(Java50CleanUp.ADD_DEPRECATED_ANNOTATION);
		refactoring.addCleanUp(cleanUp);
		
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

		assertRefactoringResultAsExpected(refactoring, new String[] {expected1, expected2});	
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		refactoring.addCompilationUnit(cu2);
		
		ICleanUp cleanUp= new Java50CleanUp(Java50CleanUp.ADD_DEPRECATED_ANNOTATION);
		refactoring.addCleanUp(cleanUp);
		
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

		assertRefactoringResultAsExpected(refactoring, new String[] {expected1, expected2});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		refactoring.addCompilationUnit(cu2);
		refactoring.addCompilationUnit(cu3);
		
		ICleanUp cleanUp= new Java50CleanUp(Java50CleanUp.ADD_OVERRIDE_ANNOATION);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1, expected2});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		refactoring.addCompilationUnit(cu2);
		refactoring.addCompilationUnit(cu3);
		
		ICleanUp cleanUp= new Java50CleanUp(Java50CleanUp.ADD_OVERRIDE_ANNOATION | Java50CleanUp.ADD_DEPRECATED_ANNOTATION);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1, expected2, expected3});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		refactoring.addCompilationUnit(cu2);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_FIELD_ACCESS);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1, expected2});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		refactoring.addCompilationUnit(cu2);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_FIELD_ACCESS | CodeStyleCleanUp.CHANGE_NON_STATIC_ACCESS_TO_STATIC);
		refactoring.addCleanUp(cleanUp);
		
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

		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		refactoring.addCompilationUnit(cu2);
		refactoring.addCompilationUnit(cu3);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.CHANGE_NON_STATIC_ACCESS_TO_STATIC);
		refactoring.addCleanUp(cleanUp);
		
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

		assertRefactoringResultAsExpected(refactoring, new String[] {expected1, expected2, expected3});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		refactoring.addCompilationUnit(cu2);
		refactoring.addCompilationUnit(cu3);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.CHANGE_NON_STATIC_ACCESS_TO_STATIC);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1, expected2, expected3});

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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		refactoring.addCompilationUnit(cu2);
		refactoring.addCompilationUnit(cu3);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_FIELD_ACCESS | CodeStyleCleanUp.CHANGE_NON_STATIC_ACCESS_TO_STATIC);
		refactoring.addCleanUp(cleanUp);
		
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
		
		String[] expected= new String[] {expected1};
		
		assertRefactoringResultAsExpected(refactoring, expected);
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_FIELD_ACCESS | CodeStyleCleanUp.CHANGE_NON_STATIC_ACCESS_TO_STATIC);
		refactoring.addCleanUp(cleanUp);
		
		assertRefactoringHasNoChange(refactoring);
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_FIELD_ACCESS | CodeStyleCleanUp.CHANGE_NON_STATIC_ACCESS_TO_STATIC);
		refactoring.addCleanUp(cleanUp);
		
		assertRefactoringHasNoChange(refactoring);
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_FIELD_ACCESS | CodeStyleCleanUp.CHANGE_NON_STATIC_ACCESS_TO_STATIC);
		refactoring.addCleanUp(cleanUp);
		
		assertRefactoringHasNoChange(refactoring);
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_FIELD_ACCESS | CodeStyleCleanUp.CHANGE_NON_STATIC_ACCESS_TO_STATIC);
		refactoring.addCleanUp(cleanUp);
		
		assertRefactoringHasNoChange(refactoring);
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_FIELD_ACCESS | CodeStyleCleanUp.CHANGE_NON_STATIC_ACCESS_TO_STATIC | CodeStyleCleanUp.QUALIFY_STATIC_FIELD_ACCESS);
		refactoring.addCleanUp(cleanUp);
		
		assertRefactoringHasNoChange(refactoring);
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		refactoring.addCompilationUnit(cu2);
		refactoring.addCompilationUnit(cu3);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_FIELD_ACCESS | CodeStyleCleanUp.CHANGE_NON_STATIC_ACCESS_TO_STATIC | CodeStyleCleanUp.QUALIFY_STATIC_FIELD_ACCESS);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1, expected2, expected3});

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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_FIELD_ACCESS | CodeStyleCleanUp.CHANGE_NON_STATIC_ACCESS_TO_STATIC | CodeStyleCleanUp.QUALIFY_STATIC_FIELD_ACCESS);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_FIELD_ACCESS | CodeStyleCleanUp.CHANGE_NON_STATIC_ACCESS_TO_STATIC | CodeStyleCleanUp.QUALIFY_STATIC_FIELD_ACCESS);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_FIELD_ACCESS | CodeStyleCleanUp.CHANGE_NON_STATIC_ACCESS_TO_STATIC | CodeStyleCleanUp.QUALIFY_STATIC_FIELD_ACCESS);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_FIELD_ACCESS | CodeStyleCleanUp.CHANGE_NON_STATIC_ACCESS_TO_STATIC | CodeStyleCleanUp.QUALIFY_STATIC_FIELD_ACCESS);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		refactoring.addCompilationUnit(cu2);
		refactoring.addCompilationUnit(cu3);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_FIELD_ACCESS | CodeStyleCleanUp.CHANGE_NON_STATIC_ACCESS_TO_STATIC | 
		                                       CodeStyleCleanUp.QUALIFY_STATIC_FIELD_ACCESS | CodeStyleCleanUp.CHANGE_INDIRECT_STATIC_ACCESS_TO_DIRECT);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1, expected2});

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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new ControlStatementsCleanUp(ControlStatementsCleanUp.ADD_BLOCK_TO_CONTROL_STATEMENTS);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new ControlStatementsCleanUp(ControlStatementsCleanUp.ADD_BLOCK_TO_CONTROL_STATEMENTS);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new ControlStatementsCleanUp(ControlStatementsCleanUp.ADD_BLOCK_TO_CONTROL_STATEMENTS);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new ControlStatementsCleanUp(ControlStatementsCleanUp.ADD_BLOCK_TO_CONTROL_STATEMENTS);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new ControlStatementsCleanUp(ControlStatementsCleanUp.ADD_BLOCK_TO_CONTROL_STATEMENTS);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_STATIC_FIELD_ACCESS);
		refactoring.addCleanUp(cleanUp);
		
		assertRefactoringHasNoChange(refactoring);
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp codeStyle= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_FIELD_ACCESS);
		refactoring.addCleanUp(codeStyle);
		ControlStatementsCleanUp statmentsCleanUp= new ControlStatementsCleanUp(ControlStatementsCleanUp.ADD_BLOCK_TO_CONTROL_STATEMENTS);
		refactoring.addCleanUp(statmentsCleanUp);
		ICleanUp stringFix= new StringCleanUp(StringCleanUp.REMOVE_UNNECESSARY_NLS_TAG);
		refactoring.addCleanUp(stringFix);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp codeStyle= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_FIELD_ACCESS);
		refactoring.addCleanUp(codeStyle);
		ControlStatementsCleanUp statmentsCleanUp= new ControlStatementsCleanUp(ControlStatementsCleanUp.ADD_BLOCK_TO_CONTROL_STATEMENTS);
		refactoring.addCleanUp(statmentsCleanUp);
		ICleanUp stringFix= new StringCleanUp(StringCleanUp.REMOVE_UNNECESSARY_NLS_TAG | StringCleanUp.ADD_MISSING_NLS_TAG);
		refactoring.addCleanUp(stringFix);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.CHANGE_INDIRECT_STATIC_ACCESS_TO_DIRECT);
		refactoring.addCleanUp(cleanUp);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(test2.I1.N);\n");
		buf.append("        I1 i1= new I1();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_METHOD_ACCESS);
		refactoring.addCleanUp(cleanUp);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {}\n");
		buf.append("    private void bar() {\n");
		buf.append("        this.foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_STATIC_METHOD_ACCESS);
		refactoring.addCleanUp(cleanUp);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static void foo() {}\n");
		buf.append("    private void bar() {\n");
		buf.append("        E1.foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_STATIC_FIELD_ACCESS);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.CHANGE_NON_STATIC_ACCESS_TO_STATIC);
		refactoring.addCleanUp(cleanUp);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(E1.i);\n");
		buf.append("    }\n");
		buf.append("    public static int i= 10;\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu2);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.CHANGE_NON_STATIC_ACCESS_TO_STATIC);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu2);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.CHANGE_NON_STATIC_ACCESS_TO_STATIC);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu2);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_FIELD_ACCESS);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_STATIC_FIELD_ACCESS);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_METHOD_ACCESS | CodeStyleCleanUp.QUALIFY_FIELD_ACCESS);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_STATIC_FIELD_ACCESS | CodeStyleCleanUp.QUALIFY_STATIC_METHOD_ACCESS);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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

		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);

		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_FIELD_ACCESS | CodeStyleCleanUp.QUALIFY_METHOD_ACCESS);
		refactoring.addCleanUp(cleanUp);

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

		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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

		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);

		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_FIELD_ACCESS | CodeStyleCleanUp.QUALIFY_METHOD_ACCESS);
		refactoring.addCleanUp(cleanUp);

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

		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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

		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);

		ICleanUp cleanUp1= new CodeStyleCleanUp(CodeStyleCleanUp.CHANGE_NON_STATIC_ACCESS_TO_STATIC);
		ICleanUp cleanUp2= new UnusedCodeCleanUp(UnusedCodeCleanUp.REMOVE_UNUSED_IMPORTS);
		refactoring.addCleanUp(cleanUp1);
		refactoring.addCleanUp(cleanUp2);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("        static class ClassA {static ClassB B;}\n");
		buf.append("        static class ClassB {static ClassC C;}\n");
		buf.append("        static class ClassC {static ClassD D;}\n");
		buf.append("        static class ClassD {}\n");
		buf.append("\n");
		buf.append("        public void foo() {\n");
		buf.append("                ClassB.C.D.toString();\n");
		buf.append("        }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new ControlStatementsCleanUp(ControlStatementsCleanUp.CONVERT_FOR_LOOP_TO_ENHANCED_FOR_LOOP);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new ControlStatementsCleanUp(ControlStatementsCleanUp.CONVERT_FOR_LOOP_TO_ENHANCED_FOR_LOOP);
		refactoring.addCleanUp(cleanUp);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        List<E1> list1= new ArrayList<E1>();\n");
		buf.append("        List<E1> list2= new ArrayList<E1>();\n");
		buf.append("        for (E1 e1 : list1) {\n");
		buf.append("            for (Object element0 : list2) {\n");
		buf.append("                E1 e2 = (E1) element0;\n");
		buf.append("                System.out.println(e2);\n");
		buf.append("            }\n");
		buf.append("            System.out.println(e1);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new ControlStatementsCleanUp(ControlStatementsCleanUp.CONVERT_FOR_LOOP_TO_ENHANCED_FOR_LOOP);
		refactoring.addCleanUp(cleanUp);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int[] array={1,2,3,4};\n");
		buf.append("        for (int element : array) {\n");
		buf.append("            String[] strs={\"1\",\"2\"};\n");
		buf.append("            for (String element0 : strs) {\n");
		buf.append("                System.out.println(element+element0);\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new ControlStatementsCleanUp(ControlStatementsCleanUp.CONVERT_FOR_LOOP_TO_ENHANCED_FOR_LOOP);
		refactoring.addCleanUp(cleanUp);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int[] array= new int[10];\n");
		buf.append("        for (int element : array) {\n");
		buf.append("            for (int element0 : array) {\n");
		buf.append("                for (int element1 : array) {\n");
		buf.append("                }\n");
		buf.append("                for (int element1 : array) {\n");
		buf.append("                }\n");
		buf.append("            }\n");
		buf.append("            for (int element0 : array) {\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new ControlStatementsCleanUp(ControlStatementsCleanUp.CONVERT_FOR_LOOP_TO_ENHANCED_FOR_LOOP);
		refactoring.addCleanUp(cleanUp);
		
		assertRefactoringHasNoChange(refactoring);
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new ControlStatementsCleanUp(ControlStatementsCleanUp.CONVERT_FOR_LOOP_TO_ENHANCED_FOR_LOOP);
		refactoring.addCleanUp(cleanUp);
		
		assertRefactoringHasNoChange(refactoring);
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new ControlStatementsCleanUp(ControlStatementsCleanUp.CONVERT_FOR_LOOP_TO_ENHANCED_FOR_LOOP);
		refactoring.addCleanUp(cleanUp);
		
		assertRefactoringHasNoChange(refactoring);
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new ControlStatementsCleanUp(ControlStatementsCleanUp.CONVERT_FOR_LOOP_TO_ENHANCED_FOR_LOOP);
		refactoring.addCleanUp(cleanUp);
		
		assertRefactoringHasNoChange(refactoring);
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new ControlStatementsCleanUp(ControlStatementsCleanUp.CONVERT_FOR_LOOP_TO_ENHANCED_FOR_LOOP);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new ControlStatementsCleanUp(ControlStatementsCleanUp.CONVERT_FOR_LOOP_TO_ENHANCED_FOR_LOOP);
		refactoring.addCleanUp(cleanUp);
		
		assertRefactoringHasNoChange(refactoring);
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new ControlStatementsCleanUp(ControlStatementsCleanUp.CONVERT_FOR_LOOP_TO_ENHANCED_FOR_LOOP);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new ControlStatementsCleanUp(ControlStatementsCleanUp.CONVERT_FOR_LOOP_TO_ENHANCED_FOR_LOOP);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new ControlStatementsCleanUp(ControlStatementsCleanUp.CONVERT_FOR_LOOP_TO_ENHANCED_FOR_LOOP);
		refactoring.addCleanUp(cleanUp);
		
		assertRefactoringHasNoChange(refactoring);
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new ControlStatementsCleanUp(ControlStatementsCleanUp.CONVERT_FOR_LOOP_TO_ENHANCED_FOR_LOOP);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp1= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_FIELD_ACCESS);
		ICleanUp cleanUp2= new UnusedCodeCleanUp(UnusedCodeCleanUp.REMOVE_UNUSED_PRIVATE_FIELDS);
		refactoring.addCleanUp(cleanUp1);
		refactoring.addCleanUp(cleanUp2);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp1= new ControlStatementsCleanUp(ControlStatementsCleanUp.ADD_BLOCK_TO_CONTROL_STATEMENTS);
		ICleanUp cleanUp2= new StringCleanUp(StringCleanUp.ADD_MISSING_NLS_TAG);
		refactoring.addCleanUp(cleanUp1);
		refactoring.addCleanUp(cleanUp2);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp1= new CodeStyleCleanUp(CodeStyleCleanUp.QUALIFY_FIELD_ACCESS);
		refactoring.addCleanUp(cleanUp1);
		ICleanUp cleanUp2= new ControlStatementsCleanUp(ControlStatementsCleanUp.CONVERT_FOR_LOOP_TO_ENHANCED_FOR_LOOP);
		refactoring.addCleanUp(cleanUp2);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp1= new ControlStatementsCleanUp(ControlStatementsCleanUp.ADD_BLOCK_TO_CONTROL_STATEMENTS | ControlStatementsCleanUp.CONVERT_FOR_LOOP_TO_ENHANCED_FOR_LOOP);
		ICleanUp cleanUp2= new UnusedCodeCleanUp(UnusedCodeCleanUp.REMOVE_UNUSED_PRIVATE_FIELDS);
		refactoring.addCleanUp(cleanUp1);
		refactoring.addCleanUp(cleanUp2);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp1= new ControlStatementsCleanUp(ControlStatementsCleanUp.ADD_BLOCK_TO_CONTROL_STATEMENTS);
		ICleanUp cleanUp2= new StringCleanUp(StringCleanUp.ADD_MISSING_NLS_TAG);
		refactoring.addCleanUp(cleanUp1);
		refactoring.addCleanUp(cleanUp2);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
	        CleanUpRefactoring refactoring= new CleanUpRefactoring();
	        refactoring.addCompilationUnit(cu1);
	        ICleanUp cleanUp1= new PotentialProgrammingProblemsCleanUp(PotentialProgrammingProblemsCleanUp.ADD_CALCULATED_SERIAL_VERSION_ID);
	        refactoring.addCleanUp(cleanUp1);
	        buf= new StringBuffer();
	        buf.append("package test1;\n");
	        buf.append("import java.io.Serializable;\n");
	        buf.append("public class E1 implements Serializable {\n");
	        buf.append("\n");
	        buf.append("    " + FIELD_COMMENT + "\n");
	        buf.append("    private static final long serialVersionUID = 1L;\n");
	        buf.append("}\n");
	        String expected1= buf.toString();
	        assertRefactoringResultAsExpectedIgnoreHashValue(refactoring, new String[] { expected1 });
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
	        CleanUpRefactoring refactoring= new CleanUpRefactoring();
	        refactoring.addCompilationUnit(cu1);
	        ICleanUp cleanUp1= new PotentialProgrammingProblemsCleanUp(PotentialProgrammingProblemsCleanUp.ADD_CALCULATED_SERIAL_VERSION_ID);
	        refactoring.addCleanUp(cleanUp1);
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
	        assertRefactoringResultAsExpectedIgnoreHashValue(refactoring, new String[] { expected1 });
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
	        CleanUpRefactoring refactoring= new CleanUpRefactoring();
	        refactoring.addCompilationUnit(cu1);
	        refactoring.addCompilationUnit(cu2);
	        ICleanUp cleanUp1= new PotentialProgrammingProblemsCleanUp(PotentialProgrammingProblemsCleanUp.ADD_CALCULATED_SERIAL_VERSION_ID);
	        refactoring.addCleanUp(cleanUp1);
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
	        buf.append("\n");
	        buf.append("    " + FIELD_COMMENT + "\n");
	        buf.append("    private static final long serialVersionUID = 1L;\n");
	        buf.append("}\n");
	        String expected1= buf.toString();
	        
	        assertRefactoringResultAsExpectedIgnoreHashValue(refactoring, new String[] {expected1, expected2});
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
	        CleanUpRefactoring refactoring= new CleanUpRefactoring();
	        refactoring.addCompilationUnit(cu1);
	        ICleanUp cleanUp1= new PotentialProgrammingProblemsCleanUp(PotentialProgrammingProblemsCleanUp.ADD_CALCULATED_SERIAL_VERSION_ID);
	        refactoring.addCleanUp(cleanUp1);
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
	        assertRefactoringResultAsExpectedIgnoreHashValue(refactoring, new String[] { expected1 });
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
	        CleanUpRefactoring refactoring= new CleanUpRefactoring();
	        refactoring.addCompilationUnit(cu1);
	        ICleanUp cleanUp1= new PotentialProgrammingProblemsCleanUp(PotentialProgrammingProblemsCleanUp.ADD_CALCULATED_SERIAL_VERSION_ID);
	        refactoring.addCleanUp(cleanUp1);
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
	        assertRefactoringResultAsExpectedIgnoreHashValue(refactoring, new String[] { expected1 });
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
	        CleanUpRefactoring refactoring= new CleanUpRefactoring();
	        refactoring.addCompilationUnit(cu1);
	        ICleanUp cleanUp1= new PotentialProgrammingProblemsCleanUp(PotentialProgrammingProblemsCleanUp.ADD_CALCULATED_SERIAL_VERSION_ID);
	        refactoring.addCleanUp(cleanUp1);
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
	        assertRefactoringResultAsExpectedIgnoreHashValue(refactoring, new String[] { expected1 });
        } finally {
    		JavaProjectHelper.set15CompilerOptions(fJProject1);
        }
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp1= new ControlStatementsCleanUp(ControlStatementsCleanUp.REMOVE_UNNECESSARY_BLOCKS | ControlStatementsCleanUp.CONVERT_FOR_LOOP_TO_ENHANCED_FOR_LOOP);
		refactoring.addCleanUp(cleanUp1);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp1= new ControlStatementsCleanUp(ControlStatementsCleanUp.REMOVE_UNNECESSARY_BLOCKS | ControlStatementsCleanUp.CONVERT_FOR_LOOP_TO_ENHANCED_FOR_LOOP);
		refactoring.addCleanUp(cleanUp1);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp1= new ControlStatementsCleanUp(ControlStatementsCleanUp.REMOVE_UNNECESSARY_BLOCKS | ControlStatementsCleanUp.CONVERT_FOR_LOOP_TO_ENHANCED_FOR_LOOP);
		refactoring.addCleanUp(cleanUp1);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp1= new ControlStatementsCleanUp(ControlStatementsCleanUp.REMOVE_UNNECESSARY_BLOCKS | ControlStatementsCleanUp.CONVERT_FOR_LOOP_TO_ENHANCED_FOR_LOOP);
		refactoring.addCleanUp(cleanUp1);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp1= new ControlStatementsCleanUp(ControlStatementsCleanUp.REMOVE_UNNECESSARY_BLOCKS | ControlStatementsCleanUp.CONVERT_FOR_LOOP_TO_ENHANCED_FOR_LOOP);
		refactoring.addCleanUp(cleanUp1);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int[] is= null;\n");
		buf.append("        for (int element : is);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp1= new ControlStatementsCleanUp(ControlStatementsCleanUp.REMOVE_UNNECESSARY_BLOCKS);
		refactoring.addCleanUp(cleanUp1);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp1= new UnnecessaryCodeCleanUp(UnnecessaryCodeCleanUp.REMOVE_UNUSED_CAST);
		refactoring.addCleanUp(cleanUp1);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private boolean foo() {\n");
		buf.append("        return Boolean.TRUE;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp1= new UnnecessaryCodeCleanUp(UnnecessaryCodeCleanUp.REMOVE_UNUSED_CAST);
		refactoring.addCleanUp(cleanUp1);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private Integer foo() {\n");
		buf.append("        return (Integer) getNumber();\n");
		buf.append("    }\n");
		buf.append("    private Number getNumber() {return null;}\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);

		ICleanUp cleanUp1= new ControlStatementsCleanUp(ControlStatementsCleanUp.ADD_BLOCK_TO_CONTROL_STATEMENTS);
		ICleanUp cleanUp2= new ExpressionsCleanUp(ExpressionsCleanUp.ADD_PARANOIC_PARENTHESIS);
		refactoring.addCleanUp(cleanUp1);
		refactoring.addCleanUp(cleanUp2);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);

		ICleanUp cleanUp1= new ControlStatementsCleanUp(ControlStatementsCleanUp.REMOVE_UNNECESSARY_BLOCKS);
		ICleanUp cleanUp2= new ExpressionsCleanUp(ExpressionsCleanUp.REMOVE_UNNECESSARY_PARENTHESIS);
		refactoring.addCleanUp(cleanUp1);
		refactoring.addCleanUp(cleanUp2);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);

		ICleanUp cleanUp1= new ControlStatementsCleanUp(ControlStatementsCleanUp.REMOVE_UNNECESSARY_BLOCKS);
		ICleanUp cleanUp2= new ExpressionsCleanUp(ExpressionsCleanUp.REMOVE_UNNECESSARY_PARENTHESIS);
		refactoring.addCleanUp(cleanUp1);
		refactoring.addCleanUp(cleanUp2);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);

		ICleanUp cleanUp= new ExpressionsCleanUp(ExpressionsCleanUp.REMOVE_UNNECESSARY_PARENTHESIS);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);

		ICleanUp cleanUp= new ExpressionsCleanUp(ExpressionsCleanUp.REMOVE_UNNECESSARY_PARENTHESIS);
		refactoring.addCleanUp(cleanUp);
		
		assertRefactoringHasNoChange(refactoring);
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
	
		ICleanUp cleanUp= new ExpressionsCleanUp(ExpressionsCleanUp.REMOVE_UNNECESSARY_PARENTHESIS);
		refactoring.addCleanUp(cleanUp);
		
		assertRefactoringHasNoChange(refactoring);
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
	
		ICleanUp cleanUp= new ExpressionsCleanUp(ExpressionsCleanUp.REMOVE_UNNECESSARY_PARENTHESIS);
		refactoring.addCleanUp(cleanUp);
		
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public boolean foo(String s1, String s2, boolean a, boolean b) {\n");
		buf.append("        return a == b == (s1 == s2);\n");
		buf.append("    }\n");
		buf.append("}\n");
		
		assertRefactoringResultAsExpected(refactoring, new String[] {buf.toString()});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
	
		ICleanUp cleanUp= new ExpressionsCleanUp(ExpressionsCleanUp.REMOVE_UNNECESSARY_PARENTHESIS);
		refactoring.addCleanUp(cleanUp);
		
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public String foo() {\n");
		buf.append("        return \"\" + 3 + (3 + 3);\n");
		buf.append("    }\n");
		buf.append("}\n");
		
		assertRefactoringResultAsExpected(refactoring, new String[] {buf.toString()});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.REMOVE_THIS_FIELD_QUALIFIER);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.REMOVE_THIS_METHOD_QUALIFIER);
		refactoring.addCleanUp(cleanUp);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public int foo() {return 0;}\n");
		buf.append("    public int getFoo() {\n");
		buf.append("        return foo();\n");
		buf.append("    }   \n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.REMOVE_THIS_FIELD_QUALIFIER);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.REMOVE_THIS_METHOD_QUALIFIER);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new CodeStyleCleanUp(CodeStyleCleanUp.REMOVE_THIS_METHOD_QUALIFIER);
		refactoring.addCleanUp(cleanUp);
		
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        setEnabled(true);\n");
		buf.append("    }\n");
		buf.append("    private void setEnabled(boolean b) {}\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new VariableDeclarationCleanUp(VariableDeclarationCleanUp.ADD_FINAL_MODIFIER_FIELDS | VariableDeclarationCleanUp.ADD_FINAL_MODIFIER_LOCAL_VARIABLES | VariableDeclarationCleanUp.ADD_FINAL_MODIFIER_PARAMETERS);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
	}
	
	public void testAddFinal02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private Object obj1;\n");
		buf.append("    protected Object obj2;\n");
		buf.append("    Object obj3;\n");
		buf.append("    public Object obj4;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new VariableDeclarationCleanUp(VariableDeclarationCleanUp.ADD_FINAL_MODIFIER_FIELDS);
		refactoring.addCleanUp(cleanUp);
		
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private final Object obj1;\n");
		buf.append("    protected Object obj2;\n");
		buf.append("    Object obj3;\n");
		buf.append("    public Object obj4;\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new VariableDeclarationCleanUp(VariableDeclarationCleanUp.ADD_FINAL_MODIFIER_LOCAL_VARIABLES);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new VariableDeclarationCleanUp(VariableDeclarationCleanUp.ADD_FINAL_MODIFIER_PARAMETERS);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new VariableDeclarationCleanUp(VariableDeclarationCleanUp.ADD_FINAL_MODIFIER_LOCAL_VARIABLES);
		refactoring.addCleanUp(cleanUp);
		refactoring.addCleanUp(new ExpressionsCleanUp(ExpressionsCleanUp.ADD_PARANOIC_PARENTHESIS));
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new VariableDeclarationCleanUp(VariableDeclarationCleanUp.ADD_FINAL_MODIFIER_PARAMETERS);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new VariableDeclarationCleanUp(VariableDeclarationCleanUp.ADD_FINAL_MODIFIER_FIELDS);
		refactoring.addCleanUp(cleanUp);
		
		assertRefactoringHasNoChange(refactoring);
	}
	
	public void testAddFinalBug134676_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E<T> { \n");
		buf.append("    private String s;\n");
		buf.append("    private T t;\n");
		buf.append("    private T t2;\n");
		buf.append("    void setT(T t) {\n");
		buf.append("        this.t = t;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new VariableDeclarationCleanUp(VariableDeclarationCleanUp.ADD_FINAL_MODIFIER_FIELDS);
		refactoring.addCleanUp(cleanUp);
		
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E<T> { \n");
		buf.append("    private final String s;\n");
		buf.append("    private T t;\n");
		buf.append("    private final T t2;\n");
		buf.append("    void setT(T t) {\n");
		buf.append("        this.t = t;\n");
		buf.append("    }\n");
		buf.append("}\n");
		
		assertRefactoringResultAsExpected(refactoring, new String[] {buf.toString()});
	}

	public void testAddFinalBug145028() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private volatile int field;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new VariableDeclarationCleanUp(VariableDeclarationCleanUp.ADD_FINAL_MODIFIER_FIELDS);
		refactoring.addCleanUp(cleanUp);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private final int field;\n");
		buf.append("}\n");
		
		assertRefactoringResultAsExpected(refactoring, new String[] {buf.toString()});
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
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		ICleanUp cleanUp= new ControlStatementsCleanUp(ControlStatementsCleanUp.REMOVE_UNNECESSARY_BLOCKS_CONTAINING_RETURN_OR_THROW);
		refactoring.addCleanUp(cleanUp);
		
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
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
	}
	
}

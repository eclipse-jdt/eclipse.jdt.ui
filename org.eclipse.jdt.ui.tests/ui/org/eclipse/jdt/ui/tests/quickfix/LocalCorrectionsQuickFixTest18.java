/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.ArrayList;
import java.util.Hashtable;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.Java18ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import junit.framework.Test;
import junit.framework.TestSuite;

public class LocalCorrectionsQuickFixTest18 extends QuickFixTest {

	private static final Class<LocalCorrectionsQuickFixTest18> THIS= LocalCorrectionsQuickFixTest18.class;

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;


	public LocalCorrectionsQuickFixTest18(String name) {
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
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, String.valueOf(99));
		options.put(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_MISSING_HASHCODE_METHOD, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_REDUNDANT_TYPE_ARGUMENTS, JavaCore.WARNING);

		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		StubUtility.setCodeTemplate(CodeTemplateContextType.CATCHBLOCK_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "", null);

		fJProject1= Java18ProjectTestSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	@Override
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, Java18ProjectTestSetup.getDefaultClasspath());
	}

	public void testUncaughtExceptionTypeUseAnnotation1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    void test(int a) {\n");
		buf.append("        throw new @Marker FileNotFoundException();\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("@interface Marker { }\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    void test(int a) throws @Marker FileNotFoundException {\n");
		buf.append("        throw new @Marker FileNotFoundException();\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("@interface Marker { }\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    void test(int a) {\n");
		buf.append("        try {\n");
		buf.append("            throw new @Marker FileNotFoundException();\n");
		buf.append("        } catch (@Marker FileNotFoundException e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("@interface Marker { }\n");
		String expected2= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
	}

	public void testOverrideDefaultMethod1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface FI1 {\n");
		buf.append("    default void foo(int i, String s) {}\n");
		buf.append("}\n");
		buf.append("interface FI2 {\n");
		buf.append("    default void foo(int j, String s) {}\n");
		buf.append("}\n");
		buf.append("public class E1 implements FI1, FI2 {\n");
		buf.append("\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface FI1 {\n");
		buf.append("    default void foo(int i, String s) {}\n");
		buf.append("}\n");
		buf.append("interface FI2 {\n");
		buf.append("    default void foo(int j, String s) {}\n");
		buf.append("}\n");
		buf.append("public class E1 implements FI1, FI2 {\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public void foo(int j, String s) {\n");
		buf.append("        FI2.super.foo(j, s);\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface FI1 {\n");
		buf.append("    default void foo(int i, String s) {}\n");
		buf.append("}\n");
		buf.append("interface FI2 {\n");
		buf.append("    default void foo(int j, String s) {}\n");
		buf.append("}\n");
		buf.append("public class E1 implements FI1, FI2 {\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public void foo(int i, String s) {\n");
		buf.append("        FI1.super.foo(i, s);\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testOverrideDefaultMethod2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface FI1 {\n");
		buf.append("    default void foo(int i, String s) {}\n");
		buf.append("}\n");
		buf.append("interface FI2 {\n");
		buf.append("    default void foo(int j, String s) {}\n");
		buf.append("}\n");
		buf.append("public interface E1 extends FI1, FI2 {\n");
		buf.append("    \n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface FI1 {\n");
		buf.append("    default void foo(int i, String s) {}\n");
		buf.append("}\n");
		buf.append("interface FI2 {\n");
		buf.append("    default void foo(int j, String s) {}\n");
		buf.append("}\n");
		buf.append("public interface E1 extends FI1, FI2 {\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    default void foo(int j, String s) {\n");
		buf.append("        FI2.super.foo(j, s);\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface FI1 {\n");
		buf.append("    default void foo(int i, String s) {}\n");
		buf.append("}\n");
		buf.append("interface FI2 {\n");
		buf.append("    default void foo(int j, String s) {}\n");
		buf.append("}\n");
		buf.append("public interface E1 extends FI1, FI2 {\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    default void foo(int i, String s) {\n");
		buf.append("        FI1.super.foo(i, s);\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testOverrideDefaultMethod3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface FI1 {\n");
		buf.append("    default void foo(int i, String s) {}\n");
		buf.append("}\n");
		buf.append("interface FI2 {\n");
		buf.append("    default void foo(int j, String s) {}\n");
		buf.append("}\n");
		buf.append("public enum E1 implements FI1, FI2 {\n");
		buf.append("    \n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface FI1 {\n");
		buf.append("    default void foo(int i, String s) {}\n");
		buf.append("}\n");
		buf.append("interface FI2 {\n");
		buf.append("    default void foo(int j, String s) {}\n");
		buf.append("}\n");
		buf.append("public enum E1 implements FI1, FI2 {\n");
		buf.append("    ;\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public void foo(int j, String s) {\n");
		buf.append("        FI2.super.foo(j, s);\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface FI1 {\n");
		buf.append("    default void foo(int i, String s) {}\n");
		buf.append("}\n");
		buf.append("interface FI2 {\n");
		buf.append("    default void foo(int j, String s) {}\n");
		buf.append("}\n");
		buf.append("public enum E1 implements FI1, FI2 {\n");
		buf.append("    ;\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public void foo(int i, String s) {\n");
		buf.append("        FI1.super.foo(i, s);\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testOverrideDefaultMethod4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface I1 {\n");
		buf.append("    default void m1() { }\n");
		buf.append("}\n");
		buf.append("interface I2<T2> {\n");
		buf.append("    void m1();\n");
		buf.append("}\n");
		buf.append("interface I22 extends I2<String> { }\n");
		buf.append("interface Both extends I1, I22 {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("I1.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface I1 {\n");
		buf.append("    default void m1() { }\n");
		buf.append("}\n");
		buf.append("interface I2<T2> {\n");
		buf.append("    void m1();\n");
		buf.append("}\n");
		buf.append("interface I22 extends I2<String> { }\n");
		buf.append("interface Both extends I1, I22 {\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    default void m1() {\n");
		buf.append("        I1.super.m1();\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface I1 {\n");
		buf.append("    default void m1() { }\n");
		buf.append("}\n");
		buf.append("interface I2<T2> {\n");
		buf.append("    void m1();\n");
		buf.append("}\n");
		buf.append("interface I22 extends I2<String> { }\n");
		buf.append("interface Both extends I1, I22 {\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    default void m1() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testOverrideDefaultMethod_multiLevel() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.List;\n");
		buf.append("\n");
		buf.append("interface I1<T1> {\n");
		buf.append("    default void def(T1 t1) { }\n");
		buf.append("}\n");
		buf.append("interface I2<T2> {\n");
		buf.append("    default void def(T2 t2) { }\n");
		buf.append("}\n");
		buf.append("interface I22<T22> extends I2<T22> { }\n");
		buf.append("interface Both extends I1<List<String>>, I22<List<String>> {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("I1.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.List;\n");
		buf.append("\n");
		buf.append("interface I1<T1> {\n");
		buf.append("    default void def(T1 t1) { }\n");
		buf.append("}\n");
		buf.append("interface I2<T2> {\n");
		buf.append("    default void def(T2 t2) { }\n");
		buf.append("}\n");
		buf.append("interface I22<T22> extends I2<T22> { }\n");
		buf.append("interface Both extends I1<List<String>>, I22<List<String>> {\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    default void def(List<String> t2) {\n");
		buf.append("        I22.super.def(t2);\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.List;\n");
		buf.append("\n");
		buf.append("interface I1<T1> {\n");
		buf.append("    default void def(T1 t1) { }\n");
		buf.append("}\n");
		buf.append("interface I2<T2> {\n");
		buf.append("    default void def(T2 t2) { }\n");
		buf.append("}\n");
		buf.append("interface I22<T22> extends I2<T22> { }\n");
		buf.append("interface Both extends I1<List<String>>, I22<List<String>> {\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    default void def(List<String> t1) {\n");
		buf.append("        I1.super.def(t1);\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testOverrideDefaultMethod_noParam() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface I1 {\n");
		buf.append("    default void def() { }\n");
		buf.append("}\n");
		buf.append("interface I2<T2> {\n");
		buf.append("    default void def() { }\n");
		buf.append("}\n");
		buf.append("interface I22 extends I2<String> { }\n");
		buf.append("interface Both extends I1, I22 {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("I1.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface I1 {\n");
		buf.append("    default void def() { }\n");
		buf.append("}\n");
		buf.append("interface I2<T2> {\n");
		buf.append("    default void def() { }\n");
		buf.append("}\n");
		buf.append("interface I22 extends I2<String> { }\n");
		buf.append("interface Both extends I1, I22 {\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    default void def() {\n");
		buf.append("        I22.super.def();\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface I1 {\n");
		buf.append("    default void def() { }\n");
		buf.append("}\n");
		buf.append("interface I2<T2> {\n");
		buf.append("    default void def() { }\n");
		buf.append("}\n");
		buf.append("interface I22 extends I2<String> { }\n");
		buf.append("interface Both extends I1, I22 {\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    default void def() {\n");
		buf.append("        I1.super.def();\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

}

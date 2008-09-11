/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.text.correction.proposals.CUCorrectionProposal;

public class JavadocQuickFixTest extends QuickFixTest {

	private static final Class THIS= JavadocQuickFixTest.class;

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public JavadocQuickFixTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return setUpTest(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}

	public static Test suite() {
		return allTests();
	}

	protected void setUp() throws Exception {
		Hashtable options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.COMPILER_PB_INVALID_JAVADOC, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_INVALID_JAVADOC_TAGS, JavaCore.ENABLED);
		options.put(JavaCore.COMPILER_PB_MISSING_JAVADOC_TAGS, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_MISSING_JAVADOC_COMMENTS, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_MISSING_JAVADOC_COMMENTS_OVERRIDING, JavaCore.ENABLED);
		JavaCore.setOptions(options);

		StringBuffer comment= new StringBuffer();
		comment.append("/**\n");
		comment.append(" * A comment.\n");
		comment.append(" * ${tags}\n");
		comment.append(" */");
		String res= comment.toString();
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORCOMMENT_ID, res, null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODCOMMENT_ID, res, null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, res, null);

		comment= new StringBuffer();
		comment.append("/**\n");
		comment.append(" * A field comment for ${field}.\n");
		comment.append(" */");
		StubUtility.setCodeTemplate(CodeTemplateContextType.FIELDCOMMENT_ID, comment.toString(), null);

		comment= new StringBuffer();
		comment.append("/**\n");
		comment.append(" * A override comment.\n");
		comment.append(" * ${see_to_overridden}\n");
		comment.append(" */");
		StubUtility.setCodeTemplate(CodeTemplateContextType.OVERRIDECOMMENT_ID, comment.toString(), null);

		comment= new StringBuffer();
		comment.append("/**\n");
		comment.append(" * A delegate comment.\n");
		comment.append(" * ${see_to_target}\n");
		comment.append(" */");
		StubUtility.setCodeTemplate(CodeTemplateContextType.DELEGATECOMMENT_ID, comment.toString(), null);

		fJProject1= ProjectTestSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}


	public void testMissingParam1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param b\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @param c\n");
		buf.append("     */\n");
		buf.append("    public void foo(int a, int b, int c) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a \n");
		buf.append("     * @param b\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @param c\n");
		buf.append("     */\n");
		buf.append("    public void foo(int a, int b, int c) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
		assertEqualString(preview2, expected);
	}

	public void testMissingParam2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @param c\n");
		buf.append("     */\n");
		buf.append("    public void foo(int a, int b, int c) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @param b \n");
		buf.append("     * @param c\n");
		buf.append("     */\n");
		buf.append("    public void foo(int a, int b, int c) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
		assertEqualString(preview2, expected);
	}
	public void testMissingParam3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @param b\n");
		buf.append("     */\n");
		buf.append("    public void foo(int a, int b, int c) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @param b\n");
		buf.append("     * @param c \n");
		buf.append("     */\n");
		buf.append("    public void foo(int a, int b, int c) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
		assertEqualString(preview2, expected);
	}

	public void testMissingParam4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param <A>\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @param a\n");
		buf.append("     */\n");
		buf.append("    public <A, B> void foo(int a) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param <A>\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @param <B> \n");
		buf.append("     * @param a\n");
		buf.append("     */\n");
		buf.append("    public <A, B> void foo(int a) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
		assertEqualString(preview2, expected);
	}

	public void testMissingParam5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" * @param <B> Hello\n");
		buf.append(" */\n");
		buf.append("public class E<A, B> {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" * @param <A> \n");
		buf.append(" * @param <B> Hello\n");
		buf.append(" */\n");
		buf.append("public class E<A, B> {\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
		assertEqualString(preview2, expected);
	}

	public void testMissingParam6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" * @author ae\n");
		buf.append(" */\n");
		buf.append("public class E<A> {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" * @author ae\n");
		buf.append(" * @param <A> \n");
		buf.append(" */\n");
		buf.append("public class E<A> {\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
		assertEqualString(preview2, expected);
	}

	public void testMissingReturn1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param b\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @param c\n");
		buf.append("     */\n");
		buf.append("    public int foo(int b, int c) {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param b\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @param c\n");
		buf.append("     * @return \n");
		buf.append("     */\n");
		buf.append("    public int foo(int b, int c) {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
		assertEqualString(preview2, expected);
	}

	public void testMissingReturn2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     */\n");
		buf.append("    public int foo() {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @return \n");
		buf.append("     */\n");
		buf.append("    public int foo() {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
		assertEqualString(preview2, expected);
	}

	public void testMissingReturn3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @throws Exception\n");
		buf.append("     */\n");
		buf.append("    public int foo() throws Exception {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @return \n");
		buf.append("     * @throws Exception\n");
		buf.append("     */\n");
		buf.append("    public int foo() throws Exception {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
		assertEqualString(preview2, expected);
	}

	public void testMissingThrows() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @return Returns an Int\n");
		buf.append("     */\n");
		buf.append("    public int foo() throws Exception {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @return Returns an Int\n");
		buf.append("     * @throws Exception \n");
		buf.append("     */\n");
		buf.append("    public int foo() throws Exception {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
		assertEqualString(preview2, expected);
	}

	public void testInsertAllMissing1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @throws Exception\n");
		buf.append("     */\n");
		buf.append("    public int foo(int a, int b) throws NullPointerException, Exception {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ArrayList proposals= collectCorrections2(cu, 4);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a \n");
		buf.append("     * @param b \n");
		buf.append("     * @return \n");
		buf.append("     * @throws NullPointerException \n");
		buf.append("     * @throws Exception\n");
		buf.append("     */\n");
		buf.append("    public int foo(int a, int b) throws NullPointerException, Exception {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	public void testInsertAllMissing2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param b\n");
		buf.append("     * @return a number\n");
		buf.append("     */\n");
		buf.append("    public int foo(int a, int b, int c) throws NullPointerException, Exception {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ArrayList proposals= collectCorrections2(cu, 4);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a \n");
		buf.append("     * @param b\n");
		buf.append("     * @param c \n");
		buf.append("     * @return a number\n");
		buf.append("     * @throws NullPointerException \n");
		buf.append("     * @throws Exception \n");
		buf.append("     */\n");
		buf.append("    public int foo(int a, int b, int c) throws NullPointerException, Exception {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	public void testInsertAllMissing3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E<S, T> {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ArrayList proposals= collectCorrections2(cu, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" * @param <S> \n");
		buf.append(" * @param <T> \n");
		buf.append(" */\n");
		buf.append("public class E<S, T> {\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	public void testInsertAllMissing4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param <B> test\n");
		buf.append("     * @param b\n");
		buf.append("     * @return a number\n");
		buf.append("     */\n");
		buf.append("    public <A, B> int foo(int a, int b) throws NullPointerException {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ArrayList proposals= collectCorrections2(cu, 3);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param <A> \n");
		buf.append("     * @param <B> test\n");
		buf.append("     * @param a \n");
		buf.append("     * @param b\n");
		buf.append("     * @return a number\n");
		buf.append("     * @throws NullPointerException \n");
		buf.append("     */\n");
		buf.append("    public <A, B> int foo(int a, int b) throws NullPointerException {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });	}

	public void testRemoveParamTag1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @param c\n");
		buf.append("     */\n");
		buf.append("    public void foo(int c) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param c\n");
		buf.append("     */\n");
		buf.append("    public void foo(int c) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	public void testRemoveParamTag2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @param a\n");
		buf.append("     */\n");
		buf.append("    public void foo(int a) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     */\n");
		buf.append("    public void foo(int a) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}


	public void testRemoveThrowsTag1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @param c\n");
		buf.append("     * @throws Exception Thrown by surprise.\n");
		buf.append("     */\n");
		buf.append("    public void foo(int a, int c) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @param c\n");
		buf.append("     */\n");
		buf.append("    public void foo(int a, int c) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	public void testRemoveThrowsTag2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @exception Exception\n");
		buf.append("     */\n");
		buf.append("    public void foo(int a) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     */\n");
		buf.append("    public void foo(int a) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	public void testRemoveThrowsTag3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @exception Exception\n");
		buf.append("     * @exception java.io.IOException\n");
		buf.append("     * @exception NullPointerException\n");
		buf.append("     */\n");
		buf.append("    public void foo(int a) throws IOException {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @exception java.io.IOException\n");
		buf.append("     * @exception NullPointerException\n");
		buf.append("     */\n");
		buf.append("    public void foo(int a) throws IOException {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	public void testRemoveReturnTag1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @return Returns the result.\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @exception Exception\n");
		buf.append("     */\n");
		buf.append("    public void foo(int a) throws Exception {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @exception Exception\n");
		buf.append("     */\n");
		buf.append("    public void foo(int a) throws Exception {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	public void testRemoveUnknownTag1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @return Returns the result.\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @exception Exception\n");
		buf.append("     */\n");
		buf.append("    public void foo(int a) throws Exception {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @exception Exception\n");
		buf.append("     */\n");
		buf.append("    public void foo(int a) throws Exception {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	public void testMissingMethodComment1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    public <A> void foo(int a) throws IOException {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * A comment.\n");
		buf.append("     * @param <A>\n");
		buf.append("     * @param a\n");
		buf.append("     * @throws IOException\n");
		buf.append("     */\n");
		buf.append("    public <A> void foo(int a) throws IOException {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	public void testMissingMethodComment2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    public String toString() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * A override comment.\n");
		buf.append("     * @see java.lang.Object#toString()\n");
		buf.append("     */\n");
		buf.append("    public String toString() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	public void testMissingMethodComment3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    public void empty() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * A comment.\n");
		buf.append("     */\n");
		buf.append("    public void empty() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	public void testMissingMethodComment4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class B extends A<Integer> {\n");
		buf.append("    public void foo(Integer x) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class A<T extends Number> {\n");
		buf.append("    /**\n");
		buf.append("     * @param x\n");
		buf.append("     */\n");
		buf.append("    public void foo(T x) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("B.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class B extends A<Integer> {\n");
		buf.append("    /**\n");
		buf.append("     * A override comment.\n");
		buf.append("     * @see pack.A#foo(java.lang.Number)\n");
		buf.append("     */\n");
		buf.append("    public void foo(Integer x) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class A<T extends Number> {\n");
		buf.append("    /**\n");
		buf.append("     * @param x\n");
		buf.append("     */\n");
		buf.append("    public void foo(T x) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
		}


	public void testMissingConstructorComment() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    public E(int a) throws IOException {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * A comment.\n");
		buf.append("     * @param a\n");
		buf.append("     * @throws IOException\n");
		buf.append("     */\n");
		buf.append("    public E(int a) throws IOException {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	public void testMissingTypeComment() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E<A, B> {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" * A comment.\n");
		buf.append(" * @param <A>\n");
		buf.append(" * @param <B>\n");
		buf.append(" */\n");
		buf.append("public class E<A, B> {\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	public void testMissingFieldComment() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    public static final int COLOR= 1;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * A field comment for COLOR.\n");
		buf.append("     */\n");
		buf.append("    public static final int COLOR= 1;\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	public void testInvalidQualification1() throws Exception {
		Map original= fJProject1.getOptions(false);
		HashMap newOptions= new HashMap(original);
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_4, newOptions);
		fJProject1.setOptions(newOptions);

		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
			StringBuffer buf= new StringBuffer();
			buf.append("package pack;\n");
			buf.append("\n");
			buf.append("public class A {\n");
			buf.append("    public static class B {\n");
			buf.append("        public static class C {\n");
			buf.append("            \n");
			buf.append("        }\n");
			buf.append("    }\n");
			buf.append("}\n");
			pack1.createCompilationUnit("A.java", buf.toString(), false, null);

			IPackageFragment pack2= fSourceFolder.createPackageFragment("pack2", false, null);
			buf= new StringBuffer();
			buf.append("package pack2;\n");
			buf.append("\n");
			buf.append("import pack.A.B.C;\n");
			buf.append("\n");
			buf.append("/**\n");
			buf.append(" * {@link C} \n");
			buf.append(" */\n");
			buf.append("public class E {\n");
			buf.append("}\n");
			ICompilationUnit cu= pack2.createCompilationUnit("E.java", buf.toString(), false, null);

			CompilationUnit astRoot= getASTRoot(cu);
			ArrayList proposals= collectCorrections(cu, astRoot);

			assertCorrectLabels(proposals);
			assertNumberOfProposals(proposals, 1);

			String[] expected= new String[1];
			buf= new StringBuffer();
			buf.append("package pack2;\n");
			buf.append("\n");
			buf.append("import pack.A;\n");
			buf.append("import pack.A.B.C;\n");
			buf.append("\n");
			buf.append("/**\n");
			buf.append(" * {@link A.B.C} \n");
			buf.append(" */\n");
			buf.append("public class E {\n");
			buf.append("}\n");
			expected[0]= buf.toString();

			assertExpectedExistInProposals(proposals, expected);
		} finally{
			fJProject1.setOptions(original);
		}
	}


}

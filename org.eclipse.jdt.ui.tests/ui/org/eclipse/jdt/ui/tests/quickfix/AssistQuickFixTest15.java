/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
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
 *     Red Hat Inc. - modified to test Java 15 quick assists
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.ArrayList;
import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.fix.FixMessages;

import org.eclipse.jdt.ui.tests.core.rules.Java15ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public class AssistQuickFixTest15 extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup = new Java15ProjectTestSetup(true);

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
		if (fJProject1 != null) {
			JavaProjectHelper.delete(fJProject1);
		}

	}

	@Test
	public void testConcatToTextBlock1() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        // comment 1\n");
		buf.append("        String x = \"\" + //$NON-NLS-1$\n");
        buf.append("            \"public void foo() {\\n\" + //$NON-NLS-1$\n");
        buf.append("            \"    System.out.println(\\\"abc\\\");\\n\" + //$NON-NLS-1$\n");
        buf.append("            \"}\\n\"; //$NON-NLS-1$ // comment 2\n");
        buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int index= buf.indexOf("x");
		IInvocationContext ctx= getCorrectionContext(cu, index, 1);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        // comment 1\n");
		buf.append("        String x = \"\"\"\n");
        buf.append("		\tpublic void foo() {\n");
        buf.append("		\t    System.out.println(\"abc\");\n");
        buf.append("		\t}\n");
        buf.append("		\t\"\"\"; //$NON-NLS-1$ // comment 2\n");
        buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertProposalExists(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToTextBlock2() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        // comment 1\n");
		buf.append("        String x = \"\" +\n");
        buf.append("            \"public void foo() { \\n\" +\n");
        buf.append("            \"    System.out.println(\\\"abc\\\");\\n\" +\n");
        buf.append("            \"}\\n\"; // comment 2\n");
        buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int index= buf.indexOf("System");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        // comment 1\n");
		buf.append("        String x = \"\"\"\n");
        buf.append("		\tpublic void foo() {\\s\n");
        buf.append("		\t    System.out.println(\"abc\");\n");
        buf.append("		\t}\n");
        buf.append("		\t\"\"\"; // comment 2\n");
        buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertProposalExists(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToTextBlock3() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        // comment 1\n");
		buf.append("        String x = \"\" +\n");
        buf.append("            \"public void foo() { \\n\" +\n");
        buf.append("            \"    System.out.println(\\\"\\\"\\\"abc\\\"\\\"\\\");\\n\" +\n");
        buf.append("            \"}\\n\"; // comment 2\n");
        buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int index= buf.indexOf("System");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        // comment 1\n");
		buf.append("        String x = \"\"\"\n");
        buf.append("		\tpublic void foo() {\\s\n");
        buf.append("		\t    System.out.println(\\\"\"\"abc\\\"\"\");\n");
        buf.append("		\t}\n");
        buf.append("		\t\"\"\"; // comment 2\n");
        buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertProposalExists(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToTextBlock4() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        // comment 1\n");
		buf.append("        String x = \"\" +\n");
        buf.append("            \"abcdef\" +\n");
        buf.append("            \"ghijkl\\\"\\\"\\\"123\\\"\\\"\\\"\" +\n");
        buf.append("            \"mnop\";\n");
        buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int index= buf.indexOf("abcdef");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        // comment 1\n");
		buf.append("        String x = \"\"\"\n");
        buf.append("		\tabcdef\\\n");
        buf.append("		\tghijkl\\\"\"\"123\\\"\"\"\\\n");
        buf.append("		\tmnop\"\"\";\n");
        buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertProposalExists(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToTextBlock5() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        // comment 1\n");
		buf.append("        StringBuffer buf= new StringBuffer(\"intro string\\n\");\n");
		buf.append("        buf.append(\"public void foo() {\\n\");\n");
		buf.append("        buf.append(\"    return null;\\n\");\n");
		buf.append("        buf.append(\"}\\n\");\n");
		buf.append("        buf.append(\"\\n\");\n");
		buf.append("        System.out.println(buf.toString());\n");
		buf.append("        System.out.println(buf.toString() + \"abc\");\n");
		buf.append("        // comment 2\n");
		buf.append("        buf = new StringBuffer(\"intro string 2\\n\");\n");
		buf.append("        buf.append(\"some string\\n\");\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int index= buf.indexOf("buf");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        // comment 1\n");
		buf.append("        String str = \"\"\"\n");
		buf.append("		\tintro string\n");
		buf.append("		\tpublic void foo() {\n");
		buf.append("		\t    return null;\n");
		buf.append("		\t}\n");
		buf.append("		\t\n");
		buf.append("		\t\"\"\";\n");
		buf.append("        System.out.println(str);\n");
		buf.append("        System.out.println(str + \"abc\");\n");
		buf.append("        // comment 2\n");
		buf.append("        StringBuffer buf = new StringBuffer(\"intro string 2\\n\");\n");
		buf.append("        buf.append(\"some string\\n\");\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertProposalExists(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToTextBlock6() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        StringBuilder buf3= new StringBuilder();\n");
		buf.append("        buf3.append(\"public void foo() {\\n\");\n");
		buf.append("        buf3.append(\"    return null;\\n\");\n");
		buf.append("        buf3.append(\"}\\n\");\n");
		buf.append("        buf3.append(\"\\n\");\n");
		buf.append("        // comment 1\n");
		buf.append("        String k = buf3.toString();\n");
		buf.append("        \n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int index= buf.indexOf("buf");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        // comment 1\n");
		buf.append("        String k = \"\"\"\n");
		buf.append("		\tpublic void foo() {\n");
		buf.append("		\t    return null;\n");
		buf.append("		\t}\n");
		buf.append("		\t\n");
		buf.append("		\t\"\"\";\n");
		buf.append("        \n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertProposalExists(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToTextBlock7() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        StringBuilder buf3= new StringBuilder();\n");
		buf.append("        buf3.append(\"public void foo() {\\n\"); //$NON-NLS-1$\n");
		buf.append("        buf3.append(\"    return null;\\n\"); //$NON-NLS-1$\n");
		buf.append("        buf3.append(\"}\\n\"); //$NON-NLS-1$\n");
		buf.append("        buf3.append(\"\\n\"); //$NON-NLS-1$\n");
		buf.append("        // comment 1\n");
		buf.append("        String k = buf3.toString();\n");
		buf.append("        \n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int index= buf.indexOf("buf3");
		IInvocationContext ctx= getCorrectionContext(cu, index, 4);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String str = \"\"\"\n");
		buf.append("		\tpublic void foo() {\n");
		buf.append("		\t    return null;\n");
		buf.append("		\t}\n");
		buf.append("		\t\n");
		buf.append("		\t\"\"\"; //$NON-NLS-1$\n");
		buf.append("        // comment 1\n");
		buf.append("        String k = str;\n");
		buf.append("        \n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertProposalExists(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToTextBlock8() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        StringBuilder buf3= new StringBuilder();\n");
		buf.append("        buf3.append(\"public void foo() {\\n\"); //$NON-NLS-1$\n");
		buf.append("        buf3.append(\"    return null;\\n\"); //$NON-NLS-1$\n");
		buf.append("        buf3.append(\"}\\n\"); //$NON-NLS-1$\n");
		buf.append("        buf3.append(\"\\n\"); //$NON-NLS-1$\n");
		buf.append("        // comment 1\n");
		buf.append("        String k = buf3.toString();\n");
		buf.append("        \n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int index= buf.indexOf("StringBuilder");
		IInvocationContext ctx= getCorrectionContext(cu, index, 4);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String str = \"\"\"\n");
		buf.append("		\tpublic void foo() {\n");
		buf.append("		\t    return null;\n");
		buf.append("		\t}\n");
		buf.append("		\t\n");
		buf.append("		\t\"\"\"; //$NON-NLS-1$\n");
		buf.append("        // comment 1\n");
		buf.append("        String k = str;\n");
		buf.append("        \n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertProposalExists(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToTextBlock9() throws Exception { //https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/1111
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        // comment 1\n");
		buf.append("        String x = \"foo \\n\" + \"bar  \" + \"baz\" + \"biz\";\n");
        buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int index= buf.indexOf("x");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        // comment 1\n");
		buf.append("        String x = \"\"\"\n");
		buf.append("		\tfoo\\s\n");
		buf.append("		\tbar  \\\n");
		buf.append("		\tbaz\\\n");
		buf.append("		\tbiz\"\"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertProposalExists(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToTextBlock10() throws Exception { //https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/1111
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		JavaCore.setOptions(hashtable);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        // comment 1\n");
		buf.append("        String x = \"foo \\n\" + \"bar  \" + \"baz\" + \"biz\";\n");
        buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int index= buf.indexOf("x");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        // comment 1\n");
		buf.append("        String x = \"\"\"\n");
		buf.append("            foo\\s\n");
		buf.append("            bar  \\\n");
		buf.append("            baz\\\n");
		buf.append("            biz\"\"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertProposalExists(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToTextBlock11() throws Exception { //https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/1240
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		JavaCore.setOptions(hashtable);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        // comment 1\n");
		buf.append("        String x =\"\\tif (true) {\\n\" +\n");
		buf.append("                \"\\t\\tstuff();\\n\" +\n");
		buf.append("                \"\\t} else\\n\" +\n");
		buf.append("                \"\\t\\tnoStuff\";\n");
		buf.append("        System.out.println(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int index= buf.indexOf("x");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        // comment 1\n");
		buf.append("        String x =\"\"\"\n");
		buf.append("            \tif (true) {\n");
		buf.append("            \t\tstuff();\n");
		buf.append("            \t} else\n");
		buf.append("            \t\tnoStuff\\\n");
		buf.append("            \"\"\";\n");
		buf.append("        System.out.println(x);\n");		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertProposalExists(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testNoConcatToTextBlock1() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        // comment 1\n");
		buf.append("        String x = \n");
        buf.append("            \"abcdef\" +\n");
        buf.append("            \"ghijkl\";\n");
        buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int index= buf.indexOf("abcdef");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		assertProposalDoesNotExist(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
	}

	@Test
	public void testNoConcatToTextBlock2() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        // comment 1\n");
		buf.append("        String x = \n");
        buf.append("            \"abcdef\" +\n");
        buf.append("            \"ghijkl\" +\n");
        buf.append("            String.valueOf(true);\n");
        buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int index= buf.indexOf("abcdef");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		assertProposalDoesNotExist(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
	}

	@Test
	public void testNoConcatToTextBlock3() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        // comment 1\n");
		buf.append("        String x = \n");
        buf.append("            \"abcdef\" +\n");
        buf.append("            \"ghijkl\" +\n");
        buf.append("            3;\n");
        buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int index= buf.indexOf("abcdef");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		assertProposalDoesNotExist(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
	}

	@Test
	public void testNoConcatToTextBlock4() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo(String a) {\n");
		buf.append("        // comment 1\n");
		buf.append("        String x = \n");
        buf.append("            \"abcdef\" +\n");
        buf.append("            \"ghijkl\" +\n");
        buf.append("            a;\n");
        buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int index= buf.indexOf("abcdef");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		assertProposalDoesNotExist(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
	}

	@Test
	public void testNoConcatToTextBlock5() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void noToString() {\n");
		buf.append("        StringBuilder buf3= new StringBuilder();\n");
		buf.append("        buf3.append(\"public void foo() {\\n\");\n");
		buf.append("        buf3.append(\"    return null;\\n\");\n");
		buf.append("        buf3.append(\"}\\n\");\n");
		buf.append("        buf3.append(\"\\n\");\n");
		buf.append("        \n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int index= buf.indexOf("buf3");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		assertProposalDoesNotExist(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
	}

	@Test
	public void testNoConcatToTextBlock6() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void extraAppend() {\n");
		buf.append("        StringBuilder buf3= new StringBuilder();\n");
		buf.append("        buf3.append(\"public void foo() {\\n\");\n");
		buf.append("        buf3.append(\"    return null;\\n\");\n");
		buf.append("        buf3.append(\"}\\n\");\n");
		buf.append("        buf3.append(\"\\n\");\n");
		buf.append("        String k = buf3.toString();\n");
		buf.append("        buf3.append(\"extra stuff\\n\");\n");
		buf.append("        \n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int index= buf.indexOf("buf3");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		assertProposalDoesNotExist(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
	}

	@Test
	public void testNoConcatToTextBlock7() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void combinedAppendWithNLS() {\n");
		buf.append("        StringBuffer buf3= new StringBuffer();\n");
		buf.append("        buf3.append(\"public void foo() {\\n\"); //$NON-NLS-1$\n");
		buf.append("        buf3.append(\"    return null;\\n\"); //$NON-NLS-1$\n");
		buf.append("        buf3.append(\"}\\n\"); //$NON-NLS-1$\n");
		buf.append("        buf3.append(\"\\n\").append(\"extra append\\n\"); //$NON-NLS-1$ //$NON-NLS-2$\n");
		buf.append("        String k = buf3.toString();\n");
		buf.append("        \n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int index= buf.indexOf("buf3");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		assertProposalDoesNotExist(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
	}

	@Test
	public void testNoConcatToTextBlock8() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void inconsistentNLSMarkers() {\n");
		buf.append("        StringBuffer buf3= new StringBuffer();\n");
		buf.append("        buf3.append(\"public void foo() {\\n\"); //$NON-NLS-1$\n");
		buf.append("        buf3.append(\"    return null;\\n\");\n");
		buf.append("        buf3.append(\"}\\n\");\n");
		buf.append("        buf3.append(\"\\n\");\n");
		buf.append("        String k = buf3.toString();\n");
		buf.append("        \n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int index= buf.indexOf("buf3");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		assertProposalDoesNotExist(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
	}

	@Test
	public void testNoConcatToTextBlock9() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void inconsistentNLS() {\n");
		buf.append("        // comment 1\n");
		buf.append("        String x = \"\" + //$NON-NLS-1$\n");
        buf.append("            \"public void foo() {\\n\" +\n");
        buf.append("            \"    System.out.println(\\\"abc\\\");\\n\" + //$NON-NLS-1$\n");
        buf.append("            \"}\\n\"; //$NON-NLS-1$ // comment 2\n");
        buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int index= buf.indexOf("x");
		IInvocationContext ctx= getCorrectionContext(cu, index, 1);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		assertProposalDoesNotExist(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
	}

	@Test
	public void testNoConcatToTextBlock10() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void inconsistentNLS() {\n");
		buf.append("        // comment 1\n");
		buf.append("        String y = \"something\";\n");
		buf.append("        String x = \"\" +\n");
        buf.append("            \"public void foo() {\\n\" +\n");
        buf.append("            \"    System.out.println(\\\"abc\\\");\\n\" +\n");
        buf.append("                  y + \n");
        buf.append("            \"}\\n\"; //$NON-NLS-1$ // comment 2\n");
        buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		int index= buf.indexOf("x");
		IInvocationContext ctx= getCorrectionContext(cu, index, 1);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		assertProposalDoesNotExist(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
	}

	@Test
	public void testConcatToMessageFormatTextBlock1() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String statement= \" * statement\\n\";\n");
		buf.append("        // comment 1\n");
		buf.append("        String copyright=\n");
		buf.append("                \"/***********\\n\" +\n");
		buf.append("                \" * simple {} \\n\" +\n");
		buf.append("                \" * copyright\\n\" +\n");
		buf.append("                statement +\n");
		buf.append("                \" * notice {0}\\n\" +\n");
		buf.append("                \"***********/\\n\"; // comment 2\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);
		int index= buf.indexOf("simple");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("\n");
		buf.append("import java.text.MessageFormat;\n");
		buf.append("\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String statement= \" * statement\\n\";\n");
		buf.append("        // comment 1\n");
		buf.append("        String copyright=\n");
		buf.append("                MessageFormat.format(\"\"\"\n");
		buf.append("		                /***********\n");
		buf.append("		                 * simple '{}'\\s\n");
		buf.append("		                 * copyright\n");
		buf.append("		                {0}\\\n");
		buf.append("		                 * notice '{'0'}'\n");
		buf.append("		                ***********/\n");
		buf.append("		                \"\"\", statement); // comment 2\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertProposalExists(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_message_format);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToMessageFormatTextBlock2() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String statement= \" * statement\\n\";\n");
		buf.append("        // comment 1\n");
		buf.append("        String copyright=\n");
		buf.append("                \"/***********\\n\" + //$NON-NLS-1$\n");
		buf.append("                \" * simple   \\n\" + //$NON-NLS-1$\n");
		buf.append("                statement +\n");
		buf.append("                \" * copyright\\n\"; //$NON-NLS-1$\n");
		buf.append("        System.out.println(copyright);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);
		int index= buf.indexOf("simple");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("\n");
		buf.append("import java.text.MessageFormat;\n");
		buf.append("\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String statement= \" * statement\\n\";\n");
		buf.append("        // comment 1\n");
		buf.append("        String copyright=\n");
		buf.append("                MessageFormat.format(\"\"\"\n");
		buf.append("\t\t                /***********\n");
		buf.append("\t\t                 * simple  \\s\n");
		buf.append("\t\t                {0}\\\n");
		buf.append("\t\t                 * copyright\n");
		buf.append("\t\t                \"\"\", statement); //$NON-NLS-1$\n");
		buf.append("        System.out.println(copyright);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertProposalExists(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_message_format);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToMessageFormatTextBlock3() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String statement= \" * statement\\n\";\n");
		buf.append("        // comment 1\n");
		buf.append("        String copyright=\n");
		buf.append("                \"/***********\\n\" + //$NON-NLS-1$\n");
		buf.append("                \" * simple   \\n\" + //$NON-NLS-1$\n");
		buf.append("                \" * copyright\\n\" + //$NON-NLS-1$\n");
		buf.append("                statement;\n");
		buf.append("        System.out.println(copyright);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);
		int index= buf.indexOf("simple");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("\n");
		buf.append("import java.text.MessageFormat;\n");
		buf.append("\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String statement= \" * statement\\n\";\n");
		buf.append("        // comment 1\n");
		buf.append("\t\t        String copyright=\n");
		buf.append("\t\t                MessageFormat.format(\"\"\"\n");
		buf.append("\t\t                /***********\n");
		buf.append("\t\t                 * simple  \\s\n");
		buf.append("\t\t                 * copyright\n");
		buf.append("\t\t                {0}\"\"\", statement); //$NON-NLS-1$\n");
		buf.append("        System.out.println(copyright);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertProposalExists(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_message_format);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToMessageFormatTextBlock4() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String statement= \" * statement\\n\";\n");
		buf.append("        // comment 1\n");
		buf.append("        String copyright=\n");
		buf.append("                \"/***********\\n\" + //$NON-NLS-1$\n");
		buf.append("                \" * simple   \\n\" + //$NON-NLS-1$\n");
		buf.append("                \" * copyright\\n\" + //$NON-NLS-1$\n");
		buf.append("                statement + \" * notice\\n\" + \"***********/\\n\"; //comment 2 //$NON-NLS-1$ //$NON-NLS-2$\n");
		buf.append("        System.out.println(copyright);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);
		int index= buf.indexOf("simple");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("\n");
		buf.append("import java.text.MessageFormat;\n");
		buf.append("\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String statement= \" * statement\\n\";\n");
		buf.append("        // comment 1\n");
		buf.append("\t\t        String copyright=\n");
		buf.append("\t\t                MessageFormat.format(\"\"\"\n");
		buf.append("\t\t                /***********\n");
		buf.append("\t\t                 * simple  \\s\n");
		buf.append("\t\t                 * copyright\n");
		buf.append("\t\t                {0}\\\n");
		buf.append("\t\t                 * notice\n");
		buf.append("\t\t                ***********/\n");
		buf.append("\t\t                \"\"\", statement); //comment 2 //$NON-NLS-1$\n");
		buf.append("        System.out.println(copyright);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertProposalExists(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_message_format);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToMessageFormatTextBlock5() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo(String name, String id) {\n");
		buf.append("        String title = \"Name: \" + name + \" ID: \" + id;\n");
		buf.append("        System.out.println(title);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);
		int index= buf.indexOf("title");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("\n");
		buf.append("import java.text.MessageFormat;\n");
		buf.append("\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo(String name, String id) {\n");
		buf.append("        String title = MessageFormat.format(\"Name: {0} ID: {1}\", name, id);\n");
		buf.append("        System.out.println(title);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertProposalExists(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_string_format);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToStringFormatTextBlock1() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String statement= \" * statement\\n\";\n");
		buf.append("        // comment 1\n");
		buf.append("        String copyright=\n");
		buf.append("                \"/***********\\n\" +\n");
		buf.append("                \" * simple   \\n\" +\n");
		buf.append("                \" * copyright %\\n\" +\n");
		buf.append("                statement +\n");
		buf.append("                \" * notice\\n\" +\n");
		buf.append("                \"***********/\\n\"; // comment 2\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);
		int index= buf.indexOf("simple");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String statement= \" * statement\\n\";\n");
		buf.append("        // comment 1\n");
		buf.append("        String copyright=\n");
		buf.append("                String.format(\"\"\"\n");
		buf.append("		                /***********\n");
		buf.append("		                 * simple  \\s\n");
		buf.append("		                 * copyright %%\n");
		buf.append("		                %s\\\n");
		buf.append("		                 * notice\n");
		buf.append("		                ***********/\n");
		buf.append("		                \"\"\", statement); // comment 2\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertProposalExists(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_string_format);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToStringFormatTextBlock2() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String statement= \" * statement\\n\";\n");
		buf.append("        // comment 1\n");
		buf.append("        String copyright=\n");
		buf.append("                \"/***********\\n\" + //$NON-NLS-1$\n");
		buf.append("                \" * simple   \\n\" + //$NON-NLS-1$\n");
		buf.append("                statement +\n");
		buf.append("                \" * copyright\\n\"; //$NON-NLS-1$\n");
		buf.append("        System.out.println(copyright);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);
		int index= buf.indexOf("simple");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String statement= \" * statement\\n\";\n");
		buf.append("        // comment 1\n");
		buf.append("        String copyright=\n");
		buf.append("                String.format(\"\"\"\n");
		buf.append("\t\t                /***********\n");
		buf.append("\t\t                 * simple  \\s\n");
		buf.append("\t\t                %s\\\n");
		buf.append("\t\t                 * copyright\n");
		buf.append("\t\t                \"\"\", statement); //$NON-NLS-1$\n");
		buf.append("        System.out.println(copyright);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertProposalExists(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_string_format);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToStringFormatTextBlock3() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String statement= \" * statement\\n\";\n");
		buf.append("        // comment 1\n");
		buf.append("        String copyright=\n");
		buf.append("                \"/***********\\n\" + //$NON-NLS-1$\n");
		buf.append("                \" * simple   \\n\" + //$NON-NLS-1$\n");
		buf.append("                \" * copyright\\n\" + //$NON-NLS-1$\n");
		buf.append("                statement;\n");
		buf.append("        System.out.println(copyright);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);
		int index= buf.indexOf("simple");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String statement= \" * statement\\n\";\n");
		buf.append("        // comment 1\n");
		buf.append("\t\t        String copyright=\n");
		buf.append("\t\t                String.format(\"\"\"\n");
		buf.append("\t\t                /***********\n");
		buf.append("\t\t                 * simple  \\s\n");
		buf.append("\t\t                 * copyright\n");
		buf.append("\t\t                %s\"\"\", statement); //$NON-NLS-1$\n");
		buf.append("        System.out.println(copyright);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertProposalExists(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_string_format);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToStringFormatTextBlock4() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String statement= \" * statement\\n\";\n");
		buf.append("        // comment 1\n");
		buf.append("        String copyright=\n");
		buf.append("                \"/***********\\n\" + //$NON-NLS-1$\n");
		buf.append("                \" * simple   \\n\" + //$NON-NLS-1$\n");
		buf.append("                \" * copyright\\n\" + //$NON-NLS-1$\n");
		buf.append("                statement + \" * notice\\n\" + \"***********/\\n\"; //comment 2 //$NON-NLS-1$ //$NON-NLS-2$\n");
		buf.append("        System.out.println(copyright);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);
		int index= buf.indexOf("simple");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String statement= \" * statement\\n\";\n");
		buf.append("        // comment 1\n");
		buf.append("\t\t        String copyright=\n");
		buf.append("\t\t                String.format(\"\"\"\n");
		buf.append("\t\t                /***********\n");
		buf.append("\t\t                 * simple  \\s\n");
		buf.append("\t\t                 * copyright\n");
		buf.append("\t\t                %s\\\n");
		buf.append("\t\t                 * notice\n");
		buf.append("\t\t                ***********/\n");
		buf.append("\t\t                \"\"\", statement); //comment 2 //$NON-NLS-1$\n");
		buf.append("        System.out.println(copyright);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertProposalExists(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_string_format);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToStringFormatTextBlock5() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo(String name, String id) {\n");
		buf.append("        String title = \"Name: \" + name + \" ID: \" + id;\n");
		buf.append("        System.out.println(title);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);
		int index= buf.indexOf("title");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void foo(String name, String id) {\n");
		buf.append("        String title = String.format(\"Name: %s ID: %s\", name, id);\n");
		buf.append("        System.out.println(title);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertProposalExists(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_string_format);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

}

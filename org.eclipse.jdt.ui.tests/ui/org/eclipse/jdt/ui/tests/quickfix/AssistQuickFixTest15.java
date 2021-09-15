/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.fix.FixMessages;

import org.eclipse.jdt.ui.tests.core.rules.Java15ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

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
		buf.append("        String x = \"\" +\n");
        buf.append("            \"public void foo() {\\n\" +\n");
        buf.append("            \"    System.out.println(\\\"abc\\\");\\n\" +\n");
        buf.append("            \"}\\n\"; // comment 2\n");
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
        buf.append("		\t\"\"\"; // comment 2\n");
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

}


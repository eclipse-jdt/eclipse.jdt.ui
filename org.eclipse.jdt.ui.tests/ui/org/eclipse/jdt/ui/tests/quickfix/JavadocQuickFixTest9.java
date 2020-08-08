/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
 *     Red Hat Inc. - new Javadoc quickfix test based on QuickFixTest9
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.ArrayList;
import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;

import org.eclipse.jdt.ui.tests.core.rules.Java9ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

public class JavadocQuickFixTest9 extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup = new Java9ProjectTestSetup();

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.COMPILER_PB_INVALID_JAVADOC, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_INVALID_JAVADOC_TAGS, JavaCore.ENABLED);
		options.put(JavaCore.COMPILER_PB_MISSING_JAVADOC_TAGS_METHOD_TYPE_PARAMETERS, JavaCore.ENABLED);
		options.put(JavaCore.COMPILER_PB_MISSING_JAVADOC_TAGS, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_MISSING_JAVADOC_COMMENTS, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_MISSING_JAVADOC_COMMENTS_OVERRIDING, JavaCore.ENABLED);
		JavaCore.setOptions(options);

		StringBuilder comment= new StringBuilder();
		comment.append("/**\n");
		comment.append(" * A comment.\n");
		comment.append(" * ${tags}\n");
		comment.append(" */");
		String res= comment.toString();
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORCOMMENT_ID, res, null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODCOMMENT_ID, res, null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, res, null);

		comment= new StringBuilder();
		comment.append("/**\n");
		comment.append(" * A field comment for ${field}.\n");
		comment.append(" */");
		StubUtility.setCodeTemplate(CodeTemplateContextType.FIELDCOMMENT_ID, comment.toString(), null);

		comment= new StringBuilder();
		comment.append("/**\n");
		comment.append(" * A override comment.\n");
		comment.append(" * ${see_to_overridden}\n");
		comment.append(" */");
		StubUtility.setCodeTemplate(CodeTemplateContextType.OVERRIDECOMMENT_ID, comment.toString(), null);

		comment= new StringBuilder();
		comment.append("/**\n");
		comment.append(" * A delegate comment.\n");
		comment.append(" * ${see_to_target}\n");
		comment.append(" */");
		StubUtility.setCodeTemplate(CodeTemplateContextType.DELEGATECOMMENT_ID, comment.toString(), null);

		comment= new StringBuilder();
		comment.append("/**\n");
		comment.append(" * A module comment.\n");
		comment.append(" * ${tags}\n");
		comment.append(" */");
		StubUtility.setCodeTemplate(CodeTemplateContextType.MODULECOMMENT_ID, comment.toString(), null);

		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		JavaProjectHelper.set9CompilerOptions(fJProject1);
		JavaProjectHelper.addRequiredModularProject(fJProject1, projectSetup.getProject());

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testMissingModuleComment() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("public class MyFormattable implements Formattable {\n");
		buf.append("  @Override\n");
		buf.append("  public void formatTo(Formatter formatter, int flags, int width, int precision) {\n");
		buf.append("  }\n");
		buf.append("}\n");
		@SuppressWarnings("unused")
		ICompilationUnit cu1= pack.createCompilationUnit("MyFormattable.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
		buf= new StringBuilder();
		buf.append("import java.util.EventListener;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("import test.MyFormattable;\n");
		buf.append("module test {\n");
		buf.append("  uses EventListener;\n");
		buf.append("  provides Formattable with MyFormattable;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("module-info.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("import java.util.EventListener;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("import test.MyFormattable;\n");
		buf.append("/**\n");
		buf.append(" * A module comment.\n");
		buf.append(" * @provides Formattable\n");
		buf.append(" * @uses EventListener\n");
		buf.append(" */\n");
		buf.append("module test {\n");
		buf.append("  uses EventListener;\n");
		buf.append("  provides Formattable with MyFormattable;\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testMissingUsesTag1() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("public class MyFormattable implements Formattable {\n");
		buf.append("  @Override\n");
		buf.append("  public void formatTo(Formatter formatter, int flags, int width, int precision) {\n");
		buf.append("  }\n");
		buf.append("}\n");
		@SuppressWarnings("unused")
		ICompilationUnit cu1= pack.createCompilationUnit("MyFormattable.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
		buf= new StringBuilder();
		buf.append("import java.util.EventListener;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("import test.MyFormattable;\n");
		buf.append("/**\n");
		buf.append(" * module test\n");
		buf.append(" * @provides Formattable\n");
		buf.append(" */\n");
		buf.append("module test {\n");
		buf.append("  uses EventListener;\n");
		buf.append("  provides Formattable with MyFormattable;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("module-info.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("import java.util.EventListener;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("import test.MyFormattable;\n");
		buf.append("/**\n");
		buf.append(" * module test\n");
		buf.append(" * @provides Formattable\n");
		buf.append(" * @uses EventListener\n");
		buf.append(" */\n");
		buf.append("module test {\n");
		buf.append("  uses EventListener;\n");
		buf.append("  provides Formattable with MyFormattable;\n");
		buf.append("}\n");

		String expected= buf.toString();
		assertEqualString(preview1, expected);

		CUCorrectionProposal proposal2= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal2);

		buf= new StringBuilder();
		buf.append("import java.util.EventListener;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("import test.MyFormattable;\n");
		buf.append("/**\n");
		buf.append(" * module test\n");
		buf.append(" * @provides Formattable\n");
		buf.append(" * @uses EventListener\n");
		buf.append(" */\n");
		buf.append("module test {\n");
		buf.append("  uses EventListener;\n");
		buf.append("  provides Formattable with MyFormattable;\n");
		buf.append("}\n");
		String expected2= buf.toString();
		assertEqualString(preview2, expected2);
	}

	@Test
	public void testMissingUsesTag2() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("import java.utio.Formatter;\n");
		buf.append("public class MyFormattable implements Formattable {\n");
		buf.append("  @Override\n");
		buf.append("  public void formatTo(Formatter formatter, int flags, int width, int precision) {\n");
		buf.append("  }\n");
		buf.append("}\n");
		@SuppressWarnings("unused")
		ICompilationUnit cu1= pack.createCompilationUnit("MyFormattable.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
		buf= new StringBuilder();
		buf.append("import java.util.EventListener;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("import java.lang.Appendable;\n");
		buf.append("import test.MyFormattable;\n");
		buf.append("/**\n");
		buf.append(" * module test\n");
		buf.append(" * @provides Formattable\n");
		buf.append(" * @uses EventListener\n");
		buf.append(" */\n");
		buf.append("module test {\n");
		buf.append("  uses EventListener;\n");
		buf.append("  uses Appendable;\n");
		buf.append("  provides Formattable with MyFormattable;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("module-info.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("import java.util.EventListener;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("import java.lang.Appendable;\n");
		buf.append("import test.MyFormattable;\n");
		buf.append("/**\n");
		buf.append(" * module test\n");
		buf.append(" * @provides Formattable\n");
		buf.append(" * @uses Appendable\n");
		buf.append(" * @uses EventListener\n");
		buf.append(" */\n");
		buf.append("module test {\n");
		buf.append("  uses EventListener;\n");
		buf.append("  uses Appendable;\n");
		buf.append("  provides Formattable with MyFormattable;\n");
		buf.append("}\n");

		String expected= buf.toString();
		assertEqualString(preview1, expected);

		CUCorrectionProposal proposal2= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal2);

		buf= new StringBuilder();
		buf.append("import java.util.EventListener;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("import java.lang.Appendable;\n");
		buf.append("import test.MyFormattable;\n");
		buf.append("/**\n");
		buf.append(" * module test\n");
		buf.append(" * @provides Formattable\n");
		buf.append(" * @uses Appendable\n");
		buf.append(" * @uses EventListener\n");
		buf.append(" */\n");
		buf.append("module test {\n");
		buf.append("  uses EventListener;\n");
		buf.append("  uses Appendable;\n");
		buf.append("  provides Formattable with MyFormattable;\n");
		buf.append("}\n");
		String expected2= buf.toString();
		assertEqualString(preview2, expected2);
	}

	@Test
	public void testMissingUsesTag3() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("import java.utio.Formatter;\n");
		buf.append("public class MyFormattable implements Formattable {\n");
		buf.append("  @Override\n");
		buf.append("  public void formatTo(Formatter formatter, int flags, int width, int precision) {\n");
		buf.append("  }\n");
		buf.append("}\n");
		@SuppressWarnings("unused")
		ICompilationUnit cu1= pack.createCompilationUnit("MyFormattable.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
		buf= new StringBuilder();
		buf.append("import java.util.EventListener;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("import java.lang.Appendable;\n");
		buf.append("import test.MyFormattable;\n");
		buf.append("/**\n");
		buf.append(" * module test\n");
		buf.append(" * @provides Formattable\n");
		buf.append(" */\n");
		buf.append("module test {\n");
		buf.append("  uses EventListener;\n");
		buf.append("  uses Appendable;\n");
		buf.append("  provides Formattable with MyFormattable;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("module-info.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2, null);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("import java.util.EventListener;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("import java.lang.Appendable;\n");
		buf.append("import test.MyFormattable;\n");
		buf.append("/**\n");
		buf.append(" * module test\n");
		buf.append(" * @provides Formattable\n");
		buf.append(" * @uses EventListener\n");
		buf.append(" */\n");
		buf.append("module test {\n");
		buf.append("  uses EventListener;\n");
		buf.append("  uses Appendable;\n");
		buf.append("  provides Formattable with MyFormattable;\n");
		buf.append("}\n");

		String expected= buf.toString();
		assertEqualString(preview1, expected);

		CUCorrectionProposal proposal2= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal2);

		buf= new StringBuilder();
		buf.append("import java.util.EventListener;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("import java.lang.Appendable;\n");
		buf.append("import test.MyFormattable;\n");
		buf.append("/**\n");
		buf.append(" * module test\n");
		buf.append(" * @provides Formattable\n");
		buf.append(" * @uses Appendable\n");
		buf.append(" * @uses EventListener\n");
		buf.append(" */\n");
		buf.append("module test {\n");
		buf.append("  uses EventListener;\n");
		buf.append("  uses Appendable;\n");
		buf.append("  provides Formattable with MyFormattable;\n");
		buf.append("}\n");
		String expected2= buf.toString();
		assertEqualString(preview2, expected2);
	}

	@Test
	public void testMissingProvidesTag1() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("public class MyFormattable implements Formattable {\n");
		buf.append("  @Override\n");
		buf.append("  public void formatTo(Formatter formatter, int flags, int width, int precision) {\n");
		buf.append("  }\n");
		buf.append("}\n");
		@SuppressWarnings("unused")
		ICompilationUnit cu1= pack.createCompilationUnit("MyFormattable.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
		buf= new StringBuilder();
		buf.append("import java.util.EventListener;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("import test.MyFormattable;\n");
		buf.append("/**\n");
		buf.append(" * module test\n");
		buf.append(" * @uses EventListener\n");
		buf.append(" */\n");
		buf.append("module test {\n");
		buf.append("  uses EventListener;\n");
		buf.append("  provides Formattable with MyFormattable;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("module-info.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("import java.util.EventListener;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("import test.MyFormattable;\n");
		buf.append("/**\n");
		buf.append(" * module test\n");
		buf.append(" * @provides Formattable\n");
		buf.append(" * @uses EventListener\n");
		buf.append(" */\n");
		buf.append("module test {\n");
		buf.append("  uses EventListener;\n");
		buf.append("  provides Formattable with MyFormattable;\n");
		buf.append("}\n");

		String expected= buf.toString();
		assertEqualString(preview1, expected);

		CUCorrectionProposal proposal2= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal2);

		buf= new StringBuilder();
		buf.append("import java.util.EventListener;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("import test.MyFormattable;\n");
		buf.append("/**\n");
		buf.append(" * module test\n");
		buf.append(" * @provides Formattable\n");
		buf.append(" * @uses EventListener\n");
		buf.append(" */\n");
		buf.append("module test {\n");
		buf.append("  uses EventListener;\n");
		buf.append("  provides Formattable with MyFormattable;\n");
		buf.append("}\n");
		String expected2= buf.toString();
		assertEqualString(preview2, expected2);
	}

	@Test
	public void testMissingProvidesTag2() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("public class MyFormattable implements Formattable, Appendable {\n");
		buf.append("  @Override\n");
		buf.append("  public void formatTo(Formatter formatter, int flags, int width, int precision) {\n");
		buf.append("  }\n");
		buf.append("  @Override\n");
		buf.append("  public Appendable append(CharSequence csq) throws IOException {\n");
		buf.append("    return null;\n");
		buf.append("  }\n");
		buf.append("  @Override\n");
		buf.append("  public Appendable append(CharSequence csq, int start, int end) throws IOException {\n");
		buf.append("    return null;\n");
		buf.append("  }\n");
		buf.append("  @Override\n");
		buf.append("  public Appendable append(char c) throws IOException {\n");
		buf.append("   	return null;\n");
		buf.append("  }\n");
		buf.append("}\n");
		@SuppressWarnings("unused")
		ICompilationUnit cu1= pack.createCompilationUnit("MyFormattable.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
		buf= new StringBuilder();
		buf.append("import java.util.EventListener;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("import test.MyFormattable;\n");
		buf.append("/**\n");
		buf.append(" * module test\n");
		buf.append(" * @uses EventListener\n");
		buf.append(" */\n");
		buf.append("module test {\n");
		buf.append("  provides Appendable with MyFormattable;\n");
		buf.append("  uses EventListener;\n");
		buf.append("  provides Formattable with MyFormattable;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("module-info.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2, null);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("import java.util.EventListener;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("import test.MyFormattable;\n");
		buf.append("/**\n");
		buf.append(" * module test\n");
		buf.append(" * @provides Appendable\n");
		buf.append(" * @uses EventListener\n");
		buf.append(" */\n");
		buf.append("module test {\n");
		buf.append("  provides Appendable with MyFormattable;\n");
		buf.append("  uses EventListener;\n");
		buf.append("  provides Formattable with MyFormattable;\n");
		buf.append("}\n");

		String expected= buf.toString();
		assertEqualString(preview1, expected);

		CUCorrectionProposal proposal2= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal2);

		buf= new StringBuilder();
		buf.append("import java.util.EventListener;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("import test.MyFormattable;\n");
		buf.append("/**\n");
		buf.append(" * module test\n");
		buf.append(" * @provides Formattable\n");
		buf.append(" * @provides Appendable\n");
		buf.append(" * @uses EventListener\n");
		buf.append(" */\n");
		buf.append("module test {\n");
		buf.append("  provides Appendable with MyFormattable;\n");
		buf.append("  uses EventListener;\n");
		buf.append("  provides Formattable with MyFormattable;\n");
		buf.append("}\n");
		String expected2= buf.toString();
		assertEqualString(preview2, expected2);
	}

	@Test
	public void testDuplicateUsesTag1() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("public class MyFormattable implements Formattable {\n");
		buf.append("  @Override\n");
		buf.append("  public void formatTo(Formatter formatter, int flags, int width, int precision) {\n");
		buf.append("  }\n");
		buf.append("}\n");
		@SuppressWarnings("unused")
		ICompilationUnit cu1= pack.createCompilationUnit("MyFormattable.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
		buf= new StringBuilder();
		buf.append("import java.util.EventListener;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("import test.MyFormattable;\n");
		buf.append("/**\n");
		buf.append(" * module test\n");
		buf.append(" * @uses EventListener\n");
		buf.append(" * @provides Formattable\n");
		buf.append(" * @uses EventListener\n");
		buf.append(" */\n");
		buf.append("module test {\n");
		buf.append("  uses EventListener;\n");
		buf.append("  provides Formattable with MyFormattable;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("module-info.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("import java.util.EventListener;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("import test.MyFormattable;\n");
		buf.append("/**\n");
		buf.append(" * module test\n");
		buf.append(" * @uses EventListener\n");
		buf.append(" * @provides Formattable\n");
		buf.append(" */\n");
		buf.append("module test {\n");
		buf.append("  uses EventListener;\n");
		buf.append("  provides Formattable with MyFormattable;\n");
		buf.append("}\n");

		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testDuplicateUsesTag2() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("public class MyFormattable implements Formattable {\n");
		buf.append("  @Override\n");
		buf.append("  public void formatTo(Formatter formatter, int flags, int width, int precision) {\n");
		buf.append("  }\n");
		buf.append("}\n");
		@SuppressWarnings("unused")
		ICompilationUnit cu1= pack.createCompilationUnit("MyFormattable.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
		buf= new StringBuilder();
		buf.append("import java.util.EventListener;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("import test.MyFormattable;\n");
		buf.append("/**\n");
		buf.append(" * module test\n");
		buf.append(" * @uses EventListener\n");
		buf.append(" * @provides Formattable\n");
		buf.append(" * @uses EventListener */\n");
		buf.append("module test {\n");
		buf.append("  uses EventListener;\n");
		buf.append("  provides Formattable with MyFormattable;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("module-info.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("import java.util.EventListener;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("import test.MyFormattable;\n");
		buf.append("/**\n");
		buf.append(" * module test\n");
		buf.append(" * @uses EventListener\n");
		buf.append(" * @provides Formattable*/\n");
		buf.append("module test {\n");
		buf.append("  uses EventListener;\n");
		buf.append("  provides Formattable with MyFormattable;\n");
		buf.append("}\n");

		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testDuplicateProvidesTag1() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("public class MyFormattable implements Formattable {\n");
		buf.append("  @Override\n");
		buf.append("  public void formatTo(Formatter formatter, int flags, int width, int precision) {\n");
		buf.append("  }\n");
		buf.append("}\n");
		@SuppressWarnings("unused")
		ICompilationUnit cu1= pack.createCompilationUnit("MyFormattable.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
		buf= new StringBuilder();
		buf.append("import java.util.EventListener;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("import test.MyFormattable;\n");
		buf.append("/**\n");
		buf.append(" * module test\n");
		buf.append(" * @provides Formattable\n");
		buf.append(" * @uses EventListener\n");
		buf.append(" * @provides Formattable\n");
		buf.append(" */\n");
		buf.append("module test {\n");
		buf.append("  uses EventListener;\n");
		buf.append("  provides Formattable with MyFormattable;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("module-info.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("import java.util.EventListener;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("import test.MyFormattable;\n");
		buf.append("/**\n");
		buf.append(" * module test\n");
		buf.append(" * @provides Formattable\n");
		buf.append(" * @uses EventListener\n");
		buf.append(" */\n");
		buf.append("module test {\n");
		buf.append("  uses EventListener;\n");
		buf.append("  provides Formattable with MyFormattable;\n");
		buf.append("}\n");

		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testDuplicateProvidesTag2() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("public class MyFormattable implements Formattable {\n");
		buf.append("  @Override\n");
		buf.append("  public void formatTo(Formatter formatter, int flags, int width, int precision) {\n");
		buf.append("  }\n");
		buf.append("}\n");
		@SuppressWarnings("unused")
		ICompilationUnit cu1= pack.createCompilationUnit("MyFormattable.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
		buf= new StringBuilder();
		buf.append("import java.util.EventListener;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("import test.MyFormattable;\n");
		buf.append("/**\n");
		buf.append(" * module test\n");
		buf.append(" * @provides Formattable\n");
		buf.append(" * @uses EventListener\n");
		buf.append(" * @provides Formattable */\n");
		buf.append("module test {\n");
		buf.append("  uses EventListener;\n");
		buf.append("  provides Formattable with MyFormattable;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("module-info.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("import java.util.EventListener;\n");
		buf.append("import java.util.Formattable;\n");
		buf.append("import test.MyFormattable;\n");
		buf.append("/**\n");
		buf.append(" * module test\n");
		buf.append(" * @provides Formattable\n");
		buf.append(" * @uses EventListener*/\n");
		buf.append("module test {\n");
		buf.append("  uses EventListener;\n");
		buf.append("  provides Formattable with MyFormattable;\n");
		buf.append("}\n");

		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

}

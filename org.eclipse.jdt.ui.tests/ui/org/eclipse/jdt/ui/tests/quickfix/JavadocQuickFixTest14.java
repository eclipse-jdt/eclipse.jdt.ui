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
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

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

import org.eclipse.jdt.ui.tests.core.rules.Java14ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

@RunWith(JUnit4.class)
public class JavadocQuickFixTest14 extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectsetup = new Java14ProjectTestSetup(true);

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
		comment.append(" * A record comment.\n");
		comment.append(" *\n");
		comment.append(" * ${tags}\n");
		comment.append(" */");
		String res= comment.toString();
		StubUtility.setCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, res, null);

		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		JavaProjectHelper.set14CompilerOptions(fJProject1, true);
		JavaProjectHelper.addRequiredModularProject(fJProject1, Java14ProjectTestSetup.getProject());

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}

	@Test
	public void testMissingRecordComment() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n\n");
		buf.append("public record Rec1(int a, int b) {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Rec1.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test;\n\n");
		buf.append("/**\n");
		buf.append(" * A record comment.\n");
		buf.append(" *\n");
		buf.append(" * @param a\n");
		buf.append(" * @param b\n");
		buf.append(" */\n");
		buf.append("public record Rec1(int a, int b) {\n");
		buf.append("}\n");

		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testMissingRecordCommentWithTypeParam() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n\n");
		buf.append("public record Rec1<N>(int a, int b) {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Rec1.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test;\n\n");
		buf.append("/**\n");
		buf.append(" * A record comment.\n");
		buf.append(" *\n");
		buf.append(" * @param <N>\n");
		buf.append(" * @param a\n");
		buf.append(" * @param b\n");
		buf.append(" */\n");
		buf.append("public record Rec1<N>(int a, int b) {\n");
		buf.append("}\n");

		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testMissingRecordCommentTag() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n\n");
		buf.append("/**\n");
		buf.append(" * A record comment.\n");
		buf.append(" *\n");
		buf.append(" * @param a\n");
		buf.append(" */\n");
		buf.append("public record Rec1(int a, int b) {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Rec1.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test;\n\n");
		buf.append("/**\n");
		buf.append(" * A record comment.\n");
		buf.append(" *\n");
		buf.append(" * @param a\n");
		buf.append(" * @param b \n");
		buf.append(" */\n");
		buf.append("public record Rec1(int a, int b) {\n");
		buf.append("}\n");

		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}


	@Test
	public void testMissingAllRecordCommentTags() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n\n");
		buf.append("/**\n");
		buf.append(" * A record comment.\n");
		buf.append(" *\n");
		buf.append(" */\n");
		buf.append("public record Rec1(int a, int b) {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Rec1.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(1);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test;\n\n");
		buf.append("/**\n");
		buf.append(" * A record comment.\n");
		buf.append(" * @param a \n");
		buf.append(" * @param b \n");
		buf.append(" *\n");
		buf.append(" */\n");
		buf.append("public record Rec1(int a, int b) {\n");
		buf.append("}\n");

		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}

	@Test
	public void testMissingTypeParamTag() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n\n");
		buf.append("/**\n");
		buf.append(" * A record comment.\n");
		buf.append(" * @param a \n");
		buf.append(" * @param b \n");
		buf.append(" *\n");
		buf.append(" */\n");
		buf.append("public record Rec1<N>(int a, int b) {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Rec1.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(1);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test;\n\n");
		buf.append("/**\n");
		buf.append(" * A record comment.\n");
		buf.append(" * @param <N> \n");
		buf.append(" * @param a \n");
		buf.append(" * @param b \n");
		buf.append(" *\n");
		buf.append(" */\n");
		buf.append("public record Rec1<N>(int a, int b) {\n");
		buf.append("}\n");

		String expected= buf.toString();
		assertEqualString(preview1, expected);
	}
}

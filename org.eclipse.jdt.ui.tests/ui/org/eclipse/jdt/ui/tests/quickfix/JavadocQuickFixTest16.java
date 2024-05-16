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

import org.eclipse.jdt.ui.tests.core.rules.Java16ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

public class JavadocQuickFixTest16 extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectsetup = new Java16ProjectTestSetup(false);

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

		String res= """
			/**
			 * A record comment.
			 *
			 * ${tags}
			 */""";
		StubUtility.setCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, res, null);

		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		JavaProjectHelper.set16CompilerOptions(fJProject1, false);
		JavaProjectHelper.addRequiredModularProject(fJProject1, projectsetup.getProject());

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectsetup.getDefaultClasspath());
	}

	@Test
	public void testMissingRecordComment() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= """
			package test;
			
			public record Rec1(int a, int b) {
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Rec1.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected= """
			package test;
			
			/**
			 * A record comment.
			 *
			 * @param a
			 * @param b
			 */
			public record Rec1(int a, int b) {
			}
			""";

		assertEqualString(preview1, expected);
	}

	@Test
	public void testMissingRecordCommentWithTypeParam() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= """
			package test;
			
			public record Rec1<N>(int a, int b) {
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Rec1.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected= """
			package test;
			
			/**
			 * A record comment.
			 *
			 * @param <N>
			 * @param a
			 * @param b
			 */
			public record Rec1<N>(int a, int b) {
			}
			""";

		assertEqualString(preview1, expected);
	}

	@Test
	public void testMissingRecordCommentTag() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= """
			package test;
			
			/**
			 * A record comment.
			 *
			 * @param a
			 */
			public record Rec1(int a, int b) {
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Rec1.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected= """
			package test;
			
			/**
			 * A record comment.
			 *
			 * @param a
			 * @param b\s
			 */
			public record Rec1(int a, int b) {
			}
			""";

		assertEqualString(preview1, expected);
	}


	@Test
	public void testMissingAllRecordCommentTags() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= """
			package test;
			
			/**
			 * A record comment.
			 *
			 */
			public record Rec1(int a, int b) {
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Rec1.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(1);
		String preview1= getPreviewContent(proposal);

		String expected= """
			package test;
			
			/**
			 * A record comment.
			 * @param a\s
			 * @param b\s
			 *
			 */
			public record Rec1(int a, int b) {
			}
			""";

		assertEqualString(preview1, expected);
	}

	@Test
	public void testMissingTypeParamTag() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= """
			package test;
			
			/**
			 * A record comment.
			 * @param a\s
			 * @param b\s
			 *
			 */
			public record Rec1<N>(int a, int b) {
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Rec1.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(1);
		String preview1= getPreviewContent(proposal);

		String expected= """
			package test;
			
			/**
			 * A record comment.
			 * @param <N>\s
			 * @param a\s
			 * @param b\s
			 *
			 */
			public record Rec1<N>(int a, int b) {
			}
			""";

		assertEqualString(preview1, expected);
	}
}

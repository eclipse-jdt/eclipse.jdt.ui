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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

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

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public class JavadocQuickFixTest extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup = new ProjectTestSetup();

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
			 * A comment.
			 * ${tags}
			 */""";
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORCOMMENT_ID, res, null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODCOMMENT_ID, res, null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, res, null);

		String str= """
			/**
			 * A field comment for ${field}.
			 */""";
		StubUtility.setCodeTemplate(CodeTemplateContextType.FIELDCOMMENT_ID, str, null);

		String str1= """
			/**
			 * A override comment.
			 * ${see_to_overridden}
			 */""";
		StubUtility.setCodeTemplate(CodeTemplateContextType.OVERRIDECOMMENT_ID, str1, null);

		String str2= """
			/**
			 * A delegate comment.
			 * ${see_to_target}
			 */""";
		StubUtility.setCodeTemplate(CodeTemplateContextType.DELEGATECOMMENT_ID, str2, null);

		fJProject1= projectSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}


	@Test
	public void testMissingParam1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @param b
			     *      comment on second line.
			     * @param c
			     */
			    public void foo(int a, int b, int c) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @param a\s
			     * @param b
			     *      comment on second line.
			     * @param c
			     */
			    public void foo(int a, int b, int c) {
			    }
			}
			""";
		assertEqualString(preview1, expected);
		assertEqualString(preview2, expected);

		assertProposalExists(proposals, CorrectionMessages.ConfigureProblemSeveritySubProcessor_name);
	}

	@Test
	public void testMissingParam2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @param a
			     *      comment on second line.
			     * @param c
			     */
			    public void foo(int a, int b, int c) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @param a
			     *      comment on second line.
			     * @param b\s
			     * @param c
			     */
			    public void foo(int a, int b, int c) {
			    }
			}
			""";
		assertEqualString(preview1, expected);
		assertEqualString(preview2, expected);
	}
	@Test
	public void testMissingParam3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @param a
			     *      comment on second line.
			     * @param b
			     */
			    public void foo(int a, int b, int c) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @param a
			     *      comment on second line.
			     * @param b
			     * @param c\s
			     */
			    public void foo(int a, int b, int c) {
			    }
			}
			""";
		assertEqualString(preview1, expected);
		assertEqualString(preview2, expected);
	}

	@Test
	public void testMissingParam4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @param <A>
			     *      comment on second line.
			     * @param a
			     */
			    public <A, B> void foo(int a) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @param <A>
			     *      comment on second line.
			     * @param <B>\s
			     * @param a
			     */
			    public <A, B> void foo(int a) {
			    }
			}
			""";
		assertEqualString(preview1, expected);
		assertEqualString(preview2, expected);
	}

	@Test
	public void testMissingParam5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			/**
			 * @param <B> Hello
			 */
			public class E<A, B> {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected= """
			package test1;
			/**
			 * @param <A>\s
			 * @param <B> Hello
			 */
			public class E<A, B> {
			}
			""";
		assertEqualString(preview1, expected);
		assertEqualString(preview2, expected);
	}

	@Test
	public void testMissingParam6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			/**
			 * @author ae
			 */
			public class E<A> {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected= """
			package test1;
			/**
			 * @author ae
			 * @param <A>\s
			 */
			public class E<A> {
			}
			""";
		assertEqualString(preview1, expected);
		assertEqualString(preview2, expected);
	}

	@Test
	public void testMissingReturn1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @param b
			     *      comment on second line.
			     * @param c
			     */
			    public int foo(int b, int c) {
			        return 1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @param b
			     *      comment on second line.
			     * @param c
			     * @return\s
			     */
			    public int foo(int b, int c) {
			        return 1;
			    }
			}
			""";
		assertEqualString(preview1, expected);
		assertEqualString(preview2, expected);
	}

	@Test
	public void testMissingReturn2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     */
			    public int foo() {
			        return 1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @return\s
			     */
			    public int foo() {
			        return 1;
			    }
			}
			""";
		assertEqualString(preview1, expected);
		assertEqualString(preview2, expected);
	}

	@Test
	public void testMissingReturn3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @throws Exception
			     */
			    public int foo() throws Exception {
			        return 1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @return\s
			     * @throws Exception
			     */
			    public int foo() throws Exception {
			        return 1;
			    }
			}
			""";
		assertEqualString(preview1, expected);
		assertEqualString(preview2, expected);
	}

	@Test
	public void testMissingThrows() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @return Returns an Int
			     */
			    public int foo() throws Exception {
			        return 1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @return Returns an Int
			     * @throws Exception\s
			     */
			    public int foo() throws Exception {
			        return 1;
			    }
			}
			""";
		assertEqualString(preview1, expected);
		assertEqualString(preview2, expected);
	}

	@Test
	public void testInsertAllMissing1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @throws Exception
			     */
			    public int foo(int a, int b) throws NullPointerException, Exception {
			        return 1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections2(cu, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @param a\s
			     * @param b\s
			     * @return\s
			     * @throws NullPointerException\s
			     * @throws Exception
			     */
			    public int foo(int a, int b) throws NullPointerException, Exception {
			        return 1;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testInsertAllMissing2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @param b
			     * @return a number
			     */
			    public int foo(int a, int b, int c) throws NullPointerException, Exception {
			        return 1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections2(cu, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @param a\s
			     * @param b
			     * @param c\s
			     * @return a number
			     * @throws NullPointerException\s
			     * @throws Exception\s
			     */
			    public int foo(int a, int b, int c) throws NullPointerException, Exception {
			        return 1;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testInsertAllMissing3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			/**
			 */
			public class E<S, T> {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections2(cu, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			/**
			 * @param <S>\s
			 * @param <T>\s
			 */
			public class E<S, T> {
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testInsertAllMissing4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @param <B> test
			     * @param b
			     * @return a number
			     */
			    public <A, B> int foo(int a, int b) throws NullPointerException {
			        return 1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections2(cu, 3);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @param <A>\s
			     * @param <B> test
			     * @param a\s
			     * @param b
			     * @return a number
			     * @throws NullPointerException\s
			     */
			    public <A, B> int foo(int a, int b) throws NullPointerException {
			        return 1;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });	}

	@Test
	public void testRemoveParamTag1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @param a
			     *      comment on second line.
			     * @param c
			     */
			    public void foo(int c) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @param c
			     */
			    public void foo(int c) {
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testRemoveParamTag2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @param a
			     *      comment on second line.
			     * @param a
			     */
			    public void foo(int a) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @param a
			     *      comment on second line.
			     */
			    public void foo(int a) {
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}


	@Test
	public void testRemoveThrowsTag1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @param a
			     *      comment on second line.
			     * @param c
			     * @throws Exception Thrown by surprise.
			     */
			    public void foo(int a, int c) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @param a
			     *      comment on second line.
			     * @param c
			     */
			    public void foo(int a, int c) {
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testRemoveThrowsTag2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @param a
			     *      comment on second line.
			     * @exception Exception
			     */
			    public void foo(int a) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @param a
			     *      comment on second line.
			     */
			    public void foo(int a) {
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testRemoveThrowsTag3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			/**
			 */
			public class E {
			    /**
			     * @param a
			     *      comment on second line.
			     * @exception Exception
			     * @exception java.io.IOException
			     * @exception NullPointerException
			     */
			    public void foo(int a) throws IOException {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected= """
			package test1;
			import java.io.IOException;
			/**
			 */
			public class E {
			    /**
			     * @param a
			     *      comment on second line.
			     * @exception java.io.IOException
			     * @exception NullPointerException
			     */
			    public void foo(int a) throws IOException {
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testRemoveReturnTag1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @param a
			     *      comment on second line.
			     * @return Returns the result.
			     *      comment on second line.
			     * @exception Exception
			     */
			    public void foo(int a) throws Exception {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @param a
			     *      comment on second line.
			     * @exception Exception
			     */
			    public void foo(int a) throws Exception {
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testRemoveUnknownTag1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @param a
			     *      comment on second line.
			     * @return Returns the result.
			     *      comment on second line.
			     * @exception Exception
			     */
			    public void foo(int a) throws Exception {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * @param a
			     *      comment on second line.
			     * @exception Exception
			     */
			    public void foo(int a) throws Exception {
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testMissingMethodComment1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			/**
			 */
			public class E {
			    public <A> void foo(int a) throws IOException {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected= """
			package test1;
			import java.io.IOException;
			/**
			 */
			public class E {
			    /**
			     * A comment.
			     * @param <A>
			     * @param a
			     * @throws IOException
			     */
			    public <A> void foo(int a) throws IOException {
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testMissingMethodComment2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			/**
			 */
			public class E {
			    public String toString() {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * A override comment.
			     * @see java.lang.Object#toString()
			     */
			    public String toString() {
			        return null;
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testMissingMethodComment3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			/**
			 */
			public class E {
			    public void empty() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * A comment.
			     */
			    public void empty() {
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testMissingMethodComment4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			/**
			 */
			public class B extends A<Integer> {
			    public void foo(Integer x) {
			    }
			}
			class A<T extends Number> {
			    /**
			     * @param x
			     */
			    public void foo(T x) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("B.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[1];
		expected[0]= """
			package pack;
			
			/**
			 */
			public class B extends A<Integer> {
			    /**
			     * A override comment.
			     * @see pack.A#foo(java.lang.Number)
			     */
			    public void foo(Integer x) {
			    }
			}
			class A<T extends Number> {
			    /**
			     * @param x
			     */
			    public void foo(T x) {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
		}


	@Test
	public void testMissingConstructorComment() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			/**
			 */
			public class E {
			    public E(int a) throws IOException {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected= """
			package test1;
			import java.io.IOException;
			/**
			 */
			public class E {
			    /**
			     * A comment.
			     * @param a
			     * @throws IOException
			     */
			    public E(int a) throws IOException {
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testMissingTypeComment() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E<A, B> {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected= """
			package test1;
			/**
			 * A comment.
			 * @param <A>
			 * @param <B>
			 */
			public class E<A, B> {
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testMissingFieldComment() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			/**
			 */
			public class E {
			    public static final int COLOR= 1;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections2(cu, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected= """
			package test1;
			/**
			 */
			public class E {
			    /**
			     * A field comment for COLOR.
			     */
			    public static final int COLOR= 1;
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testInvalidQualification1() throws Exception {
		Map<String, String> original= fJProject1.getOptions(false);
		HashMap<String, String> newOptions= new HashMap<>(original);
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_4, newOptions);
		fJProject1.setOptions(newOptions);

		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
			String str= """
				package pack;
				
				public class A {
				    public static class B {
				        public static class C {
				           \s
				        }
				    }
				}
				""";
			pack1.createCompilationUnit("A.java", str, false, null);

			IPackageFragment pack2= fSourceFolder.createPackageFragment("pack2", false, null);
			String str1= """
				package pack2;
				
				import pack.A.B.C;
				
				/**
				 * {@link C}\s
				 */
				public class E {
				}
				""";
			ICompilationUnit cu= pack2.createCompilationUnit("E.java", str1, false, null);

			CompilationUnit astRoot= getASTRoot(cu);
			ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

			assertCorrectLabels(proposals);
			assertNumberOfProposals(proposals, 2);

			String[] expected= new String[1];
			expected[0]= """
				package pack2;
				
				import pack.A.B.C;
				
				/**
				 * {@link pack.A.B.C}\s
				 */
				public class E {
				}
				""";

			assertExpectedExistInProposals(proposals, expected);
		} finally{
			fJProject1.setOptions(original);
		}
	}

	@Test
	public void testInvalidQualification2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			public class A {
			    public static class B {
			    }
			}
			""";
		pack1.createCompilationUnit("A.java", str, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("pack2", false, null);
		String str1= """
			package pack2;
			
			import pack.A;
			
			/**
			 * {@link A.B}\s
			 */
			public class E {
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[1];
		expected[0]= """
			package pack2;
			
			import pack.A;
			
			/**
			 * {@link pack.A.B}\s
			 */
			public class E {
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testInvalidQualification3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			public class A {
			    public interface B {
			        void foo();
			    }
			}
			""";
		pack1.createCompilationUnit("A.java", str, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("pack2", false, null);
		String str1= """
			package pack2;
			
			import pack.A;
			
			/**
			 * {@link A.B#foo()}\s
			 */
			public class E {
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[1];
		expected[0]= """
			package pack2;
			
			import pack.A;
			
			/**
			 * {@link pack.A.B#foo()}\s
			 */
			public class E {
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

}

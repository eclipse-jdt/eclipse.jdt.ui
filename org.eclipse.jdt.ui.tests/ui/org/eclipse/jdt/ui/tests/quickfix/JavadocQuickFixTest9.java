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

		String str3= """
			/**
			 * A module comment.
			 * ${tags}
			 */""";
		StubUtility.setCodeTemplate(CodeTemplateContextType.MODULECOMMENT_ID, str3, null);

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
		String str= """
			package test;
			import java.util.Formattable;
			public class MyFormattable implements Formattable {
			  @Override
			  public void formatTo(Formatter formatter, int flags, int width, int precision) {
			  }
			}
			""";
		@SuppressWarnings("unused")
		ICompilationUnit cu1= pack.createCompilationUnit("MyFormattable.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
		String str1= """
			import java.util.EventListener;
			import java.util.Formattable;
			import test.MyFormattable;
			module test {
			  uses EventListener;
			  provides Formattable with MyFormattable;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("module-info.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected= """
			import java.util.EventListener;
			import java.util.Formattable;
			import test.MyFormattable;
			/**
			 * A module comment.
			 * @provides Formattable
			 * @uses EventListener
			 */
			module test {
			  uses EventListener;
			  provides Formattable with MyFormattable;
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testMissingUsesTag1() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.util.Formattable;
			public class MyFormattable implements Formattable {
			  @Override
			  public void formatTo(Formatter formatter, int flags, int width, int precision) {
			  }
			}
			""";
		@SuppressWarnings("unused")
		ICompilationUnit cu1= pack.createCompilationUnit("MyFormattable.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
		String str1= """
			import java.util.EventListener;
			import java.util.Formattable;
			import test.MyFormattable;
			/**
			 * module test
			 * @provides Formattable
			 */
			module test {
			  uses EventListener;
			  provides Formattable with MyFormattable;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("module-info.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected= """
			import java.util.EventListener;
			import java.util.Formattable;
			import test.MyFormattable;
			/**
			 * module test
			 * @provides Formattable
			 * @uses EventListener
			 */
			module test {
			  uses EventListener;
			  provides Formattable with MyFormattable;
			}
			""";
		assertEqualString(preview1, expected);

		CUCorrectionProposal proposal2= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal2);

		String expected2= """
			import java.util.EventListener;
			import java.util.Formattable;
			import test.MyFormattable;
			/**
			 * module test
			 * @provides Formattable
			 * @uses EventListener
			 */
			module test {
			  uses EventListener;
			  provides Formattable with MyFormattable;
			}
			""";
		assertEqualString(preview2, expected2);
	}

	@Test
	public void testMissingUsesTag2() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.util.Formattable;
			import java.utio.Formatter;
			public class MyFormattable implements Formattable {
			  @Override
			  public void formatTo(Formatter formatter, int flags, int width, int precision) {
			  }
			}
			""";
		@SuppressWarnings("unused")
		ICompilationUnit cu1= pack.createCompilationUnit("MyFormattable.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
		String str1= """
			import java.util.EventListener;
			import java.util.Formattable;
			import java.lang.Appendable;
			import test.MyFormattable;
			/**
			 * module test
			 * @provides Formattable
			 * @uses EventListener
			 */
			module test {
			  uses EventListener;
			  uses Appendable;
			  provides Formattable with MyFormattable;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("module-info.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected= """
			import java.util.EventListener;
			import java.util.Formattable;
			import java.lang.Appendable;
			import test.MyFormattable;
			/**
			 * module test
			 * @provides Formattable
			 * @uses Appendable
			 * @uses EventListener
			 */
			module test {
			  uses EventListener;
			  uses Appendable;
			  provides Formattable with MyFormattable;
			}
			""";
		assertEqualString(preview1, expected);

		CUCorrectionProposal proposal2= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal2);

		String expected2= """
			import java.util.EventListener;
			import java.util.Formattable;
			import java.lang.Appendable;
			import test.MyFormattable;
			/**
			 * module test
			 * @provides Formattable
			 * @uses Appendable
			 * @uses EventListener
			 */
			module test {
			  uses EventListener;
			  uses Appendable;
			  provides Formattable with MyFormattable;
			}
			""";
		assertEqualString(preview2, expected2);
	}

	@Test
	public void testMissingUsesTag3() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.util.Formattable;
			import java.utio.Formatter;
			public class MyFormattable implements Formattable {
			  @Override
			  public void formatTo(Formatter formatter, int flags, int width, int precision) {
			  }
			}
			""";
		@SuppressWarnings("unused")
		ICompilationUnit cu1= pack.createCompilationUnit("MyFormattable.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
		String str1= """
			import java.util.EventListener;
			import java.util.Formattable;
			import java.lang.Appendable;
			import test.MyFormattable;
			/**
			 * module test
			 * @provides Formattable
			 */
			module test {
			  uses EventListener;
			  uses Appendable;
			  provides Formattable with MyFormattable;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("module-info.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2, null);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected= """
			import java.util.EventListener;
			import java.util.Formattable;
			import java.lang.Appendable;
			import test.MyFormattable;
			/**
			 * module test
			 * @provides Formattable
			 * @uses EventListener
			 */
			module test {
			  uses EventListener;
			  uses Appendable;
			  provides Formattable with MyFormattable;
			}
			""";
		assertEqualString(preview1, expected);

		CUCorrectionProposal proposal2= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal2);

		String expected2= """
			import java.util.EventListener;
			import java.util.Formattable;
			import java.lang.Appendable;
			import test.MyFormattable;
			/**
			 * module test
			 * @provides Formattable
			 * @uses Appendable
			 * @uses EventListener
			 */
			module test {
			  uses EventListener;
			  uses Appendable;
			  provides Formattable with MyFormattable;
			}
			""";
		assertEqualString(preview2, expected2);
	}

	@Test
	public void testMissingProvidesTag1() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.util.Formattable;
			public class MyFormattable implements Formattable {
			  @Override
			  public void formatTo(Formatter formatter, int flags, int width, int precision) {
			  }
			}
			""";
		@SuppressWarnings("unused")
		ICompilationUnit cu1= pack.createCompilationUnit("MyFormattable.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
		String str1= """
			import java.util.EventListener;
			import java.util.Formattable;
			import test.MyFormattable;
			/**
			 * module test
			 * @uses EventListener
			 */
			module test {
			  uses EventListener;
			  provides Formattable with MyFormattable;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("module-info.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected= """
			import java.util.EventListener;
			import java.util.Formattable;
			import test.MyFormattable;
			/**
			 * module test
			 * @provides Formattable
			 * @uses EventListener
			 */
			module test {
			  uses EventListener;
			  provides Formattable with MyFormattable;
			}
			""";
		assertEqualString(preview1, expected);

		CUCorrectionProposal proposal2= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal2);

		String expected2= """
			import java.util.EventListener;
			import java.util.Formattable;
			import test.MyFormattable;
			/**
			 * module test
			 * @provides Formattable
			 * @uses EventListener
			 */
			module test {
			  uses EventListener;
			  provides Formattable with MyFormattable;
			}
			""";
		assertEqualString(preview2, expected2);
	}

	@Test
	public void testMissingProvidesTag2() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.util.Formattable;
			public class MyFormattable implements Formattable, Appendable {
			  @Override
			  public void formatTo(Formatter formatter, int flags, int width, int precision) {
			  }
			  @Override
			  public Appendable append(CharSequence csq) throws IOException {
			    return null;
			  }
			  @Override
			  public Appendable append(CharSequence csq, int start, int end) throws IOException {
			    return null;
			  }
			  @Override
			  public Appendable append(char c) throws IOException {
			   	return null;
			  }
			}
			""";
		@SuppressWarnings("unused")
		ICompilationUnit cu1= pack.createCompilationUnit("MyFormattable.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
		String str1= """
			import java.util.EventListener;
			import java.util.Formattable;
			import test.MyFormattable;
			/**
			 * module test
			 * @uses EventListener
			 */
			module test {
			  provides Appendable with MyFormattable;
			  uses EventListener;
			  provides Formattable with MyFormattable;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("module-info.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2, null);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected= """
			import java.util.EventListener;
			import java.util.Formattable;
			import test.MyFormattable;
			/**
			 * module test
			 * @provides Appendable
			 * @uses EventListener
			 */
			module test {
			  provides Appendable with MyFormattable;
			  uses EventListener;
			  provides Formattable with MyFormattable;
			}
			""";
		assertEqualString(preview1, expected);

		CUCorrectionProposal proposal2= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal2);

		String expected2= """
			import java.util.EventListener;
			import java.util.Formattable;
			import test.MyFormattable;
			/**
			 * module test
			 * @provides Formattable
			 * @provides Appendable
			 * @uses EventListener
			 */
			module test {
			  provides Appendable with MyFormattable;
			  uses EventListener;
			  provides Formattable with MyFormattable;
			}
			""";
		assertEqualString(preview2, expected2);
	}

	@Test
	public void testDuplicateUsesTag1() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.util.Formattable;
			public class MyFormattable implements Formattable {
			  @Override
			  public void formatTo(Formatter formatter, int flags, int width, int precision) {
			  }
			}
			""";
		@SuppressWarnings("unused")
		ICompilationUnit cu1= pack.createCompilationUnit("MyFormattable.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
		String str1= """
			import java.util.EventListener;
			import java.util.Formattable;
			import test.MyFormattable;
			/**
			 * module test
			 * @uses EventListener
			 * @provides Formattable
			 * @uses EventListener
			 */
			module test {
			  uses EventListener;
			  provides Formattable with MyFormattable;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("module-info.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected= """
			import java.util.EventListener;
			import java.util.Formattable;
			import test.MyFormattable;
			/**
			 * module test
			 * @uses EventListener
			 * @provides Formattable
			 */
			module test {
			  uses EventListener;
			  provides Formattable with MyFormattable;
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testDuplicateUsesTag2() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.util.Formattable;
			public class MyFormattable implements Formattable {
			  @Override
			  public void formatTo(Formatter formatter, int flags, int width, int precision) {
			  }
			}
			""";
		@SuppressWarnings("unused")
		ICompilationUnit cu1= pack.createCompilationUnit("MyFormattable.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
		String str1= """
			import java.util.EventListener;
			import java.util.Formattable;
			import test.MyFormattable;
			/**
			 * module test
			 * @uses EventListener
			 * @provides Formattable
			 * @uses EventListener */
			module test {
			  uses EventListener;
			  provides Formattable with MyFormattable;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("module-info.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected= """
			import java.util.EventListener;
			import java.util.Formattable;
			import test.MyFormattable;
			/**
			 * module test
			 * @uses EventListener
			 * @provides Formattable*/
			module test {
			  uses EventListener;
			  provides Formattable with MyFormattable;
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testDuplicateProvidesTag1() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.util.Formattable;
			public class MyFormattable implements Formattable {
			  @Override
			  public void formatTo(Formatter formatter, int flags, int width, int precision) {
			  }
			}
			""";
		@SuppressWarnings("unused")
		ICompilationUnit cu1= pack.createCompilationUnit("MyFormattable.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
		String str1= """
			import java.util.EventListener;
			import java.util.Formattable;
			import test.MyFormattable;
			/**
			 * module test
			 * @provides Formattable
			 * @uses EventListener
			 * @provides Formattable
			 */
			module test {
			  uses EventListener;
			  provides Formattable with MyFormattable;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("module-info.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected= """
			import java.util.EventListener;
			import java.util.Formattable;
			import test.MyFormattable;
			/**
			 * module test
			 * @provides Formattable
			 * @uses EventListener
			 */
			module test {
			  uses EventListener;
			  provides Formattable with MyFormattable;
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testDuplicateProvidesTag2() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.util.Formattable;
			public class MyFormattable implements Formattable {
			  @Override
			  public void formatTo(Formatter formatter, int flags, int width, int precision) {
			  }
			}
			""";
		@SuppressWarnings("unused")
		ICompilationUnit cu1= pack.createCompilationUnit("MyFormattable.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
		String str1= """
			import java.util.EventListener;
			import java.util.Formattable;
			import test.MyFormattable;
			/**
			 * module test
			 * @provides Formattable
			 * @uses EventListener
			 * @provides Formattable */
			module test {
			  uses EventListener;
			  provides Formattable with MyFormattable;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("module-info.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected= """
			import java.util.EventListener;
			import java.util.Formattable;
			import test.MyFormattable;
			/**
			 * module test
			 * @provides Formattable
			 * @uses EventListener*/
			module test {
			  uses EventListener;
			  provides Formattable with MyFormattable;
			}
			""";
		assertEqualString(preview1, expected);
	}

}

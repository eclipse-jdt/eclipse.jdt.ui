/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
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
 *     Jerome Cambon <jerome.cambon@oracle.com> - [1.8][clean up][quick assist] Convert lambda to anonymous must qualify references to 'this'/'super' - https://bugs.eclipse.org/430573
 *     Jeremie Bresson <dev@jmini.fr> - Bug 439912: [1.8][quick assist] Add quick assists to add and remove parentheses around single lambda parameter - https://bugs.eclipse.org/439912
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.NullTestUtils;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.Path;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.fix.FixMessages;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
public class AssistQuickFixTest1d8 extends QuickFixTest {
	@Rule
    public ProjectTestSetup projectSetup = new Java1d8ProjectTestSetup();

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

//	public static Test setUpTest() {
//		return new Java1d8ProjectTestSetup() {
//			@Override
//			protected void setUp() throws Exception {
//				JavaProjectHelper.PERFORM_DUMMY_SEARCH++;
//				super.setUp();
//			}
//			@Override
//			protected void tearDown() throws Exception {
//				super.tearDown();
//				JavaProjectHelper.PERFORM_DUMMY_SEARCH--;
//			}
//		};
//	}

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "//TODO\n${body_statement}", null);

		fJProject1= projectSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testAssignParamToField1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public interface I {\n");
		buf.append("    default void foo(int x) {\n");
		buf.append("        System.out.println(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("I.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("x");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 0);
	}

	@Test
	public void testAssignParamToField2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public interface I {\n");
		buf.append("    static void bar(int x) {\n");
		buf.append("        System.out.println(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("I.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("x");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 0);
	}

	@Test
	public void testConvertToLambda1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    void method();\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(I i) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        bar(new I() {\n");
		buf.append("            public void method() {\n");
		buf.append("                System.out.println();\n");
		buf.append("                System.out.println();\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("I()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    void method();\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(I i) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        bar(() -> {\n");
		buf.append("            System.out.println();\n");
		buf.append("            System.out.println();\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambda2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    void method(int a, int b);\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(I i) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        bar(new I() {\n");
		buf.append("            public void method(int a, int b) {\n");
		buf.append("                System.out.println(a+b);\n");
		buf.append("                System.out.println(a+b);\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("I()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    void method(int a, int b);\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(I i) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        bar((a, b) -> {\n");
		buf.append("            System.out.println(a+b);\n");
		buf.append("            System.out.println(a+b);\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambda3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    void method();\n");
		buf.append("    boolean equals(Object obj);\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(I i) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        bar(new I() {\n");
		buf.append("            public void method() {\n");
		buf.append("                System.out.println();\n");
		buf.append("            }\n");
		buf.append("            public boolean equals(Object obj) {\n");
		buf.append("                return false;\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("I()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertProposalDoesNotExist(proposals, FixMessages.LambdaExpressionsFix_convert_to_lambda_expression);
	}

	@Test
	public void testConvertToLambda4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    void method();\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(I i) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        bar(new I() {\n");
		buf.append("            int count=0;\n");
		buf.append("            public void method() {\n");
		buf.append("                System.out.println();\n");
		buf.append("                count++;\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("I()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertProposalDoesNotExist(proposals, FixMessages.LambdaExpressionsFix_convert_to_lambda_expression);
	}

	@Test
	public void testConvertToLambda5() throws Exception {
		//Quick assist should not be offered in 1.7 mode
		JavaProjectHelper.set17CompilerOptions(fJProject1);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		try {
			StringBuilder buf= new StringBuilder();
			buf.append("package test1;\n");
			buf.append("interface I {\n");
			buf.append("    void method();\n");
			buf.append("}\n");
			buf.append("public class E {\n");
			buf.append("    void bar(I i) {\n");
			buf.append("    }\n");
			buf.append("    void foo() {\n");
			buf.append("        bar(new I() {\n");
			buf.append("            public void method() {\n");
			buf.append("                System.out.println();\n");
			buf.append("            }\n");
			buf.append("        });\n");
			buf.append("    }\n");
			buf.append("}\n");
			ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

			int offset= buf.toString().indexOf("I()");
			AssistContext context= getCorrectionContext(cu, offset, 0);
			assertNoErrors(context);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertNumberOfProposals(proposals, 1);
			assertProposalDoesNotExist(proposals, FixMessages.LambdaExpressionsFix_convert_to_lambda_expression);
		} finally {
			JavaProjectHelper.set18CompilerOptions(fJProject1);
		}
	}

	@Test
	public void testConvertToLambda6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    boolean equals(Object obj);\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(I i) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        bar(new I() {\n");
		buf.append("            public boolean equals(Object obj) {\n");
		buf.append("                return false;\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("I()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertProposalDoesNotExist(proposals, FixMessages.LambdaExpressionsFix_convert_to_lambda_expression);
	}

	@Test
	public void testConvertToLambda7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("abstract class C {\n");
		buf.append("    abstract void method();\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(C c) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        bar(new C() {\n");
		buf.append("            public void method() {\n");
		buf.append("                System.out.println();\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("C()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertProposalDoesNotExist(proposals, FixMessages.LambdaExpressionsFix_convert_to_lambda_expression);
	}

	@Test
	public void testConvertToLambda8() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    void method();\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(I i) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        bar(new I() {\n");
		buf.append("            public void method() {\n");
		buf.append("                System.out.println();\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("I()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    void method();\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(I i) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        bar(() -> System.out.println());\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambda9() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    int method();\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(I i) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        bar(new I() {\n");
		buf.append("            public int method() {\n");
		buf.append("                return 1;\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("I()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    int method();\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(I i) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        bar(() -> 1);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambda10() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    int foo(String s);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface J {\n");
		buf.append("    Integer foo(String s);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class X {\n");
		buf.append("    static void goo(I i) { }\n");
		buf.append("\n");
		buf.append("    static void goo(J j) { }\n");
		buf.append("\n");
		buf.append("    public static void main(String[] args) {\n");
		buf.append("        goo(new I() {\n");
		buf.append("            @Override\n");
		buf.append("            public int foo(String s) {\n");
		buf.append("                return 0;\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("X.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("I()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    int foo(String s);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface J {\n");
		buf.append("    Integer foo(String s);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class X {\n");
		buf.append("    static void goo(I i) { }\n");
		buf.append("\n");
		buf.append("    static void goo(J j) { }\n");
		buf.append("\n");
		buf.append("    public static void main(String[] args) {\n");
		buf.append("        goo((I) s -> 0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambda11() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    int foo(String s);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface J {\n");
		buf.append("    Integer foo(String s);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class X extends Y {\n");
		buf.append("    static void goo(I i) { }\n");
		buf.append("    public static void main(String[] args) {\n");
		buf.append("        goo(new I() {\n");
		buf.append("            @Override\n");
		buf.append("            public int foo(String s) {\n");
		buf.append("                return 0;\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Y {\n");
		buf.append("    private static void goo(J j) { }    \n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("X.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("I()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    int foo(String s);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface J {\n");
		buf.append("    Integer foo(String s);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class X extends Y {\n");
		buf.append("    static void goo(I i) { }\n");
		buf.append("    public static void main(String[] args) {\n");
		buf.append("        goo(s -> 0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Y {\n");
		buf.append("    private static void goo(J j) { }    \n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambda12() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    int foo(String s);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface J {\n");
		buf.append("    Integer foo(String s);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class X extends Y {\n");
		buf.append("    static void goo(I i) { }\n");
		buf.append("    public static void main(String[] args) {\n");
		buf.append("        goo(new I() {\n");
		buf.append("            @Override\n");
		buf.append("            public int foo(String s) {\n");
		buf.append("                return 0;\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Y {\n");
		buf.append("    static void goo(J j) { }    \n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("X.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("I()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    int foo(String s);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface J {\n");
		buf.append("    Integer foo(String s);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class X extends Y {\n");
		buf.append("    static void goo(I i) { }\n");
		buf.append("    public static void main(String[] args) {\n");
		buf.append("        goo((I) s -> 0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Y {\n");
		buf.append("    static void goo(J j) { }    \n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambda13() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface J {\n");
		buf.append("    <M> J run(M x);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Test {\n");
		buf.append("    J j = new J() {\n");
		buf.append("        @Override\n");
		buf.append("        public <M> J run(M x) {\n");
		buf.append("            return null;\n");
		buf.append("        }\n");
		buf.append("    };    \n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("J()"); // generic lambda not allowed
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, FixMessages.LambdaExpressionsFix_convert_to_lambda_expression);
	}

	@Test
	public void testConvertToLambda14() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI {\n");
		buf.append("    int foo(int x, int y, int z);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class C {\n");
		buf.append("    int i;\n");
		buf.append("    private void test(int x) {\n");
		buf.append("        int y;\n");
		buf.append("        FI fi = new FI() {\n");
		buf.append("            @Override\n");
		buf.append("            public int foo(int x/*km*/, int i /*inches*/, int y/*yards*/) {\n");
		buf.append("                return x + i + y;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("FI()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI {\n");
		buf.append("    int foo(int x, int y, int z);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class C {\n");
		buf.append("    int i;\n");
		buf.append("    private void test(int x) {\n");
		buf.append("        int y;\n");
		buf.append("        FI fi = (x1/*km*/, i /*inches*/, y1/*yards*/) -> x1 + i + y1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambda15() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI {\n");
		buf.append("    int foo(int x, int y, int z);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class C {\n");
		buf.append("    int i;\n");
		buf.append("    private void test(int x, int y, int z) {\n");
		buf.append("        FI fi = new FI() {\n");
		buf.append("            @Override\n");
		buf.append("            public int foo(int a, int b, int z) {\n");
		buf.append("                int x= 0, y=0; \n");
		buf.append("                return x + y + z;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("FI()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI {\n");
		buf.append("    int foo(int x, int y, int z);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class C {\n");
		buf.append("    int i;\n");
		buf.append("    private void test(int x, int y, int z) {\n");
		buf.append("        FI fi = (a, b, z1) -> {\n");
		buf.append("            int x1= 0, y1=0; \n");
		buf.append("            return x1 + y1 + z1;\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambda16() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface FI {\n");
		buf.append("    void foo();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class C1 {\n");
		buf.append("    void fun1() {\n");
		buf.append("        int c = 0; // [1]\n");
		buf.append("        FI test = new FI() {\n");
		buf.append("            @Override\n");
		buf.append("            public void foo() {\n");
		buf.append("                for (int c = 0; c < 10;) { /* [2] */ }\n");
		buf.append("                for (int c = 0; c < 20;) { /* [3] */ }\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("FI()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface FI {\n");
		buf.append("    void foo();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class C1 {\n");
		buf.append("    void fun1() {\n");
		buf.append("        int c = 0; // [1]\n");
		buf.append("        FI test = () -> {\n");
		buf.append("            for (int c1 = 0; c1 < 10;) { /* [2] */ }\n");
		buf.append("            for (int c2 = 0; c2 < 20;) { /* [3] */ }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambda17() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface X {\n");
		buf.append("    void foo();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class CX {\n");
		buf.append("    private void fun(int a) {\n");
		buf.append("        X x= new X() {\n");
		buf.append("            @Override\n");
		buf.append("            public void foo() {\n");
		buf.append("                int a; \n");
		buf.append("                int a1;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("CX.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("X()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface X {\n");
		buf.append("    void foo();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class CX {\n");
		buf.append("    private void fun(int a) {\n");
		buf.append("        X x= () -> {\n");
		buf.append("            int a2; \n");
		buf.append("            int a1;\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambda18() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface FIOther {\n");
		buf.append("    void run(int x);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class TestOther {\n");
		buf.append("    void init() {\n");
		buf.append("        String x;\n");
		buf.append("        m(x1 -> {\n");
		buf.append("            FIOther fi = new FIOther() {\n");
		buf.append("                @Override\n");
		buf.append("                public void run(int x1) { \n");
		buf.append("                    return;\n");
		buf.append("                }\n");
		buf.append("            };\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    void m(FIOther fi) {\n");
		buf.append("    };\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("TestOther.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("FIOther()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface FIOther {\n");
		buf.append("    void run(int x);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class TestOther {\n");
		buf.append("    void init() {\n");
		buf.append("        String x;\n");
		buf.append("        m(x1 -> {\n");
		buf.append("            FIOther fi = x11 -> { \n");
		buf.append("                return;\n");
		buf.append("            };\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    void m(FIOther fi) {\n");
		buf.append("    };\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambda19() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class C1 {\n");
		buf.append("    Runnable r1 = new/*[1]*/ Runnable() {\n");
		buf.append("        @Override @A @Deprecated\n");
		buf.append("        public void run() {\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("    Runnable r2 = new/*[2]*/ Runnable() {\n");
		buf.append("        @Override @Deprecated\n");
		buf.append("        public void run() {\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("}\n");
		buf.append("@interface A {}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("new/*[1]*/");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder buf1= new StringBuilder();
		buf1.append("package test;\n");
		buf1.append("public class C1 {\n");
		buf1.append("    Runnable r1 = () -> {\n");
		buf1.append("    };\n");
		buf1.append("    Runnable r2 = new/*[2]*/ Runnable() {\n");
		buf1.append("        @Override @Deprecated\n");
		buf1.append("        public void run() {\n");
		buf1.append("        }\n");
		buf1.append("    };\n");
		buf1.append("}\n");
		buf1.append("@interface A {}\n");
		String expected= buf1.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });

		offset= buf.toString().indexOf("new/*[2]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf1= new StringBuilder();
		buf1.append("package test;\n");
		buf1.append("public class C1 {\n");
		buf1.append("    Runnable r1 = new/*[1]*/ Runnable() {\n");
		buf1.append("        @Override @A @Deprecated\n");
		buf1.append("        public void run() {\n");
		buf1.append("        }\n");
		buf1.append("    };\n");
		buf1.append("    Runnable r2 = () -> {\n");
		buf1.append("    };\n");
		buf1.append("}\n");
		buf1.append("@interface A {}\n");
		expected= buf1.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertToLambda20() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class C1 {\n");
		buf.append("    FI fi= new  FI() {\n");
		buf.append("        @Override\n");
		buf.append("        public void foo(@A String... strs) {\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("}\n");
		buf.append("interface FI {\n");
		buf.append("    void foo(String... strs);\n");
		buf.append("}\n");
		buf.append("@interface A {}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("new");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class C1 {\n");
		buf.append("    FI fi= (@A String... strs) -> {\n");
		buf.append("    };\n");
		buf.append("}\n");
		buf.append("interface FI {\n");
		buf.append("    void foo(String... strs);\n");
		buf.append("}\n");
		buf.append("@interface A {}\n");
		String expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertToLambda21() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("\n");
		buf.append("public class C1 {\n");
		buf.append("    FI fi= new  FI() {\n");
		buf.append("        @Override\n");
		buf.append("        public void foo(@A String... strs) {\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("}\n");
		buf.append("interface FI {\n");
		buf.append("    void foo(String... strs);\n");
		buf.append("}\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("@interface A {}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("new");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("\n");
		buf.append("public class C1 {\n");
		buf.append("    FI fi= (@A String... strs) -> {\n");
		buf.append("    };\n");
		buf.append("}\n");
		buf.append("interface FI {\n");
		buf.append("    void foo(String... strs);\n");
		buf.append("}\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("@interface A {}\n");
		String expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertToLambda22() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class C1 {\n");
		buf.append("    FI fi1 = new/*[1]*/ FI() {\n");
		buf.append("        @Override\n");
		buf.append("        public void foo(java.util.@T ArrayList<IOException> x) {\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("    FI fi2 = new/*[2]*/ FI() {\n");
		buf.append("        @Override\n");
		buf.append("        public void foo(ArrayList<@T(val1=0, val2=8) IOException> x) {\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("    FI fi3 = new/*[3]*/ FI() {\n");
		buf.append("        @Override\n");
		buf.append("        public void foo(ArrayList<@T(val1=0) IOException> x) {\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("}\n");
		buf.append("interface FI {\n");
		buf.append("    void foo(ArrayList<IOException> x);\n");
		buf.append("}\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("@interface T {\n");
		buf.append("    int val1() default 1;\n");
		buf.append("    int val2() default -1;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("new/*[1]*/");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder buf1= new StringBuilder();
		buf1.append("package test;\n");
		buf1.append("import java.io.IOException;\n");
		buf1.append("import java.lang.annotation.ElementType;\n");
		buf1.append("import java.lang.annotation.Target;\n");
		buf1.append("import java.util.ArrayList;\n");
		buf1.append("public class C1 {\n");
		buf1.append("    FI fi1 = (java.util.@T ArrayList<IOException> x) -> {\n");
		buf1.append("    };\n");
		buf1.append("    FI fi2 = new/*[2]*/ FI() {\n");
		buf1.append("        @Override\n");
		buf1.append("        public void foo(ArrayList<@T(val1=0, val2=8) IOException> x) {\n");
		buf1.append("        }\n");
		buf1.append("    };\n");
		buf1.append("    FI fi3 = new/*[3]*/ FI() {\n");
		buf1.append("        @Override\n");
		buf1.append("        public void foo(ArrayList<@T(val1=0) IOException> x) {\n");
		buf1.append("        }\n");
		buf1.append("    };\n");
		buf1.append("}\n");
		buf1.append("interface FI {\n");
		buf1.append("    void foo(ArrayList<IOException> x);\n");
		buf1.append("}\n");
		buf1.append("@Target(ElementType.TYPE_USE)\n");
		buf1.append("@interface T {\n");
		buf1.append("    int val1() default 1;\n");
		buf1.append("    int val2() default -1;\n");
		buf1.append("}\n");
		String expected= buf1.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });

		offset= buf.toString().indexOf("new/*[2]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf1= new StringBuilder();
		buf1.append("package test;\n");
		buf1.append("import java.io.IOException;\n");
		buf1.append("import java.lang.annotation.ElementType;\n");
		buf1.append("import java.lang.annotation.Target;\n");
		buf1.append("import java.util.ArrayList;\n");
		buf1.append("public class C1 {\n");
		buf1.append("    FI fi1 = new/*[1]*/ FI() {\n");
		buf1.append("        @Override\n");
		buf1.append("        public void foo(java.util.@T ArrayList<IOException> x) {\n");
		buf1.append("        }\n");
		buf1.append("    };\n");
		buf1.append("    FI fi2 = (ArrayList<@T(val1=0, val2=8) IOException> x) -> {\n");
		buf1.append("    };\n");
		buf1.append("    FI fi3 = new/*[3]*/ FI() {\n");
		buf1.append("        @Override\n");
		buf1.append("        public void foo(ArrayList<@T(val1=0) IOException> x) {\n");
		buf1.append("        }\n");
		buf1.append("    };\n");
		buf1.append("}\n");
		buf1.append("interface FI {\n");
		buf1.append("    void foo(ArrayList<IOException> x);\n");
		buf1.append("}\n");
		buf1.append("@Target(ElementType.TYPE_USE)\n");
		buf1.append("@interface T {\n");
		buf1.append("    int val1() default 1;\n");
		buf1.append("    int val2() default -1;\n");
		buf1.append("}\n");
		expected= buf1.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });

		offset= buf.toString().indexOf("new/*[3]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf1= new StringBuilder();
		buf1.append("package test;\n");
		buf1.append("import java.io.IOException;\n");
		buf1.append("import java.lang.annotation.ElementType;\n");
		buf1.append("import java.lang.annotation.Target;\n");
		buf1.append("import java.util.ArrayList;\n");
		buf1.append("public class C1 {\n");
		buf1.append("    FI fi1 = new/*[1]*/ FI() {\n");
		buf1.append("        @Override\n");
		buf1.append("        public void foo(java.util.@T ArrayList<IOException> x) {\n");
		buf1.append("        }\n");
		buf1.append("    };\n");
		buf1.append("    FI fi2 = new/*[2]*/ FI() {\n");
		buf1.append("        @Override\n");
		buf1.append("        public void foo(ArrayList<@T(val1=0, val2=8) IOException> x) {\n");
		buf1.append("        }\n");
		buf1.append("    };\n");
		buf1.append("    FI fi3 = (ArrayList<@T(val1=0) IOException> x) -> {\n");
		buf1.append("    };\n");
		buf1.append("}\n");
		buf1.append("interface FI {\n");
		buf1.append("    void foo(ArrayList<IOException> x);\n");
		buf1.append("}\n");
		buf1.append("@Target(ElementType.TYPE_USE)\n");
		buf1.append("@interface T {\n");
		buf1.append("    int val1() default 1;\n");
		buf1.append("    int val2() default -1;\n");
		buf1.append("}\n");
		expected= buf1.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertToLambda23() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("public class C1 {\n");
		buf.append("    FI1 fi1 = new/*[1]*/ FI1() {\n");
		buf.append("        @Override\n");
		buf.append("        public void foo(String @T [] x[]) {\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("    FI1 fi2 = new/*[2]*/ FI1() {\n");
		buf.append("        @Override\n");
		buf.append("        public void foo(String [] x @T[]) {\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("}\n");
		buf.append("interface FI1 {\n");
		buf.append("    void foo(String[] x []);\n");
		buf.append("}\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("@interface T {}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("new/*[1]*/");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder buf1= new StringBuilder();
		buf1.append("package test;\n");
		buf1.append("import java.lang.annotation.ElementType;\n");
		buf1.append("import java.lang.annotation.Target;\n");
		buf1.append("public class C1 {\n");
		buf1.append("    FI1 fi1 = (String @T [] x[]) -> {\n");
		buf1.append("    };\n");
		buf1.append("    FI1 fi2 = new/*[2]*/ FI1() {\n");
		buf1.append("        @Override\n");
		buf1.append("        public void foo(String [] x @T[]) {\n");
		buf1.append("        }\n");
		buf1.append("    };\n");
		buf1.append("}\n");
		buf1.append("interface FI1 {\n");
		buf1.append("    void foo(String[] x []);\n");
		buf1.append("}\n");
		buf1.append("@Target(ElementType.TYPE_USE)\n");
		buf1.append("@interface T {}\n");
		String expected= buf1.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });

		offset= buf.toString().indexOf("new/*[2]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf1= new StringBuilder();
		buf1.append("package test;\n");
		buf1.append("import java.lang.annotation.ElementType;\n");
		buf1.append("import java.lang.annotation.Target;\n");
		buf1.append("public class C1 {\n");
		buf1.append("    FI1 fi1 = new/*[1]*/ FI1() {\n");
		buf1.append("        @Override\n");
		buf1.append("        public void foo(String @T [] x[]) {\n");
		buf1.append("        }\n");
		buf1.append("    };\n");
		buf1.append("    FI1 fi2 = (String [] x @T[]) -> {\n");
		buf1.append("    };\n");
		buf1.append("}\n");
		buf1.append("interface FI1 {\n");
		buf1.append("    void foo(String[] x []);\n");
		buf1.append("}\n");
		buf1.append("@Target(ElementType.TYPE_USE)\n");
		buf1.append("@interface T {}\n");
		expected= buf1.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertToLambda24() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("public class C1 {\n");
		buf.append("    FI1 fi1 = new FI1() {\n");
		buf.append("        @Override\n");
		buf.append("        public void foo(int i, String[] @T... x) {\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("}\n");
		buf.append("interface FI1 {\n");
		buf.append("    void foo(int i, String[] ... x);\n");
		buf.append("}\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("@interface T {}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("new");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("public class C1 {\n");
		buf.append("    FI1 fi1 = (int i, String[] @T... x) -> {\n");
		buf.append("    };\n");
		buf.append("}\n");
		buf.append("interface FI1 {\n");
		buf.append("    void foo(int i, String[] ... x);\n");
		buf.append("}\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("@interface T {}\n");
		String expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertToLambda25() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class C1 {\n");
		buf.append("    Runnable run = new Runnable() {\n");
		buf.append("        @Override\n");
		buf.append("        public synchronized void run() {\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("new");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 0);
	}

	@Test
	public void testConvertToLambda26() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class C1 {\n");
		buf.append("    Runnable run = new Runnable() {\n");
		buf.append("        @Override\n");
		buf.append("        public strictfp void run() {\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("new");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 0);
	}

	@Test
	public void testConvertToLambda27() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface FI {\n");
		buf.append("    int e= 0;\n");
		buf.append("    void run(int x);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Test {\n");
		buf.append("    {\n");
		buf.append("        FI fi = new FI() {\n");
		buf.append("            @Override\n");
		buf.append("            public void run(int e) {\n");
		buf.append("                FI fi = new FI() {\n");
		buf.append("                    @Override\n");
		buf.append("                    public void run(int e) { // [1]\n");
		buf.append("                        return;\n");
		buf.append("                    }\n");
		buf.append("                };\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("run(int e) { // [1]");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface FI {\n");
		buf.append("    int e= 0;\n");
		buf.append("    void run(int x);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Test {\n");
		buf.append("    {\n");
		buf.append("        FI fi = new FI() {\n");
		buf.append("            @Override\n");
		buf.append("            public void run(int e) {\n");
		buf.append("                FI fi = e1 -> { // [1]\n");
		buf.append("                    return;\n");
		buf.append("                };\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambda28() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String buf=
		"package test;\n" +
		"public class C1 {\n" +
		"    private final String s;\n" +
		"    Runnable run = new Runnable() {\n" +
		"        @Override\n" +
		"        public void run() {\n" +
		"           for (int i=0; i < s.length(); ++i) {\n" +
		"               int j = i;\n" +
		"           }\n" +
		"        }\n" +
		"    };\n" +
		"    public C1() {\n" +
		"        s = \"abc\";\n" +
		"    }\n" +
		"}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", buf, false, null);

		int offset= buf.toString().indexOf("new");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 0);
	}

	@Test
	public void testConvertToLambda29() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String buf= ""
				+ "package test;\n"
				+ "public class C1 {\n"
				+ "    private final String s = \"ABC\";\n"
				+ "    Runnable run = new Runnable() {\n"
				+ "        @Override\n"
				+ "        public void run() {\n"
				+ "           for (int i=0; i < s.length(); ++i) {\n"
				+ "               int j = i;\n"
				+ "           }\n"
				+ "        }\n"
				+ "    };\n"
				+ "}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", buf, false, null);

		int offset= buf.toString().indexOf("new");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected1= ""
				+ "package test;\n"
				+ "public class C1 {\n"
				+ "    private final String s = \"ABC\";\n"
			    + "    Runnable run = () -> {\n"
			    + "       for (int i=0; i < s.length(); ++i) {\n"
			    + "           int j = i;\n"
			    + "       }\n"
			    + "    };\n"
				+ "}\n";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambdaAmbiguousOverridden() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.function.Predicate;\n");
		buf.append("\n");
		buf.append("public class Test {\n");
		buf.append("    void foo(ArrayList<String> list) {\n");
		buf.append("        list.removeIf(new Predicate<String>() {\n");
		buf.append("            @Override\n");
		buf.append("            public boolean test(String t) {\n");
		buf.append("                return t.isEmpty();\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("public boolean test(");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("\n");
		buf.append("public class Test {\n");
		buf.append("    void foo(ArrayList<String> list) {\n");
		buf.append("        list.removeIf(t -> t.isEmpty());\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToAnonymousClassCreation1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    void method();\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(I i) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        bar(() -> {\n");
		buf.append("            System.out.println();\n");
		buf.append("            System.out.println();\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    void method();\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(I i) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        bar(new I() {\n");
		buf.append("            @Override\n");
		buf.append("            public void method() {\n");
		buf.append("                System.out.println();\n");
		buf.append("                System.out.println();\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToAnonymousClassCreation2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    void method(int a, int b);\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(I i) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        bar((int a, int b) -> {\n");
		buf.append("            System.out.println(a+b);\n");
		buf.append("            System.out.println(a+b);\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    void method(int a, int b);\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(I i) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        bar(new I() {\n");
		buf.append("            @Override\n");
		buf.append("            public void method(int a, int b) {\n");
		buf.append("                System.out.println(a+b);\n");
		buf.append("                System.out.println(a+b);\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToAnonymousClassCreation3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    void method();\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(I i) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        bar(() -> System.out.println());\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 6);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    void method();\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(I i) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        bar(new I() {\n");
		buf.append("            @Override\n");
		buf.append("            public void method() {\n");
		buf.append("                System.out.println();\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToAnonymousClassCreation4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    int method();\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(I i) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        bar(() -> 1);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    int method();\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(I i) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        bar(new I() {\n");
		buf.append("            @Override\n");
		buf.append("            public int method() {\n");
		buf.append("                return 1;\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToAnonymousClassCreation5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface FX {\n");
		buf.append("    default int defaultMethod(String x) {\n");
		buf.append("        return -1;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    int foo(int x);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class TestX {\n");
		buf.append("    FX fxx = x -> {\n");
		buf.append("        return (new FX() {\n");
		buf.append("            @Override\n");
		buf.append("            public int foo(int x) {\n");
		buf.append("                return 0;\n");
		buf.append("            }\n");
		buf.append("        }).defaultMethod(\"a\");\n");
		buf.append("    };\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("TestX.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface FX {\n");
		buf.append("    default int defaultMethod(String x) {\n");
		buf.append("        return -1;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    int foo(int x);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class TestX {\n");
		buf.append("    FX fxx = new FX() {\n");
		buf.append("        @Override\n");
		buf.append("        public int foo(int x) {\n");
		buf.append("            return (new FX() {\n");
		buf.append("                @Override\n");
		buf.append("                public int foo(int x) {\n");
		buf.append("                    return 0;\n");
		buf.append("                }\n");
		buf.append("            }).defaultMethod(\"a\");\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToAnonymousClassCreation6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.UnaryOperator;\n");
		buf.append("\n");
		buf.append("public class Snippet {\n");
		buf.append("    UnaryOperator<String> fi3 = x -> {\n");
		buf.append("        return x.toString();\n");
		buf.append("    };\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Snippet.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.UnaryOperator;\n");
		buf.append("\n");
		buf.append("public class Snippet {\n");
		buf.append("    UnaryOperator<String> fi3 = new UnaryOperator<String>() {\n");
		buf.append("        @Override\n");
		buf.append("        public String apply(String x) {\n");
		buf.append("            return x.toString();\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	// Bug 427694: [1.8][compiler] Functional interface not identified correctly
	public void _testConvertToAnonymousClassCreation7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I { Object m(Class c); }\n");
		buf.append("interface J<S> { S m(Class<?> c); }\n");
		buf.append("interface K<T> { T m(Class<?> c); }\n");
		buf.append("interface Functional<S,T> extends I, J<S>, K<T> {}\n");
		buf.append("\n");
		buf.append("class C {\n");
		buf.append("    Functional<?, ?> fun= (c) -> { return null;};\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I { Object m(Class c); }\n");
		buf.append("interface J<S> { S m(Class<?> c); }\n");
		buf.append("interface K<T> { T m(Class<?> c); }\n");
		buf.append("interface Functional<S,T> extends I, J<S>, K<T> {}\n");
		buf.append("\n");
		buf.append("class C {\n");
		buf.append("    Functional<?, ?> fun= new Functional<Object, Object>() {\n");
		buf.append("        @Override\n");
		buf.append("        public Object m(Class c) {\n");
		buf.append("            return null;\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToAnonymousClassCreation8() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.IntSupplier;\n");
		buf.append("public class B extends C {\n");
		buf.append("    private int var;\n");
		buf.append("    B() {\n");
		buf.append("        IntSupplier i = () -> {\n");
		buf.append("            int j = this.var;\n");
		buf.append("            super.o();\n");
		buf.append("            int k = super.varC;\n");
		buf.append("            return this.m();\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    public int m() {\n");
		buf.append("        return 7;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class C {\n");
		buf.append("    int varC;\n");
		buf.append("    public void o() {}\n");
		buf.append("}\n");

		ICompilationUnit cu= pack1.createCompilationUnit("B.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.IntSupplier;\n");
		buf.append("public class B extends C {\n");
		buf.append("    private int var;\n");
		buf.append("    B() {\n");
		buf.append("        IntSupplier i = new IntSupplier() {\n");
		buf.append("            @Override\n");
		buf.append("            public int getAsInt() {\n");
		buf.append("                int j = B.this.var;\n");
		buf.append("                B.super.o();\n");
		buf.append("                int k = B.super.varC;\n");
		buf.append("                return B.this.m();\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    public int m() {\n");
		buf.append("        return 7;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class C {\n");
		buf.append("    int varC;\n");
		buf.append("    public void o() {}\n");
		buf.append("}\n");


		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToAnonymousClassCreation9() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.IntSupplier;\n");
		buf.append("public class D {\n");
		buf.append("    D() {\n");
		buf.append("        F<Object> f = new F<Object>() {\n");
		buf.append("            int x= 10;\n");
		buf.append("            @Override\n");
		buf.append("            public void run() {\n");
		buf.append("                IntSupplier i = () -> {\n");
		buf.append("                    class CX {\n");
		buf.append("                        int n=10;\n");
		buf.append("                        {\n");
		buf.append("                            this.n= 0;\n");
		buf.append("                        }\n");
		buf.append("                    }\n");
		buf.append("                    D.this.n();\n");
		buf.append("                    return this.x;\n");
		buf.append("                };\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("    public int n() {\n");
		buf.append("        return 7;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    @FunctionalInterface\n");
		buf.append("    public interface F<T> {\n");
		buf.append("        void run();\n");
		buf.append("    }\n");
		buf.append("}\n");

		ICompilationUnit cu= pack1.createCompilationUnit("D.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.IntSupplier;\n");
		buf.append("\n");
		buf.append("import test1.D.F;\n");
		buf.append("public class D {\n");
		buf.append("    D() {\n");
		buf.append("        F<Object> f = new F<Object>() {\n");
		buf.append("            int x= 10;\n");
		buf.append("            @Override\n");
		buf.append("            public void run() {\n");
		buf.append("                IntSupplier i = new IntSupplier() {\n");
		buf.append("                    @Override\n");
		buf.append("                    public int getAsInt() {\n");
		buf.append("                        class CX {\n");
		buf.append("                            int n=10;\n");
		buf.append("                            {\n");
		buf.append("                                this.n= 0;\n");
		buf.append("                            }\n");
		buf.append("                        }\n");
		buf.append("                        D.this.n();\n");
		buf.append("                        return F.this.x;\n");
		buf.append("                    }\n");
		buf.append("                };\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("    public int n() {\n");
		buf.append("        return 7;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    @FunctionalInterface\n");
		buf.append("    public interface F<T> {\n");
		buf.append("        void run();\n");
		buf.append("    }\n");
		buf.append("}\n");


		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToAnonymousClassCreationWithParameterName() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.IntFunction;\n");
		buf.append("public class E {\n");
		buf.append("    IntFunction<String> toString= (int i) -> Integer.toString(i);\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.IntFunction;\n");
		buf.append("public class E {\n");
		buf.append("    IntFunction<String> toString= new IntFunction<String>() {\n");
		buf.append("        @Override\n");
		buf.append("        public String apply(int i) {\n");
		buf.append("            return Integer.toString(i);\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testChangeLambdaBodyToBlock1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("    FI1 fi1a= x -> -1;\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI1 {\n");
		buf.append("    int foo(int x);\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("    FI1 fi1a= x -> {\n");
		buf.append("        return -1;\n");
		buf.append("    };\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI1 {\n");
		buf.append("    int foo(int x);\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testChangeLambdaBodyToBlock2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("    FI1 fi1b= x -> m1();\n");
		buf.append("    int m1(){ return 0; }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI1 {\n");
		buf.append("    int foo(int x);\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("    FI1 fi1b= x -> {\n");
		buf.append("        return m1();\n");
		buf.append("    };\n");
		buf.append("    int m1(){ return 0; }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI1 {\n");
		buf.append("    int foo(int x);\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testChangeLambdaBodyToBlock3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("    FI2 fi2b= x -> m1();\n");
		buf.append("    int m1() { return 0; }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI2 {\n");
		buf.append("    void foo(int x);\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("    FI2 fi2b= x -> {\n");
		buf.append("        m1();\n");
		buf.append("    };\n");
		buf.append("    int m1() { return 0; }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI2 {\n");
		buf.append("    void foo(int x);\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testChangeLambdaBodyToBlock4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("    FI2 fi2a= x -> System.out.println();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI2 {\n");
		buf.append("    void foo(int x);\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("    FI2 fi2a= x -> {\n");
		buf.append("        System.out.println();\n");
		buf.append("    };\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI2 {\n");
		buf.append("    void foo(int x);\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testChangeLambdaBodyToExpression1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("    FI1 fi1= x -> {\n");
		buf.append("        return x=0;\n");
		buf.append("    };\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI1 {\n");
		buf.append("    int foo(int x);\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("    FI1 fi1= x -> x=0;\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI1 {\n");
		buf.append("    int foo(int x);\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testChangeLambdaBodyToExpression2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("    FI2 fi2= x -> {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    };\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI2 {\n");
		buf.append("    void foo(int x);\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("    FI2 fi2= x -> new Runnable() {\n");
		buf.append("        public void run() {\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI2 {\n");
		buf.append("    void foo(int x);\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testChangeLambdaBodyToExpression3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("    FI2 fi2= x -> { m1(); };\n");
		buf.append("    int m1(){ return 0; }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI2 {\n");
		buf.append("    void foo(int x);\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("    FI2 fi2= x -> m1();\n");
		buf.append("    int m1(){ return 0; }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI2 {\n");
		buf.append("    void foo(int x);\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testChangeLambdaBodyToExpression4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("    FI2 fi2= x -> {\n");
		buf.append("        super.toString();\n");
		buf.append("    };\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI2 {\n");
		buf.append("    void foo(int x);\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("    FI2 fi2= x -> super.toString();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI2 {\n");
		buf.append("    void foo(int x);\n");
		buf.append("}\n");
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testChangeLambdaBodyToExpression5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("    FI2 fi2= x -> {\n");
		buf.append("        --x;\n");
		buf.append("    };\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI2 {\n");
		buf.append("    void foo(int x);\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("    FI2 fi2= x -> --x;\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI2 {\n");
		buf.append("    void foo(int x);\n");
		buf.append("}\n");
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testChangeLambdaBodyToExpression6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("    FI2 fi2z= x -> { };\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI2 {\n");
		buf.append("    void foo(int x);\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_change_lambda_body_to_expression);
	}

	@Test
	public void testChangeLambdaBodyToExpression7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("    FI2 fi2c = x ->    {\n");
		buf.append("        return;\n");
		buf.append("    };\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI2 {\n");
		buf.append("    void foo(int x);\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_change_lambda_body_to_expression);
	}

	@Test
	public void testChangeLambdaBodyToExpression8() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("    FI2 fi2c = x ->    {\n");
		buf.append("        int n= 0;\n");
		buf.append("    };\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI2 {\n");
		buf.append("    void foo(int x);\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_change_lambda_body_to_expression);
	}

	@Test
	public void testExtractLambdaBodyToMethod1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("	public void foo1(int a) {\n");
		buf.append("		FI2 k = (e) -> {\n");
		buf.append("			int x = e + 3;\n");
		buf.append("			if (x > 3) {\n");
		buf.append("				return a;\n");
		buf.append("			}\n");
		buf.append("			return x;\n");
		buf.append("		};\n");
		buf.append("		k.foo(3);\n");
		buf.append("	}\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI2 {\n");
		buf.append("    int foo(int x);\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("+");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_extractmethod_description);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("	public void foo1(int a) {\n");
		buf.append("		FI2 k = (e) -> extracted(a, e);\n");
		buf.append("		k.foo(3);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("    private int extracted(int a, int e) {\n");
		buf.append("        int x = e + 3;\n");
		buf.append("        if (x > 3) {\n");
		buf.append("        	return a;\n");
		buf.append("        }\n");
		buf.append("        return x;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI2 {\n");
		buf.append("    int foo(int x);\n");
		buf.append("}\n");

		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testExtractLambdaBodyToMethod2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("	public void foo1(int a) {\n");
		buf.append("		FI2 k = (e) -> {\n");
		buf.append("			int x = e + 3;\n");
		buf.append("			if (x > 3) {\n");
		buf.append("				return a;\n");
		buf.append("			}\n");
		buf.append("			return x;\n");
		buf.append("		};\n");
		buf.append("		k.foo(3);\n");
		buf.append("	}\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI2 {\n");
		buf.append("    int foo(int x);\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("e + 3");
		AssistContext context= getCorrectionContext(cu, offset, 5);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertCorrectLabels(proposals);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("	public void foo1(int a) {\n");
		buf.append("		FI2 k = (e) -> extracted(a, e);\n");
		buf.append("		k.foo(3);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("    private int extracted(int a, int e) {\n");
		buf.append("        int x = e + 3;\n");
		buf.append("        if (x > 3) {\n");
		buf.append("        	return a;\n");
		buf.append("        }\n");
		buf.append("        return x;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI2 {\n");
		buf.append("    int foo(int x);\n");
		buf.append("}\n");

		String expected1= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("	public void foo1(int a) {\n");
		buf.append("		FI2 k = (e) -> {\n");
		buf.append("			int x = extracted(e);\n");
		buf.append("			if (x > 3) {\n");
		buf.append("				return a;\n");
		buf.append("			}\n");
		buf.append("			return x;\n");
		buf.append("		};\n");
		buf.append("		k.foo(3);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("    private int extracted(int e) {\n");
		buf.append("        return e + 3;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI2 {\n");
		buf.append("    int foo(int x);\n");
		buf.append("}\n");

		String expected2= buf.toString();
		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
	}

	@Test
	public void testExtractLambdaBodyToMethod3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("	public void foo1(int a) {\n");
		buf.append("		FI2 k = (e) -> {\n");
		buf.append("			int x = e + 3;\n");
		buf.append("            System.out.println(\"help\");\n");
		buf.append("			if (x > 3) {\n");
		buf.append("				return a;\n");
		buf.append("			}\n");
		buf.append("			return x;\n");
		buf.append("		};\n");
		buf.append("		k.foo(3);\n");
		buf.append("	}\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI2 {\n");
		buf.append("    int foo(int x);\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("elp");
		AssistContext context= getCorrectionContext(cu, offset, 5);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertCorrectLabels(proposals);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("	public void foo1(int a) {\n");
		buf.append("		FI2 k = (e) -> extracted(a, e);\n");
		buf.append("		k.foo(3);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("    private int extracted(int a, int e) {\n");
		buf.append("        int x = e + 3;\n");
		buf.append("        System.out.println(\"help\");\n");
		buf.append("        if (x > 3) {\n");
		buf.append("        	return a;\n");
		buf.append("        }\n");
		buf.append("        return x;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI2 {\n");
		buf.append("    int foo(int x);\n");
		buf.append("}\n");

		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testBug433754() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("class E {\n");
		buf.append("    private void foo() {\n");
		buf.append("        for (String str : new String[1]) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("str");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("class E {\n");
		buf.append("    private void foo() {\n");
		buf.append("        String[] strings = new String[1];\n");
		buf.append("        for (int i = 0; i < strings.length; i++) {\n");
		buf.append("            String str = strings[i];\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testAddInferredLambdaParamTypes1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    FI fi= (i, s) -> {};\n");
		buf.append("}\n");
		buf.append("interface FI {\n");
		buf.append("    void foo(Integer i, String[]... s);\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    FI fi= (Integer i, String[][] s) -> {};\n"); // no varargs
		buf.append("}\n");
		buf.append("interface FI {\n");
		buf.append("    void foo(Integer i, String[]... s);\n");
		buf.append("}\n");
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testAddInferredLambdaParamTypes2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.Function;\n");
		buf.append("public class E2<T> {\n");
		buf.append("    Function<T, Object> fi = t -> null;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.Function;\n");
		buf.append("public class E2<T> {\n");
		buf.append("    Function<T, Object> fi = (T t) -> null;\n");
		buf.append("}\n");
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testAddInferredLambdaParamTypes3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.IntFunction;\n");
		buf.append("import java.util.function.Supplier;\n");
		buf.append("public class E3 {\n");
		buf.append("    Supplier<String> s = () -> \"\";\n");
		buf.append("    IntFunction<Object> ifn= (int i) -> null; \n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E3.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_add_inferred_lambda_parameter_types);

		offset= buf.toString().indexOf("-> null");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_add_inferred_lambda_parameter_types);
	}

	@Test
	public void testAddInferredLambdaParamTypes4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("public class E4 {\n");
		buf.append("    FI fi= (i, s) -> {};\n");
		buf.append("}\n");
		buf.append("interface FI {\n");
		buf.append("    void foo(@A Integer i, @A String @B [] s @C []);\n");
		buf.append("}\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("@interface A {}\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("@interface B {}\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("@interface C {}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E4.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("public class E4 {\n");
		buf.append("    FI fi= (@A Integer i, @A String @C [] @B [] s) -> {};\n");
		buf.append("}\n");
		buf.append("interface FI {\n");
		buf.append("    void foo(@A Integer i, @A String @B [] s @C []);\n");
		buf.append("}\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("@interface A {}\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("@interface B {}\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("@interface C {}\n");
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= buf.toString();
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertMethodReferenceToLambda1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    Baz bm = E1::test;\n");
		buf.append("    static <X> void test(X x) {}\n");
		buf.append("}\n");
		buf.append("interface Foo<T, N extends Number> {\n");
		buf.append("    void m(N arg2);\n");
		buf.append("    void m(T arg1);\n");
		buf.append("}\n");
		buf.append("interface Baz extends Foo<Integer, Integer> {}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("::") + 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    Baz bm = arg2 -> test(arg2);\n");
		buf.append("    static <X> void test(X x) {}\n");
		buf.append("}\n");
		buf.append("interface Foo<T, N extends Number> {\n");
		buf.append("    void m(N arg2);\n");
		buf.append("    void m(T arg1);\n");
		buf.append("}\n");
		buf.append("interface Baz extends Foo<Integer, Integer> {}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertMethodReferenceToLambda2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface J<R> {\n");
		buf.append("    <T> R run(T t);\n");
		buf.append("}\n");
		buf.append("public class E2 {\n");
		buf.append("    J<String> j1 = E2::<Object>test;    \n");
		buf.append("    \n");
		buf.append("    static <T> String test(T t) {\n");
		buf.append("        return \"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("::") + 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_lambda_expression);
	}

	@Test
	public void testConvertMethodReferenceToLambda3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf1= new StringBuilder();
		buf1.append("package test1;\n");
		buf1.append("import java.util.HashSet;\n");
		buf1.append("import java.util.function.*;\n");
		buf1.append("class E3<T> {\n");
		buf1.append("    IntFunction<int[][][]> ma = int[][][]::/*[1]*/new;\n");
		buf1.append("    Supplier<MyHashSet<Integer>> mb = MyHashSet::/*[2]*/new;\n");
		buf1.append("    Function<T, MyHashSet<Number>> mc = MyHashSet<Number>::/*[3]*/<T>new;\n");
		buf1.append("    Function<String, MyHashSet<Integer>> md = MyHashSet::/*[4]*/new;\n");
		buf1.append("}\n");
		buf1.append("class MyHashSet<T> extends HashSet<T> {\n");
		buf1.append("    public MyHashSet() {}\n");
		buf1.append("    public <A> MyHashSet(A a) {}\n");
		buf1.append("    public MyHashSet(String i) {}\n");
		buf1.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E3.java", buf1.toString(), false, null);

		// [1]
		int offset= buf1.toString().indexOf("::/*[1]*/") + 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.HashSet;\n");
		buf.append("import java.util.function.*;\n");
		buf.append("class E3<T> {\n");
		buf.append("    IntFunction<int[][][]> ma = arg0 -> new int[arg0][][];\n");
		buf.append("    Supplier<MyHashSet<Integer>> mb = MyHashSet::/*[2]*/new;\n");
		buf.append("    Function<T, MyHashSet<Number>> mc = MyHashSet<Number>::/*[3]*/<T>new;\n");
		buf.append("    Function<String, MyHashSet<Integer>> md = MyHashSet::/*[4]*/new;\n");
		buf.append("}\n");
		buf.append("class MyHashSet<T> extends HashSet<T> {\n");
		buf.append("    public MyHashSet() {}\n");
		buf.append("    public <A> MyHashSet(A a) {}\n");
		buf.append("    public MyHashSet(String i) {}\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [2]
		offset= buf1.toString().indexOf("::/*[2]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.HashSet;\n");
		buf.append("import java.util.function.*;\n");
		buf.append("class E3<T> {\n");
		buf.append("    IntFunction<int[][][]> ma = int[][][]::/*[1]*/new;\n");
		buf.append("    Supplier<MyHashSet<Integer>> mb = () -> new MyHashSet<>();\n");
		buf.append("    Function<T, MyHashSet<Number>> mc = MyHashSet<Number>::/*[3]*/<T>new;\n");
		buf.append("    Function<String, MyHashSet<Integer>> md = MyHashSet::/*[4]*/new;\n");
		buf.append("}\n");
		buf.append("class MyHashSet<T> extends HashSet<T> {\n");
		buf.append("    public MyHashSet() {}\n");
		buf.append("    public <A> MyHashSet(A a) {}\n");
		buf.append("    public MyHashSet(String i) {}\n");
		buf.append("}\n");
		expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [3]
		offset= buf1.toString().indexOf("::/*[3]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.HashSet;\n");
		buf.append("import java.util.function.*;\n");
		buf.append("class E3<T> {\n");
		buf.append("    IntFunction<int[][][]> ma = int[][][]::/*[1]*/new;\n");
		buf.append("    Supplier<MyHashSet<Integer>> mb = MyHashSet::/*[2]*/new;\n");
		buf.append("    Function<T, MyHashSet<Number>> mc = arg0 -> new <T>MyHashSet<Number>(arg0);\n");
		buf.append("    Function<String, MyHashSet<Integer>> md = MyHashSet::/*[4]*/new;\n");
		buf.append("}\n");
		buf.append("class MyHashSet<T> extends HashSet<T> {\n");
		buf.append("    public MyHashSet() {}\n");
		buf.append("    public <A> MyHashSet(A a) {}\n");
		buf.append("    public MyHashSet(String i) {}\n");
		buf.append("}\n");
		expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [4]
		offset= buf1.toString().indexOf("::/*[4]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.HashSet;\n");
		buf.append("import java.util.function.*;\n");
		buf.append("class E3<T> {\n");
		buf.append("    IntFunction<int[][][]> ma = int[][][]::/*[1]*/new;\n");
		buf.append("    Supplier<MyHashSet<Integer>> mb = MyHashSet::/*[2]*/new;\n");
		buf.append("    Function<T, MyHashSet<Number>> mc = MyHashSet<Number>::/*[3]*/<T>new;\n");
		buf.append("    Function<String, MyHashSet<Integer>> md = arg0 -> new MyHashSet<>(arg0);\n");
		buf.append("}\n");
		buf.append("class MyHashSet<T> extends HashSet<T> {\n");
		buf.append("    public MyHashSet() {}\n");
		buf.append("    public <A> MyHashSet(A a) {}\n");
		buf.append("    public MyHashSet(String i) {}\n");
		buf.append("}\n");
		expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertMethodReferenceToLambda4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.Function;\n");
		buf.append("public class E4 {\n");
		buf.append("    Function<String, String> p1 = E4::<Float>staticMethod;\n");
		buf.append("    static <F> String staticMethod(String s) {\n");
		buf.append("        return \"s\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E4.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("::") + 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.Function;\n");
		buf.append("public class E4 {\n");
		buf.append("    Function<String, String> p1 = arg0 -> E4.<Float>staticMethod(arg0);\n");
		buf.append("    static <F> String staticMethod(String s) {\n");
		buf.append("        return \"s\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertMethodReferenceToLambda5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf1= new StringBuilder();
		buf1.append("package test1;\n");
		buf1.append("import java.util.function.*;\n");
		buf1.append("public class E5<T> {\n");
		buf1.append("    <F> String method1() {\n");
		buf1.append("        return \"a\";\n");
		buf1.append("    }\n");
		buf1.append("    <F> String method1(E5<T> e) {\n");
		buf1.append("        return \"b\";\n");
		buf1.append("    }\n");
		buf1.append("}\n");
		buf1.append("class Sub extends E5<Integer> {\n");
		buf1.append("    Supplier<String> s1 = super::/*[1]*/method1;\n");
		buf1.append("    Supplier<String> s2 = Sub.super::/*[2]*/<Float>method1;\n");
		buf1.append("    Function<E5<Integer>, String> s3 = super::/*[3]*/<Float>method1;\n");
		buf1.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E5.java", buf1.toString(), false, null);

		// [1]
		int offset= buf1.toString().indexOf("::/*[1]*/") + 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.*;\n");
		buf.append("public class E5<T> {\n");
		buf.append("    <F> String method1() {\n");
		buf.append("        return \"a\";\n");
		buf.append("    }\n");
		buf.append("    <F> String method1(E5<T> e) {\n");
		buf.append("        return \"b\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class Sub extends E5<Integer> {\n");
		buf.append("    Supplier<String> s1 = () -> super./*[1]*/method1();\n");
		buf.append("    Supplier<String> s2 = Sub.super::/*[2]*/<Float>method1;\n");
		buf.append("    Function<E5<Integer>, String> s3 = super::/*[3]*/<Float>method1;\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [2]
		offset= buf1.toString().indexOf("::/*[2]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.*;\n");
		buf.append("public class E5<T> {\n");
		buf.append("    <F> String method1() {\n");
		buf.append("        return \"a\";\n");
		buf.append("    }\n");
		buf.append("    <F> String method1(E5<T> e) {\n");
		buf.append("        return \"b\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class Sub extends E5<Integer> {\n");
		buf.append("    Supplier<String> s1 = super::/*[1]*/method1;\n");
		buf.append("    Supplier<String> s2 = () -> Sub.super.<Float>method1();\n");
		buf.append("    Function<E5<Integer>, String> s3 = super::/*[3]*/<Float>method1;\n");
		buf.append("}\n");
		expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [3]
		offset= buf1.toString().indexOf("::/*[3]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.*;\n");
		buf.append("public class E5<T> {\n");
		buf.append("    <F> String method1() {\n");
		buf.append("        return \"a\";\n");
		buf.append("    }\n");
		buf.append("    <F> String method1(E5<T> e) {\n");
		buf.append("        return \"b\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class Sub extends E5<Integer> {\n");
		buf.append("    Supplier<String> s1 = super::/*[1]*/method1;\n");
		buf.append("    Supplier<String> s2 = Sub.super::/*[2]*/<Float>method1;\n");
		buf.append("    Function<E5<Integer>, String> s3 = arg0 -> super.<Float>method1(arg0);\n");
		buf.append("}\n");
		expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertMethodReferenceToLambda6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf1= new StringBuilder();
		buf1.append("package test1;\n");
		buf1.append("import java.util.function.*;\n");
		buf1.append("public class E6<T> {\n");
		buf1.append("    <F> String method1() {\n");
		buf1.append("        return \"a\";\n");
		buf1.append("    }\n");
		buf1.append("    <F> String method1(E6<T> e) {\n");
		buf1.append("        return \"b\";\n");
		buf1.append("    }\n");
		buf1.append("    Supplier<String> v1 = new E6<Integer>()::/*[1]*/method1;\n");
		buf1.append("    Supplier<String> v2 = this::/*[2]*/<Float>method1;\n");
		buf1.append("    Function<E6<Integer>, String> v3 = new E6<Integer>()::/*[3]*/<Float>method1;\n");
		buf1.append("    T1[] ts = new T1[5];\n");
		buf1.append("    BiFunction<Integer, Integer, Integer> m6 = ts[1]::/*[4]*/bar;\n");
		buf1.append("    int[] is = new int[5];\n");
		buf1.append("    Supplier<int[]> m10 = is::/*[5]*/clone;\n");
		buf1.append("}\n");
		buf1.append("class T1 {\n");
		buf1.append("    int bar(int i, int j) { return i + j; }\n");
		buf1.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E6.java", buf1.toString(), false, null);

		// [1]
		int offset= buf1.toString().indexOf("::/*[1]*/") + 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.*;\n");
		buf.append("public class E6<T> {\n");
		buf.append("    <F> String method1() {\n");
		buf.append("        return \"a\";\n");
		buf.append("    }\n");
		buf.append("    <F> String method1(E6<T> e) {\n");
		buf.append("        return \"b\";\n");
		buf.append("    }\n");
		buf.append("    Supplier<String> v1 = () -> new E6<Integer>()./*[1]*/method1();\n");
		buf.append("    Supplier<String> v2 = this::/*[2]*/<Float>method1;\n");
		buf.append("    Function<E6<Integer>, String> v3 = new E6<Integer>()::/*[3]*/<Float>method1;\n");
		buf.append("    T1[] ts = new T1[5];\n");
		buf.append("    BiFunction<Integer, Integer, Integer> m6 = ts[1]::/*[4]*/bar;\n");
		buf.append("    int[] is = new int[5];\n");
		buf.append("    Supplier<int[]> m10 = is::/*[5]*/clone;\n");
		buf.append("}\n");
		buf.append("class T1 {\n");
		buf.append("    int bar(int i, int j) { return i + j; }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [2]
		offset= buf1.toString().indexOf("::/*[2]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.*;\n");
		buf.append("public class E6<T> {\n");
		buf.append("    <F> String method1() {\n");
		buf.append("        return \"a\";\n");
		buf.append("    }\n");
		buf.append("    <F> String method1(E6<T> e) {\n");
		buf.append("        return \"b\";\n");
		buf.append("    }\n");
		buf.append("    Supplier<String> v1 = new E6<Integer>()::/*[1]*/method1;\n");
		buf.append("    Supplier<String> v2 = () -> this.<Float>method1();\n");
		buf.append("    Function<E6<Integer>, String> v3 = new E6<Integer>()::/*[3]*/<Float>method1;\n");
		buf.append("    T1[] ts = new T1[5];\n");
		buf.append("    BiFunction<Integer, Integer, Integer> m6 = ts[1]::/*[4]*/bar;\n");
		buf.append("    int[] is = new int[5];\n");
		buf.append("    Supplier<int[]> m10 = is::/*[5]*/clone;\n");
		buf.append("}\n");
		buf.append("class T1 {\n");
		buf.append("    int bar(int i, int j) { return i + j; }\n");
		buf.append("}\n");
		expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [3]
		offset= buf1.toString().indexOf("::/*[3]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.*;\n");
		buf.append("public class E6<T> {\n");
		buf.append("    <F> String method1() {\n");
		buf.append("        return \"a\";\n");
		buf.append("    }\n");
		buf.append("    <F> String method1(E6<T> e) {\n");
		buf.append("        return \"b\";\n");
		buf.append("    }\n");
		buf.append("    Supplier<String> v1 = new E6<Integer>()::/*[1]*/method1;\n");
		buf.append("    Supplier<String> v2 = this::/*[2]*/<Float>method1;\n");
		buf.append("    Function<E6<Integer>, String> v3 = arg0 -> new E6<Integer>().<Float>method1(arg0);\n");
		buf.append("    T1[] ts = new T1[5];\n");
		buf.append("    BiFunction<Integer, Integer, Integer> m6 = ts[1]::/*[4]*/bar;\n");
		buf.append("    int[] is = new int[5];\n");
		buf.append("    Supplier<int[]> m10 = is::/*[5]*/clone;\n");
		buf.append("}\n");
		buf.append("class T1 {\n");
		buf.append("    int bar(int i, int j) { return i + j; }\n");
		buf.append("}\n");
		expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [4]
		offset= buf1.toString().indexOf("::/*[4]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.*;\n");
		buf.append("public class E6<T> {\n");
		buf.append("    <F> String method1() {\n");
		buf.append("        return \"a\";\n");
		buf.append("    }\n");
		buf.append("    <F> String method1(E6<T> e) {\n");
		buf.append("        return \"b\";\n");
		buf.append("    }\n");
		buf.append("    Supplier<String> v1 = new E6<Integer>()::/*[1]*/method1;\n");
		buf.append("    Supplier<String> v2 = this::/*[2]*/<Float>method1;\n");
		buf.append("    Function<E6<Integer>, String> v3 = new E6<Integer>()::/*[3]*/<Float>method1;\n");
		buf.append("    T1[] ts = new T1[5];\n");
		buf.append("    BiFunction<Integer, Integer, Integer> m6 = (arg0, arg1) -> ts[1]./*[4]*/bar(arg0, arg1);\n");
		buf.append("    int[] is = new int[5];\n");
		buf.append("    Supplier<int[]> m10 = is::/*[5]*/clone;\n");
		buf.append("}\n");
		buf.append("class T1 {\n");
		buf.append("    int bar(int i, int j) { return i + j; }\n");
		buf.append("}\n");
		expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [5]
		offset= buf1.toString().indexOf("::/*[5]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.*;\n");
		buf.append("public class E6<T> {\n");
		buf.append("    <F> String method1() {\n");
		buf.append("        return \"a\";\n");
		buf.append("    }\n");
		buf.append("    <F> String method1(E6<T> e) {\n");
		buf.append("        return \"b\";\n");
		buf.append("    }\n");
		buf.append("    Supplier<String> v1 = new E6<Integer>()::/*[1]*/method1;\n");
		buf.append("    Supplier<String> v2 = this::/*[2]*/<Float>method1;\n");
		buf.append("    Function<E6<Integer>, String> v3 = new E6<Integer>()::/*[3]*/<Float>method1;\n");
		buf.append("    T1[] ts = new T1[5];\n");
		buf.append("    BiFunction<Integer, Integer, Integer> m6 = ts[1]::/*[4]*/bar;\n");
		buf.append("    int[] is = new int[5];\n");
		buf.append("    Supplier<int[]> m10 = () -> is./*[5]*/clone();\n");
		buf.append("}\n");
		buf.append("class T1 {\n");
		buf.append("    int bar(int i, int j) { return i + j; }\n");
		buf.append("}\n");
		expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertMethodReferenceToLambda7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf1= new StringBuilder();
		buf1.append("package test1;\n");
		buf1.append("import java.util.function.*;\n");
		buf1.append("public class E7<T> {\n");
		buf1.append("    <F> String method1() {\n");
		buf1.append("        return \"a\";\n");
		buf1.append("    }\n");
		buf1.append("    Function<E7<Integer>, String> v1 = E7<Integer>::/*[1]*/<Float>method1;\n");
		buf1.append("    Function<int[], int[]> v2 = int[]::/*[2]*/clone;\n");
		buf1.append("    BiFunction<int[], int[], Boolean> v3 = int[]::/*[3]*/equals;\n");
		buf1.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E7.java", buf1.toString(), false, null);

		// [1]
		int offset= buf1.toString().indexOf("::/*[1]*/") + 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.*;\n");
		buf.append("public class E7<T> {\n");
		buf.append("    <F> String method1() {\n");
		buf.append("        return \"a\";\n");
		buf.append("    }\n");
		buf.append("    Function<E7<Integer>, String> v1 = arg0 -> arg0.<Float>method1();\n");
		buf.append("    Function<int[], int[]> v2 = int[]::/*[2]*/clone;\n");
		buf.append("    BiFunction<int[], int[], Boolean> v3 = int[]::/*[3]*/equals;\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [2]
		offset= buf1.toString().indexOf("::/*[2]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.*;\n");
		buf.append("public class E7<T> {\n");
		buf.append("    <F> String method1() {\n");
		buf.append("        return \"a\";\n");
		buf.append("    }\n");
		buf.append("    Function<E7<Integer>, String> v1 = E7<Integer>::/*[1]*/<Float>method1;\n");
		buf.append("    Function<int[], int[]> v2 = arg0 -> arg0./*[2]*/clone();\n");
		buf.append("    BiFunction<int[], int[], Boolean> v3 = int[]::/*[3]*/equals;\n");
		buf.append("}\n");
		expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [3]
		offset= buf1.toString().indexOf("::/*[3]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.*;\n");
		buf.append("public class E7<T> {\n");
		buf.append("    <F> String method1() {\n");
		buf.append("        return \"a\";\n");
		buf.append("    }\n");
		buf.append("    Function<E7<Integer>, String> v1 = E7<Integer>::/*[1]*/<Float>method1;\n");
		buf.append("    Function<int[], int[]> v2 = int[]::/*[2]*/clone;\n");
		buf.append("    BiFunction<int[], int[], Boolean> v3 = (arg0, arg1) -> arg0./*[3]*/equals(arg1);\n");
		buf.append("}\n");
		expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertMethodReferenceToLambda8() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.*;\n");
		buf.append("import java.util.function.Supplier;\n");
		buf.append("public class E8 {\n");
		buf.append("    List<String> list = new ArrayList<>();\n");
		buf.append("    Supplier<Iterator<String>> mr = (list.size() == 5 ? list.subList(0, 3) : list)::iterator;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E8.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("::") + 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.*;\n");
		buf.append("import java.util.function.Supplier;\n");
		buf.append("public class E8 {\n");
		buf.append("    List<String> list = new ArrayList<>();\n");
		buf.append("    Supplier<Iterator<String>> mr = () -> (list.size() == 5 ? list.subList(0, 3) : list).iterator();\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertMethodReferenceToLambda9() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E9 {\n");
		buf.append("    private void test(int t) {\n");
		buf.append("        FI t1= E9::bar;\n");
		buf.append("    }\n");
		buf.append("    private static void bar(int x, int y, int z) {}\n");
		buf.append("}\n");
		buf.append("interface FI {\n");
		buf.append("    void foo(int t, int t1, int t2);\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E9.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("::") + 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E9 {\n");
		buf.append("    private void test(int t) {\n");
		buf.append("        FI t1= (t3, t11, t2) -> bar(t3, t11, t2);\n");
		buf.append("    }\n");
		buf.append("    private static void bar(int x, int y, int z) {}\n");
		buf.append("}\n");
		buf.append("interface FI {\n");
		buf.append("    void foo(int t, int t1, int t2);\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertMethodReferenceToLambda10() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.lang.annotation.*;\n");
		buf.append("import java.util.function.*;\n");
		buf.append("\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("@interface Great {}\n");
		buf.append("\n");
		buf.append("public class E10 {\n");
		buf.append("    LongSupplier foo() {\n");
		buf.append("        return @Great System::currentTimeMillis;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E10.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("::") + 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.lang.annotation.*;\n");
		buf.append("import java.util.function.*;\n");
		buf.append("\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("@interface Great {}\n");
		buf.append("\n");
		buf.append("public class E10 {\n");
		buf.append("    LongSupplier foo() {\n");
		buf.append("        return () -> System.currentTimeMillis();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertMethodReferenceToLambda11() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test01", false, null);
		StringBuilder buf1= new StringBuilder();
		buf1.append("package test01;\n");
		buf1.append("import java.util.function.Supplier;\n");
		buf1.append("public class E10 extends Sup {\n");
		buf1.append("    {\n");
		buf1.append("        Supplier<String> v1 = this::/*[1]*/method1;\n");
		buf1.append("        Supplier<String> v2 = this::/*[2]*/<Number>method1;\n");
		buf1.append("\n");
		buf1.append("        Supplier<String> n1 = E10::/*[3]*/method2;\n");
		buf1.append("        Supplier<String> n2 = E10::/*[4]*/method2a;\n");
		buf1.append("        Supplier<String> n3 = E10::/*[5]*/method3;\n");
		buf1.append("        Supplier<String> n4 = E10::/*[6]*/<Number>method2a;\n");
		buf1.append("\n");
		buf1.append("        Supplier<String> a1 = E10a::/*[7]*/method4;\n");
		buf1.append("    }\n");
		buf1.append("\n");
		buf1.append("    <T> String method1() {\n");
		buf1.append("        return \"1\";\n");
		buf1.append("    }\n");
		buf1.append("    static String method2() {\n");
		buf1.append("        return \"2\";\n");
		buf1.append("    }\n");
		buf1.append("    static <T> String method2a() {\n");
		buf1.append("        return \"2a\";\n");
		buf1.append("    }\n");
		buf1.append("    static String method4() {\n");
		buf1.append("        return \"4\";\n");
		buf1.append("    }\n");
		buf1.append("}\n");
		buf1.append("\n");
		buf1.append("class Sup {\n");
		buf1.append("    static String method3() {\n");
		buf1.append("        return \"3\";\n");
		buf1.append("    }\n");
		buf1.append("}\n");
		buf1.append("\n");
		buf1.append("class E10a {\n");
		buf1.append("    static String method4() {\n");
		buf1.append("        return \"4\";\n");
		buf1.append("    }\n");
		buf1.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E10.java", buf1.toString(), false, null);

		// [1]
		int offset= buf1.toString().indexOf("::/*[1]*/") + 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		StringBuilder buf= new StringBuilder();
		buf.append("package test01;\n");
		buf.append("import java.util.function.Supplier;\n");
		buf.append("public class E10 extends Sup {\n");
		buf.append("    {\n");
		buf.append("        Supplier<String> v1 = () -> /*[1]*/method1();\n");
		buf.append("        Supplier<String> v2 = this::/*[2]*/<Number>method1;\n");
		buf.append("\n");
		buf.append("        Supplier<String> n1 = E10::/*[3]*/method2;\n");
		buf.append("        Supplier<String> n2 = E10::/*[4]*/method2a;\n");
		buf.append("        Supplier<String> n3 = E10::/*[5]*/method3;\n");
		buf.append("        Supplier<String> n4 = E10::/*[6]*/<Number>method2a;\n");
		buf.append("\n");
		buf.append("        Supplier<String> a1 = E10a::/*[7]*/method4;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    <T> String method1() {\n");
		buf.append("        return \"1\";\n");
		buf.append("    }\n");
		buf.append("    static String method2() {\n");
		buf.append("        return \"2\";\n");
		buf.append("    }\n");
		buf.append("    static <T> String method2a() {\n");
		buf.append("        return \"2a\";\n");
		buf.append("    }\n");
		buf.append("    static String method4() {\n");
		buf.append("        return \"4\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Sup {\n");
		buf.append("    static String method3() {\n");
		buf.append("        return \"3\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class E10a {\n");
		buf.append("    static String method4() {\n");
		buf.append("        return \"4\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [2]
		offset= buf1.toString().indexOf("::/*[2]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test01;\n");
		buf.append("import java.util.function.Supplier;\n");
		buf.append("public class E10 extends Sup {\n");
		buf.append("    {\n");
		buf.append("        Supplier<String> v1 = this::/*[1]*/method1;\n");
		buf.append("        Supplier<String> v2 = () -> this.<Number>method1();\n");
		buf.append("\n");
		buf.append("        Supplier<String> n1 = E10::/*[3]*/method2;\n");
		buf.append("        Supplier<String> n2 = E10::/*[4]*/method2a;\n");
		buf.append("        Supplier<String> n3 = E10::/*[5]*/method3;\n");
		buf.append("        Supplier<String> n4 = E10::/*[6]*/<Number>method2a;\n");
		buf.append("\n");
		buf.append("        Supplier<String> a1 = E10a::/*[7]*/method4;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    <T> String method1() {\n");
		buf.append("        return \"1\";\n");
		buf.append("    }\n");
		buf.append("    static String method2() {\n");
		buf.append("        return \"2\";\n");
		buf.append("    }\n");
		buf.append("    static <T> String method2a() {\n");
		buf.append("        return \"2a\";\n");
		buf.append("    }\n");
		buf.append("    static String method4() {\n");
		buf.append("        return \"4\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Sup {\n");
		buf.append("    static String method3() {\n");
		buf.append("        return \"3\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class E10a {\n");
		buf.append("    static String method4() {\n");
		buf.append("        return \"4\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [3]
		offset= buf1.toString().indexOf("::/*[3]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test01;\n");
		buf.append("import java.util.function.Supplier;\n");
		buf.append("public class E10 extends Sup {\n");
		buf.append("    {\n");
		buf.append("        Supplier<String> v1 = this::/*[1]*/method1;\n");
		buf.append("        Supplier<String> v2 = this::/*[2]*/<Number>method1;\n");
		buf.append("\n");
		buf.append("        Supplier<String> n1 = () -> /*[3]*/method2();\n");
		buf.append("        Supplier<String> n2 = E10::/*[4]*/method2a;\n");
		buf.append("        Supplier<String> n3 = E10::/*[5]*/method3;\n");
		buf.append("        Supplier<String> n4 = E10::/*[6]*/<Number>method2a;\n");
		buf.append("\n");
		buf.append("        Supplier<String> a1 = E10a::/*[7]*/method4;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    <T> String method1() {\n");
		buf.append("        return \"1\";\n");
		buf.append("    }\n");
		buf.append("    static String method2() {\n");
		buf.append("        return \"2\";\n");
		buf.append("    }\n");
		buf.append("    static <T> String method2a() {\n");
		buf.append("        return \"2a\";\n");
		buf.append("    }\n");
		buf.append("    static String method4() {\n");
		buf.append("        return \"4\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Sup {\n");
		buf.append("    static String method3() {\n");
		buf.append("        return \"3\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class E10a {\n");
		buf.append("    static String method4() {\n");
		buf.append("        return \"4\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [4]
		offset= buf1.toString().indexOf("::/*[4]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test01;\n");
		buf.append("import java.util.function.Supplier;\n");
		buf.append("public class E10 extends Sup {\n");
		buf.append("    {\n");
		buf.append("        Supplier<String> v1 = this::/*[1]*/method1;\n");
		buf.append("        Supplier<String> v2 = this::/*[2]*/<Number>method1;\n");
		buf.append("\n");
		buf.append("        Supplier<String> n1 = E10::/*[3]*/method2;\n");
		buf.append("        Supplier<String> n2 = () -> /*[4]*/method2a();\n");
		buf.append("        Supplier<String> n3 = E10::/*[5]*/method3;\n");
		buf.append("        Supplier<String> n4 = E10::/*[6]*/<Number>method2a;\n");
		buf.append("\n");
		buf.append("        Supplier<String> a1 = E10a::/*[7]*/method4;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    <T> String method1() {\n");
		buf.append("        return \"1\";\n");
		buf.append("    }\n");
		buf.append("    static String method2() {\n");
		buf.append("        return \"2\";\n");
		buf.append("    }\n");
		buf.append("    static <T> String method2a() {\n");
		buf.append("        return \"2a\";\n");
		buf.append("    }\n");
		buf.append("    static String method4() {\n");
		buf.append("        return \"4\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Sup {\n");
		buf.append("    static String method3() {\n");
		buf.append("        return \"3\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class E10a {\n");
		buf.append("    static String method4() {\n");
		buf.append("        return \"4\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [5]
		offset= buf1.toString().indexOf("::/*[5]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test01;\n");
		buf.append("import java.util.function.Supplier;\n");
		buf.append("public class E10 extends Sup {\n");
		buf.append("    {\n");
		buf.append("        Supplier<String> v1 = this::/*[1]*/method1;\n");
		buf.append("        Supplier<String> v2 = this::/*[2]*/<Number>method1;\n");
		buf.append("\n");
		buf.append("        Supplier<String> n1 = E10::/*[3]*/method2;\n");
		buf.append("        Supplier<String> n2 = E10::/*[4]*/method2a;\n");
		buf.append("        Supplier<String> n3 = () -> /*[5]*/method3();\n");
		buf.append("        Supplier<String> n4 = E10::/*[6]*/<Number>method2a;\n");
		buf.append("\n");
		buf.append("        Supplier<String> a1 = E10a::/*[7]*/method4;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    <T> String method1() {\n");
		buf.append("        return \"1\";\n");
		buf.append("    }\n");
		buf.append("    static String method2() {\n");
		buf.append("        return \"2\";\n");
		buf.append("    }\n");
		buf.append("    static <T> String method2a() {\n");
		buf.append("        return \"2a\";\n");
		buf.append("    }\n");
		buf.append("    static String method4() {\n");
		buf.append("        return \"4\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Sup {\n");
		buf.append("    static String method3() {\n");
		buf.append("        return \"3\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class E10a {\n");
		buf.append("    static String method4() {\n");
		buf.append("        return \"4\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [6]
		offset= buf1.toString().indexOf("::/*[6]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test01;\n");
		buf.append("import java.util.function.Supplier;\n");
		buf.append("public class E10 extends Sup {\n");
		buf.append("    {\n");
		buf.append("        Supplier<String> v1 = this::/*[1]*/method1;\n");
		buf.append("        Supplier<String> v2 = this::/*[2]*/<Number>method1;\n");
		buf.append("\n");
		buf.append("        Supplier<String> n1 = E10::/*[3]*/method2;\n");
		buf.append("        Supplier<String> n2 = E10::/*[4]*/method2a;\n");
		buf.append("        Supplier<String> n3 = E10::/*[5]*/method3;\n");
		buf.append("        Supplier<String> n4 = () -> E10.<Number>method2a();\n");
		buf.append("\n");
		buf.append("        Supplier<String> a1 = E10a::/*[7]*/method4;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    <T> String method1() {\n");
		buf.append("        return \"1\";\n");
		buf.append("    }\n");
		buf.append("    static String method2() {\n");
		buf.append("        return \"2\";\n");
		buf.append("    }\n");
		buf.append("    static <T> String method2a() {\n");
		buf.append("        return \"2a\";\n");
		buf.append("    }\n");
		buf.append("    static String method4() {\n");
		buf.append("        return \"4\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Sup {\n");
		buf.append("    static String method3() {\n");
		buf.append("        return \"3\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class E10a {\n");
		buf.append("    static String method4() {\n");
		buf.append("        return \"4\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [7]
		offset= buf1.toString().indexOf("::/*[7]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test01;\n");
		buf.append("import java.util.function.Supplier;\n");
		buf.append("public class E10 extends Sup {\n");
		buf.append("    {\n");
		buf.append("        Supplier<String> v1 = this::/*[1]*/method1;\n");
		buf.append("        Supplier<String> v2 = this::/*[2]*/<Number>method1;\n");
		buf.append("\n");
		buf.append("        Supplier<String> n1 = E10::/*[3]*/method2;\n");
		buf.append("        Supplier<String> n2 = E10::/*[4]*/method2a;\n");
		buf.append("        Supplier<String> n3 = E10::/*[5]*/method3;\n");
		buf.append("        Supplier<String> n4 = E10::/*[6]*/<Number>method2a;\n");
		buf.append("\n");
		buf.append("        Supplier<String> a1 = () -> E10a./*[7]*/method4();\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    <T> String method1() {\n");
		buf.append("        return \"1\";\n");
		buf.append("    }\n");
		buf.append("    static String method2() {\n");
		buf.append("        return \"2\";\n");
		buf.append("    }\n");
		buf.append("    static <T> String method2a() {\n");
		buf.append("        return \"2a\";\n");
		buf.append("    }\n");
		buf.append("    static String method4() {\n");
		buf.append("        return \"4\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Sup {\n");
		buf.append("    static String method3() {\n");
		buf.append("        return \"3\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class E10a {\n");
		buf.append("    static String method4() {\n");
		buf.append("        return \"4\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertLambdaToMethodReference1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.BiFunction;\n");
		buf.append("import java.util.function.Consumer;\n");
		buf.append("import java.util.function.Function;\n");
		buf.append("import java.util.function.IntFunction;\n");
		buf.append("import java.util.function.Supplier;\n");
		buf.append("public class E1 extends E {\n");
		buf.append("    Supplier<String> a1= () ->/*[1]*/ {\n");
		buf.append("        String s = \"\";\n");
		buf.append("        return s;\n");
		buf.append("    };\n");
		buf.append("    Consumer<String> a2= s ->/*[2]*/ {\n");
		buf.append("        return;\n");
		buf.append("    };\n");
		buf.append("\n");
		buf.append("    Supplier<E1.In> a3= () ->/*[3]*/ (new E1()).new In();\n");
		buf.append("    Supplier<E1> a4= () ->/*[4]*/ new E1() {\n");
		buf.append("        void test() {\n");
		buf.append("            System.out.println(\"hey\");\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("    Function<String, Integer> a5= s ->/*[5]*/ Integer.valueOf(s+1);\n");
		buf.append("\n");
		buf.append("    BiFunction<Integer, Integer, int[][][]> a6 = (a, b) ->/*[6]*/ new int[a][b][];\n");
		buf.append("    IntFunction<Integer[][][]> a61 = value ->/*[61]*/ new Integer[][][] {{{7, 8}}};\n");
		buf.append("    Function<Integer, int[]> a7 = t ->/*[7]*/ new int[100];\n");
		buf.append("\n");
		buf.append("    BiFunction<Character, Integer, String> a8 = (c, i) ->/*[8]*/ super.method1();\n");
		buf.append("    BiFunction<Character, Integer, String> a9 = (c, i) ->/*[9]*/ method1();\n");
		buf.append("    \n");
		buf.append("    class In {}\n");
		buf.append("}\n");
		buf.append("class E {\n");
		buf.append("    String method1() {\n");
		buf.append("        return \"a\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->/*[1]*/");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_method_reference);

		offset= buf.toString().indexOf("->/*[2]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_method_reference);

		offset= buf.toString().indexOf("->/*[3]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_method_reference);

		offset= buf.toString().indexOf("->/*[4]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_method_reference);

		offset= buf.toString().indexOf("->/*[5]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_method_reference);

		offset= buf.toString().indexOf("->/*[6]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_method_reference);

		offset= buf.toString().indexOf("->/*[61]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_method_reference);

		offset= buf.toString().indexOf("->/*[7]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_method_reference);

		offset= buf.toString().indexOf("->/*[8]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_method_reference);

		offset= buf.toString().indexOf("->/*[9]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_method_reference);
	}

	@Test
	public void testConvertLambdaToMethodReference2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf1= new StringBuilder();
		buf1.append("package test1;\n");
		buf1.append("import java.util.function.Function;\n");
		buf1.append("public class E2<T> {\n");
		buf1.append("    public <A> E2(A a) {}\n");
		buf1.append("    public E2(String s) {}\n");
		buf1.append("    \n");
		buf1.append("    Function<T, E2<Integer>> a1 = t ->/*[1]*/ (new <T>E2<Integer>(t));\n");
		buf1.append("    Function<String, E2<Integer>> a2 = t ->/*[2]*/ new E2<>(t);\n");
		buf1.append("    \n");
		buf1.append("    Function<Integer, Float[]> a3 = t ->/*[3]*/ new Float[t];\n");
		buf1.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", buf1.toString(), false, null);

		int offset= buf1.toString().indexOf("->/*[1]*/");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.Function;\n");
		buf.append("public class E2<T> {\n");
		buf.append("    public <A> E2(A a) {}\n");
		buf.append("    public E2(String s) {}\n");
		buf.append("    \n");
		buf.append("    Function<T, E2<Integer>> a1 = E2<Integer>::<T>new;\n");
		buf.append("    Function<String, E2<Integer>> a2 = t ->/*[2]*/ new E2<>(t);\n");
		buf.append("    \n");
		buf.append("    Function<Integer, Float[]> a3 = t ->/*[3]*/ new Float[t];\n");
		buf.append("}\n");
		String expected1= buf.toString();
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		offset= buf1.toString().indexOf("->/*[2]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.Function;\n");
		buf.append("public class E2<T> {\n");
		buf.append("    public <A> E2(A a) {}\n");
		buf.append("    public E2(String s) {}\n");
		buf.append("    \n");
		buf.append("    Function<T, E2<Integer>> a1 = t ->/*[1]*/ (new <T>E2<Integer>(t));\n");
		buf.append("    Function<String, E2<Integer>> a2 = E2::new;\n");
		buf.append("    \n");
		buf.append("    Function<Integer, Float[]> a3 = t ->/*[3]*/ new Float[t];\n");
		buf.append("}\n");
		expected1= buf.toString();
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		offset= buf1.toString().indexOf("->/*[3]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.Function;\n");
		buf.append("public class E2<T> {\n");
		buf.append("    public <A> E2(A a) {}\n");
		buf.append("    public E2(String s) {}\n");
		buf.append("    \n");
		buf.append("    Function<T, E2<Integer>> a1 = t ->/*[1]*/ (new <T>E2<Integer>(t));\n");
		buf.append("    Function<String, E2<Integer>> a2 = t ->/*[2]*/ new E2<>(t);\n");
		buf.append("    \n");
		buf.append("    Function<Integer, Float[]> a3 = Float[]::new;\n");
		buf.append("}\n");
		expected1= buf.toString();
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertLambdaToMethodReference3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf1= new StringBuilder();
		buf1.append("package test1;\n");
		buf1.append("import java.util.function.Function;\n");
		buf1.append("public class E3<T> extends SuperE3<Number> {\n");
		buf1.append("    Function<Integer, String> a1 = t -> /*[1]*/super.method1(t);\n");
		buf1.append("    Function<Integer, String> a2 = t -> /*[2]*/E3.super.method1(t);\n");
		buf1.append("    Function<Integer, String> a3 = t -> /*[3]*/super.<Float>staticMethod1(t);\n");
		buf1.append("\n");
		buf1.append("    Function<Integer, String> s1 = t -> /*[4]*/(new E3()).staticMethod1(t);\n");
		buf1.append("    Function<Integer, String> s2 = t -> /*[5]*/staticMethod1(t);\n");
		buf1.append("\n");
		buf1.append("    Function<Integer, String> b1 = t -> /*[6]*/method1(t);\n");
		buf1.append("    Function<Integer, String> b2 = t -> /*[7]*/this.method1(t);\n");
		buf1.append("    Function<Integer, String> b3 = t -> /*[8]*/(new SuperE3<String>()).method1(t);\n");
		buf1.append("\n");
		buf1.append("    Function<E3<Integer>, String> p1 = t -> /*[9]*/t.<Float>method2();\n");
		buf1.append("    Function<E3, String> p2 = t -> /*[10]*/t.method2();\n");
		buf1.append("\n");
		buf1.append("    <V> String method2() { return \"m2\";    }\n");
		buf1.append("}\n");
		buf1.append("class SuperE3<S> {\n");
		buf1.append("    String method1(int i) { return \"m1\"; }\n");
		buf1.append("    static <V> String staticMethod1(int i) { return \"s\"; }\n");
		buf1.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E3.java", buf1.toString(), false, null);

		int offset= buf1.toString().indexOf("-> /*[1]*/");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.Function;\n");
		buf.append("public class E3<T> extends SuperE3<Number> {\n");
		buf.append("    Function<Integer, String> a1 = super::method1;\n");
		buf.append("    Function<Integer, String> a2 = t -> /*[2]*/E3.super.method1(t);\n");
		buf.append("    Function<Integer, String> a3 = t -> /*[3]*/super.<Float>staticMethod1(t);\n");
		buf.append("\n");
		buf.append("    Function<Integer, String> s1 = t -> /*[4]*/(new E3()).staticMethod1(t);\n");
		buf.append("    Function<Integer, String> s2 = t -> /*[5]*/staticMethod1(t);\n");
		buf.append("\n");
		buf.append("    Function<Integer, String> b1 = t -> /*[6]*/method1(t);\n");
		buf.append("    Function<Integer, String> b2 = t -> /*[7]*/this.method1(t);\n");
		buf.append("    Function<Integer, String> b3 = t -> /*[8]*/(new SuperE3<String>()).method1(t);\n");
		buf.append("\n");
		buf.append("    Function<E3<Integer>, String> p1 = t -> /*[9]*/t.<Float>method2();\n");
		buf.append("    Function<E3, String> p2 = t -> /*[10]*/t.method2();\n");
		buf.append("\n");
		buf.append("    <V> String method2() { return \"m2\";    }\n");
		buf.append("}\n");
		buf.append("class SuperE3<S> {\n");
		buf.append("    String method1(int i) { return \"m1\"; }\n");
		buf.append("    static <V> String staticMethod1(int i) { return \"s\"; }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		offset= buf1.toString().indexOf("-> /*[2]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.Function;\n");
		buf.append("public class E3<T> extends SuperE3<Number> {\n");
		buf.append("    Function<Integer, String> a1 = t -> /*[1]*/super.method1(t);\n");
		buf.append("    Function<Integer, String> a2 = E3.super::method1;\n");
		buf.append("    Function<Integer, String> a3 = t -> /*[3]*/super.<Float>staticMethod1(t);\n");
		buf.append("\n");
		buf.append("    Function<Integer, String> s1 = t -> /*[4]*/(new E3()).staticMethod1(t);\n");
		buf.append("    Function<Integer, String> s2 = t -> /*[5]*/staticMethod1(t);\n");
		buf.append("\n");
		buf.append("    Function<Integer, String> b1 = t -> /*[6]*/method1(t);\n");
		buf.append("    Function<Integer, String> b2 = t -> /*[7]*/this.method1(t);\n");
		buf.append("    Function<Integer, String> b3 = t -> /*[8]*/(new SuperE3<String>()).method1(t);\n");
		buf.append("\n");
		buf.append("    Function<E3<Integer>, String> p1 = t -> /*[9]*/t.<Float>method2();\n");
		buf.append("    Function<E3, String> p2 = t -> /*[10]*/t.method2();\n");
		buf.append("\n");
		buf.append("    <V> String method2() { return \"m2\";    }\n");
		buf.append("}\n");
		buf.append("class SuperE3<S> {\n");
		buf.append("    String method1(int i) { return \"m1\"; }\n");
		buf.append("    static <V> String staticMethod1(int i) { return \"s\"; }\n");
		buf.append("}\n");
		expected1= buf.toString();
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		offset= buf1.toString().indexOf("-> /*[3]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.Function;\n");
		buf.append("public class E3<T> extends SuperE3<Number> {\n");
		buf.append("    Function<Integer, String> a1 = t -> /*[1]*/super.method1(t);\n");
		buf.append("    Function<Integer, String> a2 = t -> /*[2]*/E3.super.method1(t);\n");
		buf.append("    Function<Integer, String> a3 = SuperE3::<Float>staticMethod1;\n");
		buf.append("\n");
		buf.append("    Function<Integer, String> s1 = t -> /*[4]*/(new E3()).staticMethod1(t);\n");
		buf.append("    Function<Integer, String> s2 = t -> /*[5]*/staticMethod1(t);\n");
		buf.append("\n");
		buf.append("    Function<Integer, String> b1 = t -> /*[6]*/method1(t);\n");
		buf.append("    Function<Integer, String> b2 = t -> /*[7]*/this.method1(t);\n");
		buf.append("    Function<Integer, String> b3 = t -> /*[8]*/(new SuperE3<String>()).method1(t);\n");
		buf.append("\n");
		buf.append("    Function<E3<Integer>, String> p1 = t -> /*[9]*/t.<Float>method2();\n");
		buf.append("    Function<E3, String> p2 = t -> /*[10]*/t.method2();\n");
		buf.append("\n");
		buf.append("    <V> String method2() { return \"m2\";    }\n");
		buf.append("}\n");
		buf.append("class SuperE3<S> {\n");
		buf.append("    String method1(int i) { return \"m1\"; }\n");
		buf.append("    static <V> String staticMethod1(int i) { return \"s\"; }\n");
		buf.append("}\n");
		expected1= buf.toString();
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		offset= buf1.toString().indexOf("-> /*[4]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.Function;\n");
		buf.append("public class E3<T> extends SuperE3<Number> {\n");
		buf.append("    Function<Integer, String> a1 = t -> /*[1]*/super.method1(t);\n");
		buf.append("    Function<Integer, String> a2 = t -> /*[2]*/E3.super.method1(t);\n");
		buf.append("    Function<Integer, String> a3 = t -> /*[3]*/super.<Float>staticMethod1(t);\n");
		buf.append("\n");
		buf.append("    Function<Integer, String> s1 = E3::staticMethod1;\n");
		buf.append("    Function<Integer, String> s2 = t -> /*[5]*/staticMethod1(t);\n");
		buf.append("\n");
		buf.append("    Function<Integer, String> b1 = t -> /*[6]*/method1(t);\n");
		buf.append("    Function<Integer, String> b2 = t -> /*[7]*/this.method1(t);\n");
		buf.append("    Function<Integer, String> b3 = t -> /*[8]*/(new SuperE3<String>()).method1(t);\n");
		buf.append("\n");
		buf.append("    Function<E3<Integer>, String> p1 = t -> /*[9]*/t.<Float>method2();\n");
		buf.append("    Function<E3, String> p2 = t -> /*[10]*/t.method2();\n");
		buf.append("\n");
		buf.append("    <V> String method2() { return \"m2\";    }\n");
		buf.append("}\n");
		buf.append("class SuperE3<S> {\n");
		buf.append("    String method1(int i) { return \"m1\"; }\n");
		buf.append("    static <V> String staticMethod1(int i) { return \"s\"; }\n");
		buf.append("}\n");
		expected1= buf.toString();
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		offset= buf1.toString().indexOf("-> /*[5]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.Function;\n");
		buf.append("public class E3<T> extends SuperE3<Number> {\n");
		buf.append("    Function<Integer, String> a1 = t -> /*[1]*/super.method1(t);\n");
		buf.append("    Function<Integer, String> a2 = t -> /*[2]*/E3.super.method1(t);\n");
		buf.append("    Function<Integer, String> a3 = t -> /*[3]*/super.<Float>staticMethod1(t);\n");
		buf.append("\n");
		buf.append("    Function<Integer, String> s1 = t -> /*[4]*/(new E3()).staticMethod1(t);\n");
		buf.append("    Function<Integer, String> s2 = E3::staticMethod1;\n");
		buf.append("\n");
		buf.append("    Function<Integer, String> b1 = t -> /*[6]*/method1(t);\n");
		buf.append("    Function<Integer, String> b2 = t -> /*[7]*/this.method1(t);\n");
		buf.append("    Function<Integer, String> b3 = t -> /*[8]*/(new SuperE3<String>()).method1(t);\n");
		buf.append("\n");
		buf.append("    Function<E3<Integer>, String> p1 = t -> /*[9]*/t.<Float>method2();\n");
		buf.append("    Function<E3, String> p2 = t -> /*[10]*/t.method2();\n");
		buf.append("\n");
		buf.append("    <V> String method2() { return \"m2\";    }\n");
		buf.append("}\n");
		buf.append("class SuperE3<S> {\n");
		buf.append("    String method1(int i) { return \"m1\"; }\n");
		buf.append("    static <V> String staticMethod1(int i) { return \"s\"; }\n");
		buf.append("}\n");
		expected1= buf.toString();
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		offset= buf1.toString().indexOf("-> /*[6]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.Function;\n");
		buf.append("public class E3<T> extends SuperE3<Number> {\n");
		buf.append("    Function<Integer, String> a1 = t -> /*[1]*/super.method1(t);\n");
		buf.append("    Function<Integer, String> a2 = t -> /*[2]*/E3.super.method1(t);\n");
		buf.append("    Function<Integer, String> a3 = t -> /*[3]*/super.<Float>staticMethod1(t);\n");
		buf.append("\n");
		buf.append("    Function<Integer, String> s1 = t -> /*[4]*/(new E3()).staticMethod1(t);\n");
		buf.append("    Function<Integer, String> s2 = t -> /*[5]*/staticMethod1(t);\n");
		buf.append("\n");
		buf.append("    Function<Integer, String> b1 = this::method1;\n");
		buf.append("    Function<Integer, String> b2 = t -> /*[7]*/this.method1(t);\n");
		buf.append("    Function<Integer, String> b3 = t -> /*[8]*/(new SuperE3<String>()).method1(t);\n");
		buf.append("\n");
		buf.append("    Function<E3<Integer>, String> p1 = t -> /*[9]*/t.<Float>method2();\n");
		buf.append("    Function<E3, String> p2 = t -> /*[10]*/t.method2();\n");
		buf.append("\n");
		buf.append("    <V> String method2() { return \"m2\";    }\n");
		buf.append("}\n");
		buf.append("class SuperE3<S> {\n");
		buf.append("    String method1(int i) { return \"m1\"; }\n");
		buf.append("    static <V> String staticMethod1(int i) { return \"s\"; }\n");
		buf.append("}\n");
		expected1= buf.toString();
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		offset= buf1.toString().indexOf("-> /*[7]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.Function;\n");
		buf.append("public class E3<T> extends SuperE3<Number> {\n");
		buf.append("    Function<Integer, String> a1 = t -> /*[1]*/super.method1(t);\n");
		buf.append("    Function<Integer, String> a2 = t -> /*[2]*/E3.super.method1(t);\n");
		buf.append("    Function<Integer, String> a3 = t -> /*[3]*/super.<Float>staticMethod1(t);\n");
		buf.append("\n");
		buf.append("    Function<Integer, String> s1 = t -> /*[4]*/(new E3()).staticMethod1(t);\n");
		buf.append("    Function<Integer, String> s2 = t -> /*[5]*/staticMethod1(t);\n");
		buf.append("\n");
		buf.append("    Function<Integer, String> b1 = t -> /*[6]*/method1(t);\n");
		buf.append("    Function<Integer, String> b2 = this::method1;\n");
		buf.append("    Function<Integer, String> b3 = t -> /*[8]*/(new SuperE3<String>()).method1(t);\n");
		buf.append("\n");
		buf.append("    Function<E3<Integer>, String> p1 = t -> /*[9]*/t.<Float>method2();\n");
		buf.append("    Function<E3, String> p2 = t -> /*[10]*/t.method2();\n");
		buf.append("\n");
		buf.append("    <V> String method2() { return \"m2\";    }\n");
		buf.append("}\n");
		buf.append("class SuperE3<S> {\n");
		buf.append("    String method1(int i) { return \"m1\"; }\n");
		buf.append("    static <V> String staticMethod1(int i) { return \"s\"; }\n");
		buf.append("}\n");
		expected1= buf.toString();
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		offset= buf1.toString().indexOf("-> /*[8]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.Function;\n");
		buf.append("public class E3<T> extends SuperE3<Number> {\n");
		buf.append("    Function<Integer, String> a1 = t -> /*[1]*/super.method1(t);\n");
		buf.append("    Function<Integer, String> a2 = t -> /*[2]*/E3.super.method1(t);\n");
		buf.append("    Function<Integer, String> a3 = t -> /*[3]*/super.<Float>staticMethod1(t);\n");
		buf.append("\n");
		buf.append("    Function<Integer, String> s1 = t -> /*[4]*/(new E3()).staticMethod1(t);\n");
		buf.append("    Function<Integer, String> s2 = t -> /*[5]*/staticMethod1(t);\n");
		buf.append("\n");
		buf.append("    Function<Integer, String> b1 = t -> /*[6]*/method1(t);\n");
		buf.append("    Function<Integer, String> b2 = t -> /*[7]*/this.method1(t);\n");
		buf.append("    Function<Integer, String> b3 = (new SuperE3<String>())::method1;\n");
		buf.append("\n");
		buf.append("    Function<E3<Integer>, String> p1 = t -> /*[9]*/t.<Float>method2();\n");
		buf.append("    Function<E3, String> p2 = t -> /*[10]*/t.method2();\n");
		buf.append("\n");
		buf.append("    <V> String method2() { return \"m2\";    }\n");
		buf.append("}\n");
		buf.append("class SuperE3<S> {\n");
		buf.append("    String method1(int i) { return \"m1\"; }\n");
		buf.append("    static <V> String staticMethod1(int i) { return \"s\"; }\n");
		buf.append("}\n");
		expected1= buf.toString();
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		offset= buf1.toString().indexOf("-> /*[9]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.Function;\n");
		buf.append("public class E3<T> extends SuperE3<Number> {\n");
		buf.append("    Function<Integer, String> a1 = t -> /*[1]*/super.method1(t);\n");
		buf.append("    Function<Integer, String> a2 = t -> /*[2]*/E3.super.method1(t);\n");
		buf.append("    Function<Integer, String> a3 = t -> /*[3]*/super.<Float>staticMethod1(t);\n");
		buf.append("\n");
		buf.append("    Function<Integer, String> s1 = t -> /*[4]*/(new E3()).staticMethod1(t);\n");
		buf.append("    Function<Integer, String> s2 = t -> /*[5]*/staticMethod1(t);\n");
		buf.append("\n");
		buf.append("    Function<Integer, String> b1 = t -> /*[6]*/method1(t);\n");
		buf.append("    Function<Integer, String> b2 = t -> /*[7]*/this.method1(t);\n");
		buf.append("    Function<Integer, String> b3 = t -> /*[8]*/(new SuperE3<String>()).method1(t);\n");
		buf.append("\n");
		buf.append("    Function<E3<Integer>, String> p1 = E3<Integer>::<Float>method2;\n");
		buf.append("    Function<E3, String> p2 = t -> /*[10]*/t.method2();\n");
		buf.append("\n");
		buf.append("    <V> String method2() { return \"m2\";    }\n");
		buf.append("}\n");
		buf.append("class SuperE3<S> {\n");
		buf.append("    String method1(int i) { return \"m1\"; }\n");
		buf.append("    static <V> String staticMethod1(int i) { return \"s\"; }\n");
		buf.append("}\n");
		expected1= buf.toString();
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		String match10= "-> /*[10]*/t.method2";
		offset= buf1.toString().indexOf(match10) + match10.length();
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.Function;\n");
		buf.append("public class E3<T> extends SuperE3<Number> {\n");
		buf.append("    Function<Integer, String> a1 = t -> /*[1]*/super.method1(t);\n");
		buf.append("    Function<Integer, String> a2 = t -> /*[2]*/E3.super.method1(t);\n");
		buf.append("    Function<Integer, String> a3 = t -> /*[3]*/super.<Float>staticMethod1(t);\n");
		buf.append("\n");
		buf.append("    Function<Integer, String> s1 = t -> /*[4]*/(new E3()).staticMethod1(t);\n");
		buf.append("    Function<Integer, String> s2 = t -> /*[5]*/staticMethod1(t);\n");
		buf.append("\n");
		buf.append("    Function<Integer, String> b1 = t -> /*[6]*/method1(t);\n");
		buf.append("    Function<Integer, String> b2 = t -> /*[7]*/this.method1(t);\n");
		buf.append("    Function<Integer, String> b3 = t -> /*[8]*/(new SuperE3<String>()).method1(t);\n");
		buf.append("\n");
		buf.append("    Function<E3<Integer>, String> p1 = t -> /*[9]*/t.<Float>method2();\n");
		buf.append("    Function<E3, String> p2 = E3::method2;\n");
		buf.append("\n");
		buf.append("    <V> String method2() { return \"m2\";    }\n");
		buf.append("}\n");
		buf.append("class SuperE3<S> {\n");
		buf.append("    String method1(int i) { return \"m1\"; }\n");
		buf.append("    static <V> String staticMethod1(int i) { return \"s\"; }\n");
		buf.append("}\n");
		expected1= buf.toString();
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertLambdaToMethodReference4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf1= new StringBuilder();
		buf1.append("package test1;\n");
		buf1.append("import java.util.function.BiFunction;\n");
		buf1.append("import java.util.function.Function;\n");
		buf1.append("public class E4 {\n");
		buf1.append("    static String staticMethod1() {    return \"s\"; }\n");
		buf1.append("    Function<E4, String> s1 = t ->/*[1]*/ staticMethod1();\n");
		buf1.append("    \n");
		buf1.append("    int myVal= 0;\n");
		buf1.append("    String method1(int i) { return \"m1\"; }\n");
		buf1.append("    BiFunction<Float, Integer, String> p1 = (t, u) ->/*[2]*/ method1(u);\n");
		buf1.append("    BiFunction<SubE4, Integer, String> p2 = (t, u) ->/*[3]*/ method1(u);\n");
		buf1.append("    \n");
		buf1.append("    BiFunction<SubE4, Integer, String> p3 = (t, u) ->/*[4]*/ t.method1(u);\n");
		buf1.append("    BiFunction<E4, Integer, String> p4 = (t, u) ->/*[5]*/ t.method1(u);\n");
		buf1.append("    \n");
		buf1.append("    Function<int[], int[]> a1 = t ->/*[6]*/ t.clone();\n");
		buf1.append("}\n");
		buf1.append("class SubE4 extends E4 {}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E4.java", buf1.toString(), false, null);

		int offset= buf1.toString().indexOf("->/*[1]*/");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_method_reference);

		offset= buf1.toString().indexOf("->/*[2]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_method_reference);

		offset= buf1.toString().indexOf("->/*[3]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_method_reference);

		offset= buf1.toString().indexOf("->/*[4]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.BiFunction;\n");
		buf.append("import java.util.function.Function;\n");
		buf.append("public class E4 {\n");
		buf.append("    static String staticMethod1() {    return \"s\"; }\n");
		buf.append("    Function<E4, String> s1 = t ->/*[1]*/ staticMethod1();\n");
		buf.append("    \n");
		buf.append("    int myVal= 0;\n");
		buf.append("    String method1(int i) { return \"m1\"; }\n");
		buf.append("    BiFunction<Float, Integer, String> p1 = (t, u) ->/*[2]*/ method1(u);\n");
		buf.append("    BiFunction<SubE4, Integer, String> p2 = (t, u) ->/*[3]*/ method1(u);\n");
		buf.append("    \n");
		buf.append("    BiFunction<SubE4, Integer, String> p3 = SubE4::method1;\n");
		buf.append("    BiFunction<E4, Integer, String> p4 = (t, u) ->/*[5]*/ t.method1(u);\n");
		buf.append("    \n");
		buf.append("    Function<int[], int[]> a1 = t ->/*[6]*/ t.clone();\n");
		buf.append("}\n");
		buf.append("class SubE4 extends E4 {}\n");
		String expected1= buf.toString();
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		offset= buf1.toString().indexOf("->/*[5]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.BiFunction;\n");
		buf.append("import java.util.function.Function;\n");
		buf.append("public class E4 {\n");
		buf.append("    static String staticMethod1() {    return \"s\"; }\n");
		buf.append("    Function<E4, String> s1 = t ->/*[1]*/ staticMethod1();\n");
		buf.append("    \n");
		buf.append("    int myVal= 0;\n");
		buf.append("    String method1(int i) { return \"m1\"; }\n");
		buf.append("    BiFunction<Float, Integer, String> p1 = (t, u) ->/*[2]*/ method1(u);\n");
		buf.append("    BiFunction<SubE4, Integer, String> p2 = (t, u) ->/*[3]*/ method1(u);\n");
		buf.append("    \n");
		buf.append("    BiFunction<SubE4, Integer, String> p3 = (t, u) ->/*[4]*/ t.method1(u);\n");
		buf.append("    BiFunction<E4, Integer, String> p4 = E4::method1;\n");
		buf.append("    \n");
		buf.append("    Function<int[], int[]> a1 = t ->/*[6]*/ t.clone();\n");
		buf.append("}\n");
		buf.append("class SubE4 extends E4 {}\n");
		expected1= buf.toString();
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		offset= buf1.toString().indexOf("->/*[6]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.BiFunction;\n");
		buf.append("import java.util.function.Function;\n");
		buf.append("public class E4 {\n");
		buf.append("    static String staticMethod1() {    return \"s\"; }\n");
		buf.append("    Function<E4, String> s1 = t ->/*[1]*/ staticMethod1();\n");
		buf.append("    \n");
		buf.append("    int myVal= 0;\n");
		buf.append("    String method1(int i) { return \"m1\"; }\n");
		buf.append("    BiFunction<Float, Integer, String> p1 = (t, u) ->/*[2]*/ method1(u);\n");
		buf.append("    BiFunction<SubE4, Integer, String> p2 = (t, u) ->/*[3]*/ method1(u);\n");
		buf.append("    \n");
		buf.append("    BiFunction<SubE4, Integer, String> p3 = (t, u) ->/*[4]*/ t.method1(u);\n");
		buf.append("    BiFunction<E4, Integer, String> p4 = (t, u) ->/*[5]*/ t.method1(u);\n");
		buf.append("    \n");
		buf.append("    Function<int[], int[]> a1 = int[]::clone;\n");
		buf.append("}\n");
		buf.append("class SubE4 extends E4 {}\n");
		expected1= buf.toString();
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertLambdaToMethodReference5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf1= new StringBuilder();
		buf1.append("package test1;\n");
		buf1.append("public interface I {\n");
		buf1.append("    public default void i1() {\n");
		buf1.append("    }\n");
		buf1.append("    void i2();\n");
		buf1.append("    \n");
		buf1.append("    class E5 implements I {\n");
		buf1.append("        Thread o1 = new Thread(() ->/*[1]*/ i1());\n");
		buf1.append("        Thread o2 = new Thread(() ->/*[2]*/ i2());\n");
		buf1.append("        public void i2() {\n");
		buf1.append("        }\n");
		buf1.append("    }\n");
		buf1.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("I.java", buf1.toString(), false, null);


		int offset= buf1.toString().indexOf("->/*[1]*/");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public interface I {\n");
		buf.append("    public default void i1() {\n");
		buf.append("    }\n");
		buf.append("    void i2();\n");
		buf.append("    \n");
		buf.append("    class E5 implements I {\n");
		buf.append("        Thread o1 = new Thread(this::i1);\n");
		buf.append("        Thread o2 = new Thread(() ->/*[2]*/ i2());\n");
		buf.append("        public void i2() {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		offset= buf1.toString().indexOf("->/*[2]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public interface I {\n");
		buf.append("    public default void i1() {\n");
		buf.append("    }\n");
		buf.append("    void i2();\n");
		buf.append("    \n");
		buf.append("    class E5 implements I {\n");
		buf.append("        Thread o1 = new Thread(() ->/*[1]*/ i1());\n");
		buf.append("        Thread o2 = new Thread(this::i2);\n");
		buf.append("        public void i2() {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected1= buf.toString();
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testFixParenthesesInLambdaExpressionAdd() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf1= new StringBuilder();
		buf1.append("package test1;\n");
		buf1.append("import java.util.function.Consumer;\n");
		buf1.append("public class MyClass {\n");
		buf1.append("    public void foo() {\n");
		buf1.append("        Consumer<Integer> c = id -> {System.out.println(id);};\n");
		buf1.append("    }\n");
		buf1.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("MyClass.java", buf1.toString(), false, null);

		int offset= buf1.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 7);
		assertCorrectLabels(proposals);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.Consumer;\n");
		buf.append("public class MyClass {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Consumer<Integer> c = (id) -> {System.out.println(id);};\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testFixParenthesesInLambdaExpressionRemove() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf1= new StringBuilder();
		buf1.append("package test1;\n");
		buf1.append("import java.util.function.Consumer;\n");
		buf1.append("public class MyClass {\n");
		buf1.append("    public void foo() {\n");
		buf1.append("        Consumer<Integer> c = (id) -> {System.out.println(id);};\n");
		buf1.append("    }\n");
		buf1.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("MyClass.java", buf1.toString(), false, null);

		int offset= buf1.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 7);
		assertCorrectLabels(proposals);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.Consumer;\n");
		buf.append("public class MyClass {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Consumer<Integer> c = id -> {System.out.println(id);};\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testFixParenthesesInLambdaExpressionCannotRemove1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class MyClass {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Runnable r = () -> System.out.println(\"Hello world!\");\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("MyClass.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_removeParenthesesInLambda);
	}

	@Test
	public void testFixParenthesesInLambdaExpressionCannotRemove2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Comparator;\n");
		buf.append("public class MyClass {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Comparator<String> c = (String s1, String s2) -> {return s1.length() - s2.length();};\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("MyClass.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_removeParenthesesInLambda);
	}

	@Test
	public void testFixParenthesesInLambdaExpressionCannotRemove3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf1= new StringBuilder();
		buf1.append("package test1;\n");
		buf1.append("import java.util.function.Consumer;\n");
		buf1.append("public class MyClass {\n");
		buf1.append("    public void foo() {\n");
		buf1.append("        Consumer<Integer> c = (Integer id) -> {System.out.println(id);};\n");
		buf1.append("    }\n");
		buf1.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("MyClass.java", buf1.toString(), false, null);

		int offset= buf1.toString().indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 6);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_removeParenthesesInLambda);
	}

	@Test
	public void testBug514203_wildCard() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Comparator;\n");
		buf.append("import java.util.List;\n");
		buf.append("\n");
		buf.append("public class Lambda1 {\n");
		buf.append("	Comparator<List<?>> c = (l1, l2) -> Integer.compare(l1.size(), l2.size());\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Lambda1.java", buf.toString(), false, null);

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf("->"), 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);


		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Comparator;\n");
		buf.append("import java.util.List;\n");
		buf.append("\n");
		buf.append("public class Lambda1 {\n");
		buf.append("	Comparator<List<?>> c = (List<?> l1, List<?> l2) -> Integer.compare(l1.size(), l2.size());\n");
		buf.append("}\n");
		assertProposalPreviewEquals(buf.toString(), "Add inferred lambda parameter types", proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Comparator;\n");
		buf.append("import java.util.List;\n");
		buf.append("\n");
		buf.append("public class Lambda1 {\n");
		buf.append("	Comparator<List<?>> c = new Comparator<List<?>>() {\n");
		buf.append("        @Override\n");
		buf.append("        public int compare(List<?> l1, List<?> l2) {\n");
		buf.append("            return Integer.compare(l1.size(), l2.size());\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("}\n");
		assertProposalPreviewEquals(buf.toString(), "Convert to anonymous class creation", proposals);

	}

	@Test
	public void testBug514203_capture1() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		JavaCore.setOptions(options);
		JavaProjectHelper.addLibrary(fJProject1, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		StringBuilder buf;

		buf= new StringBuilder();
		buf.append("@org.eclipse.jdt.annotation.NonNullByDefault\n");
		buf.append("package test1;\n");
		pack1.createCompilationUnit("package-info.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class Lambda2 {\n");
		buf.append("	interface Sink<T> {\n");
		buf.append("		void receive(T t);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("	interface Source<U> {\n");
		buf.append("		void sendTo(Sink<? super U> c);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("	void f(Source<? extends Number> source) {\n");
		buf.append("		source.sendTo(a -> a.doubleValue());\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Lambda2.java", buf.toString(), false, null);

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf("->"), 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class Lambda2 {\n");
		buf.append("	interface Sink<T> {\n");
		buf.append("		void receive(T t);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("	interface Source<U> {\n");
		buf.append("		void sendTo(Sink<? super U> c);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("	void f(Source<? extends Number> source) {\n");
		buf.append("		source.sendTo(Number::doubleValue);\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertProposalPreviewEquals(buf.toString(), "Convert to method reference", proposals);


		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class Lambda2 {\n");
		buf.append("	interface Sink<T> {\n");
		buf.append("		void receive(T t);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("	interface Source<U> {\n");
		buf.append("		void sendTo(Sink<? super U> c);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("	void f(Source<? extends Number> source) {\n");
		buf.append("		source.sendTo((Number a) -> a.doubleValue());\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertProposalPreviewEquals(buf.toString(), "Add inferred lambda parameter types", proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class Lambda2 {\n");
		buf.append("	interface Sink<T> {\n");
		buf.append("		void receive(T t);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("	interface Source<U> {\n");
		buf.append("		void sendTo(Sink<? super U> c);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("	void f(Source<? extends Number> source) {\n");
		buf.append("		source.sendTo(new Sink<Number>() {\n");
		buf.append("            @Override\n");
		buf.append("            public void receive(Number a) {\n");
		buf.append("                a.doubleValue();\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertProposalPreviewEquals(buf.toString(), "Convert to anonymous class creation", proposals);

	}

	@Test
	public void testBug514203_capture2() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		JavaCore.setOptions(options);
		JavaProjectHelper.addLibrary(fJProject1, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		StringBuilder buf;

		buf= new StringBuilder();
		buf.append("@org.eclipse.jdt.annotation.NonNullByDefault\n");
		buf.append("package test1;\n");
		pack1.createCompilationUnit("package-info.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.math.BigDecimal;\n");
		buf.append("\n");
		buf.append("public class Lambda3 {\n");
		buf.append("	interface Sink<T extends Number> {\n");
		buf.append("		void receive(T t);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("	interface Source<U extends BigDecimal> {\n");
		buf.append("		void sendTo(Sink<? super U> c);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("	void f(Source<?> source) {\n");
		buf.append("		source.sendTo(a -> a.scale());\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Lambda3.java", buf.toString(), false, null);

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf("->"), 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.math.BigDecimal;\n");
		buf.append("\n");
		buf.append("public class Lambda3 {\n");
		buf.append("	interface Sink<T extends Number> {\n");
		buf.append("		void receive(T t);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("	interface Source<U extends BigDecimal> {\n");
		buf.append("		void sendTo(Sink<? super U> c);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("	void f(Source<?> source) {\n");
		buf.append("		source.sendTo(BigDecimal::scale);\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertProposalPreviewEquals(buf.toString(), "Convert to method reference", proposals);


		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.math.BigDecimal;\n");
		buf.append("\n");
		buf.append("public class Lambda3 {\n");
		buf.append("	interface Sink<T extends Number> {\n");
		buf.append("		void receive(T t);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("	interface Source<U extends BigDecimal> {\n");
		buf.append("		void sendTo(Sink<? super U> c);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("	void f(Source<?> source) {\n");
		buf.append("		source.sendTo((BigDecimal a) -> a.scale());\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertProposalPreviewEquals(buf.toString(), "Add inferred lambda parameter types", proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.math.BigDecimal;\n");
		buf.append("\n");
		buf.append("public class Lambda3 {\n");
		buf.append("	interface Sink<T extends Number> {\n");
		buf.append("		void receive(T t);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("	interface Source<U extends BigDecimal> {\n");
		buf.append("		void sendTo(Sink<? super U> c);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("	void f(Source<?> source) {\n");
		buf.append("		source.sendTo(new Sink<BigDecimal>() {\n");
		buf.append("            @Override\n");
		buf.append("            public void receive(BigDecimal a) {\n");
		buf.append("                a.scale();\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertProposalPreviewEquals(buf.toString(), "Convert to anonymous class creation", proposals);

	}

	@Test
	public void testBug514203_capture3() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		JavaCore.setOptions(options);
		JavaProjectHelper.addLibrary(fJProject1, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		StringBuilder buf;

		buf= new StringBuilder();
		buf.append("@org.eclipse.jdt.annotation.NonNullByDefault\n");
		buf.append("package test1;\n");
		pack1.createCompilationUnit("package-info.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.List;\n");
		buf.append("\n");
		buf.append("public class Lambda4 {\n");
		buf.append("	interface Sink<T extends List<Number>> {\n");
		buf.append("		void receive(T t);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("	interface Source<U extends ArrayList<Number>> {\n");
		buf.append("		void sendTo(Sink<? super U> c);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("	void f(Source<?> source) {\n");
		buf.append("		source.sendTo(a -> a.size());\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Lambda4.java", buf.toString(), false, null);

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf("->"), 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.List;\n");
		buf.append("\n");
		buf.append("public class Lambda4 {\n");
		buf.append("	interface Sink<T extends List<Number>> {\n");
		buf.append("		void receive(T t);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("	interface Source<U extends ArrayList<Number>> {\n");
		buf.append("		void sendTo(Sink<? super U> c);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("	void f(Source<?> source) {\n");
		buf.append("		source.sendTo(ArrayList<Number>::size);\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertProposalPreviewEquals(buf.toString(), "Convert to method reference", proposals);


		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.List;\n");
		buf.append("\n");
		buf.append("public class Lambda4 {\n");
		buf.append("	interface Sink<T extends List<Number>> {\n");
		buf.append("		void receive(T t);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("	interface Source<U extends ArrayList<Number>> {\n");
		buf.append("		void sendTo(Sink<? super U> c);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("	void f(Source<?> source) {\n");
		buf.append("		source.sendTo((ArrayList<Number> a) -> a.size());\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertProposalPreviewEquals(buf.toString(), "Add inferred lambda parameter types", proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.List;\n");
		buf.append("\n");
		buf.append("public class Lambda4 {\n");
		buf.append("	interface Sink<T extends List<Number>> {\n");
		buf.append("		void receive(T t);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("	interface Source<U extends ArrayList<Number>> {\n");
		buf.append("		void sendTo(Sink<? super U> c);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("	void f(Source<?> source) {\n");
		buf.append("		source.sendTo(new Sink<ArrayList<Number>>() {\n");
		buf.append("            @Override\n");
		buf.append("            public void receive(ArrayList<Number> a) {\n");
		buf.append("                a.size();\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertProposalPreviewEquals(buf.toString(), "Convert to anonymous class creation", proposals);

	}

	@Test
	public void testBug514203_lambdaNN() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		JavaCore.setOptions(options);
		JavaProjectHelper.addLibrary(fJProject1, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		StringBuilder buf;

		// --- Set up @NonNullByDefault for the package, including ARRAY_CONTENTS --

		buf= new StringBuilder();
		buf.append("@NonNullByDefault({ PARAMETER, RETURN_TYPE, FIELD, TYPE_BOUND, TYPE_ARGUMENT, ARRAY_CONTENTS })\n");
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import static org.eclipse.jdt.annotation.DefaultLocation.*;\n");
		buf.append("import org.eclipse.jdt.annotation.NonNullByDefault;\n");
		pack1.createCompilationUnit("package-info.java", buf.toString(), false, null);

		// --- Classes that are only referenced --

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("@interface X {}\n");
		pack1.createCompilationUnit("X.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("@interface Y {}\n");
		pack1.createCompilationUnit("Y.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("@interface Z {}\n");
		pack1.createCompilationUnit("Z.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("class Ref<A> {}\n");
		pack1.createCompilationUnit("Ref.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface SAM<A> {\n");
		buf.append("	void f(A[] a);\n");
		buf.append("}\n");
		pack1.createCompilationUnit("SAM.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class Test {\n");
		buf.append("	static int nn(Object o) {\n");
		buf.append("		return 0;\n");
		buf.append("	}\n");
		buf.append("}\n");
		buf.append("");
		pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		// --- Classes in which the quick assists are checked (without and with NonNullByDefault in effect at the target location) ---

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("public class LambdaNN1 {\n");
		buf.append("	void g(Ref<? extends Ref<@X @Nullable String @Y [] @Z []>>[] data) {\n");
		buf.append("		@NonNullByDefault({})\n");
		buf.append("		SAM<? super @NonNull Ref<? extends @NonNull @Y Ref<@X @Nullable String @Y @NonNull [] @Z @NonNull []>>> sam0 = a0 -> Test.nn(a0);\n");
		buf.append("		sam0.f(data);\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("LambdaNN1.java", buf.toString(), false, null);

		AssistContext context1= getCorrectionContext(cu1, buf.toString().indexOf("->"), 0);
		assertNoErrors(context1);
		List<IJavaCompletionProposal> proposals1= collectAssists(context1, false);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("public class LambdaNN2 {\n");
		buf.append("	void g(Ref<? extends Ref<@X @Nullable String @Y [] @Z []>>[] data) {\n");
		buf.append("		SAM<? super @NonNull Ref<? extends @NonNull @Y Ref<@X @Nullable String @Y @NonNull [] @Z @NonNull []>>> sam0 = a0 -> Test.nn(a0);\n");
		buf.append("		sam0.f(data);\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("LambdaNN2.java", buf.toString(), false, null);

		AssistContext context2= getCorrectionContext(cu2, buf.toString().indexOf("->"), 0);
		assertNoErrors(context2);

		List<IJavaCompletionProposal> proposals2= collectAssists(context2, false);

		// --- Convert to method reference without and with NNBD ---

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("public class LambdaNN1 {\n");
		buf.append("	void g(Ref<? extends Ref<@X @Nullable String @Y [] @Z []>>[] data) {\n");
		buf.append("		@NonNullByDefault({})\n");
		buf.append("		SAM<? super @NonNull Ref<? extends @NonNull @Y Ref<@X @Nullable String @Y @NonNull [] @Z @NonNull []>>> sam0 = Test::nn;\n");
		buf.append("		sam0.f(data);\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertProposalPreviewEquals(buf.toString(), "Convert to method reference", proposals1);


		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("public class LambdaNN2 {\n");
		buf.append("	void g(Ref<? extends Ref<@X @Nullable String @Y [] @Z []>>[] data) {\n");
		buf.append("		SAM<? super @NonNull Ref<? extends @NonNull @Y Ref<@X @Nullable String @Y @NonNull [] @Z @NonNull []>>> sam0 = Test::nn;\n");
		buf.append("		sam0.f(data);\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertProposalPreviewEquals(buf.toString(), "Convert to method reference", proposals2);

		// --- Add inferred lambda parameter types without and with NNBD ---

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("public class LambdaNN1 {\n");
		buf.append("	void g(Ref<? extends Ref<@X @Nullable String @Y [] @Z []>>[] data) {\n");
		buf.append("		@NonNullByDefault({})\n");
		buf.append("		SAM<? super @NonNull Ref<? extends @NonNull @Y Ref<@X @Nullable String @Y @NonNull [] @Z @NonNull []>>> sam0 = (@NonNull Ref<? extends @NonNull @Y Ref<@X @Nullable String @Y @NonNull [] @Z @NonNull []>> @NonNull [] a0) -> Test.nn(a0);\n");
		buf.append("		sam0.f(data);\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertProposalPreviewEquals(buf.toString(), "Add inferred lambda parameter types", proposals1);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("public class LambdaNN2 {\n");
		buf.append("	void g(Ref<? extends Ref<@X @Nullable String @Y [] @Z []>>[] data) {\n");
		buf.append("		SAM<? super @NonNull Ref<? extends @NonNull @Y Ref<@X @Nullable String @Y @NonNull [] @Z @NonNull []>>> sam0 = (Ref<? extends @Y Ref<@X @Nullable String @Y [] @Z []>>[] a0) -> Test.nn(a0);\n");
		buf.append("		sam0.f(data);\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertProposalPreviewEquals(buf.toString(), "Add inferred lambda parameter types", proposals2);

		// --- Convert to anonymous class creation without and with NNBD --
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("public class LambdaNN1 {\n");
		buf.append("	void g(Ref<? extends Ref<@X @Nullable String @Y [] @Z []>>[] data) {\n");
		buf.append("		@NonNullByDefault({})\n");
		buf.append("		SAM<? super @NonNull Ref<? extends @NonNull @Y Ref<@X @Nullable String @Y @NonNull [] @Z @NonNull []>>> sam0 = new SAM<@NonNull Ref<? extends @NonNull @Y Ref<@X @Nullable String @Y @NonNull [] @Z @NonNull []>>>() {\n");
		buf.append("            @Override\n");
		buf.append("            public void f(\n");
		buf.append("                    @NonNull Ref<? extends @NonNull @Y Ref<@X @Nullable String @Y @NonNull [] @Z @NonNull []>> @NonNull [] a0) {\n");
		buf.append("                Test.nn(a0);\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("		sam0.f(data);\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertProposalPreviewEquals(buf.toString(), "Convert to anonymous class creation", proposals1);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("public class LambdaNN2 {\n");
		buf.append("	void g(Ref<? extends Ref<@X @Nullable String @Y [] @Z []>>[] data) {\n");
		buf.append("		SAM<? super @NonNull Ref<? extends @NonNull @Y Ref<@X @Nullable String @Y @NonNull [] @Z @NonNull []>>> sam0 = new SAM<Ref<? extends @Y Ref<@X @Nullable String @Y [] @Z []>>>() {\n");
		buf.append("            @Override\n");
		buf.append("            public void f(\n");
		buf.append("                    Ref<? extends @Y Ref<@X @Nullable String @Y [] @Z []>>[] a0) {\n");
		buf.append("                Test.nn(a0);\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("		sam0.f(data);\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertProposalPreviewEquals(buf.toString(), "Convert to anonymous class creation", proposals2);
	}
	@Test
	public void testBug514203_annotatedParametrizedType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class Example {\n");
		buf.append("	@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE)\n");
		buf.append("	public @interface X {}\n");
		buf.append("\n");
		buf.append("	interface SAM<T> {\n");
		buf.append("		T f(T t);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("	@X\n");
		buf.append("	SAM<String> c = a -> a;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Example.java", buf.toString(), false, null);

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf("->"), 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class Example {\n");
		buf.append("	@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE)\n");
		buf.append("	public @interface X {}\n");
		buf.append("\n");
		buf.append("	interface SAM<T> {\n");
		buf.append("		T f(T t);\n");
		buf.append("	}\n");
		buf.append("\n");
		buf.append("	@X\n");
		buf.append("	SAM<String> c = new @X SAM<String>() {\n");
		buf.append("        @Override\n");
		buf.append("        public String f(String a) {\n");
		buf.append("            return a;\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("}\n");
		assertProposalPreviewEquals(buf.toString(), "Convert to anonymous class creation", proposals);
	}

	@Test
	public void testNoRedundantNonNullInConvertArrayForLoop() throws Exception {
		NullTestUtils.prepareNullTypeAnnotations(fSourceFolder);
		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			StringBuilder buf= new StringBuilder();
			buf.append("package test1;\n");
			buf.append("import annots.*;\n");
			buf.append("public class A {\n");
			buf.append("    public void foo(@NonNull String[] array) {\n");
			buf.append("		for (int i = 0; i < array.length; i++){\n");
			buf.append("			System.out.println(array[i]);\n");
			buf.append("		}\n");
			buf.append("    }\n");
			buf.append("}\n");
			ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

	        AssistContext context= getCorrectionContext(cu, buf.toString().indexOf("for"), 0);
			ArrayList<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertCorrectLabels(proposals);
			assertNumberOfProposals(proposals, 3);

			buf= new StringBuilder();
			buf.append("package test1;\n");
			buf.append("import annots.*;\n");
			buf.append("public class A {\n");
			buf.append("    public void foo(@NonNull String[] array) {\n");
			buf.append("		for (String element : array) {\n");
			buf.append("			System.out.println(element);\n");
			buf.append("		}\n");
			buf.append("    }\n");
			buf.append("}\n");
			assertProposalPreviewEquals(buf.toString(), "Convert to enhanced 'for' loop", proposals);
		} finally {
			NullTestUtils.disableAnnotationBasedNullAnalysis(fSourceFolder);
		}
	}
	@Test
	public void testNoRedundantNonNullInConvertIterableForLoop() throws Exception {
		NullTestUtils.prepareNullTypeAnnotations(fSourceFolder);
		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			StringBuilder buf= new StringBuilder();
			buf.append("package test1;\n");
			buf.append("import java.util.Iterator;\n");
			buf.append("import annots.*;\n");
			buf.append("@NonNullByDefault\n");
			buf.append("public class A {\n");
			buf.append("    public void foo(Iterable<String> x) {\n");
			buf.append("		for (Iterator<String> iterator = x.iterator(); iterator.hasNext();){\n");
			buf.append("			System.out.println(iterator.next());\n");
			buf.append("		}\n");
			buf.append("    }\n");
			buf.append("}\n");
			ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

	        AssistContext context= getCorrectionContext(cu, buf.toString().indexOf("for"), 0);
			ArrayList<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertCorrectLabels(proposals);
			assertNumberOfProposals(proposals, 3);

			buf= new StringBuilder();
			buf.append("package test1;\n");
			buf.append("import annots.*;\n");
			buf.append("@NonNullByDefault\n");
			buf.append("public class A {\n");
			buf.append("    public void foo(Iterable<String> x) {\n");
			buf.append("		for (String string : x) {\n");
			buf.append("			System.out.println(string);\n");
			buf.append("		}\n");
			buf.append("    }\n");
			buf.append("}\n");
			assertProposalPreviewEquals(buf.toString(), "Convert to enhanced 'for' loop", proposals);
		} finally {
			NullTestUtils.disableAnnotationBasedNullAnalysis(fSourceFolder);
		}
	}

	@Test
	public void testSurroundWithTryWithResource_01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		StringBuilder bufOrg= new StringBuilder();
		bufOrg.append("package p;\n");
		bufOrg.append("\n");
		bufOrg.append("import java.io.IOException;\n");
		bufOrg.append("import java.io.InputStream;\n");
		bufOrg.append("import java.net.Socket;\n");
		bufOrg.append("\n");
		bufOrg.append("public class E {\n");
		bufOrg.append("    public void foo() throws IOException {\n");
		bufOrg.append("        /*1*/Socket s = new Socket(), s2 = new Socket();\n");
		bufOrg.append("        /*2*/InputStream is = s.getInputStream();\n");
		bufOrg.append("        /*3*/int i = 0;\n");
		bufOrg.append("        System.out.println(s.getInetAddress().toString());\n");
		bufOrg.append("        System.out.println(is.markSupported());/*0*/\n");
		bufOrg.append("    }\n");
		bufOrg.append("}\n");
		bufOrg.append("\n");

		ICompilationUnit cu= pack1.createCompilationUnit("E.java", bufOrg.toString(), false, null);
		String strEnd= "/*0*/";

		String str1= "/*1*/";
		AssistContext context= getCorrectionContext(cu, bufOrg.toString().indexOf(str1), bufOrg.toString().indexOf(strEnd) - bufOrg.toString().indexOf(str1));
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) findProposalByName("Surround with try-with-resources", proposals);
		String preview1= getPreviewContent(proposal);

		StringBuilder buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.io.InputStream;\n");
		buf.append("import java.net.Socket;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() throws IOException {\n");
		buf.append("        try (/*1*/Socket s = new Socket();\n");
		buf.append("                Socket s2 = new Socket();\n");
		buf.append("                /*2*/InputStream is = s.getInputStream()) {\n");
		buf.append("            /*3*/int i = 0;\n");
		buf.append("            System.out.println(s.getInetAddress().toString());\n");
		buf.append("            System.out.println(is.markSupported());/*0*/\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		String expected1= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });

		str1= "/*2*/";
		context= getCorrectionContext(cu, bufOrg.toString().indexOf(str1), bufOrg.toString().indexOf(strEnd) - bufOrg.toString().indexOf(str1));
		proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		proposal= (CUCorrectionProposal) findProposalByName("Surround with try-with-resources", proposals);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.io.InputStream;\n");
		buf.append("import java.net.Socket;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() throws IOException {\n");
		buf.append("        /*1*/Socket s = new Socket(), s2 = new Socket();\n");
		buf.append("        try (/*2*/InputStream is = s.getInputStream()) {\n");
		buf.append("            /*3*/int i = 0;\n");
		buf.append("            System.out.println(s.getInetAddress().toString());\n");
		buf.append("            System.out.println(is.markSupported());/*0*/\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview2 }, new String[] { expected2 });

		str1= "/*3*/";
		context= getCorrectionContext(cu, bufOrg.toString().indexOf(str1), bufOrg.toString().indexOf(strEnd) - bufOrg.toString().indexOf(str1));
		proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
	}
	@Test
	public void testSurroundWithTryWithResource_02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		StringBuilder bufOrg= new StringBuilder();
		bufOrg.append("package p;\n");
		bufOrg.append("\n");
		bufOrg.append("import java.io.FileInputStream;\n");
		bufOrg.append("import java.io.FileNotFoundException;\n");
		bufOrg.append("import java.io.InputStream;\n");
		bufOrg.append("import java.net.Socket;\n");
		bufOrg.append("\n");
		bufOrg.append("public class E {\n");
		bufOrg.append("    public void foo() throws FileNotFoundException {\n");
		bufOrg.append("        /*1*/Socket s = new Socket(), s2 = new Socket();\n");
		bufOrg.append("        /*2*/InputStream is = s.getInputStream();\n");
		bufOrg.append("        /*3*/FileInputStream f = new FileInputStream(\"a.b\");\n");
		bufOrg.append("        /*4*/int i = 0;\n");
		bufOrg.append("        System.out.println(s.getInetAddress().toString());\n");
		bufOrg.append("        System.out.println(is.markSupported());/*0*/\n");
		bufOrg.append("    }\n");
		bufOrg.append("}\n");
		bufOrg.append("\n");

		ICompilationUnit cu= pack1.createCompilationUnit("E.java", bufOrg.toString(), false, null);
		String strEnd= "/*0*/";

		String str1= "/*1*/";
		AssistContext context= getCorrectionContext(cu, bufOrg.toString().indexOf(str1), bufOrg.toString().indexOf(strEnd) - bufOrg.toString().indexOf(str1));
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) findProposalByName("Surround with try-with-resources", proposals);
		String preview1= getPreviewContent(proposal);

		StringBuilder buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("import java.io.FileInputStream;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.io.InputStream;\n");
		buf.append("import java.net.Socket;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() throws FileNotFoundException {\n");
		buf.append("        try (/*1*/Socket s = new Socket();\n");
		buf.append("                Socket s2 = new Socket();\n");
		buf.append("                /*2*/InputStream is = s.getInputStream();\n");
		buf.append("                /*3*/FileInputStream f = new FileInputStream(\"a.b\")) {\n");
		buf.append("            /*4*/int i = 0;\n");
		buf.append("            System.out.println(s.getInetAddress().toString());\n");
		buf.append("            System.out.println(is.markSupported());/*0*/\n");
		buf.append("        } catch (FileNotFoundException e) {\n");
		buf.append("            throw e;\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("            // TODO Auto-generated catch block\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		String expected1= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });

		str1= "/*2*/";
		context= getCorrectionContext(cu, bufOrg.toString().indexOf(str1), bufOrg.toString().indexOf(strEnd) - bufOrg.toString().indexOf(str1));
		proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		proposal= (CUCorrectionProposal) findProposalByName("Surround with try-with-resources", proposals);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("import java.io.FileInputStream;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.io.InputStream;\n");
		buf.append("import java.net.Socket;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() throws FileNotFoundException {\n");
		buf.append("        /*1*/Socket s = new Socket(), s2 = new Socket();\n");
		buf.append("        try (/*2*/InputStream is = s.getInputStream();\n");
		buf.append("                /*3*/FileInputStream f = new FileInputStream(\"a.b\")) {\n");
		buf.append("            /*4*/int i = 0;\n");
		buf.append("            System.out.println(s.getInetAddress().toString());\n");
		buf.append("            System.out.println(is.markSupported());/*0*/\n");
		buf.append("        } catch (FileNotFoundException e) {\n");
		buf.append("            throw e;\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("            // TODO Auto-generated catch block\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview2 }, new String[] { expected2 });

		str1= "/*4*/";
		context= getCorrectionContext(cu, bufOrg.toString().indexOf(str1), bufOrg.toString().indexOf(strEnd) - bufOrg.toString().indexOf(str1));
		proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
	}
	@Test
	public void testSurroundWithTryWithResource_03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		StringBuilder bufOrg= new StringBuilder();
		bufOrg.append("package p;\n");
		bufOrg.append("\n");
		bufOrg.append("import java.io.FileInputStream;\n");
		bufOrg.append("import java.io.FileNotFoundException;\n");
		bufOrg.append("import java.io.InputStream;\n");
		bufOrg.append("import java.net.Socket;\n");
		bufOrg.append("\n");
		bufOrg.append("public class E {\n");
		bufOrg.append("    public void foo() {\n");
		bufOrg.append("        try {\n");
		bufOrg.append("            /*1*/Socket s = new Socket(), s2 = new Socket();\n");
		bufOrg.append("            /*2*/InputStream is = s.getInputStream();\n");
		bufOrg.append("            /*3*/FileInputStream f = new FileInputStream(\"a.b\");\n");
		bufOrg.append("            /*4*/int i = 0;\n");
		bufOrg.append("            System.out.println(s.getInetAddress().toString());\n");
		bufOrg.append("            System.out.println(is.markSupported());/*0*/\n");
		bufOrg.append("        } catch (FileNotFoundException e) {\n");
		bufOrg.append("        }\n");
		bufOrg.append("    }\n");
		bufOrg.append("}\n");
		bufOrg.append("\n");

		ICompilationUnit cu= pack1.createCompilationUnit("E.java", bufOrg.toString(), false, null);
		String strEnd= "/*0*/";

		String str1= "/*1*/";
		AssistContext context= getCorrectionContext(cu, bufOrg.toString().indexOf(str1), bufOrg.toString().indexOf(strEnd) - bufOrg.toString().indexOf(str1));
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) findProposalByName("Surround with try-with-resources", proposals);
		String preview1= getPreviewContent(proposal);

		StringBuilder buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("import java.io.FileInputStream;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.io.InputStream;\n");
		buf.append("import java.net.Socket;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        try (/*1*/Socket s = new Socket();\n");
		buf.append("                Socket s2 = new Socket();\n");
		buf.append("                /*2*/InputStream is = s.getInputStream();\n");
		buf.append("                /*3*/FileInputStream f = new FileInputStream(\"a.b\")) {\n");
		buf.append("            /*4*/int i = 0;\n");
		buf.append("            System.out.println(s.getInetAddress().toString());\n");
		buf.append("            System.out.println(is.markSupported());/*0*/\n");
		buf.append("        } catch (FileNotFoundException e) {\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("            // TODO Auto-generated catch block\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		String expected1= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
		assertCorrectLabels(proposals);
	}
	@Test
	public void testSurroundWithTryWithResource_04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		StringBuilder bufOrg= new StringBuilder();
		bufOrg.append("package p;\n");
		bufOrg.append("\n");
		bufOrg.append("import java.io.FileInputStream;\n");
		bufOrg.append("import java.io.FileNotFoundException;\n");
		bufOrg.append("import java.io.InputStream;\n");
		bufOrg.append("import java.net.Socket;\n");
		bufOrg.append("\n");
		bufOrg.append("public class E {\n");
		bufOrg.append("    public void foo() throws FileNotFoundException {\n");
		bufOrg.append("        try {\n");
		bufOrg.append("            /*1*/Socket s = new Socket(), s2 = new Socket();\n");
		bufOrg.append("            /*2*/InputStream is = s.getInputStream();\n");
		bufOrg.append("            /*3*/FileInputStream f = new FileInputStream(\"a.b\");\n");
		bufOrg.append("            /*4*/int i = 0;\n");
		bufOrg.append("            System.out.println(s.getInetAddress().toString());\n");
		bufOrg.append("            System.out.println(is.markSupported());/*0*/\n");
		bufOrg.append("        } catch (FileNotFoundException e) {\n");
		bufOrg.append("        }\n");
		bufOrg.append("    }\n");
		bufOrg.append("}\n");
		bufOrg.append("\n");

		ICompilationUnit cu= pack1.createCompilationUnit("E.java", bufOrg.toString(), false, null);
		String strEnd= "/*0*/";

		String str1= "/*1*/";
		AssistContext context= getCorrectionContext(cu, bufOrg.toString().indexOf(str1), bufOrg.toString().indexOf(strEnd) - bufOrg.toString().indexOf(str1));
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) findProposalByName("Surround with try-with-resources", proposals);
		String preview1= getPreviewContent(proposal);

		StringBuilder buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("import java.io.FileInputStream;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.io.InputStream;\n");
		buf.append("import java.net.Socket;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() throws FileNotFoundException {\n");
		buf.append("        try (/*1*/Socket s = new Socket();\n");
		buf.append("                Socket s2 = new Socket();\n");
		buf.append("                /*2*/InputStream is = s.getInputStream();\n");
		buf.append("                /*3*/FileInputStream f = new FileInputStream(\"a.b\")) {\n");
		buf.append("            /*4*/int i = 0;\n");
		buf.append("            System.out.println(s.getInetAddress().toString());\n");
		buf.append("            System.out.println(is.markSupported());/*0*/\n");
		buf.append("        } catch (FileNotFoundException e) {\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("            // TODO Auto-generated catch block\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		String expected1= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
		assertCorrectLabels(proposals);
	}

	@Test
	public void testSurroundWithTryWithResource_05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		StringBuilder bufOrg= new StringBuilder();
		bufOrg.append("package p;\n");
		bufOrg.append("\n");
		bufOrg.append("import java.io.File;\n"
				+ "import java.util.stream.Stream;\n"
				+ "\n"
				+ "public class X {\n"
				+ "	public static void main(String[] args) throws Exception {\n"
				+ "		try {\n"
				+ "			try {\n"
				+ "				Stream<File> stream = Stream.of(new File(\"\"));\n"
				+ "				System.out.println(stream);\n"
				+ "			} catch (Exception e) {\n"
				+ "			}\n"
				+ "		} catch (Exception e) {\n"
				+ "		}\n"
				+ "	}\n"
				+ "}");

		ICompilationUnit cu= pack1.createCompilationUnit("X.java", bufOrg.toString(), false, null);

		int cursor = bufOrg.toString().indexOf("Stream<File>");
		AssistContext context= getCorrectionContext(cu, cursor + 1, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) findProposalByName("Surround with try-with-resources", proposals);
		String preview1= getPreviewContent(proposal);

		StringBuilder buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("import java.io.File;\n"
				+ "import java.util.stream.Stream;\n"
				+ "\n"
				+ "public class X {\n"
				+ "	public static void main(String[] args) throws Exception {\n"
				+ "		try {\n"
				+ "			try (Stream<File> stream = Stream.of(new File(\"\"))) {\n"
				+ "				System.out.println(stream);\n"
				+ "			} catch (Exception e) {\n"
				+ "			}\n"
				+ "		} catch (Exception e) {\n"
				+ "		}\n"
				+ "	}\n"
				+ "}");
		String expected1= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
		assertCorrectLabels(proposals);
	}


	@Test
	public void testWrapInOptional_01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("import java.util.Optional;\n");
		buf.append("public class E {\n");
		buf.append("	Optional<Integer> a = 1;\n");
		buf.append("}\n");

		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		CompilationUnit compilationUnit= getASTRoot(cu);
		IProblem[] problems= compilationUnit.getProblems();
		assertNumberOfProblems(1, problems);

		List<IJavaCompletionProposal> proposals= collectCorrections(cu, problems[0], null);

		assertNumberOfProposals(proposals, 3);

		buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("import java.util.Optional;\n");
		buf.append("public class E {\n");
		buf.append("	Optional<Integer> a = Optional.of(1);\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] { buf.toString() });

		buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("import java.util.Optional;\n");
		buf.append("public class E {\n");
		buf.append("	Optional<Integer> a = Optional.empty();\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] { buf.toString() });
	}
	@Test
	public void testWrapInOptional_02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("import java.util.Optional;\n");
		buf.append("public class E {\n");
		buf.append("	Optional<Object> foo(int x) {\n");
		buf.append("		return bar();\n");
		buf.append("	}\n");
		buf.append("	Object bar() {\n");
		buf.append("		return null;\n");
		buf.append("	}\n");
		buf.append("}\n");

		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		CompilationUnit compilationUnit= getASTRoot(cu);
		IProblem[] problems= compilationUnit.getProblems();
		assertNumberOfProblems(1, problems);

		List<IJavaCompletionProposal> proposals= collectCorrections(cu, problems[0], null);

		assertNumberOfProposals(proposals, 6);

		buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("import java.util.Optional;\n");
		buf.append("public class E {\n");
		buf.append("	Optional<Object> foo(int x) {\n");
		buf.append("		return Optional.of(bar());\n");
		buf.append("	}\n");
		buf.append("	Object bar() {\n");
		buf.append("		return null;\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] { buf.toString() });

		buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("import java.util.Optional;\n");
		buf.append("public class E {\n");
		buf.append("	Optional<Object> foo(int x) {\n");
		buf.append("		return Optional.ofNullable(bar());\n");
		buf.append("	}\n");
		buf.append("	Object bar() {\n");
		buf.append("		return null;\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] { buf.toString() });

		buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("import java.util.Optional;\n");
		buf.append("public class E {\n");
		buf.append("	Optional<Object> foo(int x) {\n");
		buf.append("		return Optional.empty();\n");
		buf.append("	}\n");
		buf.append("	Object bar() {\n");
		buf.append("		return null;\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] { buf.toString() });
	}
	@Test
	public void testWrapInOptional_03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("import java.util.Optional;\n");
		buf.append("public class E <T> {\n");
		buf.append("	Optional<T> a = 1;\n");
		buf.append("}\n");

		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		CompilationUnit compilationUnit= getASTRoot(cu);
		IProblem[] problems= compilationUnit.getProblems();
		assertNumberOfProblems(1, problems);

		List<IJavaCompletionProposal> proposals= collectCorrections(cu, problems[0], null);

		assertNumberOfProposals(proposals, 2);

		buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("import java.util.Optional;\n");
		buf.append("public class E <T> {\n");
		buf.append("	Optional<T> a = Optional.empty();\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] { buf.toString() });
	}

	@Test
	public void testAssignInTryWithResources_01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String src=
				"package test1;\n" +
				"\n" +
				"import java.io.FileInputStream;\n" +
				"import java.io.IOException;\n" +
				"\n" +
				"class E {\n" +
				"    void f() throws IOException {\n" +
				"        new FileInputStream(\"f\");\n" +
				"    }\n" +
				"}";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", src, false, null);

		int offset= src.indexOf("new");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 6);
		assertCorrectLabels(proposals);

		String expected=
				"package test1;\n" +
				"\n" +
				"import java.io.FileInputStream;\n" +
				"import java.io.IOException;\n" +
				"\n" +
				"class E {\n" +
				"    void f() throws IOException {\n" +
				"        try (FileInputStream fileInputStream = new FileInputStream(\"f\")) {\n" +
				"            \n" +
				"        };\n" +
				"    }\n" +
				"}";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testAssignInTryWithResources_02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String src=
				"package test1;\n" +
				"\n" +
				"import java.io.FileInputStream;\n" +
				"import java.io.FileNotFoundException;\n" +
				"\n" +
				"class E {\n" +
				"    void f() throws FileNotFoundException {\n" +
				"        new FileInputStream(\"f\");\n" +
				"    }\n" +
				"}";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", src, false, null);

		int offset= src.indexOf("new");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 6);
		assertCorrectLabels(proposals);

		String expected=
				"package test1;\n" +
				"\n" +
				"import java.io.FileInputStream;\n" +
				"import java.io.FileNotFoundException;\n" +
				"import java.io.IOException;\n" +
				"\n" +
				"class E {\n" +
				"    void f() throws FileNotFoundException {\n" +
				"        try (FileInputStream fileInputStream = new FileInputStream(\"f\")) {\n" +
				"            \n" +
				"        } catch (FileNotFoundException e) {\n" +
				"            throw e;\n" +
				"        } catch (IOException e) {\n" +
	            "            // TODO Auto-generated catch block\n" +
				"            e.printStackTrace();\n" +
				"        };\n" +
				"    }\n" +
				"}";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testAssignInTryWithResources_03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String src=
				"package test1;\n" +
				"\n" +
				"import java.io.FileInputStream;\n" +
				"import java.io.FileNotFoundException;\n" +
				"\n" +
				"class E {\n" +
				"    void f() {\n" +
				"        try {\n" +
				"            new FileInputStream(\"f\");\n" +
				"        } catch (FileNotFoundException e) {\n" +
				"            // some action\n" +
				"        }\n" +
				"    }\n" +
				"}";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", src, false, null);

		int offset= src.indexOf("new");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 6);
		assertCorrectLabels(proposals);

		String expected=
				"package test1;\n" +
				"\n" +
				"import java.io.FileInputStream;\n" +
				"import java.io.FileNotFoundException;\n" +
				"import java.io.IOException;\n" +
				"\n" +
				"class E {\n" +
				"    void f() {\n" +
				"        try (FileInputStream fileInputStream = new FileInputStream(\"f\")) {\n" +
				"        } catch (FileNotFoundException e) {\n" +
				"            // some action\n" +
				"        } catch (IOException e) {\n" +
	            "            // TODO Auto-generated catch block\n" +
				"            e.printStackTrace();\n" +
				"        }\n" +
				"    }\n" +
				"}";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testAssignInTryWithResources_04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String src=
				"package test1;\n" +
				"\n" +
				"import java.io.FileInputStream;\n" +
				"import java.io.FileNotFoundException;\n" +
				"\n" +
				"class E {\n" +
				"    void f() throws FileNotFoundException {\n" +
				"        try {\n" +
				"            String s = \"a.b\";\n" +
				"            new FileInputStream(s);\n" +
				"        } catch (FileNotFoundException e) {\n" +
				"            // some action\n" +
				"        }\n" +
				"    }\n" +
				"}";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", src, false, null);

		int offset= src.indexOf("new");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		String expected=
				"package test1;\n" +
				"\n" +
				"import java.io.FileInputStream;\n" +
				"import java.io.FileNotFoundException;\n" +
				"import java.io.IOException;\n" +
				"\n" +
				"class E {\n" +
				"    void f() throws FileNotFoundException {\n" +
				"        try {\n" +
				"            String s = \"a.b\";\n" +
				"            try (FileInputStream fileInputStream = new FileInputStream(s)) {\n" +
				"                \n" +
				"            } catch (FileNotFoundException e) {\n" +
				"                throw e;\n" +
				"            } catch (IOException e) {\n" +
	            "                // TODO Auto-generated catch block\n" +
				"                e.printStackTrace();\n" +
				"            };\n" +
				"        } catch (FileNotFoundException e) {\n" +
				"            // some action\n" +
				"        }\n" +
				"    }\n" +
				"}";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

}


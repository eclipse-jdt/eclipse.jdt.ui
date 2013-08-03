/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * This is an implementation of an early-draft specification developed under the Java Community Process (JCP) and
 * is made available for testing and evaluation purposes only.
 * The code is not compatible with any specification of the JCP.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.Hashtable;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.fix.FixMessages;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.Java18ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;

public class AssistQuickFixTest18 extends QuickFixTest {

	private static final Class THIS= AssistQuickFixTest18.class;

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	public AssistQuickFixTest18(String name) {
		super(name);
	}

	public static Test suite() {
		return setUpTest(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new Java18ProjectTestSetup(test);
	}

	protected void setUp() throws Exception {
		Hashtable options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "//TODO\n${body_statement}", null);

		fJProject1= Java18ProjectTestSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, Java18ProjectTestSetup.getDefaultClasspath());
	}

	public void testAssignParamToField1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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
		List proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 0);
	}

	public void testAssignParamToField2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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
		List proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 0);
	}

	public void testConvertToLambda1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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
		List proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
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

	public void testConvertToLambda2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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
		List proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
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
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	public void testConvertToLambda3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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
		List proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertProposalDoesNotExist(proposals, FixMessages.LambdaExpressionsFix_convert_to_lambda_expression);
	}

	public void testConvertToLambda4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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
		List proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertProposalDoesNotExist(proposals, FixMessages.LambdaExpressionsFix_convert_to_lambda_expression);
	}

	public void testConvertToLambda5() throws Exception {
		//Quick assist should not be offered in 1.7 mode
		JavaProjectHelper.set17CompilerOptions(fJProject1);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		try {
			StringBuffer buf= new StringBuffer();
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
			List proposals= collectAssists(context, false);

			assertNumberOfProposals(proposals, 1);
			assertProposalDoesNotExist(proposals, FixMessages.LambdaExpressionsFix_convert_to_lambda_expression);
		} finally {
			JavaProjectHelper.set18CompilerOptions(fJProject1);
		}
	}

	public void testConvertToLambda6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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
		List proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertProposalDoesNotExist(proposals, FixMessages.LambdaExpressionsFix_convert_to_lambda_expression);
	}

	public void testConvertToLambda7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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
		List proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertProposalDoesNotExist(proposals, FixMessages.LambdaExpressionsFix_convert_to_lambda_expression);
	}

	public void testConvertToLambda8() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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
		List proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
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

	public void testConvertToLambda9() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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
		List proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
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

	public void testConvertToLambda10() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    int method();\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(I i) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        //selection start\n");
		buf.append("        bar(new I() {\n");
		buf.append("            public int method() {\n");
		buf.append("                return 1;\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("        bar(new I() {\n");
		buf.append("            public int method() {\n");
		buf.append("                return 2;\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("        //selection end\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset1= buf.toString().indexOf("//selection start");
		int offset2= buf.toString().indexOf("//selection end");
		AssistContext context= getCorrectionContext(cu, offset1, offset2 - offset1);
		assertNoErrors(context);
		List proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    int method();\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(I i) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        //selection start\n");
		buf.append("        bar(() -> 1);\n");
		buf.append("        bar(() -> 2);\n");
		buf.append("        //selection end\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	public void testConvertToAnonymousClassCreation1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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
		List proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
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

	public void testConvertToAnonymousClassCreation2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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
		List proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
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

	public void testConvertToAnonymousClassCreation3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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
		List proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
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

	public void testConvertToAnonymousClassCreation4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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
		List proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
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

	public void testConvertToAnonymousClassCreation5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    void method();\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(I i) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        //selection start\n");
		buf.append("        bar(() -> System.out.println());\n");
		buf.append("        bar(() -> System.out.println());\n");
		buf.append("        //selection end\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset1= buf.toString().indexOf("//selection start");
		int offset2= buf.toString().indexOf("//selection end");
		AssistContext context= getCorrectionContext(cu, offset1, offset2 - offset1);
		assertNoErrors(context);
		List proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    void method();\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(I i) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        //selection start\n");
		buf.append("        bar(new I() {\n");
		buf.append("            @Override\n");
		buf.append("            public void method() {\n");
		buf.append("                System.out.println();\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("        bar(new I() {\n");
		buf.append("            @Override\n");
		buf.append("            public void method() {\n");
		buf.append("                System.out.println();\n");
		buf.append("            }\n");
		buf.append("        });\n");
		buf.append("        //selection end\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}
}

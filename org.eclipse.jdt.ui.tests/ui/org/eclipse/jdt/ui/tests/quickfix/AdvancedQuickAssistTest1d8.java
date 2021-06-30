/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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

import java.util.Hashtable;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
public class AdvancedQuickAssistTest1d8 extends QuickFixTest {
	@Rule
    public ProjectTestSetup projectSetup= new Java1d8ProjectTestSetup();

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");

		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
		store.setValue(PreferenceConstants.CODEGEN_KEYWORD_THIS, false);

		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "//TODO\n${body_statement}", null);

		fJProject1= projectSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testConvertToIfReturn1() throws Exception {
		// 'if' in lambda body - positive cases
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface A {\n");
		buf.append("    void run(int n);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface B {\n");
		buf.append("    A foo(int x);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class Test {\n");
		buf.append("    A fi0 = (n1) -> {\n");
		buf.append("        if (n1 == 0) {\n");
		buf.append("            System.out.println(n1);\n");
		buf.append("            return;\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("    \n");
		buf.append("    int fun1(int a, int b) {\n");
		buf.append("        A fi2 = (n2) -> {\n");
		buf.append("            if (a == b) {\n");
		buf.append("                System.out.println(n2);\n");
		buf.append("                return;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("        return a + b;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    A fun2(int a1, int b1) {\n");
		buf.append("        return (n) -> {\n");
		buf.append("            if (a1 == b1) {\n");
		buf.append("                System.out.println(n);\n");
		buf.append("                return;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    int fun3(int a2, int b2) {\n");
		buf.append("        B fi3 = (x) -> (n) -> {\n");
		buf.append("            if (a2 == b2) {\n");
		buf.append("                System.out.println(a2);\n");
		buf.append("                return;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("        return a2 + b2;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		String str= "if (n1 == 0)";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);
		StringBuilder buf1= new StringBuilder();
		buf1.append("package test1;\n");
		buf1.append("@FunctionalInterface\n");
		buf1.append("interface A {\n");
		buf1.append("    void run(int n);\n");
		buf1.append("}\n");
		buf1.append("\n");
		buf1.append("@FunctionalInterface\n");
		buf1.append("interface B {\n");
		buf1.append("    A foo(int x);\n");
		buf1.append("}\n");
		buf1.append("\n");
		buf1.append("public class Test {\n");
		buf1.append("    A fi0 = (n1) -> {\n");
		buf1.append("        if (n1 != 0)\n");
		buf1.append("            return;\n");
		buf1.append("        System.out.println(n1);\n");
		buf1.append("    };\n");
		buf1.append("    \n");
		buf1.append("    int fun1(int a, int b) {\n");
		buf1.append("        A fi2 = (n2) -> {\n");
		buf1.append("            if (a == b) {\n");
		buf1.append("                System.out.println(n2);\n");
		buf1.append("                return;\n");
		buf1.append("            }\n");
		buf1.append("        };\n");
		buf1.append("        return a + b;\n");
		buf1.append("    }\n");
		buf1.append("\n");
		buf1.append("    A fun2(int a1, int b1) {\n");
		buf1.append("        return (n) -> {\n");
		buf1.append("            if (a1 == b1) {\n");
		buf1.append("                System.out.println(n);\n");
		buf1.append("                return;\n");
		buf1.append("            }\n");
		buf1.append("        };\n");
		buf1.append("    }\n");
		buf1.append("\n");
		buf1.append("    int fun3(int a2, int b2) {\n");
		buf1.append("        B fi3 = (x) -> (n) -> {\n");
		buf1.append("            if (a2 == b2) {\n");
		buf1.append("                System.out.println(a2);\n");
		buf1.append("                return;\n");
		buf1.append("            }\n");
		buf1.append("        };\n");
		buf1.append("        return a2 + b2;\n");
		buf1.append("    }\n");
		buf1.append("}\n");
		String expected1= buf1.toString();
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		str= "if (a == b)";
		context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		buf1= new StringBuilder();
		buf1.append("package test1;\n");
		buf1.append("@FunctionalInterface\n");
		buf1.append("interface A {\n");
		buf1.append("    void run(int n);\n");
		buf1.append("}\n");
		buf1.append("\n");
		buf1.append("@FunctionalInterface\n");
		buf1.append("interface B {\n");
		buf1.append("    A foo(int x);\n");
		buf1.append("}\n");
		buf1.append("\n");
		buf1.append("public class Test {\n");
		buf1.append("    A fi0 = (n1) -> {\n");
		buf1.append("        if (n1 == 0) {\n");
		buf1.append("            System.out.println(n1);\n");
		buf1.append("            return;\n");
		buf1.append("        }\n");
		buf1.append("    };\n");
		buf1.append("    \n");
		buf1.append("    int fun1(int a, int b) {\n");
		buf1.append("        A fi2 = (n2) -> {\n");
		buf1.append("            if (a != b)\n");
		buf1.append("                return;\n");
		buf1.append("            System.out.println(n2);\n");
		buf1.append("        };\n");
		buf1.append("        return a + b;\n");
		buf1.append("    }\n");
		buf1.append("\n");
		buf1.append("    A fun2(int a1, int b1) {\n");
		buf1.append("        return (n) -> {\n");
		buf1.append("            if (a1 == b1) {\n");
		buf1.append("                System.out.println(n);\n");
		buf1.append("                return;\n");
		buf1.append("            }\n");
		buf1.append("        };\n");
		buf1.append("    }\n");
		buf1.append("\n");
		buf1.append("    int fun3(int a2, int b2) {\n");
		buf1.append("        B fi3 = (x) -> (n) -> {\n");
		buf1.append("            if (a2 == b2) {\n");
		buf1.append("                System.out.println(a2);\n");
		buf1.append("                return;\n");
		buf1.append("            }\n");
		buf1.append("        };\n");
		buf1.append("        return a2 + b2;\n");
		buf1.append("    }\n");
		buf1.append("}\n");
		expected1= buf1.toString();
		assertExpectedExistInProposals(proposals, new String[] { expected1 });


		str= "if (a1 == b1)";
		context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		buf1= new StringBuilder();
		buf1.append("package test1;\n");
		buf1.append("@FunctionalInterface\n");
		buf1.append("interface A {\n");
		buf1.append("    void run(int n);\n");
		buf1.append("}\n");
		buf1.append("\n");
		buf1.append("@FunctionalInterface\n");
		buf1.append("interface B {\n");
		buf1.append("    A foo(int x);\n");
		buf1.append("}\n");
		buf1.append("\n");
		buf1.append("public class Test {\n");
		buf1.append("    A fi0 = (n1) -> {\n");
		buf1.append("        if (n1 == 0) {\n");
		buf1.append("            System.out.println(n1);\n");
		buf1.append("            return;\n");
		buf1.append("        }\n");
		buf1.append("    };\n");
		buf1.append("    \n");
		buf1.append("    int fun1(int a, int b) {\n");
		buf1.append("        A fi2 = (n2) -> {\n");
		buf1.append("            if (a == b) {\n");
		buf1.append("                System.out.println(n2);\n");
		buf1.append("                return;\n");
		buf1.append("            }\n");
		buf1.append("        };\n");
		buf1.append("        return a + b;\n");
		buf1.append("    }\n");
		buf1.append("\n");
		buf1.append("    A fun2(int a1, int b1) {\n");
		buf1.append("        return (n) -> {\n");
		buf1.append("            if (a1 != b1)\n");
		buf1.append("                return;\n");
		buf1.append("            System.out.println(n);\n");
		buf1.append("        };\n");
		buf1.append("    }\n");
		buf1.append("\n");
		buf1.append("    int fun3(int a2, int b2) {\n");
		buf1.append("        B fi3 = (x) -> (n) -> {\n");
		buf1.append("            if (a2 == b2) {\n");
		buf1.append("                System.out.println(a2);\n");
		buf1.append("                return;\n");
		buf1.append("            }\n");
		buf1.append("        };\n");
		buf1.append("        return a2 + b2;\n");
		buf1.append("    }\n");
		buf1.append("}\n");
		expected1= buf1.toString();
		assertExpectedExistInProposals(proposals, new String[] { expected1 });


		str= "if (a2 == b2)";
		context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		buf1= new StringBuilder();
		buf1.append("package test1;\n");
		buf1.append("@FunctionalInterface\n");
		buf1.append("interface A {\n");
		buf1.append("    void run(int n);\n");
		buf1.append("}\n");
		buf1.append("\n");
		buf1.append("@FunctionalInterface\n");
		buf1.append("interface B {\n");
		buf1.append("    A foo(int x);\n");
		buf1.append("}\n");
		buf1.append("\n");
		buf1.append("public class Test {\n");
		buf1.append("    A fi0 = (n1) -> {\n");
		buf1.append("        if (n1 == 0) {\n");
		buf1.append("            System.out.println(n1);\n");
		buf1.append("            return;\n");
		buf1.append("        }\n");
		buf1.append("    };\n");
		buf1.append("    \n");
		buf1.append("    int fun1(int a, int b) {\n");
		buf1.append("        A fi2 = (n2) -> {\n");
		buf1.append("            if (a == b) {\n");
		buf1.append("                System.out.println(n2);\n");
		buf1.append("                return;\n");
		buf1.append("            }\n");
		buf1.append("        };\n");
		buf1.append("        return a + b;\n");
		buf1.append("    }\n");
		buf1.append("\n");
		buf1.append("    A fun2(int a1, int b1) {\n");
		buf1.append("        return (n) -> {\n");
		buf1.append("            if (a1 == b1) {\n");
		buf1.append("                System.out.println(n);\n");
		buf1.append("                return;\n");
		buf1.append("            }\n");
		buf1.append("        };\n");
		buf1.append("    }\n");
		buf1.append("\n");
		buf1.append("    int fun3(int a2, int b2) {\n");
		buf1.append("        B fi3 = (x) -> (n) -> {\n");
		buf1.append("            if (a2 != b2)\n");
		buf1.append("                return;\n");
		buf1.append("            System.out.println(a2);\n");
		buf1.append("        };\n");
		buf1.append("        return a2 + b2;\n");
		buf1.append("    }\n");
		buf1.append("}\n");
		expected1= buf1.toString();
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToIfReturn2() throws Exception {
		// 'if' in lambda body - negative cases
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface A {\n");
		buf.append("    void run(int n);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface B {\n");
		buf.append("    A foo(int x);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class Test {\n");
		buf.append("    int f1(int a2, int b2) {\n");
		buf.append("        B fi3 = (x) -> {\n");
		buf.append("            if (x != 100) {\n");
		buf.append("                return (n) -> System.out.println(n + x);\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("        return a2 + b2;\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    void f2(int a1, int b1) {\n");
		buf.append("        A a= (n) -> {\n");
		buf.append("            if (a1 == b1) {\n");
		buf.append("                System.out.println(n);\n");
		buf.append("                return;\n");
		buf.append("            }\n");
		buf.append("            bar();\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void bar() {}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		String str= "if (x != 100)"; // #foo does not return void
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.AdvancedQuickAssistProcessor_convertToIfReturn);

		str= "if (a1 == b1)"; // not the last executable statement in lambda body
		context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.AdvancedQuickAssistProcessor_convertToIfReturn);
	}
}

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
 *     Benjamin Muskalla <b.muskalla@gmx.net> - [surround with] "Surround With runnable" crash - https://bugs.eclipse.org/bugs/show_bug.cgi?id=238226
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Preferences;

import org.eclipse.text.templates.TemplatePersistenceData;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.persistence.TemplateStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.QuickTemplateProcessor;

public class SurroundWithTemplateTest extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup = new ProjectTestSetup();

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

		Preferences corePrefs= JavaPlugin.getJavaCorePluginPreferences();
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_PREFIXES, "");
		corePrefs.setValue(JavaCore.CODEASSIST_STATIC_FIELD_PREFIXES, "");
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_SUFFIXES, "");
		corePrefs.setValue(JavaCore.CODEASSIST_STATIC_FIELD_SUFFIXES, "");

		fJProject1= projectSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	private static List<IJavaCompletionProposal> getRunnableProposal(AssistContext context) throws CoreException {

		StringBuilder buf= new StringBuilder();
		buf.append("Runnable runnable = new Runnable() {\n");
		buf.append("    public void run() {\n");
		buf.append("        ${line_selection}\n");
		buf.append("    }\n");
		buf.append("};");

		TemplateStore templateStore= JavaPlugin.getDefault().getTemplateStore();
		for (TemplatePersistenceData t : templateStore.getTemplateData(false)) {
			templateStore.delete(t);
		}
		TemplatePersistenceData surroundWithRunnableTemplate= new TemplatePersistenceData(new Template("runnable", "surround with runnable", "java", buf.toString(), false), true);
		templateStore.add(surroundWithRunnableTemplate);

		IJavaCompletionProposal[] templateProposals= (new QuickTemplateProcessor()).getAssists(context, null);
		if (templateProposals == null || templateProposals.length != 1)
			return Collections.EMPTY_LIST;

		return Arrays.asList(templateProposals);
	}

	@Test
	public void testSurroundWithRunnable1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(1);\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("        System.out.println(1);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                System.out.println(1);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		expected1.append("\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i = 10;\n");
		buf.append("        final int j = 10;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("        int k = 10;\n");
		buf.append("        k++;\n");
		buf.append("        System.out.println(k);\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("        System.out.println(i);\n");
		selection.append("        System.out.println(j);\n");
		selection.append("        int k = 10;\n");
		selection.append("        k++;\n");
		selection.append("        System.out.println(k);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        final int i = 10;\n");
		expected1.append("        final int j = 10;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("                System.out.println(j);\n");
		expected1.append("                int k = 10;\n");
		expected1.append("                k++;\n");
		expected1.append("                System.out.println(k);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        System.out.println(i);\n");
		expected1.append("        System.out.println(j);\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		expected1.append("\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable3() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i = 10;\n");
		buf.append("        int k = 10;\n");
		buf.append("        k++;\n");
		buf.append("        int h = 10;\n");
		buf.append("        int j = 10;\n");
		buf.append("        j++;\n");
		buf.append("        System.out.println(k);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(h);\n");
		buf.append("        i++;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("        j++;\n");
		selection.append("        System.out.println(k);\n");
		selection.append("        System.out.println(j);\n");
		selection.append("        System.out.println(i);\n");
		selection.append("        System.out.println(h);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        final int i = 10;\n");
		expected1.append("        final int k = 10;\n");
		expected1.append("        k++;\n");
		expected1.append("        final int h = 10;\n");
		expected1.append("        final int j = 10;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                j++;\n");
		expected1.append("                System.out.println(k);\n");
		expected1.append("                System.out.println(j);\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("                System.out.println(h);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        i++;\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		expected1.append("\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable4() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int j = 10;\n");
		buf.append("        while (j > 0) {\n");
		buf.append("            System.out.println(j);\n");
		buf.append("            j--;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("            System.out.println(j);\n");
		selection.append("            j--;\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        final int j = 10;\n");
		expected1.append("        while (j > 0) {\n");
		expected1.append("            Runnable runnable = new Runnable() {\n");
		expected1.append("                public void run() {\n");
		expected1.append("                    System.out.println(j);\n");
		expected1.append("                    j--;\n");
		expected1.append("                }\n");
		expected1.append("            };\n");
		expected1.append("        }\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable5() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i = 10;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("        int i = 10;\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        int i;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                i = 10;\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        System.out.println(i);\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		expected1.append("\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable6() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        /***/ int i = 10;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("        /***/ int i = 10;\n");
		selection.append("        System.out.println(i);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        /***/ int i;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                i = 10;\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        System.out.println(i);\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		expected1.append("\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable7() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        /***/ int i = 10;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("        System.out.println(i);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        /***/ final int i = 10;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		expected1.append("\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable8() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        //TextTextText\n");
		buf.append("        \n");
		buf.append("        //TextTextText\n");
		buf.append("        //\n");
		buf.append("        //TextTextText\n");
		buf.append("        /***/ int i = 10;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("int i = 10;\n");
		selection.append("        System.out.println(i);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        //TextTextText\n");
		expected1.append("        \n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                //TextTextText\n");
		expected1.append("                //\n");
		expected1.append("                //TextTextText\n");
		expected1.append("                /***/\n");
		expected1.append("                int i = 10;\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		expected1.append("\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable9() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        /***/ int i = 10;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("int i = 10;\n");
		selection.append("        System.out.println(i);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        /***/ int i;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                i = 10;\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        System.out.println(i);\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable10() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i = 10;\n");
		buf.append("        int j;\n");
		buf.append("        System.out.println(10);\n");
		buf.append("        j = 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("        int i = 10;\n");
		selection.append("        int j;\n");
		selection.append("        System.out.println(10);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        int j;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                int i = 10;\n");
		expected1.append("                System.out.println(10);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        j = 10;\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		expected1.append("\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable11() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("        int i;\n");
		selection.append("        System.out.println(i);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        final int i;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        System.out.println(i);\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable12() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i, String s) {\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(s);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("        System.out.println(i);\n");
		selection.append("        System.out.println(s);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo(final int i, final String s) {\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("                System.out.println(s);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable13() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i, String s) {\n");
		buf.append("        i = 10;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(s);\n");
		buf.append("        s = \"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("        System.out.println(i);\n");
		selection.append("        System.out.println(s);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo(final int i, final String s) {\n");
		expected1.append("        i = 10;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("                System.out.println(s);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        s = \"\";\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable14() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int j,i = 10;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("        System.out.println(i);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        int j;\n");
		expected1.append("        final int i = 10;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        System.out.println(j);\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable15() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int j,i = 10;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        j = 10;\n");
		buf.append("        j++;\n");
		buf.append("        System.out.println(j);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("        System.out.println(i);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        int j;\n");
		expected1.append("        final int i = 10;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        j = 10;\n");
		expected1.append("        j++;\n");
		expected1.append("        System.out.println(j);\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable16() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int j, i = 10;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("        int j, i = 10;\n");
		selection.append("        System.out.println(i);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        int j;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                int i = 10;\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        System.out.println(j);\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable17() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i = 10, j = i;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("        System.out.println(i);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        final int i = 10;\n");
		expected1.append("        int j = i;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        System.out.println(j);\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable18() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i = 10, j = i;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("        System.out.println(i);\n");
		selection.append("        System.out.println(j);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        final int i = 10, j = i;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("                System.out.println(j);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable19() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i = 10, k = i, j = k;\n");
		buf.append("        System.out.println(k);\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("        System.out.println(k);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        int i = 10;\n");
		expected1.append("        final int k = i;\n");
		expected1.append("        int j = k;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                System.out.println(k);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        System.out.println(i);\n");
		expected1.append("        System.out.println(j);\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable20() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i = 10, j = 10;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("        int i = 10, j = 10;\n");
		selection.append("        System.out.println(i);\n");
		selection.append("        System.out.println(j);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                int i = 10, j = 10;\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("                System.out.println(j);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable21() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("            int j = 10;\n");
		buf.append("            System.out.println(j);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("            int j = 10;\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        if (true) {\n");
		expected1.append("            int j;\n");
		expected1.append("            Runnable runnable = new Runnable() {\n");
		expected1.append("                public void run() {\n");
		expected1.append("                    j = 10;\n");
		expected1.append("                }\n");
		expected1.append("            };\n");
		expected1.append("            System.out.println(j);\n");
		expected1.append("        }\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable22() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("            int j = 10;\n");
		buf.append("            while (j == 10) {\n");
		buf.append("                System.out.println(j);\n");
		buf.append("                j--;\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("            while (j == 10) {\n");
		selection.append("                System.out.println(j);\n");
		selection.append("                j--;\n");
		selection.append("            }\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        if (true) {\n");
		expected1.append("            final int j = 10;\n");
		expected1.append("            Runnable runnable = new Runnable() {\n");
		expected1.append("                public void run() {\n");
		expected1.append("                    while (j == 10) {\n");
		expected1.append("                        System.out.println(j);\n");
		expected1.append("                        j--;\n");
		expected1.append("                    }\n");
		expected1.append("                }\n");
		expected1.append("            };\n");
		expected1.append("        }\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable23() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    int i= 9;\n");
		buf.append("    {\n");
		buf.append("        /***/ int k = 10;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("        /***/ int k = 10;\n");
		selection.append("        System.out.println(i);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    int i= 9;\n");
		expected1.append("    {\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                /***/\n");
		expected1.append("                int k = 10;\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable24() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int k = 0, v = 0;\n");
		buf.append("        {\n");
		buf.append("            System.out.println(v);\n");
		buf.append("            System.out.println(k);\n");
		buf.append("        }\n");
		buf.append("        k++;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("            System.out.println(v);\n");
		selection.append("            System.out.println(k);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        final int k = 0, v = 0;\n");
		expected1.append("        {\n");
		expected1.append("            Runnable runnable = new Runnable() {\n");
		expected1.append("                public void run() {\n");
		expected1.append("                    System.out.println(v);\n");
		expected1.append("                    System.out.println(k);\n");
		expected1.append("                }\n");
		expected1.append("            };\n");
		expected1.append("        }\n");
		expected1.append("        k++;\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}
	/*
	@Test
	public void testSurroundWithRunnable25() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int y = 1;\n");
		buf.append("        switch (y) {\n");
		buf.append("        case 1:\n");
		buf.append("            int e4 = 9, e5 = 0;\n");
		buf.append("            System.out.println(e4);\n");
		buf.append("            e5++;\n");
		buf.append("        default:\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuffer selection= new StringBuffer();
		selection.append("            System.out.println(e4);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 9);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        int y = 1;\n");
		expected1.append("        switch (y) {\n");
		expected1.append("        case 1:\n");
		expected1.append("            final int e4 = 9;\n");
		expected1.append("                int e5 = 0;\n");
		expected1.append("            Runnable runnable = new Runnable() {\n");
		expected1.append("                    public void run() {\n");
		expected1.append("                        System.out.println(e4);\n");
		expected1.append("                    }\n");
		expected1.append("                };\n");
		expected1.append("                e5++;\n");
		expected1.append("        default:\n");
		expected1.append("        }\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable26() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int y = 1;\n");
		buf.append("        switch (y) {\n");
		buf.append("        case 1:\n");
		buf.append("            int e4 = 9, e5 = 0;\n");
		buf.append("            System.out.println(e4);\n");
		buf.append("            e5++;\n");
		buf.append("        default:\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuffer selection= new StringBuffer();
		selection.append("            int e4 = 9, e5 = 0;\n");
		selection.append("            System.out.println(e4);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 9);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        int y = 1;\n");
		expected1.append("        switch (y) {\n");
		expected1.append("        case 1:\n");
		expected1.append("            int e5;\n");
		expected1.append("                Runnable runnable = new Runnable() {\n");
		expected1.append("                    public void run() {\n");
		expected1.append("                        int e4 = 9;\n");
		expected1.append("                        e5 = 0;\n");
		expected1.append("                        System.out.println(e4);\n");
		expected1.append("                    }\n");
		expected1.append("                };\n");
		expected1.append("                e5++;\n");
		expected1.append("        default:\n");
		expected1.append("        }\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}*/

	@Test
	public void testSurroundWithRunnable27() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String s = \"\", c = \"\";\n");
		buf.append("        System.out.println(s);\n");
		buf.append("        c = \"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("        System.out.println(s);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        final String s = \"\";\n");
		expected1.append("        String c = \"\";\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                System.out.println(s);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        c = \"\";\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable28() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i = 10, j, k, v;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("        System.out.println(v);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("        int i = 10, j, k, v;\n");
		selection.append("        System.out.println(i);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        int j;\n");
		expected1.append("        int v;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                int i = 10;\n");
		expected1.append("                int k;\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        System.out.println(j);\n");
		expected1.append("        System.out.println(v);\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable29() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        @SuppressWarnings(\"nls\") String s= \"\", k = \"\";\n");
		buf.append("        System.out.println(s);\n");
		buf.append("        System.out.println(k);\n");
		buf.append("        k=\"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("        @SuppressWarnings(\"nls\") String s= \"\", k = \"\";\n");
		selection.append("        System.out.println(s);\n");
		selection.append("        System.out.println(k);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        @SuppressWarnings(\"nls\")\n");
		expected1.append("        String k;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                @SuppressWarnings(\"nls\")\n");
		expected1.append("                String s = \"\";\n");
		expected1.append("                k = \"\";\n");
		expected1.append("                System.out.println(s);\n");
		expected1.append("                System.out.println(k);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        k=\"\";\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable30() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) \n");
		buf.append("            System.out.println(1);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("            System.out.println(1);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		StringBuilder expected1= new StringBuilder();
		expected1.append("package test;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        if (true) {\n");
		expected1.append("            Runnable runnable = new Runnable() {\n");
		expected1.append("                public void run() {\n");
		expected1.append("                    System.out.println(1);\n");
		expected1.append("                }\n");
		expected1.append("            };\n");
		expected1.append("        }\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnableBug133560() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int i = 0; i < 10; i++) {\n");
		buf.append("            System.out.println(i);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("            System.out.println(i);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (final int i = 0; i < 10; i++) {\n");
		buf.append("            Runnable runnable = new Runnable() {\n");
		buf.append("                public void run() {\n");
		buf.append("                    System.out.println(i);\n");
		buf.append("                }\n");
		buf.append("            };\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {buf.toString()});
	}

	@Test
	public void testSurroundWithRunnableBug233278() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("  {\n");
		buf.append("    final int x = 0, y = 1;\n");
		buf.append("    new Object() {\n");
		buf.append("      void method() {\n");
		buf.append("        if (x == y)\n");
		buf.append("          return;\n");
		buf.append("        toString();\n");
		buf.append("      }\n");
		buf.append("    };\n");
		buf.append("  }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("        if (x == y)\n");
		selection.append("          return;\n");
		selection.append("        toString();\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("  {\n");
		buf.append("    final int x = 0, y = 1;\n");
		buf.append("    new Object() {\n");
		buf.append("      void method() {\n");
		buf.append("        Runnable runnable = new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                if (x == y)\n");
		buf.append("                    return;\n");
		buf.append("                toString();\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("      }\n");
		buf.append("    };\n");
		buf.append("  }\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {buf.toString()});
	}

	@Test
	public void testSurroundWithRunnableBug138323() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E<I> {\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(this);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("        System.out.println(this);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E<I> {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Runnable runnable = new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                System.out.println(E.this);\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {buf.toString()});
	}

	@Test
	public void testSurroundWithBug162549() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void m() {\n");
		buf.append("        if (true) {\n");
		buf.append("            System.out.println(\"T\");\n");
		buf.append("        } // else {\n");
		buf.append("        // System.out.println(\"F\");\n");
		buf.append("        // }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuilder selection= new StringBuilder();
		selection.append("if (true) {\n");
		selection.append("            System.out.println(\"T\");\n");
		selection.append("        } // else {\n");
		selection.append("        // System.out.println(\"F\");\n");
		selection.append("        // }\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void m() {\n");
		buf.append("        Runnable runnable = new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                if (true) {\n");
		buf.append("                    System.out.println(\"T\");\n");
		buf.append("                } // else {\n");
		buf.append("                  // System.out.println(\"F\");\n");
		buf.append("                  // }\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {buf.toString()});
	}

}

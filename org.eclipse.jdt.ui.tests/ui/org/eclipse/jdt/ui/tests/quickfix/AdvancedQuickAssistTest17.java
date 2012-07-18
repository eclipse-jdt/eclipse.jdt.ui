/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import org.eclipse.core.runtime.Preferences;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.Java17ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public class AdvancedQuickAssistTest17 extends QuickFixTest {

	private static final Class THIS= AdvancedQuickAssistTest17.class;

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	public AdvancedQuickAssistTest17(String name) {
		super(name);
	}

	public static Test suite() {
		return setUpTest(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new Java17ProjectTestSetup(test);
	}


	protected void setUp() throws Exception {
		Hashtable options= TestOptions.getDefaultOptions();
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

		fJProject1= Java17ProjectTestSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, Java17ProjectTestSetup.getDefaultClasspath());
	}

	public void testConvertSwitchToIf() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String s) {\n");
		buf.append("        switch (s) {\n");
		buf.append("        case \"abc\":\n");
		buf.append("            System.out.println();\n");
		buf.append("            break;\n");
		buf.append("        case \"xyz\":\n");
		buf.append("            System.out.println();\n");
		buf.append("            break;\n");
		buf.append("        default:\n");
		buf.append("            System.out.println();\n");
		buf.append("            break;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("switch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String s) {\n");
		buf.append("        if (\"abc\".equals(s)) {\n");
		buf.append("            System.out.println();\n");
		buf.append("        } else if (\"xyz\".equals(s)) {\n");
		buf.append("            System.out.println();\n");
		buf.append("        } else {\n");
		buf.append("            System.out.println();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String s) {\n");
		buf.append("        if (s.equals(\"abc\")) {\n");
		buf.append("            System.out.println();\n");
		buf.append("        } else if (s.equals(\"xyz\")) {\n");
		buf.append("            System.out.println();\n");
		buf.append("        } else {\n");
		buf.append("            System.out.println();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
	}

	public void testConvertIfToSwitch1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String s) {\n");
		buf.append("        if (s.equals(\"abc\")) {\n");
		buf.append("            System.out.println();\n");
		buf.append("        } else if (s.equals(\"xyz\")) {\n");
		buf.append("            System.out.println();\n");
		buf.append("        } else {\n");
		buf.append("            System.out.println();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String s) {\n");
		buf.append("        switch (s) {\n");
		buf.append("            case \"abc\" :\n");
		buf.append("                System.out.println();\n");
		buf.append("                break;\n");
		buf.append("            case \"xyz\" :\n");
		buf.append("                System.out.println();\n");
		buf.append("                break;\n");
		buf.append("            default :\n");
		buf.append("                System.out.println();\n");
		buf.append("                break;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");

		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	public void testConvertIfToSwitch2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String s) {\n");
		buf.append("        if (\"abc\" == s) {\n");
		buf.append("            System.out.println();\n");
		buf.append("        } else if (\"xyz\" == s) {\n");
		buf.append("            System.out.println();\n");
		buf.append("        } else {\n");
		buf.append("            System.out.println();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		assertProposalDoesNotExist(proposals, CorrectionMessages.AdvancedQuickAssistProcessor_convertIfElseToSwitch);
	}

	public void testConvertIfToSwitch3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String s) {\n");
		buf.append("        if (\"abc\".equals(s)) {\n");
		buf.append("            System.out.println();\n");
		buf.append("        } else if (\"xyz\".equals(s)) {\n");
		buf.append("            System.out.println();\n");
		buf.append("        } else {\n");
		buf.append("            System.out.println();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String s) {\n");
		buf.append("        switch (s) {\n");
		buf.append("            case \"abc\" :\n");
		buf.append("                System.out.println();\n");
		buf.append("                break;\n");
		buf.append("            case \"xyz\" :\n");
		buf.append("                System.out.println();\n");
		buf.append("                break;\n");
		buf.append("            default :\n");
		buf.append("                System.out.println();\n");
		buf.append("                break;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String s) {\n");
		buf.append("        if (s == null) {\n");
		buf.append("            System.out.println();\n");
		buf.append("        } else {\n");
		buf.append("            switch (s) {\n");
		buf.append("                case \"abc\" :\n");
		buf.append("                    System.out.println();\n");
		buf.append("                    break;\n");
		buf.append("                case \"xyz\" :\n");
		buf.append("                    System.out.println();\n");
		buf.append("                    break;\n");
		buf.append("                default :\n");
		buf.append("                    System.out.println();\n");
		buf.append("                    break;\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
	}

	public void testConvertIfToSwitch4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String s) {\n");
		buf.append("        if (s.equals(\"abc\")) {\n");
		buf.append("            System.out.println();\n");
		buf.append("        } else if (\"xyz\".equals(s)) {\n");
		buf.append("            System.out.println();\n");
		buf.append("        } else {\n");
		buf.append("            System.out.println();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String s) {\n");
		buf.append("        switch (s) {\n");
		buf.append("            case \"abc\" :\n");
		buf.append("                System.out.println();\n");
		buf.append("                break;\n");
		buf.append("            case \"xyz\" :\n");
		buf.append("                System.out.println();\n");
		buf.append("                break;\n");
		buf.append("            default :\n");
		buf.append("                System.out.println();\n");
		buf.append("                break;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String s) {\n");
		buf.append("        if (s == null) {\n");
		buf.append("            System.out.println();\n");
		buf.append("        } else {\n");
		buf.append("            switch (s) {\n");
		buf.append("                case \"abc\" :\n");
		buf.append("                    System.out.println();\n");
		buf.append("                    break;\n");
		buf.append("                case \"xyz\" :\n");
		buf.append("                    System.out.println();\n");
		buf.append("                    break;\n");
		buf.append("                default :\n");
		buf.append("                    System.out.println();\n");
		buf.append("                    break;\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
	}

	public void testConvertIfToSwitch5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String s) {\n");
		buf.append("        if (\"abc\".equals(s)) {\n");
		buf.append("            System.out.println();\n");
		buf.append("        } else if (\"xyz\".equals(s)) {\n");
		buf.append("            System.out.println();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String s) {\n");
		buf.append("        switch (s) {\n");
		buf.append("            case \"abc\" :\n");
		buf.append("                System.out.println();\n");
		buf.append("                break;\n");
		buf.append("            case \"xyz\" :\n");
		buf.append("                System.out.println();\n");
		buf.append("                break;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String s) {\n");
		buf.append("        if (s == null) {\n");
		buf.append("        } else {\n");
		buf.append("            switch (s) {\n");
		buf.append("                case \"abc\" :\n");
		buf.append("                    System.out.println();\n");
		buf.append("                    break;\n");
		buf.append("                case \"xyz\" :\n");
		buf.append("                    System.out.println();\n");
		buf.append("                    break;\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
	}

	public void testReplaceReturnConditionWithIf4() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=112443
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Collections;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E {\n");
		buf.append("    List<String> foo(int a) {\n");
		buf.append("        return a > 0 ? new ArrayList<>() : new ArrayList<>();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("?");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Collections;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E {\n");
		buf.append("    List<String> foo(int a) {\n");
		buf.append("        if (a > 0)\n");
		buf.append("            return new ArrayList<>();\n");
		buf.append("        else\n");
		buf.append("            return new ArrayList<>();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}
	
	public void testReplaceReturnIfWithCondition3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E {\n");
		buf.append("    public List<String> foo(int a) {\n");
		buf.append("        if (a > 0) {\n");
		buf.append("            return new ArrayList<>();\n");
		buf.append("        } else {\n");
		buf.append("            return new ArrayList<>();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E {\n");
		buf.append("    public List<String> foo(int a) {\n");
		buf.append("        return a > 0 ? new ArrayList<String>() : new ArrayList<String>();\n");
		buf.append("    }\n");
		buf.append("}\n");

		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}
	
	public void testReplaceReturnIfWithCondition4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Collections;\n");
		buf.append("import java.util.Map;\n");
		buf.append("public class E {\n");
		buf.append("    public Map<String, java.io.IOException> foo(int a) {\n");
		buf.append("        if (a > 0) {\n");
		buf.append("            return Collections.emptyMap();\n");
		buf.append("        } else {\n");
		buf.append("            return Collections.singletonMap(\"none\", null);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		int offset= buf.toString().indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List proposals= collectAssists(context, false);
		
		assertCorrectLabels(proposals);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.util.Collections;\n");
		buf.append("import java.util.Map;\n");
		buf.append("public class E {\n");
		buf.append("    public Map<String, java.io.IOException> foo(int a) {\n");
		buf.append("        return a > 0 ? Collections.<String, IOException>emptyMap() : Collections.<String, IOException>singletonMap(\"none\", null);\n");
		buf.append("    }\n");
		buf.append("}\n");
		
		String expected1= buf.toString();
		
		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}
	
}

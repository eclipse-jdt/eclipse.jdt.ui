/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
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

public class AssistQuickFixTest17 extends QuickFixTest {

	private static final String CONVERT_TO_A_SINGLE_MULTI_CATCH_BLOCK= "Convert to a single multi-catch block";
	private static final String CONVERT_TO_MULTIPLE_CATCH_BLOCKS= "Convert to multiple single-type catch blocks";

	private static final Class THIS= AssistQuickFixTest17.class;

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	public AssistQuickFixTest17(String name) {
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

	public void testConvertToMultiCatch1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        try {\n");
		buf.append("            System.out.println(\"foo\");\n");
		buf.append("        } catch (IllegalArgumentException ex) {\n");
		buf.append("            ex.printStackTrace();\n");
		buf.append("        } catch (NullPointerException ex) {\n");
		buf.append("            ex.printStackTrace();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("catch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        try {\n");
		buf.append("            System.out.println(\"foo\");\n");
		buf.append("        } catch (IllegalArgumentException | NullPointerException ex) {\n");
		buf.append("            ex.printStackTrace();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	public void testConvertToMultiCatch2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        try {\n");
		buf.append("            System.out.println(\"foo\");\n");
		buf.append("        } catch (IllegalArgumentException e) {\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        } catch (NullPointerException e) {\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("catch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        try {\n");
		buf.append("            System.out.println(\"foo\");\n");
		buf.append("        } catch (IllegalArgumentException | NullPointerException e) {\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	public void testConvertToMultiCatch3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        try {\n");
		buf.append("            System.out.println(\"foo\");\n");
		buf.append("        } catch (IllegalArgumentException | NullPointerException e) {\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        } catch (RuntimeException e) {\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("catch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        try {\n");
		buf.append("            System.out.println(\"foo\");\n");
		buf.append("        } catch (IllegalArgumentException | NullPointerException | RuntimeException e) {\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	public void testConvertToMultiCatch4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        try {\n");
		buf.append("            System.out.println(\"foo\");\n");
		buf.append("        } catch (IllegalArgumentException e) {\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        } catch (NullPointerException e) {\n");
		buf.append("            \n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("catch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List proposals= collectAssists(context, false);

		assertProposalDoesNotExist(proposals, CONVERT_TO_A_SINGLE_MULTI_CATCH_BLOCK);
	}

	public void testConvertToMultiCatch5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        try {\n");
		buf.append("            System.out.println(\"foo\");\n");
		buf.append("        } catch (IllegalArgumentException e) {\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("catch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List proposals= collectAssists(context, false);

		assertProposalDoesNotExist(proposals, CONVERT_TO_A_SINGLE_MULTI_CATCH_BLOCK);
	}

	public void testConvertToMultiCatch6() throws Exception {
		//Quick assist should not be offered in 1.5 mode
		JavaProjectHelper.set15CompilerOptions(fJProject1);
		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			StringBuffer buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("public class E {\n");
			buf.append("    void foo() {\n");
			buf.append("        try {\n");
			buf.append("            System.out.println(\"foo\");\n");
			buf.append("        } catch (IllegalArgumentException e) {\n");
			buf.append("            e.printStackTrace();\n");
			buf.append("        } catch (NullPointerException e) {\n");
			buf.append("            e.printStackTrace();\n");
			buf.append("        }\n");
			buf.append("    }\n");
			buf.append("}\n");
			ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

			int offset= buf.toString().indexOf("catch");
			AssistContext context= getCorrectionContext(cu, offset, 0);
			assertNoErrors(context);
			List proposals= collectAssists(context, false);

			assertProposalDoesNotExist(proposals, CONVERT_TO_A_SINGLE_MULTI_CATCH_BLOCK);
		} finally {
			JavaProjectHelper.set17CompilerOptions(fJProject1);
		}
	}

	public void testUnrollMultiCatch1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        try {\n");
		buf.append("            System.out.println(\"foo\");\n");
		buf.append("        } catch (IllegalArgumentException | NullPointerException ex) {\n");
		buf.append("            ex.printStackTrace();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("catch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        try {\n");
		buf.append("            System.out.println(\"foo\");\n");
		buf.append("        } catch (IllegalArgumentException ex) {\n");
		buf.append("            ex.printStackTrace();\n");
		buf.append("        } catch (NullPointerException ex) {\n");
		buf.append("            ex.printStackTrace();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	public void testUnrollMultiCatch2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        try {\n");
		buf.append("            System.out.println(\"foo\");\n");
		buf.append("        } catch (IllegalArgumentException | NullPointerException e) {\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        } catch (RuntimeException e) {\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("catch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        try {\n");
		buf.append("            System.out.println(\"foo\");\n");
		buf.append("        } catch (IllegalArgumentException e) {\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        } catch (NullPointerException e) {\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        } catch (RuntimeException e) {\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	public void testUnrollMultiCatch3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        try {\n");
		buf.append("            System.out.println(\"foo\");\n");
		buf.append("        } catch (IllegalArgumentException e) {\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        } catch (NullPointerException | ClassCastException e) {\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("catch (NullPointerException");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        try {\n");
		buf.append("            System.out.println(\"foo\");\n");
		buf.append("        } catch (IllegalArgumentException e) {\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        } catch (NullPointerException e) {\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        } catch (ClassCastException e) {\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	public void testUnrollMultiCatch4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        try {\n");
		buf.append("            System.out.println(\"foo\");\n");
		buf.append("        } catch (IllegalArgumentException e) {\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        } catch (NullPointerException | ClassCastException e) {\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        } catch (ArrayIndexOutOfBoundsException e) {\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("catch (NullPointerException");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        try {\n");
		buf.append("            System.out.println(\"foo\");\n");
		buf.append("        } catch (IllegalArgumentException e) {\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        } catch (NullPointerException e) {\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        } catch (ClassCastException e) {\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        } catch (ArrayIndexOutOfBoundsException e) {\n");
		buf.append("            e.printStackTrace();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	public void testUnrollMultiCatch5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        try {\n");
		buf.append("            System.out.println(\"foo\");\n");
		buf.append("        } catch (NullPointerException ex) {\n");
		buf.append("            ex.printStackTrace();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("catch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List proposals= collectAssists(context, false);

		assertProposalDoesNotExist(proposals, CONVERT_TO_MULTIPLE_CATCH_BLOCKS);
	}

	public void testReplaceMultiCatchClauseWithThrows() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        try {\n");
		buf.append("            goo();\n");
		buf.append("        } catch (IllegalArgumentException | NullPointerException e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		String str= "catch";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
		List proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() throws IllegalArgumentException, NullPointerException {\n");
		buf.append("        goo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        goo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
	}

}

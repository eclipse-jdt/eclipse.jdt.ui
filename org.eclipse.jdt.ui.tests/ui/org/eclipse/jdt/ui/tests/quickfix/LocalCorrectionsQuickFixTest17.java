/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.Hashtable;

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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.Java17ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class LocalCorrectionsQuickFixTest17 extends QuickFixTest {

	private static final Class THIS= LocalCorrectionsQuickFixTest17.class;

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;


	public LocalCorrectionsQuickFixTest17(String name) {
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
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, String.valueOf(99));
		options.put(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_MISSING_HASHCODE_METHOD, JavaCore.WARNING);

		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		StubUtility.setCodeTemplate(CodeTemplateContextType.CATCHBLOCK_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "", null);

		fJProject1= Java17ProjectTestSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, Java17ProjectTestSetup.getDefaultClasspath());
	}

	public void testUncaughtExceptionUnionType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.io.InterruptedIOException;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(int a) {\n");
		buf.append("        try {\n");
		buf.append("            if (a < 10)\n");
		buf.append("                throw new FileNotFoundException();\n");
		buf.append("            else if (a < 20)\n");
		buf.append("                throw new InterruptedIOException();\n");
		buf.append("            else\n");
		buf.append("                throw new IOException();\n");
		buf.append("        } catch (FileNotFoundException | InterruptedIOException ex) {\n");
		buf.append("            ex.printStackTrace();\n");
		buf.append("        } \n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.io.InterruptedIOException;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(int a) throws IOException {\n");
		buf.append("        try {\n");
		buf.append("            if (a < 10)\n");
		buf.append("                throw new FileNotFoundException();\n");
		buf.append("            else if (a < 20)\n");
		buf.append("                throw new InterruptedIOException();\n");
		buf.append("            else\n");
		buf.append("                throw new IOException();\n");
		buf.append("        } catch (FileNotFoundException | InterruptedIOException ex) {\n");
		buf.append("            ex.printStackTrace();\n");
		buf.append("        } \n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.io.InterruptedIOException;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(int a) {\n");
		buf.append("        try {\n");
		buf.append("            if (a < 10)\n");
		buf.append("                throw new FileNotFoundException();\n");
		buf.append("            else if (a < 20)\n");
		buf.append("                throw new InterruptedIOException();\n");
		buf.append("            else\n");
		buf.append("                throw new IOException();\n");
		buf.append("        } catch (FileNotFoundException | InterruptedIOException ex) {\n");
		buf.append("            ex.printStackTrace();\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("        } \n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
	}


}

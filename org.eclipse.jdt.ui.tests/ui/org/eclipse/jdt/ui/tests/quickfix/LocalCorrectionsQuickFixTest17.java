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
import org.eclipse.jdt.internal.ui.text.correction.proposals.CUCorrectionProposal;

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
		assertNumberOfProposals(proposals, 4);
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
		buf.append("        } catch (FileNotFoundException | InterruptedIOException | IOException ex) {\n");
		buf.append("            ex.printStackTrace();\n");
		buf.append("        } \n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3 });
	}

	public void testUncaughtExceptionTryWithResources1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileInputStream;\n");
		buf.append("class MyException extends Exception {\n");
		buf.append("    static final long serialVersionUID = 1L;\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(int n) throws IllegalArgumentException, MyException {\n");
		buf.append("        if (n == 1)\n");
		buf.append("            throw new IllegalArgumentException();\n");
		buf.append("        else\n");
		buf.append("            throw new MyException();\n");
		buf.append("    }\n");
		buf.append("    void foo(String name, boolean b) {\n");
		buf.append("        try (FileInputStream fis = new FileInputStream(name)) {\n");
		buf.append("            bar(1);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");

		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 3, 0); //quick fix on 1st problem
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);


		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileInputStream;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("class MyException extends Exception {\n");
		buf.append("    static final long serialVersionUID = 1L;\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(int n) throws IllegalArgumentException, MyException {\n");
		buf.append("        if (n == 1)\n");
		buf.append("            throw new IllegalArgumentException();\n");
		buf.append("        else\n");
		buf.append("            throw new MyException();\n");
		buf.append("    }\n");
		buf.append("    void foo(String name, boolean b) throws FileNotFoundException, IOException {\n");
		buf.append("        try (FileInputStream fis = new FileInputStream(name)) {\n");
		buf.append("            bar(1);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileInputStream;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("class MyException extends Exception {\n");
		buf.append("    static final long serialVersionUID = 1L;\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(int n) throws IllegalArgumentException, MyException {\n");
		buf.append("        if (n == 1)\n");
		buf.append("            throw new IllegalArgumentException();\n");
		buf.append("        else\n");
		buf.append("            throw new MyException();\n");
		buf.append("    }\n");
		buf.append("    void foo(String name, boolean b) {\n");
		buf.append("        try (FileInputStream fis = new FileInputStream(name)) {\n");
		buf.append("            bar(1);\n");
		buf.append("        } catch (FileNotFoundException e) {\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileInputStream;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("class MyException extends Exception {\n");
		buf.append("    static final long serialVersionUID = 1L;\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(int n) throws IllegalArgumentException, MyException {\n");
		buf.append("        if (n == 1)\n");
		buf.append("            throw new IllegalArgumentException();\n");
		buf.append("        else\n");
		buf.append("            throw new MyException();\n");
		buf.append("    }\n");
		buf.append("    void foo(String name, boolean b) {\n");
		buf.append("        try (FileInputStream fis = new FileInputStream(name)) {\n");
		buf.append("            bar(1);\n");
		buf.append("        } catch (FileNotFoundException | IOException e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	public void testUncaughtExceptionTryWithResources2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileInputStream;\n");
		buf.append("class MyException extends Exception {\n");
		buf.append("    static final long serialVersionUID = 1L;\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(int n) throws IllegalArgumentException, MyException {\n");
		buf.append("        if (n == 1)\n");
		buf.append("            throw new IllegalArgumentException();\n");
		buf.append("        else\n");
		buf.append("            throw new MyException();\n");
		buf.append("    }\n");
		buf.append("    void foo(String name, boolean b) {\n");
		buf.append("        try (FileInputStream fis = new FileInputStream(name)) {\n");
		buf.append("            bar(1);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");

		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 3, 1); //quick fix on 2nd problem
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);


		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileInputStream;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("class MyException extends Exception {\n");
		buf.append("    static final long serialVersionUID = 1L;\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(int n) throws IllegalArgumentException, MyException {\n");
		buf.append("        if (n == 1)\n");
		buf.append("            throw new IllegalArgumentException();\n");
		buf.append("        else\n");
		buf.append("            throw new MyException();\n");
		buf.append("    }\n");
		buf.append("    void foo(String name, boolean b) throws FileNotFoundException, IOException {\n");
		buf.append("        try (FileInputStream fis = new FileInputStream(name)) {\n");
		buf.append("            bar(1);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileInputStream;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("class MyException extends Exception {\n");
		buf.append("    static final long serialVersionUID = 1L;\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(int n) throws IllegalArgumentException, MyException {\n");
		buf.append("        if (n == 1)\n");
		buf.append("            throw new IllegalArgumentException();\n");
		buf.append("        else\n");
		buf.append("            throw new MyException();\n");
		buf.append("    }\n");
		buf.append("    void foo(String name, boolean b) {\n");
		buf.append("        try (FileInputStream fis = new FileInputStream(name)) {\n");
		buf.append("            bar(1);\n");
		buf.append("        } catch (FileNotFoundException e) {\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileInputStream;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("class MyException extends Exception {\n");
		buf.append("    static final long serialVersionUID = 1L;\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(int n) throws IllegalArgumentException, MyException {\n");
		buf.append("        if (n == 1)\n");
		buf.append("            throw new IllegalArgumentException();\n");
		buf.append("        else\n");
		buf.append("            throw new MyException();\n");
		buf.append("    }\n");
		buf.append("    void foo(String name, boolean b) {\n");
		buf.append("        try (FileInputStream fis = new FileInputStream(name)) {\n");
		buf.append("            bar(1);\n");
		buf.append("        } catch (FileNotFoundException | IOException e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	public void testUncaughtExceptionTryWithResources3() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileInputStream;\n");
		buf.append("class MyException extends Exception {\n");
		buf.append("    static final long serialVersionUID = 1L;\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(int n) throws IllegalArgumentException, MyException {\n");
		buf.append("        if (n == 1)\n");
		buf.append("            throw new IllegalArgumentException();\n");
		buf.append("        else\n");
		buf.append("            throw new MyException();\n");
		buf.append("    }\n");
		buf.append("    void foo(String name, boolean b) {\n");
		buf.append("        try (FileInputStream fis = new FileInputStream(name)) {\n");
		buf.append("            bar(1);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");

		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 3, 2); //quick fix on 3rd problem
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);


		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileInputStream;\n");
		buf.append("class MyException extends Exception {\n");
		buf.append("    static final long serialVersionUID = 1L;\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(int n) throws IllegalArgumentException, MyException {\n");
		buf.append("        if (n == 1)\n");
		buf.append("            throw new IllegalArgumentException();\n");
		buf.append("        else\n");
		buf.append("            throw new MyException();\n");
		buf.append("    }\n");
		buf.append("    void foo(String name, boolean b) throws IllegalArgumentException, MyException {\n");
		buf.append("        try (FileInputStream fis = new FileInputStream(name)) {\n");
		buf.append("            bar(1);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileInputStream;\n");
		buf.append("class MyException extends Exception {\n");
		buf.append("    static final long serialVersionUID = 1L;\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(int n) throws IllegalArgumentException, MyException {\n");
		buf.append("        if (n == 1)\n");
		buf.append("            throw new IllegalArgumentException();\n");
		buf.append("        else\n");
		buf.append("            throw new MyException();\n");
		buf.append("    }\n");
		buf.append("    void foo(String name, boolean b) {\n");
		buf.append("        try (FileInputStream fis = new FileInputStream(name)) {\n");
		buf.append("            bar(1);\n");
		buf.append("        } catch (IllegalArgumentException e) {\n");
		buf.append("        } catch (MyException e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileInputStream;\n");
		buf.append("class MyException extends Exception {\n");
		buf.append("    static final long serialVersionUID = 1L;\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(int n) throws IllegalArgumentException, MyException {\n");
		buf.append("        if (n == 1)\n");
		buf.append("            throw new IllegalArgumentException();\n");
		buf.append("        else\n");
		buf.append("            throw new MyException();\n");
		buf.append("    }\n");
		buf.append("    void foo(String name, boolean b) {\n");
		buf.append("        try (FileInputStream fis = new FileInputStream(name)) {\n");
		buf.append("            bar(1);\n");
		buf.append("        } catch (IllegalArgumentException | MyException e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(3);
		String preview4= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileInputStream;\n");
		buf.append("class MyException extends Exception {\n");
		buf.append("    static final long serialVersionUID = 1L;\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(int n) throws IllegalArgumentException, MyException {\n");
		buf.append("        if (n == 1)\n");
		buf.append("            throw new IllegalArgumentException();\n");
		buf.append("        else\n");
		buf.append("            throw new MyException();\n");
		buf.append("    }\n");
		buf.append("    void foo(String name, boolean b) {\n");
		buf.append("        try (FileInputStream fis = new FileInputStream(name)) {\n");
		buf.append("            try {\n");
		buf.append("                bar(1);\n");
		buf.append("            } catch (IllegalArgumentException e) {\n");
		buf.append("            } catch (MyException e) {\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected4= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(4);
		String preview5= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileInputStream;\n");
		buf.append("class MyException extends Exception {\n");
		buf.append("    static final long serialVersionUID = 1L;\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void bar(int n) throws IllegalArgumentException, MyException {\n");
		buf.append("        if (n == 1)\n");
		buf.append("            throw new IllegalArgumentException();\n");
		buf.append("        else\n");
		buf.append("            throw new MyException();\n");
		buf.append("    }\n");
		buf.append("    void foo(String name, boolean b) {\n");
		buf.append("        try (FileInputStream fis = new FileInputStream(name)) {\n");
		buf.append("            try {\n");
		buf.append("                bar(1);\n");
		buf.append("            } catch (IllegalArgumentException | MyException e) {\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected5= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4, preview5 }, new String[] { expected1, expected2, expected3, expected4, expected5 });
	}

	public void testUnneededCaughtException1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        try {\n");
		buf.append("            throw new FileNotFoundException();\n");
		buf.append("        } catch (FileNotFoundException | IOException e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);


		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        try {\n");
		buf.append("            throw new FileNotFoundException();\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() throws FileNotFoundException {\n");
		buf.append("        try {\n");
		buf.append("            throw new FileNotFoundException();\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	public void testUnneededCaughtException2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        try {\n");
		buf.append("            throw new FileNotFoundException();\n");
		buf.append("        } catch (java.io.FileNotFoundException | java.io.IOException e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);


		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        try {\n");
		buf.append("            throw new FileNotFoundException();\n");
		buf.append("        } catch (java.io.IOException e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() throws java.io.FileNotFoundException {\n");
		buf.append("        try {\n");
		buf.append("            throw new FileNotFoundException();\n");
		buf.append("        } catch (java.io.IOException e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	public void testUnneededCatchBlockTryWithResources() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileReader;\n");
		buf.append("class MyException extends Exception {\n");
		buf.append("    static final long serialVersionUID = 1L;\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void foo() throws Exception {\n");
		buf.append("        try (FileReader reader1 = new FileReader(\"file\")) {\n");
		buf.append("            int ch;\n");
		buf.append("            while ((ch = reader1.read()) != -1) {\n");
		buf.append("                System.out.println(ch);\n");
		buf.append("            }\n");
		buf.append("        } catch (MyException e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);


		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileReader;\n");
		buf.append("class MyException extends Exception {\n");
		buf.append("    static final long serialVersionUID = 1L;\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void foo() throws Exception {\n");
		buf.append("        try (FileReader reader1 = new FileReader(\"file\")) {\n");
		buf.append("            int ch;\n");
		buf.append("            while ((ch = reader1.read()) != -1) {\n");
		buf.append("                System.out.println(ch);\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileReader;\n");
		buf.append("class MyException extends Exception {\n");
		buf.append("    static final long serialVersionUID = 1L;\n");
		buf.append("}\n");
		buf.append("public class E {\n");
		buf.append("    void foo() throws Exception {\n");
		buf.append("        try (FileReader reader1 = new FileReader(\"file\")) {\n");
		buf.append("            int ch;\n");
		buf.append("            while ((ch = reader1.read()) != -1) {\n");
		buf.append("                System.out.println(ch);\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

}

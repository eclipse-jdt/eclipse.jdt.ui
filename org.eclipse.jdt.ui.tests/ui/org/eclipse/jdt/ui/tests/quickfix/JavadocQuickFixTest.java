/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
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

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal;

public class JavadocQuickFixTest extends QuickFixTest {
	
	private static final Class THIS= JavadocQuickFixTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public JavadocQuickFixTest(String name) {
		super(name);
	}


	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}
	
	public static Test suite() {
		if (false) {
			return allTests();
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new JavadocQuickFixTest("testInsertAllMissing2"));
			return new ProjectTestSetup(suite);
		}
	}


	protected void setUp() throws Exception {
		Hashtable options= JavaCore.getDefaultOptions();
		options.put(JavaCore.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(JavaCore.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.COMPILER_PB_INVALID_ANNOTATION, JavaCore.ERROR);
		options.put("org.eclipse.jdt.core.compiler.problem.missingAnnotation", JavaCore.ENABLED);
		JavaCore.setOptions(options);			

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
		
		fJProject1= ProjectTestSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}

	
	public void testMissingParam1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param b\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @param c\n");
		buf.append("     */\n");
		buf.append("    public void foo(int a, int b, int c) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a\n");
		buf.append("     * @param b\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @param c\n");
		buf.append("     */\n");
		buf.append("    public void foo(int a, int b, int c) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
		assertEqualString(preview2, expected);
	}
	
	public void testMissingParam2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @param c\n");
		buf.append("     */\n");
		buf.append("    public void foo(int a, int b, int c) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @param b\n");
		buf.append("     * @param c\n");
		buf.append("     */\n");
		buf.append("    public void foo(int a, int b, int c) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
		assertEqualString(preview2, expected);
	}
	public void testMissingParam3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @param b\n");
		buf.append("     */\n");
		buf.append("    public void foo(int a, int b, int c) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @param b\n");
		buf.append("     * @param c\n");
		buf.append("     */\n");
		buf.append("    public void foo(int a, int b, int c) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
		assertEqualString(preview2, expected);
	}
	
	public void testMissingReturn1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param b\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @param c\n");
		buf.append("     */\n");
		buf.append("    public int foo(int b, int c) {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param b\n");
		buf.append("     *      comment on second line.\n");
		buf.append("     * @param c\n");
		buf.append("     * @return\n");
		buf.append("     */\n");
		buf.append("    public int foo(int b, int c) {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
		assertEqualString(preview2, expected);
	}
	
	public void testMissingReturn2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     */\n");
		buf.append("    public int foo() {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @return\n");
		buf.append("     */\n");
		buf.append("    public int foo() {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
		assertEqualString(preview2, expected);
	}
	
	public void testMissingReturn3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @throws Exception\n");
		buf.append("     */\n");
		buf.append("    public int foo() throws Exception {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		ArrayList proposals= collectCorrections2(cu, 1);
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @return\n");
		buf.append("     * @throws Exception\n");
		buf.append("     */\n");
		buf.append("    public int foo() throws Exception {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview1, expected);
		assertEqualString(preview2, expected);
	}
	
	public void testInsertAllMissing1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @throws Exception\n");
		buf.append("     */\n");
		buf.append("    public int foo(int a, int b) throws NullPointerException, Exception {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		ArrayList proposals= collectCorrections2(cu, 4);
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a\n");
		buf.append("     * @param b\n");
		buf.append("     * @return\n");
		buf.append("     * @throws NullPointerException\n");
		buf.append("     * @throws Exception\n");
		buf.append("     */\n");
		buf.append("    public int foo(int a, int b) throws NullPointerException, Exception {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @return\n");
		buf.append("     * @throws Exception\n");
		buf.append("     */\n");
		buf.append("    public int foo(int a, int b) throws NullPointerException, Exception {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();		
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });	
		
	}
	
	public void testInsertAllMissing2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param b\n");
		buf.append("     * @return\n");
		buf.append("     */\n");
		buf.append("    public int foo(int a, int b, int c) throws NullPointerException, Exception {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		ArrayList proposals= collectCorrections2(cu, 4);
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a\n");
		buf.append("     * @param b\n");
		buf.append("     * @param c\n");
		buf.append("     * @return\n");
		buf.append("     * @throws NullPointerException\n");
		buf.append("     * @throws Exception\n");
		buf.append("     */\n");
		buf.append("    public int foo(int a, int b, int c) throws NullPointerException, Exception {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param a\n");
		buf.append("     * @param b\n");
		buf.append("     * @return\n");
		buf.append("     */\n");
		buf.append("    public int foo(int a, int b, int c) throws NullPointerException, Exception {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();		
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });	
		
	}
	
}

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
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.corext.template.java.CodeTemplates;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.NewMethodCompletionProposal;

public class UnresolvedMethodsQuickFixTest extends QuickFixTest {
	private static final Class THIS= UnresolvedMethodsQuickFixTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public UnresolvedMethodsQuickFixTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}
	
	public static Test suite() {
		if (true) {
			return allTests();
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new UnresolvedMethodsQuickFixTest("testParameterMismatchMoreArguments4"));
			return new ProjectTestSetup(suite);
		}
	}


	protected void setUp() throws Exception {
		Hashtable options= JavaCore.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.COMPILER_PB_NO_EFFECT_ASSIGNMENT, JavaCore.IGNORE);
		JavaCore.setOptions(options);			

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
		
		fJProject1= ProjectTestSetup.getProject();
		
		CodeTemplates.getCodeTemplate(CodeTemplates.CATCHBLOCK).setPattern("");
		CodeTemplates.getCodeTemplate(CodeTemplates.CONSTRUCTORSTUB).setPattern("");
		CodeTemplates.getCodeTemplate(CodeTemplates.METHODSTUB).setPattern("");		

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}

	
	public void testMethodInSameType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        int i= goo(vec, true);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		NewMethodCompletionProposal proposal= (NewMethodCompletionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        int i= goo(vec, true);\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private int goo(Vector vec, boolean b) {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
		
	public void testMethodInForInit() throws Exception {		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        for (int i= 0, j= goo(3); i < 0; i++) {\n");
		buf.append("        }\n");	
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		NewMethodCompletionProposal proposal= (NewMethodCompletionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        for (int i= 0, j= goo(3); i < 0; i++) {\n");
		buf.append("        }\n");	
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private int goo(int i) {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testMethodInInfixExpression1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private boolean foo() {\n");
		buf.append("        return f(1) || f(2);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		NewMethodCompletionProposal proposal= (NewMethodCompletionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private boolean foo() {\n");
		buf.append("        return f(1) || f(2);\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private boolean f(int i) {\n");
		buf.append("        return false;\n");
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testMethodInInfixExpression2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private boolean foo() {\n");
		buf.append("        return f(1) == f(2);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		NewMethodCompletionProposal proposal= (NewMethodCompletionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private boolean foo() {\n");
		buf.append("        return f(1) == f(2);\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private Object f(int i) {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}		

	public void testMethodSpacing0EmptyLines() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    void fred() {\n");
		buf.append("    }\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        int i= goo(vec, true);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		NewMethodCompletionProposal proposal= (NewMethodCompletionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    void fred() {\n");
		buf.append("    }\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        int i= goo(vec, true);\n");
		buf.append("    }\n");
		buf.append("    private int goo(Vector vec, boolean b) {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testMethodSpacing1EmptyLine() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    void fred() {\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        int i= goo(vec, true);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		NewMethodCompletionProposal proposal= (NewMethodCompletionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    void fred() {\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        int i= goo(vec, true);\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private int goo(Vector vec, boolean b) {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testMethodSpacing2EmptyLines() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    void fred() {\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    \n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        int i= goo(vec, true);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		NewMethodCompletionProposal proposal= (NewMethodCompletionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    void fred() {\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    \n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        int i= goo(vec, true);\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("\n");
		buf.append("    private int goo(Vector vec, boolean b) {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testMethodSpacingComment() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    void fred() {\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("//comment\n");
		buf.append("\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        int i= goo(vec, true);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		NewMethodCompletionProposal proposal= (NewMethodCompletionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    void fred() {\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("//comment\n");
		buf.append("\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        int i= goo(vec, true);\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private int goo(Vector vec, boolean b) {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testMethodSpacingJavadoc() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    void fred() {\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    /**\n");
		buf.append("     * javadoc\n");
		buf.append("     */\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        int i= goo(vec, true);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		NewMethodCompletionProposal proposal= (NewMethodCompletionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    void fred() {\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    /**\n");
		buf.append("     * javadoc\n");
		buf.append("     */\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        int i= goo(vec, true);\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private int goo(Vector vec, boolean b) {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testMethodSpacingNonJavadoc() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    void fred() {\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    /*\n");
		buf.append("     * non javadoc\n");
		buf.append("     */\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        int i= goo(vec, true);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		NewMethodCompletionProposal proposal= (NewMethodCompletionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    void fred() {\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    /*\n");
		buf.append("     * non javadoc\n");
		buf.append("     */\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        int i= goo(vec, true);\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private int goo(Vector vec, boolean b) {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testMethodInSameTypeUsingThis() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        int i= this.goo(vec, true);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		NewMethodCompletionProposal proposal= (NewMethodCompletionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        int i= this.goo(vec, true);\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private int goo(Vector vec, boolean b) {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testMethodInDifferentClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(X x) {\n");
		buf.append("        boolean i= x.goo(1, 2.1);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class X {\n");
		buf.append("}\n");
		pack1.createCompilationUnit("X.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		NewMethodCompletionProposal proposal= (NewMethodCompletionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class X {\n");
		buf.append("\n");
		buf.append("    public boolean goo(int i, double d) {\n");
		buf.append("        return false;\n");
		buf.append("    }\n");				
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testMethodInAnonymous1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
				
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                xoo();\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                xoo();\n");
		buf.append("            }\n");
		buf.append("\n");
		buf.append("            private void xoo() {\n");
		buf.append("            }\n");		
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                xoo();\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    protected void xoo() {\n");
		buf.append("    }\n");		
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= proposal.getCompilationUnitChange().getPreviewContent();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                foo();\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

				
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });		
	}
	
	public void testMethodInAnonymous2() throws Exception {
		IPackageFragment pack0= fSourceFolder.createPackageFragment("other", false, null);
		
		StringBuffer buf= new StringBuffer();
		buf= new StringBuffer();
		buf.append("package other;\n");
		buf.append("public class A {\n");
		buf.append("}\n");
		pack0.createCompilationUnit("A.java", buf.toString(), false, null);


		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import other.A;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                A.xoo();\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package other;\n");
		buf.append("public class A {\n");
		buf.append("\n");
		buf.append("    public static void xoo() {\n");
		buf.append("    }\n");		
		buf.append("}\n");
		String expected1= buf.toString();
		
		assertEqualString(preview1, expected1);			
	}
	
	public void testMethodInAnonymousCovering1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void run(int i) {\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                run(1);\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void run(int i) {\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                E.this.run(1);\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void run(int i) {\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                run();\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void run(int i) {\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run(int i) {\n");
		buf.append("                run(1);\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void run(int i) {\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                run(1);\n");
		buf.append("            }\n");
		buf.append("\n");
		buf.append("            private void run(int i) {\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected4= buf.toString();		

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });
	}
	
	public void testMethodInAnonymousCovering2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public static void run(int i) {\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                run(1);\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public static void run(int i) {\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                E.run(1);\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public static void run(int i) {\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                run();\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public static void run(int i) {\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run(int i) {\n");
		buf.append("                run(1);\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public static void run(int i) {\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                run(1);\n");
		buf.append("            }\n");
		buf.append("\n");
		buf.append("            private void run(int i) {\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected4= buf.toString();		

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });
	}
	
	public void testMethodInAnonymousCovering3() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void run(int i) {\n");
		buf.append("    }\n");
		buf.append("    public class Inner {\n");
		buf.append("        public void run() {\n");
		buf.append("            run(1);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void run(int i) {\n");
		buf.append("    }\n");
		buf.append("    public class Inner {\n");
		buf.append("        public void run() {\n");
		buf.append("            E.this.run(1);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void run(int i) {\n");
		buf.append("    }\n");
		buf.append("    public class Inner {\n");
		buf.append("        public void run() {\n");
		buf.append("            run();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void run(int i) {\n");
		buf.append("    }\n");
		buf.append("    public class Inner {\n");
		buf.append("        public void run(int i) {\n");
		buf.append("            run(1);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void run(int i) {\n");
		buf.append("    }\n");
		buf.append("    public class Inner {\n");
		buf.append("        public void run() {\n");
		buf.append("            run(1);\n");
		buf.append("        }\n");
		buf.append("\n");
		buf.append("        private void run(int i) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected4= buf.toString();		

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });
	}
	
	public void testMethodInAnonymousCovering4() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void run(int i) {\n");
		buf.append("    }\n");
		buf.append("    public static class Inner {\n");
		buf.append("        public void run() {\n");
		buf.append("            run(1);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void run(int i) {\n");
		buf.append("    }\n");
		buf.append("    public static class Inner {\n");
		buf.append("        public void run() {\n");
		buf.append("            run(1);\n");
		buf.append("        }\n");
		buf.append("\n");
		buf.append("        private void run(int i) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void run(int i) {\n");
		buf.append("    }\n");
		buf.append("    public static class Inner {\n");
		buf.append("        public void run() {\n");
		buf.append("            run();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void run(int i) {\n");
		buf.append("    }\n");
		buf.append("    public static class Inner {\n");
		buf.append("        public void run(int i) {\n");
		buf.append("            run(1);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}	
	
		
	
	
	public void testMethodInDifferentInterface() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(X x) {\n");
		buf.append("        boolean i= x.goo(getClass());\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface X {\n");
		buf.append("}\n");
		pack1.createCompilationUnit("X.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		NewMethodCompletionProposal proposal= (NewMethodCompletionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface X {\n");
		buf.append("\n");
		buf.append("    boolean goo(Class class1);\n");				
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testParameterMismatchCast() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
				
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i) {\n");
		buf.append("        long x= 0;\n");
		buf.append("        foo(x + 1);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i) {\n");
		buf.append("        long x= 0;\n");
		buf.append("        foo((int) (x + 1));\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i) {\n");
		buf.append("        long x= 0;\n");
		buf.append("        foo(x + 1);\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void foo(long l) {\n");
		buf.append("    }\n");		
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(long l) {\n");
		buf.append("        long x= 0;\n");
		buf.append("        foo(x + 1);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });		
	}
	
	public void testParameterMismatchCast2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
				
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i) {\n");
		buf.append("        double x= 0.0;\n");
		buf.append("        X.xoo((float) x, this);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class X {\n");
		buf.append("    public static void xoo(int i, Object o) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("X.java", buf.toString(), false, null);				

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i) {\n");
		buf.append("        double x= 0.0;\n");
		buf.append("        X.xoo((int) x, this);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class X {\n");
		buf.append("    public static void xoo(int i, Object o) {\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    public static void xoo(float f, E e) {\n");
		buf.append("    }\n");		
		buf.append("}\n");		
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class X {\n");
		buf.append("    public static void xoo(float f, Object o) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });		
	}
	
	
	public void testParameterMismatchLessArguments() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
				
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String s, int i, Object o) {\n");
		buf.append("        int x= 0;\n");
		buf.append("        foo(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String s, int i, Object o) {\n");
		buf.append("        int x= 0;\n");
		buf.append("        foo(s, x, o);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String s, int i, Object o) {\n");
		buf.append("        int x= 0;\n");
		buf.append("        foo(x);\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void foo(int x) {\n");
		buf.append("    }\n");		
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i) {\n");
		buf.append("        int x= 0;\n");
		buf.append("        foo(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });		
	}
	
	public void testParameterMismatchLessArguments2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
				
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        X.xoo(null);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class X {\n");
		buf.append("    public static void xoo(int i, Object o) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("X.java", buf.toString(), false, null);		
		

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        X.xoo(0, null);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class X {\n");
		buf.append("    public static void xoo(int i, Object o) {\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    public static void xoo(Object object) {\n");
		buf.append("    }\n");		
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class X {\n");
		buf.append("    public static void xoo(Object o) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });		
	}
	
	public void testSuperConstructorLessArguments() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
				
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class X {\n");
		buf.append("    public X(Object o, int i) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("X.java", buf.toString(), false, null);		

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E extends X {\n");
		buf.append("    public E() {\n");
		buf.append("        super(new Vector());\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E extends X {\n");
		buf.append("    public E() {\n");
		buf.append("        super(new Vector(), 0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class X {\n");
		buf.append("    public X(Object o, int i) {\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    public X(Vector vector) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class X {\n");
		buf.append("    public X(Object o) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });		
	}
	
	public void testConstructorInvocationLessArguments() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
				
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    public E(Object o, int i) {\n");
		buf.append("    }\n");
		buf.append("    public E() {\n");
		buf.append("        this(new Vector());\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    public E(Object o, int i) {\n");
		buf.append("    }\n");
		buf.append("    public E() {\n");
		buf.append("        this(new Vector(), 0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    public E(Object o) {\n");
		buf.append("    }\n");
		buf.append("    public E() {\n");
		buf.append("        this(new Vector());\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    public E(Object o, int i) {\n");
		buf.append("    }\n");
		buf.append("    public E() {\n");
		buf.append("        this(new Vector());\n");
		buf.append("    }\n");
		buf.append("    public E(Vector vector) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });		
	}	
	
	
	public void testParameterMismatchMoreArguments() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
				
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(X x) {\n");
		buf.append("        x.xoo(1, 1, x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class X {\n");
		buf.append("    public void xoo(int i, Object o) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("X.java", buf.toString(), false, null);		

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(X x) {\n");
		buf.append("        x.xoo(1, x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class X {\n");
		buf.append("    public void xoo(int i, int j, Object o) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class X {\n");
		buf.append("    public void xoo(int i, Object o) {\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    public void xoo(int i, int j, X x) {\n");
		buf.append("    }\n");		
		buf.append("}\n");
		String expected3= buf.toString();

		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });		
	}
	
	public void testParameterMismatchMoreArguments2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
				
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String s) {\n");
		buf.append("        int x= 0;\n");
		buf.append("        foo(s, x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String s) {\n");
		buf.append("        int x= 0;\n");
		buf.append("        foo(s);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String s) {\n");
		buf.append("        int x= 0;\n");
		buf.append("        foo(s, x);\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void foo(String s, int x) {\n");
		buf.append("    }\n");		
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String s, int x2) {\n");
		buf.append("        int x= 0;\n");
		buf.append("        foo(s, x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });		
	}
	
	public void testParameterMismatchMoreArguments3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
				
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Collections;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(X x) {\n");
		buf.append("        x.xoo(Collections.EMPTY_SET, 1, 2);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class X {\n");
		buf.append("    public void xoo(int i) {\n");
		buf.append("       int j= 0;\n");		
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("X.java", buf.toString(), false, null);		

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Collections;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(X x) {\n");
		buf.append("        x.xoo(1);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");	
		buf.append("import java.util.Set;\n");
		buf.append("\n");	
		buf.append("public class X {\n");
		buf.append("    public void xoo(Set set, int i, int k) {\n");
		buf.append("       int j= 0;\n");		
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");	
		buf.append("import java.util.Set;\n");
		buf.append("\n");	
		buf.append("public class X {\n");
		buf.append("    public void xoo(int i) {\n");
		buf.append("       int j= 0;\n");		
		buf.append("    }\n");
		buf.append("\n");		
		buf.append("    public void xoo(Set set, int i, int j) {\n");
		buf.append("    }\n");		
		buf.append("}\n");
		String expected3= buf.toString();

		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });		
	}
	
	public void testSuperConstructorMoreArguments() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
				
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class X {\n");
		buf.append("    public X() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("X.java", buf.toString(), false, null);		

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E extends X {\n");
		buf.append("    public E() {\n");
		buf.append("        super(new Vector());\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E extends X {\n");
		buf.append("    public E() {\n");
		buf.append("        super();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class X {\n");
		buf.append("    public X() {\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    public X(Vector vector) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class X {\n");
		buf.append("    public X(Vector vector) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });		
	}
	
	public void testConstructorInvocationMoreArguments() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
				
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    public E() {\n");
		buf.append("    }\n");
		buf.append("    public E(Object o, int i) {\n");
		buf.append("        this(new Vector());\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    public E() {\n");
		buf.append("    }\n");
		buf.append("    public E(Object o, int i) {\n");
		buf.append("        this();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    public E(Vector vector) {\n");
		buf.append("    }\n");
		buf.append("    public E(Object o, int i) {\n");
		buf.append("        this(new Vector());\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    public E() {\n");
		buf.append("    }\n");
		buf.append("    public E(Object o, int i) {\n");
		buf.append("        this(new Vector());\n");
		buf.append("    }\n");
		buf.append("    public E(Vector vector) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });		
	}	
		
	
	
	
	public void testParameterMismatchSwap() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
				
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i, Object o) {\n");
		buf.append("        foo(new Object[] { o }, i - 1);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i, Object o) {\n");
		buf.append("        foo(i - 1, new Object[] { o });\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i, Object o) {\n");
		buf.append("        foo(new Object[] { o }, i - 1);\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void foo(Object[] objects, int i) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object o, int i) {\n");
		buf.append("        foo(new Object[] { o }, i - 1);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();		
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });		
	}		
	
	
	
	public void testSuperConstructor() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends A {\n");
		buf.append("    public E(int i) {\n");
		buf.append("        super(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1=pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("}\n");
		pack1.createCompilationUnit("A.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("\n");
		buf.append("    public A(int i) {\n");
		buf.append("    }\n");		
		buf.append("}\n");
		String expected1= buf.toString();	

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends A {\n");
		buf.append("    public E(int i) {\n");
		buf.append("        super();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();		
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		

	}
	
	
	public void testClassInstanceCreation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i) {\n");
		buf.append("        A a= new A(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1=pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("}\n");
		pack1.createCompilationUnit("A.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("\n");
		buf.append("    public A(int i) {\n");
		buf.append("    }\n");		
		buf.append("}\n");
		String expected1= buf.toString();	

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i) {\n");
		buf.append("        A a= new A();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();		
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		
	}
	
	public void testClassInstanceCreationMoreArguments() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i) {\n");
		buf.append("        A a= new A(i, String.valueOf(i), true);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1=pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public A(int i) {\n");
		buf.append("    }\n");		
		buf.append("}\n");
		pack1.createCompilationUnit("A.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public A(int i, String string, boolean b) {\n");
		buf.append("    }\n");		
		buf.append("}\n");
		String expected1= buf.toString();	

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i) {\n");
		buf.append("        A a= new A(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public A(int i) {\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    public A(int i, String string, boolean b) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();			
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });		
	}	

	public void testClassInstanceCreationLessArguments() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i) {\n");
		buf.append("        A a= new A();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1=pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public A(int i, String s) {\n");
		buf.append("    }\n");		
		buf.append("}\n");
		pack1.createCompilationUnit("A.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public A() {\n");
		buf.append("    }\n");		
		buf.append("}\n");
		String expected1= buf.toString();	

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i) {\n");
		buf.append("        A a= new A(i, null);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public A(int i, String s) {\n");
		buf.append("    }\n");	
		buf.append("\n");
		buf.append("    public A() {\n");
		buf.append("    }\n");	
		buf.append("}\n");
		String expected3= buf.toString();			
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });		
	}		
	
	
	public void testConstructorInvocation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public E(int i) {\n");
		buf.append("        this(i, true);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1=pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public E(int i) {\n");
		buf.append("        this(i, true);\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    public E(int i, boolean b) {\n");
		buf.append("    }\n");		
		buf.append("}\n");
		String expected1= buf.toString();
				
		assertEqualString(preview1, expected1);
	}
		
	public void testSuperMethodInvocation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends A {\n");
		buf.append("    public void foo(int i) {\n");
		buf.append("        super.foo(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1=pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("}\n");
		pack1.createCompilationUnit("A.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		NewMethodCompletionProposal proposal= (NewMethodCompletionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("\n");
		buf.append("    public void foo(int i) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

	}
	
	public void testSuperMethodMoreArguments() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
				
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class X {\n");
		buf.append("    public int foo() {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("X.java", buf.toString(), false, null);		

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E extends X {\n");
		buf.append("    public void xoo() {\n");
		buf.append("        super.foo(new Vector());\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E extends X {\n");
		buf.append("    public void xoo() {\n");
		buf.append("        super.foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class X {\n");
		buf.append("    public int foo(Vector vector) {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class X {\n");
		buf.append("    public int foo() {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    public void foo(Vector vector) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });		
	}
	
	public void testSuperMethodLessArguments() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
				
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class X {\n");
		buf.append("    public int foo(Object o, boolean b) {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("X.java", buf.toString(), false, null);		

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E extends X {\n");
		buf.append("    public void xoo() {\n");
		buf.append("        super.foo(new Vector());\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E extends X {\n");
		buf.append("    public void xoo() {\n");
		buf.append("        super.foo(new Vector(), false);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class X {\n");
		buf.append("    public int foo(Object o) {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class X {\n");
		buf.append("    public int foo(Object o, boolean b) {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    public void foo(Vector vector) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });		
	}
	
	
	public void testMissingCastParents1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object o) {\n");
		buf.append("        String x= (String) o.substring(1);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object o) {\n");
		buf.append("        String x= ((String) o).substring(1);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();		

		assertEqualString(preview1, expected1);
	}
	
	public void testMissingCastParents2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object o) {\n");
		buf.append("        String x= (String) o.substring(1).toLowerCase();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object o) {\n");
		buf.append("        String x= ((String) o).substring(1).toLowerCase();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();		

		assertEqualString(preview1, expected1);
	}
	
	public void testMissingCastParents3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private static Object obj;\n");
		buf.append("    public void foo() {\n");
		buf.append("        String x= (String) E.obj.substring(1).toLowerCase();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private static Object obj;\n");
		buf.append("    public void foo() {\n");
		buf.append("        String x= ((String) E.obj).substring(1).toLowerCase();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();		

		assertEqualString(preview1, expected1);
	}

}

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

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.dom.TypeRules;
import org.eclipse.jdt.internal.corext.template.CodeTemplates;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionContext;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;
import org.eclipse.jdt.internal.ui.text.correction.NewMethodCompletionProposal;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.ui.PreferenceConstants;

public class UnresolvedMethodsQuickFixTest extends QuickFixTest {
	private static final Class THIS= UnresolvedMethodsQuickFixTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public UnresolvedMethodsQuickFixTest(String name) {
		super(name);
	}


	public static Test suite() {
		if (false) {
			return new TestSuite(THIS);
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new UnresolvedMethodsQuickFixTest("testMethodInForInit"));
			return suite;
		}
	}


	protected void setUp() throws Exception {
		Hashtable options= JavaCore.getDefaultOptions();
		options.put(JavaCore.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(JavaCore.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.COMPILER_PB_NO_EFFECT_ASSIGNMENT, JavaCore.IGNORE);
		JavaCore.setOptions(options);			

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
		
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		assertTrue("rt not found", JavaProjectHelper.addRTJar(fJProject1) != null);
		
		CodeTemplates.getCodeTemplate(CodeTemplates.CATCHBLOCK).setPattern("");
		CodeTemplates.getCodeTemplate(CodeTemplates.CONSTRUCTORSTUB).setPattern("");
		CodeTemplates.getCodeTemplate(CodeTemplates.METHODSTUB).setPattern("");		

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu, problems[0]);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
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
	
	private static final boolean bug_37381= true;
	
	public void testMethodInForInit() throws Exception {
		if (bug_37381) {
			return; 
		}
		
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu, problems[0]);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu, problems[0]);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu, problems[0]);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu, problems[0]);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu, problems[0]);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu, problems[0]);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu, problems[0]);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu, problems[0]);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu, problems[0]);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu, problems[0]);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
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

		CompilationUnit astRoot= AST.parseCompilationUnit(cu1, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu1, problems[0]);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 3);
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

		CompilationUnit astRoot= AST.parseCompilationUnit(cu1, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu1, problems[0]);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 3);
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

		CompilationUnit astRoot= AST.parseCompilationUnit(cu1, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu1, problems[0]);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String s, int i, Object o) {\n");
		buf.append("        int x= 0;\n");
		buf.append("        foo(null, x, null);\n");
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
		

		CompilationUnit astRoot= AST.parseCompilationUnit(cu1, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu1, problems[0]);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 3);
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

		CompilationUnit astRoot= AST.parseCompilationUnit(cu1, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu1, problems[0]);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 3);
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

		CompilationUnit astRoot= AST.parseCompilationUnit(cu1, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu1, problems[0]);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 3);
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

		CompilationUnit astRoot= AST.parseCompilationUnit(cu1, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu1, problems[0]);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 3);
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

		CompilationUnit astRoot= AST.parseCompilationUnit(cu1, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu1, problems[0]);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 3);
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu1, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu1, problems[0]);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
		assertCorrectLabels(proposals);

		NewMethodCompletionProposal proposal= (NewMethodCompletionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("\n");
		buf.append("    public A(int i) {\n");
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu1, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu1, problems[0]);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
		assertCorrectLabels(proposals);

		NewMethodCompletionProposal proposal= (NewMethodCompletionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("\n");
		buf.append("    public A(int i) {\n");
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu1, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu1, problems[0]);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 3);
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
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public E(int i, boolean b) {\n");
		buf.append("        this(i, true);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public E(int i) {\n");
		buf.append("        this(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();		
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });			
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu1, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu1, problems[0]);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context, proposals);
		assertNumberOf("proposals", proposals.size(), 1);
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
	
	public void testCanAssign() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("import java.util.Collection;\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("public class E {\n");
		buf.append("    boolean bool;\n");
		buf.append("    char c;\n");
		buf.append("    byte b;\n");
		buf.append("    short s;\n");
		buf.append("    int i;\n");
		buf.append("    long l;\n");
		buf.append("    float f;\n");
		buf.append("    double d;\n");
		buf.append("    Object object;\n");
		buf.append("    Vector vector;\n");
		buf.append("    Cloneable cloneable;\n");
		buf.append("    Collection collection;\n");
		buf.append("    Serializable serializable;\n");
		buf.append("    Object[] objectArr;\n");
		buf.append("    int[] int_arr;\n");
		buf.append("    long[] long_arr;\n");
		buf.append("    Vector[] vector_arr;\n");
		buf.append("    Collection[] collection_arr;\n");
		buf.append("    Object[][] objectArrArr;\n");
		buf.append("    Collection[][] collection_arrarr;\n");
		buf.append("    Vector[][] vector_arrarr;\n");
		buf.append("}\n");
		ICompilationUnit cu1=pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu1, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 0);
		
		TypeDeclaration type= findTypeDeclaration(astRoot, "E");
		VariableDeclarationFragment bool= findFieldDeclaration(type, "bool");
		VariableDeclarationFragment c= findFieldDeclaration(type, "c");
		VariableDeclarationFragment b= findFieldDeclaration(type, "b");
		VariableDeclarationFragment s= findFieldDeclaration(type, "s");
		VariableDeclarationFragment i= findFieldDeclaration(type, "i");
		VariableDeclarationFragment l= findFieldDeclaration(type, "l");
		VariableDeclarationFragment f= findFieldDeclaration(type, "f");
		VariableDeclarationFragment d= findFieldDeclaration(type, "d");		
		VariableDeclarationFragment object= findFieldDeclaration(type, "object");
		VariableDeclarationFragment vector= findFieldDeclaration(type, "vector");
		VariableDeclarationFragment cloneable= findFieldDeclaration(type, "cloneable");
		VariableDeclarationFragment collection= findFieldDeclaration(type, "collection");
		VariableDeclarationFragment serializable= findFieldDeclaration(type, "serializable");
		VariableDeclarationFragment objectArr= findFieldDeclaration(type, "objectArr");
		VariableDeclarationFragment int_arr= findFieldDeclaration(type, "int_arr");
		VariableDeclarationFragment long_arr= findFieldDeclaration(type, "long_arr");			
		VariableDeclarationFragment vector_arr= findFieldDeclaration(type, "vector_arr");
		VariableDeclarationFragment collection_arr= findFieldDeclaration(type, "collection_arr");
		VariableDeclarationFragment objectArrArr= findFieldDeclaration(type, "objectArrArr");
		VariableDeclarationFragment collection_arrarr= findFieldDeclaration(type, "collection_arrarr");
		VariableDeclarationFragment vector_arrarr= findFieldDeclaration(type, "vector_arrarr");
		
		VariableDeclarationFragment[] targets= new VariableDeclarationFragment[] { 
			bool, c, b, s, i, l, f, d, object, vector, cloneable, serializable, collection, objectArr, int_arr, long_arr,
			vector_arr, collection_arr, objectArrArr, collection_arrarr, vector_arrarr
		};
		for (int k= 0; k < targets.length; k++) {
			for (int n= 0; n < targets.length; n++) {
				VariableDeclarationFragment f1= targets[k];
				VariableDeclarationFragment f2= targets[n];
				String line= f2.getName().getIdentifier() + "= " + f1.getName().getIdentifier();
				
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("public class F extends E {\n");
				buf.append("    void foo() {\n");
				buf.append("        ").append(line).append(";\n");
				buf.append("    }\n");
				buf.append("}\n");
				char[] content= buf.toString().toCharArray();
				
				astRoot= AST.parseCompilationUnit(content, "F.java", cu1.getJavaProject());
				problems= astRoot.getProblems();

				ITypeBinding b1= f1.resolveBinding().getType();
				assertNotNull(b1);
				ITypeBinding b2= f2.resolveBinding().getType();
				assertNotNull(b2);
				boolean res= TypeRules.canAssign(b1, b2.getQualifiedName());
				assertEquals(line, problems.length == 0, res);
				boolean res2= TypeRules.canAssign(b1, b2);
				assertEquals(line, problems.length == 0, res2);			
			}	
		}
	}

}

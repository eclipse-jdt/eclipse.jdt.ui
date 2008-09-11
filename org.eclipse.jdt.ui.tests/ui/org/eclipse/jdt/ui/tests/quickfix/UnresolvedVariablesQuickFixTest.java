/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CUCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewVariableCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.RenameNodeCorrectionProposal;

public class UnresolvedVariablesQuickFixTest extends QuickFixTest {

	private static final Class THIS= UnresolvedVariablesQuickFixTest.class;

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public UnresolvedVariablesQuickFixTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return setUpTest(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}

	public static Test suite() {
		return allTests();
	}


	protected void setUp() throws Exception {
		Hashtable options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "1");
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.COMPILER_PB_UNUSED_IMPORT, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.IGNORE);
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= ProjectTestSetup.getProject();

		StubUtility.setCodeTemplate(CodeTemplateContextType.NEWTYPE_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, "", null);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}

	public void testVarInAssignment() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        iter= vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    private Iterator iter;\n");
		buf.append("\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        iter= vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        Iterator iter = vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector vec, Iterator iter) {\n");
		buf.append("        iter= vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected4= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });
	}

	public void testVarAssingmentInIfBody() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        if (vec != null)\n");
		buf.append("            iter= vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    private Iterator iter;\n");
		buf.append("\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        if (vec != null)\n");
		buf.append("            iter= vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        Iterator iter;\n");
		buf.append("        if (vec != null)\n");
		buf.append("            iter= vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector vec, Iterator iter) {\n");
		buf.append("        if (vec != null)\n");
		buf.append("            iter= vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        if (vec != null) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected4= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });
	}

	public void testVarAssingmentInThenBody() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        if (vec == null) {\n");
		buf.append("        } else\n");
		buf.append("            iter= vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    private Iterator iter;\n");
		buf.append("\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        if (vec == null) {\n");
		buf.append("        } else\n");
		buf.append("            iter= vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        Iterator iter;\n");
		buf.append("        if (vec == null) {\n");
		buf.append("        } else\n");
		buf.append("            iter= vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector vec, Iterator iter) {\n");
		buf.append("        if (vec == null) {\n");
		buf.append("        } else\n");
		buf.append("            iter= vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        if (vec == null) {\n");
		buf.append("        } else {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected4= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });
	}

	public void testVarInAssignmentWithGenerics() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector<String> vec) {\n");
		buf.append("        iter= vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    private Iterator<String> iter;\n");
		buf.append("\n");
		buf.append("    void foo(Vector<String> vec) {\n");
		buf.append("        iter= vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector<String> vec) {\n");
		buf.append("        Iterator<String> iter = vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector<String> vec, Iterator<String> iter) {\n");
		buf.append("        iter= vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector<String> vec) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected4= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });
	}

	public void testVarAssignedByWildcard1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector<?> vec) {\n");
		buf.append("        elem = vec.get(0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector<?> vec) {\n");
		buf.append("        Object elem = vec.get(0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	public void testVarAssignedByWildcard2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector<? super Number> vec) {\n");
		buf.append("        elem = vec.get(0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector<? super Number> vec) {\n");
		buf.append("        Object elem = vec.get(0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	public void testVarAssignedByWildcard3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector<? extends Number> vec) {\n");
		buf.append("        elem = vec.get(0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector<? extends Number> vec) {\n");
		buf.append("        Number elem = vec.get(0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	public void testVarAssignedToWildcard1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector<? super Number> vec) {\n");
		buf.append("        vec.add(elem);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector<? super Number> vec, Number elem) {\n");
		buf.append("        vec.add(elem);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	public void testVarAssignedToWildcard2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector<? extends Number> vec) {\n");
		buf.append("        vec.add(elem);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector<? extends Number> vec, Object elem) {\n");
		buf.append("        vec.add(elem);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	public void testVarAssignedToWildcard3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector<?> vec) {\n");
		buf.append("        vec.add(elem);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector<?> vec, Object elem) {\n");
		buf.append("        vec.add(elem);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	public void testVarAssingmentInIfBodyWithGenerics() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector<String> vec) {\n");
		buf.append("        if (vec != null)\n");
		buf.append("            iter= vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    private Iterator<String> iter;\n");
		buf.append("\n");
		buf.append("    void foo(Vector<String> vec) {\n");
		buf.append("        if (vec != null)\n");
		buf.append("            iter= vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector<String> vec) {\n");
		buf.append("        Iterator<String> iter;\n");
		buf.append("        if (vec != null)\n");
		buf.append("            iter= vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector<String> vec, Iterator<String> iter) {\n");
		buf.append("        if (vec != null)\n");
		buf.append("            iter= vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector<String> vec) {\n");
		buf.append("        if (vec != null) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected4= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });
	}

	public void testVarAssingmentInThenBodyWithGenerics() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector<String> vec) {\n");
		buf.append("        if (vec == null) {\n");
		buf.append("        } else\n");
		buf.append("            iter= vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    private Iterator<String> iter;\n");
		buf.append("\n");
		buf.append("    void foo(Vector<String> vec) {\n");
		buf.append("        if (vec == null) {\n");
		buf.append("        } else\n");
		buf.append("            iter= vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector<String> vec) {\n");
		buf.append("        Iterator<String> iter;\n");
		buf.append("        if (vec == null) {\n");
		buf.append("        } else\n");
		buf.append("            iter= vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector<String> vec, Iterator<String> iter) {\n");
		buf.append("        if (vec == null) {\n");
		buf.append("        } else\n");
		buf.append("            iter= vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector<String> vec) {\n");
		buf.append("        if (vec == null) {\n");
		buf.append("        } else {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected4= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });
	}


	public void testVarInVarArgs1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Arrays.<Number>asList(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[4];
		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("public class E {\n");
		buf.append("    private Number x;\n");
		buf.append("\n");
		buf.append("    public void foo() {\n");
		buf.append("        Arrays.<Number>asList(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("public class E {\n");
		buf.append("    private static final Number x = null;\n");
		buf.append("\n");
		buf.append("    public void foo() {\n");
		buf.append("        Arrays.<Number>asList(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Number x) {\n");
		buf.append("        Arrays.<Number>asList(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[2]= buf.toString();

		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Number x;\n");
		buf.append("        Arrays.<Number>asList(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[3]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testVarInVarArgs2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("import java.io.File;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String name) {\n");
		buf.append("        Arrays.<File>asList( new File(name), new XXX(name) );\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);

		String[] expected= new String[2];
		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("import java.io.File;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String name) {\n");
		buf.append("        Arrays.<File>asList( new File(name), new File(name) );\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("import java.io.File;\n");
		buf.append("\n");
		buf.append("public class XXX extends File {\n");
		buf.append("\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testVarInForInitializer() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        for (i= 0;;) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int i;\n");
		buf.append("\n");
		buf.append("    void foo() {\n");
		buf.append("        for (i= 0;;) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        for (int i = 0;;) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(int i) {\n");
		buf.append("        for (i= 0;;) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	public void testVarInForInitializer2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @return Returns a number\n");
		buf.append("     */\n");
		buf.append("    int foo() {\n");
		buf.append("        for (i= new int[] { 1 };;) {\n");
		buf.append("        }\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int[] i;\n");
		buf.append("\n");
		buf.append("    /**\n");
		buf.append("     * @return Returns a number\n");
		buf.append("     */\n");
		buf.append("    int foo() {\n");
		buf.append("        for (i= new int[] { 1 };;) {\n");
		buf.append("        }\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @return Returns a number\n");
		buf.append("     */\n");
		buf.append("    int foo() {\n");
		buf.append("        for (int[] i = new int[] { 1 };;) {\n");
		buf.append("        }\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param i \n");
		buf.append("     * @return Returns a number\n");
		buf.append("     */\n");
		buf.append("    int foo(int[] i) {\n");
		buf.append("        for (i= new int[] { 1 };;) {\n");
		buf.append("        }\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}


	public void testVarInInitializer() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int i= k;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();

		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int k;\n");
		buf.append("    private int i= k;\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private static final int k = 0;\n");
		buf.append("    private int i= k;\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	public void testVarInOtherType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("    void foo(E e) {\n");
		buf.append("         e.var2= 2;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("F.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    protected int var1;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();

		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    protected int var1;\n");
		buf.append("    public int var2;\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("    void foo(E e) {\n");
		buf.append("         e.var1= 2;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	public void testVarInSuperFieldAccess() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F extends E {\n");
		buf.append("    void foo() {\n");
		buf.append("         super.var2= 2;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("F.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    protected int var1;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F extends E {\n");
		buf.append("    void foo() {\n");
		buf.append("         super.var1= 2;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    protected int var1;\n");
		buf.append("    public int var2;\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	public void testVarInSuper() throws Exception {
		StringBuffer buf= new StringBuffer();

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test3.E;\n");
		buf.append("public class F extends E {\n");
		buf.append("    void foo() {\n");
		buf.append("         this.color= baz();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("F.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class E {\n");
		buf.append("}\n");
		pack2.createCompilationUnit("E.java", buf.toString(), false, null);

		IPackageFragment pack3= fSourceFolder.createPackageFragment("test3", false, null);
		buf= new StringBuffer();
		buf.append("package test3;\n");
		buf.append("public class E {\n");
		buf.append("    protected Object olor;\n");
		buf.append("    public test2.E baz() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack3.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test3.E;\n");
		buf.append("public class F extends E {\n");
		buf.append("    void foo() {\n");
		buf.append("         this.olor= baz();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test3.E;\n");
		buf.append("public class F extends E {\n");
		buf.append("    private test2.E color;\n");
		buf.append("\n");
		buf.append("    void foo() {\n");
		buf.append("         this.color= baz();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}


	public void testVarInAnonymous() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int fcount) {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                fCount= 7;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 6);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int fcount) {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            private int fCount;\n");
		buf.append("\n");
		buf.append("            public void run() {\n");
		buf.append("                fCount= 7;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    protected int fCount;\n");
		buf.append("\n");
		buf.append("    public void foo(int fcount) {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                fCount= 7;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int fcount) {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                int fCount = 7;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int fcount) {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run(int fCount) {\n");
		buf.append("                fCount= 7;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected4= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(4);
		String preview5= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int fcount) {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                fcount= 7;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected5= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(5);
		String preview6= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int fcount) {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected6= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4, preview5, preview6 }, new String[] { expected1, expected2, expected3, expected4, expected5, expected6 });
	}

	public void testVarInAnnotation1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("public class E {\n");
		buf.append("    public @interface Annot {\n");
		buf.append("        String value();\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    @Annot(x)\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("public class E {\n");
		buf.append("    public @interface Annot {\n");
		buf.append("        String value();\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private static final String x = null;\n");
		buf.append("    \n");
		buf.append("    @Annot(x)\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testVarInAnnotation2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("public class E {\n");
		buf.append("    public @interface Annot {\n");
		buf.append("        float value();\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    @Annot(value=x)\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("public class E {\n");
		buf.append("    public @interface Annot {\n");
		buf.append("        float value();\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private static final float x = 0;\n");
		buf.append("    \n");
		buf.append("    @Annot(value=x)\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testVarInAnnotation3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("public class E {\n");
		buf.append("    public @interface Annot {\n");
		buf.append("        float[] value();\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    @Annot(value={x})\n");
		buf.append("    class Inner {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 1);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("public class E {\n");
		buf.append("    public @interface Annot {\n");
		buf.append("        float[] value();\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private static final float x = 0;\n");
		buf.append("    \n");
		buf.append("    @Annot(value={x})\n");
		buf.append("    class Inner {\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}



	public void testLongVarRef() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("    public int mash;\n");
		buf.append("    void foo(E e) {\n");
		buf.append("         e.var.hash= 2;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("F.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public F var;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("    public int mash;\n");
		buf.append("    private int hash;\n");
		buf.append("    void foo(E e) {\n");
		buf.append("         e.var.hash= 2;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("    public int mash;\n");
		buf.append("    void foo(E e) {\n");
		buf.append("         e.var.mash= 2;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2}, new String[] { expected1, expected2});
	}

	public void testVarAndTypeRef() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.File;\n");
		buf.append("public class F {\n");
		buf.append("    void foo() {\n");
		buf.append("        char ch= Fixe.pathSeparatorChar;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("F.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertCorrectLabels(proposals);

		int i= 0;
		String[] expected= new String[proposals.size()];

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.File;\n");
		buf.append("public class F {\n");
		buf.append("    private Object Fixe;\n");
		buf.append("\n");
		buf.append("    void foo() {\n");
		buf.append("        char ch= Fixe.pathSeparatorChar;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[i++]= buf.toString();


		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.File;\n");
		buf.append("public class F {\n");
		buf.append("    void foo() {\n");
		buf.append("        Object Fixe;\n");
		buf.append("        char ch= Fixe.pathSeparatorChar;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[i++]= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.File;\n");
		buf.append("public class F {\n");
		buf.append("    void foo(Object Fixe) {\n");
		buf.append("        char ch= Fixe.pathSeparatorChar;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[i++]= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.File;\n");
		buf.append("public class F {\n");
		buf.append("    private static final String Fixe = null;\n");
		buf.append("\n");
		buf.append("    void foo() {\n");
		buf.append("        char ch= Fixe.pathSeparatorChar;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[i++]= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class Fixe {\n");
		buf.append("\n");
		buf.append("}\n");
		expected[i++]= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public interface Fixe {\n");
		buf.append("\n");
		buf.append("}\n");
		expected[i++]= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public enum Fixe {\n");
		buf.append("\n");
		buf.append("}\n");
		expected[i++]= buf.toString();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.File;\n");
		buf.append("public class F {\n");
		buf.append("    void foo() {\n");
		buf.append("        char ch= File.pathSeparatorChar;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[i++]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testVarWithGenericType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class F {\n");
		buf.append("    void foo(E e) {\n");
		buf.append("         e.var2= new ArrayList<String>();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("F.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    protected int var1;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();

		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    protected int var1;\n");
		buf.append("    public ArrayList<String> var2;\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class F {\n");
		buf.append("    void foo(E e) {\n");
		buf.append("         e.var1= new ArrayList<String>();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}


	public void testSimilarVariableNames1() throws Exception {
		StringBuffer buf= new StringBuffer();

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test3", false, null);
		buf= new StringBuffer();
		buf.append("package test3;\n");
		buf.append("public class E {\n");
		buf.append("    private static final short CON1= 1;\n");
		buf.append("    private static final float CON2= 1.0f;\n");
		buf.append("    private String bla;\n");
		buf.append("    private String cout;\n");
		buf.append("    public int foo() {\n");
		buf.append("        return count;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		for (int i= proposals.size() - 1; i >= 0; i--) {
			Object curr= proposals.get(i);
			if (!(curr instanceof RenameNodeCorrectionProposal)) {
				proposals.remove(i);
			}
		}

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test3;\n");
		buf.append("public class E {\n");
		buf.append("    private static final short CON1= 1;\n");
		buf.append("    private static final float CON2= 1.0f;\n");
		buf.append("    private String bla;\n");
		buf.append("    private String cout;\n");
		buf.append("    public int foo() {\n");
		buf.append("        return CON1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test3;\n");
		buf.append("public class E {\n");
		buf.append("    private static final short CON1= 1;\n");
		buf.append("    private static final float CON2= 1.0f;\n");
		buf.append("    private String bla;\n");
		buf.append("    private String cout;\n");
		buf.append("    public int foo() {\n");
		buf.append("        return cout;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	public void testSimilarVariableNames2() throws Exception {
		StringBuffer buf= new StringBuffer();

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test3", false, null);
		buf= new StringBuffer();
		buf.append("package test3;\n");
		buf.append("public class E {\n");
		buf.append("    private static final short CON1= 1;\n");
		buf.append("    private static final float CON2= 1.0f;\n");
		buf.append("    private static short var1= 1;\n");
		buf.append("    private static float var2= 1.0f;\n");
		buf.append("    private String bla;\n");
		buf.append("    private String cout;\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        count= x;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		for (int i= proposals.size() - 1; i >= 0; i--) {
			Object curr= proposals.get(i);
			if (!(curr instanceof RenameNodeCorrectionProposal)) {
				proposals.remove(i);
			}
		}

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test3;\n");
		buf.append("public class E {\n");
		buf.append("    private static final short CON1= 1;\n");
		buf.append("    private static final float CON2= 1.0f;\n");
		buf.append("    private static short var1= 1;\n");
		buf.append("    private static float var2= 1.0f;\n");
		buf.append("    private String bla;\n");
		buf.append("    private String cout;\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        var2= x;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test3;\n");
		buf.append("public class E {\n");
		buf.append("    private static final short CON1= 1;\n");
		buf.append("    private static final float CON2= 1.0f;\n");
		buf.append("    private static short var1= 1;\n");
		buf.append("    private static float var2= 1.0f;\n");
		buf.append("    private String bla;\n");
		buf.append("    private String cout;\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        cout= x;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	public void testSimilarVariableNamesMultipleOcc() throws Exception {
		StringBuffer buf= new StringBuffer();

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test3", false, null);
		buf= new StringBuffer();
		buf.append("package test3;\n");
		buf.append("public class E {\n");
		buf.append("    private int cout;\n");
		buf.append("    public void setCount(int x) {\n");
		buf.append("        count= x;\n");
		buf.append("        count++;\n");
		buf.append("    }\n");
		buf.append("    public int getCount() {\n");
		buf.append("        return count;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 3);
		for (int i= proposals.size() - 1; i >= 0; i--) {
			Object curr= proposals.get(i);
			if (!(curr instanceof RenameNodeCorrectionProposal)) {
				proposals.remove(i);
			}
		}

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test3;\n");
		buf.append("public class E {\n");
		buf.append("    private int cout;\n");
		buf.append("    public void setCount(int x) {\n");
		buf.append("        cout= x;\n");
		buf.append("        cout++;\n");
		buf.append("    }\n");
		buf.append("    public int getCount() {\n");
		buf.append("        return cout;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertEqualString( preview1, expected1);
	}

	public void testVarMultipleOccurances1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        for (i= 0; i > 9; i++) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 3);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal localProposal= null;
		for (int i = 0; i < proposals.size(); i++) {
			Object curr= proposals.get(i);
			if (curr instanceof NewVariableCorrectionProposal && ((NewVariableCorrectionProposal) curr).getVariableKind() == NewVariableCorrectionProposal.LOCAL) {
				localProposal= (CUCorrectionProposal) curr;
			}
		}
		assertNotNull(localProposal);

		String preview= getPreviewContent(localProposal);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        for (int i = 0; i > 9; i++) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertEqualString(preview, expected);
	}

	public void testVarMultipleOccurances2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        for (i= 0; i > 9;) {\n");
		buf.append("            i++;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 3);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal localProposal= null;
		for (int i = 0; i < proposals.size(); i++) {
			Object curr= proposals.get(i);
			if (curr instanceof NewVariableCorrectionProposal && ((NewVariableCorrectionProposal) curr).getVariableKind() == NewVariableCorrectionProposal.LOCAL) {
				localProposal= (CUCorrectionProposal) curr;
			}
		}
		assertNotNull(localProposal);

		String preview= getPreviewContent(localProposal);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        for (int i = 0; i > 9;) {\n");
		buf.append("            i++;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertEqualString(preview, expected);
	}

	public void testVarMultipleOccurances3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        for (i = 0; i > 9;) {\n");
		buf.append("        }\n");
		buf.append("        i= 9;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 3);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal localProposal= null;
		for (int i = 0; i < proposals.size(); i++) {
			Object curr= proposals.get(i);
			if (curr instanceof NewVariableCorrectionProposal && ((NewVariableCorrectionProposal) curr).getVariableKind() == NewVariableCorrectionProposal.LOCAL) {
				localProposal= (CUCorrectionProposal) curr;
			}
		}
		assertNotNull(localProposal);

		String preview= getPreviewContent(localProposal);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        int i;\n");
		buf.append("        for (i = 0; i > 9;) {\n");
		buf.append("        }\n");
		buf.append("        i= 9;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertEqualString(preview, expected);
	}

	public void testVarInArray() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Object[] arr) {\n");
		buf.append("        for (int i = 0; i > arr.lenght; i++) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Object[] arr) {\n");
		buf.append("        for (int i = 0; i > arr.length; i++) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	public void testVarInEnumSwitch() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public enum Colors {\n");
		buf.append("    RED\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Colors.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Colors c) {\n");
		buf.append("        switch (c) {\n");
		buf.append("            case BLUE:\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public enum Colors {\n");
		buf.append("    RED, BLUE\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Colors c) {\n");
		buf.append("        switch (c) {\n");
		buf.append("            case RED:\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	public void testVarInMethodInvocation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void goo(String s) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        goo(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private String x;\n");
		buf.append("    void goo(String s) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        goo(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void goo(String s) {\n");
		buf.append("    }\n");
		buf.append("    void foo(String x) {\n");
		buf.append("        goo(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void goo(String s) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        String x;\n");
		buf.append("        goo(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private static final String x = null;\n");
		buf.append("    void goo(String s) {\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("        goo(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected4= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });
	}

	public void testVarInConstructurInvocation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public E(String s) {\n");
		buf.append("    }\n");
		buf.append("    public E() {\n");
		buf.append("        this(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private static String x;\n");
		buf.append("    public E(String s) {\n");
		buf.append("    }\n");
		buf.append("    public E() {\n");
		buf.append("        this(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public E(String s) {\n");
		buf.append("    }\n");
		buf.append("    public E(String x) {\n");
		buf.append("        this(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private static final String x = null;\n");
		buf.append("    public E(String s) {\n");
		buf.append("    }\n");
		buf.append("    public E() {\n");
		buf.append("        this(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	public void testVarInSuperConstructurInvocation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("    public F(String s) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("F.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends F {\n");
		buf.append("    public E() {\n");
		buf.append("        super(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends F {\n");
		buf.append("    private static String x;\n");
		buf.append("\n");
		buf.append("    public E() {\n");
		buf.append("        super(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends F {\n");
		buf.append("    public E(String x) {\n");
		buf.append("        super(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends F {\n");
		buf.append("    private static final String x = null;\n");
		buf.append("\n");
		buf.append("    public E() {\n");
		buf.append("        super(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	public void testVarInClassInstanceCreation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("    public F(String s) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("F.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public E() {\n");
		buf.append("        new F(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private String x;\n");
		buf.append("\n");
		buf.append("    public E() {\n");
		buf.append("        new F(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public E(String x) {\n");
		buf.append("        new F(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private static final String x = null;\n");
		buf.append("\n");
		buf.append("    public E() {\n");
		buf.append("        new F(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public E() {\n");
		buf.append("        String x;\n");
		buf.append("        new F(x);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected4= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });
	}

	public void testVarInArrayAccess() throws Exception {
		// bug 194913
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    void foo(int i) {\n");
		buf.append("        bar[0][i] = \"bar\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[4];
		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    private String[][] bar;\n");
		buf.append("\n");
		buf.append("    void foo(int i) {\n");
		buf.append("        bar[0][i] = \"bar\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    private static final String[][] bar = null;\n");
		buf.append("\n");
		buf.append("    void foo(int i) {\n");
		buf.append("        bar[0][i] = \"bar\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    void foo(int i, String[][] bar) {\n");
		buf.append("        bar[0][i] = \"bar\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[2]= buf.toString();

		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    void foo(int i) {\n");
		buf.append("        String[][] bar;\n");
		buf.append("        bar[0][i] = \"bar\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[3]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testVarWithMethodName1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    int foo(String str) {\n");
		buf.append("        for (int i = 0; i > str.length; i++) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    int foo(String str) {\n");
		buf.append("        for (int i = 0; i > str.length(); i++) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	public void testVarWithMethodName2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    int foo(String str) {\n");
		buf.append("        return length;\n");
		buf.append("    }\n");
		buf.append("    int getLength() {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    int foo(String str) {\n");
		buf.append("        return getLength();\n");
		buf.append("    }\n");
		buf.append("    int getLength() {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	public void testSimilarVarsAndVisibility() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    int var1;\n");
		buf.append("    static int var2;\n");
		buf.append("    public static void main(String[] var3) {\n");
		buf.append("        println(var);\n");
		buf.append("    }\n");
		buf.append("    public static void println(String[] s) {}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 6);

		String[] expected= new String[6];
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    int var1;\n");
		buf.append("    static int var2;\n");
		buf.append("    public static void main(String[] var3) {\n");
		buf.append("        println(var3);\n");
		buf.append("    }\n");
		buf.append("    public static void println(String[] s) {}\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    int var1;\n");
		buf.append("    static int var2;\n");
		buf.append("    public static void main(String[] var3) {\n");
		buf.append("        println(var2);\n");
		buf.append("    }\n");
		buf.append("    public static void println(String[] s) {}\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    int var1;\n");
		buf.append("    static int var2;\n");
		buf.append("    private static String[] var;\n");
		buf.append("    public static void main(String[] var3) {\n");
		buf.append("        println(var);\n");
		buf.append("    }\n");
		buf.append("    public static void println(String[] s) {}\n");
		buf.append("}\n");
		expected[2]= buf.toString();

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    private static final String[] var = null;\n");
		buf.append("    int var1;\n");
		buf.append("    static int var2;\n");
		buf.append("    public static void main(String[] var3) {\n");
		buf.append("        println(var);\n");
		buf.append("    }\n");
		buf.append("    public static void println(String[] s) {}\n");
		buf.append("}\n");
		expected[3]= buf.toString();

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    int var1;\n");
		buf.append("    static int var2;\n");
		buf.append("    public static void main(String[] var3, String[] var) {\n");
		buf.append("        println(var);\n");
		buf.append("    }\n");
		buf.append("    public static void println(String[] s) {}\n");
		buf.append("}\n");
		expected[4]= buf.toString();

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    int var1;\n");
		buf.append("    static int var2;\n");
		buf.append("    public static void main(String[] var3) {\n");
		buf.append("        String[] var;\n");
		buf.append("        println(var);\n");
		buf.append("    }\n");
		buf.append("    public static void println(String[] s) {}\n");
		buf.append("}\n");
		expected[5]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}



}

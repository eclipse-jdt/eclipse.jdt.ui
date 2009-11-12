/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Muskalla <bmuskalla@innoopract.com> - [quick fix] 'Remove invalid modifiers' does not appear for enums and annotations - https://bugs.eclipse.org/bugs/show_bug.cgi?id=110589
 *     Benjamin Muskalla <b.muskalla@gmx.net> - [quick fix] Quick fix for missing synchronized modifier - https://bugs.eclipse.org/bugs/show_bug.cgi?id=245250
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
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CUCorrectionProposal;

public class ModifierCorrectionsQuickFixTest extends QuickFixTest {

	private static final Class THIS= ModifierCorrectionsQuickFixTest.class;

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	private static final boolean BUG_84358= true;

	public ModifierCorrectionsQuickFixTest(String name) {
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
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_LOCAL_VARIABLE, JavaCore.DO_NOT_INSERT);
		options.put(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_MISSING_SYNCHRONIZED_ON_INHERITED_METHOD, JavaCore.ERROR);


		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= ProjectTestSetup.getProject();

		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "", null);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}

	public void testStaticMethodRequestedInSameType1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void xoo() {\n");
		buf.append("    }\n");
		buf.append("    public static void foo() {\n");
		buf.append("        xoo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public static void xoo() {\n");
		buf.append("    }\n");
		buf.append("    public static void foo() {\n");
		buf.append("        xoo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testStaticMethodRequestedInSameType2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void xoo() {\n");
		buf.append("    }\n");
		buf.append("    public static void foo() {\n");
		buf.append("        E.xoo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public static void xoo() {\n");
		buf.append("    }\n");
		buf.append("    public static void foo() {\n");
		buf.append("        E.xoo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testStaticMethodRequestedInSameGenericType() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E<T> {\n");
		buf.append("    public <M> void xoo(M m) {\n");
		buf.append("    }\n");
		buf.append("    public static void foo() {\n");
		buf.append("        xoo(Boolean.TRUE);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E<T> {\n");
		buf.append("    public static <M> void xoo(M m) {\n");
		buf.append("    }\n");
		buf.append("    public static void foo() {\n");
		buf.append("        xoo(Boolean.TRUE);\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testStaticMethodRequestedInOtherType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class X {\n");
		buf.append("    public void xoo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("X.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("         X.xoo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class X {\n");
		buf.append("    public static void xoo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testInvisibleMethodRequestedInSuperType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    private void xoo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends C {\n");
		buf.append("    public void foo() {\n");
		buf.append("         xoo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    protected void xoo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testInvisibleSuperMethodRequestedInSuperType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    private void xoo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends C {\n");
		buf.append("    public void foo() {\n");
		buf.append("         super.xoo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    protected void xoo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testInvisibleSuperMethodRequestedInGenericSuperType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C<T> {\n");
		buf.append("    private void xoo(T... t) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends C<Boolean> {\n");
		buf.append("    public void foo() {\n");
		buf.append("         super.xoo(Boolean.TRUE);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C<T> {\n");
		buf.append("    protected void xoo(T... t) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testInvisibleMethodRequestedInOtherPackage() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class C {\n");
		buf.append("    private void xoo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack2.createCompilationUnit("C.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.C;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(C c) {\n");
		buf.append("         c.xoo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class C {\n");
		buf.append("    public void xoo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testInvisibleConstructorRequestedInOtherType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    private C() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("         C c= new C();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    C() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testInvisibleDefaultConstructorRequestedInOtherType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    protected class Inner{\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("C.java", buf.toString(), false, null);
		
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class E extends test1.C {\n");
		buf.append("    Object o= new Inner();\n");
		buf.append("}\n");
		ICompilationUnit cu= pack2.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    protected class Inner{\n");
		buf.append("\n");
		buf.append("        public Inner() {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testInvisibleConstructorRequestedInSuperType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    private C() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends C {\n");
		buf.append("    public E() {\n");
		buf.append("         super();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    protected C() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testInvisibleFieldRequestedInSamePackage1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    private int fXoo;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(C c) {\n");
		buf.append("         c.fXoo= 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(1);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    int fXoo;\n");
		buf.append("}\n");

		assertEqualString(preview, buf.toString());
	}


	public void testInvisibleFieldRequestedInSamePackage2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    private int fXoo;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends C {\n");
		buf.append("    public void foo() {\n");
		buf.append("         fXoo= 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 6);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(1);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    protected int fXoo;\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends C {\n");
		buf.append("    private int fXoo;\n");
		buf.append("\n");
		buf.append("    public void foo() {\n");
		buf.append("         fXoo= 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends C {\n");
		buf.append("    public void foo(int fXoo) {\n");
		buf.append("         fXoo= 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(4);
		String preview4= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends C {\n");
		buf.append("    public void foo() {\n");
		buf.append("         int fXoo = 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected4= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(5);
		String preview5= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends C {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected5= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4, preview5 }, new String[] { expected1, expected2, expected3, expected4, expected5 });

	}


	public void testNonStaticMethodRequestedInConstructor() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int xoo() { return 1; };\n");
		buf.append("    public E(int val) {\n");
		buf.append("    }\n");
		buf.append("    public E() {\n");
		buf.append("         this(xoo());\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private static int xoo() { return 1; };\n");
		buf.append("    public E(int val) {\n");
		buf.append("    }\n");
		buf.append("    public E() {\n");
		buf.append("         this(xoo());\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testNonStaticFieldRequestedInConstructor() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int fXoo= 1;\n");
		buf.append("    public E(int val) {\n");
		buf.append("    }\n");
		buf.append("    public E() {\n");
		buf.append("         this(fXoo);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private static int fXoo= 1;\n");
		buf.append("    public E(int val) {\n");
		buf.append("    }\n");
		buf.append("    public E() {\n");
		buf.append("         this(fXoo);\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testInvisibleTypeRequestedInDifferentPackage() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("class C {\n");
		buf.append("}\n");
		pack2.createCompilationUnit("C.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("         test2.C c= null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class C {\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testInvisibleTypeRequestedInGenericType() throws Exception {
		if (BUG_84358) {
			return;
		}


		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("class C<T> {\n");
		buf.append("}\n");
		pack2.createCompilationUnit("C.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("         test2.C<String> c= null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class C<T> {\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}


	public void testInvisibleTypeRequestedFromSuperClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    private class CInner {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends C {\n");
		buf.append("    public void foo() {\n");
		buf.append("         CInner c= null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    class CInner {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}


	public void testInvisibleImport() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("class C {\n");
		buf.append("}\n");
		pack2.createCompilationUnit("C.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("         test2.C c= null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class C {\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testAbstractMethodWithBody() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public abstract void foo() {\n");
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
		buf.append("public abstract class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public abstract void foo();\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}

	public void testAbstractMethodInNonAbstractClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public abstract int foo();\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOfProblems(2, problems);
		
		ArrayList proposals= collectCorrections(cu, problems[0], null);
		
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public abstract int foo();\n");
		buf.append("}\n");
		String expectedMakeClassAbstract= buf.toString();
		
		assertEquals(preview1, expectedMakeClassAbstract);
		
		
		proposals= collectCorrections(cu, problems[1], null);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		proposal= (CUCorrectionProposal) proposals.get(0);
		preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public int foo() {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expectedMakeClassAbstract });
	}

	public void testNativeMethodWithBody() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public native void foo() {\n");
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
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public native void foo();\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	public void testOuterLocalMustBeFinal() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= 9;\n");
		buf.append("        Runnable run= new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                int x= i;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        final int i= 9;\n");
		buf.append("        Runnable run= new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                int x= i;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testOuterLocalMustBeFinal2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        List<String> i= null, j= null;\n");
		buf.append("        Runnable run= new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                Object x= i;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        final List<String> i= null;\n");
		buf.append("        List<String> j= null;\n");
		buf.append("        Runnable run= new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                Object x= i;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}


	public void testOuterParameterMustBeFinal() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i) {\n");
		buf.append("        Runnable run= new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                int x= i;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(final int i) {\n");
		buf.append("        Runnable run= new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                int x= i;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testOuterForParamMustBeFinal() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int i= 1; true;) {\n");
		buf.append("            Runnable run= new Runnable() {\n");
		buf.append("                public void run() {\n");
		buf.append("                    int x= i;\n");
		buf.append("                }\n");
		buf.append("            };\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (final int i= 1; true;) {\n");
		buf.append("            Runnable run= new Runnable() {\n");
		buf.append("                public void run() {\n");
		buf.append("                    int x= i;\n");
		buf.append("                }\n");
		buf.append("            };\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}


	public void testMethodRequiresBody() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo();\n");
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
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public abstract void foo();\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}

	public void testMethodRequiresBody2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public int foo();\n");
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
		buf.append("    public int foo() {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public abstract int foo();\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	public void testNeedToEmulateMethodAccess() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_SYNTHETIC_ACCESS_EMULATION, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void x() {\n");
		buf.append("        Runnable r= new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                foo();\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("    private void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void x() {\n");
		buf.append("        Runnable r= new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                foo();\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("    void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testNeedToEmulateConstructorAccess() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_SYNTHETIC_ACCESS_EMULATION, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void x() {\n");
		buf.append("        Runnable r= new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("               E e= new E();\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("    private E() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void x() {\n");
		buf.append("        Runnable r= new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("               E e= new E();\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("    E() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testNeedToEmulateFieldRead() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_SYNTHETIC_ACCESS_EMULATION, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void x() {\n");
		buf.append("        Runnable r= new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("               int x= fCount;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("    private int fCount; {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void x() {\n");
		buf.append("        Runnable r= new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("               int x= fCount;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("    int fCount; {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testNeedToEmulateFieldReadInGeneric() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_SYNTHETIC_ACCESS_EMULATION, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E<T> {\n");
		buf.append("    public void x() {\n");
		buf.append("        Runnable r= new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("               T x= fCount;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("    private T fCount; {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E<T> {\n");
		buf.append("    public void x() {\n");
		buf.append("        Runnable r= new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("               T x= fCount;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("    T fCount; {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testNeedToEmulateFieldWrite() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_SYNTHETIC_ACCESS_EMULATION, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void x() {\n");
		buf.append("        Runnable r= new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("               fCount= 2;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("    private int fCount; {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void x() {\n");
		buf.append("        Runnable r= new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("               fCount= 2;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("    int fCount; {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testSetFinalVariable1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private final int x= 9;\n");
		buf.append("    public void foo() {\n");
		buf.append("        x= 8;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int x= 9;\n");
		buf.append("    public void foo() {\n");
		buf.append("        x= 8;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testSetFinalVariable2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private final int x;\n");
		buf.append("    public E() {\n");
		buf.append("        x= 8;\n");
		buf.append("        x= 9;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int x;\n");
		buf.append("    public E() {\n");
		buf.append("        x= 8;\n");
		buf.append("        x= 9;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testSetFinalVariable3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public E() {\n");
		buf.append("        final int x= 8;\n");
		buf.append("        x= 9;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public E() {\n");
		buf.append("        int x= 8;\n");
		buf.append("        x= 9;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testSetFinalVariable4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E {\n");
		buf.append("    final List<String> a= null, x= a, y= a;\n");
		buf.append("    public void foo() {\n");
		buf.append("        x= new ArrayList<String>();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E {\n");
		buf.append("    final List<String> a= null;\n");
		buf.append("    List<String> x= a;\n");
		buf.append("    final List<String> y= a;\n");
		buf.append("    public void foo() {\n");
		buf.append("        x= new ArrayList<String>();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testSetFinalVariableInGeneric() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class C<T> {\n");
		buf.append("        T x;\n");
		buf.append("}\n");
		pack2.createCompilationUnit("C.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(test2.C<String> c) {\n");
		buf.append("         c.x= null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(1);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class C<T> {\n");
		buf.append("        public T x;\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}


	public void testOverrideFinalMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    protected final void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends C {\n");
		buf.append("    protected void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    protected void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}


	public void testOverridesNonVisibleMethod() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);

		StringBuffer buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class C {\n");
		buf.append("    void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack2.createCompilationUnit("C.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.C;\n");
		buf.append("public class E extends C {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class C {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testOverridesStaticMethod() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);

		StringBuffer buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class C {\n");
		buf.append("    public static void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack2.createCompilationUnit("C.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.C;\n");
		buf.append("public class E extends C {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class C {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testStaticOverridesMethod() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);

		StringBuffer buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class C {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack2.createCompilationUnit("C.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.C;\n");
		buf.append("public class E extends C {\n");
		buf.append("    public static void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.C;\n");
		buf.append("public class E extends C {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testOverridesMoreVisibleMethod() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);

		StringBuffer buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class C {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack2.createCompilationUnit("C.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.C;\n");
		buf.append("public class E extends C {\n");
		buf.append("    protected void foo() {\n");
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
		buf.append("package test2;\n");
		buf.append("public class C {\n");
		buf.append("    protected void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.C;\n");
		buf.append("public class E extends C {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}

	public void testOverridesMoreVisibleMethodInGeneric() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);

		StringBuffer buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class C<T> {\n");
		buf.append("    public <M> T foo(M m) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack2.createCompilationUnit("C.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.C;\n");
		buf.append("public class E extends C<String> {\n");
		buf.append("    protected <M> String foo(M m) {\n");
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
		buf.append("package test2;\n");
		buf.append("public class C<T> {\n");
		buf.append("    protected <M> T foo(M m) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.C;\n");
		buf.append("public class E extends C<String> {\n");
		buf.append("    public <M> String foo(M m) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}


	public void testInvalidInterfaceModifiers() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public static interface E  {\n");
		buf.append("    public void foo();\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface E  {\n");
		buf.append("    public void foo();\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testInvalidMemberInterfaceModifiers() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface E  {\n");
		buf.append("    private interface Inner {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface E  {\n");
		buf.append("    interface Inner {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testInvalidInterfaceFieldModifiers() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface E  {\n");
		buf.append("    public native int i;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface E  {\n");
		buf.append("    public int i;\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testInvalidInterfaceMethodModifiers() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface E  {\n");
		buf.append("    private strictfp void foo();\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface E  {\n");
		buf.append("    void foo();\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testInvalidClassModifiers() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public volatile class E  {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E  {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}


	public void testInvalidMemberClassModifiers() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface E  {\n");
		buf.append("    private class Inner {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface E  {\n");
		buf.append("    class Inner {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testInvalidLocalClassModifiers() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E  {\n");
		buf.append("    private void foo() {\n");
		buf.append("        static class Local {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E  {\n");
		buf.append("    private void foo() {\n");
		buf.append("        class Local {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testInvalidClassFieldModifiers() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E  {\n");
		buf.append("    strictfp public native int i;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E  {\n");
		buf.append("    public int i;\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testInvalidClassMethodModifiers() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E  {\n");
		buf.append("    volatile abstract void foo();\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E  {\n");
		buf.append("    abstract void foo();\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testInvalidConstructorModifiers() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("public class Bug {\n");
		buf.append("    public static Bug() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Bug.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("public class Bug {\n");
		buf.append("    public Bug() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
		}


	public void testInvalidParamModifiers() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E  {\n");
		buf.append("    private void foo(private int x) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E  {\n");
		buf.append("    private void foo(int x) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testInvalidVariableModifiers() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E  {\n");
		buf.append("    private void foo() {\n");
		buf.append("        native int x;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E  {\n");
		buf.append("    private void foo() {\n");
		buf.append("        int x;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testInvalidMultiVariableModifiers() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E  {\n");
		buf.append("    public E() {\n");
		buf.append("        @Deprecated final @SuppressWarnings(\"all\") int a, b, c;\n");
		buf.append("        a = 0; b = 0; c = 12;\n");
		buf.append("        b = 5;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E  {\n");
		buf.append("    public E() {\n");
		buf.append("        @Deprecated final @SuppressWarnings(\"all\") int a;\n");
		buf.append("        @Deprecated @SuppressWarnings(\"all\") int b;\n");
		buf.append("        @Deprecated final @SuppressWarnings(\"all\") int c;\n");
		buf.append("        a = 0; b = 0; c = 12;\n");
		buf.append("        b = 5;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testInvalidMultiFieldModifiers() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E  {\n");
		buf.append("    private final int a, b;\n");
		buf.append("    public E() {\n");
		buf.append("        a = 0; b = 0;\n");
		buf.append("        a = 5;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E  {\n");
		buf.append("    private int a;\n");
		buf.append("    private final int b;\n");
		buf.append("    public E() {\n");
		buf.append("        a = 0; b = 0;\n");
		buf.append("        a = 5;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testInvalidMultiFieldModifiers2() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E  {\n");
		buf.append("    private final int b, a;\n");
		buf.append("    public E() {\n");
		buf.append("        a = 0; b = 0;\n");
		buf.append("        a = 5;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E  {\n");
		buf.append("    private final int b;\n");
		buf.append("    private int a;\n");
		buf.append("    public E() {\n");
		buf.append("        a = 0; b = 0;\n");
		buf.append("        a = 5;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testExtendsFinalClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public final class C {\n");
		buf.append("    protected void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends C {\n");
		buf.append("    protected void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    protected void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testExtendsFinalClass2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public final class C {\n");
		buf.append("    protected void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    protected void foo() {\n");
		buf.append("        C c= new C() { };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    protected void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testMissingOverrideAnnotation() throws Exception {
		Hashtable options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_MISSING_DEPRECATED_ANNOTATION, JavaCore.ERROR);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public final class C {\n");
		buf.append("    public String toString() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public final class C {\n");
		buf.append("    @Override\n");
		buf.append("    public String toString() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testMissingTypeDeprecationAnnotation() throws Exception {
		Hashtable options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_MISSING_DEPRECATED_ANNOTATION, JavaCore.ERROR);
		JavaCore.setOptions(options);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" * @deprecated\n");
		buf.append(" */\n");
		buf.append("public final class C {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" * @deprecated\n");
		buf.append(" */\n");
		buf.append("@Deprecated\n");
		buf.append("public final class C {\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testMissingMethodDeprecationAnnotation() throws Exception {
		Hashtable options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_MISSING_DEPRECATED_ANNOTATION, JavaCore.ERROR);
		JavaCore.setOptions(options);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public final class C {\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Override\n");
		buf.append("    public String toString() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public final class C {\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    @Override\n");
		buf.append("    public String toString() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testMissingFieldDeprecationAnnotation() throws Exception {
		Hashtable options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_MISSING_DEPRECATED_ANNOTATION, JavaCore.ERROR);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public final class C {\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    public String name;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public final class C {\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    public String name;\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testSuppressNLSWarningAnnotation1() throws Exception {

		Hashtable options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_NON_NLS_STRING_LITERAL, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    static {\n");
		buf.append("        @SuppressWarnings(\"unused\") String str= \"foo\";");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);

		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("@SuppressWarnings(\"nls\")\n");
		buf.append("public class E {\n");
		buf.append("    static {\n");
		buf.append("        @SuppressWarnings(\"unused\") String str= \"foo\";");
		buf.append("    }\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {buf.toString()});
	}

	public void testSuppressNLSWarningAnnotation2() throws Exception {

		Hashtable options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_NON_NLS_STRING_LITERAL, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1; \n");
		buf.append("public class E {\n");
		buf.append("    private class Q {\n");
		buf.append("        String s = \"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);

		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1; \n");
		buf.append("public class E {\n");
		buf.append("    @SuppressWarnings(\"nls\")\n");
		buf.append("    private class Q {\n");
		buf.append("        String s = \"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {buf.toString()});
	}

	public void testSuppressNLSWarningAnnotation3() throws Exception {

		Hashtable options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_NON_NLS_STRING_LITERAL, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1; \n");
		buf.append("public class E {\n");
		buf.append("    String s = \"\";\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);

		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1; \n");
		buf.append("@SuppressWarnings(\"nls\")\n");
		buf.append("public class E {\n");
		buf.append("    String s = \"\";\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {buf.toString()});
	}

	public void testSuppressNLSWarningAnnotation4() throws Exception {

		Hashtable options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_NON_NLS_STRING_LITERAL, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1; \n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        @SuppressWarnings(\"unused\") String s = \"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);

		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1; \n");
		buf.append("public class E {\n");
		buf.append("    @SuppressWarnings(\"nls\")\n");
		buf.append("    public void foo() {\n");
		buf.append("        @SuppressWarnings(\"unused\") String s = \"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {buf.toString()});
	}

	public void testSuppressNLSWarningAnnotation5() throws Exception {
		Hashtable options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.WARNING);
		JavaCore.setOptions(options);


		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1; \n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("\n");
		buf.append("public class A {\n");
		buf.append("    public void foo(ArrayList<String> c) {\n");
		buf.append("        new ArrayList(c);\n"); // two warnings on this -> only one proposal!
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 2);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuffer();
		buf.append("package test1; \n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("\n");
		buf.append("public class A {\n");
		buf.append("    @SuppressWarnings(\"unchecked\")\n");
		buf.append("    public void foo(ArrayList<String> c) {\n");
		buf.append("        new ArrayList(c);\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testMisspelledSuppressToken() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("a", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package a;\n");
		buf.append("\n");
		buf.append("public class A {\n");
		buf.append("    @SuppressWarnings(\"unusd\")\n");
		buf.append("    public static void main(String[] args) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		buf= new StringBuffer();
		buf.append("package a;\n");
		buf.append("\n");
		buf.append("public class A {\n");
		buf.append("    @SuppressWarnings(\"unused\")\n");
		buf.append("    public static void main(String[] args) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuffer();
		buf.append("package a;\n");
		buf.append("\n");
		buf.append("public class A {\n");
		buf.append("    public static void main(String[] args) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[1]= buf.toString();
		
		assertExpectedExistInProposals(proposals, expected);
	}

	public void testSuppressBug169446() throws Exception {

		IPackageFragment other= fSourceFolder.createPackageFragment("other", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package other; \n");
		buf.append("\n");
		buf.append("public @interface SuppressWarnings {\n");
		buf.append("    String value();\n");
		buf.append("}\n");
		other.createCompilationUnit("SuppressWarnings.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("a.b", false, null);

		buf= new StringBuffer();
		buf.append("package a.b;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    @Deprecated()\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package a.b;\n");
		buf.append("\n");
		buf.append("import other.SuppressWarnings;\n");
		buf.append("\n");
		buf.append("public class Test {\n");
		buf.append("    @SuppressWarnings(\"BC\")\n");
		buf.append("    public void foo() {\n");
		buf.append("        new E().foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuffer();
		buf.append("package a.b;\n");
		buf.append("\n");
		buf.append("import other.SuppressWarnings;\n");
		buf.append("\n");
		buf.append("public class Test {\n");
		buf.append("    @java.lang.SuppressWarnings(\"deprecation\")\n");
		buf.append("    @SuppressWarnings(\"BC\")\n");
		buf.append("    public void foo() {\n");
		buf.append("        new E().foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testSuppressWarningInImports() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class A {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);

		String[] expected= new String[1];

		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("@SuppressWarnings(\"unused\")\n");
		buf.append("public class A {\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testUnusedSuppressWarnings1() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNHANDLED_WARNING_TOKEN, JavaCore.IGNORE);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_WARNING_TOKEN, JavaCore.WARNING);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("@SuppressWarnings(value=\"unused\")\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testUnusedSuppressWarnings2() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNHANDLED_WARNING_TOKEN, JavaCore.IGNORE);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_WARNING_TOKEN, JavaCore.WARNING);
		JavaCore.setOptions(hashtable);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("@SuppressWarnings(\"unused\")\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testUnusedSuppressWarnings3() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNHANDLED_WARNING_TOKEN, JavaCore.IGNORE);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_WARNING_TOKEN, JavaCore.WARNING);
		JavaCore.setOptions(hashtable);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("@SuppressWarnings({ \"unused\", \"X\" })\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("@SuppressWarnings({ \"X\" })\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}


	public void testMakeFinalBug129165() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("public class E {\n");
		buf.append("    @SuppressWarnings(\"serial\")\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= 1, j= i + 1, h= j + 1;\n");
		buf.append("        Serializable ser= new Serializable() {\n");
		buf.append("            public void bar() {\n");
		buf.append("                System.out.println(j);\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("public class E {\n");
		buf.append("    @SuppressWarnings(\"serial\")\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= 1;\n");
		buf.append("        final int j= i + 1;\n");
		buf.append("        int h= j + 1;\n");
		buf.append("        Serializable ser= new Serializable() {\n");
		buf.append("            public void bar() {\n");
		buf.append("                System.out.println(j);\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testStaticFieldInInnerClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    class F {\n");
		buf.append("        static int x;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		String[] expected= new String[3];
		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    class F {\n");
		buf.append("        int x;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    class F {\n");
		buf.append("        static final int x;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    static class F {\n");
		buf.append("        static int x;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[2]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testStaticMethodInInnerClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    class F {\n");
		buf.append("        static int foo() {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    class F {\n");
		buf.append("        int foo() {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    static class F {\n");
		buf.append("        static int foo() {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testFinalVolatileField() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    class F {\n");
		buf.append("        volatile final int x;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    class F {\n");
		buf.append("        final int x;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    class F {\n");
		buf.append("        volatile int x;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}


	public void testOverrideAnnotationButNotOverriding() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);

		StringBuffer buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("public class OtherMachine {\n");
		buf.append("}\n");
		pack1.createCompilationUnit("OtherMachine.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("import java.io.IOException;\n");
		buf.append("\n");
		buf.append("public class Machine extends OtherMachine {\n");
		buf.append("    @Override\n");
		buf.append("    public boolean isAlive(OtherMachine m) throws IOException {\n");
		buf.append("        return true;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Machine.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("import java.io.IOException;\n");
		buf.append("\n");
		buf.append("public class Machine extends OtherMachine {\n");
		buf.append("    public boolean isAlive(OtherMachine m) throws IOException {\n");
		buf.append("        return true;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("import java.io.IOException;\n");
		buf.append("\n");
		buf.append("public class OtherMachine {\n");
		buf.append("\n");
		buf.append("    public boolean isAlive(OtherMachine m) throws IOException {\n");
		buf.append("        return false;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testMethodOverrideDeprecated1() throws Exception {
		Hashtable options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEPRECATION_WHEN_OVERRIDING_DEPRECATED_METHOD, JavaCore.ENABLED);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("public class E {\n");
		buf.append("    @Deprecated\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}    \n");
		buf.append("\n");
		buf.append("class F extends E {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("public class E {\n");
		buf.append("    @Deprecated\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}    \n");
		buf.append("\n");
		buf.append("class F extends E {\n");
		buf.append("    @Deprecated\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		expected[0]= buf.toString();

		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("public class E {\n");
		buf.append("    @Deprecated\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}    \n");
		buf.append("\n");
		buf.append("class F extends E {\n");
		buf.append("    @SuppressWarnings(\"deprecation\")\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testMethodOverrideDeprecated2() throws Exception {
		Hashtable options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEPRECATION_WHEN_OVERRIDING_DEPRECATED_METHOD, JavaCore.ENABLED);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("public class E {\n");
		buf.append("    @Deprecated\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}    \n");
		buf.append("\n");
		buf.append("class F extends E {\n");
		buf.append("    /**\n");
		buf.append("     */\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("public class E {\n");
		buf.append("    @Deprecated\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}    \n");
		buf.append("\n");
		buf.append("class F extends E {\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		expected[0]= buf.toString();

		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("public class E {\n");
		buf.append("    @Deprecated\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}    \n");
		buf.append("\n");
		buf.append("class F extends E {\n");
		buf.append("    /**\n");
		buf.append("     */\n");
		buf.append("    @SuppressWarnings(\"deprecation\")\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testAbstractMethodInEnumWithoutEnumConstants() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("r", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package r;\n");
		buf.append("\n");
		buf.append("enum E {\n");
		buf.append("    ;\n");
		buf.append("    public abstract boolean foo();\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuffer();
		buf.append("package r;\n");
		buf.append("\n");
		buf.append("enum E {\n");
		buf.append("    ;\n");
		buf.append("    public boolean foo() {\n");
		buf.append("        return false;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testInvalidEnumModifier() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("r", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package r;\n");
		buf.append("\n");
		buf.append("private final strictfp enum E {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuffer();
		buf.append("package r;\n");
		buf.append("\n");
		buf.append("strictfp enum E {\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testInvalidEnumModifier2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("r", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package r;\n");
		buf.append("\n");
		buf.append("public abstract enum E {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuffer();
		buf.append("package r;\n");
		buf.append("\n");
		buf.append("public enum E {\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testInvalidEnumConstantModifier() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("r", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package r;\n");
		buf.append("\n");
		buf.append("enum E {\n");
		buf.append("	private final WHITE;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuffer();
		buf.append("package r;\n");
		buf.append("\n");
		buf.append("enum E {\n");
		buf.append("	WHITE;\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testInvalidEnumConstructorModifier() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("r", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package r;\n");
		buf.append("\n");
		buf.append("enum E {\n");
		buf.append("	WHITE(1);\n");
		buf.append("\n");
		buf.append("	public final E(int foo) {\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuffer();
		buf.append("package r;\n");
		buf.append("\n");
		buf.append("enum E {\n");
		buf.append("	WHITE(1);\n");
		buf.append("\n");
		buf.append("	E(int foo) {\n");
		buf.append("	}\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testInvalidMemberEnumModifier() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("r", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package r;\n");
		buf.append("\n");
		buf.append("class E {\n");
		buf.append("	final enum A {\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuffer();
		buf.append("package r;\n");
		buf.append("\n");
		buf.append("class E {\n");
		buf.append("	enum A {\n");
		buf.append("	}\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	public void testMissingSynchronizedOnInheritedMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("r", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package r;\n");
		buf.append("\n");
		buf.append("public class A {\n");
		buf.append("	protected synchronized void foo() {\n");
		buf.append("	}\n");
		buf.append("}\n");
		pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package r;\n");
		buf.append("\n");
		buf.append("class B extends A {\n");
		buf.append("	protected void foo() {\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("B.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuffer();
		buf.append("package r;\n");
		buf.append("\n");
		buf.append("class B extends A {\n");
		buf.append("	protected synchronized void foo() {\n");
		buf.append("	}\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}
}

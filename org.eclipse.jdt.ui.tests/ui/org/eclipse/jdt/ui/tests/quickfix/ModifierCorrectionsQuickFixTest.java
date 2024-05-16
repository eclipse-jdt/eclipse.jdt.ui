/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *     Benjamin Muskalla <bmuskalla@innoopract.com> - [quick fix] 'Remove invalid modifiers' does not appear for enums and annotations - https://bugs.eclipse.org/bugs/show_bug.cgi?id=110589
 *     Benjamin Muskalla <b.muskalla@gmx.net> - [quick fix] Quick fix for missing synchronized modifier - https://bugs.eclipse.org/bugs/show_bug.cgi?id=245250
 *     Rabea Gransberger <rgransberger@gmx.de> - [quick fix] Fix several visibility issues - https://bugs.eclipse.org/394692
 *     Jerome Cambon <jerome.cambon@oracle.com> - [code style] don't generate redundant modifiers "public static final abstract" for interface members - https://bugs.eclipse.org/71627
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class ModifierCorrectionsQuickFixTest extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup= new ProjectTestSetup();

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_LOCAL_VARIABLE, JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_LOCAL_VARIABLE, DefaultCodeFormatterConstants.createAlignmentValue(false, DefaultCodeFormatterConstants.WRAP_NO_SPLIT));
		options.put(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_MISSING_SYNCHRONIZED_ON_INHERITED_METHOD, JavaCore.ERROR);


		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= projectSetup.getProject();

		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "", null);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testStaticMethodRequestedInSameType1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void xoo() {
			    }
			    public static void foo() {
			        xoo();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E {
			    public static void xoo() {
			    }
			    public static void foo() {
			        xoo();
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testStaticMethodRequestedInSameType2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void xoo() {
			    }
			    public static void foo() {
			        E.xoo();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String[] previews= getAllPreviewContent(proposals);
		String[] expected = new String[previews.length];

		expected[0] = """
			package test1;
			public class E {
			    public static void xoo() {
			    }
			    public static void foo() {
			        E.xoo();
			    }
			}
			""";

		expected[1] = """
			package test1;
			public class E {
			    public void xoo() {
			    }
			    public static void foo() {
			        E e = new E();
			        e.xoo();
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(previews, expected);
	}

	@Test
	public void testStaticMethodRequestedInSameGenericType() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E<T> {
			    public <M> void xoo(M m) {
			    }
			    public static void foo() {
			        xoo(Boolean.TRUE);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E<T> {
			    public static <M> void xoo(M m) {
			    }
			    public static void foo() {
			        xoo(Boolean.TRUE);
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testStaticMethodRequestedInOtherType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class X {
			    public void xoo() {
			    }
			}
			""";
		pack1.createCompilationUnit("X.java", str, false, null);

		String str1= """
			package test1;
			public class E {
			    public void foo() {
			         X.xoo();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String[] previews= getAllPreviewContent(proposals);
		String[] expected = new String[previews.length];

		expected[0] = """
			package test1;
			public class X {
			    public static void xoo() {
			    }
			}
			""";

		expected[1] = """
			package test1;
			public class E {
			    public void foo() {
			         X x = new X();
			        x.xoo();
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(previews, expected);
	}

	@Test
	public void testInvisibleMethodRequestedInSuperType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class C {
			    private void xoo() {
			    }
			}
			""";
		pack1.createCompilationUnit("C.java", str, false, null);

		String str1= """
			package test1;
			public class E extends C {
			    public void foo() {
			         xoo();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class C {
			    protected void xoo() {
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testInvisibleSuperMethodRequestedInSuperType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class C {
			    private void xoo() {
			    }
			}
			""";
		pack1.createCompilationUnit("C.java", str, false, null);

		String str1= """
			package test1;
			public class E extends C {
			    public void foo() {
			         super.xoo();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class C {
			    protected void xoo() {
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testInvisibleSuperMethodRequestedInGenericSuperType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class C<T> {
			    private void xoo(T... t) {
			    }
			}
			""";
		pack1.createCompilationUnit("C.java", str, false, null);

		String str1= """
			package test1;
			public class E extends C<Boolean> {
			    public void foo() {
			         super.xoo(Boolean.TRUE);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class C<T> {
			    protected void xoo(T... t) {
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testInvisibleMethodRequestedInOtherPackage() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		String str= """
			package test2;
			import java.util.List;
			public class C {
			    private void xoo() {
			    }
			}
			""";
		pack2.createCompilationUnit("C.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			import test2.C;
			public class E {
			    public void foo(C c) {
			         c.xoo();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test2;
			import java.util.List;
			public class C {
			    public void xoo() {
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testInvisibleConstructorRequestedInOtherType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class C {
			    private C() {
			    }
			}
			""";
		pack1.createCompilationUnit("C.java", str, false, null);

		String str1= """
			package test1;
			public class E {
			    public void foo() {
			         C c= new C();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class C {
			    C() {
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testInvisibleDefaultConstructorRequestedInOtherType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class C {
			    protected class Inner{
			    }
			}
			""";
		pack1.createCompilationUnit("C.java", str, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		String str1= """
			package test2;
			public class E extends test1.C {
			    Object o= new Inner();
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class C {
			    protected class Inner{
			
			        public Inner() {
			        }
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testInvisibleConstructorRequestedInSuperType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class C {
			    private C() {
			    }
			}
			""";
		pack1.createCompilationUnit("C.java", str, false, null);

		String str1= """
			package test1;
			public class E extends C {
			    public E() {
			         super();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class C {
			    protected C() {
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testInvisibleFieldRequestedInSamePackage1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class C {
			    private int fXoo;
			}
			""";
		pack1.createCompilationUnit("C.java", str, false, null);

		String str1= """
			package test1;
			public class E {
			    public void foo(C c) {
			         c.fXoo= 1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(1);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class C {
			    int fXoo;
			}
			""";
		assertEqualString(preview, str2);
	}


	@Test
	public void testInvisibleFieldRequestedInSamePackage2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class C {
			    private int fXoo;
			}
			""";
		pack1.createCompilationUnit("C.java", str, false, null);

		String str1= """
			package test1;
			public class E extends C {
			    public void foo() {
			         fXoo= 1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 6);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(1);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class C {
			    protected int fXoo;
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E extends C {
			    private int fXoo;
			
			    public void foo() {
			         fXoo= 1;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E extends C {
			    public void foo(int fXoo) {
			         fXoo= 1;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(4);
		String preview4= getPreviewContent(proposal);

		String expected4= """
			package test1;
			public class E extends C {
			    public void foo() {
			         int fXoo = 1;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(5);
		String preview5= getPreviewContent(proposal);

		String expected5= """
			package test1;
			public class E extends C {
			    public void foo() {
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4, preview5 }, new String[] { expected1, expected2, expected3, expected4, expected5 });

	}


	@Test
	public void testNonStaticMethodRequestedInConstructor() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private int xoo() { return 1; };
			    public E(int val) {
			    }
			    public E() {
			         this(xoo());
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E {
			    private static int xoo() { return 1; };
			    public E(int val) {
			    }
			    public E() {
			         this(xoo());
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testNonStaticFieldRequestedInConstructor() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private int fXoo= 1;
			    public E(int val) {
			    }
			    public E() {
			         this(fXoo);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E {
			    private static int fXoo= 1;
			    public E(int val) {
			    }
			    public E() {
			         this(fXoo);
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testInvisibleTypeRequestedInDifferentPackage() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		String str= """
			package test2;
			class C {
			}
			""";
		pack2.createCompilationUnit("C.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			         test2.C c= null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test2;
			public class C {
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testInvisibleTypeRequestedInGenericType() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		String str= """
			package test2;
			class C<T> {
			}
			""";
		pack2.createCompilationUnit("C.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			         test2.C<String> c= null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test2;
			public class C<T> {
			}
			""";
		assertEqualString(preview, str2);
	}


	@Test
	public void testInvisibleTypeRequestedFromSuperClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class C {
			    private class CInner {
			    }
			}
			""";
		pack1.createCompilationUnit("C.java", str, false, null);

		String str1= """
			package test1;
			public class E extends C {
			    public void foo() {
			         CInner c= null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class C {
			    class CInner {
			    }
			}
			""";
		assertEqualString(preview, str2);
	}


	@Test
	public void testInvisibleImport() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		String str= """
			package test2;
			class C {
			}
			""";
		pack2.createCompilationUnit("C.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			         test2.C c= null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test2;
			public class C {
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testAbstractMethodWithBody() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public abstract class E {
			    public abstract void foo() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public abstract class E {
			    public void foo() {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public abstract class E {
			    public abstract void foo();
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}

	@Test
	public void testAbstractMethodWithBody2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			abstract class C {
			    public abstract strictfp void foo() {}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<ICompletionProposal> proposals= collectAllCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			abstract class C {
			    public strictfp void foo() {}
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			abstract class C {
			    public abstract void foo();
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}

	@Test
	public void testAbstractMethodWithBody3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			enum E {
			    A {
			        public void foo() {}
			    };
			    public abstract void foo() {}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Snippet.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package test1;
			
			enum E {
			    A {
			        public void foo() {}
			    };
			    public abstract void foo();
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testAbstractMethodInNonAbstractClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public abstract int foo();
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOfProblems(2, problems);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, problems[0], null);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expectedMakeClassAbstract= """
			package test1;
			public abstract class E {
			    public abstract int foo();
			}
			""";

		assertEquals(preview1, expectedMakeClassAbstract);


		proposals= collectCorrections(cu, problems[1], null);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		proposal= (CUCorrectionProposal) proposals.get(0);
		preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public int foo() {
			        return 0;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expectedMakeClassAbstract });
	}

	@Test
	public void testNativeMethodWithBody() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public native void foo() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public native void foo();
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testOuterLocalMustBeFinal() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        int i= 9;
			        Runnable run= new Runnable() {
			            public void run() {
			                int x= i;
			            }
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        final int i= 9;
			        Runnable run= new Runnable() {
			            public void run() {
			                int x= i;
			            }
			        };
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testOuterLocalMustBeFinal2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.List;
			public class E {
			    public void foo() {
			        List<String> i= null, j= null;
			        Runnable run= new Runnable() {
			            public void run() {
			                Object x= i;
			            }
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import java.util.List;
			public class E {
			    public void foo() {
			        final List<String> i= null;
			        List<String> j= null;
			        Runnable run= new Runnable() {
			            public void run() {
			                Object x= i;
			            }
			        };
			    }
			}
			""";
		assertEqualString(preview, str1);
	}


	@Test
	public void testOuterParameterMustBeFinal() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int i) {
			        Runnable run= new Runnable() {
			            public void run() {
			                int x= i;
			            }
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E {
			    public void foo(final int i) {
			        Runnable run= new Runnable() {
			            public void run() {
			                int x= i;
			            }
			        };
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testOuterForParamMustBeFinal() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        for (int i= 1; true;) {
			            Runnable run= new Runnable() {
			                public void run() {
			                    int x= i;
			                }
			            };
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        for (final int i= 1; true;) {
			            Runnable run= new Runnable() {
			                public void run() {
			                    int x= i;
			                }
			            };
			        }
			    }
			}
			""";
		assertEqualString(preview, str1);
	}


	@Test
	public void testMethodRequiresBody() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo();
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public abstract class E {
			    public abstract void foo();
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}

	@Test
	public void testMethodRequiresBody2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public int foo();
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public int foo() {
			        return 0;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public abstract class E {
			    public abstract int foo();
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testMethodRequiresBody3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public static class E1 {
			        public int foo();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public static class E1 {
			        public int foo() {
			            return 0;
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public static abstract class E1 {
			        public abstract int foo();
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testNeedToEmulateMethodAccess() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_SYNTHETIC_ACCESS_EMULATION, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void x() {
			        Runnable r= new Runnable() {
			            public void run() {
			                foo();
			            }
			        };
			    }
			    private void foo() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E {
			    public void x() {
			        Runnable r= new Runnable() {
			            public void run() {
			                foo();
			            }
			        };
			    }
			    void foo() {
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testNeedToEmulateConstructorAccess() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_SYNTHETIC_ACCESS_EMULATION, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void x() {
			        Runnable r= new Runnable() {
			            public void run() {
			               E e= new E();
			            }
			        };
			    }
			    private E() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E {
			    public void x() {
			        Runnable r= new Runnable() {
			            public void run() {
			               E e= new E();
			            }
			        };
			    }
			    E() {
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testNeedToEmulateFieldRead() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_SYNTHETIC_ACCESS_EMULATION, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void x() {
			        Runnable r= new Runnable() {
			            public void run() {
			               int x= fCount;
			            }
			        };
			    }
			    private int fCount; {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E {
			    public void x() {
			        Runnable r= new Runnable() {
			            public void run() {
			               int x= fCount;
			            }
			        };
			    }
			    int fCount; {
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testNeedToEmulateFieldReadInGeneric() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_SYNTHETIC_ACCESS_EMULATION, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E<T> {
			    public void x() {
			        Runnable r= new Runnable() {
			            public void run() {
			               T x= fCount;
			            }
			        };
			    }
			    private T fCount; {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E<T> {
			    public void x() {
			        Runnable r= new Runnable() {
			            public void run() {
			               T x= fCount;
			            }
			        };
			    }
			    T fCount; {
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testNeedToEmulateFieldWrite() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_SYNTHETIC_ACCESS_EMULATION, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void x() {
			        Runnable r= new Runnable() {
			            public void run() {
			               fCount= 2;
			            }
			        };
			    }
			    private int fCount; {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E {
			    public void x() {
			        Runnable r= new Runnable() {
			            public void run() {
			               fCount= 2;
			            }
			        };
			    }
			    int fCount; {
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testSetFinalVariable1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private final int x= 9;
			    public void foo() {
			        x= 8;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E {
			    private int x= 9;
			    public void foo() {
			        x= 8;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testSetFinalVariable2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private final int x;
			    public E() {
			        x= 8;
			        x= 9;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E {
			    private int x;
			    public E() {
			        x= 8;
			        x= 9;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testSetFinalVariable3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public E() {
			        final int x= 8;
			        x= 9;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E {
			    public E() {
			        int x= 8;
			        x= 9;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testSetFinalVariable4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.ArrayList;
			import java.util.List;
			public class E {
			    final List<String> a= null, x= a, y= a;
			    public void foo() {
			        x= new ArrayList<String>();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import java.util.ArrayList;
			import java.util.List;
			public class E {
			    final List<String> a= null;
			    List<String> x= a;
			    final List<String> y= a;
			    public void foo() {
			        x= new ArrayList<String>();
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testSetFinalVariableInGeneric() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		String str= """
			package test2;
			public class C<T> {
			        T x;
			}
			""";
		pack2.createCompilationUnit("C.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo(test2.C<String> c) {
			         c.x= null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(1);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test2;
			public class C<T> {
			        public T x;
			}
			""";
		assertEqualString(preview, str2);
	}


	@Test
	public void testOverrideFinalMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			public class C {
			    protected final void foo() {
			    }
			}
			""";
		pack1.createCompilationUnit("C.java", str, false, null);

		String str1= """
			package test1;
			public class E extends C {
			    protected void foo() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class C {
			    protected void foo() {
			    }
			}
			""";
		assertEqualString(preview, str2);
	}


	@Test
	public void testOverridesNonVisibleMethod() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);

		String str= """
			package test2;
			public class C {
			    void foo() {
			    }
			}
			""";
		pack2.createCompilationUnit("C.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			import test2.C;
			public class E extends C {
			    public void foo() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test2;
			public class C {
			    public void foo() {
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testOverridesStaticMethod() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);

		String str= """
			package test2;
			public class C {
			    public static void foo() {
			    }
			}
			""";
		pack2.createCompilationUnit("C.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			import test2.C;
			public class E extends C {
			    public void foo() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test2;
			public class C {
			    public void foo() {
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testStaticOverridesMethod() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);

		String str= """
			package test2;
			public class C {
			    public void foo() {
			    }
			}
			""";
		pack2.createCompilationUnit("C.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			import test2.C;
			public class E extends C {
			    public static void foo() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			import test2.C;
			public class E extends C {
			    public void foo() {
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testOverridesMoreVisibleMethod() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);

		String str= """
			package test2;
			public class C {
			    public void foo() {
			    }
			}
			""";
		pack2.createCompilationUnit("C.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			import test2.C;
			public class E extends C {
			    protected void foo() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test2;
			public class C {
			    protected void foo() {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import test2.C;
			public class E extends C {
			    public void foo() {
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}

	@Test
	public void testOverridesMoreVisibleMethodInGeneric() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);

		String str= """
			package test2;
			public class C<T> {
			    public <M> T foo(M m) {
			    }
			}
			""";
		pack2.createCompilationUnit("C.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			import test2.C;
			public class E extends C<String> {
			    protected <M> String foo(M m) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test2;
			public class C<T> {
			    protected <M> T foo(M m) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import test2.C;
			public class E extends C<String> {
			    public <M> String foo(M m) {
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}


	@Test
	public void testInvalidInterfaceModifiers() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public static interface E  {
			    public void foo();
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public interface E  {
			    public void foo();
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testInvalidMemberInterfaceModifiers() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public interface E  {
			    private interface Inner {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public interface E  {
			    interface Inner {
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testInvalidInterfaceFieldModifiers() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public interface E  {
			    public native int i;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public interface E  {
			    public int i;
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testInvalidInterfaceMethodModifiers() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public interface E  {
			    private strictfp void foo();
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public interface E  {
			    void foo();
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testInvalidClassModifiers() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public volatile class E  {
			    public void foo() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E  {
			    public void foo() {
			    }
			}
			""";
		assertEqualString(preview, str1);
	}


	@Test
	public void testInvalidMemberClassModifiers() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public interface E  {
			    private class Inner {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public interface E  {
			    class Inner {
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testInvalidLocalClassModifiers() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E  {
			    private void foo() {
			        static class Local {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E  {
			    private void foo() {
			        class Local {
			        }
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testInvalidClassFieldModifiers() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E  {
			    strictfp public native int i;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E  {
			    public int i;
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testInvalidClassMethodModifiers() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public abstract class E  {
			    volatile abstract void foo();
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public abstract class E  {
			    abstract void foo();
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testInvalidConstructorModifiers() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			public class Bug {
			    public static Bug() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Bug.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package pack;
			
			public class Bug {
			    public Bug() {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
		}


	@Test
	public void testInvalidParamModifiers() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E  {
			    private void foo(private int x) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E  {
			    private void foo(int x) {
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testInvalidVariableModifiers() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E  {
			    private void foo() {
			        native int x;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E  {
			    private void foo() {
			        int x;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testInvalidMultiVariableModifiers() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E  {
			    public E() {
			        @Deprecated final @SuppressWarnings("all") int a, b, c;
			        a = 0; b = 0; c = 12;
			        b = 5;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E  {
			    public E() {
			        @Deprecated final @SuppressWarnings("all") int a;
			        @Deprecated @SuppressWarnings("all") int b;
			        @Deprecated final @SuppressWarnings("all") int c;
			        a = 0; b = 0; c = 12;
			        b = 5;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testInvalidMultiFieldModifiers() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E  {
			    private final int a, b;
			    public E() {
			        a = 0; b = 0;
			        a = 5;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E  {
			    private int a;
			    private final int b;
			    public E() {
			        a = 0; b = 0;
			        a = 5;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testInvalidMultiFieldModifiers2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E  {
			    private final int b, a;
			    public E() {
			        a = 0; b = 0;
			        a = 5;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E  {
			    private final int b;
			    private int a;
			    public E() {
			        a = 0; b = 0;
			        a = 5;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testExtendsFinalClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			public final class C {
			    protected void foo() {
			    }
			}
			""";
		pack1.createCompilationUnit("C.java", str, false, null);

		String str1= """
			package test1;
			public class E extends C {
			    protected void foo() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class C {
			    protected void foo() {
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testExtendsFinalClass2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			public final class C {
			    protected void foo() {
			    }
			}
			""";
		pack1.createCompilationUnit("C.java", str, false, null);

		String str1= """
			package test1;
			public class E {
			    protected void foo() {
			        C c= new C() { };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class C {
			    protected void foo() {
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testMissingOverrideAnnotation() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_MISSING_DEPRECATED_ANNOTATION, JavaCore.ERROR);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			public final class C {
			    public String toString() {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public final class C {
			    @Override
			    public String toString() {
			        return null;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testMissingTypeDeprecationAnnotation() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_MISSING_DEPRECATED_ANNOTATION, JavaCore.ERROR);
		JavaCore.setOptions(options);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			/**
			 * @deprecated
			 */
			public final class C {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			/**
			 * @deprecated
			 */
			@Deprecated
			public final class C {
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testMissingMethodDeprecationAnnotation() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_MISSING_DEPRECATED_ANNOTATION, JavaCore.ERROR);
		JavaCore.setOptions(options);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			public final class C {
			    /**
			     * @deprecated
			     */
			    @Override
			    public String toString() {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public final class C {
			    /**
			     * @deprecated
			     */
			    @Deprecated
			    @Override
			    public String toString() {
			        return null;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testMissingFieldDeprecationAnnotation() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_MISSING_DEPRECATED_ANNOTATION, JavaCore.ERROR);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			public final class C {
			    /**
			     * @deprecated
			     */
			    public String name;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public final class C {
			    /**
			     * @deprecated
			     */
			    @Deprecated
			    public String name;
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testSuppressNLSWarningAnnotation1() throws Exception {

		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_NON_NLS_STRING_LITERAL, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    static {
			        @SuppressWarnings("unused") String str= "foo";\
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		String[] expected= new String[2];
		expected[0]= """
			package test1;
			public class E {
			    static {
			        @SuppressWarnings({"unused", "nls"}) String str= "foo";\
			    }
			}
			""";

		expected[1]= """
			package test1;
			@SuppressWarnings("nls")
			public class E {
			    static {
			        @SuppressWarnings("unused") String str= "foo";\
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testSuppressNLSWarningAnnotation2() throws Exception {

		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_NON_NLS_STRING_LITERAL, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;\s
			public class E {
			    private class Q {
			        String s = "";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String str1= """
			package test1;\s
			public class E {
			    private class Q {
			        @SuppressWarnings("nls")
			        String s = "";
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str1});
	}

	@Test
	public void testSuppressNLSWarningAnnotation3() throws Exception {

		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_NON_NLS_STRING_LITERAL, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;\s
			public class E {
			    String s = "";
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String str1= """
			package test1;\s
			public class E {
			    @SuppressWarnings("nls")
			    String s = "";
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str1});
	}

	@Test
	public void testSuppressNLSWarningAnnotation4() throws Exception {

		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_NON_NLS_STRING_LITERAL, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;\s
			public class E {
			    public void foo() {
			        @SuppressWarnings("unused") String s = "";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		String[] expected= new String[2];
		expected[0]= """
			package test1;\s
			public class E {
			    public void foo() {
			        @SuppressWarnings({"unused", "nls"}) String s = "";
			    }
			}
			""";

		expected[1]= """
			package test1;\s
			public class E {
			    @SuppressWarnings("nls")
			    public void foo() {
			        @SuppressWarnings("unused") String s = "";
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testSuppressNLSWarningAnnotation5() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.WARNING);
		JavaCore.setOptions(options);


		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;\s
			import java.util.ArrayList;
			
			public class A {
			    public void foo(ArrayList<String> c) {
			        new ArrayList(c);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[1];
		expected[0]= """
			package test1;\s
			import java.util.ArrayList;
			
			public class A {
			    @SuppressWarnings("unchecked")
			    public void foo(ArrayList<String> c) {
			        new ArrayList(c);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testSuppressWarningsForLocalVariables() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;\s
			import java.util.ArrayList;
			
			public class A {
			    public void foo() {
			         @SuppressWarnings("unused")
			         ArrayList localVar= new ArrayList();\
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 5);
		String[] expected= new String[2];
		expected[0]= """
			package test1;\s
			import java.util.ArrayList;
			
			public class A {
			    public void foo() {
			         @SuppressWarnings({"unused", "rawtypes"})
			         ArrayList localVar= new ArrayList();\
			    }
			}
			""";

		expected[1]= """
			package test1;\s
			import java.util.ArrayList;
			
			public class A {
			    @SuppressWarnings("rawtypes")
			    public void foo() {
			         @SuppressWarnings("unused")
			         ArrayList localVar= new ArrayList();\
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testSuppressWarningsForFieldVariables() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;\s
			import java.util.*;
			
			public class A {
			    List<String> myList = new ArrayList();
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[1];
		expected[0]= """
			package test1;\s
			import java.util.*;
			
			public class A {
			    @SuppressWarnings("unchecked")
			    List<String> myList = new ArrayList();
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testSuppressWarningsForFieldVariables2() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;\s
			import java.util.ArrayList;
			
			class A {
			    @SuppressWarnings("rawtypes")
			    ArrayList array;
			    boolean a= array.add(1), b= array.add(1);
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[1];
		expected[0]= """
			package test1;\s
			import java.util.ArrayList;
			
			class A {
			    @SuppressWarnings("rawtypes")
			    ArrayList array;
			    @SuppressWarnings("unchecked")
			    boolean a= array.add(1), b= array.add(1);
			}
			""";

		assertExpectedExistInProposals(proposals, expected);

	}

	@Test
	public void testSuppressWarningsForMethodParameters() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;\s
			import java.util.*;
			
			public class A {
			    public int foo(int param1, List param2) {
			         return param1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 5);
		String[] expected= new String[2];
		expected[0]= """
			package test1;\s
			import java.util.*;
			
			public class A {
			    public int foo(int param1, @SuppressWarnings("rawtypes") List param2) {
			         return param1;
			    }
			}
			""";

		expected[1]= """
			package test1;\s
			import java.util.*;
			
			public class A {
			    @SuppressWarnings("rawtypes")
			    public int foo(int param1, List param2) {
			         return param1;
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, expected);

	}

	@Test
	public void testSuppressWarningsAnonymousClass1() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_LOCAL_VARIABLE, JavaCore.INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_LOCAL_VARIABLE, DefaultCodeFormatterConstants.createAlignmentValue(true, DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE));
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;\s
			import java.util.*;
			
			public class A {
			    public void foo() {
			        @SuppressWarnings("unused")
			        final Object object = new Object() {
			            {
			                for (List l = new ArrayList(), x = new Vector();;) {
			                    if (l == x)
			                        break;
			                }
			            }
			        };
			    };
			};
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 3);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 6);
		String[] expected= new String[3];

		expected[0]= """
			package test1;\s
			import java.util.*;
			
			public class A {
			    public void foo() {
			        @SuppressWarnings("unused")
			        final Object object = new Object() {
			            {
			                for (@SuppressWarnings("rawtypes")
			                List l = new ArrayList(), x = new Vector();;) {
			                    if (l == x)
			                        break;
			                }
			            }
			        };
			    };
			};
			""";

		expected[1]= """
			package test1;\s
			import java.util.*;
			
			public class A {
			    public void foo() {
			        @SuppressWarnings({"unused", "rawtypes"})
			        final Object object = new Object() {
			            {
			                for (List l = new ArrayList(), x = new Vector();;) {
			                    if (l == x)
			                        break;
			                }
			            }
			        };
			    };
			};
			""";

		expected[1]= """
			package test1;\s
			import java.util.*;
			
			public class A {
			    @SuppressWarnings("rawtypes")
			    public void foo() {
			        @SuppressWarnings("unused")
			        final Object object = new Object() {
			            {
			                for (List l = new ArrayList(), x = new Vector();;) {
			                    if (l == x)
			                        break;
			                }
			            }
			        };
			    };
			};
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testSuppressWarningsAnonymousClass2() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;\s
			import java.util.*;
			
			public class A {
			    final Runnable r= new Runnable() {
			        public void run() {
			            boolean b;
			            for (b = new ArrayList().add(1);;) {
			                if (b)
			                    return;
			                        break;
			            }
			        }
			    };
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);
		String[] expected= new String[1];
		expected[0]= """
			package test1;\s
			import java.util.*;
			
			public class A {
			    final Runnable r= new Runnable() {
			        @SuppressWarnings("unchecked")
			        public void run() {
			            boolean b;
			            for (b = new ArrayList().add(1);;) {
			                if (b)
			                    return;
			                        break;
			            }
			        }
			    };
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}
	@Test
	public void testMisspelledSuppressToken() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("a", false, null);
		String str= """
			package a;
			
			public class A {
			    @SuppressWarnings("unusd")
			    public static void main(String[] args) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		String[] expected= new String[2];
		expected[0]= """
			package a;
			
			public class A {
			    @SuppressWarnings("unused")
			    public static void main(String[] args) {
			    }
			}
			""";

		expected[1]= """
			package a;
			
			public class A {
			    public static void main(String[] args) {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testSuppressBug169446() throws Exception {

		IPackageFragment other= fSourceFolder.createPackageFragment("other", false, null);
		String str= """
			package other;\s
			
			public @interface SuppressWarnings {
			    String value();
			}
			""";
		other.createCompilationUnit("SuppressWarnings.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("a.b", false, null);

		String str1= """
			package a.b;
			
			public class E {
			    @Deprecated()
			    public void foo() {
			    }
			}
			""";
		pack1.createCompilationUnit("E.java", str1, false, null);

		String str2= """
			package a.b;
			
			import other.SuppressWarnings;
			
			public class Test {
			    @SuppressWarnings("BC")
			    public void foo() {
			        new E().foo();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", str2, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[1];
		expected[0]= """
			package a.b;
			
			import other.SuppressWarnings;
			
			public class Test {
			    @java.lang.SuppressWarnings("deprecation")
			    @SuppressWarnings("BC")
			    public void foo() {
			        new E().foo();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testSuppressWarningInImports() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			import java.util.Vector;
			
			public class A {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);

		String[] expected= new String[1];

		expected[0]= """
			package p;
			
			import java.util.Vector;
			
			@SuppressWarnings("unused")
			public class A {
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedSuppressWarnings1() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNHANDLED_WARNING_TOKEN, JavaCore.IGNORE);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_WARNING_TOKEN, JavaCore.WARNING);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			@SuppressWarnings(value="unused")
			public class E {
			
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[1];
		expected[0]= """
			package p;
			
			public class E {
			
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedSuppressWarnings2() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNHANDLED_WARNING_TOKEN, JavaCore.IGNORE);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_WARNING_TOKEN, JavaCore.WARNING);
		JavaCore.setOptions(hashtable);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			@SuppressWarnings("unused")
			public class E {
			
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[1];
		expected[0]= """
			package p;
			
			public class E {
			
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedSuppressWarnings3() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNHANDLED_WARNING_TOKEN, JavaCore.IGNORE);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_WARNING_TOKEN, JavaCore.WARNING);
		JavaCore.setOptions(hashtable);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			@SuppressWarnings({ "unused", "X" })
			public class E {
			
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[1];
		expected[0]= """
			package p;
			
			@SuppressWarnings({ "X" })
			public class E {
			
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}


	@Test
	public void testMakeFinalBug129165() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.Serializable;
			public class E {
			    @SuppressWarnings("serial")
			    public void foo() {
			        int i= 1, j= i + 1, h= j + 1;
			        Serializable ser= new Serializable() {
			            public void bar() {
			                System.out.println(j);
			            }
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import java.io.Serializable;
			public class E {
			    @SuppressWarnings("serial")
			    public void foo() {
			        int i= 1;
			        final int j= i + 1;
			        int h= j + 1;
			        Serializable ser= new Serializable() {
			            public void bar() {
			                System.out.println(j);
			            }
			        };
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testStaticFieldInInnerClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			public class E {
			    class F {
			        static int x;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		expected[0]= """
			package pack;
			
			public class E {
			    class F {
			        int x;
			    }
			}
			""";

		expected[1]= """
			package pack;
			
			public class E {
			    static class F {
			        static int x;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testStaticMethodInInnerClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			public class E {
			    class F {
			        static int foo() {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		expected[0]= """
			package pack;
			
			public class E {
			    class F {
			        int foo() {
			        }
			    }
			}
			""";

		expected[1]= """
			package pack;
			
			public class E {
			    static class F {
			        static int foo() {
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testFinalVolatileField() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			public class E {
			    class F {
			        volatile final int x;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		expected[0]= """
			package pack;
			
			public class E {
			    class F {
			        final int x;
			    }
			}
			""";

		expected[1]= """
			package pack;
			
			public class E {
			    class F {
			        volatile int x;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}


	@Test
	public void testOverrideAnnotationButNotOverriding() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);

		String str= """
			package pack;
			
			public class OtherMachine {
			}
			""";
		pack1.createCompilationUnit("OtherMachine.java", str, false, null);

		String str1= """
			package pack;
			
			import java.io.IOException;
			
			public class Machine extends OtherMachine {
			    @Override
			    public boolean isAlive(OtherMachine m) throws IOException {
			        return true;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Machine.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		expected[0]= """
			package pack;
			
			import java.io.IOException;
			
			public class Machine extends OtherMachine {
			    public boolean isAlive(OtherMachine m) throws IOException {
			        return true;
			    }
			}
			""";

		expected[1]= """
			package pack;
			
			import java.io.IOException;
			
			public class OtherMachine {
			
			    public boolean isAlive(OtherMachine m) throws IOException {
			        return false;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testCreateMethodWhenOverrideAnnotation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);

		String str= """
			package pack;
			
			public abstract class OtherMachine {
			}
			""";
		pack1.createCompilationUnit("OtherMachine.java", str, false, null);

		String str1= """
			package pack;
			
			public abstract class Machine extends OtherMachine {
			    @Override
			    public abstract void m1();
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Machine.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		expected[0]= """
			package pack;
			
			public abstract class OtherMachine {
			
			    public abstract void m1();
			}
			""";

		expected[1]= """
			package pack;
			
			public abstract class Machine extends OtherMachine {
			    public abstract void m1();
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testMethodOverrideDeprecated1() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEPRECATION_WHEN_OVERRIDING_DEPRECATED_METHOD, JavaCore.ENABLED);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class E {
			    @Deprecated
			    public void foo() {
			    }
			}   \s
			
			class F extends E {
			    public void foo() {
			    }
			}
			
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		String[] expected= new String[2];
		expected[0]= """
			package pack;
			public class E {
			    @Deprecated
			    public void foo() {
			    }
			}   \s
			
			class F extends E {
			    @Deprecated
			    public void foo() {
			    }
			}
			
			""";

		expected[1]= """
			package pack;
			public class E {
			    @Deprecated
			    public void foo() {
			    }
			}   \s
			
			class F extends E {
			    @SuppressWarnings("deprecation")
			    public void foo() {
			    }
			}
			
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testMethodOverrideDeprecated2() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEPRECATION_WHEN_OVERRIDING_DEPRECATED_METHOD, JavaCore.ENABLED);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class E {
			    @Deprecated
			    public void foo() {
			    }
			}   \s
			
			class F extends E {
			    /**
			     */
			    public void foo() {
			    }
			}
			
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		String[] expected= new String[2];
		expected[0]= """
			package pack;
			public class E {
			    @Deprecated
			    public void foo() {
			    }
			}   \s
			
			class F extends E {
			    /**
			     * @deprecated
			     */
			    @Deprecated
			    public void foo() {
			    }
			}
			
			""";

		expected[1]= """
			package pack;
			public class E {
			    @Deprecated
			    public void foo() {
			    }
			}   \s
			
			class F extends E {
			    /**
			     */
			    @SuppressWarnings("deprecation")
			    public void foo() {
			    }
			}
			
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testAbstractMethodInEnumWithoutEnumConstants() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("r", false, null);
		String str= """
			package r;
			
			enum E {
			    ;
			    public abstract boolean foo();
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package r;
			
			enum E {
			    ;
			    public boolean foo() {
			        return false;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testInvalidEnumModifier() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("r", false, null);
		String str= """
			package r;
			
			private final strictfp enum E {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package r;
			
			strictfp enum E {
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testInvalidEnumModifier2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("r", false, null);
		String str= """
			package r;
			
			public abstract enum E {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package r;
			
			public enum E {
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testInvalidEnumConstantModifier() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("r", false, null);
		String str= """
			package r;
			
			enum E {
				private final WHITE;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package r;
			
			enum E {
				WHITE;
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testInvalidEnumConstructorModifier() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("r", false, null);
		String str= """
			package r;
			
			enum E {
				WHITE(1);
			
				public final E(int foo) {
				}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package r;
			
			enum E {
				WHITE(1);
			
				E(int foo) {
				}
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testInvalidMemberEnumModifier() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("r", false, null);
		String str= """
			package r;
			
			class E {
				final enum A {
				}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package r;
			
			class E {
				enum A {
				}
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testMissingSynchronizedOnInheritedMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("r", false, null);
		String str= """
			package r;
			
			public class A {
				protected synchronized void foo() {
				}
			}
			""";
		pack1.createCompilationUnit("A.java", str, false, null);

		String str1= """
			package r;
			
			class B extends A {
				protected void foo() {
				}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("B.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[1];
		expected[0]= """
			package r;
			
			class B extends A {
				protected synchronized void foo() {
				}
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testMethodCanBeStatic() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_MISSING_STATIC_ON_METHOD, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_POTENTIALLY_MISSING_STATIC_ON_METHOD, JavaCore.WARNING);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private void foo() {
			        System.out.println("doesn't need class");
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E {
			    private static void foo() {
			        System.out.println("doesn't need class");
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testMethodCanPotentiallyBeStatic() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_MISSING_STATIC_ON_METHOD, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_POTENTIALLY_MISSING_STATIC_ON_METHOD, JavaCore.WARNING);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo() {
			        System.out.println("doesn't need class");
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String[] expected= new String[2];
		expected[0]= """
			package test1;
			public class E {
			    static void foo() {
			        System.out.println("doesn't need class");
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E {
			    @SuppressWarnings("static-method")
			    void foo() {
			        System.out.println("doesn't need class");
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}


	/**
	 * Quick Fix proposes wrong visibility for overriding/overridden method.
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=216898
	 *
	 * @throws Exception if anything goes wrong
	 * @since 3.9
	 */
	@Test
	public void testOverridingMethodIsPrivate() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			public abstract class A1 {
			  protected abstract void m1();
			}
			""";

		pack1.createCompilationUnit("A1.java", str, false, null);

		String str1= """
			package test1;
			public class B1 extends A1 {
			  private void m1() {
			  }
			}
			""";

		ICompilationUnit cu= pack1.createCompilationUnit("B1.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package test1;
			public class B1 extends A1 {
			  protected void m1() {
			  }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}


	/**
	 * Quick Fix proposes wrong visibility for overriding/overridden method.
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=216898#c1
	 *
	 * @throws Exception if anything goes wrong
	 * @since 3.9
	 */
	@Test
	public void testInvalidVisabilityOverrideMethod() throws Exception {
		// No simple solution to this problemID IProblem.AbstractMethodCannotBeOverridden
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);

		String str= """
			package test1;
			public abstract class Abs {
			  abstract String getName();
			}
			""";

		pack1.createCompilationUnit("Abs.java", str, false, null);

		String str1= """
			package test2;
			public class AbsImpl extends test1.Abs {
			  String getName() {
			    return "name";
			  }
			}
			""";

		ICompilationUnit cu= pack2.createCompilationUnit("AbsImpl.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 0);
	}

	/**
	 * Quick Fix proposes a visibility for method overriding/overridden from one interface.
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=551383
	 *
	 * @throws Exception if anything goes wrong
	 * @since 4.15
	 */
	@Test
	public void testProposesVisibilityFromOneInterfaceMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);

		String sample= """
			package test1;
			
			public interface Interface1 {
			  String getName();
			}
			""";
		pack1.createCompilationUnit("Interface1.java", sample, false, null);

		sample= """
			package test2;
			
			public class AbsImpl implements test1.Interface1 {
			  String getName() {
			    return "name";
			  }
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("AbsImpl.java", sample, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		List<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);
	}

	/**
	 * Quick Fix does not propose a visibility for method overriding/overridden from conflicting
	 * interfaces. https://bugs.eclipse.org/bugs/show_bug.cgi?id=551383
	 *
	 * @throws Exception if anything goes wrong
	 * @since 4.15
	 */
	@Test
	public void testDoNotProposeVisibilityFromDifferentInterfaceMethods() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);

		String sample= """
			package test;
			
			public interface Interface1 {
			  String getName();
			}
			""";
		pack.createCompilationUnit("Interface1.java", sample, false, null);

		sample= """
			package test;
			
			protected interface Interface2 {
			  protected String getName();
			}
			""";
		pack.createCompilationUnit("Interface2.java", sample, false, null);

		sample= """
			package test;
			
			public class AbsImpl implements Interface1, Interface2 {
			  String getName() {
			    return "name";
			  }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("AbsImpl.java", sample, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		List<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);
	}

	/**
	 * Quick Fix does not propose a visibility for method overriding/overridden from conflicting
	 * interfaces. https://bugs.eclipse.org/bugs/show_bug.cgi?id=551383
	 *
	 * @throws Exception if anything goes wrong
	 * @since 4.15
	 */
	@Test
	public void testProposeVisibilityFromIdenticalInterfaceMethods() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		IPackageFragment pack3= fSourceFolder.createPackageFragment("test3", false, null);

		String sample= """
			package test1;
			
			public interface Interface1 {
			  String getName();
			}
			""";
		pack1.createCompilationUnit("Interface1.java", sample, false, null);

		sample= """
			package test2;
			
			public interface Interface2 {
			  String getName();
			}
			""";
		pack2.createCompilationUnit("Interface2.java", sample, false, null);

		sample= """
			package test3;
			
			public class AbsImpl implements test1.Interface1, test2.Interface2 {
			  String getName() {
			    return "name";
			  }
			}
			""";
		ICompilationUnit cu= pack3.createCompilationUnit("AbsImpl.java", sample, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		List<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);
	}

	/**
	 * Quick Fix does not propose a visibility for method overriding/overridden from conflicting
	 * interfaces. https://bugs.eclipse.org/bugs/show_bug.cgi?id=551383
	 *
	 * @throws Exception if anything goes wrong
	 * @since 4.15
	 */
	@Test
	public void testProposeVisibilityFromMotherInterfaceMethods() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		IPackageFragment pack3= fSourceFolder.createPackageFragment("test3", false, null);

		String sample= """
			package test1;
			
			public interface Interface1 {
			  String getName();
			}
			""";
		pack1.createCompilationUnit("Interface1.java", sample, false, null);

		sample= """
			package test2;
			
			public interface Interface2 extends test1.Interface1 {
			}
			""";
		pack2.createCompilationUnit("Interface2.java", sample, false, null);

		sample= """
			package test3;
			
			public class AbsImpl implements test2.Interface2 {
			  String getName() {
			    return "name";
			  }
			}
			""";
		ICompilationUnit cu= pack3.createCompilationUnit("AbsImpl.java", sample, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		List<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);
	}

	/**
	 * Quick Fix proposes wrong visibility for overriding/overridden method.
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=216898#c1
	 *
	 * @throws Exception if anything goes wrong
	 * @since 3.9
	 */
	@Test
	public void test216898Comment1Variation() throws Exception {
		// Changing Abs.getName to protected by hand to allow solution for AbsImpl
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);

		String str= """
			package test1;
			public abstract class Abs {
			  protected abstract String getName();
			}
			""";

		pack1.createCompilationUnit("Abs.java", str, false, null);

		String str1= """
			package test2;
			public class AbsImpl extends test1.Abs {
			  String getName() {
			    return "name";
			  }
			}
			""";

		ICompilationUnit cu= pack2.createCompilationUnit("AbsImpl.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package test2;
			public class AbsImpl extends test1.Abs {
			  protected String getName() {
			    return "name";
			  }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	/**
	 * Wrong visibility for overriding method in interface.
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=87239
	 *
	 * @throws Exception if anything goes wrong
	 * @since 3.9
	 */
	@Test
	public void testImplementExtendSameMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			interface I {
			  void xxx();
			}
			class A {
			  void xxx() {}
			}
			class B extends A implements I {
			  void xxx() {}//error
			}
			""";
		ICompilationUnit cu=  pack1.createCompilationUnit("I.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package test1;
			interface I {
			  void xxx();
			}
			class A {
			  void xxx() {}
			}
			class B extends A implements I {
			  public void xxx() {}//error
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

}

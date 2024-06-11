/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Rabea Gransberger <rgransberger@gmx.de> - [quick fix] Fix several visibility issues - https://bugs.eclipse.org/394692
 *     Jens Reimann <jreimann@redhat.com> Bug 38201: [quick assist] Allow creating abstract method - https://bugs.eclipse.org/38201
 *     Red Hat Inc. - Bug 546819: Quickfix may remove varargs parameter
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.ArrayList;
import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class UnresolvedMethodsQuickFixTest extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup = new ProjectTestSetup();

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.COMPILER_PB_NO_EFFECT_ASSIGNMENT, JavaCore.IGNORE);
		options.put(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");
		options.put(JavaCore.CODEASSIST_STATIC_FIELD_PREFIXES, "fg");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= projectSetup.getProject();

		StubUtility.setCodeTemplate(CodeTemplateContextType.CATCHBLOCK_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "", null);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testMethodInSameType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector vec) {
			        int i= goo(vec, true);
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
			import java.util.Vector;
			public class E {
			    void foo(Vector vec) {
			        int i= goo(vec, true);
			    }
			
			    private int goo(Vector vec, boolean b) {
			        return 0;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testMethodInForInit() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo() {
			        for (int i= 0, j= goo(3); i < 0; i++) {
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
			    void foo() {
			        for (int i= 0, j= goo(3); i < 0; i++) {
			        }
			    }
			
			    private int goo(int i) {
			        return 0;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testMethodInInfixExpression1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private boolean foo() {
			        return f(1) || f(2);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E {
			    private boolean foo() {
			        return f(1) || f(2);
			    }
			
			    private boolean f(int i) {
			        return false;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testMethodInInfixExpression2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private boolean foo() {
			        return f(1) == f(2);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E {
			    private boolean foo() {
			        return f(1) == f(2);
			    }
			
			    private Object f(int i) {
			        return null;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testMethodSpacing0EmptyLines() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			
			    void fred() {
			    }
			    void foo(Vector vec) {
			        int i= goo(vec, true);
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
			import java.util.Vector;
			public class E {
			
			    void fred() {
			    }
			    void foo(Vector vec) {
			        int i= goo(vec, true);
			    }
			    private int goo(Vector vec, boolean b) {
			        return 0;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testMethodSpacing1EmptyLine() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			
			    void fred() {
			    }
			
			    void foo(Vector vec) {
			        int i= goo(vec, true);
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
			import java.util.Vector;
			public class E {
			
			    void fred() {
			    }
			
			    void foo(Vector vec) {
			        int i= goo(vec, true);
			    }
			
			    private int goo(Vector vec, boolean b) {
			        return 0;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testMethodSpacing2EmptyLines() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			
			    void fred() {
			    }
			   \s
			   \s
			    void foo(Vector vec) {
			        int i= goo(vec, true);
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
			import java.util.Vector;
			public class E {
			
			    void fred() {
			    }
			   \s
			   \s
			    void foo(Vector vec) {
			        int i= goo(vec, true);
			    }
			
			
			    private int goo(Vector vec, boolean b) {
			        return 0;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testMethodSpacingComment() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			
			    void fred() {
			    }
			
			//comment
			
			    void foo(Vector vec) {
			        int i= goo(vec, true);
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
			import java.util.Vector;
			public class E {
			
			    void fred() {
			    }
			
			//comment
			
			    void foo(Vector vec) {
			        int i= goo(vec, true);
			    }
			
			    private int goo(Vector vec, boolean b) {
			        return 0;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testMethodSpacingJavadoc() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			
			    void fred() {
			    }
			
			    /**
			     * javadoc
			     */
			    void foo(Vector vec) {
			        int i= goo(vec, true);
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
			import java.util.Vector;
			public class E {
			
			    void fred() {
			    }
			
			    /**
			     * javadoc
			     */
			    void foo(Vector vec) {
			        int i= goo(vec, true);
			    }
			
			    private int goo(Vector vec, boolean b) {
			        return 0;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testMethodSpacingNonJavadoc() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			
			    void fred() {
			    }
			
			    /*
			     * non javadoc
			     */
			    void foo(Vector vec) {
			        int i= goo(vec, true);
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
			import java.util.Vector;
			public class E {
			
			    void fred() {
			    }
			
			    /*
			     * non javadoc
			     */
			    void foo(Vector vec) {
			        int i= goo(vec, true);
			    }
			
			    private int goo(Vector vec, boolean b) {
			        return 0;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testMethodInSameTypeUsingThis() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector vec) {
			        int i= this.goo(vec, true);
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
			import java.util.Vector;
			public class E {
			    void foo(Vector vec) {
			        int i= this.goo(vec, true);
			    }
			
			    private int goo(Vector vec, boolean b) {
			        return 0;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testMethodInDifferentClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo(X x) {
			        if (x instanceof Y) {
			            boolean i= x.goo(1, 2.1);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			public class X {
			}
			""";
		pack1.createCompilationUnit("X.java", str1, false, null);

		String str2= """
			package test1;
			public interface Y {
			    public boolean goo(int i, double d);
			}
			""";
		pack1.createCompilationUnit("Y.java", str2, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class X {
			
			    public boolean goo(int i, double d) {
			        return false;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    void foo(X x) {
			        if (x instanceof Y) {
			            boolean i= ((Y) x).goo(1, 2.1);
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}

	@Test
	public void testParameterWithTypeVariable() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class Bork<T> {
			    private Help help = new Help();
			    public void method() {
			        help.help(this);
			    }
			}
			
			class Help {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Bork.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class Bork<T> {
			    private Help help = new Help();
			    public void method() {
			        help.help(this);
			    }
			}
			
			class Help {
			
			    public void help(Bork<T> bork) {
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testParameterAnonymous() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public E() {
			        foo(new Runnable() {
			            public void run() {}
			        });
			    }
			}
			""";
		ICompilationUnit cu1=pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public E() {
			        foo(new Runnable() {
			            public void run() {}
			        });
			    }
			
			    private void foo(Runnable runnable) {
			    }
			}
			""";

		assertEqualString(preview1, expected1);
	}

	@Test
	public void testMethodInGenericType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			public class X<A> {
			}
			""";
		pack1.createCompilationUnit("X.java", str, false, null);

		String str1= """
			package test1;
			public interface Y<A> {
			    public boolean goo(X<A> a);
			}
			""";
		pack1.createCompilationUnit("Y.java", str1, false, null);

		String str2= """
			package test1;
			public class E {
			    public Y<Object> y;
			    void foo(X<String> x) {
			        boolean i= x.goo(x);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str2, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class X<A> {
			
			    public boolean goo(X<String> x) {
			        return false;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public Y<Object> y;
			    void foo(X<String> x) {
			        boolean i= ((Y<Object>) x).goo(x);
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testMethodAssignedToWildcard() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector<? extends Number> vec) {
			        vec.add(goo());
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector<? extends Number> vec) {
			        vec.add(goo());
			    }
			
			    private Object goo() {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testMethodAssignedToWildcard2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector<? super Number> vec) {
			        vec.add(goo());
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector<? super Number> vec) {
			        vec.add(goo());
			    }
			
			    private Number goo() {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testMethodAssignedFromWildcard1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector<? super Number> vec) {
			        goo(vec.get(0));
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector<? super Number> vec) {
			        goo(vec.get(0));
			    }
			
			    private void goo(Object object) {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}


	@Test
	public void testMethodAssignedFromWildcard2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    void testMethod(Vector<? extends Number> vec) {
			        goo(vec.get(0));
			    }
			
			    private void goo(int i) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.Vector;
			public class E {
			    void testMethod(Vector<? extends Number> vec) {
			        goo(vec.get(0));
			    }
			
			    private void goo(Number number) {
			    }
			
			    private void goo(int i) {
			    }
			}
			""";

		String expected2= """
			package test1;
			import java.util.Vector;
			public class E {
			    void testMethod(Vector<? extends Number> vec) {
			        goo(vec.get(0));
			    }
			
			    private void goo(Number number) {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
	}

	@Test
	public void testMethodInvokedOnWildcard() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public abstract class E<T extends E<?>> {
			    abstract T self();\s
			    void testMethod(E<?> e) {
			        e.self().goo();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.Vector;
			public abstract class E<T extends E<?>> {
			    abstract T self();\s
			    void testMethod(E<?> e) {
			        e.self().goo();
			    }
			    private void goo() {
			    }
			}
			""";

		String expected2= """
			package test1;
			import java.util.Vector;
			public abstract class E<T extends E<?>> {
			    abstract T self();\s
			    void testMethod(E<?> e) {
			        ((Object) e.self()).goo();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
	}
	@Test
	public void testMethodInvokedOnBoundedTypeVariable() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public abstract class E<T extends E<?>> {
			    void testMethod(T t) {
			        t.goo();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.Vector;
			public abstract class E<T extends E<?>> {
			    void testMethod(T t) {
			        t.goo();
			    }
			
			    private void goo() {
			    }
			}
			""";

		String expected2= """
			package test1;
			import java.util.Vector;
			public abstract class E<T extends E<?>> {
			    void testMethod(T t) {
			        ((Object) t).goo();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
	}
	@Test
	public void testMethodInGenericTypeSameCU() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			public class E {
			    public class X<A> {
			    }
			    int foo(X<String> x) {
			        return x.goo(x);
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
			    public class X<A> {
			
			        public int goo(X<String> x) {
			            return 0;
			        }
			    }
			    int foo(X<String> x) {
			        return x.goo(x);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public class X<A> {
			    }
			    int foo(X<String> x) {
			        return ((Object) x).goo(x);
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}


	@Test
	public void testMethodInRawType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			public class X<A> {
			}
			""";
		pack1.createCompilationUnit("X.java", str, false, null);

		String str1= """
			package test1;
			public interface Y<A> {
			    public boolean goo(X<A> a);
			}
			""";
		pack1.createCompilationUnit("Y.java", str1, false, null);

		String str2= """
			package test1;
			public class E {
			    public Y<Object> y;
			    void foo(X x) {
			        boolean i= x.goo(x);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str2, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class X<A> {
			
			    public boolean goo(X x) {
			        return false;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public Y<Object> y;
			    void foo(X x) {
			        boolean i= ((Y<Object>) x).goo(x);
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}


	@Test
	public void testMethodInAnonymous1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        new Runnable() {
			            public void run() {
			                xoo();
			            }
			        };
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        new Runnable() {
			            public void run() {
			                xoo();
			            }
			
			            private void xoo() {
			            }
			        };
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public void foo() {
			        new Runnable() {
			            public void run() {
			                xoo();
			            }
			        };
			    }
			
			    protected void xoo() {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E {
			    public void foo() {
			        new Runnable() {
			            public void run() {
			                foo();
			            }
			        };
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testMethodInAnonymous2() throws Exception {
		IPackageFragment pack0= fSourceFolder.createPackageFragment("other", false, null);

		String str= """
			package other;
			public class A {
			}
			""";
		pack0.createCompilationUnit("A.java", str, false, null);


		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			import other.A;
			public class E {
			    public void foo() {
			        new Runnable() {
			            public void run() {
			                A.xoo();
			            }
			        };
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package other;
			public class A {
			
			    public static void xoo() {
			    }
			}
			""";

		assertEqualString(preview1, expected1);
	}

	@Test
	public void testMethodInAnonymous3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public static void foo() {
			        new Runnable() {
			            public void run() {
			                xoo();
			            }
			        };
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public static void foo() {
			        new Runnable() {
			            public void run() {
			                xoo();
			            }
			
			            private void xoo() {
			            }
			        };
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public static void foo() {
			        new Runnable() {
			            public void run() {
			                xoo();
			            }
			        };
			    }
			
			    protected static void xoo() {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E {
			    public static void foo() {
			        new Runnable() {
			            public void run() {
			                foo();
			            }
			        };
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testMethodInAnonymous4() throws Exception {
		// bug 266032
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public static void foo(final E e) {
			        new Runnable() {
			            public void run() {
			                e.foobar();
			            }
			        };
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public static void foo(final E e) {
			        new Runnable() {
			            public void run() {
			                e.foobar();
			            }
			        };
			    }
			
			    protected void foobar() {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public static void foo(final E e) {
			        new Runnable() {
			            public void run() {
			                ((Object) e).foobar();
			            }
			        };
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testMethodInAnonymousGenericType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E<T> {
			    public void foo() {
			        new Comparable<String>() {
			            public int compareTo(String s) {
			                xoo();
			            }
			        };
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E<T> {
			    public void foo() {
			        new Comparable<String>() {
			            public int compareTo(String s) {
			                xoo();
			            }
			
			            private void xoo() {
			            }
			        };
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E<T> {
			    public void foo() {
			        new Comparable<String>() {
			            public int compareTo(String s) {
			                xoo();
			            }
			        };
			    }
			
			    protected void xoo() {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E<T> {
			    public void foo() {
			        new Comparable<String>() {
			            public int compareTo(String s) {
			                foo();
			            }
			        };
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });

	}

	@Test
	public void testMethodInAnonymousCovering1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void run(int i) {
			    }
			    public void foo() {
			        new Runnable() {
			            public void run() {
			                run(1);
			            }
			        };
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void run(int i) {
			    }
			    public void foo() {
			        new Runnable() {
			            public void run() {
			                E.this.run(1);
			            }
			        };
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public void run(int i) {
			    }
			    public void foo() {
			        new Runnable() {
			            public void run() {
			                run();
			            }
			        };
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E {
			    public void run(int i) {
			    }
			    public void foo() {
			        new Runnable() {
			            public void run(int i) {
			                run(1);
			            }
			        };
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		String expected4= """
			package test1;
			public class E {
			    public void run(int i) {
			    }
			    public void foo() {
			        new Runnable() {
			            public void run() {
			                run(1);
			            }
			
			            private void run(int i) {
			            }
			        };
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });
	}

	@Test
	public void testMethodInAnonymousCovering2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public static void run(int i) {
			    }
			    public void foo() {
			        new Runnable() {
			            public void run() {
			                run(1);
			            }
			        };
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public static void run(int i) {
			    }
			    public void foo() {
			        new Runnable() {
			            public void run() {
			                E.run(1);
			            }
			        };
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public static void run(int i) {
			    }
			    public void foo() {
			        new Runnable() {
			            public void run() {
			                run();
			            }
			        };
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E {
			    public static void run(int i) {
			    }
			    public void foo() {
			        new Runnable() {
			            public void run(int i) {
			                run(1);
			            }
			        };
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		String expected4= """
			package test1;
			public class E {
			    public static void run(int i) {
			    }
			    public void foo() {
			        new Runnable() {
			            public void run() {
			                run(1);
			            }
			
			            private void run(int i) {
			            }
			        };
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });
	}

	@Test
	public void testMethodInAnonymousCovering3() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void run(int i) {
			    }
			    public class Inner {
			        public void run() {
			            run(1);
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void run(int i) {
			    }
			    public class Inner {
			        public void run() {
			            E.this.run(1);
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public void run(int i) {
			    }
			    public class Inner {
			        public void run() {
			            run();
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E {
			    public void run(int i) {
			    }
			    public class Inner {
			        public void run(int i) {
			            run(1);
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		String expected4= """
			package test1;
			public class E {
			    public void run(int i) {
			    }
			    public class Inner {
			        public void run() {
			            run(1);
			        }
			
			        private void run(int i) {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });
	}

	@Test
	public void testMethodInAnonymousCovering4() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void run(int i) {
			    }
			    public static class Inner {
			        public void run() {
			            run(1);
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void run(int i) {
			    }
			    public static class Inner {
			        public void run() {
			            run(1);
			        }
			
			        private void run(int i) {
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public void run(int i) {
			    }
			    public static class Inner {
			        public void run() {
			            run();
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E {
			    public void run(int i) {
			    }
			    public static class Inner {
			        public void run(int i) {
			            run(1);
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}




	@Test
	public void testMethodInDifferentInterface() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo(X x) {
			        boolean i= x.goo(getClass());
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			public interface X {
			}
			""";
		pack1.createCompilationUnit("X.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public interface X {
			
			    boolean goo(Class<? extends E> class1);
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    void foo(X x) {
			        boolean i= ((Object) x).goo(getClass());
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testMethodInArrayAccess() throws Exception {
		// bug 148011
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			public class E {
			    void foo() {
			        int i = bar()[0];
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
			package p;
			
			public class E {
			    void foo() {
			        int i = bar()[0];
			    }
			
			    private int[] bar() {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}



	@Test
	public void testParameterMismatchCast() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int i) {
			        long x= 0;
			        foo(x + 1);
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int i) {
			        long x= 0;
			        foo((int) (x + 1));
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public void foo(int i) {
			        long x= 0;
			        foo(x + 1);
			    }
			
			    private void foo(long l) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E {
			    public void foo(long l) {
			        long x= 0;
			        foo(x + 1);
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testParameterMismatchCast2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int i) {
			        double x= 0.0;
			        X.xoo((float) x, this);
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			public class X {
			    public static void xoo(int i, Object o) {
			    }
			}
			""";
		pack1.createCompilationUnit("X.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int i) {
			        double x= 0.0;
			        X.xoo((int) x, this);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class X {
			    public static void xoo(int i, Object o) {
			    }
			
			    public static void xoo(float x, E o) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class X {
			    public static void xoo(float x, Object o) {
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testParameterMismatchCastBoxing() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Integer i) {
			        foo(1.0);
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo(Integer i) {
			        foo((int) 1.0);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public void foo(Integer i) {
			        foo(1.0);
			    }
			
			    private void foo(double d) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E {
			    public void foo(double d) {
			        foo(1.0);
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testParameterMismatchChangeVarType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    public void goo(Vector v) {
			    }
			    public void foo() {
			        long x= 0;
			        goo(x);
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.Vector;
			public class E {
			    public void goo(long x) {
			    }
			    public void foo() {
			        long x= 0;
			        goo(x);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.Vector;
			public class E {
			    public void goo(Vector v) {
			    }
			    public void foo() {
			        long x= 0;
			        goo(x);
			    }
			    private void goo(long x) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			import java.util.Vector;
			public class E {
			    public void goo(Vector v) {
			    }
			    public void foo() {
			        Vector x= 0;
			        goo(x);
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testParameterMismatchChangeVarTypeInGeneric() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			import java.util.Vector;
			public class A<T> {
			    public void goo(Vector<T> v) {
			    }
			}
			""";
		pack1.createCompilationUnit("A.java", str, false, null);

		String str1= """
			package test1;
			public class E<T> {
			    public void foo(A<Number> a, long x) {
			        a.goo(x);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.Vector;
			public class A<T> {
			    public void goo(long x) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			
			import java.util.Vector;
			
			public class E<T> {
			    public void foo(A<Number> a, Vector<Number> x) {
			        a.goo(x);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			import java.util.Vector;
			public class A<T> {
			    public void goo(Vector<T> v) {
			    }
			
			    public void goo(long x) {
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}


	@Test
	public void testParameterMismatchKeepModifiers() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			import java.util.Collections;
			
			class E {
			    void foo(@Deprecated final String map){}
			    {foo(Collections.EMPTY_MAP);}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		expected[0]= """
			package test1;
			
			import java.util.Collections;
			import java.util.Map;
			
			class E {
			    void foo(@Deprecated final Map emptyMap){}
			    {foo(Collections.EMPTY_MAP);}
			}
			""";

		expected[1]= """
			package test1;
			
			import java.util.Collections;
			import java.util.Map;
			
			class E {
			    void foo(@Deprecated final String map){}
			    {foo(Collections.EMPTY_MAP);}
			    private void foo(Map emptyMap) {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}



	@Test
	public void testParameterMismatchChangeFieldType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    int fCount= 0;
			    public void goo(Vector v) {
			    }
			    public void foo() {
			        goo(fCount);
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.Vector;
			public class E {
			    int fCount= 0;
			    public void goo(int count) {
			    }
			    public void foo() {
			        goo(fCount);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.Vector;
			public class E {
			    int fCount= 0;
			    public void goo(Vector v) {
			    }
			    public void foo() {
			        goo(fCount);
			    }
			    private void goo(int count) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			import java.util.Vector;
			public class E {
			    Vector fCount= 0;
			    public void goo(Vector v) {
			    }
			    public void foo() {
			        goo(fCount);
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testParameterMismatchChangeFieldTypeInGeneric() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			public class X<A> {
			    String count= 0;
			}
			""";
		pack1.createCompilationUnit("X.java", str, false, null);

		String str1= """
			package test1;
			import java.util.Vector;
			public class E {
			    public void goo(Vector<String> v) {
			    }
			    public void foo(X<String> x, int y) {
			        goo(x.count);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			
			import java.util.Vector;
			
			public class X<A> {
			    Vector<String> count= 0;
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.Vector;
			public class E {
			    public void goo(String count) {
			    }
			    public void foo(X<String> x, int y) {
			        goo(x.count);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			import java.util.Vector;
			public class E {
			    public void goo(Vector<String> v) {
			    }
			    public void foo(X<String> x, int y) {
			        goo(x.count);
			    }
			    private void goo(String count) {
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testParameterMismatchChangeMethodType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    public void goo(Vector v) {
			    }
			    public int foo() {
			        goo(this.foo());
			        return 9;
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.Vector;
			public class E {
			    public void goo(int i) {
			    }
			    public int foo() {
			        goo(this.foo());
			        return 9;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.Vector;
			public class E {
			    public void goo(Vector v) {
			    }
			    public int foo() {
			        goo(this.foo());
			        return 9;
			    }
			    private void goo(int foo) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			import java.util.Vector;
			public class E {
			    public void goo(Vector v) {
			    }
			    public Vector foo() {
			        goo(this.foo());
			        return 9;
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testParameterMismatchChangeMethodTypeBug102142() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class Foo {
			    Foo(String string) {
			        System.out.println(string);
			    } \s
			    private void bar() {
			        new Foo(3);
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("Foo.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class Foo {
			    Foo(int i) {
			        System.out.println(i);
			    } \s
			    private void bar() {
			        new Foo(3);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class Foo {
			    Foo(String string) {
			        System.out.println(string);
			    } \s
			    public Foo(int i) {
			    }
			    private void bar() {
			        new Foo(3);
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2}, new String[] { expected1, expected2});
	}

	@Test
	public void testParameterMismatchChangeMethodTypeInGeneric() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E<T> {
			    public void goo(Vector<String> v) {
			    }
			    public int foo() {
			        goo(this.foo());
			        return 9;
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.Vector;
			public class E<T> {
			    public void goo(int i) {
			    }
			    public int foo() {
			        goo(this.foo());
			        return 9;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.Vector;
			public class E<T> {
			    public void goo(Vector<String> v) {
			    }
			    public int foo() {
			        goo(this.foo());
			        return 9;
			    }
			    private void goo(int foo) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			import java.util.Vector;
			public class E<T> {
			    public void goo(Vector<String> v) {
			    }
			    public Vector<String> foo() {
			        goo(this.foo());
			        return 9;
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}



	@Test
	public void testParameterMismatchLessArguments() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(String s, int i, Object o) {
			        int x= 0;
			        foo(x);
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo(String s, int i, Object o) {
			        int x= 0;
			        foo(s, x, o);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public void foo(String s, int i, Object o) {
			        int x= 0;
			        foo(x);
			    }
			
			    private void foo(int x) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E {
			    public void foo(int i) {
			        int x= 0;
			        foo(x);
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testParameterMismatchLessArguments2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        X.xoo(null);
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			public class X {
			    public static void xoo(int i, Object o) {
			    }
			}
			""";
		pack1.createCompilationUnit("X.java", str1, false, null);


		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        X.xoo(0, null);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class X {
			    public static void xoo(int i, Object o) {
			    }
			
			    public static void xoo(Object object) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class X {
			    public static void xoo(Object o) {
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testParameterMismatchLessArguments3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        X.xoo(1);
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			public class X {
			    /**
			     * @param i The int value
			     *                  More about the int value
			     * @param o The Object value
			     */
			    public static void xoo(int i, Object o) {
			    }
			}
			""";
		pack1.createCompilationUnit("X.java", str1, false, null);


		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        X.xoo(1, null);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class X {
			    /**
			     * @param i The int value
			     *                  More about the int value
			     * @param o The Object value
			     */
			    public static void xoo(int i, Object o) {
			    }
			
			    public static void xoo(int i) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class X {
			    /**
			     * @param i The int value
			     *                  More about the int value
			     */
			    public static void xoo(int i) {
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testParameterMismatchLessArgumentsInGeneric() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public interface X<S, T extends Number> {
			    public void foo(S s, int i, T t);
			}
			""";
		pack1.createCompilationUnit("X.java", str, false, null);

		String str1= """
			package test1;
			public abstract class E implements X<String, Integer> {
			    public void meth(E e, String s) {
			        int x= 0;
			        e.foo(x);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public abstract class E implements X<String, Integer> {
			    public void meth(E e, String s) {
			        int x= 0;
			        e.foo(s, x, x);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public abstract class E implements X<String, Integer> {
			    public void meth(E e, String s) {
			        int x= 0;
			        e.foo(x);
			    }
			
			    private void foo(int x) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public interface X<S, T extends Number> {
			    public void foo(int i);
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		String expected4= """
			package test1;
			public abstract class E implements X<String, Integer> {
			    public void meth(E e, String s) {
			        int x= 0;
			        e.foo(x);
			    }
			
			    protected abstract void foo(int x);
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });
	}

	@Test
	public void testCreateAbstractMethodInAbstractParent() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public abstract class A {
			    private class B {
			      void test () {
			        foo ();
			      }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public abstract class A {
			    private class B {
			      void test () {
			        foo ();
			      }
			
			    private void foo() {
			    }
			    }
			}
			""";

		String expected2= """
			package test1;
			public abstract class A {
			    private class B {
			      void test () {
			        foo ();
			      }
			    }
			
			    public void foo() {
			    }
			}
			""";

		String expected3= """
			package test1;
			public abstract class A {
			    private class B {
			      void test () {
			        foo ();
			      }
			    }
			
			    protected abstract void foo();
			}
			""";

		String[] previews= getAllPreviewContent(proposals);
		assertEqualStringsIgnoreOrder(previews, new String[] { expected1, expected2, expected3 });

		String[] displayStrings= getAllDisplayStrings(proposals);
		assertEqualStringsIgnoreOrder(displayStrings, new String[] {
				"Create method 'foo()'",
				"Create method 'foo()' in type 'A'",
				"Create abstract method 'foo()' in type 'A'"
		});
	}

	@Test
	public void testCreateAbstractMethodInAbstractParentWithAbstractClass() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public abstract class A {
			    private abstract class B {
			      void test () {
			        foo ();
			      }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public abstract class A {
			    private abstract class B {
			      void test () {
			        foo ();
			      }
			
			    private void foo() {
			    }
			    }
			}
			""";

		String expected2= """
			package test1;
			public abstract class A {
			    private abstract class B {
			      void test () {
			        foo ();
			      }
			    }
			
			    public void foo() {
			    }
			}
			""";

		String expected3= """
			package test1;
			public abstract class A {
			    private abstract class B {
			      void test () {
			        foo ();
			      }
			    }
			
			    protected abstract void foo();
			}
			""";

		String expected4= """
			package test1;
			public abstract class A {
			    private abstract class B {
			      void test () {
			        foo ();
			      }
			
			    protected abstract void foo();
			    }
			}
			""";

		String[] previews= getAllPreviewContent(proposals);
		assertEqualStringsIgnoreOrder(previews, new String[] { expected1, expected2, expected3, expected4 });

		String[] displayStrings= getAllDisplayStrings(proposals);
		assertEqualStringsIgnoreOrder(displayStrings, new String[] {
				"Create method 'foo()'",
				"Create method 'foo()' in type 'A'",
				"Create abstract method 'foo()' in type 'A'",
				"Create abstract method 'foo()'"
		});
	}

	@Test
	public void testSuperConstructorLessArguments() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class X {
			    public X(Object o, int i) {
			    }
			}
			""";

		pack1.createCompilationUnit("X.java", str, false, null);

		String str1= """
			package test1;
			import java.util.Vector;
			public class E extends X {
			    public E() {
			        super(new Vector());
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str1, false, null);


		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.Vector;
			public class E extends X {
			    public E() {
			        super(new Vector(), 0);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			
			import java.util.Vector;
			
			public class X {
			    public X(Object o, int i) {
			    }
			
			    public X(Vector vector) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class X {
			    public X(Object o) {
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testConstructorInvocationLessArguments() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    public E(Object o, int i) {
			    }
			    public E() {
			        this(new Vector());
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);


		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.Vector;
			public class E {
			    public E(Object o, int i) {
			    }
			    public E() {
			        this(new Vector(), 0);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.Vector;
			public class E {
			    public E(Object o) {
			    }
			    public E() {
			        this(new Vector());
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			import java.util.Vector;
			public class E {
			    public E(Object o, int i) {
			    }
			    public E() {
			        this(new Vector());
			    }
			    public E(Vector vector) {
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testConstructorInvocationLessArgumentsInGenericType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E<T> {
			    public E(Object o, int i) {
			    }
			    public E() {
			        this(new Vector());
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);


		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.Vector;
			public class E<T> {
			    public E(Object o, int i) {
			    }
			    public E() {
			        this(new Vector(), 0);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.Vector;
			public class E<T> {
			    public E(Object o) {
			    }
			    public E() {
			        this(new Vector());
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			import java.util.Vector;
			public class E<T> {
			    public E(Object o, int i) {
			    }
			    public E() {
			        this(new Vector());
			    }
			    public E(Vector vector) {
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testMethodMismatchVarargsAddedArguments() throws Exception { // fix for Bug 546819
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean b, String ... strings) {
			    }
			    public void foo1(int i) {
			        foo(true, 42);
			    }
			}
			""";
		ICompilationUnit cu1=pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean b, String ... strings) {
			    }
			    public void foo1(int i) {
			        foo(true);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public void foo(boolean b, int i, String ... strings) {
			    }
			    public void foo1(int i) {
			        foo(true, 42);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E {
			    public void foo(boolean b, String ... strings) {
			    }
			    public void foo1(int i) {
			        foo(true, 42);
			    }
			    private void foo(boolean b, int i) {
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}


	@Test
	public void testParameterMismatchMoreArguments() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(X x) {
			        x.xoo(1, 1, x.toString());
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			public class X {
			    public void xoo(int i, String o) {
			    }
			}
			""";
		pack1.createCompilationUnit("X.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo(X x) {
			        x.xoo(1, x.toString());
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class X {
			    public void xoo(int i, int j, String o) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class X {
			    public void xoo(int i, String o) {
			    }
			
			    public void xoo(int i, int j, String string) {
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testParameterMismatchMoreArguments2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(String s) {
			        int x= 0;
			        foo(s, x);
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo(String s) {
			        int x= 0;
			        foo(s);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public void foo(String s) {
			        int x= 0;
			        foo(s, x);
			    }
			
			    private void foo(String s, int x) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E {
			    public void foo(String s, int x2) {
			        int x= 0;
			        foo(s, x);
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testParameterMismatchMoreArguments3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Collections;
			public class E {
			    public void foo(X x) {
			        x.xoo(Collections.EMPTY_SET, 1, 2);
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			public class X {
			    /**
			     * @param i The int value
			     */
			    public void xoo(int i) {
			       int j= 0;
			    }
			}
			""";
		pack1.createCompilationUnit("X.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.Collections;
			public class E {
			    public void foo(X x) {
			        x.xoo(1);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			
			import java.util.Set;
			
			public class X {
			    /**
			     * @param emptySet\s
			     * @param i The int value
			     * @param k\s
			     */
			    public void xoo(Set emptySet, int i, int k) {
			       int j= 0;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			
			import java.util.Set;
			
			public class X {
			    /**
			     * @param i The int value
			     */
			    public void xoo(int i) {
			       int j= 0;
			    }
			
			    public void xoo(Set emptySet, int i, int j) {
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}


	@Test
	public void testParameterMismatchMoreArguments4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        Object[] o= null;
			        foo(o.length);
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        Object[] o= null;
			        foo();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public void foo() {
			        Object[] o= null;
			        foo(o.length);
			    }
			
			    private void foo(int length) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E {
			    public void foo(int length) {
			        Object[] o= null;
			        foo(o.length);
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testParameterMismatchMoreArgumentsInGeneric() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E<T> {
			    public void foo(X<T> x) {
			        x.xoo(x.toString(), x, 2);
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			public class X<T> {
			    /**
			     * @param i The int value
			     */
			    public void xoo(String s) {
			    }
			}
			""";
		pack1.createCompilationUnit("X.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);
		String expected= """
			package test1;
			public class E<T> {
			    public void foo(X<T> x) {
			        x.xoo(x.toString());
			    }
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });

		proposal= (CUCorrectionProposal) proposals.get(1);
		preview= getPreviewContent(proposal);
		expected= """
			package test1;
			public class X<T> {
			    /**
			     * @param i The int value
			     */
			    public void xoo(String s) {
			    }
			
			    public void xoo(String string, X<T> x, int i) {
			    }
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}


	@Test
	public void testSuperConstructorMoreArguments() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class X {
			    public X() {
			    }
			}
			""";

		pack1.createCompilationUnit("X.java", str, false, null);

		String str1= """
			package test1;
			import java.util.Vector;
			public class E extends X {
			    public E() {
			        super(new Vector());
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str1, false, null);


		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.Vector;
			public class E extends X {
			    public E() {
			        super();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			
			import java.util.Vector;
			
			public class X {
			    public X() {
			    }
			
			    public X(Vector vector) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			
			import java.util.Vector;
			
			public class X {
			    public X(Vector vector) {
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testConstructorInvocationMoreArguments() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    public E() {
			    }
			    public E(Object o, int i) {
			        this(new Vector());
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);


		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.Vector;
			public class E {
			    public E() {
			    }
			    public E(Object o, int i) {
			        this();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.Vector;
			public class E {
			    public E(Vector vector) {
			    }
			    public E(Object o, int i) {
			        this(new Vector());
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			import java.util.Vector;
			public class E {
			    public E() {
			    }
			    public E(Object o, int i) {
			        this(new Vector());
			    }
			    public E(Vector vector) {
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testConstructorInvocationMoreArguments2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    /**
			     * My favourite constructor.
			     */
			    public E() {
			    }
			    public E(Object o, int i) {
			        this(new Vector());
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);


		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.Vector;
			public class E {
			    /**
			     * My favourite constructor.
			     */
			    public E() {
			    }
			    public E(Object o, int i) {
			        this();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.Vector;
			public class E {
			    /**
			     * My favourite constructor.
			     * @param vector\s
			     */
			    public E(Vector vector) {
			    }
			    public E(Object o, int i) {
			        this(new Vector());
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			import java.util.Vector;
			public class E {
			    /**
			     * My favourite constructor.
			     */
			    public E() {
			    }
			    public E(Object o, int i) {
			        this(new Vector());
			    }
			    public E(Vector vector) {
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}



	@Test
	public void testParameterMismatchSwap() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int i, String[] o) {
			        foo(new String[] { }, i - 1);
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int i, String[] o) {
			        foo(i - 1, new String[] { });
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public void foo(int i, String[] o) {
			        foo(new String[] { }, i - 1);
			    }
			
			    private void foo(String[] strings, int i) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E {
			    public void foo(String[] o, int i) {
			        foo(new String[] { }, i - 1);
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testParameterMismatchSwapInGenericType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A<T> {
			    public void b(int i, T[] t) {
			    }
			}
			""";

		pack1.createCompilationUnit("A.java", str, false, null);

		String str1= """
			package test1;
			public enum E {
			    CONST1, CONST2;
			    public void foo(A<String> a) {
			        a.b(new String[1], 1);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class A<T> {
			    public void b(T[] t, int i) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class A<T> {
			    public void b(int i, T[] t) {
			    }
			
			    public void b(String[] strings, int i) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public enum E {
			    CONST1, CONST2;
			    public void foo(A<String> a) {
			        a.b(1, new String[1]);
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testParameterMismatchWithExtraDimensions() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class ArrayTest {
			        public void test(String[] a){
			                foo(a);
			        }
			        private void foo(int a[]) {
			        }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("ArrayTest.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		String[] expected= new String[3];
		expected[0]= """
			package test1;
			public class ArrayTest {
			        public void test(String[] a){
			                foo(a);
			        }
			        private void foo(String[] a) {
			        }
			}
			""";

		expected[1]= """
			package test1;
			public class ArrayTest {
			        public void test(String[] a){
			                foo(a);
			        }
			        private void foo(String[] a) {
			        }
			        private void foo(int a[]) {
			        }
			}
			""";

		expected[2]= """
			package test1;
			public class ArrayTest {
			        public void test(int[] a){
			                foo(a);
			        }
			        private void foo(int a[]) {
			        }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testParameterMismatchWithVarArgs() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class ArrayTest {
			        public void test(String[] a){
			                foo(a, a);
			        }
			        private void foo(int[] a, int... i) {
			        }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("ArrayTest.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		expected[0]= """
			package test1;
			public class ArrayTest {
			        public void test(String[] a){
			                foo(a, a);
			        }
			        private void foo(String[] a, String... a2) {
			        }
			}
			""";

		expected[1]= """
			package test1;
			public class ArrayTest {
			        public void test(String[] a){
			                foo(a, a);
			        }
			        private void foo(String[] a, String[] a2) {
			        }
			        private void foo(int[] a, int... i) {
			        }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testMissingParameterWithVarArgs() throws Exception { // test for Bug 521070
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class ArrayTest {
			        public void test(String a, String...b){
			                a.concat(c);
			        }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("ArrayTest.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);

		String[] expected= new String[1];
		expected[0]= """
			package test1;
			public class ArrayTest {
			        public void test(String a, String c, String...b){
			                a.concat(c);
			        }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testMissingParameterWithoutVarArgs() throws Exception { // test for Bug 521070
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class ArrayTest {
			        public void test(String a, String b){
			                a.concat(c);
			        }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("ArrayTest.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);

		String[] expected= new String[1];
		expected[0]= """
			package test1;
			public class ArrayTest {
			        public void test(String a, String b, String c){
			                a.concat(c);
			        }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}



	@Test
	public void testParameterMismatchSwap2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    /**
			     * @param i The int value
			     * @param o The Object value
			     * @param b The boolean value
			     *                  More about the boolean value
			     */
			    public void foo(int i, Object o, boolean b) {
			        foo(false, o, i - 1);
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    /**
			     * @param i The int value
			     * @param o The Object value
			     * @param b The boolean value
			     *                  More about the boolean value
			     */
			    public void foo(int i, Object o, boolean b) {
			        foo(i - 1, o, false);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    /**
			     * @param i The int value
			     * @param o The Object value
			     * @param b The boolean value
			     *                  More about the boolean value
			     */
			    public void foo(int i, Object o, boolean b) {
			        foo(false, o, i - 1);
			    }
			
			    private void foo(boolean b, Object o, int i) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E {
			    /**
			     * @param b The boolean value
			     *                  More about the boolean value
			     * @param o The Object value
			     * @param i The int value
			     */
			    public void foo(boolean b, Object o, int i) {
			        foo(false, o, i - 1);
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testSuperConstructor() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E extends A {
			    public E(int i) {
			        super(i);
			    }
			}
			""";
		ICompilationUnit cu1=pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			public class A {
			}
			""";
		pack1.createCompilationUnit("A.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class A {
			
			    public A(int i) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E extends A {
			    public E(int i) {
			        super();
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}


	@Test
	public void testClassInstanceCreation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int i) {
			        A a= new A(i);
			    }
			}
			""";
		ICompilationUnit cu1=pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			public class A {
			}
			""";
		pack1.createCompilationUnit("A.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class A {
			
			    public A(int i) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public void foo(int i) {
			        A a= new A();
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testClassInstanceCreation2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int i) {
			        A a= new A("test");
			    }
			    class A {
			    }
			}
			""";
		ICompilationUnit cu1=pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int i) {
			        A a= new A("test");
			    }
			    class A {
			
			        public A(String string) {
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public void foo(int i) {
			        A a= new A();
			    }
			    class A {
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}


	@Test
	public void testClassInstanceCreationInGenericType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int i) {
			        A<String> a= new A<String>(i);
			    }
			}
			""";
		ICompilationUnit cu1=pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			public class A<T> {
			}
			""";
		pack1.createCompilationUnit("A.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class A<T> {
			
			    public A(int i) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public void foo(int i) {
			        A<String> a= new A<String>();
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}


	@Test
	public void testClassInstanceCreationMoreArguments() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int i) {
			        A a= new A(i, String.valueOf(i), true);
			    }
			}
			""";
		ICompilationUnit cu1=pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			public class A {
			    public A(int i) {
			    }
			}
			""";
		pack1.createCompilationUnit("A.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class A {
			    public A(int i, String string, boolean b) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public void foo(int i) {
			        A a= new A(i);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class A {
			    public A(int i) {
			    }
			
			    public A(int i, String valueOf, boolean b) {
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testClassInstanceCreationVarargsAddedArguments() throws Exception { // fix for Bug 546819
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int i) {
			        A a= new A(i, true);
			    }
			}
			""";
		ICompilationUnit cu1=pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			public class A {
			    public A(boolean b, String ... strings) {
			    }
			}
			""";
		pack1.createCompilationUnit("A.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int i) {
			        A a= new A(true);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class A {
			    public A(int i, boolean b, String ... strings) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class A {
			    public A(boolean b, String ... strings) {
			    }
			
			    public A(int i, boolean b) {
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testClassInstanceCreationMoreArgumentsInGenericType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.List;
			public class E {
			    public void foo(int i) {
			        A<List<? extends E>> a= new A<List<? extends E>>(i, String.valueOf(i), true);
			    }
			}
			""";
		ICompilationUnit cu1=pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			public class A<T> {
			    public A(int i) {
			    }
			}
			""";
		pack1.createCompilationUnit("A.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class A<T> {
			    public A(int i, String string, boolean b) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.List;
			public class E {
			    public void foo(int i) {
			        A<List<? extends E>> a= new A<List<? extends E>>(i);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class A<T> {
			    public A(int i) {
			    }
			
			    public A(int i, String valueOf, boolean b) {
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testClassInstanceCreationLessArguments() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int i) {
			        A a= new A();
			    }
			}
			""";
		ICompilationUnit cu1=pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			public class A {
			    public A(int i, String s) {
			    }
			}
			""";
		pack1.createCompilationUnit("A.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class A {
			    public A() {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public void foo(int i) {
			        A a= new A(i, null);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class A {
			    public A(int i, String s) {
			    }
			
			    public A() {
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testClassInstanceCreationLessArgumentsInGenericType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.List;
			public class E {
			    public void foo(int i) {
			        A<List<String>> a= new A<List<String>>();
			    }
			}
			""";
		ICompilationUnit cu1=pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			public class A<T> {
			    public A(int i, String s) {
			    }
			}
			""";
		pack1.createCompilationUnit("A.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class A<T> {
			    public A() {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.List;
			public class E {
			    public void foo(int i) {
			        A<List<String>> a= new A<List<String>>(i, null);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class A<T> {
			    public A(int i, String s) {
			    }
			
			    public A() {
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}


	@Test
	public void testConstructorInvocation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public E(int i) {
			        this(i, true);
			    }
			}
			""";
		ICompilationUnit cu1=pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public E(int i) {
			        this(i, true);
			    }
			
			    public E(int i, boolean b) {
			    }
			}
			""";

		assertEqualString(preview1, expected1);
	}


	@Test
	public void testConstructorInvocationInGenericType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E<S, T> {
			    public E(int i) {
			        this(i, true);
			    }
			}
			""";
		ICompilationUnit cu1=pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E<S, T> {
			    public E(int i) {
			        this(i, true);
			    }
			
			    public E(int i, boolean b) {
			    }
			}
			""";

		assertEqualString(preview1, expected1);
	}

	@Test
	public void testSuperMethodInvocation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E extends A {
			    public void foo(int i) {
			        super.foo(i);
			    }
			}
			""";
		ICompilationUnit cu1=pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			public class A {
			}
			""";
		pack1.createCompilationUnit("A.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class A {
			
			    public void foo(int i) {
			    }
			}
			""";
		assertEqualString(preview, str2);

	}

	@Test
	public void testSuperMethodVarargsAddedArguments() throws Exception { // fix for Bug 546819
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E extends A {
			    public E(int i) {
			        super(i, true);
			    }
			}
			""";
		ICompilationUnit cu1=pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			public class A {
			    public A(boolean b, String ... strings) {
			    }
			}
			""";
		pack1.createCompilationUnit("A.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E extends A {
			    public E(int i) {
			        super(true);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class A {
			    public A(int i, boolean b, String ... strings) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class A {
			    public A(boolean b, String ... strings) {
			    }
			
			    public A(int i, boolean b) {
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testSuperMethodMoreArguments() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class X {
			    public int foo() {
			        return 0;
			    }
			}
			""";

		pack1.createCompilationUnit("X.java", str, false, null);

		String str1= """
			package test1;
			import java.util.Vector;
			public class E extends X {
			    public void xoo() {
			        super.foo(new Vector());
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str1, false, null);


		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.Vector;
			public class E extends X {
			    public void xoo() {
			        super.foo();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			
			import java.util.Vector;
			
			public class X {
			    public int foo(Vector vector) {
			        return 0;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			
			import java.util.Vector;
			
			public class X {
			    public int foo() {
			        return 0;
			    }
			
			    public void foo(Vector vector) {
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testSuperMethodLessArguments() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class X {
			    public int foo(Object o, boolean b) {
			        return 0;
			    }
			}
			""";

		pack1.createCompilationUnit("X.java", str, false, null);

		String str1= """
			package test1;
			import java.util.Vector;
			public class E extends X {
			    public void xoo() {
			        super.foo(new Vector());
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str1, false, null);


		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.Vector;
			public class E extends X {
			    public void xoo() {
			        super.foo(new Vector(), false);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class X {
			    public int foo(Object o) {
			        return 0;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			
			import java.util.Vector;
			
			public class X {
			    public int foo(Object o, boolean b) {
			        return 0;
			    }
			
			    public void foo(Vector vector) {
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}


	@Test
	public void testMissingCastParents1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			public class E {
			    public void foo(Object o) {
			        String x= (String) o.substring(1);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo(Object o) {
			        String x= ((String) o).substring(1);
			    }
			}
			""";

		assertEqualString(preview1, expected1);
	}

	@Test
	public void testMissingCastParents2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			public class E {
			    public void foo(Object o) {
			        String x= (String) o.substring(1).toLowerCase();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo(Object o) {
			        String x= ((String) o).substring(1).toLowerCase();
			    }
			}
			""";

		assertEqualString(preview1, expected1);
	}

	@Test
	public void testMissingCastParents3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			public class E {
			    private static Object obj;
			    public void foo() {
			        String x= (String) E.obj.substring(1).toLowerCase();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    private static Object obj;
			    public void foo() {
			        String x= ((String) E.obj).substring(1).toLowerCase();
			    }
			}
			""";

		assertEqualString(preview1, expected1);
	}

	@Test
	public void testArrayAccess() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			public class E {
			    private static Object obj;
			    public String foo(Object[] array) {
			        return array.tostring();
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
			    private static Object obj;
			    public String foo(Object[] array) {
			        return array.toString();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    private static Object obj;
			    public String foo(Object[] array) {
			        return array.length;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}

	@Test
	public void testIncompleteThrowsStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			public class E {
			    public void foo(Object[] array) {
			        throw RuntimeException();
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
			    public void foo(Object[] array) {
			        throw new RuntimeException();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public void foo(Object[] array) {
			        throw RuntimeException();
			    }
			
			    private Exception RuntimeException() {
			        return null;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}

	@Test
	public void testMissingAnnotationAttribute1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class E {
			    public @interface Annot {
			    }
			
			    @Annot(count= 1)
			    public void foo() {
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
			package pack;
			public class E {
			    public @interface Annot {
			
			        int count();
			    }
			
			    @Annot(count= 1)
			    public void foo() {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testMissingAnnotationAttribute2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class E {
			    public @interface Annot {
			    }
			
			    @Annot(1)
			    public void foo() {
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
			package pack;
			public class E {
			    public @interface Annot {
			
			        int value();
			    }
			
			    @Annot(1)
			    public void foo() {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testStaticImportFavorite1() throws Exception {
		IPreferenceStore preferenceStore= PreferenceConstants.getPreferenceStore();
		preferenceStore.setValue(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS, "java.lang.Math.*");
		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
			String str= """
				package pack;
				
				public class E {
				    private int foo() {
				        return max(1, 2);
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

			CompilationUnit astRoot= getASTRoot(cu);
			ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

			assertCorrectLabels(proposals);

			String[] expected= new String[1];
			expected[0]= """
				package pack;
				
				import static java.lang.Math.max;
				
				public class E {
				    private int foo() {
				        return max(1, 2);
				    }
				}
				""";

			assertExpectedExistInProposals(proposals, expected);
		} finally {
			preferenceStore.setValue(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS, "");
		}
	}

	@Test
	public void testStaticImportFavorite2() throws Exception {
		IPreferenceStore preferenceStore= PreferenceConstants.getPreferenceStore();
		preferenceStore.setValue(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS, "java.lang.Math.max");
		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
			String str= """
				package pack;
				
				public class E {
				    private int max() {
				        return max(1, 2);
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

			CompilationUnit astRoot= getASTRoot(cu);
			ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

			assertCorrectLabels(proposals);

			String[] expected= new String[1];
			expected[0]= """
				package pack;
				
				public class E {
				    private int max() {
				        return Math.max(1, 2);
				    }
				}
				""";

			assertExpectedExistInProposals(proposals, expected);
		} finally {
			preferenceStore.setValue(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS, "");
		}
	}


	/**
	 * Visibility: fix for public or protected not appropriate.
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=65876#c5
	 *
	 * @throws Exception if anything goes wrong
	 * @since 3.9
	 */
	@Test
	public void testIndirectProtectedMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);

		String str= """
			package test1;
			public class A {
			    protected void method() {
			    }
			}
			""";
		pack1.createCompilationUnit("A.java", str, false, null);

		String str1= """
			package test2;
			import test1.A;
			public class B extends A {
			    private void bMethod() {
			        A a = new A();
			        a.method();
			    }
			}
			""";
		ICompilationUnit cu = pack2.createCompilationUnit("B.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package test1;
			public class A {
			    public void method() {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testStaticMethodInInterface1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface Snippet {
			    public abstract String name();
			}
			class Ref {
			    void foo(Snippet c) {
			        int[] v= Snippet.values();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Snippet.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 0);
	}

	@Test
	public void testStaticMethodInInterface2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface Snippet {
			    public abstract String name();
			}
			interface Ref {
			   int[] v= Snippet.values();
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Snippet.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 0);
	}

	@Test
	public void testStaticMethodInInterface3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class XX {
			    interface I {
			        int i= n();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("XX.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);

		String str1= """
			package test1;
			public class XX {
			    interface I {
			        int i= n();
			    }
			
			    protected static int n() {
			        return 0;
			    }
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { str1 });
	}

	@Test
	public void testStaticMethodInInterface4() throws Exception {
		String str= """
			package test1;
			interface I {
			    int i= n();
			}
			""";
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		ICompilationUnit cu= pack1.createCompilationUnit("I.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 0);
	}

	@Test
	public void testAbstractMehodInInterface() throws Exception {
		String str= """
			package test1;
			interface Snippet {
			    abstract String name();
			}
			class Ref {
			    void foo(Snippet c) {
			        int[] v= c.values();
			    }
			}
			""";
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		ICompilationUnit cu= pack1.createCompilationUnit("Snippet.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);

		String str1= """
			package test1;
			interface Snippet {
			    abstract String name();
			
			    abstract int[] values();
			}
			class Ref {
			    void foo(Snippet c) {
			        int[] v= c.values();
			    }
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { str1 });
	}

}

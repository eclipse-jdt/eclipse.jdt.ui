/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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
 *     Benjamin Muskalla <bmuskalla@innoopract.com> - [quick fix] Shouldn't offer "Add throws declaration" quickfix for overriding signature if result would conflict with overridden signature
 *     Lukas Hanke <hanke@yatta.de> - Bug 241696 [quick fix] quickfix to iterate over a collection - https://bugs.eclipse.org/bugs/show_bug.cgi?id=241696
 *     Lukas Hanke <hanke@yatta.de> - Bug 430818 [1.8][quick fix] Quick fix for "for loop" is not shown for bare local variable/argument/field - https://bugs.eclipse.org/bugs/show_bug.cgi?id=430818
 *     Sandra Lions <sandra.lions-piron@oracle.com> - [quick fix] for qualified enum constants in switch-case labels - https://bugs.eclipse.org/bugs/90140
 *     Yves Joan <yves.joan@oracle.com> - [quick fix] Dead code quick fix should remove unnecessary parentheses - https://bugs.eclipse.org/257505
 *     Stephan Herrmann - Contribution for Bug 463360 - [override method][null] generating method override should not create redundant null annotations
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedNamesAssistProposal;

public class LocalCorrectionsQuickFixTest extends QuickFixTest {

	@Rule
	public ProjectTestSetup projectSetup= new ProjectTestSetup();

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
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

		fJProject1= projectSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testFieldAccessToStatic() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.File;
			public class E {
			    public char foo() {
			        return (new File("x.txt")).separatorChar;
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
			import java.io.File;
			public class E {
			    public char foo() {
			        return File.separatorChar;
			    }
			}
			""";
		assertEqualString(preview, str1);

		assertProposalExists(proposals, CorrectionMessages.ConfigureProblemSeveritySubProcessor_name);
	}

	@Test
	public void testInheritedAccessOnStatic() throws Exception {
		IPackageFragment pack0= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class A {
			    public static void foo() {
			    }
			}
			""";
		pack0.createCompilationUnit("A.java", str, false, null);

		String str1= """
			package pack;
			public class B extends A {
			}
			""";
		pack0.createCompilationUnit("B.java", str1, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str2= """
			package test1;
			import pack.B;
			public class E {
			    public void foo(B b) {
			        b.foo();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str2, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import pack.B;
			public class E {
			    public void foo(B b) {
			        B.foo();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import pack.A;
			import pack.B;
			public class E {
			    public void foo(B b) {
			        A.foo();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package pack;
			public class A {
			    public void foo() {
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testInheritedAccessOnStaticInGeneric() throws Exception {
		IPackageFragment pack0= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class A<T> {
			    public static void foo() {
			    }
			}
			""";
		pack0.createCompilationUnit("A.java", str, false, null);

		String str1= """
			package pack;
			public class B<T> extends A<String> {
			}
			""";
		pack0.createCompilationUnit("B.java", str1, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str2= """
			package test1;
			import pack.B;
			public class E {
			    public void foo(B<Number> b) {
			        b.foo();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str2, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import pack.B;
			public class E {
			    public void foo(B<Number> b) {
			        B.foo();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import pack.A;
			import pack.B;
			public class E {
			    public void foo(B<Number> b) {
			        A.foo();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package pack;
			public class A<T> {
			    public void foo() {
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testQualifiedAccessToStatic() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Thread t) throws InterruptedException {
			        t.sleep(10);
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
			    public void foo(Thread t) throws InterruptedException {
			        Thread.sleep(10);
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testThisAccessToStatic() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public static void goo() {
			    }
			    public void foo() {
			        this.goo();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public static void goo() {
			    }
			    public void foo() {
			        E.goo();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public void goo() {
			    }
			    public void foo() {
			        this.goo();
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testThisAccessToStaticField() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public static int fCount;
			
			    public void foo() {
			        this.fCount= 1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public static int fCount;
			
			    public void foo() {
			        E.fCount= 1;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public int fCount;
			
			    public void foo() {
			        this.fCount= 1;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}

	@Test
	public void testCastMissingInVarDecl() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Object o) {
			        Thread th= o;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo(Object o) {
			        Thread th= (Thread) o;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public void foo(Object o) {
			        Object th= o;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E {
			    public void foo(Thread o) {
			        Thread th= o;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testCastMissingInVarDecl2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.List;
			public class Container {
			    public List[] getLists() {
			        return null;
			    }
			}
			""";
		pack1.createCompilationUnit("Container.java", str, false, null);

		String str1= """
			package test1;
			import java.util.ArrayList;
			public class E {
			    public void foo(Container c) {
			         ArrayList[] lists= c.getLists();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.ArrayList;
			public class E {
			    public void foo(Container c) {
			         ArrayList[] lists= (ArrayList[]) c.getLists();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.ArrayList;
			import java.util.List;
			public class E {
			    public void foo(Container c) {
			         List[] lists= c.getLists();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			import java.util.ArrayList;
			import java.util.List;
			public class Container {
			    public ArrayList[] getLists() {
			        return null;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });

	}

	@Test
	public void testCastMissingInVarDecl3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        Thread th= foo();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public Thread foo() {
			        Thread th= foo();
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	@Test
	public void testCastMissingInVarDecl4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.List;
			public class Container {
			    public List getLists()[] {
			        return null;
			    }
			}
			""";
		pack1.createCompilationUnit("Container.java", str, false, null);

		String str1= """
			package test1;
			import java.util.ArrayList;
			public class E extends Container {
			    public void foo() {
			         ArrayList[] lists= super.getLists();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.ArrayList;
			public class E extends Container {
			    public void foo() {
			         ArrayList[] lists= (ArrayList[]) super.getLists();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.ArrayList;
			import java.util.List;
			public class E extends Container {
			    public void foo() {
			         List[] lists= super.getLists();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			import java.util.ArrayList;
			import java.util.List;
			public class Container {
			    public ArrayList[] getLists() {
			        return null;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });

	}


	@Test
	public void testCastMissingInFieldDecl() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    int time= System.currentTimeMillis();
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
			    int time= (int) System.currentTimeMillis();
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    long time= System.currentTimeMillis();
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testCastMissingInAssignment() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Iterator;
			public class E {
			    public void foo(Iterator iter) {
			        String str;
			        str= iter.next();
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
			import java.util.Iterator;
			public class E {
			    public void foo(Iterator iter) {
			        String str;
			        str= (String) iter.next();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.Iterator;
			public class E {
			    public void foo(Iterator iter) {
			        Object str;
			        str= iter.next();
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}

	@Test
	public void testCastMissingInAssignment2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Iterator;
			public class E {
			    public void foo(Iterator iter) {
			        String str, str2;
			        str= iter.next();
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
			import java.util.Iterator;
			public class E {
			    public void foo(Iterator iter) {
			        String str, str2;
			        str= (String) iter.next();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.Iterator;
			public class E {
			    public void foo(Iterator iter) {
			        Object str;
			        String str2;
			        str= iter.next();
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}

	@Test
	public void testCastMissingInExpression() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.List;
			public class E {
			    public String[] foo(List list) {
			        return list.toArray(new List[list.size()]);
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
			import java.util.List;
			public class E {
			    public String[] foo(List list) {
			        return (String[]) list.toArray(new List[list.size()]);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.List;
			public class E {
			    public Object[] foo(List list) {
			        return list.toArray(new List[list.size()]);
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testCastOnCastExpression() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.ArrayList;
			import java.util.List;
			public class E {
			    public void foo(List list) {
			        ArrayList a= (Cloneable) list;
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
			import java.util.ArrayList;
			import java.util.List;
			public class E {
			    public void foo(List list) {
			        ArrayList a= (ArrayList) list;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.ArrayList;
			import java.util.List;
			public class E {
			    public void foo(List list) {
			        Cloneable a= (Cloneable) list;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}


	@Test
	public void testUncaughtException() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			public class E {
			    public void goo() throws IOException {
			    }
			    public void foo() {
			        goo();
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
			import java.io.IOException;
			public class E {
			    public void goo() throws IOException {
			    }
			    public void foo() throws IOException {
			        goo();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.IOException;
			public class E {
			    public void goo() throws IOException {
			    }
			    public void foo() {
			        try {
			            goo();
			        } catch (IOException e) {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testUncaughtException2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			public class E {
			    public String goo() throws IOException {
			        return null;
			    }
			    /**
			     * Not much to say here.
			     */
			    public void foo() {
			        goo().substring(2);
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
			import java.io.IOException;
			public class E {
			    public String goo() throws IOException {
			        return null;
			    }
			    /**
			     * Not much to say here.
			     * @throws IOException\s
			     */
			    public void foo() throws IOException {
			        goo().substring(2);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.IOException;
			public class E {
			    public String goo() throws IOException {
			        return null;
			    }
			    /**
			     * Not much to say here.
			     */
			    public void foo() {
			        try {
			            goo().substring(2);
			        } catch (IOException e) {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testUncaughtException3() throws Exception {


		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    public String goo() throws IOException, ParseException {
			        return null;
			    }
			    /**
			     * Not much to say here.
			     * @throws ParseException Parsing failed
			     */
			    public void foo() throws ParseException {
			        goo().substring(2);
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
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    public String goo() throws IOException, ParseException {
			        return null;
			    }
			    /**
			     * Not much to say here.
			     * @throws ParseException Parsing failed
			     * @throws IOException\s
			     */
			    public void foo() throws ParseException, IOException {
			        goo().substring(2);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    public String goo() throws IOException, ParseException {
			        return null;
			    }
			    /**
			     * Not much to say here.
			     * @throws ParseException Parsing failed
			     */
			    public void foo() throws ParseException {
			        try {
			            goo().substring(2);
			        } catch (IOException e) {
			        } catch (ParseException e) {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testUncaughtException4() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.FileNotFoundException;
			import java.io.InterruptedIOException;
			public class E {
			    public E goo(int i) throws InterruptedIOException {
			        return new E();
			    }
			    public E bar() throws FileNotFoundException {
			        return new E();
			    }
			    /**
			     * Not much to say here.
			     */
			    public void foo() {
			        goo(1).bar();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);


		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.io.FileNotFoundException;
			import java.io.InterruptedIOException;
			public class E {
			    public E goo(int i) throws InterruptedIOException {
			        return new E();
			    }
			    public E bar() throws FileNotFoundException {
			        return new E();
			    }
			    /**
			     * Not much to say here.
			     * @throws InterruptedIOException\s
			     * @throws FileNotFoundException\s
			     */
			    public void foo() throws FileNotFoundException, InterruptedIOException {
			        goo(1).bar();
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.FileNotFoundException;
			import java.io.InterruptedIOException;
			public class E {
			    public E goo(int i) throws InterruptedIOException {
			        return new E();
			    }
			    public E bar() throws FileNotFoundException {
			        return new E();
			    }
			    /**
			     * Not much to say here.
			     */
			    public void foo() {
			        try {
			            goo(1).bar();
			        } catch (FileNotFoundException e) {
			        } catch (InterruptedIOException e) {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testUncaughtException5() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=31554
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			public class E {
			    void foo() {
			        try {
			            throw new IOException();
			        } catch (IOException e) {
			            throw new IOException();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);


		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.io.IOException;
			public class E {
			    void foo() throws IOException {
			        try {
			            throw new IOException();
			        } catch (IOException e) {
			            throw new IOException();
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	@Test
	public void testUncaughtExceptionImportConflict() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class Test {
			    public void test1() {
			        test2();
			    }
			
			    public void test2() throws de.muenchen.test.Exception {
			        throw new de.muenchen.test.Exception();
			    }
			
			    public void test3() {
			        try {
			            java.io.File.createTempFile("", ".tmp");
			        } catch (Exception ex) {
			
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", str, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("de.muenchen.test", false, null);
		String str1= """
			package de.muenchen.test;
			
			public class Exception extends java.lang.Throwable {
			}
			""";
		pack2.createCompilationUnit("Exception.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);


		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class Test {
			    public void test1() {
			        try {
			            test2();
			        } catch (de.muenchen.test.Exception e) {
			        }
			    }
			
			    public void test2() throws de.muenchen.test.Exception {
			        throw new de.muenchen.test.Exception();
			    }
			
			    public void test3() {
			        try {
			            java.io.File.createTempFile("", ".tmp");
			        } catch (Exception ex) {
			
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class Test {
			    public void test1() throws de.muenchen.test.Exception {
			        test2();
			    }
			
			    public void test2() throws de.muenchen.test.Exception {
			        throw new de.muenchen.test.Exception();
			    }
			
			    public void test3() {
			        try {
			            java.io.File.createTempFile("", ".tmp");
			        } catch (Exception ex) {
			
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testUncaughtExceptionExtendedSelection() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class E {\n");
		buf.append("    public String goo(int i) throws IOException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(goo(1));\n");
		buf.append("        System.out.println(goo(2));\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		String begin= "goo(1)", end= "goo(2));";

		int offset= buf.indexOf(begin);
		int length= buf.indexOf(end) + end.length() - offset;
		AssistContext context= getCorrectionContext(cu, offset, length);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2, context);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);


		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.io.IOException;
			public class E {
			    public String goo(int i) throws IOException {
			        return null;
			    }
			    public void foo() throws IOException {
			        System.out.println(goo(1));
			        System.out.println(goo(2));
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.IOException;
			public class E {
			    public String goo(int i) throws IOException {
			        return null;
			    }
			    public void foo() {
			        try {
			            System.out.println(goo(1));
			            System.out.println(goo(2));
			        } catch (IOException e) {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}


	@Test
	public void testUncaughtExceptionRemoveMoreSpecific() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			import java.net.SocketException;
			public class E {
			    public void goo() throws IOException {
			        return;
			    }
			    /**
			     * @throws SocketException Sockets are dangerous
			     * @since 3.0
			     */
			    public void foo() throws SocketException {
			        this.goo();
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
			import java.io.IOException;
			public class E {
			    public void goo() throws IOException {
			        return;
			    }
			    /**
			     * @throws IOException\s
			     * @since 3.0
			     */
			    public void foo() throws IOException {
			        this.goo();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.IOException;
			import java.net.SocketException;
			public class E {
			    public void goo() throws IOException {
			        return;
			    }
			    /**
			     * @throws SocketException Sockets are dangerous
			     * @since 3.0
			     */
			    public void foo() throws SocketException {
			        try {
			            this.goo();
			        } catch (IOException e) {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testUncaughtExceptionToSurroundingTry() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    public static void goo() throws IOException, ParseException {
			        return;
			    }
			    public void foo() {
			        try {
			            E.goo();
			        } catch (IOException e) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);


		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    public static void goo() throws IOException, ParseException {
			        return;
			    }
			    public void foo() throws ParseException {
			        try {
			            E.goo();
			        } catch (IOException e) {
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    public static void goo() throws IOException, ParseException {
			        return;
			    }
			    public void foo() {
			        try {
			            try {
			                E.goo();
			            } catch (ParseException e) {
			            }
			        } catch (IOException e) {
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    public static void goo() throws IOException, ParseException {
			        return;
			    }
			    public void foo() {
			        try {
			            E.goo();
			        } catch (IOException e) {
			        } catch (ParseException e) {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testUncaughtExceptionOnSuper1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.FileInputStream;
			public class E extends FileInputStream {
			    public E() {
			        super("x");
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
			import java.io.FileInputStream;
			import java.io.FileNotFoundException;
			public class E extends FileInputStream {
			    public E() throws FileNotFoundException {
			        super("x");
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testUncaughtExceptionOnSuper2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    public A() throws Exception {
			    }
			}
			""";
		pack1.createCompilationUnit("A.java", str, false, null);

		String str1= """
			package test1;
			public class E extends A {
			    /**
			     * @throws Exception sometimes...
			     */
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
			public class E extends A {
			    /**
			     * @throws Exception sometimes...
			     */
			    public E() throws Exception {
			        super();
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testUncaughtExceptionOnSuper3() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A implements Runnable {
			    public void run() {
			        Class.forName(null);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class A implements Runnable {
			    public void run() {
			        try {
			            Class.forName(null);
			        } catch (ClassNotFoundException e) {
			        }
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testUncaughtExceptionOnSuper4() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    public void foo() {
			    }
			}
			""";
		pack1.createCompilationUnit("A.java", str, false, null);

		String str1= """
			package test1;
			public class E extends A {
			    public void foo() {
			        throw new Exception();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);


		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1 = """
			package test1;
			public class E extends A {
			    public void foo() throws Exception {
			        throw new Exception();
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] {preview1}, new String[] {expected1});
	}

	@Test
	public void testUncaughtExceptionOnSuper5() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=349051
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.Closeable;
			import java.io.FileNotFoundException;
			public class A implements Closeable {
			    public void close() {
			        throw new FileNotFoundException();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);


		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.io.Closeable;
			import java.io.FileNotFoundException;
			public class A implements Closeable {
			    public void close() throws FileNotFoundException {
			        throw new FileNotFoundException();
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	@Test
	public void testUncaughtExceptionOnSuper6() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=349051
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.Closeable;
			public class A implements Closeable {
			    public void close() {
			        throw new Throwable();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 0);
		assertCorrectLabels(proposals);
	}
	@Test
	public void testUncaughtExceptionOnThis() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			public class E {
			    public E() {
			        this(null);
			    }
			    public E(Object x) throws IOException {
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
			import java.io.IOException;
			public class E {
			    public E() throws IOException {
			        this(null);
			    }
			    public E(Object x) throws IOException {
			    }
			}
			""";
		assertEqualString(preview, str1);
	}


	@Test
	public void testUncaughtExceptionDuplicate() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class MyException extends Exception {
			}
			""";
		pack1.createCompilationUnit("MyException.java", str, false, null);

		String str1= """
			package test1;
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    public void m1() throws IOException {
			        m2();
			    }
			    public void m2() throws IOException, ParseException, MyException {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2); // 2 uncaught exceptions
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);


		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    public void m1() throws IOException, ParseException, MyException {
			        m2();
			    }
			    public void m2() throws IOException, ParseException, MyException {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    public void m1() throws IOException {
			        try {
			            m2();
			        } catch (IOException e) {
			        } catch (ParseException e) {
			        } catch (MyException e) {
			        }
			    }
			    public void m2() throws IOException, ParseException, MyException {
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testMultipleUncaughtExceptions() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    public void goo() throws IOException, ParseException {
			    }
			    public void foo() {
			        goo();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2); // 2 uncaught exceptions
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);


		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    public void goo() throws IOException, ParseException {
			    }
			    public void foo() throws IOException, ParseException {
			        goo();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    public void goo() throws IOException, ParseException {
			    }
			    public void foo() {
			        try {
			            goo();
			        } catch (IOException e) {
			        } catch (ParseException e) {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testUncaughtExceptionInInitializer() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    {
			        Class.forName(null);
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
			    {
			        try {
			            Class.forName(null);
			        } catch (ClassNotFoundException e) {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}


	@Test
	public void testUnneededCatchBlock() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    public void goo() throws IOException {
			    }
			    public void foo() {
			        try {
			            goo();
			        } catch (IOException e) {
			        } catch (ParseException e) {
			        }
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
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    public void goo() throws IOException {
			    }
			    public void foo() {
			        try {
			            goo();
			        } catch (IOException e) {
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    public void goo() throws IOException {
			    }
			    public void foo() throws ParseException {
			        try {
			            goo();
			        } catch (IOException e) {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testUnneededCatchBlockInInitializer() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.text.ParseException;
			public class E {
			    static {
			        try {
			            int x= 1;
			        } catch (ParseException e) {
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
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.text.ParseException;
			public class E {
			    static {
			        int x= 1;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	@Test
	public void testUnneededCatchBlockSingle() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			public class E {
			    public void goo() {
			    }
			    public void foo() {
			        try {
			            goo();
			        } catch (IOException e) {
			        }
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
			import java.io.IOException;
			public class E {
			    public void goo() {
			    }
			    public void foo() {
			        goo();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.IOException;
			public class E {
			    public void goo() {
			    }
			    public void foo() throws IOException {
			        goo();
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testUnneededCatchBlockBug47221() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			public class E {
			    public Object foo() {
			        try {
			            Object o= null;
			            return o;
			        } catch (IOException e) {
			        }
			        return null;
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
			import java.io.IOException;
			public class E {
			    public Object foo() {
			        Object o= null;
			        return o;
			        return null;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.IOException;
			public class E {
			    public Object foo() throws IOException {
			        Object o= null;
			        return o;
			        return null;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}


	@Test
	public void testUnneededCatchBlockWithFinally() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			public class E {
			    public void goo() {
			    }
			    public void foo() {
			        try {
			            goo();
			        } catch (IOException e) {
			        } finally {
			        }
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
			import java.io.IOException;
			public class E {
			    public void goo() {
			    }
			    public void foo() {
			        try {
			            goo();
			        } finally {
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.IOException;
			public class E {
			    public void goo() {
			    }
			    public void foo() throws IOException {
			        try {
			            goo();
			        } finally {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}

	@Test
	public void testUninitializedField() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.Serializable;
			import java.util.List;
			public class E {
			    public final String foo1;
			    public final int foo2;
			    public final Object foo3;
			    public final Serializable foo4;
			    public final List<E> foo5;
			    public final List<? super String> foo6;
			    public final List<List<? extends String>> foo7;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOfProblems(7, problems);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, problems[0], null);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.io.Serializable;
			import java.util.List;
			public class E {
			    public final String foo1 = "";
			    public final int foo2;
			    public final Object foo3;
			    public final Serializable foo4;
			    public final List<E> foo5;
			    public final List<? super String> foo6;
			    public final List<List<? extends String>> foo7;
			}
			""";

		proposals= collectCorrections(cu, problems[1], null);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		proposal= (CUCorrectionProposal) proposals.get(0);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.Serializable;
			import java.util.List;
			public class E {
			    public final String foo1;
			    public final int foo2 = 0;
			    public final Object foo3;
			    public final Serializable foo4;
			    public final List<E> foo5;
			    public final List<? super String> foo6;
			    public final List<List<? extends String>> foo7;
			}
			""";

		proposals= collectCorrections(cu, problems[2], null);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		proposal= (CUCorrectionProposal) proposals.get(0);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			import java.io.Serializable;
			import java.util.List;
			public class E {
			    public final String foo1;
			    public final int foo2;
			    public final Object foo3 = new Object();
			    public final Serializable foo4;
			    public final List<E> foo5;
			    public final List<? super String> foo6;
			    public final List<List<? extends String>> foo7;
			}
			""";

		proposals= collectCorrections(cu, problems[3], null);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		proposal= (CUCorrectionProposal) proposals.get(0);
		String preview4= getPreviewContent(proposal);

		String expected4= """
			package test1;
			import java.io.Serializable;
			import java.util.List;
			public class E {
			    public final String foo1;
			    public final int foo2;
			    public final Object foo3;
			    public final Serializable foo4 = null;
			    public final List<E> foo5;
			    public final List<? super String> foo6;
			    public final List<List<? extends String>> foo7;
			}
			""";

		proposals= collectCorrections(cu, problems[4], null);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		proposal= (CUCorrectionProposal) proposals.get(0);
		String preview5= getPreviewContent(proposal);

		String expected5= """
			package test1;
			import java.io.Serializable;
			import java.util.List;
			public class E {
			    public final String foo1;
			    public final int foo2;
			    public final Object foo3;
			    public final Serializable foo4;
			    public final List<E> foo5 = null;
			    public final List<? super String> foo6;
			    public final List<List<? extends String>> foo7;
			}
			""";

		proposals= collectCorrections(cu, problems[5], null);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		proposal= (CUCorrectionProposal) proposals.get(0);
		String preview6= getPreviewContent(proposal);

		String expected6= """
			package test1;
			import java.io.Serializable;
			import java.util.List;
			public class E {
			    public final String foo1;
			    public final int foo2;
			    public final Object foo3;
			    public final Serializable foo4;
			    public final List<E> foo5;
			    public final List<? super String> foo6 = null;
			    public final List<List<? extends String>> foo7;
			}
			""";

		proposals= collectCorrections(cu, problems[6], null);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		proposal= (CUCorrectionProposal) proposals.get(0);
		String preview7= getPreviewContent(proposal);

		String expected7= """
			package test1;
			import java.io.Serializable;
			import java.util.List;
			public class E {
			    public final String foo1;
			    public final int foo2;
			    public final Object foo3;
			    public final Serializable foo4;
			    public final List<E> foo5;
			    public final List<? super String> foo6;
			    public final List<List<? extends String>> foo7 = null;
			}
			""";

		assertEqualStringsIgnoreOrder(
				new String[] { preview1, preview2, preview3, preview4, preview5, preview6, preview7 },
				new String[] { expected1, expected2, expected3, expected4, expected5, expected6, expected7 });
	}

	@Test
	public void testUninitializedField_2() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=37872
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public final int foo;
			    public E() {
			    }
			    public E(int bar) {
			    }
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

		String expected1= """
			package test1;
			public class E {
			    public final int foo;
			    public E() {
			        this.foo = 0;
			    }
			    public E(int bar) {
			        this.foo = 0;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(
				new String[] { preview1 },
				new String[] { expected1 });
	}

	@Test
	public void testUninitializedField_3() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=37872
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public final int foo;
			    public E() {
			        this(1);
			    }
			    public E(int bar1) {
			    }
			    public E(int bar1, int bar2) {
			    }
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

		String expected1= """
			package test1;
			public class E {
			    public final int foo;
			    public E() {
			        this(1);
			    }
			    public E(int bar1) {
			        this.foo = 0;
			    }
			    public E(int bar1, int bar2) {
			        this.foo = 0;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(
				new String[] { preview1 },
				new String[] { expected1 });
	}

	@Test
	public void testUninitializedField_4() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=37872
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public final int foo;
			    public E(String bar) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOfProblems(1, problems);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, problems[0], null);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public final int foo;
			    public E(String bar, int foo) {
			        this.foo = foo;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testUninitializedField_5() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=37872
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public final Object foo;
			    public E(String foo) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOfProblems(1, problems);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, problems[0], null);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public final Object foo;
			    public E(String foo, Object foo2) {
			        this.foo = foo2;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testUninitializedField_6() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=37872
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public final int foo1;
			    public final int foo2;
			    public E(String bar) {
			        String a;
			        this.foo1 = 0;
			        String b;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOfProblems(1, problems);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, problems[0], null);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public final int foo1;
			    public final int foo2;
			    public E(String bar) {
			        String a;
			        this.foo1 = 0;
			        this.foo2 = 0;
			        String b;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testUninitializedField_7() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=37872
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public final int foo1;
			    public E() {
			    }
			    public E(String bar) {
			        foo1 = 0;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOfProblems(1, problems);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, problems[0], null);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public final int foo1;
			    public E() {
			        this.foo1 = 0;
			    }
			    public E(String bar) {
			        foo1 = 0;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testUninitializedField_8() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=37872
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public final int foo1;
			    public E() {
			        int a = 0;
			        int b = foo1;
			    }
			    public E(String bar) {
			        foo1 = 0;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOfProblems(2, problems);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, problems[0], null);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public final int foo1;
			    public E() {
			        this.foo1 = 0;
			        int a = 0;
			        int b = foo1;
			    }
			    public E(String bar) {
			        foo1 = 0;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testUninitializedField_9() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=37872
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public final int a;
			    public final int b;
			    public E(int a) {
			        a = b;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOfProblems(3, problems);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, problems[0], null);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public final int a;
			    public final int b;
			    public E(int a) {
			        this.a = 0;
			        a = b;
			    }
			}
			""";

		String expected2= """
			package test1;
			public class E {
			    public final int a;
			    public final int b;
			    public E(int a, int a2) {
			        this.a = a2;
			        a = b;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
	}

	@Test
	public void testUninitializedField_10() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=37872
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public final int b;
			    public E(int a) {
			        int b = 0;
			        a = b;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOfProblems(1, problems);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, problems[0], null);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public final int b;
			    public E(int a) {
			        this.b = 0;
			        int b = 0;
			        a = b;
			    }
			}
			""";

		String expected2= """
			package test1;
			public class E {
			    public final int b;
			    public E(int a, int b2) {
			        this.b = b2;
			        int b = 0;
			        a = b;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
	}

	@Test
	public void testUninitializedField_11() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=563285
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public enum E {
			    a, b, c;
			    private final int foo1;
			    private final int foo2;
			    private E(int i1, int i2) {
			        this.foo1 = i1;
			        this.foo2 = i2;
			    }
			    E() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOfProblems(2, problems);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, problems[0], null);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public enum E {
			    a, b, c;
			    private final int foo1;
			    private final int foo2;
			    private E(int i1, int i2) {
			        this.foo1 = i1;
			        this.foo2 = i2;
			    }
			    E() {
			        this.foo1 = 0;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testUninitializedField_12() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=572571
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E<T> {
			    final Class<Integer> i1;
			    final Class<? extends Class<Integer>> i2;
			    final E<String> e;
			    final E<T> et;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		IProblem[] problems= astRoot.getProblems();

		List<ICompletionProposal> proposals= collectAllCorrections(cu, astRoot, problems.length);
		assertNumberOfProposals(proposals, problems.length);
		assertCorrectLabels(proposals);

		String[] previews = new String[proposals.size()];
		for (int i= 0; i < proposals.size(); i++) {
			previews[i] =  getPreviewContent((CUCorrectionProposal)proposals.get(i));
		}
		String[] expected = new String[previews.length];

		expected[0]= """
			package test1;
			public class E<T> {
			    final Class<Integer> i1 = new Class<Integer>();
			    final Class<? extends Class<Integer>> i2;
			    final E<String> e;
			    final E<T> et;
			}
			""";

		expected[1]= """
			package test1;
			public class E<T> {
			    final Class<Integer> i1;
			    final Class<? extends Class<Integer>> i2 = new Class<? extends Class<Integer>>();
			    final E<String> e;
			    final E<T> et;
			}
			""";

		expected[2]= """
			package test1;
			public class E<T> {
			    final Class<Integer> i1;
			    final Class<? extends Class<Integer>> i2;
			    final E<String> e = new E<String>();
			    final E<T> et;
			}
			""";

		expected[3]= """
			package test1;
			public class E<T> {
			    final Class<Integer> i1;
			    final Class<? extends Class<Integer>> i2;
			    final E<String> e;
			    final E<T> et = new E<T>();
			}
			""";

		assertEqualStringsIgnoreOrder(previews, expected);
	}

	@Test
	public void testUnimplementedMethods() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		String str= """
			package test2;
			import java.io.IOException;
			public interface Inter {
			    int getCount(Object[] o) throws IOException;
			}
			""";
		pack2.createCompilationUnit("Inter.java", str, false, null);


		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			import test2.Inter;
			public class E implements Inter{
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
			package test1;
			import test2.Inter;
			public abstract class E implements Inter{
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.IOException;
			
			import test2.Inter;
			public class E implements Inter{
			
			    public int getCount(Object[] o) throws IOException {
			        return 0;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}

	@Test
	public void testUnimplementedMethods2() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		String str= """
			package test2;
			import java.io.IOException;
			public interface Inter {
			    int getCount(Object[] o) throws IOException;
			}
			""";
		pack2.createCompilationUnit("Inter.java", str, false, null);

		String str1= """
			package test2;
			import java.io.IOException;
			public abstract class InterImpl implements Inter {
			    protected abstract int[] getMusic() throws IOException;
			}
			""";
		pack2.createCompilationUnit("InterImpl.java", str1, false, null);


		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str2= """
			package test1;
			import test2.InterImpl;
			public class E extends InterImpl {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str2, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import test2.InterImpl;
			public abstract class E extends InterImpl {
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.IOException;
			
			import test2.InterImpl;
			public class E extends InterImpl {
			
			    public int getCount(Object[] o) throws IOException {
			        return 0;
			    }
			
			    @Override
			    protected int[] getMusic() throws IOException {
			        return null;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testUnimplementedMethods_bug62931() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		String str= """
			package test2;
			public interface Inter {
			    int foo();
			}
			""";
		pack2.createCompilationUnit("Inter.java", str, false, null);

		String str1= """
			package test2;
			public class A {
			    int foo() { }
			}
			""";
		pack2.createCompilationUnit("A.java", str1, false, null);


		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str2= """
			package test1;
			import test2.A;
			import test2.Inter;
			public class E extends A implements Inter {
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
			import test2.A;
			import test2.Inter;
			public class E extends A implements Inter {
			
			    public int foo() {
			        return 0;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import test2.A;
			import test2.Inter;
			public abstract class E extends A implements Inter {
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testUnimplementedMethods_bug113665() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		String str= """
			package test2;
			public interface F {
			      public void c() throws Exception;
			      public void e();
			}
			""";
		pack2.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test2;
			public class A implements F {
			    public void c() throws Exception, RuntimeException { }
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("A.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test2;
			public class A implements F {
			    public void c() throws Exception, RuntimeException { }
			
			    public void e() {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test2;
			public abstract class A implements F {
			    public void c() throws Exception, RuntimeException { }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testUnimplementedMethods_bug122906() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		String str= """
			package test2;
			import java.util.Properties;
			public interface F {
			    public void b(Properties p);
			    public void g(test2.Properties p);
			}
			""";
		pack2.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test2;
			public class Properties {}
			""";
		pack2.createCompilationUnit("Properties.java", str1, false, null);

		String str2= """
			package test2;
			public class A implements F {
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("A.java", str2, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test2;
			
			import java.util.Properties;
			
			public class A implements F {
			
			    public void b(Properties p) {
			    }
			
			    public void g(test2.Properties p) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test2;
			public abstract class A implements F {
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testUnimplementedMethods_bug123084() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		String str= """
			package test2;
			public class Class {}
			""";
		pack2.createCompilationUnit("Class.java", str, false, null);

		String str1= """
			package test2;
			public interface IT {
			    public void foo(java.lang.Class clazz);
			}
			""";
		pack2.createCompilationUnit("IT.java", str1, false, null);

		String str2= """
			package test2;
			public class A implements IT {
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("A.java", str2, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test2;
			public class A implements IT {
			
			    public void foo(java.lang.Class clazz) {
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test2;
			public abstract class A implements IT {
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testUnimplementedMethodsExtendingGenericType1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Collection;
			public interface Inter<T> {
			    T doT(Collection<T> in);
			}
			""";
		pack1.createCompilationUnit("Inter.java", str, false, null);

		String str1= """
			package test1;
			public class E implements Inter<String> {
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
			package test1;
			public abstract class E implements Inter<String> {
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			
			import java.util.Collection;
			
			public class E implements Inter<String> {
			
			    public String doT(Collection<String> in) {
			        return null;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testUnimplementedMethodsExtendingGenericType2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public interface Inter<T> {
			    T doT(T in);
			}
			""";
		pack1.createCompilationUnit("Inter.java", str, false, null);

		String str1= """
			package test1;
			public class E implements Inter<String> {
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
			package test1;
			public abstract class E implements Inter<String> {
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E implements Inter<String> {
			
			    public String doT(String in) {
			        return null;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}




	@Test
	public void testUnimplementedMethodsWithTypeParameters() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Collection;
			public interface Inter {
			    <T> T doX(Collection<T> in);
			    <T extends Exception> T getException();
			}
			""";
		pack1.createCompilationUnit("Inter.java", str, false, null);

		String str1= """
			package test1;
			public class E implements Inter {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public abstract class E implements Inter {
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			
			import java.util.Collection;
			
			public class E implements Inter {
			
			    public <T> T doX(Collection<T> in) {
			        return null;
			    }
			
			    public <T extends Exception> T getException() {
			        return null;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testUnimplementedMethodsWithTypeParameters2() throws Exception {
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=330241
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.List;
			public interface Inter {
			    <T> List<T> findElements(Class<T> clazz, List<String> tagsToMatch);
			    <T> List<T> findPerspectiveElements(Class<T> clazz, List<String> tagsToMatch);
			}
			""";
		pack1.createCompilationUnit("Inter.java", str, false, null);

		String str1= """
			package test1;
			import java.util.List;
			public class E implements Inter {
			    public <T> List<T> findPerspectiveElements(Class<T> clazz, List<String> tagsToMatch) {
			        return null;
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
			package test1;
			import java.util.List;
			public abstract class E implements Inter {
			    public <T> List<T> findPerspectiveElements(Class<T> clazz, List<String> tagsToMatch) {
			        return null;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.List;
			public class E implements Inter {
			    public <T> List<T> findPerspectiveElements(Class<T> clazz, List<String> tagsToMatch) {
			        return null;
			    }
			
			    public <T> List<T> findElements(Class<T> clazz, List<String> tagsToMatch) {
			        return null;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testUnimplementedEnumConstructor() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			public enum E {
			    E(1, "E");
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(1);
		String preview= getPreviewContent(proposal);

		String str1= """
			package p;
			public enum E {
			    E(1, "E");
			
			    E(int i, String string) {
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testUnimplementedMethodsInEnum() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			enum TestEnum implements IA {
			    test1,test2;
			}
			interface IA {
			    void foo();
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("TestEnum.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package pack;
			enum TestEnum implements IA {
			    test1,test2;
			
			    public void foo() {
			    }
			}
			interface IA {
			    void foo();
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnimplementedMethodsInEnumConstant1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			enum TestEnum {
			    A {
			        @Override
			        public boolean foo() {
			            return false;
			        }
			    };
			    public abstract boolean foo();
			    public abstract void bar();
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("TestEnum.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package test;
			enum TestEnum {
			    A {
			        @Override
			        public boolean foo() {
			            return false;
			        }
			
			        @Override
			        public void bar() {
			        }
			    };
			    public abstract boolean foo();
			    public abstract void bar();
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnimplementedMethodsInEnumConstant2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			enum TestEnum {
			    A {
			    };
			    public abstract boolean foo();
			    public abstract void bar();
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("TestEnum.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package test;
			enum TestEnum {
			    A {
			
			        @Override
			        public boolean foo() {
			            return false;
			        }
			
			        @Override
			        public void bar() {
			        }
			    };
			    public abstract boolean foo();
			    public abstract void bar();
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnimplementedMethodsInEnumConstant3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			enum TestEnum implements Runnable {
			    A;
			    public abstract boolean foo();
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("TestEnum.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package test;
			enum TestEnum implements Runnable {
			    A {
			        public void run() {
			        }
			        @Override
			        public boolean foo() {
			            return false;
			        }
			    };
			    public abstract boolean foo();
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnimplementedMethodsWithAnnotations() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_NONNULL_ANNOTATION_NAME, "test.NonNull");
		hashtable.put(JavaCore.COMPILER_NULLABLE_ANNOTATION_NAME, "test.Nullable");
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			
			import java.lang.annotation.Retention;
			import java.lang.annotation.RetentionPolicy;
			
			@Retention(RetentionPolicy.CLASS)
			@interface NonNull {}
			@Retention(RetentionPolicy.CLASS)
			@interface Nullable {}
			
			@Retention(RetentionPolicy.RUNTIME)
			@interface Sour {
			    byte[] value();
			    byte CONST= 12;
			    Class<?> c() default Object[].class;
			    String name() default "";
			    RetentionPolicy policy() default RetentionPolicy.SOURCE;
			    Deprecated d() default @Deprecated;
			}
			
			abstract class A {
			    @SuppressWarnings("unused")
			    @Sour(value={- 42, 13}, c= Integer[][].class, name="\\u0040hi", policy=RetentionPolicy.CLASS, d=@Deprecated())
			    public abstract @NonNull Object foo(
			            @SuppressWarnings("unused")
			            @Sour(value={Sour.CONST}) @Nullable(unresolved) Object input);
			}
			class B extends A {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("B.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 3, 2);

		assertCorrectLabels(proposals);

		String[] expected= new String[1];
		expected[0]= """
			package test;
			
			import java.lang.annotation.Retention;
			import java.lang.annotation.RetentionPolicy;
			
			@Retention(RetentionPolicy.CLASS)
			@interface NonNull {}
			@Retention(RetentionPolicy.CLASS)
			@interface Nullable {}
			
			@Retention(RetentionPolicy.RUNTIME)
			@interface Sour {
			    byte[] value();
			    byte CONST= 12;
			    Class<?> c() default Object[].class;
			    String name() default "";
			    RetentionPolicy policy() default RetentionPolicy.SOURCE;
			    Deprecated d() default @Deprecated;
			}
			
			abstract class A {
			    @SuppressWarnings("unused")
			    @Sour(value={- 42, 13}, c= Integer[][].class, name="\\u0040hi", policy=RetentionPolicy.CLASS, d=@Deprecated())
			    public abstract @NonNull Object foo(
			            @SuppressWarnings("unused")
			            @Sour(value={Sour.CONST}) @Nullable(unresolved) Object input);
			}
			class B extends A {
			
			    @Override
			    public @NonNull Object foo(@Nullable Object input) {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnimplementedMethodsWithAnnotations2() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=387940
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class A implements I {
			}
			interface I {
			    @SuppressWarnings("unused")
			    void foo();
			}""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);

		String[] expected= new String[1];
		expected[0]= """
			package test;
			public class A implements I {
			
			    public void foo() {
			    }
			}
			interface I {
			    @SuppressWarnings("unused")
			    void foo();
			}""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnimplementedMethodsWithAnnotations3() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		hashtable.put(JavaCore.COMPILER_NONNULL_ANNOTATION_NAME, "annots.NonNull");
		hashtable.put(JavaCore.COMPILER_NULLABLE_ANNOTATION_NAME, "annots.Nullable");
		hashtable.put(JavaCore.COMPILER_NONNULL_BY_DEFAULT_ANNOTATION_NAME, "annots.NonNullByDefault");
		JavaCore.setOptions(hashtable);

		IPackageFragment pack0= fSourceFolder.createPackageFragment("annots", false, null);
		String str= """
			package annots;
			
			@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)
			public @interface NonNull {}
			""";
		pack0.createCompilationUnit("NonNull.java", str, false, null);

		String str1= """
			package annots;
			
			@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)
			public @interface Nullable {}
			""";
		pack0.createCompilationUnit("Nullable.java", str1, false, null);

		String str2= """
			package annots;
			
			@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)
			public @interface NonNullByDefault {}
			""";
		pack0.createCompilationUnit("NonNullByDefault.java", str2, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str3= """
			@annots.NonNullByDefault
			package test;
			""";
		pack1.createCompilationUnit("package-info.java", str3, false, null);

		String str4= """
			package test;
			
			import annots.*;
			
			abstract class A {
			    @SuppressWarnings("unused")
			    public abstract @NonNull Object foo(@Nullable Object i1, @NonNull Object i2);
			}
			class B extends A {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("B.java", str4, false, null);

		CompilationUnit astRoot= getASTRoot(cu);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 3, 2); // 2 warnings regarding redundant null annotations

		assertCorrectLabels(proposals);

		String[] expected= new String[1];
		expected[0]= """
			package test;
			
			import annots.*;
			
			abstract class A {
			    @SuppressWarnings("unused")
			    public abstract @NonNull Object foo(@Nullable Object i1, @NonNull Object i2);
			}
			class B extends A {
			
			    @Override
			    public Object foo(@Nullable Object i1, Object i2) {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnimplementedMethodsWithAnnotations4() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		hashtable.put(JavaCore.COMPILER_NONNULL_ANNOTATION_NAME, "annots.NonNull");
		hashtable.put(JavaCore.COMPILER_NULLABLE_ANNOTATION_NAME, "annots.Nullable");
		hashtable.put(JavaCore.COMPILER_NONNULL_BY_DEFAULT_ANNOTATION_NAME, "annots.NonNullByDefault");
		JavaCore.setOptions(hashtable);


		IPackageFragment pack0= fSourceFolder.createPackageFragment("annots", false, null);
		String str= """
			package annots;
			
			@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)
			public @interface NonNull {}
			""";
		pack0.createCompilationUnit("NonNull.java", str, false, null);

		String str1= """
			package annots;
			
			@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)
			public @interface Nullable {}
			""";
		pack0.createCompilationUnit("Nullable.java", str1, false, null);

		String str2= """
			package annots;
			
			@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)
			public @interface NonNullByDefault { boolean value(); }
			""";
		pack0.createCompilationUnit("NonNullByDefault.java", str2, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str3= """
			@annots.NonNullByDefault(true)
			package test;
			""";
		pack1.createCompilationUnit("package-info.java", str3, false, null);

		String str4= """
			package test;
			
			import annots.*;
			
			abstract class A {
			    @SuppressWarnings({"unused", "null"})
			    public abstract @NonNull Object foo(@Nullable Object i1, @NonNull Object i2);
			}
			@NonNullByDefault(false)
			class B extends A {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("B.java", str4, false, null);

		CompilationUnit astRoot= getASTRoot(cu);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1, 0);

		assertCorrectLabels(proposals);

		String[] expected= new String[1];
		expected[0]= """
			package test;
			
			import annots.*;
			
			abstract class A {
			    @SuppressWarnings({"unused", "null"})
			    public abstract @NonNull Object foo(@Nullable Object i1, @NonNull Object i2);
			}
			@NonNullByDefault(false)
			class B extends A {
			
			    @Override
			    public @NonNull Object foo(@Nullable Object i1, @NonNull Object i2) {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnimplementedMethodsWithAnnotations5() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		hashtable.put(JavaCore.COMPILER_NONNULL_ANNOTATION_NAME, "annots.NonNull");
		hashtable.put(JavaCore.COMPILER_NULLABLE_ANNOTATION_NAME, "annots.Nullable");
		hashtable.put(JavaCore.COMPILER_NONNULL_BY_DEFAULT_ANNOTATION_NAME, "annots.NonNullByDefault");
		JavaCore.setOptions(hashtable);


		IPackageFragment pack0= fSourceFolder.createPackageFragment("annots", false, null);
		String str= """
			package annots;
			
			@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)
			public @interface NonNull {}
			""";
		pack0.createCompilationUnit("NonNull.java", str, false, null);

		String str1= """
			package annots;
			
			@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)
			public @interface Nullable {}
			""";
		pack0.createCompilationUnit("Nullable.java", str1, false, null);

		String str2= """
			package annots;
			
			@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)
			public @interface NonNullByDefault { boolean value() default true; }
			""";
		pack0.createCompilationUnit("NonNullByDefault.java", str2, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		// no package default

		String str3= """
			package test;
			
			import annots.*;
			
			abstract class A {
			    @SuppressWarnings("unused")
			    public abstract @NonNull Object foo(@Nullable Object i1, @NonNull Object i2);
			}
			class B {
			    @NonNullByDefault
			    A f = new A() {
			    };
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("B.java", str3, false, null);

		CompilationUnit astRoot= getASTRoot(cu);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1, 0);

		assertCorrectLabels(proposals);

		String[] expected= new String[1];
		expected[0]= """
			package test;
			
			import annots.*;
			
			abstract class A {
			    @SuppressWarnings("unused")
			    public abstract @NonNull Object foo(@Nullable Object i1, @NonNull Object i2);
			}
			class B {
			    @NonNullByDefault
			    A f = new A() {
			
			        @Override
			        public Object foo(@Nullable Object i1, Object i2) {
			            return null;
			        }
			    };
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnimplementedMethodsWithCovariantReturn() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=272657
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class Test {
			    interface Interface1 { Object getX(); }
			    interface Interface2 { Integer getX(); }
			    class Cls implements Interface1, Interface2 {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		expected[0]= """
			package test;
			public class Test {
			    interface Interface1 { Object getX(); }
			    interface Interface2 { Integer getX(); }
			    class Cls implements Interface1, Interface2 {
			
			        public Integer getX() {
			            return null;
			        }
			    }
			}
			""";

		expected[1]= """
			package test;
			public class Test {
			    interface Interface1 { Object getX(); }
			    interface Interface2 { Integer getX(); }
			    abstract class Cls implements Interface1, Interface2 {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnimplementedMethodsWithSubsignature() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=272657 , https://bugs.eclipse.org/bugs/show_bug.cgi?id=424509#c6
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.math.BigInteger;
			
			interface A { Object m(Class c); }
			interface B<S extends Number> { Object m(Class<S> c); }
			interface C<T extends BigInteger> { Object m(Class<T> c); }
			interface D<S,T> extends A, B<BigInteger>, C<BigInteger> {}
			
			//Add unimplemented methods
			class M implements D<BigInteger,BigInteger> {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[1];
		expected[0]= """
			package test;
			import java.math.BigInteger;
			
			interface A { Object m(Class c); }
			interface B<S extends Number> { Object m(Class<S> c); }
			interface C<T extends BigInteger> { Object m(Class<T> c); }
			interface D<S,T> extends A, B<BigInteger>, C<BigInteger> {}
			
			//Add unimplemented methods
			class M implements D<BigInteger,BigInteger> {
			
			    public Object m(Class c) {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnimplementedMethodsWithSubsignature2() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=272657 , https://bugs.eclipse.org/bugs/show_bug.cgi?id=424509#c6
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.math.BigInteger;
			
			interface A { Object m(Class c); }
			interface B<S extends Number> { Object m(Class<S> c); }
			interface D<S,T> extends A, B<BigInteger> {}
			
			class M implements D<BigInteger,BigInteger> {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[1];
		expected[0]= """
			package test;
			import java.math.BigInteger;
			
			interface A { Object m(Class c); }
			interface B<S extends Number> { Object m(Class<S> c); }
			interface D<S,T> extends A, B<BigInteger> {}
			
			class M implements D<BigInteger,BigInteger> {
			
			    public Object m(Class c) {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnimplementedMethodsWithSubsignature3() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=272657 , https://bugs.eclipse.org/bugs/show_bug.cgi?id=424509#c6
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.math.BigInteger;
			
			interface A { Object m(Class c); }
			interface B<S extends Number> { Object m(Class<S> c); }
			interface D<S,T> extends B<BigInteger>, A {}
			
			class M implements D<BigInteger,BigInteger> {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[1];
		expected[0]= """
			package test;
			import java.math.BigInteger;
			
			interface A { Object m(Class c); }
			interface B<S extends Number> { Object m(Class<S> c); }
			interface D<S,T> extends B<BigInteger>, A {}
			
			class M implements D<BigInteger,BigInteger> {
			
			    public Object m(Class c) {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnitializedVariable() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        int s;
			        try {
			            s= 1;
			        } catch (Exception e) {
			            System.out.println(s);
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
			        int s = 0;
			        try {
			            s= 1;
			        } catch (Exception e) {
			            System.out.println(s);
			        }
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testUndefinedConstructorInDefaultConstructor1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F {
			    public F(Runnable runnable) {
			    }
			}
			""";
		pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E extends F {
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
			public class E extends F {
			
			    public E(Runnable runnable) {
			        super(runnable);
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testUndefinedConstructorInDefaultConstructor2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			public class F {
			    public F(Runnable runnable) throws IOException {
			    }
			
			    public F(int i, Runnable runnable) {
			    }
			}
			""";
		pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E extends F {
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
			package test1;
			public class E extends F {
			
			    public E(int i, Runnable runnable) {
			        super(i, runnable);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			
			import java.io.IOException;
			
			public class E extends F {
			
			    public E(Runnable runnable) throws IOException {
			        super(runnable);
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testUndefinedConstructorWithGenericSuperClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F<T extends Runnable> {
			    public F(Runnable runnable) {
			    }
			}
			""";
		pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E extends F<Runnable> {
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
			public class E extends F<Runnable> {
			
			    public E(Runnable runnable) {
			        super(runnable);
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testUndefinedConstructorWithLineBreaks() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, "30");
		String optionValue= DefaultCodeFormatterConstants.createAlignmentValue(false, DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE, DefaultCodeFormatterConstants.INDENT_DEFAULT);
		hashtable.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_CONSTRUCTOR_DECLARATION, optionValue);
		hashtable.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_EXPLICIT_CONSTRUCTOR_CALL, optionValue);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F {
			    public F(Runnable runnable, boolean isGreen, boolean isBlue, boolean isRed) {
			    }
			}
			""";
		pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E extends F {
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
			public class E extends F {
			
			    public E(
			            Runnable runnable,
			            boolean isGreen,
			            boolean isBlue,
			            boolean isRed) {
			        super(
			                runnable,
			                isGreen,
			                isBlue,
			                isRed);
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testUndefinedConstructorWithEnclosing1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F {
			    public class SubF {
			        public SubF(int i) {
			        }
			    }
			}
			""";
		pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E {
			    public class SubE extends F.SubF {
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
			public class E {
			    public class SubE extends F.SubF {
			
			        public SubE(F f, int i) {
			            f.super(i);
			        }
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testUndefinedConstructorWithEnclosing2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F {
			    public static class SubF {
			        public SubF(int i) {
			        }
			    }
			}
			""";
		pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E {
			    public class SubE extends F.SubF {
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
			public class E {
			    public class SubE extends F.SubF {
			
			        public SubE(int i) {
			            super(i);
			        }
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testUndefinedConstructorWithEnclosingInGeneric() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F<S> {
			    public class SubF <T>{
			        public SubF(S s, T t) {
			        }
			    }
			}
			""";
		pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E extends F<String>.SubF<String> {
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
			public class E extends F<String>.SubF<String> {
			
			    public E(F<String> f, String s, String t) {
			        f.super(s, t);
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testUndefinedConstructorWithEnclosing3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F {
			    public static class SubF {
			        public SubF(int i) {
			        }
			        public class SubF2 extends SubF {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("F.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class F {
			    public static class SubF {
			        public SubF(int i) {
			        }
			        public class SubF2 extends SubF {
			
			            public SubF2(int i) {
			                super(i);
			            }
			        }
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testNotVisibleConstructorInDefaultConstructor() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F {
			    private F() {
			    }
			    public F(Runnable runnable) {
			    }
			}
			""";
		pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E extends F {
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
			public class E extends F {
			
			    public E(Runnable runnable) {
			        super(runnable);
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testUnhandledExceptionInDefaultConstructor() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			public class F {
			    public F() throws IOException{
			    }
			}
			""";
		pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E extends F {
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
			
			import java.io.IOException;
			
			public class E extends F {
			
			    public E() throws IOException {
			        super();
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testUnusedPrivateField() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private int count;
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
			}
			""";

		expected[1]= """
			package test1;
			public class E {
			    private int count;
			
			    public int getCount() {
			        return count;
			    }
			
			    public void setCount(int count) {
			        this.count = count;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedPrivateField1() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private int count, color= count;
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
			    private int count;
			}
			""";

		expected[1]= """
			package test1;
			public class E {
			    private int count, color= count;
			
			    public int getColor() {
			        return color;
			    }
			
			    public void setColor(int color) {
			        this.color = color;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedPrivateField2() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private int count= 0;
			    public void foo() {
			        count= 1 + 2;
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
			    public void foo() {
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E {
			    private int count= 0;
			    public void foo() {
			        setCount(1 + 2);
			    }
			    public int getCount() {
			        return count;
			    }
			    public void setCount(int count) {
			        this.count = count;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedPrivateField3() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private E e= new E();
			    private int value;
			    public void foo() {
			        value= 0;
			        this.value= 0;
			        e.value= 0;
			        this.e.value= 0;
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
			    private E e= new E();
			    public void foo() {
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E {
			    private E e= new E();
			    private int value;
			    public void foo() {
			        setValue(0);
			        this.setValue(0);
			        e.setValue(0);
			        this.e.setValue(0);
			    }
			    public int getValue() {
			        return value;
			    }
			    public void setValue(int value) {
			        this.value = value;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedPrivateFieldBug328481() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private int count;
			    void foo(){;
			        count++;
			        count--;
			        --count;
			        ++count;
			        for ( ; ; count++) {
			        }
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
			    void foo(){;
			        for ( ; ;) {
			        }
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E {
			    private int count;
			    void foo(){;
			        setCount(getCount() + 1);
			        setCount(getCount() - 1);
			        setCount(getCount() - 1);
			        setCount(getCount() + 1);
			        for ( ; ; count++) {
			        }
			    }
			    public int getCount() {
			        return count;
			    }
			    public void setCount(int count) {
			        this.count = count;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedVariable() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        boolean res= process();
			        res= (super.hashCode() == 1);
			    }
			    public boolean process() {
			        return true;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String expected[]=new String[2];
		expected[0]="""
			package test1;
			public class E {
			    public void foo() {
			        process();
			        (super.hashCode() == 1);
			    }
			    public boolean process() {
			        return true;
			    }
			}
			""";

		expected[1]="""
			package test1;
			public class E {
			    public void foo() {
			    }
			    public boolean process() {
			        return true;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedVariable1() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private Object[] foo() {
			        Object[] i, j= new Object[0];
			        i= j = null;
			        i= (new Object[] { null, null });
			        return j;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String[] expected=new String[2];
		expected[0]="""
			package test1;
			public class E {
			    private Object[] foo() {
			        Object[] j= new Object[0];
			        j = null;
			        return j;
			    }
			}
			""";

		expected[1]="""
			package test1;
			public class E {
			    private Object[] foo() {
			        Object[] j= new Object[0];
			        j = null;
			        return j;
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedVariable2() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private void foo() {
			        for (int j= 0, i= 0; i < 3; i++) {
			             j= i;
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String[] expected=new String[2];
		expected[0]="""
			package test1;
			public class E {
			    private void foo() {
			        for (int i= 0; i < 3; i++) {
			        };
			    }
			}
			""";

		expected[1]="""
			package test1;
			public class E {
			    private void foo() {
			        for (int i= 0; i < 3; i++) {
			        };
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedVariable4() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER_INCLUDE_DOC_COMMENT_REFERENCE, JavaCore.DISABLED);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    /**
			     * @param i
			     */
			    private void foo(int i) {
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
			    /**
			     */
			    private void foo() {
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testUnusedVariable5() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class B {
			    private final String c="Test";
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("B.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		String[] expected=new String[2];
		expected[0]="""
			package pack;
			public class B {
			}
			""";

		expected[1]="""
			package pack;
			public class B {
			    private final String c="Test";
			
			    public String getC() {
			        return c;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);

	}

	@Test
	public void testUnusedVariable6() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class B {
			    private String c=String.valueOf(true),d="test";
			    String f=d;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("B.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		String[] expected= new String[2];
		expected[0]= """
			package pack;
			public class B {
			    private String d="test";
			    String f=d;
			}
			""";

		expected[1]="""
			package pack;
			public class B {
			    private String c=String.valueOf(true),d="test";
			    String f=d;
			    public String getC() {
			        return c;
			    }
			    public void setC(String c) {
			        this.c = c;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}


	@Test
	public void testUnusedVariable7() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class B {
			    void test(){
			        String c="Test",d=String.valueOf(true),e=c;
			        e+="";
			        d="blubb";
			        d=String.valueOf(12);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("B.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		String[] expected= new String[2];
		expected[0]= """
			package pack;
			public class B {
			    void test(){
			        String c="Test";
			        String.valueOf(true);
			        String e=c;
			        e+="";
			        String.valueOf(12);
			    }
			}
			""";

		expected[1]= """
			package pack;
			public class B {
			    void test(){
			        String c="Test",e=c;
			        e+="";
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedVariable8() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			public class E {
			    public void foo() {
			        E x = (E) bar();
			    }
			
			    private Object bar() {
			        throw null;
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
			    public void foo() {
			        bar();
			    }
			
			    private Object bar() {
			        throw null;
			    }
			}
			""";

		expected[1]= """
			package pack;
			
			public class E {
			    public void foo() {
			    }
			
			    private Object bar() {
			        throw null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedVariableBug328481_1() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private void foo() {
			        int a= 10;
			        a++;
			        a--;
			        --a;
			        ++a;
			        for ( ; ; a++) {
			        }
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
			    private void foo() {
			        for ( ; ;) {
			        }
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E {
			    private void foo() {
			        for ( ; ;) {
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedVariableBug328481_2() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private void foo(boolean b) {
			        int a= 10;
			        if (b)
			            a++;
			        System.out.println("hi");
			        a -= 18;
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
			    private void foo(boolean b) {
			        if (b) {
			        }
			        System.out.println("hi");
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E {
			    private void foo(boolean b) {
			        if (b) {
			        }
			        System.out.println("hi");
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedVariableBug513404_1() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private void foo(Object o) {
			        String s = o == null ? "" : o.toString();
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
			    private void foo(Object o) {
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E {
			    private void foo(Object o) {
			        if (o == null) {
			        } else {
			            o.toString();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedVariableBug513404_2() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private void foo(Object o) {
			        String s = o == null ? "A" : "B";
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
			    private void foo(Object o) {
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E {
			    private void foo(Object o) {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedVariableBug513404_3() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private void foo(Object o) {
			        String s = (o != null) ? ((o.toString())) : "";
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
			    private void foo(Object o) {
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E {
			    private void foo(Object o) {
			        if (o != null) {
			            o.toString();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedVariableBug513404_4() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private void foo() {
			        int r = 0;
			        int s = (r == 0) ? r++ : (r -= 1);
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
			    private void foo() {
			        int r = 0;
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E {
			    private void foo() {
			        int r = 0;
			        if (r == 0) {
			            r++;
			        } else {
			            r -= 1;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedVariableAsSwitchStatement() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class B {
			    void test(int i){
			        switch (i) {
			            case 3:
			                String c="Test",d=String.valueOf(true),e=c;
			                e+="";
			                d="blubb";
			                d=String.valueOf(12);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("B.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		String[] expected= new String[2];
		expected[0]= """
			package pack;
			public class B {
			    void test(int i){
			        switch (i) {
			            case 3:
			                String c="Test";
			                String.valueOf(true);
			                String e=c;
			                e+="";
			                String.valueOf(12);
			        }
			    }
			}
			""";

		expected[1]= """
			package pack;
			public class B {
			    void test(int i){
			        switch (i) {
			            case 3:
			                String c="Test",e=c;
			                e+="";
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedVariableBug120579() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E  {
			    public void foo() {
			        char[] array= new char[0];
			        for (char element: array) {}
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
	}


	@Test
	public void testUnusedVariableWithSideEffectAssignments() throws Exception {
		// https://bugs.eclipse.org/421717
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			public class E {
			    void foo() {
			        int h= super.hashCode();
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
			    void foo() {
			    }
			}
			""";

		expected[1]= """
			package pack;
			
			public class E {
			    void foo() {
			        super.hashCode();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedVariableWithSideEffectAssignments2() throws Exception {
		// https://bugs.eclipse.org/421717
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			public class E {
			    void foo(int a) {
			        int f= 1 + a-- + (int) Math.ceil(a);
			        f= -a;
			        f= ~a;
			        f= a++;
			        f= Math.abs(a);
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
			    void foo(int a) {
			    }
			}
			""";

		expected[1]= """
			package pack;
			
			public class E {
			    void foo(int a) {
			        a--;
			        Math.ceil(a);
			        a++;
			        Math.abs(a);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedVariableWithSideEffectAssignments3() throws Exception {
		// https://bugs.eclipse.org/421717
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			public class E {
			    int f;
			    void foo() {
			        int a = 1, b= f++ - --f, c= a;
			        System.out.println(a);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		String[] expected= new String[2];
		expected[0]= """
			package pack;
			
			public class E {
			    int f;
			    void foo() {
			        int a = 1, c= a;
			        System.out.println(a);
			    }
			}
			""";

		expected[1]= """
			package pack;
			
			public class E {
			    int f;
			    void foo() {
			        int a = 1;
			        f++;
			        --f;
			        int c= a;
			        System.out.println(a);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedVariableWithSideEffectAssignments4() throws Exception {
		// https://bugs.eclipse.org/421717
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			public class E {
			    int f;
			    void foo() {
			        int a = 1, b = "".hashCode() + 1;
			        System.out.println(a);
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
			    int f;
			    void foo() {
			        int a = 1;
			        System.out.println(a);
			    }
			}
			""";

		expected[1]= """
			package pack;
			
			public class E {
			    int f;
			    void foo() {
			        int a = 1;
			        "".hashCode();
			        System.out.println(a);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedParam() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private void foo(Object str) {
			        {
			            str= toString();
			            str= new String[] { toString(), toString() };
			        }
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
			    private void foo() {
			        {
			            toString();
			            new String[] { toString(), toString() };
			        }
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E {
			    /**
			     * @param str \s
			     */
			    private void foo(Object str) {
			        {
			            str= toString();
			            str= new String[] { toString(), toString() };
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedParam2() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    /**
			     * @see E
			     */
			    private void foo(Object str) {
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
			    /**
			     * @see E
			     */
			    private void foo() {
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E {
			    /**
			     * @param str\s
			     * @see E
			     */
			    private void foo(Object str) {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedParamBug328481() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private void foo(int a) {
			        a++;
			        a--;
			        --a;
			        ++a;
			        for ( ; ; a++) {
			        }
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
			    private void foo() {
			        for ( ; ;) {
			        }
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E {
			    /**
			     * @param a \s
			     */
			    private void foo(int a) {
			        a++;
			        a--;
			        --a;
			        ++a;
			        for ( ; ; a++) {
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedPrivateMethod() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public int fCount;
			\s
			    private void foo() {
			        fCount= 1;
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
			    public int fCount;
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testUnusedPrivateConstructor() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public E() {
			    }
			\s
			    private E(int i) {
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
			    public E() {
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testUnusedPrivateType() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private class F {
			    }
			\s
			    public E() {
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
			    public E() {
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testUnnecessaryCast1() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int i) {
			        int s = (int) i;
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
			    public void foo(int i) {
			        int s = i;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testUnnecessaryCast2() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(String s) {
			        String r = ((String) s);
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
			    public void foo(String s) {
			        String r = (s);
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testUnnecessaryCast3() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int i) {
			        int s = ((int) 1 + 2) * 3;
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
			    public void foo(int i) {
			        int s = (1 + 2) * 3;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testUnnecessaryCastBug335173_1() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Integer n) {
			        int i = (((Integer) n)).intValue();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E {
			    public void foo(Integer n) {
			        int i = ((n)).intValue();
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testUnnecessaryCastBug335173_2() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Integer n) {
			        int i = ((Integer) (n)).intValue();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E {
			    public void foo(Integer n) {
			        int i = (n).intValue();
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testUnnecessaryCastBug578911() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Integer n) {
			        n = (Integer)n;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E {
			    public void foo(Integer n) {
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testSuperfluousSemicolon() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_EMPTY_STATEMENT, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int i) {
			        int s= 1;;
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
			    public void foo(int i) {
			        int s= 1;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testIndirectStaticAccess1() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_INDIRECT_STATIC_ACCESS, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment other= fSourceFolder.createPackageFragment("other", false, null);
		String str= """
			package other;
			public class A {
			    public static final int CONST=1;
			}
			""";
		other.createCompilationUnit("A.java", str, false, null);

		IPackageFragment pack0= fSourceFolder.createPackageFragment("pack", false, null);
		String str1= """
			package pack;
			public class B extends other.A {
			}
			""";
		pack0.createCompilationUnit("B.java", str1, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str2= """
			package test1;
			import pack.B;
			public class E {
			    public int foo(B b) {
			        return B.CONST;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str2, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str3= """
			package test1;
			import other.A;
			import pack.B;
			public class E {
			    public int foo(B b) {
			        return A.CONST;
			    }
			}
			""";
		assertEqualString(preview, str3);
	}

	@Test
	public void testIndirectStaticAccess2() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_INDIRECT_STATIC_ACCESS, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment other= fSourceFolder.createPackageFragment("other", false, null);
		String str= """
			package other;
			public class A {
			    public static int foo() {
			        return 1;
			    }
			}
			""";
		other.createCompilationUnit("A.java", str, false, null);

		IPackageFragment pack0= fSourceFolder.createPackageFragment("pack", false, null);
		String str1= """
			package pack;
			public class B extends other.A {
			}
			""";
		pack0.createCompilationUnit("B.java", str1, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str2= """
			package test1;
			public class E {
			    public int foo() {
			        return pack.B.foo();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str2, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str3= """
			package test1;
			
			import other.A;
			
			public class E {
			    public int foo() {
			        return A.foo();
			    }
			}
			""";
		assertEqualString(preview, str3);
	}

	@Test
	public void testIndirectStaticAccess_bug40880() throws Exception {

		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_INDIRECT_STATIC_ACCESS, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			class FileType {
			    public String extension;
			}
			""";
		pack1.createCompilationUnit("FileType.java", str, false, null);

		String str1= """
			package test1;
			interface ToolConfigurationSettingsConstants {
			     FileType FILE_TYPE = null;
			}
			""";
		pack1.createCompilationUnit("ToolConfigurationSettingsConstants.java", str1, false, null);


		String str2= """
			package test1;
			interface ToolUserSettingsConstants extends ToolConfigurationSettingsConstants {
			}
			""";
		pack1.createCompilationUnit("ToolUserSettingsConstants.java", str2, false, null);

		String str3= """
			package test1;
			public class E {
			    public void foo() {
			        ToolUserSettingsConstants.FILE_TYPE.extension.toLowerCase();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str3, false, null);


		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str4= """
			package test1;
			public class E {
			    public void foo() {
			        ToolConfigurationSettingsConstants.FILE_TYPE.extension.toLowerCase();
			    }
			}
			""";
		assertEqualString(preview, str4);
	}

	@Test
	public void testIndirectStaticAccess_bug32022() throws Exception {

		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_INDIRECT_STATIC_ACCESS, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class StaticField {
			    public boolean flag;
			}
			""";
		pack1.createCompilationUnit("StaticField.java", str, false, null);

		String str1= """
			package test1;
			public class ConstClass {
			     public static StaticField staticField = new StaticField();
			}
			""";
		pack1.createCompilationUnit("ConstClass.java", str1, false, null);


		String str2= """
			package test1;
			public class E {
			    public void foo(ConstClass constclass) {
			        constclass.staticField.flag= true;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str2, false, null);


		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo(ConstClass constclass) {
			        ConstClass.staticField.flag= true;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class ConstClass {
			     public StaticField staticField = new StaticField();
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testIndirectStaticAccess_bug307407() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_INDIRECT_STATIC_ACCESS, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private final String localString = new MyClass().getMyString();
			    public static class MyClass {
			        public static String getMyString() {
			            return "a";
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    private final String localString = MyClass.getMyString();
			    public static class MyClass {
			        public static String getMyString() {
			            return "a";
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    private final String localString = new MyClass().getMyString();
			    public static class MyClass {
			        public String getMyString() {
			            return "a";
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}


	@Test
	public void testUnnecessaryInstanceof1() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(String b) {
			        return (b instanceof String);
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
			    public boolean foo(String b) {
			        return (b != null);
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testUnnecessaryInstanceof2() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(String b) {
			        if  (b instanceof String && b.getClass() != null) {
			            System.out.println();
			        }
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
			    public void foo(String b) {
			        if  (b != null && b.getClass() != null) {
			            System.out.println();
			        }
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testUnnecessaryThrownException1() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			public class E {
			    public void foo(String b) throws IOException {
			        if  (b != null) {
			            System.out.println();
			        }
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
			    public void foo(String b) {
			        if  (b != null) {
			            System.out.println();
			        }
			    }
			}
			""";

		expected[1]= """
			package test1;
			import java.io.IOException;
			public class E {
			    /**
			     * @throws IOException \s
			     */
			    public void foo(String b) throws IOException {
			        if  (b != null) {
			            System.out.println();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnnecessaryThrownException2() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    /**
			     * @throws IOException
			     */
			    public E(int i) throws IOException, ParseException {
			        if  (i == 0) {
			            throw new IOException();
			        }
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
			import java.io.IOException;
			public class E {
			    /**
			     * @throws IOException
			     */
			    public E(int i) throws IOException {
			        if  (i == 0) {
			            throw new IOException();
			        }
			    }
			}
			""";

		expected[1]= """
			package test1;
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    /**
			     * @throws IOException
			     * @throws ParseException\s
			     */
			    public E(int i) throws IOException, ParseException {
			        if  (i == 0) {
			            throw new IOException();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnnecessaryThrownException3() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_INCLUDE_DOC_COMMENT_REFERENCE, JavaCore.DISABLED);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    /**
			     * @param i
			     * @throws IOException
			     * @throws ParseException
			     */
			    public void foo(int i) throws IOException, ParseException {
			        if  (i == 0) {
			            throw new IOException();
			        }
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
			import java.io.IOException;
			public class E {
			    /**
			     * @param i
			     * @throws IOException
			     */
			    public void foo(int i) throws IOException {
			        if  (i == 0) {
			            throw new IOException();
			        }
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testUnnecessaryThrownException4() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    /**
			     * @throws IOException
			     */
			    public E(int i) throws IOException, ParseException {
			        if  (i == 0) {
			            throw new IOException();
			        }
			    }
			    public void foo(int i) throws ParseException {
			        if  (i == 0) {
			            throw new ParseException(null, 4);
			        }
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
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    /**
			     * @throws IOException
			     */
			    public E(int i) throws IOException {
			        if  (i == 0) {
			            throw new IOException();
			        }
			    }
			    public void foo(int i) throws ParseException {
			        if  (i == 0) {
			            throw new ParseException(null, 4);
			        }
			    }
			}
			""";

		expected[1]= """
			package test1;
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    /**
			     * @throws IOException
			     * @throws ParseException\s
			     */
			    public E(int i) throws IOException, ParseException {
			        if  (i == 0) {
			            throw new IOException();
			        }
			    }
			    public void foo(int i) throws ParseException {
			        if  (i == 0) {
			            throw new ParseException(null, 4);
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnqualifiedFieldAccess1() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNQUALIFIED_FIELD_ACCESS, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private int count;
			    public E(int i) {
			        count= i;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String[] expecteds=new String[1];
		expecteds[0]="""
			package test1;
			public class E {
			    private int count;
			    public E(int i) {
			        this.count= i;
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, expecteds);
	}

	@Test
	public void testUnqualifiedFieldAccess2() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNQUALIFIED_FIELD_ACCESS, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F {
			    public int count;
			}
			""";
		pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E {
			    private F f= new F();
			    public E(int i) {
			        f.count= i;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String[] expecteds=new String[1];
		expecteds[0]="""
			package test1;
			public class E {
			    private F f= new F();
			    public E(int i) {
			        this.f.count= i;
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, expecteds);
	}

	@Test
	public void testUnqualifiedFieldAccess3() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNQUALIFIED_FIELD_ACCESS, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F {
			    public void setCount(int i) {}
			}
			""";
		pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E {
			    private F f= new F();
			    public E(int i) {
			        f.setCount(i);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String[] expecteds=new String[1];
		expecteds[0]="""
			package test1;
			public class E {
			    private F f= new F();
			    public E(int i) {
			        this.f.setCount(i);
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, expecteds);
	}

	@Test
	public void testUnqualifiedFieldAccess4() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNQUALIFIED_FIELD_ACCESS, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private int count;
			    public E(int i) {
			        class Inner {
			            public void foo() {
			               count= 1;
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String[] expecteds=new String[1];
		expecteds[0]="""
			package test1;
			public class E {
			    private int count;
			    public E(int i) {
			        class Inner {
			            public void foo() {
			               E.this.count= 1;
			            }
			        }
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, expecteds);
	}

	@Test
	public void testUnqualifiedFieldAccess_bug50960() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNQUALIFIED_FIELD_ACCESS, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F {
			    private int count;
			}
			""";
		pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E extends F {
			    private int count;
			    public E(int i) {
			        count= 1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String[] expecteds=new String[1];
		expecteds[0]="""
			package test1;
			public class E extends F {
			    private int count;
			    public E(int i) {
			        this.count= 1;
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, expecteds);
	}

	@Test
	public void testUnqualifiedFieldAccess_bug88313() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNQUALIFIED_FIELD_ACCESS, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F {
			    protected Object someObject;
			}
			""";
		pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E extends F {
			    public void foo() {
			        new Object() {
			            public String toString() {
			                return someObject.getClass().getName();
			            }
			         };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String[] expecteds=new String[1];
		expecteds[0]="""
			package test1;
			public class E extends F {
			    public void foo() {
			        new Object() {
			            public String toString() {
			                return E.this.someObject.getClass().getName();
			            }
			         };
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, expecteds);
	}

	@Test
	public void testUnqualifiedFieldAccess_bug115277() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNQUALIFIED_FIELD_ACCESS, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F {
			    public abstract class E1Inner1 {
			        public abstract void foo();
			        protected int n;
			    }
			    public abstract class E1Inner2 {
			        public abstract void run();
			    }
			    public void foo() {
			        E1Inner1 inner= new E1Inner1() {
			            public void foo() {
			                E1Inner2 inner2= new E1Inner2() {
			                    public void run() {
			                        System.out.println(n);
			                    }
			                };
			            }
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("F.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
	}

	@Test
	public void testUnqualifiedFieldAccess_bug138325_1() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNQUALIFIED_FIELD_ACCESS, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E<T> {
			    public int i;
			    public void foo() {
			        System.out.println(i);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String[] expecteds=new String[1];
		expecteds[0]="""
			package test1;
			public class E<T> {
			    public int i;
			    public void foo() {
			        System.out.println(this.i);
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, expecteds);
	}

	@Test
	public void testUnqualifiedFieldAccess_bug138325_2() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNQUALIFIED_FIELD_ACCESS, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E<T> {
			    public int i;
			    public void foo() {
			        Runnable runnable = new Runnable() {
			            public void run() {
			                System.out.println(i);
			            }
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String[] expecteds=new String[1];
		expecteds[0]="""
			package test1;
			public class E<T> {
			    public int i;
			    public void foo() {
			        Runnable runnable = new Runnable() {
			            public void run() {
			                System.out.println(E.this.i);
			            }
			        };
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, expecteds);
	}

	@Test
	public void testUnqualifiedFieldAccessWithGenerics() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNQUALIFIED_FIELD_ACCESS, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F<T> {
			    protected T someObject;
			}
			""";
		pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E<T> extends F<String> {
			    public void foo() {
			        class X {
			            public String toString() {
			                return someObject.getClass().getName();
			            }
			         };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String[] expecteds=new String[1];
		expecteds[0]="""
			package test1;
			public class E<T> extends F<String> {
			    public void foo() {
			        class X {
			            public String toString() {
			                return E.this.someObject.getClass().getName();
			            }
			         };
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, expecteds);
	}

	@Test
	public void testHidingVariable1() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_LOCAL_VARIABLE_HIDING, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private int count;
			    public void foo() {
			       int count= 1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		assertTrue(proposals.get(0) instanceof LinkedNamesAssistProposal);
	}

	@Test
	public void testHidingVariable2() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_LOCAL_VARIABLE_HIDING, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private int count;
			    public void foo(int count) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		assertTrue(proposals.get(0) instanceof LinkedNamesAssistProposal);
	}

	@Test
	public void testHidingVariable3() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_LOCAL_VARIABLE_HIDING, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int count) {
			        class Inner {
			            private int count;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		assertTrue(proposals.get(0) instanceof LinkedNamesAssistProposal);
	}

	@Test
	public void testHidingVariable4() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_LOCAL_VARIABLE_HIDING, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private int count;
			    public void foo() {
			        class Inner {
			            private int count;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		assertTrue(proposals.get(0) instanceof LinkedNamesAssistProposal);
	}

	@Test
	public void testHidingVariable5() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_LOCAL_VARIABLE_HIDING, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int count) {
			        class Inner {
			            public void foo() {
			                 int count;
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		assertTrue(proposals.get(0) instanceof LinkedNamesAssistProposal);
	}

	@Test
	public void testHidingVariable6() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_LOCAL_VARIABLE_HIDING, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int count) {
			        class Inner {
			            public void foo(int count) {
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		assertTrue(proposals.get(0) instanceof LinkedNamesAssistProposal);
	}

	@Test
	public void testSetParenteses1() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_LOCAL_VARIABLE_HIDING, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Object x) {
			        if (!x instanceof Runnable) {
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
			    public void foo(Object x) {
			        if (!(x instanceof Runnable)) {
			        }
			    }
			}
			""";
		assertEqualString(preview, str1);

	}

	@Test
	public void testSetParenteses2() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_LOCAL_VARIABLE_HIDING, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(int x) {
			        return !x instanceof Runnable || true;
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
			    public boolean foo(int x) {
			        return !(x instanceof Runnable) || true;
			    }
			}
			""";
		assertEqualString(preview, str1);

	}

	@Test
	public void testUnnecessaryElse1() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNNECESSARY_ELSE, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(int x) {
			        if (x == 9) {
			            return true;
			        } else {
			            return false;
			        }
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
			    public boolean foo(int x) {
			        if (x == 9) {
			            return true;
			        }
			        return false;
			    }
			}
			""";
		assertEqualString(preview, str1);

	}
	@Test
	public void testUnnecessaryElse2() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNNECESSARY_ELSE, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(int x) {
			        if (x == 9) {
			            return true;
			        } else {
			            x= 9;
			            return false;
			        }
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
			    public boolean foo(int x) {
			        if (x == 9) {
			            return true;
			        }
			        x= 9;
			        return false;
			    }
			}
			""";
		assertEqualString(preview, str1);

	}

	@Test
	public void testUnnecessaryElse3() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNNECESSARY_ELSE, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(int x) {
			        if (x == 9) {
			            return true;
			        } else
			            return false;
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
			    public boolean foo(int x) {
			        if (x == 9) {
			            return true;
			        }
			        return false;
			    }
			}
			""";
		assertEqualString(preview, str1);

	}

	@Test
	public void testUnnecessaryElse4() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNNECESSARY_ELSE, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			public class E {
			    boolean foo(int i) {
			        if (i < 100)
			            if (i == 42)
			                return true;
			            else
			                i = i + 3;
			        return false;
			    }
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
			    boolean foo(int i) {
			        if (i < 100) {
			            if (i == 42)
			                return true;
			            i = i + 3;
			        }
			        return false;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnnecessaryElse5() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNNECESSARY_ELSE, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			public class E {
			    boolean foo(int i) {
			        switch (i) {
			            case 42:
			                if (foo(i+1))
			                    return true;
			                else
			                    i = i + 3;
			        }
			        return false;
			    }
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
			    boolean foo(int i) {
			        switch (i) {
			            case 42:
			                if (foo(i+1))
			                    return true;
			                i = i + 3;
			        }
			        return false;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testInterfaceExtendsClass() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNNECESSARY_ELSE, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.List;
			public class E extends List {
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
			import java.util.List;
			public class E implements List {
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.List;
			public interface E extends List {
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testRemoveUnreachableCodeStmt() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNNECESSARY_ELSE, JavaCore.IGNORE);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(int x) {
			        if (x == 9) {
			            return true;
			        } else
			            return false;
			        return false;
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
			    public boolean foo(int x) {
			        if (x == 9) {
			            return true;
			        } else
			            return false;
			    }
			}
			""";
		assertEqualString(preview, str1);

	}

	@Test
	public void testRemoveUnreachableCodeStmt2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public String getName() {
			        try{
			            return "fred";
			        }
			        catch (Exception e){
			            return e.getLocalizedMessage();
			        }
			        System.err.print("wow");
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
			package test;
			public class E {
			    public String getName() {
			        try{
			            return "fred";
			        }
			        catch (Exception e){
			            return e.getLocalizedMessage();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testRemoveUnreachableCodeWhile() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo() {
			        while (false) {
			            return true;
			        }
			        return false;
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
			    public boolean foo() {
			        return false;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testRemoveDeadCodeIfThen() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEAD_CODE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        if (false) {
			            System.out.println("a");
			        } else {
			            System.out.println("b");
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        System.out.println("b");
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    @SuppressWarnings("unused")
			    public void foo() {
			        if (false) {
			            System.out.println("a");
			        } else {
			            System.out.println("b");
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testRemoveDeadCodeIfThen2() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEAD_CODE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        Object o = new Object();
			        if (o != null) {
			            if (o == null) {
			            	System.out.println("hello");
			        	} else {
			            	System.out.println("bye");
			        	}
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        Object o = new Object();
			        if (o != null) {
			            System.out.println("bye");
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    @SuppressWarnings("unused")
			    public void foo() {
			        Object o = new Object();
			        if (o != null) {
			            if (o == null) {
			            	System.out.println("hello");
			        	} else {
			            	System.out.println("bye");
			        	}
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testRemoveDeadCodeIfThen3() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEAD_CODE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        Object o = new Object();
			        if (o != null)\s
			            if (o == null) {
			            	System.out.println("hello");
			        	} else {
			            	System.out.println("bye");
			            	System.out.println("bye-bye");
			        	}
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        Object o = new Object();
			        if (o != null) {
			        	System.out.println("bye");
			        	System.out.println("bye-bye");
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    @SuppressWarnings("unused")
			    public void foo() {
			        Object o = new Object();
			        if (o != null)\s
			            if (o == null) {
			            	System.out.println("hello");
			        	} else {
			            	System.out.println("bye");
			            	System.out.println("bye-bye");
			        	}
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testRemoveDeadCodeIfThen4() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEAD_CODE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        Object o = new Object();
			        if (o != null)\s
			            if (true)\s
			            	if (o == null)\s
			            		System.out.println("hello");
					System.out.println("bye");
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        Object o = new Object();
			        if (o != null)\s
			            if (true) {
			            }
					System.out.println("bye");
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    @SuppressWarnings("unused")
			    public void foo() {
			        Object o = new Object();
			        if (o != null)\s
			            if (true)\s
			            	if (o == null)\s
			            		System.out.println("hello");
					System.out.println("bye");
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testRemoveDeadCodeIfThen5() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEAD_CODE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        Object o = new Object();
			        if (o != null)\s
			            if (false)\s
			            	if (o == null)\s
			            		System.out.println("hello");
					System.out.println("bye");
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        Object o = new Object();
			        if (o != null) {
			        }
					System.out.println("bye");
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    @SuppressWarnings("unused")
			    public void foo() {
			        Object o = new Object();
			        if (o != null)\s
			            if (false)\s
			            	if (o == null)\s
			            		System.out.println("hello");
					System.out.println("bye");
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testRemoveDeadCodeIfThenSwitch() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEAD_CODE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        switch (1) {
			            case 1:
			                if (false) {
			                	foo();
								System.out.println("hi");
							} else {
			                	System.out.println("bye");
							}
			                break;
			            case 2:
			                foo();
			                break;
			            default:
			                break;
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        switch (1) {
			            case 1:
			                System.out.println("bye");
			                break;
			            case 2:
			                foo();
			                break;
			            default:
			                break;
			        };
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    @SuppressWarnings("unused")
			    public void foo() {
			        switch (1) {
			            case 1:
			                if (false) {
			                	foo();
								System.out.println("hi");
							} else {
			                	System.out.println("bye");
							}
			                break;
			            case 2:
			                foo();
			                break;
			            default:
			                break;
			        };
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testRemoveDeadCodeIfElse() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEAD_CODE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        if (Math.random() == -1 || true) {
			            System.out.println("a");
			        } else {
			            System.out.println("b");
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        System.out.println("a");
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    @SuppressWarnings("unused")
			    public void foo() {
			        if (Math.random() == -1 || true) {
			            System.out.println("a");
			        } else {
			            System.out.println("b");
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testRemoveDeadCodeAfterIf() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEAD_CODE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo() {
			        if (true) return false;
			        return true;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public boolean foo() {
			        return false;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    @SuppressWarnings("unused")
			    public boolean foo() {
			        if (true) return false;
			        return true;
			    }
			}
			""";

		String str1= """
			package test1;
			public class E1 {
			    public boolean foo() {
			        if (true) {
			            return true;
			        }
			        return false;
			    }
			}
			""";
		cu= pack1.createCompilationUnit("E1.java", str1, false, null);
		astRoot= getASTRoot(cu);
		proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		proposal= (CUCorrectionProposal) proposals.get(0);
		String preview3= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class E2 {
			    public boolean foo() {
			        if (false) {
			            return true;
			        }
			        return false;
			    }
			}
			""";
		cu= pack1.createCompilationUnit("E2.java", str2, false, null);
		astRoot= getASTRoot(cu);
		proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		proposal= (CUCorrectionProposal) proposals.get(0);
		String preview4= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E1 {
			    public boolean foo() {
			        return true;
			    }
			}
			""";

		String expected4= """
			package test1;
			public class E2 {
			    public boolean foo() {
			        return false;
			    }
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });
	}

	@Test
	public void testRemoveDeadCodeAfterIf2() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEAD_CODE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(boolean b1, boolean b2) {
			        if ((false && b1) && b2) {
			            return true;
			        }
			        return false;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public boolean foo(boolean b1, boolean b2) {
			        if (false && b2) {
			            return true;
			        }
			        return false;
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    @SuppressWarnings("unused")
			    public boolean foo(boolean b1, boolean b2) {
			        if ((false && b1) && b2) {
			            return true;
			        }
			        return false;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testRemoveDeadCodeAfterIf3() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEAD_CODE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(boolean b1, boolean b2) {
			        if ((b1 && false) && b2) {
			            return true;
			        }
			        return false;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public boolean foo(boolean b1, boolean b2) {
			        if (b1 && false) {
			            return true;
			        }
			        return false;
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    @SuppressWarnings("unused")
			    public boolean foo(boolean b1, boolean b2) {
			        if ((b1 && false) && b2) {
			            return true;
			        }
			        return false;
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E {
			    public boolean foo(boolean b1, boolean b2) {
			        if (b1 && false) {
			            if (b2) {
			                return true;
			            }
			        }
			        return false;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testRemoveDeadCodeAfterIf4() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEAD_CODE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(boolean b1, boolean b2) {
			        if ((((b1 && false))) && b2) {
			            return true;
			        }
			        return false;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public boolean foo(boolean b1, boolean b2) {
			        if (b1 && false) {
			            return true;
			        }
			        return false;
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    @SuppressWarnings("unused")
			    public boolean foo(boolean b1, boolean b2) {
			        if ((((b1 && false))) && b2) {
			            return true;
			        }
			        return false;
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E {
			    public boolean foo(boolean b1, boolean b2) {
			        if (b1 && false) {
			            if (b2) {
			                return true;
			            }
			        }
			        return false;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testRemoveDeadCodeAfterIf5() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEAD_CODE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(boolean b1, boolean b2) {
			        if ((((b1 && false) && b2))) {
			            return true;
			        }
			        return false;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public boolean foo(boolean b1, boolean b2) {
			        if (b1 && false) {
			            return true;
			        }
			        return false;
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    @SuppressWarnings("unused")
			    public boolean foo(boolean b1, boolean b2) {
			        if ((((b1 && false) && b2))) {
			            return true;
			        }
			        return false;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testRemoveDeadCodeAfterIf6() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEAD_CODE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(boolean b1, boolean b2) {
			        if ((((false && b1) && b2))) {
			            return true;
			        }
			        return false;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public boolean foo(boolean b1, boolean b2) {
			        if (((false && b2))) {
			            return true;
			        }
			        return false;
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    @SuppressWarnings("unused")
			    public boolean foo(boolean b1, boolean b2) {
			        if ((((false && b1) && b2))) {
			            return true;
			        }
			        return false;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testRemoveDeadCodeAfterIf7() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEAD_CODE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(boolean b1, boolean b2) {
			        if ((((false && b1))) && b2) {
			            return true;
			        }
			        return false;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public boolean foo(boolean b1, boolean b2) {
			        if (false && b2) {
			            return true;
			        }
			        return false;
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    @SuppressWarnings("unused")
			    public boolean foo(boolean b1, boolean b2) {
			        if ((((false && b1))) && b2) {
			            return true;
			        }
			        return false;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testRemoveDeadCodeAfterIf8() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEAD_CODE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(boolean b1) {
			        if ((((false && b1)))) {
			            return true;
			        }
			        return false;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public boolean foo(boolean b1) {
			        if (false) {
			            return true;
			        }
			        return false;
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    @SuppressWarnings("unused")
			    public boolean foo(boolean b1) {
			        if ((((false && b1)))) {
			            return true;
			        }
			        return false;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testRemoveDeadCodeAfterIf9() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEAD_CODE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(boolean b1, boolean b2) {
			        if (false && b1 && b2) {
			            return true;
			        }
			        return false;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public boolean foo(boolean b1, boolean b2) {
			        if (false) {
			            return true;
			        }
			        return false;
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    @SuppressWarnings("unused")
			    public boolean foo(boolean b1, boolean b2) {
			        if (false && b1 && b2) {
			            return true;
			        }
			        return false;
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E {
			    public boolean foo(boolean b1, boolean b2) {
			        if (false) {
			            if (b1 && b2) {
			                return true;
			            }
			        }
			        return false;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testRemoveDeadCodeAfterIf10() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEAD_CODE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(boolean b1, boolean b2) {
			        if (((false && b1 && b2))) {
			            return true;
			        }
			        return false;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public boolean foo(boolean b1, boolean b2) {
			        if (false) {
			            return true;
			        }
			        return false;
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    @SuppressWarnings("unused")
			    public boolean foo(boolean b1, boolean b2) {
			        if (((false && b1 && b2))) {
			            return true;
			        }
			        return false;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testRemoveDeadCodeAfterIf11() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEAD_CODE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(boolean b1) {
			        if ((true || b1) && false) {
			            return true;
			        }
			        return false;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public boolean foo(boolean b1) {
			        if (true && false) {
			            return true;
			        }
			        return false;
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    @SuppressWarnings("unused")
			    public boolean foo(boolean b1) {
			        if ((true || b1) && false) {
			            return true;
			        }
			        return false;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testRemoveDeadCodeAfterIf12() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEAD_CODE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(boolean b1, boolean b2, boolean b3) {
			        if (((b1 && false) && b2) | b3) {
			            return true;
			        }
			        return false;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public boolean foo(boolean b1, boolean b2, boolean b3) {
			        if ((b1 && false) | b3) {
			            return true;
			        }
			        return false;
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    @SuppressWarnings("unused")
			    public boolean foo(boolean b1, boolean b2, boolean b3) {
			        if (((b1 && false) && b2) | b3) {
			            return true;
			        }
			        return false;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testRemoveDeadCodeAfterIf13() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEAD_CODE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(boolean b1, boolean b2) {
			        if ((false | false && b1) & b2) {
			            return true;
			        }
			        return false;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public boolean foo(boolean b1, boolean b2) {
			        if ((false | false) & b2) {
			            return true;
			        }
			        return false;
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    @SuppressWarnings("unused")
			    public boolean foo(boolean b1, boolean b2) {
			        if ((false | false && b1) & b2) {
			            return true;
			        }
			        return false;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testRemoveDeadCodeConditional() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEAD_CODE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public int foo() {
			        return true ? 1 : 0;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public int foo() {
			        return 1;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    @SuppressWarnings("unused")
			    public int foo() {
			        return true ? 1 : 0;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testRemoveDeadCodeConditional2() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEAD_CODE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        Object o = true ? Integer.valueOf(1) + 2 : Double.valueOf(0.0) + 3;
			        System.out.println(o);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        Object o = (double) (Integer.valueOf(1) + 2);
			        System.out.println(o);
			    }
			}
			""";
		String[] expected= new String[] { str1 };

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testRemoveDeadCodeConditional3() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEAD_CODE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        Object o = true ? Integer.valueOf(1) : Double.valueOf(0.0);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        Object o = (double) Integer.valueOf(1);
			    }
			}
			""";
		String[] expected= new String[] { str1 };

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testRemoveDeadCodeMultiStatements() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEAD_CODE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        if (true)
			            return;
			        foo();
			        foo();
			        foo();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        return;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    @SuppressWarnings("unused")
			    public void foo() {
			        if (true)
			            return;
			        foo();
			        foo();
			        foo();
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testRemoveUnreachableCodeMultiStatementsSwitch() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEAD_CODE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        switch (1) {
			            case 1:
			                foo();
			                break;
			                foo();
			                new Object();
			            case 2:
			                foo();
			                break;
			            default:
			                break;
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
			        switch (1) {
			            case 1:
			                foo();
			                break;
			            case 2:
			                foo();
			                break;
			            default:
			                break;
			        };
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testUnusedObjectAllocation1() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_UNUSED_OBJECT_ALLOCATION, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			public class E {
			    public Object foo() {
			        if (Boolean.TRUE) {
			            /*a*/new Object()/*b*/;/*c*/
			        }
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 6);

		String[] expected= new String[5];
		expected[0]= """
			package test1;
			
			public class E {
			    public Object foo() {
			        if (Boolean.TRUE) {
			            /*a*/return new Object()/*b*/;/*c*/
			        }
			        return null;
			    }
			}
			""";

		expected[1]= """
			package test1;
			
			public class E {
			    public Object foo() {
			        if (Boolean.TRUE) {
			        }
			        return null;
			    }
			}
			""";

		expected[2]= """
			package test1;
			
			public class E {
			    @SuppressWarnings("unused")
			    public Object foo() {
			        if (Boolean.TRUE) {
			            /*a*/new Object()/*b*/;/*c*/
			        }
			        return null;
			    }
			}
			""";

		expected[3]= """
			package test1;
			
			public class E {
			    public Object foo() {
			        if (Boolean.TRUE) {
			            /*a*/Object object = new Object()/*b*/;/*c*/
			        }
			        return null;
			    }
			}
			""";

		expected[4]= """
			package test1;
			
			public class E {
			    private Object object;
			
			    public Object foo() {
			        if (Boolean.TRUE) {
			            /*a*/object = new Object()/*b*/;/*c*/
			        }
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedObjectAllocation2() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_UNUSED_OBJECT_ALLOCATION, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			public class E {
			    public void foo() {
			        /*a*/new Exception()/*b*/;/*c*/
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 7);

		String[] expected= new String[6];
		expected[0]= """
			package test1;
			
			public class E {
			    public void foo() {
			        /*a*/throw new Exception()/*b*/;/*c*/
			    }
			}
			""";

		expected[1]= """
			package test1;
			
			public class E {
			    public void foo() {
			    }
			}
			""";

		expected[2]= """
			package test1;
			
			public class E {
			    @SuppressWarnings("unused")
			    public void foo() {
			        /*a*/new Exception()/*b*/;/*c*/
			    }
			}
			""";

		expected[3]= """
			package test1;
			
			public class E {
			    public void foo() {
			        /*a*/return new Exception()/*b*/;/*c*/
			    }
			}
			""";

		expected[4]= """
			package test1;
			
			public class E {
			    public void foo() {
			        /*a*/Exception exception = new Exception()/*b*/;/*c*/
			    }
			}
			""";

		expected[5]= """
			package test1;
			
			public class E {
			    private Exception exception;
			
			    public void foo() {
			        /*a*/exception = new Exception()/*b*/;/*c*/
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedObjectAllocation3() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_UNUSED_OBJECT_ALLOCATION, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			public class E {
			    private Object name;
			    public E() {
			        if (name == null)
			            new IllegalArgumentException();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 6);

		String expected= """
			package test1;
			
			public class E {
			    private Object name;
			    public E() {
			        if (name == null)
			            throw new IllegalArgumentException();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testNecessaryNLSTag1() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_NON_NLS_STRING_LITERAL, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			public class E {
			    String e = "abc";
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);

		String[] expected= new String[1];
		expected[0]= """
			package test1;
			
			public class E {
			    String e = "abc"; //$NON-NLS-1$
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnnessecaryNLSTag1() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_NON_NLS_STRING_LITERAL, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			public class E {
			    String e;    //$NON-NLS-1$
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		String[] expected= new String[2];
		expected[0]= """
			package test1;
			
			public class E {
			    String e;
			}
			""";

		expected[1]= """
			package test1;
			
			@SuppressWarnings("nls")
			public class E {
			    String e;    //$NON-NLS-1$
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnnessecaryNLSTag2() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_NON_NLS_STRING_LITERAL, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			public class E {
			    String e; //   //$NON-NLS-1$
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		String[] expected= new String[2];
		expected[0]= """
			package test1;
			
			public class E {
			    String e; //
			}
			""";

		expected[1]= """
			package test1;
			
			@SuppressWarnings("nls")
			public class E {
			    String e; //   //$NON-NLS-1$
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnnessecaryNLSTag3() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_NON_NLS_STRING_LITERAL, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo() {
			        foo(); //$NON-NLS-1$ // more comment
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
			package test;
			public class E {
			    public void foo() {
			        foo(); // more comment
			    }
			}
			""";

		expected[1]= """
			package test;
			public class E {
			    @SuppressWarnings("nls")
			    public void foo() {
			        foo(); //$NON-NLS-1$ // more comment
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnnessecaryNLSTag4() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_NON_NLS_STRING_LITERAL, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo() {
			        foo(); //$NON-NLS-1$ more comment  \s
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
			package test;
			public class E {
			    public void foo() {
			        foo(); // more comment  \s
			    }
			}
			""";

		expected[1]= """
			package test;
			public class E {
			    @SuppressWarnings("nls")
			    public void foo() {
			        foo(); //$NON-NLS-1$ more comment  \s
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnnessecaryNLSTag5() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_NON_NLS_STRING_LITERAL, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo() {
			        foo(); //$NON-NLS-1$    \s
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
			package test;
			public class E {
			    public void foo() {
			        foo();
			    }
			}
			""";

		expected[1]= """
			package test;
			public class E {
			    @SuppressWarnings("nls")
			    public void foo() {
			        foo(); //$NON-NLS-1$    \s
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnnessecaryNLSTag6() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_NON_NLS_STRING_LITERAL, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo() {
			        foo(); //$NON-NLS-1$ / more
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
			package test;
			public class E {
			    public void foo() {
			        foo(); // / more
			    }
			}
			""";

		expected[1]= """
			package test;
			public class E {
			    @SuppressWarnings("nls")
			    public void foo() {
			        foo(); //$NON-NLS-1$ / more
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}


	@Test
	public void testAssignmentWithoutSideEffect1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			public class E {
			    int count;
			    public void foo(int count) {
			        count= count;
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
			package test1;
			
			public class E {
			    int count;
			    public void foo(int count) {
			        this.count= count;
			    }
			}
			""";

		expected[1]= """
			package test1;
			
			public class E {
			    int count;
			    public void foo(int count) {
			        count= this.count;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testAssignmentWithoutSideEffect2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			public class E {
			    static int count;
			    public void foo(int count) {
			        count= count;
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
			package test1;
			
			public class E {
			    static int count;
			    public void foo(int count) {
			        E.count= count;
			    }
			}
			""";

		expected[1]= """
			package test1;
			
			public class E {
			    static int count;
			    public void foo(int count) {
			        count= E.count;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testAssignmentWithoutSideEffect3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class E {
			    int bar;
			    public void foo() {
			        this.bar= bar;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[1];
		expected[0]= """
			package pack;
			public class E {
			    int bar;
			    public void foo(int bar) {
			        this.bar= bar;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testAddTypeParametersToClassInstanceCreationTest01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			import java.util.ArrayList;
			import java.util.List;
			public class E {
			    public void foo() {
			        List<E> l= new ArrayList();\s
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.WARNING);
		JavaCore.setOptions(options);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 5);

		String[] expected= new String[1];
		expected[0]= """
			package pack;
			import java.util.ArrayList;
			import java.util.List;
			public class E {
			    public void foo() {
			        List<E> l= new ArrayList<E>();\s
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testAddTypeParametersToClassInstanceCreationTest02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			import java.util.ArrayList;
			import java.util.HashSet;
			import java.util.Hashtable;
			import java.util.List;
			public class E {
			    public void foo() {
			        List<List<Hashtable<Integer, HashSet<E>>>> l= new ArrayList();\s
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.WARNING);
		JavaCore.setOptions(options);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 5);

		String[] expected= new String[1];
		expected[0]= """
			package pack;
			import java.util.ArrayList;
			import java.util.HashSet;
			import java.util.Hashtable;
			import java.util.List;
			public class E {
			    public void foo() {
			        List<List<Hashtable<Integer, HashSet<E>>>> l= new ArrayList<List<Hashtable<Integer, HashSet<E>>>>();\s
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testMissingAnnotationAttributes1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class E {
			    public @interface Annot {
			        public int foo();
			    }
			    @Annot()
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
			        public int foo();
			    }
			    @Annot(foo = 0)
			    public void foo() {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testMissingAnnotationAttributes2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class E {
			    public @interface Other {
			    }
			    public @interface Annot {
			        public Other[] foo();
			        public String hoo();
			    }
			    @Annot()
			    public void foo() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package pack;
			public class E {
			    public @interface Other {
			    }
			    public @interface Annot {
			        public Other[] foo();
			        public String hoo();
			    }
			    @Annot(foo = {@Other}, hoo = "")
			    public void foo() {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testMissingAnnotationAttributes3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class E {
			    public @interface Annot {
			        public int foo();
			        public String hoo() default "hello";
			    }
			    @Annot()
			    public void foo() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package pack;
			public class E {
			    public @interface Annot {
			        public int foo();
			        public String hoo() default "hello";
			    }
			    @Annot(foo = 0)
			    public void foo() {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testMissingAnnotationAttributes_bug179316 () throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("e", false, null);
		String str= """
			package e;
			@Requires1
			@interface Requires1 {
			        String value();
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Requires1.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package e;
			@Requires1(value = "")
			@interface Requires1 {
			        String value();
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}


	@Test
	public void testTypeParametersToRawTypeReference01() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			import java.util.List;
			public class E {
			    public void test() {
			        List l;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 5);

		String[] expected= new String[3];

		expected[0]= """
			package pack;
			import java.util.List;
			public class E {
			    public void test() {
			        @SuppressWarnings("rawtypes")
			        List l;
			    }
			}
			""";

		expected[1]= """
			package pack;
			import java.util.List;
			public class E {
			    @SuppressWarnings("rawtypes")
			    public void test() {
			        List l;
			    }
			}
			""";

		expected[2]= """
			package pack;
			import java.util.List;
			public class E {
			    public void test() {
			        List<?> l;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testTypeParametersToRawTypeReference02() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class E {
			    private class E1<P1, P2> {}
			    public void test() {
			        E1 e1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 5);

		String[] expected= new String[3];

		expected[0]= """
			package pack;
			public class E {
			    private class E1<P1, P2> {}
			    public void test() {
			        @SuppressWarnings("rawtypes")
			        E1 e1;
			    }
			}
			""";

		expected[1]= """
			package pack;
			public class E {
			    private class E1<P1, P2> {}
			    @SuppressWarnings("rawtypes")
			    public void test() {
			        E1 e1;
			    }
			}
			""";

		expected[2]= """
			package pack;
			public class E {
			    private class E1<P1, P2> {}
			    public void test() {
			        E1<?, ?> e1;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	//Disabled depends on bug Bug 124626 Infer Type Arguments infers ? instaed of more precise type
//	public void testTypeParametersToRawTypeReference03() throws Exception {
//		Hashtable options= JavaCore.getOptions();
//		options.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);
//		JavaCore.setOptions(options);
//
//		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
//		StringBuffer buf= new StringBuffer();
//		buf.append("package pack;\n");
//		buf.append("public class E {\n");
//		buf.append("    private class E1<P2 extends E2> {}\n");
//		buf.append("    private class E2 {}\n");
//		buf.append("    public void test() {\n");
//		buf.append("        E1 e1;\n");
//		buf.append("    }\n");
//		buf.append("}\n");
//		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
//
//		CompilationUnit astRoot= getASTRoot(cu);
//		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
//
//		assertCorrectLabels(proposals);
//		assertNumberOfProposals(proposals, 3);
//
//		String[] expected= new String[2];
//
//		buf= new StringBuffer();
//		buf.append("package pack;\n");
//		buf.append("public class E {\n");
//		buf.append("    private class E1<P2 extends E2> {}\n");
//		buf.append("    private class E2 {}\n");
//		buf.append("    @SuppressWarnings(\"unchecked\")\n");
//		buf.append("    public void test() {\n");
//		buf.append("        E1 e1;\n");
//		buf.append("    }\n");
//		buf.append("}\n");
//		expected[0]= buf.toString();
//
//		buf= new StringBuffer();
//		buf.append("package pack;\n");
//		buf.append("public class E {\n");
//		buf.append("    private class E1<P2 extends E2> {}\n");
//		buf.append("    private class E2 {}\n");
//		buf.append("    public void test() {\n");
//		buf.append("        E1<E2> e1;\n");
//		buf.append("    }\n");
//		buf.append("}\n");
//		expected[1]= buf.toString();
//
//		assertExpectedExistInProposals(proposals, expected);
//	}
//
//	public void testTypeParametersToRawTypeReference04() throws Exception {
//		Hashtable options= JavaCore.getOptions();
//		options.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);
//		JavaCore.setOptions(options);
//
//		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
//		StringBuffer buf= new StringBuffer();
//		buf.append("package pack;\n");
//		buf.append("public class E {\n");
//		buf.append("    private class E1<P2 extends E2<Integer>> {}\n");
//		buf.append("    private class E2<P1> {}\n");
//		buf.append("    public void test() {\n");
//		buf.append("        E1 e1;\n");
//		buf.append("    }\n");
//		buf.append("}\n");
//		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
//
//		CompilationUnit astRoot= getASTRoot(cu);
//		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
//
//		assertCorrectLabels(proposals);
//		assertNumberOfProposals(proposals, 3);
//
//		String[] expected= new String[2];
//
//		buf= new StringBuffer();
//		buf.append("package pack;\n");
//		buf.append("public class E {\n");
//		buf.append("    private class E1<P2 extends E2<Integer>> {}\n");
//		buf.append("    private class E2<P1> {}\n");
//		buf.append("    @SuppressWarnings(\"unchecked\")\n");
//		buf.append("    public void test() {\n");
//		buf.append("        E1 e1;\n");
//		buf.append("    }\n");
//		buf.append("}\n");
//		expected[0]= buf.toString();
//
//		buf= new StringBuffer();
//		buf.append("package pack;\n");
//		buf.append("public class E {\n");
//		buf.append("    private class E1<P2 extends E2<Integer>> {}\n");
//		buf.append("    private class E2<P1> {}\n");
//		buf.append("    public void test() {\n");
//		buf.append("        E1<E2<Integer>> e1;\n");
//		buf.append("    }\n");
//		buf.append("}\n");
//		expected[1]= buf.toString();
//
//		assertExpectedExistInProposals(proposals, expected);
//	}
//
//	public void testTypeParametersToRawTypeReference05() throws Exception {
//		Hashtable options= JavaCore.getOptions();
//		options.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);
//		JavaCore.setOptions(options);
//
//		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
//		StringBuffer buf= new StringBuffer();
//		buf.append("package pack;\n");
//		buf.append("import java.io.InputStream;\n");
//		buf.append("public class E2<P extends InputStream> {}\n");
//		pack1.createCompilationUnit("E2.java", buf.toString(), false, null);
//
//		buf= new StringBuffer();
//		buf.append("package pack;\n");
//		buf.append("public class E {\n");
//		buf.append("    public void test() {\n");
//		buf.append("        E2 e1;\n");
//		buf.append("    }\n");
//		buf.append("}\n");
//		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
//
//		CompilationUnit astRoot= getASTRoot(cu);
//		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
//
//		assertCorrectLabels(proposals);
//		assertNumberOfProposals(proposals, 3);
//
//		String[] expected= new String[2];
//
//		buf= new StringBuffer();
//		buf.append("package pack;\n");
//		buf.append("public class E {\n");
//		buf.append("    @SuppressWarnings(\"unchecked\")\n");
//		buf.append("    public void test() {\n");
//		buf.append("        E2 e1;\n");
//		buf.append("    }\n");
//		buf.append("}\n");
//		expected[0]= buf.toString();
//
//		buf= new StringBuffer();
//		buf.append("package pack;\n");
//		buf.append("\n");
//		buf.append("import java.io.InputStream;\n");
//		buf.append("\n");
//		buf.append("public class E {\n");
//		buf.append("    public void test() {\n");
//		buf.append("        E2<InputStream> e1;\n");
//		buf.append("    }\n");
//		buf.append("}\n");
//		expected[1]= buf.toString();
//
//		assertExpectedExistInProposals(proposals, expected);
//	}

	@Test
	public void testTypeParametersToRawTypeReference06() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			import java.util.ArrayList;
			import java.util.List;
			public class E {
			    private List l= new ArrayList<String>();
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 5);

		String[] expected= new String[3];

		expected[0]= """
			package pack;
			import java.util.ArrayList;
			import java.util.List;
			public class E {
			    @SuppressWarnings("rawtypes")
			    private List l= new ArrayList<String>();
			}
			""";

		expected[1]= """
			package pack;
			import java.util.ArrayList;
			import java.util.List;
			public class E {
			    private List<String> l= new ArrayList<String>();
			}
			""";

		expected[2]= """
			package pack;
			import java.util.ArrayList;
			import java.util.List;
			public class E {
			    private ArrayList<String> l= new ArrayList<String>();
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testTypeParametersToRawTypeReference07() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			import java.util.List;
			public class E {
			    private List l;
			    private void foo() {
			        l.add("String");
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[2];

		expected[0]= """
			package pack;
			import java.util.List;
			public class E {
			    @SuppressWarnings("rawtypes")
			    private List l;
			    private void foo() {
			        l.add("String");
			    }
			}
			""";

		expected[1]= """
			package pack;
			import java.util.List;
			public class E {
			    private List<String> l;
			    private void foo() {
			        l.add("String");
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testTypeParametersToRawTypeReference08() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class E {
			    private class E1<T> {
			        public void foo(T t) {
			            return;
			        }
			    }
			    private void foo(E1 e1) {
			        e1.foo("");
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[2];

		expected[0]= """
			package pack;
			public class E {
			    private class E1<T> {
			        public void foo(T t) {
			            return;
			        }
			    }
			    @SuppressWarnings("unchecked")
			    private void foo(E1 e1) {
			        e1.foo("");
			    }
			}
			""";

		expected[1]= """
			package pack;
			public class E {
			    private class E1<T> {
			        public void foo(T t) {
			            return;
			        }
			    }
			    private void foo(E1<String> e1) {
			        e1.foo("");
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testTypeParametersToRawTypeReference09() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			import java.util.ArrayList;
			import java.util.List;
			public class E {
			    private List<String> l= new ArrayList();
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[3];

		expected[0]= """
			package pack;
			import java.util.ArrayList;
			import java.util.List;
			public class E {
			    @SuppressWarnings("rawtypes")
			    private List<String> l= new ArrayList();
			}
			""";

		expected[1]= """
			package pack;
			import java.util.ArrayList;
			import java.util.List;
			public class E {
			    private List<String> l= new ArrayList<String>();
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testTypeParametersToRawTypeReferenceBug212557() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E1 {
			    public Class[] get() {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String[] expected= new String[1];
		expected[0]= """
			package test1;
			public class E1 {
			    @SuppressWarnings("rawtypes")
			    public Class[] get() {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testTypeParametersToRawTypeReferenceBug280193() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			import java.util.List;
			
			public class E1 {
			    public void foo(List<List> list) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String[] expected= new String[2];
		expected[0]= """
			package test1;
			import java.util.List;
			
			public class E1 {
			    public void foo(@SuppressWarnings("rawtypes") List<List> list) {
			    }
			}
			""";

		expected[1]= """
			package test1;
			import java.util.List;
			
			public class E1 {
			    @SuppressWarnings("rawtypes")
			    public void foo(List<List> list) {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testSwitchCaseFallThrough1() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_FALLTHROUGH_CASE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class E {
			    public long foo(int i) {
			        long time= 0;
			        switch (i) {
			            case 1:
			                time= System.currentTimeMillis();
			            case 2:
			                time= 3;
			        }
			        return time;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[3];
		expected[0]= """
			package pack;
			public class E {
			    public long foo(int i) {
			        long time= 0;
			        switch (i) {
			            case 1:
			                time= System.currentTimeMillis();
			                break;
			            case 2:
			                time= 3;
			        }
			        return time;
			    }
			}
			""";

		expected[1]= """
			package pack;
			public class E {
			    @SuppressWarnings("fallthrough")
			    public long foo(int i) {
			        long time= 0;
			        switch (i) {
			            case 1:
			                time= System.currentTimeMillis();
			            case 2:
			                time= 3;
			        }
			        return time;
			    }
			}
			""";

		expected[2]= """
			package pack;
			public class E {
			    public long foo(int i) {
			        long time= 0;
			        switch (i) {
			            case 1:
			                time= System.currentTimeMillis();
			                //$FALL-THROUGH$
			            case 2:
			                time= 3;
			        }
			        return time;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testSwitchCaseFallThrough2() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_FALLTHROUGH_CASE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class E {
			    public long foo(int i) {
			        long time= 0;
			        switch (i) {
			            case 1:
			                time= System.currentTimeMillis();
			            default:
			                time= 3;
			        }
			        return time;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[3];
		expected[0]= """
			package pack;
			public class E {
			    public long foo(int i) {
			        long time= 0;
			        switch (i) {
			            case 1:
			                time= System.currentTimeMillis();
			                break;
			            default:
			                time= 3;
			        }
			        return time;
			    }
			}
			""";

		expected[1]= """
			package pack;
			public class E {
			    @SuppressWarnings("fallthrough")
			    public long foo(int i) {
			        long time= 0;
			        switch (i) {
			            case 1:
			                time= System.currentTimeMillis();
			            default:
			                time= 3;
			        }
			        return time;
			    }
			}
			""";

		expected[2]= """
			package pack;
			public class E {
			    public long foo(int i) {
			        long time= 0;
			        switch (i) {
			            case 1:
			                time= System.currentTimeMillis();
			                //$FALL-THROUGH$
			            default:
			                time= 3;
			        }
			        return time;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testSwitchCaseFallThrough3() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_FALLTHROUGH_CASE, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class E {
			    public long foo(int i) {
			        long time= 0;
			        switch (i) {
			            case 1:
			                time= System.currentTimeMillis();
			                // fall through is OK
			            default:
			                time= 3;
			        }
			        return time;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[3];
		expected[0]= """
			package pack;
			public class E {
			    public long foo(int i) {
			        long time= 0;
			        switch (i) {
			            case 1:
			                time= System.currentTimeMillis();
			                break;
			            // fall through is OK
			            default:
			                time= 3;
			        }
			        return time;
			    }
			}
			""";

		expected[1]= """
			package pack;
			public class E {
			    @SuppressWarnings("fallthrough")
			    public long foo(int i) {
			        long time= 0;
			        switch (i) {
			            case 1:
			                time= System.currentTimeMillis();
			                // fall through is OK
			            default:
			                time= 3;
			        }
			        return time;
			    }
			}
			""";

		expected[2]= """
			package pack;
			public class E {
			    public long foo(int i) {
			        long time= 0;
			        switch (i) {
			            case 1:
			                time= System.currentTimeMillis();
			                // fall through is OK
			                //$FALL-THROUGH$
			            default:
			                time= 3;
			        }
			        return time;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testReplaceWithUnqualifiedEnumConstant1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class E {
			    public enum color {black, white}
			    public void foo(color c) {
					switch (c) {
			            case color.black:
			                System.out.println("Black");
			                break;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2, 1);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package pack;
			public class E {
			    public enum color {black, white}
			    public void foo(color c) {
					switch (c) {
			            case black:
			                System.out.println("Black");
			                break;
			        }
			    }
			}
			""";
		assertEqualString(preview, str1);
		String expected= str1;
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testReplaceWithUnqualifiedEnumConstant2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class E {
			    public enum color {black, white}
			    public void foo(color c) {
					switch (c) {
			            case (color.black):
			                System.out.println("Black");
			                break;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 3, 2);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package pack;
			public class E {
			    public enum color {black, white}
			    public void foo(color c) {
					switch (c) {
			            case black:
			                System.out.println("Black");
			                break;
			        }
			    }
			}
			""";
		assertEqualString(preview, str1);
		String expected= str1;
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testCollectionsFieldMethodReplacement() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_TYPE_PARAMETER_HIDING, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("b112441", false, null);
		String str= """
			package b112441;
			
			import java.util.Collections;
			import java.util.Map;
			
			public class CollectionsTest {
			    Map<String,String> m=Collections.EMPTY_MAP;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("CollectionsTest.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[1];
		expected[0]= """
			package b112441;
			
			import java.util.Collections;
			import java.util.Map;
			
			public class CollectionsTest {
			    Map<String,String> m=Collections.emptyMap();
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testCollectionsFieldMethodReplacement2() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_TYPE_PARAMETER_HIDING, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			import java.util.Collections;
			import java.util.Map;
			
			public class CollectionsTest {
			    public void foo(Map<Object, Integer> map) { };
			    {
			        foo(Collections.EMPTY_MAP);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("CollectionsTest.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[1];
		expected[0]= """
			package p;
			
			import java.util.Collections;
			import java.util.Map;
			
			public class CollectionsTest {
			    public void foo(Map<Object, Integer> map) { };
			    {
			        foo(Collections.<Object, Integer>emptyMap());
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testCollectionsFieldMethodReplacement3() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_TYPE_PARAMETER_HIDING, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			import java.util.*;
			
			public class CollectionsTest {
			    public void foo(Map<Date, Integer> map) { };
			    {
			        foo(Collections.EMPTY_MAP);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("CollectionsTest.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[1];
		expected[0]= """
			package p;
			
			import java.util.*;
			
			public class CollectionsTest {
			    public void foo(Map<Date, Integer> map) { };
			    {
			        foo(Collections.<Date, Integer>emptyMap());
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testMissingEnumConstantsInCase1() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_INCOMPLETE_ENUM_SWITCH, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_SUPPRESS_WARNINGS, JavaCore.DISABLED);

		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			public class E {
			    enum MyEnum {
			        X1, X2, X3
			    }
			   \s
			    public void foo(MyEnum x) {
			        switch (x) {
			       \s
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 3);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		String[] expected= new String[2];
		expected[0]= """
			package p;
			
			public class E {
			    enum MyEnum {
			        X1, X2, X3
			    }
			   \s
			    public void foo(MyEnum x) {
			        switch (x) {
			            default :
			                break;
			       \s
			        }
			    }
			}
			""";

		expected[1]= """
			package p;
			
			public class E {
			    enum MyEnum {
			        X1, X2, X3
			    }
			   \s
			    public void foo(MyEnum x) {
			        switch (x) {
			            case X1 :
			                break;
			            case X2 :
			                break;
			            case X3 :
			                break;
			            default :
			                break;
			       \s
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testMissingEnumConstantsInCase2() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_INCOMPLETE_ENUM_SWITCH, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_SUPPRESS_WARNINGS, JavaCore.DISABLED);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			public class E {
			    enum MyEnum {
			        X1, X2, X3
			    }
			   \s
			    public void foo(MyEnum x) {
			        switch (x) {
			            case X1 :
			                break;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		String[] expected= new String[2];
		expected[0]= """
			package p;
			
			public class E {
			    enum MyEnum {
			        X1, X2, X3
			    }
			   \s
			    public void foo(MyEnum x) {
			        switch (x) {
			            case X1 :
			                break;
			            default :
			                break;
			        }
			    }
			}
			""";

		expected[1]= """
			package p;
			
			public class E {
			    enum MyEnum {
			        X1, X2, X3
			    }
			   \s
			    public void foo(MyEnum x) {
			        switch (x) {
			            case X1 :
			                break;
			            case X2 :
			                break;
			            case X3 :
			                break;
			            default :
			                break;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testMissingEnumConstantsInCase3() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=372840
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_INCOMPLETE_ENUM_SWITCH, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_SWITCH_MISSING_DEFAULT_CASE, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_SUPPRESS_WARNINGS, JavaCore.DISABLED);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			public class E {
			    enum MyEnum {
			        X1, X2, X3
			    }
			   \s
			    public void foo(MyEnum x) {
			        switch (x) {
			            case X1 :
			                break;
			            case X2 :
			                break;
			            case X3 :
			                break;
			        }
			    }
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
			    enum MyEnum {
			        X1, X2, X3
			    }
			   \s
			    public void foo(MyEnum x) {
			        switch (x) {
			            case X1 :
			                break;
			            case X2 :
			                break;
			            case X3 :
			                break;
			            default :
			                break;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testMissingEnumConstantsInCase4() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=379086
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_INCOMPLETE_ENUM_SWITCH, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_SUPPRESS_WARNINGS, JavaCore.DISABLED);

		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			public class E {
			    enum MyEnum {
			        X1, X2, X3
			    }
			   \s
			    public void foo() {
			        switch (bar()) {
			       \s
			        }
			    }
			    public MyEnum bar() {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 3);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		String[] expected= new String[2];
		expected[0]= """
			package p;
			
			public class E {
			    enum MyEnum {
			        X1, X2, X3
			    }
			   \s
			    public void foo() {
			        switch (bar()) {
			            default :
			                break;
			       \s
			        }
			    }
			    public MyEnum bar() {
			        return null;
			    }
			}
			""";

		expected[1]= """
			package p;
			
			public class E {
			    enum MyEnum {
			        X1, X2, X3
			    }
			   \s
			    public void foo() {
			        switch (bar()) {
			            case X1 :
			                break;
			            case X2 :
			                break;
			            case X3 :
			                break;
			            default :
			                break;
			       \s
			        }
			    }
			    public MyEnum bar() {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testMissingHashCode1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			public class E {
			    private int fField;
			
			    public boolean equals(Object o) {
			        return true;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		String[] expected= new String[1];
		expected[0]= """
			package p;
			
			public class E {
			    private int fField;
			
			    public boolean equals(Object o) {
			        return true;
			    }
			
			    @Override
			    public int hashCode() {
			        return super.hashCode();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testMissingHashCode2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			public class E extends java.io.File{
			    private static final long serialVersionUID= 1L;
			    public E() { super("x"); }
			    public boolean equals(Object o) {
			        return o instanceof E && super.equals(o);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		assertEquals(0, astRoot.getProblems().length); // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38751#c7
	}

	@Test
	public void testUnusedTypeParameter1() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_TYPE_PARAMETER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public interface E<T extends Exception> {
			    public void foo(Object str);
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
			public interface E {
			    public void foo(Object str);
			}
			""";

		expected[1]= """
			package test1;
			/**
			 * @param <T> \s
			 */
			public interface E<T extends Exception> {
			    public void foo(Object str);
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedTypeParameter2() throws Exception {

		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_TYPE_PARAMETER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			    /**
			     * @param <X>\s
			     * @see E
			     */
			public interface E<X, T> {
			    public void foo(Object str);
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
			    /**
			     * @param <X>\s
			     * @see E
			     */
			public interface E<X> {
			    public void foo(Object str);
			}
			""";

		expected[1]= """
			package test1;
			    /**
			     * @param <X>\s
			     * @param <T>\s
			     * @see E
			     */
			public interface E<X, T> {
			    public void foo(Object str);
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnusedTypeParameter3() throws Exception {
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_TYPE_PARAMETER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public <T> void foo(Object str){}
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
			    public void foo(Object str){}
			}
			""";

		expected[1]= """
			package test1;
			public class E {
			    /**
			     * @param <T> \s
			     */
			    public <T> void foo(Object str){}
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	/**
	 * Tests if the quick fix to loop over a variable name is added correctly. The complete
	 * functionality of the for loop generation is tested in {@link AssistQuickFixTest}
	 */
	@Ignore("Bug 434188: [quick fix] shows sign of quick fix, but says no suggestions available.")
	@Test
	public void testLoopOverAddedToFixesForVariable() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Collection;
			public class E {
			    void foo(Collection<String> collection) {
			        collection
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		Map<String, String> saveOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>(saveOptions);
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, "true");
		try {
			fJProject1.setOptions(newOptions);
			List<IJavaCompletionProposal> proposals= collectCorrections(cu, getASTRoot(cu), 3, null);

			assertNumberOfProposals(proposals, 2);
			assertCorrectLabels(proposals);

			String[] expected= new String[2];
			expected[0]= """
				package test1;
				import java.util.Collection;
				public class E {
				    void foo(Collection<String> collection) {
				        for (String string : collection) {
				           \s
				        }
				    }
				}
				""";

			expected[1]= """
				package test1;
				import java.util.Collection;
				import java.util.Iterator;
				public class E {
				    void foo(Collection<String> collection) {
				        for (Iterator<String> iterator = collection.iterator(); iterator.hasNext();) {
				            String string = iterator.next();
				           \s
				        }
				    }
				}
				""";

			assertExpectedExistInProposals(proposals, expected);
		} finally {
			fJProject1.setOptions(saveOptions);
		}
	}

	/**
	 * Tests if the quick fix to loop over a method invocation is added correctly. The complete
	 * functionality of the for loop generation is tested in {@link AssistQuickFixTest}
	 */
	@Ignore("Bug 434188: [quick fix] shows sign of quick fix, but says no suggestions available.")
	@Test
	public void testLoopOverAddedToFixesForMethodInvocation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Map;
			public class E {
			    void foo(Map<String, String> map) {
			        map.keySet()
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		Map<String, String> saveOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>(saveOptions);
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, "true");
		try {
			fJProject1.setOptions(newOptions);
			List<IJavaCompletionProposal> proposals= collectCorrections(cu, getASTRoot(cu));

			assertNumberOfProposals(proposals, 2);
			assertCorrectLabels(proposals);

			String[] expected= new String[2];
			expected[0]= """
				package test1;
				import java.util.Map;
				public class E {
				    void foo(Map<String, String> map) {
				        for (String string : map.keySet()) {
				           \s
				        }
				    }
				}
				""";

			expected[1]= """
				package test1;
				import java.util.Iterator;
				import java.util.Map;
				public class E {
				    void foo(Map<String, String> map) {
				        for (Iterator<String> iterator = map.keySet().iterator(); iterator
				                .hasNext();) {
				            String string = iterator.next();
				           \s
				        }
				    }
				}
				""";

			assertExpectedExistInProposals(proposals, expected);
		} finally {
			fJProject1.setOptions(saveOptions);
		}
	}

	/**
	 * Tests if the quick fix to loop over a method invocation is added correctly. The complete
	 * functionality of the for loop generation is tested in {@link AssistQuickFixTest}
	 */
	@Ignore("Bug 434188: [quick fix] shows sign of quick fix, but says no suggestions available.")
	@Test
	public void testGenerateForeachNotAddedForLowVersion() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Collection;
			public class E {
			    void foo(Collection collection) {
			        collection
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		Map<String, String> saveOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_4, newOptions);
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, "true");
		try {
			fJProject1.setOptions(newOptions);
			List<IJavaCompletionProposal> proposals= collectCorrections(cu, getASTRoot(cu), 3, null);

			assertNumberOfProposals(proposals, 1);
			assertCorrectLabels(proposals);
			assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_generate_enhanced_for_loop);

			String[] expected= new String[1];

			// no generics should be added to iterator since the version is too low
			String str1= """
				package test1;
				import java.util.Collection;
				import java.util.Iterator;
				public class E {
				    void foo(Collection collection) {
				        for (Iterator iterator = collection.iterator(); iterator.hasNext();) {
				            Object object = iterator.next();
				           \s
				        }
				    }
				}
				""";
			expected[0]= str1;

			assertExpectedExistInProposals(proposals, expected);
		} finally {
			fJProject1.setOptions(saveOptions);
		}
	}

	@Test
	public void testInsertInferredTypeArguments() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.ArrayList;
			import java.util.List;
			
			public class E {
			
			    private void foo() {
			        List<String> al1 = new ArrayList<>();
			
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections2(cu, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import java.util.ArrayList;
			import java.util.List;
			
			public class E {
			
			    private void foo() {
			        List<String> al1 = new ArrayList<String>();
			
			    }
			}
			""";
		assertEqualString(preview, str1);

		String expected1= str1;
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	// regression test for https://bugs.eclipse.org/434188 - [quick fix] shows sign of quick fix, but says no suggestions available.
	@Test
	public void testNoFixFor_ParsingErrorInsertToComplete() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.*;
			class E {
			    public class MyLayout {
			        int indent;
			    }
			    public void foo() {
			        new MyLayout().indent // no real quick fix
			    }
			   \s
			    private int[] fField;
			    public void bar() {
			        fField[0] // no quick fix
			    }
			    public void baz() {
			        try { // no quick fix
			        }
			    }
			    void foo(Map<String, String> map) {
			        map..keySet(); // no quick fix
			    }
			    void // no quick fix
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		List<ICompletionProposal> proposals= collectAllCorrections(cu, astRoot, 9);
		assertNumberOfProposals(proposals, 0);

		IProblem[] problems= astRoot.getProblems();
		for (int i= 0; i < problems.length; i++) {
			IProblem problem= problems[i];
			if (i == 5) {
				assertEquals(IProblem.ParsingErrorDeleteToken, problem.getID());
			} else {
				assertEquals(IProblem.ParsingErrorInsertToComplete, problem.getID());
			}
		}
		assertFalse("IProblem.ParsingErrorInsertToComplete is very general and should not trigger the quick fix lightbulb everywhere",
				JavaCorrectionProcessor.hasCorrections(cu, IProblem.ParsingErrorInsertToComplete, IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER));
	}

	@Test
	public void testConvertLambdaToAnonymous() throws Exception {
		assertFalse("error should not appear in 1.8 or higher", JavaModelUtil.is1d8OrHigher(fJProject1));

		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			public class Lambda {
			    Runnable r= () -> { System.out.println(Lambda.this.r); };
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Lambda.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		expected[0]= """
			package p;
			
			public class Lambda {
			    Runnable r= new Runnable() {
			        public void run() { System.out.println(Lambda.this.r); }
			    };
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testCreateNewObjectProposal_1() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=395216
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F {
			    public void foo(String s) {
			    }
			}
			""";
		pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E extends F {
			    public static void main(String[] args) {
			        F.foo("");
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String[] expected= new String[2];
		expected[0]= """
			package test1;
			public class E extends F {
			    public static void main(String[] args) {
			        F f = new F();
			        f.foo("");
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class F {
			    public static void foo(String s) {
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testCreateNewObjectProposal_2() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=395216
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F {
			    public F(int i, String s) {
			    }
			    public void foo(String s) {
			    }
			}
			""";
		pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E extends F {
			    public E() {
			        super(0, "");
			    }
			    public static void main(String[] args) {
			        F.foo("");
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String[] expected= new String[2];
		expected[0]= """
			package test1;
			public class E extends F {
			    public E() {
			        super(0, "");
			    }
			    public static void main(String[] args) {
			        F f = new F(0, null);
			        f.foo("");
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class F {
			    public F(int i, String s) {
			    }
			    public static void foo(String s) {
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testCreateNewObjectProposal_3() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=395216
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F {
			    public F(int i, String s) {
			    }
			    public String s;
			}
			""";
		pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E extends F {
			    public E() {
			        super(0, "");
			    }
			    public static void main(String[] args) {
			        String s1 = F.s;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String[] expected= new String[3];
		expected[0]= """
			package test1;
			public class E extends F {
			    public E() {
			        super(0, "");
			    }
			    public static void main(String[] args) {
			        String s1 = new String();
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class F {
			    public F(int i, String s) {
			    }
			    public static String s;
			}
			""";

		expected[2]= """
			package test1;
			public class E extends F {
			    public E() {
			        super(0, "");
			    }
			    public static void main(String[] args) {
			        F f = new F(0, null);
			        String s1 = f.s;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testCreateNewObjectProposal_4() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=395216
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F {
			    public F(int i, String s) {
			    }
			    public F f;
			}
			""";
		pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E extends F {
			    public E() {
			        super(0, "");
			    }
			    public static void main(String[] args) {
			        F f1 = F.f;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String[] expected= new String[3];
		expected[0]= """
			package test1;
			public class E extends F {
			    public E() {
			        super(0, "");
			    }
			    public static void main(String[] args) {
			        F f1 = new F(0, null);
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class F {
			    public F(int i, String s) {
			    }
			    public static F f;
			}
			""";

		expected[2]= """
			package test1;
			public class E extends F {
			    public E() {
			        super(0, "");
			    }
			    public static void main(String[] args) {
			        F f2 = new F(0, null);
			        F f1 = f2.f;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testCreateNewObjectProposal_5() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=395216
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E<T> {
			    public E<String> e;
			    public static void main(String[] args) {
			        E<String> e1 = E.e;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String[] expected= new String[3];
		expected[0]= """
			package test1;
			public class E<T> {
			    public E<String> e;
			    public static void main(String[] args) {
			        E<String> e1 = new E<String>();
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E<T> {
			    public static E<String> e;
			    public static void main(String[] args) {
			        E<String> e1 = E.e;
			    }
			}
			""";

		expected[2]= """
			package test1;
			public class E<T> {
			    public E<String> e;
			    public static void main(String[] args) {
			        E e2 = new E();
			        E<String> e1 = e2.e;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testCreateNewObjectProposal_6() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=395216
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E<T> {
			    public E<String> e;
			    public static void main(String[] args) {
			        E<String> e1 = E.e;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String[] expected= new String[3];
		expected[0]= """
			package test1;
			public class E<T> {
			    public E<String> e;
			    public static void main(String[] args) {
			        E<String> e1 = new E<String>();
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E<T> {
			    public static E<String> e;
			    public static void main(String[] args) {
			        E<String> e1 = E.e;
			    }
			}
			""";

		expected[2]= """
			package test1;
			public class E<T> {
			    public E<String> e;
			    public static void main(String[] args) {
			        E e2 = new E();
			        E<String> e1 = e2.e;
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testCreateNewObjectReferenceProposal_1() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=395216
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F {
			    public void foo(String s) {
			    }
			}
			""";
		pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E extends F {
			    public static void main(String[] args) {
			        F f2 = new F();
			        F.foo("");
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String[] expected= new String[3];
		expected[0]= """
			package test1;
			public class E extends F {
			    public static void main(String[] args) {
			        F f2 = new F();
			        f2.foo("");
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E extends F {
			    public static void main(String[] args) {
			        F f2 = new F();
			        F f = new F();
			        f.foo("");
			    }
			}
			""";

		expected[2]= """
			package test1;
			public class F {
			    public static void foo(String s) {
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testCreateNewObjectReferenceProposal_2() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=395216
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F {
			    public void foo(String s) {
			    }
			}
			""";
		pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E extends F {
			    static F f2 = new F();
			    public static void main(String[] args) {
			        F.foo("");
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String[] expected= new String[3];
		expected[0]= """
			package test1;
			public class E extends F {
			    static F f2 = new F();
			    public static void main(String[] args) {
			        f2.foo("");
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E extends F {
			    static F f2 = new F();
			    public static void main(String[] args) {
			        F f = new F();
			        f.foo("");
			    }
			}
			""";

		expected[2]= """
			package test1;
			public class F {
			    public static void foo(String s) {
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testCreateNewObjectReferenceProposal_3() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=395216
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F {
			    public String fld;
			}
			""";
		pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E extends F {
			    public static void main(String[] args) {
			        F f2 = new F();
			        String x = F.fld;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String[] expected= new String[4];
		expected[0]= """
			package test1;
			public class E extends F {
			    public static void main(String[] args) {
			        F f2 = new F();
			        String x = f2.fld;
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E extends F {
			    public static void main(String[] args) {
			        F f2 = new F();
			        F f = new F();
			        String x = f.fld;
			    }
			}
			""";

		expected[2]= """
			package test1;
			public class E extends F {
			    public static void main(String[] args) {
			        F f2 = new F();
			        String x = new String();
			    }
			}
			""";

		expected[3]= """
			package test1;
			public class F {
			    public static String fld;
			}
			""";
		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testCreateNewObjectReferenceProposal_4() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=395216
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F {
			    public String fld;
			}
			""";
		pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E extends F {
			    static F f2 = new F();
			    public static void main(String[] args) {
			        String x = F.fld;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String[] expected= new String[4];
		expected[0]= """
			package test1;
			public class E extends F {
			    static F f2 = new F();
			    public static void main(String[] args) {
			        String x = f2.fld;
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E extends F {
			    static F f2 = new F();
			    public static void main(String[] args) {
			        F f = new F();
			        String x = f.fld;
			    }
			}
			""";

		expected[2]= """
			package test1;
			public class E extends F {
			    static F f2 = new F();
			    public static void main(String[] args) {
			        String x = new String();
			    }
			}
			""";

		expected[3]= """
			package test1;
			public class F {
			    public static String fld;
			}
			""";
		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testCreateNewVariableReferenceProposal_1() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=395216
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F {
			    public String s;
			    public void foo(String s) {
			    }
			}
			""";
		pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E extends F {
			    static String s2;
			    public static void main(String[] args) {
			        String s = F.s;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String[] expected= new String[4];
		expected[0]= """
			package test1;
			public class E extends F {
			    static String s2;
			    public static void main(String[] args) {
			        String s = s2;
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E extends F {
			    static String s2;
			    public static void main(String[] args) {
			        String s = new String();
			    }
			}
			""";

		expected[2]= """
			package test1;
			public class F {
			    public static String s;
			    public void foo(String s) {
			    }
			}
			""";

		expected[3]= """
			package test1;
			public class E extends F {
			    static String s2;
			    public static void main(String[] args) {
			        F f = new F();
			        String s = f.s;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testCreateNewVariableReferenceProposal_2() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=395216
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F {
			    public String s;
			    public void foo(String s) {
			    }
			}
			""";
		pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E extends F {
			    static String s2;
			    public static void main(String[] args) {
			        String s1;
			        String s = F.s;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String[] expected= new String[4];
		expected[0]= """
			package test1;
			public class E extends F {
			    static String s2;
			    public static void main(String[] args) {
			        String s1;
			        String s = s1;
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E extends F {
			    static String s2;
			    public static void main(String[] args) {
			        String s1;
			        String s = new String();
			    }
			}
			""";

		expected[2]= """
			package test1;
			public class F {
			    public static String s;
			    public void foo(String s) {
			    }
			}
			""";

		expected[3]= """
			package test1;
			public class E extends F {
			    static String s2;
			    public static void main(String[] args) {
			        String s1;
			        F f = new F();
			        String s = f.s;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}


	@Test
	public void testCreateNewVariableReferenceProposal_3() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=395216
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F {
			    public F f1;
			}
			""";
		pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E extends F {
			    public static void main(String[] args) {
			        F f2 = null, f3 = F.f1, f4 = null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String[] expected= new String[4];
		expected[0]= """
			package test1;
			public class E extends F {
			    public static void main(String[] args) {
			        F f2 = null, f3 = f2, f4 = null;
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E extends F {
			    public static void main(String[] args) {
			        F f2 = null, f3 = new F(), f4 = null;
			    }
			}
			""";

		expected[2]= """
			package test1;
			public class F {
			    public static F f1;
			}
			""";

		expected[3]= """
			package test1;
			public class E extends F {
			    public static void main(String[] args) {
			        F f = new F();
			        F f2 = null, f3 = f.f1, f4 = null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testDuplicateConstructor() throws Exception { //https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/1123
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public E() {
			        System.out.println("first");
			    }
			    public E() {
			        System.out.println("second");
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String[] expected= new String[1];

		expected[0]= """
			package test1;
			public class E {
			    public E() {
			        System.out.println("second");
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}
}

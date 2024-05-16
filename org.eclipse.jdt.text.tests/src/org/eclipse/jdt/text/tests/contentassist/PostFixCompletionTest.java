/*******************************************************************************
 * Copyright (c) 2019, 2020 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat - Initial Contribution
 *******************************************************************************/
package org.eclipse.jdt.text.tests.contentassist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.swt.SWT;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.text.java.PostfixCompletionProposalComputer;
import org.eclipse.jdt.internal.ui.text.template.contentassist.PostfixTemplateProposal;

public class PostFixCompletionTest {
	private IJavaProject fJProject;

	private IPackageFragmentRoot javaSrc;

	private IPackageFragment pkg;

	private Hashtable<String, String> savedOptions;

	@Rule
	public ProjectTestSetup cts= new ProjectTestSetup();

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= JavaCore.getDefaultOptions();
		savedOptions= new Hashtable<>(options);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "2");
		JavaCore.setOptions(options);

		fJProject= JavaProjectHelper.createJavaProject("TestProject", "bin");
		JavaProjectHelper.addRTJar18(fJProject);
		javaSrc= JavaProjectHelper.addSourceContainer(fJProject, "src");
		pkg= javaSrc.createPackageFragment("test", false, null);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject);
		JavaCore.setOptions(savedOptions);
	}

	@Test
	public void testStringVar() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			public class StringVar {
			  public void test () {
			    "Some String Value".var$
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "StringVar.java");
		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertProposalsExist(Arrays.asList("var - Creates a new variable"), proposals);

		ITextViewer viewer= initializeViewer(cu);
		applyProposal(viewer, proposals, "var", completionIndex);

		String str= """
			package test;
			public class StringVar {
			  public void test () {
			    String string = "Some String Value";
			  }
			}""";
		assertEquals(str, viewer.getDocument().get());
	}

	@Test
	public void testStringVar2() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			
			public class StringVar2 {
			  public void test () {
			    "foo".$
			    if (true);
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "StringVar2.java");
		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertProposalsExist(Arrays.asList("var - Creates a new variable"), proposals);

		ITextViewer viewer= initializeViewer(cu);
		applyProposal(viewer, proposals, "var", completionIndex);

		String str= """
			package test;
			
			public class StringVar2 {
			  public void test () {
			    String string = "foo";
			    if (true);
			  }
			}""";
		assertEquals(str, viewer.getDocument().get());
	}

	@Test
	public void testIntegerVar() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			public class IntegerVar {
			  public void test () {
			    new Integer(0).var$
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "IntegerVar.java");
		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertProposalsExist(Arrays.asList("var - Creates a new variable"), proposals);

		ITextViewer viewer= initializeViewer(cu);
		applyProposal(viewer, proposals, "var", completionIndex);

		String str= """
			package test;
			public class IntegerVar {
			  public void test () {
			    Integer integer = new Integer(0);
			  }
			}""";
		assertEquals(str, viewer.getDocument().get());
	}

	@Test
	public void testBooleanVar() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			public class BooleanVar {
			  public void test () {
			    false.var$
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "BooleanVar.java");
		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertProposalsExist(Arrays.asList("var - Creates a new variable"), proposals);

		ITextViewer viewer= initializeViewer(cu);
		applyProposal(viewer, proposals, "var", completionIndex);

		String str= """
			package test;
			public class BooleanVar {
			  public void test () {
			    boolean b = false;
			  }
			}""";
		assertEquals(str, viewer.getDocument().get());
	}

	@Test
	public void testIntVar() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			public class IntVar {
			  public void test () {
			    (2 + 2).var$
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "IntVar.java");
		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertProposalsExist(Arrays.asList("var - Creates a new variable"), proposals);

		ITextViewer viewer= initializeViewer(cu);
		applyProposal(viewer, proposals, "var", completionIndex);

		String str= """
			package test;
			public class IntVar {
			  public void test () {
			    int name = (2 + 2);
			  }
			}""";
		assertEquals(str, viewer.getDocument().get());
	}

	@Test
	public void testStringConcatVar() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			public class StringConcatVar {
			  public void test () {
			    ("two" + 2).var$
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "StringConcatVar.java");
		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertProposalsExist(Arrays.asList("var - Creates a new variable"), proposals);

		ITextViewer viewer= initializeViewer(cu);
		applyProposal(viewer, proposals, "var", completionIndex);

		String str= """
			package test;
			public class StringConcatVar {
			  public void test () {
			    String string = ("two" + 2);
			  }
			}""";
		assertEquals(str, viewer.getDocument().get());
	}

	@Test
	public void testStringConcatVar2() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			public class testStringConcatVar2 {
			  public void test () {
			    (((("two" + 2)))).var$
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "testStringConcatVar2.java");
		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertProposalsExist(Arrays.asList("var - Creates a new variable"), proposals);

		ITextViewer viewer= initializeViewer(cu);
		applyProposal(viewer, proposals, "var", completionIndex);

		String str= """
			package test;
			public class testStringConcatVar2 {
			  public void test () {
			    String string = (((("two" + 2))));
			  }
			}""";
		assertEquals(str, viewer.getDocument().get());
	}

	@Test
	public void testArrayVar() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			public class ArrayVar {
			  public void test () {
			    new byte[] { 0, 1, 3 }.var$
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "ArrayVar.java");
		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertProposalsExist(Arrays.asList("var - Creates a new variable"), proposals);

		ITextViewer viewer= initializeViewer(cu);
		applyProposal(viewer, proposals, "var", completionIndex);

		String str= """
			package test;
			public class ArrayVar {
			  public void test () {
			    byte[] name = new byte[] { 0, 1, 3 };
			  }
			}""";
		assertEquals(str, viewer.getDocument().get());
	}

	@Test
	public void testArrayAccessVar() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			public class ArrayAccessVar {
			  public void test () {
			    String [] args = new String [] { "one", "two" };
			    args[0].var$
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "ArrayAccessVar.java");
		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertProposalsExist(Arrays.asList("var - Creates a new variable"), proposals);

		ITextViewer viewer= initializeViewer(cu);
		applyProposal(viewer, proposals, "var", completionIndex);

		String str= """
			package test;
			public class ArrayAccessVar {
			  public void test () {
			    String [] args = new String [] { "one", "two" };
			    String string = args[0];
			  }
			}""";
		assertEquals(str, viewer.getDocument().get());
	}

	@Test
	public void testBoundedExtendsTypeParameterVar() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			import java.util.List;
			public class BoundedExtendsTypeParameterVar {
			  public void test () {
			    List<? extends Number> x = null;
			    x.get(0).var$
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "BoundedExtendsTypeParameterVar.java");
		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertProposalsExist(Arrays.asList("var - Creates a new variable"), proposals);

		ITextViewer viewer= initializeViewer(cu);
		applyProposal(viewer, proposals, "var", completionIndex);

		String str= """
			package test;
			import java.util.List;
			public class BoundedExtendsTypeParameterVar {
			  public void test () {
			    List<? extends Number> x = null;
			    Number number = x.get(0);
			  }
			}""";
		assertEquals(str, viewer.getDocument().get());
	}

	@Test
	public void testBoundedSuperTypeParameterVar() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			import java.util.List;
			public class testBoundedSuperTypeParameterVar {
			  public void test () {
			    List<? super Number> x = null;
			    x.get(0).var$
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "testBoundedSuperTypeParameterVar.java");
		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertProposalsExist(Arrays.asList("var - Creates a new variable"), proposals);

		ITextViewer viewer= initializeViewer(cu);
		applyProposal(viewer, proposals, "var", completionIndex);

		String str= """
			package test;
			import java.util.List;
			public class testBoundedSuperTypeParameterVar {
			  public void test () {
			    List<? super Number> x = null;
			    Object number = x.get(0);
			  }
			}""";
		assertEquals(str, viewer.getDocument().get());
	}

	@Test
	public void testVarForMethodInvocation() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			import java.util.Arrays;
			import java.util.List;
			public class VarForMethodInvocation {
			  public void test () {
			    List<String> res = Arrays.asList("a", "b");
			    res.get(0).isEmpty().var$
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "VarForMethodInvocation.java");
		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertProposalsExist(Arrays.asList("var - Creates a new variable"), proposals);

		ITextViewer viewer= initializeViewer(cu);
		applyProposal(viewer, proposals, "var", completionIndex);

		String str= """
			package test;
			import java.util.Arrays;
			import java.util.List;
			public class VarForMethodInvocation {
			  public void test () {
			    List<String> res = Arrays.asList("a", "b");
			    boolean empty = res.get(0).isEmpty();
			  }
			}""";
		assertEquals(str, viewer.getDocument().get());
	}

	@Test
	public void testVarForMethodInvocation2() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			public class VarForMethodInvocation2 {
			  public void test () {
			    String s = "5";
			    Integer.valueOf(s).var$
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "VarForMethodInvocation2.java");
		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertProposalsExist(Arrays.asList("var - Creates a new variable"), proposals);

		ITextViewer viewer= initializeViewer(cu);
		applyProposal(viewer, proposals, "var", completionIndex);

		String str= """
			package test;
			public class VarForMethodInvocation2 {
			  public void test () {
			    String s = "5";
			    Integer valueOf = Integer.valueOf(s);
			  }
			}""";
		assertEquals(str, viewer.getDocument().get());
	}

	@Test
	public void testVarForMethodInvocation3() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			public class VarForMethodInvocation3 {
			  public <T> T getAdapter(Class<T> required) {
			    return null;
			  }\s
			  public class Child extends VarForMethodInvocation3 {
			    public void test() {
			      super.getAdapter(VarForMethodInvocation3.class).var$
			    }
			  }\s
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "VarForMethodInvocation3.java");
		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertProposalsExist(Arrays.asList("var - Creates a new variable"), proposals);

		ITextViewer viewer= initializeViewer(cu);
		applyProposal(viewer, proposals, "var", completionIndex);

		String str= """
			package test;
			public class VarForMethodInvocation3 {
			  public <T> T getAdapter(Class<T> required) {
			    return null;
			  }\s
			  public class Child extends VarForMethodInvocation3 {
			    public void test() {
			      VarForMethodInvocation3 adapter = super.getAdapter(VarForMethodInvocation3.class);
			    }
			  }\s
			}""";
		assertEquals(str, viewer.getDocument().get());
	}

	@Test
	public void testVarForClassCreation() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			public class VarForClassCreation {
			  public static final int STYLE = 7;
			  public void test () {
			    new Integer(VarForClassCreation.STYLE).var$
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "VarForClassCreation.java");
		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertProposalsExist(Arrays.asList("var - Creates a new variable"), proposals);

		ITextViewer viewer= initializeViewer(cu);
		applyProposal(viewer, proposals, "var", completionIndex);

		String str= """
			package test;
			public class VarForClassCreation {
			  public static final int STYLE = 7;
			  public void test () {
			    Integer integer = new Integer(VarForClassCreation.STYLE);
			  }
			}""";
		assertEquals(str, viewer.getDocument().get());
	}

	@Test
	public void testBegForVoidMethodInvocation() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			public class BegForVoidMethodInvocation {
			  public void test() {
			    getFoo(String.class).beg$
			  }\s
			  public <T> void getFoo (Class <T> foo) {
			  }\s
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "BegForVoidMethodInvocation.java");
		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertProposalsExist(Arrays.asList("beg - Sets the cursor to the begin of the expression"), proposals);

		ITextViewer viewer= initializeViewer(cu);
		applyProposal(viewer, proposals, "beg", completionIndex);

		int expectedBeg = buf.toString().indexOf("getFoo(String.class)");
		assertEquals(expectedBeg, viewer.getSelectedRange().x);
	}

	@Test
	public void testVarForAnonymousClass() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			import java.io.File;
			import java.io.FileFilter;
			public class Test {
			  public void test() {
			    new File("").listFiles(new FileFilter() {
			      @Override
			      public boolean accept(File pathname) {
			        return false;
			      }
			    }).var$
			  }\s
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "VarForAnonymousClass.java");
		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertProposalsExist(Arrays.asList("var - Creates a new variable"), proposals);

		ITextViewer viewer= initializeViewer(cu);
		applyProposal(viewer, proposals, "var", completionIndex);

		String str= """
			package test;
			import java.io.File;
			import java.io.FileFilter;
			public class Test {
			  public void test() {
			    File[] listFiles = new File("").listFiles(new FileFilter() {
			      @Override
			      public boolean accept(File pathname) {
			        return false;
			      }
			    });
			  }\s
			}""";
		assertEquals(str, viewer.getDocument().get());
	}

	@Test
	public void testNestedQualifiedNames() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			public class NestedQualifiedNames {
			public Foo foo;\s
			public void foo () {
			  Foo foo = new Foo ();
			  foo.bar.res.$
			}
			public class Foo {
			  public Bar bar;
			}
			public class Bar {
			  public String res;
			}
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "NestedQualifiedNames.java");
		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertProposalsExist(Arrays.asList("var - Creates a new variable"), proposals);

		ITextViewer viewer= initializeViewer(cu);
		applyProposal(viewer, proposals, "var", completionIndex);

		String str= """
			package test;
			public class NestedQualifiedNames {
			public Foo foo;\s
			public void foo () {
			  Foo foo = new Foo ();
			  String res = foo.bar.res;
			}
			public class Foo {
			  public Bar bar;
			}
			public class Bar {
			  public String res;
			}
			}""";
		assertEquals(str, viewer.getDocument().get());
	}

	@Test
	public void testFieldAccess() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			public class FieldAccess {
			  public class Foo {
			    public String res;
			    public void foo () {
			      this.res.$
			    }
			  }\
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "FieldAccess.java");
		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertProposalsExist(Arrays.asList("var - Creates a new variable"), proposals);

		ITextViewer viewer= initializeViewer(cu);
		applyProposal(viewer, proposals, "var", completionIndex);

		String str= """
			package test;
			public class FieldAccess {
			  public class Foo {
			    public String res;
			    public void foo () {
			      String res = this.res;
			    }
			  }\
			}""";
		assertEquals(str, viewer.getDocument().get());
	}

	@Test
	public void testForStatement() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			import java.util.List;
			public class ForStatement {
			  public void test () {
			    List<String> a;
			    a.$
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "ForStatement.java");
		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertProposalsExist(Arrays.asList("for - Creates a for statement"), proposals);

		ITextViewer viewer= initializeViewer(cu);
		applyProposal(viewer, proposals, "for", completionIndex);

		String str= """
			package test;
			import java.util.List;
			public class ForStatement {
			  public void test () {
			    List<String> a;
			    for (String a2 : a) {
			     \s
			    }
			  }
			}""";
		assertEquals(str, viewer.getDocument().get());
	}

	@Test
	public void testForStatement2() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			import java.util.List;
			public class ForStatement2 {
			  public int test (String [] inp) {
			    inp.fo$
			    return 0;
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "ForStatement2.java");
		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertProposalsExist(Arrays.asList("for - Creates a for statement"), proposals);

		ITextViewer viewer= initializeViewer(cu);
		applyProposal(viewer, proposals, "for", completionIndex);

		String str= """
			package test;
			import java.util.List;
			public class ForStatement2 {
			  public int test (String [] inp) {
			    for (String inp2 : inp) {
			     \s
			    }
			    return 0;
			  }
			}""";
		assertEquals(str, viewer.getDocument().get());
	}

	@Test
	public void testShorthandIfStatement() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			public class ShorthandIfStatement {
			  public void test () {
			    true.$
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "ShorthandIfStatement.java");
		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertProposalsExist(Arrays.asList("sif - Creates a short if statement"), proposals);

		ITextViewer viewer= initializeViewer(cu);
		applyProposal(viewer, proposals, "sif", completionIndex);

		String str= """
			package test;
			public class ShorthandIfStatement {
			  public void test () {
			    ((true) ?  : )
			  }
			}""";
		assertEquals(str, viewer.getDocument().get());
	}

	@Test
	public void testConcatenatedShorthandIfStatement() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			public class ConcatenatedShorthandIfStatement {
			  public void test () {
			    System.out.println("two + " + true.$
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "ConcatenatedShorthandIfStatement.java");
		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertProposalsExist(Arrays.asList("sif - Creates a short if statement"), proposals);

		ITextViewer viewer= initializeViewer(cu);
		applyProposal(viewer, proposals, "sif", completionIndex);

		String str= """
			package test;
			public class ConcatenatedShorthandIfStatement {
			  public void test () {
			    System.out.println("two + " + ((true) ?  : )
			  }
			}""";
		assertEquals(str, viewer.getDocument().get());
	}

	@Test
	public void testNoThrownExceptions() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			x.$
			public class NoThrownExceptions {
			  public void test () {
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "NoThrownExceptions.java");
		computeCompletionProposals(cu, completionIndex);
	}

	@Test
	public void testStreamOf() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			public class StreamOf {
			  public void test (String[] args) {
			    args.stream$
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "StreamOf.java");
		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertProposalsExist(Arrays.asList("stream - Creates a new stream using Stream.of"), proposals);

		ITextViewer viewer= initializeViewer(cu);
		applyProposal(viewer, proposals, "stream", completionIndex);

		String str= """
			package test;
			public class StreamOf {
			  public void test (String[] args) {
			    Stream.of(args)
			  }
			}""";
		assertEquals(str, viewer.getDocument().get());
	}


	private ITextViewer initializeViewer(ICompilationUnit cu) throws Exception {
		IEditorPart editor= EditorUtility.openInEditor(cu);
		ITextViewer viewer= new TextViewer(editor.getSite().getShell(), SWT.NONE);
		viewer.setDocument(new Document(cu.getSource()));
		return viewer;
	}

	private ICompilationUnit getCompilationUnit(IPackageFragment pack, StringBuffer buf, String name) throws JavaModelException {
		return pack.createCompilationUnit(name, buf.toString().replace("$", ""), false, null);
	}

	private int getCompletionIndex(StringBuffer buf) {
		return buf.toString().indexOf('$');
	}

	private List<ICompletionProposal> computeCompletionProposals(ICompilationUnit cu, int completionIndex) throws Exception {
		PostfixCompletionProposalComputer comp= new PostfixCompletionProposalComputer();

		IEditorPart editor= EditorUtility.openInEditor(cu);
		ITextViewer viewer= new TextViewer(editor.getSite().getShell(), SWT.NONE);
		viewer.setDocument(new Document(cu.getSource()));
		JavaContentAssistInvocationContext ctx= new JavaContentAssistInvocationContext(viewer, completionIndex, editor);

		return comp.computeCompletionProposals(ctx, null);
	}

	private void assertProposalsExist(List<String> expected, List<ICompletionProposal> proposals) {
		for (String propDisplay : expected) {
			assertTrue(proposals.stream().anyMatch(p -> propDisplay.equals(p.getDisplayString())));
		}
	}

	private void applyProposal(ITextViewer viewer, List<ICompletionProposal> proposals, String name, int offset) throws Exception {
		PostfixTemplateProposal proposal= (PostfixTemplateProposal) proposals.stream().filter(p -> ((PostfixTemplateProposal) p).getTemplate().getName().equals(name)).findFirst().get();
		proposal.apply(viewer, '0', -1, offset);
	}
}

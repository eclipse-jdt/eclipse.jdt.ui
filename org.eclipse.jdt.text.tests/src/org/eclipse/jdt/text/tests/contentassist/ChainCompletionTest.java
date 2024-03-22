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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.swt.SWT;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.eclipse.core.resources.ProjectScope;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.manipulation.JavaManipulation;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.text.java.ChainCompletionProposalComputer;

public class ChainCompletionTest {
	private IJavaProject fJProject;

	private IPackageFragmentRoot javaSrc;

	private IPackageFragment pkg;

	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	@Before
	public void setUp() throws Exception {
		fJProject= JavaProjectHelper.createJavaProject("TestProject", "bin");
		JavaProjectHelper.addRTJar18(fJProject);
		javaSrc= JavaProjectHelper.addSourceContainer(fJProject, "src");
		pkg= javaSrc.createPackageFragment("test", false, null);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject);
	}

	@Test
	public void testNullExpectedType() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			$package test;
			public class Foo {
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "Foo.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertEquals(0, proposals.size());
	}

	@Test
	public void testInvalidPreferenceHandling() throws Exception {
		IEclipsePreferences node= new ProjectScope(fJProject.getProject()).getNode(JavaManipulation.getPreferenceNodeId());
		node.put(PreferenceConstants.PREF_MAX_CHAIN_LENGTH, "number_four");

		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			public class Foo {
			  public void foo () {
			    String s = "";
			    int length = $
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "Foo.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);
		assertEquals(0, proposals.size());
	}

	@Test
	public void testBasic() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			public class Foo {
			  public Bar getBar() {
			    return new Bar();
			  }
			 \s
			  public class Bar {
			    Baz getBaz () {
			      return new Baz();
			    }
			  }
			 \s
			  public class Baz {
			  }
			
			  public static void mainMethod () {
			    Foo f = new Foo();
			    Baz b = f.$
			  }
			
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "Foo.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertEquals(1, proposals.size());
		assertEquals("getBar().getBaz() - 2 elements", proposals.get(0).getDisplayString());
	}

	@Test
	public void testPrimitiveCompletion() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			public class Foo {
			  public void foo () {
			    String s = "";
			    int length = $
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "Foo.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertFalse(proposals.isEmpty());
		List<String> expected= Arrays.asList(
				"s.length() - 2 elements",
				"s.hashCode() - 2 elements",
				"s.indexOf(String) - 2 elements",
				"s.compareTo(String) - 2 elements");

		assertProposalsExist(expected, proposals);
	}

	@Test
	public void testAccessMethodParameters() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			
			import java.util.Iterator;
			import java.util.List;
			
			public class Foo {
			  public void method(final List list){
			    Iterator it = $
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "Foo.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		List<String> expected= Arrays.asList(
				"list.iterator() - 2 elements",
				"list.listIterator() - 2 elements",
				"list.listIterator(int) - 2 elements",
				"list.subList(int, int).iterator() - 3 elements",
				"list.stream().iterator() - 3 elements");

		assertProposalsExist(expected, proposals);
	}

	@Test
	public void testAvoidRecursiveCallToMember() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			
			import java.io.File;
			
			public class AvoidRecursiveCallsToMember {
			  public File findMe = new AtomicBoolean();
			  public AvoidRecursiveCallsToMember getSubElement() {
			    return new AvoidRecursiveCallsToMember();
			  }
			  public static void method2() {
			    final AvoidRecursiveCallsToMember useMe = new AvoidRecursiveCallsToMember();
			    final File c = useMe.get$
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "AvoidRecursiveCallsToMember.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertEquals(1, proposals.size());
		assertEquals("getSubElement().findMe - 2 elements", proposals.get(0).getDisplayString());
	}

	@Test
	public void testCompletionOnArrayMemberAccessInMethod1() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			
			import java.lang.Boolean;
			import java.lang.Integer;
			
			public class CompletionOnArrayMemberAccessInMethod {
			  public Integer findUs[] = { new Integer(1), new Integer(2) };
			  public Boolean findUs1[][][] = new Boolean[1][1][1];
			
			  public static void method1() {
			    final CompletionOnArrayMemberAccessInMethod obj = new CompletionOnArrayMemberAccessInMethod();
			    final Integer c = $
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "CompletionOnArrayMemberAccessInMethod.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		List<String> expected= Arrays.asList("obj.findUs[] - 2 elements");
		assertProposalsExist(expected, proposals);
	}

	@Test
	public void testCompletionOnArrayWithCastsSupertype1() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			
			import java.lang.Integer;
			import java.lang.Number;
			
			public class CompletionOnArrayWithCastsSupertype {
			  public Integer[][][] findme;
			  public int i;
			
			  public static void method1() {
			    final CompletionOnArrayWithCastsSupertype obj = new CompletionOnArrayWithCastsSupertype();
			    final Number c = $
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "CompletionOnArrayWithCastsSupertype.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertEquals(1, proposals.size());
		assertEquals("obj.findme[][][] - 2 elements", proposals.get(0).getDisplayString());
	}

	@Test
	public void testCompletionOnGenericTypeInMethod1() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			
			import java.util.ArrayList;
			import java.util.List;
			
			public class CompletionOnGenericTypeInMethod {
			  public List<String> findMe = new ArrayList<String>();
			
			  public static void test_exactGenericType() {
			    final CompletionOnGenericTypeInMethod variable = new CompletionOnGenericTypeInMethod();
			    final List<String> c = $
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "CompletionOnGenericTypeInMethod.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertEquals(1, proposals.size());
		assertEquals("variable.findMe - 2 elements", proposals.get(0).getDisplayString());
	}

	@Test
	public void testCompletionOnMemberCallChainDepth1() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			
			import java.io.File;
			
			public class A {
			  public B b = new B();
			  public class B {
			    public File findMember = new File("");
			    public File findMethod() {
			      return null;
			    }
			  }
			  public static void mainMethod () {
			    A a = new A();
			    File c = $
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "A.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		List<String> expected= Arrays.asList(
				"a.b.findMethod() - 3 elements",
				"a.b.findMember - 3 elements");

		assertProposalsExist(expected, proposals);
	}

	@Test
	public void testCompletionOnMemberInMethodWithPrefix() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			
			import java.io.File;
			
			public class CompletionOnMemberInMethodWithPrefix {
			
			  public File findMe;
			
			  public CompletionOnMemberInMethodWithPrefix getSubElement() {
			    return new CompletionOnMemberInMethodWithPrefix();
			  }
			
			  public static void method2() {
			    final CompletionOnMemberInMethodWithPrefix useMe = new CompletionOnMemberInMethodWithPrefix();
			    final File c = useMe.get$
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "CompletionOnMemberInMethodWithPrefix.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertEquals(1, proposals.size());
		assertEquals("getSubElement().findMe - 2 elements", proposals.get(0).getDisplayString());
	}

	@Test
	public void testCompletionOnMethodReturn() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			
			import java.io.File;
			import java.util.Iterator;
			import java.util.LinkedList;
			import java.util.List;
			
			public class Foo {
			  public void method() {
			    final Iterator<File> c = $
			  }
			  private List<File> getList() {
			    return new LinkedList<File>();
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "Foo.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		List<String> expected= Arrays.asList(
				"getList().iterator() - 2 elements",
				"getList().listIterator() - 2 elements",
				"getList().listIterator(int) - 2 elements",
				"getList().subList(int, int).iterator() - 3 elements",
				"getList().subList(int, int).listIterator() - 3 elements",
				"getList().subList(int, int).listIterator(int) - 3 elements");

		assertProposalsExist(expected, proposals);
	}

	@Test
	public void testCompletionOnNonPublicMemberInMethod1() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			
			public class CompletionOnNonPublicMemberInMethod {
			  protected Boolean findMe1 = new Boolean();
			  Integer findMe2 = new Integer();
			  private final Long findMe3 = new Long();
			
			  public static void test_protected() {
			    final CompletionOnNonPublicMemberInMethod useMe = new CompletionOnNonPublicMemberInMethod();
			    final Boolean c = $
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "CompletionOnNonPublicMemberInMethod.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		List<String> expected= Arrays.asList("useMe.findMe1 - 2 elements");
		assertProposalsExist(expected, proposals);
	}

	@Test
	public void testCompletionOnSuperTypeInMethod1() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			
			import java.io.ByteArrayInputStream;
			import java.io.InputStream;
			
			public class CompletionOnSupertypeInMethod {
			  public ByteArrayInputStream findMe = new ByteArrayInputStream(new byte[] { 0, 1, 2, 3 });
			
			  public static void method() {
			    final CompletionOnSupertypeInMethod useMe = new CompletionOnSupertypeInMethod();
			    final InputStream c = $
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "CompletionOnSupertypeInMethod.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertEquals(1, proposals.size());
		assertEquals("useMe.findMe - 2 elements", proposals.get(0).getDisplayString());
	}

	@Test
	public void testCompletionOnSuperTypeMemberInMethod1() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			
			public class CompletionOnSupertypeMemberInMethod {
			
			  public static class Subtype extends CompletionOnSupertypeMemberInMethod {
			  }
			
			  public Boolean findMe = new Boolean();
			
			  public static void test_onAttribute() {
			    final Subtype useMe = new Subtype();
			    final Boolean c = $
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "CompletionOnSupertypeMemberInMethod.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		List<String> expected= Arrays.asList("useMe.findMe - 2 elements");
		assertProposalsExist(expected, proposals);
	}

	@Test
	public void testCompletionOnSuperTypeMemberInMethod2() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			
			public class CompletionOnSupertypeMemberInMethod {
			
			  public static class Subtype extends CompletionOnSupertypeMemberInMethod {
			  }
			
			  public Boolean findMe() {
			    return Boolean.TRUE;
			  }
			
			  public static void test_onAttribute() {
			    final Subtype useMe = new Subtype();
			    final Boolean c = $
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "CompletionOnSupertypeMemberInMethod.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		List<String> expected= Arrays.asList("useMe.findMe() - 2 elements");
		assertProposalsExist(expected, proposals);
	}

	@Test
	public void testCompletionOnThisAndLocal() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			import java.util.Collection;
			import java.util.HashMap;
			import java.util.Map;
			
			package test;
			
			public class TestCompletionOnThisAndLocal {
			  public void method() {
			    final Map map = new HashMap();
			    final Collection c = $\s
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "TestCompletionOnThisAndLocal.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		List<String> expected= Arrays.asList(
				"map.entrySet() - 2 elements",
				"map.keySet() - 2 elements",
				"map.values() - 2 elements"
				);
		assertProposalsExist(expected, proposals);
	}

	@Test
	public void testCompletionOnType() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			
			public class TestCompletionOnType {
			  public class S {
			
			    private static S INSTANCE = new S();
			    private S () {}
			
			    public Integer findMe() {
			      return 0;
			    }
			
			    public static S getInstance() {
			      return INSTANCE;
			    }
			  }
			
			  public void __test() {
			    Integer i = S.$
			  }\s
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "TestCompletionOnType.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		List<String> expected= Arrays.asList("getInstance().findMe() - 2 elements");
		assertProposalsExist(expected, proposals);
	}

	@Test
	public void testBug552849 () throws Exception {
		IPackageFragment pkg2= javaSrc.createPackageFragment("test2", false, null);
		IPackageFragment pkg3= javaSrc.createPackageFragment("test3", false, null);

		// test.TestBug552849 -> test.Foo -> test2.Bar -> test3.Foo
		StringBuffer buf= new StringBuffer();
		buf.append("""
			import java.io.File;
			package test3;
			public class Foo {
				protected File fVal= "";
			}""");
		ICompilationUnit cu= getCompilationUnit(pkg3, buf, "Foo.java");

		buf= new StringBuffer();
		buf.append("""
			package test2;
			import test3.Foo;
			public class Bar extends Foo {
			}""");
		cu= getCompilationUnit(pkg2, buf, "Bar.java");

		buf= new StringBuffer();
		buf.append("""
			package test;
			import test2.Bar;
			public class Foo extends Bar {
			}""");
		cu= getCompilationUnit(pkg, buf, "Foo.java");

		buf= new StringBuffer();
		buf.append("""
			import java.io.File;
			package test;
			public class TestBug552849 extends Foo {
				public void test () {
					TestBug552849 foo = new TestBug552849();
					File res = $
				}
			}""");

		int completionIndex= getCompletionIndex(buf);
		cu= getCompilationUnit(pkg, buf, "TestBug552849.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		List<String> expected= Arrays.asList("foo.fVal - 2 elements");
		assertProposalsExist(expected, proposals);
	}

	@Test
	public void testBug559385 () throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			public @interface Command {
				String name();
			}""");
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "Command.java");

		buf= new StringBuffer();
		buf.append("""
			package test;
			import java.util.concurrent.Callable;
			@Command(name = $"")
			public class TestBug559385 implements Callable<String> {
				@Override
				public String call() throws Exception {
					return null;
				}
			}""");
		cu= getCompilationUnit(pkg, buf, "TestBug559385.java");
		int completionIndex= getCompletionIndex(buf);

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);
		assertTrue(proposals.size() > 0);
	}

	@Test
	public void testNoTriggerCompletionInvocation() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("""
			package test;
			
			import java.util.Iterator;
			import java.util.List;
			
			public class NoTriggerCompletionInvocation {
			  public void method(){
			    List longVariableName, longVariableName2;
			    Iterator it = longVariableName$
			    String foo = null;
			  }
			}""");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "NoTriggerCompletionInvocation.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);
		String expectedProposal= "longVariableName.iterator() - 2 elements";
		assertProposalsExist(Arrays.asList(expectedProposal), proposals);

		ICompletionProposal proposal= proposals.stream().filter(p -> p.getDisplayString().equals(expectedProposal)).findFirst().get();
		IDocument doc= new Document(buf.toString().replace("$", ""));
		applyProposal(proposal, doc, cu, completionIndex);
		String expectedContent = """
			package test;
			
			import java.util.Iterator;
			import java.util.List;
			
			public class NoTriggerCompletionInvocation {
			  public void method(){
			    List longVariableName, longVariableName2;
			    Iterator it = longVariableName.iterator()
			    String foo = null;
			  }
			}""";
		assertEquals(expectedContent,doc.get());
	}

	private ICompilationUnit getCompilationUnit(IPackageFragment pack, StringBuffer buf, String name) throws JavaModelException {
		return pack.createCompilationUnit(name, buf.toString().replace("$", ""), false, null);
	}

	private int getCompletionIndex(StringBuffer buf) {
		return buf.toString().indexOf('$');
	}

	private List<ICompletionProposal> computeCompletionProposals(ICompilationUnit cu, int completionIndex) throws Exception {
		ChainCompletionProposalComputer comp= new ChainCompletionProposalComputer();

		IEditorPart editor= EditorUtility.openInEditor(cu);
		ITextViewer viewer= new TextViewer(editor.getSite().getShell(), SWT.NONE);
		viewer.setDocument(new Document(cu.getSource()));
		JavaContentAssistInvocationContext ctx= new JavaContentAssistInvocationContext(viewer, completionIndex, editor);

		return comp.computeCompletionProposals(ctx, null);
	}

	private void applyProposal (ICompletionProposal prop, IDocument doc, ICompilationUnit cu, int completionIndex) throws Exception {
		IEditorPart editor= EditorUtility.openInEditor(cu);
		ITextViewer viewer= new TextViewer(editor.getSite().getShell(), SWT.NONE);
		viewer.setDocument(doc);
		if (prop instanceof ICompletionProposalExtension2) {
			((ICompletionProposalExtension2)prop).apply(viewer, '\0', 0, completionIndex);
		}
	}

	private void assertProposalsExist(List<String> expected, List<ICompletionProposal> proposals) {
		for (String propDisplay : expected) {
			assertTrue(proposals.stream().anyMatch(p -> propDisplay.equals(p.getDisplayString())));
		}
	}
}

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
		buf.append("$package test;\n" +
				"public class Foo {\n" +
				"}");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "Foo.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertEquals(0, proposals.size());
	}

	@Test
	public void testBasic() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n" +
				"public class Foo {\n" +
				"  public Bar getBar() {\n" +
				"    return new Bar();\n" +
				"  }\n" +
				"  \n" +
				"  public class Bar {\n" +
				"    Baz getBaz () {\n" +
				"      return new Baz();\n" +
				"    }\n" +
				"  }\n" +
				"  \n" +
				"  public class Baz {\n" +
				"  }\n" +
				"\n" +
				"  public static void mainMethod () {\n" +
				"    Foo f = new Foo();\n" +
				"    Baz b = f.$\n" +
				"  }\n" +
				"\n" +
				"}");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "Foo.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertEquals(1, proposals.size());
		assertEquals("getBar().getBaz() - 2 elements", proposals.get(0).getDisplayString());
	}

	@Test
	public void testPrimitiveCompletion() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n" +
				"public class Foo {\n" +
				"  public void foo () {\n" +
				"    String s = \"\";\n" +
				"    int length = $\n" +
				"  }\n" +
				"}");

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
		buf.append("package test;\n" +
				"\n" +
				"import java.util.Iterator;\n" +
				"import java.util.List;\n" +
				"\n" +
				"public class Foo {\n" +
				"  public void method(final List list){\n" +
				"    Iterator it = $\n" +
				"  }\n" +
				"}");

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
		buf.append("package test;\n" +
				"\n" +
				"import java.io.File;\n" +
				"\n" +
				"public class AvoidRecursiveCallsToMember {\n" +
				"  public File findMe = new AtomicBoolean();\n" +
				"  public AvoidRecursiveCallsToMember getSubElement() {\n" +
				"    return new AvoidRecursiveCallsToMember();\n" +
				"  }\n" +
				"  public static void method2() {\n" +
				"    final AvoidRecursiveCallsToMember useMe = new AvoidRecursiveCallsToMember();\n" +
				"    final File c = useMe.get$\n" +
				"  }\n" +
				"}");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "AvoidRecursiveCallsToMember.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertEquals(1, proposals.size());
		assertEquals("getSubElement().findMe - 2 elements", proposals.get(0).getDisplayString());
	}

	@Test
	public void testCompletionOnArrayMemberAccessInMethod1() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n" +
				"\n" +
				"import java.lang.Boolean;\n" +
				"import java.lang.Integer;\n" +
				"\n" +
				"public class CompletionOnArrayMemberAccessInMethod {\n" +
				"  public Integer findUs[] = { new Integer(1), new Integer(2) };\n" +
				"  public Boolean findUs1[][][] = new Boolean[1][1][1];\n" +
				"\n" +
				"  public static void method1() {\n" +
				"    final CompletionOnArrayMemberAccessInMethod obj = new CompletionOnArrayMemberAccessInMethod();\n" +
				"    final Integer c = $\n" +
				"  }\n" +
				"}");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "CompletionOnArrayMemberAccessInMethod.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		List<String> expected= Arrays.asList("obj.findUs[] - 2 elements");
		assertProposalsExist(expected, proposals);
	}

	@Test
	public void testCompletionOnArrayWithCastsSupertype1() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n" +
				"\n" +
				"import java.lang.Integer;\n" +
				"import java.lang.Number;\n" +
				"\n" +
				"public class CompletionOnArrayWithCastsSupertype {\n" +
				"  public Integer[][][] findme;\n" +
				"  public int i;\n" +
				"\n" +
				"  public static void method1() {\n" +
				"    final CompletionOnArrayWithCastsSupertype obj = new CompletionOnArrayWithCastsSupertype();\n" +
				"    final Number c = $\n" +
				"  }\n" +
				"}");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "CompletionOnArrayWithCastsSupertype.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertEquals(1, proposals.size());
		assertEquals("obj.findme[][][] - 2 elements", proposals.get(0).getDisplayString());
	}

	@Test
	public void testCompletionOnGenericTypeInMethod1() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n" +
				"\n" +
				"import java.util.ArrayList;\n" +
				"import java.util.List;\n" +
				"\n" +
				"public class CompletionOnGenericTypeInMethod {\n" +
				"  public List<String> findMe = new ArrayList<String>();\n" +
				"\n" +
				"  public static void test_exactGenericType() {\n" +
				"    final CompletionOnGenericTypeInMethod variable = new CompletionOnGenericTypeInMethod();\n" +
				"    final List<String> c = $\n" +
				"  }\n" +
				"}");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "CompletionOnGenericTypeInMethod.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertEquals(1, proposals.size());
		assertEquals("variable.findMe - 2 elements", proposals.get(0).getDisplayString());
	}

	@Test
	public void testCompletionOnMemberCallChainDepth1() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n" +
				"\n" +
				"import java.io.File;\n" +
				"\n" +
				"public class A {\n" +
				"  public B b = new B();\n" +
				"  public class B {\n" +
				"    public File findMember = new File(\"\");\n" +
				"    public File findMethod() {\n" +
				"      return null;\n" +
				"    }\n" +
				"  }\n" +
				"  public static void mainMethod () {\n" +
				"    A a = new A();\n" +
				"    File c = $\n" +
				"  }\n" +
				"}");

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
		buf.append("package test;\n" +
				"\n" +
				"import java.io.File;\n" +
				"\n" +
				"public class CompletionOnMemberInMethodWithPrefix {\n" +
				"\n" +
				"  public File findMe;\n" +
				"\n" +
				"  public CompletionOnMemberInMethodWithPrefix getSubElement() {\n" +
				"    return new CompletionOnMemberInMethodWithPrefix();\n" +
				"  }\n" +
				"\n" +
				"  public static void method2() {\n" +
				"    final CompletionOnMemberInMethodWithPrefix useMe = new CompletionOnMemberInMethodWithPrefix();\n" +
				"    final File c = useMe.get$\n" +
				"  }\n" +
				"}");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "CompletionOnMemberInMethodWithPrefix.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertEquals(1, proposals.size());
		assertEquals("getSubElement().findMe - 2 elements", proposals.get(0).getDisplayString());
	}

	@Test
	public void testCompletionOnMethodReturn() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n" +
				"\n" +
				"import java.io.File;\n" +
				"import java.util.Iterator;\n" +
				"import java.util.LinkedList;\n" +
				"import java.util.List;\n" +
				"\n" +
				"public class Foo {\n" +
				"  public void method() {\n" +
				"    final Iterator<File> c = $\n" +
				"  }\n" +
				"  private List<File> getList() {\n" +
				"    return new LinkedList<File>();\n" +
				"  }\n" +
				"}");

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
		buf.append("package test;\n" +
				"\n" +
				"public class CompletionOnNonPublicMemberInMethod {\n" +
				"  protected Boolean findMe1 = new Boolean();\n" +
				"  Integer findMe2 = new Integer();\n" +
				"  private final Long findMe3 = new Long();\n" +
				"\n" +
				"  public static void test_protected() {\n" +
				"    final CompletionOnNonPublicMemberInMethod useMe = new CompletionOnNonPublicMemberInMethod();\n" +
				"    final Boolean c = $\n" +
				"  }\n" +
				"}");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "CompletionOnNonPublicMemberInMethod.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		List<String> expected= Arrays.asList("useMe.findMe1 - 2 elements");
		assertProposalsExist(expected, proposals);
	}

	@Test
	public void testCompletionOnSuperTypeInMethod1() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n" +
				"\n" +
				"import java.io.ByteArrayInputStream;\n" +
				"import java.io.InputStream;\n" +
				"\n" +
				"public class CompletionOnSupertypeInMethod {\n" +
				"  public ByteArrayInputStream findMe = new ByteArrayInputStream(new byte[] { 0, 1, 2, 3 });\n" +
				"\n" +
				"  public static void method() {\n" +
				"    final CompletionOnSupertypeInMethod useMe = new CompletionOnSupertypeInMethod();\n" +
				"    final InputStream c = $\n" +
				"  }\n" +
				"}");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "CompletionOnSupertypeInMethod.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		assertEquals(1, proposals.size());
		assertEquals("useMe.findMe - 2 elements", proposals.get(0).getDisplayString());
	}

	@Test
	public void testCompletionOnSuperTypeMemberInMethod1() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n" +
				"\n" +
				"public class CompletionOnSupertypeMemberInMethod {\n" +
				"\n" +
				"  public static class Subtype extends CompletionOnSupertypeMemberInMethod {\n" +
				"  }\n" +
				"\n" +
				"  public Boolean findMe = new Boolean();\n" +
				"\n" +
				"  public static void test_onAttribute() {\n" +
				"    final Subtype useMe = new Subtype();\n" +
				"    final Boolean c = $\n" +
				"  }\n" +
				"}");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "CompletionOnSupertypeMemberInMethod.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		List<String> expected= Arrays.asList("useMe.findMe - 2 elements");
		assertProposalsExist(expected, proposals);
	}

	@Test
	public void testCompletionOnSuperTypeMemberInMethod2() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n" +
				"\n" +
				"public class CompletionOnSupertypeMemberInMethod {\n" +
				"\n" +
				"  public static class Subtype extends CompletionOnSupertypeMemberInMethod {\n" +
				"  }\n" +
				"\n" +
				"  public Boolean findMe() {\n" +
				"    return Boolean.TRUE;\n" +
				"  }\n" +
				"\n" +
				"  public static void test_onAttribute() {\n" +
				"    final Subtype useMe = new Subtype();\n" +
				"    final Boolean c = $\n" +
				"  }\n" +
				"}");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "CompletionOnSupertypeMemberInMethod.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		List<String> expected= Arrays.asList("useMe.findMe() - 2 elements");
		assertProposalsExist(expected, proposals);
	}

	@Test
	public void testCompletionOnThisAndLocal() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("import java.util.Collection;\n" +
				"import java.util.HashMap;\n" +
				"import java.util.Map;\n" +
				"\n" +
				"package test;\n" +
				"\n" +
				"public class TestCompletionOnThisAndLocal {\n" +
				"  public void method() {\n" +
				"    final Map map = new HashMap();\n" +
				"    final Collection c = $ \n" +
				"  }\n" +
				"}");

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
		buf.append("package test;\n" +
				"\n" +
				"public class TestCompletionOnType {\n" +
				"  public class S {\n" +
				"\n" +
				"    private static S INSTANCE = new S();\n" +
				"    private S () {}\n" +
				"\n" +
				"    public Integer findMe() {\n" +
				"      return 0;\n" +
				"    }\n" +
				"\n" +
				"    public static S getInstance() {\n" +
				"      return INSTANCE;\n" +
				"    }\n" +
				"  }\n" +
				"\n" +
				"  public void __test() {\n" +
				"    Integer i = S.$\n" +
				"  } \n" +
				"}");

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
		buf.append("import java.io.File;\n" +
				"package test3;\n" +
				"public class Foo {\n" +
				"	protected File fVal= \"\";\n" +
				"}");
		ICompilationUnit cu= getCompilationUnit(pkg3, buf, "Foo.java");

		buf= new StringBuffer();
		buf.append("package test2;\n" +
				"import test3.Foo;\n" +
				"public class Bar extends Foo {\n" +
				"}");
		cu= getCompilationUnit(pkg2, buf, "Bar.java");

		buf= new StringBuffer();
		buf.append("package test;\n" +
				"import test2.Bar;\n" +
				"public class Foo extends Bar {\n" +
				"}");
		cu= getCompilationUnit(pkg, buf, "Foo.java");

		buf= new StringBuffer();
		buf.append("import java.io.File;\n" +
				"package test;\n" +
				"public class TestBug552849 extends Foo {\n" +
				"	public void test () {\n" +
				"		TestBug552849 foo = new TestBug552849();\n" +
				"		File res = $\n" +
				"	}\n" +
				"}");

		int completionIndex= getCompletionIndex(buf);
		cu= getCompilationUnit(pkg, buf, "TestBug552849.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);

		List<String> expected= Arrays.asList("foo.fVal - 2 elements");
		assertProposalsExist(expected, proposals);
	}

	@Test
	public void testBug559385 () throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n" +
				"public @interface Command {\n" +
				"	String name();\n" +
				"}");
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "Command.java");

		buf= new StringBuffer();
		buf.append("package test;\n" +
				"import java.util.concurrent.Callable;\n" +
				"@Command(name = $\"\")\n" +
				"public class TestBug559385 implements Callable<String> {\n" +
				"	@Override\n" +
				"	public String call() throws Exception {\n" +
				"		return null;\n" +
				"	}\n" +
				"}");
		cu= getCompilationUnit(pkg, buf, "TestBug559385.java");
		int completionIndex= getCompletionIndex(buf);

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);
		assertTrue(proposals.size() > 0);
	}

	@Test
	public void testNoTriggerCompletionInvocation() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n" +
				"\n" +
				"import java.util.Iterator;\n" +
				"import java.util.List;\n" +
				"\n" +
				"public class NoTriggerCompletionInvocation {\n" +
				"  public void method(){\n" +
				"    List longVariableName, longVariableName2;\n" +
				"    Iterator it = longVariableName$\n" +
				"    String foo = null;\n" +
				"  }\n" +
				"}");

		int completionIndex= getCompletionIndex(buf);
		ICompilationUnit cu= getCompilationUnit(pkg, buf, "NoTriggerCompletionInvocation.java");

		List<ICompletionProposal> proposals= computeCompletionProposals(cu, completionIndex);
		String expectedProposal= "longVariableName.iterator() - 2 elements";
		assertProposalsExist(Arrays.asList(expectedProposal), proposals);

		ICompletionProposal proposal= proposals.stream().filter(p -> p.getDisplayString().equals(expectedProposal)).findFirst().get();
		IDocument doc= new Document(buf.toString().replace("$", ""));
		applyProposal(proposal, doc, cu, completionIndex);
		String expectedContent = "package test;\n" +
				"\n" +
				"import java.util.Iterator;\n" +
				"import java.util.List;\n" +
				"\n" +
				"public class NoTriggerCompletionInvocation {\n" +
				"  public void method(){\n" +
				"    List longVariableName, longVariableName2;\n" +
				"    Iterator it = longVariableName.iterator()\n" +
				"    String foo = null;\n" +
				"  }\n" +
				"}";
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

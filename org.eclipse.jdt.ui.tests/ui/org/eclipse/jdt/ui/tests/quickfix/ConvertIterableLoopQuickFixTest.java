/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.Hashtable;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.FixCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.QuickAssistProcessor;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

public final class ConvertIterableLoopQuickFixTest extends QuickFixTest {

	private static final Class THIS= ConvertIterableLoopQuickFixTest.class;

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}

	public static Test suite() {
		return allTests();
	}

	private FixCorrectionProposal fConvertLoopProposal;

	private IJavaProject fProject;

	private IPackageFragmentRoot fSourceFolder;

	public ConvertIterableLoopQuickFixTest(String name) {
		super(name);
	}

	private List fetchConvertingProposal(StringBuffer buf, ICompilationUnit cu) throws Exception {
		int offset= buf.toString().indexOf("for");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List proposals= collectAssists(context, false);
		fConvertLoopProposal= (FixCorrectionProposal) findProposalByCommandId(QuickAssistProcessor.CONVERT_FOR_LOOP_ID, proposals);
		return proposals;
	}

	protected void setUp() throws Exception {
		Hashtable options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fProject= ProjectTestSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fProject, "src");
		fConvertLoopProposal= null;
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fProject, ProjectTestSetup.getDefaultClasspath());
	}

	public void testSimplestSmokeCase() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\r\n" + 
				"import java.util.Collection;\r\n" + 
				"import java.util.Iterator;\r\n" + 
				"public class A {\r\n" + 
				"	Collection<String> c;\r\n" + 
				"	public A() {\r\n" + 
				"		for (final Iterator<String> iterator= c.iterator(); iterator.hasNext();) {\r\n" + 
				"			String test= iterator.next();\r\n" + 
				"			System.out.println(test);\r\n" + 
				"		}\r\n" + 
				"	}\r\n" + 
				"}");
		ICompilationUnit unit= pack.createCompilationUnit("A.java", buf.toString(), false, null);

		List proposals= fetchConvertingProposal(buf, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
		buf.append("package test;\r\n" + 
				"import java.util.Collection;\r\n" + 
				"public class A {\r\n" + 
				"	Collection<String> c;\r\n" + 
				"	public A() {\r\n" + 
				"		for (String test : c) {\r\n" + 
				"			System.out.println(test);\r\n" + 
				"		}\r\n" + 
				"	}\r\n" + 
				"}");
		String expected= buf.toString();
		assertEqualString(preview, expected);
	}

	public void testEnumeration() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\r\n" + 
				"import java.util.Enumeration;\r\n" + 
				"import java.util.Vector;\r\n" + 
				"public class A {\r\n" + 
				"	Vector<String> c;\r\n" + 
				"	public A() {\r\n" + 
				"		for (Enumeration<String> e= c.elements(); e.hasMoreElements(); ) {\r\n" + 
				"			String nextElement = e.nextElement();\r\n" + 
				"			System.out.println(nextElement);\r\n" + 
				"		}\r\n" + 
				"	}\r\n" + 
				"}");
		ICompilationUnit unit= pack.createCompilationUnit("A.java", buf.toString(), false, null);

		List proposals= fetchConvertingProposal(buf, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
		buf.append("package test;\r\n" + 
				"import java.util.Vector;\r\n" + 
				"public class A {\r\n" + 
				"	Vector<String> c;\r\n" + 
				"	public A() {\r\n" + 
				"		for (String nextElement : c) {\r\n" + 
				"			System.out.println(nextElement);\r\n" + 
				"		}\r\n" + 
				"	}\r\n" + 
				"}");
		String expected= buf.toString();
		assertEqualString(preview, expected);
	}

	public void testSplitAssignment() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\r\n" + 
				"import java.util.Collection;\r\n" + 
				"import java.util.Iterator;\r\n" + 
				"public class A {\r\n" + 
				"	Collection<String> c;\r\n" + 
				"	public A() {\r\n" + 
				"		for (final Iterator<String> iterator= c.iterator(); iterator.hasNext();) {\r\n" + 
				"			String test= null;\r\n" + 
				"			test= iterator.next();\r\n" + 
				"			System.out.println(test);\r\n" + 
				"		}\r\n" + 
				"	}\r\n" + 
				"}");
		ICompilationUnit unit= pack.createCompilationUnit("A.java", buf.toString(), false, null);

		List proposals= fetchConvertingProposal(buf, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
		buf.append("package test;\r\n" + 
				"import java.util.Collection;\r\n" + 
				"public class A {\r\n" + 
				"	Collection<String> c;\r\n" + 
				"	public A() {\r\n" + 
				"		for (String test : c) {\r\n" + 
				"			System.out.println(test);\r\n" + 
				"		}\r\n" + 
				"	}\r\n" + 
				"}");
		String expected= buf.toString();
		assertEqualString(preview, expected);
	}

	public void testIndirectUsage() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\r\n" + 
				"import java.util.Collection;\r\n" + 
				"import java.util.Iterator;\r\n" + 
				"public class A {\r\n" + 
				"	Collection<String> c;\r\n" + 
				"	public A() {\r\n" + 
				"		for (final Iterator<String> iterator= c.iterator(); iterator.hasNext();) {\r\n" + 
				"			String test= null;\r\n" + 
				"			test= iterator.next();\r\n" + 
				"			String backup= test;\r\n" + 
				"			System.out.println(backup);\r\n" + 
				"		}\r\n" + 
				"	}\r\n" + 
				"}");
		ICompilationUnit unit= pack.createCompilationUnit("A.java", buf.toString(), false, null);

		List proposals= fetchConvertingProposal(buf, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
		buf.append("package test;\r\n" + 
				"import java.util.Collection;\r\n" + 
				"public class A {\r\n" + 
				"	Collection<String> c;\r\n" + 
				"	public A() {\r\n" + 
				"		for (String test : c) {\r\n" + 
				"			String backup= test;\r\n" + 
				"			System.out.println(backup);\r\n" + 
				"		}\r\n" + 
				"	}\r\n" + 
				"}");
		String expected= buf.toString();
		assertEqualString(preview, expected);
	}

	public void testMethodCall1() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\r\n" + 
				"import java.util.Collection;\r\n" + 
				"import java.util.Iterator;\r\n" + 
				"public class A {\r\n" + 
				"	Collection<String> c;\r\n" + 
				"	private Collection<String> getCollection() {\r\n" + 
				"		return c;\r\n" + 
				"	}\r\n" + 
				"	public A() {\r\n" + 
				"		for (final Iterator<String> iterator= getCollection().iterator(); iterator.hasNext();) {\r\n" + 
				"			String test= iterator.next();\r\n" + 
				"			String backup= test;\r\n" + 
				"			System.out.println(backup);\r\n" + 
				"		}\r\n" + 
				"	}\r\n" + 
				"}");
		ICompilationUnit unit= pack.createCompilationUnit("A.java", buf.toString(), false, null);

		List proposals= fetchConvertingProposal(buf, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
		buf.append("package test;\r\n" + 
				"import java.util.Collection;\r\n" + 
				"public class A {\r\n" + 
				"	Collection<String> c;\r\n" + 
				"	private Collection<String> getCollection() {\r\n" + 
				"		return c;\r\n" + 
				"	}\r\n" + 
				"	public A() {\r\n" + 
				"		for (String test : getCollection()) {\r\n" + 
				"			String backup= test;\r\n" + 
				"			System.out.println(backup);\r\n" + 
				"		}\r\n" + 
				"	}\r\n" + 
				"}");
		String expected= buf.toString();
		assertEqualString(preview, expected);
	}

	public void testMethodCall2() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\r\n" + 
				"import java.util.Collection;\r\n" + 
				"import java.util.Iterator;\r\n" + 
				"public class A {\r\n" + 
				"	Collection<String> c;\r\n" + 
				"	private Collection<String> getCollection() {\r\n" + 
				"		return c;\r\n" + 
				"	}\r\n" + 
				"	public A() {\r\n" + 
				"		for (final Iterator<String> iterator= this.getCollection().iterator(); iterator.hasNext();) {\r\n" + 
				"			String test= iterator.next();\r\n" + 
				"			String backup= test;\r\n" + 
				"			System.out.println(backup);\r\n" + 
				"		}\r\n" + 
				"	}\r\n" + 
				"}");
		ICompilationUnit unit= pack.createCompilationUnit("A.java", buf.toString(), false, null);

		List proposals= fetchConvertingProposal(buf, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
		buf.append("package test;\r\n" + 
				"import java.util.Collection;\r\n" + 
				"public class A {\r\n" + 
				"	Collection<String> c;\r\n" + 
				"	private Collection<String> getCollection() {\r\n" + 
				"		return c;\r\n" + 
				"	}\r\n" + 
				"	public A() {\r\n" + 
				"		for (String test : this.getCollection()) {\r\n" + 
				"			String backup= test;\r\n" + 
				"			System.out.println(backup);\r\n" + 
				"		}\r\n" + 
				"	}\r\n" + 
				"}");
		String expected= buf.toString();
		assertEqualString(preview, expected);
	}

	public void testNested() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\r\n" + 
				"import java.util.Collection;\r\n" + 
				"import java.util.Iterator;\r\n" + 
				"public class A {\r\n" + 
				"	public A() {\r\n" + 
				"		Collection<Collection<String>> cc= null;\r\n" + 
				"		for (final Iterator<Collection<String>> outer= cc.iterator(); outer.hasNext();) {\r\n" + 
				"			final Collection<String> c = outer.next();\r\n" + 
				"			for (final Iterator<String> inner= c.iterator(); inner.hasNext();) {\r\n" + 
				"				System.out.println(inner.next());\r\n" + 
				"			}\r\n" + 
				"		}\r\n" + 
				"	}\r\n" + 
				"}");
		ICompilationUnit unit= pack.createCompilationUnit("A.java", buf.toString(), false, null);

		List proposals= fetchConvertingProposal(buf, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
		buf.append("package test;\r\n" + 
				"import java.util.Collection;\r\n" + 
				"public class A {\r\n" + 
				"	public A() {\r\n" + 
				"		Collection<Collection<String>> cc= null;\r\n" + 
				"		for (Collection<String> c : cc) {\r\n" + 
				"			for (final Iterator<String> inner= c.iterator(); inner.hasNext();) {\r\n" + 
				"				System.out.println(inner.next());\r\n" + 
				"			}\r\n" + 
				"		}\r\n" + 
				"	}\r\n" + 
				"}");
		String expected= buf.toString();
		assertEqualString(preview, expected);
	}

	public void testMethodCall3() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\r\n" + 
				"import java.util.Collection;\r\n" + 
				"import java.util.Iterator;\r\n" + 
				"public class A {\r\n" + 
				"	Collection<String> c;\r\n" + 
				"	private Collection<String> getCollection() {\r\n" + 
				"		return c;\r\n" + 
				"	}\r\n" + 
				"	public A() {\r\n" + 
				"		for (final Iterator<String> iterator= new A().getCollection().iterator(); iterator.hasNext();) {\r\n" + 
				"			String test= iterator.next();\r\n" + 
				"			String backup= test;\r\n" + 
				"			System.out.println(backup);\r\n" + 
				"		}\r\n" + 
				"	}\r\n" + 
				"}");
		ICompilationUnit unit= pack.createCompilationUnit("A.java", buf.toString(), false, null);

		List proposals= fetchConvertingProposal(buf, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
		buf.append("package test;\r\n" + 
				"import java.util.Collection;\r\n" + 
				"public class A {\r\n" + 
				"	Collection<String> c;\r\n" + 
				"	private Collection<String> getCollection() {\r\n" + 
				"		return c;\r\n" + 
				"	}\r\n" + 
				"	public A() {\r\n" + 
				"		for (String test : new A().getCollection()) {\r\n" + 
				"			String backup= test;\r\n" + 
				"			System.out.println(backup);\r\n" + 
				"		}\r\n" + 
				"	}\r\n" + 
				"}");
		String expected= buf.toString();
		assertEqualString(preview, expected);
	}

	public void testNoAssignment() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\r\n" + 
				"import java.util.Collection;\r\n" + 
				"import java.util.Iterator;\r\n" + 
				"public class A {\r\n" + 
				"	public A() {\r\n" + 
				"		Collection<Collection<String>> cc= null;\r\n" + 
				"		for (final Iterator<Collection<String>> outer= cc.iterator(); outer.hasNext();) {\r\n" + 
				"			for (final Iterator<String> inner= outer.next().iterator(); inner.hasNext();) {\r\n" + 
				"				System.out.println(inner.next());\r\n" + 
				"			}\r\n" + 
				"		}\r\n" + 
				"	}\r\n" + 
				"}");
		ICompilationUnit unit= pack.createCompilationUnit("A.java", buf.toString(), false, null);

		List proposals= fetchConvertingProposal(buf, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
		buf.append("package test;\r\n" + 
				"import java.util.Collection;\r\n" + 
				"public class A {\r\n" + 
				"	public A() {\r\n" + 
				"		Collection<Collection<String>> cc= null;\r\n" + 
				"		for (Collection<String> name : cc) {\r\n" + 
				"			for (final Iterator<String> inner= name.iterator(); inner.hasNext();) {\r\n" + 
				"				System.out.println(inner.next());\r\n" + 
				"			}\r\n" + 
				"		}\r\n" + 
				"	}\r\n" + 
				"}");
		String expected= buf.toString();
		assertEqualString(preview, expected);
	}

	public void testOutsideAssignment1() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\r\n" + 
				"import java.util.Collection;\r\n" + 
				"import java.util.Iterator;\r\n" + 
				"public class A {\r\n" + 
				"	Collection<String> c;\r\n" + 
				"	public A() {\r\n" + 
				"		String test= null;\r\n" + 
				"		for (final Iterator<String> iterator= c.iterator(); iterator.hasNext();) {\r\n" + 
				"			test= iterator.next();\r\n" + 
				"			String backup= test;\r\n" + 
				"			System.out.println(backup);\r\n" + 
				"		}\r\n" + 
				"	}\r\n" + 
				"}");
		ICompilationUnit unit= pack.createCompilationUnit("A.java", buf.toString(), false, null);

		List proposals= fetchConvertingProposal(buf, unit);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	public void testOutsideAssignment2() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\r\n" + 
				"import java.util.Collection;\r\n" + 
				"import java.util.Iterator;\r\n" + 
				"public class A {\r\n" + 
				"	Collection<String> c;\r\n" + 
				"	public A() {\r\n" + 
				"		String test;\r\n" + 
				"		for (final Iterator<String> iterator= c.iterator(); iterator.hasNext();) {\r\n" + 
				"			test= iterator.next();\r\n" + 
				"			String backup= test;\r\n" + 
				"			System.out.println(backup);\r\n" + 
				"		}\r\n" + 
				"	}\r\n" + 
				"}");
		ICompilationUnit unit= pack.createCompilationUnit("A.java", buf.toString(), false, null);

		List proposals= fetchConvertingProposal(buf, unit);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}
	

	public void testWildcard1() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("a", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package a;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("  void f(List<? super Number> x){\n");
		buf.append("    for (Iterator<? super Number> iter = x.iterator(); iter.hasNext();) {\n");
		buf.append("    }\n");
		buf.append("  }\n");
		buf.append("}\n");
		ICompilationUnit unit= pack.createCompilationUnit("A.java", buf.toString(), false, null);

		List proposals= fetchConvertingProposal(buf, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
		buf.append("package a;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("  void f(List<? super Number> x){\n");
		buf.append("    for (Object number : x) {\n");
		buf.append("    }\n");
		buf.append("  }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview, expected);
	}
	
	public void testWildcard2() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("a", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package a;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("  void f(List<? extends Number> x){\n");
		buf.append("    for (Iterator<? extends Number> iter = x.iterator(); iter.hasNext();) {\n");
		buf.append("    }\n");
		buf.append("  }\n");
		buf.append("}\n");
		ICompilationUnit unit= pack.createCompilationUnit("A.java", buf.toString(), false, null);

		List proposals= fetchConvertingProposal(buf, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		buf= new StringBuffer();
		buf.append("package a;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("  void f(List<? extends Number> x){\n");
		buf.append("    for (Number number : x) {\n");
		buf.append("    }\n");
		buf.append("  }\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEqualString(preview, expected);
	}
	
	public void testBug129508_1() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(List<Integer> list) {\n");
		buf.append("       for (Iterator<Integer> iter = list.iterator(); iter.hasNext();) {\n");
		buf.append("            Integer id = iter.next();\n");
		buf.append("            iter.remove();\n");
		buf.append("       } \n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit unit= pack.createCompilationUnit("E1.java", buf.toString(), false, null);

		List proposals= fetchConvertingProposal(buf, unit);
		assertNull(fConvertLoopProposal);
		assertCorrectLabels(proposals);
	}
	
	public void testBug129508_2() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(List<Integer> list) {\n");
		buf.append("       for (Iterator<Integer> iter = list.iterator(); iter.hasNext();) {\n");
		buf.append("            Integer id = iter.next();\n");
		buf.append("            iter.next();\n");
		buf.append("       } \n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit unit= pack.createCompilationUnit("E1.java", buf.toString(), false, null);

		List proposals= fetchConvertingProposal(buf, unit);
		assertNull(fConvertLoopProposal);
		assertCorrectLabels(proposals);
	}
	
	public void testBug129508_3() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(List<Integer> list) {\n");
		buf.append("       for (Iterator<Integer> iter = list.iterator(); iter.hasNext();) {\n");
		buf.append("            Integer id = iter.next();\n");
		buf.append("            boolean x= iter.hasNext();\n");
		buf.append("       } \n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit unit= pack.createCompilationUnit("E1.java", buf.toString(), false, null);

		List proposals= fetchConvertingProposal(buf, unit);
		assertNull(fConvertLoopProposal);
		assertCorrectLabels(proposals);
	}
	
	public void testBug129508_4() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo(List<Integer> list) {\n");
		buf.append("       for (Iterator<Integer> iter = list.iterator(); iter.hasNext();) {\n");
		buf.append("            Integer id = iter.next();\n");
		buf.append("            Integer id2= iter.next();\n");
		buf.append("       } \n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit unit= pack.createCompilationUnit("E1.java", buf.toString(), false, null);

		List proposals= fetchConvertingProposal(buf, unit);
		assertNull(fConvertLoopProposal);
		assertCorrectLabels(proposals);
	}
}

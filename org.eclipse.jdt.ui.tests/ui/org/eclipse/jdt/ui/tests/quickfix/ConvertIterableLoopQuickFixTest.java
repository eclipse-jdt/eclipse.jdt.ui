/*******************************************************************************
 * Copyright (c) 2005, 2020 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.QuickAssistProcessor;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal;

public final class ConvertIterableLoopQuickFixTest extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup= new ProjectTestSetup();

	private FixCorrectionProposal fConvertLoopProposal;

	private IJavaProject fProject;

	private IPackageFragmentRoot fSourceFolder;

	private List<IJavaCompletionProposal> fetchConvertingProposal(String sample, ICompilationUnit cu) throws Exception {
		int offset= sample.indexOf("for");
		return fetchConvertingProposal(cu, offset);
	}

	private List<IJavaCompletionProposal> fetchConvertingProposal(ICompilationUnit cu, int offset) throws Exception {
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		fConvertLoopProposal= (FixCorrectionProposal)findProposalByCommandId(QuickAssistProcessor.CONVERT_FOR_LOOP_ID, proposals);
		return proposals;
	}

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fProject= projectSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fProject, "src");
		fConvertLoopProposal= null;
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fProject, projectSetup.getDefaultClasspath());
		fConvertLoopProposal= null;
		fProject= null;
		fSourceFolder= null;
	}

	@Test
	public void testSimplestSmokeCase() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\r\n" //
				+ "import java.util.Collection;\r\n" //
				+ "import java.util.Iterator;\r\n" //
				+ "public class A {\r\n" //
				+ "	Collection<String> c;\r\n" //
				+ "	public A() {\r\n" //
				+ "		for (final Iterator<String> iterator= c.iterator(); iterator.hasNext();) {\r\n" //
				+ "			String test= iterator.next();\r\n" //
				+ "			System.out.println(test);\r\n" //
				+ "		}\r\n" //
				+ "	}\r\n" //
				+ "}";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= "" //
				+ "package test;\r\n" //
				+ "import java.util.Collection;\r\n" //
				+ "public class A {\r\n" //
				+ "	Collection<String> c;\r\n" //
				+ "	public A() {\r\n" //
				+ "		for (String test : c) {\r\n" //
				+ "			System.out.println(test);\r\n" //
				+ "		}\r\n" //
				+ "	}\r\n" //
				+ "}";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testRawIterator() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "import java.util.Collection;\n" //
				+ "import java.util.Iterator;\n" //
				+ "\n" //
				+ "public class B {\n" //
				+ "  Collection c;\n" //
				+ "  public void useForeach() {\n" //
				+ "    for (final Iterator<?> iterator= c.iterator(); iterator.hasNext();) {\n" //
				+ "      Object test= iterator.next();\n" //
				+ "      System.out.println(test);\n" //
				+ "    }\n" //
				+ "  }\n" //
				+ "}\n";
		ICompilationUnit unit= pack.createCompilationUnit("B.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull("A quickfix should be proposed", fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "import java.util.Collection;\n" //
				+ "\n" //
				+ "public class B {\n" //
				+ "  Collection c;\n" //
				+ "  public void useForeach() {\n" //
				+ "    for (Object test : c) {\n" //
				+ "      System.out.println(test);\n" //
				+ "    }\n" //
				+ "  }\n" //
				+ "}\n";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testWrongInitializer() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "\n" //
				+ "import java.util.Collection;\n" //
				+ "import java.util.Iterator;\n" //
				+ "\n" //
				+ "public class A {\n" //
				+ "	Iterator<String> otherIterator;\n" //
				+ "	Iterator<String> anotherIterator;\n" //
				+ "	Collection<String> c;\n" //
				+ "\n" //
				+ "	public A() {\n" //
				+ "		for (final Iterator<String> iterator= (c.iterator() == null) ? otherIterator : anotherIterator; iterator.hasNext();) {\n" //
				+ "			String test= iterator.next();\n" //
				+ "			System.out.println(test);\n" //
				+ "		}\n" //
				+ "	}\n" //
				+ "}";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testChildren() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "import java.util.Collection;\n" //
				+ "import java.util.Iterator;\n" //
				+ "public class A {\n" //
				+ "	Collection<String> children;\n" //
				+ "	public A() {\n" //
				+ "		for (Iterator<String> iterator= children.iterator(); iterator.hasNext();) {\n" //
				+ "			System.out.println(iterator.next());\n" //
				+ "		}\n" //
				+ "	}\n" //
				+ "}";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= "" //
				+ "package test;\n" //
				+ "import java.util.Collection;\n" //
				+ "public class A {\n" //
				+ "	Collection<String> children;\n" //
				+ "	public A() {\n" //
				+ "		for (String child : children) {\n" //
				+ "			System.out.println(child);\n" //
				+ "		}\n" //
				+ "	}\n" //
				+ "}";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testBug553634() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\r\n" //
				+ "import java.util.Collection;\r\n" //
				+ "import java.util.Iterator;\r\n" //
				+ "public class A {\r\n" //
				+ "	Collection<String> c;\r\n" //
				+ "	public A() {\r\n" //
				+ "		for (final Iterator<String> iterator= c.iterator(); iterator.hasNext();) {\r\n" //
				+ "			// Comment line 1\r\n" //
				+ "			String test= iterator.next(); // Comment 2\r\n" //
				+ "			System.out.println(test); /* Comment 3 */\r\n" //
				+ "		}\r\n" //
				+ "	}\r\n" //
				+ "}";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= "" //
				+ "package test;\r\n" //
				+ "import java.util.Collection;\r\n" //
				+ "public class A {\r\n" //
				+ "	Collection<String> c;\r\n" //
				+ "	public A() {\r\n" //
				+ "		for (String test : c) {\r\n" //
				+ "			// Comment line 1\r\n" //
				+ "			System.out.println(test); /* Comment 3 */\r\n" //
				+ "		}\r\n" //
				+ "	}\r\n" //
				+ "}";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	/**
	 * quickfix creates strange indentation because of the return in the start statement
	 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=553635
	 * @throws Exception Any exception
	 */
	@Ignore("Bug 553635")
	@Test
	public void testIndentation() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\r\n" //
				+ "import java.util.Collection;\r\n" //
				+ "import java.util.Iterator;\r\n" //
				+ "public class A {\r\n" //
				+ "	Collection<String> c;\r\n" //
				+ "	public A() {\r\n" //
				+ "		for (final Iterator<String> iterator= c.\r\niterator(); iterator.hasNext();) {\r\n" //
				+ "			String test= iterator.next();\r\n" //
				+ "			System.out.println(test);\r\n" //
				+ "		}\r\n" //
				+ "	}\r\n" //
				+ "}";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= "" //
				+ "package test;\r\n" //
				+ "import java.util.Collection;\r\n" //
				+ "public class A {\r\n" //
				+ "	Collection<String> c;\r\n" //
				+ "	public A() {\r\n" //
				+ "		for (String test : c) {\r\n" //
				+ "			System.out.println(test);\r\n" //
				+ "		}\r\n" //
				+ "	}\r\n" //
				+ "}";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testEnumeration() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\r\n" //
				+ "import java.util.Enumeration;\r\n" //
				+ "import java.util.Vector;\r\n" //
				+ "public class A {\r\n" //
				+ "	Vector<String> c;\r\n" //
				+ "	public A() {\r\n" //
				+ "		for (Enumeration<String> e= c.elements(); e.hasMoreElements(); ) {\r\n" //
				+ "			String nextElement = e.nextElement();\r\n" //
				+ "			System.out.println(nextElement);\r\n" //
				+ "		}\r\n" //
				+ "	}\r\n" //
				+ "}";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= "" //
				+ "package test;\r\n" //
				+ "import java.util.Vector;\r\n" //
				+ "public class A {\r\n" //
				+ "	Vector<String> c;\r\n" //
				+ "	public A() {\r\n" //
				+ "		for (String nextElement : c) {\r\n" //
				+ "			System.out.println(nextElement);\r\n" //
				+ "		}\r\n" //
				+ "	}\r\n" //
				+ "}";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testSplitAssignment() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\r\n" //
				+ "import java.util.Collection;\r\n" //
				+ "import java.util.Iterator;\r\n" //
				+ "public class A {\r\n" //
				+ "	Collection<String> c;\r\n" //
				+ "	public A() {\r\n" //
				+ "		for (final Iterator<String> iterator= c.iterator(); iterator.hasNext();) {\r\n" //
				+ "			String test= null;\r\n" //
				+ "			test= iterator.next();\r\n" //
				+ "			System.out.println(test);\r\n" //
				+ "		}\r\n" //
				+ "	}\r\n" //
				+ "}";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= "" //
				+ "package test;\r\n" //
				+ "import java.util.Collection;\r\n" //
				+ "public class A {\r\n" //
				+ "	Collection<String> c;\r\n" //
				+ "	public A() {\r\n" //
				+ "		for (String test : c) {\r\n" //
				+ "			System.out.println(test);\r\n" //
				+ "		}\r\n" //
				+ "	}\r\n" //
				+ "}";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testIndirectUsage() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\r\n" //
				+ "import java.util.Collection;\r\n" //
				+ "import java.util.Iterator;\r\n" //
				+ "public class A {\r\n" //
				+ "	Collection<String> c;\r\n" //
				+ "	public A() {\r\n" //
				+ "		for (final Iterator<String> iterator= c.iterator(); iterator.hasNext();) {\r\n" //
				+ "			String test= null;\r\n" //
				+ "			test= iterator.next();\r\n" //
				+ "			String backup= test;\r\n" //
				+ "			System.out.println(backup);\r\n" //
				+ "		}\r\n" //
				+ "	}\r\n" //
				+ "}";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= "" //
				+ "package test;\r\n" //
				+ "import java.util.Collection;\r\n" //
				+ "public class A {\r\n" //
				+ "	Collection<String> c;\r\n" //
				+ "	public A() {\r\n" //
				+ "		for (String test : c) {\r\n" //
				+ "			String backup= test;\r\n" //
				+ "			System.out.println(backup);\r\n" //
				+ "		}\r\n" //
				+ "	}\r\n" //
				+ "}";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testMethodCall1() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\r\n" //
				+ "import java.util.Collection;\r\n" //
				+ "import java.util.Iterator;\r\n" //
				+ "public class A {\r\n" //
				+ "	Collection<String> c;\r\n" //
				+ "	private Collection<String> getCollection() {\r\n" //
				+ "		return c;\r\n" //
				+ "	}\r\n" //
				+ "	public A() {\r\n" //
				+ "		for (final Iterator<String> iterator= getCollection().iterator(); iterator.hasNext();) {\r\n" //
				+ "			String test= iterator.next();\r\n" //
				+ "			String backup= test;\r\n" //
				+ "			System.out.println(backup);\r\n" //
				+ "		}\r\n" //
				+ "	}\r\n" //
				+ "}";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= "" //
				+ "package test;\r\n" //
				+ "import java.util.Collection;\r\n" //
				+ "public class A {\r\n" //
				+ "	Collection<String> c;\r\n" //
				+ "	private Collection<String> getCollection() {\r\n" //
				+ "		return c;\r\n" //
				+ "	}\r\n" //
				+ "	public A() {\r\n" //
				+ "		for (String test : getCollection()) {\r\n" //
				+ "			String backup= test;\r\n" //
				+ "			System.out.println(backup);\r\n" //
				+ "		}\r\n" //
				+ "	}\r\n" //
				+ "}";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testMethodCall2() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\r\n" //
				+ "import java.util.Collection;\r\n" //
				+ "import java.util.Iterator;\r\n" //
				+ "public class A {\r\n" //
				+ "	Collection<String> c;\r\n" //
				+ "	private Collection<String> getCollection() {\r\n" //
				+ "		return c;\r\n" //
				+ "	}\r\n" //
				+ "	public A() {\r\n" //
				+ "		for (final Iterator<String> iterator= this.getCollection().iterator(); iterator.hasNext();) {\r\n" //
				+ "			String test= iterator.next();\r\n" //
				+ "			String backup= test;\r\n" //
				+ "			System.out.println(backup);\r\n" //
				+ "		}\r\n" //
				+ "	}\r\n" //
				+ "}";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= "" //
				+ "package test;\r\n" //
				+ "import java.util.Collection;\r\n" //
				+ "public class A {\r\n" //
				+ "	Collection<String> c;\r\n" //
				+ "	private Collection<String> getCollection() {\r\n" //
				+ "		return c;\r\n" //
				+ "	}\r\n" //
				+ "	public A() {\r\n" //
				+ "		for (String test : this.getCollection()) {\r\n" //
				+ "			String backup= test;\r\n" //
				+ "			System.out.println(backup);\r\n" //
				+ "		}\r\n" //
				+ "	}\r\n" //
				+ "}";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testNested() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\r\n" //
				+ "import java.util.Collection;\r\n" //
				+ "import java.util.Iterator;\r\n" //
				+ "public class A {\r\n" //
				+ "	public A() {\r\n" //
				+ "		Collection<Collection<String>> cc= null;\r\n" //
				+ "		for (final Iterator<Collection<String>> outer= cc.iterator(); outer.hasNext();) {\r\n" //
				+ "			final Collection<String> c = outer.next();\r\n" //
				+ "			for (final Iterator<String> inner= c.iterator(); inner.hasNext();) {\r\n" //
				+ "				System.out.println(inner.next());\r\n" //
				+ "			}\r\n" //
				+ "		}\r\n" //
				+ "	}\r\n" //
				+ "}";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= "" //
				+ "package test;\r\n" //
				+ "import java.util.Collection;\r\n" //
				+ "import java.util.Iterator;\r\n" //
				+ "public class A {\r\n" //
				+ "	public A() {\r\n" //
				+ "		Collection<Collection<String>> cc= null;\r\n" //
				+ "		for (Collection<String> c : cc) {\r\n" //
				+ "			for (final Iterator<String> inner= c.iterator(); inner.hasNext();) {\r\n" //
				+ "				System.out.println(inner.next());\r\n" //
				+ "			}\r\n" //
				+ "		}\r\n" //
				+ "	}\r\n" //
				+ "}";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testMethodCall3() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\r\n" //
				+ "import java.util.Collection;\r\n" //
				+ "import java.util.Iterator;\r\n" //
				+ "public class A {\r\n" //
				+ "	Collection<String> c;\r\n" //
				+ "	private Collection<String> getCollection() {\r\n" //
				+ "		return c;\r\n" //
				+ "	}\r\n" //
				+ "	public A() {\r\n" //
				+ "		for (final Iterator<String> iterator= new A().getCollection().iterator(); iterator.hasNext();) {\r\n" //
				+ "			String test= iterator.next();\r\n" //
				+ "			String backup= test;\r\n" //
				+ "			System.out.println(backup);\r\n" //
				+ "		}\r\n" //
				+ "	}\r\n" //
				+ "}";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= "" //
				+ "package test;\r\n" //
				+ "import java.util.Collection;\r\n" //
				+ "public class A {\r\n" //
				+ "	Collection<String> c;\r\n" //
				+ "	private Collection<String> getCollection() {\r\n" //
				+ "		return c;\r\n" //
				+ "	}\r\n" //
				+ "	public A() {\r\n" //
				+ "		for (String test : new A().getCollection()) {\r\n" //
				+ "			String backup= test;\r\n" //
				+ "			System.out.println(backup);\r\n" //
				+ "		}\r\n" //
				+ "	}\r\n" //
				+ "}";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testNoAssignment() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\r\n" //
				+ "import java.util.Collection;\r\n" //
				+ "import java.util.Iterator;\r\n" //
				+ "public class A {\r\n" //
				+ "	public A() {\r\n" //
				+ "		Collection<Collection<String>> cc= null;\r\n" //
				+ "		for (final Iterator<Collection<String>> outer= cc.iterator(); outer.hasNext();) {\r\n" //
				+ "			for (final Iterator<String> inner= outer.next().iterator(); inner.hasNext();) {\r\n" //
				+ "				System.out.println(inner.next());\r\n" //
				+ "			}\r\n" //
				+ "		}\r\n" //
				+ "	}\r\n" //
				+ "}";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= "" //
				+ "package test;\r\n" //
				+ "import java.util.Collection;\r\n" //
				+ "import java.util.Iterator;\r\n" //
				+ "public class A {\r\n" //
				+ "	public A() {\r\n" //
				+ "		Collection<Collection<String>> cc= null;\r\n" //
				+ "		for (Collection<String> collection : cc) {\r\n" //
				+ "			for (final Iterator<String> inner= collection.iterator(); inner.hasNext();) {\r\n" //
				+ "				System.out.println(inner.next());\r\n" //
				+ "			}\r\n" //
				+ "		}\r\n" //
				+ "	}\r\n" //
				+ "}";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testOutsideAssignment1() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\r\n" //
				+ "import java.util.Collection;\r\n" //
				+ "import java.util.Iterator;\r\n" //
				+ "public class A {\r\n" //
				+ "	Collection<String> c;\r\n" //
				+ "	public A() {\r\n" //
				+ "		String test= null;\r\n" //
				+ "		for (final Iterator<String> iterator= c.iterator(); iterator.hasNext();) {\r\n" //
				+ "			test= iterator.next();\r\n" //
				+ "			String backup= test;\r\n" //
				+ "			System.out.println(backup);\r\n" //
				+ "		}\r\n" //
				+ "	}\r\n" //
				+ "}";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testOutsideAssignment2() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= "" //
				+ "package test;\r\n" //
				+ "import java.util.Collection;\r\n" //
				+ "import java.util.Iterator;\r\n" //
				+ "public class A {\r\n" //
				+ "	Collection<String> c;\r\n" //
				+ "	public A() {\r\n" //
				+ "		String test;\r\n" //
				+ "		for (final Iterator<String> iterator= c.iterator(); iterator.hasNext();) {\r\n" //
				+ "			test= iterator.next();\r\n" //
				+ "			String backup= test;\r\n" //
				+ "			System.out.println(backup);\r\n" //
				+ "		}\r\n" //
				+ "	}\r\n" //
				+ "}";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testWildcard1() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("a", false, null);
		String sample= "" //
				+ "package a;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "public class A {\n" //
				+ "  void f(List<? super Number> x){\n" //
				+ "    for (Iterator<? super Number> iter = x.iterator(); iter.hasNext();) {\n" //
				+ "    }\n" //
				+ "  }\n" //
				+ "}\n";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= "" //
				+ "package a;\n" //
				+ "import java.util.List;\n" //
				+ "public class A {\n" //
				+ "  void f(List<? super Number> x){\n" //
				+ "    for (Object number : x) {\n" //
				+ "    }\n" //
				+ "  }\n" //
				+ "}\n";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testWildcard2() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("a", false, null);
		String sample= "" //
				+ "package a;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "public class A {\n" //
				+ "  void f(List<? extends Number> x){\n" //
				+ "    for (Iterator<? extends Number> iter = x.iterator(); iter.hasNext();) {\n" //
				+ "    }\n" //
				+ "  }\n" //
				+ "}\n";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= "" //
				+ "package a;\n" //
				+ "import java.util.List;\n" //
				+ "public class A {\n" //
				+ "  void f(List<? extends Number> x){\n" //
				+ "    for (Number number : x) {\n" //
				+ "    }\n" //
				+ "  }\n" //
				+ "}\n";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testBug129508_1() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(List<Integer> list) {\n" //
				+ "       for (Iterator<Integer> iter = list.iterator(); iter.hasNext();) {\n" //
				+ "            Integer id = iter.next();\n" //
				+ "            iter.remove();\n" //
				+ "       } \n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit unit= pack.createCompilationUnit("E1.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);
		assertNull(fConvertLoopProposal);
		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug129508_2() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(List<Integer> list) {\n" //
				+ "       for (Iterator<Integer> iter = list.iterator(); iter.hasNext();) {\n" //
				+ "            Integer id = iter.next();\n" //
				+ "            iter.next();\n" //
				+ "       } \n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit unit= pack.createCompilationUnit("E1.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);
		assertNull(fConvertLoopProposal);
		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug129508_3() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(List<Integer> list) {\n" //
				+ "       for (Iterator<Integer> iter = list.iterator(); iter.hasNext();) {\n" //
				+ "            Integer id = iter.next();\n" //
				+ "            boolean x= iter.hasNext();\n" //
				+ "       } \n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit unit= pack.createCompilationUnit("E1.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);
		assertNull(fConvertLoopProposal);
		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug129508_4() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(List<Integer> list) {\n" //
				+ "       for (Iterator<Integer> iter = list.iterator(); iter.hasNext();) {\n" //
				+ "            Integer id = iter.next();\n" //
				+ "            Integer id2= iter.next();\n" //
				+ "       } \n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit unit= pack.createCompilationUnit("E1.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);
		assertNull(fConvertLoopProposal);
		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug110599() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("a", false, null);
		String sample= "" //
				+ "package a;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "public class A {\n" //
				+ "    public void a(List<String> l) {\n" //
				+ "        //Comment\n" //
				+ "        for (Iterator<String> iterator = l.iterator(); iterator.hasNext();) {\n" //
				+ "            String str = iterator.next();\n" //
				+ "            System.out.println(str);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= "" //
				+ "package a;\n" //
				+ "import java.util.List;\n" //
				+ "public class A {\n" //
				+ "    public void a(List<String> l) {\n" //
				+ "        //Comment\n" //
				+ "        for (String str : l) {\n" //
				+ "            System.out.println(str);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testBug176595() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(List<Object> list1, List list2) {\n" //
				+ "        for (Iterator<?> it1 = list1.iterator(), it2 = null; it1.hasNext();) {\n" //
				+ "                Object e1 = it1.next();\n" //
				+ "                System.out.println(it2.toString());\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", sample, false, null);

		fetchConvertingProposal(sample, cu);

		assertNull(fConvertLoopProposal);
	}

	@Test
	public void testBug176502() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "import java.util.Vector;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(List<String> l) {\n" //
				+ "        for (Iterator<String> iterator = l.iterator(); iterator.hasNext();) {\n" //
				+ "            new Vector<String>();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit unit= pack.createCompilationUnit("E1.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.util.List;\n" //
				+ "import java.util.Vector;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(List<String> l) {\n" //
				+ "        for (String string : l) {\n" //
				+ "            new Vector<String>();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testBug203693() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.Collection;\n" //
				+ "import java.util.Iterator;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(Collection<String> col) {\n" //
				+ "        for (Iterator<String> iter = col.iterator(); iter.hasNext();) {\n" //
				+ "            String item = iter.next();\n" //
				+ "            System.out.println(item);\n" //
				+ "\n" //
				+ "            String dummy = null;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit unit= pack.createCompilationUnit("E1.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.util.Collection;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(Collection<String> col) {\n" //
				+ "        for (String item : col) {\n" //
				+ "            System.out.println(item);\n" //
				+ "\n" //
				+ "            String dummy = null;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testBug194639() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test;\n" //
				+ "import java.util.Collections;\n" //
				+ "import java.util.Iterator;\n" //
				+ "public class E01 {\n" //
				+ "    public void foo(Integer i) {\n" //
				+ "        for (Iterator iterator = Collections.singleton(i).iterator(); iterator.hasNext();) {\n" //
				+ "            Integer inter = (Integer) iterator.next();\n" //
				+ "            System.out.println(inter);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit unit= pack.createCompilationUnit("E1.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= "" //
				+ "package test;\n" //
				+ "import java.util.Collections;\n" //
				+ "public class E01 {\n" //
				+ "    public void foo(Integer i) {\n" //
				+ "        for (Object element : Collections.singleton(i)) {\n" //
				+ "            Integer inter = (Integer) element;\n" //
				+ "            System.out.println(inter);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testWrongIteratorMethod() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package snippet;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.Set;\n" //
				+ "interface NavigableSet<T> extends Set<T> {\n" //
				+ "    Iterator<?> descendingIterator();\n" //
				+ "}\n" //

				+ "public class Snippet {\n" //
				+ "    public static void main(String[] args) {\n" //
				+ "        NavigableSet<String> set= null;\n" //
				+ "        for (Iterator<?> it = set.descendingIterator(); it.hasNext();) {\n" //
				+ "            Object element = it.next();\n" //
				+ "            System.out.println(element);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";

		ICompilationUnit unit= pack.createCompilationUnit("E1.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal.getFixStatus());
		assertEquals(IStatus.WARNING, fConvertLoopProposal.getFixStatus().getCode());

		assertCorrectLabels(proposals);

		assertNotNull(fConvertLoopProposal.getStatusMessage());

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= "" //
				+ "package snippet;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.Set;\n" //
				+ "interface NavigableSet<T> extends Set<T> {\n" //
				+ "    Iterator<?> descendingIterator();\n" //
				+ "}\n" //

				+ "public class Snippet {\n" //
				+ "    public static void main(String[] args) {\n" //
				+ "        NavigableSet<String> set= null;\n" //
				+ "        for (Object element : set) {\n" //
				+ "            System.out.println(element);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";

		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testWrongIteratorMethod_bug411588() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package p;\n" //
				+ "import java.util.Iterator;\n" //
				+ "\n" //
				+ "public class TestSaveActionConvertToEnhancedForLoop {\n" //
				+ "    static class Something implements Iterable<Object>{\n" //
				+ "        @Override\n" //
				+ "        public Iterator<Object> iterator() {\n" //
				+ "            return null;\n" //
				+ "        }\n" //
				+ "        \n" //
				+ "        public Iterator<Object> iterator(int filter) {\n" //
				+ "            return null;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "    \n" //
				+ "    public static void main(String[] args) {          \n" //
				+ "        Something s = new Something();\n" //
				+ "        for (Iterator<Object> it = s.iterator(42) ; it.hasNext(); ) {\n" //
				+ "             Object obj = it.next();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";

		ICompilationUnit unit= pack.createCompilationUnit("TestSaveActionConvertToEnhancedForLoop.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal.getFixStatus());
		assertEquals(IStatus.WARNING, fConvertLoopProposal.getFixStatus().getCode());

		assertCorrectLabels(proposals);

		assertNotNull(fConvertLoopProposal.getStatusMessage());

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= "" //
				+ "package p;\n" //
				+ "import java.util.Iterator;\n" //
				+ "\n" //
				+ "public class TestSaveActionConvertToEnhancedForLoop {\n" //
				+ "    static class Something implements Iterable<Object>{\n" //
				+ "        @Override\n" //
				+ "        public Iterator<Object> iterator() {\n" //
				+ "            return null;\n" //
				+ "        }\n" //
				+ "        \n" //
				+ "        public Iterator<Object> iterator(int filter) {\n" //
				+ "            return null;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "    \n" //
				+ "    public static void main(String[] args) {          \n" //
				+ "        Something s = new Something();\n" //
				+ "        for (Object obj : s) {\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";

		String expected= sample;
		assertEqualString(preview, expected);

	}

	@Test
	public void testCorrectIteratorMethod() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package snippet;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.Set;\n" //
				+ "interface NavigableSet<T> extends Set<T> {\n" //
				+ "    Iterator<?> descendingIterator();\n" //
				+ "}\n" //

				+ "public class Snippet {\n" //
				+ "    public static void main(String[] args) {\n" //
				+ "        NavigableSet<String> set= null;\n" //
				+ "        for (Iterator<?> it = set.iterator(); it.hasNext();) {\n" //
				+ "            Object element = it.next();\n" //
				+ "            System.out.println(element);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";

		ICompilationUnit unit= pack.createCompilationUnit("E1.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal.getFixStatus());
		assertTrue(fConvertLoopProposal.getFixStatus().isOK());

		assertCorrectLabels(proposals);

		assertNotNull(fConvertLoopProposal.getStatusMessage());

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= "" //
				+ "package snippet;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.Set;\n" //
				+ "interface NavigableSet<T> extends Set<T> {\n" //
				+ "    Iterator<?> descendingIterator();\n" //
				+ "}\n" //

				+ "public class Snippet {\n" //
				+ "    public static void main(String[] args) {\n" //
				+ "        NavigableSet<String> set= null;\n" //
				+ "        for (Object element : set) {\n" //
				+ "            System.out.println(element);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";

		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void test487429() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package p;\n" //
				+ "\n" //
				+ "import java.util.HashMap;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.Map;\n" //
				+ "import java.util.Map.Entry;\n" //
				+ "\n" //
				+ "public class Snippet {\n" //
				+ "    private Map<Integer, String> fPositions= new HashMap<>();\n" //
				+ "    {\n" //
				+ "        for (Iterator<Entry<Integer, String>> it= fPositions.entrySet().iterator(); it.hasNext();) {\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";

		ICompilationUnit unit= pack.createCompilationUnit("Snippet.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal.getFixStatus());
		assertTrue(fConvertLoopProposal.getFixStatus().isOK());

		assertCorrectLabels(proposals);

		assertNotNull(fConvertLoopProposal.getStatusMessage());

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= "" //
				+ "package p;\n" //
				+ "\n" //
				+ "import java.util.HashMap;\n" //
				+ "import java.util.Map;\n" //
				+ "import java.util.Map.Entry;\n" //
				+ "\n" //
				+ "public class Snippet {\n" //
				+ "    private Map<Integer, String> fPositions= new HashMap<>();\n" //
				+ "    {\n" //
				+ "        for (Entry<Integer, String> entry : fPositions.entrySet()) {\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";

		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testBug510758_1() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(List<Integer> list) {\n" //
				+ "       for (int i=0; i<10; i++) {\n" //
				+ "           String tag= null;\n" //
				+ "           for (Iterator<String> iter = list.iterator(); iter.hasNext();) {\n" //
				+ "                tag = iter.next();\n" //
				+ "                System.out.print(tag);\n" //
				+ "           } \n" //
				+ "       } \n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit unit= pack.createCompilationUnit("E1.java", sample, false, null);

		int offset= sample.indexOf("for");
		offset= sample.indexOf("for", offset+1);
		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(unit, offset);
		assertNull(fConvertLoopProposal);
		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug510758_2() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.Iterator;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(List<String> list) {\n" //
				+ "       for (int i=0; i<10; i++) {\n" //
				+ "           for (Iterator<String> iter = list.iterator(); iter.hasNext();) {\n" //
				+ "                String tag = iter.next();\n" //
				+ "                System.out.print(tag);\n" //
				+ "           } \n" //
				+ "       } \n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit unit= pack.createCompilationUnit("E1.java", sample, false, null);

		int offset= sample.indexOf("for");
		offset= sample.indexOf("for", offset+1);
		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(unit, offset);
		assertNotNull(fConvertLoopProposal);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.util.List;\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(List<String> list) {\n" //
				+ "       for (int i=0; i<10; i++) {\n" //
				+ "           for (String tag : list) {\n" //
				+ "                System.out.print(tag);\n" //
				+ "           } \n" //
				+ "       } \n" //
				+ "    }\n" //
				+ "}\n";

		String expected= sample;
		assertEqualString(preview, expected);

		assertCorrectLabels(proposals);
	}
}

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
		String sample= """
			package test;\r
			import java.util.Collection;\r
			import java.util.Iterator;\r
			public class A {\r
				Collection<String> c;\r
				public A() {\r
					for (final Iterator<String> iterator= c.iterator(); iterator.hasNext();) {\r
						String test= iterator.next();\r
						System.out.println(test);\r
					}\r
				}\r
			}""";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= """
			package test;\r
			import java.util.Collection;\r
			public class A {\r
				Collection<String> c;\r
				public A() {\r
					for (String test : c) {\r
						System.out.println(test);\r
					}\r
				}\r
			}""";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testNextFromOtherClass() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;\r
			import java.util.Collection;\r
			import java.util.Iterator;\r
			public class A {\r
				Collection<String> c;\r
			public void foo() {\r
			    class Other {\r
			        public Object next() {\r
			            return null;\r
			        }\r
			    }\r
			    Other i = new Other();\r
			    for (Iterator<String> iterator = c.iterator(); iterator.hasNext();) {\r
			        i.next();\r
			        iterator.next();\r
			    }\r
			}\
			}""";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		String expected = """
			package test;\r
			import java.util.Collection;\r
			public class A {\r
				Collection<String> c;\r
			public void foo() {\r
			    class Other {\r
			        public Object next() {\r
			            return null;\r
			        }\r
			    }\r
			    Other i = new Other();\r
			    for (String string : c) {\r
			        i.next();\r
			    }\r
			}\
			}""";
		assertEqualString(preview, expected);
	}

	@Test
	public void testIteratorWithoutAssignment() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;\r
			import java.util.Collection;\r
			import java.util.Iterator;\r
			public class A {\r
				Collection<String> c;\r
				public A() {\r
					for (final Iterator<String> iterator= c.iterator(); iterator.hasNext();) {\r
						iterator.next();\r
						System.out.println("test");\r
					}\r
				}\r
			}""";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		String expected = """
			package test;\r
			import java.util.Collection;\r
			public class A {\r
				Collection<String> c;\r
				public A() {\r
					for (String string : c) {\r
						System.out.println("test");\r
					}\r
				}\r
			}""";
		assertEqualString(preview, expected);
	}

	@Test
	public void testIteratorWithParentMethodBinding() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;\r
			import java.util.Collection;\r
			import java.util.Iterator;\r
			public class A {\r
				Collection<String> c;\r
				public A() {\r
					for (final Iterator<String> iterator= c.iterator(); iterator.hasNext();) {\r
						iterator.next().toString();\r
						System.out.println("test");\r
					}\r
				}\r
			}""";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test;\r
			import java.util.Collection;\r
			public class A {\r
				Collection<String> c;\r
				public A() {\r
					for (String string : c) {\r
						string.toString();\r
						System.out.println("test");\r
					}\r
				}\r
			}""";
		assertEqualString(preview, expected);
	}

	@Test
	public void testCommentRetainedOnIteratorRemoval() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;\r
			import java.util.Collection;\r
			import java.util.Iterator;\r
			public class A {\r
				Collection<String> c;\r
				public A() {\r
					for (final Iterator<String> iterator= c.iterator(); iterator.hasNext();) {\r
						// comment\r
						iterator.next();\r
						System.out.println("test");\r
					}\r
				}\r
			}""";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		String expected = """
			package test;\r
			import java.util.Collection;\r
			public class A {\r
				Collection<String> c;\r
				public A() {\r
					for (String string : c) {\r
						// comment\r
						System.out.println("test");\r
					}\r
				}\r
			}""";
		assertEqualString(preview, expected);
	}

	@Test
	public void testRawIterator() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			
			import java.util.Collection;
			import java.util.Iterator;
			
			public class B {
			  Collection c;
			  public void useForeach() {
			    for (final Iterator<?> iterator= c.iterator(); iterator.hasNext();) {
			      Object test= iterator.next();
			      System.out.println(test);
			    }
			  }
			}
			""";
		ICompilationUnit unit= pack.createCompilationUnit("B.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull("A quickfix should be proposed", fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= """
			package test;
			
			import java.util.Collection;
			
			public class B {
			  Collection c;
			  public void useForeach() {
			    for (Object test : c) {
			      System.out.println(test);
			    }
			  }
			}
			""";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testWrongInitializer() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			
			import java.util.Collection;
			import java.util.Iterator;
			
			public class A {
				Iterator<String> otherIterator;
				Iterator<String> anotherIterator;
				Collection<String> c;
			
				public A() {
					for (final Iterator<String> iterator= (c.iterator() == null) ? otherIterator : anotherIterator; iterator.hasNext();) {
						String test= iterator.next();
						System.out.println(test);
					}
				}
			}""";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testChildren() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.Collection;
			import java.util.Iterator;
			public class A {
				Collection<String> children;
				public A() {
					for (Iterator<String> iterator= children.iterator(); iterator.hasNext();) {
						System.out.println(iterator.next());
					}
				}
			}""";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= """
			package test;
			import java.util.Collection;
			public class A {
				Collection<String> children;
				public A() {
					for (String child : children) {
						System.out.println(child);
					}
				}
			}""";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testBug553634() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;\r
			import java.util.Collection;\r
			import java.util.Iterator;\r
			public class A {\r
				Collection<String> c;\r
				public A() {\r
					for (final Iterator<String> iterator= c.iterator(); iterator.hasNext();) {\r
						// Comment line 1\r
						String test= iterator.next(); // Comment 2\r
						System.out.println(test); /* Comment 3 */\r
					}\r
				}\r
			}""";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= """
			package test;\r
			import java.util.Collection;\r
			public class A {\r
				Collection<String> c;\r
				public A() {\r
					for (String test : c) {\r
						// Comment line 1\r
						System.out.println(test); /* Comment 3 */\r
					}\r
				}\r
			}""";
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
		String sample= """
			package test;\r
			import java.util.Collection;\r
			import java.util.Iterator;\r
			public class A {\r
				Collection<String> c;\r
				public A() {\r
					for (final Iterator<String> iterator= c.\r
			iterator(); iterator.hasNext();) {\r
						String test= iterator.next();\r
						System.out.println(test);\r
					}\r
				}\r
			}""";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= """
			package test;\r
			import java.util.Collection;\r
			public class A {\r
				Collection<String> c;\r
				public A() {\r
					for (String test : c) {\r
						System.out.println(test);\r
					}\r
				}\r
			}""";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testEnumeration() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;\r
			import java.util.Enumeration;\r
			import java.util.Vector;\r
			public class A {\r
				Vector<String> c;\r
				public A() {\r
					for (Enumeration<String> e= c.elements(); e.hasMoreElements(); ) {\r
						String nextElement = e.nextElement();\r
						System.out.println(nextElement);\r
					}\r
				}\r
			}""";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= """
			package test;\r
			import java.util.Vector;\r
			public class A {\r
				Vector<String> c;\r
				public A() {\r
					for (String nextElement : c) {\r
						System.out.println(nextElement);\r
					}\r
				}\r
			}""";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testSplitAssignment() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;\r
			import java.util.Collection;\r
			import java.util.Iterator;\r
			public class A {\r
				Collection<String> c;\r
				public A() {\r
					for (final Iterator<String> iterator= c.iterator(); iterator.hasNext();) {\r
						String test= null;\r
						test= iterator.next();\r
						System.out.println(test);\r
					}\r
				}\r
			}""";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= """
			package test;\r
			import java.util.Collection;\r
			public class A {\r
				Collection<String> c;\r
				public A() {\r
					for (String test : c) {\r
						System.out.println(test);\r
					}\r
				}\r
			}""";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testIndirectUsage() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;\r
			import java.util.Collection;\r
			import java.util.Iterator;\r
			public class A {\r
				Collection<String> c;\r
				public A() {\r
					for (final Iterator<String> iterator= c.iterator(); iterator.hasNext();) {\r
						String test= null;\r
						test= iterator.next();\r
						String backup= test;\r
						System.out.println(backup);\r
					}\r
				}\r
			}""";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= """
			package test;\r
			import java.util.Collection;\r
			public class A {\r
				Collection<String> c;\r
				public A() {\r
					for (String test : c) {\r
						String backup= test;\r
						System.out.println(backup);\r
					}\r
				}\r
			}""";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testMethodCall1() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;\r
			import java.util.Collection;\r
			import java.util.Iterator;\r
			public class A {\r
				Collection<String> c;\r
				private Collection<String> getCollection() {\r
					return c;\r
				}\r
				public A() {\r
					for (final Iterator<String> iterator= getCollection().iterator(); iterator.hasNext();) {\r
						String test= iterator.next();\r
						String backup= test;\r
						System.out.println(backup);\r
					}\r
				}\r
			}""";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= """
			package test;\r
			import java.util.Collection;\r
			public class A {\r
				Collection<String> c;\r
				private Collection<String> getCollection() {\r
					return c;\r
				}\r
				public A() {\r
					for (String test : getCollection()) {\r
						String backup= test;\r
						System.out.println(backup);\r
					}\r
				}\r
			}""";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testMethodCall2() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;\r
			import java.util.Collection;\r
			import java.util.Iterator;\r
			public class A {\r
				Collection<String> c;\r
				private Collection<String> getCollection() {\r
					return c;\r
				}\r
				public A() {\r
					for (final Iterator<String> iterator= this.getCollection().iterator(); iterator.hasNext();) {\r
						String test= iterator.next();\r
						String backup= test;\r
						System.out.println(backup);\r
					}\r
				}\r
			}""";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= """
			package test;\r
			import java.util.Collection;\r
			public class A {\r
				Collection<String> c;\r
				private Collection<String> getCollection() {\r
					return c;\r
				}\r
				public A() {\r
					for (String test : this.getCollection()) {\r
						String backup= test;\r
						System.out.println(backup);\r
					}\r
				}\r
			}""";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testNested() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;\r
			import java.util.Collection;\r
			import java.util.Iterator;\r
			public class A {\r
				public A() {\r
					Collection<Collection<String>> cc= null;\r
					for (final Iterator<Collection<String>> outer= cc.iterator(); outer.hasNext();) {\r
						final Collection<String> c = outer.next();\r
						for (final Iterator<String> inner= c.iterator(); inner.hasNext();) {\r
							System.out.println(inner.next());\r
						}\r
					}\r
				}\r
			}""";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= """
			package test;\r
			import java.util.Collection;\r
			import java.util.Iterator;\r
			public class A {\r
				public A() {\r
					Collection<Collection<String>> cc= null;\r
					for (Collection<String> c : cc) {\r
						for (final Iterator<String> inner= c.iterator(); inner.hasNext();) {\r
							System.out.println(inner.next());\r
						}\r
					}\r
				}\r
			}""";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testMethodCall3() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;\r
			import java.util.Collection;\r
			import java.util.Iterator;\r
			public class A {\r
				Collection<String> c;\r
				private Collection<String> getCollection() {\r
					return c;\r
				}\r
				public A() {\r
					for (final Iterator<String> iterator= new A().getCollection().iterator(); iterator.hasNext();) {\r
						String test= iterator.next();\r
						String backup= test;\r
						System.out.println(backup);\r
					}\r
				}\r
			}""";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= """
			package test;\r
			import java.util.Collection;\r
			public class A {\r
				Collection<String> c;\r
				private Collection<String> getCollection() {\r
					return c;\r
				}\r
				public A() {\r
					for (String test : new A().getCollection()) {\r
						String backup= test;\r
						System.out.println(backup);\r
					}\r
				}\r
			}""";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testNoAssignment() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;\r
			import java.util.Collection;\r
			import java.util.Iterator;\r
			public class A {\r
				public A() {\r
					Collection<Collection<String>> cc= null;\r
					for (final Iterator<Collection<String>> outer= cc.iterator(); outer.hasNext();) {\r
						for (final Iterator<String> inner= outer.next().iterator(); inner.hasNext();) {\r
							System.out.println(inner.next());\r
						}\r
					}\r
				}\r
			}""";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= """
			package test;\r
			import java.util.Collection;\r
			import java.util.Iterator;\r
			public class A {\r
				public A() {\r
					Collection<Collection<String>> cc= null;\r
					for (Collection<String> collection : cc) {\r
						for (final Iterator<String> inner= collection.iterator(); inner.hasNext();) {\r
							System.out.println(inner.next());\r
						}\r
					}\r
				}\r
			}""";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testOutsideAssignment1() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;\r
			import java.util.Collection;\r
			import java.util.Iterator;\r
			public class A {\r
				Collection<String> c;\r
				public A() {\r
					String test= null;\r
					for (final Iterator<String> iterator= c.iterator(); iterator.hasNext();) {\r
						test= iterator.next();\r
						String backup= test;\r
						System.out.println(backup);\r
					}\r
				}\r
			}""";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testOutsideAssignment2() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;\r
			import java.util.Collection;\r
			import java.util.Iterator;\r
			public class A {\r
				Collection<String> c;\r
				public A() {\r
					String test;\r
					for (final Iterator<String> iterator= c.iterator(); iterator.hasNext();) {\r
						test= iterator.next();\r
						String backup= test;\r
						System.out.println(backup);\r
					}\r
				}\r
			}""";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testWildcard1() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("a", false, null);
		String sample= """
			package a;
			import java.util.Iterator;
			import java.util.List;
			public class A {
			  void f(List<? super Number> x){
			    for (Iterator<? super Number> iter = x.iterator(); iter.hasNext();) {
			    }
			  }
			}
			""";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= """
			package a;
			import java.util.List;
			public class A {
			  void f(List<? super Number> x){
			    for (Object number : x) {
			    }
			  }
			}
			""";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testWildcard2() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("a", false, null);
		String sample= """
			package a;
			import java.util.Iterator;
			import java.util.List;
			public class A {
			  void f(List<? extends Number> x){
			    for (Iterator<? extends Number> iter = x.iterator(); iter.hasNext();) {
			    }
			  }
			}
			""";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= """
			package a;
			import java.util.List;
			public class A {
			  void f(List<? extends Number> x){
			    for (Number number : x) {
			    }
			  }
			}
			""";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testBug129508_1() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.Iterator;
			import java.util.List;
			public class E1 {
			    public void foo(List<Integer> list) {
			       for (Iterator<Integer> iter = list.iterator(); iter.hasNext();) {
			            Integer id = iter.next();
			            iter.remove();
			       }\s
			    }
			}
			""";
		ICompilationUnit unit= pack.createCompilationUnit("E1.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);
		assertNull(fConvertLoopProposal);
		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug129508_2() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.Iterator;
			import java.util.List;
			public class E1 {
			    public void foo(List<Integer> list) {
			       for (Iterator<Integer> iter = list.iterator(); iter.hasNext();) {
			            Integer id = iter.next();
			            iter.next();
			       }\s
			    }
			}
			""";
		ICompilationUnit unit= pack.createCompilationUnit("E1.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);
		assertNull(fConvertLoopProposal);
		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug129508_3() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.Iterator;
			import java.util.List;
			public class E1 {
			    public void foo(List<Integer> list) {
			       for (Iterator<Integer> iter = list.iterator(); iter.hasNext();) {
			            Integer id = iter.next();
			            boolean x= iter.hasNext();
			       }\s
			    }
			}
			""";
		ICompilationUnit unit= pack.createCompilationUnit("E1.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);
		assertNull(fConvertLoopProposal);
		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug129508_4() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.Iterator;
			import java.util.List;
			public class E1 {
			    public void foo(List<Integer> list) {
			       for (Iterator<Integer> iter = list.iterator(); iter.hasNext();) {
			            Integer id = iter.next();
			            Integer id2= iter.next();
			       }\s
			    }
			}
			""";
		ICompilationUnit unit= pack.createCompilationUnit("E1.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);
		assertNull(fConvertLoopProposal);
		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug110599() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("a", false, null);
		String sample= """
			package a;
			import java.util.Iterator;
			import java.util.List;
			public class A {
			    public void a(List<String> l) {
			        //Comment
			        for (Iterator<String> iterator = l.iterator(); iterator.hasNext();) {
			            String str = iterator.next();
			            System.out.println(str);
			        }
			    }
			}
			""";
		ICompilationUnit unit= pack.createCompilationUnit("A.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= """
			package a;
			import java.util.List;
			public class A {
			    public void a(List<String> l) {
			        //Comment
			        for (String str : l) {
			            System.out.println(str);
			        }
			    }
			}
			""";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testBug176595() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test;
			import java.util.Iterator;
			import java.util.List;
			public class E1 {
			    public void foo(List<Object> list1, List list2) {
			        for (Iterator<?> it1 = list1.iterator(), it2 = null; it1.hasNext();) {
			                Object e1 = it1.next();
			                System.out.println(it2.toString());
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", sample, false, null);

		fetchConvertingProposal(sample, cu);

		assertNull(fConvertLoopProposal);
	}

	@Test
	public void testBug176502() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.Iterator;
			import java.util.List;
			import java.util.Vector;
			public class E1 {
			    public void foo(List<String> l) {
			        for (Iterator<String> iterator = l.iterator(); iterator.hasNext();) {
			            new Vector<String>();
			        }
			    }
			}
			""";
		ICompilationUnit unit= pack.createCompilationUnit("E1.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= """
			package test1;
			import java.util.List;
			import java.util.Vector;
			public class E1 {
			    public void foo(List<String> l) {
			        for (String string : l) {
			            new Vector<String>();
			        }
			    }
			}
			""";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testBug203693() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.Collection;
			import java.util.Iterator;
			
			public class E1 {
			    public void foo(Collection<String> col) {
			        for (Iterator<String> iter = col.iterator(); iter.hasNext();) {
			            String item = iter.next();
			            System.out.println(item);
			
			            String dummy = null;
			        }
			    }
			}
			""";
		ICompilationUnit unit= pack.createCompilationUnit("E1.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= """
			package test1;
			import java.util.Collection;
			
			public class E1 {
			    public void foo(Collection<String> col) {
			        for (String item : col) {
			            System.out.println(item);
			
			            String dummy = null;
			        }
			    }
			}
			""";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testBug194639() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test;
			import java.util.Collections;
			import java.util.Iterator;
			public class E01 {
			    public void foo(Integer i) {
			        for (Iterator iterator = Collections.singleton(i).iterator(); iterator.hasNext();) {
			            Integer inter = (Integer) iterator.next();
			            System.out.println(inter);
			        }
			    }
			}
			""";
		ICompilationUnit unit= pack.createCompilationUnit("E1.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= """
			package test;
			import java.util.Collections;
			public class E01 {
			    public void foo(Integer i) {
			        for (Object element : Collections.singleton(i)) {
			            Integer inter = (Integer) element;
			            System.out.println(inter);
			        }
			    }
			}
			""";
		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testWrongIteratorMethod() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package snippet;
			import java.util.Iterator;
			import java.util.Set;
			interface NavigableSet<T> extends Set<T> {
			    Iterator<?> descendingIterator();
			}
			public class Snippet {
			    public static void main(String[] args) {
			        NavigableSet<String> set= null;
			        for (Iterator<?> it = set.descendingIterator(); it.hasNext();) {
			            Object element = it.next();
			            System.out.println(element);
			        }
			    }
			}
			""";

		ICompilationUnit unit= pack.createCompilationUnit("E1.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal.getFixStatus());
		assertEquals(IStatus.WARNING, fConvertLoopProposal.getFixStatus().getCode());

		assertCorrectLabels(proposals);

		assertNotNull(fConvertLoopProposal.getStatusMessage());

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= """
			package snippet;
			import java.util.Iterator;
			import java.util.Set;
			interface NavigableSet<T> extends Set<T> {
			    Iterator<?> descendingIterator();
			}
			public class Snippet {
			    public static void main(String[] args) {
			        NavigableSet<String> set= null;
			        for (Object element : set) {
			            System.out.println(element);
			        }
			    }
			}
			""";

		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testWrongIteratorMethod_bug411588() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package p;
			import java.util.Iterator;
			
			public class TestSaveActionConvertToEnhancedForLoop {
			    static class Something implements Iterable<Object>{
			        @Override
			        public Iterator<Object> iterator() {
			            return null;
			        }
			       \s
			        public Iterator<Object> iterator(int filter) {
			            return null;
			        }
			    }
			   \s
			    public static void main(String[] args) {         \s
			        Something s = new Something();
			        for (Iterator<Object> it = s.iterator(42) ; it.hasNext(); ) {
			             Object obj = it.next();
			        }
			    }
			}
			""";

		ICompilationUnit unit= pack.createCompilationUnit("TestSaveActionConvertToEnhancedForLoop.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal.getFixStatus());
		assertEquals(IStatus.WARNING, fConvertLoopProposal.getFixStatus().getCode());

		assertCorrectLabels(proposals);

		assertNotNull(fConvertLoopProposal.getStatusMessage());

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= """
			package p;
			import java.util.Iterator;
			
			public class TestSaveActionConvertToEnhancedForLoop {
			    static class Something implements Iterable<Object>{
			        @Override
			        public Iterator<Object> iterator() {
			            return null;
			        }
			       \s
			        public Iterator<Object> iterator(int filter) {
			            return null;
			        }
			    }
			   \s
			    public static void main(String[] args) {         \s
			        Something s = new Something();
			        for (Object obj : s) {
			        }
			    }
			}
			""";

		String expected= sample;
		assertEqualString(preview, expected);

	}

	@Test
	public void testCorrectIteratorMethod() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package snippet;
			import java.util.Iterator;
			import java.util.Set;
			interface NavigableSet<T> extends Set<T> {
			    Iterator<?> descendingIterator();
			}
			public class Snippet {
			    public static void main(String[] args) {
			        NavigableSet<String> set= null;
			        for (Iterator<?> it = set.iterator(); it.hasNext();) {
			            Object element = it.next();
			            System.out.println(element);
			        }
			    }
			}
			""";

		ICompilationUnit unit= pack.createCompilationUnit("E1.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal.getFixStatus());
		assertTrue(fConvertLoopProposal.getFixStatus().isOK());

		assertCorrectLabels(proposals);

		assertNotNull(fConvertLoopProposal.getStatusMessage());

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= """
			package snippet;
			import java.util.Iterator;
			import java.util.Set;
			interface NavigableSet<T> extends Set<T> {
			    Iterator<?> descendingIterator();
			}
			public class Snippet {
			    public static void main(String[] args) {
			        NavigableSet<String> set= null;
			        for (Object element : set) {
			            System.out.println(element);
			        }
			    }
			}
			""";

		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void test487429() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package p;
			
			import java.util.HashMap;
			import java.util.Iterator;
			import java.util.Map;
			import java.util.Map.Entry;
			
			public class Snippet {
			    private Map<Integer, String> fPositions= new HashMap<>();
			    {
			        for (Iterator<Entry<Integer, String>> it= fPositions.entrySet().iterator(); it.hasNext();) {
			        }
			    }
			}
			""";

		ICompilationUnit unit= pack.createCompilationUnit("Snippet.java", sample, false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(sample, unit);

		assertNotNull(fConvertLoopProposal.getFixStatus());
		assertTrue(fConvertLoopProposal.getFixStatus().isOK());

		assertCorrectLabels(proposals);

		assertNotNull(fConvertLoopProposal.getStatusMessage());

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= """
			package p;
			
			import java.util.HashMap;
			import java.util.Map;
			import java.util.Map.Entry;
			
			public class Snippet {
			    private Map<Integer, String> fPositions= new HashMap<>();
			    {
			        for (Entry<Integer, String> entry : fPositions.entrySet()) {
			        }
			    }
			}
			""";

		String expected= sample;
		assertEqualString(preview, expected);
	}

	@Test
	public void testBug510758_1() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.Iterator;
			import java.util.List;
			public class E1 {
			    public void foo(List<Integer> list) {
			       for (int i=0; i<10; i++) {
			           String tag= null;
			           for (Iterator<String> iter = list.iterator(); iter.hasNext();) {
			                tag = iter.next();
			                System.out.print(tag);
			           }\s
			       }\s
			    }
			}
			""";
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
		String sample= """
			package test1;
			import java.util.Iterator;
			import java.util.List;
			public class E1 {
			    public void foo(List<String> list) {
			       for (int i=0; i<10; i++) {
			           for (Iterator<String> iter = list.iterator(); iter.hasNext();) {
			                String tag = iter.next();
			                System.out.print(tag);
			           }\s
			       }\s
			    }
			}
			""";
		ICompilationUnit unit= pack.createCompilationUnit("E1.java", sample, false, null);

		int offset= sample.indexOf("for");
		offset= sample.indexOf("for", offset+1);
		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(unit, offset);
		assertNotNull(fConvertLoopProposal);

		String preview= getPreviewContent(fConvertLoopProposal);

		sample= """
			package test1;
			import java.util.List;
			public class E1 {
			    public void foo(List<String> list) {
			       for (int i=0; i<10; i++) {
			           for (String tag : list) {
			                System.out.print(tag);
			           }\s
			       }\s
			    }
			}
			""";

		String expected= sample;
		assertEqualString(preview, expected);

		assertCorrectLabels(proposals);
	}
}

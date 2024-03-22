/*******************************************************************************
 * Copyright (c) 2014, 2020 GK Software AG, IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann <stephan.herrmann@berlin.de> - initial API and implementation - https://bugs.eclipse.org/425183
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.contentassist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.NullTestUtils;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.java.JavaLambdaCompletionProposal;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
public class CodeCompletionTest1d8 extends AbstractCompletionTest {
	@Rule
	public Java1d8ProjectTestSetup j18s= new Java1d8ProjectTestSetup();

	private IJavaProject fJProject1;

	@Override
	public void setUp() throws Exception {
		fJProject1= j18s.getProject();

		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "1");
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, true);
		store.setValue(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS, false);
		store.setValue(PreferenceConstants.CODEASSIST_SHOW_VISIBLE_PROPOSALS, false);

		StubUtility.setCodeTemplate(CodeTemplateContextType.OVERRIDECOMMENT_ID, "/* (non-Javadoc)\n * ${see_to_overridden}\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.DELEGATECOMMENT_ID, "/* (non-Javadoc)\n * ${see_to_target}\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "//TODO\n${body_statement}", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORCOMMENT_ID, "/**\n * Constructor.\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODCOMMENT_ID, "/**\n * Method.\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "//TODO\n${body_statement}", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.GETTERCOMMENT_ID, "/**\n * @return the ${bare_field_name}\n */", fJProject1);
		StubUtility.setCodeTemplate(CodeTemplateContextType.SETTERCOMMENT_ID, "/**\n * @param ${param} the ${bare_field_name} to set\n */", fJProject1);
	}

	@Override
	public void tearDown() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setToDefault(PreferenceConstants.CODEGEN_ADD_COMMENTS);
		store.setToDefault(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS);
		store.setToDefault(PreferenceConstants.CODEASSIST_SHOW_VISIBLE_PROPOSALS);
		closeAllEditors();
		JavaProjectHelper.clear(fJProject1, j18s.getDefaultClasspath());
	}

	public static void closeEditor(IEditorPart editor) {
		IWorkbenchPartSite site;
		IWorkbenchPage page;
		if (editor != null && (site= editor.getSite()) != null && (page= site.getPage()) != null)
			page.closeEditor(editor, false);
	}

	public static void closeAllEditors() {
		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (IWorkbenchPage page : window.getPages()) {
				for (IEditorReference editorReference : page.getEditorReferences()) {
					closeEditor(editorReference.getEditor(false));
				}
			}
		}
	}

	private CompletionProposalCollector createCollector(ICompilationUnit cu, int offset) throws PartInitException, JavaModelException {
		CompletionProposalCollector collector= new CompletionProposalCollector(cu);
		collector.setInvocationContext(createContext(offset, cu));
		return collector;
	}

	private JavaContentAssistInvocationContext createContext(int offset, ICompilationUnit cu) throws PartInitException, JavaModelException {
		JavaEditor editor= (JavaEditor) JavaUI.openInEditor(cu);
		ISourceViewer viewer= editor.getViewer();
		return new JavaContentAssistInvocationContext(viewer, offset, editor);
	}

	private void codeComplete(ICompilationUnit cu, int offset, CompletionProposalCollector collector) throws JavaModelException {
		cu.codeComplete(offset, collector, new NullProgressMonitor());
	}

	@Test
	public void testBug425183_comment8() throws CoreException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents=
			"""
			package test1
			public class X {
			    void foo() {
			        java.util.Comparator.reverseOrder().  // content assist after '.' => NPE
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("X.java", contents, false, null);


		String str= "java.util.Comparator.reverseOrder().";

		int offset= contents.indexOf(str) + str.length();

		CompletionProposalCollector collector= createCollector(cu, offset);

		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		ICompletionProposal proposal= null;

		for (IJavaCompletionProposal curr : collector.getJavaCompletionProposals()) {
			if (curr.getDisplayString().startsWith("thenComparingLong")) {
				assertNull("more than one proposal for thenComparingLong()", proposal);
				proposal= curr;
			}
		}
		assertNotNull("no proposal for thenComparingLong()", proposal);
		IEditorPart part= JavaUI.openInEditor(cu);
		IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
		proposal.apply(doc);

		String expectedContents=
				"""
			package test1
			public class X {
			    void foo() {
			        java.util.Comparator.reverseOrder().thenComparingLong()  // content assist after '.' => NPE
			    }
			}
			""";
		assertEquals(expectedContents, doc.get());
	}

	@Test
	public void testOverride1() throws CoreException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class Main {
			    private static class Cls implements Interface {
			        hello
			    }
			    private static interface Interface {
			        default void hello() {
			            System.out.println("Hello");
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Main.java", str1, false, null);

		String str= "hello";
		int offset= str1.indexOf(str) + str.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);
		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();
		assertEquals(1, proposals.length);
		IEditorPart part= JavaUI.openInEditor(cu);
		IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
		proposals[0].apply(doc);

		String str2= """
			package test1;
			public class Main {
			    private static class Cls implements Interface {
			        /* (non-Javadoc)
			         * @see test1.Main.Interface#hello()
			         */
			        @Override
			        public void hello() {
			            //TODO
			            Interface.super.hello();
			        }
			    }
			    private static interface Interface {
			        default void hello() {
			            System.out.println("Hello");
			        }
			    }
			}
			""";
		assertEquals(str2, doc.get());
	}

	@Test
	public void testOverride2() throws CoreException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class Main {
			    private static interface Bar extends Foo {
			        hello
			    }
			    private static interface Foo {
			        default void hello() {
			            System.out.println("Hello");
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Main.java", str1, false, null);

		String str= "hello";
		int offset= str1.indexOf(str) + str.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);
		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();
		assertEquals(1, proposals.length);
		IEditorPart part= JavaUI.openInEditor(cu);
		IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
		proposals[0].apply(doc);

		String str2= """
			package test1;
			public class Main {
			    private static interface Bar extends Foo {
			        /* (non-Javadoc)
			         * @see test1.Main.Foo#hello()
			         */
			        @Override
			        default void hello() {
			            //TODO
			            Foo.super.hello();
			        }
			    }
			    private static interface Foo {
			        default void hello() {
			            System.out.println("Hello");
			        }
			    }
			}
			""";
		assertEquals(str2, doc.get());
	}

	@Test
	public void testOverride3() throws CoreException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public interface I {
			    default int getSize(String name) {
			        return name.length();
			    }
			}
			interface I2 extends I {
			    getSize
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("I.java", str1, false, null);

		String str= "getSize";
		int offset= str1.lastIndexOf(str) + str.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);
		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();
		assertEquals(1, proposals.length);
		IEditorPart part= JavaUI.openInEditor(cu);
		IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
		proposals[0].apply(doc);

		String str2= """
			package test1;
			public interface I {
			    default int getSize(String name) {
			        return name.length();
			    }
			}
			interface I2 extends I {
			    /* (non-Javadoc)
			     * @see test1.I#getSize(java.lang.String)
			     */
			    @Override
			    default int getSize(String name) {
			        //TODO
			        return I.super.getSize(name);
			    }
			}
			""";
		assertEquals(str2, doc.get());
	}

	@Test
	public void testOverride0() throws CoreException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			import java.util.Collection;
			abstract class X implements Collection<Integer> {
			    parallelStre
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("X.java", str1, false, null);

		String str= "parallelStre";
		int offset= str1.lastIndexOf(str) + str.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);
		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();
		IEditorPart part= JavaUI.openInEditor(cu);
		IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
		proposals[0].apply(doc);

		String str2= """
			package test1;
			import java.util.Collection;
			import java.util.stream.Stream;
			abstract class X implements Collection<Integer> {
			    /* (non-Javadoc)
			     * @see java.util.Collection#parallelStream()
			     */
			    @Override
			    public Stream<Integer> parallelStream() {
			        //TODO
			        return Collection.super.parallelStream();
			    }
			}
			""";
		assertEquals(str2, doc.get());
	}

	@Test
	public void testOverride5() throws CoreException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			
			public interface I1 {
			    equals
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("I1.java", str1, false, null);

		String str= "equals";
		int offset= str1.indexOf(str) + str.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);
		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();
		assertEquals(1, proposals.length);
		IEditorPart part= JavaUI.openInEditor(cu);
		IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
		proposals[0].apply(doc);

		String str2= """
			package test1;
			
			public interface I1 {
			    /* (non-Javadoc)
			     * @see java.lang.Object#equals(java.lang.Object)
			     */
			    @Override
			    boolean equals(Object arg0);
			}
			""";
		assertEquals(str2, doc.get());
	}

	@Test
	public void testOverride6() throws CoreException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			
			public interface I2 {
			    hashCode
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("I2.java", str1, false, null);

		String str= "hashCode";
		int offset= str1.indexOf(str) + str.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);
		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();
		assertEquals(1, proposals.length);
		IEditorPart part= JavaUI.openInEditor(cu);
		IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
		proposals[0].apply(doc);

		String str2= """
			package test1;
			
			public interface I2 {
			    /* (non-Javadoc)
			     * @see java.lang.Object#hashCode()
			     */
			    @Override
			    int hashCode();
			}
			""";
		assertEquals(str2, doc.get());
	}

	@Test
	public void testOverride7() throws CoreException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			
			public interface I3 {
			    toString
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("I3.java", str1, false, null);

		String str= "toString";
		int offset= str1.indexOf(str) + str.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);
		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();
		assertEquals(1, proposals.length);
		IEditorPart part= JavaUI.openInEditor(cu);
		IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
		proposals[0].apply(doc);

		String str2= """
			package test1;
			
			public interface I3 {
			    /* (non-Javadoc)
			     * @see java.lang.Object#toString()
			     */
			    @Override
			    String toString();
			}
			""";
		assertEquals(str2, doc.get());
	}

	@Test
	public void testOverride8() throws CoreException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			
			public interface I4 {
			    clone
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("I4.java", str1, false, null);

		String str= "clone";
		int offset= str1.indexOf(str) + str.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);
		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();
		assertEquals(3, proposals.length);
		IEditorPart part= JavaUI.openInEditor(cu);
		IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
		proposals[2].apply(doc);

		String str2= """
			package test1;
			
			public interface I4 {
			    /* (non-Javadoc)
			     * @see java.lang.Object#clone()
			     */
			    Object clone() throws CloneNotSupportedException;
			}
			""";
		assertEquals(str2, doc.get());
	}

	@Test
	public void testOverride9() throws CoreException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			
			public interface I5 {
			    finalize
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("I5.java", str1, false, null);

		String str= "finalize";
		int offset= str1.indexOf(str) + str.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);
		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();
		assertEquals(2, proposals.length);
		IEditorPart part= JavaUI.openInEditor(cu);
		IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
		proposals[1].apply(doc);

		String str2= """
			package test1;
			
			public interface I5 {
			    /* (non-Javadoc)
			     * @see java.lang.Object#finalize()
			     */
			    void finalize() throws Throwable;
			}
			""";
		assertEquals(str2, doc.get());
	}

	@Test
	public void testOverride10() throws CoreException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			
			public interface I6 extends A {
			    myAbs
			}
			
			interface A {
			    void myAbstractM();
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("I6.java", str1, false, null);

		String str= "myAbs";
		int offset= str1.indexOf(str) + str.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);
		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();
		assertEquals(2, proposals.length);
		IEditorPart part= JavaUI.openInEditor(cu);
		IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
		proposals[0].apply(doc);

		String str2= """
			package test1;
			
			public interface I6 extends A {
			    /* (non-Javadoc)
			     * @see test1.A#myAbstractM()
			     */
			    @Override
			    default void myAbstractM() {
			        //TODO
			       \s
			    }
			}
			
			interface A {
			    void myAbstractM();
			}
			""";
		assertEquals(str2, doc.get());
	}

	@Test
	public void testOverride11() throws CoreException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= sourceFolder.createPackageFragment("pp", false, null);
		String str1= """
			package pp;
			
			public class CC implements I2 {
			    dispose
			}
			interface I2 {
			    default void dispose2() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("CC.java", str1, false, null);

		String str= "dispose";
		int offset= str1.indexOf(str) + str.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);
		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();
		assertEquals(2, proposals.length);
		IEditorPart part= JavaUI.openInEditor(cu);
		IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());

		proposals[0].apply(doc);
		String str2= """
			package pp;
			
			public class CC implements I2 {
			    /* (non-Javadoc)
			     * @see pp.I2#dispose2()
			     */
			    @Override
			    public void dispose2() {
			        //TODO
			        I2.super.dispose2();
			    }
			}
			interface I2 {
			    default void dispose2() {
			    }
			}
			""";
		assertEquals(str2, doc.get());
	}

	@Test
	public void testOverride12() throws CoreException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= sourceFolder.createPackageFragment("pp", false, null);
		String str1= """
			package pp;
			
			public class CC extends S1 {
			    dispose
			}
			class S1 {
			    void dispose1() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("CC.java", str1, false, null);

		String str= "dispose";
		int offset= str1.indexOf(str) + str.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);
		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();
		assertEquals(2, proposals.length);
		IEditorPart part= JavaUI.openInEditor(cu);
		IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());

		proposals[0].apply(doc);
		String str2= """
			package pp;
			
			public class CC extends S1 {
			    /* (non-Javadoc)
			     * @see pp.S1#dispose1()
			     */
			    @Override
			    void dispose1() {
			        //TODO
			        super.dispose1();
			    }
			}
			class S1 {
			    void dispose1() {
			    }
			}
			""";
		assertEquals(str2, doc.get());
	}

	@Test
	public void testOverride13() throws CoreException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= sourceFolder.createPackageFragment("pp", false, null);
		String str1= """
			package pp;
			
			public class CC extends S1 {
			    dispose
			}
			class S1 implements I1 {
			    void dispose1() {
			    }
			}
			interface I1 {
			    default void dispose() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("CC.java", str1, false, null);

		String str= "dispose";
		int offset= str1.indexOf(str) + str.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);
		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();
		assertEquals(2, proposals.length);
		IEditorPart part= JavaUI.openInEditor(cu);
		IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());

		proposals[1].apply(doc);
		String str2= """
			package pp;
			
			public class CC extends S1 {
			    /* (non-Javadoc)
			     * @see pp.I1#dispose()
			     */
			    @Override
			    public void dispose() {
			        //TODO
			        super.dispose();
			    }
			}
			class S1 implements I1 {
			    void dispose1() {
			    }
			}
			interface I1 {
			    default void dispose() {
			    }
			}
			""";
		assertEquals(str2, doc.get());
	}
	private static void assertNumberOf(String name, int is, int expected) {
		assertEquals("Wrong number of " + name + ", is: " + is + ", expected: " + expected, expected, is);
	}

	@Test
	public void testBug528871() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		NullTestUtils.prepareNullTypeAnnotations(sourceFolder);
		try {
			IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
			String contents= """
				package test1;
				
				import annots.NonNull;
				import annots.NonNullByDefault;
				
				interface A {
				    @NonNull String m();
				}
				
				@NonNullByDefault
				public class Test {
				    void f() {
				        new A()
				    }
				}
				""";

			ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

			String str= "new A(";

			int offset= contents.indexOf(str) + str.length();

			CompletionProposalCollector collector= createCollector(cu, offset);
			collector.setReplacementLength(0);

			codeComplete(cu, offset, collector);

			IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

			assertNumberOf("proposals", proposals.length, 1);

			IDocument doc= new Document(contents);

			proposals[0].apply(doc);

			String str1= """
				package test1;
				
				import annots.NonNull;
				import annots.NonNullByDefault;
				
				interface A {
				    @NonNull String m();
				}
				
				@NonNullByDefault
				public class Test {
				    void f() {
				        new A() {
				           \s
				            @Override
				            public String m() {
				                //TODO
				                return null;
				            }
				        }
				    }
				}
				""";
			assertEquals(str1, doc.get());
		} finally {
			NullTestUtils.disableAnnotationBasedNullAnalysis(sourceFolder);
		}
	}

	@Test
	public void testBug458321() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			
			import java.util.function.Consumer;
			
			public class A {
			    public void foo() {
			        Runnable r = () -> {
			        };
			        add(o -> r.)
			    }
			    static void add(Consumer<Object> consumer) {}
			}
			""";

		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

		IEditorPart part= JavaUI.openInEditor(cu);
		try {
			String str= "r.";

			int offset= contents.indexOf(str) + 2;

			CompletionProposalCollector collector= createCollector(cu, offset);
			collector.setReplacementLength(0);

			codeComplete(cu, offset, collector);

			IJavaCompletionProposal proposal= null;

			for (IJavaCompletionProposal p : collector.getJavaCompletionProposals()) {
				if (p.getDisplayString().startsWith("run")) {
					proposal= p;
				}
			}
			assertNotNull("no proposal for run()", proposal);

			IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			proposal.apply(doc);

			String str1= """
				package test1;
				
				import java.util.function.Consumer;
				
				public class A {
				    public void foo() {
				        Runnable r = () -> {
				        };
				        add(o -> r.run())
				    }
				    static void add(Consumer<Object> consumer) {}
				}
				""";
			assertEquals(str1, doc.get());
		} finally {
			part.getSite().getPage().closeAllEditors(false);
		}
	}

	@Test
	public void testBug575377() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			
			class Strong {}
			public class A {
			    void lambda(Object o) {
			        Runnable r = () -> ((Stron))
			    }
			}
			""";

		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

		IEditorPart part= JavaUI.openInEditor(cu);
		try {
			String str= "(Stron";

			int offset= contents.indexOf(str) + str.length();

			CompletionProposalCollector collector= createCollector(cu, offset);
			collector.setReplacementLength(0);

			codeComplete(cu, offset, collector);

			IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();
			for (IJavaCompletionProposal iJavaCompletionProposal : proposals) {
				System.out.println(iJavaCompletionProposal);
			}
			assertEquals("number of proposals", 1, proposals.length);

			IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			IJavaCompletionProposal proposal= proposals[0];
			assertTrue("valid proposal", ((ICompletionProposalExtension2)proposal).validate(doc, offset, null));
			proposal.apply(doc);

			String str1= """
				package test1;
				
				class Strong {}
				public class A {
				    void lambda(Object o) {
				        Runnable r = () -> ((Strong))
				    }
				}
				""";
			assertEquals(str1, doc.get());
		} finally {
			part.getSite().getPage().closeAllEditors(false);
		}
	}

	@Test
	public void testBug443091_expectLambdaExpressionCompletion_onArgumentAssignment() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			
			import java.util.function.Consumer;
			public class Bug443091 {
				void foo(Consumer<Integer> c){}
			    void test() {
			        this.foo()
			    }
			}
			""";

		ICompilationUnit cu= pack1.createCompilationUnit("Bug443091.java", contents, false, null);

		IEditorPart part= JavaUI.openInEditor(cu);
		try {
			String str= "this.foo(";

			int offset= contents.indexOf(str) + str.length();

			CompletionProposalCollector collector= createCollector(cu, offset);
			collector.setReplacementLength(0);

			codeComplete(cu, offset, collector);

			IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();
			Optional<IJavaCompletionProposal> result = Stream.of(proposals).filter(JavaLambdaCompletionProposal.class::isInstance).findFirst();
			assertTrue("doesn't contain JavaLambdaCompletionProposal", result.isPresent());

			IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			IJavaCompletionProposal proposal= result.get();
			assertTrue("valid proposal", ((ICompletionProposalExtension2)proposal).validate(doc, offset, null));
			proposal.apply(doc);

			String str1= """
				package test1;
				
				import java.util.function.Consumer;
				public class Bug443091 {
					void foo(Consumer<Integer> c){}
				    void test() {
				        this.foo(arg0 -> )
				    }
				}
				""";
			assertEquals(str1, doc.get());
		} finally {
			part.getSite().getPage().closeAllEditors(false);
		}
	}

	@Test
	public void testBug443091_expectLambdaExpressionCompletion_onVariableAssignment() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			
			import java.util.function.Consumer;
			public class Bug443091 {
			    void test() {
			        Consumer<Integer> c =
			    }
			}
			""";

		ICompilationUnit cu= pack1.createCompilationUnit("Bug443091.java", contents, false, null);

		IEditorPart part= JavaUI.openInEditor(cu);
		try {
			String str= "Consumer<Integer> c =";

			int offset= contents.indexOf(str) + str.length();

			CompletionProposalCollector collector= createCollector(cu, offset);
			collector.setReplacementLength(0);

			codeComplete(cu, offset, collector);

			IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();
			Optional<IJavaCompletionProposal> result = Stream.of(proposals).filter(JavaLambdaCompletionProposal.class::isInstance).findFirst();
			assertTrue("doesn't contain JavaLambdaCompletionProposal", result.isPresent());

			IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			IJavaCompletionProposal proposal= result.get();
			assertTrue("valid proposal", ((ICompletionProposalExtension2)proposal).validate(doc, offset, null));
			proposal.apply(doc);

			String str1= """
				package test1;
				
				import java.util.function.Consumer;
				public class Bug443091 {
				    void test() {
				        Consumer<Integer> c =arg0 ->\s
				    }
				}
				""";
			assertEquals(str1, doc.get());
		} finally {
			part.getSite().getPage().closeAllEditors(false);
		}
	}

	@Test
	public void testBug443091_expectZeroArgLambdaExpressionCompletion_onVariableAssignment() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			
			public class Bug443091 {
			    void test() {
			        Runnable run =
			    }
			}
			""";

		ICompilationUnit cu= pack1.createCompilationUnit("Bug443091.java", contents, false, null);

		IEditorPart part= JavaUI.openInEditor(cu);
		try {
			String str= "Runnable run =";

			int offset= contents.indexOf(str) + str.length();

			CompletionProposalCollector collector= createCollector(cu, offset);
			collector.setReplacementLength(0);

			codeComplete(cu, offset, collector);

			IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();
			Optional<IJavaCompletionProposal> result = Stream.of(proposals).filter(JavaLambdaCompletionProposal.class::isInstance).findFirst();
			assertTrue("doesn't contain JavaLambdaCompletionProposal", result.isPresent());

			IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			IJavaCompletionProposal proposal= result.get();
			assertTrue("valid proposal", ((ICompletionProposalExtension2)proposal).validate(doc, offset, null));
			proposal.apply(doc);

			String str1= """
				package test1;
				
				public class Bug443091 {
				    void test() {
				        Runnable run =() ->\s
				    }
				}
				""";
			assertEquals(str1, doc.get());
		} finally {
			part.getSite().getPage().closeAllEditors(false);
		}
	}
	@Test
	public void testBug443091_expectMultiArgLambdaExpressionCompletion_onVariableAssignment() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			
			import java.util.function.BiConsumer;
			public class Bug443091 {
			    void test() {
			        BiConsumer<Integer, String> bi =
			    }
			}
			""";

		ICompilationUnit cu= pack1.createCompilationUnit("Bug443091.java", contents, false, null);

		IEditorPart part= JavaUI.openInEditor(cu);
		try {
			String str= "BiConsumer<Integer, String> bi =";

			int offset= contents.indexOf(str) + str.length();

			CompletionProposalCollector collector= createCollector(cu, offset);
			collector.setReplacementLength(0);

			codeComplete(cu, offset, collector);

			IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();
			Optional<IJavaCompletionProposal> result = Stream.of(proposals).filter(JavaLambdaCompletionProposal.class::isInstance).findFirst();
			assertTrue("doesn't contain JavaLambdaCompletionProposal", result.isPresent());

			IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			IJavaCompletionProposal proposal= result.get();
			assertTrue("valid proposal", ((ICompletionProposalExtension2)proposal).validate(doc, offset, null));
			proposal.apply(doc);

			String str1= """
				package test1;
				
				import java.util.function.BiConsumer;
				public class Bug443091 {
				    void test() {
				        BiConsumer<Integer, String> bi =(arg0, arg1) ->\s
				    }
				}
				""";
			assertEquals(str1, doc.get());
		} finally {
			part.getSite().getPage().closeAllEditors(false);
		}
	}
}

/*******************************************************************************
 * Copyright (c) 2014 GK Software AG, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephan Herrmann <stephan.herrmann@berlin.de> - initial API and implementation - https://bugs.eclipse.org/425183
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.contentassist;

import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
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

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.Java18ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

public class CodeCompletionTest18 extends AbstractCompletionTest {

	private static final Class THIS= CodeCompletionTest18.class;
	
	public static Test suite() {
		return new Java18ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new Java18ProjectTestSetup(test);
	}

	private IJavaProject fJProject1;


	protected void setUp() throws Exception {
		fJProject1= Java18ProjectTestSetup.getProject();

		Hashtable options= TestOptions.getDefaultOptions();
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

	protected void tearDown() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setToDefault(PreferenceConstants.CODEGEN_ADD_COMMENTS);
		store.setToDefault(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS);
		store.setToDefault(PreferenceConstants.CODEASSIST_SHOW_VISIBLE_PROPOSALS);
		closeAllEditors();
		JavaProjectHelper.clear(fJProject1, Java18ProjectTestSetup.getDefaultClasspath());
	}

	public static void closeEditor(IEditorPart editor) {
		IWorkbenchPartSite site;
		IWorkbenchPage page;
		if (editor != null && (site= editor.getSite()) != null && (page= site.getPage()) != null)
			page.closeEditor(editor, false);
	}

	public static void closeAllEditors() {
		IWorkbenchWindow[] windows= PlatformUI.getWorkbench().getWorkbenchWindows();
		for (int i= 0; i < windows.length; i++) {
			IWorkbenchPage[] pages= windows[i].getPages();
			for (int j= 0; j < pages.length; j++) {
				IEditorReference[] editorReferences= pages[j].getEditorReferences();
				for (int k= 0; k < editorReferences.length; k++)
					closeEditor(editorReferences[k].getEditor(false));
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

	public void testBug425183_comment8() throws CoreException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents=
			"package test1\n" +
			"public class X {\n" +
			"    void foo() {\n" + 
			"        java.util.Comparator.reverseOrder().  // content assist after '.' => NPE\n" + 
			"    }\n" +
			"}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("X.java", contents, false, null);


		String str= "java.util.Comparator.reverseOrder().";

		int offset= contents.indexOf(str) + str.length();

		CompletionProposalCollector collector= createCollector(cu, offset);

		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();
		ICompletionProposal proposal= null;

		for (int i= 0; i < proposals.length; i++) {
			IJavaCompletionProposal curr= proposals[i];
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
				"package test1\n" +
				"public class X {\n" +
				"    void foo() {\n" + 
				"        java.util.Comparator.reverseOrder().thenComparingLong()  // content assist after '.' => NPE\n" + 
				"    }\n" +
				"}\n";
		assertEquals(expectedContents, doc.get());
	}

	public void testOverride1() throws CoreException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class Main {\n");
		buf.append("    private static class Cls implements Interface {\n");
		buf.append("        hello\n");
		buf.append("    }\n");
		buf.append("    private static interface Interface {\n");
		buf.append("        default void hello() {\n");
		buf.append("            System.out.println(\"Hello\");\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Main.java", buf.toString(), false, null);

		String str= "hello";
		int offset= buf.toString().indexOf(str) + str.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);
		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();
		assertEquals(1, proposals.length);
		IEditorPart part= JavaUI.openInEditor(cu);
		IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
		proposals[0].apply(doc);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class Main {\n");
		buf.append("    private static class Cls implements Interface {\n");
		buf.append("        /* (non-Javadoc)\n");
		buf.append("         * @see test1.Main.Interface#hello()\n");
		buf.append("         */\n");
		buf.append("        @Override\n");
		buf.append("        public void hello() {\n");
		buf.append("            //TODO\n");
		buf.append("            Interface.super.hello();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private static interface Interface {\n");
		buf.append("        default void hello() {\n");
		buf.append("            System.out.println(\"Hello\");\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEquals(buf.toString(), doc.get());
	}

	public void testOverride2() throws CoreException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class Main {\n");
		buf.append("    private static interface Bar extends Foo {\n");
		buf.append("        hello\n");
		buf.append("    }\n");
		buf.append("    private static interface Foo {\n");
		buf.append("        default void hello() {\n");
		buf.append("            System.out.println(\"Hello\");\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Main.java", buf.toString(), false, null);

		String str= "hello";
		int offset= buf.toString().indexOf(str) + str.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);
		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();
		assertEquals(1, proposals.length);
		IEditorPart part= JavaUI.openInEditor(cu);
		IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
		proposals[0].apply(doc);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class Main {\n");
		buf.append("    private static interface Bar extends Foo {\n");
		buf.append("        /* (non-Javadoc)\n");
		buf.append("         * @see test1.Main.Foo#hello()\n");
		buf.append("         */\n");
		buf.append("        @Override\n");
		buf.append("        public default void hello() {\n");
		buf.append("            //TODO\n");
		buf.append("            Foo.super.hello();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private static interface Foo {\n");
		buf.append("        default void hello() {\n");
		buf.append("            System.out.println(\"Hello\");\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEquals(buf.toString(), doc.get());
	}

	public void testOverride3() throws CoreException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface I {\n");
		buf.append("    default int getSize(String name) {\n");
		buf.append("        return name.length();\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("interface I2 extends I {\n");
		buf.append("    getSize\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("I.java", buf.toString(), false, null);
	
		String str= "getSize";
		int offset= buf.toString().lastIndexOf(str) + str.length();
	
		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);
		codeComplete(cu, offset, collector);
	
		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();
		assertEquals(1, proposals.length);
		IEditorPart part= JavaUI.openInEditor(cu);
		IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
		proposals[0].apply(doc);
	
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface I {\n");
		buf.append("    default int getSize(String name) {\n");
		buf.append("        return name.length();\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("interface I2 extends I {\n");
		buf.append("    /* (non-Javadoc)\n");
		buf.append("     * @see test1.I#getSize(java.lang.String)\n");
		buf.append("     */\n");
		buf.append("    @Override\n");
		buf.append("    public default int getSize(String name) {\n");
		buf.append("        //TODO\n");
		buf.append("        return I.super.getSize(name);\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEquals(buf.toString(), doc.get());
	}

}

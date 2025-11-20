/*******************************************************************************
 * Copyright (c) 2024, 2025 GK Software AG, IBM Corporation and others.
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
 *     Red Hat Inc. - modified to test Java 16
 *******************************************************************************/
package org.eclipse.jdt.text.tests.contentassist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Hashtable;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.CoreException;

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

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.Java16ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.java.JavaAllCompletionProposalComputer;
import org.eclipse.jdt.internal.ui.text.java.JavaTypeCompletionProposalComputer;

/**
 * Those tests are made to run on Java 16
 */
public class CodeCompletionTest16 extends AbstractCompletionTest {
	@Rule
	public Java16ProjectTestSetup j16s= new Java16ProjectTestSetup(true);

	private IJavaProject fJProject1;

	@Override
	public void setUp() throws Exception {
		fJProject1= j16s.getProject();
		fJProject1.setOption(JavaCore.COMPILER_COMPLIANCE, "16");
		fJProject1.setOption(JavaCore.COMPILER_SOURCE, "16");
		fJProject1.setOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "16");
		fJProject1.setOption(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, JavaCore.ENABLED);


		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "1");
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");
		options.put(JavaCore.COMPILER_COMPLIANCE, "16");
		options.put(JavaCore.COMPILER_SOURCE, "16");
		options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "16");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, true);
		store.setValue(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS, true);
		store.setValue(PreferenceConstants.CODEASSIST_SHOW_VISIBLE_PROPOSALS, true);

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
		JavaProjectHelper.clear(fJProject1, j16s.getDefaultClasspath());
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

	private JavaContentAssistInvocationContext createContext(int offset, ICompilationUnit cu) throws PartInitException, JavaModelException {
		JavaEditor editor= (JavaEditor) JavaUI.openInEditor(cu);
		ISourceViewer viewer= editor.getViewer();
		return new JavaContentAssistInvocationContext(viewer, offset, editor);
	}

	@Test
	public void testBug560674() throws CoreException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents=
			"""
			package test1
			public record X1(int abcd) {
			      abc\s
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("X1.java", contents, false, null);


		String str= "abc ";

		int offset= contents.indexOf(str) + str.length() - 1;

		JavaTypeCompletionProposalComputer comp= new JavaAllCompletionProposalComputer();

		List<ICompletionProposal> proposals= comp.computeCompletionProposals(createContext(offset, cu), null);
		ICompletionProposal proposal = proposals.get(0);

		IEditorPart part= JavaUI.openInEditor(cu);
		IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
		if (proposal != null) {
			proposal.apply(doc);
		}

		String expectedContents=
				"""
			package test1
			public record X1(int abcd) {
			      /**
			     * Method.
			     */
			    public int abcd() {
			        return abcd;
			    }\s
			}
			""";
		assertEquals(expectedContents, doc.get());
	}
	private ICompletionProposal findProposal(List<ICompletionProposal> proposals, String displayName) {
		for (ICompletionProposal proposal : proposals) {
			if (proposal.getDisplayString().equals(displayName)) {
				return proposal;
			}
		}
		return null;
	}
	@Test
	public void testRecordComponentJavadoc1() throws CoreException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents=
				"""
					package test1;
					/**
					 * A foo bar.
					 * @param foo The foo.
					 * @param bar The bar.
					 */
					public record FooBar(String foo, String bar) {}
					""";
		ICompilationUnit cu= pack1.createCompilationUnit("FooBar.java", contents, false, null);

		contents=
				"""
					package test1;
					public class X1 {
						public static void main(String[] args) {
							FooBar fooBar = new FooBar("some foo", "some bar");
							fooBar.f
						}
					}
					""";
		cu= pack1.createCompilationUnit("X1.java", contents, false, null);

		String str= "fooBar.f";

		int offset= contents.indexOf(str) + str.length() - 1;

		JavaTypeCompletionProposalComputer comp= new JavaAllCompletionProposalComputer();

		List<ICompletionProposal> proposals= comp.computeCompletionProposals(createContext(offset, cu), null);
		ICompletionProposal proposal= findProposal(proposals, "foo() : String - FooBar");
		assertNotNull("Proposal not found", proposal.getDisplayString());
		String info= proposal.getAdditionalProposalInfo();
		int idx = info.indexOf("A foo bar.<dl><dt>Parameters:");
		if (idx != -1) {
			fail("Unexpected Javadoc found. Found instead: " + info);
		}
		idx = info.indexOf("The foo.");
		if (idx == -1) {
			fail("Expected Javadoc not found. Found instead: " + info);
		}
		proposal= findProposal(proposals, "toString() : String - Object");
		assertNotNull("Proposal not found", proposal.getDisplayString());

		proposal= findProposal(proposals, "foo : String - FooBar");
		assertNull("Record component should not be proposed", proposal);
	}
	@Test
	public void testRecordComponentJavadoc2() throws CoreException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents=
				"""
					package test1;
					/**
					 * A foo bar.
					 * @param foo The foo.
					 * @param bar The bar.
					 */
					public record FooBar(String foo, String bar) {
						public void abc() {
							/* after this */fo
						}
					}
					""";
		ICompilationUnit cu= pack1.createCompilationUnit("FooBar.java", contents, false, null);


		String str= "/* after this */fo";

		int offset= contents.indexOf(str) + str.length() - 1;

		JavaTypeCompletionProposalComputer comp= new JavaAllCompletionProposalComputer();

		List<ICompletionProposal> proposals= comp.computeCompletionProposals(createContext(offset, cu), null);
		ICompletionProposal proposal= findProposal(proposals, "foo() : String - FooBar");
		assertNotNull("Proposal not found", proposal.getDisplayString());
		String info= proposal.getAdditionalProposalInfo();
		int idx = info.indexOf("A foo bar.<dl><dt>Parameters:");
		if (idx != -1) {
			fail("Unexpected Javadoc found. Found instead: " + info);
		}
		idx = info.indexOf("The foo.");
		if (idx == -1) {
			fail("Expected Javadoc not found. Found instead: " + info);
		}

		proposal= findProposal(proposals, "foo : String - FooBar");
		assertNotNull("Proposal not found", proposal.getDisplayString());
		info= proposal.getAdditionalProposalInfo();
		idx = info.indexOf("A foo bar.<dl><dt>Parameters:");
		if (idx != -1) {
			fail("Unexpected Javadoc found. Found instead: " + info);
		}
		idx = info.indexOf("The foo.");
		if (idx == -1) {
			fail("Expected Javadoc not found. Found instead: " + info);
		}
	}
	@Test
	public void testRecordConstructor() throws CoreException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents=
				"""
					package test1;
					/**
					 * A foo bar.
					 * @param foo The foo.
					 * @param bar The bar.
					 */
					public record FooBar(String foo, String bar) {
						/* here */
						Foo
					}
					""";
		ICompilationUnit cu= pack1.createCompilationUnit("FooBar.java", contents, false, null);

		int offset= contents.indexOf("here");
		offset= contents.indexOf("Foo", offset) + "Foo".length() - 1;

		JavaTypeCompletionProposalComputer comp= new JavaAllCompletionProposalComputer();

		List<ICompletionProposal> proposals= comp.computeCompletionProposals(createContext(offset, cu), null);
		ICompletionProposal proposal= findProposal(proposals, "FooBar() - Constructor");
		assertNotNull("Proposal not found", proposal.getDisplayString());
		IEditorPart part= JavaUI.openInEditor(cu);
		IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
		proposal.apply(doc);

		String proposalString= doc.get();
		assertTrue(proposalString.contains("public FooBar(String foo, String bar) {"), "proper constructor not found");
		assertTrue(proposalString.contains("this.foo = foo;"), "first component not initialized");
		assertTrue(proposalString.contains("this.bar = bar;"), "second component not initialized");
	}
}

/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.contentassist;

import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.text.javadoc.JavaDocCompletionEvaluator;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;
import org.eclipse.jdt.text.tests.performance.EditorTestHelper;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;

public class JavadocCompletionTest extends TestCase {


	private static final Class THIS= JavadocCompletionTest.class;
	private static final boolean OLD= true;
	private static final String CARET= "|";
	private static final String EMPTY_TYPE= "public class Completion {\n" +
			"}\n";
	private static final String[] BLOCK_TAGS= {"@exception", "@param", "@serialField", "@throws", "@author", "@deprecated", "@return", "@see", "@serial", "@serialData", "@since", "@version", "@inheritDoc", };
	private static final String[] INLINE_TAGS= {"@docRoot", "@inheritDoc", "@link", "@linkplain", "@value",};
	private static final String[] HTML_TAGS= {"b", "blockquote", "br", "code", "dd", "dl", "dt", "em", "hr", "h1", "h2", "h3", "h4", "h5", "h6", "i", "li", "nl", "ol", "p", "pre", "q", "td", "th", "tr", "tt", "ul",};

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}

	public static Test suite() {
		return allTests();
	}

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;
	private IPackageFragment fPackage;
	private String fTypeDeclaration;
	private String fImports;

	public JavadocCompletionTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		JavaProjectHelper.addRTJar(fJProject1);
		JavaProjectHelper.addRequiredProject(fJProject1, ProjectTestSetup.getProject());
		fSourceFolder= setUpSourceFolder();
		fPackage= fSourceFolder.createPackageFragment("test1", false, null);

		Hashtable options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "1");
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");
		JavaCore.setOptions(options);

		IPreferenceStore store= getJDTUIPrefs();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, true);
		store.setValue(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS, false);

		StubUtility.setCodeTemplate(CodeTemplateContextType.OVERRIDECOMMENT_ID, "/* (non-Javadoc)\n * ${see_to_overridden}\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "//TODO\n${body_statement}", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORCOMMENT_ID, "/**\n * Constructor.\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODCOMMENT_ID, "/**\n * Method.\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "//TODO\n${body_statement}", null);
		
		fTypeDeclaration= EMPTY_TYPE;
		fImports= "";
	}

	private IPreferenceStore getJDTUIPrefs() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return store;
	}

	private IPackageFragmentRoot setUpSourceFolder() throws CoreException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		return sourceFolder;
	}
	
	private ICompilationUnit createCU(IPackageFragment pack1, String contents) throws JavaModelException {
		ICompilationUnit cu= pack1.createCompilationUnit("Completion.java", contents, false, null);
		return cu;
	}

	protected void tearDown() throws Exception {
		IPreferenceStore store= getJDTUIPrefs();
		store.setToDefault(PreferenceConstants.CODEGEN_ADD_COMMENTS);
		store.setToDefault(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS);
		
		JavaProjectHelper.delete(fJProject1);
	}

	public void testSeeType() throws Exception {
		assertTypeJavadocProposal(" * @see java.util.List|", " * @see List|", "List ");
	}
	
	public void testSeeTypeJavaLang() throws Exception {
		assertTypeJavadocProposal(" * @see java.lang.String|", " * @see Str|", "String ");
	}
	
	public void testSeeImportedType() throws Exception {
		fImports= "import java.util.List;\n";
		assertTypeJavadocProposal(" * @see java.util.List|", " * @see Lis|", "List ");
	}
	
	public void testSeeTypeSameType() throws Exception {
		assertTypeJavadocProposal(" * @see Completion|", " * @see Comple|", "Completion ");
	}
	
	public void testSeeMethod() throws Exception {
		assertTypeJavadocProposal(" * @see java.util.List#size()|", " * @see java.util.List#siz|", "size(");
	}
	
	public void testSeeMethodWithoutImport() throws Exception {
		if (!OLD) {
			System.out.println("JavadocCompletionTest.testSeeMethodWithoutImport() - no best-effort imports with Core completion");
			return;
		}
		assertTypeJavadocProposal(" * @see List#size()|", " * @see List#siz|", "size(");
	}
	
	public void testSeeMethodWithParam() throws Exception {
		assertTypeJavadocProposal(" * @see java.util.List#get(int)|", " * @see java.util.List#ge|", "get(");
	}
	
	public void testSeeMethodWithTypeVariableParameter() throws Exception {
		if (OLD) // TODO erased vs. generic references - what does the spec say? 
			assertTypeJavadocProposal(" * @see java.util.List#add(E)|", " * @see java.util.List#ad|", "add(E");
		else
			assertTypeJavadocProposal(" * @see java.util.List#add(Object)|", " * @see java.util.List#ad|", "add(E");
	}
	
	public void testSeeMethodLocal() throws Exception {
		fTypeDeclaration= "public class Completion {\n" +
				"		public void method() {}\n" +
				"}\n";
		assertTypeJavadocProposal(" * @see #method()|", " * @see #me|", "met");
	}
	
	public void testSeeConstant() throws Exception {
		assertTypeJavadocProposal(" * @see java.util.Collections#EMPTY_LIST|", " * @see java.util.Collections#|", "EMPTY_LI");
	}
	
	public void testLinkType() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.util.List|", " * Prefix {@link List|", "List ");
	}
	public void testLinkTypeClosed() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.util.List|}", " * Prefix {@link List|}", "List ");
	}
	
	public void testLinkTypeJavaLang() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.lang.String|", " * Prefix {@link Str|", "String ");
	}
	public void testLinkTypeJavaLangClosed() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.lang.String|}", " * Prefix {@link Str|}", "String ");
	}
	
	public void testLinkTypeSameType() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link Completion|", " * Prefix {@link Comple|", "Completion ");
	}
	public void testLinkTypeSameTypeClosed() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link Completion|}", " * Prefix {@link Comple|}", "Completion ");
	}
	
	public void testLinkMethodWithoutImport() throws Exception {
		if (!OLD) {
			System.out.println("JavadocCompletionTest.testLinkMethodWithoutImport() - no best-effort imports with Core completion");
			return;
		}
		assertTypeJavadocProposal(" * Prefix {@link List#size()|", " * Prefix {@link List#siz|", "size(");
	}
	
	public void testLinkMethod() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#size()|", " * Prefix {@link java.util.List#siz|", "size(");
	}
	
	public void testLinkMethodLocal() throws Exception {
		fTypeDeclaration= "public class Completion {\n" +
				"		public void method() {}\n" +
				"}\n";
		assertTypeJavadocProposal(" * {@link #method()|", " * {@link #me|", "met");
	}
	
	public void testLinkMethodClosed() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#size()|}", " * Prefix {@link java.util.List#siz|}", "size(");
	}
	
	public void testLinkMethodWithParam() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#get(int)|", " * Prefix {@link java.util.List#ge|", "get(");
	}
	
	public void testLinkMethodWithParamNoOverwrite() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#clear()|add} postfix", " * Prefix {@link java.util.List#|add} postfix", "clear");
	}
	
	public void testLinkMethodWithParamNoOverwriteWithParams() throws Exception {
		// TODO fails with OLD==false
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#clear()|add(int, Object)} postfix", " * Prefix {@link java.util.List#|add(int, Object)} postfix", "clear");
	}
	
	public void testLinkMethodWithParamOverwriteNoPrefix() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, false);
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#clear()|add} postfix", " * Prefix {@link java.util.List#|add} postfix", "clear");
	}
	
	public void testLinkMethodWithParamOverwrite() throws Exception {
		// TODO fails with OLD==false
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, false);
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#get(int)|} postfix", " * Prefix {@link java.util.List#g|et} postfix", "get(");
	}
	
	public void testLinkMethodWithParamOverwriteWithParamsNoPrefix() throws Exception {
		// TODO fails with OLD==false
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, false);
		assertTypeJavadocProposal(" * Prefix {@linkjava.util. List#clear()|add(int, Object)} postfix", " * Prefix {@link java.util.List#|add(int, Object)} postfix", "clear");
	}
	
	public void testLinkMethodWithParamOverwriteWithParams() throws Exception {
		// TODO fails with OLD==false
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, false);
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#get(int)|} postfix", " * Prefix {@link java.util.List#g|et(long)} postfix", "get(");
	}
	
	public void testLinkMethodWithParamClosed() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#get(int)|}", " * Prefix {@link java.util.List#ge|}", "get(");
	}
	
	public void testLinkMethodWithTypeVariableParameter() throws Exception {
		if (OLD) // TODO erased vs. generic references - what does the spec say?  - nothing found yet, but List#addAll suggests erased types
			assertTypeJavadocProposal(" * Prefix {@link java.util.List#add(E)|", " * Prefix {@link java.util.List#ad|", "add(E");
		else
			assertTypeJavadocProposal(" * Prefix {@link java.util.List#add(Object)|", " * Prefix {@link java.util.List#ad|", "add(E");
	}
	public void testLinkMethodWithTypeVariableParameterClosed() throws Exception {
		if (OLD) // TODO erased vs. generic references - what does the spec say? 
			assertTypeJavadocProposal(" * Prefix {@link java.util.List#add(E)|}", " * Prefix {@link java.util.List#ad|}", "add(E");
		else
			assertTypeJavadocProposal(" * Prefix {@link java.util.List#add(Object)|}", " * Prefix {@link java.util.List#ad|}", "add(E");
	}
	
	public void testLinkConstant() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.util.Collections#EMPTY_LIST|", " * Prefix {@link java.util.Collections#|", "EMPTY_LI");
	}
	
	public void testBlockTags() throws Exception {
		// TODO fails with OLD==false
		tearDown();
		for (int i= 0; i < BLOCK_TAGS.length; i++) {
			setUp();
			String tag= BLOCK_TAGS[i];
			assertTypeJavadocProposal(" * " + tag, " * @|",  tag);
			tearDown();
		}
		setUp();
	}
	
 	public void testNoInlineAsBlockTags() throws Exception {
 		if (OLD) {
 			System.out.println("JavadocCompletionTest.testNoInlineAsRootTags() - disabled in legacy mode");
 			return;
 		}
 		tearDown();
 		for (int i= 0; i < INLINE_TAGS.length; i++) {
 			setUp();
			String tag= INLINE_TAGS[i];
			assertNoProposals(" * @|", tag);
			tearDown();
		}
		setUp();
	}
 	
	public void testInlineTags() throws Exception {
		// TODO fails with OLD==false
 		tearDown();
		for (int i= 0; i < INLINE_TAGS.length; i++) {
			setUp();
			String tag= INLINE_TAGS[i];
			assertTypeJavadocProposal(" * {" + tag, " * {@|", tag);
			tearDown();
		}
		setUp();
	}
	
	public void testNoBlockAsInlineTags() throws Exception {
 		if (OLD) {
 			System.out.println("JavadocCompletionTest.testNoRootAsInlineTags() - disabled in legacy mode");
 			return;
 		}
		tearDown();
		for (int i= 0; i < BLOCK_TAGS.length; i++) {
			setUp();
			String tag= BLOCK_TAGS[i];
			assertNoProposals(" * {@|", tag);
			tearDown();
		}
		setUp();
	}
	
	public void testHTMLTags() throws Exception {
 		if (!OLD) {
 			System.out.println("no HTML tag proposals in core jdoc assist");
 			return;
 		}
		tearDown();
		for (int i= 0; i < HTML_TAGS.length; i++) {
			setUp();
			String tag= HTML_TAGS[i];
			assertTypeJavadocProposal(" * Prefix <" + tag + ">| postfix", " * Prefix <" + tag.charAt(0) + "| postfix", "<" + tag);
			tearDown();
		}
		setUp();
	}
 	
	/**
	 * Creates a CU with a type javadoc that contains solely <code>javadocLine</code>, then runs
	 * code assist and applies the first proposal whose display name matches <code>selector</code>
	 * and asserts that the javadoc line now has the content of <code>expected</code>.
	 * 
	 * @param expected the expected contents of the type javadoc line
	 * @param javadocLine the contents of the javadoc line before code completion is run
	 * @param selector the prefix to match a proposal with
	 * @throws CoreException
	 */
	private void assertTypeJavadocProposal(String expected, String javadocLine, String selector) throws CoreException {
		StringBuffer contents= new StringBuffer();
		IRegion preSelection= assembleTestCUExtractSelection(contents, javadocLine);
		ICompilationUnit cu= createCU(fPackage, contents.toString());

		ITextEditor editor= (ITextEditor) EditorUtility.openInEditor(cu);
		IDocument doc;
		ITextSelection postSelection;
		try {
			IJavaCompletionProposal proposal= findNonNullProposal(selector, cu, preSelection);
			doc= editor.getDocumentProvider().getDocument(editor.getEditorInput());
			apply(editor, doc, proposal, preSelection);
			postSelection= (ITextSelection) editor.getSelectionProvider().getSelection();
		} finally {
			EditorTestHelper.closeEditor(editor);
		}
		
		StringBuffer result= new StringBuffer();
		IRegion expectedSelection= assembleTestCUExtractSelection(result, expected);

		assertEquals(result.toString(), doc.get());
		assertEquals(expectedSelection.getOffset(), postSelection.getOffset());
		assertEquals(expectedSelection.getLength(), postSelection.getLength());
	}
	
	private void assertNoProposals(String javadocLine, String selector) throws CoreException {
		StringBuffer contents= new StringBuffer();
		IRegion preSelection= assembleTestCUExtractSelection(contents, javadocLine);
		ICompilationUnit cu= createCU(fPackage, contents.toString());

		ITextEditor editor= (ITextEditor) EditorUtility.openInEditor(cu);
		try {
			assertNull(findNamedProposal(selector, cu, preSelection));
		} finally {
			EditorTestHelper.closeEditor(editor);
		}
		
	}

	private IRegion assembleTestCUExtractSelection(StringBuffer buffer, String javadocLine) {
		String prefix= "package test1;\n" +
				fImports +
				 "\n" +
				 "/**\n";
		String postfix= "\n" +
				 " */\n" +
				 fTypeDeclaration;
		StringBuffer lineBuffer= new StringBuffer(javadocLine);
		int firstPipe= lineBuffer.indexOf(CARET);
		int secondPipe;
		if (firstPipe == -1) {
			firstPipe= lineBuffer.length();
			secondPipe= firstPipe;
		} else {
			lineBuffer.replace(firstPipe, firstPipe + CARET.length(), "");
			secondPipe= lineBuffer.indexOf(CARET, firstPipe);
			if (secondPipe ==-1)
				secondPipe= firstPipe;
			else
				lineBuffer.replace(secondPipe, secondPipe + CARET.length(), "");
		}
		buffer.append(prefix + lineBuffer + postfix);
		return new Region(firstPipe + prefix.length(), secondPipe - firstPipe);
	}

	private IJavaCompletionProposal findNonNullProposal(String prefix, ICompilationUnit cu, IRegion selection) throws JavaModelException, PartInitException {
		IJavaCompletionProposal proposal= findNamedProposal(prefix, cu, selection);
		assertNotNull("no proposal starting with \"" + prefix + "\"", proposal);
		return proposal;
	}
	
	private IJavaCompletionProposal findNamedProposal(String prefix, ICompilationUnit cu, IRegion selection) throws JavaModelException, PartInitException {
		IJavaCompletionProposal[] proposals= collectProposals(cu, selection);
		
		for (int i= 0; i < proposals.length; i++) {
			if (proposals[i].getDisplayString().startsWith(prefix)) {
				return proposals[i];
			}
		}
		return null;
	}

	private IJavaCompletionProposal[] collectProposals(ICompilationUnit cu, IRegion selection) throws JavaModelException, PartInitException {
		if (OLD) {
			JavaDocCompletionEvaluator evaluator= new JavaDocCompletionEvaluator();
			IJavaCompletionProposal[] proposals= evaluator.computeCompletionProposals(cu, selection.getOffset(), selection.getLength(), 0);
			return proposals == null ? new IJavaCompletionProposal[0] : proposals;
		}
		
		CompletionProposalCollector collector= new CompletionProposalCollector(cu);
		collector.setReplacementLength(selection.getLength());
		codeComplete(cu, selection.getOffset(), collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();
		return proposals;
	}

	private void codeComplete(ICompilationUnit cu, int offset, CompletionProposalCollector collector) throws JavaModelException {
		cu.codeComplete(offset, collector);
	}

	private void apply(ITextEditor editor, IDocument doc, IJavaCompletionProposal proposal, IRegion selection) {
		if (proposal instanceof ICompletionProposalExtension2) {
			ICompletionProposalExtension2 ext= (ICompletionProposalExtension2) proposal;
			ITextViewer viewer= (ITextViewer) editor.getAdapter(ITextOperationTarget.class);
			ext.selected(viewer, false);
			viewer.setSelectedRange(selection.getOffset(), selection.getLength());
			ext.apply(viewer, (char) 0, 0, selection.getOffset());
			Point range= proposal.getSelection(doc);
			if (range != null)
				viewer.setSelectedRange(range.x, range.y);
		} else if (proposal instanceof ICompletionProposalExtension) {
			ICompletionProposalExtension ext= (ICompletionProposalExtension) proposal;
			ext.apply(doc, (char) 0, selection.getOffset() + selection.getLength());
		} else {
			proposal.apply(doc);
		}
	}

}

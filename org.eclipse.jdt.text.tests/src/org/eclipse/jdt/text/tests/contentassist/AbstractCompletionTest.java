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

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;
import org.eclipse.jdt.ui.text.IJavaPartitions;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProcessor;

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

public class AbstractCompletionTest extends TestCase {
	protected static final String CARET= "|";

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;
	private IPackageFragment fPackage;
	private ICompilationUnit fCU;
	private JavaEditor fEditor;

	private String fBeforeImports;
	private String fAfterImports;
	private String fMembers;

	private String fLocals;

	protected void setUp() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		JavaProjectHelper.addRTJar(fJProject1);
		JavaProjectHelper.addRequiredProject(fJProject1, ProjectTestSetup.getProject());
		fSourceFolder= setUpSourceFolder();
		fPackage= fSourceFolder.createPackageFragment("test1", false, null);

		Hashtable options= TestOptions.getDefaultOptions();
		configureCoreOptions(options);
		JavaCore.setOptions(options);

		IPreferenceStore store= getJDTUIPrefs();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, true);
		store.setValue(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS, false);
		store.setValue(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES, false);
		store.setValue(PreferenceConstants.CODEASSIST_ADDIMPORT, true);
		store.setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, true);

		StubUtility.setCodeTemplate(CodeTemplateContextType.OVERRIDECOMMENT_ID, "/* (non-Javadoc)\n * ${see_to_overridden}\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "//TODO\n${body_statement}", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORCOMMENT_ID, "/**\n * Constructor.\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODCOMMENT_ID, "/**\n * Method.\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "//TODO\n${body_statement}", null);
		
		fBeforeImports= "";
		fAfterImports= "";
		fMembers= "";
		fLocals= "";
	}

	protected void configureCoreOptions(Hashtable options) {
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "1");
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");
	}

	protected IPreferenceStore getJDTUIPrefs() {
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
		store.setToDefault(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES);
		store.setToDefault(PreferenceConstants.CODEASSIST_INSERT_COMPLETION);
		store.setToDefault(PreferenceConstants.CODEASSIST_ADDIMPORT);
		store.setToDefault(PreferenceConstants.EDITOR_CLOSE_BRACKETS);
		store.setToDefault(PreferenceConstants.CODEASSIST_INSERT_COMPLETION);

		JavaProjectHelper.delete(fJProject1);
		fCU= null;
	}

	protected void expectImports(String before, String after) {
		fBeforeImports= before;
		fAfterImports= after;
	}
	
	protected void addMembers(String member) {
		fMembers += "\n	" + member + "\n";
	}
	
	protected void addLocalVariables(String variable) {
		fLocals += "\n		" + variable + "\n";
	}
	
	/**
	 * Creates a CU with a method containing <code>before</code>, then runs
	 * code assist and applies the first proposal whose display name matches <code>selector</code>
	 * and asserts that the method's body now has the content of <code>expected</code>.
	 * 
	 * @param expected the expected contents of the type javadoc line
	 * @param javadocLine the contents of the javadoc line before code completion is run
	 * @param selector the prefix to match a proposal with
	 * @throws CoreException
	 */
	protected void assertMethodBodyProposal(String before, String selector, String expected) throws CoreException {
		StringBuffer contents= new StringBuffer();
		IRegion preSelection= assembleMethodBodyTestCUExtractSelection(contents, before, fBeforeImports);
		StringBuffer result= new StringBuffer();
		IRegion expectedSelection= assembleMethodBodyTestCUExtractSelection(result, expected, fAfterImports);

		assertProposal(selector, contents, preSelection, result, expectedSelection);
	}
	
	/**
	 * Creates a CU with a method containing <code>before</code>, then runs
	 * code assist and applies the first proposal whose display name matches <code>selector</code>
	 * and asserts that the method's body now has the content of <code>expected</code>.
	 * 
	 * @param expected the expected contents of the type javadoc line
	 * @param javadocLine the contents of the javadoc line before code completion is run
	 * @param selector the prefix to match a proposal with
	 * @throws CoreException
	 */
	protected void assertTypeBodyProposal(String before, String selector, String expected) throws CoreException {
		StringBuffer contents= new StringBuffer();
		IRegion preSelection= assembleClassBodyTestCUExtractSelection(contents, before, fBeforeImports);
		StringBuffer result= new StringBuffer();
		IRegion expectedSelection= assembleClassBodyTestCUExtractSelection(result, expected, fAfterImports);
		
		assertProposal(selector, contents, preSelection, result, expectedSelection);
	}
	
	/**
	 * Creates a CU with a method containing <code>before</code>, then runs
	 * code assist and asserts that there is no proposal starting with selector.
	 * 
	 * @param expected the expected contents of the type javadoc line
	 * @param javadocLine the contents of the javadoc line before code completion is run
	 * @param selector the prefix to match a proposal with
	 * @throws CoreException
	 */
	protected void assertNoMethodBodyProposals(String before, String selector) throws CoreException {
		StringBuffer contents= new StringBuffer();
		IRegion preSelection= assembleMethodBodyTestCUExtractSelection(contents, before, fBeforeImports);

		assertNoProposal(selector, contents, preSelection);
	}

	private void assertProposal(String selector, StringBuffer contents, IRegion preSelection, StringBuffer result, IRegion expectedSelection) throws JavaModelException, PartInitException {
		fCU= createCU(fPackage, contents.toString());
		fEditor= (JavaEditor) EditorUtility.openInEditor(fCU);
		IDocument doc;
		ITextSelection postSelection;
		try {
			ICompletionProposal proposal= findNonNullProposal(selector, fCU, preSelection);
			doc= fEditor.getDocumentProvider().getDocument(fEditor.getEditorInput());
			apply(fEditor, doc, proposal, preSelection);
			postSelection= (ITextSelection) fEditor.getSelectionProvider().getSelection();
		} finally {
			EditorTestHelper.closeEditor(fEditor);
		}

		assertEquals(result.toString(), doc.get());
		assertEquals(expectedSelection.getOffset(), postSelection.getOffset());
		assertEquals(expectedSelection.getLength(), postSelection.getLength());
	}
	
	private void assertNoProposal(String selector, StringBuffer contents, IRegion preSelection) throws JavaModelException, PartInitException {
		fCU= createCU(fPackage, contents.toString());
		fEditor= (JavaEditor) EditorUtility.openInEditor(fCU);
		try {
			assertNull(findNamedProposal(selector, fCU, preSelection));
		} finally {
			EditorTestHelper.closeEditor(fEditor);
		}
	}
	
	private IRegion assembleMethodBodyTestCUExtractSelection(StringBuffer buffer, String javadocLine, String imports) {
		String prefix= "package test1;\n" +
				imports +
				"\n" +
				"public class Completion<T> {\n" +
				"	public void testMethod(int param) {\n" +
				fLocals +
				"		";
		String postfix= "\n" +
				"	}\n" +
				fMembers +
				"}\n";
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
	
	private IRegion assembleClassBodyTestCUExtractSelection(StringBuffer buffer, String javadocLine, String imports) {
		String prefix= "package test1;\n" +
		imports +
		"\n" +
		"public class Completion<T> {\n" +
		fLocals +
		"    ";
		String postfix= "\n" +
		"\n" +
		fMembers +
		"}\n";
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
	
	private ICompletionProposal findNonNullProposal(String prefix, ICompilationUnit cu, IRegion selection) throws JavaModelException, PartInitException {
		ICompletionProposal proposal= findNamedProposal(prefix, cu, selection);
		assertNotNull("no proposal starting with \"" + prefix + "\"", proposal);
		return proposal;
	}
	
	private ICompletionProposal findNamedProposal(String prefix, ICompilationUnit cu, IRegion selection) throws JavaModelException, PartInitException {
		ICompletionProposal[] proposals= collectProposals(cu, selection);
		
		for (int i= 0; i < proposals.length; i++) {
			if (proposals[i].getDisplayString().startsWith(prefix)) {
				return proposals[i];
			}
		}
		return null;
	}

	private ICompletionProposal[] collectProposals(ICompilationUnit cu, IRegion selection) throws JavaModelException, PartInitException {
		
		ContentAssistant assistant= new ContentAssistant();
		assistant.setDocumentPartitioning(IJavaPartitions.JAVA_PARTITIONING);
		IContentAssistProcessor javaProcessor= new JavaCompletionProcessor(fEditor, assistant, IDocument.DEFAULT_CONTENT_TYPE);

		ICompletionProposal[] proposals= javaProcessor.computeCompletionProposals(fEditor.getViewer(), selection.getOffset());
		return proposals;
	}

	private void apply(ITextEditor editor, IDocument doc, ICompletionProposal proposal, IRegion selection) {
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

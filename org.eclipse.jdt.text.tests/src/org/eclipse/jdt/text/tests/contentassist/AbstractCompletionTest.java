/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.text.tests.contentassist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;
import org.eclipse.jdt.testplugin.util.DisplayHelper;
import org.eclipse.jdt.text.tests.performance.EditorTestHelper;

import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.tests.Accessor;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.IJavaPartitions;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.java.ContentAssistProcessor;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProcessor;

public abstract class AbstractCompletionTest {

	@Rule
	public CompletionTestSetup cts= new CompletionTestSetup();

	protected static String suiteName(Class<?> fqn) {
		String name= fqn.toString();
		name= name.substring(name.lastIndexOf('.') + 1);
		return name;
	}

	protected static final String CARET= "|";

	private ICompilationUnit fCU;
	private JavaEditor fEditor;

	private String fBeforeImports;
	private String fAfterImports;
	private String fMembers;
	private String fLocals;
	private char fTrigger;
	private boolean fWaitBeforeCompleting;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
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
		fTrigger= '\0';
		fWaitBeforeCompleting= false;
	}

	protected IPackageFragment getAnonymousTestPackage() throws CoreException {
		return cts.getAnonymousTestPackage();
	}

	protected void configureCoreOptions(Hashtable<String, String> options) {
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "1");
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.DISABLED);
		options.put(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");
	}

	protected void setCoreOption(String key, String value) {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(key, value);
		JavaCore.setOptions(options);
	}

	protected void waitBeforeCompleting(boolean wait) {
		fWaitBeforeCompleting= wait;
	}

	protected IPreferenceStore getJDTUIPrefs() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return store;
	}

	private ICompilationUnit createCU(IPackageFragment pack1, String contents) throws JavaModelException {
		ICompilationUnit cu= pack1.createCompilationUnit("Completion_" + getName() + ".java", contents, false, null);
		return cu;
	}

	@After
	public void tearDown() throws Exception {
		IPreferenceStore store= getJDTUIPrefs();
		store.setToDefault(PreferenceConstants.CODEGEN_ADD_COMMENTS);
		store.setToDefault(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS);
		store.setToDefault(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES);
		store.setToDefault(PreferenceConstants.CODEASSIST_INSERT_COMPLETION);
		store.setToDefault(PreferenceConstants.CODEASSIST_ADDIMPORT);
		store.setToDefault(PreferenceConstants.EDITOR_CLOSE_BRACKETS);
		store.setToDefault(PreferenceConstants.CODEASSIST_INSERT_COMPLETION);
		store.setToDefault(PreferenceConstants.CODEASSIST_PREFIX_COMPLETION);
		store.setToDefault(PreferenceConstants.CODEASSIST_AUTOINSERT);

		if (fEditor != null) {
			EditorTestHelper.closeEditor(fEditor);
			fEditor= null;
		}

		if (fCU != null) {
			JavaProjectHelper.delete(fCU);
			fCU= null;
		}

		JavaProjectHelper.emptyDisplayLoop();
	}

	protected void addImport(String imp) {
		imp= normalizeImport(imp);
		if (fBeforeImports.length() == 0)
			fBeforeImports= "\n" + imp + "\n";
		else
			fBeforeImports += imp + "\n";
	}

	protected void expectImport(String imp) {
		imp= normalizeImport(imp);
		if (fAfterImports.length() == 0)
			fAfterImports= "\n" + imp + "\n";
		else
			fAfterImports += imp + "\n";
	}

	private String normalizeImport(String imp) {
		if (!imp.startsWith("import "))
			imp= "import " + imp;
		if (!imp.endsWith(";"))
			imp+= ";";
		return imp;
	}

	protected void addMembers(String member) {
		fMembers += "\n	" + member + "\n";
	}

	protected void addLocalVariables(String variable) {
		fLocals += "\n		" + variable + "\n";
	}

	protected void setTrigger(char trigger) {
		fTrigger= trigger;
	}

	/**
	 * Creates a CU with a method containing <code>before</code>, then runs code assist and applies
	 * the first proposal whose display name matches <code>selector</code> and asserts that the
	 * method's body now has the content of <code>expected</code>.
	 *
	 * @param before the contents of the javadoc line before code completion is run
	 * @param expected the expected contents of the type javadoc line
	 * @param selector the prefix to match a proposal with
	 * @throws CoreException if asserting the proposal failed
	 */
	protected void assertMethodBodyProposal(String before, String selector, String expected) throws CoreException {
		StringBuffer contents= new StringBuffer();
		IRegion preSelection= assembleMethodBodyTestCUExtractSelection(contents, before, fBeforeImports);
		StringBuffer result= new StringBuffer();
		IRegion expectedSelection= assembleMethodBodyTestCUExtractSelection(result, expected, fAfterImports);

		assertProposal(selector, contents, preSelection, result, expectedSelection);
	}

	/**
	 * Creates a CU with a method containing <code>before</code>, then runs incremental code assist
	 * and asserts that the method's body now has the content of <code>expected</code>.
	 *
	 * @param expected the expected contents of the type javadoc line
	 * @param before the contents of the javadoc line before code completion is run
	 * @throws CoreException if a failure happened during assertion
	 */
	protected void assertMethodBodyIncrementalCompletion(String before, String expected) throws CoreException {
		StringBuffer contents= new StringBuffer();
		IRegion preSelection= assembleMethodBodyTestCUExtractSelection(contents, before, fBeforeImports);
		StringBuffer result= new StringBuffer();
		IRegion expectedSelection= assembleMethodBodyTestCUExtractSelection(result, expected, fAfterImports);

		assertIncrementalCompletion(contents, preSelection, result, expectedSelection);
	}

	/**
	 * Creates a CU with a method containing <code>before</code>, then runs code assist and applies
	 * the first proposal whose display name matches <code>selector</code> and asserts that the
	 * method's body now has the content of <code>expected</code>.
	 *
	 * @param before the contents of the javadoc line before code completion is run
	 * @param expected the expected contents of the type javadoc line
	 * @param selector the prefix to match a proposal with
	 * @throws CoreException if asserting the proposal failed
	 */
	protected void assertMemberJavadocProposal(String before, String selector, String expected) throws CoreException {
		StringBuffer contents= new StringBuffer();
		IRegion preSelection= assembleMethodJavadocTestCUExtractSelection(contents, before);
		StringBuffer result= new StringBuffer();
		IRegion expectedSelection= assembleMethodJavadocTestCUExtractSelection(result, expected);

		assertProposal(selector, contents, preSelection, result, expectedSelection);
	}

	/**
	 * Creates a CU with a class containing <code>before</code>, then runs code assist and applies
	 * the first proposal whose display name matches <code>selector</code> and asserts that the
	 * method's body now has the content of <code>expected</code>.
	 *
	 * @param before the contents of the class body line before code completion is run
	 * @param expected the expected contents of the class body
	 * @param selector the prefix to match a proposal with
	 * @throws CoreException if asserting the proposal failed
	 */
	protected void assertTypeBodyProposal(String before, String selector, String expected) throws CoreException {
		StringBuffer contents= new StringBuffer();
		IRegion preSelection= assembleClassBodyTestCUExtractSelection(contents, before, fBeforeImports);
		StringBuffer result= new StringBuffer();
		IRegion expectedSelection= assembleClassBodyTestCUExtractSelection(result, expected, fAfterImports);

		assertProposal(selector, contents, preSelection, result, expectedSelection);
	}

	/**
	 * Creates a CU with a class containing <code>before</code>, then runs code assist and asserts
	 * that there is no proposal.
	 *
	 * @param before the contents of the class body line before code completion is run
	 * @param selector the prefix to match a proposal with
	 * @throws CoreException if asserting the proposal failed
	 * @since 3.6
	 */
	protected void assertNoTypeBodyProposal(String before, String selector) throws CoreException {
		StringBuffer contents= new StringBuffer();
		IRegion preSelection= assembleClassBodyTestCUExtractSelection(contents, before, fBeforeImports);
		assertNoProposal(selector, contents, preSelection);
	}

	/**
	 * Creates a CU with a method containing <code>before</code>, then runs code assist and applies
	 * the first proposal whose display name matches <code>selector</code> and asserts that the
	 * method's body now has the content of <code>expected</code>.
	 *
	 * @param expected the expected contents of the type javadoc line
	 * @param before the contents of the line where before code completion is run
	 * @param selector the prefix to match a proposal with
	 * @throws CoreException if asserting the proposal failed
	 */
	protected void assertTypeJavadocProposal(String before, String selector, String expected) throws CoreException {
		StringBuffer contents= new StringBuffer();
		IRegion preSelection= assembleClassJavadocTestCUExtractSelection(contents, before, fBeforeImports);
		StringBuffer result= new StringBuffer();
		IRegion expectedSelection= assembleClassJavadocTestCUExtractSelection(result, expected, fAfterImports);

		assertProposal(selector, contents, preSelection, result, expectedSelection);
	}

	/**
	 * Creates a CU with a method containing <code>before</code>, then runs code assist and asserts
	 * that there is no proposal starting with selector.
	 *
	 * @param before the contents of the javadoc line before code completion is run
	 * @param selector the prefix to match a proposal with
	 * @throws CoreException if a failure happened during assertion
	 */
	protected void assertNoMethodBodyProposals(String before, String selector) throws CoreException {
		StringBuffer contents= new StringBuffer();
		IRegion preSelection= assembleMethodBodyTestCUExtractSelection(contents, before, fBeforeImports);

		assertNoProposal(selector, contents, preSelection);
	}

	private void assertProposal(String selector, StringBuffer contents, IRegion preSelection, StringBuffer result, IRegion expectedSelection) throws CoreException {
		if (fEditor != null) {
			EditorTestHelper.closeEditor(fEditor);
		}

		fCU= createCU(getAnonymousTestPackage(), contents.toString());
		fEditor= (JavaEditor) EditorUtility.openInEditor(fCU);

		ICompletionProposal proposal= findNonNullProposal(selector, preSelection);
		IDocument doc= fEditor.getDocumentProvider().getDocument(fEditor.getEditorInput());
		apply(fEditor, doc, proposal, preSelection);
		ITextSelection postSelection= (ITextSelection) fEditor.getSelectionProvider().getSelection();

		assertEquals(result.toString(), doc.get());
		assertEquals(expectedSelection.getOffset(), postSelection.getOffset());
		assertEquals(expectedSelection.getLength(), postSelection.getLength());
	}

	protected void typeAndVerify(String typedText, String expectedResult) throws Exception {
		StringBuffer expected= new StringBuffer();
		IRegion expectedSelection= assembleMethodBodyTestCUExtractSelection(expected, expectedResult, fAfterImports);

		IDocument document= fEditor.getDocumentProvider().getDocument(fEditor.getEditorInput());
		Field declaredField= TextViewer.class.getDeclaredField("fVerifyKeyListenersManager");
		declaredField.setAccessible(true);
		VerifyKeyListener verifyKeyListeners= (VerifyKeyListener) declaredField.get(fEditor.getViewer());

		for (char c : typedText.toCharArray()) {
			Event event= new Event();
			event.widget= fEditor.getViewer().getTextWidget();
			VerifyEvent e= new VerifyEvent(event);
			e.character= c;
			e.doit= true;

			verifyKeyListeners.verifyKey(e);
			DisplayHelper.driveEventQueue(Display.getCurrent());

			if (!e.doit)
				continue;

			ITextSelection selection= (ITextSelection) fEditor.getSelectionProvider().getSelection();

			document.replace(selection.getOffset(), selection.getLength(), String.valueOf(c));
			fEditor.getSelectionProvider().setSelection(new TextSelection(selection.getOffset() + 1, 0));
		}

		assertEquals(expected.toString(), document.get());
		ITextSelection actualSelection= (ITextSelection) fEditor.getSelectionProvider().getSelection();
		assertEquals(expectedSelection.getOffset(), actualSelection.getOffset());
		assertEquals(expectedSelection.getLength(), actualSelection.getLength());
	}

	private void assertIncrementalCompletion(StringBuffer contents, IRegion preSelection, StringBuffer result, IRegion expectedSelection) throws CoreException {
		fCU= createCU(getAnonymousTestPackage(), contents.toString());
		fEditor= (JavaEditor) EditorUtility.openInEditor(fCU);
		IDocument doc;
		ITextSelection postSelection;
		try {
			incrementalAssist(preSelection);
			doc= fEditor.getDocumentProvider().getDocument(fEditor.getEditorInput());
			postSelection= (ITextSelection) fEditor.getSelectionProvider().getSelection();
		} finally {
			EditorTestHelper.closeEditor(fEditor);
			fEditor= null;
		}

		assertEquals(result.toString(), doc.get());
		assertEquals(expectedSelection.getOffset(), postSelection.getOffset());
		assertEquals(expectedSelection.getLength(), postSelection.getLength());
	}

	private void assertNoProposal(String selector, StringBuffer contents, IRegion preSelection) throws CoreException {
		fCU= createCU(getAnonymousTestPackage(), contents.toString());
		fEditor= (JavaEditor) EditorUtility.openInEditor(fCU);
		try {
			assertNull(findNamedProposal(selector, preSelection));
		} finally {
			EditorTestHelper.closeEditor(fEditor);
			fEditor= null;
		}
	}

	private IRegion assembleMethodBodyTestCUExtractSelection(StringBuffer buffer, String javadocLine, String imports) {
		String prefix= "package test1;\n" +
				imports +
				"\n" +
				"public class Completion_" + getName() + "<T> {\n" +
				"	public void testMethod(int param) {\n" +
				fLocals +
				"		";
		String postfix= "\n" +
				"	}\n" +
				fMembers +
				"}\n";
		StringBuilder lineBuffer= new StringBuilder(javadocLine);
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
		"public class Completion_" + getName() + "<T> {\n" +
		fLocals +
		"    ";
		String postfix= "\n" +
		"\n" +
		fMembers +
		"}\n";
		StringBuilder lineBuffer= new StringBuilder(javadocLine);
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

	private IRegion assembleClassJavadocTestCUExtractSelection(StringBuffer buffer, String javadocLine, String imports) {
		String prefix= "package test1;\n" +
				imports +
				 "\n" +
				 "/**\n";
		String postfix= "\n" +
				 " */\n" +
				"public class Completion_" + getName() + "<T> {\n" +
				"\n" +
				fMembers +
				"}\n";
		StringBuilder lineBuffer= new StringBuilder(javadocLine);
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

	private IRegion assembleMethodJavadocTestCUExtractSelection(StringBuffer buffer, String javadocLine) {
		String prefix= "package test1;\n" +
				"\n" +
				"public class Completion_" + getName() + "<T> {\n" +
				"	/**\n	";
		String postfix= "\n" +
				"	 */\n" +
				fMembers +
				"}\n";
		StringBuilder lineBuffer= new StringBuilder(javadocLine);
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

	@Rule
	public TestName tn= new TestName();

	protected String getName() {
		return tn.getMethodName();
	}

	private ICompletionProposal findNonNullProposal(String prefix, IRegion selection) {
		ICompletionProposal proposal= findNamedProposal(prefix, selection);
		assertNotNull("no proposal starting with \"" + prefix + "\"", proposal);
		return proposal;
	}

	private ICompletionProposal findNamedProposal(String prefix, IRegion selection) {
		ICompletionProposal found= null;
		for (ICompletionProposal proposal : collectProposals(selection)) {
			String displayString= proposal.getDisplayString();
			if (displayString.startsWith(prefix)) {
				if (found == null || displayString.equals(prefix)) {
					found= proposal;
				}
			}
		}
		return found;
	}

	private ICompletionProposal[] collectProposals(IRegion selection) {
		waitBeforeCoreCompletion();
		ContentAssistant assistant= new ContentAssistant();
		assistant.setDocumentPartitioning(IJavaPartitions.JAVA_PARTITIONING);
		JavaCompletionProcessor javaProcessor= new JavaCompletionProcessor(fEditor, assistant, getContentType());

		// Must ensure same setup of the processor as when using via UI
		Accessor accessor= new Accessor(javaProcessor, ContentAssistProcessor.class);
		accessor.invoke("setCategoryIteration", new Object[0]);
		accessor.set("fRepetition", 0);

		ICompletionProposal[] proposals= javaProcessor.computeCompletionProposals(fEditor.getViewer(), selection.getOffset());
		final String errorMessage= javaProcessor.getErrorMessage();
		if (errorMessage != null && !JavaUIMessages.JavaEditor_codeassist_noCompletions.equals(errorMessage))
			fail(errorMessage);
		return proposals;
	}

	private void incrementalAssist(IRegion selection) {
		waitBeforeCoreCompletion();
		ContentAssistant assistant= new ContentAssistant();
		assistant.enableAutoInsert(true);
		final ISourceViewer viewer= fEditor.getViewer();
		viewer.setSelectedRange(selection.getOffset(), selection.getLength());
		assistant.install(viewer);
		assistant.setDocumentPartitioning(IJavaPartitions.JAVA_PARTITIONING);
		IContentAssistProcessor javaProcessor= new JavaCompletionProcessor(fEditor, assistant, getContentType());
		assistant.setContentAssistProcessor(javaProcessor, getContentType());
		assistant.completePrefix();
		final String errorMessage= javaProcessor.getErrorMessage();
		if (errorMessage != null && !JavaUIMessages.JavaEditor_codeassist_noCompletions.equals(errorMessage))
			fail(errorMessage);
		assistant.uninstall();
	}

	/**
	 * Invokes {@link Thread#sleep(long)} if {@link #waitBeforeCompleting(boolean)} was set to
	 * <code>true</code> or camel case completions are enabled. For some reasons, inner types and
	 * camel case matches don't show up otherwise.
	 *
	 * @since 3.2
	 */
	private void waitBeforeCoreCompletion() {
		// Make sure indexer is up to date
		try {
			JavaProjectHelper.mustPerformDummySearch();
		} catch (JavaModelException e) {
			fail("could not synchronize with indexer");
		}

	    if (fWaitBeforeCompleting || JavaCore.ENABLED.equals(JavaCore.getOption(JavaCore.CODEASSIST_CAMEL_CASE_MATCH))) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException x) {
			}
		}
    }

	protected String getContentType() {
		return IDocument.DEFAULT_CONTENT_TYPE;
	}

	private void apply(ITextEditor editor, IDocument doc, ICompletionProposal proposal, IRegion selection) {
		if (proposal instanceof ICompletionProposalExtension2) {
			ICompletionProposalExtension2 ext= (ICompletionProposalExtension2) proposal;
			ITextViewer viewer= (ITextViewer) editor.getAdapter(ITextOperationTarget.class);
			ext.selected(viewer, false);
			viewer.setSelectedRange(selection.getOffset(), selection.getLength());
			ext.unselected(viewer);
			ext.apply(viewer, fTrigger, 0, selection.getOffset());
			Point range= proposal.getSelection(doc);
			if (range != null)
				viewer.setSelectedRange(range.x, range.y);
		} else if (proposal instanceof ICompletionProposalExtension) {
			ICompletionProposalExtension ext= (ICompletionProposalExtension) proposal;
			ext.apply(doc, fTrigger, selection.getOffset() + selection.getLength());
		} else {
			proposal.apply(doc);
		}
	}
}

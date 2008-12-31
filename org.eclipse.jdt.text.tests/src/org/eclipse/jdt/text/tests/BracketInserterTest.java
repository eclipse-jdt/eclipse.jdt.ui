/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.text.tests.performance.DisplayHelper;
import org.eclipse.jdt.text.tests.performance.EditorTestHelper;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Event;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.text.tests.Accessor;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedPosition;

import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;


/**
 * Tests the automatic bracket insertion feature of the CUEditor. Also tests
 * linked mode along the way.
 *
 * @since 3.1
 */
public class BracketInserterTest extends TestCase {

	private static final String SRC= "src";
	private static final String SEP= "/";
	private static final String CU_NAME= "PR75423.java";
	private static final String CU_CONTENTS= "package com.example.bugs;\n" +
				"\n" +
				"import java.lang.String;\n" +
				"import java.lang.Integer;\n" +
				"\n" +
				"public class PR75423 {\n" +
				"    String string;\n" +
				"    Integer integer;\n" +
				"\n" +
				"    public static void main(String[] args) {\n" +
				"        \n" +
				"    }\n" +
				"    void foo(String[] args) {\n" +
				"        \n" +
				"    }\n" +
				"    \n" +
				"    HashMap hm= new HashMap();\n" +
				"}\n";

	// document offsets
	private static final int BODY_OFFSET= 196;
	private static final int ARGS_OFFSET= 171;
	private static final int BRACKETS_OFFSET= 178;
	private static final int MAIN_VOID_OFFSET= 161;
	private static final int FOO_VOID_OFFSET= 207;
	private static final int FIELD_OFFSET= 264;

	public static Test suite() {
		TestSuite suite= new TestSuite(BracketInserterTest.class);
//		BracketInserterTest loop= new BracketInserterTest();
//		loop.setName("testLoop");
		return suite;
//		return loop;
	}

	private JavaEditor fEditor;
	private StyledText fTextWidget;
	private IDocument fDocument;
	private Accessor fAccessor;
	private IJavaProject fProject;

	protected void setUp() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_CLOSE_BRACKETS, true);
	}

	private void setUpProject(String sourceLevel) throws CoreException, JavaModelException {
		fProject= JavaProjectHelper.createJavaProject(getName(), "bin");
		fProject.setOption(JavaCore.COMPILER_SOURCE, sourceLevel);
		JavaProjectHelper.addSourceContainer(fProject, SRC);
		IPackageFragment fragment= fProject.findPackageFragment(new Path(SEP + getName() + SEP + SRC));
		fragment.createCompilationUnit(CU_NAME, CU_CONTENTS, true, new NullProgressMonitor());
	}

	private void setUpEditor() {
		fEditor= openJavaEditor(new Path(SEP + getName() + SEP + SRC + SEP + CU_NAME));
		assertNotNull(fEditor);
		fTextWidget= fEditor.getViewer().getTextWidget();
		assertNotNull(fTextWidget);
		fAccessor= new Accessor(fTextWidget, StyledText.class);
		fDocument= fEditor.getDocumentProvider().getDocument(fEditor.getEditorInput());
		assertNotNull(fDocument);
		assertEquals(CU_CONTENTS, fDocument.get());
	}

	private JavaEditor openJavaEditor(IPath path) {
		IFile file= ResourcesPlugin.getWorkspace().getRoot().getFile(path);
		assertTrue(file != null && file.exists());
		try {
			return (JavaEditor)EditorTestHelper.openInEditor(file, true);
		} catch (PartInitException e) {
			fail();
			return null;
		}
	}

	protected void tearDown() throws Exception {
		EditorTestHelper.closeEditor(fEditor);
		fEditor= null;
		if (fProject != null) {
			JavaProjectHelper.delete(fProject);
			fProject= null;
		}

		// reset to defaults
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_CLOSE_BRACKETS, true);
	}

	public void testInsertClosingParenthesis() throws BadLocationException, JavaModelException, CoreException, JavaModelException, CoreException {
		use14();
		setCaret(BODY_OFFSET);
		type('(');

		assertEquals("()", fDocument.get(BODY_OFFSET, 2));
		assertSingleLinkedPosition(BODY_OFFSET + 1);
	}

	public void testDeletingParenthesis() throws JavaModelException, CoreException {
		use14();
		setCaret(BODY_OFFSET);
		type('(');
		type(SWT.BS);

		assertEquals(CU_CONTENTS, fDocument.get());
		assertFalse(LinkedModeModel.hasInstalledModel(fDocument));
	}

	public void testMultipleParenthesisInsertion() throws BadLocationException, JavaModelException, CoreException {
		use14();
		setCaret(BODY_OFFSET);
		type("((((");

		assertEquals("(((())))", fDocument.get(BODY_OFFSET, 8));
		assertEquals(BODY_OFFSET + 4, getCaret());

		assertModel(true);
	}

	public void testDeletingMultipleParenthesisInertion() throws BadLocationException, JavaModelException, CoreException {
		use14();
		setCaret(BODY_OFFSET);
		type("((((");

		// delete two levels
		linkedType(SWT.BS, true, ILinkedModeListener.EXTERNAL_MODIFICATION);
		linkedType(SWT.BS, true, ILinkedModeListener.EXTERNAL_MODIFICATION);

		assertEquals("(())", fDocument.get(BODY_OFFSET, 4));
		assertEquals(BODY_OFFSET + 2, getCaret());

		// delete the second-last level
		linkedType(SWT.BS, true, ILinkedModeListener.EXTERNAL_MODIFICATION);
		assertEquals("()", fDocument.get(BODY_OFFSET, 2));
		assertEquals(BODY_OFFSET + 1, getCaret());

		// delete last level
		linkedType(SWT.BS, false, ILinkedModeListener.EXTERNAL_MODIFICATION);
		assertEquals(CU_CONTENTS, fDocument.get());
		assertEquals(BODY_OFFSET, getCaret());

		assertEquals(CU_CONTENTS, fDocument.get());
		assertFalse(LinkedModeModel.hasInstalledModel(fDocument));
	}

	public void testNoInsertInsideText() throws BadLocationException, JavaModelException, CoreException {
		use14();
		setCaret(ARGS_OFFSET);
		type('(');

		assertEquals("(St", fDocument.get(ARGS_OFFSET, 3));
		assertEquals(ARGS_OFFSET + 1, getCaret());
		assertFalse(LinkedModeModel.hasInstalledModel(fDocument));
	}

	public void testInsertInsideBrackets() throws BadLocationException, JavaModelException, CoreException {
		use14();
		setCaret(BRACKETS_OFFSET);
		type('(');

		assertEquals("()", fDocument.get(BRACKETS_OFFSET, 2));
		assertSingleLinkedPosition(BRACKETS_OFFSET + 1);
	}

	public void testPeerEntry() throws BadLocationException, JavaModelException, CoreException {
		use14();
		setCaret(BODY_OFFSET);
		type("()");

		assertEquals("()", fDocument.get(BODY_OFFSET, 2));
		assertEquals(BODY_OFFSET + 2, getCaret());

		assertFalse(LinkedModeModel.hasInstalledModel(fDocument));
	}

//	public void testLoop() throws Exception {
//		for (int i= 0; i < 50; i++) {
//			setUp();
//			testExitOnTab();
//			tearDown();
//		}
//	}
//
	public void testMultiplePeerEntry() throws BadLocationException, JavaModelException, CoreException {
		use14();
		setCaret(BODY_OFFSET);
		type("((((");

		linkedType(')', true, ILinkedModeListener.UPDATE_CARET);
		linkedType(')', true, ILinkedModeListener.UPDATE_CARET);
		linkedType(')', true, ILinkedModeListener.UPDATE_CARET);

		assertEquals("(((())))", fDocument.get(BODY_OFFSET, 8));
		assertEquals(BODY_OFFSET + 7, getCaret());

		LinkedPosition position= assertModel(false).findPosition(new LinkedPosition(fDocument, BODY_OFFSET + 1, 0));
		assertNotNull(position);
		assertEquals(BODY_OFFSET + 1, position.getOffset());
		assertEquals(6, position.getLength());

		linkedType(')', false, ILinkedModeListener.UPDATE_CARET);

		assertEquals("(((())))", fDocument.get(BODY_OFFSET, 8));
		assertEquals(BODY_OFFSET + 8, getCaret());
		assertFalse(LinkedModeModel.hasInstalledModel(fDocument));
	}

	public void testExitOnTab() throws BadLocationException, JavaModelException, CoreException {
		use14();
		setCaret(BODY_OFFSET);
		type("((((");
		linkedType('\t', true, ILinkedModeListener.NONE);

		assertEquals("(((())))", fDocument.get(BODY_OFFSET, 8));
		assertEquals(BODY_OFFSET + 5, getCaret());

		linkedType('\t', true, ILinkedModeListener.NONE);
		linkedType('\t', true, ILinkedModeListener.NONE);

		assertEquals("(((())))", fDocument.get(BODY_OFFSET, 8));
		assertEquals(BODY_OFFSET + 7, getCaret());

		linkedType('\t', false, ILinkedModeListener.NONE);

		assertEquals("(((())))", fDocument.get(BODY_OFFSET, 8));
		assertEquals(BODY_OFFSET + 8, getCaret());

		assertFalse(LinkedModeModel.hasInstalledModel(fDocument));
	}

	public void testExitOnReturn() throws BadLocationException, JavaModelException, CoreException {
		use14();
		setCaret(BODY_OFFSET);
		type("((((");
		linkedType(SWT.CR, true, ILinkedModeListener.UPDATE_CARET | ILinkedModeListener.EXIT_ALL);

		assertEquals("(((())))", fDocument.get(BODY_OFFSET, 8));
		assertEquals(BODY_OFFSET + 8, getCaret());

		assertFalse(LinkedModeModel.hasInstalledModel(fDocument));
	}

	public void testExitOnEsc() throws BadLocationException, JavaModelException, CoreException {
		use14();
		setCaret(BODY_OFFSET);
		type("((((");
		linkedType(SWT.ESC, true, ILinkedModeListener.EXIT_ALL);

		assertEquals("(((())))", fDocument.get(BODY_OFFSET, 8));
		assertEquals(BODY_OFFSET + 4, getCaret());

		assertFalse(LinkedModeModel.hasInstalledModel(fDocument));
	}

	public void testInsertClosingQuote() throws BadLocationException, JavaModelException, CoreException {
		use14();
		setCaret(BODY_OFFSET);
		type('"');

		assertEquals("\"\"", fDocument.get(BODY_OFFSET, 2));

		assertSingleLinkedPosition(BODY_OFFSET + 1);
	}

	public void testPreferences() throws BadLocationException, JavaModelException, CoreException {
		use14();
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_CLOSE_BRACKETS, false);

		setCaret(BODY_OFFSET);
		type('(');

		assertEquals("(", fDocument.get(BODY_OFFSET, 1));
		assertEquals(BODY_OFFSET + 1, getCaret());

		assertFalse(LinkedModeModel.hasInstalledModel(fDocument));
	}

	public void testAngleBracketsAsOperator() throws Exception {
		use15();
		setCaret(BODY_OFFSET);
		type("test<");

		assertEquals("test<", fDocument.get(BODY_OFFSET, 5));
		assertFalse(">".equals(fDocument.get(BODY_OFFSET + 5, 1)));
		assertEquals(BODY_OFFSET + 5, getCaret());

		assertFalse(LinkedModeModel.hasInstalledModel(fDocument));
	}

	public void testAngleBracketsIn14Project() throws BadLocationException, JavaModelException, CoreException {
		use14();
		setCaret(BODY_OFFSET);
		type("Test<");

		assertEquals("Test<", fDocument.get(BODY_OFFSET, 5));
		assertFalse(">".equals(fDocument.get(BODY_OFFSET + 5, 1)));
		assertEquals(BODY_OFFSET + 5, getCaret());

		assertFalse(LinkedModeModel.hasInstalledModel(fDocument));
	}

	public void testAngleBracketsIn15Project() throws Exception {
		use15();

		setCaret(BODY_OFFSET);
		type("Test<");

		assertEquals("Test<>", fDocument.get(BODY_OFFSET, 6));
		assertSingleLinkedPosition(BODY_OFFSET + 5);
	}

	public void testAngleBracketsInFieldDecl15() throws Exception {
		use15();

		setCaret(FIELD_OFFSET);
		type('<');

		assertEquals("HashMap<> hm", fDocument.get(FIELD_OFFSET - 7, 12));
		assertSingleLinkedPosition(FIELD_OFFSET + 1);
	}

	public void testAngleBracketsInsideMethodDecl15() throws Exception {
		use15();

		setCaret(MAIN_VOID_OFFSET);
		type('<');

		assertEquals("public static <>void", fDocument.get(MAIN_VOID_OFFSET - 14, 20));
		assertSingleLinkedPosition(MAIN_VOID_OFFSET + 1);
	}

	public void testAngleBracketsBeforeMethodDecl15() throws Exception {
		use15();

		setCaret(FOO_VOID_OFFSET);

		type('<');

		assertEquals("<>void foo", fDocument.get(FOO_VOID_OFFSET, 10));
		assertSingleLinkedPosition(FOO_VOID_OFFSET + 1);
	}

	/* utilities */

	private void assertSingleLinkedPosition(int offset) {
		assertEquals(offset, getCaret());

		LinkedPosition position= assertModel(false).findPosition(new LinkedPosition(fDocument, offset, 0));
		assertNotNull(position);
		assertEquals(offset, position.getOffset());
		assertEquals(0, position.getLength());
	}

	private void use15() throws CoreException, JavaModelException {
		setUpProject(JavaCore.VERSION_1_5);
		setUpEditor();
	}

	private void use14() throws CoreException, JavaModelException {
		setUpProject(JavaCore.VERSION_1_4);
		setUpEditor();
	}

	/**
	 * Type characters into the styled text.
	 *
	 * @param characters the characters to type
	 */
	private void type(CharSequence characters) {
		for (int i= 0; i < characters.length(); i++)
			type(characters.charAt(i), 0, 0);
	}

	/**
	 * Type a character into the styled text.
	 *
	 * @param character the character to type
	 */
	private void type(char character) {
		type(character, 0, 0);
	}

	/**
	 * Ensure there is a linked mode and type a character into the styled text.
	 *
	 * @param character the character to type
	 * @param nested whether the linked mode is expected to be nested or not
	 * @param expectedExitFlags the expected exit flags for the current linked mode after typing the character, -1 for no exit
	 */
	private void linkedType(char character, boolean nested, int expectedExitFlags) {
		final int[] exitFlags= { -1 };
		assertModel(nested).addLinkingListener(new ILinkedModeListener() {
			public void left(LinkedModeModel model, int flags) {
				exitFlags[0]= flags;
			}
			public void resume(LinkedModeModel model, int flags) {
			}
			public void suspend(LinkedModeModel model) {
			}
		});
		type(character, 0, 0);
		assertEquals(expectedExitFlags, exitFlags[0]);
	}

	private LinkedModeModel assertModel(boolean nested) {
		LinkedModeModel model= LinkedModeModel.getModel(fDocument, 0); // offset does not matter
		assertNotNull(model);
		assertEquals(nested, model.isNested());
		return model;
	}

	/**
	 * Type a character into the styled text.
	 *
	 * @param character the character to type
	 * @param keyCode the key code
	 * @param stateMask the state mask
	 */
	private void type(char character, int keyCode, int stateMask) {
		Event event= new Event();
		event.character= character;
		event.keyCode= keyCode;
		event.stateMask= stateMask;
		fAccessor.invoke("handleKeyDown", new Object[] {event});

		new DisplayHelper() {
			protected boolean condition() {
				return false;
			}
		}.waitForCondition(EditorTestHelper.getActiveDisplay(), 200);

	}

	private int getCaret() {
		return ((ITextSelection) fEditor.getSelectionProvider().getSelection()).getOffset();
	}

	private void setCaret(int offset) {
		fEditor.getSelectionProvider().setSelection(new TextSelection(offset, 0));
		int newOffset= ((ITextSelection)fEditor.getSelectionProvider().getSelection()).getOffset();
		assertEquals(offset, newOffset);
	}
}

/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.text.tests.Accessor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Event;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedPosition;

import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.text.tests.performance.EditorTestHelper;

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

	private static final String PROJECT= "BracketInserterTest";
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
				"    " +
				"    HashMap hm= new HashMap();" +
				"}\n";
	
	// document offsets 
	private static final int BODY_OFFSET= 196;
	private static final int ARGS_OFFSET= 171;
	private static final int BRACKETS_OFFSET= 178;
	private static final int MAIN_VOID_OFFSET= 161;
	private static final int FOO_VOID_OFFSET= 207;
	private static final int FIELD_OFFSET= 263;
	
	public static Test suite() {
		return new TestSuite(BracketInserterTest.class);
	}
	
	private JavaEditor fEditor;
	private StyledText fTextWidget;
	private IDocument fDocument;
	private Accessor fAccessor;
	private IJavaProject fProject;

	protected void setUp() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_CLOSE_BRACKETS, true);

		setUpProject(JavaCore.VERSION_1_4);
		setUpEditor();
	}
	
	private void setUpProject(String sourceLevel) throws CoreException, JavaModelException {
		fProject= JavaProjectHelper.createJavaProject(PROJECT, "bin");
		fProject.setOption(JavaCore.COMPILER_SOURCE, sourceLevel);
		JavaProjectHelper.addSourceContainer(fProject, SRC);
		IPackageFragment fragment= fProject.findPackageFragment(new Path(SEP + PROJECT + SEP + SRC));
		fragment.createCompilationUnit(CU_NAME, CU_CONTENTS, true, new NullProgressMonitor());
	}

	private void setUpEditor() {
		fEditor= openJavaEditor(new Path(SEP + PROJECT + SEP + SRC + SEP + CU_NAME));
		assertNotNull(fEditor);
		fTextWidget= fEditor.getViewer().getTextWidget();
		assertNotNull(fTextWidget);
		fAccessor= new Accessor(fTextWidget, StyledText.class);
		fDocument= fEditor.getDocumentProvider().getDocument(fEditor.getEditorInput());
		assertNotNull(fDocument);
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
		EditorTestHelper.closeAllEditors();
		JavaProjectHelper.delete(fProject);

		// reset to defaults
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_CLOSE_BRACKETS, true);
	}
	
	public void testInsertClosingParenthesis() throws BadLocationException {
		setCaret(BODY_OFFSET);
		type('(');
		
		assertEquals("()", fDocument.get(BODY_OFFSET, 2));
		assertSingleLinkedPosition(BODY_OFFSET + 1);
	}
	
	public void testDeletingParenthesis() {
		setCaret(BODY_OFFSET);
		type('(');
		type(SWT.BS);
		
		assertEquals(CU_CONTENTS, fDocument.get());
		assertFalse(LinkedModeModel.hasInstalledModel(fDocument));
	}
	
	public void testMultipleParenthesisInsertion() throws BadLocationException {
		setCaret(BODY_OFFSET);
		type('(');
		type('(');
		type('(');
		type('(');
		
		assertEquals("(((())))", fDocument.get(BODY_OFFSET, 8));
		assertEquals(BODY_OFFSET + 4, getCaret());
		
		LinkedModeModel model= LinkedModeModel.getModel(fDocument, BODY_OFFSET + 4);
		assertNotNull(model);
		assertTrue(model.isNested());
	}
	
	public void testDeletingMultipleParenthesisInertion() throws BadLocationException {
		setCaret(BODY_OFFSET);
		type('(');
		type('(');
		type('(');
		type('(');
		
		// delete two levels
		type(SWT.BS);
		type(SWT.BS);

		assertEquals("(())", fDocument.get(BODY_OFFSET, 4));
		assertEquals(BODY_OFFSET + 2, getCaret());
		
		LinkedModeModel model= LinkedModeModel.getModel(fDocument, BODY_OFFSET + 2);
		assertNotNull(model);
		assertTrue("models must be nested", model.isNested());
		
		// delete the second-last level
		type(SWT.BS);
		assertEquals("()", fDocument.get(BODY_OFFSET, 2));
		assertEquals(BODY_OFFSET + 1, getCaret());
		
		model= LinkedModeModel.getModel(fDocument, BODY_OFFSET + 1);
		assertNotNull(model);
		assertFalse("model must not be nested", model.isNested());
		
		// delete last level
		type(SWT.BS);
		assertEquals(CU_CONTENTS, fDocument.get());
		assertEquals(BODY_OFFSET, getCaret());
		
		assertEquals(CU_CONTENTS, fDocument.get());
		assertFalse("no linked model must exist", LinkedModeModel.hasInstalledModel(fDocument));
	}
	
	public void testNoInsertInsideText() throws BadLocationException {
		setCaret(ARGS_OFFSET);
		type('(');
		
		assertEquals("(St", fDocument.get(ARGS_OFFSET, 3));
		assertEquals(ARGS_OFFSET + 1, getCaret());
		assertFalse(LinkedModeModel.hasInstalledModel(fDocument));
	}
	
	public void testInsertInsideBrackets() throws BadLocationException {
		setCaret(BRACKETS_OFFSET);
		type('(');
		
		assertEquals("()", fDocument.get(BRACKETS_OFFSET, 2));
		assertSingleLinkedPosition(BRACKETS_OFFSET + 1);
	}
	
	public void testPeerEntry() throws BadLocationException {
		setCaret(BODY_OFFSET);
		type('(');
		typeAndRun(')');
		
		assertEquals("()", fDocument.get(BODY_OFFSET, 2));
		assertEquals(BODY_OFFSET + 2, getCaret());

		assertFalse(LinkedModeModel.hasInstalledModel(fDocument));
	}
	
	public void testMultiplePeerEntry() throws BadLocationException {
		setCaret(BODY_OFFSET);
		type('(');
		type('(');
		type('(');
		type('(');

		typeAndRun(')');
		typeAndRun(')');
		typeAndRun(')');
		
		assertEquals("(((())))", fDocument.get(BODY_OFFSET, 8));
		assertEquals(BODY_OFFSET + 7, getCaret());
		
		LinkedModeModel model= LinkedModeModel.getModel(fDocument, BODY_OFFSET + 4);
		assertNotNull(model);
		assertFalse(model.isNested());
		LinkedPosition position= model.findPosition(new LinkedPosition(fDocument, BODY_OFFSET + 1, 0));
		assertNotNull(position);
		assertEquals(BODY_OFFSET + 1, position.getOffset());
		assertEquals(6, position.getLength());
		
		typeAndRun(')');
		
		assertEquals("(((())))", fDocument.get(BODY_OFFSET, 8));
		assertEquals(BODY_OFFSET + 8, getCaret());
		assertFalse(LinkedModeModel.hasInstalledModel(fDocument));
	}
	
	public void testExitOnTab() throws BadLocationException {
		setCaret(BODY_OFFSET);
		type('(');
		type('(');
		type('(');
		type('(');
		typeAndRun('\t');
		
		assertEquals("(((())))", fDocument.get(BODY_OFFSET, 8));
		assertEquals(BODY_OFFSET + 5, getCaret());

		LinkedModeModel model= LinkedModeModel.getModel(fDocument, BODY_OFFSET + 5);
		assertNotNull(model);
		assertTrue(model.isNested());
		
		typeAndRun('\t');
		typeAndRun('\t');
		
		assertEquals("(((())))", fDocument.get(BODY_OFFSET, 8));
		assertEquals(BODY_OFFSET + 7, getCaret());

		model= LinkedModeModel.getModel(fDocument, BODY_OFFSET + 7);
		assertNotNull(model);
		assertFalse(model.isNested());
		
		typeAndRun('\t');
		
		assertEquals("(((())))", fDocument.get(BODY_OFFSET, 8));
		assertEquals(BODY_OFFSET + 8, getCaret());

		assertFalse(LinkedModeModel.hasInstalledModel(fDocument));
	}
	
	public void testExitOnReturn() throws BadLocationException {
		setCaret(BODY_OFFSET);
		type('(');
		type('(');
		type('(');
		type('(');
		typeAndRun(SWT.CR);
		
		assertEquals("(((())))", fDocument.get(BODY_OFFSET, 8));
		assertEquals(BODY_OFFSET + 8, getCaret());

		assertFalse(LinkedModeModel.hasInstalledModel(fDocument));
	}
	
	public void testExitOnEsc() throws BadLocationException {
		setCaret(BODY_OFFSET);
		type('(');
		type('(');
		type('(');
		type('(');
		typeAndRun(SWT.ESC);
		
		assertEquals("(((())))", fDocument.get(BODY_OFFSET, 8));
		assertEquals(BODY_OFFSET + 4, getCaret());

		assertFalse(LinkedModeModel.hasInstalledModel(fDocument));
	}
	
	public void testInsertClosingQuote() throws BadLocationException {
		setCaret(BODY_OFFSET);
		type('"');
		
		assertEquals("\"\"", fDocument.get(BODY_OFFSET, 2));
		
		assertSingleLinkedPosition(BODY_OFFSET + 1);
	}
	
	public void testPreferences() throws BadLocationException {
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
	
	public void testAngleBracketsIn14Project() throws BadLocationException {
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
		
		LinkedModeModel model= LinkedModeModel.getModel(fDocument, offset);
		assertNotNull(model);
		assertFalse(model.isNested());
		LinkedPosition position= model.findPosition(new LinkedPosition(fDocument, offset, 0));
		assertNotNull(position);
		assertEquals(offset, position.getOffset());
		assertEquals(0, position.getLength());
	}
	
	private void use15() throws Exception, CoreException, JavaModelException {
		tearDown();
		setUpProject(JavaCore.VERSION_1_5);
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
	}

	/**
	 * Type a character into the styled text and drive the event loop.
	 * 
	 * @param character the character to type
	 */
	private void typeAndRun(char character) {
		type(character);
		runEvents();
	}

	private int getCaret() {
		return ((ITextSelection) fEditor.getSelectionProvider().getSelection()).getOffset();
	}

	private void setCaret(int offset) {
		fEditor.getSelectionProvider().setSelection(new TextSelection(offset, 0));
	}

	private void runEvents() {
		while (fTextWidget.getDisplay().readAndDispatch()) {
		}
	}

}

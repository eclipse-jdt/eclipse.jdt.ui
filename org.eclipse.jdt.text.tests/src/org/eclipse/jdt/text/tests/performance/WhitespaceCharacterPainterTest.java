/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.text.tests.performance;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ui.internal.editors.text.EditorsPlugin;

import org.eclipse.ui.texteditor.AbstractTextEditor;

/**
 * Measure the time spent while scrolling in the text editor with 'Show whitespace characters'
 * option enabled.
 * 
 * @since 3.7
 */
public class WhitespaceCharacterPainterTest extends ScrollEditorTest {

	private static final Class THIS= WhitespaceCharacterPainterTest.class;

	private IPreferenceStore fPreferenceStore= EditorsPlugin.getDefault().getPreferenceStore();

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	protected String getEditor() {
		return EditorTestHelper.TEXT_EDITOR_ID;
	}

	protected void setUp() throws Exception {
		super.setUp();
		fPreferenceStore.setValue(AbstractTextEditor.PREFERENCE_SHOW_WHITESPACE_CHARACTERS, true);
	}

	protected void tearDown() throws Exception {
		fPreferenceStore.setValue(AbstractTextEditor.PREFERENCE_SHOW_WHITESPACE_CHARACTERS, false);
		super.tearDown();
	}

	/**
	 * Measure the time spent while scrolling page wise in the text editor.
	 * 
	 * @throws Exception if something goes wrong
	 */
	public void testScrollTextEditorPageWise() throws Exception {
		measure(PAGE_WISE);
	}

	/**
	 * Measure the time spent while scrolling line wise in the text editor.
	 * 
	 * @throws Exception if something goes wrong
	 */
	public void testScrollTextEditorLineWiseMoveCaret2() throws Exception {
		measure(LINE_WISE);
	}

	/**
	 * Measure the time spent while scrolling and selecting line wise in the text editor.
	 * 
	 * @throws Exception if something goes wrong
	 */
	public void testScrollTextEditorLineWiseSelect2() throws Exception {
		measure(LINE_WISE_SELECT);
	}

	/**
	 * Measure the time spent while scrolling line wise without moving the caret in the text editor.
	 * 
	 * @throws Exception if something goes wrong
	 */
	public void testScrollTextEditorLineWise2() throws Exception {
		measure(LINE_WISE_NO_CARET_MOVE);
	}
}

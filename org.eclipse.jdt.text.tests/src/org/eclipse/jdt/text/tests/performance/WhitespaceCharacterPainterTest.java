/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
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

package org.eclipse.jdt.text.tests.performance;

import java.lang.reflect.Field;
import java.util.Set;

import org.eclipse.test.internal.performance.PerformanceMeterFactory;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ui.internal.editors.text.EditorsPlugin;

import org.eclipse.ui.texteditor.AbstractTextEditor;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Measure the time spent while scrolling in the text editor with 'Show whitespace characters'
 * option enabled.
 *
 * @since 3.7
 */
public class WhitespaceCharacterPainterTest extends ScrollEditorTest {

	private static final Class<WhitespaceCharacterPainterTest> THIS= WhitespaceCharacterPainterTest.class;

	private IPreferenceStore fPreferenceStore= EditorsPlugin.getDefault().getPreferenceStore();

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new PerformanceTestSetup(test) {
			@Override
			protected void setUp() throws Exception {
				// reset the PerformanceMeterFactory, so that the same scenario can be run again:
				Field field = PerformanceMeterFactory.class.getDeclaredField("fScenarios");
				field.setAccessible(true);
				Set<?> set = (Set<?>) field.get(null);
				set.clear();

				super.setUp();
			}
		};
	}

	@Override
	protected String getEditor() {
		return EditorTestHelper.TEXT_EDITOR_ID;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		fPreferenceStore.setValue(AbstractTextEditor.PREFERENCE_SHOW_WHITESPACE_CHARACTERS, true);
	}

	@Override
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

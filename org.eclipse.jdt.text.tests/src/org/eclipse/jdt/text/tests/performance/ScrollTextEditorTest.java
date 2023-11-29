/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * Measure the time spent while scrolling in the text editor.
 *
 * @since 3.1
 */
public class ScrollTextEditorTest extends ScrollEditorTest {

	private static final boolean BUG_HOLDING_SCROLL_TESTS_DISABLED= true;
	private static final Class<ScrollTextEditorTest> THIS= ScrollTextEditorTest.class;

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	@Override
	protected String getEditor() {
		return EditorTestHelper.TEXT_EDITOR_ID;
	}

	/**
	 * Measure the time spent while scrolling page wise in the text editor.
	 */
	public void testScrollTextEditorPageWise() throws Exception {
		measure(PAGE_WISE);
	}

	/**
	 * Measure the time spent while scrolling line wise in the text editor.
	 */
	public void testScrollTextEditorLineWiseMoveCaret2() throws Exception {
		measure(LINE_WISE);
	}

	/**
	 * Measure the time spent while scrolling and selecting line wise in the
	 * text editor.
	 */
	public void testScrollTextEditorLineWiseSelect2() throws Exception {
		measure(LINE_WISE_SELECT);
	}

	/**
	 * Measure the time spent while scrolling line wise without moving the
	 * caret in the text editor.
	 */
	public void testScrollTextEditorLineWise2() throws Exception {
		measure(LINE_WISE_NO_CARET_MOVE);
	}

	/**
	 * Measure the time spent while scrolling line wise without moving the
	 * caret in the text editor, holding the key combination down.
	 */
	public void testScrollTextEditorLineWiseSelectHoldKeys() throws Exception {
		if (BUG_HOLDING_SCROLL_TESTS_DISABLED) {
			System.out.println("holding scroll tests disabled");
			return;
		}
		measure(LINE_WISE_SELECT_HOLD_KEYS);
	}

	/**
	 * Measure the time spent while scrolling line wise without moving the
	 * caret in the text editor.
	 */
	public void testScrollTextEditorLineWiseHoldKeys() throws Exception {
		if (BUG_HOLDING_SCROLL_TESTS_DISABLED) {
			System.out.println("holding scroll tests disabled");
			return;
		}
		measure(LINE_WISE_NO_CARET_MOVE_HOLD_KEYS);
	}
}

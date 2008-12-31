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

package org.eclipse.jdt.text.tests.performance;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Measure the time spent while scrolling in the text editor.
 *
 * @since 3.1
 */
public class ScrollTextEditorTest extends ScrollEditorTest {

	private static final Class THIS= ScrollTextEditorTest.class;

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	protected String getEditor() {
		return EditorTestHelper.TEXT_EDITOR_ID;
	}

	/**
	 * Measure the time spent while scrolling page wise in the text editor.
	 *
	 * @throws Exception
	 */
	public void testScrollTextEditorPageWise() throws Exception {
		measure(PAGE_WISE);
	}

	/**
	 * Measure the time spent while scrolling line wise in the text editor.
	 *
	 * @throws Exception
	 */
	public void testScrollTextEditorLineWiseMoveCaret2() throws Exception {
		measure(LINE_WISE);
	}

	/**
	 * Measure the time spent while scrolling and selecting line wise in the
	 * text editor.
	 *
	 * @throws Exception
	 */
	public void testScrollTextEditorLineWiseSelect2() throws Exception {
		measure(LINE_WISE_SELECT);
	}

	/**
	 * Measure the time spent while scrolling line wise without moving the
	 * caret in the text editor.
	 *
	 * @throws Exception
	 */
	public void testScrollTextEditorLineWise2() throws Exception {
		measure(LINE_WISE_NO_CARET_MOVE);
	}

	/**
	 * Measure the time spent while scrolling line wise without moving the
	 * caret in the Java editor, holding the key combination down.
	 *
	 * @throws Exception
	 */
	public void testScrollTextEditorLineWiseSelectHoldKeys() throws Exception {
		if (true) {
			System.out.println("holding scroll tests disabled");
			return;
		}
		measure(LINE_WISE_SELECT_HOLD_KEYS);
	}

	/**
	 * Measure the time spent while scrolling line wise without moving the
	 * caret in the text editor.
	 *
	 * @throws Exception
	 */
	public void testScrollTextEditorLineWiseHoldKeys() throws Exception {
		if (true) {
			System.out.println("holding scroll tests disabled");
			return;
		}
		measure(LINE_WISE_NO_CARET_MOVE_HOLD_KEYS);
	}
}

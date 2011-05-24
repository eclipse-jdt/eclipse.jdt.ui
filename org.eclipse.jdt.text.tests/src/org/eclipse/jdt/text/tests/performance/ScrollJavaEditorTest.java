/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
 * Measure the time spent while scrolling in the Java editor.
 *
 * @since 3.1
 */
public class ScrollJavaEditorTest extends ScrollEditorTest {

	private static final boolean BUG_HOLDING_SCROLL_TESTS_DISABLED= true;
	private static final Class THIS= ScrollJavaEditorTest.class;

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	protected String getEditor() {
		return EditorTestHelper.COMPILATION_UNIT_EDITOR_ID;
	}

	/**
	 * Measure the time spent while scrolling page wise in the Java editor.
	 *
	 * @throws Exception
	 */
	public void testScrollJavaEditorPageWise() throws Exception {
		measure(PAGE_WISE);
	}

	/**
	 * Measure the time spent while scrolling line wise in the Java editor.
	 *
	 * @throws Exception
	 */
	public void testScrollJavaEditorLineWiseMoveCaret2() throws Exception {
		measure(LINE_WISE);
	}

	/**
	 * Measure the time spent while scrolling and selecting line wise in the
	 * Java editor.
	 *
	 * @throws Exception
	 */
	public void testScrollJavaEditorLineWiseSelect2() throws Exception {
		measure(LINE_WISE_SELECT);
	}

	/**
	 * Measure the time spent while scrolling line wise without moving the
	 * caret in the Java editor.
	 *
	 * @throws Exception
	 */
	public void testScrollJavaEditorLineWise2() throws Exception {
		measure(LINE_WISE_NO_CARET_MOVE);
	}

	/**
	 * Measure the time spent while scrolling line wise without moving the
	 * caret in the Java editor, holding the key combination down.
	 *
	 * @throws Exception
	 */
	public void testScrollJavaEditorLineWiseSelectHoldKeys() throws Exception {
		if (BUG_HOLDING_SCROLL_TESTS_DISABLED) {
			System.out.println("holding scroll tests disabled");
			return;
		}
		measure(LINE_WISE_SELECT_HOLD_KEYS);
	}

	/**
	 * Measure the time spent while scrolling line wise without moving the
	 * caret in the Java editor, holding the key combination down.
	 *
	 * @throws Exception
	 */
	public void testScrollJavaEditorLineWiseHoldKeys() throws Exception {
		if (BUG_HOLDING_SCROLL_TESTS_DISABLED) {
			System.out.println("holding scroll tests disabled");
			return;
		}
		measure(LINE_WISE_NO_CARET_MOVE_HOLD_KEYS);
	}
}

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

package org.eclipse.jdt.text.tests.performance;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ScrollJavaEditorTest extends ScrollEditorTest {
	
	private static final Class THIS= ScrollJavaEditorTest.class;
	
	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	protected String getEditor() {
		return EditorTestHelper.COMPILATION_UNIT_EDITOR_ID;
	}

	public void testScrollJavaEditorLineWise2() throws Exception {
		measure(LINE_SCROLLING_FILE, LINE_WISE_NO_CARET_MOVE);
	}

	public void testScrollJavaEditorPageWise() throws Exception {
		measure(PAGE_SCROLLING_FILE, PAGE_WISE);
	}

	public void testScrollJavaEditorLineWiseMoveCaret2() throws Exception {
		measure(LINE_SCROLLING_FILE, LINE_WISE);
	}

	public void testScrollJavaEditorLineWiseSelect2() throws Exception {
		measure(LINE_SCROLLING_FILE, LINE_WISE_SELECT);
	}
}

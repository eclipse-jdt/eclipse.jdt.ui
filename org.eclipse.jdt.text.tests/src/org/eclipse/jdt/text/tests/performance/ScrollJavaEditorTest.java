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
	
	private static final String PAGE_SCROLLING_FILE= "/org.eclipse.swt/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/StyledText.java";

	private static final String LINE_SCROLLING_FILE= "/org.eclipse.swt/Eclipse SWT/win32/org/eclipse/swt/graphics/TextLayout.java";

	private static final int N_OF_RUNS= 6;

	private static final int N_OF_COLD_RUNS= 3;

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	public void testScrollJavaEditorLineWise2() throws Exception {
		measureScrolling(LINE_SCROLLING_FILE, LINE_WISE_NO_CARET_MOVE, false, N_OF_RUNS, N_OF_COLD_RUNS);
	}

	public void testScrollJavaEditorPageWise() throws Exception {
		measureScrolling(PAGE_SCROLLING_FILE, PAGE_WISE, false, N_OF_RUNS, N_OF_COLD_RUNS);
	}

	public void testScrollJavaEditorLineWisePreloaded2() throws Exception {
		measureScrolling(LINE_SCROLLING_FILE, LINE_WISE_NO_CARET_MOVE, true, N_OF_RUNS, N_OF_COLD_RUNS);
	}

	public void testScrollJavaEditorPageWisePreloaded2() throws Exception {
		measureScrolling(PAGE_SCROLLING_FILE, PAGE_WISE, true, N_OF_RUNS, N_OF_COLD_RUNS);
	}

	public void testScrollJavaEditorLineWiseMoveCaret2() throws Exception {
		measureScrolling(LINE_SCROLLING_FILE, LINE_WISE, false, N_OF_RUNS, N_OF_COLD_RUNS);
	}

	public void testScrollJavaEditorLineWiseMoveCaretPreloaded2() throws Exception {
		measureScrolling(LINE_SCROLLING_FILE, LINE_WISE, true, N_OF_RUNS, N_OF_COLD_RUNS);
	}

	public void testScrollJavaEditorLineWiseSelect2() throws Exception {
		measureScrolling(LINE_SCROLLING_FILE, LINE_WISE_SELECT, false, N_OF_RUNS, N_OF_COLD_RUNS);
	}
}

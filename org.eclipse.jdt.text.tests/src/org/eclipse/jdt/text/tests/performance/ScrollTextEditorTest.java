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


public class ScrollTextEditorTest extends ScrollEditorTest {
	
	private static final Class THIS= ScrollTextEditorTest.class;
	
	private static final String PAGE_FILE_PREFIX= "/org.eclipse.swt/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/StyledText";
	
	private static final String ORIG_PAGE_FILE= PAGE_FILE_PREFIX + ".java";
	
	private static final String PAGE_FILE= PAGE_FILE_PREFIX + ".txt";
	
	private static final String LINE_FILE_PREFIX= "/org.eclipse.swt/Eclipse SWT/win32/org/eclipse/swt/graphics/TextLayout";
	
	private static final String ORIG_LINE_FILE= LINE_FILE_PREFIX + ".java";
	
	private static final String LINE_FILE= LINE_FILE_PREFIX + ".txt";
	
	private static final int WARM_UP_RUNS= 3;
	
	private static final int MEASURED_RUNS= 3;
	
	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
	}

	public void testScrollTextEditorLineWise2() throws Exception {
		try {
			ResourceTestHelper.copy(ORIG_LINE_FILE, LINE_FILE);
			measureScrolling(LINE_FILE, LINE_WISE_NO_CARET_MOVE);
		} finally {
			ResourceTestHelper.delete(LINE_FILE);
		}
	}
	
	public void testScrollTextEditorPageWise() throws Exception {
		try {
			ResourceTestHelper.copy(ORIG_PAGE_FILE, PAGE_FILE);
			measureScrolling(PAGE_FILE, PAGE_WISE);
		} finally {
			ResourceTestHelper.delete(PAGE_FILE);
		}
	}
	
	public void testScrollTextEditorLineWiseMoveCaret2() throws Exception {
		try {
			ResourceTestHelper.copy(ORIG_LINE_FILE, LINE_FILE);
			measureScrolling(LINE_FILE, LINE_WISE);
		} finally {
			ResourceTestHelper.delete(LINE_FILE);
		}
	}
	
	public void testScrollTextEditorLineWiseSelect2() throws Exception {
		try {
			ResourceTestHelper.copy(ORIG_LINE_FILE, LINE_FILE);
			measureScrolling(LINE_FILE, LINE_WISE_SELECT);
		} finally {
			ResourceTestHelper.delete(LINE_FILE);
		}
	}
}

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

import org.eclipse.core.resources.IFile;

public class ScrollTextEditorTest extends ScrollEditorTest {
	
	private static final String FILE_PREFIX= "/org.eclipse.swt/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/StyledText";
	
	private static final String ORIG_FILE= FILE_PREFIX + ".java";

	private static final String FILE= FILE_PREFIX + ".txt";

	private static final int N_OF_RUNS= 10;

	private IFile fFile;

	protected void setUp() throws Exception {
		ResourceTestHelper.copy(ORIG_FILE, FILE);
		super.setUp();
	}
	
	protected void tearDown() throws Exception {
		super.tearDown();
		ResourceTestHelper.delete(FILE);
	}
	
	public void testScrollTextEditorLineWise1() {
		setScrollingMode(LINE_WISE_NO_CARET_MOVE);
		setPreloadEvents(false);
		measureScrolling(N_OF_RUNS);
	}

	public void testScrollTextEditorLineWise2() {
		setScrollingMode(LINE_WISE_NO_CARET_MOVE);
		setPreloadEvents(false);
		measureScrolling(N_OF_RUNS);
	}

	public void testScrollTextEditorPageWise() {
		setScrollingMode(PAGE_WISE);
		setPreloadEvents(false);
		measureScrolling(N_OF_RUNS);
	}

	public void testScrollTextEditorLineWisePreloaded1() {
		setScrollingMode(LINE_WISE_NO_CARET_MOVE);
		setPreloadEvents(true);
		measureScrolling(N_OF_RUNS);
	}
	
	public void testScrollTextEditorLineWisePreloaded2() {
		setScrollingMode(LINE_WISE_NO_CARET_MOVE);
		setPreloadEvents(true);
		measureScrolling(N_OF_RUNS);
	}

	public void testScrollTextEditorPageWisePreloaded1() {
		setScrollingMode(PAGE_WISE);
		setPreloadEvents(true);
		measureScrolling(N_OF_RUNS);
	}

	public void testScrollTextEditorPageWisePreloaded2() {
		setScrollingMode(PAGE_WISE);
		setPreloadEvents(true);
		measureScrolling(N_OF_RUNS);
	}

	public void testScrollTextEditorLineWiseMoveCaret1() {
		setScrollingMode(LINE_WISE);
		setPreloadEvents(false);
		measureScrolling(N_OF_RUNS);
	}
	
	public void testScrollTextEditorLineWiseMoveCaret2() {
		setScrollingMode(LINE_WISE);
		setPreloadEvents(false);
		measureScrolling(N_OF_RUNS);
	}

	public void testScrollTextEditorLineWiseMoveCaretPreloaded1() {
		setScrollingMode(LINE_WISE);
		setPreloadEvents(true);
		measureScrolling(N_OF_RUNS);
	}
	
	public void testScrollTextEditorLineWiseMoveCaretPreloaded2() {
		setScrollingMode(LINE_WISE);
		setPreloadEvents(true);
		measureScrolling(N_OF_RUNS);
	}

	public void testScrollTextEditorLineWiseSelect1() {
		setScrollingMode(LINE_WISE_SELECT);
		setPreloadEvents(false);
		measureScrolling(N_OF_RUNS);
	}
	
	public void testScrollTextEditorLineWiseSelect2() {
		setScrollingMode(LINE_WISE_SELECT);
		setPreloadEvents(false);
		measureScrolling(N_OF_RUNS);
	}

	protected IFile getFile() {
		if (fFile == null)
			fFile= ResourceTestHelper.findFile(FILE);
		return fFile;
	}
}

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

import junit.framework.TestCase;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.ui.IEditorPart;

public abstract class ScrollEditorTest extends TestCase {
	
	private static final int[] CTRL_HOME= new int[] { SWT.CTRL, SWT.HOME };
	private static final int[] CTRL_DOWN= new int[] { SWT.CTRL, SWT.ARROW_DOWN };
	private static final int[] PG_DOWN= new int[] { SWT.PAGE_DOWN };
	private static final int[] SHIFT_PG_DOWN= new int[] { SWT.SHIFT, SWT.PAGE_DOWN };
	private static final int[] DOWN= new int[] { SWT.ARROW_DOWN };
	private static final int[] SHIFT_DOWN= new int[] { SWT.SHIFT, SWT.ARROW_DOWN };
	private static final int[] PG_UP= new int[] { SWT.PAGE_UP };
	
	protected abstract static class ScrollingMode {
		ScrollingMode(int[] scroll_combo, int[] home_combo) {
			super();
			SCROLL_COMBO= scroll_combo;
			HOME_COMBO= home_combo;
		}
		final int[] SCROLL_COMBO;
		final int[] HOME_COMBO;
		abstract int computeOperations(int numberOfLines, int visibleLines);
	}
	
	public static final ScrollingMode PAGE_WISE= new ScrollingMode(PG_DOWN, CTRL_HOME) {
		int computeOperations(int numberOfLines, int visibleLines) {
			return numberOfLines / visibleLines;
		}
	};
	public static final ScrollingMode PAGE_WISE_SELECT= new ScrollingMode(SHIFT_PG_DOWN, CTRL_HOME) {
		int computeOperations(int numberOfLines, int visibleLines) {
			return numberOfLines / visibleLines;
		}
	};
	public static final ScrollingMode LINE_WISE_NO_CARET_MOVE= new ScrollingMode(CTRL_DOWN, PG_UP) {
		int computeOperations(int numberOfLines, int visibleLines) {
			return numberOfLines - visibleLines;
		}
	};
	public static final ScrollingMode LINE_WISE= new ScrollingMode(DOWN, CTRL_HOME) {
		int computeOperations(int numberOfLines, int visibleLines) {
			return numberOfLines - 1;
		}
	};
	public static final ScrollingMode LINE_WISE_SELECT= new ScrollingMode(SHIFT_DOWN, CTRL_HOME) {
		int computeOperations(int numberOfLines, int visibleLines) {
			return numberOfLines - 1;
		}
	};
	
	private PerformanceMeter fPerformanceMeter;
	
	protected void setUp() throws Exception {
		Performance performance= Performance.getDefault();
		fPerformanceMeter= performance.createPerformanceMeter(performance.getDefaultScenarioId(this));
		EditorTestHelper.bringToTop();
	}
	
	protected void tearDown() throws Exception {
		fPerformanceMeter.dispose();
	}
	
	protected void measureScrolling(String file, ScrollingMode mode, boolean preload, int nOfRuns) throws Exception {
		IEditorPart editor= null;
		try {
			editor= EditorTestHelper.openInEditor(ResourceTestHelper.findFile(file), true);
			EditorTestHelper.joinJobs(3000, 10000, 100);
			
			setUp(editor);
			
			Display display= EditorTestHelper.getActiveDisplay();
			
			StyledText text= (StyledText) editor.getAdapter(Control.class);
			int numberOfLines= text.getLineCount();
			int visibleLinesInViewport= text.getClientArea().height / text.getLineHeight();
			int operations= mode.computeOperations(numberOfLines, visibleLinesInViewport);
			
			for (int i= 0; i < nOfRuns; i++) {
				if (preload) {
					for (int j= 0; j < operations; j++) {
						// avoid overhead: assertTrue(text.getTopIndex() + visibleLinesInViewport < numberOfLines - 1);
						SWTEventHelper.pressKeyCodeCombination(display, mode.SCROLL_COMBO, false);
					}
					fPerformanceMeter.start();
					EditorTestHelper.runEventQueue(100);
					fPerformanceMeter.stop();
				} else {
					fPerformanceMeter.start();
					for (int j= 0; j < operations; j++) {
						// avoid overhead: assertTrue(text.getTopIndex() + visibleLinesInViewport < numberOfLines - 1);
						SWTEventHelper.pressKeyCodeCombination(display, mode.SCROLL_COMBO);
					}
					fPerformanceMeter.stop();
					EditorTestHelper.runEventQueue(100);
				}
				assertTrue("TopIndex: "+text.getTopIndex() + " visibleLines: "+visibleLinesInViewport + " totalLines: " + numberOfLines + " operations: " + operations, text.getTopIndex() + visibleLinesInViewport >= numberOfLines - 1);
				SWTEventHelper.pressKeyCodeCombination(display, mode.HOME_COMBO);
				EditorTestHelper.runEventQueue(100);
				assertEquals(0, text.getTopIndex());
			}
			fPerformanceMeter.commit();
			Performance.getDefault().assertPerformance(fPerformanceMeter);
		} finally {
			tearDown(editor);
			EditorTestHelper.closeAllEditors();
		}
	}

	protected void setUp(IEditorPart editor) throws Exception {
	}

	protected void tearDown(IEditorPart editor) throws Exception {
	}
}

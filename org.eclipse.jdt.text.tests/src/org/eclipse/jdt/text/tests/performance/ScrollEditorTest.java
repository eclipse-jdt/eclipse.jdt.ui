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

import org.eclipse.core.resources.IFile;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.text.tests.performance.eval.Evaluator;
import org.eclipse.jdt.text.tests.performance.eval.IEvaluator;

public abstract class ScrollEditorTest extends TestCase {

	private static final int[] CTRL_HOME= new int[] { SWT.CTRL, SWT.HOME };
	private static final int[] CTRL_DOWN= new int[] { SWT.CTRL, SWT.ARROW_DOWN };
	private static final int[] PG_DOWN= new int[] { SWT.PAGE_DOWN };
	private static final int[] DOWN= new int[] { SWT.ARROW_DOWN };
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
	public static final ScrollingMode LINE_WISE= new ScrollingMode(CTRL_DOWN, PG_UP) {
		int computeOperations(int numberOfLines, int visibleLines) {
			return numberOfLines - visibleLines;
		}
	};
	public static final ScrollingMode LINE_WISE_MOVE_CARET= new ScrollingMode(DOWN, CTRL_HOME) {
		int computeOperations(int numberOfLines, int visibleLines) {
			return numberOfLines - 1;
		}
	};
	
	private PerformanceMeter fPerformanceMeter;
	protected IEditorPart fEditor;
	private ScrollingMode fMode;
	private boolean fPreloadEvents= false;
	private IEvaluator fEvaluator;
	
	protected ScrollEditorTest() {
		setScrollingMode(PAGE_WISE);
	}

	/**
	 * Sets the scrolling mode, defaults to PAGE_WISE.
	 * 
	 * @param mode the new scrolling mode
	 */
	protected void setScrollingMode(ScrollingMode mode) {
		fMode= mode;
	}

	protected void setPreloadEvents(boolean preloadEvents) {
		fPreloadEvents= preloadEvents;
	}

	protected void setUp() throws Exception {
		fPerformanceMeter= createPerformanceMeter();
		fEvaluator= createEvaluator();
		EditorTestHelper.bringToTop();
		fEditor= EditorTestHelper.openInEditor(getFile(), true);
		EditorTestHelper.calmDown(1000, 10000, 100);
	}

	protected IEvaluator createEvaluator() {
		return Evaluator.getDefaultEvaluator();
	}

	protected PerformanceMeter createPerformanceMeter() {
		return Performance.createPerformanceMeterFactory().createPerformanceMeter(this);
	}

	protected abstract IFile getFile();

	protected void tearDown() throws Exception {
		EditorTestHelper.closeAllEditors();
	}

	protected void measureScrolling(int nOfRuns) {
		Display display= SWTEventHelper.getActiveDisplay();
		
		StyledText text= (StyledText) fEditor.getAdapter(Control.class);
		int numberOfLines= text.getLineCount();
		int visibleLinesInViewport= text.getClientArea().height / text.getLineHeight();
		int operations= fMode.computeOperations(numberOfLines, visibleLinesInViewport);
		
		for (int i= 0; i < nOfRuns; i++) {
			if (fPreloadEvents) {
				for (int j= 0; j < operations; j++) {
					// avoid overhead: assertTrue(text.getTopIndex() + visibleLinesInViewport < numberOfLines - 1);
					SWTEventHelper.pressKeyCodeCombination(display, fMode.SCROLL_COMBO, false);
				}
				fPerformanceMeter.start();
				EditorTestHelper.runEventQueue(100);
				fPerformanceMeter.stop();
			} else {
				fPerformanceMeter.start();
				for (int j= 0; j < operations; j++) {
					// avoid overhead: assertTrue(text.getTopIndex() + visibleLinesInViewport < numberOfLines - 1);
					SWTEventHelper.pressKeyCodeCombination(display, fMode.SCROLL_COMBO);
				}
				fPerformanceMeter.stop();
				EditorTestHelper.runEventQueue(100);
			}
			assertTrue("TopIndex: "+text.getTopIndex() + " visibleLines: "+visibleLinesInViewport + " totalLines: " + numberOfLines + " operations: " + operations, text.getTopIndex() + visibleLinesInViewport >= numberOfLines - 1);
			SWTEventHelper.pressKeyCodeCombination(display, fMode.HOME_COMBO);
			EditorTestHelper.runEventQueue(100);
			assertEquals(0, text.getTopIndex());
		}
		fPerformanceMeter.commit();
		fEvaluator.evaluate(fPerformanceMeter.getSessionData());
	}
}

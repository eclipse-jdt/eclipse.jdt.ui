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
import org.eclipse.swt.widgets.Display;

import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.jface.action.IAction;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

public class JavaIndenterTest extends TestCase {
	
	private static final String FILE= "org.eclipse.swt/Eclipse SWT/win32/org/eclipse/swt/graphics/TextLayout.java";

	private static final int N_OF_RUNS= 2;

	private static final int[] CTRL_END= new int[] { SWT.CTRL, SWT.END };
	
	private PerformanceMeter fPerformanceMeter;

	private ITextEditor fEditor;

	protected void setUp() throws Exception {
		EditorTestHelper.runEventQueue();
		Performance performance= Performance.getDefault();
		fPerformanceMeter= performance.createPerformanceMeter(performance.getDefaultScenarioId(this));
		
		EditorTestHelper.bringToTop();
		fEditor= (ITextEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(FILE), true);
		runAction(fEditor.getAction(ITextEditorActionConstants.SELECT_ALL));
		runAction(fEditor.getAction("ToggleComment"));
		SWTEventHelper.pressKeyCodeCombination(EditorTestHelper.getActiveDisplay(), CTRL_END);
		EditorTestHelper.joinJobs(2000, 5000, 100);
	}

	protected void tearDown() throws Exception {
		EditorTestHelper.closeAllEditors();
		fPerformanceMeter.dispose();
	}
	
	public void testJavaIndenter1() {
		// cold run
		measureJavaIndenter();
	}

	public void testJavaIndenter2() {
		// warm run
		measureJavaIndenter();
	}

	private void measureJavaIndenter() {
		IDocument document= ((JavaEditor) fEditor).getViewer().getDocument();
		Display display= EditorTestHelper.getActiveDisplay();
		IAction undo= fEditor.getAction(ITextEditorActionConstants.UNDO);
		int originalNumberOfLines= document.getNumberOfLines();
		for (int i= 0; i < N_OF_RUNS; i++) {
			fPerformanceMeter.start();
			SWTEventHelper.pressKeyCode(display, SWT.CR);
			fPerformanceMeter.stop();
			assertEquals(originalNumberOfLines + 1, document.getNumberOfLines());
			runAction(undo);
			assertEquals(originalNumberOfLines, document.getNumberOfLines());
			sleep(2000); // NOTE: runnables posted from other threads, while the main thread waits here, are executed and measured only in the next iteration
		}
		fPerformanceMeter.commit();
		Performance.getDefault().assertPerformance(fPerformanceMeter);
	}

	private void runAction(IAction action) {
		action.run();
		EditorTestHelper.runEventQueue();
	}

	private synchronized void sleep(int time) {
		try {
			wait(time);
		} catch (InterruptedException e) {
		}
	}
}

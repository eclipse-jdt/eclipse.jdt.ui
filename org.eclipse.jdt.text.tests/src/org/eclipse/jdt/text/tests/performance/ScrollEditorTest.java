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

import org.eclipse.ui.PartInitException;

public class ScrollEditorTest extends TestCase {

	private static final int[] CTRL_HOME= new int[] { SWT.CTRL, SWT.HOME };
	
	private PerformanceMeter fPerformanceMeter;

	protected void setUp() throws Exception {
		fPerformanceMeter= Performance.createPerformanceMeterFactory().createPerformanceMeter(this);
		EditorTestHelper.runEventQueue();
	}

	protected void tearDown() throws Exception {
		EditorTestHelper.closeAllEditors();
	}

	protected void measureScrolling(IFile file, int nOfRuns) throws PartInitException {
		Display display= SWTEventHelper.getActiveDisplay();
		
		StyledText text= (StyledText) EditorTestHelper.openInEditor(file, true).getAdapter(Control.class);
		int numberOfLines= text.getLineCount();
		int visibleLinesInViewport= text.getClientArea().height / text.getLineHeight();
		int m= numberOfLines / visibleLinesInViewport;
		
		for (int i= 0; i < nOfRuns; i++) {
			fPerformanceMeter.start();
			for (int j= 0; j < m; j++)
				SWTEventHelper.pressKeyCode(display, SWT.PAGE_DOWN);
			fPerformanceMeter.stop();
			assertTrue(text.getTopIndex() + visibleLinesInViewport >= numberOfLines - 1);
			SWTEventHelper.pressKeyCodeCombination(display, CTRL_HOME);
			assertEquals(0, text.getTopIndex());
		}
		fPerformanceMeter.commit();
	}
}

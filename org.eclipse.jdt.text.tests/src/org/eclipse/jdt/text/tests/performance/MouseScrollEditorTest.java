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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;

public abstract class MouseScrollEditorTest extends TestCase {

	private static final int[] CTRL_HOME= new int[] { SWT.CTRL, SWT.HOME };

	private static final int[] CTRL_END= new int[] { SWT.CTRL, SWT.END };
	
	private PerformanceMeter fPerformanceMeter;

	private IEditorPart fEditor;

	private volatile boolean fDone;

	protected void setUp() throws Exception {
		fPerformanceMeter= Performance.createPerformanceMeterFactory().createPerformanceMeter(this);
		EditorTestHelper.bringToTop();
		fEditor= EditorTestHelper.openInEditor(getFile(), true);
		EditorTestHelper.calmDown(3000, 10000, 100);
	}

	protected void tearDown() throws Exception {
		EditorTestHelper.closeAllEditors();
	}

	protected abstract IFile getFile();

	protected void measureScrolling(int nOfRuns) throws PartInitException {
		final Display display= SWTEventHelper.getActiveDisplay();
		final StyledText text= (StyledText) fEditor.getAdapter(Control.class);
		
		int thumbX= text.getBounds().width - (text.getVerticalBar().getSize().x >> 1);
		int thumbY= text.computeTrim(0, 0, 0, 0).height + 2;
		final Point thumb= display.map(text, null, thumbX, thumbY);
		
		final int numberOfLines= text.getLineCount();
		final int visibleLinesInViewport= text.getClientArea().height / text.getLineHeight();
		
		Runnable runnable= new Runnable() {
			private volatile int fTopIndex;
			private volatile int fTopPixel;
			private Runnable fRunnable= new Runnable() {
				public void run() {
					fTopIndex= text.getTopIndex();
					fTopPixel= text.getTopPixel();
				}
			};
			public void run() {
				fTopIndex= 0;
				fTopPixel= 0;
				int i= thumb.y + 1;
				int posts= 0;
				SWTEventHelper.mouseMoveEvent(display, thumb.x, thumb.y, false);
				SWTEventHelper.mouseDownEvent(display, 1, false);
				while (fTopIndex + visibleLinesInViewport < numberOfLines - 1) {
					SWTEventHelper.mouseMoveEvent(display, thumb.x, i++, false);
					int oldTopPixel= fTopPixel;
					while (oldTopPixel == fTopPixel) {
						posts++;
						display.syncExec(fRunnable);
					}
				}
				SWTEventHelper.mouseUpEvent(display, 1, false);
				fDone= true;
				display.wake();
//				System.out.println("scrolls == " + (i - mappedGuessY - 1) + ", posts == " + posts);
			}
		};
		for (int i= 0; i < nOfRuns; i++) {
			fDone= false;
			new Thread(runnable).start();
			fPerformanceMeter.start();
			while (!fDone)
				if (!display.readAndDispatch())
					display.sleep();
			fPerformanceMeter.stop();
			assertTrue(text.getTopIndex() + visibleLinesInViewport >= numberOfLines - 1);
			SWTEventHelper.pressKeyCodeCombination(display, CTRL_END);
			SWTEventHelper.pressKeyCodeCombination(display, CTRL_HOME);
			assertEquals(0, text.getTopIndex());
		}
		fPerformanceMeter.commit();
	}
}

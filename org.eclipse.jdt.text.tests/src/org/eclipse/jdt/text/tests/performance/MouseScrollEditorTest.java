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
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFile;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;

public class MouseScrollEditorTest extends TestCase {

	private static final Class THIS= MouseScrollEditorTest.class;
	
	private static final int[] CTRL_HOME= new int[] { SWT.CTRL, SWT.HOME };
	
	private PerformanceMeter fPerformanceMeter;

	private IEditorPart fEditor;

	private volatile boolean fDone;

	public static Test allTests() {
		return new TestSuite(THIS);
	}

	public static Test suite() {
		return allTests();
	}
	
	protected void setUp() throws Exception {
		fPerformanceMeter= Performance.createPerformanceMeterFactory().createPerformanceMeter(this);
		EditorTestHelper.bringToTop();
		fEditor= EditorTestHelper.openInEditor(getFile(), true);
		EditorTestHelper.calmDown(1000, 10000, 100);
	}

	protected IFile getFile() {
		return ResourceTestHelper.findFile("org.eclipse.swt/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/StyledText.java");
	}
	public void testMouseScrollEditor() throws PartInitException {
		measureScrolling(1);
	}
	
	protected void tearDown() throws Exception {
		EditorTestHelper.closeAllEditors();
	}

	protected void measureScrolling(int nOfRuns) throws PartInitException {
		final Display display= SWTEventHelper.getActiveDisplay();
		
		final StyledText text= (StyledText) fEditor.getAdapter(Control.class);
		final Rectangle textBounds= text.getBounds();
		Point scrollBarSize= text.getVerticalBar().getSize();
		int guessX= textBounds.width - ((int) (0.5*scrollBarSize.x));
		int guessY= scrollBarSize.x + 5;
		Point point= display.map(text, null, guessX, guessY);
		final int mappedGuessX= point.x;
		final int mappedGuessY= point.y;
		point= display.map(text, null, guessX, textBounds.height);

		final int numberOfLines= text.getLineCount();
		final int visibleLinesInViewport= text.getClientArea().height / text.getLineHeight();
		
		for (int i= 0; i < nOfRuns; i++) {
			fPerformanceMeter.start();
			new Thread() { public void run() {
				SWTEventHelper.mouseMoveEvent(display, mappedGuessX, mappedGuessY, false);
				SWTEventHelper.mouseDownEvent(display, 1, false);
				final int[] result= new int[2];
				int j= mappedGuessY + 1;
				while (result[0] + visibleLinesInViewport < numberOfLines - 1) {
					SWTEventHelper.mouseMoveEvent(display, mappedGuessX, j++, false);
					int oldTopPixel= result[1];
//					int k= 0;
					while (oldTopPixel == result[1]) {
						display.syncExec(new Runnable() {
							public void run() {
								result[0]= text.getTopIndex();
								result[1]= text.getTopPixel();
							}
						});
//						k++;
					}
//					if (k > 1)
//						System.out.println("k = " + k);
				}
				SWTEventHelper.mouseUpEvent(display, 1, false);
				fDone= true;
			} }.start();
			while (!fDone)
				if (!display.readAndDispatch())
					display.sleep();
			fPerformanceMeter.stop();
			assertTrue(text.getTopIndex() + visibleLinesInViewport >= numberOfLines - 1);
			SWTEventHelper.pressKeyCodeCombination(display, CTRL_HOME);
			assertEquals(0, text.getTopIndex());
		}
		fPerformanceMeter.commit();
	}
}

/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.text.tests.performance;

import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import org.eclipse.core.resources.IFile;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;

public abstract class MouseScrollEditorTest extends TextPerformanceTestCase {

	public static abstract class Poster {

		public abstract void initializeFromForeground(StyledText text);

		public abstract void initializeFromBackground();

		public abstract void driveFromBackground();
	}

	public static class ThumbScrollPoster extends Poster {

		private Display fDisplay;

		private Point fThumb;

		public void initializeFromForeground(StyledText text) {
			fDisplay= text.getDisplay();
			Rectangle textBounds= fDisplay.map(text.getParent(), null, text.getBounds());
			int thumbX= textBounds.width - (text.getVerticalBar().getSize().x >> 1);
			int thumbY= text.computeTrim(0, 0, 0, 0).height + 2;
			fThumb= fDisplay.map(text, null, thumbX, thumbY);
		}

		public void initializeFromBackground() {
			SWTEventHelper.mouseMoveEvent(fDisplay, fThumb.x, fThumb.y++, false);
			SWTEventHelper.mouseDownEvent(fDisplay, 1, false);
		}

		public void driveFromBackground() {
			SWTEventHelper.mouseMoveEvent(fDisplay, fThumb.x, fThumb.y++, false);
		}
	}

	public static class AutoScrollPoster extends Poster {

		private Display fDisplay;

		private Rectangle fTextBounds;

		public void initializeFromForeground(StyledText text) {
			fDisplay= text.getDisplay();
			fTextBounds= fDisplay.map(text.getParent(), null, text.getBounds());
		}

		public void initializeFromBackground() {
			SWTEventHelper.mouseMoveEvent(fDisplay, fTextBounds.x + 1, fTextBounds.y + 1, false);
			SWTEventHelper.mouseDownEvent(fDisplay, 1, false);
			SWTEventHelper.mouseMoveEvent(fDisplay, fTextBounds.x + 1, fTextBounds.y + fTextBounds.height + 1, false);
			SWTEventHelper.mouseMoveEvent(fDisplay, fTextBounds.x + 2, fTextBounds.y + fTextBounds.height + 1, false); // needed for GTK
		}

		public void driveFromBackground() {
		}
	}

	private PerformanceMeter fPerformanceMeter;

	private volatile boolean fDone;

	private Display fDisplay;

	private StyledText fText;

	private int fMaxTopPixel;

	private Poster fPoster;

	private Error fBackgroundError;

	private Runnable fThreadRunnable= new Runnable() {
		private volatile int fTopPixel;
		private int fOldTopPixel;
		private Runnable fRunnable= new Runnable() {
			public void run() {
				fTopPixel= fText.getTopPixel();
			}
		};
		private Condition fCondition= new Condition() {
			public boolean isTrue() {
				fDisplay.syncExec(fRunnable);
				return fOldTopPixel != fTopPixel;
			}
		};
		public void run() {
			try {
				fTopPixel= 0;
				fPoster.initializeFromBackground();
				while (fTopPixel < fMaxTopPixel) {
					fPoster.driveFromBackground();
					fOldTopPixel= fTopPixel;
					assertTrue(fCondition.busyWaitFor(1000));
				}
			} catch (Error e) {
				fBackgroundError= e;
				throw e;
			} finally {
				SWTEventHelper.mouseUpEvent(fDisplay, 1, false);
				fDone= true;
				fDisplay.wake();
			}
		}
	};

	protected void setUp() throws Exception {
		super.setUp();
		Performance performance= Performance.getDefault();
		fPerformanceMeter= performance.createPerformanceMeter(performance.getDefaultScenarioId(this));
		EditorTestHelper.bringToTop();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		fPerformanceMeter.dispose();
	}

	protected void measureScrolling(Poster poster, IFile file) throws PartInitException {
		try {
			IEditorPart editor= EditorTestHelper.openInEditor(file, true);
			EditorTestHelper.joinJobs(5000, 10000, 100);

			fText= (StyledText) editor.getAdapter(Control.class);
			fDisplay= fText.getDisplay();

			fText.setTopPixel(Integer.MAX_VALUE);
			fMaxTopPixel= fText.getTopPixel();
			fText.setTopPixel(0);
			EditorTestHelper.joinJobs(100, 1000, 100);

			fPoster= poster;

			int warmUpRuns= getWarmUpRuns();
			int measuredRuns= getMeasuredRuns();
			for (int i= 0; i < warmUpRuns + measuredRuns; i++) {
				fPoster.initializeFromForeground(fText);

				fDone= false;
				new Thread(fThreadRunnable).start();
				if (i >= warmUpRuns)
					fPerformanceMeter.start();
				while (!fDone)
					if (!fDisplay.readAndDispatch())
						fDisplay.sleep();
				if (fBackgroundError != null)
					throw fBackgroundError;
				if (i >= warmUpRuns)
					fPerformanceMeter.stop();
				assertEquals(fMaxTopPixel, fText.getTopPixel());
				EditorTestHelper.joinJobs(100, 1000, 100);
				fText.setTopPixel(0);
				EditorTestHelper.joinJobs(100, 1000, 100);
			}
			fPerformanceMeter.commit();
			assertPerformance(fPerformanceMeter);
		} finally {
			EditorTestHelper.closeAllEditors();
		}
	}
}

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

import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

public class ScrollJavaEditorTest extends TestCase {
	
	private static final String FILE= "org.eclipse.swt/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/StyledText.java";

	private static final int N_OF_RUNS= 10;

	private static final int[] CTRL_HOME= new int[] { SWT.CTRL, SWT.HOME };

	private PerformanceMeterFactory fPerformanceMeterFactory= Performance.createPerformanceMeterFactory();

	protected void setUp() {
		EditorTestHelper.runEventQueue();
	}

	public void testScrollJavaEditor1() throws PartInitException {
		// cold run
		measureScrolling();
	}

	public void testScrollJavaEditor2() throws PartInitException {
		// warm run
		measureScrolling();
	}

	private void measureScrolling() throws PartInitException {
		Display display= SWTEventHelper.getActiveDisplay();
		
		PerformanceMeter performanceMeter= fPerformanceMeterFactory.createPerformanceMeter(this);
		try {
			ISourceViewer viewer= ((JavaEditor) EditorTestHelper.openInEditor(EditorTestHelper.findFile(FILE), true)).getViewer();
			int maxLine= viewer.getDocument().getNumberOfLines() - 1;
			
			for (int i= 0; i < N_OF_RUNS; i++) {
				performanceMeter.start();
				while (viewer.getBottomIndex() < maxLine)
					SWTEventHelper.pressKeyCode(display, SWT.PAGE_DOWN);
				performanceMeter.stop();
				
				SWTEventHelper.pressKeyCodeCombination(display, CTRL_HOME);
			}
		} finally {
			EditorTestHelper.closeAllEditors();
			performanceMeter.commit();
		}
	}
}

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

import org.eclipse.jface.action.IAction;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

public class ToggleCommentTest extends TestCase {
	
	private static final String FILE= "org.eclipse.swt/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/StyledText.java";

	private static final int N_OF_RUNS= 5;

	private PerformanceMeter fCommentMeter;

	private PerformanceMeter fUncommentMeter;

	private ITextEditor fEditor;

	protected void setUp() throws Exception {
		fCommentMeter= Performance.createPerformanceMeterFactory().createPerformanceMeter(this, "comment");
		fUncommentMeter= Performance.createPerformanceMeterFactory().createPerformanceMeter(this, "uncomment");
		fEditor= (ITextEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(FILE), true);
		runAction(fEditor.getAction(ITextEditorActionConstants.SELECT_ALL));
	}
	
	protected void tearDown() throws Exception {
		EditorTestHelper.closeAllEditors();
	}

	public void testToggleComment1() throws PartInitException {
		// cold run
		measureToggleComment();
	}

	public void testToggleComment2() throws PartInitException {
		// warm run
		measureToggleComment();
	}

	private void measureToggleComment() throws PartInitException {
		IAction toggleComment= fEditor.getAction("ToggleComment");
		for (int i= 0; i < N_OF_RUNS; i++) {
			fCommentMeter.start();
			runAction(toggleComment);
			fCommentMeter.stop();
			sleep(5000);
			fUncommentMeter.start();
			runAction(toggleComment);
			fUncommentMeter.stop();
			sleep(5000);
		}
		fCommentMeter.commit();
		fUncommentMeter.commit();
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

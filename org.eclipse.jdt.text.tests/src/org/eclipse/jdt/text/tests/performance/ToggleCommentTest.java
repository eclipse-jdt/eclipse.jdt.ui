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

	private static final int N_OF_RUNS= 10;

	private PerformanceMeterFactory fPerformanceMeterFactory= Performance.createPerformanceMeterFactory();

	protected void setUp() {
		EditorTestHelper.runEventQueue();
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
		PerformanceMeter performanceMeter= fPerformanceMeterFactory.createPerformanceMeter(this);
		try {
			ITextEditor editor= (ITextEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(FILE), true);
			
			editor.getAction(ITextEditorActionConstants.SELECT_ALL).run();
			EditorTestHelper.runEventQueue();
			
			IAction action= editor.getAction("ToggleComment");
			for (int i= 0; i < N_OF_RUNS; i++) {
				performanceMeter.start();
				action.run();
				EditorTestHelper.runEventQueue();
				performanceMeter.stop();
				sleep(5000); // NOTE: runnables posted from other threads, while the main thread waits here, are executed and measured only in the next iteration
			}
		} finally {
			EditorTestHelper.closeAllEditors();
			performanceMeter.commit();
		}
	}

	private synchronized void sleep(int time) {
		try {
			wait(time);
		} catch (InterruptedException e) {
		}
	}
}

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
import junit.framework.TestSuite;
import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.jface.action.IAction;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

public class ToggleCommentTest extends TextPerformanceTestCase {
	
	private static final Class THIS= ToggleCommentTest.class;
	
	private static final String FILE= "org.eclipse.swt/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/StyledText.java";

	private static final int WARM_UP_RUNS= 3;

	private static final int MEASURED_RUNS= 3;

	private PerformanceMeter fCommentMeter;

	private PerformanceMeter fUncommentMeter;

	private ITextEditor fEditor;
	
	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	protected void setUp() throws Exception {
		Performance performance= Performance.getDefault();
		fCommentMeter= performance.createPerformanceMeter(performance.getDefaultScenarioId(this, "comment"));
		fUncommentMeter= performance.createPerformanceMeter(performance.getDefaultScenarioId(this, "uncomment"));
		fEditor= (ITextEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(FILE), true);
		runAction(fEditor.getAction(ITextEditorActionConstants.SELECT_ALL));
		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
	}
	
	protected void tearDown() throws Exception {
		EditorTestHelper.closeAllEditors();
		fCommentMeter.dispose();
		fUncommentMeter.dispose();
	}

	public void testToggleComment2() throws PartInitException {
		// warm run
		measureToggleComment();
	}

	private void measureToggleComment() throws PartInitException {
		IAction toggleComment= fEditor.getAction("ToggleComment");
		int warmUpRuns= getWarmUpRuns();
		int measuredRuns= getMeasuredRuns();
		for (int i= 0; i < warmUpRuns + measuredRuns; i++) {
			if (i >= warmUpRuns)
				fCommentMeter.start();
			runAction(toggleComment);
			if (i >= warmUpRuns)
				fCommentMeter.stop();
			EditorTestHelper.runEventQueue(5000);
			if (i >= warmUpRuns)
				fUncommentMeter.start();
			runAction(toggleComment);
			if (i >= warmUpRuns)
				fUncommentMeter.stop();
			EditorTestHelper.runEventQueue(5000);
		}
		fCommentMeter.commit();
		fUncommentMeter.commit();
		Performance.getDefault().assertPerformance(fCommentMeter);
		Performance.getDefault().assertPerformance(fUncommentMeter);
	}

	private void runAction(IAction action) {
		action.run();
		EditorTestHelper.runEventQueue();
	}
}

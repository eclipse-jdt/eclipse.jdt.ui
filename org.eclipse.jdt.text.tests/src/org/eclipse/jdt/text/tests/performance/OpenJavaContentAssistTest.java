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
import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.jface.action.IAction;

import org.eclipse.jface.text.BadLocationException;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.AbstractTextEditor;


public class OpenJavaContentAssistTest extends TextPerformanceTestCase {
	
	private static final Class THIS= OpenJavaContentAssistTest.class;
	
	private static final String PATH= "/org.eclipse.swt/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/";
	
	private static final String ORIG_NAME= "StyledText";
	
	private static final String ORIG_FILE= PATH + ORIG_NAME + ".java";

	private static final int WARM_UP_RUNS= 10;

	private static final int MEASURED_RUNS= 10;

	private static final int LINE= 3897;

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
		ResourceTestHelper.replicate(ORIG_FILE, PATH + ORIG_NAME, ".java", getWarmUpRuns() + getMeasuredRuns(), ORIG_NAME, ORIG_NAME, ResourceTestHelper.OVERWRITE_IF_EXISTS);
		ResourceTestHelper.incrementalBuild();
		EditorTestHelper.bringToTop();
		EditorTestHelper.joinJobs(1000, 10000, 100);
	}
	
	protected void tearDown() throws Exception {
		super.tearDown();
		ResourceTestHelper.delete(PATH + ORIG_NAME, ".java", getWarmUpRuns() + getMeasuredRuns());
	}

	public void testOpenContentAssist() throws Exception {
		measureOpenContentAssist(getNullPerformanceMeter(), getNullPerformanceMeter(), 0, getWarmUpRuns());
		PerformanceMeter coldMeter= createPerformanceMeter(getDefaultScenarioId() + "-cold");
		PerformanceMeter warmMeter= createPerformanceMeter(getDefaultScenarioId() + "-warm");
		measureOpenContentAssist(coldMeter, warmMeter, getWarmUpRuns(), getMeasuredRuns());
		commitAllMeasurements();
		assertAllPerformance();
	}

	private void measureOpenContentAssist(PerformanceMeter coldMeter, PerformanceMeter warmMeter, int index, int runs) throws PartInitException, BadLocationException {
		for (int i= 0; i < runs; i++) {
			String name= ORIG_NAME + (index + i);
			String file= PATH + name + ".java";
			AbstractTextEditor editor= (AbstractTextEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(file), true);
			EditorTestHelper.joinReconciler(EditorTestHelper.getSourceViewer(editor), 100, 10000, 100);
			
			measureOpenContentAssist(editor, coldMeter);
			measureOpenContentAssist(editor, warmMeter);
			
			EditorTestHelper.closeAllEditors();
		}
	}

	private void measureOpenContentAssist(AbstractTextEditor editor, PerformanceMeter performanceMeter) throws BadLocationException {
		IAction showContentAssist= editor.getAction("ContentAssistProposal");
		editor.selectAndReveal(EditorTestHelper.getDocument(editor).getLineOffset(LINE), 0);
		EditorTestHelper.runEventQueue(10);
		performanceMeter.start();
		runAction(showContentAssist);
		EditorTestHelper.runEventQueue();
		performanceMeter.stop();
		EditorTestHelper.closeAllPopUps(EditorTestHelper.getSourceViewer(editor));
	}

	private void runAction(IAction action) {
		action.run();
		EditorTestHelper.runEventQueue();
	}
}

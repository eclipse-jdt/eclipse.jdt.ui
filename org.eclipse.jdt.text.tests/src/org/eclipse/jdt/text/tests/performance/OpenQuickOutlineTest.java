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

import org.eclipse.swt.widgets.Shell;

import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.jface.action.IAction;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;

public class OpenQuickOutlineTest extends TextPerformanceTestCase {
	
	private static final Class THIS= OpenQuickOutlineTest.class;
	
	private static final String SHORT_NAME_COLD= "Open Quick Outline (first time)";
	
	private static final String PATH= "/org.eclipse.swt/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/";
	
	private static final String ORIG_NAME= "StyledText";
	
	private static final String ORIG_FILE= PATH + ORIG_NAME + ".java";

	private static final int WARM_UP_RUNS= 10;

	private static final int MEASURED_RUNS= 10;

	private static final String OUTLINE_VIEW= "org.eclipse.ui.views.ContentOutline";

	private boolean fWasOutlineViewShown;
	
	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
		fWasOutlineViewShown= EditorTestHelper.hideView(OUTLINE_VIEW);
		ResourceTestHelper.replicate(ORIG_FILE, PATH + ORIG_NAME, ".java", getWarmUpRuns() + getMeasuredRuns(), ORIG_NAME, ORIG_NAME, ResourceTestHelper.OVERWRITE_IF_EXISTS);
		ResourceTestHelper.incrementalBuild();
		EditorTestHelper.bringToTop();
		EditorTestHelper.joinJobs(1000, 10000, 100);
	}
	
	protected void tearDown() throws Exception {
		super.tearDown();
		ResourceTestHelper.delete(PATH + ORIG_NAME, ".java", getWarmUpRuns() + getMeasuredRuns());
		if (fWasOutlineViewShown)
			EditorTestHelper.showView(OUTLINE_VIEW);
	}

	public void testOpenQuickOutline1() throws Exception {
		measureOpenQuickOutline(getNullPerformanceMeter(), getNullPerformanceMeter(), 0, getWarmUpRuns());
		PerformanceMeter coldMeter= createPerformanceMeterForSummary(getDefaultScenarioId() + "-cold", SHORT_NAME_COLD, Dimension.ELAPSED_PROCESS);
		PerformanceMeter warmMeter= createPerformanceMeter(getDefaultScenarioId() + "-warm");
		measureOpenQuickOutline(coldMeter, warmMeter, getWarmUpRuns(), getMeasuredRuns());
		commitAllMeasurements();
		assertAllPerformance();
	}

	private void measureOpenQuickOutline(PerformanceMeter coldMeter, PerformanceMeter warmMeter, int index, int runs) throws PartInitException {
		for (int i= 0; i < runs; i++) {
			String name= ORIG_NAME + (index + i);
			String file= PATH + name + ".java";
			AbstractTextEditor editor= (AbstractTextEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(file), true);
			EditorTestHelper.joinReconciler(EditorTestHelper.getSourceViewer(editor), 100, 10000, 100);
			
			measureOpenQuickOutline(editor, coldMeter);
			measureOpenQuickOutline(editor, warmMeter);
			
			EditorTestHelper.closeAllEditors();
		}
	}

	private void measureOpenQuickOutline(ITextEditor editor, PerformanceMeter performanceMeter) {
		IAction showOutline= editor.getAction(IJavaEditorActionDefinitionIds.SHOW_OUTLINE);
		EditorTestHelper.joinJobs(500, 1000, 100);
		performanceMeter.start();
		runAction(showOutline);
		performanceMeter.stop();
		Shell shell= EditorTestHelper.getActiveDisplay().getActiveShell();
		assertEquals("", shell.getText());
		shell.dispose();
		shell= EditorTestHelper.getActiveDisplay().getActiveShell();
		assertFalse("".equals(shell.getText()));
	}

	private void runAction(IAction action) {
		action.run();
		EditorTestHelper.runEventQueue();
	}
}

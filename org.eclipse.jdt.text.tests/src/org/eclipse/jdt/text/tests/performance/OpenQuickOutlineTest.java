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

import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.jface.action.IAction;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;

public class OpenQuickOutlineTest extends TextPerformanceTestCase {
	
	private static final Class THIS= OpenQuickOutlineTest.class;
	
	private static final String PATH= "/org.eclipse.swt/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/";
	
	private static final String ORIG_NAME= "StyledText";
	
	private static final String ORIG_FILE= PATH + ORIG_NAME + ".java";

	private static final int WARM_UP_RUNS= 10;

	private static final int MEASURED_RUNS= 10;

	private PerformanceMeter fFirstMeter;

	private PerformanceMeter fSecondMeter;

	private static final String OUTLINE_VIEW= "org.eclipse.ui.views.ContentOutline";

	private boolean fWasOutlineViewShown;
	
	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
		Performance performance= Performance.getDefault();
		fFirstMeter= performance.createPerformanceMeter(performance.getDefaultScenarioId(this, "cold"));
		fSecondMeter= performance.createPerformanceMeter(performance.getDefaultScenarioId(this, "warm"));
		fWasOutlineViewShown= EditorTestHelper.hideView(OUTLINE_VIEW); // TODO: find solution to hide view in other perspectives too
		ResourceTestHelper.replicate(ORIG_FILE, PATH + ORIG_NAME, ".java", getWarmUpRuns() + getMeasuredRuns(), ORIG_NAME, ORIG_NAME, ResourceTestHelper.FAIL_IF_EXISTS);
		ResourceTestHelper.incrementalBuild();
		EditorTestHelper.bringToTop();
		EditorTestHelper.joinJobs(1000, 10000, 100);
	}
	
	protected void tearDown() throws Exception {
		super.tearDown();
		for (int i= 0, n= getWarmUpRuns() + getMeasuredRuns(); i < n; i++)
			ResourceTestHelper.delete(PATH + ORIG_NAME + i + ".java");
		if (fWasOutlineViewShown)
			EditorTestHelper.showView(OUTLINE_VIEW);
		fFirstMeter.dispose();
		fSecondMeter.dispose();
	}

	public void testOpenQuickOutline1() throws IOException, CoreException {
		int warmUpRuns= getWarmUpRuns();
		int measuredRuns= getMeasuredRuns();
		for (int i= 0; i < warmUpRuns + measuredRuns; i++) {
			String name= ORIG_NAME + i;
			String file= PATH + name + ".java";
			ITextEditor editor= (ITextEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(file), true);
			EditorTestHelper.joinJobs(5000, 10000, 100);
			
			measureOpenQuickOutline(editor, i < warmUpRuns ? null : fFirstMeter);
			measureOpenQuickOutline(editor, i < warmUpRuns ? null : fSecondMeter);
			
			EditorTestHelper.closeAllEditors();
		}
		fFirstMeter.commit();
		fSecondMeter.commit();
		Performance.getDefault().assertPerformance(fFirstMeter);
		Performance.getDefault().assertPerformance(fSecondMeter);
	}

	private void measureOpenQuickOutline(ITextEditor editor, PerformanceMeter performanceMeter) {
		IAction showOutline= editor.getAction(IJavaEditorActionDefinitionIds.SHOW_OUTLINE);
		EditorTestHelper.joinJobs(500, 1000, 100);
		if (performanceMeter != null)
			performanceMeter.start();
		runAction(showOutline);
		if (performanceMeter != null)
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

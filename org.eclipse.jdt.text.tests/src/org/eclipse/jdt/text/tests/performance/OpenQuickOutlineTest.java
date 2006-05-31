/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.text.tests.performance;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;

import org.eclipse.jface.action.IAction;
import org.eclipse.test.performance.PerformanceMeter;
import org.eclipse.ui.texteditor.AbstractTextEditor;

public class OpenQuickOutlineTest extends OpenQuickControlTest {
	
	private static final Class THIS= OpenQuickOutlineTest.class;
	
	private boolean fWasOutlineViewShown;
	
	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		fWasOutlineViewShown= EditorTestHelper.showView(EditorTestHelper.OUTLINE_VIEW_ID, false);
	}
	
	protected void tearDown() throws Exception {
		super.tearDown();
		if (fWasOutlineViewShown)
			EditorTestHelper.showView(EditorTestHelper.OUTLINE_VIEW_ID, true);
	}

	protected IAction setUpMeasurement(AbstractTextEditor editor) throws Exception {
		EditorTestHelper.runEventQueue(100);
		return editor.getAction(IJavaEditorActionDefinitionIds.SHOW_OUTLINE);
	}

	protected void tearDownMeasurement(AbstractTextEditor editor) throws Exception {
		Shell shell= EditorTestHelper.getActiveDisplay().getActiveShell();
		assertEquals("", shell.getText());
		shell.dispose();
		shell= EditorTestHelper.getActiveDisplay().getActiveShell();
		assertFalse("".equals(shell.getText()));
	}

	public void testOpenQuickOutline1() throws Exception {
		PerformanceMeter coldMeter= createPerformanceMeter("-cold");
		PerformanceMeter warmMeter= createPerformanceMeter("-warm");
		measureOpenQuickControl(coldMeter, warmMeter);
	}
}

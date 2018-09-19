/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.text.tests.performance;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;

import org.eclipse.ui.texteditor.AbstractTextEditor;

import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;

public class OpenQuickOutlineTest extends OpenQuickControlTest {

	private static final Class<OpenQuickOutlineTest> THIS= OpenQuickOutlineTest.class;

	private boolean fWasOutlineViewShown;

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		fWasOutlineViewShown= EditorTestHelper.showView(EditorTestHelper.OUTLINE_VIEW_ID, false);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		if (fWasOutlineViewShown)
			EditorTestHelper.showView(EditorTestHelper.OUTLINE_VIEW_ID, true);
	}

	@Override
	protected IAction setUpMeasurement(AbstractTextEditor editor) throws Exception {
		EditorTestHelper.runEventQueue(100);
		return editor.getAction(IJavaEditorActionDefinitionIds.SHOW_OUTLINE);
	}

	@Override
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

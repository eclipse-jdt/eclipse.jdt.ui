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

import org.eclipse.jface.action.IAction;

import org.eclipse.ui.texteditor.AbstractTextEditor;


public class OpenJavaContentAssistTest extends OpenQuickControlTest {

	private static final Class<OpenJavaContentAssistTest> THIS= OpenJavaContentAssistTest.class;

	private static final int LINE= 3897;

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	@Override
	protected IAction setUpMeasurement(AbstractTextEditor editor) throws Exception {
		editor.selectAndReveal(EditorTestHelper.getDocument(editor).getLineOffset(LINE), 0);
		EditorTestHelper.runEventQueue(100);
		return editor.getAction("ContentAssistProposal");
	}

	@Override
	protected void tearDownMeasurement(AbstractTextEditor editor) throws Exception {
		EditorTestHelper.closeAllPopUps(EditorTestHelper.getSourceViewer(editor));
	}

	public void test1() throws Exception {
		PerformanceMeter coldMeter= createPerformanceMeter("-cold");
		PerformanceMeter warmMeter= createPerformanceMeter("-warm");
		measureOpenQuickControl(coldMeter, warmMeter);
	}
}

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

import org.eclipse.swt.widgets.Shell;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.jface.action.IAction;

import org.eclipse.ui.texteditor.AbstractTextEditor;


public class OpenJavaContentAssistTest extends OpenQuickControlTest {
	
	private static final Class THIS= OpenJavaContentAssistTest.class;
	
	private static final int LINE= 3897;
	
	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	protected IAction setUpMeasurement(AbstractTextEditor editor) throws Exception {
		editor.selectAndReveal(EditorTestHelper.getDocument(editor).getLineOffset(LINE), 0);
		EditorTestHelper.runEventQueue(100);
		return editor.getAction("ContentAssistProposal");
	}

	protected void tearDownMeasurement(AbstractTextEditor editor) throws Exception {
		Shell[] shells= EditorTestHelper.getActiveDisplay().getShells();
		for (int i= 0; i < shells.length; i++) {
			Shell shell= shells[i];
			if ("".equals(shell.getText()))
				shell.dispose();
		}
	}
	
	public void test1() throws Exception {
		PerformanceMeter coldMeter= createPerformanceMeter("-cold");
		PerformanceMeter warmMeter= createPerformanceMeter("-warm");
		measureOpenQuickControl(coldMeter, warmMeter);
	}
}

/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.text.tests.performance;

import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.ui.texteditor.AbstractTextEditor;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @since 3.1
 */
public class ActivateJavaEditorTest extends ActivateEditorTest {

	private static final Class THIS= ActivateJavaEditorTest.class;
	
	private static final String SHORT_NAME= "Activate " + ActivateEditorTest.getNumberOfEditors() + " Java editors";
	
	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	protected String getEditorId() {
		return EditorTestHelper.COMPILATION_UNIT_EDITOR_ID;
	}
	
	public void testActivateEditor() {
		setShortName(SHORT_NAME);
		super.testActivateEditor();
	}
	
	/*
	 * @see org.eclipse.jdt.text.tests.performance.ActivateEditorTest#measureActivateEditor(org.eclipse.ui.texteditor.AbstractTextEditor[], int, org.eclipse.test.performance.PerformanceMeter)
	 * @since 3.2
	 */
	protected void measureActivateEditor(AbstractTextEditor[] editors, int runs, PerformanceMeter performanceMeter) {
		explainDegradation("Java editor activation was not working correctly in 3.1 (see bug 119326 and bug 120572). The fixes result this expected performance degradation.", performanceMeter);
		super.measureActivateEditor(editors, runs, performanceMeter);
	}
}

/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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

/**
 * @since 3.1
 */
public class ActivateTextEditorTest extends ActivateEditorTest {

	private static final Class THIS= ActivateTextEditorTest.class;
	
	private static final String SHORT_NAME= "Activate " + ActivateEditorTest.getNumberOfEditors() + " text editors";
	
	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	protected String getEditorId() {
		return EditorTestHelper.TEXT_EDITOR_ID;
	}
	
	public void testActivateEditor() {
		setShortName(SHORT_NAME);
		super.testActivateEditor();
	}
	
	/*
	 * @see org.eclipse.jdt.text.tests.performance.ActivateEditorTest#getDegradationComment()
	 * @since 3.4
	 */
	protected String getDegradationComment() {
		return "This test is slower than in 3.3 due the changes in Platform UI, see bugs 232489, 232499 and 232513.";
	}
}

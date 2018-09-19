/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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


/**
 * @since 3.1
 */
public class ActivateTextEditorTest extends ActivateEditorTest {

	private static final Class<ActivateTextEditorTest> THIS= ActivateTextEditorTest.class;

	private static final String SHORT_NAME= "Activate " + ActivateEditorTest.getNumberOfEditors() + " text editors";

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	@Override
	protected String getEditorId() {
		return EditorTestHelper.TEXT_EDITOR_ID;
	}

	@Override
	public void testActivateEditor() {
		setShortName(SHORT_NAME);
		super.testActivateEditor();
	}

}

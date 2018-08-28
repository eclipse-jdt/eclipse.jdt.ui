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

import org.eclipse.jface.text.BadLocationException;

import org.eclipse.ui.PartInitException;


/**
 * @since 3.1
 */
public class RevertTextEditorTest extends RevertEditorTest {

	private static final Class<RevertTextEditorTest> THIS= RevertTextEditorTest.class;

	private static final String PREFIX= "/" + PerformanceTestSetup.PROJECT + "/Eclipse SWT/win32/org/eclipse/swt/graphics/TextLayout";

	private static final String ORIG_FILE= PREFIX + ".java";

	private static final String FILE= PREFIX + ".txt";

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	@Override
	protected void setUp() throws Exception {
		ResourceTestHelper.copy(ORIG_FILE, FILE);
		super.setUp();
		EditorTestHelper.runEventQueue();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		ResourceTestHelper.delete(FILE);
	}

	public void testRevertTextEditor() throws PartInitException, BadLocationException {
		measureRevert(ResourceTestHelper.findFile(FILE));
	}
}

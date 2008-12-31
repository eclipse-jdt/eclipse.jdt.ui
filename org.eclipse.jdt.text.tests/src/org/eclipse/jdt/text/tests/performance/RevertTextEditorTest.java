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

import org.eclipse.jface.text.BadLocationException;

import org.eclipse.ui.PartInitException;


/**
 * @since 3.1
 */
public class RevertTextEditorTest extends RevertEditorTest {

	private static final Class THIS= RevertTextEditorTest.class;

	private static final String PREFIX= "/" + PerformanceTestSetup.PROJECT + "/Eclipse SWT/win32/org/eclipse/swt/graphics/TextLayout";

	private static final String ORIG_FILE= PREFIX + ".java";

	private static final String FILE= PREFIX + ".txt";

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	protected void setUp() throws Exception {
		ResourceTestHelper.copy(ORIG_FILE, FILE);
		super.setUp();
		EditorTestHelper.runEventQueue();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		ResourceTestHelper.delete(FILE);
	}

	public void testRevertTextEditor() throws PartInitException, BadLocationException {
		measureRevert(ResourceTestHelper.findFile(FILE));
	}
}

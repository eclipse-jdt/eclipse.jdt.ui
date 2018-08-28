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


/**
 * @since 3.1
 */
public class RevertJavaEditorTest extends RevertEditorTest {

	private static final Class<RevertJavaEditorTest> THIS= RevertJavaEditorTest.class;

	private static final String FILE= PerformanceTestSetup.PROJECT + "/Eclipse SWT/win32/org/eclipse/swt/graphics/TextLayout.java";

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		EditorTestHelper.runEventQueue();
	}

	public void testRevertJavaEditor() throws Exception {
		try {
			EditorTestHelper.enableFolding(true);
			measureRevert(ResourceTestHelper.findFile(FILE));
		} finally {
			EditorTestHelper.resetFolding();
		}
	}

	public void test2() throws Exception {
		try {
			EditorTestHelper.enableFolding(false);
			measureRevert(ResourceTestHelper.findFile(FILE));
		} finally {
			EditorTestHelper.resetFolding();
		}
	}
}

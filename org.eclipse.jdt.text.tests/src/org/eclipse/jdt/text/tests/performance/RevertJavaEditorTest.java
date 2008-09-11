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


/**
 * @since 3.1
 */
public class RevertJavaEditorTest extends RevertEditorTest {

	private static final Class THIS= RevertJavaEditorTest.class;

	private static final String FILE= PerformanceTestSetup.PROJECT + "/Eclipse SWT/win32/org/eclipse/swt/graphics/TextLayout.java";

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

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

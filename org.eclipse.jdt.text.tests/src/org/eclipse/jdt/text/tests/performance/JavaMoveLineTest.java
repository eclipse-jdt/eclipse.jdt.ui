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
 * Measures the time to move a line within a large file in the Java editor.
 *
 * @since 3.1
 */
public class JavaMoveLineTest extends MoveLineTest {

	private static final Class<JavaMoveLineTest> THIS= JavaMoveLineTest.class;

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	@Override
	protected String getEditorId() {
		return EditorTestHelper.COMPILATION_UNIT_EDITOR_ID;
	}
}

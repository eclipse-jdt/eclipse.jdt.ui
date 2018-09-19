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
 * Measures the time to type in one single method into a large text file
 * @since 3.1
 */
public class TextNonInitialTypingTest extends NonInitialTypingTest {

	private static final String SHORT_NAME_TYPING= "Text editor typing speed";

	private static final Class<TextNonInitialTypingTest> THIS= TextNonInitialTypingTest.class;

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	@Override
	protected String getSummaryName() {
		if ("testTypeAMethod".equals(getName()))
			return SHORT_NAME_TYPING;
		return null;
	}

	@Override
	protected String getEditorId() {
		return "org.eclipse.ui.DefaultTextEditor";
	}
}

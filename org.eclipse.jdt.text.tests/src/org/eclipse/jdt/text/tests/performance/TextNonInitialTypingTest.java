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
 * Measures the time to type in one single method into a large text file
 * @since 3.1
 */
public class TextNonInitialTypingTest extends NonInitialTypingTest {

	private static final String SHORT_NAME_TYPING= "Text editor typing speed";

	private static final Class THIS= TextNonInitialTypingTest.class;

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	protected String getSummaryName() {
		if ("testTypeAMethod".equals(getName()))
			return SHORT_NAME_TYPING;
		return null;
	}

	protected String getEditorId() {
		return "org.eclipse.ui.DefaultTextEditor";
	}
}

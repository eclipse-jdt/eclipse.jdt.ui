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
 * Measures the time to type in one single method into a large Java class
 * @since 3.1
 */
public class JavaNonInitialTypingTest extends NonInitialTypingTest {

	private static final String SHORT_NAME_TYPING= "Java editor typing speed";

	private static final Class THIS= JavaNonInitialTypingTest.class;

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	protected String getScenarioId() {
		if ("testTypeAMethod".equals(getName()))
			return "org.eclipse.jdt.text.tests.performance.NonInitialTypingTest#testTypeAMethod()";
		return super.getScenarioId();
	}

	protected String getSummaryName() {
		if ("testTypeAMethod".equals(getName()))
			return SHORT_NAME_TYPING;
		return null;
	}

	protected String getEditorId() {
		return "org.eclipse.jdt.ui.CompilationUnitEditor";
	}
}

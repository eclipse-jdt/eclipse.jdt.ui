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
 * @deprecated since INVOCATION_COUNT dimension is no longer supported.
 */
@Deprecated
public class DebuggingPerformanceTestSuite extends TestSuite {

	public static Test suite() {
		return new PerformanceTestSetup(new DebuggingPerformanceTestSuite());
	}

	public DebuggingPerformanceTestSuite() {
		addTest(TextTypingInvocationCountTest.suite());
		addTest(JavaTypingInvocationCountTest.suite());
		addTest(OpenJavaEditorInvocationCountTest.suite());
		addTest(ScrollAnnotatedJavaEditorInvocationCountTest.suite());
		addTest(QuickDiffInvocationCountTest.suite());
	}
}

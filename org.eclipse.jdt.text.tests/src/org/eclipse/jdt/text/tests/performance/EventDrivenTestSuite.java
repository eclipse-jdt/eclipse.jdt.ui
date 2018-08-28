/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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

import org.eclipse.jdt.text.tests.performance.TextPerformanceTestCase.DebugSetup;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * Depends on {@link PerformanceTestSetup}, which is currently run by
 * {@link PerformanceTestSuite}. {@link PerformanceTestSuite} precedes
 * this suite in the test.xml.
 *
 * @since 3.1
 */
public class EventDrivenTestSuite extends TestSuite {

	public static Test suite() {
		return new PerformanceTestSetup(new EventDrivenTestSuite());
	}

	public EventDrivenTestSuite() {
		addTest(new DebugSetup(JavaNonInitialTypingTest.suite()));
		addTest(new DebugSetup(TextNonInitialTypingTest.suite()));
		addTest(new DebugSetup(ScrollJavaEditorTest.suite()));
		addTest(new DebugSetup(ScrollTextEditorTest.suite()));
		addTest(new DebugSetup(WhitespaceCharacterPainterTest.suite()));
		addTest(JavaIndenterTest.suite());
		addTest(OpenPreferencePageTest.suite());
		addTest(ScrollAnnotatedJavaEditorTest.suite());
		addTest(ScrollVerticalRulerTest.suite());

		//FIXME: The test is currently excluded because it only takes 0..2ms.
//		addTest(JavaDocIndentStrategyTest.suite());

		addTest(TextMoveLineTest.suite());
		addTest(JavaMoveLineTest.suite());
		addTest(JavaReplaceAllTest.suite());
		addTest(JavaReplaceAllWithQuickDiffTest.suite());

		// TODO: enable when Bug 72244: "Posting mouse events locks up event handling" is fixed
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=72244
//		addTest(MouseScrollJavaEditorTest.suite());
//		addTest(MouseScrollTextEditorTest.suite());
	}
}

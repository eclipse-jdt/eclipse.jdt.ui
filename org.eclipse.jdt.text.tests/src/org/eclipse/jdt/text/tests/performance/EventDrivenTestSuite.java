/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

import org.eclipse.jdt.text.tests.performance.TextPerformanceTestCase.DebugSetup;


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
		addTest(new DebugSetup(ScrollJavaEditorTest.suite()));
		addTest(new DebugSetup(ScrollTextEditorTest.suite()));
		addTest(new DebugSetup(WhitespaceCharacterPainterTest.suite()));
		addTest(JavaIndenterTest.suite());
		addTest(JavaNonInitialTypingTest.suite());
		addTest(TextNonInitialTypingTest.suite());
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

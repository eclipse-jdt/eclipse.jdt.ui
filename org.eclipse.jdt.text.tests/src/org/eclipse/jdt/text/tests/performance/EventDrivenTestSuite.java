/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.performance;

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
		return new CloseWorkbenchDecorator(new PerformanceTestSetup(new EventDrivenTestSuite()));
	}
	
	public EventDrivenTestSuite() {
		addTestSuite(ScrollJavaEditorTest.class);
		addTestSuite(ScrollTextEditorTest.class);
		addTest(JavaIndenterTest.suite());
		addTest(JavaNonInitialTypingTest.suite());
		addTest(TextNonInitialTypingTest.suite());
		addTestSuite(OpenPreferencePageTest.class);
		addTestSuite(ScrollAnnotatedJavaEditorTest.class);
		// TODO: enable when Bug 72244: "Posting mouse events locks up event handling" is fixed
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=72244
//		addTestSuite(MouseScrollJavaEditorTest.class);
//		addTestSuite(MouseScrollTextEditorTest.class);
	}
}

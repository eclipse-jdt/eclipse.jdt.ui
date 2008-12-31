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
 * Measure the time spent while scrolling with error annotations in the Java editor.
 *
 * @since 3.1
 */
public class ScrollAnnotatedJavaEditorTest extends AbstractScrollAnnotatedJavaEditorTest {

	private static final Class THIS= ScrollAnnotatedJavaEditorTest.class;

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	public static Test setUpTest(Test someTest) {
		return new PerformanceTestSetup(someTest);
	}

	/**
	 * Measure the time spent while scrolling page wise with error
	 * annotations in the Java editor.
	 *
	 * @throws Exception
	 */
	public void testScrollJavaEditorPageWise() throws Exception {
		measure(PAGE_WISE);
	}

	/**
	 * Measure the time spent while scrolling line wise with error
	 * annotations in the Java editor.
	 *
	 * @throws Exception
	 */
	public void testScrollJavaEditorLineWiseMoveCaret1() throws Exception {
		measure(LINE_WISE);
	}

	/**
	 * Measure the time spent while scrolling and selecting line wise with
	 * error annotations in the Java editor.
	 *
	 * @throws Exception
	 */
	public void testScrollJavaEditorLineWiseSelect1() throws Exception {
		measure(LINE_WISE_SELECT);
	}

	/**
	 * Measure the time spent while scrolling line wise without moving the
	 * caret with error annotations in the Java editor.
	 *
	 * @throws Exception
	 */
	public void testScrollJavaEditorLineWise1() throws Exception {
		measure(LINE_WISE_NO_CARET_MOVE);
	}
}

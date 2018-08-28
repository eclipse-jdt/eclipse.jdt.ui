/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
 * @since 3.4
 */
public class ScrollVerticalRuler1000Test extends ScrollVerticalRulerTest {

	private static final Class<ScrollVerticalRuler1000Test> THIS= ScrollVerticalRuler1000Test.class;

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	@Override
	protected int getNumberOfAnnotations() {
		return 1000;
	}

	/**
	 * Measure the time spent while scrolling page wise in the text editor.
	 *
	 * @throws Exception
	 */
	public void testScrollTextEditorPageWise() throws Exception {
		measure(PAGE_WISE);
	}

	/**
	 * Measure the time spent while scrolling line wise in the text editor.
	 *
	 * @throws Exception
	 */
	public void testScrollTextEditorLineWiseMoveCaret2() throws Exception {
		measure(LINE_WISE);
	}

}

/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

import org.eclipse.core.runtime.CoreException;

public class MouseScrollTextEditorTest extends MouseScrollEditorTest {

	private static final Class THIS= MouseScrollTextEditorTest.class;

	private static final String THUMB_SCROLLING_FILE_PREFIX= "/org.eclipse.swt/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/StyledText";

	private static final String THUMB_SCROLLING_ORIG_FILE= THUMB_SCROLLING_FILE_PREFIX + ".java";

	private static final String THUMB_SCROLLING_FILE= THUMB_SCROLLING_FILE_PREFIX + ".txt";

	private static final String AUTO_SCROLLING_FILE_PREFIX= "/org.eclipse.swt/Eclipse SWT/win32/org/eclipse/swt/graphics/TextLayout";

	private static final String AUTO_SCROLLING_ORIG_FILE= AUTO_SCROLLING_FILE_PREFIX + ".java";

	private static final String AUTO_SCROLLING_FILE= AUTO_SCROLLING_FILE_PREFIX + ".txt";

	private static final int WARM_UP_RUNS= 3;

	private static final int MEASURED_RUNS= 3;

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	protected void setUp() throws Exception {
		super.setUp();
		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
	}

	public void testThumbScrollTextEditor1() throws CoreException {
		try {
			ResourceTestHelper.copy(THUMB_SCROLLING_ORIG_FILE, THUMB_SCROLLING_FILE);
			measureScrolling(new ThumbScrollPoster(), ResourceTestHelper.findFile(THUMB_SCROLLING_FILE));
		} finally {
			ResourceTestHelper.delete(THUMB_SCROLLING_FILE);
		}
	}

	public void testAutoScrollTextEditor1() throws CoreException {
		try {
			ResourceTestHelper.copy(AUTO_SCROLLING_ORIG_FILE, AUTO_SCROLLING_FILE);
			measureScrolling(new AutoScrollPoster(), ResourceTestHelper.findFile(AUTO_SCROLLING_FILE));
		} finally {
			ResourceTestHelper.delete(AUTO_SCROLLING_FILE);
		}
	}
}

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

import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.ui.PartInitException;

public class UndoJavaEditorTest extends UndoEditorTest {

	private static final Class<UndoJavaEditorTest> THIS= UndoJavaEditorTest.class;

	private static final String FILE= "org.eclipse.swt/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/StyledText.java";

	private static final int WARM_UP_RUNS= 2;

	private static final int MEASURED_RUNS= 2;

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
	}

	public void testUndoJavaEditor2() throws PartInitException {
		measureUndo(ResourceTestHelper.findFile(FILE));
	}

	/*
	 * @see org.eclipse.jdt.text.tests.performance.UndoEditorTest#createMeter()
	 * @since 3.3
	 */
	@Override
	protected PerformanceMeter createMeter() {
		Performance performance= Performance.getDefault();
		PerformanceMeter meter= performance.createPerformanceMeter(performance.getDefaultScenarioId(this));
		performance.tagAsSummary(meter, "Undo in Java editor", Dimension.ELAPSED_PROCESS);
		return meter;
	}
}

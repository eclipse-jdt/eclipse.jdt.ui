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

public class UndoTextEditorTest extends UndoEditorTest {

	private static final Class<UndoTextEditorTest> THIS= UndoTextEditorTest.class;

	private static final String FILE_PREFIX= "/org.eclipse.swt/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/StyledText";

	private static final String ORIG_FILE= FILE_PREFIX + ".java";

	private static final String FILE= FILE_PREFIX + ".txt";

	private static final int WARM_UP_RUNS= 2;

	private static final int MEASURED_RUNS= 2;

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	@Override
	protected void setUp() throws Exception {
		ResourceTestHelper.copy(ORIG_FILE, FILE);
		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		ResourceTestHelper.delete(FILE);
	}

	public void testUndoTextEditor2() throws PartInitException {
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
		performance.tagAsSummary(meter, "Undo in text editor", Dimension.ELAPSED_PROCESS);
		return meter;
	}
}

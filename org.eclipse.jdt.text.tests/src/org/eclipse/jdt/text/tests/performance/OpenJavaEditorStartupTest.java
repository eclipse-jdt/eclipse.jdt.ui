/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.PerformanceMeter;


/**
 * Startup performance with an open Java editor.
 *
 * @since 3.1
 */
public class OpenJavaEditorStartupTest extends StartupPerformanceTestCase {

	public static class Setup extends TestSetup {

		private boolean fTearDown;

		private boolean fSetUp;

		public Setup(Test test) {
			this(test, true, true);
		}

		public Setup(Test test, boolean setUp, boolean tearDown) {
			super(test);
			fSetUp= setUp;
			fTearDown= tearDown;
		}

		@Override
		protected void setUp() throws Exception {
			if (fSetUp)
				EditorTestHelper.openInEditor(ResourceTestHelper.findFile(FILE), true);
		}

		@Override
		protected void tearDown() throws Exception {
			if (fTearDown)
				EditorTestHelper.closeAllEditors();
		}
	}

	private static final Class<OpenJavaEditorStartupTest> THIS= OpenJavaEditorStartupTest.class;

	private static final String SHORT_NAME= "Eclipse SDK startup with Java editor open in Java perspective";

	private static final String FILE= "/" + PerformanceTestSetup.PROJECT + "/Eclipse SWT/win32/org/eclipse/swt/graphics/TextLayout.java";

	public static Test suite() {
		return new PerformanceTestSetup(new Setup(new TestSuite(THIS)));
	}

	public static Test suiteForMeasurement() {
		return new Setup(new TestSuite(THIS), false, true);
	}

	public void testJavaEditorStartup() {
		PerformanceMeter perfMeter= createPerformanceMeterForSummary(SHORT_NAME, Dimension.ELAPSED_PROCESS);
		measureStartup(perfMeter);
	}
}

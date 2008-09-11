/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 * Startup performance with an open text editor.
 *
 * @since 3.1
 */
public class OpenTextEditorStartupTest extends StartupPerformanceTestCase {

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

		protected void setUp() throws Exception {
			if (fSetUp) {
				ResourceTestHelper.copy(ORIG_FILE, FILE);
				EditorTestHelper.showPerspective(EditorTestHelper.RESOURCE_PERSPECTIVE_ID);
				EditorTestHelper.openInEditor(ResourceTestHelper.findFile(FILE), true);
			}
		}

		protected void tearDown() throws Exception {
			if (fTearDown) {
				EditorTestHelper.closeAllEditors();
				EditorTestHelper.showPerspective(EditorTestHelper.JAVA_PERSPECTIVE_ID);
				ResourceTestHelper.delete(FILE);
			}
		}
	}

	private static final Class THIS= OpenTextEditorStartupTest.class;

	private static final String SHORT_NAME= "Eclipse SDK startup with text editor open in Java perspective";

	private static final String PREFIX= "/" + PerformanceTestSetup.PROJECT + "/Eclipse SWT/win32/org/eclipse/swt/graphics/TextLayout";

	private static final String ORIG_SUFFIX= ".java";

	private static final String SUFFIX= ".txt";

	private static final String ORIG_FILE= PREFIX + ORIG_SUFFIX;

	private static final String FILE= PREFIX + SUFFIX;

	public static Test suite() {
		return new PerformanceTestSetup(new Setup(new TestSuite(THIS)));
	}

	public static Test suiteForMeasurement() {
		return new Setup(new TestSuite(THIS), false, true);
	}

	public void testTextEditorStartup() {
		PerformanceMeter perfMeter= createPerformanceMeterForSummary(SHORT_NAME, Dimension.ELAPSED_PROCESS);
		measureStartup(perfMeter);

	}
}

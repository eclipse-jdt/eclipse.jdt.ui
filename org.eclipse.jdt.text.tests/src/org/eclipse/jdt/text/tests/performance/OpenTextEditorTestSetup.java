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

import junit.extensions.TestSetup;
import junit.framework.Test;

public class OpenTextEditorTestSetup extends TestSetup {

	public OpenTextEditorTestSetup(Test test) {
		super(test);
	}

	protected void setUp() throws Exception {
		ResourceTestHelper.replicate("/" + PerformanceTestSetup.PROJECT + OpenTextEditorTest.ORIG_FILE, "/" + PerformanceTestSetup.PROJECT + OpenTextEditorTest.PATH + OpenTextEditorTest.FILE_PREFIX, OpenTextEditorTest.FILE_SUFFIX, OpenTextEditorTest.N_OF_RUNS, ResourceTestHelper.SKIP_IF_EXISTS);
	}
	
	protected void tearDown() {
		// do nothing, the actual test runs in its own workbench
	}
}

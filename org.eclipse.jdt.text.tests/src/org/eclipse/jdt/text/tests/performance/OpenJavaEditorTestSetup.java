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

public class OpenJavaEditorTestSetup extends TestSetup {

	public OpenJavaEditorTestSetup(Test test) {
		super(test);
	}

	protected void setUp() throws Exception {
		String src= "/" + PerformanceTestSetup.PROJECT + OpenJavaEditorTest.PATH + OpenJavaEditorTest.FILE_PREFIX + OpenJavaEditorTest.FILE_SUFFIX;
		String destPrefix= "/" + PerformanceTestSetup.PROJECT + OpenJavaEditorTest.PATH + OpenJavaEditorTest.FILE_PREFIX;
		String destSuffix= OpenJavaEditorTest.FILE_SUFFIX;
		String name= OpenJavaEditorTest.FILE_PREFIX;
		ResourceTestHelper.replicate(src, destPrefix, destSuffix, OpenJavaEditorTest.N_OF_RUNS, name, name, ResourceTestHelper.SKIP_IF_EXISTS);
	}
	
	protected void tearDown() throws Exception {
		// do nothing, the actual test runs in its own workbench
	}
}

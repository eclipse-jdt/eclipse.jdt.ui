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
 * @since 3.1
 */
public class PerformanceTestSuite extends TestSuite {

	public static Test suite() {
		return new PerformanceTestSetup(new PerformanceTestSuite());
	}
	
	public PerformanceTestSuite() {
		addTestSuite(RevertTextEditorTest.class);
		addTestSuite(RevertJavaEditorTest.class);
		addTestSuite(ScrollJavaEditorTest.class);
		addTestSuite(ToggleCommentTest.class);
		addTestSuite(UndoJavaEditorTest.class);
	}
}

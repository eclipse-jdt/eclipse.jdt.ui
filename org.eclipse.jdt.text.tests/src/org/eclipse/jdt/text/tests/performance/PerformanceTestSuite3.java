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
 * @since 3.1
 */
public class PerformanceTestSuite3 extends TestSuite {

	public static Test suite() {
		return new PerformanceTestSetup(new PerformanceTestSuite3());
	}

	public PerformanceTestSuite3() {
		addTest(OpenTextEditorTest.suite());
		addTest(new OpenJavaEditorStartupTest.Setup(EmptyTestCase.suite(), true, false)); // the actual test runs in its own workbench (see test.xml)
	}
}

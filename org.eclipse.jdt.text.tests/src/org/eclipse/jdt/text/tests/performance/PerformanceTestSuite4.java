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
public class PerformanceTestSuite4 extends TestSuite {

	public static Test suite() {
		// exclude PerformanceTestSetup because this suite measures startup performance
		return new PerformanceTestSuite4();
	}

	public PerformanceTestSuite4() {
		addTest(OpenJavaEditorStartupTest.suiteForMeasurement());
		addTest(new OpenTextEditorStartupTest.Setup(EmptyTestCase.suite(), true, false)); // the actual test runs in its own workbench (see test.xml)
	}
}

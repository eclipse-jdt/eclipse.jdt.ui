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

import org.eclipse.test.performance.PerformanceMeter;


/**
 * Startup performance tests.
 *
 * @since 3.1
 */
public class StartupPerformanceTestCase extends TextPerformanceTestCase {

	/*
	 * @see TextPerformanceTestCase#TestCase()
	 */
	public StartupPerformanceTestCase() {
		super();
	}

	/*
	 * @see TextPerformanceTestCase#TestCase(String)
	 */
	public StartupPerformanceTestCase(String name) {
		super(name);
	}

	protected void measureStartup(PerformanceMeter performanceMeter) {
		performanceMeter.stop();
		performanceMeter.commit();
		assertPerformance(performanceMeter);
	}
}

/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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

import org.eclipse.jface.text.GapTextStore;
import org.eclipse.jface.text.ITextStore;


/**
 * Performance test for the gap text store.
 * @since 3.3
 */
public class GapTextStorePerformanceTest extends TextStorePerformanceTest {
	public static Test suite() {
		return new PerformanceTestSetup(new PerfTestSuite(GapTextStorePerformanceTest.class));
	}

	public static Test setUpTest(Test test) {
		return new PerformanceTestSetup(test);
	}

	@Override
	protected ITextStore createTextStore() {
		return new GapTextStore(256, 4096, 0.1f); // watermark values from Document ctor
	}

}

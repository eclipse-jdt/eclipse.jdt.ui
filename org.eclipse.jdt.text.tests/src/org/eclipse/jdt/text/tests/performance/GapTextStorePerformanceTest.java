/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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

	protected ITextStore createTextStore() {
		return new GapTextStore(256, 4096, 0.1f); // watermark values from Document ctor
	}

}

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

import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;
import junit.framework.TestCase;

public class PerformanceTestCase extends TestCase {
	
	protected void runTest() throws Throwable {
		Performance performance= Performance.getDefault();
		PerformanceMeter meter= performance.createPerformanceMeter(performance.getDefaultScenarioId(this));
		meter.start();
		super.runTest();
		meter.stop();
		meter.commit();
		Performance.getDefault().assertPerformance(meter);
	}
}

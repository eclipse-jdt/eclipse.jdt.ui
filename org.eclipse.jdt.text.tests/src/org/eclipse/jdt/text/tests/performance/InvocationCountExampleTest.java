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


import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.eclipse.test.performance.Performance;


/**
 * To run this test add the following VM arguments:
 * <code>-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=7777,suspend=n,server=y -Declipse.perf.debugPort=7777.</code>
 * Try a different port if 7777 does not work.
 */
public class InvocationCountExampleTest extends TestCase {

	public void test() throws Exception {
		InvocationCountPerformanceMeter performanceMeter= new InvocationCountPerformanceMeter(Performance.getDefault().getDefaultScenarioId(this), new Method[] {
			Double.class.getDeclaredMethod("hashCode", new Class[] { }),
			Double.class.getDeclaredMethod("equals", new Class[] { Object.class }),
		});
		try {
			Set<Double> set= new HashSet<>();
			performanceMeter.start();
			set.add(Double.valueOf(10));
			set.add(Double.valueOf(9));
			set.add(Double.valueOf(11));
			set.add(Double.valueOf(10));
			performanceMeter.stop();
			performanceMeter.commit();
		} finally {
			performanceMeter.dispose();
		}
	}
}

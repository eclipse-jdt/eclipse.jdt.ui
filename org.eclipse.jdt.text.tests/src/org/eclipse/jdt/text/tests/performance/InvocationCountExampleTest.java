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
			Set set= new HashSet();
			performanceMeter.start();
			set.add(new Double(10));
			set.add(new Double(9));
			set.add(new Double(11));
			set.add(new Double(10));
			performanceMeter.stop();
			performanceMeter.commit();
		} finally {
			performanceMeter.dispose();
		}
	}
}

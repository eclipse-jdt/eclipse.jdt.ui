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

package org.eclipse.jdt.ui.tests.performance;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

public abstract class PerformanceMeterFactory {
	
	private static Set fScenarios= new HashSet();
	
	public PerformanceMeter createPerformanceMeter(String scenario) {
		assertUniqueScenario(scenario);
		return doCreatePerformanceMeter(scenario);
	}

	public PerformanceMeter createPerformanceMeter(TestCase testCase, String monitorId) {
		String scenario= testCase.getClass().getName() + "#" + testCase.getName() + "()";
		if (monitorId != null && monitorId.length() > 0)
			scenario= scenario + "-" + monitorId;
		return createPerformanceMeter(scenario);
	}

	public PerformanceMeter createPerformanceMeter(TestCase testCase) {
		return createPerformanceMeter(testCase, null);
	}

	protected abstract PerformanceMeter doCreatePerformanceMeter(String scenario);
	
	private static void assertUniqueScenario(String scenario) {
		if (fScenarios.contains(scenario))
			throw new IllegalArgumentException();
		fScenarios.add(scenario);
	}
}

/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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

package org.eclipse.jdt.ui.tests.performance;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.ui.tests.performance.views.TypeHierarchyPerfTest;

public class PerformanceTestSuite {
	public static Test suite() {
		TestSuite suite= new TestSuite(PerformanceTestSuite.class.getName());
		suite.addTest(TypeHierarchyPerfTest.suite());
		return suite;
	}
}

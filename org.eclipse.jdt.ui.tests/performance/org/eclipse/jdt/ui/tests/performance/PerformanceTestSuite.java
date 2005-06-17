/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
		TestSuite suite= new TestSuite("jdt.ui performance tests");
		suite.addTest(TypeHierarchyPerfTest.suite());
		return suite;
	}
}

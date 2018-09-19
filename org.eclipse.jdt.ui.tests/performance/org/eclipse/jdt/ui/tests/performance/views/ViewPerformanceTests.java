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
 package org.eclipse.jdt.ui.tests.performance.views;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ViewPerformanceTests extends TestCase {
	public static Test suite() {
		TestSuite suite= new TestSuite(ViewPerformanceTests.class.getName());
		suite.addTest(PackageExplorerPerfTest.suite());
		suite.addTest(PackageExplorerColdPerfTest.suite());
		suite.addTest(PackageExplorerEmptyPerfTest.suite());
		suite.addTest(PackageExplorerWarmPerfTest.suite());
		suite.addTest(PackageExplorerWorkspacePerfTest.suite());
		suite.addTest(PackageExplorerWorkspaceWarmPerfTest.suite());
		suite.addTest(TypeHierarchyPerfTest.suite());
		return suite;
	}
}

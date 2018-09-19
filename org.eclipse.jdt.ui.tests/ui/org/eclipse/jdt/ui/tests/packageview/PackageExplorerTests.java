/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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

package org.eclipse.jdt.ui.tests.packageview;

import junit.framework.Test;
import junit.framework.TestSuite;

public class PackageExplorerTests {

	public static Test suite() {
		TestSuite suite= new TestSuite(PackageExplorerTests.class.getName());
		//$JUnit-BEGIN$
		suite.addTest(ContentProviderTests1.suite());
		suite.addTest(ContentProviderTests2.suite());
		suite.addTest(ContentProviderTests3.suite());
		suite.addTest(ContentProviderTests4.suite());
		suite.addTest(ContentProviderTests5.suite());
		suite.addTest(ContentProviderTests6.suite());
		suite.addTest(ContentProviderTests7.suite());
		suite.addTest(PackageExplorerShowInTests.suite());
		suite.addTestSuite(WorkingSetDropAdapterTest.class);
		suite.addTest(HierarchicalContentProviderTests.suite());
		suite.addTestSuite(PackageCacheTest.class);
		//$JUnit-END$
		return suite;
	}
}

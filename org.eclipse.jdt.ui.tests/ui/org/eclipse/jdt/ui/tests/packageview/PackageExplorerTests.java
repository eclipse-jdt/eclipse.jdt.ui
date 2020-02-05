/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	ContentProviderTests1.class,
	ContentProviderTests2.class,
	ContentProviderTests3.class,
	ContentProviderTests4.class,
	ContentProviderTests5.class,
	ContentProviderTests6.class,
	ContentProviderTests7.class,
	PackageExplorerShowInTests.class,
	WorkingSetDropAdapterTest.class,
	HierarchicalContentProviderTests.class,
	PackageCacheTest.class
})
public class PackageExplorerTests {
}

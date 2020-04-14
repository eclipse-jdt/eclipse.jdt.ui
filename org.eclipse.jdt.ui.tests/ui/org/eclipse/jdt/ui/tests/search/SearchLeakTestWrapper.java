/*******************************************************************************
 * Copyright (c) 2007, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.search;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.ui.tests.core.rules.LeakTestSetup;


/**
 * This class is a wrapper for {@link SearchLeakTest}
 * in order to prevent the loading of the Search plug-in when the VM
 * verifies some JDT UI code.
 *
 * @since 3.4
 */
public class SearchLeakTestWrapper {

	@Rule
	public LeakTestSetup projectSetup = new LeakTestSetup();

	SearchLeakTest fTest;

	@Before
	public void setUp() throws Exception {
		fTest= new SearchLeakTest();
		fTest.setUp();
	}

	@After
	public void tearDown() throws Exception {
//		fTest.tearDown();
		fTest= null;
	}

	@Test
	public void testRemoveSearchQueries() throws Exception {
		fTest.testRemoveSearchQueries();
	}

	@Test
	public void testRemoveAllQueries() throws Exception {
		fTest.testRemoveAllQueries();
	}

	@Test
	public void testSearchResultEditorClose() throws Exception {
		fTest.testSearchResultEditorClose();
	}
}

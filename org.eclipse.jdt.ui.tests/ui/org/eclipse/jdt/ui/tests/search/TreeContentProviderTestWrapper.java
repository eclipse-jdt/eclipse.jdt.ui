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

package org.eclipse.jdt.ui.tests.search;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.ui.tests.core.rules.JUnitSourceSetup;

/**
 * This class is a wrapper for {@link org.eclipse.jdt.ui.tests.search.TreeContentProviderTest}
 * in order to prevent the loading of the Search plug-in when the VM
 * verifies some JDT UI code.
 *
 * @since 3.1
 */
public class TreeContentProviderTestWrapper {

	TreeContentProviderTest fTest;

	@Rule
	public JUnitSourceSetup projectSetup = new JUnitSourceSetup();

	@Before
	public void setUp() throws Exception {
		fTest= new TreeContentProviderTest();
		fTest.setUp();
	}

	@After
	public void tearDown() throws Exception {
		fTest.tearDown();
		fTest= null;
	}

	@Test
	public void testSimpleAdd() throws Exception {
		fTest.testSimpleAdd();
	}

	@Test
	public void testRemove() throws Exception {
		fTest.testRemove();
	}

	@Test
	public void testRemoveParentFirst() throws Exception {
		fTest.testRemoveParentFirst();
	}

	@Test
	public void testRemoveParentLast() throws Exception {
		fTest.testRemoveParentLast();
	}
}

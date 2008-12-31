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

package org.eclipse.jdt.ui.tests.search;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * This class is a wrapper for {@link org.eclipse.jdt.ui.tests.search.TreeContentProviderTest}
 * in order to prevent the loading of the Search plug-in when the VM
 * verifies some JDT UI code.
 *
 * @since 3.1
 */
public class TreeContentProviderTestWrapper extends TestCase {

	TreeContentProviderTest fTest;
	private String fName;

	public static Test allTests() {
		return new JUnitSourceSetup(new TestSuite(TreeContentProviderTestWrapper.class));
	}

	public static Test suite() {
		return allTests();
	}

	public TreeContentProviderTestWrapper(String name) {
		super(name);
		fName= name;
	}

	protected void setUp() throws Exception {
		fTest= new TreeContentProviderTest(fName);
		fTest.setUp();
	}


	/*
	 * @see junit.framework.TestCase#tearDown()
	 * @since 3.1
	 */
	protected void tearDown() throws Exception {
		fTest.tearDown();
		fTest= null;
	}
	public void testSimpleAdd() throws Exception {
		fTest.testSimpleAdd();
	}

	public void testRemove() throws Exception {
		fTest.testRemove();
	}

	public void testRemoveParentFirst() throws Exception {
		fTest.testRemoveParentFirst();
	}

	public void testRemoveParentLast() throws Exception {
		fTest.testRemoveParentLast();
	}
}

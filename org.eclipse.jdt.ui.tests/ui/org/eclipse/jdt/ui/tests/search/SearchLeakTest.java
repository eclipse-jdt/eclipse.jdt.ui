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
package org.eclipse.jdt.ui.tests.search;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.search2.internal.ui.InternalSearchUI;

import org.eclipse.jdt.ui.leaktest.LeakTestCase;
import org.eclipse.jdt.ui.leaktest.LeakTestSetup;

import org.eclipse.jdt.internal.ui.search.JavaSearchQuery;
import org.eclipse.jdt.internal.ui.search.JavaSearchResult;

/**
 */
public class SearchLeakTest extends LeakTestCase {
	public SearchLeakTest(String name) {
		super(name);
	}
	
	public static Test allTests() {
		return new LeakTestSetup(new JUnitSourceSetup(new TestSuite(SearchLeakTest.class)));
	}
	
	public static Test suite() {
		return allTests();
	}
	
	public void testRemoveSearchQueries() throws Exception {
		JavaSearchQuery query1= SearchTest.runMethodRefQuery("junit.framework.Test", "countTestCases", new String[0]);
		JavaSearchQuery query2= SearchTest.runMethodRefQuery("junit.framework.TestCase", "countTestCases", new String[0]);
		InternalSearchUI.getInstance().removeQuery(query1);
		InternalSearchUI.getInstance().removeQuery(query2);	
		query1= null;
		query2= null;
		assertEquals(0, getInstanceCount(JavaSearchResult.class));
	}
	public void testRemoveAllQueries() throws Exception {
		SearchTest.runMethodRefQuery("junit.framework.Test", "countTestCases", new String[0]);
		SearchTest.runMethodRefQuery("junit.framework.TestCase", "countTestCases", new String[0]);
		InternalSearchUI.getInstance().removeAllQueries();
		assertEquals(0, getInstanceCount(JavaSearchResult.class));
	}
}

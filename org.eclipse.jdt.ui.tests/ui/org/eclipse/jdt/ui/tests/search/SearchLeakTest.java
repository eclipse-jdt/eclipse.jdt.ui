/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.search;

import org.eclipse.jdt.testplugin.util.DisplayHelper;

import org.eclipse.swt.widgets.Display;

import org.eclipse.ui.editors.text.TextEditor;

import org.eclipse.search.internal.ui.text.FileSearchPage;
import org.eclipse.search.internal.ui.text.FileSearchQuery;
import org.eclipse.search.ui.ISearchResultViewPart;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.text.FileTextSearchScope;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.search2.internal.ui.InternalSearchUI;

import org.eclipse.jdt.ui.leaktest.LeakTestCase;
import org.eclipse.jdt.ui.leaktest.LeakTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.search.JavaSearchQuery;
import org.eclipse.jdt.internal.ui.search.JavaSearchResult;

/**
 * XXX: Every test in this class needs a delegate method in {@link SearchLeakTestWrapper}!
 */
public class SearchLeakTest extends LeakTestCase {

	private static final Class THIS= SearchLeakTest.class;

	public SearchLeakTest(String name) {
		super(name);
	}

	public static Test suite() {
		return setUpTest(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new LeakTestSetup(new JUnitSourceSetup(test));
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testRemoveSearchQueries() throws Exception {
		InternalSearchUI.getInstance().removeAllQueries();
		JavaSearchQuery query1= SearchTestHelper.runMethodRefQuery("junit.framework.Test", "countTestCases", new String[0]);
		JavaSearchQuery query2= SearchTestHelper.runMethodRefQuery("junit.framework.TestCase", "countTestCases", new String[0]);
		InternalSearchUI.getInstance().removeQuery(query1);
		InternalSearchUI.getInstance().removeQuery(query2);
		query1= null;
		query2= null;
		assertInstanceCount(JavaSearchResult.class, 0);
	}
	
	public void testRemoveAllQueries() throws Exception {
		SearchTestHelper.runMethodRefQuery("junit.framework.Test", "countTestCases", new String[0]);
		SearchTestHelper.runMethodRefQuery("junit.framework.TestCase", "countTestCases", new String[0]);
		InternalSearchUI.getInstance().removeAllQueries();
		assertInstanceCount(JavaSearchResult.class, 0);
	}
	
	public void testSearchResultEditorClose() throws Exception {
		assertInstanceCount(TextEditor.class, 0);
		
		FileTextSearchScope scope= FileTextSearchScope.newWorkspaceScope(null, false);
		FileSearchQuery query= new FileSearchQuery("projectDescription", false, false, scope);
		NewSearchUI.runQueryInForeground(null, query);
		ISearchResultViewPart view= NewSearchUI.getSearchResultView();
		FileSearchPage page= (FileSearchPage) view.getActivePage();
		
		DisplayHelper.sleep(Display.getDefault(), 2000);
		page.gotoNextMatch();
		
		assertInstanceCount(TextEditor.class, 1);
		
		assertTrue(JavaPlugin.getActivePage().closeAllEditors(false));
		
		assertInstanceCount(TextEditor.class, 0);
		
		NewSearchUI.removeQuery(query);
	}

}

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
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.search.ui.NewSearchUI;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

import org.eclipse.jdt.internal.ui.search.JavaSearchQuery;
import org.eclipse.jdt.internal.ui.search.JavaSearchResult;
import org.eclipse.jdt.internal.ui.search.ReferenceScopeFactory;

/**
 */
public class WorkspaceReferenceTest extends TestCase {

		public static Test allTests() {
			return new JUnitSourceSetup(new TestSuite(WorkspaceReferenceTest.class));
		};
	
	public WorkspaceReferenceTest(String name) {
		super(name);
	}
	
	public void testSimpleMethodRef() throws Exception {
		IJavaProject p= JUnitSourceSetup.getProject();
		IType type= p.findType("junit.framework.Test");
		IMethod method= type.getMethod("countTestCases", new String[0]);
		NewSearchUI.activateSearchResultView();
		JavaSearchQuery query= new JavaSearchQuery(method, IJavaSearchConstants.REFERENCES, ReferenceScopeFactory.create(method), "workspace scope");
		NewSearchUI.runQueryInForeground(null, query);
		JavaSearchResult result= (JavaSearchResult) query.getSearchResult();
		assertEquals(9, result.getMatchCount());
	}

	public void testFindOverridden() throws Exception {
		IJavaProject p= JUnitSourceSetup.getProject();
		IType type= p.findType("junit.framework.TestCase");
		IMethod method= type.getMethod("countTestCases", new String[0]);
		NewSearchUI.activateSearchResultView();
		JavaSearchQuery query= new JavaSearchQuery(method, IJavaSearchConstants.REFERENCES, ReferenceScopeFactory.create(method), "workspace scope");
		NewSearchUI.runQueryInForeground(null, query);
		JavaSearchResult result= (JavaSearchResult) query.getSearchResult();
		assertEquals(6, result.getMatchCount());
	}
}

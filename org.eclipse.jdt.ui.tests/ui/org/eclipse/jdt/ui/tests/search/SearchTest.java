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

import org.eclipse.search.ui.NewSearchUI;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

import org.eclipse.jdt.internal.ui.search.JavaSearchQuery;
import org.eclipse.jdt.internal.ui.search.JavaSearchResult;
import org.eclipse.jdt.internal.ui.search.ReferenceScopeFactory;

public class SearchTest {
	public static Test suite() {
		TestSuite suite= new TestSuite("Java Search Tests"); //$NON-NLS-1$
		//suite.addTestSuite(WorkspaceScopeTest.class);
		suite.addTest(WorkspaceReferenceTest.allTests());
		return suite;
	}

	static int countMethodRefs(String TypeName, String methodName, String[] parameterTypes) throws JavaModelException {
		JavaSearchQuery query= runMethodRefQuery(TypeName, methodName, parameterTypes);
		JavaSearchResult result= (JavaSearchResult) query.getSearchResult();
		return result.getMatchCount();
	}

	static JavaSearchQuery runMethodRefQuery(String TypeName, String methodName, String[] parameterTypes) throws JavaModelException {
		IJavaProject p= JUnitSourceSetup.getProject();
		IType type= p.findType(TypeName);
		IMethod method= type.getMethod(methodName, parameterTypes);
		NewSearchUI.activateSearchResultView();
		JavaSearchQuery query= new JavaSearchQuery(method, IJavaSearchConstants.REFERENCES, ReferenceScopeFactory.create(method), "workspace scope");
		NewSearchUI.runQueryInForeground(null, query);
		return query;
	}
}

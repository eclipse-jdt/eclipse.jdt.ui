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
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;
import org.eclipse.jdt.core.search.IJavaSearchScope;

/**
 * Describes the a java search query.  This class is not to be subclassed by 
 * clients.
 * Clients must not instantiate or subclass this class.
 */
public abstract class QuerySpecification {
	private IJavaSearchScope fScope;
	private int fLimitTo;
	private String fScopeDescription;

	QuerySpecification(int limitTo, IJavaSearchScope scope, String scopeDescription) {
		fScope= scope;
		fLimitTo= limitTo;
		fScopeDescription= scopeDescription;
	}

	/**
	 * Returns the serach scope to be used in the query.
	 * @return The search scope.
	 */
	public IJavaSearchScope getScope() {
		return fScope;
	}
	
	/**
	 * Returns a human readable description of the search scope.
	 * @see #getScope()
	 * @return A description of the search scope. 
	 */
	public String getScopeDescription() {
		return fScopeDescription;
	}
	
	/**
	 * Returns what kind of occurences the query should look for. 
	 * @see org.eclipse.jdt.core.search.IJavaSearchConstants
	 * @see org.eclipse.jdt.core.search.SearchEngine#search(IWorkspace, IJavaElement, int, IJavaSearchScope, IJavaSearchResultCollector)
	 * @return
	 */
	public int getLimitTo() {
		return fLimitTo;
	}

}

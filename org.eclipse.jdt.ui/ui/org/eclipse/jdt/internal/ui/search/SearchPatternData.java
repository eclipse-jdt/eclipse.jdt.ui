/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.ui.IWorkingSet;

import org.eclipse.search.ui.ISearchPageContainer;

import org.eclipse.jdt.core.IJavaElement;


public class SearchPatternData {
	private int			searchFor;
	private int			limitTo;
	private String			pattern;
	private boolean		isCaseSensitive;
	private IJavaElement	javaElement;
	private int			scope;
	private IWorkingSet[]	 	workingSets;
	
	public SearchPatternData(int searchFor, int limitTo, boolean isCaseSensitive, String pattern, IJavaElement element) {
		this(searchFor, limitTo, pattern, isCaseSensitive, element, ISearchPageContainer.WORKSPACE_SCOPE, null);
	}
	
	public SearchPatternData(int s, int l, String p, boolean i, IJavaElement element, int scope, IWorkingSet[] workingSets) {
		setSearchFor(s);
		setLimitTo(l);
		setPattern(p);
		setCaseSensitive(i);
		setJavaElement(element);
		this.setScope(scope);
		this.setWorkingSets(workingSets);
	}

	public void setCaseSensitive(boolean isCaseSensitive) {
		this.isCaseSensitive= isCaseSensitive;
	}

	public boolean isCaseSensitive() {
		return isCaseSensitive;
	}

	public void setJavaElement(IJavaElement javaElement) {
		this.javaElement= javaElement;
	}

	public IJavaElement getJavaElement() {
		return javaElement;
	}

	public void setLimitTo(int limitTo) {
		this.limitTo= limitTo;
	}

	public int getLimitTo() {
		return limitTo;
	}

	public void setPattern(String pattern) {
		this.pattern= pattern;
	}

	public String getPattern() {
		return pattern;
	}

	public void setScope(int scope) {
		this.scope= scope;
	}

	public int getScope() {
		return scope;
	}

	public void setSearchFor(int searchFor) {
		this.searchFor= searchFor;
	}

	public int getSearchFor() {
		return searchFor;
	}

	public void setWorkingSets(IWorkingSet[] workingSets) {
		this.workingSets= workingSets;
	}

	public IWorkingSet[] getWorkingSets() {
		return workingSets;
	}
}

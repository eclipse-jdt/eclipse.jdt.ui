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
package org.eclipse.jdt.internal.corext.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.Assert;

public class SearchResultGroup {

	private final IResource fResouce;
	private final List fSearchResults;
	
	public SearchResultGroup(IResource res, SearchResult[] results){
		Assert.isNotNull(results);
		fResouce= res;
		fSearchResults= new ArrayList(Arrays.asList(results));//have to is this way to allow adding
	}

	public void add(SearchResult result) {
		Assert.isNotNull(result);
		fSearchResults.add(result);		
	}
	
	public IResource getResource() {
		return fResouce;
	}
	
	public SearchResult[] getSearchResults() {
		return (SearchResult[]) fSearchResults.toArray(new SearchResult[fSearchResults.size()]);
	}
	
	public static IResource[] getResources(SearchResultGroup[] searchResultGroups){
		Set resourceSet= new HashSet(searchResultGroups.length);
		for (int i= 0; i < searchResultGroups.length; i++) {
			resourceSet.add(searchResultGroups[i].getResource());
		}
		return (IResource[]) resourceSet.toArray(new IResource[resourceSet.size()]);
	}
	
	public ICompilationUnit getCompilationUnit(){
		if (getSearchResults() == null || getSearchResults().length == 0)
			return null;
		return getSearchResults()[0].getCompilationUnit();
	}
}
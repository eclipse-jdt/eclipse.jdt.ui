/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.ICompilationUnit;

public class SearchResultGroup {

	private IResource fResouce;
	private SearchResult[] fSearchResults;
	
	public SearchResultGroup(IResource res, SearchResult[] results){
		fResouce= res;
		fSearchResults= results;
	}

	public IResource getResource() {
		return fResouce;
	}

	public SearchResult[] getSearchResults() {
		return fSearchResults;
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


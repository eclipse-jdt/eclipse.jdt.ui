/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring;

import org.eclipse.core.resources.IResource;

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
}


/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;

/**
 * Collects the results returned by a <code>SearchEngine</code>.
 */
public class SearchResultCollector implements IJavaSearchResultCollector {

	private ArrayList fFound;
	private IProgressMonitor fPM;

	public SearchResultCollector(IProgressMonitor pm) {
		fPM= pm;
		fFound= new ArrayList();
	}

	/**
	 * @see IJavaSearchResultCollector#aboutToStart
	 */
	public void aboutToStart() {
	}
	
	/**
	 * @see IJavaSearchResultCollector#accept(org.eclipse.core.resources.IResource, int, int, org.eclipse.jdt.core.IJavaElement, int)
	 */
	public void accept(IResource res, int start, int end, IJavaElement element, int accuracy) {
		fFound.add(new SearchResult(res, start, end, element, accuracy));
	}
	
	/**
	 * @see IJavaSearchResultCollector#done()
	 */
	public void done() {
	}

	/**
	 * @see IJavaSearchResultCollector#getProgressMonitor()
	 */
	public IProgressMonitor getProgressMonitor() {
		return fPM;
	}

	/**
	 * returns collected results.
	 * returns a List of <code>SearchResults</code> (not sorted)
	 */
	public List getResults() {
		return fFound;
	}

}



/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.internal.core.refactoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

/**
 * Convenience wrapper for <code>SearchEngine</code> - performs searching and sorts the results.
 */
public class RefactoringSearchEngine{

	private static Comparator fgSearchResultComparator;
	private static SearchEngine fgSearchEngine= new SearchEngine();
	
	//no instances
	private RefactoringSearchEngine(){
	}
	
	private static void search(IProgressMonitor pm, IJavaSearchScope scope, ISearchPattern pattern, IJavaSearchResultCollector collector) throws JavaModelException {
		if (pattern == null)
			return;
		Assert.isNotNull(scope, "scope must not be null");
		try{
			fgSearchEngine.search(ResourcesPlugin.getWorkspace(), pattern, scope, collector);
		} catch (CoreException e){
			throw new JavaModelException(e);
		}	
	}
	
	/**
	 * Performs searching for a given <code>SearchPattern</code>.
	 * Returns a List of Lists or <code>SearchResults</code>.
	 * Each of these lists collects <code>SearchResults</code> found in one resource
	 * and each is sorted backwards by <code>SearchResult#getStart()</code> 
	 * @see SearchResult
	 */			
	public static List search(IProgressMonitor pm, IJavaSearchScope scope, ISearchPattern pattern) throws JavaModelException {
		SearchResultCollector collector= new SearchResultCollector(pm);
		search(pm, scope, pattern, collector);	
		List l= collector.getResults();
		Collections.sort(l, createComparator());
		return groupByResource(l);
	}
	
	public static List customSearch(IProgressMonitor pm, IJavaSearchScope scope, ISearchPattern pattern) throws JavaModelException {
		SearchEngine engine= fgSearchEngine;
		fgSearchEngine= new CustomSearchEngine();
		try{
			return search(pm, scope, pattern);
		} finally{
			fgSearchEngine= engine;
		}
	}

	private static Comparator createComparator() {
		if (fgSearchResultComparator == null) {
			fgSearchResultComparator= new Comparator() {
				public int compare(Object o1, Object o2) {
					/* it's enough to sort them by starting position
					 * i sort them backwards (then i can substitute substrings easily)
					 * i group them by resources
					 */
					SearchResult sr1= (SearchResult) o1;
					SearchResult sr2= (SearchResult) o2;

					if (!(sr2.getResource().equals(sr1.getResource()))) {
						return sr2.getResource().getFullPath().toString().compareTo(sr1.getResource().getFullPath().toString());
					}
					return sr2.getStart() - sr1.getStart();
				}
			};
		}
		return fgSearchResultComparator;
	}

	/**
	 * returns a list of lists of SearchResults (grouped by resource)
	 */
	private static List groupByResource(List searchResults){
		if (searchResults == null || searchResults.isEmpty())
			return new ArrayList(0); 
			
		Iterator iter= searchResults.iterator();
		List result= new ArrayList(5);
	
		List subResult= new ArrayList(3);
	
		SearchResult t= (SearchResult)iter.next();
		IPath tPath= t.getResource().getFullPath();
		subResult.add(t);
		
		boolean same= true;
		while (iter.hasNext()){
			SearchResult each= (SearchResult)iter.next();
			same= each.getResource().getFullPath().equals(tPath);
			if (same){
				subResult.add(each);
			} else {
				result.add(subResult);
				subResult= new ArrayList(3);
				t= each;
				tPath= t.getResource().getFullPath();
				subResult.add(t);
			}
		}
		result.add(subResult);
		return result;
	}
}




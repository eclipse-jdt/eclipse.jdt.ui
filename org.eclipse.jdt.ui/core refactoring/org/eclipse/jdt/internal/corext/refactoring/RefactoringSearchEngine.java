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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.Assert;

/**
 * Convenience wrapper for <code>SearchEngine</code> - performs searching and sorts the results.
 */
public class RefactoringSearchEngine {

	private RefactoringSearchEngine(){
		//no instances
	}
	
	public static ICompilationUnit[] findAffectedCompilationUnits(final IProgressMonitor pm, IJavaSearchScope scope, ISearchPattern pattern) throws JavaModelException {
		final Set matches= new HashSet(5);
		IJavaSearchResultCollector collector = new IJavaSearchResultCollector() {
			private IResource fLastMatch;
			public void aboutToStart() { /*nothing*/ }
			public void accept(IResource resource, int start, int end, IJavaElement enclosingElement, int accuracy) throws CoreException {
				if (fLastMatch != resource) {
					matches.add(resource);	
					fLastMatch= resource;
				}
			}
			public void done() { /*nothing*/ }
			public IProgressMonitor getProgressMonitor() {
				return pm;
			}
		};
		new SearchEngine().search(ResourcesPlugin.getWorkspace(), pattern, scope, collector);

		List result= new ArrayList(matches.size());
		for (Iterator iter= matches.iterator(); iter.hasNext(); ) {
			IResource resource= (IResource)iter.next();
			IJavaElement element= JavaCore.create(resource);
			if (element instanceof ICompilationUnit) {
				result.add(element);
			}
		}
		return (ICompilationUnit[])result.toArray(new ICompilationUnit[result.size()]);
	}
			
	/**
	 * Performs searching for a given <code>SearchPattern</code>.
	 * Returns SearchResultGroup[] 
	 * In each of SearchResultGroups all SearchResults are
	 * sorted backwards by <code>SearchResult#getStart()</code> 
	 * @see SearchResult
	 */			
	public static SearchResultGroup[] search(IProgressMonitor pm, IJavaSearchScope scope, ISearchPattern pattern) throws JavaModelException {
		return search(scope, pattern, new SearchResultCollector(pm));
	}
	
	public static SearchResultGroup[] search(IJavaSearchScope scope, ISearchPattern pattern, SearchResultCollector collector) throws JavaModelException {
		return search(scope, pattern, collector, null);
	}
	
	public static SearchResultGroup[] search(IProgressMonitor pm, IJavaSearchScope scope, ISearchPattern pattern, ICompilationUnit[] workingCopies) throws JavaModelException {
		return search(scope, pattern, new SearchResultCollector(pm), workingCopies);
	}
	
	public static SearchResultGroup[] search(IJavaSearchScope scope, ISearchPattern pattern, SearchResultCollector collector, ICompilationUnit[] workingCopies) throws JavaModelException {
		internalSearch(scope, pattern, collector, workingCopies);	
		return groupByResource(createSearchResultArray(collector.getResults()));
	}
	
	public static SearchResultGroup[] search(WorkingCopyOwner owner, IJavaSearchScope scope, ISearchPattern pattern, SearchResultCollector collector) throws JavaModelException {
		new SearchEngine(owner).search(ResourcesPlugin.getWorkspace(), pattern, scope, collector);
		return groupByResource(createSearchResultArray(collector.getResults()));
	}
	
	public static SearchResultGroup[] groupByResource(SearchResult[] results){
		Map grouped= groupByResource(Arrays.asList(results));
		
		SearchResultGroup[] result= new SearchResultGroup[grouped.keySet().size()];
		int i= 0;
		for (Iterator iter= grouped.keySet().iterator(); iter.hasNext();) {
			IResource resource= (IResource)iter.next();
			List searchResults= (List)grouped.get(resource);
			result[i]= new SearchResultGroup(resource, createSearchResultArray(searchResults));
			i++;
		}
		return result;		
	}
	
	private static SearchResult[] createSearchResultArray(List searchResults){
		return (SearchResult[])searchResults.toArray(new SearchResult[searchResults.size()]);
	}
	
	private static Map groupByResource(List searchResults){
		Map grouped= new HashMap(); //IResource -> List of SearchResults
		for (Iterator iter= searchResults.iterator(); iter.hasNext();) {
			SearchResult searchResult= (SearchResult) iter.next();
			if (! grouped.containsKey(searchResult.getResource()))
				grouped.put(searchResult.getResource(), new ArrayList(1));
			((List)grouped.get(searchResult.getResource())).add(searchResult);
		}
		return grouped;
	}
	
	private static void internalSearch(IJavaSearchScope scope, ISearchPattern pattern, IJavaSearchResultCollector collector, ICompilationUnit[] workingCopies) throws JavaModelException {
		if (pattern == null)
			return;
		Assert.isNotNull(scope, "scope"); //$NON-NLS-1$
		createSearchEngine(workingCopies).search(ResourcesPlugin.getWorkspace(), pattern, scope, collector);
	}
	
	private static SearchEngine createSearchEngine(ICompilationUnit[] workingCopies){
		if (workingCopies == null)
			return new SearchEngine();
		else 	
			return  new SearchEngine(workingCopies);
	}
	
	public static ISearchPattern createSearchPattern(IJavaElement[] elements, int limitTo) {
		if (elements == null || elements.length == 0)
			return null;
		Set set= new HashSet(Arrays.asList(elements));
		Iterator iter= set.iterator();
		IJavaElement first= (IJavaElement)iter.next();
		ISearchPattern pattern= createSearchPattern(first, limitTo);
		while(iter.hasNext()){
			IJavaElement each= (IJavaElement)iter.next();
			pattern= SearchEngine.createOrSearchPattern(pattern, createSearchPattern(each, limitTo));
		}
		return pattern;
	}

	private static ISearchPattern createSearchPattern(IJavaElement element, int limitTo) {
		return SearchEngine.createSearchPattern(element, limitTo);
	}
	
}

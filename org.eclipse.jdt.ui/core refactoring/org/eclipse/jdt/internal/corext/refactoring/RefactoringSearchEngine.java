/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.refactoring.util.ResourceManager;

/**
 * Convenience wrapper for <code>SearchEngine</code> - performs searching and sorts the results.
 */
public class RefactoringSearchEngine {

	private static Comparator fgSearchResultComparator;
	private static SearchEngine fgSearchEngine= new SearchEngine();
	
	//no instances
	private RefactoringSearchEngine(){
	}
	
	public static ICompilationUnit[] findAffectedCompilationUnits(final IProgressMonitor pm, IJavaSearchScope scope, ISearchPattern pattern) throws JavaModelException {
		final Set matches= new HashSet(5);
		IJavaSearchResultCollector collector = new IJavaSearchResultCollector() {
			private IResource fLastMatch;
			public void aboutToStart() {};
			public void accept(IResource resource, int start, int end, IJavaElement enclosingElement, int accuracy) throws CoreException {
				if (fLastMatch != resource) {
					matches.add(resource);	
					fLastMatch= resource;
				}
			}
			public void done() {};
			public IProgressMonitor getProgressMonitor() {
				return pm;
			};
		};
		fgSearchEngine.search(ResourcesPlugin.getWorkspace(), pattern, scope, collector);
		ICompilationUnit[] workingCopies= ResourceManager.getWorkingCopies();
		List result= new ArrayList(matches.size());
		for (Iterator iter= matches.iterator(); iter.hasNext(); ) {
			IResource resource= (IResource)iter.next();
			IJavaElement element= JavaCore.create(resource);
			if (element instanceof ICompilationUnit) {
				ICompilationUnit original= (ICompilationUnit)element;
				ICompilationUnit wcopy= getWorkingCopy(original, workingCopies);
				if (wcopy != null)
					result.add(wcopy);
				else
					result.add(original);
			}
		}
		return (ICompilationUnit[])result.toArray(new ICompilationUnit[result.size()]);
	}
	
	private static ICompilationUnit getWorkingCopy(ICompilationUnit unit, ICompilationUnit[] workingCopies) {
		for (int i= 0; i < workingCopies.length; i++) {
			ICompilationUnit wcopy= workingCopies[i];
			if (unit.equals(wcopy.getOriginalElement()))
				return wcopy;
		}
		return null;
	}
	
	private static void search(IProgressMonitor pm, IJavaSearchScope scope, ISearchPattern pattern, IJavaSearchResultCollector collector) throws JavaModelException {
		if (pattern == null)
			return;
		Assert.isNotNull(scope, "scope"); //$NON-NLS-1$
		fgSearchEngine.search(ResourcesPlugin.getWorkspace(), pattern, scope, collector);
	}
	
	/**
	 * Performs searching for a given <code>SearchPattern</code>.
	 * Returns SearchResultGroup[] 
	 * In each of SearchResultGroups all SearchResults are
	 * sorted backwards by <code>SearchResult#getStart()</code> 
	 * @see SearchResult
	 */			
	public static SearchResultGroup[] search(IProgressMonitor pm, IJavaSearchScope scope, ISearchPattern pattern) throws JavaModelException {
		SearchResultCollector collector= new SearchResultCollector(pm);
		search(pm, scope, pattern, collector);	
		List l= collector.getResults();
		Collections.sort(l, createComparator());
		List grouped= groupByResource(l);
		
		SearchResultGroup[] result= new SearchResultGroup[grouped.size()];
		for (int i= 0; i < result.length; i++){
			List searchResults= (List)grouped.get(i);
			IResource res= ((SearchResult)searchResults.get(0)).getResource();
			result[i]= new SearchResultGroup(res, createSearchResultArray(searchResults));
		}
		return result;
	}
	
	private static SearchResult[] createSearchResultArray(List searchResults){
		return (SearchResult[])searchResults.toArray(new SearchResult[searchResults.size()]);
	}
	
	//XXX: should get rid of this
	public static SearchResultGroup[] customSearch(IProgressMonitor pm, IJavaSearchScope scope, ISearchPattern pattern) throws JavaModelException {
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




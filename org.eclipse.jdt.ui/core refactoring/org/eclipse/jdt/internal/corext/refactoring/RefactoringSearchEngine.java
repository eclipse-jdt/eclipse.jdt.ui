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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

import org.eclipse.jdt.internal.corext.util.SearchUtils;

/**
 * Convenience wrapper for {@link SearchEngine} - performs searching and sorts the results by {@link IResource}.
 * TODO: throw CoreExceptions from search(..) methods instead of wrapped JavaModelExceptions.
 */
public class RefactoringSearchEngine {

	private RefactoringSearchEngine(){
		//no instances
	}
	
	//TODO: throw CoreException
	public static ICompilationUnit[] findAffectedCompilationUnits(SearchPattern pattern,
			IJavaSearchScope scope, final IProgressMonitor pm) throws JavaModelException {
		
		final Set resources= new HashSet(5);
		SearchRequestor requestor = new SearchRequestor() {
			private IResource fLastResource;
			public void acceptSearchMatch(SearchMatch match) {
				if (fLastResource != match.getResource()) {
					fLastResource= match.getResource();
					resources.add(fLastResource);	
				}
			}
		};
		try {
			new SearchEngine().search(pattern, SearchUtils.getDefaultSearchParticipants(), scope, requestor, pm);
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}

		List result= new ArrayList(resources.size());
		for (Iterator iter= resources.iterator(); iter.hasNext(); ) {
			IResource resource= (IResource) iter.next();
			IJavaElement element= JavaCore.create(resource);
			if (element instanceof ICompilationUnit) {
				result.add(element);
			}
		}
		return (ICompilationUnit[]) result.toArray(new ICompilationUnit[result.size()]);
	}
			
	/**
	 * Performs a search and groups the resulting {@link SearchMatch}es by
	 * {@link SearchMatch#getResource()}.
	 * 
	 * @return a {@link SearchResultGroup}[], where each {@link SearchResultGroup} 
	 * 		has a different {@link SearchMatch#getResource() getResource()}s.
	 * @see SearchMatch
	 * @throws CoreException when the search failed
	 */
	//TODO: throw CoreException
	public static SearchResultGroup[] search(SearchPattern pattern, IJavaSearchScope scope, IProgressMonitor monitor)
			throws JavaModelException {
		return internalSearch(new SearchEngine(), pattern, scope, new CollectingSearchRequestor(), monitor);
	}
	
	//TODO: throw CoreException
	public static SearchResultGroup[] search(SearchPattern pattern, IJavaSearchScope scope, CollectingSearchRequestor requestor,
			IProgressMonitor monitor) throws JavaModelException {
		return internalSearch(new SearchEngine(), pattern, scope, requestor, monitor);
	}
	
	//TODO: throw CoreException
	public static SearchResultGroup[] search(SearchPattern pattern, IJavaSearchScope scope, CollectingSearchRequestor requestor,
			IProgressMonitor monitor, WorkingCopyOwner owner) throws JavaModelException {
		return internalSearch(new SearchEngine(owner), pattern, scope, requestor, monitor);
	}
	
	/** @deprecated use {@link #search(SearchPattern, IJavaSearchScope, CollectingSearchRequestor, IProgressMonitor, WorkingCopyOwner)} */
	//TODO: throw CoreException
	public static SearchResultGroup[] search(SearchPattern pattern, IJavaSearchScope scope,
			IProgressMonitor monitor, ICompilationUnit[] workingCopies) throws JavaModelException {
		return internalSearch(new SearchEngine(workingCopies), pattern, scope, new CollectingSearchRequestor(), monitor);
	}
	
	//TODO: throw CoreException
	private static SearchResultGroup[] internalSearch(SearchEngine searchEngine, SearchPattern pattern, IJavaSearchScope scope,
			CollectingSearchRequestor requestor, IProgressMonitor monitor) throws JavaModelException {
		try {
			searchEngine.search(pattern, SearchUtils.getDefaultSearchParticipants(), scope, requestor, monitor);
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}
		return groupByResource(requestor.getResults());
	}

	public static SearchResultGroup[] groupByResource(SearchMatch[] matches){
		return groupByResource(Arrays.asList(matches));	
	}
	
	/**
	 * @param matchList a List of SearchMatch
	 * @return a SearchResultGroup[], sorted by SearchMatch#getResource()
	 */
	public static SearchResultGroup[] groupByResource(List matchList) {
		Map grouped= new HashMap();
		for (Iterator iter= matchList.iterator(); iter.hasNext();) {
			SearchMatch searchMatch= (SearchMatch) iter.next();
			if (! grouped.containsKey(searchMatch.getResource()))
				grouped.put(searchMatch.getResource(), new ArrayList(1));
			((List) grouped.get(searchMatch.getResource())).add(searchMatch);
		}
		
		SearchResultGroup[] result= new SearchResultGroup[grouped.keySet().size()];
		int i= 0;
		for (Iterator iter= grouped.keySet().iterator(); iter.hasNext();) {
			IResource resource= (IResource) iter.next();
			List searchMatches= (List) grouped.get(resource);
			SearchMatch[] matchArray= (SearchMatch[]) searchMatches.toArray(new SearchMatch[searchMatches.size()]);
			result[i]= new SearchResultGroup(resource, matchArray);
			i++;
		}
		return result;
	}
	
	public static SearchPattern createOrPattern(IJavaElement[] elements, int limitTo) {
		if (elements == null || elements.length == 0)
			return null;
		Set set= new HashSet(Arrays.asList(elements));
		Iterator iter= set.iterator();
		IJavaElement first= (IJavaElement)iter.next();
		SearchPattern pattern= SearchPattern.createPattern(first, limitTo);
		while(iter.hasNext()){
			IJavaElement each= (IJavaElement)iter.next();
			pattern= SearchPattern.createOrPattern(pattern, SearchPattern.createPattern(each, limitTo));
		}
		return pattern;
	}
}

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

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

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
			IJavaSearchScope scope, final IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		
		final boolean[] hasPotentialMatches= { false };
		boolean hasNonCuMatches= false;
		
		final Set resources= new HashSet(5);
		SearchRequestor requestor = new SearchRequestor() {
			private IResource fLastResource;
			public void acceptSearchMatch(SearchMatch match) {
				if (match.getAccuracy() == SearchMatch.A_INACCURATE)
					hasPotentialMatches[0]= true;
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
			} else {
				hasNonCuMatches= true;
			}
		}
		addStatusErrors(status, hasPotentialMatches[0], hasNonCuMatches);
		return (ICompilationUnit[]) result.toArray(new ICompilationUnit[result.size()]);
	}
	
	/**
	 * Performs a search and groups the resulting {@link SearchMatch}es by
	 * {@link SearchResultGroup#getCompilationUnit()}.
	 *
	 * @param status an error is added here if inaccurate or non-cu matches have been found
	 * @return a {@link SearchResultGroup}[], where each {@link SearchResultGroup} 
	 * 		has a different {@link SearchMatch#getResource() getResource()}s.
	 * @see SearchMatch
	 * @throws JavaModelException when the search failed
	 */
	//TODO: throw CoreException
	public static SearchResultGroup[] search(SearchPattern pattern, IJavaSearchScope scope, IProgressMonitor monitor, RefactoringStatus status)
			throws JavaModelException {
		return internalSearch(new SearchEngine(), pattern, scope, new CollectingSearchRequestor(), monitor, status);
	}
	
	//TODO: throw CoreException
	public static SearchResultGroup[] search(SearchPattern pattern, IJavaSearchScope scope, CollectingSearchRequestor requestor,
			IProgressMonitor monitor, RefactoringStatus status) throws JavaModelException {
		return internalSearch(new SearchEngine(), pattern, scope, requestor, monitor, status);
	}
	
	//TODO: throw CoreException
	public static SearchResultGroup[] search(SearchPattern pattern, IJavaSearchScope scope, CollectingSearchRequestor requestor,
			IProgressMonitor monitor, WorkingCopyOwner owner, RefactoringStatus status) throws JavaModelException {
		return internalSearch(new SearchEngine(owner), pattern, scope, requestor, monitor, status);
	}
	
	/** @deprecated use {@link #search(SearchPattern, IJavaSearchScope, CollectingSearchRequestor, IProgressMonitor, WorkingCopyOwner, RefactoringStatus)} */
	//TODO: throw CoreException
	public static SearchResultGroup[] search(SearchPattern pattern, IJavaSearchScope scope,
			IProgressMonitor monitor, ICompilationUnit[] workingCopies, RefactoringStatus status) throws JavaModelException {
		return internalSearch(new SearchEngine(workingCopies), pattern, scope, new CollectingSearchRequestor(), monitor, status);
	}
	
	//TODO: throw CoreException
	private static SearchResultGroup[] internalSearch(SearchEngine searchEngine, SearchPattern pattern, IJavaSearchScope scope,
			CollectingSearchRequestor requestor, IProgressMonitor monitor, RefactoringStatus status) throws JavaModelException {
		try {
			searchEngine.search(pattern, SearchUtils.getDefaultSearchParticipants(), scope, requestor, monitor);
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}
		return groupByCu(requestor.getResults(), status);
	}

	public static SearchResultGroup[] groupByCu(SearchMatch[] matches, RefactoringStatus status) {
		return groupByCu(Arrays.asList(matches), status);	
	}
	
	/**
	 * @param matchList a List of SearchMatch
	 * @param status the status to report errors.
	 * @return a SearchResultGroup[], grouped by SearchMatch#getResource()
	 */
	public static SearchResultGroup[] groupByCu(List matchList, RefactoringStatus status) {
		Map/*<IResource, List<SearchMatch>>*/ grouped= new HashMap();
		boolean hasPotentialMatches= false;
		boolean hasNonCuMatches= false;
		
		for (Iterator iter= matchList.iterator(); iter.hasNext();) {
			SearchMatch searchMatch= (SearchMatch) iter.next();
			if (searchMatch.getAccuracy() == SearchMatch.A_INACCURATE)
				hasPotentialMatches= true;
			if (! grouped.containsKey(searchMatch.getResource()))
				grouped.put(searchMatch.getResource(), new ArrayList(1));
			((List) grouped.get(searchMatch.getResource())).add(searchMatch);
		}
		
		for (Iterator iter= grouped.keySet().iterator(); iter.hasNext();) {
			IResource resource= (IResource) iter.next();
			IJavaElement element= JavaCore.create(resource);
			if (! (element instanceof ICompilationUnit)) {
				iter.remove();
				hasNonCuMatches= true;
			}
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
		addStatusErrors(status, hasPotentialMatches, hasNonCuMatches);
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
	
	private static void addStatusErrors(RefactoringStatus status, boolean hasPotentialMatches, boolean hasNonCuMatches) {
		if (hasPotentialMatches)
			status.addError(RefactoringCoreMessages.getString("RefactoringSearchEngine.potential_matches")); //$NON-NLS-1$
		if (hasNonCuMatches)
			status.addError(RefactoringCoreMessages.getString("RefactoringSearchEngine.non_cu_matches")); //$NON-NLS-1$
	}
}

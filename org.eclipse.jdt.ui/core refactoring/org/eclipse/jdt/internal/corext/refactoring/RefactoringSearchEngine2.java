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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IProject;
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

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.util.SearchUtils;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

/**
 * Helper class to use the search engine in refactorings.
 * 
 * @since 3.1
 */
public final class RefactoringSearchEngine2 {

	/** Default implementation of a search requestor */
	private static class DefaultSearchRequestor implements IRefactoringSearchRequestor {

		public final boolean acceptSearchMatch(final SearchMatch match) {
			return true;
		}
	}

	/** Search match collector to perform reporting */
	private class RefactoringSearchCollector extends SearchRequestor {

		/** The binary resources */
		private final Set fBinaryResources= new HashSet();

		/** The collected matches */
		private final List fCollectedMatches= new ArrayList();

		/** The inaccurate matches */
		private final Set fInaccurateMatches= new HashSet();

		public final void acceptSearchMatch(final SearchMatch match) throws CoreException {
			Assert.isNotNull(match);
			if (fRequestor.acceptSearchMatch(match)) {
				fCollectedMatches.add(match);
				if (fBinary) {
					final IResource resource= match.getResource();
					final IJavaElement element= JavaCore.create(resource);
					if (!(element instanceof ICompilationUnit)) {
						final IProject project= resource.getProject();
						if (!fGrouping)
							fStatus.addEntry(fSeverity, RefactoringCoreMessages.getFormattedString("RefactoringSearchEngine.binary.match.ungrouped", project.getName()), null, null, RefactoringStatusEntry.NO_CODE); //$NON-NLS-1$
						else if (!fBinaryResources.contains(resource))
							fStatus.addEntry(fSeverity, RefactoringCoreMessages.getFormattedString("RefactoringSearchEngine.binary.match.grouped", project.getName()), null, null, RefactoringStatusEntry.NO_CODE); //$NON-NLS-1$
						fBinaryResources.add(resource);
					}
				}
				if (fInaccurate) {
					final IResource resource= match.getResource();
					if (match.getAccuracy() == SearchMatch.A_INACCURATE) {
						if (!fInaccurateMatches.contains(match)) {
							fStatus.addEntry(fSeverity, RefactoringCoreMessages.getFormattedString("RefactoringSearchEngine.inaccurate.match", resource.getName()), null, null, RefactoringStatusEntry.NO_CODE); //$NON-NLS-1$
							fInaccurateMatches.add(match);
						}
					}
				}
			}
		}

		public final void clearResults() {
			fCollectedMatches.clear();
			fInaccurateMatches.clear();
			fBinaryResources.clear();
		}

		public final Collection getBinaryResources() {
			return fBinaryResources;
		}

		public final Collection getCollectedMatches() {
			return fCollectedMatches;
		}

		public final Collection getInaccurateMatches() {
			return fInaccurateMatches;
		}
	}

	/** Should binary matches be filtered? */
	private boolean fBinary= false;

	/** The search match collector */
	private final RefactoringSearchCollector fCollector= new RefactoringSearchCollector();

	/** Should the matches be grouped by resource? */
	private boolean fGrouping= true;

	/** Should inaccurate matches be filtered? */
	private boolean fInaccurate= true;

	/** The working copy owner, or <code>null</code> */
	private WorkingCopyOwner fOwner= null;

	/** The search pattern, or <code>null</code> */
	private SearchPattern fPattern= null;

	/** The search requestor */
	private IRefactoringSearchRequestor fRequestor= new DefaultSearchRequestor();

	/** The search scope */
	private IJavaSearchScope fScope= SearchEngine.createWorkspaceScope();

	/** The severity */
	private int fSeverity= RefactoringStatus.OK;

	/** The search status */
	private RefactoringStatus fStatus= new RefactoringStatus();

	/**
	 * Creates a new refactoring search engine.
	 */
	public RefactoringSearchEngine2() {
		// Do nothing
	}

	/**
	 * Creates a new refactoring search engine.
	 * 
	 * @param pattern the search pattern
	 */
	public RefactoringSearchEngine2(final SearchPattern pattern) {
		Assert.isNotNull(pattern);
		fPattern= pattern;
	}

	/**
	 * Clears all results found so far, and sets resets the status to {@link RefactoringStatus#OK}.
	 */
	public final void clearResults() {
		fCollector.clearResults();
		fStatus= new RefactoringStatus();
	}

	/**
	 * Returns the found search matches in grouped by their containing resource.
	 * 
	 * @return the found search matches
	 */
	private SearchResultGroup[] getGroupedMatches() {
		final Map grouped= new HashMap();
		List matches= null;
		IResource resource= null;
		SearchMatch match= null;
		for (final Iterator iterator= getSearchMatches().iterator(); iterator.hasNext();) {
			match= (SearchMatch) iterator.next();
			resource= match.getResource();
			if (!grouped.containsKey(resource))
				grouped.put(resource, new ArrayList(4));
			matches= (List) grouped.get(resource);
			matches.add(match);
		}
		if (fBinary) {
			final Collection collection= fCollector.getBinaryResources();
			for (final Iterator iterator= grouped.keySet().iterator(); iterator.hasNext();) {
				resource= (IResource) iterator.next();
				if (collection.contains(resource))
					iterator.remove();
			}
		}
		final SearchResultGroup[] result= new SearchResultGroup[grouped.keySet().size()];
		int index= 0;
		for (final Iterator iterator= grouped.keySet().iterator(); iterator.hasNext();) {
			resource= (IResource) iterator.next();
			matches= (List) grouped.get(resource);
			result[index++]= new SearchResultGroup(resource, ((SearchMatch[]) matches.toArray(new SearchMatch[matches.size()])));
		}
		return result;
	}

	/**
	 * Returns the results of the previous search queries.
	 * <p>
	 * If grouping by resource is enabled, the results are elements of type {@link SearchResultGroup}, otherwise the elements are of type {@link SearchMatch}.
	 * 
	 * @return the results of the previous queries
	 */
	public final Object[] getResults() {
		if (fGrouping)
			return getGroupedMatches();
		else
			return getUngroupedMatches();
	}

	/**
	 * Returns the search matches filtered by their accuracy.
	 * 
	 * @return the filtered search matches
	 */
	private Collection getSearchMatches() {
		Collection results= null;
		if (fInaccurate) {
			results= new LinkedList(fCollector.getCollectedMatches());
			final Collection collection= fCollector.getInaccurateMatches();
			SearchMatch match= null;
			for (final Iterator iterator= results.iterator(); iterator.hasNext();) {
				match= (SearchMatch) iterator.next();
				if (collection.contains(match))
					iterator.remove();
			}
		} else
			results= fCollector.getCollectedMatches();
		return results;
	}

	/**
	 * Returns the refactoring status of this search engine.
	 * 
	 * @return the refactoring status
	 */
	public final RefactoringStatus getStatus() {
		return fStatus;
	}

	/**
	 * Returns the found search matches in no particular order.
	 * 
	 * @return the found search matches
	 */
	private SearchMatch[] getUngroupedMatches() {
		Collection results= null;
		if (fBinary) {
			results= new LinkedList(getSearchMatches());
			final Collection collection= fCollector.getBinaryResources();
			SearchMatch match= null;
			for (final Iterator iterator= results.iterator(); iterator.hasNext();) {
				match= (SearchMatch) iterator.next();
				if (collection.contains(match.getResource()))
					iterator.remove();
			}
		} else
			results= getSearchMatches();
		final SearchMatch[] matches= new SearchMatch[results.size()];
		results.toArray(matches);
		return matches;
	}

	/**
	 * Performs the search according to the specified pattern.
	 * 
	 * @param monitor the progress monitor, or <code>null</code>
	 * @throws JavaModelException if an error occurs during search
	 */
	public final void searchPattern(final IProgressMonitor monitor) throws JavaModelException {
		Assert.isNotNull(fPattern);
		try {
			SearchEngine engine= null;
			if (fOwner != null)
				engine= new SearchEngine(fOwner);
			else
				engine= new SearchEngine();
			engine.search(fPattern, SearchUtils.getDefaultSearchParticipants(), fScope, fCollector, monitor);
		} catch (CoreException exception) {
			throw new JavaModelException(exception);
		}
	}

	/**
	 * Performs the search of referenced fields.
	 * 
	 * @param element the java element whose referenced fields have to be found
	 * @param monitor the progress monitor, or <code>null</code>
	 * @throws JavaModelException if an error occurs during search
	 */
	public final void searchReferencedFields(final IJavaElement element, final IProgressMonitor monitor) throws JavaModelException {
		Assert.isNotNull(element);
		try {
			SearchEngine engine= null;
			if (fOwner != null)
				engine= new SearchEngine(fOwner);
			else
				engine= new SearchEngine();
			engine.searchDeclarationsOfAccessedFields(element, fCollector, monitor);
		} catch (CoreException exception) {
			throw new JavaModelException(exception);
		}
	}

	/**
	 * Performs the search of referenced methods.
	 * 
	 * @param element the java element whose referenced methods have to be found
	 * @param monitor the progress monitor, or <code>null</code>
	 * @throws JavaModelException if an error occurs during search
	 */
	public final void searchReferencedMethods(final IJavaElement element, final IProgressMonitor monitor) throws JavaModelException {
		Assert.isNotNull(element);
		try {
			SearchEngine engine= null;
			if (fOwner != null)
				engine= new SearchEngine(fOwner);
			else
				engine= new SearchEngine();
			engine.searchDeclarationsOfSentMessages(element, fCollector, monitor);
		} catch (CoreException exception) {
			throw new JavaModelException(exception);
		}
	}

	/**
	 * Performs the search of referenced types.
	 * 
	 * @param element the java element whose referenced types have to be found
	 * @param monitor the progress monitor, or <code>null</code>
	 * @throws JavaModelException if an error occurs during search
	 */
	public final void searchReferencedTypes(final IJavaElement element, final IProgressMonitor monitor) throws JavaModelException {
		Assert.isNotNull(element);
		try {
			SearchEngine engine= null;
			if (fOwner != null)
				engine= new SearchEngine(fOwner);
			else
				engine= new SearchEngine();
			engine.searchDeclarationsOfReferencedTypes(element, fCollector, monitor);
		} catch (CoreException exception) {
			throw new JavaModelException(exception);
		}
	}

	/**
	 * Determines how search matches are filtered.
	 * <p>
	 * This method must be called before start searching. The default is to filter inaccurate matches only.
	 * 
	 * @param inaccurate <code>true</code> to filter inaccurate matches, <code>false</code> otherwise
	 * @param binary <code>true</code> to filter binary matches, <code>false</code> otherwise
	 */
	public final void setFiltering(final boolean inaccurate, final boolean binary) {
		fInaccurate= inaccurate;
		fBinary= binary;
	}

	/**
	 * Determines how search matches are grouped.
	 * <p>
	 * This method must be called before start searching. The default is to group by containing resource.
	 * 
	 * @param grouping <code>true</code> to group matches by their containing resource, <code>false</code> otherwise
	 */
	public final void setGrouping(final boolean grouping) {
		fGrouping= grouping;
	}

	/**
	 * Sets the working copy owner to use during search.
	 * <p>
	 * This method must be called before start searching. The default is to use no working copy owner.
	 * 
	 * @param owner the working copy owner to use
	 */
	public final void setOwner(final WorkingCopyOwner owner) {
		Assert.isNotNull(owner);
		fOwner= owner;
	}

	/**
	 * Sets the search pattern.
	 * <p>
	 * This method must be called before {@link RefactoringSearchEngine2#searchPattern(IProgressMonitor)}
	 * 
	 * @param pattern the search pattern to set
	 */
	public final void setPattern(final SearchPattern pattern) {
		Assert.isNotNull(pattern);
		fPattern= pattern;
	}

	/**
	 * Sets the search requestor for this search engine.
	 * <p>
	 * This method must be called before start searching. The default is a non-filtering search requestor.
	 * 
	 * @param requestor the search requestor to set
	 */
	public final void setRequestor(final IRefactoringSearchRequestor requestor) {
		Assert.isNotNull(requestor);
		fRequestor= requestor;
	}

	/**
	 * Sets the search scope for this search engine.
	 * <p>
	 * This method must be called before start searching. The default is the entire workspace as search scope.
	 * 
	 * @param scope the search scope to set
	 */
	public final void setScope(final IJavaSearchScope scope) {
		Assert.isNotNull(scope);
		fScope= scope;
	}

	/**
	 * Sets the severity of the generated status entries.
	 * <p>
	 * This method must be called before start searching. The default is a severity of {@link RefactoringStatus#OK}.
	 * 
	 * @param severity the severity to set
	 */
	public void setSeverity(final int severity) {
		Assert.isTrue(severity == RefactoringStatus.OK || severity == RefactoringStatus.WARNING || severity == RefactoringStatus.INFO || severity == RefactoringStatus.FATAL || severity == RefactoringStatus.ERROR);
		fSeverity= severity;
	}

	/**
	 * Sets the refactoring status for this search engine.
	 * <p>
	 * This method must be called before start searching. The default is an empty status with status {@link RefactoringStatus#OK}.
	 * 
	 * @param status the refactoring status to set
	 */
	public final void setStatus(final RefactoringStatus status) {
		Assert.isNotNull(status);
		fStatus= status;
	}
}
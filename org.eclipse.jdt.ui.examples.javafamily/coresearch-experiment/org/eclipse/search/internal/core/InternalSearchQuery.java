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
package org.eclipse.search.internal.core;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.search.core.*;

/**
 * TODO add spec
 */
public abstract class InternalSearchQuery {

	public static SearchQuery createAndQuery(SearchQuery[] queries) {
		return new AndQuery(queries);
	}
	
	public static SearchQuery createOrQuery(SearchQuery[] queries) {
		return new OrQuery(queries);
	}
	
	/**
	 * Query a given index for matching entries. 
	 */
	public void findIndexMatches(Index index, SearchContext scope, IndexQueryRequestor requestor, IProgressMonitor monitor) throws IOException {
	
		if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();
		
		/* narrow down a set of entries using prefix criteria */
		char[][] categories = getIndexCategories();
		if (categories == null) return;
		char[] key = getIndexKey();
		if (key == null) return;
		int matchRule = getMatchRule();
		index.query(categories, key, matchRule, requestor, monitor);
	}
	
	/**
	 * Searches for matches to a given query. Search queries can be created using helper
	 * methods (from a String pattern or a Java element) and encapsulate the description of what is
	 * being searched (for example, search method declarations in a case sensitive way).
	 *
	 * @param scope the search result has to be limited to the given scope
	 * @param resultCollector a callback object to which each match is reported
	 */
	public void findMatches(SearchContext scope, SearchQueryRequestor requestor, IProgressMonitor monitor) throws CoreException {
		
		if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();
		
		/* initialize progress monitor */
		if (monitor != null) {
			monitor.beginTask(Util.bind("engine.searching"), 100); //$NON-NLS-1$
		}

		if (SearchCore.VERBOSE) {
			System.out.println("Searching for " + this + " in " + scope); //$NON-NLS-1$//$NON-NLS-2$
		}
	
		IndexManager manager = IndexManager.getIndexManager();
		try {
			requestor.beginReporting();
			SearchParticipant[] participants = scope.getParticipants();
			
			for (int iParticipant = 0, length = participants == null ? 0 : participants.length; iParticipant < length; iParticipant++) {
				
				if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();
	
				SearchParticipant participant = participants[iParticipant];
				try {
					requestor.enterParticipant(participant);
		
					// find index matches			
					PathCollector pathCollector = new PathCollector(scope);
					manager.jobManager.performConcurrentJob(
						new SearchQueryJob(
							(SearchQuery)this, 
							scope, 
							participant,
							pathCollector),
						JobManager.WAIT_UNTIL_READY,
						monitor);
					if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();
		
					// locate index matches if any
					IPath[] indexMatchPaths = pathCollector.getPaths();
					pathCollector = null; // release
					int indexMatchLength = indexMatchPaths == null ? 0 : indexMatchPaths.length;
					if (indexMatchLength > 0) {
						Document[] indexMatches = new Document[indexMatchLength];
						for (int iMatch = 0;iMatch < indexMatchLength; iMatch++) {
							indexMatches[iMatch] = participant.getDocument(indexMatchPaths[iMatch]);
						}
						MatchLocator matchLocator = participant.getMatchLocator();
						matchLocator.locateMatches(indexMatches, (SearchQuery)this, scope, requestor, monitor);
					}
				} finally {		
					requestor.exitParticipant(participant);
				}

				if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();
			}
		} finally {
			requestor.endReporting();
			if (monitor != null) monitor.done();
		}
	}			

	/**
	 * Returns an array of index categories to consider for this index query.
	 * These potential matches will be further narrowed by the match locator, but precise
	 * match locating can be expensive, and index query should be as accurate as possible
	 * so as to eliminate obvious false hits.
	 */
	public abstract char[][] getIndexCategories();

	/**
	 * Returns a key to find in relevant index categories. The key will be matched according to some match rule.
	 * These potential matches will be further narrowed by the match locator, but precise
	 * match locating can be expensive, and index query should be as accurate as possible
	 * so as to eliminate obvious false hits.
	 */
	public abstract char[] getIndexKey();

	/**
	 * Returns the rule to apply for matching index keys. Can be exact match, prefix match, pattern match or regexp match.
	 * Rule can also be combined with a case sensitivity flag.
	 */	
	public abstract int getMatchRule();
	
}

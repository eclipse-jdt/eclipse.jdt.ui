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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.search.core.SearchQuery;
import org.eclipse.search.core.SearchContext;

/**
 * TODO add spec
 */
public class AndQuery extends SearchQuery {
	
	SearchQuery[] subQueries;
	
	public AndQuery(SearchQuery[] subQueries) {
		super(0);
		this.subQueries = subQueries;
	}
	/*
	 * @see org.eclipse.search.core.SearchQuery#getIndexCategories()
	 */
	public char[][] getIndexCategories() {
		// no index category per se
		return null;
	}
	/* 
	 * @see org.eclipse.search.core.SearchQuery#getIndexKey()
	 */
	public char[] getIndexKey() {
		// no index key per se
		return null;
	}	
	
	/**
	 * Query a given index for matching entries. 
	 */
	public void findIndexMatches(Index index, SearchContext scope, final IndexQueryRequestor requestor, IProgressMonitor monitor) throws IOException {
	
		if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();
		
		// only record new document references
		class AndCombiner extends IndexQueryRequestor {
			int combination = 0;
			HashMap acceptedAnswers = new HashMap(5);
			public void acceptIndexMatch(char[] category, char[] key, IPath documentPath) {
				if (combination == 0) {
					acceptedAnswers.put(documentPath, subQueries[combination]); // use query as marker
				} else {
					if (acceptedAnswers.get(documentPath) == subQueries[combination-1]) { // previous query found it?
						acceptedAnswers.put(documentPath, subQueries[combination]); // update marker
					} else {
						acceptedAnswers.remove(documentPath);
					}
				}
			}
			public void acceptCombinedIndexMatches() {
				Iterator iter = acceptedAnswers.entrySet().iterator();
				SearchQuery lastQuery = subQueries[subQueries.length-1];
				while (iter.hasNext()) {
					Map.Entry entry = (Map.Entry) iter.next();
					if (entry.getValue() == lastQuery) {
						requestor.acceptIndexMatch(null, null, (IPath)entry.getKey()); // did not record category/key since called with path collector
					}
				}
			}
		}
		AndCombiner combiner = new AndCombiner();
		for (int i = 0, length = this.subQueries.length; i < length; i++) {
			combiner.combination = i;
			this.subQueries[i].findIndexMatches(index, scope, combiner, monitor);
		}
		combiner.acceptCombinedIndexMatches();
	}

}

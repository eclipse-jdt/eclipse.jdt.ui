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
import java.util.HashSet;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.search.core.SearchQuery;
import org.eclipse.search.core.SearchContext;

/**
 * TODO add spec
 */
public class OrQuery extends SearchQuery {
	
	SearchQuery[] subQueries;
	
	public OrQuery(SearchQuery[] subQueries) {
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
	 * Per constructor may only be called with a PathCollector. If not, then combining mode must be revised
	 */
	public void findIndexMatches(Index index, SearchContext scope, final IndexQueryRequestor requestor, IProgressMonitor monitor) throws IOException {
	
		if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();

		// since called with a path collector which already filters duplicate, can directly feed the same requestor
		for (int i = 0, length = this.subQueries.length; i < length; i++) {
			this.subQueries[i].findIndexMatches(index, scope, requestor, monitor);
		}
	}

}

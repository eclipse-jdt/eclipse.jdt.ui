/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.indexsearch;

import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.internal.core.index.IIndex;
import org.eclipse.jdt.internal.core.search.PathCollector;

/**
 * An IndeyQuery is used to perform a query against the indexing framework.
 */
public interface IIndexQuery {

	/**
	 * Compute the list of paths which are keying index files and add them to the given list.
	 */
	void computePathsKeyingIndexFiles(ArrayList requiredIndexKeys);
	
	/**
	 * Perform the query on the given index and adds the paths of all found documents to the given collector.
	 */
	void findIndexMatches(IIndex index, PathCollector collector, IProgressMonitor progressMonitor) throws IOException;

	/**
	 * Locate all matches of this query in the given file candidate and return them via the resultcollector. 
	 */
	void locateMatches(IFile candidate, ISearchResultCollector resultCollector);
}

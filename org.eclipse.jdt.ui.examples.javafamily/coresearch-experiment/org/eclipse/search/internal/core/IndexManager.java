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
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.search.core.SearchParticipant;

/**
 * TODO add spec
 * 
 * Manages all accesses to index infrastructure. In particular, it is responsible for using locks when reading or writing
 * index files.
 */
public class IndexManager {

	private static IndexManager INDEX_MANAGER = new IndexManager();

	private Map readWriteLocks = new HashMap(10); // index         --> lock
	public JobManager jobManager = new JobManager();
	
	
/**
 * Returns the index for a given project, according to the following algorithm:
 * - if index is already in memory: answers this one back
 * - if (reuseExistingFile) then read it and return this index and record it in memory
 * - if (createIfMissing) then create a new empty index and record it in memory
 */
public synchronized Index getIndex(IPath indexPath, boolean reuseExistingFile, boolean createIfMissing) {
	return null;
}


	public static IndexManager getIndexManager() {
		return INDEX_MANAGER;
	}
	
	public ReadWriteLock getLock(Index index) {
		ReadWriteLock lock = (ReadWriteLock) this.readWriteLocks.get(index);
		return lock;
	}

	public void indexAddDocument(SearchParticipant participant, IPath documentPath, Object indexJobFamily, IProgressMonitor monitor) {
		// TODO ensure old entries are removed first
		jobManager.request(new IndexAddition(participant, documentPath, indexJobFamily, monitor));
	}
	
	public void indexAddFolder(SearchParticipant participant, IPath documentPath, Object indexJobFamily, IProgressMonitor monitor) {
	}

	public void indexRemoveDocument(SearchParticipant participant, IPath documentPath, Object indexJobFamily, IProgressMonitor monitor) {
//		jobManager.request(new IndexRemoval(participant, documentPath, indexJobFamily, monitor));
	}

	public void indexRemoveFolder(SearchParticipant participant, IPath documentPath, Object indexJobFamily, IProgressMonitor monitor) {
	}
	
	/**
	 * Functionality for removing an index entirely
	 */
	public void removeIndex(IPath indexPath) throws IOException {
	}	

	/**
	 * Functionality for removing an index entirely
	 */
	public void saveIndex(Index index) throws IOException {
	}	
}

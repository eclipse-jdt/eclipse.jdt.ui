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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.search.core.SearchParticipant;
import org.eclipse.search.core.SearchQuery;
import org.eclipse.search.core.SearchContext;

/**
 * TODO add spec
 */
public class SearchQueryJob extends Job {

	SearchQuery query;
	SearchContext scope;
	SearchParticipant participant;
	IndexQueryRequestor requestor;
	IPath[] indexPaths;
	private Index[] selectedIndexes; // cache positionned only if all selected indexes are available
	long executionTime = 0;
		
	public SearchQueryJob(SearchQuery query, SearchContext scope, SearchParticipant participant, IndexQueryRequestor requestor) {
		this.query = query;
		this.scope = scope;
		this.participant = participant;
		this.requestor = requestor;
		this.indexPaths = this.participant.selectIndexes(query, scope);

	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.search.internal.core.index.Job#belongsTo(java.lang.Object)
	 */
	public boolean belongsTo(Object jobFamily) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.internal.core.index.Job#cancel()
	 */
	public void cancel() {
		// TODO Auto-generated method stub

	}

	/**
	 * Can be called more than once, first to initiate inconsistent index rebuild actions, or later on to collect the indexes to query
	 * Will only cache selected indexes if all of them are available
	 */
	public Index[] getSelectedIndexes() {

		if (this.selectedIndexes != null) return this.selectedIndexes;
		
		IndexManager manager = IndexManager.getIndexManager();
		int length = this.indexPaths.length;
		Index[] availableIndexes = new Index[length];
		int count = 0;
		for (int i = 0; i < length; i++) {
			// may trigger some index recreation work
			Index index = manager.getIndex(this.indexPaths[i], true /*reuse index file*/, false /*do not create if none*/);
			if (index != null) {
				availableIndexes[count++] = index;
			}
		}
		if (count != length) {
			System.arraycopy(availableIndexes, 0, availableIndexes = new Index[count], 0, count);
		} else {
			this.selectedIndexes = availableIndexes;
		}
		return availableIndexes;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.search.internal.core.index.Job#isReadyToRun()
	 */
	public boolean isReadyToRun() {
		getSelectedIndexes();
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.internal.core.index.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public boolean run(IProgressMonitor monitor) {

		if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();
		boolean isComplete = true;
		this.executionTime = 0;
		Index[] availableIndexes = getSelectedIndexes();
		try {
			int max = selectedIndexes.length;
			if (monitor != null) monitor.beginTask("", max); //$NON-NLS-1$

			for (int i = 0; i < max; i++) {
				isComplete &= query(availableIndexes[i], monitor);
				if (monitor != null) {
					if (monitor.isCanceled()) {
						throw new OperationCanceledException();
					} else {
						monitor.worked(1);
					}
				}
			}
			if (JobManager.VERBOSE) {
				JobManager.verbose("-> execution time: " + executionTime + "ms - " + this);//$NON-NLS-1$//$NON-NLS-2$
			}
			return isComplete;
		} finally {
			if (monitor != null) {
				monitor.done();
			}
		}
	}
		
	public boolean query(Index index, IProgressMonitor monitor) {

		if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();
		IndexManager manager = IndexManager.getIndexManager();
		ReadWriteLock lock = manager.getLock(index);
		if (lock == null) return true; // index got deleted since acquired
		try {
			lock.enterRead(); // ask permission to read

			/* if index has changed, commit these before querying */
			if (index.hasUnsavedChanges()) {
				try {
					lock.exitRead(); // free read lock
					lock.enterWrite(); // ask permission to write
					manager.saveIndex(index);
				} catch (IOException e) {
					return false;
				} finally {
					lock.exitWriteEnterRead(); // finished writing and reacquire read permission
				}
			}
			long start = System.currentTimeMillis();
			query.findIndexMatches(
				index,
				this.scope,
				this.requestor,
				monitor);
			executionTime += System.currentTimeMillis() - start;
			return true;
		} catch (IOException e) {
			return false;
		} finally {
			lock.exitRead(); // finished reading
		}
	}
		
}

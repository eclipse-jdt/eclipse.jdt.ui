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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.internal.core.index.IIndex;
import org.eclipse.jdt.internal.core.search.PathCollector;
import org.eclipse.jdt.internal.core.search.indexing.IndexManager;
import org.eclipse.jdt.internal.core.search.indexing.ReadWriteMonitor;
import org.eclipse.jdt.internal.core.search.processing.IJob;


class SearchJob implements IJob {
	
	private IndexManager fIndexManager;
	private IIndexQuery fSearch;
	private PathCollector fPathCollector;
	private IPath[] fIndexKeys; // cache of the keys for looking index up
	

	public SearchJob(IndexManager indexManager, IIndexQuery search, PathCollector pathCollector) {
		fIndexManager= indexManager;
		fSearch= search;
		fPathCollector= pathCollector;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.core.search.processing.IJob#belongsTo(java.lang.String)
	 */
	public boolean belongsTo(String jobFamily) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.core.search.processing.IJob#cancel()
	 */
	public void cancel() {
		// intentionally left blank
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.core.search.processing.IJob#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public boolean execute(IProgressMonitor progressMonitor) {

		if (progressMonitor != null && progressMonitor.isCanceled())
			throw new OperationCanceledException();
		boolean isComplete= COMPLETE;
		IIndex[] searchIndexes = getIndexes();
		try {
			int max= searchIndexes.length;
			if (progressMonitor != null) {
				progressMonitor.beginTask("", max); //$NON-NLS-1$
			}
			for (int i = 0; i < max; i++) {
				isComplete &= search(searchIndexes[i], progressMonitor);
				if (progressMonitor != null) {
					if (progressMonitor.isCanceled()) {
						throw new OperationCanceledException();
					} else {
						progressMonitor.worked(1);
					}
				}
			}
			return isComplete;
		} finally {
			if (progressMonitor != null) {
				progressMonitor.done();
			}
		}
	}

	public IIndex[] getIndexes() {
		if (fIndexKeys == null) {
			ArrayList requiredIndexKeys= new ArrayList();
			fSearch.computePathsKeyingIndexFiles(requiredIndexKeys);
			fIndexKeys= new IPath[requiredIndexKeys.size()];
			requiredIndexKeys.toArray(fIndexKeys);
		}
		// acquire the in-memory indexes on the fly
		int length = fIndexKeys.length;
		IIndex[] indexes = new IIndex[length];
		int count = 0;
		for (int i = 0; i < length; i++){
			// may trigger some index recreation work
			IIndex index = fIndexManager.getIndex(fIndexKeys[i], true /*reuse index file*/, false | true /*do not create if none*/);
			if (index != null) indexes[count++] = index; // only consider indexes which are ready yet
		}
		if (count != length) {
			System.arraycopy(indexes, 0, indexes=new IIndex[count], 0, count);
		}
		return indexes;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.core.search.processing.IJob#isReadyToRun()
	 */
	public boolean isReadyToRun() {
		return true;
	}

	public boolean search(IIndex index, IProgressMonitor progressMonitor) {

		if (progressMonitor != null && progressMonitor.isCanceled())
			throw new OperationCanceledException();

		if (index == null)
			return COMPLETE;
		ReadWriteMonitor monitor= fIndexManager.getMonitorFor(index);
		if (monitor == null)
			return COMPLETE; // index got deleted since acquired
		try {
			monitor.enterRead(); // ask permission to read

			/* if index has changed, commit these before querying */
			if (index.hasChanged()) {
				try {
					monitor.exitRead(); // free read lock
					monitor.enterWrite(); // ask permission to write
					fIndexManager.saveIndex(index);
				} catch (IOException e) {
					return FAILED;
				} finally {
					monitor.exitWriteEnterRead(); // finished writing and reacquire read permission
				}
			}
			fSearch.findIndexMatches(index, fPathCollector, progressMonitor);
			return COMPLETE;
		} catch (IOException e) {
			return FAILED;
		} finally {
			monitor.exitRead(); // finished reading
		}
	}
}

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
import org.eclipse.search.core.Document;
import org.eclipse.search.core.Indexer;
import org.eclipse.search.core.SearchParticipant;

/**
 * TODO add spec
 */
public class IndexAddition extends Job {

		SearchParticipant participant;
		IPath documentPath;
		Object indexingJobFamily;
		IProgressMonitor monitor;
				
		public IndexAddition(SearchParticipant participant, IPath documentPath, Object indexingJobFamily, IProgressMonitor monitor) {

			this.participant = participant;
			this.documentPath = documentPath;
			this.indexingJobFamily = indexingJobFamily;
			this.monitor = monitor;
		}
		public boolean belongsTo(Object jobFamily) {
			return this.indexingJobFamily.equals(jobFamily);
		}
		public void cancel() {
//			IndexManager indexManager = IndexManager.getIndexManager();
//			indexManager.indexingCancelled(indexManager.getIndex(participant, documentPath));
//			super.cancel();
		}
		public boolean isReadyToRun() {
			return false;
		}
		public boolean run(IProgressMonitor batchMonitor) { // in background no progress
			
			if (this.monitor != null && this.monitor.isCanceled()) return false;

			IPath indexPath = this.participant.getIndex(documentPath);
			IndexManager indexManager = IndexManager.getIndexManager();
			Index index = indexManager.getIndex(indexPath,  true /*reuse index file*/, true /*create if none*/);
			if (index == null) return false;

			ReadWriteLock indexLock = indexManager.getLock(index);
			if (indexLock == null) return true; // index got deleted since acquired

			Indexer indexer = this.participant.getIndexer(this.documentPath);
			if (indexer == null) return true;

			Document document = this.participant.getDocument(this.documentPath);
			if (document == null) return true;
			
			try {
				indexLock.enterWrite();  // ask permission to write the index
				indexer.index(document, index);
			} catch(IOException e) {
				if (JobManager.VERBOSE) {
					JobManager.verbose("-> participant: "+ this.participant.getName()+" failed to index " + this.documentPath + " because of the following exception:"); //$NON-NLS-1$ //$NON-NLS-2$
					e.printStackTrace();
				}
				return false;
			} finally {
				indexLock.exitWrite();  // finished writing the index
			}
			return true;
		}

}

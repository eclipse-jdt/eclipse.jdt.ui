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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.search.PathCollector;
import org.eclipse.jdt.internal.core.search.indexing.IndexManager;
import org.eclipse.jdt.internal.core.search.processing.IJob;
import org.eclipse.jsp.copied_from_jdtcore.AddFileToIndex;

/**
 * A <code>SearchEngine</code> searches for java elements following a search pattern.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 */
public class SearchEngine {
	
	/* Waiting policies */
	/**
	 * The search operation throws an <code>org.eclipse.core.runtime.OperationCanceledException</code>
	 * if the underlying indexer has not finished indexing the workspace.
	 */
	public static int CANCEL_IF_NOT_READY_TO_SEARCH = IJob.CancelIfNotReady;
	/**
	 * The search operation waits for the underlying indexer to finish indexing 
	 * the workspace before starting the search.
	 */
	public static int WAIT_UNTIL_READY_TO_SEARCH = IJob.WaitUntilReady;

	
	private static SearchEngine fgSearchEngine;
	
	private IndexManager fIndexManager;
	
	private SearchEngine() {
		JavaModelManager modelManager= JavaModelManager.getJavaModelManager();
		fIndexManager= modelManager.getIndexManager();		
	}
	
	/**
	 * 
	 * @return
	 */
	public static SearchEngine getSearchEngine() {
		if (fgSearchEngine == null)
			fgSearchEngine= new SearchEngine();
		return fgSearchEngine;
	}
	
	public IndexManager getIndexManager() {
		return fIndexManager;		
	}
	
	/**
	 * Trigger removal of a resource to an index
	 * Note: the actual operation is performed in background
	 */
	public void remove(String resourceName, IPath indexedContainer) {
		fIndexManager.remove(resourceName, indexedContainer);
	}
	
	public void add(AddFileToIndex job) {
		/* TODO_SEARCH
		if (fIndexManager.awaitingJobsCount() < IndexManager.MAX_FILES_IN_MEMORY) {
			// reduces the chance that the file is open later on, preventing it from being deleted
			if (!job.initializeContents())
				return;
		}
		*/
		fIndexManager.request(job);
	}

	/**
	 * Perform the given query against the index and return results via the resultCollector.
	 */
	public void search(IIndexQuery search, ISearchResultCollector resultCollector,
						IProgressMonitor progressMonitor, int waitingPolicy) {
				
		PathCollector pathCollector= new PathCollector();
		IJob job= new SearchJob(fIndexManager, search, pathCollector);
		IProgressMonitor pm= progressMonitor == null ? null : new SubProgressMonitor(progressMonitor, 5);
		fIndexManager.performConcurrentJob(job, waitingPolicy, pm);
		
		/* TODO_SEARCH
		IFile[] files= pathCollector.getFiles(ResourcesPlugin.getWorkspace());
		for (int i= 0; i < files.length; i++) {
			IFile file= files[i];
			search.locateMatches(file, resultCollector);
		}
		*/
	}
}

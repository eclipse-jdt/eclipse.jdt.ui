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
package org.eclipse.jsp.copied_from_jdtcore;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.internal.core.index.IIndex;
import org.eclipse.jdt.internal.core.search.indexing.IndexManager;
import org.eclipse.jdt.internal.core.search.indexing.IndexRequest;
import org.eclipse.jdt.internal.core.search.indexing.ReadWriteMonitor;
import org.eclipse.jdt.internal.core.search.processing.JobManager;

public abstract class AddFileToIndex extends IndexRequest {
	
	IFile fResource;

	public AddFileToIndex(IFile resource, IPath indexPath0, IndexManager manager0) {
		super(indexPath0, manager0);
		fResource = resource;
	}
	
	public IFile getResource() {
		return fResource;
	}
	
	public boolean execute(IProgressMonitor progressMonitor) {

		if (progressMonitor != null && progressMonitor.isCanceled())
			return true;

		/* ensure no concurrent write access to index */
		/* TODO_SEARCH */
		//IIndex index = manager.getIndex(this.indexPath, true, /*reuse index file*/ true /*create if none*/);
		IIndex index= null;
		/* TODO_SEARCH */
		if (index == null) return true;
		ReadWriteMonitor monitor = manager.getMonitorFor(index);
		if (monitor == null) return true; // index got deleted since acquired

		try {
			monitor.enterWrite(); // ask permission to write
			if (!indexDocument(index)) return false;
		} catch (IOException e) {
			if (JobManager.VERBOSE) {
				JobManager.verbose("-> failed to index " + fResource + " because of the following exception:"); //$NON-NLS-1$ //$NON-NLS-2$
				e.printStackTrace();
			}
			return false;
		} finally {
			monitor.exitWrite(); // free write lock
		}
		return true;
	}
	
	protected abstract boolean indexDocument(IIndex index) throws IOException;
	
	public abstract boolean initializeContents();
	
	public String toString() {
		return "indexing " + fResource.getFullPath(); //$NON-NLS-1$
	}
}

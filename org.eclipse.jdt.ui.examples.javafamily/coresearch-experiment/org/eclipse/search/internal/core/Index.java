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

/**
 * TODO add spec
 */
/**
 * Indexes are not thread safe. It is assumed that client code will protect concurrent accesses to it.
 * The search facility will ensure index updates are done inside a write lock, while queries are performed 
 * inside a read lock. Multiple concurrent readers are allowed.
 * @see IndexManager.getIndexLock(Index)
 */
public abstract class Index {

	public Index(IPath indexFileLocation, boolean reuseExistingFile) throws IOException {
	}
		
	/**
	 * Adds the given document to the index.
	 */
	public abstract void addEntry(char[] category, char[] key, IPath documentPath) throws IOException;

	/**
	 * Returns the index file location on the disk.
	 */
	public abstract IPath getLocation();
	
	/**
	 * Answers true if has some changes to save.
	 */
	public abstract boolean hasUnsavedChanges();

	/**
	 * Returns the paths of the documents containing the given word - query a group of categories
	 * matchRules are defined on SearchQuery
	 */
	public abstract void query(char[][] categories, char[] key, int matchRule, IndexQueryRequestor requestor, IProgressMonitor monitor) throws IOException;

	/**
	/**
	 * Removes the corresponding document from the index.
	 */
	public abstract void remove(String documentPath) throws IOException;

	/**
	 * Saves the index on the disk.
	 */
	public abstract void save() throws IOException;
}

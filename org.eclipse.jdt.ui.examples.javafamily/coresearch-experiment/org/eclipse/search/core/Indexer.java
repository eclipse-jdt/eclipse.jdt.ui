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
package org.eclipse.search.core;

import java.io.IOException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.search.internal.core.IndexManager;
import org.eclipse.search.internal.core.InternalIndexer;

/**
 * An indexer is implicity tied to an index, and provides functionality to add entries to this index.
 * The bound index is corresponding to the client participant implementation of SearchParticipant#getIndex(IPath documentPath).
 * Client implementations should specify how to index a particular document.
 */
public abstract class Indexer extends InternalIndexer {
	
	/**
	 * Record an entry into a particular category of an index
	 */
	public final void addEntry(char[] category, char[] key, IPath documentPath) throws IOException {
		super.addEntry(category, key, documentPath);
	}
		
	/**
	 * Indexes the given document, adding the document name and the word references 
	 * to this document to the given <code>Index</code>.
	 */
	public abstract void index(Document document) throws IOException;
	
	/**
	 * Trigger an indexing action at earliest convenience (in background). When the corresponding action is performed, the
	 * participant will be asked for an indexer, which in turn is going to perform #index(Document).
	 */
	public static void addDocument(IPath documentPath, SearchParticipant participant, Object indexJobFamily, IProgressMonitor monitor) {
		IndexManager.getIndexManager().indexAddDocument(participant, documentPath, indexJobFamily, monitor);
	}
	
	public static void addFolder(IPath folderPath,SearchParticipant participant, Object indexJobFamily, IProgressMonitor monitor) {
		IndexManager.getIndexManager().indexAddFolder(participant, folderPath, indexJobFamily, monitor);
	}
	
	/**
	 * Functionality for removing an index entirely
	 */
	public static void removeIndex(IPath indexPath) throws IOException {
		IndexManager.getIndexManager().removeIndex(indexPath);
	}	

	public static void removeDocument(IPath documentPath,SearchParticipant participant, Object indexJobFamily, IProgressMonitor monitor) {
		IndexManager.getIndexManager().indexRemoveDocument(participant, documentPath, indexJobFamily, monitor);
	}

	public static void removeFolder(IPath folderPath,SearchParticipant participant, Object indexJobFamily, IProgressMonitor monitor) {
		IndexManager.getIndexManager().indexRemoveFolder(participant, folderPath, indexJobFamily, monitor);
	}
}

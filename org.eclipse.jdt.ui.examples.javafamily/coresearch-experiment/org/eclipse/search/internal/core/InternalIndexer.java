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
import org.eclipse.search.core.Document;

/**
 * TODO add spec
 */
public class InternalIndexer {
	
	private Index index;
	
	/**
	 * Indexes the given document, adding the document name and the word references 
	 * to this document to the given <code>IIndex</code>.The caller should use 
	 * <code>shouldIndex()</code> first to determine whether this indexer handles 
	 * the given type of file, and only call this method if so. 
	 */
	public void index(Document document, IPath indexPath) throws IOException {
		try {
			this.setTargetIndex(indexPath);
			index(document);
		} finally {
			this.setTargetIndex(null);
		}
	}

	/**
	 * Indexes the given document, adding the document name and the word references 
	 * to this document to the given <code>Index</code>.
	 */
	public void index(Document document) throws IOException {
		// API subclass redefines it as an abstract method
	}
	
	/**
	 * Record an entry into a particular category of an index
	 */
	public void addEntry(char[] category, char[] key, IPath documentPath) throws IOException {
		this.index.addEntry(category, key, documentPath);
	}	

	/**
	 * Allow to manually set the target index output (when combining various indexers)
	 */
	public IPath getTargetIndex() {
		return this.index.getLocation();
	}
	public void setTargetIndex(IPath indexPath) {
		if (indexPath == null) {
			this.index = null;
		} else {
			IndexManager indexManager = IndexManager.getIndexManager();
			this.index = indexManager.getIndex(indexPath,  true /*reuse index file*/, true /*create if none*/);		
		}
	}
}

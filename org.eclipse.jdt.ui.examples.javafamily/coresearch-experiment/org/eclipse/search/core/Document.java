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

import org.eclipse.core.runtime.IPath;

/**
 * TODO add spec
 */
public abstract class Document {
	
	/**
	 * Contents may be different from actual resource at corresponding document path,
	 * in case of preprocessing.
	 */
	public abstract byte[] getByteContents();

	/**
	 * Contents may be different from actual resource at corresponding document path,
	 * in case of preprocessing.
	 */
	public abstract char[] getCharContents();

	/**
	 * Returns the original source position of a given position in the document contents.
	 * Since contents may be different from actual resource at corresponding document path,
	 * (in case of preprocessing), then the position in the (virtual) document contents will need to
	 * be remapped back for the original resource contents.
	 */
	public int getOriginalPosition(int sourcePosition) {
		return sourcePosition; // by default, assume no mapping
	}

	/**
	 * Path to the original document to publicly mention in index or search results.
	 */	
	public abstract IPath getPath();
}

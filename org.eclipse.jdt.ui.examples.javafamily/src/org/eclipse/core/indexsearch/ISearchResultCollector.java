/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.indexsearch;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * A <code>ISearchResultCollector</code> collects search results from a <code>search</code>
 * query to a <code>SearchEngine</code>. Clients must implement this interface and pass
 * an instance to the <code>search(...)</code> methods.
 * <p>
 * The order of the results is unspecified. Clients must not rely on this order to display results, 
 * but they should sort these results.
 * <p>
 * Clients may implement this interface.
 * </p>
 *
 * @see SearchEngine#search
 */
public interface ISearchResultCollector {
	
	/**
	 * Accepts the given search result.
	 *
	 * @param resource the resource in which the match has been found
	 * @param start the start position of the match, -1 if it is unknown
	 * @param length the length of the match
	 * @exception CoreException if this collector had a problem accepting the search result
	 */
	public void accept(IResource resource, int start, int length) throws CoreException;
}

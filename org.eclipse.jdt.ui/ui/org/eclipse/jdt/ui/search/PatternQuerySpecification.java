/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.search;

import org.eclipse.jdt.core.search.IJavaSearchScope;


/**
 * <p>
 * Describes a search query by giving a textual pattern to search for.
 * </p>
 * <p>
 * This class isn't intended to be instantiated to subclassed by clients.
 * </p>
 * 
 * @see org.eclipse.jdt.ui.search.QuerySpecification
 * 
 * @since 3.0
 */
public class PatternQuerySpecification extends QuerySpecification {
	private String fPattern;
	private int fSearchFor;
	private boolean fCaseSensitive;

	/**
	 * @param pattern The string that the query should search for.
	 * @see org.eclipse.jdt.core.search.SearchEngine#createSearchPattern(String,
	 *         int, int, boolean) for a description of the possible search patterns
	 * @param searchFor What kind of <code>IJavaElement</code> the query
	 *                should search for. (@see
	 *                org.eclipse.jdt.core.search.IJavaSearchConstants#TYPE, for
	 *                example).
	 * @param caseSensitive Whether the query shoudl be case sensitive.
	 * @param limitTo The kind of occurrence the query should search for
	 * @param scope The scope to search in.
	 * @param scopeDescription A human readable description of the search
	 *                scope.
	 */
	public PatternQuerySpecification(String pattern, int searchFor, boolean caseSensitive, int limitTo, IJavaSearchScope scope, String scopeDescription) {
		super(limitTo, scope, scopeDescription);
		fPattern= pattern;
		fSearchFor= searchFor;
		fCaseSensitive= caseSensitive;
	}

	/**
	 * Whether the query should be case sensitive.
	 * @return Whether the query should be case sensitive.
	 */
	public boolean isCaseSensitive() {
		return fCaseSensitive;
	}

	/**
	 * Returns the search pattern the query should search for. 
	 * @return The search pattern
	 * @see org.eclipse.jdt.core.search.SearchEngine#createSearchPattern(String,
	 *         int, int, boolean) for a description of the possible search patterns
	 */
	public String getPattern() {
		return fPattern;
	}

	/**
	 * Returns what kind of <code>IJavaElement</code> the query should search for.
	 * 
	 * @return The kind of <code>IJavaElement</code> to search for.
	 * 
	 *@see org.eclipse.jdt.core.search.IJavaSearchConstants#TYPE, for example
	 */
	public int getSearchFor() {
		return fSearchFor;
	}
}

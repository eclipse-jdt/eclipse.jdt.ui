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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.search.internal.core.InternalSearchQuery;

/**
 * Pattern denotes one particular element which is to be searched for. It can 
 * support multiple flavors for matching (declaration, reference, both, ...).
 * TODO add spec
 */
public abstract class SearchQuery extends InternalSearchQuery {
	
	private final int matchRule;

	/**
	 * Rules for pattern matching: (exact, prefix, pattern) [ | case sensitive]
	 */
	/**
	 * Match rule: The search pattern matches exactly the search result,
	 * that is, the source of the search result equals the search pattern.
	 */
	public static final int R_EXACT_MATCH = 0;
	/**
	 * Match rule: The search pattern is a prefix of the search result.
	 */
	public static final int R_PREFIX_MATCH = 1;
	/**
	 * Match rule: The search pattern contains one or more wild cards ('*') where a 
	 * wild-card can replace 0 or more characters in the search result.
	 */
	public static final int R_PATTERN_MATCH = 2;
	/**
	 * Match rule: The search pattern contains a regular expression.
	 */
	public static final int R_REGEXP_MATCH = 3;
	/**
	 * Match rule: The search pattern matches the search result only if cases are the same.
	 * Can be combined to previous rules, e.g. R_EXACT_MATCH | R_CASE_SENSITIVE
	 */
	public static final int R_CASE_SENSITIVE = 4;

	public SearchQuery(int matchRule) {
		this.matchRule = matchRule;
	}

	public static final SearchQuery createAndQuery(SearchQuery[] queries) {
		return InternalSearchQuery.createAndQuery(queries);
	}
	
	public static final SearchQuery createOrQuery(SearchQuery[] queries) {
		return InternalSearchQuery.createOrQuery(queries);
	}

	/**
	 * Find matches to a given query. Search queries can be created using helper
	 * methods (from a String pattern or a Java element) and encapsulate the description of what is
	 * being searched (for example, search method declarations in a case sensitive way).
	 *
	 * @param scope the search result has to be limited to the given scope
	 * @param resultCollector a callback object to which each match is reported
	 */	
	public final void findMatches(SearchContext scope, SearchQueryRequestor requestor, IProgressMonitor monitor) throws CoreException {
		super.findMatches(scope, requestor, monitor);
	}	
	
	//TODO (philippe) should add API to only perform search in indexes --> search all type names
	
	/**
	 * Returns an array of index categories to consider for this index query.
	 * These potential matches will be further narrowed by the match locator, but precise
	 * match locating can be expensive, and index query should be as accurate as possible
	 * so as to eliminate obvious false hits.
	 */
	public abstract char[][] getIndexCategories();

	/**
	 * Returns a key to find in relevant index categories. The key will be matched according to some match rule.
	 * These potential matches will be further narrowed by the match locator, but precise
	 * match locating can be expensive, and index query should be as accurate as possible
	 * so as to eliminate obvious false hits.
	 */
	public abstract char[] getIndexKey();

	/**
	 * Returns the rule to apply for matching index keys. Can be exact match, prefix match, pattern match or regexp match.
	 * Rule can also be combined with a case sensitivity flag.
	 */	
	public int getMatchRule() {
		return this.matchRule;
	}


}

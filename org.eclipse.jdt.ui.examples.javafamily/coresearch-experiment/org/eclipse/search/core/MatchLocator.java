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

/**
 * responsible for feeding appropriate kind of matches to the search match requestor if answering well-known Java entities
 * TODO add spec
 */
public abstract class MatchLocator { 
	
	/**
	 * Locate the matches in the given documents and report them using the search requestor. 
	 * Note: allows to combine match locators (e.g. jsp match locator can preprocess jsp unit contents and feed it to Java match locator asking for virtual matches
	 * by contributing document implementations which do the conversion). It is assumed that virtual matches are rearranged by requestor for adapting line/source 
	 * positions before submitting final results so the provided searchRequestor should intercept virtual matches and do appropriate conversions.
	 */
	public abstract void locateMatches(Document[] indexMatches, SearchQuery query, SearchContext scope, SearchQueryRequestor requestor , IProgressMonitor monitor) throws CoreException;
}

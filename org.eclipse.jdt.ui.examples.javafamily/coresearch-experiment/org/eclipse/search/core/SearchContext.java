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

// TODO : should be renamed into SearchScope in the end (name collision issue) or merged with SearchScope?

/**
 * Scope defines extent of search action, both in term of portion of the workspace to consider and tools participating in the search
 * TODO: add spec
 */
public abstract class SearchContext {

	/**
	 * Invoked to eliminate some unnecessary index matches (e.g. if index granularity is bigger than actual search scope)
	 */
	public abstract boolean contains(IPath documentPath);
	
	public abstract String getDescription();
	
	/**
	 * Returns the set of participants to consider during this search action.
	 */
	public abstract SearchParticipant[] getParticipants();
}

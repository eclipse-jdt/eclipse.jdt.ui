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
import org.eclipse.search.internal.core.SearchCore;

/**
 * A search participant describes a particular extension to a generic search mechanism, allowing thus to 
 * perform combined search actions which will involve all required participants
 * 
 * A search scope defines which participants are involved. 
 * 
 * A search participant is responsible for holding index files, and selecting the appropriate ones to feed to
 * index queries. It also can map a document path to an actual document (note that documents could live outside
 * the workspace or no exist yet, and thus aren't just resources).
 */
public abstract class SearchParticipant {

	public static final SearchParticipant[] NO_PARTICIPANT = {};
	
	/**
	 * Returns a displayable name of this search participant. e.g. "Java".
	 */
	public abstract String getName();

	/**
	 * Bind a document path to an actual document. A document path is interpreted by a participant.
	 */
	public abstract Document getDocument(IPath documentPath);
	
	/**
	 * Returns the index path containing relevant information for a given document
	 * Each participant should record index files inside its own metadata area to avoid index collisions.
	 */
	public abstract IPath getIndex(IPath documentPath);

	/**
	 * Returns a instance of an indexer able to process a given document.
	 */
	public abstract Indexer getIndexer(IPath documentPath);

	/**
	 * Returns the collection of index paths to consider when performing a given search query in a given scope.
	 */
	public abstract IPath[] selectIndexes(SearchQuery query, SearchContext scope);

	/**
	 * Returns the match locator to use so as to narrow and localize index matches.
	 */
	public abstract MatchLocator getMatchLocator();

	/**
	 * Returns all registered search participants
	 */
	/**
	 * Helper method finding the classpath container initializer registered for a given classpath container ID 
	 * or <code>null</code> if none was found while iterating over the contributions to extension point to
	 * the extension point "org.eclipse.jdt.core.classpathContainerInitializer".
	 * <p>
	 * A containerID is the first segment of any container path, used to identify the registered container initializer.
	 * <p>
	 * @param String - a containerID identifying a registered initializer
	 * @return ClasspathContainerInitializer - the registered classpath container initializer or <code>null</code> if 
	 * none was found.
	 * @since 2.1
	 */
	public static SearchParticipant[] getRegisteredParticipants() {
		return SearchCore.getParticipants();
	}		
}

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

import org.eclipse.search.ui.text.Match;

/**
 * A callback interface to report matches against. This class serves as a bottleneck and minimal interface
 * to report matches to the java search infrastructure. Query participants will be passed an
 * instance of this interface when their <code>search(...)</code> method is called.
 * Clients must not implement this interface.
 */
public interface ISearchRequestor {
	/**
	 * Adds a match to the search that issued this particular <code>ISearchRequestor</code>.
	 * @param match The match to be reported.
	 */
	void reportMatch(Match match);
}

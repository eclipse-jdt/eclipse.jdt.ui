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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.search.core.SearchContext;

/**
 * Collects the resource paths reported by a client to this search requestor.
 */
public class PathCollector  extends IndexQueryRequestor {
	
	SearchContext scope;
	
	public PathCollector(SearchContext scope) {
		this.scope = scope;
	}
	
	/* a set of accumulated document paths */
	public Set paths = new HashSet(5);
	
	public void acceptIndexMatch(
				char[] category,
				char[] key,
				IPath documentPath) {
			
			if (scope.contains(documentPath)) {
				this.paths.add(documentPath);
			}
	}

	/**
	 * Returns the paths that have been collected.
	 */
	public IPath[] getPaths() {
		
		IPath[] result = new IPath[this.paths.size()];
		this.paths.toArray(result);
		return result;
	}
}

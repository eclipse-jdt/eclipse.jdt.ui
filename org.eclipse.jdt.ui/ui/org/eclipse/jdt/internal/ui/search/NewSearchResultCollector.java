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
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.search.ui.text.Match;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;

/**
 * @author Thomas Mäder
 *
 */
public class NewSearchResultCollector implements IJavaSearchResultCollector {
	private JavaSearchResult fSearch;
	private IProgressMonitor fProgressMonitor;
	private int fMatchCount;
	private boolean fIgnoreImports;

	public NewSearchResultCollector(JavaSearchResult search, IProgressMonitor monitor, boolean ignoreImports) {
		super();
		fSearch= search;
		fProgressMonitor= monitor;
		fIgnoreImports= ignoreImports;
	}

	public void aboutToStart() {
		// do nothing
	}

	public void accept(IResource resource, int start, int end, IJavaElement enclosingElement, int accuracy) {
		fMatchCount++;
		if (enclosingElement != null) {
			if (fIgnoreImports && enclosingElement.getElementType() == IJavaElement.IMPORT_DECLARATION)
				return;
			fSearch.addMatch(new Match(enclosingElement, start, end-start));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.search.IJavaSearchResultCollector#done()
	 */
	public void done() {
		//System.out.println("done search, "+fMatchCount+" matches");
	}

	public IProgressMonitor getProgressMonitor() {
		return fProgressMonitor;
	}
	


}

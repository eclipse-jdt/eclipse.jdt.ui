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
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchRequestor;

public class NewSearchResultCollector extends SearchRequestor {
	private JavaSearchResult fSearch;
	private boolean fIgnoreImports;
	private boolean fIgnorePotentials;

	public NewSearchResultCollector(JavaSearchResult search, boolean ignoreImports, boolean ignorePotentials) {
		super();
		fSearch= search;
		fIgnoreImports= ignoreImports;
		fIgnorePotentials= ignorePotentials;
	}
	
	public boolean acceptSearchMatch(SearchMatch match) throws CoreException {
		IJavaElement enclosingElement= (IJavaElement) match.getElement();
		if (enclosingElement != null) {
			if (fIgnoreImports && enclosingElement.getElementType() == IJavaElement.IMPORT_DECLARATION)
				return true;
			if (fIgnorePotentials && (match.getAccuracy() == SearchMatch.A_INACCURATE))
				return true;
			fSearch.addMatch(new JavaElementMatch(enclosingElement, match.getOffset(), match.getLength(), match.getAccuracy()));
		}
		return true;
	}

	public void beginReporting() {
	}

	public void endReporting() {
	}

	public void enterParticipant(SearchParticipant participant) {
	}

	public void exitParticipant(SearchParticipant participant) {
	}


}

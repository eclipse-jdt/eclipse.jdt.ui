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
import org.eclipse.jdt.core.search.FieldReferenceMatch;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchRequestor;

public class NewSearchResultCollector extends SearchRequestor {
	private JavaSearchResult fSearch;
	private boolean fIgnorePotentials;

	public NewSearchResultCollector(JavaSearchResult search, boolean ignorePotentials) {
		super();
		fSearch= search;
		fIgnorePotentials= ignorePotentials;
	}
	
	public void acceptSearchMatch(SearchMatch match) throws CoreException {
		IJavaElement enclosingElement= (IJavaElement) match.getElement();
		if (enclosingElement != null) {
			if (fIgnorePotentials && (match.getAccuracy() == SearchMatch.A_INACCURATE))
				return;
			boolean isWriteAccess= false;
			boolean isReadAccess= false;
			if (match instanceof FieldReferenceMatch) {
				FieldReferenceMatch fieldRef= ((FieldReferenceMatch)match);
				isWriteAccess= fieldRef.isWriteAccess();
				isReadAccess= fieldRef.isReadAccess();
			}
			fSearch.addMatch(new JavaElementMatch(enclosingElement, match.getOffset(), match.getLength(), match.getAccuracy(), isWriteAccess, isWriteAccess, match.isInsideDocComment()));
		}
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

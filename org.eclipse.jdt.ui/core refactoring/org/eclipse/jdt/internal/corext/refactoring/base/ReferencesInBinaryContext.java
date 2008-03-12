/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.base;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;

import org.eclipse.jdt.core.search.SearchMatch;

public class ReferencesInBinaryContext extends RefactoringStatusContext {

	private List/*<SearchMatch>*/fMatches= new ArrayList();

	public void add(SearchMatch match) {
		fMatches.add(match);
	}

	public List/*<SearchMatch>*/getMatches() {
		return fMatches;
	}
	
	/*
	 * @see org.eclipse.ltk.core.refactoring.RefactoringStatusContext#getCorrespondingElement()
	 */
	public Object getCorrespondingElement() {
		return null;
	}
	
}

/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.search.ui.NewSearchUI;

import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.ui.SharedASTProvider;


public final class FindOccurrencesEngine {

	public static FindOccurrencesEngine create(ITypeRoot root, IOccurrencesFinder finder) {
		if (root == null || finder == null)
			return null;
		return new FindOccurrencesEngine(root, finder);
	}
	
	private IOccurrencesFinder fFinder;
	private ITypeRoot fTypeRoot;
		
	private FindOccurrencesEngine(ITypeRoot typeRoot, IOccurrencesFinder finder) {
		fFinder= finder;
		fTypeRoot= typeRoot;
	}

	public CompilationUnit createAST() {
		return SharedASTProvider.getAST(fTypeRoot, SharedASTProvider.WAIT_YES, null);
	}
	
	public ITypeRoot getInput() {
		return fTypeRoot;
	}
	
	public IOccurrencesFinder getOccurrencesFinder() {
		return fFinder;
	}

	public String run(int offset, int length) throws JavaModelException {
		ISourceReference sr= getInput();
		if (sr.getSourceRange() == null) {
			return SearchMessages.FindOccurrencesEngine_noSource_text; 
		}
		
		final CompilationUnit root= createAST();
		if (root == null) {
			return SearchMessages.FindOccurrencesEngine_cannotParse_text; 
		}
		String message= fFinder.initialize(root, offset, length);
		if (message != null)
			return message;
		
		performNewSearch(fFinder, getInput());
		return null;
	}
	
	private void performNewSearch(IOccurrencesFinder finder, ITypeRoot element) {
		NewSearchUI.runQueryInBackground(new OccurrencesSearchQuery(finder, element));
	}
}

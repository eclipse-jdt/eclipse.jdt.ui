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

import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.text.IDocument;

import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.Match;

import org.eclipse.jdt.core.IJavaElement;


public class OccurrencesSearchQuery implements ISearchQuery {

	private OccurrencesSearchResult fResult;
	private IOccurrencesFinder fFinder;
	private IDocument fDocument;
	private IJavaElement fElement;
	private String fJobLabel;
	
	public OccurrencesSearchQuery(IOccurrencesFinder finder, IDocument document, IJavaElement element) {
		fFinder= finder;
		fDocument= document;
		fElement= element;
		fJobLabel= fFinder.getJobLabel();
	}

	/*
	 * @see org.eclipse.search.ui.ISearchQuery#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus run(IProgressMonitor monitor) {
		if (fFinder != null) {
			fFinder.perform();
			ArrayList resultingMatches= new ArrayList();
			fFinder.collectOccurrenceMatches(fElement, fDocument, resultingMatches);
			if (!resultingMatches.isEmpty()) {
				fResult.addMatches((Match[]) resultingMatches.toArray(new Match[resultingMatches.size()]));
			}
			//Don't leak AST:
			fFinder= null;
			fDocument= null;
			monitor.done();
		}
		return Status.OK_STATUS;
	}
	
	/*
	 * @see org.eclipse.search.ui.ISearchQuery#getLabel()
	 */
	public String getLabel() {
		return fJobLabel;
	}
	
	public String getResultLabel(int nMatches) {
		return fFinder.getResultLabel(fElement.getElementName(), nMatches);
	}
		
	/*
	 * @see org.eclipse.search.ui.ISearchQuery#canRerun()
	 */
	public boolean canRerun() {
		return false; //can't retain fFinder (would leak AST)
	}

	/*
	 * @see org.eclipse.search.ui.ISearchQuery#canRunInBackground()
	 */
	public boolean canRunInBackground() {
		return true;
	}

	/*
	 * @see org.eclipse.search.ui.ISearchQuery#getSearchResult()
	 */
	public ISearchResult getSearchResult() {
		if (fResult == null)
			fResult= new OccurrencesSearchResult(this);
		return fResult;
	}
}

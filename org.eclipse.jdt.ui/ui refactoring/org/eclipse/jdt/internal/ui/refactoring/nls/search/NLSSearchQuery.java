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

package org.eclipse.jdt.internal.ui.refactoring.nls.search;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;

import org.eclipse.jdt.internal.ui.JavaPlugin;


public class NLSSearchQuery implements ISearchQuery {
	private NLSSearchResult fResult;
	private IJavaElement fWrapperClass;
	private IFile fPropertiesFile;
	private IJavaSearchScope fScope;
	private String fScopeDescription;
	
	public NLSSearchQuery(IJavaElement wrapperClass, IFile propertiesFile, IJavaSearchScope scope, String scopeDescription) {
		fWrapperClass= wrapperClass;
		fPropertiesFile= propertiesFile;
		fScope= scope;
		fScopeDescription= scopeDescription;
	}
	
	IFile getPropertiesFile() {
		return fPropertiesFile;
	}
	
	/*
	 * @see org.eclipse.search.ui.ISearchQuery#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus run(IProgressMonitor monitor) {
		NLSSearchResultCollector2 collector= new NLSSearchResultCollector2(fPropertiesFile, monitor, fResult);
		SearchEngine engine= new SearchEngine();
		try {
			engine.search(JavaPlugin.getWorkspace(), fWrapperClass, IJavaSearchConstants.REFERENCES, fScope, collector);
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return 	Status.OK_STATUS;
	}

	/*
	 * @see org.eclipse.search.ui.ISearchQuery#getLabel()
	 */
	public String getLabel() {
		return NLSSearchMessages.getString("NLSSearchQuery.label"); //$NON-NLS-1$
	}

	String getSingularLabel() {
		String[] args= new String[] {fWrapperClass.getElementName(), fScopeDescription}; //$NON-NLS-1$		
		return NLSSearchMessages.getFormattedString("SearchOperation.singularLabelPostfix", args); //$NON-NLS-1$
	}

	String getPluralLabelPattern() {
		String[] args= new String[] {fWrapperClass.getElementName(), "{0}", fScopeDescription}; //$NON-NLS-1$		
		return NLSSearchMessages.getFormattedString("SearchOperation.pluralLabelPatternPostfix", args); //$NON-NLS-1$
	}
	
	/*
	 * @see org.eclipse.search.ui.ISearchQuery#canRerun()
	 */
	public boolean canRerun() {
		return false; //TODO: would have to re-check existence of files
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
			fResult= new NLSSearchResult(this);
		return fResult;
	}
}

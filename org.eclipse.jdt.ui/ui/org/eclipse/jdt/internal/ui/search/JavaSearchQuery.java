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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.WorkInProgressPreferencePage;

/**
 * @author Thomas Mäder
 *
 */
public class JavaSearchQuery implements ISearchQuery {
	private IJavaSearchScope fScope;
	private String fScopeDescription;
	private String fName;
	private int fLimitTo;
	private IJavaElement fElement;
	private String fPattern;
	private boolean fIsCaseSensitive;
	private int fSearchFor;
	
	private JavaSearchQuery(IJavaSearchScope scope, String scopeDescription) {
		fName= "Java Search Job";
		fScope= scope;
		fScopeDescription= scopeDescription;
	}

	public JavaSearchQuery(SearchPatternData data, IJavaSearchScope scope, String scopeDescription) {
			
		this(scope, scopeDescription);
		fLimitTo= data.getLimitTo();
		fSearchFor= data.getSearchFor();
		fElement= data.getJavaElement();
		fPattern= data.getPattern();
		fIsCaseSensitive= data.isCaseSensitive();
	}

	
	public JavaSearchQuery(
			int searchFor,
			int limitTo,
			String pattern,
			boolean isCaseSensitive,
			IJavaSearchScope scope, String scopeDescription) {
				
		this(scope, scopeDescription);
			fLimitTo= limitTo;
			fSearchFor= searchFor;
			fPattern= pattern;
			fIsCaseSensitive= isCaseSensitive;
		}

	public JavaSearchQuery(IJavaElement element, int limitTo, IJavaSearchScope scope, String scopeDescription) {
		this(scope, scopeDescription);
		fElement= element;
		fLimitTo= limitTo;
	}

	public IStatus run(IProgressMonitor monitor, ISearchResult result) {
		JavaSearchResult textResult= (JavaSearchResult) result;
		textResult.removeAll();
		// Also search working copies
		SearchEngine engine= new SearchEngine(JavaUI.getSharedWorkingCopiesOnClasspath());

		try {
			boolean ignoreImports= (fLimitTo == IJavaSearchConstants.REFERENCES);
			ignoreImports &= PreferenceConstants.getPreferenceStore().getBoolean(WorkInProgressPreferencePage.PREF_SEARCH_IGNORE_IMPORTS);
			NewSearchResultCollector collector= new NewSearchResultCollector(textResult, monitor, ignoreImports);
			if (fElement != null)
				engine.search(JavaPlugin.getWorkspace(), fElement, fLimitTo, fScope, collector);
			else
				engine.search(
					JavaPlugin.getWorkspace(),
					SearchEngine.createSearchPattern(fPattern, fSearchFor, fLimitTo, fIsCaseSensitive),
					fScope,
					collector);
			
		} catch (CoreException e) {
			return e.getStatus();
		}
		return new Status(IStatus.OK, JavaPlugin.getPluginId(), 0, "", null); //$NON-NLS-1$
	}


	public String getName() {
		return fName;
	}

	String getSingularLabel() {
		String desc= null;
		if (fElement != null) {
			if (fLimitTo == IJavaSearchConstants.REFERENCES
			&& fElement.getElementType() == IJavaElement.METHOD)
				desc= PrettySignature.getUnqualifiedMethodSignature((IMethod)fElement);
			else
				desc= fElement.getElementName();
			if ("".equals(desc) && fElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT) //$NON-NLS-1$
				desc= SearchMessages.getString("JavaSearchOperation.default_package"); //$NON-NLS-1$
		}
		else
			desc= fPattern;

		desc= "\""+desc+"\"";
		String[] args= new String[] {desc, fScopeDescription}; //$NON-NLS-1$
		switch (fLimitTo) {
			case IJavaSearchConstants.IMPLEMENTORS:
				return SearchMessages.getFormattedString("JavaSearchOperation.singularImplementorsPostfix", args); //$NON-NLS-1$
			case IJavaSearchConstants.DECLARATIONS:
				return SearchMessages.getFormattedString("JavaSearchOperation.singularDeclarationsPostfix", args); //$NON-NLS-1$
			case IJavaSearchConstants.REFERENCES:
				return SearchMessages.getFormattedString("JavaSearchOperation.singularReferencesPostfix", args); //$NON-NLS-1$
			case IJavaSearchConstants.ALL_OCCURRENCES:
				return SearchMessages.getFormattedString("JavaSearchOperation.singularOccurrencesPostfix", args); //$NON-NLS-1$
			case IJavaSearchConstants.READ_ACCESSES:
				return SearchMessages.getFormattedString("JavaSearchOperation.singularReadReferencesPostfix", args); //$NON-NLS-1$
			case IJavaSearchConstants.WRITE_ACCESSES:
				return SearchMessages.getFormattedString("JavaSearchOperation.singularWriteReferencesPostfix", args); //$NON-NLS-1$
			default:
				return SearchMessages.getFormattedString("JavaSearchOperation.singularOccurrencesPostfix", args); //$NON-NLS-1$;
		}
	}

	String getPluralLabelPattern() {
		String desc= null;
		if (fElement != null) {
			if (fLimitTo == IJavaSearchConstants.REFERENCES
			&& fElement.getElementType() == IJavaElement.METHOD)
				desc= PrettySignature.getUnqualifiedMethodSignature((IMethod)fElement);
			else
				desc= fElement.getElementName();
			if ("".equals(desc) && fElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT) //$NON-NLS-1$
				desc= SearchMessages.getString("JavaSearchOperation.default_package"); //$NON-NLS-1$
		}
		else
			desc= fPattern;

		desc= "\""+desc+"\"";
		String[] args= new String[] {desc, "{0}", fScopeDescription}; //$NON-NLS-1$
		switch (fLimitTo) {
			case IJavaSearchConstants.IMPLEMENTORS:
				return SearchMessages.getFormattedString("JavaSearchOperation.pluralImplementorsPostfix", args); //$NON-NLS-1$
			case IJavaSearchConstants.DECLARATIONS:
				return SearchMessages.getFormattedString("JavaSearchOperation.pluralDeclarationsPostfix", args); //$NON-NLS-1$
			case IJavaSearchConstants.REFERENCES:
				return SearchMessages.getFormattedString("JavaSearchOperation.pluralReferencesPostfix", args); //$NON-NLS-1$
			case IJavaSearchConstants.ALL_OCCURRENCES:
				return SearchMessages.getFormattedString("JavaSearchOperation.pluralOccurrencesPostfix", args); //$NON-NLS-1$
			case IJavaSearchConstants.READ_ACCESSES:
				return SearchMessages.getFormattedString("JavaSearchOperation.pluralReadReferencesPostfix", args); //$NON-NLS-1$
			case IJavaSearchConstants.WRITE_ACCESSES:
				return SearchMessages.getFormattedString("JavaSearchOperation.pluralWriteReferencesPostfix", args); //$NON-NLS-1$
			default:
				return SearchMessages.getFormattedString("JavaSearchOperation.pluralOccurrencesPostfix", args); //$NON-NLS-1$;
		}
	}

	ImageDescriptor getImageDescriptor() {
		if (fLimitTo == IJavaSearchConstants.IMPLEMENTORS || fLimitTo == IJavaSearchConstants.DECLARATIONS)
			return JavaPluginImages.DESC_OBJS_SEARCH_DECL;
		else
			return JavaPluginImages.DESC_OBJS_SEARCH_REF;
	}
}

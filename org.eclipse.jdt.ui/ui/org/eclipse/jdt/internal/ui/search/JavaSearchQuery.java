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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.Match;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.search.*;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.WorkInProgressPreferencePage;

/**
 * @author Thomas Mäder
 *
 */
public class JavaSearchQuery implements ISearchQuery {
	private ISearchResult fResult;
	private QuerySpecification fPatternData;
	private IQueryParticipant[] fParticipants;
	
	public JavaSearchQuery(QuerySpecification data) {
		this(data, getParticipants());
	}
	
	private static IQueryParticipant[] getParticipants() {
		return new IQueryParticipant[0];
	}
	
	private static class SearchRequestor implements ISearchRequestor {
		private IQueryParticipant fParticipant;
		private JavaSearchResult fSearchResult;
		public void reportMatch(Match match) {
			ISearchUIParticipant participant= fParticipant.getUIParticipant();
			if (participant == null || match.getElement() instanceof IJavaElement || match.getElement() instanceof IResource) {
				fSearchResult.addMatch(match);
			} else {
				fSearchResult.addMatch(match, participant);
			}
		}
		
		protected SearchRequestor(IQueryParticipant participant, JavaSearchResult result) {
			super();
			fParticipant= participant;
			fSearchResult= result;
		}
	}

	public JavaSearchQuery(QuerySpecification data, IQueryParticipant[] participants) {
		fPatternData= data;
		fParticipants= participants;
	}

	public IStatus run(IProgressMonitor monitor) {
		final JavaSearchResult textResult= (JavaSearchResult) getSearchResult();
		textResult.removeAll();
		// Also search working copies
		SearchEngine engine= new SearchEngine(JavaUI.getSharedWorkingCopiesOnClasspath());

		try {

			int totalTicks= 1000;
			for (int i= 0; i < fParticipants.length; i++) {
				totalTicks+= fParticipants[i].estimateTicks(fPatternData);
			}
			monitor.beginTask("Searching", totalTicks);
			IProgressMonitor mainSearchPM= new SubProgressMonitor(monitor, 1000);

			boolean ignoreImports= (fPatternData.getLimitTo() == IJavaSearchConstants.REFERENCES);
			ignoreImports &= PreferenceConstants.getPreferenceStore().getBoolean(WorkInProgressPreferencePage.PREF_SEARCH_IGNORE_IMPORTS);
			NewSearchResultCollector collector= new NewSearchResultCollector(textResult, mainSearchPM, ignoreImports);
			
			ISearchPattern pattern;
			if (fPatternData instanceof ElementQuerySpecification)
				pattern= SearchEngine.createSearchPattern(((ElementQuerySpecification)fPatternData).getElement(), fPatternData.getLimitTo());
			else {
				PatternQuerySpecification patternSpec= (PatternQuerySpecification)fPatternData;
				pattern= SearchEngine.createSearchPattern(patternSpec.getPattern(), patternSpec.getSearchFor(), patternSpec.getLimitTo(), patternSpec.isCaseSensitive());
			}
			
			engine.search(JavaPlugin.getWorkspace(), pattern, fPatternData.getScope(), collector);
			for (int i= 0; i < fParticipants.length; i++) {
				ISearchRequestor requestor= new SearchRequestor(fParticipants[i], textResult);
				IProgressMonitor participantPM= new SubProgressMonitor(monitor, fParticipants[i].estimateTicks(fPatternData));
				fParticipants[i].search(requestor, fPatternData, participantPM);
			}
			
		} catch (CoreException e) {
			return e.getStatus();
		}
		return new Status(IStatus.OK, JavaPlugin.getPluginId(), 0, "", null); //$NON-NLS-1$
	}


	public String getLabel() {
		if (fPatternData.getLimitTo() == IJavaSearchConstants.REFERENCES)
			return "Search for References";
		else if (fPatternData.getLimitTo() == IJavaSearchConstants.DECLARATIONS)
			return "Search for Declarations";
		else if (fPatternData.getLimitTo() == IJavaSearchConstants.READ_ACCESSES)
			return "Search for Read Accesses";
		else if (fPatternData.getLimitTo() == IJavaSearchConstants.WRITE_ACCESSES)
			return "Search for Write Accesses";
		else if (fPatternData.getLimitTo() == IJavaSearchConstants.IMPLEMENTORS)
			return "Search for Implementors";
		return "Search";
	}

	String getSingularLabel() {
		String desc= null;
		if (fPatternData instanceof ElementQuerySpecification) {
			IJavaElement element= ((ElementQuerySpecification)fPatternData).getElement();
			if (fPatternData.getLimitTo() == IJavaSearchConstants.REFERENCES
			&& element.getElementType() == IJavaElement.METHOD)
				desc= PrettySignature.getUnqualifiedMethodSignature((IMethod)element);
			else
				desc= element.getElementName();
			if ("".equals(desc) && element.getElementType() == IJavaElement.PACKAGE_FRAGMENT) //$NON-NLS-1$
				desc= SearchMessages.getString("JavaSearchOperation.default_package"); //$NON-NLS-1$
		} else {
			desc= ((PatternQuerySpecification)fPatternData).getPattern();
		}

		desc= "\""+desc+"\""; //$NON-NLS-1$ //$NON-NLS-2$
		String[] args= new String[] {desc, fPatternData.getScopeDescription()}; //$NON-NLS-1$
		switch (fPatternData.getLimitTo()) {
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
		if (fPatternData instanceof ElementQuerySpecification) {
			IJavaElement element= ((ElementQuerySpecification)fPatternData).getElement();
			if (fPatternData.getLimitTo() == IJavaSearchConstants.REFERENCES
			&& element.getElementType() == IJavaElement.METHOD)
				desc= PrettySignature.getUnqualifiedMethodSignature((IMethod)element);
			else
				desc= element.getElementName();
			if ("".equals(desc) && element.getElementType() == IJavaElement.PACKAGE_FRAGMENT) //$NON-NLS-1$
				desc= SearchMessages.getString("JavaSearchOperation.default_package"); //$NON-NLS-1$
		}
		else {
			desc= ((PatternQuerySpecification)fPatternData).getPattern();
		}

		desc= "\""+desc+"\""; //$NON-NLS-1$ //$NON-NLS-2$
		String[] args= new String[] {desc, "{0}", fPatternData.getScopeDescription()}; //$NON-NLS-1$
		switch (fPatternData.getLimitTo()) {
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
		if (fPatternData.getLimitTo() == IJavaSearchConstants.IMPLEMENTORS || fPatternData.getLimitTo() == IJavaSearchConstants.DECLARATIONS)
			return JavaPluginImages.DESC_OBJS_SEARCH_DECL;
		else
			return JavaPluginImages.DESC_OBJS_SEARCH_REF;
	}

	public boolean canRerun() {
		return true;
	}

	public boolean canRunInBackground() {
		return true;
	}

	public ISearchResult getSearchResult() {
		if (fResult == null)
			fResult= new JavaSearchResult(this);
		return fResult;
	}
}

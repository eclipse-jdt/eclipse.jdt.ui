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

import java.text.MessageFormat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.ui.search.ElementQuerySpecification;
import org.eclipse.jdt.ui.search.IMatchPresentation;
import org.eclipse.jdt.ui.search.IQueryParticipant;
import org.eclipse.jdt.ui.search.ISearchRequestor;
import org.eclipse.jdt.ui.search.PatternQuerySpecification;
import org.eclipse.jdt.ui.search.QuerySpecification;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.internal.ui.SearchPreferencePage;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.Match;

public class JavaSearchQuery implements ISearchQuery {
	private ISearchResult fResult;
	private QuerySpecification fPatternData;
	
	
	public JavaSearchQuery(QuerySpecification data) {
		fPatternData= data;
	}

	
	private static class SearchRequestor implements ISearchRequestor {
		private IQueryParticipant fParticipant;
		private JavaSearchResult fSearchResult;
		public void reportMatch(Match match) {
			IMatchPresentation participant= fParticipant.getUIParticipant();
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
	
	public IStatus run(IProgressMonitor monitor) {
		final JavaSearchResult textResult= (JavaSearchResult) getSearchResult();
		textResult.removeAll();
		// Don't need to pass in working copies in 3.0 here
		SearchEngine engine= new SearchEngine();
		try {

			int totalTicks= 1000;
			IProject[] projects= JavaSearchScopeFactory.getInstance().getProjects(fPatternData.getScope());
			final SearchParticipantRecord[] participantDescriptors= SearchParticipantsExtensionPoint.getInstance().getSearchParticipants(projects);
			final int[] ticks= new int[participantDescriptors.length];
			for (int i= 0; i < participantDescriptors.length; i++) {
				final int iPrime= i;
				ISafeRunnable runnable= new ISafeRunnable() {
					public void handleException(Throwable exception) {
						ticks[iPrime]= 0;
						String message= SearchMessages.getString("JavaSearchQuery.error.participant.estimate"); //$NON-NLS-1$
						JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, message, exception));
					}

					public void run() throws Exception {
						ticks[iPrime]= participantDescriptors[iPrime].getParticipant().estimateTicks(fPatternData);
					}
				};
				
				Platform.run(runnable);
				totalTicks+= ticks[i];
			}
			monitor.beginTask(SearchMessages.getString("JavaSearchQuery.task.label"), totalTicks); //$NON-NLS-1$
			IProgressMonitor mainSearchPM= new SubProgressMonitor(monitor, 1000);

			boolean ignorePotentials= SearchPreferencePage.arePotentialMatchesIgnored();
			NewSearchResultCollector collector= new NewSearchResultCollector(textResult, ignorePotentials);
			
			SearchPattern pattern;
			String stringPattern= null;
			
			if (fPatternData instanceof ElementQuerySpecification) {
				pattern= SearchPattern.createPattern(((ElementQuerySpecification)fPatternData).getElement(), fPatternData.getLimitTo());
				stringPattern= ((ElementQuerySpecification)fPatternData).getElement().getElementName();
			} else {
				PatternQuerySpecification patternSpec = (PatternQuerySpecification) fPatternData;
				stringPattern = patternSpec.getPattern();
				int matchMode = stringPattern.indexOf('*') != -1 || stringPattern.indexOf('?') != -1 ? SearchPattern.R_PATTERN_MATCH : SearchPattern.R_EXACT_MATCH;
				if (patternSpec.isCaseSensitive())
					matchMode |= SearchPattern.R_CASE_SENSITIVE;
				pattern = SearchPattern.createPattern(patternSpec.getPattern(), patternSpec.getSearchFor(), patternSpec.getLimitTo(), matchMode);
			}
			
			if (pattern == null) {
				return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, SearchMessages.getFormattedString("JavaSearchQuery.error.unsupported_pattern", stringPattern), null);  //$NON-NLS-1$
			}
			engine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, fPatternData.getScope(), collector, mainSearchPM);
			for (int i= 0; i < participantDescriptors.length; i++) {
				final ISearchRequestor requestor= new SearchRequestor(participantDescriptors[i].getParticipant(), textResult);
				final IProgressMonitor participantPM= new SubProgressMonitor(monitor, ticks[i]);

				final int iPrime= i;
				ISafeRunnable runnable= new ISafeRunnable() {
					public void handleException(Throwable exception) {
						participantDescriptors[iPrime].getDescriptor().disable();
						String message= SearchMessages.getString("JavaSearchQuery.error.participant.search"); //$NON-NLS-1$
						JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, message, exception));
					}

					public void run() throws Exception {
						participantDescriptors[iPrime].getParticipant().search(requestor, fPatternData, participantPM);
					}
				};
				
				Platform.run(runnable);

			}
			
		} catch (CoreException e) {
			return e.getStatus();
		}
		// TODO fix status message
		String message= SearchMessages.getString("JavaSearchQuery.status.ok.message"); //$NON-NLS-1$
		MessageFormat.format(message, new Object[] { new Integer(textResult.getMatchCount()) });
		return new Status(IStatus.OK, JavaPlugin.getPluginId(), 0, message, null);
	}

	public String getLabel() {
		Object[] args= new Object[] { quote(getSearchPattern()), fPatternData.getScopeDescription() };
		if (fPatternData.getLimitTo() == IJavaSearchConstants.REFERENCES)
			return SearchMessages.getFormattedString("JavaSearchQuery.searchfor_references", args); //$NON-NLS-1$
		else if (fPatternData.getLimitTo() == IJavaSearchConstants.DECLARATIONS)
			return SearchMessages.getFormattedString("JavaSearchQuery.searchfor_declarations", args); //$NON-NLS-1$
		else if (fPatternData.getLimitTo() == IJavaSearchConstants.READ_ACCESSES)
			return SearchMessages.getFormattedString("JavaSearchQuery.searchfor_read_access", args); //$NON-NLS-1$
		else if (fPatternData.getLimitTo() == IJavaSearchConstants.WRITE_ACCESSES)
			return SearchMessages.getFormattedString("JavaSearchQuery.searchfor_write_access", args); //$NON-NLS-1$
		else if (fPatternData.getLimitTo() == IJavaSearchConstants.IMPLEMENTORS)
			return SearchMessages.getFormattedString("JavaSearchQuery.searchfor_implementors", args); //$NON-NLS-1$
		return SearchMessages.getString("JavaSearchQuery.search_label"); //$NON-NLS-1$
	}

	String getSingularLabel() {
		String desc= null;
		desc= getSearchPattern();

		desc= quote(desc);
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
	
	private String getSearchPattern() {
		String desc;
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
		return desc;
	}

	public static String quote(String searchString) {
		searchString= searchString.replaceAll("\\{", "'{'"); //$NON-NLS-1$ //$NON-NLS-2$
		return searchString.replaceAll("\\}", "'}'"); //$NON-NLS-1$ //$NON-NLS-2$
		
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

		desc= quote(desc);
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
	
	QuerySpecification getSpecification() {
		return fPatternData;
	}
}

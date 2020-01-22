/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.search;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PerformanceStats;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.text.Match;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.SearchUtils;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.search.ElementQuerySpecification;
import org.eclipse.jdt.ui.search.IMatchPresentation;
import org.eclipse.jdt.ui.search.IQueryParticipant;
import org.eclipse.jdt.ui.search.ISearchRequestor;
import org.eclipse.jdt.ui.search.PatternQuerySpecification;
import org.eclipse.jdt.ui.search.QuerySpecification;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;

public class JavaSearchQuery implements ISearchQuery {

	private static final String PERF_SEARCH_PARTICIPANT= "org.eclipse.jdt.ui/perf/search/participants"; //$NON-NLS-1$

	private ISearchResult fResult;
	private final List<QuerySpecification> fPatternDataList;

	public JavaSearchQuery(QuerySpecification data) {
		if (data == null) {
			throw new IllegalArgumentException("data must not be null"); //$NON-NLS-1$
		}
		fPatternDataList= new ArrayList<>();
		fPatternDataList.add(data);
	}

	public JavaSearchQuery(List<QuerySpecification> dataList) {
		if (dataList == null || dataList.isEmpty()) {
			throw new IllegalArgumentException("data must not be null"); //$NON-NLS-1$
		}
		fPatternDataList= dataList;
	}

	private static class SearchRequestor implements ISearchRequestor {
		private IQueryParticipant fParticipant;
		private JavaSearchResult fSearchResult;
		@Override
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

	@Override
	public IStatus run(IProgressMonitor monitor) {
		final JavaSearchResult textResult= (JavaSearchResult) getSearchResult();
		textResult.removeAll();
		// Don't need to pass in working copies in 3.0 here
		SearchEngine engine= new SearchEngine();
		try {

			int totalTicks= 1000;
			IProject[] projects= JavaSearchScopeFactory.getInstance().getProjects(getFirstSpecification().getScope());
			final SearchParticipantRecord[] participantDescriptors= SearchParticipantsExtensionPoint.getInstance().getSearchParticipants(projects);
			final int[] ticks= new int[participantDescriptors.length];
			for (int i= 0; i < participantDescriptors.length; i++) {
				final int iPrime= i;
				ISafeRunnable runnable= new ISafeRunnable() {
					@Override
					public void handleException(Throwable exception) {
						ticks[iPrime]= 0;
						String message= SearchMessages.JavaSearchQuery_error_participant_estimate;
						JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, message, exception));
					}

					@Override
					public void run() throws Exception {
						for (QuerySpecification querySpecification : fPatternDataList) {
							ticks[iPrime]+= participantDescriptors[iPrime].getParticipant().estimateTicks(querySpecification);
						}
					}
				};

				SafeRunner.run(runnable);
				totalTicks+= ticks[i];
			}

			SearchPattern pattern= null;
			String stringPattern= null;

			if (fPatternDataList.size() == 1) {
				if (getFirstSpecification() instanceof ElementQuerySpecification) {
					IJavaElement element= ((ElementQuerySpecification) getFirstSpecification()).getElement();
					stringPattern= JavaElementLabels.getElementLabel(element, JavaElementLabels.ALL_DEFAULT);
					if (!element.exists()) {
						return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, Messages.format(SearchMessages.JavaSearchQuery_error_element_does_not_exist, stringPattern), null);
					}
					pattern= SearchPattern.createPattern(element, getFirstSpecification().getLimitTo(), SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE);
				} else if (getFirstSpecification() instanceof PatternQuerySpecification) {
					PatternQuerySpecification patternSpec= (PatternQuerySpecification) getFirstSpecification();
					stringPattern= patternSpec.getPattern();
					int matchMode= getMatchMode(stringPattern) | SearchPattern.R_ERASURE_MATCH;
					if (patternSpec.isCaseSensitive())
						matchMode|= SearchPattern.R_CASE_SENSITIVE;
					pattern= SearchPattern.createPattern(patternSpec.getPattern(), patternSpec.getSearchFor(), patternSpec.getLimitTo(), matchMode);
				}
			} else {
				for (QuerySpecification querySpecification : fPatternDataList) {
					if (!(querySpecification instanceof ElementQuerySpecification)) {
						break;
					}
					IJavaElement element= ((ElementQuerySpecification) querySpecification).getElement();
					stringPattern= JavaElementLabels.getElementLabel(element, JavaElementLabels.ALL_DEFAULT);
					if (!element.exists()) {
						return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, Messages.format(SearchMessages.JavaSearchQuery_error_element_does_not_exist, stringPattern), null);
					}
					SearchPattern elementPattern= SearchPattern.createPattern(element, getFirstSpecification().getLimitTo(), SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE);
					pattern= pattern == null ? elementPattern : SearchPattern.createOrPattern(pattern, elementPattern);
				}
			}

			if (pattern == null) {
				return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, Messages.format(SearchMessages.JavaSearchQuery_error_unsupported_pattern, stringPattern), null);
			}
			SubMonitor subMonitor= SubMonitor.convert(monitor, Messages.format(SearchMessages.JavaSearchQuery_task_label, stringPattern), totalTicks);

			boolean ignorePotentials= NewSearchUI.arePotentialMatchesIgnored();
			NewSearchResultCollector collector= new NewSearchResultCollector(textResult, ignorePotentials);


			engine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, getFirstSpecification().getScope(), collector, subMonitor.split(1000));
			for (int i= 0; i < participantDescriptors.length; i++) {
				final ISearchRequestor requestor= new SearchRequestor(participantDescriptors[i].getParticipant(), textResult);
				final IProgressMonitor participantPM= subMonitor.split(ticks[i]);

				final int iPrime= i;
				ISafeRunnable runnable= new ISafeRunnable() {
					@Override
					public void handleException(Throwable exception) {
						participantDescriptors[iPrime].getDescriptor().disable();
						String message= SearchMessages.JavaSearchQuery_error_participant_search;
						JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, message, exception));
					}

					@Override
					public void run() throws Exception {

						final IQueryParticipant participant= participantDescriptors[iPrime].getParticipant();

						final PerformanceStats stats= PerformanceStats.getStats(PERF_SEARCH_PARTICIPANT, participant);
						stats.startRun();

						for (QuerySpecification querySpecification : fPatternDataList) {
							participant.search(requestor, querySpecification, participantPM);
						}

						stats.endRun();
					}
				};

				SafeRunner.run(runnable);
			}

		} catch (CoreException e) {
			return e.getStatus();
		}
		String message= Messages.format(SearchMessages.JavaSearchQuery_status_ok_message, String.valueOf(textResult.getMatchCount()));
		return new Status(IStatus.OK, JavaPlugin.getPluginId(), 0, message, null);
	}

	private int getMatchMode(String pattern) {
		if (pattern.indexOf('*') != -1 || pattern.indexOf('?') != -1) {
			return SearchPattern.R_PATTERN_MATCH;
		} else if (SearchUtils.isCamelCasePattern(pattern)) {
			return SearchPattern.R_CAMELCASE_MATCH;
		}
		return SearchPattern.R_EXACT_MATCH;
	}

	@Override
	public String getLabel() {
		return SearchMessages.JavaSearchQuery_label;
	}

	public String getResultLabel(int nMatches) {
		int limitTo= getMaskedLimitTo();
		if (nMatches == 1) {
			String[] args= { getSearchPatternDescription(), getFirstSpecification().getScopeDescription() };
			switch (limitTo) {
				case IJavaSearchConstants.IMPLEMENTORS:
					return Messages.format(SearchMessages.JavaSearchOperation_singularImplementorsPostfix, args);
				case IJavaSearchConstants.DECLARATIONS:
					return Messages.format(SearchMessages.JavaSearchOperation_singularDeclarationsPostfix, args);
				case IJavaSearchConstants.REFERENCES:
					return Messages.format(SearchMessages.JavaSearchOperation_singularReferencesPostfix, args);
				case IJavaSearchConstants.ALL_OCCURRENCES:
					return Messages.format(SearchMessages.JavaSearchOperation_singularOccurrencesPostfix, args);
				case IJavaSearchConstants.READ_ACCESSES:
					return Messages.format(SearchMessages.JavaSearchOperation_singularReadReferencesPostfix, args);
				case IJavaSearchConstants.WRITE_ACCESSES:
					return Messages.format(SearchMessages.JavaSearchOperation_singularWriteReferencesPostfix, args);
				default:
					String matchLocations= MatchLocations.getMatchLocationDescription(limitTo, 3);
					return Messages.format(SearchMessages.JavaSearchQuery_singularReferencesWithMatchLocations, new Object[] { args[0], args[1], matchLocations });
			}
		} else {
			Object[] args= { getSearchPatternDescription(), Integer.valueOf(nMatches), getFirstSpecification().getScopeDescription() };
			switch (limitTo) {
				case IJavaSearchConstants.IMPLEMENTORS:
					return Messages.format(SearchMessages.JavaSearchOperation_pluralImplementorsPostfix, args);
				case IJavaSearchConstants.DECLARATIONS:
					return Messages.format(SearchMessages.JavaSearchOperation_pluralDeclarationsPostfix, args);
				case IJavaSearchConstants.REFERENCES:
					return Messages.format(SearchMessages.JavaSearchOperation_pluralReferencesPostfix, args);
				case IJavaSearchConstants.ALL_OCCURRENCES:
					return Messages.format(SearchMessages.JavaSearchOperation_pluralOccurrencesPostfix, args);
				case IJavaSearchConstants.READ_ACCESSES:
					return Messages.format(SearchMessages.JavaSearchOperation_pluralReadReferencesPostfix, args);
				case IJavaSearchConstants.WRITE_ACCESSES:
					return Messages.format(SearchMessages.JavaSearchOperation_pluralWriteReferencesPostfix, args);
				default:
					String matchLocations= MatchLocations.getMatchLocationDescription(limitTo, 3);
					return Messages.format(SearchMessages.JavaSearchQuery_pluralReferencesWithMatchLocations, new Object[] { args[0], args[1], args[2], matchLocations });
			}
		}
	}

	private String getSearchPatternDescription() {
		QuerySpecification firstSpecification= getFirstSpecification();
		if (firstSpecification instanceof ElementQuerySpecification) {
			IJavaElement element= ((ElementQuerySpecification) firstSpecification).getElement();
			return JavaElementLabels.getElementLabel(element, JavaElementLabels.ALL_DEFAULT
					| JavaElementLabels.ALL_FULLY_QUALIFIED | JavaElementLabels.USE_RESOLVED | JavaElementLabels.P_COMPRESSED);
		} else if (firstSpecification instanceof PatternQuerySpecification) {
			return BasicElementLabels.getFilePattern(((PatternQuerySpecification) firstSpecification).getPattern());
		} else {
			return ""; //$NON-NLS-1$
		}
	}

	private int getMaskedLimitTo() {
		return getFirstSpecification().getLimitTo() & ~(IJavaSearchConstants.IGNORE_RETURN_TYPE | IJavaSearchConstants.IGNORE_DECLARING_TYPE);
	}

	ImageDescriptor getImageDescriptor() {
		int limitTo= getMaskedLimitTo();
		if (limitTo == IJavaSearchConstants.IMPLEMENTORS || limitTo == IJavaSearchConstants.DECLARATIONS)
			return JavaPluginImages.DESC_OBJS_SEARCH_DECL;
		else
			return JavaPluginImages.DESC_OBJS_SEARCH_REF;
	}

	@Override
	public boolean canRerun() {
		return true;
	}

	@Override
	public boolean canRunInBackground() {
		return true;
	}

	@Override
	public ISearchResult getSearchResult() {
		if (fResult == null) {
			JavaSearchResult result= new JavaSearchResult(this);
			new SearchResultUpdater(result);
			fResult= result;
		}
		return fResult;
	}

	private QuerySpecification getFirstSpecification() {
		return fPatternDataList.get(0);
	}

	List<QuerySpecification> getSpecification() {
		return fPatternDataList;
	}
}

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.search.IInfoConstants;
import org.eclipse.jdt.internal.core.search.PathCollector;
import org.eclipse.jdt.internal.core.search.PatternSearchJob;
import org.eclipse.jdt.internal.core.search.indexing.IndexManager;
import org.eclipse.jdt.internal.core.search.matching.MatchLocator;
import org.eclipse.jdt.internal.core.search.matching.SearchPattern;

class CustomSearchEngine extends SearchEngine {

	public void search(IWorkspace workspace, ISearchPattern searchPattern, IJavaSearchScope scope, IJavaSearchResultCollector resultCollector) throws JavaModelException {
		//XXX: code copied from org.eclipse.jdt.core.search.SearchEngine

	/* search is starting */
	resultCollector.aboutToStart();

	try {	
		if (searchPattern == null) return;

		/* initialize progress monitor */
		IProgressMonitor progressMonitor = resultCollector.getProgressMonitor();
		if (progressMonitor != null) {
			progressMonitor.beginTask(RefactoringCoreMessages.getString("CustomSearchEngine.Searching"), 105); // 5 for getting paths, 100 for locating matches //$NON-NLS-1$
		}

		/* index search */
		PathCollector pathCollector = new PathCollector();

		IndexManager indexManager = ((JavaModelManager)JavaModelManager.getJavaModelManager())
										.getIndexManager();
		int detailLevel = IInfoConstants.PathInfo | IInfoConstants.PositionInfo;
		MatchLocator matchLocator = new RefactoringMatchLocator((SearchPattern)searchPattern, detailLevel, resultCollector, scope);
		if (indexManager != null) {
			indexManager.performConcurrentJob(
				new PatternSearchJob(
					(SearchPattern)searchPattern, 
					scope, 
					detailLevel, 
					pathCollector, 
					indexManager, 
					progressMonitor),
				IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
				progressMonitor);

			if (progressMonitor != null) {
				progressMonitor.worked(5);
			}
				
			/* eliminating false matches and locating them */
			if (progressMonitor != null && progressMonitor.isCanceled()) throw new OperationCanceledException();
			matchLocator.locateMatches(pathCollector.getPaths(), workspace);
		}

		if (progressMonitor != null) {
			progressMonitor.done();
		}

		matchLocator.locatePackageDeclarations(workspace);
	} finally {
		/* search has ended */
		resultCollector.done();
	}
	}
}



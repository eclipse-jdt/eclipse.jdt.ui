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
import java.util.HashMap;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.OptionalMessageDialog;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.search.ui.IActionGroupFactory;
import org.eclipse.search.ui.ISearchResultView;
import org.eclipse.search.ui.SearchUI;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.ide.IDE;


public class JavaSearchResultCollector implements IJavaSearchResultCollector {

	private static final String MATCH= SearchMessages.getString("SearchResultCollector.match"); //$NON-NLS-1$
	private static final String MATCHES= SearchMessages.getString("SearchResultCollector.matches"); //$NON-NLS-1$
	private static final String DONE= SearchMessages.getString("SearchResultCollector.done"); //$NON-NLS-1$
	private static final Boolean POTENTIAL_MATCH_VALUE= new Boolean(true);
	private static final String POTENTIAL_MATCH_DIALOG_ID= "Search.PotentialMatchDialog"; //$NON-NLS-1$
	
	private IProgressMonitor fMonitor;
	private ISearchResultView fView;
	private JavaSearchOperation fOperation;
	private int fMatchCount= 0;
	private int fPotentialMatchCount= 0;
	private long fLastUpdateTime;
	private Integer[] fMessageFormatArgs= new Integer[1];
	
	private class ActionGroupFactory implements IActionGroupFactory {
		public ActionGroup createActionGroup(ISearchResultView part) {
			return new SearchViewActionGroup(part);
		}
	}
	
	public JavaSearchResultCollector() {
		// This ensures that the image class is loaded correctly
		JavaPlugin.getDefault().getImageRegistry();
	}

	/**
	 * @see IJavaSearchResultCollector#aboutToStart().
	 */
	public void aboutToStart() {
		fPotentialMatchCount= 0;
		fView= SearchUI.getSearchResultView();
		fMatchCount= 0;
		fLastUpdateTime= 0;
		if (fView != null) {
			fView.searchStarted(
				new ActionGroupFactory(),
				fOperation.getSingularLabel(),
				fOperation.getPluralLabelPattern(),
				fOperation.getImageDescriptor(),
				JavaSearchPage.EXTENSION_POINT_ID,
				new JavaSearchResultLabelProvider(),
				new GotoMarkerAction(),
				new GroupByKeyComputer(),
				fOperation);
		}
	}
	
	/**
	 * @see IJavaSearchResultCollector#accept
	 */
	public void accept(IResource resource, int start, int end, IJavaElement enclosingElement, int accuracy) throws CoreException {
		if (enclosingElement == null || accuracy == POTENTIAL_MATCH && SearchUI.arePotentialMatchesIgnored())
			return;
		
		IMarker marker= resource.createMarker(SearchUI.SEARCH_MARKER);
		HashMap attributes;
		Object groupKey= enclosingElement;
		attributes= new HashMap(7);
		if (accuracy == POTENTIAL_MATCH) {
			fPotentialMatchCount++;
			attributes.put(SearchUI.POTENTIAL_MATCH, POTENTIAL_MATCH_VALUE);
			if (groupKey == null)
				groupKey= "?:null"; //$NON-NLS-1$
			else
				groupKey= "?:" + enclosingElement.getHandleIdentifier(); //$NON-NLS-1$
		}			
		ICompilationUnit cu= SearchUtil.findCompilationUnit(enclosingElement);
		if (cu != null && cu.isWorkingCopy())
			attributes.put(IJavaSearchUIConstants.ATT_IS_WORKING_COPY, new Boolean(true)); //$NON-NLS-1$
			
		JavaCore.addJavaElementMarkerAttributes(attributes, enclosingElement);
		attributes.put(IJavaSearchUIConstants.ATT_JE_HANDLE_ID, enclosingElement.getHandleIdentifier());
		attributes.put(IMarker.CHAR_START, new Integer(Math.max(start, 0)));
		attributes.put(IMarker.CHAR_END, new Integer(Math.max(end, 0)));
		if (enclosingElement instanceof IMember && ((IMember)enclosingElement).isBinary())
			attributes.put(IDE.EDITOR_ID_ATTR, JavaUI.ID_CF_EDITOR);
		else
			attributes.put(IDE.EDITOR_ID_ATTR, JavaUI.ID_CU_EDITOR);
		marker.setAttributes(attributes);

		if (fView != null)
			fView.addMatch(enclosingElement.getElementName(), groupKey, resource, marker);
		fMatchCount++;
		if (!getProgressMonitor().isCanceled() && System.currentTimeMillis() - fLastUpdateTime > 1000) {
			getProgressMonitor().subTask(getFormattedMatchesString(fMatchCount));
			fLastUpdateTime= System.currentTimeMillis();
		}
	}
	
	/**
	 * @see IJavaSearchResultCollector#done().
	 */
	public void done() {
		if (!getProgressMonitor().isCanceled()) {
			String matchesString= getFormattedMatchesString(fMatchCount);
			getProgressMonitor().setTaskName(MessageFormat.format(DONE, new String[]{matchesString}));
		}

		if (fView != null) {
			if (fPotentialMatchCount > 0)
				explainPotentialMatch(fPotentialMatchCount);
			fView.searchFinished();
		}

		// Cut no longer unused references because the collector might be re-used
		fView= null;
		fMonitor= null;
	}

	private void explainPotentialMatch(final int potentialMatchCount) {
		// Make sure we are doing it in the right thread.
		final Shell shell= fView.getSite().getShell();
		final String title;
		if (potentialMatchCount == 1)
			title= new String(SearchMessages.getString("Search.potentialMatchDialog.title.foundPotentialMatch")); //$NON-NLS-1$
		else
			title= new String(SearchMessages.getFormattedString("Search.potentialMatchDialog.title.foundPotentialMatches", "" + potentialMatchCount)); //$NON-NLS-1$ //$NON-NLS-2$
		
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				OptionalMessageDialog.open(
					POTENTIAL_MATCH_DIALOG_ID, //$NON-NLS-1$,
					shell,
					title,
					null,
					SearchMessages.getString("Search.potentialMatchDialog.message"), //$NON-NLS-1$,
					MessageDialog.INFORMATION,
					new String[] { IDialogConstants.OK_LABEL },
					0);
			}
		});
	}
	
	/*
	 * @see IJavaSearchResultCollector#getProgressMonitor().
	 */
	public IProgressMonitor getProgressMonitor() {
		return fMonitor;
	}
	
	void setProgressMonitor(IProgressMonitor pm) {
		fMonitor= pm;
	}	
	
	void setOperation(JavaSearchOperation operation) {
		fOperation= operation;
	}
	
	private String getFormattedMatchesString(int count) {
		if (fMatchCount == 1)
			return MATCH;
		fMessageFormatArgs[0]= new Integer(count);
		return MessageFormat.format(MATCHES, fMessageFormatArgs);

	}
}

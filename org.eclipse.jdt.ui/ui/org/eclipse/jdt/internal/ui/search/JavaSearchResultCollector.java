/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import java.util.HashMap;
import java.text.MessageFormat;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IInputSelectionProvider;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;

import org.eclipse.search.ui.IContextMenuContributor;
import org.eclipse.search.ui.ISearchResultView;
import org.eclipse.search.ui.ISearchResultViewEntry;
import org.eclipse.search.ui.SearchUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.JavaSearchActionGroup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.OpenTypeHierarchyUtil;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;


public class JavaSearchResultCollector implements IJavaSearchResultCollector {

	private static final String MATCH= SearchMessages.getString("SearchResultCollector.match"); //$NON-NLS-1$
	private static final String MATCHES= SearchMessages.getString("SearchResultCollector.matches"); //$NON-NLS-1$
	private static final String DONE= SearchMessages.getString("SearchResultCollector.done"); //$NON-NLS-1$
	private static final String SEARCHING= SearchMessages.getString("SearchResultCollector.searching"); //$NON-NLS-1$
	private static final Integer POTENTIAL_MATCH_VALUE= new Integer(POTENTIAL_MATCH);
	
	private IProgressMonitor fMonitor;
	private IContextMenuContributor fContextMenu;
	private ISearchResultView fView;
	private JavaSearchOperation fOperation;
	private int fMatchCount= 0;
	private int fPotentialMatchCount= 0;
	private Integer[] fMessageFormatArgs= new Integer[1];
	
	private class ContextMenuContributor implements IContextMenuContributor {

		public void fill(IMenuManager menu, IInputSelectionProvider inputProvider) {
			JavaPlugin.createStandardGroups(menu);
			ActionGroup searchGroup= new JavaSearchActionGroup(SearchUI.getSearchResultView());
			searchGroup.setContext(new ActionContext(inputProvider.getSelection()));
			searchGroup.fillContextMenu(menu);
			searchGroup.setContext(null);
			OpenTypeHierarchyUtil.addToMenu(getWorbenchWindow(), menu, convertSelection(inputProvider.getSelection()));
		}
		
		private Object convertSelection(ISelection selection) {
			Object element= SelectionUtil.getSingleElement(selection);
			if (!(element instanceof ISearchResultViewEntry))
				return null;
			IMarker marker= ((ISearchResultViewEntry)element).getSelectedMarker();
			try {
				return JavaCore.create((String) ((IMarker) marker).getAttribute(IJavaSearchUIConstants.ATT_JE_HANDLE_ID));	
			} catch (CoreException ex) {
				ExceptionHandler.log(ex, SearchMessages.getString("Search.Error.createJavaElement.message")); //$NON-NLS-1$
				return null;
			}
		}
	}
	
	public JavaSearchResultCollector() {
		fContextMenu= new ContextMenuContributor();
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
		if (fView != null) {
			fView.searchStarted(
				JavaSearchPage.EXTENSION_POINT_ID,
				fOperation.getSingularLabel(),
				fOperation.getPluralLabelPattern(),
				fOperation.getImageDescriptor(),
				fContextMenu,
				new JavaSearchResultLabelProvider(),
				new GotoMarkerAction(),
				new GroupByKeyComputer(),
				fOperation);
		}
		if (!getProgressMonitor().isCanceled())
			getProgressMonitor().subTask(SEARCHING);
	}
	
	/**
	 * @see IJavaSearchResultCollector#accept
	 */
	public void accept(IResource resource, int start, int end, IJavaElement enclosingElement, int accuracy) throws CoreException {
		IMarker marker= resource.createMarker(SearchUI.SEARCH_MARKER);
		HashMap attributes;
		Object groupKey= enclosingElement;
		attributes= new HashMap(7);
		if (accuracy == POTENTIAL_MATCH) {
			fPotentialMatchCount++;
			attributes.put(IJavaSearchUIConstants.ATT_ACCURACY, POTENTIAL_MATCH_VALUE);
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
			attributes.put(IWorkbenchPage.EDITOR_ID_ATTR, JavaUI.ID_CF_EDITOR);
		else
			attributes.put(IWorkbenchPage.EDITOR_ID_ATTR, JavaUI.ID_CU_EDITOR);
		marker.setAttributes(attributes);

		fView.addMatch(enclosingElement.getElementName(), groupKey, resource, marker);
		fMatchCount++;
		if (!getProgressMonitor().isCanceled())
			getProgressMonitor().subTask(getFormattedMatchesString(fMatchCount));
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
			if (fPotentialMatchCount > 0 && PotentialMatchDialog.shouldExplain())
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
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				PotentialMatchDialog.open(shell, potentialMatchCount);
			}
		});
	}
	
	/*
	 * @see IJavaSearchResultCollector#getProgressMonitor().
	 */
	public IProgressMonitor getProgressMonitor() {
		return fMonitor;
	};
	
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

	private IWorkbenchWindow getWorbenchWindow() {
		IWorkbenchWindow wbWindow= null;
		if (fView != null && fView.getSite() != null)
			wbWindow= fView.getSite().getWorkbenchWindow();
		if (wbWindow == null)
			wbWindow= JavaPlugin.getActiveWorkbenchWindow();
		return wbWindow;
	}
}
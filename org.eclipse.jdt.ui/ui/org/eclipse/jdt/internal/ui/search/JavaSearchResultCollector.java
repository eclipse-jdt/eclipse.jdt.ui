/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import java.text.MessageFormat;
import java.util.HashMap;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IInputSelectionProvider;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

import org.eclipse.search.ui.IContextMenuContributor;
import org.eclipse.search.ui.ISearchResultView;
import org.eclipse.search.ui.ISearchResultViewEntry;
import org.eclipse.search.ui.SearchUI;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.ui.actions.GroupContext;
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
	private Integer[] fMessageFormatArgs= new Integer[1];
	
	private class ContextMenuContributor implements IContextMenuContributor {

		public void fill(IMenuManager menu, IInputSelectionProvider inputProvider) {
			JavaPlugin.createStandardGroups(menu);
			new JavaSearchGroup().fill(menu, new GroupContext(inputProvider));
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
	}

	/**
	 * @see IJavaSearchResultCollector#aboutToStart().
	 */
	public void aboutToStart() {
		fView= SearchUI.getSearchResultView();
		fMatchCount= 0;
		if (fView != null) {
			fView.searchStarted(
				JavaSearchPage.EXTENSION_POINT_ID,
				fOperation.getDescription(),
				fOperation.getImageDescriptor(),
				fContextMenu,
				JavaSearchResultLabelProvider.INSTANCE,
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
		if (accuracy == POTENTIAL_MATCH) {
			attributes= new HashMap(6);
			attributes.put(IJavaSearchUIConstants.ATT_ACCURACY, POTENTIAL_MATCH_VALUE);
		} else
			attributes= new HashMap(5);
		JavaCore.addJavaElementMarkerAttributes(attributes, enclosingElement);
		attributes.put(IJavaSearchUIConstants.ATT_JE_HANDLE_ID, enclosingElement.getHandleIdentifier());
		attributes.put(IMarker.CHAR_START, new Integer(Math.max(start, 0)));
		attributes.put(IMarker.CHAR_END, new Integer(Math.max(end, 0)));
		if (enclosingElement instanceof IMember && ((IMember)enclosingElement).isBinary())
			attributes.put(IWorkbenchPage.EDITOR_ID_ATTR, JavaUI.ID_CF_EDITOR);
		else
			attributes.put(IWorkbenchPage.EDITOR_ID_ATTR, JavaUI.ID_CU_EDITOR);
		marker.setAttributes(attributes);

		fView.addMatch(enclosingElement.getElementName(), enclosingElement, resource, marker);

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

		if (fView != null)
			fView.searchFinished();

		// Cut no longer unused references because the collector might be re-used
		fView= null;
		fMonitor= null;
	}
	
	/**
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
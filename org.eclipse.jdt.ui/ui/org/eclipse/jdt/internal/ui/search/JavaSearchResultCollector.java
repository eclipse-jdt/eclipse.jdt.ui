/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import java.util.HashMap;import org.eclipse.jface.action.IMenuManager;import org.eclipse.jface.viewers.IInputSelectionProvider;import org.eclipse.jface.viewers.ISelection;import org.eclipse.core.resources.IMarker;import org.eclipse.core.resources.IResource;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.ui.IWorkbenchPage;import org.eclipse.search.ui.IContextMenuContributor;import org.eclipse.search.ui.ISearchResultView;import org.eclipse.search.ui.ISearchResultViewEntry;import org.eclipse.search.ui.SearchUI;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IMember;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.search.IJavaSearchResultCollector;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;import org.eclipse.jdt.internal.ui.util.OpenTypeHierarchyHelper;import org.eclipse.jdt.internal.ui.util.SelectionUtil;import org.eclipse.jdt.ui.JavaUI;


public class JavaSearchResultCollector implements IJavaSearchResultCollector {

	private static final String SPACE_MATCHES= " " + SearchMessages.getString("SearchResultCollector.matches"); //$NON-NLS-2$ //$NON-NLS-1$
	
	private IProgressMonitor fMonitor;
	private IContextMenuContributor fContextMenu;
	private ISearchResultView fView;
	private JavaSearchOperation fOperation;
	private int fMatchCount= 0;
	
	private static class ContextMenuContributor implements IContextMenuContributor {
		private ElementSearchAction[] fSearchActions= new ElementSearchAction[] {
			new FindReferencesAction(), 
			new FindDeclarationsAction(),
			new FindHierarchyDeclarationsAction(),
			new FindImplementorsAction()};
						
		public void fill(IMenuManager menu, IInputSelectionProvider inputProvider) {
			JavaPlugin.createStandardGroups(menu);
			for (int i= 0; i < fSearchActions.length; i++) {
				ElementSearchAction action= fSearchActions[i];
				if (action.canOperateOn(inputProvider.getSelection()))
					menu.add(action);
			}
			// XXX should get the workbench window from the site. 
			OpenTypeHierarchyHelper.addToMenu(JavaPlugin.getActiveWorkbenchWindow(), 
				menu, convertSelection(inputProvider.getSelection()));
		}
		
		private Object convertSelection(ISelection selection) {
			Object element= SelectionUtil.getSingleElement(selection);
			if (!(element instanceof ISearchResultViewEntry))
				return null;
			IMarker marker= ((ISearchResultViewEntry)element).getSelectedMarker();
			try {
				return JavaCore.create((String) ((IMarker) marker).getAttribute(IJavaSearchUIConstants.ATT_JE_HANDLE_ID));	
			} catch (CoreException e) {
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
	}
	 
	/**
	 * @see IJavaSearchResultCollector#accept
	 */
	public void accept(IResource resource, int start, int end, IJavaElement enclosingElement, int accuracy) throws CoreException {
		IMarker marker= resource.createMarker(SearchUI.SEARCH_MARKER);
		HashMap attributes= new HashMap(4);
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
		if (!getProgressMonitor().isCanceled())
			getProgressMonitor().subTask(++fMatchCount + SPACE_MATCHES);
	}
	
	/**
	 * @see IJavaSearchResultCollector#done().
	 */
	public void done() {
		if (!getProgressMonitor().isCanceled())
			getProgressMonitor().setTaskName(SearchMessages.getString("SearchResultCollector.done") + ": " + fMatchCount + SPACE_MATCHES + "   "); //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$

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
}
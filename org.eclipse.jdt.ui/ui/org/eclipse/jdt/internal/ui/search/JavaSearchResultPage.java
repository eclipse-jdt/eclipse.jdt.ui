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
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.viewsupport.ProblemTableViewer;
import org.eclipse.jdt.internal.ui.viewsupport.ProblemTreeViewer;
import org.eclipse.jdt.ui.search.IMatchPresentation;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.search.ui.IContextMenuConstants;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.ISearchResultViewPart;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.SearchUI;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.texteditor.ITextEditor;

public class JavaSearchResultPage extends AbstractTextSearchViewPage {
	private static final String KEY_GROUPING= "org.eclipse.jdt.search.resultpage.grouping"; //$NON-NLS-1$
	private static final String KEY_SORTING= "org.eclipse.jdt.search.resultpage.sorting"; //$NON-NLS-1$
	
	
	private NewSearchViewActionGroup fActionGroup;
	private JavaSearchContentProvider fContentProvider;
	private int fCurrentSortOrder;
	private SortAction fSortByNameAction;
	private SortAction fSortByParentName;
	private SortAction fSortByPathAction;
	
	private GroupAction fGroupTypeAction;
	private GroupAction fGroupFileAction;
	private GroupAction fGroupPackageAction;
	private GroupAction fGroupProjectAction;
	private int fCurrentGrouping;
	
	private Set fMatchFilters= new HashSet();
	private FilterAction fFilterImportsAction;
	private FilterAction fFilterReadsAction;
	private FilterAction fFilterWritesAction;
	private FilterAction fFilterJavadocAction;
	
	public JavaSearchResultPage() {
		initSortActions();
		initGroupingActions();
		initFilterActions();
	}
	
	private void initFilterActions() {
		fFilterImportsAction= new FilterAction(this, new ImportFilter());
		fFilterReadsAction= new FilterAction(this, new ReadFilter());
		fFilterWritesAction= new FilterAction(this, new WriteFilter());
		fFilterJavadocAction= new FilterAction(this, new JavadocFilter());
	}

	private void initSortActions() {
		fSortByNameAction= new SortAction(SearchMessages.getString("JavaSearchResultPage.sortByName"), this, SortingLabelProvider.SHOW_ELEMENT_CONTAINER); //$NON-NLS-1$
		fSortByPathAction= new SortAction(SearchMessages.getString("JavaSearchResultPage.sortByPath"), this, SortingLabelProvider.SHOW_PATH); //$NON-NLS-1$
		fSortByParentName= new SortAction(SearchMessages.getString("JavaSearchResultPage.sortByParentName"), this, SortingLabelProvider.SHOW_CONTAINER_ELEMENT); //$NON-NLS-1$
	}

	private void initGroupingActions() {
		fGroupProjectAction= new GroupAction(SearchMessages.getString("JavaSearchResultPage.groupby_project"), SearchMessages.getString("JavaSearchResultPage.groupby_project.tooltip"), this, LevelTreeContentProvider.LEVEL_PROJECT); //$NON-NLS-1$ //$NON-NLS-2$
		JavaPluginImages.setLocalImageDescriptors(fGroupProjectAction, "prj_mode.gif"); //$NON-NLS-1$
		fGroupPackageAction= new GroupAction(SearchMessages.getString("JavaSearchResultPage.groupby_package"), SearchMessages.getString("JavaSearchResultPage.groupby_package.tooltip"), this, LevelTreeContentProvider.LEVEL_PACKAGE); //$NON-NLS-1$ //$NON-NLS-2$
		JavaPluginImages.setLocalImageDescriptors(fGroupPackageAction, "package_mode.gif"); //$NON-NLS-1$
		fGroupFileAction= new GroupAction(SearchMessages.getString("JavaSearchResultPage.groupby_file"), SearchMessages.getString("JavaSearchResultPage.groupby_file.tooltip"), this, LevelTreeContentProvider.LEVEL_FILE); //$NON-NLS-1$ //$NON-NLS-2$
		JavaPluginImages.setLocalImageDescriptors(fGroupFileAction, "file_mode.gif"); //$NON-NLS-1$
		fGroupTypeAction= new GroupAction(SearchMessages.getString("JavaSearchResultPage.groupby_type"), SearchMessages.getString("JavaSearchResultPage.groupby_type.tooltip"), this, LevelTreeContentProvider.LEVEL_TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		JavaPluginImages.setLocalImageDescriptors(fGroupTypeAction, "class_obj.gif"); //$NON-NLS-1$
	}

	public void setViewPart(ISearchResultViewPart part) {
		super.setViewPart(part);
		fActionGroup= new NewSearchViewActionGroup(part);
	}
	
	public void showMatch(Match match, int offset, int length) throws PartInitException {
		IEditorPart editor= null;
		Object element= match.getElement();
		if (element instanceof IJavaElement) {
			IJavaElement javaElement= (IJavaElement) element;
			try {
				editor= EditorUtility.openInEditor(javaElement, false);
			} catch (PartInitException e1) {
				return;
			} catch (JavaModelException e1) {
				return;
			}
		} else if (element instanceof IFile) {
			editor= IDE.openEditor(JavaPlugin.getActivePage(), (IFile) element, false);
		}
		if (editor instanceof ITextEditor) {
			ITextEditor textEditor= (ITextEditor) editor;
			textEditor.selectAndReveal(offset, length);
		} else if (editor != null){
			if (element instanceof IFile) {
				IFile file= (IFile) element;
				showWithMarker(editor, file, offset, length);
			}
		} else {
			JavaSearchResult result= (JavaSearchResult) getInput();
			IMatchPresentation participant= result.getSearchParticpant(element);
			if (participant != null)
				participant.showMatch(match, offset, length);
		}
	}
	
	private void showWithMarker(IEditorPart editor, IFile file, int offset, int length) throws PartInitException {
		try {
			IMarker marker= file.createMarker(SearchUI.SEARCH_MARKER);
			HashMap attributes= new HashMap(4);
			attributes.put(IMarker.CHAR_START, new Integer(offset));
			attributes.put(IMarker.CHAR_END, new Integer(offset + length));
			marker.setAttributes(attributes);
			IDE.gotoMarker(editor, marker);
			marker.delete();
		} catch (CoreException e) {
			throw new PartInitException(SearchMessages.getString("JavaSearchResultPage.error.marker"), e); //$NON-NLS-1$
		}
	}

	protected void fillContextMenu(IMenuManager mgr) {
		super.fillContextMenu(mgr);
		addSortActions(mgr);

		fActionGroup.setContext(new ActionContext(getSite().getSelectionProvider().getSelection()));
		fActionGroup.fillContextMenu(mgr);
		
	}
	
	private void addFilterActions(IMenuManager viewMenu) {
		String filteringGroup= "Filtering"; //$NON-NLS-1$
		viewMenu.add(new Separator(filteringGroup));
		MenuManager mgr= new MenuManager(SearchMessages.getString("JavaSearchResultPage.filter.submenu")); //$NON-NLS-1$
		mgr.add(fFilterImportsAction);
		mgr.add(fFilterJavadocAction);
		mgr.add(fFilterReadsAction);
		mgr.add(fFilterWritesAction);
		
		viewMenu.appendToGroup(filteringGroup, mgr);

	}

	private void addSortActions(IMenuManager mgr) {
		if (getLayout() != FLAG_LAYOUT_FLAT)
			return;
		MenuManager sortMenu= new MenuManager(SearchMessages.getString("JavaSearchResultPage.sortBylabel")); //$NON-NLS-1$
		sortMenu.add(fSortByNameAction);
		sortMenu.add(fSortByPathAction);
		sortMenu.add(fSortByParentName);
		
		fSortByNameAction.setChecked(fCurrentSortOrder == fSortByNameAction.getSortOrder());
		fSortByPathAction.setChecked(fCurrentSortOrder == fSortByPathAction.getSortOrder());
		fSortByParentName.setChecked(fCurrentSortOrder == fSortByParentName.getSortOrder());
		
		mgr.appendToGroup(IContextMenuConstants.GROUP_VIEWER_SETUP, sortMenu);
	}
	
	protected void fillToolbar(IToolBarManager tbm) {
		super.fillToolbar(tbm);
		if (getLayout() != FLAG_LAYOUT_FLAT)
			addGroupActions(tbm);
	}
		
	private void addGroupActions(IToolBarManager mgr) {
		mgr.appendToGroup(IContextMenuConstants.GROUP_VIEWER_SETUP, fGroupProjectAction);
		mgr.appendToGroup(IContextMenuConstants.GROUP_VIEWER_SETUP, fGroupPackageAction);
		mgr.appendToGroup(IContextMenuConstants.GROUP_VIEWER_SETUP, fGroupFileAction);
		mgr.appendToGroup(IContextMenuConstants.GROUP_VIEWER_SETUP, fGroupTypeAction);
		
		updateGroupingActions();
	}


	private void updateGroupingActions() {
		fGroupProjectAction.setChecked(fCurrentGrouping == LevelTreeContentProvider.LEVEL_PROJECT);
		fGroupPackageAction.setChecked(fCurrentGrouping == LevelTreeContentProvider.LEVEL_PACKAGE);
		fGroupFileAction.setChecked(fCurrentGrouping == LevelTreeContentProvider.LEVEL_FILE);
		fGroupTypeAction.setChecked(fCurrentGrouping == LevelTreeContentProvider.LEVEL_TYPE);
	}


	public void dispose() {
		fActionGroup.dispose();
		super.dispose();
	}
	
	protected void elementsChanged(Object[] objects) {
		if (fContentProvider != null)
			fContentProvider.elementsChanged(objects);
	}

	protected void clear() {
		if (fContentProvider != null)
			fContentProvider.clear();
	}

	protected void configureTableViewer(TableViewer viewer) {
		viewer.setLabelProvider(new DecoratingLabelProvider(new SortingLabelProvider(this), PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator()));
		fContentProvider=new JavaSearchTableContentProvider(this);
		viewer.setContentProvider(fContentProvider);
		setSortOrder(fCurrentSortOrder);
	}

	protected void configureTreeViewer(TreeViewer viewer) {
		viewer.setSorter(new ViewerSorter());
		viewer.setLabelProvider(new DecoratingLabelProvider(new PostfixLabelProvider(this), PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator()));
		fContentProvider= new LevelTreeContentProvider(this, fCurrentGrouping);
		viewer.setContentProvider(fContentProvider);
	}
	
	protected TreeViewer createTreeViewer(Composite parent) {
		return new ProblemTreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
	}
	
	protected TableViewer createTableViewer(Composite parent) {
		return new ProblemTableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
	}
	
	void setSortOrder(int order) {
		fCurrentSortOrder= order;
		StructuredViewer viewer= getViewer();
		viewer.getControl().setRedraw(false);
		DecoratingLabelProvider dlp= (DecoratingLabelProvider) viewer.getLabelProvider();
		((SortingLabelProvider)dlp.getLabelProvider()).setOrder(order);
		if (order == SortingLabelProvider.SHOW_ELEMENT_CONTAINER) {
			viewer.setSorter(new NameSorter());
		} else if (order == SortingLabelProvider.SHOW_PATH) {
			viewer.setSorter(new PathSorter());
		} else
			viewer.setSorter(new ParentSorter());
		viewer.getControl().setRedraw(true);
		getSettings().put(KEY_SORTING, fCurrentSortOrder);
	}

	public void init(IPageSite site) {
		super.init(site);
		fActionGroup.fillActionBars(site.getActionBars());
		addFilterActions(site.getActionBars().getMenuManager());
	}

	/**
	 * Precondition here: the viewer must be showing a tree with a LevelContentProvider.
	 * @param order
	 */
	void setGrouping(int grouping) {
		fCurrentGrouping= grouping;
		StructuredViewer viewer= getViewer();
		LevelTreeContentProvider cp= (LevelTreeContentProvider) viewer.getContentProvider();
		cp.setLevel(grouping);
		updateGroupingActions();
		getSettings().put(KEY_GROUPING, fCurrentGrouping);
	}
	
	protected StructuredViewer getViewer() {
		// override so that it's visible in the package.
		return super.getViewer();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#restoreState(org.eclipse.ui.IMemento)
	 */
	public void restoreState(IMemento memento) {
		super.restoreState(memento);
		try {
			fCurrentSortOrder= getSettings().getInt(KEY_SORTING);
		} catch (NumberFormatException e) {
			fCurrentSortOrder=  SortingLabelProvider.SHOW_ELEMENT_CONTAINER;
		}
		try {
			fCurrentGrouping= getSettings().getInt(KEY_GROUPING);
		} catch (NumberFormatException e) {
			fCurrentGrouping= LevelTreeContentProvider.LEVEL_PACKAGE;
		}
		if (memento != null) {
			Integer value= memento.getInteger(KEY_GROUPING);
			if (value != null)
				fCurrentGrouping= value.intValue();
			value= memento.getInteger(KEY_SORTING);
			if (value != null)
				fCurrentSortOrder= value.intValue();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#saveState(org.eclipse.ui.IMemento)
	 */
	public void saveState(IMemento memento) {
		super.saveState(memento);
		memento.putInteger(KEY_GROUPING, fCurrentGrouping);
		memento.putInteger(KEY_SORTING, fCurrentSortOrder);
	}
	
	void addMatchFilter(MatchFilter filter) {
		fMatchFilters.add(filter);
		filtersChanged();
	}
	
	void removeMatchFilter(MatchFilter filter) {
		fMatchFilters.remove(filter);
		filtersChanged();
	}
	
	private void filtersChanged() {
		StructuredViewer viewer= getViewer();
		JavaSearchContentProvider cp= (JavaSearchContentProvider) viewer.getContentProvider();
		cp.filtersChanged(getMatchFilters());
		
		fFilterImportsAction.updateCheckState();
		fFilterReadsAction.updateCheckState();
		fFilterWritesAction.updateCheckState();
		fFilterJavadocAction.updateCheckState();
		
		getSite().getActionBars().updateActionBars();
		getSite().getActionBars().getMenuManager().updateAll(true);
		getViewPart().updateLabel();
	}

	boolean hasMatchFilter(MatchFilter filter) {
		return fMatchFilters.contains(filter);
	}
	
	MatchFilter[] getMatchFilters() {
		MatchFilter[] filters= new MatchFilter[fMatchFilters.size()];
		return (MatchFilter[]) fMatchFilters.toArray(filters);
	}
	
	public int getDisplayedMatchCount(Object element) {
		if (getMatchFilters().length == 0)
			return super.getDisplayedMatchCount(element);
		Match[] matches= super.getDisplayedMatches(element);
		int count= 0;
		for (int i= 0; i < matches.length; i++) {
			if (!isFiltered(matches[i]))
				count++;
		}
		return count;
	}
	
	public Match[] getDisplayedMatches(Object element) {
		if (getMatchFilters().length == 0)
			return super.getDisplayedMatches(element);
		Match[] matches= super.getDisplayedMatches(element);
		int count= 0;
		for (int i= 0; i < matches.length; i++) {
			if (isFiltered(matches[i]))
				matches[i]= null;
			else 
				count++;
		}
		Match[] filteredMatches= new Match[count];
		
		int writeIndex= 0;
		for (int i= 0; i < matches.length; i++) {
			if (matches[i] != null)
				filteredMatches[writeIndex++]= matches[i];
		}
		
		return filteredMatches;
	}
	
	private boolean isFiltered(Match match) {
		MatchFilter[] filters= getMatchFilters();
		for (int j= 0; j < filters.length; j++) {
			if (filters[j].filters((JavaElementMatch) match))
				return true;
		}
		return false;

	}

	public void setInput(ISearchResult search, Object viewState) {
		updateFilterEnablement((JavaSearchResult)search);
		super.setInput(search, viewState);
	}

	/**
	 * @param result
	 */
	private void updateFilterEnablement(JavaSearchResult result) {
		fFilterImportsAction.setEnabled(shouldEnable(result, fFilterImportsAction));
		fFilterReadsAction.setEnabled(shouldEnable(result, fFilterReadsAction));
		fFilterWritesAction.setEnabled(shouldEnable(result, fFilterWritesAction));
		fFilterJavadocAction.setEnabled(shouldEnable(result, fFilterJavadocAction));
	}

	private boolean shouldEnable(JavaSearchResult result, FilterAction filterAction) {
		if (result == null) {
			return false;
		}
		JavaSearchQuery query= (JavaSearchQuery) result.getQuery();
		if (query == null)
			return false;
		return filterAction.getFilter().isApplicable(query);
	}
	
	private boolean isQueryRunning() {
		AbstractTextSearchResult result= getInput();
		if (result != null) {
			return NewSearchUI.isQueryRunning(result.getQuery());
		}
		return false;
	}

	public String getLabel() {
		String label= super.getLabel();
		if (getMatchFilters().length > 0) {
			if (isQueryRunning()) {
				String message= SearchMessages.getString("JavaSearchResultPage.filtered.message"); //$NON-NLS-1$
				return MessageFormat.format(message, new Object[] { label });
			
			} else {
				String message= SearchMessages.getString("JavaSearchResultPage.filteredWithCount.message"); //$NON-NLS-1$
				return MessageFormat.format(message, new Object[] { label, new Integer(getFilteredMatchCount()) });
			}
		}
		return label;
	}

	private int getFilteredMatchCount() {
		AbstractTextSearchResult result= getInput();
		if (result == null)
			return 0;
		Object[] elements= result.getElements();
		int count= 0;
		for (int i= 0; i < elements.length; i++) {
			count+= getDisplayedMatchCount(elements[i]);
		}
		return count;
	}
}

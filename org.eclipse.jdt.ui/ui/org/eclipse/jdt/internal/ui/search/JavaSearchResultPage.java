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
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.ProblemTableViewer;
import org.eclipse.jdt.internal.ui.viewsupport.ProblemTreeViewer;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.search.IMatchPresentation;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.search.ui.IContextMenuConstants;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.ISearchResultViewPart;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.texteditor.ITextEditor;

public class JavaSearchResultPage extends AbstractTextSearchViewPage implements IAdaptable {
	private static final int DEFAULT_ELEMENT_LIMIT = 1000;
	private static final String FALSE = "FALSE"; //$NON-NLS-1$
	private static final String TRUE = "TRUE"; //$NON-NLS-1$
	private static final String KEY_GROUPING= "org.eclipse.jdt.search.resultpage.grouping"; //$NON-NLS-1$
	private static final String KEY_SORTING= "org.eclipse.jdt.search.resultpage.sorting"; //$NON-NLS-1$
	private static final String KEY_FILTERS= "org.eclipse.jdt.search.resultpage.filters"; //$NON-NLS-1$
	private static final String KEY_LIMIT_ENABLED= "org.eclipse.jdt.search.resultpage.limit_enabled"; //$NON-NLS-1$
	private static final String KEY_LIMIT= "org.eclipse.jdt.search.resultpage.limit"; //$NON-NLS-1$
	
	private static final String GROUP_GROUPING= "org.eclipse.jdt.search.resultpage.grouping"; //$NON-NLS-1$
	private static final String GROUP_FILTERING = "org.eclipse.jdt.search.resultpage.filtering"; //$NON-NLS-1$
	
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
	private FilterAction[] fFilterActions;
	private FiltersDialogAction fFilterDialogAction;
	
	private static final String[] SHOW_IN_TARGETS= new String[] { JavaUI.ID_PACKAGES , IPageLayout.ID_RES_NAV };
	public static final IShowInTargetList SHOW_IN_TARGET_LIST= new IShowInTargetList() {
		public String[] getShowInTargetIds() {
			return SHOW_IN_TARGETS;
		}
	};
	
	private JavaSearchEditorOpener fEditorOpener= new JavaSearchEditorOpener();
	private boolean fLimitElements= false;
	private int fElementLimit;

	public JavaSearchResultPage() {
		initSortActions();
		initGroupingActions();
		initFilterActions();

	}
	
	private void initFilterActions() {
		MatchFilter[] allFilters= MatchFilter.allFilters();
		fFilterActions= new FilterAction[allFilters.length];
		for (int i= 0; i < fFilterActions.length; i++) {
			fFilterActions[i]= new FilterAction(this, allFilters[i]);
			fFilterActions[i].setId("org.eclipse.jdt.search.filters."+i); //$NON-NLS-1$
		}
		fFilterDialogAction= new FiltersDialogAction(this);
		fFilterDialogAction.setId("org.eclipse.jdt.search.filters."+allFilters.length); //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(fFilterDialogAction, "filter_ps.gif"); //$NON-NLS-1$
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
		JavaPluginImages.setLocalImageDescriptors(fGroupTypeAction, "type_mode.gif"); //$NON-NLS-1$
	}

	public void setViewPart(ISearchResultViewPart part) {
		super.setViewPart(part);
		fActionGroup= new NewSearchViewActionGroup(part);
	}
	
	public void showMatch(Match match, int offset, int length, boolean activate) throws PartInitException {
		IEditorPart editor;
		try {
			editor= fEditorOpener.open(match);
		} catch (JavaModelException e) {
			throw new PartInitException(e.getStatus());
		}
		
		if (editor != null && activate)
			editor.getEditorSite().getPage().activate(editor);
		Object element= match.getElement();
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
				participant.showMatch(match, offset, length, activate);
		}
	}
	
	private void showWithMarker(IEditorPart editor, IFile file, int offset, int length) throws PartInitException {
		try {
			IMarker marker= file.createMarker(NewSearchUI.SEARCH_MARKER);
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
		mgr.appendToGroup(IContextMenuConstants.GROUP_VIEWER_SETUP, new Separator(GROUP_GROUPING));
		mgr.appendToGroup(GROUP_GROUPING, fGroupProjectAction);
		mgr.appendToGroup(GROUP_GROUPING, fGroupPackageAction);
		mgr.appendToGroup(GROUP_GROUPING, fGroupFileAction);
		mgr.appendToGroup(GROUP_GROUPING, fGroupTypeAction);
		
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
		viewer.setUseHashlookup(true);
		viewer.setLabelProvider(new ColorDecoratingLabelProvider(new SortingLabelProvider(this), PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator()));
		fContentProvider=new JavaSearchTableContentProvider(this);
		viewer.setContentProvider(fContentProvider);
		setSortOrder(fCurrentSortOrder);
	}

	protected void configureTreeViewer(TreeViewer viewer) {
		viewer.setUseHashlookup(true);
		viewer.setSorter(new ViewerSorter());
		viewer.setLabelProvider(new ColorDecoratingLabelProvider(new PostfixLabelProvider(this), PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator()));
		fContentProvider= new LevelTreeContentProvider(this, fCurrentGrouping);
		viewer.setContentProvider(fContentProvider);
	}
	
	protected TreeViewer createTreeViewer(Composite parent) {
		return new ProblemTreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL) {
			public void add(Object parentElement, Object[] childElements) {
				if (limitElements() && parentElement.equals(getInput())) {
					int elementLimit= getElementLimit();
					Widget parentWidget= findItem(parentElement);
					if (parentWidget == null)
						return;
					Item[] children= getChildren(parentWidget);
					if (children.length >= elementLimit)
						return;
					if (children.length + childElements.length <= elementLimit) {
						super.add(parentElement, childElements);
						return;
					}
					int toAdd= elementLimit-children.length;
					Object[] limited= new Object[toAdd];
					System.arraycopy(childElements, 0, limited, 0, limited.length);
					super.add(parentElement, limited);
					return;
				} else {
					super.add(parentElement, childElements);
				}
			}

			protected Object[] getFilteredChildren(Object parentElement) {
				if (parentElement == null)
					return new Object[0];
				Object[] filtered= super.getFilteredChildren(parentElement);
				int elementLimit = getElementLimit();
				if (limitElements() && parentElement.equals(getInput()) &&  filtered.length > elementLimit) {
					Object[] limited= new Object[elementLimit];
					System.arraycopy(filtered, 0, limited, 0, limited.length);
					return limited;
				} else 
					return filtered;
			}
		};
	}
	
	int getElementLimit() {
		return fElementLimit;
	}

	protected TableViewer createTableViewer(Composite parent) {
		return new ProblemTableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL) {
			protected void handleLabelProviderChanged(LabelProviderChangedEvent event) {
				getTable().setRedraw(false);
				try {
					super.handleLabelProviderChanged(event);
				} finally {
					getTable().setRedraw(true);
				}
			}
			
			public void add(Object[] elements) {
				if (limitElements()) {
					int elementLimit= getElementLimit();
					int currentCount= getTable().getItemCount();
					if (currentCount >= elementLimit)
						return;
					if (currentCount + elements.length <= elementLimit) {
						super.add(elements);
						return;
					}
					int toAdd= elementLimit-currentCount;
					Object[] limited= new Object[toAdd];
					System.arraycopy(elements, 0, limited, 0, limited.length);
					super.add(limited);
					return;
				} else {
					super.add(elements);
				}
			}

			protected Object[] getFilteredChildren(Object parentElement) {
				if (parentElement == null)
					return new Object[0];
				Object[] filtered= super.getFilteredChildren(parentElement);
				int elementLimit = getElementLimit();
				if (limitElements() && parentElement.equals(getInput()) &&  filtered.length > elementLimit) {
					Object[] limited= new Object[elementLimit];
					System.arraycopy(filtered, 0, limited, 0, limited.length);
					return limited;
				} else 
					return filtered;
			}
		};
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
		IMenuManager menuManager = site.getActionBars().getMenuManager();
		menuManager.insertBefore(IContextMenuConstants.GROUP_PROPERTIES, new Separator(GROUP_FILTERING));
		fActionGroup.fillActionBars(site.getActionBars());
		menuManager.appendToGroup(GROUP_FILTERING, fFilterDialogAction);
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
		String encodedFilters= getSettings().get(KEY_FILTERS);
		restoreFilters(encodedFilters);
		fLimitElements= !FALSE.equals(getSettings().get(KEY_LIMIT_ENABLED));
		try {
			fElementLimit= getSettings().getInt(KEY_LIMIT);
		} catch (NumberFormatException e) {
			fElementLimit= DEFAULT_ELEMENT_LIMIT;
		}
		if (memento != null) {
			Integer value= memento.getInteger(KEY_GROUPING);
			if (value != null)
				fCurrentGrouping= value.intValue();
			value= memento.getInteger(KEY_SORTING);
			if (value != null)
				fCurrentSortOrder= value.intValue();
			encodedFilters= memento.getString(KEY_FILTERS);
			restoreFilters(encodedFilters);
			fLimitElements= !FALSE.equals(memento.getString(KEY_LIMIT_ENABLED));
			value= memento.getInteger(KEY_LIMIT);
			if (value != null)
				fElementLimit= value.intValue();
		}
	}
	
	private void restoreFilters(String encodedFilters) {
		if (encodedFilters != null) {
			fMatchFilters.clear();
			String[] decodedFilters= decodeFiltersString(encodedFilters);
			for (int i= 0; i < decodedFilters.length; i++) {
				MatchFilter filter= findMatchFilter(decodedFilters[i]);
				if (filter != null)
					fMatchFilters.add(filter);
			}
		}
		updateFilterActions();
	}

	private MatchFilter findMatchFilter(String id) {
		MatchFilter[] allFilters= MatchFilter.allFilters();
		for (int i= 0; i < allFilters.length; i++) {
			if (allFilters[i].getID().equals(id))
				return allFilters[i];
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#saveState(org.eclipse.ui.IMemento)
	 */
	public void saveState(IMemento memento) {
		super.saveState(memento);
		memento.putInteger(KEY_GROUPING, fCurrentGrouping);
		memento.putInteger(KEY_SORTING, fCurrentSortOrder);
		memento.putString(KEY_FILTERS, encodeFilters());
		if (fLimitElements)
			memento.putString(KEY_LIMIT_ENABLED, TRUE);
		else 
			memento.putString(KEY_LIMIT_ENABLED, FALSE);
		memento.putInteger(KEY_LIMIT, getElementLimit());
	}
	
	void addMatchFilter(MatchFilter filter) {
		fMatchFilters.add(filter);
		filtersChanged();
	}
	
	void enableLimit(boolean enable) {
		fLimitElements= enable;
		if (fLimitElements)
			getSettings().put(KEY_GROUPING, TRUE);
		else 
			getSettings().put(KEY_LIMIT_ENABLED, FALSE);
		limitChanged();
	}
	
	/**
	 * 
	 */
	private void limitChanged() {
		getViewer().refresh();
		getViewPart().updateLabel();
	}

	boolean limitElements() {
		return fLimitElements;
	}
	
	void removeMatchFilter(MatchFilter filter) {
		fMatchFilters.remove(filter);
		filtersChanged();
	}
	
	private void filtersChanged() {
		StructuredViewer viewer= getViewer();
		JavaSearchContentProvider cp= (JavaSearchContentProvider) viewer.getContentProvider();
		cp.filtersChanged(getMatchFilters());
		
		updateFilterActions();
		getViewPart().updateLabel();
		getSettings().put(KEY_FILTERS, encodeFilters());
	}

	private void updateFilterActions() {
		IMenuManager menu= getSite().getActionBars().getMenuManager();
		
		for (int i= 0; i < fFilterActions.length; i++) {
			fFilterActions[i].updateCheckState();
		}
		
		getSite().getActionBars().updateActionBars();
		menu.updateAll(true);
	}

	private String encodeFilters() {
		StringBuffer buf= new StringBuffer();
		MatchFilter[] enabledFilters= getMatchFilters();
		buf.append(enabledFilters.length);
		for (int i= 0; i < enabledFilters.length; i++) {
			buf.append(';');
			buf.append(enabledFilters[i].getID());
		}
		return buf.toString();
	}
	
	private String[] decodeFiltersString(String encodedString) {
		StringTokenizer tokenizer= new StringTokenizer(encodedString, ";"); //$NON-NLS-1$
		int count= Integer.valueOf(tokenizer.nextToken()).intValue();
		String[] ids= new String[count];
		for (int i= 0; i < count; i++) {
			ids[i]= tokenizer.nextToken();
		}
		return ids;
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
			if ((match instanceof JavaElementMatch) && filters[j].filters((JavaElementMatch) match))
				return true;
		}
		return false;

	}

	public void setInput(ISearchResult search, Object viewState) {
		updateFilterEnablement((JavaSearchResult)search);
		super.setInput(search, viewState);
	}

	private void updateFilterEnablement(JavaSearchResult result) {
		IActionBars bars= getSite().getActionBars();
		IMenuManager menu= bars.getMenuManager();
		for (int i= 0; i < fFilterActions.length; i++) {
			menu.remove(fFilterActions[i].getId());
		}

		for (int i= fFilterActions.length-1; i >= 0 ; i--) {
			if (shouldEnable(result, fFilterActions[i]))
				menu.prependToGroup(GROUP_FILTERING, fFilterActions[i]);
		}
		
		menu.updateAll(true);
		bars.updateActionBars();
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
		int matchFilterCount= getMatchFiltersCount();
		int filteredMatchCount = getFilteredMatchCount();
		if (getInput() != null &&  (matchFilterCount > 0 || filteredMatchCount < getInput().getMatchCount())) {
			if (isQueryRunning()) {
				String message= SearchMessages.getString("JavaSearchResultPage.filtered.message"); //$NON-NLS-1$
				return MessageFormat.format(message, new Object[] { label });
			
			} else {
				String message= SearchMessages.getString("JavaSearchResultPage.filteredWithCount.message"); //$NON-NLS-1$
				return MessageFormat.format(message, new Object[] { label, new Integer(filteredMatchCount) });
			}
		}
		return label;
	}

	private int getMatchFiltersCount() {
		MatchFilter[] filters= getMatchFilters();
		AbstractTextSearchResult result= getInput();
		if (result == null)
			return 0;
		int filterCount= 0;
		for (int i= 0; i < filters.length; i++) {
			if (filters[i].isApplicable((JavaSearchQuery) result.getQuery()))
				filterCount++;
		}
		return filterCount;
	}

	private int getFilteredMatchCount() {
		IContentProvider cp= getViewer().getContentProvider();
		if (cp instanceof ITreeContentProvider) {
			ITreeContentProvider tp= (ITreeContentProvider) cp;
			return getMatchCount(tp, getRootElements((TreeViewer) getViewer()));
		} else {
			return getMatchCount((IStructuredContentProvider) cp);
		}
	}
	
	private Object[] getRootElements(TreeViewer viewer) {
		Tree t= viewer.getTree();
		Item[] roots= t.getItems();
		Object[] elements= new Object[roots.length];
		for (int i = 0; i < elements.length; i++) {
			elements[i]= roots[i].getData();
		}
		return elements;
	}

	private int getMatchCount(ITreeContentProvider cp, Object[] elements) {
		int count= 0;
		for (int j = 0; j < elements.length; j++) {
			count+= getDisplayedMatchCount(elements[j]);
			Object[] children = cp.getChildren(elements[j]);
			count+= getMatchCount(cp, children);
		}
		return count;
	}
	
	private int getMatchCount(IStructuredContentProvider cp) {
		Object[] elements= cp.getElements(getInput());
		int count= 0;
		for (int i = 0; i < elements.length; i++) {
			count+= getDisplayedMatchCount(elements[i]);
		}
		return count;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (IShowInTargetList.class.equals(adapter)) {
			return SHOW_IN_TARGET_LIST;
		}
		return null;
	}
	
	protected void handleOpen(OpenEvent event) {
		Object firstElement= ((IStructuredSelection)event.getSelection()).getFirstElement();
		if (firstElement instanceof ICompilationUnit || 
				firstElement instanceof IClassFile || 
				firstElement instanceof IMember) {
			if (getDisplayedMatchCount(firstElement) == 0) {
				fActionGroup.handleOpen(event);
				return;
			}
		}
		super.handleOpen(event);
	}

	public void setFilters(MatchFilter[] enabledFilters) {
		fMatchFilters.clear();
		for (int i = 0; i < enabledFilters.length; i++) {
			fMatchFilters.add(enabledFilters[i]);
		}
		filtersChanged();
	}

	void setElementLimit(int elementLimit) {
		fElementLimit= elementLimit;
		getSettings().put(KEY_LIMIT, elementLimit);
		limitChanged();
	}
	
}

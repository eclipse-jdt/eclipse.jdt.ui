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
package org.eclipse.jdt.ui.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.actions.ActionGroup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.filters.CustomFiltersDialog;
import org.eclipse.jdt.internal.ui.filters.FilterDescriptor;
import org.eclipse.jdt.internal.ui.filters.FilterMessages;
import org.eclipse.jdt.internal.ui.filters.NamePatternFilter;

/**
 * Action group to add the filter action to a view part's toolbar
 * menu.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class CustomFiltersActionGroup extends ActionGroup {

	private static final String TAG_CUSTOM_FILTERS = "customFilters"; //$NON-NLS-1$
	private static final String TAG_USER_DEFINED_PATTERNS_ENABLED= "userDefinedPatternsEnabled"; //$NON-NLS-1$
	private static final String TAG_USER_DEFINED_PATTERNS= "userDefinedPatterns"; //$NON-NLS-1$
	private static final String TAG_XML_DEFINED_FILTERS= "xmlDefinedFilters"; //$NON-NLS-1$

	private static final String TAG_CHILD= "child"; //$NON-NLS-1$
	private static final String TAG_PATTERN= "pattern"; //$NON-NLS-1$
	private static final String TAG_FILTER_ID= "filterId"; //$NON-NLS-1$
	private static final String TAG_IS_ENABLED= "isEnabled"; //$NON-NLS-1$

	private static final String SEPARATOR= ",";  //$NON-NLS-1$

	private IViewPart fPart;
	private StructuredViewer fViewer;

	private NamePatternFilter fPatternFilter;
	private Map fInstalledBuiltInFilters;
	
	private Map fEnabledFilterIds;
	private boolean fUserDefinedPatternsEnabled;
	private String[] fUserDefinedPatterns;

	/**
	 * Creates a new <code>CustomFiltersActionGroup</code>.
	 * 
	 * @param part		the view part that owns this action group
	 * @param viewer	the viewer to be filtered
	 */
	public CustomFiltersActionGroup(IViewPart part, StructuredViewer viewer) {
		Assert.isNotNull(part);
		Assert.isNotNull(viewer);
		fPart= part;
		fViewer= viewer;
		
		initializeWithPluginContributions();
		initializeWithViewDefaults();
		
		installFilters();
	}

	/* (non-Javadoc)
	 * Method declared on ActionGroup.
	 */
	public void fillActionBars(IActionBars actionBars) {
		fillToolBar(actionBars.getToolBarManager());
		fillViewMenu(actionBars.getMenuManager());
	}

	private String[] getEnabledFilterIds() {
		Set enabledFilterIds= new HashSet(fEnabledFilterIds.size());
		Iterator iter= fEnabledFilterIds.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry= (Map.Entry)iter.next();
			String id= (String)entry.getKey();
			boolean isEnabled= ((Boolean)entry.getValue()).booleanValue();
			if (isEnabled)
				enabledFilterIds.add(id);
		}
		return (String[])enabledFilterIds.toArray(new String[enabledFilterIds.size()]);
	}

	private void setEnabledFilterIds(String[] enabledIds) {
		Iterator iter= fEnabledFilterIds.keySet().iterator();
		while (iter.hasNext()) {
			String id= (String)iter.next();
			fEnabledFilterIds.put(id, Boolean.FALSE);
		}
		for (int i= 0; i < enabledIds.length; i++)
			fEnabledFilterIds.put(enabledIds[i], Boolean.TRUE);
	}

	private void setUserDefinedPatterns(String[] patterns) {
		fUserDefinedPatterns= patterns;
		cleanUpPatternDuplicates();
	}

	private boolean areUserDefinedPatternsEnabled() {
		return fUserDefinedPatternsEnabled;
	}

	private void setUserDefinedPatternsEnabled(boolean state) {
		fUserDefinedPatternsEnabled= state;
	}

	private void fillToolBar(IToolBarManager tooBar) {
	}

	private void fillViewMenu(IMenuManager viewMenu) {
		viewMenu.add(new Separator("filters")); //$NON-NLS-1$
		viewMenu.add(new ContributionItem() {
			public void fill(Menu menu, int index) {
				MenuItem mi= new MenuItem(menu, SWT.NONE, index);
				mi.setText(FilterMessages.getString("OpenCustomFiltersDialogAction.text")); //$NON-NLS-1$
				mi.setSelection(areUserDefinedPatternsEnabled() || getEnabledFilterIds().length > 0);
				mi.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						openDialog();
					}
				});
			}
			public boolean isDynamic() {
				return true;
			}
		});
	}

	/* (non-Javadoc)
	 * Method declared on ActionGroup.
	 */
	public void dispose() {
		super.dispose();
	}
	
	private void initializeWithPluginContributions() {
		fUserDefinedPatterns= new String[0];
		fUserDefinedPatternsEnabled= false;

		String viewId= fPart.getViewSite().getId();		
		FilterDescriptor[] filterDescs= FilterDescriptor.getFilterDescriptors(viewId);
		fEnabledFilterIds= new HashMap(filterDescs.length);
		for (int i= 0; i < filterDescs.length; i++) {
			String id= filterDescs[i].getId();
			Boolean isEnabled= new Boolean(filterDescs[i].isEnabled());
			if (fEnabledFilterIds.containsKey(id))
				JavaPlugin.logErrorMessage("WARNING: Duplicate id for extension-point \"org.eclipse.jdt.ui.javaElementFilters\""); //$NON-NLS-1$
			fEnabledFilterIds.put(id, isEnabled);
		}
	}

	// ---------- viewer filter handling ----------
	
	private void installFilters() {
		fInstalledBuiltInFilters= new HashMap(fEnabledFilterIds.size());
		fPatternFilter= new NamePatternFilter();
		fPatternFilter.setPatterns(getUserAndBuiltInPatterns());
		fViewer.addFilter(fPatternFilter);
		updateBuiltInFilters();
	}

	private void updateViewerFilters(boolean refresh) {
		String[] patterns= getUserAndBuiltInPatterns();
		fPatternFilter.setPatterns(patterns);
		fViewer.getControl().setRedraw(false);
		updateBuiltInFilters();
		if (refresh)
			fViewer.refresh();
		fViewer.getControl().setRedraw(true);
	}
	
	private void updateBuiltInFilters() {
		Set installedFilters= fInstalledBuiltInFilters.keySet();
		Set filtersToAdd= new HashSet(fEnabledFilterIds.size());
		Set filtersToRemove= new HashSet(fEnabledFilterIds.size());
		Iterator iter= fEnabledFilterIds.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry= (Map.Entry)iter.next();
			String id= (String)entry.getKey();
			boolean isEnabled= ((Boolean)entry.getValue()).booleanValue();
			if (isEnabled && !installedFilters.contains(id))
				filtersToAdd.add(id);
			else if (!isEnabled && installedFilters.contains(id))
				filtersToRemove.add(id);
		}
		
		// Install the filters
		String viewId= fPart.getViewSite().getId();
		FilterDescriptor[] filterDescs= FilterDescriptor.getFilterDescriptors(viewId);
		for (int i= 0; i < filterDescs.length; i++) {
			String id= filterDescs[i].getId();
			// just to double check - id should denote a custom filter anyway
			boolean isCustomFilter= filterDescs[i].isCustomFilter();
			if (isCustomFilter) {
				if (filtersToAdd.contains(id)) {
					ViewerFilter filter= filterDescs[i].createViewerFilter();
					if (filter != null) {
						fViewer.addFilter(filter);
						fInstalledBuiltInFilters.put(id, filter);
					}
				}
				if (filtersToRemove.contains(id)) {
					fViewer.removeFilter((ViewerFilter)fInstalledBuiltInFilters.get(id));
					fInstalledBuiltInFilters.remove(id);
				}
			}
		}
	}

	private String[] getUserAndBuiltInPatterns() {
		String viewId= fPart.getViewSite().getId();
		List patterns= new ArrayList(fUserDefinedPatterns.length);
		if (areUserDefinedPatternsEnabled())
			patterns.addAll(Arrays.asList(fUserDefinedPatterns));
		FilterDescriptor[] filterDescs= FilterDescriptor.getFilterDescriptors(viewId);
		for (int i= 0; i < filterDescs.length; i++) {
			String id= filterDescs[i].getId();
			boolean isPatternFilter= filterDescs[i].isPatternFilter();
			Object isEnabled= fEnabledFilterIds.get(id);
			if (isEnabled != null && isPatternFilter && ((Boolean)isEnabled).booleanValue())
				patterns.add(filterDescs[i].getPattern());
		}
		return (String[])patterns.toArray(new String[patterns.size()]);
	}

	// ---------- view kind/defaults persistency ----------
		
	private void initializeWithViewDefaults() {
		// get default values for view
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();

		// XXX: can be removed once bug 22533 is fixed.
		if (!store.contains(getPreferenceKey("TAG_DUMMY_TO_TEST_EXISTENCE")))//$NON-NLS-1$
			return;

		// XXX: Uncomment once bug 22533 is fixed.
//		if (!store.contains(getPreferenceKey(TAG_USER_DEFINED_PATTERNS_ENABLED)))
//			return;
		
		fUserDefinedPatternsEnabled= store.getBoolean(getPreferenceKey(TAG_USER_DEFINED_PATTERNS_ENABLED));
		setUserDefinedPatterns(CustomFiltersDialog.convertFromString(store.getString(getPreferenceKey(TAG_USER_DEFINED_PATTERNS)), SEPARATOR));

		Iterator iter= fEnabledFilterIds.keySet().iterator();
		while (iter.hasNext()) {
			String id= (String)iter.next();
			Boolean isEnabled= new Boolean(store.getBoolean(id));
			fEnabledFilterIds.put(id, isEnabled);
		}
		
	}

	private void storeViewDefaults() {
		// get default values for view
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();

		// XXX: can be removed once bug 22533 is fixed.
		store.setValue(getPreferenceKey("TAG_DUMMY_TO_TEST_EXISTENCE"), "storedViewPreferences");//$NON-NLS-1$//$NON-NLS-2$
		
		store.setValue(getPreferenceKey(TAG_USER_DEFINED_PATTERNS_ENABLED), fUserDefinedPatternsEnabled);
		store.setValue(getPreferenceKey(TAG_USER_DEFINED_PATTERNS), CustomFiltersDialog.convertToString(fUserDefinedPatterns ,SEPARATOR));

		Iterator iter= fEnabledFilterIds.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry= (Map.Entry)iter.next();
			String id= (String)entry.getKey();
			boolean isEnabled= ((Boolean)entry.getValue()).booleanValue();
			store.setValue(id, isEnabled);
		}
	}
	
	private String getPreferenceKey(String tag) {
		return "CustomFiltersActionGroup." + fPart.getViewSite().getId() + '.' + tag; //$NON-NLS-1$
	}

	// ---------- view instance persistency ----------

	/**
	 * Saves the state of the custom filters in a memento.
	 * 
	 * @param memento the memento into which the state is saved
	 */
	public void saveState(IMemento memento) {
		IMemento customFilters= memento.createChild(TAG_CUSTOM_FILTERS);
		customFilters.putString(TAG_USER_DEFINED_PATTERNS_ENABLED, new Boolean(fUserDefinedPatternsEnabled).toString());
		saveUserDefinedPatterns(customFilters);
		saveXmlDefinedPatterns(customFilters);
	}

	private void saveXmlDefinedPatterns(IMemento memento) {
		if(fEnabledFilterIds != null && !fEnabledFilterIds.isEmpty()) {
			IMemento xmlDefinedFilters= memento.createChild(TAG_XML_DEFINED_FILTERS);
			Iterator iter= fEnabledFilterIds.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry entry= (Map.Entry)iter.next();
				String id= (String)entry.getKey();
				Boolean isEnabled= (Boolean)entry.getValue();
				IMemento child= xmlDefinedFilters.createChild(TAG_CHILD);
				child.putString(TAG_FILTER_ID, id);
				child.putString(TAG_IS_ENABLED, isEnabled.toString());
			}
		}
	}

	private void saveUserDefinedPatterns(IMemento memento) {
		if(fUserDefinedPatterns != null && fUserDefinedPatterns.length > 0) {
			IMemento userDefinedPatterns= memento.createChild(TAG_USER_DEFINED_PATTERNS);
			for (int i= 0; i < fUserDefinedPatterns.length; i++) {
				IMemento child= userDefinedPatterns.createChild(TAG_CHILD);
				child.putString(TAG_PATTERN, fUserDefinedPatterns[i]);
			}
		}
	}

	/**
	 * Restores the state of the filter actions from a memento.
	 * <p>
	 * Note: This method does not refresh the viewer.
	 * </p>
	 * 
	 * @param memento the memento from which the state is restored
	 */	
	public void restoreState(IMemento memento) {
		if (memento == null)
			return;
		IMemento customFilters= memento.getChild(TAG_CUSTOM_FILTERS);
		if (customFilters == null)
			return;
		String userDefinedPatternsEnabled= customFilters.getString(TAG_USER_DEFINED_PATTERNS_ENABLED);
		if (userDefinedPatternsEnabled == null)
			return;

		fUserDefinedPatternsEnabled= Boolean.valueOf(userDefinedPatternsEnabled).booleanValue();
		restoreUserDefinedPatterns(customFilters);
		restoreXmlDefinedFilters(customFilters);
		
		updateViewerFilters(false);
	}

	private void restoreUserDefinedPatterns(IMemento memento) {
		IMemento userDefinedPatterns= memento.getChild(TAG_USER_DEFINED_PATTERNS);
		if(userDefinedPatterns != null) {	
			IMemento children[]= userDefinedPatterns.getChildren(TAG_CHILD);
			String[] patterns= new String[children.length];
			for (int i = 0; i < children.length; i++)
				patterns[i]= children[i].getString(TAG_PATTERN);

			setUserDefinedPatterns(patterns);
		} else
			setUserDefinedPatterns(new String[0]);
	}

	private void restoreXmlDefinedFilters(IMemento memento) {
		IMemento xmlDefinedPatterns= memento.getChild(TAG_XML_DEFINED_FILTERS);
		if(xmlDefinedPatterns != null) {
			IMemento[] children= xmlDefinedPatterns.getChildren(TAG_CHILD);
			for (int i= 0; i < children.length; i++) {
				String id= children[i].getString(TAG_FILTER_ID);
				Boolean isEnabled= new Boolean(children[i].getString(TAG_IS_ENABLED));
				fEnabledFilterIds.put(id, isEnabled);
			}
		}
	}

	private void cleanUpPatternDuplicates() {
		if (!areUserDefinedPatternsEnabled())
			return;
		List userDefinedPatterns= new ArrayList(Arrays.asList(fUserDefinedPatterns));
		FilterDescriptor[] filters= FilterDescriptor.getFilterDescriptors(fPart.getViewSite().getId());

		for (int i= 0; i < filters.length; i++) {
			if (filters[i].isPatternFilter()) {
				String pattern= filters[i].getPattern();
				if (userDefinedPatterns.contains(pattern)) {
					fEnabledFilterIds.put(filters[i].getId(), Boolean.TRUE);
					boolean hasMore= true;
					while (hasMore)
						hasMore= userDefinedPatterns.remove(pattern);
				}
			}
		}
		fUserDefinedPatterns= (String[])userDefinedPatterns.toArray(new String[userDefinedPatterns.size()]);
		setUserDefinedPatternsEnabled(fUserDefinedPatternsEnabled && fUserDefinedPatterns.length > 0);
	}
	
	// ---------- dialog related code ----------

	private void openDialog() {
		CustomFiltersDialog dialog= new CustomFiltersDialog(
			fPart.getViewSite().getShell(),
			fPart.getViewSite().getId(),
			areUserDefinedPatternsEnabled(),
			fUserDefinedPatterns,
			getEnabledFilterIds());
		
		if (dialog.open() == Window.OK) {
			setEnabledFilterIds(dialog.getEnabledFilterIds());
			setUserDefinedPatternsEnabled(dialog.areUserDefinedPatternsEnabled());
			setUserDefinedPatterns(dialog.getUserDefinedPatterns());

			storeViewDefaults();

			updateViewerFilters(true);
		}
	}
}

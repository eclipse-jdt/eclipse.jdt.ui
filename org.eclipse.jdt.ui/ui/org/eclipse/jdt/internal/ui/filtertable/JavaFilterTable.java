/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *     Jesper Steen Moller - Enhancement 254677 - filter getters/setters
 *     Zsombor Gegesy - creating a standalone component.
 * Note:
 *     The code was extracted from org.eclipse.jdt.internal.debug.ui.JavaStepFilterPreferencePage
 *     from the eclipse.jdt.debug project.
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.filtertable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TableItem;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.dialogs.PackageSelectionDialog;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Component to manage a list of Java filters, where a filter can be a Java class name, or a package
 * name, and every filter can be active or inactive, and they are stored in the preference store.
 *
 * @since 3.26
 */
public class JavaFilterTable {
	/**
	 * Content provider for the table. Content consists of instances of Filter.
	 *
	 * @since 3.26
	 */
	class FilterContentProvider implements IStructuredContentProvider {
		public FilterContentProvider() {
			initTableState(false);
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return getAllFiltersFromTable();
		}
	}

	/**
	 * Interface for encapsulating the storage of the {@link Filter}s
	 *
	 * @since 3.26
	 *
	 */
	public interface FilterStorage {
		/**
		 * Returns all of the stored filters
		 *
		 * @param defaults if the defaults should be returned
		 * @return an array of stored filters
		 */
		Filter[] getStoredFilters(boolean defaults);

		/**
		 * Saves the filters in a persistent storage.
		 *
		 * @param store the current preference store, or null if not applicable.
		 * @param filters an array of filters, what the user is configured
		 */
		void setStoredFilters(IPreferenceStore store, Filter[] filters);

	}

	public static class ButtonLabel {
		final String label;

		final String tooltip;

		public ButtonLabel(String label, String tooltip) {
			this.label= label;
			this.tooltip= tooltip;
		}

		public ButtonLabel(String label) {
			this(label, null);
		}
	}

	public static class DialogLabels {
		final String title;

		final String message;

		public DialogLabels(String title, String message) {
			this.title= title;
			this.message= message;
		}
	}

	public static class FilterTableConfig {
		String labelText;

		ButtonLabel addFilter;

		ButtonLabel addType;

		ButtonLabel addPackage;

		ButtonLabel editFilter;

		ButtonLabel remove;

		ButtonLabel selectAll;

		ButtonLabel deselectAll;

		DialogLabels addTypeDialog;

		DialogLabels errorAddTypeDialog;

		DialogLabels addPackageDialog;

		String helpContextId;

		boolean showDefaultPackage;
		boolean showParents;
		boolean considerAllTypes;
		boolean checkable = true;

		public FilterTableConfig setLabelText(String labelText) {
			this.labelText= labelText;
			return this;
		}

		public FilterTableConfig setAddFilter(ButtonLabel buttonLabel) {
			this.addFilter= buttonLabel;
			return this;
		}

		public FilterTableConfig setAddType(ButtonLabel buttonLabel) {
			this.addType= buttonLabel;
			return this;
		}

		public FilterTableConfig setEditFilter(ButtonLabel buttonLabel) {
			this.editFilter= buttonLabel;
			return this;
		}

		public FilterTableConfig setAddPackage(ButtonLabel buttonLabel) {
			this.addPackage= buttonLabel;
			return this;
		}

		public FilterTableConfig setRemove(ButtonLabel buttonLabel) {
			this.remove= buttonLabel;
			return this;
		}

		public FilterTableConfig setSelectAll(ButtonLabel buttonLabel) {
			this.selectAll= buttonLabel;
			return this;
		}

		public FilterTableConfig setDeselectAll(ButtonLabel buttonLabel) {
			this.deselectAll= buttonLabel;
			return this;
		}

		public FilterTableConfig setAddTypeDialog(DialogLabels dialog) {
			this.addTypeDialog= dialog;
			return this;
		}

		public FilterTableConfig setErrorAddTypeDialog(DialogLabels dialog) {
			this.errorAddTypeDialog= dialog;
			return this;
		}

		public FilterTableConfig setAddPackageDialog(DialogLabels dialog) {
			this.addPackageDialog= dialog;
			return this;
		}

		public FilterTableConfig setHelpContextId(String helpContextId) {
			this.helpContextId= helpContextId;
			return this;
		}

		public FilterTableConfig setShowDefaultPackage(boolean showDefaultPackage) {
			this.showDefaultPackage= showDefaultPackage;
			return this;
		}

		public FilterTableConfig setShowParents(boolean showParents) {
			this.showParents= showParents;
			return this;
		}

		public FilterTableConfig setConsiderAllTypes(boolean considerAllTypes) {
			this.considerAllTypes= considerAllTypes;
			return this;
		}

		public FilterTableConfig setCheckable(boolean checkable) {
			this.checkable= checkable;
			return this;
		}
	}

	private final FilterStorage fFilterStorage;

	private final FilterTableConfig config;

	private TableViewer fTableViewer;

	private Button fAddPackageButton;

	private Button fAddTypeButton;

	private Button fRemoveFilterButton;

	private Button fAddFilterButton;

	private Button fEditFilterButton;

	private Button fSelectAllButton;

	private Button fDeselectAllButton;

	public JavaFilterTable(PreferencePage preferencePage, FilterManager filterManager, FilterTableConfig config) {
		this(new FilterStorage() {
			@Override
			public Filter[] getStoredFilters(boolean defaults) {
				return filterManager.getAllStoredFilters(preferencePage.getPreferenceStore(), defaults);
			}

			@Override
			public void setStoredFilters(IPreferenceStore store, Filter[] filters) {
				filterManager.save(store, filters);
			}
		}, config);
		Objects.requireNonNull(filterManager);
	}

	public JavaFilterTable(FilterStorage filterStorage, FilterTableConfig config) {
		this.fFilterStorage= Objects.requireNonNull(filterStorage);
		this.config= config != null ? config : new FilterTableConfig();
	}

	public void createTable(Composite container) {
		if (config.labelText != null) {
			SWTFactory.createLabel(container, config.labelText, 2);
		}
		createFilterTableViewer(container);
		createFilterButtons(container);
	}

	private void createFilterTableViewer(Composite container) {
		if (config.checkable) {
			fTableViewer= CheckboxTableViewer.newCheckList(container, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		} else {
			fTableViewer= new TableViewer(container, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		}
		fTableViewer.getTable().setFont(container.getFont());
		fTableViewer.setLabelProvider(new FilterLabelProvider());
		fTableViewer.setComparator(new FilterViewerComparator());
		fTableViewer.setContentProvider(new FilterContentProvider());
		fTableViewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
		if (fTableViewer instanceof CheckboxTableViewer) {
			CheckboxTableViewer checkableViewer = (CheckboxTableViewer) fTableViewer;
			checkableViewer.addCheckStateListener(new ICheckStateListener() {
				@Override
				public void checkStateChanged(CheckStateChangedEvent event) {
					((Filter) event.getElement()).setChecked(event.getChecked());
				}
			});
		}
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				boolean hasSelection= !event.getSelection().isEmpty();
				setEnabled(fRemoveFilterButton, hasSelection);
				setEnabled(fEditFilterButton, hasSelection);
			}
		});
		fTableViewer.getControl().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				handleFilterViewerKeyPress(event);
			}
		});
	}


	/**
	 * Creates the add/remove/etc buttons for the filters
	 *
	 * @param container the parent container
	 */
	private void createFilterButtons(Composite container) {
		// button container
		Composite buttonContainer= new Composite(container, SWT.NONE);
		GridData gd= new GridData(GridData.FILL_VERTICAL);
		buttonContainer.setLayoutData(gd);
		GridLayout buttonLayout= new GridLayout();
		buttonLayout.numColumns= 1;
		buttonLayout.marginHeight= 0;
		buttonLayout.marginWidth= 0;
		buttonContainer.setLayout(buttonLayout);

		//Add filter button
		fAddFilterButton= createPushButton(buttonContainer, config.addFilter, event -> addFilter());
		//Add type button
		fAddTypeButton= createPushButton(buttonContainer, config.addType, event -> addType());
		//Add package button
		fAddPackageButton= createPushButton(buttonContainer, config.addPackage, event -> addPackage());
		//Add edit button
		fEditFilterButton= createPushButton(buttonContainer, config.editFilter, event -> editFilter(), false);
		//Remove button
		fRemoveFilterButton= createPushButton(buttonContainer, config.remove, event -> removeFilters(), false);

		Label separator= new Label(buttonContainer, SWT.NONE);
		separator.setVisible(false);
		gd= new GridData();
		gd.horizontalAlignment= GridData.FILL;
		gd.verticalAlignment= GridData.BEGINNING;
		gd.heightHint= 4;
		separator.setLayoutData(gd);
		if (fTableViewer instanceof CheckboxTableViewer) {
			CheckboxTableViewer checkableViewer = (CheckboxTableViewer) fTableViewer;
			//Select All button
			fSelectAllButton= createPushButton(buttonContainer, config.selectAll, event -> checkableViewer.setAllChecked(true));

			//De-Select All button
			fDeselectAllButton= createPushButton(buttonContainer, config.deselectAll, event -> checkableViewer.setAllChecked(false));
		}

	}

	private static Button createPushButton(Composite parent, ButtonLabel buttonLabels, Listener listener) {
		return createPushButton(parent, buttonLabels, listener, true);
	}

	private static Button createPushButton(Composite parent, ButtonLabel buttonLabels, Listener listener, boolean enabled) {
		if (buttonLabels != null && buttonLabels.label != null && !buttonLabels.label.isBlank()) {
			Button button= SWTFactory.createPushButton(parent, buttonLabels.label, buttonLabels.tooltip, null);
			button.addListener(SWT.Selection, listener);
			button.setEnabled(enabled);
			return button;
		}
		return null;
	}

	private void setEnabled(Button buttonToEnable, boolean enabled) {
		if (buttonToEnable != null) {
			buttonToEnable.setEnabled(enabled);
		}
	}

	/**
	 * handles the filter button being clicked
	 *
	 * @param event the clicked event
	 */
	private void handleFilterViewerKeyPress(KeyEvent event) {
		if (event.character == SWT.DEL && event.stateMask == 0) {
			removeFilters();
		}
	}

	/**
	 * Removes the currently selected filters.
	 */
	protected void removeFilters() {
		fTableViewer.remove(((IStructuredSelection) fTableViewer.getSelection()).toArray());
	}

	/**
	 * Allows a new filter to be added to the listing
	 */
	private void addFilter() {
		List<String> existingEntries= getExistingFilters();
		TypeFilterInputDialog dialog= new TypeFilterInputDialog(fAddFilterButton.getShell(), existingEntries, config.helpContextId);
		if (dialog.open() == Window.OK) {
			String res= (String) dialog.getResult();
			addFilter(res, true);
		}
	}

	private List<String> getExistingFilters() {
		TableItem[] items= fTableViewer.getTable().getItems();
		List<String> existingEntries= new ArrayList<>(items.length);
		for (TableItem item : items) {
			Filter data= (Filter) item.getData();
			existingEntries.add(data.getName());
		}
		return existingEntries;
	}

	/**
	 * Allows a filter to be edited
	 */
	private void editFilter() {
		List<Filter> selected= ((IStructuredSelection) fTableViewer.getSelection()).toList();
		if (selected.isEmpty()) {
			return;
		}
		Filter editedEntry= selected.get(0);
		List<String> existing= getExistingFilters();
		existing.remove(editedEntry.getName());
		TypeFilterInputDialog dialog= new TypeFilterInputDialog(fEditFilterButton.getShell(), existing, config.helpContextId);
		dialog.setInitialString(editedEntry.getName());
		if (dialog.open() == Window.OK) {
			editedEntry.setName((String) dialog.getResult());
			fTableViewer.refresh(editedEntry);
		}
	}

	/**
	 * add a new type to the listing of available filters
	 */
	private void addType() {
		try {
			int searchType = config.considerAllTypes ? IJavaElementSearchConstants.CONSIDER_ALL_TYPES : IJavaElementSearchConstants.CONSIDER_CLASSES;
			SelectionDialog dialog= JavaUI.createTypeDialog(fAddTypeButton.getShell(),
					PlatformUI.getWorkbench().getProgressService(),
					getTypeSearchScope(),
					searchType,
					false);
			apply(config.addTypeDialog, dialog);
			if (dialog.open() == IDialogConstants.OK_ID) {
				Object[] types= dialog.getResult();
				if (types != null && types.length > 0) {
					IType type= (IType) types[0];
					addFilter(type.getFullyQualifiedName(), true);
				}
			}
		} catch (JavaModelException jme) {
			ExceptionHandler.handle(jme, config.errorAddTypeDialog.title, config.errorAddTypeDialog.message);
		}
	}

	/**
	 * add a new package to the list of all available package filters
	 */
	private void addPackage() {
		int packageSelectionFlags = PackageSelectionDialog.F_REMOVE_DUPLICATES;
		if (!config.showDefaultPackage) {
			packageSelectionFlags |= PackageSelectionDialog.F_HIDE_DEFAULT_PACKAGE;
		}
		if (config.showParents) {
			packageSelectionFlags |= PackageSelectionDialog.F_SHOW_PARENTS;
		}
		SelectionDialog dialog= JavaUI.createPackageDialog(
				fAddPackageButton.getShell(),
				true,
				packageSelectionFlags,
				""); //$NON-NLS-1$
		apply(config.addPackageDialog, dialog);
		if (dialog.open() == IDialogConstants.OK_ID) {
			Object[] packages= dialog.getResult();
			if (packages != null) {
				IJavaElement pkg= null;
				for (int i= 0; i < packages.length; i++) {
					pkg= (IJavaElement) packages[i];
					String filter= pkg.getElementName() + ".*"; //$NON-NLS-1$
					addFilter(filter, true);
				}
			}
		}
	}

	/**
	 * Enables or disables the buttons and the table control
	 *
	 * @param enabled the new enabled status of the widgets
	 * @since 3.24
	 */
	public void setEnabled(boolean enabled) {
		fAddFilterButton.setEnabled(enabled);
		fAddPackageButton.setEnabled(enabled);
		fAddTypeButton.setEnabled(enabled);
		fDeselectAllButton.setEnabled(enabled);
		fSelectAllButton.setEnabled(enabled);
		fTableViewer.getTable().setEnabled(enabled);
		fRemoveFilterButton.setEnabled(enabled & !fTableViewer.getSelection().isEmpty());
	}

	/**
	 * initializes the checked state of the filters when the dialog opens
	 *
	 * @param defaults if the defaults should be returned
	 * @since 3.24
	 */
	private void initTableState(boolean defaults) {
		Filter[] filters= getAllStoredFilters(defaults);
		for (int i= 0; i < filters.length; i++) {
			fTableViewer.add(filters[i]);
			setChecked(filters[i], filters[i].isChecked());
		}
	}

	private void setChecked(Object element, boolean checked) {
		if (fTableViewer instanceof CheckboxTableViewer) {
			((CheckboxTableViewer) fTableViewer).setChecked(element, checked);
		}
	}

	private void apply(DialogLabels dialogLabels, SelectionDialog dialog) {
		if (dialogLabels != null) {
			dialog.setTitle(dialogLabels.title);
			dialog.setMessage(dialogLabels.message);
		}
	}

	/**
	 * Returns all of the committed filters
	 *
	 * @param defaults if the defaults should be returned
	 * @return an array of committed filters
	 * @since 3.24
	 */
	private Filter[] getAllStoredFilters(boolean defaults) {
		return fFilterStorage.getStoredFilters(defaults);
	}

	/**
	 * adds a single filter to the viewer
	 *
	 * @param filter the new filter to add
	 * @param checked the checked state of the new filter
	 * @since 3.24
	 */
	protected void addFilter(String filter, boolean checked) {
		if (filter != null) {
			Filter filterObj = new Filter(filter, checked);
			for (var item : fTableViewer.getTable().getItems()) {
				var current = (Filter) item.getData();
				if (filterObj.equals(current)) {
					item.setChecked(checked);
					return;
				}
			}
			fTableViewer.add(filterObj);
			setChecked(filterObj, checked);
		}
	}

	/**
	 * returns all of the filters from the table, this includes ones that have not yet been saved
	 *
	 * @return a possibly empty lits of filters fron the table
	 * @since 3.24
	 */
	protected Filter[] getAllFiltersFromTable() {
		TableItem[] items= fTableViewer.getTable().getItems();
		Filter[] filters= new Filter[items.length];
		for (int i= 0; i < items.length; i++) {
			filters[i]= (Filter) items[i].getData();
			filters[i].setChecked(items[i].getChecked());
		}
		return filters;
	}

	/**
	 * @return the search scope for the types, by default it search in the whole workspace.
	 */
	protected IJavaSearchScope getTypeSearchScope() {
		return SearchEngine.createWorkspaceScope();
	}

	/**
	 * Resets the component to it's default state.
	 */
	public void performDefaults() {
		fTableViewer.getTable().removeAll();
		initTableState(true);
	}

	/**
	 * Saves the current state of the table into the underlying {@link FilterStorage}
	 *
	 * @param store the current {@link IPreferenceStore} if the component is inside a preference
	 *            page.
	 */
	public void performOk(IPreferenceStore store) {
		fFilterStorage.setStoredFilters(store, getAllFiltersFromTable());
	}

}

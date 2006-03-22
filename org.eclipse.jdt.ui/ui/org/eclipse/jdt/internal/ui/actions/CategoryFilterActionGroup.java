/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import com.ibm.icu.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.Assert;

import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.dialogs.SelectionStatusDialog;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.CheckedListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;

public class CategoryFilterActionGroup extends ActionGroup {

	private class CategoryFilter extends ViewerFilter {

		/**
		 * {@inheritDoc}
		 */
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof IMember) {
				IMember member= (IMember)element;
				try {
					String[] categories= member.getCategories();
					for (int i= 0; i < categories.length; i++) {
						if (fFilteredCategories.contains(categories[i]))
							return false;
					}
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
				}
			}
			return true;
		}
		
	}
	
	private class CategoryFilterSelectionDialog extends SelectionStatusDialog implements IListAdapter {
		
		private static final int SELECT_ALL= 0;
		private static final int DESELECT_ALL= 1;

		private final CheckedListDialogField fCategoryList;

		public CategoryFilterSelectionDialog(Shell parent, List categories, List selectedCategories) {
			super(parent);
			
			setTitle(ActionMessages.CategoryFilterActionGroup_JavaCategoryFilter_title);
			
			String[] buttons= {
					ActionMessages.CategoryFilterActionGroup_SelectAllCategories, 
					ActionMessages.CategoryFilterActionGroup_DeselectAllCategories
					};
			
			fCategoryList= new CheckedListDialogField(this, buttons, new ILabelProvider() {
							public Image getImage(Object element) {return null;}
							public String getText(Object element) {return (String)element;}
							public void addListener(ILabelProviderListener listener) {}
							public void dispose() {}
							public boolean isLabelProperty(Object element, String property) {return false;}
							public void removeListener(ILabelProviderListener listener) {}
						});
			fCategoryList.addElements(categories);
			fCategoryList.setViewerSorter(new ViewerSorter());
			fCategoryList.setLabelText(ActionMessages.CategoryFilterActionGroup_SelectCategoriesDescription);
			fCategoryList.checkAll(true);
			for (Iterator iter= selectedCategories.iterator(); iter.hasNext();) {
				String selected= (String)iter.next();
				fCategoryList.setChecked(selected, false);
			}
			if (categories.size() == 0) {
				fCategoryList.setEnabled(false);
			}
		}
		
		/**
		 * {@inheritDoc}
		 */
		protected Control createDialogArea(Composite parent) {
			Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			composite.setLayout(new GridLayout(1, true));
			composite.setFont(parent.getFont());
			
			Composite list= new Composite(composite, SWT.NONE);
			list.setFont(composite.getFont());
			LayoutUtil.doDefaultLayout(list, new DialogField[] { fCategoryList }, true);
			LayoutUtil.setHorizontalGrabbing(fCategoryList.getListControl(null));
			Dialog.applyDialogFont(composite);
			
			return composite;
		}

		/**
		 * {@inheritDoc}
		 */
		protected void computeResult() {
			setResult(fCategoryList.getCheckedElements());
		}

		/**
		 * {@inheritDoc}
		 */
		public void customButtonPressed(ListDialogField field, int index) {
			if (index == SELECT_ALL) {
				fCategoryList.checkAll(true);
				fCategoryList.refresh();
			} else if (index == DESELECT_ALL) {
				fCategoryList.checkAll(false);
				fCategoryList.refresh();
			}
		}

		public void selectionChanged(ListDialogField field) {
			List selectedElements= field.getSelectedElements();
			if (selectedElements.size() == 1) {
				Object selected= selectedElements.get(0);
				fCategoryList.setChecked(selected, !fCategoryList.isChecked(selected));
			}
		}
		public void doubleClicked(ListDialogField field) {}
	}
	
	private class CategoryFilterMenuAction extends Action {
		
		private IJavaElement[] fInput;
		
		public CategoryFilterMenuAction(IJavaElement[] input) {
			fInput= input;
			setDescription(ActionMessages.CategoryFilterActionGroup_ShowCategoriesActionDescription); 
			setToolTipText(ActionMessages.CategoryFilterActionGroup_ShowCategoriesToolTip); 
			setText(ActionMessages.CategoryFilterActionGroup_ShowCategoriesLabel);
			JavaPluginImages.setLocalImageDescriptors(this, "category_menu.gif"); //$NON-NLS-1$
		}
		
		/**
		 * {@inheritDoc}
		 */
		public void run() {
			showCategorySelectionDialog(fInput);
		}

		public void setInput(IJavaElement[] input) {
			fInput= input;
		}
	}
		
	private class CategoryFilterAction extends Action {
		
		private final String fCategory;

		public CategoryFilterAction(String category) {
			fCategory= category;
			setText(fCategory);
			setChecked(!fFilteredCategories.contains(fCategory));
			setId(FILTER_CATEGORY_ACTION_ID);
		}

		/**
		 * {@inheritDoc}
		 */
		public void run() {
			super.run();
			if (fFilteredCategories.contains(fCategory)) {
				fFilteredCategories.remove(fCategory);
			} else {
				fFilteredCategories.add(fCategory);
			}
			storeFilteredCategories();
			fireSelectionChange();
		}

	}
	
	private static final String FILTER_CATEGORY_ACTION_ID= "FilterCategoryActionId"; //$NON-NLS-1$
	private static final String CATEGORY_MENU_GROUP_NAME= "CategoryMenuGroup"; //$NON-NLS-1$
	private static final int MAX_NUMBER_OF_CATEGORIES_IN_MENU= 5;

	private final StructuredViewer fViewer;
	private final String fViewerId;
	private final CategoryFilter fFilter;
	private final HashSet fFilteredCategories;
	private IJavaElement[] fInputElement;
	private final CategoryFilterMenuAction fMenuAction;
	private IMenuManager fMenuManager;
	private IMenuListener fMenuListener;

	public CategoryFilterActionGroup(final StructuredViewer viewer, final String viewerId, IJavaElement[] input) {
		Assert.isLegal(viewer != null);
		Assert.isLegal(viewerId != null);
		Assert.isLegal(input != null);
		
		fViewer= viewer;
		fViewerId= viewerId;
		fInputElement= input;
		
		fFilter= new CategoryFilter();
		
		fFilteredCategories= new HashSet();
		loadFilteredCategories();

		fMenuAction= new CategoryFilterMenuAction(input);
		
		fViewer.addFilter(fFilter);
	}
	
	public void setInput(IJavaElement[] input) {
		Assert.isLegal(input != null);
		
		fInputElement= input;
		if (fMenuManager != null) {
			updateMenu(fMenuManager);
		}
	}
	
	private void loadFilteredCategories() {
		fFilteredCategories.clear();
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		String string= store.getString(getPreferenceKey());
		if (string != null) {
			String[] categories= string.split(";"); //$NON-NLS-1$
			for (int i= 0; i < categories.length; i++) {
				fFilteredCategories.add(categories[i]);
			}
		}
	}

	private void storeFilteredCategories() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		if (fFilteredCategories.size() == 0) {
			store.setValue(getPreferenceKey(), ""); //$NON-NLS-1$
		} else {
			StringBuffer buf= new StringBuffer();
			Iterator iter= fFilteredCategories.iterator();
			String element= (String)iter.next();
			buf.append(element);
			while (iter.hasNext()) {
				element= (String)iter.next();
				buf.append(';');
				buf.append(element);
			}
			store.setValue(getPreferenceKey(), buf.toString());
		}
	}
	
	public void contributeToViewMenu(IMenuManager menuManager) {
		menuManager.add(new Separator(CATEGORY_MENU_GROUP_NAME));
		menuManager.appendToGroup(CATEGORY_MENU_GROUP_NAME, fMenuAction);
		fMenuListener= new IMenuListener() {
					public void menuAboutToShow(IMenuManager manager) {
						updateMenu(manager);
					}			
				};
		menuManager.addMenuListener(fMenuListener);
		fMenuManager= menuManager;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		super.dispose();
		if (fMenuManager != null) {
			fMenuManager.removeMenuListener(fMenuListener);
			fMenuManager= null;
			fMenuListener= null;
		}
	}

	private void updateMenu(IMenuManager manager) {
		IContributionItem[] items= manager.getItems();
		if (items != null) {
			for (int i= 0; i < items.length; i++) {
				IContributionItem item= items[i];
				if (item != null && item.getId() != null && item.getId().equals(FILTER_CATEGORY_ACTION_ID))
					manager.remove(item);
			}
		}
		HashSet/*<String>*/ categories= new HashSet();
		for (int i= 0; i < fInputElement.length; i++) {
			collectCategories(fInputElement[i], categories);
		}
		List sortedCategories= new ArrayList(categories);
		Collections.sort(sortedCategories, new Comparator() {
			public int compare(Object o1, Object o2) {
				return -Collator.getInstance().compare(o1, o2);
			}
		});
		int count= 0;
		for (Iterator iter= sortedCategories.iterator(); iter.hasNext() && count < MAX_NUMBER_OF_CATEGORIES_IN_MENU;) {
			String category= (String)iter.next();
			manager.appendToGroup(CATEGORY_MENU_GROUP_NAME, new CategoryFilterAction(category));
			count++;
		}
		fMenuAction.setInput(fInputElement);
	}

	private void collectCategories(IJavaElement element, HashSet result) {
		try {
			if (element instanceof IMember) {
				IMember member= (IMember)element;
				String[] categories= member.getCategories();
				for (int i= 0; i < categories.length; i++) {
					result.add(categories[i]);
				}
				processChildren(member.getChildren(), result);
			} else if (element instanceof ICompilationUnit) {
				processChildren(((ICompilationUnit)element).getChildren(), result);
			} else if (element instanceof IClassFile) {
				processChildren(((IClassFile)element).getChildren(), result);
			} else if (element instanceof IJavaModel) {
				processChildren(((IJavaModel)element).getChildren(), result);
			} else if (element instanceof IJavaProject) {
				processChildren(((IJavaProject)element).getChildren(), result);
			} else if (element instanceof IPackageFragment) {
				processChildren(((IPackageFragment)element).getChildren(), result);
			} else if (element instanceof IPackageFragmentRoot)	 {
				processChildren(((IPackageFragmentRoot)element).getChildren(), result);
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
	}

	private void processChildren(IJavaElement[] children, HashSet result) {
		for (int i= 0; i < children.length; i++) {
			collectCategories(children[i], result);
		}
	}

	private void fireSelectionChange() {
		fViewer.getControl().setRedraw(false);
		BusyIndicator.showWhile(fViewer.getControl().getDisplay(), new Runnable() {
			public void run() {
				fViewer.refresh();
			}
		});
		fViewer.getControl().setRedraw(true);
	}
	
	private String getPreferenceKey() {
		return "CategoryFilterActionGroup." + fViewerId; //$NON-NLS-1$
	}
	
	private void showCategorySelectionDialog(IJavaElement[] input) {
		HashSet/*<String>*/ categories= new HashSet();
		for (int i= 0; i < input.length; i++) {
			collectCategories(input[i], categories);
		}
		CategoryFilterSelectionDialog dialog= new CategoryFilterSelectionDialog(fViewer.getControl().getShell(), new ArrayList(categories), new ArrayList(fFilteredCategories));
		if (dialog.open() == Window.OK) {
			Object[] selected= dialog.getResult();
			for (Iterator iter= categories.iterator(); iter.hasNext();) {
				String category= (String)iter.next();
				if (contains(selected, category)) {
					fFilteredCategories.remove(category);
				} else {
					fFilteredCategories.add(category);
				}
			}
			storeFilteredCategories();
			fireSelectionChange();
		}
	}
	
	private boolean contains(Object[] selected, String category) {
		for (int i= 0; i < selected.length; i++) {
			if (selected[i].equals(category))
				return true;
		}
		return false;
	}

}

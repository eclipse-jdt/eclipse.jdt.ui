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
package org.eclipse.jdt.internal.ui.actions;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.corext.codemanipulation.IOverrideMethodQuery;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;

public class OverrideMethodQuery implements IOverrideMethodQuery {
	
	private static class OverrideTreeSelectionDialog extends CheckedTreeSelectionDialog {

		private OverrideMethodContentProvider fContentProvider;

		public OverrideTreeSelectionDialog(Shell parent, ILabelProvider labelProvider, OverrideMethodContentProvider contentProvider) {
			super(parent, labelProvider, contentProvider);
			fContentProvider= contentProvider;
		}

		/*
		 * @see CheckedTreeSelectionDialog#createTreeViewer(Composite)
		 */
		protected CheckboxTreeViewer createTreeViewer(Composite composite) {
			Composite inner= new Composite(composite, SWT.NONE);
			GridLayout layout= new GridLayout();
			layout.marginHeight= 0;
			layout.marginWidth= 0;
			layout.numColumns= 1;
			inner.setLayout(layout);
			inner.setLayoutData(new GridData(GridData.FILL_BOTH));
			
			CheckboxTreeViewer treeViewer= super.createTreeViewer(inner);
			
			
			Button flatListButton= new Button(inner, SWT.CHECK);
			flatListButton.setText(ActionMessages.getString("OverrideMethodQuery.groupMethodsByTypes")); //$NON-NLS-1$
			flatListButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
			
			flatListButton.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					boolean isSelected= (((Button) e.widget).getSelection());
					fContentProvider.setShowTypes(isSelected);
				}

				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
			});
			flatListButton.setSelection(fContentProvider.isShowTypes());
			return treeViewer;		
		}
		/*
		 * @see org.eclipse.jface.window.Window#configureShell(Shell)
		 */
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			WorkbenchHelp.setHelp(newShell, IJavaHelpContextIds.OVERRIDE_TREE_SELECTION_DIALOG);
		}
	}



	private static class OverrideMethodContentProvider implements ITreeContentProvider {

		private final String SETTINGS_SECTION= "OverrideMethodDialog"; //$NON-NLS-1$
		private final String SETTINGS_SHOWTYPES= "showtypes"; //$NON-NLS-1$

		private Object[] fTypes;
		private IMethod[] fMethods;
		private final Object[] fEmpty= new Object[0];
		
		private boolean fShowTypes;
		private Viewer fViewer;
		private IDialogSettings fSettings;

		/**
		 * Constructor for OverrideMethodContentProvider.
		 */
		public OverrideMethodContentProvider(IMethod[] methods, Object[] types) {
			fMethods= methods;
			fTypes= types;
			IDialogSettings dialogSettings= JavaPlugin.getDefault().getDialogSettings();
			fSettings= dialogSettings.getSection(SETTINGS_SECTION);
			if (fSettings == null) {
				fSettings= dialogSettings.addNewSection(SETTINGS_SECTION);
				fSettings.put(SETTINGS_SHOWTYPES, true);
			}
			fShowTypes= fSettings.getBoolean(SETTINGS_SHOWTYPES);
		}
		
		/*
		 * @see ITreeContentProvider#getChildren(Object)
		 */
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof IType) {
				ArrayList result= new ArrayList(fMethods.length);
				for (int i= 0; i < fMethods.length; i++) {
					if (fMethods[i].getDeclaringType().equals(parentElement)) {
						result.add(fMethods[i]);
					}
				}
				return result.toArray();
			}
			return fEmpty;
		}

		/*
		 * @see ITreeContentProvider#getParent(Object)
		 */
		public Object getParent(Object element) {
			if (element instanceof IMethod) {
				return ((IMethod)element).getDeclaringType();
			}
			return null;
		}

		/*
		 * @see ITreeContentProvider#hasChildren(Object)
		 */
		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

		/*
		 * @see IStructuredContentProvider#getElements(Object)
		 */
		public Object[] getElements(Object inputElement) {
			return fShowTypes ? fTypes : fMethods;
		}

		/*
		 * @see IContentProvider#dispose()
		 */
		public void dispose() {
		}

		/*
		 * @see IContentProvider#inputChanged(Viewer, Object, Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			fViewer= viewer;
		}
		
		
		public boolean isShowTypes() {
			return fShowTypes;
		}

		public void setShowTypes(boolean showTypes) {
			if (fShowTypes != showTypes) {
				fShowTypes= showTypes;
				fSettings.put(SETTINGS_SHOWTYPES, showTypes);
				if (fViewer != null) {
					fViewer.refresh();
				}
			}
		}

	}
	
	private static class OverrideMethodSorter extends ViewerSorter {

		private IType[] fAllTypes;

		public OverrideMethodSorter(ITypeHierarchy typeHierarchy) {
			IType curr= typeHierarchy.getType();
			IType[] superTypes= typeHierarchy.getAllSupertypes(curr);
			fAllTypes= new IType[superTypes.length + 1];
			fAllTypes[0]= curr;
			System.arraycopy(superTypes, 0, fAllTypes, 1, superTypes.length);
		}

		/*
		 * @see ViewerSorter#compare(Viewer, Object, Object)
		 */
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (e1 instanceof IType && e2 instanceof IType) {
				if (e1.equals(e2)) {
					return 0;
				}
				for (int i= 0; i < fAllTypes.length; i++) {
					IType curr= fAllTypes[i];
					if (curr.equals(e1)) {
						return -1;
					}
					if (curr.equals(e2)) {
						return 1;
					}	
				}
				return 0;
			} else {
				return super.compare(viewer, e1, e2);
			}
		}

	}
	
	private class OverrideMethodValidator implements ISelectionStatusValidator {
				
		/*
		 * @see ISelectionValidator#validate(Object[])
		 */
		public IStatus validate(Object[] selection) {
			int count= 0;
			for (int i= 0; i < selection.length; i++) {
				if (selection[i] instanceof IMethod) {
					count++;
				}
			}
			if (count == 0 && !fEmptySelectionAllowed) {
				return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
			}
			String message;
			if (count == 1) {
				message= ActionMessages.getFormattedString("OverrideMethodQuery.selectioninfo.one", String.valueOf(count)); //$NON-NLS-1$
			} else {
				message= ActionMessages.getFormattedString("OverrideMethodQuery.selectioninfo.more", String.valueOf(count)); //$NON-NLS-1$
			}
			return new StatusInfo(IStatus.INFO, message);
		}

	}
	
	private boolean fEmptySelectionAllowed;
	private Shell fShell;
	
	public OverrideMethodQuery(Shell shell, boolean emptySelectionAllowed) {
		fShell= shell;
		fEmptySelectionAllowed= emptySelectionAllowed;
	}

	/*
	 * @see IOverrideMethodQuery#select(IMethod[], IMethod[], ITypeHierarchy)
	 */
	public IMethod[] select(IMethod[] methods, IMethod[] defaultSelected, ITypeHierarchy typeHierarchy) {
		HashSet types= new HashSet(methods.length);
		for (int i= 0; i < methods.length; i++) {
			types.add(methods[i].getDeclaringType());
		}
		Object[] typesArrays= types.toArray();
		ViewerSorter sorter= new OverrideMethodSorter(typeHierarchy);
		sorter.sort(null, typesArrays);
		
		HashSet expanded= new HashSet(defaultSelected.length); 
		for (int i= 0; i < defaultSelected.length; i++) {
			expanded.add(defaultSelected[i].getDeclaringType());
		}

		if (expanded.isEmpty() && typesArrays.length > 0) {
			expanded.add(typesArrays[0]);
		}
		
		ILabelProvider lprovider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
		OverrideMethodContentProvider contentProvider= new OverrideMethodContentProvider(methods, typesArrays);
		
		OverrideTreeSelectionDialog dialog= new OverrideTreeSelectionDialog(fShell, lprovider, contentProvider);
		dialog.setValidator(new OverrideMethodValidator());
		dialog.setTitle(ActionMessages.getString("OverrideMethodQuery.dialog.title")); //$NON-NLS-1$
		dialog.setMessage(ActionMessages.getString("OverrideMethodQuery.dialog.description")); //$NON-NLS-1$
		dialog.setInitialSelections(defaultSelected);
		dialog.setExpandedElements(expanded.toArray());
		dialog.setContainerMode(true);
		dialog.setSorter(sorter);
		dialog.setSize(60, 18);
		dialog.setInput(this); // input does not matter
		if (dialog.open() == OverrideTreeSelectionDialog.OK) {
			Object[] checkedElements= dialog.getResult();
			ArrayList result= new ArrayList(checkedElements.length);
			for (int i= 0; i < checkedElements.length; i++) {
				Object curr= checkedElements[i];
				if (curr instanceof IMethod) {
					result.add(curr);
				}
			}
			return (IMethod[]) result.toArray(new IMethod[result.size()]);
		}
		return null;
	}
	
}


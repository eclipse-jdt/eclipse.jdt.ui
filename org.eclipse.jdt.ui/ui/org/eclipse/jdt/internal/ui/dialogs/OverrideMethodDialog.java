/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.internal.dialogs.ContainerCheckedTreeViewer;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.util.ViewerPane;


public class OverrideMethodDialog extends SourceActionDialog {
	
	private class OverrideFlatTreeAction extends Action {
		private boolean fToggle;
		
		public OverrideFlatTreeAction() {
			super(); //$NON-NLS-1$
			setToolTipText(JavaUIMessages.getString("OverrideMethodDialog.groupMethodsByTypes")); //$NON-NLS-1$
	
			JavaPluginImages.setLocalImageDescriptors(this, "impl_co.gif"); //$NON-NLS-1$

			fToggle= getOverrideContentProvider().isShowTypes();
			setChecked(fToggle);
		}
	
		public void run() {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=39264
			Object[] elementList= getOverrideContentProvider().getViewer().getCheckedElements();
			fToggle= !fToggle;
			setChecked(fToggle);
			getOverrideContentProvider().setShowTypes(fToggle);		
			getOverrideContentProvider().getViewer().setCheckedElements(elementList);
		}
		
		private OverrideMethodContentProvider getOverrideContentProvider() {
			return (OverrideMethodContentProvider) getContentProvider();
		}
		
	}
	
	private static class OverrideMethodContentProvider implements ITreeContentProvider {
		private final String SETTINGS_SECTION= "OverrideMethodDialog"; //$NON-NLS-1$
		private final String SETTINGS_SHOWTYPES= "showtypes"; //$NON-NLS-1$
	
		private Object[] fTypes;
		private IMethod[] fMethods;
		private final Object[] fEmpty= new Object[0];
			
		private boolean fShowTypes;
		private ContainerCheckedTreeViewer fViewer;
		private IDialogSettings fSettings;
	
		public ContainerCheckedTreeViewer getViewer() {
			return fViewer;
		}

		/**
		 * Constructor for OverrideMethodContentProvider.
		 */
		public OverrideMethodContentProvider() {
			IDialogSettings dialogSettings= JavaPlugin.getDefault().getDialogSettings();
			fSettings= dialogSettings.getSection(SETTINGS_SECTION);
			if (fSettings == null) {
				fSettings= dialogSettings.addNewSection(SETTINGS_SECTION);
				fSettings.put(SETTINGS_SHOWTYPES, true);
			}
			fShowTypes= fSettings.getBoolean(SETTINGS_SHOWTYPES);
		}
		
		public void init(IMethod[] methods, IType[] types) {
			fMethods= methods;
			fTypes= types;
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
			fViewer= (ContainerCheckedTreeViewer) viewer;
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
	
	private static class OverrideMethodValidator implements ISelectionStatusValidator {
		private static int fNumMethods;
			
		public OverrideMethodValidator(int entries) {
			super();
			fNumMethods= entries;
		}		
			
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
			if (count == 0) {
				return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
			}
			String[] args= { String.valueOf(count), String.valueOf(fNumMethods)};
			String message= JavaUIMessages.getFormattedString("OverrideMethodDialog.selectioninfo.more", args); //$NON-NLS-1$

			return new StatusInfo(IStatus.INFO, message);
		}
	}		
	
	public OverrideMethodDialog(Shell parent, CompilationUnitEditor editor, IType type, boolean isSubType) throws JavaModelException {
		super(parent, new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT), new OverrideMethodContentProvider(), editor, type, false);
		init(type, isSubType);
	}
	
	public boolean hasMethodsToOverride() {
		return getContentProvider().getElements(null).length > 0;
	}
	
	
	private void init(IType type, boolean isSubType) throws JavaModelException {
		ITypeHierarchy typeHierarchy= type.newSupertypeHierarchy(null);
		IMethod[] inheritedMethods= StubUtility.getOverridableMethods(type, typeHierarchy, isSubType);
	
		List toImplement= new ArrayList();
		for (int i= 0; i < inheritedMethods.length; i++) {
			IMethod curr= inheritedMethods[i];
			if (JdtFlags.isAbstract(curr)) {
				toImplement.add(curr);
			}
		}
		IMethod[] toImplementArray= (IMethod[]) toImplement.toArray(new IMethod[toImplement.size()]);		
		setInitialSelections(toImplementArray);

		HashSet expanded= new HashSet(toImplementArray.length); 
		for (int i= 0; i < toImplementArray.length; i++) {
			expanded.add(toImplementArray[i].getDeclaringType());
		}

		HashSet types= new HashSet(inheritedMethods.length);
		for (int i= 0; i < inheritedMethods.length; i++) {
			types.add(inheritedMethods[i].getDeclaringType());
		}
		
		IType[] typesArrays= (IType[]) types.toArray(new IType[types.size()]);
		ViewerSorter sorter= new OverrideMethodSorter(typeHierarchy);
		if (expanded.isEmpty() && typesArrays.length > 0) {
			sorter.sort(null, typesArrays); // sort to get the first one
			expanded.add(typesArrays[0]);
		}
		setExpandedElements(expanded.toArray());
		
		ITreeContentProvider contentProvider= getContentProvider();
		((OverrideMethodContentProvider) contentProvider).init(inheritedMethods, typesArrays);

		setTitle(JavaUIMessages.getString("OverrideMethodDialog.dialog.title")); //$NON-NLS-1$
		setMessage(null);			
		setValidator(new OverrideMethodValidator(inheritedMethods.length));
		setSorter(sorter);
		setContainerMode(true);
		setSize(60, 18);			
		setInput(new Object());
	}
	

	/*
	 * @see CheckedTreeSelectionDialog#createTreeViewer(Composite)
	 */
	protected CheckboxTreeViewer createTreeViewer(Composite composite) {
		initializeDialogUnits(composite);
		ViewerPane pane= new ViewerPane(composite, SWT.BORDER | SWT.FLAT);
		pane.setText(JavaUIMessages.getString("OverrideMethodDialog.dialog.description")); //$NON-NLS-1$
	
		CheckboxTreeViewer treeViewer= super.createTreeViewer(pane);
		pane.setContent(treeViewer.getControl());
		GridLayout paneLayout= new GridLayout();
		paneLayout.marginHeight= 0;
		paneLayout.marginWidth= 0;
		paneLayout.numColumns= 1;
		pane.setLayout(paneLayout);
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint = convertWidthInCharsToPixels(55);
		gd.heightHint = convertHeightInCharsToPixels(15);
		pane.setLayoutData(gd);

		ToolBarManager tbm= pane.getToolBarManager();
		tbm.add(new OverrideFlatTreeAction()); // create after tree is created
		tbm.update(true);
		treeViewer.getTree().setFocus();
					
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

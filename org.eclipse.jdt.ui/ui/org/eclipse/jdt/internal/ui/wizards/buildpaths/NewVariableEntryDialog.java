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
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.preferences.ClasspathVariablesPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.PreferencePageSupport;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;

public class NewVariableEntryDialog extends StatusDialog {

	private class VariablesAdapter implements IDialogFieldListener, IListAdapter {
		
		// -------- IListAdapter --------
			
		public void customButtonPressed(ListDialogField field, int index) {
			switch (index) {
			case IDX_EXTEND: /* extend */
				extendButtonPressed();
				break;
			}		
		}
		
		public void selectionChanged(ListDialogField field) {
			doSelectionChanged();
		}
		
		public void doubleClicked(ListDialogField field) {
			doDoubleClick();
		}			
			
		// ---------- IDialogFieldListener --------
	
		public void dialogFieldChanged(DialogField field) {
			if (field == fConfigButton) {
				configButtonPressed();
			}
			
		}
	
	}
	
	private final int IDX_EXTEND= 0;
	
	private ListDialogField fVariablesList;
	private boolean fCanExtend;
	private boolean fIsValidSelection;
	
	private IPath[] fResultPaths;

	private SelectionButtonDialogField fConfigButton;
	
	public NewVariableEntryDialog(Shell parent) {
		super(parent);
		setTitle(NewWizardMessages.getString("NewVariableEntryDialog.title")); //$NON-NLS-1$
		
		int shellStyle= getShellStyle();
		setShellStyle(shellStyle | SWT.MAX | SWT.RESIZE);
		updateStatus(new StatusInfo(IStatus.ERROR, "")); //$NON-NLS-1$

		String[] buttonLabels= new String[] { 
			/* IDX_EXTEND */ NewWizardMessages.getString("NewVariableEntryDialog.vars.extend"), //$NON-NLS-1$
		};
				
		VariablesAdapter adapter= new VariablesAdapter();
		
		CPVariableElementLabelProvider labelProvider= new CPVariableElementLabelProvider(false);
		
		fVariablesList= new ListDialogField(adapter, buttonLabels, labelProvider);
		fVariablesList.setDialogFieldListener(adapter);
		fVariablesList.setLabelText(NewWizardMessages.getString("NewVariableEntryDialog.vars.label")); //$NON-NLS-1$
		
		fVariablesList.enableButton(IDX_EXTEND, false);
		
		fVariablesList.setViewerSorter(new ViewerSorter() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				if (e1 instanceof CPVariableElement && e2 instanceof CPVariableElement) {
					return ((CPVariableElement)e1).getName().compareTo(((CPVariableElement)e2).getName());
				}
				return super.compare(viewer, e1, e2);
			}
		});
		
		
		fConfigButton= new SelectionButtonDialogField(SWT.PUSH);
		fConfigButton.setLabelText(NewWizardMessages.getString("NewVariableEntryDialog.configbutton.label")); //$NON-NLS-1$
		fConfigButton.setDialogFieldListener(adapter);
		
		initializeElements();

		fCanExtend= false;
		fIsValidSelection= false;
		fResultPaths= null;
	}
	
	private void initializeElements() {
		String[] entries= JavaCore.getClasspathVariableNames();
		ArrayList elements= new ArrayList(entries.length);
		for (int i= 0; i < entries.length; i++) {
			String name= entries[i];
			IPath entryPath= JavaCore.getClasspathVariable(name);
			if (entryPath != null) {
				elements.add(new CPVariableElement(name, entryPath, false));
			}
		}
		
		fVariablesList.setElements(elements);
	}
	
	
	/* (non-Javadoc)
	 * @see Window#configureShell(Shell)
	 */
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IJavaHelpContextIds.NEW_VARIABLE_ENTRY_DIALOG);
	}	
			
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		initializeDialogUnits(parent);
		
		Composite composite= (Composite) super.createDialogArea(parent);
		GridLayout layout= (GridLayout) composite.getLayout();
		layout.numColumns= 2;
		
		fVariablesList.doFillIntoGrid(composite, 3);
		
		LayoutUtil.setHorizontalSpan(fVariablesList.getLabelControl(null), 2);
		
		GridData listData= (GridData) fVariablesList.getListControl(null).getLayoutData();
		listData.grabExcessHorizontalSpace= true;
		listData.heightHint= convertHeightInCharsToPixels(10);
		
		Composite lowerComposite= new Composite(composite, SWT.NONE);
		lowerComposite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		
		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		lowerComposite.setLayout(layout);
		
		fConfigButton.doFillIntoGrid(lowerComposite, 1);
		
		applyDialogFont(composite);		
		return composite;
	}
	
	public IPath[] getResult() {
		return fResultPaths;
	}
	
	/*
 	 * @see IDoubleClickListener#doubleClick(DoubleClickEvent)
 	 */
	private void doDoubleClick() {
		if (fIsValidSelection) {
			okPressed();
		} else if (fCanExtend) {
			extendButtonPressed();
		}
	}
	
	private void doSelectionChanged() {
		boolean isValidSelection= true;
		boolean canExtend= false;
		StatusInfo status= new StatusInfo();
		
		List selected= fVariablesList.getSelectedElements();
		int nSelected= selected.size();
		
		if (nSelected > 0) {
			fResultPaths= new Path[nSelected];
			for (int i= 0; i < nSelected; i++) {
				CPVariableElement curr= (CPVariableElement) selected.get(i);
				fResultPaths[i]= new Path(curr.getName());
				if (!curr.getPath().toFile().isFile()) {
					status.setInfo(NewWizardMessages.getString("NewVariableEntryDialog.info.isfolder")); //$NON-NLS-1$
					canExtend= true;
				}
			}
		} else {
			isValidSelection= false;
			status.setInfo(NewWizardMessages.getString("NewVariableEntryDialog.info.noselection")); //$NON-NLS-1$
		}
		if (isValidSelection && nSelected > 1) {
			String str= NewWizardMessages.getFormattedString("NewVariableEntryDialog.info.selected", String.valueOf(nSelected)); //$NON-NLS-1$
			status.setInfo(str);
		}
		fCanExtend= nSelected == 1 && canExtend;
		fVariablesList.enableButton(0, fCanExtend);
		
		updateStatus(status);
		fIsValidSelection= isValidSelection;
		Button okButton= getButton(IDialogConstants.OK_ID);
		if (okButton != null  && !okButton.isDisposed()) {
			okButton.setEnabled(isValidSelection);
		}
	}
	
	private IPath[] chooseExtensions(CPVariableElement elem) {
		File file= elem.getPath().toFile();

		JARFileSelectionDialog dialog= new JARFileSelectionDialog(getShell(), true, false);
		dialog.setTitle(NewWizardMessages.getString("NewVariableEntryDialog.ExtensionDialog.title")); //$NON-NLS-1$
		dialog.setMessage(NewWizardMessages.getFormattedString("NewVariableEntryDialog.ExtensionDialog.description", elem.getName())); //$NON-NLS-1$
		dialog.setInput(file);
		if (dialog.open() == Window.OK) {
			Object[] selected= dialog.getResult();
			IPath[] paths= new IPath[selected.length];
			for (int i= 0; i < selected.length; i++) {
				IPath filePath= Path.fromOSString(((File) selected[i]).getPath());
				IPath resPath=  new Path(elem.getName());
				for (int k= elem.getPath().segmentCount(); k < filePath.segmentCount(); k++) {
					resPath= resPath.append(filePath.segment(k));
				}
				paths[i]= resPath;
			}
			return paths;
		}
		return null;
	}
	
	protected final void extendButtonPressed() {
		List selected= fVariablesList.getSelectedElements();
		if (selected.size() == 1) {
			IPath[] extendedPaths= chooseExtensions((CPVariableElement) selected.get(0));
			if (extendedPaths != null) {
				fResultPaths= extendedPaths;
				super.buttonPressed(IDialogConstants.OK_ID);
			}
		}
	}
		
	protected final void configButtonPressed() {
		ClasspathVariablesPreferencePage page= new ClasspathVariablesPreferencePage();
		PreferencePageSupport.showPreferencePage(getShell(), ClasspathVariablesPreferencePage.ID, page);
		initializeElements();
	}	

}

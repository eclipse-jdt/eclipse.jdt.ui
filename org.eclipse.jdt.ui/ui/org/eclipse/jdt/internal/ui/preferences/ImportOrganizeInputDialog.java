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
package org.eclipse.jdt.internal.ui.preferences;

import java.util.List;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.dialogs.PackageSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.preferences.ImportOrganizeConfigurationBlock.ImportOrderEntry;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;

/**
 * Dialog to enter a new package entry in the organize import preference page.
 */
public class ImportOrganizeInputDialog extends StatusDialog {
	
	private class ImportOrganizeInputAdapter implements IDialogFieldListener, IStringButtonAdapter {
		/**
		 * @see IDialogFieldListener#dialogFieldChanged(DialogField)
		 */
		public void dialogFieldChanged(DialogField field) {
			doDialogFieldChanged(field);
		}			

		/**
		 * @see IStringButtonAdapter#changeControlPressed(DialogField)
		 */
		public void changeControlPressed(DialogField field) {
			doBrowsePackages();
		}
	}
	
	private SelectionButtonDialogField fNameGroup;
	private SelectionButtonDialogField fOtherGroup;
	private SelectionButtonDialogField fOtherStaticGroup;
	
	private StringButtonDialogField fNameDialogField;
	private SelectionButtonDialogField fBrowseTypeButton;
	private SelectionButtonDialogField fIsStatic;
	private List fExistingEntries;
		
	public ImportOrganizeInputDialog(Shell parent, List/*<ImportOrderEntry>*/ existingEntries) {
		super(parent);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		
		fExistingEntries= existingEntries;
		
		setTitle(PreferencesMessages.getString("ImportOrganizeInputDialog.title")); //$NON-NLS-1$

		ImportOrganizeInputAdapter adapter= new ImportOrganizeInputAdapter();
		
		fIsStatic= new SelectionButtonDialogField(SWT.CHECK);
		fIsStatic.setLabelText(PreferencesMessages.getString("ImportOrganizeInputDialog.static.label")); //$NON-NLS-1$
		fIsStatic.setDialogFieldListener(adapter);
		
		fNameGroup= new SelectionButtonDialogField(SWT.RADIO);
		fNameGroup.setLabelText(PreferencesMessages.getString("ImportOrganizeInputDialog.name_group.label")); //$NON-NLS-1$
		fNameGroup.setDialogFieldListener(adapter);

		fNameDialogField= new StringButtonDialogField(adapter);
		fNameDialogField.setLabelText(PreferencesMessages.getString("ImportOrganizeInputDialog.message")); //$NON-NLS-1$
		fNameDialogField.setButtonLabel(PreferencesMessages.getString("ImportOrganizeInputDialog.browse_packages.button")); //$NON-NLS-1$
		fNameDialogField.setDialogFieldListener(adapter);
		
		fBrowseTypeButton= new SelectionButtonDialogField(SWT.PUSH);
		fBrowseTypeButton.setLabelText(PreferencesMessages.getString("ImportOrganizeInputDialog.browse_types.label")); //$NON-NLS-1$
		fBrowseTypeButton.setDialogFieldListener(adapter);
		
		fOtherGroup= new SelectionButtonDialogField(SWT.RADIO);
		fOtherGroup.setLabelText(PreferencesMessages.getString("ImportOrganizeInputDialog.other_group.label")); //$NON-NLS-1$
		fOtherGroup.setDialogFieldListener(adapter);
		
		fOtherStaticGroup= new SelectionButtonDialogField(SWT.RADIO);
		fOtherStaticGroup.setLabelText(PreferencesMessages.getString("ImportOrganizeInputDialog.other_static_group.label")); //$NON-NLS-1$
		fOtherStaticGroup.setDialogFieldListener(adapter);
		
		fNameGroup.attachDialogFields(new DialogField[] { fNameDialogField, fBrowseTypeButton, fIsStatic });
		
	}
		
	public void setInitialSelection(ImportOrderEntry editedEntry) {
		Assert.isNotNull(editedEntry);
		if (editedEntry.name.length() == 0) {
			if (editedEntry.isStatic) {
				fOtherStaticGroup.setSelection(true);
			} else {
				fOtherGroup.setSelection(true);
			}
		} else {
			fNameDialogField.setText(editedEntry.name);
			fIsStatic.setSelection(editedEntry.isStatic);
		}
	}
	
	public ImportOrderEntry getResult() {
		if (fNameGroup.isSelected()) {
			return new ImportOrderEntry(fNameDialogField.getText(), fIsStatic.isSelected());
		} else {
			return new ImportOrderEntry("", fOtherStaticGroup.isSelected()); //$NON-NLS-1$
		}
	}
		
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite) super.createDialogArea(parent);
		
		GridLayout layout= new GridLayout(3, false);
		composite.setLayout(layout);
		

		fNameGroup.doFillIntoGrid(composite, 3);
		
		fNameDialogField.doFillIntoGrid(composite, 3);
		
		int indent= convertWidthInCharsToPixels(4);
		LayoutUtil.setHorizontalIndent(fNameDialogField.getLabelControl(null), indent);
		
		int fieldWidthHint= convertWidthInCharsToPixels(50);
		LayoutUtil.setWidthHint(fNameDialogField.getTextControl(null), fieldWidthHint);
		LayoutUtil.setHorizontalGrabbing(fNameDialogField.getTextControl(null));
		
		DialogField.createEmptySpace(composite, 1);
		fIsStatic.doFillIntoGrid(composite, 1);
		fBrowseTypeButton.doFillIntoGrid(composite, 1);
		
		fOtherGroup.doFillIntoGrid(composite, 3);
		fOtherStaticGroup.doFillIntoGrid(composite, 3);
		
		fNameDialogField.postSetFocusOnDialogField(parent.getDisplay());
		
		applyDialogFont(composite);		
		return composite;
	}
	
	final void doBrowsePackages() {
		IRunnableContext context= new BusyIndicatorRunnableContext();
		IJavaSearchScope scope= SearchEngine.createWorkspaceScope();
		int style= PackageSelectionDialog.F_REMOVE_DUPLICATES | PackageSelectionDialog.F_SHOW_PARENTS | PackageSelectionDialog.F_HIDE_DEFAULT_PACKAGE;
		PackageSelectionDialog dialog= new PackageSelectionDialog(getShell(), context, style, scope);
		dialog.setFilter(fNameDialogField.getText());
		dialog.setIgnoreCase(false);
		dialog.setTitle(PreferencesMessages.getString("ImportOrganizeInputDialog.ChoosePackageDialog.title")); //$NON-NLS-1$
		dialog.setMessage(PreferencesMessages.getString("ImportOrganizeInputDialog.ChoosePackageDialog.description")); //$NON-NLS-1$
		dialog.setEmptyListMessage(PreferencesMessages.getString("ImportOrganizeInputDialog.ChoosePackageDialog.empty")); //$NON-NLS-1$
		if (dialog.open() == Window.OK) {
			IPackageFragment res= (IPackageFragment) dialog.getFirstResult();
			fNameDialogField.setText(res.getElementName());
		}
	}
	
	private void doBrowseTypes() {		
		IRunnableContext context= new BusyIndicatorRunnableContext();
		IJavaSearchScope scope= SearchEngine.createWorkspaceScope();
		int style= IJavaElementSearchConstants.CONSIDER_TYPES;
		try {
			SelectionDialog dialog= JavaUI.createTypeDialog(getShell(), context, scope, style, false, fNameDialogField.getText());
			dialog.setTitle(PreferencesMessages.getString("ImportOrganizeInputDialog.ChooseTypeDialog.title")); //$NON-NLS-1$
			dialog.setMessage(PreferencesMessages.getString("ImportOrganizeInputDialog.ChooseTypeDialog.description")); //$NON-NLS-1$
			if (dialog.open() == Window.OK) {
				IType res= (IType) dialog.getResult()[0];
				fNameDialogField.setText(res.getFullyQualifiedName('.'));
			}
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, getShell(), PreferencesMessages.getString("ImportOrganizeInputDialog.ChooseTypeDialog.title"), PreferencesMessages.getString("ImportOrganizeInputDialog.ChooseTypeDialog.error.message"));  //$NON-NLS-1$//$NON-NLS-2$
		}
	}
	
	/**
	 * @param field
	 */
	final void doDialogFieldChanged(DialogField field) {
		if (field == fBrowseTypeButton) {
			doBrowseTypes();
		} else {
			doValidation();
		}
	}
	
	
	private void doValidation() {
		StatusInfo status= new StatusInfo();
		if (fNameGroup.isSelected()) {
			String newText= fNameDialogField.getText();
			if (newText.length() == 0) {
				status.setError(PreferencesMessages.getString("ImportOrganizeInputDialog.error.enterName")); //$NON-NLS-1$
			} else {
				IStatus val= JavaConventions.validatePackageName(newText);
				if (val.matches(IStatus.ERROR)) {
					status.setError(PreferencesMessages.getFormattedString("ImportOrganizeInputDialog.error.invalidName", val.getMessage())); //$NON-NLS-1$
				} else {
					if (doesExist(newText, fIsStatic.isSelected())) {
						status.setError(PreferencesMessages.getString("ImportOrganizeInputDialog.error.entryExists")); //$NON-NLS-1$
					}
				}
			}
		} else {
			if (doesExist("", fOtherStaticGroup.isSelected())) { //$NON-NLS-1$
				status.setError(PreferencesMessages.getString("ImportOrganizeInputDialog.error.entryExists")); //$NON-NLS-1$
			}
		}
		updateStatus(status);
	}
	
	private boolean doesExist(String name, boolean isStatic) {
		for (int i= 0; i < fExistingEntries.size(); i++) {
			ImportOrderEntry entry= (ImportOrderEntry) fExistingEntries.get(i);
			if (name.equals(entry.name) && isStatic == entry.isStatic) {
				return true;
			}
		}
		return false;
	}
	

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		WorkbenchHelp.setHelp(newShell, IJavaHelpContextIds.IMPORT_ORGANIZE_INPUT_DIALOG);
	}


}

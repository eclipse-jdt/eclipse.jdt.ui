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
package org.eclipse.jdt.internal.ui.preferences;

import java.util.List;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.util.Assert;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.dialogs.PackageSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;

/**
 * Dialog to enter a new entry in the type filter preference page.
 */
public class TypeFilterInputDialog extends StatusDialog {
	
	private class ImportOrganizeInputAdapter implements IDialogFieldListener, IStringButtonAdapter {
		/*
		 * @see IDialogFieldListener#dialogFieldChanged(DialogField)
		 */
		public void dialogFieldChanged(DialogField field) {
			doValidation();
		}			
		/*
		 * @see IStringButtonAdapter#changeControlPressed(DialogField)
		 */
		public void changeControlPressed(DialogField field) {
			doButtonPressed();
		}
	}
	
	private StringButtonDialogField fNameDialogField;
	private List fExistingEntries;
		
	public TypeFilterInputDialog(Shell parent, List existingEntries) {
		super(parent);
		
		fExistingEntries= existingEntries;
		
		setTitle(PreferencesMessages.getString("TypeFilterInputDialog.title")); //$NON-NLS-1$

		ImportOrganizeInputAdapter adapter= new ImportOrganizeInputAdapter();

		fNameDialogField= new StringButtonDialogField(adapter);
		fNameDialogField.setLabelText(PreferencesMessages.getString("TypeFilterInputDialog.message")); //$NON-NLS-1$
		fNameDialogField.setButtonLabel(PreferencesMessages.getString("TypeFilterInputDialog.browse.button")); //$NON-NLS-1$
		fNameDialogField.setDialogFieldListener(adapter);
		
		fNameDialogField.setText("");		 //$NON-NLS-1$
	}
	
	public void setInitialString(String input) {
		Assert.isNotNull(input);
		fNameDialogField.setText(input);
	}
	
	public Object getResult() {
		return fNameDialogField.getText();
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite) super.createDialogArea(parent);
		
		Composite inner= new Composite(composite, SWT.NONE);
		LayoutUtil.doDefaultLayout(inner, new DialogField[] { fNameDialogField }, true, 0, 0);
		
		int fieldWidthHint= convertWidthInCharsToPixels(60);
		LayoutUtil.setWidthHint(fNameDialogField.getTextControl(null), fieldWidthHint);
		LayoutUtil.setHorizontalGrabbing(fNameDialogField.getTextControl(null));
		
		fNameDialogField.postSetFocusOnDialogField(parent.getDisplay());
		
		applyDialogFont(composite);		
		return composite;
	}
	
	private void doButtonPressed() {
		IJavaSearchScope scope= SearchEngine.createWorkspaceScope();
		BusyIndicatorRunnableContext context= new BusyIndicatorRunnableContext();
		int flags= PackageSelectionDialog.F_SHOW_PARENTS | PackageSelectionDialog.F_HIDE_DEFAULT_PACKAGE | PackageSelectionDialog.F_REMOVE_DUPLICATES;
		PackageSelectionDialog dialog = new PackageSelectionDialog(getShell(), context, flags , scope);
		dialog.setTitle(PreferencesMessages.getString("TypeFilterInputDialog.choosepackage.label")); //$NON-NLS-1$
		dialog.setMessage(PreferencesMessages.getString("TypeFilterInputDialog.choosepackage.description")); //$NON-NLS-1$
		dialog.setMultipleSelection(false);
		dialog.setFilter(fNameDialogField.getText());
		if (dialog.open() == IDialogConstants.OK_ID) {
			IPackageFragment res= (IPackageFragment) dialog.getFirstResult();
			fNameDialogField.setText(res.getElementName() + "*"); //$NON-NLS-1$
		}
	}
	
	private void doValidation() {
		StatusInfo status= new StatusInfo();
		String newText= fNameDialogField.getText();
		if (newText.length() == 0) {
			status.setError(PreferencesMessages.getString("TypeFilterInputDialog.error.enterName")); //$NON-NLS-1$
		} else {
			newText= newText.replace('*', 'X').replace('?', 'Y');
			IStatus val= JavaConventions.validatePackageName(newText);
			if (val.matches(IStatus.ERROR)) {
				status.setError(PreferencesMessages.getFormattedString("TypeFilterInputDialog.error.invalidName", val.getMessage())); //$NON-NLS-1$
			} else {
				if (fExistingEntries.contains(newText)) {
					status.setError(PreferencesMessages.getString("TypeFilterInputDialog.error.entryExists")); //$NON-NLS-1$
				}
			}
		}
		updateStatus(status);
	}

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		WorkbenchHelp.setHelp(newShell, IJavaHelpContextIds.IMPORT_ORGANIZE_INPUT_DIALOG);
	}
}

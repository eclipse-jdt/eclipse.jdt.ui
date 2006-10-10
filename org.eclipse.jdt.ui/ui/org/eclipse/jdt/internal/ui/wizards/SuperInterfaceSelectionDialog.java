/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards;

import java.util.List;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableContext;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.TypeNameMatch;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.TypeSelectionDialog2;

public class SuperInterfaceSelectionDialog extends TypeSelectionDialog2 {
	
	private static final int ADD_ID= IDialogConstants.CLIENT_ID + 1;
	
	private NewTypeWizardPage fTypeWizardPage;
	private List fOldContent;
	
	public SuperInterfaceSelectionDialog(Shell parent, IRunnableContext context, NewTypeWizardPage page, IJavaProject p) {
		super(parent, true, context, createSearchScope(p), IJavaSearchConstants.INTERFACE);
		fTypeWizardPage= page;
		// to restore the content of the dialog field if the dialog is canceled
		fOldContent= fTypeWizardPage.getSuperInterfaces();
		setStatusLineAboveButtons(true);
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, ADD_ID, NewWizardMessages.SuperInterfaceSelectionDialog_addButton_label, true); 
		super.createButtonsForButtonBar(parent);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#getDialogBoundsSettings()
	 */
	protected IDialogSettings getDialogBoundsSettings() {
		return JavaPlugin.getDefault().getDialogSettingsSection("DialogBounds_SuperInterfaceSelectionDialog"); //$NON-NLS-1$
	}
	
	protected void updateButtonsEnableState(IStatus status) {
	    super.updateButtonsEnableState(status);
	    Button addButton = getButton(ADD_ID);
	    if (addButton != null && !addButton.isDisposed())
	        addButton.setEnabled(!status.matches(IStatus.ERROR));
	}
	
	protected void handleShellCloseEvent() {
		super.handleShellCloseEvent();
		// Handle the closing of the shell by selecting the close icon
		fTypeWizardPage.setSuperInterfaces(fOldContent, true);
	}	

	protected void cancelPressed() {
		fTypeWizardPage.setSuperInterfaces(fOldContent, true);
		super.cancelPressed();
	}
	
	protected void buttonPressed(int buttonId) {
		if (buttonId == ADD_ID){
			addSelectedInterface();
		}
		super.buttonPressed(buttonId);	
	}
	
	protected void okPressed() {
		addSelectedInterface();
		super.okPressed();
	}
		
	private void addSelectedInterface() {
		TypeNameMatch[] selection= getSelectedTypes();
		if (selection == null)
			return;
		for (int i= 0; i < selection.length; i++) {
			TypeNameMatch type= selection[i];
			String qualifiedName= type.getFullyQualifiedName();
			String message;
			if (fTypeWizardPage.addSuperInterface(qualifiedName)) {
				message= Messages.format(NewWizardMessages.SuperInterfaceSelectionDialog_interfaceadded_info, qualifiedName); 
			} else {
				message= Messages.format(NewWizardMessages.SuperInterfaceSelectionDialog_interfacealreadyadded_info, qualifiedName); 
			}
			updateStatus(new StatusInfo(IStatus.INFO, message));
		}
	}

	private static IJavaSearchScope createSearchScope(IJavaProject p) {
		return SearchEngine.createJavaSearchScope(new IJavaProject[] { p });
	}
	
	protected void handleDefaultSelected(TypeNameMatch[] selection) {
		if (selection.length > 0)
			buttonPressed(ADD_ID);
	}
	
	protected void handleWidgetSelected(TypeNameMatch[] selection) {
		super.handleWidgetSelected(selection);
		getButton(ADD_ID).setEnabled(selection.length > 0);
	}

	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.SUPER_INTERFACE_SELECTION_DIALOG);
	}
}

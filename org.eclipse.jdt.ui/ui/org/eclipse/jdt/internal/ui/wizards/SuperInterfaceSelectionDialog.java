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
package org.eclipse.jdt.internal.ui.wizards;

import java.util.List;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableContext;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.TypeInfo;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.TypeSelectionDialog2;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;

public class SuperInterfaceSelectionDialog extends TypeSelectionDialog2 {
	
	private static final int ADD_ID= IDialogConstants.CLIENT_ID + 1;
	
	private ListDialogField fList;
	private List fOldContent;
	
	public SuperInterfaceSelectionDialog(Shell parent, IRunnableContext context, ListDialogField list, IJavaProject p) {
		super(parent, true, context, createSearchScope(p), IJavaSearchConstants.INTERFACE);
		fList= list;
		// to restore the content of the dialog field if the dialog is canceled
		fOldContent= fList.getElements(); 
		setStatusLineAboveButtons(true);
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, ADD_ID, NewWizardMessages.SuperInterfaceSelectionDialog_addButton_label, true); 
		super.createButtonsForButtonBar(parent);
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
		fList.setElements(fOldContent);
	}	

	protected void cancelPressed() {
		fList.setElements(fOldContent);
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
		TypeInfo[] selection= getSelectedTypes();
		if (selection == null)
			return;
		for (int i= 0; i < selection.length; i++) {
			TypeInfo type= selection[i];
			String qualifiedName= type.getFullyQualifiedName();
			addStringWrapper(qualifiedName);
			String message= Messages.format(NewWizardMessages.SuperInterfaceSelectionDialog_interfaceadded_info, qualifiedName); 
			updateStatus(new StatusInfo(IStatus.INFO, message));
		}
	}

	private void addStringWrapper(String qualifiedName) {
		for (int i= 0; i < fList.getSize(); i++) {
			StringWrapper element= (StringWrapper) fList.getElement(i);
			if (qualifiedName.equals(element.getString()))
				return; // don't add again
		}
		fList.addElement(new StringWrapper(qualifiedName));
	}
	
	private static IJavaSearchScope createSearchScope(IJavaProject p) {
		return SearchEngine.createJavaSearchScope(new IJavaProject[] { p });
	}
	
	protected void handleDefaultSelected(TypeInfo[] selection) {
		if (selection.length > 0)
			buttonPressed(ADD_ID);
	}
	
	protected void handleWidgetSelected(TypeInfo[] selection) {
		super.handleWidgetSelected(selection);
		getButton(ADD_ID).setEnabled(selection.length > 0);
	}

	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.SUPER_INTERFACE_SELECTION_DIALOG);
	}
}
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
package org.eclipse.jdt.internal.ui.wizards;

import java.util.List;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableContext;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.util.TypeInfo;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.TypeSelectionDialog;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;

public class SuperInterfaceSelectionDialog extends TypeSelectionDialog {
	
	private static final int ADD_ID= IDialogConstants.CLIENT_ID + 1;
	
	private ListDialogField fList;
	private List fOldContent;
	
	public SuperInterfaceSelectionDialog(Shell parent, IRunnableContext context, ListDialogField list, IJavaProject p) {
		super(parent, context, IJavaSearchConstants.INTERFACE, createSearchScope(p));
		fList= list;
		// to restore the content of the dialog field if the dialog is canceled
		fOldContent= fList.getElements(); 
		setStatusLineAboveButtons(true);
	}

	/*
	 * @see Dialog#createButtonsForButtonBar
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, ADD_ID, NewWizardMessages.getString("SuperInterfaceSelectionDialog.addButton.label"), true); //$NON-NLS-1$
		super.createButtonsForButtonBar(parent);
	}
	
	/*(non-Javadoc)
	 * @see org.eclipse.jface.window.Window#handleShellCloseEvent()
	 */
	protected void handleShellCloseEvent() {
		super.handleShellCloseEvent();

		//Handle the closing of the shell by selecting the close icon
		fList.setElements(fOldContent);
	}	

	/*
	 * @see Dialog#cancelPressed
	 */
	protected void cancelPressed() {
		fList.setElements(fOldContent);
		super.cancelPressed();
	}
	
	/*
	 * @see Dialog#buttonPressed
	 */
	protected void buttonPressed(int buttonId) {
		if (buttonId == ADD_ID){
			addSelectedInterface();
		}
		super.buttonPressed(buttonId);	
	}
	
	/*
	 * @see Dialog#okPressed
	 */
	protected void okPressed() {
		addSelectedInterface();
		super.okPressed();
	}
		
	private void addSelectedInterface(){
		Object ref= getLowerSelectedElement();
		if (ref instanceof TypeInfo) {
			String qualifiedName= ((TypeInfo) ref).getFullyQualifiedName();
			fList.addElement(qualifiedName);
			String message= NewWizardMessages.getFormattedString("SuperInterfaceSelectionDialog.interfaceadded.info", qualifiedName); //$NON-NLS-1$
			updateStatus(new StatusInfo(IStatus.INFO, message));
		}
	}
	
	private static IJavaSearchScope createSearchScope(IJavaProject p) {
		return SearchEngine.createJavaSearchScope(new IJavaProject[] { p });
	}
	
	/*
	 * @see AbstractElementListSelectionDialog#handleDefaultSelected()
	 */
	protected void handleDefaultSelected() {
		if (validateCurrentSelection())
			buttonPressed(ADD_ID);
	}

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		WorkbenchHelp.setHelp(newShell, IJavaHelpContextIds.SUPER_INTERFACE_SELECTION_DIALOG);
	}


}

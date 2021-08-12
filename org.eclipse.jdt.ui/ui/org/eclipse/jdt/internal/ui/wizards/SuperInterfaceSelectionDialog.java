/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards;

import java.util.List;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.TypeNameMatch;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.OpenTypeSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;

/**
 * A type selection dialog providing means to open interface(s).
 */
public class SuperInterfaceSelectionDialog extends OpenTypeSelectionDialog {

	private static final int ADD_ID= IDialogConstants.CLIENT_ID + 1;

	private NewTypeWizardPage fTypeWizardPage;
	private List<String> fOldContent;

	/**
	 * Creates new instance of SuperInterfaceSelectionDialog
	 *
	 * @param parent
	 *            shell to parent the dialog on
	 * @param context
	 *            context used to execute long-running operations associated
	 *            with this dialog
	 * @param page
	 *            page that opened this dialog
	 * @param p
	 *            the java project which will be considered when searching for
	 *            interfaces
	 */
	public SuperInterfaceSelectionDialog(Shell parent, IRunnableContext context, NewTypeWizardPage page, IJavaProject p) {
		super(parent, true, context, createSearchScope(p), IJavaSearchConstants.INTERFACE);
		fTypeWizardPage= page;
		// to restore the content of the dialog field if the dialog is canceled
		fOldContent= fTypeWizardPage.getSuperInterfaces();
		setStatusLineAboveButtons(true);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, ADD_ID, NewWizardMessages.SuperInterfaceSelectionDialog_addButton_label, true);
		super.createButtonsForButtonBar(parent);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		return JavaPlugin.getDefault().getDialogSettingsSection("DialogBounds_SuperInterfaceSelectionDialog"); //$NON-NLS-1$
	}

	@Override
	protected void updateButtonsEnableState(IStatus status) {
		super.updateButtonsEnableState(status);
		Button addButton= getButton(ADD_ID);
		if (addButton != null && !addButton.isDisposed())
			addButton.setEnabled(!status.matches(IStatus.ERROR));
	}

	@Override
	protected void handleShellCloseEvent() {
		super.handleShellCloseEvent();
		// Handle the closing of the shell by selecting the close icon
		fTypeWizardPage.setSuperInterfaces(fOldContent, true);
	}

	@Override
	protected void cancelPressed() {
		fTypeWizardPage.setSuperInterfaces(fOldContent, true);
		super.cancelPressed();
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == ADD_ID) {
			addSelectedInterfaces();
		} else {
			super.buttonPressed(buttonId);
		}
	}

	@Override
	protected void okPressed() {
		addSelectedInterfaces();
		super.okPressed();
	}

	/*
	 * Adds selected interfaces to the list.
	 */
	private void addSelectedInterfaces() {
		StructuredSelection selection= getSelectedItems();
		if (selection == null)
			return;
		for (Object obj : selection) {
			if (obj instanceof TypeNameMatch) {
				accessedHistoryItem(obj);
				TypeNameMatch type= (TypeNameMatch) obj;
				IType ttype= type.getType();
				String qualifiedName= getNameWithTypeParameters(ttype);
				String message;

				if (fTypeWizardPage.addSuperInterface(qualifiedName, ttype)) {
					message= Messages.format(NewWizardMessages.SuperInterfaceSelectionDialog_interfaceadded_info, BasicElementLabels.getJavaElementName(qualifiedName));
				} else {
					message= Messages.format(NewWizardMessages.SuperInterfaceSelectionDialog_interfacealreadyadded_info, BasicElementLabels.getJavaElementName(qualifiedName));
				}
				updateStatus(new StatusInfo(IStatus.INFO, message));
			}
		}
	}

	/*
	 * Creates a searching scope including only one project.
	 */
	private static IJavaSearchScope createSearchScope(IJavaProject p) {
		return SearchEngine.createJavaSearchScope(new IJavaProject[] { p });
	}

	@Override
	protected void handleDoubleClick() {
		buttonPressed(ADD_ID);
	}

	@Override
	protected void handleSelected(StructuredSelection selection) {
		super.handleSelected(selection);

		if (selection.size() == 0 && fTypeWizardPage.getSuperInterfaces().size() > fOldContent.size()) {
			// overrides updateStatus() from handleSelected() if
			// list of super interfaces was modified
			// the <code>super.handleSelected(selection)</code> has to be
			// called, because superclass implementation of this class updates
			// state of the table.

			updateStatus(Status.OK_STATUS);

			getButton(ADD_ID).setEnabled(false);
		} else {
			// if selection isn't empty, the add button should be enabled in
			// exactly the same scenarios as the OK button
			getButton(ADD_ID).setEnabled(getButton(OK).isEnabled());
		}
	}


	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.SUPER_INTERFACE_SELECTION_DIALOG);
	}

	public static String getNameWithTypeParameters(IType type) {
		String superName= type.getFullyQualifiedName('.');
		if (!JavaModelUtil.is50OrHigher(type.getJavaProject())) {
			return superName;
		}
		try {
			ITypeParameter[] typeParameters= type.getTypeParameters();
			if (typeParameters.length > 0) {
				StringBuilder buf= new StringBuilder(superName);
				buf.append('<');
				for (int k= 0; k < typeParameters.length; k++) {
					if (k != 0) {
						buf.append(',').append(' ');
					}
					buf.append(typeParameters[k].getElementName());
				}
				buf.append('>');
				return buf.toString();
			}
		} catch (JavaModelException e) {
			// ignore
		}
		return superName;

	}

}

/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.exampleprojects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.wizard.WizardPage;

public class ExampleProjectCreationWizardPage extends WizardPage {

	private IStatus fCurrStatus;

	private boolean fPageVisible;

	private final IConfigurationElement fConfigurationElement;

	private final String fNameLabel;
	private String fProjectName;

	private Text fTextControl;

	public ExampleProjectCreationWizardPage(int pageNumber, IConfigurationElement elem) {
		super("page" + pageNumber); //$NON-NLS-1$
		fCurrStatus= createStatus(IStatus.OK, ""); //$NON-NLS-1$

		fConfigurationElement= elem;

		setTitle(getAttribute(elem, "pagetitle")); //$NON-NLS-1$
		setDescription(getAttribute(elem, "pagedescription")); //$NON-NLS-1$

		fNameLabel= getAttribute(elem, "label"); //$NON-NLS-1$
		fProjectName= getAttribute(elem, "name");		 //$NON-NLS-1$

	}

	private String getAttribute(IConfigurationElement elem, String tag) {
		String res= elem.getAttribute(tag);
		if (res == null) {
			return '!' + tag + '!';
		}
		return res;
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout gd= new GridLayout();
		gd.numColumns= 2;
		composite.setLayout(gd);

		Label label= new Label(composite, SWT.LEFT);
		label.setText(fNameLabel);
		label.setLayoutData(new GridData());

		fTextControl= new Text(composite, SWT.SINGLE | SWT.BORDER);
		fTextControl.setText(fProjectName);
		fTextControl.setSelection(fProjectName.length());
		fTextControl.addModifyListener(e -> {
			if (!fTextControl.isDisposed()) {
				validateText(fTextControl.getText());
			}
		});
		fTextControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fTextControl.setFocus();

		setControl(composite);
		Dialog.applyDialogFont(composite);

		validateText(fProjectName);
	}

	private void validateText(String text) {
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		IStatus status= workspace.validateName(text, IResource.PROJECT);
		if (status.isOK()) {
			if (workspace.getRoot().getProject(text).exists()) {
				status= createStatus(IStatus.ERROR, ExampleProjectMessages.ExampleProjectCreationWizardPage_error_alreadyexists);
			}
		}
		updateStatus(status);

		fProjectName= text;
	}


	/*
	 * @see WizardPage#becomesVisible
	 */
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		fPageVisible= visible;
		// policy: wizards are not allowed to come up with an error message
		if (visible && fCurrStatus.matches(IStatus.ERROR)) {
			// keep the error state, but remove the message
			fCurrStatus= createStatus(IStatus.ERROR, ""); //$NON-NLS-1$
		}
		updateStatus(fCurrStatus);
	}

	/*
	 * Updates the status line and the ok button depending on the status
	 */
	private void updateStatus(IStatus status) {
		fCurrStatus= status;
		setPageComplete(!status.matches(IStatus.ERROR));
		if (fPageVisible) {
			applyToStatusLine(this, status);
		}
	}

	/*
	 * Applies the status to a dialog page
	 */
	private static void applyToStatusLine(DialogPage page, IStatus status) {
		String errorMessage= null;
		String warningMessage= null;
		String statusMessage= status.getMessage();
		if (statusMessage.length() > 0) {
			if (status.matches(IStatus.ERROR)) {
				errorMessage= statusMessage;
			} else if (!status.isOK()) {
				warningMessage= statusMessage;
			}
		}
		page.setErrorMessage(errorMessage);
		page.setMessage(warningMessage);
	}


	private static IStatus createStatus(int severity, String message) {
		return new Status(severity, ExampleProjectsPlugin.getPluginId(), severity, message, null);
	}

	/**
	 * @return Returns the name entered by the user
	 */
	@Override
	public String getName() {
		return fProjectName;
	}

	/**
	 * Returns the configuration element of this page.
	 * @return Returns a IConfigurationElement
	 */
	public IConfigurationElement getConfigurationElement() {
		return fConfigurationElement;
	}

}


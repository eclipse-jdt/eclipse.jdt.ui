/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Sebastian Davids <sdavids@gmx.de> bug 38692
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javadocexport;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.dialogs.Dialog;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.preferences.JavadocPreferencePage;

public class JavadocCommandWizardPage extends JavadocWizardPage {

	private JavadocPreferencePage fPage;

	protected JavadocCommandWizardPage(String pageName) {
		super(pageName);

		setDescription(JavadocExportMessages.getString("JavadocCommandWizardPage.description")); //$NON-NLS-1$
	}

	/*
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(new FillLayout());

		fPage= new JavadocPreferencePage() {
			public void createControl(Composite parentComposite) {
				noDefaultAndApplyButton();
				super.createControl(parentComposite);
			}

			protected void updateStatus(IStatus status) {
				if (status.matches(IStatus.INFO)) {
					status= new StatusInfo(IStatus.ERROR, status.getMessage());
				}
				JavadocCommandWizardPage.this.updateStatus(status);
			}
		};
		fPage.createControl(composite);

		setControl(composite);
		Dialog.applyDialogFont(composite);
		WorkbenchHelp.setHelp(composite, IJavaHelpContextIds.JAVADOC_COMMAND_PAGE);
	}

	public void init() {
		updateStatus(new StatusInfo(IStatus.ERROR, "")); //$NON-NLS-1$
	}

	protected void finish() {
		fPage.performOk();
	}
}

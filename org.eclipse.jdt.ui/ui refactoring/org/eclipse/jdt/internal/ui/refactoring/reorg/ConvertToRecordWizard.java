/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.Dialog;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

import org.eclipse.jdt.internal.corext.refactoring.code.ConvertToRecordRefactoring;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

public class ConvertToRecordWizard extends RefactoringWizard {

	public ConvertToRecordWizard(ConvertToRecordRefactoring refactoring) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE);
		setDefaultPageTitle(RefactoringMessages.ConvertToRecordWizard_defaultPageTitle);
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
	}
	@Override
	protected void addUserInputPages() {
		addPage(new ConvertToRecordPage());
	}

	private class ConvertToRecordPage extends UserInputWizardPage{

		public static final String PAGE_NAME= "ConvertToRecordInputPage";//$NON-NLS-1$

		public ConvertToRecordPage() {
			super(PAGE_NAME);
		}

		@Override
		public void createControl(Composite parent) {
			Composite composite= new Composite(parent, SWT.NONE);
			setControl(composite);
			composite.setLayout(new GridLayout());
			composite.setLayoutData(new GridData());

			Label label= new Label(composite, SWT.NONE);
			label.setText(Messages.format(
					RefactoringMessages.ConvertToRecordWizard_message,
					((ConvertToRecordRefactoring) getRefactoring()).getClassName()));
			label.setLayoutData(new GridData());

			Dialog.applyDialogFont(composite);
		}
	}


}

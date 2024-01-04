/*******************************************************************************
 * Copyright (c) 2023 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.Dialog;

import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;

class MakeStaticInputPage extends UserInputWizardPage {

	public MakeStaticInputPage() {
		super(RefactoringCoreMessages.MakeStaticRefactoring_name);
	}

	@Override
	public void createControl(Composite parent) {
		Composite mainPart= new Composite(parent, SWT.NONE);
		mainPart.setLayout(new GridLayout(2, false));
		GridData layoutData= new GridData(SWT.FILL, SWT.FILL, true, true);
		mainPart.setLayoutData(layoutData);
		mainPart.setFont(parent.getFont());


		Label description= new Label(mainPart, SWT.NONE);
		description.setText(RefactoringMessages.MakeStaticAction_description);
		description.setLayoutData(new GridData());
		setControl(mainPart);

		Dialog.applyDialogFont(mainPart);
	}
}

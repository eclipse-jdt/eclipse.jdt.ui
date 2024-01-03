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

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import org.eclipse.jdt.internal.corext.refactoring.code.MakeStaticRefactoring;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * The MakeStaticWizard class represents a wizard for performing the "Make Static" refactoring
 * operation.
 *
 * @since 3.29
 */
public class MakeStaticWizard extends RefactoringWizard {

	/**
	 * Constructs a new MakeStaticWizard with the specified MakeStaticRefactoring and page title.
	 *
	 * @param refactoring the MakeStaticRefactoring object representing the refactoring operation
	 * @param pagetitle the title to be set as the default page title
	 */
	public MakeStaticWizard(MakeStaticRefactoring refactoring, String pagetitle) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		setDefaultPageTitle(pagetitle);
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
	}

	@Override
	protected void addUserInputPages() {
		addPage(new MakeStaticInputPage());
	}
}

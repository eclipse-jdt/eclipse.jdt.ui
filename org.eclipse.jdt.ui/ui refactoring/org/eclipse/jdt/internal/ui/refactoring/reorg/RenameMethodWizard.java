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
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import org.eclipse.ltk.core.refactoring.Refactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

public class RenameMethodWizard extends RenameRefactoringWizard {

	public RenameMethodWizard(Refactoring refactoring) {
		super(refactoring,
			RefactoringMessages.RenameMethodWizard_defaultPageTitle,
			RefactoringMessages.RenameMethodWizard_inputPage_description,
			JavaPluginImages.DESC_WIZBAN_REFACTOR_METHOD,
			IJavaHelpContextIds.RENAME_METHOD_WIZARD_PAGE);
	}
}

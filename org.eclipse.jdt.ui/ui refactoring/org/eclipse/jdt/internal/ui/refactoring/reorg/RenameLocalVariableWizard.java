/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

import org.eclipse.ltk.core.refactoring.Refactoring;

public final class RenameLocalVariableWizard extends RenameRefactoringWizard {

	public RenameLocalVariableWizard(Refactoring refactoring) {
		super(
				refactoring,
				RefactoringMessages.getString("RenameLocalVariableWizard.defaultPageTitle"), //$NON-NLS-1$
				RefactoringMessages.getString("RenameTypeParameterWizard.inputPage.description"), //$NON-NLS-1$
				JavaPluginImages.DESC_WIZBAN_REFACTOR, IJavaHelpContextIds.RENAME_LOCAL_VARIABLE_WIZARD_PAGE); //$NON-NLS-1$
	}
}

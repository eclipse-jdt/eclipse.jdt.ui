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

/**
 * Wizard for the rename type parameter refactoring.
 */
public final class RenameTypeParameterWizard extends RenameRefactoringWizard {

	/**
	 * Creates a new rename type parameter wizard.
	 * 
	 * @param refactoring
	 *        the refactoring to create the wizard for
	 */
	public RenameTypeParameterWizard(Refactoring refactoring) {
		super(refactoring, RefactoringMessages.getString("RenameTypeParameterWizard.defaultPageTitle"), RefactoringMessages.getString("RenameTypeParameterWizard.inputPage.description"), JavaPluginImages.DESC_WIZBAN_REFACTOR, IJavaHelpContextIds.RENAME_TYPE_PARAMETER_WIZARD_PAGE); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

import org.eclipse.ltk.core.refactoring.Refactoring;

public class RenamePackageWizard extends RenameRefactoringWizard {
	
	public RenamePackageWizard(Refactoring refactoring) {
		super(refactoring, 
			RefactoringMessages.getString("RenamePackageWizard.defaultPageTitle"), //$NON-NLS-1$
			RefactoringMessages.getString("RenamePackageWizard.inputPage.description"), //$NON-NLS-1$
			JavaPluginImages.DESC_WIZBAN_REFACTOR_PACKAGE,
			IJavaHelpContextIds.RENAME_PACKAGE_WIZARD_PAGE);
	}
}

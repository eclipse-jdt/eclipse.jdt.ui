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
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class RenameCuWizard extends RenameRefactoringWizard {
	
	public RenameCuWizard(Refactoring refactoring) {
		super(refactoring,
			RefactoringMessages.getString("RenameCuWizard.defaultPageTitle"), //$NON-NLS-1$
			RefactoringMessages.getString("RenameCuWizard.inputPage.description"), //$NON-NLS-1$
			JavaPluginImages.DESC_WIZBAN_REFACTOR_CU,
			IJavaHelpContextIds.RENAME_CU_WIZARD_PAGE);
	}
	
	protected RefactoringStatus validateNewName(String newName) {
		String fullName= newName + ".java";  //$NON-NLS-1$
		return super.validateNewName(fullName);
	}	
}

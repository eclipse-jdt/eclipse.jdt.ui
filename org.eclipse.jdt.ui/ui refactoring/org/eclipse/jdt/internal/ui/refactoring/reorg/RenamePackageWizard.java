/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

import org.eclipse.ltk.core.refactoring.Refactoring;

public class RenamePackageWizard extends RenameRefactoringWizard {
	
	public RenamePackageWizard(Refactoring refactoring) {
		super(refactoring, 
			RefactoringMessages.RenamePackageWizard_defaultPageTitle, 
			RefactoringMessages.RenamePackageWizard_inputPage_description, 
			JavaPluginImages.DESC_WIZBAN_REFACTOR_PACKAGE,
			IJavaHelpContextIds.RENAME_PACKAGE_WIZARD_PAGE);
	}
}

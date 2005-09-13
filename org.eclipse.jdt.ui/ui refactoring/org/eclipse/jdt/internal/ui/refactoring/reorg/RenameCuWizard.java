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


import org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class RenameCuWizard extends RenameRefactoringWizard {
	
	private static final String JAVA_FILE_EXT= ".java";  //$NON-NLS-1$
	
	public RenameCuWizard(Refactoring refactoring) {
		super(refactoring,
			RefactoringMessages.RenameCuWizard_defaultPageTitle, 
			RefactoringMessages.RenameCuWizard_inputPage_description, 
			JavaPluginImages.DESC_WIZBAN_REFACTOR_CU,
			IJavaHelpContextIds.RENAME_CU_WIZARD_PAGE);
	}
	
	protected RefactoringStatus validateNewName(String newName) {
		String fullName= newName + ".java";  //$NON-NLS-1$
		return super.validateNewName(fullName);
	}
	
	protected RenameInputWizardPage createInputPage(String message, String initialSetting) {
		return new RenameInputWizardPage(message, IJavaHelpContextIds.RENAME_CU_WIZARD_PAGE, true, initialSetting) {
			protected RefactoringStatus validateTextField(String text) {
				return validateNewName(text);
			}
			protected String getNewName(INameUpdating nameUpdating) {
				String result= nameUpdating.getNewElementName();
				// If renaming a CU we have to remove the java file extension
				if (result.endsWith(JAVA_FILE_EXT))
					result= result.substring(0, result.length() - JAVA_FILE_EXT.length());
				return result;
			}
		};
	}
}

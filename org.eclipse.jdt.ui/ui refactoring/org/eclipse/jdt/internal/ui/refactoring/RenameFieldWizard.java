/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring;

class RenameFieldWizard extends RenameRefactoringWizard {

	public RenameFieldWizard(IRenameRefactoring ref, String title, String message, String pageContextHelpId, String errorContextHelpId) {
		super(ref, title, message, pageContextHelpId, errorContextHelpId);
	}

	/* non java-doc
	 * @see RenameRefactoringWizard#createInputPage
	 */ 
	protected RenameInputWizardPage createInputPage(String message, String initialSetting) {
		return new RenameFieldInputWizardPage(message, getPageContextHelpId(), initialSetting) {
			protected RefactoringStatus validateTextField(String text) {
				RefactoringStatus result= validateNewName(text);
				updateGetterSetterLabels();
				return result;
			}	
		};
	}
}

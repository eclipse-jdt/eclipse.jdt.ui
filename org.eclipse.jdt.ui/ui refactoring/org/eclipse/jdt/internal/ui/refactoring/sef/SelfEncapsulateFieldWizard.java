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
package org.eclipse.jdt.internal.ui.refactoring.sef;

import org.eclipse.jdt.internal.corext.refactoring.sef.SelfEncapsulateFieldRefactoring;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

import org.eclipse.ltk.internal.ui.refactoring.PreviewWizardPage;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

public class SelfEncapsulateFieldWizard extends RefactoringWizard {
	
	/* package */ static final String DIALOG_SETTING_SECTION= "SelfEncapsulateFieldWizard"; //$NON-NLS-1$
	
	public SelfEncapsulateFieldWizard(SelfEncapsulateFieldRefactoring refactoring) {
		super(refactoring, RefactoringMessages.getString("SelfEncapsulateField.sef")); //$NON-NLS-1$
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
	}

	protected void addUserInputPages() {
		addPage(new SelfEncapsulateFieldInputPage());
	}

	protected void addPreviewPage() {
		PreviewWizardPage page= new PreviewWizardPage();
		addPage(page);
	}
}

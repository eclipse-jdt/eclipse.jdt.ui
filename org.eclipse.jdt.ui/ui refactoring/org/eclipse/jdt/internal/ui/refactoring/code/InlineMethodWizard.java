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
package org.eclipse.jdt.internal.ui.refactoring.code;

import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

public class InlineMethodWizard extends RefactoringWizard {
	
	/* package */ static final String DIALOG_SETTING_SECTION= "InlineMethodWizard"; //$NON-NLS-1$
	
	public InlineMethodWizard(InlineMethodRefactoring ref){
		super(ref, DIALOG_BASED_UESR_INTERFACE);
		setDefaultPageTitle(RefactoringMessages.getString("InlineMethodWizard.page_title"));  //$NON-NLS-1$
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
	}

	protected void addUserInputPages(){
		addPage(new InlineMethodInputPage());
	}
}

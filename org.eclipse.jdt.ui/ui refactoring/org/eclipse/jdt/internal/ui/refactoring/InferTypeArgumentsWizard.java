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

package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.Dialog;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

public class InferTypeArgumentsWizard extends RefactoringWizard {

	public InferTypeArgumentsWizard(Refactoring refactoring) {
		super(refactoring, DIALOG_BASED_UESR_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		setDefaultPageTitle(RefactoringMessages.getString("InferTypeArgumentsWizard.defaultPageTitle")); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.ltk.ui.refactoring.RefactoringWizard#addUserInputPages()
	 */
	protected void addUserInputPages() {
		addPage(new InferTypeArgumentsInputPage());
	}

	private static class InferTypeArgumentsInputPage extends UserInputWizardPage {
		public static final String PAGE_NAME= "InferTypeArgumentsInputPage"; //$NON-NLS-1$

		private static final String DESCRIPTION= RefactoringMessages.getString("InferTypeArgumentsInputPage.description"); //$NON-NLS-1$
    
		public InferTypeArgumentsInputPage() {
			super(PAGE_NAME);
			setDescription(DESCRIPTION);
		}

		public void createControl(Composite parent) {
			Composite result= new Composite(parent, SWT.NONE);
			setControl(result);
			GridLayout layout= new GridLayout();
			layout.numColumns= 2;
			layout.verticalSpacing= 8;
			result.setLayout(layout);
			
			Label doit= new Label(result, SWT.WRAP);
			doit.setText(RefactoringMessages.getString("InferTypeArgumentsWizard.lengthyDescription") + //$NON-NLS-1$
					RefactoringMessages.getString("InferTypeArgumentsWizard.workInProgress")); //$NON-NLS-1$
			doit.setLayoutData(new GridData());
			
			updateStatus();
			Dialog.applyDialogFont(result);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.INTRODUCE_PARAMETER_WIZARD_PAGE);
		}

		private void updateStatus() {
			setPageComplete(true);
			//TODO: validate?
		}
	}
}

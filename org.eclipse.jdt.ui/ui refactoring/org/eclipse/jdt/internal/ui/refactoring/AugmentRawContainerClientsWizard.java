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

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.corext.refactoring.generics.AugmentRawContainerClientsRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

public class AugmentRawContainerClientsWizard extends RefactoringWizard {

	public AugmentRawContainerClientsWizard(Refactoring refactoring) {
		super(refactoring, DIALOG_BASED_UESR_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		setDefaultPageTitle(RefactoringMessages.getString("AugmentRawContainerClientsWizard.defaultPageTitle")); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.ltk.ui.refactoring.RefactoringWizard#addUserInputPages()
	 */
	protected void addUserInputPages() {
		addPage(new AugmentRawContainerClientsInputPage());
	}

	private static class AugmentRawContainerClientsInputPage extends UserInputWizardPage {
		public static final String PAGE_NAME= "AugmentRawContainerClientsInputPage"; //$NON-NLS-1$

		private static final String DESCRIPTION= RefactoringMessages.getString("AugmentRawContainerClientsInputPage.description"); //$NON-NLS-1$
    
		public AugmentRawContainerClientsInputPage() {
			super(PAGE_NAME);
			setDescription(DESCRIPTION);
		}

		private AugmentRawContainerClientsRefactoring getAugmentRawContainerClientsRefactoring(){
			return (AugmentRawContainerClientsRefactoring) getRefactoring();
		}

		public void createControl(Composite parent) {
			Composite result= new Composite(parent, SWT.NONE);
			setControl(result);
			GridLayout layout= new GridLayout();
			layout.numColumns= 2;
			layout.verticalSpacing= 8;
			result.setLayout(layout);
			
			Label doit= new Label(result, SWT.WRAP);
			doit.setText("Infer generic type parameters for Collection classes and remove unnecessary casts.\n\n" +
					"Note: This refactoring is work in progress.");
			doit.setLayoutData(new GridData());
			
			updateStatus();
			Dialog.applyDialogFont(result);
			WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.INTRODUCE_PARAMETER_WIZARD_PAGE);
		}

		private void updateStatus() {
			setPageComplete(true);
//			setPageComplete(getAugmentRawContainerClientsRefactoring().validateInput());
		}
	}
}

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
package org.eclipse.jdt.internal.ui.refactoring.nls;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardContainer;

import org.eclipse.jdt.internal.corext.refactoring.nls.NLSRefactoring;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

import org.eclipse.ltk.ui.refactoring.IRefactoringWizardDialog;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

/**
 * good citizen problems - wizard is only valid after constructor (when the pages toggle
 * some values and force an validate the validate cant get a wizard)
 */
public class ExternalizeWizard extends RefactoringWizard {

	public ExternalizeWizard(NLSRefactoring refactoring) {
		super(refactoring, NLSUIMessages.getFormattedString("wizard.page.title", refactoring.getCu().getElementName())); //$NON-NLS-1$
		setWindowTitle(NLSUIMessages.getString("wizard.name"));//$NON-NLS-1$
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_EXTERNALIZE_STRINGS);
	}

	/**
	 * @see RefactoringWizard#hasMultiPageUserInput
	 */
	public boolean hasMultiPageUserInput() {
		return true;
	}

	/**
	 * @see RefactoringWizard#addUserInputPages
	 */
	protected void addUserInputPages() {

		NLSRefactoring nlsRefac= (NLSRefactoring)getRefactoring();
		ExternalizeWizardPage page= new ExternalizeWizardPage(nlsRefac);
		page.setMessage(NLSUIMessages.getString("wizard.select")); //$NON-NLS-1$
		addPage(page);

		ExternalizeWizardPage2 page2= new ExternalizeWizardPage2(nlsRefac);
		page2.setMessage(NLSUIMessages.getString("wizard.select_values")); //$NON-NLS-1$
		addPage(page2);
	}

	/**
	 * @see RefactoringWizard#checkActivationOnOpen
	 */
	protected boolean checkActivationOnOpen() {
		return true;
	}

	/**
	 * @see IWizard#setContainer(IWizardContainer)
	 */
	public void setContainer(IWizardContainer wizardContainer) {
		super.setContainer(wizardContainer);
		if (wizardContainer instanceof IRefactoringWizardDialog) {
			IRefactoringWizardDialog dialog= (IRefactoringWizardDialog)wizardContainer;
			dialog.makeNextButtonDefault();
		}
	}

}
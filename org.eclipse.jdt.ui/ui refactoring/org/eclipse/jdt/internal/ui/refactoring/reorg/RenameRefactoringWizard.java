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

package org.eclipse.jdt.internal.ui.refactoring.reorg;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating;

import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

public class RenameRefactoringWizard extends RefactoringWizard {
	
	private final String fInputPageDescription;
	private final String fPageContextHelpId;
	private final ImageDescriptor fInputPageImageDescriptor;
	
	public RenameRefactoringWizard(Refactoring refactoring, String defaultPageTitle, String inputPageDescription, 
			ImageDescriptor inputPageImageDescriptor, String pageContextHelpId) {
		super(refactoring, DIALOG_BASED_UESR_INTERFACE);
		setDefaultPageTitle(defaultPageTitle);
		fInputPageDescription= inputPageDescription;
		fInputPageImageDescriptor= inputPageImageDescriptor;
		fPageContextHelpId= pageContextHelpId;
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages() {
		String initialSetting= getNameUpdating().getCurrentElementName();
		RenameInputWizardPage inputPage= createInputPage(fInputPageDescription, initialSetting);
		inputPage.setImageDescriptor(fInputPageImageDescriptor);
		addPage(inputPage);
	}

	private INameUpdating getNameUpdating() {
		return (INameUpdating)getRefactoring().getAdapter(INameUpdating.class);	
	}
	
	protected RenameInputWizardPage createInputPage(String message, String initialSetting) {
		return new RenameInputWizardPage(message, fPageContextHelpId, true, initialSetting) {
			protected RefactoringStatus validateTextField(String text) {
				return validateNewName(text);
			}	
		};
	}
	
	protected RefactoringStatus validateNewName(String newName) {
		INameUpdating ref= getNameUpdating();
		ref.setNewElementName(newName);
		try{
			return ref.checkNewElementName(newName);
		} catch (CoreException e){
			//XXX: should log the exception
			String msg= e.getMessage() == null ? "": e.getMessage(); //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(RefactoringMessages.getFormattedString("RenameRefactoringWizard.internal_error", msg));//$NON-NLS-1$
		}	
	}
}

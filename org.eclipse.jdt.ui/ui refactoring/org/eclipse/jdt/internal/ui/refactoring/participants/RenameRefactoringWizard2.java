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

package org.eclipse.jdt.internal.ui.refactoring.participants;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring;

public class RenameRefactoringWizard2 extends RefactoringWizard {
	
	private final String fPageMessage;
	private final String fPageContextHelpId;
	private ImageDescriptor fInputPageImageDescriptor;
	
	public RenameRefactoringWizard2(IRenameRefactoring ref, String title, String message, String pageContextHelpId, String errorContextHelpId){
		super((Refactoring) ref, title, errorContextHelpId);
		fPageMessage= message;
		fPageContextHelpId= pageContextHelpId;
	}
	
	public void setInputPageImageDescriptor(ImageDescriptor desc){
		fInputPageImageDescriptor= desc;
	}
	
	protected String getPageContextHelpId() {
		return fPageContextHelpId;
	}
	
	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		String initialSetting= getRenameRefactoring().getCurrentName();
		setPageTitle(getPageTitle());
		RenameInputWizardPage2 inputPage= createInputPage(fPageMessage, initialSetting);
		inputPage.setImageDescriptor(fInputPageImageDescriptor);
		addPage(inputPage);
	}

	protected RenameInputWizardPage2 createInputPage(String message, String initialSetting) {
		return new RenameInputWizardPage2(message, fPageContextHelpId, true, initialSetting) {
			protected RefactoringStatus validateTextField(String text) {
				return validateNewName(text);
			}	
		};
	}
	
	private IRenameRefactoring getRenameRefactoring(){
		return (IRenameRefactoring)getRefactoring();	
	}
	
	protected RefactoringStatus validateNewName(String newName){
		IRenameRefactoring ref= getRenameRefactoring();
		ref.setNewName(newName);
		try{
			return ref.checkNewName(newName);
		} catch (JavaModelException e){
			//XXX: should log the exception
			String msg= e.getMessage() == null ? "": e.getMessage(); //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(RefactoringMessages.getFormattedString("RenameRefactoringWizard.internal_error", msg));//$NON-NLS-1$
		}	
	}
}

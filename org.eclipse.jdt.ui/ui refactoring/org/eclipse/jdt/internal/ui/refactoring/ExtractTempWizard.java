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

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.code.ExtractTempRefactoring;

public class ExtractTempWizard extends RefactoringWizard {

	public ExtractTempWizard(ExtractTempRefactoring ref, String pageTitle, String errorPageContextHelpId) {
		super(ref, pageTitle, errorPageContextHelpId);
		setExpandFirstNode(true);
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		try {
			addPage(new ExtractTempInputPage(getExtractTempRefactoring().guessTempName()));
		} catch (JavaModelException e) {
			addPage(new ExtractTempInputPage("")); //$NON-NLS-1$
		}
	}
	
	private ExtractTempRefactoring getExtractTempRefactoring(){
		return (ExtractTempRefactoring)getRefactoring();
	}
	
}

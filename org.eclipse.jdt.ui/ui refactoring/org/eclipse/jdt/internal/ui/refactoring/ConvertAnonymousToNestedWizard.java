/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.internal.corext.refactoring.code.ConvertAnonymousToNestedRefactoring;

public class ConvertAnonymousToNestedWizard extends RefactoringWizard {

	public ConvertAnonymousToNestedWizard(ConvertAnonymousToNestedRefactoring ref, String pageTitle, String errorPageContextHelpId) {
		super(ref, pageTitle, errorPageContextHelpId);
		setExpandFirstNode(true);
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new ConvertAnonymousToNestedInputPage());
	}
	
	private ConvertAnonymousToNestedRefactoring getPromoteTempRefactoring(){
		return (ConvertAnonymousToNestedRefactoring)getRefactoring();
	}
	
}

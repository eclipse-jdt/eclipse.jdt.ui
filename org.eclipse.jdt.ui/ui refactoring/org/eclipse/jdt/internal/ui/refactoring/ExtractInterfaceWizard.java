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

import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractInterfaceRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

public class ExtractInterfaceWizard extends RefactoringWizard {
	
	public ExtractInterfaceWizard(ExtractInterfaceRefactoring ref) {
		super(ref, RefactoringMessages.getString("ExtractInterfaceWizard.Extract_Interface"), IJavaHelpContextIds.EXTRACT_INTERFACE_ERROR_WIZARD_PAGE); //$NON-NLS-1$
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new ExtractInterfaceInputPage());
	}
}

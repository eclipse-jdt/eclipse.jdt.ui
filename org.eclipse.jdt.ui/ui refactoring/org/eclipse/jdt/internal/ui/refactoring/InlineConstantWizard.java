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

import org.eclipse.jdt.internal.corext.refactoring.code.InlineConstantRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

public class InlineConstantWizard extends RefactoringWizard {

	private static final String MESSAGE = RefactoringMessages.getString("InlineConstantWizard.message"); //$NON-NLS-1$

	public InlineConstantWizard(InlineConstantRefactoring ref) {
		super(ref, RefactoringMessages.getString("InlineConstantWizard.Inline_Constant"), IJavaHelpContextIds.INLINE_CONSTANT_ERROR_WIZARD_PAGE); //$NON-NLS-1$
		setExpandFirstNode(true);
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */
	protected void addUserInputPages() {

		String message= null;
		int messageType= IMessageProvider.NONE;			
		if(!getInlineConstantRefactoring().isInitializerAllStaticFinal()) {
			message= RefactoringMessages.getString("InlineConstantWizard.initializer_refers_to_fields"); //$NON-NLS-1$
			messageType= IMessageProvider.INFORMATION;
		} else {	
			message= MESSAGE;
			messageType= IMessageProvider.NONE;
		}
		
		addPage(new InlineConstantInputPage(message, messageType));
	}

	private InlineConstantRefactoring getInlineConstantRefactoring(){
		return (InlineConstantRefactoring)getRefactoring();
	}
	
}

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

import org.eclipse.jdt.internal.corext.refactoring.code.InlineTempRefactoring;

public class InlineTempWizard extends RefactoringWizard {

	public InlineTempWizard(InlineTempRefactoring ref) {
		super(ref, RefactoringMessages.getString("InlineTempWizard.defaultPageTitle")); //$NON-NLS-1$
	}

	protected void addUserInputPages() {
		addPage(new InlineTempInputPage());
	}

	protected int getMessageLineWidthInChars() {
		return 0;
	}
	
	private static class InlineTempInputPage extends MessageWizardPage {

		public static final String PAGE_NAME= "InlineTempInputPage"; //$NON-NLS-1$
	
		public InlineTempInputPage() {
			super(PAGE_NAME, true, MessageWizardPage.STYLE_QUESTION);
		}

		protected String getMessageString() {
			InlineTempRefactoring refactoring= (InlineTempRefactoring)getRefactoring();
			int occurences= refactoring.getReferencesCount();
			if (occurences == 1) 
				return RefactoringMessages.getFormattedString("InlineTempInputPage.message.one",  refactoring.getTempName()); //$NON-NLS-1$
			else
				return RefactoringMessages.getFormattedString("InlineTempInputPage.message.multi",  //$NON-NLS-1$
					new Object[] { new Integer(occurences),  refactoring.getTempName() });
		}
	}
}
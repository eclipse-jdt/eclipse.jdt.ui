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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.Dialog;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

import org.eclipse.jdt.internal.corext.refactoring.code.InlineTempRefactoring;

public class InlineTempWizard extends RefactoringWizard {

	public InlineTempWizard(InlineTempRefactoring ref) {
		super(ref, "Inline Local Variable", IJavaHelpContextIds.INLINE_TEMP_ERROR_WIZARD_PAGE);
	}

	protected void addUserInputPages() {
		addPage(new InlineTempInputPage());
	}

	protected int getMessageLineWidthInChars() {
		return 0;
	}
	
	private static class InlineTempInputPage extends UserInputWizardPage {

		public static final String PAGE_NAME= "InlineTempInputPage"; //$NON-NLS-1$
	
		public InlineTempInputPage() {
			super(PAGE_NAME, true);
		}

		public void createControl(Composite parent) {
			initializeDialogUnits(parent);
			Composite result= new Composite(parent, SWT.NONE);
			setControl(result);
			GridLayout layout= new GridLayout();
			layout.marginWidth= 0; layout.marginHeight= 0;
			result.setLayout(layout);
			Label spacer= new Label(result, SWT.NONE);
			GridData gd= new GridData();
			gd.heightHint= convertHeightInCharsToPixels(1) / 2;
			spacer.setLayoutData(gd);
			Label label= new Label(result, SWT.CENTER | SWT.WRAP);
			label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			InlineTempRefactoring refactoring= (InlineTempRefactoring)getRefactoring();
			int occurences= refactoring.getOccurences();
			String message;
			if (occurences == 1) {
				message= RefactoringMessages.getFormattedString("InlineTempInputPage.message.one",  refactoring.getTempName()); //$NON-NLS-1$
			} else {
				message= RefactoringMessages.getFormattedString("InlineTempInputPage.message.multi",  //$NON-NLS-1$
					new Object[] { new Integer(occurences),  refactoring.getTempName() });
			}
		
			label.setText(message);
			Dialog.applyDialogFont(result);
		}
	}
}
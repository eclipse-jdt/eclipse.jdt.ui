/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others. All
 * rights reserved. This program and the accompanying materials are made
 * available under the terms of the Common Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/cpl-v10.
 * html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jdt.internal.corext.refactoring.code.InlineTempRefactoring;

public class InlineTempInputPage extends UserInputWizardPage {

	public static final String PAGE_NAME= "InlineTempInputPage"; //$NON-NLS-1$
	
	public InlineTempInputPage() {
		super(PAGE_NAME, true);
	}

	public void createControl(Composite parent) {
		Label label= new Label(parent, SWT.CENTER | SWT.WRAP);
		setControl(label);
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
	}
}

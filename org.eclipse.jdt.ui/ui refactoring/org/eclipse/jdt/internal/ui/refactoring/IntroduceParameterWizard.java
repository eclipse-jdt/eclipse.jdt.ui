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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.Dialog;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.corext.refactoring.code.IntroduceParameterRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

public class IntroduceParameterWizard extends RefactoringWizard {

	public IntroduceParameterWizard(IntroduceParameterRefactoring ref) {
		super(ref, DIALOG_BASED_UESR_INTERFACE | PREVIEW_EXPAND_FIRST_NODE); 
		setDefaultPageTitle(RefactoringMessages.getString("IntroduceParameterWizard.defaultPageTitle")); //$NON-NLS-1$
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new IntroduceParameterInputPage());
	}
	
	private static class IntroduceParameterInputPage extends UserInputWizardPage {

		private static final String DESCRIPTION = RefactoringMessages.getString("IntroduceParameterInputPage.description"); //$NON-NLS-1$
		public static final String PAGE_NAME= "IntroduceParameterInputPage";//$NON-NLS-1$
    
		public IntroduceParameterInputPage() {
			super(PAGE_NAME);
			setDescription(DESCRIPTION);
		}

		private IntroduceParameterRefactoring getIntroduceParameterRefactoring(){
			return (IntroduceParameterRefactoring)getRefactoring();
		}

		public void createControl(Composite parent) {
			Composite result= new Composite(parent, SWT.NONE);
			setControl(result);
			GridLayout layout= new GridLayout();
			layout.numColumns= 2;
			layout.verticalSpacing= 8;
			result.setLayout(layout);
			
			Text textField= addParameterNameField(result);
			textField.setText(getIntroduceParameterRefactoring().guessedParameterName());
			textField.selectAll();
			textField.setFocus();

			updateStatus();
			Dialog.applyDialogFont(result);
			WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.INTRODUCE_PARAMETER_WIZARD_PAGE);
		}

		private Text addParameterNameField(Composite parent) {
			Label nameLabel= new Label(parent, SWT.NONE);
			nameLabel.setText(RefactoringMessages.getString("IntroduceParameterInputPage.parameter_name")); //$NON-NLS-1$
			nameLabel.setLayoutData(new GridData());
        
			final Text parameterNameField= new Text(parent, SWT.BORDER | SWT.SINGLE);
			parameterNameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			parameterNameField.addModifyListener(new ModifyListener(){
				public void modifyText(ModifyEvent e) {
					getIntroduceParameterRefactoring().setParameterName(parameterNameField.getText());
					updateStatus();
				}
			});
			return parameterNameField;
		}
	
		private void updateStatus() {
			setPageComplete(getIntroduceParameterRefactoring().validateInput());
		}
	
	
	}
}


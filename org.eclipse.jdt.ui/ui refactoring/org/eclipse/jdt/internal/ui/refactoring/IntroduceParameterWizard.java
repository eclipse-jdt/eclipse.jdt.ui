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
import org.eclipse.jface.text.Assert;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.corext.refactoring.code.IntroduceParameterRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.ControlContentAssistHelper;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.TempNameProcessor;

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
		addPage(new IntroduceParameterInputPage(getIntroduceParameterRefactoring().guessParameterNames()));
	}
	
	private IntroduceParameterRefactoring getIntroduceParameterRefactoring(){
		return (IntroduceParameterRefactoring)getRefactoring();
	}
	
	private static class IntroduceParameterInputPage extends UserInputWizardPage {

		private static final String DESCRIPTION = RefactoringMessages.getString("IntroduceParameterInputPage.description"); //$NON-NLS-1$
		public static final String PAGE_NAME= "IntroduceParameterInputPage";//$NON-NLS-1$
		private String[] fParamNameProposals;
    
		public IntroduceParameterInputPage(String[] tempNameProposals) {
			super(PAGE_NAME);
			setDescription(DESCRIPTION);
			Assert.isNotNull(tempNameProposals);
			fParamNameProposals= tempNameProposals; //$NON-NLS-1$
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
			textField.setText(fParamNameProposals.length == 0 ? "" : fParamNameProposals[0]); //$NON-NLS-1$
			textField.selectAll();
			textField.setFocus();
			ControlContentAssistHelper.createTextContentAssistant(textField, new TempNameProcessor(fParamNameProposals));

			updateStatus();
			Dialog.applyDialogFont(result);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.INTRODUCE_PARAMETER_WIZARD_PAGE);
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


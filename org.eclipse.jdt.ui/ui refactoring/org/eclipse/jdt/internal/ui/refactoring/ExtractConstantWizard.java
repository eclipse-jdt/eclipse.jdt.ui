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

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.corext.refactoring.code.ExtractConstantRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.RowLayouter;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

public class ExtractConstantWizard extends RefactoringWizard {

	private static final String MESSAGE = RefactoringMessages.getString("ExtractConstantInputPage.enter_name"); //$NON-NLS-1$

	public ExtractConstantWizard(ExtractConstantRefactoring ref) {
		super(ref, DIALOG_BASED_UESR_INTERFACE | PREVIEW_EXPAND_FIRST_NODE); 
		setDefaultPageTitle(RefactoringMessages.getString("ExtractConstantWizard.defaultPageTitle")); //$NON-NLS-1$
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */
	protected void addUserInputPages() {

		String message= null;
		int messageType= IMessageProvider.NONE;			
		if(!getExtractConstantRefactoring().selectionAllStaticFinal()) {
			message= RefactoringMessages.getString("ExtractConstantInputPage.selection_refers_to_nonfinal_fields");  //$NON-NLS-1$
			messageType= IMessageProvider.INFORMATION;
		} else {	
			message= MESSAGE;
			messageType= IMessageProvider.NONE;
		}
		
		addPage(new ExtractConstantInputPage(message, messageType, guessName()));
	}

	private String guessName() {
		try {
			return getExtractConstantRefactoring().guessConstantName();
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return "";//default value. no ui here, just log //$NON-NLS-1$
		}
	}

	private ExtractConstantRefactoring getExtractConstantRefactoring(){
		return (ExtractConstantRefactoring)getRefactoring();
	}

	private static class ExtractConstantInputPage extends TextInputWizardPage {

		private Label fLabel;
		private final boolean fInitialValid;
		private final int fOriginalMessageType;
		private final String fOriginalMessage;
	
		public ExtractConstantInputPage(String description, int messageType, String initialValue) {
			super(description, true, initialValue);
			fOriginalMessage= description;
			fOriginalMessageType= messageType;
			fInitialValid= ! ("".equals(initialValue)); //$NON-NLS-1$
		}

		public void createControl(Composite parent) {
			Composite result= new Composite(parent, SWT.NONE);
			setControl(result);
			GridLayout layout= new GridLayout();
			layout.numColumns= 2;
			layout.verticalSpacing= 8;
			result.setLayout(layout);
			RowLayouter layouter= new RowLayouter(2);
		
			Label label= new Label(result, SWT.NONE);
			label.setText(RefactoringMessages.getString("ExtractConstantInputPage.constant_name")); //$NON-NLS-1$
		
			Text text= createTextInputField(result);
			text.selectAll();
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				
			layouter.perform(label, text, 1);
		
			addAccessModifierGroup(result, layouter);
			addReplaceAllCheckbox(result, layouter);
			addQualifyReferencesCheckbox(result, layouter);
			addSeparator(result, layouter);
			addLabel(result, layouter);
		
			validateTextField(text.getText());
		
			Dialog.applyDialogFont(result);
			WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.EXTRACT_CONSTANT_WIZARD_PAGE);		
		}
	
		private void addAccessModifierGroup(Composite result, RowLayouter layouter) {
			Label label= new Label(result, SWT.NONE);
			label.setText(RefactoringMessages.getString("ExtractConstantInputPage.access_modifiers")); //$NON-NLS-1$
		
			Composite group= new Composite(result, SWT.NONE);
			group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			GridLayout layout= new GridLayout();
			layout.numColumns= 4; layout.marginWidth= 0;
			group.setLayout(layout);
		
			String[] labels= new String[] {
				RefactoringMessages.getString("ExtractMethodInputPage.public"),  //$NON-NLS-1$
				RefactoringMessages.getString("ExtractMethodInputPage.protected"), //$NON-NLS-1$
				RefactoringMessages.getString("ExtractMethodInputPage.default"), //$NON-NLS-1$
				RefactoringMessages.getString("ExtractMethodInputPage.private") //$NON-NLS-1$
			};
			String[] data= new String[] { ExtractConstantRefactoring.PUBLIC,
										  ExtractConstantRefactoring.PROTECTED,
										  ExtractConstantRefactoring.PACKAGE,
										  ExtractConstantRefactoring.PRIVATE }; //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$

			String visibility= getExtractConstantRefactoring().getAccessModifier();
			for (int i= 0; i < labels.length; i++) {
				Button radio= new Button(group, SWT.RADIO);
				radio.setText(labels[i]);
				radio.setData(data[i]);
				if (data[i] == visibility)
					radio.setSelection(true);
				radio.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent event) {
						setAccessModifier((String)event.widget.getData());
					}
				});
			}
			layouter.perform(label, group, 1);	
		}
	
		private void addReplaceAllCheckbox(Composite result, RowLayouter layouter) {
			String title= RefactoringMessages.getString("ExtractConstantInputPage.replace_all"); //$NON-NLS-1$
			boolean defaultValue= getExtractConstantRefactoring().replaceAllOccurrences();
			final Button checkBox= createCheckbox(result,  title, defaultValue, layouter);
			getExtractConstantRefactoring().setReplaceAllOccurrences(checkBox.getSelection());
			checkBox.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					getExtractConstantRefactoring().setReplaceAllOccurrences(checkBox.getSelection());
				}
			});		
		}

		private void addQualifyReferencesCheckbox(Composite result, RowLayouter layouter) {
			String title= RefactoringMessages.getString("ExtractConstantInputPage.qualify_constant_references_with_class_name"); //$NON-NLS-1$
			boolean defaultValue= getExtractConstantRefactoring().qualifyReferencesWithDeclaringClassName();
			final Button checkBox= createCheckbox(result,  title, defaultValue, layouter);
			getExtractConstantRefactoring().setQualifyReferencesWithDeclaringClassName(checkBox.getSelection());
			checkBox.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					getExtractConstantRefactoring().setQualifyReferencesWithDeclaringClassName(checkBox.getSelection());
				}
			});	
		}

		private void addLabel(Composite result, RowLayouter layouter) {
			fLabel= new Label(result, SWT.WRAP);
			GridData gd= new GridData(GridData.FILL_BOTH);
			gd.widthHint= convertWidthInCharsToPixels(50);
			fLabel.setLayoutData(gd);
			updatePreviewLabel();
			layouter.perform(fLabel);
		}

		private void addSeparator(Composite result, RowLayouter layouter) {
			Label separator= new Label(result, SWT.SEPARATOR | SWT.HORIZONTAL);
			separator.setLayoutData((new GridData(GridData.FILL_HORIZONTAL)));
			layouter.perform(separator);
		}
	
		private void updatePreviewLabel(){
			try {
				if (fLabel != null)
					fLabel.setText(RefactoringMessages.getString("ExtractConstantInputPage.signature_preview") + getExtractConstantRefactoring().getConstantSignaturePreview()); //$NON-NLS-1$
			} catch(JavaModelException e) {
				ExceptionHandler.handle(e, RefactoringMessages.getString("ExtractTempInputPage.extract_local"), RefactoringMessages.getString("ExtractConstantInputPage.exception")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	
		/*
		 * @see org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage#validateTextField(String)
		 */
		protected RefactoringStatus validateTextField(String text) {
			try {
				getExtractConstantRefactoring().setConstantName(text);
				updatePreviewLabel();
				RefactoringStatus result= getExtractConstantRefactoring().checkConstantNameOnChange();
				if (fOriginalMessageType == IMessageProvider.INFORMATION && result.getSeverity() == RefactoringStatus.OK)
					return RefactoringStatus.createInfoStatus(fOriginalMessage);
				else 
					return result;
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
				return RefactoringStatus.createFatalErrorStatus(RefactoringMessages.getString("ExtractConstantInputPage.Internal_Error")); //$NON-NLS-1$
			}
		}

		private void setAccessModifier(String accessModifier) {
			getExtractConstantRefactoring().setAccessModifier(accessModifier);
			updatePreviewLabel();
		}
	
		private ExtractConstantRefactoring getExtractConstantRefactoring(){
			return (ExtractConstantRefactoring)getRefactoring();
		}
	
		private static Button createCheckbox(Composite parent, String title, boolean value, RowLayouter layouter){
			Button checkBox= new Button(parent, SWT.CHECK);
			checkBox.setText(title);
			checkBox.setSelection(value);
			layouter.perform(checkBox);
			return checkBox;		
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage#isInitialInputValid()
		 */
		protected boolean isInitialInputValid() {
			return fInitialValid;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage#restoreMessage()
		 */
		protected void restoreMessage() {
			setMessage(fOriginalMessage, fOriginalMessageType);
		}
	}	
}

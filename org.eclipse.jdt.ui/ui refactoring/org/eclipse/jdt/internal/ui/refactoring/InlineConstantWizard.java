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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;

import org.eclipse.jdt.internal.corext.refactoring.code.InlineConstantRefactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

public class InlineConstantWizard extends RefactoringWizard {

	private static final String MESSAGE = RefactoringMessages.getString("InlineConstantWizard.message"); //$NON-NLS-1$

	public InlineConstantWizard(InlineConstantRefactoring ref) {
		super(ref, RefactoringMessages.getString("InlineConstantWizard.Inline_Constant")); //$NON-NLS-1$
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
	
	private static class InlineConstantInputPage extends UserInputWizardPage {

		public static final String PAGE_NAME= "InlineConstantInputPage";//$NON-NLS-1$

		private InlineConstantRefactoring fRefactoring;
		private Group fInlineMode;
		private Button fRemove;

		private final int fOriginalMessageType;
		private final String fOriginalMessage;
	
		public InlineConstantInputPage(String description, int messageType) {
			super(PAGE_NAME, true);
			fOriginalMessage= description;
			fOriginalMessageType= messageType;
			setDescription(description);
		}

		public void createControl(Composite parent) {
			initializeDialogUnits(parent);
			fRefactoring= (InlineConstantRefactoring)getRefactoring();
		
			Composite result= new Composite(parent, SWT.NONE);
			setControl(result);
			GridLayout layout= new GridLayout();
			result.setLayout(layout);
			GridData gd= null;

			fInlineMode= new Group(result, SWT.NONE);
			fInlineMode.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fInlineMode.setLayout(new GridLayout());
			fInlineMode.setText(RefactoringMessages.getString("InlineConstantInputPage.Inline")); //$NON-NLS-1$
		
			final Button all= new Button(fInlineMode, SWT.RADIO);
			all.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			all.setText(RefactoringMessages.getString("InlineConstantInputPage.All_references")); //$NON-NLS-1$
			all.setSelection(fRefactoring.getReplaceAllReferences());
			all.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					if (! all.getSelection())
						return;
					fRemove.setEnabled(true);
					fRefactoring.setReplaceAllReferences(true);
				}
			});

			fRemove= new Button(fInlineMode, SWT.CHECK);
			gd= new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalIndent= convertWidthInCharsToPixels(3);
			fRemove.setLayoutData(gd);
			fRemove.setText(RefactoringMessages.getString("InlineConstantInputPage.Delete_constant")); //$NON-NLS-1$
			fRemove.setEnabled(all.getSelection());
			fRemove.setSelection(fRefactoring.getRemoveDeclaration());
			fRemove.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					fRefactoring.setRemoveDeclaration(fRemove.getSelection());
				}
			});

		
			final Button onlySelected= new Button(fInlineMode, SWT.RADIO);
			onlySelected.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			onlySelected.setText(RefactoringMessages.getString("InlineConstantInputPage.Only_selected")); //$NON-NLS-1$
			onlySelected.setSelection(!fRefactoring.getReplaceAllReferences());
			onlySelected.setEnabled(!fRefactoring.isDeclarationSelected());
			onlySelected.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					if (! onlySelected.getSelection())
						return;
					fRemove.setSelection(false);
					fRemove.setEnabled(false);
					fRefactoring.setRemoveDeclaration(false);
					fRefactoring.setReplaceAllReferences(false);
				}
			});		
			Dialog.applyDialogFont(result);
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage#restoreMessage()
		 */
		protected void restoreMessage() {
			setMessage(fOriginalMessage, fOriginalMessageType);
		}
	}
}

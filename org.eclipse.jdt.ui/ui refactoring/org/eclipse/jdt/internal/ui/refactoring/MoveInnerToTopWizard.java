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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.Dialog;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInnerToTopRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class MoveInnerToTopWizard extends RefactoringWizard {

	public MoveInnerToTopWizard(Refactoring ref) {
		super(ref, RefactoringMessages.getString("MoveInnerToTopWizard.Move_Inner")); //$NON-NLS-1$
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		if (getMoveRefactoring().isCreatingInstanceFieldPossible())
			addPage(new MoveInnerToToplnputPage(getInitialNameForEnclosingInstance()));
		else
			setChangeCreationCancelable(false);
	}

	private String getInitialNameForEnclosingInstance() {
		return getMoveRefactoring().getEnclosingInstanceName();
	}

	private MoveInnerToTopRefactoring getMoveRefactoring() {
		return (MoveInnerToTopRefactoring)getRefactoring();
	}
	
	private static class MoveInnerToToplnputPage extends TextInputWizardPage {

		private final boolean fIsInitialInputValid;
		private static final String DESCRIPTION = RefactoringMessages.getString("MoveInnerToToplnputPage.description"); //$NON-NLS-1$
		private Button fCreateFieldCheckBox;
		private Button fFinalCheckBox;
		private Label fFieldNameLabel;
		private Text fFieldNameEntryText;
	
		public MoveInnerToToplnputPage(String initialValue) {
			super(DESCRIPTION, true, initialValue);
			fIsInitialInputValid= ! ("".equals(initialValue)); //$NON-NLS-1$
		}

		public void createControl(Composite parent) {
			initializeDialogUnits(parent);
			Composite newControl= new Composite(parent, SWT.NONE);
			setControl(newControl);
			WorkbenchHelp.setHelp(newControl, IJavaHelpContextIds.MOVE_INNER_TO_TOP_WIZARD_PAGE);
			newControl.setLayout(new GridLayout());
			Dialog.applyDialogFont(newControl);

			GridLayout layout= new GridLayout();
			layout.numColumns= 2;
			layout.verticalSpacing= 8;
			newControl.setLayout(layout);

			int indentSize= convertWidthInCharsToPixels(3);

			addCreateFieldCheckBox(newControl);
			addFinalCheckBox(newControl, indentSize);
			addFieldNameEntry(newControl, indentSize);

			fCreateFieldCheckBox.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					updateControlEnablement(fCreateFieldCheckBox, fFinalCheckBox, fFieldNameLabel, fFieldNameEntryText);
					getMoveRefactoring().setCreateInstanceField(fCreateFieldCheckBox.getSelection());
				}
			});
			updateControlEnablement(fCreateFieldCheckBox, fFinalCheckBox, fFieldNameLabel, fFieldNameEntryText);
		}

		private void addFieldNameEntry(Composite newControl, int indentSize) {
			fFieldNameLabel= new Label(newControl, SWT.NONE);
			fFieldNameLabel.setText(RefactoringMessages.getString("MoveInnerToToplnputPage.enter_name")); //$NON-NLS-1$
			GridData gd1= new GridData();
			gd1.horizontalIndent= indentSize;
			fFieldNameLabel.setLayoutData(gd1);
			
			fFieldNameEntryText= createTextInputField(newControl);
			fFieldNameEntryText.selectAll();
			fFieldNameEntryText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}

		private void addFinalCheckBox(Composite newControl, int indentSize) {
			fFinalCheckBox= new Button(newControl, SWT.CHECK);
			fFinalCheckBox.setText(RefactoringMessages.getString("MoveInnerToToplnputPage.instance_final")); //$NON-NLS-1$
			fFinalCheckBox.setSelection(getMoveRefactoring().isInstanceFieldMarkedFinal());
			GridData gd= new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan= 2;
			gd.horizontalIndent= indentSize;
			fFinalCheckBox.setLayoutData(gd);		
			fFinalCheckBox.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					getMoveRefactoring().setMarkInstanceFieldAsFinal(fFinalCheckBox.getSelection());
				}
			});
		}

		private void addCreateFieldCheckBox(Composite newControl) {
			fCreateFieldCheckBox= new Button(newControl, SWT.CHECK);
			fCreateFieldCheckBox.setText(RefactoringMessages.getString("MoveInnerToToplnputPage.create_field")); //$NON-NLS-1$
			Assert.isTrue(getMoveRefactoring().isCreatingInstanceFieldPossible());//checked before page got created
			fCreateFieldCheckBox.setEnabled(! getMoveRefactoring().isCreatingInstanceFieldMandatory());
			fCreateFieldCheckBox.setSelection(getMoveRefactoring().getCreateInstanceField());
			GridData gd0= new GridData(GridData.FILL_HORIZONTAL);
			gd0.horizontalSpan= 2;
			fCreateFieldCheckBox.setLayoutData(gd0);
		}

		private void updateControlEnablement(final Button createFieldCheckBox, final Button finalCheckBox, final Label label, final Text text) {
			boolean selected= createFieldCheckBox.getSelection();
			finalCheckBox.setEnabled(selected);
			label.setEnabled(selected);
			text.setEnabled(selected);
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage#validateTextField(String)
		 */
		protected RefactoringStatus validateTextField(String text) {
			getMoveRefactoring().setEnclosingInstanceName(text);
			return getMoveRefactoring().checkEnclosingInstanceName(text);
		}	

		/*
		 * @see org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage#isInitialInputValid()
		 */
		protected boolean isInitialInputValid() {
			return fIsInitialInputValid;
		}

		private MoveInnerToTopRefactoring getMoveRefactoring() {
			return (MoveInnerToTopRefactoring)getRefactoring();
		}
	}
}

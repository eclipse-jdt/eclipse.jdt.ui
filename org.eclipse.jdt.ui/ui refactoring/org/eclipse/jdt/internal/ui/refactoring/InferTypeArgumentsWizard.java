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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.corext.refactoring.generics.InferTypeArgumentsRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

public class InferTypeArgumentsWizard extends RefactoringWizard {

	public InferTypeArgumentsWizard(Refactoring refactoring) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		setDefaultPageTitle(RefactoringMessages.getString("InferTypeArgumentsWizard.defaultPageTitle")); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.ltk.ui.refactoring.RefactoringWizard#addUserInputPages()
	 */
	protected void addUserInputPages() {
		addPage(new InferTypeArgumentsInputPage());
	}

	private static class InferTypeArgumentsInputPage extends UserInputWizardPage {

		public static final String PAGE_NAME= "InferTypeArgumentsInputPage"; //$NON-NLS-1$

		private static final String DESCRIPTION= RefactoringMessages.getString("InferTypeArgumentsInputPage.description"); //$NON-NLS-1$
		
		private static final String DIALOG_SETTING_SECTION= "InferTypeArguments"; //$NON-NLS-1$
		private static final String ASSUME_CLONE_RETURNS_SAME_TYPE= "assumeCloneReturnsSameType"; //$NON-NLS-1$
		private static final String LEAVE_UNCONSTRAINED_RAW= "leaveUnconstrainedRaw"; //$NON-NLS-1$
		
		IDialogSettings fSettings;

		private InferTypeArgumentsRefactoring fRefactoring;

		
		public InferTypeArgumentsInputPage() {
			super(PAGE_NAME);
			setDescription(DESCRIPTION);
		}

		public void createControl(Composite parent) {
			fRefactoring= (InferTypeArgumentsRefactoring) getRefactoring();
			loadSettings();
			
			Composite result= new Composite(parent, SWT.NONE);
			setControl(result);
			GridLayout layout= new GridLayout();
			layout.numColumns= 1;
			layout.verticalSpacing= 8;
			result.setLayout(layout);
			
			Label doit= new Label(result, SWT.WRAP);
			doit.setText(RefactoringMessages.getString("InferTypeArgumentsWizard.lengthyDescription")); //$NON-NLS-1$
			doit.setLayoutData(new GridData());
			
			Label separator= new Label(result, SWT.SEPARATOR | SWT.HORIZONTAL);
			separator.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
			
			Button cloneCheckBox= new Button(result, SWT.CHECK);
			cloneCheckBox.setText("Assume implementations of clone() return an object of the same type");
			boolean assumeCloneValue= fSettings.getBoolean(ASSUME_CLONE_RETURNS_SAME_TYPE);
			fRefactoring.setAssumeCloneReturnsSameType(assumeCloneValue);
			cloneCheckBox.setSelection(assumeCloneValue);
			cloneCheckBox.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					setAssumeCloseReturnsSameType(((Button)e.widget).getSelection());
				}
			});
			
			Button leaveRawCheckBox= new Button(result, SWT.CHECK);
			leaveRawCheckBox.setText("Leave unconstrained type arguments raw (rather than infering <?>)");
			boolean leaveRawValue= fSettings.getBoolean(LEAVE_UNCONSTRAINED_RAW);
			fRefactoring.setLeaveUnconstrainedRaw(leaveRawValue);
			leaveRawCheckBox.setSelection(leaveRawValue);
			leaveRawCheckBox.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					setLeaveUnconstrainedRaw(((Button)e.widget).getSelection());
				}
			});
			
			updateStatus();
			Dialog.applyDialogFont(result);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.INTRODUCE_PARAMETER_WIZARD_PAGE);
		}

		private void setAssumeCloseReturnsSameType(boolean selection) {
			fSettings.put(ASSUME_CLONE_RETURNS_SAME_TYPE, selection);
			fRefactoring.setAssumeCloneReturnsSameType(selection);
		}
		
		private void setLeaveUnconstrainedRaw(boolean selection) {
			fSettings.put(LEAVE_UNCONSTRAINED_RAW, selection);
			fRefactoring.setLeaveUnconstrainedRaw(selection);
		}
		
		private void updateStatus() {
			setPageComplete(true);
			//TODO: validate?
		}
		
		private void loadSettings() {
			fSettings= getDialogSettings().getSection(DIALOG_SETTING_SECTION);
			if (fSettings == null) {
				fSettings= getDialogSettings().addNewSection(DIALOG_SETTING_SECTION);
				fSettings.put(ASSUME_CLONE_RETURNS_SAME_TYPE, false);
			}
			fRefactoring.setAssumeCloneReturnsSameType(fSettings.getBoolean(ASSUME_CLONE_RETURNS_SAME_TYPE));
		}
		
		
	}
}

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
package org.eclipse.jdt.internal.ui.refactoring.code;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import org.eclipse.jface.dialogs.Dialog;

import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

public class InlineMethodInputPage extends UserInputWizardPage {

	public static final String PAGE_NAME= "InlineMethodInputPage";//$NON-NLS-1$
	private static final String DESCRIPTION = RefactoringMessages.getString("InlineMethodInputPage.description"); //$NON-NLS-1$

	private InlineMethodRefactoring fRefactoring;
	private Group fInlineMode;
	private Button fRemove;
	
	public InlineMethodInputPage() {
		super(PAGE_NAME);
		setImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_CU);
		setDescription(DESCRIPTION);
	}

	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		fRefactoring= (InlineMethodRefactoring)getRefactoring();
		
		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		GridLayout layout= new GridLayout();
		result.setLayout(layout);
		GridData gd= null;

		boolean all= fRefactoring.getInitialMode() == InlineMethodRefactoring.Mode.INLINE_ALL;
		fInlineMode= new Group(result, SWT.NONE);
		fInlineMode.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fInlineMode.setLayout(new GridLayout());
		fInlineMode.setText(RefactoringMessages.getString("InlineMethodInputPage.inline")); //$NON-NLS-1$
		
		Button radio= new Button(fInlineMode, SWT.RADIO);
		radio.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		radio.setText(RefactoringMessages.getString("InlineMethodInputPage.all_invocations")); //$NON-NLS-1$
		radio.setSelection(all);
		radio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				fRemove.setEnabled(true);
				if (((Button)event.widget).getSelection())
					changeRefactoring(InlineMethodRefactoring.Mode.INLINE_ALL);
			}
		});

		fRemove= new Button(fInlineMode, SWT.CHECK);
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent= convertWidthInCharsToPixels(3);
		fRemove.setLayoutData(gd);
		fRemove.setText(RefactoringMessages.getString("InlineMethodInputPage.delete_declaration")); //$NON-NLS-1$
		fRemove.setEnabled(all);
		fRemove.setSelection(fRefactoring.getDeleteSource());
		fRemove.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fRefactoring.setDeleteSource(((Button)e.widget).getSelection());
			}
		});

		
		radio= new Button(fInlineMode, SWT.RADIO);
		radio.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		radio.setText(RefactoringMessages.getString("InlineMethodInputPage.only_selected")); //$NON-NLS-1$
		radio.setSelection(!all);
		if (all) {
			radio.setEnabled(false);
		}
		radio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				fRemove.setEnabled(false);
				if (((Button)event.widget).getSelection())
					changeRefactoring(InlineMethodRefactoring.Mode.INLINE_SINGLE);
			}
		});		
		Dialog.applyDialogFont(result);
	}
	
	private void changeRefactoring(InlineMethodRefactoring.Mode mode) {
		RefactoringStatus status;
		try {
			status= fRefactoring.setCurrentMode(mode);
		} catch (JavaModelException e) {
			status= RefactoringStatus.createFatalErrorStatus(e.getMessage());
		}
		setPageComplete(status);
	}	
}

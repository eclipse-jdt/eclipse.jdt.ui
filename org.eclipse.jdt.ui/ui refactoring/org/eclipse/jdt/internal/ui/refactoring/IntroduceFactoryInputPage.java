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

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.code.IntroduceFactoryRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * @author rfuhrer
 */
public class IntroduceFactoryInputPage extends UserInputWizardPage {
	/**
	 * The name of the factory method to be created.
	 */
	private Text fMethodName;

	/**
	 * Constructor for IntroduceFactoryInputPage.
	 * @param name
	 * @param isLastUserPage
	 */
	public IntroduceFactoryInputPage(String name) {
		super(name, true);
	}

	private Text createTextInputField(Composite result) {
		final Text	textField = new Text(result, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		
		textField.selectAll();
		textField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		return textField;
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite result = new Composite(parent, SWT.NONE);

		setControl(result);

		GridLayout layout= new GridLayout();

		layout.numColumns=      1;
		layout.verticalSpacing= 8;
		result.setLayout(layout);

		Composite	topHalf= new Composite(result, SWT.NONE);
		GridLayout	topLayout= new GridLayout();

		topHalf.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		topLayout.numColumns=      2;
		topLayout.verticalSpacing= 8;
		topHalf.setLayout(topLayout);

		Label		methNameLabel= new Label(topHalf, SWT.NONE);

		fMethodName = createTextInputField(topHalf);

		methNameLabel.setText(RefactoringMessages.getString("IntroduceFactoryInputPage.method_name")); //$NON-NLS-1$
		fMethodName.setText(getUseFactoryRefactoring().getNewMethodName());

		final Button	protectCtorCB= new Button(result, SWT.CHECK);

		protectCtorCB.setText(RefactoringMessages.getString("IntroduceFactoryInputPage.protectConstructorLabel")); //$NON-NLS-1$
		protectCtorCB.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fMethodName.addModifyListener(new ModifyListener()
			{
				public void modifyText(ModifyEvent e)
				{
					RefactoringStatus	status= getUseFactoryRefactoring().setNewMethodName(fMethodName.getText());
					boolean				nameOk= status.isOK();

					IntroduceFactoryInputPage.this.setPageComplete(nameOk);
					IntroduceFactoryInputPage.this.setErrorMessage(nameOk ?
						"" : //$NON-NLS-1$
						status.getFirstMessage(RefactoringStatus.ERROR));
				}
			});
		protectCtorCB.addSelectionListener(new SelectionAdapter()
			{
				public void widgetSelected(SelectionEvent e) {
					boolean	isChecked = protectCtorCB.getSelection();

					getUseFactoryRefactoring().setProtectConstructor(isChecked);
				}
			});

		// Set up the initial state of the various dialog options.
		if (getUseFactoryRefactoring().canProtectConstructor())
			protectCtorCB.setSelection(true);
		else {
			protectCtorCB.setSelection(false);
			protectCtorCB.setEnabled(false);
			getUseFactoryRefactoring().setProtectConstructor(false);
		}

		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.INTRODUCE_FACTORY_WIZARD_PAGE);		
	}

	private IntroduceFactoryRefactoring getUseFactoryRefactoring() {
		return (IntroduceFactoryRefactoring) getRefactoring();
	}
}

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

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

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

import org.eclipse.jface.window.Window;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.corext.refactoring.code.IntroduceFactoryRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.dialogs.TypeSelectionDialog;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

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
		super(name);
	}

	private Text createTextInputField(Composite result) {
		final Text textField = new Text(result, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		textField.selectAll();
		return textField;
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite result = new Composite(parent, SWT.NONE);

		setControl(result);

		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		result.setLayout(layout);

		Label methNameLabel= new Label(result, SWT.NONE);
		methNameLabel.setText(RefactoringMessages.getString("IntroduceFactoryInputPage.method_name")); //$NON-NLS-1$
		
		fMethodName= createTextInputField(result);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		fMethodName.setLayoutData(gd);
		fMethodName.setText(getUseFactoryRefactoring().getNewMethodName());

		final Label	factoryTypeLabel= new Label(result, SWT.NONE);
		factoryTypeLabel.setText(RefactoringMessages.getString("IntroduceFactoryInputPage.factoryClassLabel")); //$NON-NLS-1$
		
		Composite inner= new Composite(result, SWT.NONE);
		GridLayout innerLayout= new GridLayout();
		innerLayout.marginHeight= 0; innerLayout.marginWidth= 0;
		innerLayout.numColumns= 2;
		inner.setLayout(innerLayout);
		inner.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		final Text factoryTypeName= createTextInputField(inner);
		factoryTypeName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		final Button browseTypes= new Button(inner, SWT.PUSH);
		browseTypes.setText(RefactoringMessages.getString("IntroduceFactoryInputPage.browseLabel")); //$NON-NLS-1$
		gd= new GridData();
		gd.horizontalAlignment= GridData.END;
		gd.heightHint = SWTUtil.getButtonHeightHint(browseTypes);
		gd.widthHint = SWTUtil.getButtonWidthHint(browseTypes);		
		browseTypes.setLayoutData(gd);

		final Button protectCtorCB= new Button(result, SWT.CHECK);
		protectCtorCB.setText(RefactoringMessages.getString("IntroduceFactoryInputPage.protectConstructorLabel")); //$NON-NLS-1$
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= 2;
		protectCtorCB.setLayoutData(gd);

		fMethodName.addModifyListener(new ModifyListener()
			{
				public void modifyText(ModifyEvent e) {
					RefactoringStatus	status= getUseFactoryRefactoring().setNewMethodName(fMethodName.getText());
					boolean				nameOk= status.isOK();

					IntroduceFactoryInputPage.this.setPageComplete(nameOk);
					IntroduceFactoryInputPage.this.setErrorMessage(nameOk ?
						"" : //$NON-NLS-1$
						status.getMessageMatchingSeverity(RefactoringStatus.ERROR));
				}
			});
		protectCtorCB.addSelectionListener(new SelectionAdapter()
			{
				public void widgetSelected(SelectionEvent e) {
					boolean	isChecked = protectCtorCB.getSelection();

					getUseFactoryRefactoring().setProtectConstructor(isChecked);
				}
			});

		factoryTypeName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				RefactoringStatus	status;

				status= getUseFactoryRefactoring().setFactoryClass(factoryTypeName.getText());

				boolean	nameOk= status.isOK();

				IntroduceFactoryInputPage.this.setPageComplete(nameOk);
				IntroduceFactoryInputPage.this.setErrorMessage(nameOk ? "" : //$NON-NLS-1$
															   status.getMessageMatchingSeverity(RefactoringStatus.ERROR));
			}
		});
		browseTypes.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IType factoryType= chooseFactoryClass();

				if (factoryType == null)
					return;

				RefactoringStatus status= getUseFactoryRefactoring().setFactoryClass(factoryType.getFullyQualifiedName());
				boolean nameOk= status.isOK();

				factoryTypeName.setText(factoryType.getFullyQualifiedName());
				IntroduceFactoryInputPage.this.setPageComplete(nameOk);
				IntroduceFactoryInputPage.this.setErrorMessage(nameOk ? "" : //$NON-NLS-1$
															   status.getMessageMatchingSeverity(RefactoringStatus.ERROR));
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
		factoryTypeName.setText(getUseFactoryRefactoring().getFactoryClassName());

		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.INTRODUCE_FACTORY_WIZARD_PAGE);		
	}

	private IType chooseFactoryClass() {
		IJavaProject	proj= getUseFactoryRefactoring().getProject();

		if (proj == null)
			return null;

		IJavaElement[] elements= new IJavaElement[] { proj };
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(elements);

		TypeSelectionDialog dialog= new TypeSelectionDialog(getShell(), getWizard().getContainer(), IJavaSearchConstants.CLASS, scope);

		dialog.setTitle(RefactoringMessages.getString("IntroduceFactoryInputPage.chooseFactoryClass.title")); //$NON-NLS-1$
		dialog.setMessage(RefactoringMessages.getString("IntroduceFactoryInputPage.chooseFactoryClass.message")); //$NON-NLS-1$

		if (dialog.open() == Window.OK) {
			return (IType) dialog.getFirstResult();
		}
		return null;
	}

	private IntroduceFactoryRefactoring getUseFactoryRefactoring() {
		return (IntroduceFactoryRefactoring) getRefactoring();
	}
}

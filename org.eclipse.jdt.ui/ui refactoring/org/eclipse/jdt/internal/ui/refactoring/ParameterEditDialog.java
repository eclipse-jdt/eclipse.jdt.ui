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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaConventions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeSignatureRefactoring;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.ControlContentAssistHelper;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.JavaTypeCompletionProcessor;

public class ParameterEditDialog extends StatusDialog {
	
	private ParameterInfo fParameter;
	private boolean fEditType;
	private Text fType;
	private Text fName;
	private Text fDefaultValue;
	private IPackageFragment fContext;
	
	/**
	 * @param context the <code>IPackageFragment</code> for type ContentAssist.
	 * Can be <code>null</code> if <code>canEditType</code> is <code>false</code>.
	 */
	public ParameterEditDialog(Shell parentShell, ParameterInfo parameter, boolean canEditType, IPackageFragment context) {
		super(parentShell);
		fParameter= parameter;
		fEditType= canEditType;
		fContext= context;
	}
	
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(RefactoringMessages.getString("ParameterEditDialog.title")); //$NON-NLS-1$
	}

	protected Control createDialogArea(Composite parent) {
		Composite result= (Composite)super.createDialogArea(parent);
		GridLayout layout= (GridLayout)result.getLayout();
		layout.numColumns= 2;
		Label label;
		GridData gd;
		
		label= new Label(result, SWT.NONE);
		String newName = fParameter.getNewName();
		if (newName.length() == 0)
			label.setText(RefactoringMessages.getString("ParameterEditDialog.message.new")); //$NON-NLS-1$
		else
			label.setText(RefactoringMessages.getFormattedString("ParameterEditDialog.message", newName)); //$NON-NLS-1$
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= 2;
		label.setLayoutData(gd);
		
		if (fEditType) {
			label= new Label(result, SWT.NONE);
			label.setText(RefactoringMessages.getString("ParameterEditDialog.type")); //$NON-NLS-1$
			fType= new Text(result, SWT.BORDER);
			gd= new GridData(GridData.FILL_HORIZONTAL);
			fType.setLayoutData(gd);
			fType.setText(fParameter.getNewTypeName());
			fType.addModifyListener(
				new ModifyListener() {
					public void modifyText(ModifyEvent e) {
						validate((Text)e.widget);
					}
				});
			JavaTypeCompletionProcessor processor= new JavaTypeCompletionProcessor(true, false);
			processor.setPackageFragment(fContext);
			ControlContentAssistHelper.createTextContentAssistant(fType, processor);
		}

		label= new Label(result, SWT.NONE);
		fName= new Text(result, SWT.BORDER);
		initializeDialogUnits(fName);
		label.setText(RefactoringMessages.getString("ParameterEditDialog.name")); //$NON-NLS-1$
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint= convertWidthInCharsToPixels(45);
		fName.setLayoutData(gd);
		fName.setText(newName);
		fName.addModifyListener(
			new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					validate((Text)e.widget);
				}
			});

		if (fParameter.isAdded()) {
			label= new Label(result, SWT.NONE);
			label.setText(RefactoringMessages.getString("ParameterEditDialog.defaultValue")); //$NON-NLS-1$
			fDefaultValue= new Text(result, SWT.BORDER);
			gd= new GridData(GridData.FILL_HORIZONTAL);
			fDefaultValue.setLayoutData(gd);
			fDefaultValue.setText(fParameter.getDefaultValue());
			fDefaultValue.addModifyListener(
				new ModifyListener() {
					public void modifyText(ModifyEvent e) {
						validate((Text)e.widget);
					}
				});
		}
		applyDialogFont(result);		
		return result;
	}
	
	protected void okPressed() {
		if (fType != null) {
			fParameter.setNewTypeName(fType.getText());
		}
		fParameter.setNewName(fName.getText());
		if (fDefaultValue != null) {
			fParameter.setDefaultValue(fDefaultValue.getText());
		}
		super.okPressed();
	}
	
	private void validate(Text first) {
		IStatus[] result= new IStatus[3];
		if (first == fType) {
			result[0]= validateType();
			result[1]= validateName();
			result[2]= validateDefaultValue();
		} else if (first == fName) {
			result[0]= validateName();
			result[1]= validateType();
			result[2]= validateDefaultValue();
		} else {
			result[0]= validateDefaultValue();
			result[1]= validateName();
			result[2]= validateType();
		}
		for (int i= 0; i < result.length; i++) {
			IStatus status= result[i];
			if (status != null && !status.isOK()) {
				updateStatus(status);
				return;
			}
		}
		updateStatus(createOkStatus());
	}
	
	private IStatus validateType() {
		if (fType == null)
			return null;
		String typeName= fType.getText();
		if (typeName.length() == 0)
			return createErrorStatus(RefactoringMessages.getString("ParameterEditDialog.type.error"));//$NON-NLS-1$
		if (ChangeSignatureRefactoring.isValidParameterTypeName(typeName))
			return createOkStatus();
		String msg= RefactoringMessages.getFormattedString("ParameterEditDialog.type.invalid", new String[]{typeName}); //$NON-NLS-1$
		return createErrorStatus(msg); 
	}
	
	private IStatus validateName() {
		if (fName == null) 
			return null;
		String text= fName.getText();
		if (text.length() == 0)
			return createErrorStatus(RefactoringMessages.getString("ParameterEditDialog.name.error"));//$NON-NLS-1$
		IStatus status= JavaConventions.validateFieldName(text);
		if (status.matches(IStatus.ERROR))
			return status;
		if (! Checks.startsWithLowerCase(text))
			return createWarningStatus(RefactoringCoreMessages.getString("ExtractTempRefactoring.convention")); //$NON-NLS-1$
		return createOkStatus();
	}
	
	private IStatus validateDefaultValue() {
		if (fDefaultValue == null)
			return null;
		String defaultValue= fDefaultValue.getText();
		if (defaultValue.length() == 0)
			return createErrorStatus(RefactoringMessages.getString("ParameterEditDialog.defaultValue.error"));//$NON-NLS-1$
		if (ChangeSignatureRefactoring.isValidExpression(defaultValue))
			return createOkStatus();
		String msg= RefactoringMessages.getFormattedString("ParameterEditDialog.defaultValue.invalid", new String[]{defaultValue}); //$NON-NLS-1$
		return createErrorStatus(msg);
		
	}
	
	private Status createOkStatus() {
		return new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
	}
	
	private Status createWarningStatus(String message) {
		return new Status(IStatus.WARNING, JavaPlugin.getPluginId(), IStatus.WARNING, message, null);
	}
	
	private Status createErrorStatus(String message) {
		return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR, message, null);
	}
}

/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.wizards;

import org.eclipse.equinox.bidi.StructuredTextTypeHandlerFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.util.BidiUtils;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

/**
 * Wizard page to create a new module-info file.
 *
 * <p>
 * Note: This class is not intended to be subclassed, but clients can instantiate.
 * </p>
 *
 * @since 3.14
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class NewModuleInfoWizardPage extends NewTypeWizardPage{

	private static final String PAGE_NAME= "NewModuleInfoWizardPage"; //$NON-NLS-1$

	private IJavaProject fProject;

	private static final String MODULE_INFO_JAVA_FILENAME= JavaModelUtil.MODULE_INFO_JAVA;

	private StringDialogField fModuleNameDialogField;

	/*
	 * Status of last validation of the module name field
	 */
	private IStatus fModuleNameStatus;

	public NewModuleInfoWizardPage() {
		super(false, PAGE_NAME);
		String title= Messages.format(NewWizardMessages.NewModuleInfoWizardPage_title, MODULE_INFO_JAVA_FILENAME);
		setTitle(title);
		String description= Messages.format(NewWizardMessages.NewModuleInfoWizardPage_description, MODULE_INFO_JAVA_FILENAME);
		setDescription(description);

		ModuleFieldAdapter adapter= new ModuleFieldAdapter();

		fModuleNameDialogField= new StringDialogField();
		fModuleNameDialogField.setDialogFieldListener(adapter);
		fModuleNameDialogField.setLabelText(NewWizardMessages.NewModuleInfoWizardPage_module_label);

		fModuleNameStatus= new StatusInfo();
	}

	// -------- Initialization ---------

	/**
	 * The wizard owning this page is responsible for calling this method with the
	 * current selection's project. The project is used to initialize the fields of the wizard page.
	 *
	 * @param project used to initialize the fields
	 */
	public void init(IJavaProject project) {

		if (project != null) {
			fProject= project;
		} else {
			return;
		}

		IModuleDescription moduleDescription= null;
		try {
			moduleDescription= fProject.getModuleDescription();
		} catch (JavaModelException e) {
			// Ignore
		}
		String moduleName= moduleDescription != null ? moduleDescription.getElementName() : fProject.getElementName();

		IStatus status= getModuleStatus(moduleName);
		if (status.getSeverity() == IStatus.OK || status.getSeverity() == IStatus.WARNING || status.getSeverity() == IStatus.INFO) {
			setModuleText(moduleName, true);
		} else {
			setModuleText("", true); //$NON-NLS-1$
		}
		setAddComments(StubUtility.doAddComments(project), true); // from project or workspace

	}

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite= new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());
		int nColumns= 2;

		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		composite.setLayout(layout);

		createModuleInfoControls(composite, nColumns);
		createCommentWithLinkControls(composite, nColumns, true);
		enableCommentControl(true);

		setControl(composite);
		Dialog.applyDialogFont(composite);

	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			setFocus();
		}
	}

	/**
	 * Sets the focus to the module name field.
	 */
	@Override
	protected void setFocus() {
		fModuleNameDialogField.setFocus();
	}

	private void createModuleInfoControls(Composite composite, int nColumns) {
		fModuleNameDialogField.doFillIntoGrid(composite, nColumns);
		Text text= fModuleNameDialogField.getTextControl(null);
		LayoutUtil.setWidthHint(text, getMaxFieldWidth());
		LayoutUtil.setHorizontalGrabbing(text);
		DialogField.createEmptySpace(composite);
		TextFieldNavigationHandler.install(text);
		BidiUtils.applyBidiProcessing(fModuleNameDialogField.getTextControl(null), StructuredTextTypeHandlerFactory.JAVA);
	}

	private class ModuleFieldAdapter implements IDialogFieldListener {

		@Override
		public void dialogFieldChanged(DialogField field) {
			fModuleNameStatus= getModuleStatus(getModuleNameText());
			// tell all others
			handleFieldChanged();
		}
	}

	/**
	 * Returns the recommended maximum width for text fields (in pixels). This method requires that
	 * createContent has been called before this method is call.
	 *
	 * @return the recommended maximum width for text fields.
	 */
	@Override
	protected int getMaxFieldWidth() {
		return convertWidthInCharsToPixels(25);
	}

	private void handleFieldChanged() {
		updateStatus(fModuleNameStatus);
	}

	private IStatus validateModuleName(String name) {
		String sourceLevel= fProject.getOption(JavaCore.COMPILER_SOURCE, true);
		String complianceLevel= fProject.getOption(JavaCore.COMPILER_COMPLIANCE, true);
		return JavaConventions.validateModuleName(name, sourceLevel, complianceLevel);
	}

	/**
	 * Validates the module name and returns the status of the validation.
	 *
	 * @param moduleName the module name
	 *
	 * @return the status of the validation
	 */
	private IStatus getModuleStatus(String moduleName) {
		StatusInfo status= new StatusInfo();
		if (moduleName != null && moduleName.length() > 0) {
			IStatus val= validateModuleName(moduleName);
			if (val.getSeverity() == IStatus.ERROR) {
				status.setError(Messages.format(NewWizardMessages.NewModuleInfoWizardPage_error_InvalidModuleName, val.getMessage()));
			} else if (val.getSeverity() == IStatus.WARNING) {
				status.setWarning(Messages.format(NewWizardMessages.NewModuleInfoWizardPage_warning_DiscouragedModuleName, val.getMessage()));
			}
		} else {
			status.setError(NewWizardMessages.NewModuleInfoWizardPage_error_EnterName);
		}
		return status;
	}

	/**
	 * Returns the content of the module name field.
	 *
	 * @return the content of the module name field
	 */
	public String getModuleNameText() {
		return fModuleNameDialogField.getText();
	}

	public IJavaProject getProject() {
		return fProject;
	}

	public IStatus getModuleNameStatus() {
		return fModuleNameStatus;
	}

	/**
	 * Sets the content of the module name field to the given value.
	 *
	 * @param moduleName the new module name field text
	 * @param canBeModified if <code>true</code> the module input field can be modified; otherwise it is
	 *            read-only.
	 */
	private void setModuleText(String moduleName, boolean canBeModified) {
		fModuleNameDialogField.setText(moduleName);
		fModuleNameDialogField.setEnabled(canBeModified);
	}
	@Override
	public IJavaProject getJavaProject() {
	 return fProject;
	}

}

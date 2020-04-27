/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogFieldGroup;

/**
 * Wizard page to  create a new record.
 * <p>
 * Note: This class is not intended to be subclassed, but clients can instantiate.
 * To implement a different kind of a new record wizard page, extend <code>NewTypeWizardPage</code>.
 * </p>
 *
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public class NewRecordWizardPage extends NewTypeWizardPage {

	private final static String PAGE_NAME= "NewRecordWizardPage"; //$NON-NLS-1$
    private final static int TYPE = NewTypeWizardPage.RECORD_TYPE;
    private final static String SETTINGS_CREATEUNIMPLEMENTED= "create_unimplemented"; //$NON-NLS-1$

    private SelectionButtonDialogFieldGroup fMethodStubsButtons;

	/**
	 * Creates a new <code>NewRecordWizardPage</code>
	 */
	public NewRecordWizardPage() {
		super(TYPE, PAGE_NAME);

		setTitle(NewWizardMessages.NewRecordWizardPage_title);
		setDescription(NewWizardMessages.NewRecordWizardPage_description);

		String[] buttonNames= new String[] {
				NewWizardMessages.NewRecordWizardPage_methods_inherited
		};
		fMethodStubsButtons= new SelectionButtonDialogFieldGroup(SWT.CHECK, buttonNames, 1);
		fMethodStubsButtons.setLabelText(NewWizardMessages.NewRecordWizardPage_methods_label);
	}

	// -------- Initialization ---------

	/**
	 * The wizard owning this page is responsible for calling this method with the
	 * current selection. The selection is used to initialize the fields of the wizard
	 * page.
	 *
	 * @param selection used to initialize the fields
	 */
	public void init(IStructuredSelection selection) {
		IJavaElement jelem= getInitialJavaElement(selection);
		initContainerPage(jelem);
		initTypePage(jelem);
		doStatusUpdate();

		boolean createUnimplemented= true;
		IDialogSettings dialogSettings= getDialogSettings();
		if (dialogSettings != null) {
			IDialogSettings section= dialogSettings.getSection(PAGE_NAME);
			if (section != null) {
				createUnimplemented= section.getBoolean(SETTINGS_CREATEUNIMPLEMENTED);
			}
		}

		setMethodStubSelection(createUnimplemented, true);
	}

	// ------ validation --------

	private void doStatusUpdate() {
		// all used component status
		IStatus[] status= new IStatus[] {
			fContainerStatus,
			isEnclosingTypeSelected() ? fEnclosingTypeStatus : fPackageStatus,
			fTypeNameStatus,
			fModifierStatus,
			fSuperInterfacesStatus
		};

		// the mode severe status will be displayed and the OK button enabled/disabled.
		updateStatus(status);
	}


	/*
	 * @see NewContainerWizardPage#handleFieldChanged
	 */
	@Override
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);

		doStatusUpdate();
	}


	// ------ UI --------

	/*
	 * @see WizardPage#createControl
	 */
	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite= new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());

		int nColumns= 4;

		GridLayout layout= new GridLayout();
		layout.numColumns= nColumns;
		composite.setLayout(layout);

		createContainerControls(composite, nColumns);
		createPackageControls(composite, nColumns);
		createEnclosingTypeControls(composite, nColumns);

		createSeparator(composite, nColumns);

		createTypeNameControls(composite, nColumns);
		createModifierControls(composite, nColumns);

		createSuperInterfacesControls(composite, nColumns);

		createMethodStubSelectionControls(composite, nColumns);

		createCommentControls(composite, nColumns);
		enableCommentControl(true);

		setControl(composite);

		Dialog.applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IJavaHelpContextIds.NEW_RECORD_WIZARD_PAGE);
	}

	private void createMethodStubSelectionControls(Composite composite, int nColumns) {
		Control labelControl= fMethodStubsButtons.getLabelControl(composite);
		LayoutUtil.setHorizontalSpan(labelControl, nColumns);

		DialogField.createEmptySpace(composite);

		Control buttonGroup= fMethodStubsButtons.getSelectionButtonsGroup(composite);
		LayoutUtil.setHorizontalSpan(buttonGroup, nColumns - 1);
	}

	/*
	 * @see WizardPage#becomesVisible
	 */
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			setFocus();
		}
	}

	/**
	 * Sets the selection state of the method stub checkboxes.
	 *
	 * @param createInherited initial selection state of the 'Create inherited abstract methods' checkbox.
	 * @param canBeModified if <code>true</code> the method stub checkboxes can be changed by
	 * the user. If <code>false</code> the buttons are "read-only"
	 */
	public void setMethodStubSelection(boolean createInherited, boolean canBeModified) {
		fMethodStubsButtons.setSelection(0, createInherited);

		fMethodStubsButtons.setEnabled(canBeModified);
	}

	/**
	 * Returns the current selection state of the 'Create inherited abstract methods'
	 * checkbox.
	 *
	 * @return the selection state of the 'Create inherited abstract methods' checkbox
	 */
	public boolean isCreateInherited() {
		return fMethodStubsButtons.isSelected(0);
	}

	// ---- creation ----------------

		/*
		 * @see NewTypeWizardPage#createTypeMembers
		 */
		@Override
		protected void createTypeMembers(IType type, ImportsManager imports, IProgressMonitor monitor) throws CoreException {
			boolean doInherited= isCreateInherited();
			createInheritedMethods(type, false, doInherited, imports, SubMonitor.convert(monitor, 1));

			IDialogSettings dialogSettings= getDialogSettings();
			if (dialogSettings != null) {
				IDialogSettings section= dialogSettings.getSection(PAGE_NAME);
				if (section == null) {
					section= dialogSettings.addNewSection(PAGE_NAME);
				}
				section.put(SETTINGS_CREATEUNIMPLEMENTED, doInherited);
			}

			if (monitor != null) {
				monitor.done();
			}
		}
}

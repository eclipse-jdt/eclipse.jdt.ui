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
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.dialogs.Dialog;

import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;

/**
 * Property page used to configure project specific task tags settings
 */
public class TodoTaskPropertyPage extends PropertyPage {

	private TodoTaskConfigurationBlock fConfigurationBlock;
	private Control fConfigurationBlockControl;
	private SelectionButtonDialogField fUseWorkspaceSettings;
	private SelectionButtonDialogField fChangeWorkspaceSettings;
	private SelectionButtonDialogField fUseProjectSettings;
	private IStatus fBlockStatus;
	

	public TodoTaskPropertyPage() {
		setDescription(PreferencesMessages.getString("TodoTaskPropertyPage.description")); //$NON-NLS-1$
		
		fBlockStatus= new StatusInfo();
		IDialogFieldListener listener= new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				doDialogFieldChanged(field);
			}
		};
		
		fUseWorkspaceSettings= new SelectionButtonDialogField(SWT.RADIO);
		fUseWorkspaceSettings.setDialogFieldListener(listener);
		fUseWorkspaceSettings.setLabelText(PreferencesMessages.getString("TodoTaskPropertyPage.useworkspacesettings.label")); //$NON-NLS-1$

		fChangeWorkspaceSettings= new SelectionButtonDialogField(SWT.PUSH);
		fChangeWorkspaceSettings.setLabelText(PreferencesMessages.getString("TodoTaskPropertyPage.useworkspacesettings.change")); //$NON-NLS-1$
		fChangeWorkspaceSettings.setDialogFieldListener(listener);
	
		fUseWorkspaceSettings.attachDialogField(fChangeWorkspaceSettings);

		fUseProjectSettings= new SelectionButtonDialogField(SWT.RADIO);
		fUseProjectSettings.setDialogFieldListener(listener);
		fUseProjectSettings.setLabelText(PreferencesMessages.getString("TodoTaskPropertyPage.useprojectsettings.label")); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.TODOTASK_PROPERTY_PAGE);
	}

	/*
	 * @see org.eclipse.jface.preference.IPreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		IStatusChangeListener listener= new IStatusChangeListener() {
			public void statusChanged(IStatus status) {
				fBlockStatus= status;
				doStatusChanged();
			}
		};		
		fConfigurationBlock= new TodoTaskConfigurationBlock(listener, getProject());
		
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 1;
		composite.setLayout(layout);
		
		fUseWorkspaceSettings.doFillIntoGrid(composite, 1);
		LayoutUtil.setHorizontalGrabbing(fUseWorkspaceSettings.getSelectionButton(null));
		
		fChangeWorkspaceSettings.doFillIntoGrid(composite, 1);
		GridData data= (GridData) fChangeWorkspaceSettings.getSelectionButton(null).getLayoutData();
		data.horizontalIndent= convertWidthInCharsToPixels(3);
		data.horizontalAlignment= GridData.BEGINNING;
		
		fUseProjectSettings.doFillIntoGrid(composite, 1);
		
		data= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL );
		data.horizontalSpan= 1;
		data.horizontalIndent= convertWidthInCharsToPixels(2);
		
		fConfigurationBlockControl= fConfigurationBlock.createContents(composite);
		fConfigurationBlockControl.setLayoutData(data);
		
		boolean useProjectSettings= fConfigurationBlock.hasProjectSpecificOptions();
		
		fUseProjectSettings.setSelection(useProjectSettings);
		fUseWorkspaceSettings.setSelection(!useProjectSettings);
		
		updateEnableState();
		Dialog.applyDialogFont(composite);
		return composite;
	}
	
	private boolean useProjectSettings() {
		return fUseProjectSettings.isSelected();
	}
	
	private void doDialogFieldChanged(DialogField field) {
		if (field == fChangeWorkspaceSettings) {
			TodoTaskPreferencePage page= new TodoTaskPreferencePage();
			PreferencePageSupport.showPreferencePage(getShell(), TodoTaskPreferencePage.ID, page);
		} else {
			updateEnableState();
			doStatusChanged();
		}
	}	

	private void doStatusChanged() {
		updateStatus(useProjectSettings() ? fBlockStatus : new StatusInfo());
	}
	
	private IJavaProject getProject() {
		return (IJavaProject) getElement().getAdapter(IJavaElement.class);		
	}
	
	private void updateEnableState() {
		fConfigurationBlock.setEnabled(useProjectSettings());
	}
	
	/*
	 * @see org.eclipse.jface.preference.IPreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		if (useProjectSettings()) {
			fConfigurationBlock.performDefaults();
			fUseProjectSettings.setSelection(false);
			fUseWorkspaceSettings.setSelection(true);
		}
		super.performDefaults();
	}

	/*
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	public boolean performOk() {
		return fConfigurationBlock.performOk(useProjectSettings());
	}
	
	private void updateStatus(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}
	
}

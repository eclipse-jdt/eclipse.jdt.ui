/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.dialogs.ControlEnableState;

import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;

/**
 * Property page used to set the project's Javadoc location for sources
 */
public class CompilerPropertyPage extends PropertyPage {

	private CompilerConfigurationBlock fConfigurationBlock;
	private Control fConfigurationBlockControl;
	private ControlEnableState fBlockEnableState;
	private Button fUseWorkspaceSettings;
	private Button fUseProjectSettings;
	private IStatus fBlockStatus;
	

	public CompilerPropertyPage() {
		fBlockStatus= new StatusInfo();
		fBlockEnableState= null;
	}

	/*
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.COMPILER_PREFERENCE_PAGE);
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
		fConfigurationBlock= new CompilerConfigurationBlock(listener, getProject());		
		
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;		
		composite.setLayout(layout);
				
		fUseWorkspaceSettings= new Button(composite, SWT.RADIO);
		fUseWorkspaceSettings.setText(JavaUIMessages.getString("CompilerPropertyPage.useworkspacesettings.label"));
		fUseWorkspaceSettings.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fUseProjectSettings= new Button(composite, SWT.RADIO);
		fUseProjectSettings.setText(JavaUIMessages.getString("CompilerPropertyPage.useprojectsettings.label"));
		fUseProjectSettings.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));	
		fUseProjectSettings.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				doRadioButtonChanged();
			}
			public void widgetDefaultSelected(SelectionEvent event) {
				doRadioButtonChanged();
			}
		});
		
		fConfigurationBlockControl= fConfigurationBlock.createContents(composite);
		fConfigurationBlockControl.setLayoutData(new GridData(GridData.FILL));
		
		boolean useProjectSettings= fConfigurationBlock.hasProjectSpecificOptions();
		
		fUseProjectSettings.setSelection(useProjectSettings);
		fUseWorkspaceSettings.setSelection(!useProjectSettings);
		
		updateEnableState();
		return composite;
	}
	
	private boolean useProjectSettings() {
		return fUseProjectSettings.getSelection();
	}
	
	private void doRadioButtonChanged() {
		updateEnableState();
		doStatusChanged();
	}	
	/**
	 * Method statusChanged.
	 */
	private void doStatusChanged() {
		updateStatus(useProjectSettings() ? fBlockStatus : new StatusInfo());
	}
	
	/**
	 * Method getProject.
	 */
	private IJavaProject getProject() {
		return (IJavaProject) getElement().getAdapter(IJavaElement.class);		
	}
	
	private void updateEnableState() {
		if (useProjectSettings()) {
			if (fBlockEnableState != null) {
				fBlockEnableState.restore();
				fBlockEnableState= null;
			}
		} else {
			if (fBlockEnableState == null) {
				fBlockEnableState= ControlEnableState.disable(fConfigurationBlockControl);
			}
		}	
	}
	
	/*
	 * @see org.eclipse.jface.preference.IPreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		if (useProjectSettings()) {
			fConfigurationBlock.performDefaults();
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
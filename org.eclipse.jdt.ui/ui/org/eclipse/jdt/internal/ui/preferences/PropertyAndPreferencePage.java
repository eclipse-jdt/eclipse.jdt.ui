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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;

/**
 * Base for project property and preference pages
 */
public abstract class PropertyAndPreferencePage extends PreferencePage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {
	
	private Control fConfigurationBlockControl;
	private ControlEnableState fBlockEnableState;
	private Hyperlink fChangeWorkspaceSettings;
	private SelectionButtonDialogField fUseProjectSettings;
	private IStatus fBlockStatus;
	
	private IProject fProject; // project or null
	
	public PropertyAndPreferencePage() {
		fBlockStatus= new StatusInfo();
		fBlockEnableState= null;
		fProject= null;
	}

	protected abstract Control createPreferenceContent(Composite composite);
	
	protected abstract boolean hasProjectSpecificOptions();
	
	/*
	 * @see org.eclipse.jface.preference.IPreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		//layout.marginHeight= 0;
		//layout.marginWidth= 0;
		layout.numColumns= 2;
		composite.setLayout(layout);
		
		if (isProjectPreferencePage()) {
			IDialogFieldListener listener= new IDialogFieldListener() {
				public void dialogFieldChanged(DialogField field) {
					doProjectWorkspaceStateChanged();
				}
			};
			
			fUseProjectSettings= new SelectionButtonDialogField(SWT.CHECK);
			fUseProjectSettings.setDialogFieldListener(listener);
			fUseProjectSettings.setLabelText(PreferencesMessages.getString("PropertyAndPreferencePage.useprojectsettings.label")); //$NON-NLS-1$
			fUseProjectSettings.doFillIntoGrid(composite, 1);
			LayoutUtil.setHorizontalGrabbing(fUseProjectSettings.getSelectionButton(null));
			
			fChangeWorkspaceSettings= new Hyperlink(composite, SWT.NONE);
			fChangeWorkspaceSettings.setText(PreferencesMessages.getString("PropertyAndPreferencePage.useworkspacesettings.change")); //$NON-NLS-1$
			fChangeWorkspaceSettings.setLayoutData(new GridData());
			fChangeWorkspaceSettings.setUnderlined(true);
			fChangeWorkspaceSettings.setForeground(JFaceResources.getColorRegistry().get(JFacePreferences.HYPERLINK_COLOR));
			
			fChangeWorkspaceSettings.addHyperlinkListener(new HyperlinkAdapter() {
				public void linkActivated(HyperlinkEvent e) {
					openWorkspacePreferences();
				}
				
				public void linkEntered(HyperlinkEvent e) {
					fChangeWorkspaceSettings.setForeground(JFaceResources.getColorRegistry().get(JFacePreferences.ACTIVE_HYPERLINK_COLOR));
				}
				
				public void linkExited(HyperlinkEvent e) {
					fChangeWorkspaceSettings.setForeground(JFaceResources.getColorRegistry().get(JFacePreferences.HYPERLINK_COLOR));
				}
				
			});
			Label horizontalLine= new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
			horizontalLine.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
		}

		
		GridData data= new GridData(GridData.FILL, GridData.FILL, true, true);
		data.horizontalSpan= 2;
		data.heightHint= 0;
		
		fConfigurationBlockControl= createPreferenceContent(composite);
		fConfigurationBlockControl.setLayoutData(data);

		if (isProjectPreferencePage()) {
			boolean useProjectSettings= hasProjectSpecificOptions();
			
			fUseProjectSettings.setSelection(useProjectSettings);
			//fUseWorkspaceSettings.setSelection(!useProjectSettings);
			
			doProjectWorkspaceStateChanged();
		}

		Dialog.applyDialogFont(composite);
		return composite;
	}
	
	protected boolean useProjectSettings() {
		return isProjectPreferencePage() && fUseProjectSettings != null && fUseProjectSettings.isSelected();
	}
	
	protected boolean isProjectPreferencePage() {
		return fProject != null;
	}
	
	protected IProject getProject() {
		return fProject;
	}
	
	protected abstract void openWorkspacePreferences();
	
	private void doProjectWorkspaceStateChanged() {
		enablePreferenceContent(useProjectSettings());
		fChangeWorkspaceSettings.setVisible(!useProjectSettings());
		doStatusChanged();
	}

	protected void setPreferenceContentStatus(IStatus status) {
		fBlockStatus= status;
		doStatusChanged();
	}
	
	/**
	 * Returns a new status change listener that calls {@link #setPreferenceContentStatus(IStatus)}
	 * when the status has changed
	 * @return The new listener
	 */
	protected IStatusChangeListener getNewStatusChangedListener() {
		return new IStatusChangeListener() {
			public void statusChanged(IStatus status) {
				setPreferenceContentStatus(status);
			}
		};		
	}
	
	protected IStatus getPreferenceContentStatus() {
		return fBlockStatus;
	}

	protected void doStatusChanged() {
		if (!isProjectPreferencePage() || useProjectSettings()) {
			updateStatus(fBlockStatus);
		} else {
			updateStatus(new StatusInfo());
		}
	}
		
	protected void enablePreferenceContent(boolean enable) {
		if (enable) {
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
			fUseProjectSettings.setSelection(false);
			//fUseWorkspaceSettings.setSelection(true);
		}
		super.performDefaults();
	}

	private void updateStatus(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPropertyPage#getElement()
	 */
	public IAdaptable getElement() {
		return fProject;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPropertyPage#setElement(org.eclipse.core.runtime.IAdaptable)
	 */
	public void setElement(IAdaptable element) {
		fProject= (IProject) element.getAdapter(IResource.class);
	}
	
}

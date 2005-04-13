/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import java.util.HashSet;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

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
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PreferencesUtil;

import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

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
	private Composite fParentComposite;
	
	private IProject fProject; // project or null
	private Object fData; // page data
	
	private static final String NO_LINK_DATA= "nolink"; //$NON-NLS-1$
	
	public PropertyAndPreferencePage() {
		fBlockStatus= new StatusInfo();
		fBlockEnableState= null;
		fProject= null;
		fData= null;
	}

	protected abstract Control createPreferenceContent(Composite composite);
	protected abstract boolean hasProjectSpecificOptions(IProject project);
	
	protected abstract String getPreferencePageID();
	protected abstract String getPropertyPageID();
	
	protected boolean supportsProjectSpecificOptions() {
		return getPropertyPageID() != null;
	}
	
	protected boolean offerLink() {
		if (fData instanceof String) {
			String str= (String) fData;
			return str.indexOf(NO_LINK_DATA) == -1;
		}
		return true;
	}
	
    protected Label createDescriptionLabel(Composite parent) {
		fParentComposite= parent;
		if (isProjectPreferencePage()) {
			Composite composite= new Composite(parent, SWT.NONE);
			GridLayout layout= new GridLayout();
			layout.marginHeight= 0;
			layout.marginWidth= 0;
			layout.numColumns= 2;
			composite.setLayout(layout);
			composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			
			IDialogFieldListener listener= new IDialogFieldListener() {
				public void dialogFieldChanged(DialogField field) {
					doProjectWorkspaceStateChanged();
				}
			};
			
			fUseProjectSettings= new SelectionButtonDialogField(SWT.CHECK);
			fUseProjectSettings.setDialogFieldListener(listener);
			fUseProjectSettings.setLabelText(PreferencesMessages.PropertyAndPreferencePage_useprojectsettings_label); 
			fUseProjectSettings.doFillIntoGrid(composite, 1);
			LayoutUtil.setHorizontalGrabbing(fUseProjectSettings.getSelectionButton(null));
			
			if (offerLink()) {
				fChangeWorkspaceSettings= createLink(composite);
				fChangeWorkspaceSettings.setText(PreferencesMessages.PropertyAndPreferencePage_useworkspacesettings_change); 
				fChangeWorkspaceSettings.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
			} else {
				LayoutUtil.setHorizontalSpan(fUseProjectSettings.getSelectionButton(null), 2);
			}
			
			Label horizontalLine= new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
			horizontalLine.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));

		} else if (supportsProjectSpecificOptions() && offerLink()) {
			fChangeWorkspaceSettings= createLink(parent);
			fChangeWorkspaceSettings.setText(PreferencesMessages.PropertyAndPreferencePage_showprojectspecificsettings_label); 
			fChangeWorkspaceSettings.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
		}
		return super.createDescriptionLabel(parent);
    }
	
	/*
	 * @see org.eclipse.jface.preference.IPreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		composite.setLayout(layout);
			
		GridData data= new GridData(GridData.FILL, GridData.FILL, true, true);
		data.heightHint= 0;
		
		fConfigurationBlockControl= createPreferenceContent(composite);
		fConfigurationBlockControl.setLayoutData(data);

		if (isProjectPreferencePage()) {
			boolean useProjectSettings= hasProjectSpecificOptions(getProject());
			
			fUseProjectSettings.setSelection(useProjectSettings);
			
			doProjectWorkspaceStateChanged();
		}

		Dialog.applyDialogFont(composite);
		return composite;
	}

	private Hyperlink createLink(Composite composite) {
		Hyperlink link= new Hyperlink(composite, SWT.NONE);
		link.setUnderlined(true);
		link.setForeground(JFaceResources.getColorRegistry().get(JFacePreferences.HYPERLINK_COLOR));
		link.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				doLinkActivated((Hyperlink) e.widget);
			}
			
			public void linkEntered(HyperlinkEvent e) {
				((Hyperlink) e.widget).setForeground(JFaceResources.getColorRegistry().get(JFacePreferences.ACTIVE_HYPERLINK_COLOR));
			}
			
			public void linkExited(HyperlinkEvent e) {
				((Hyperlink) e.widget).setForeground(JFaceResources.getColorRegistry().get(JFacePreferences.HYPERLINK_COLOR));
			}
		});
		return link;
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
	
	final void doLinkActivated(Hyperlink link) {
		if (isProjectPreferencePage()) {
			openWorkspacePreferences(NO_LINK_DATA);
		} else {
			HashSet projectsWithSpecifics= new HashSet();
			try {
				IJavaProject[] projects= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
				for (int i= 0; i < projects.length; i++) {
					IJavaProject curr= projects[i];
					if (hasProjectSpecificOptions(curr.getProject())) {
						projectsWithSpecifics.add(curr);
					}
				}
			} catch (JavaModelException e) {
				// ignore
			}
			ProjectSelectionDialog dialog= new ProjectSelectionDialog(getShell(), projectsWithSpecifics);
			if (dialog.open() == Window.OK) {
				IJavaProject res= (IJavaProject) dialog.getFirstResult();
				openProjectProperties(res.getProject(), NO_LINK_DATA);
			}
		}
	}
	
	protected final void openWorkspacePreferences(Object data) {
		String id= getPreferencePageID();
		PreferencesUtil.createPreferenceDialogOn(getShell(), id, new String[] { id }, data).open();
	}
	
	protected final void openProjectProperties(IProject project, Object data) {
		String id= getPropertyPageID();
		if (id != null) {
			PreferencesUtil.createPropertyDialogOn(getShell(), project, id, new String[] { id }, data).open();
		}
	}
	
	private void doProjectWorkspaceStateChanged() {
		enablePreferenceContent(useProjectSettings());
		updateLinkVisibility();
		doStatusChanged();
	}
	
	private void updateLinkVisibility() {
		if (fChangeWorkspaceSettings == null || fChangeWorkspaceSettings.isDisposed()) {
			return;
		}
		
		if (isProjectPreferencePage()) {
			fChangeWorkspaceSettings.setVisible(offerLink() && !useProjectSettings());
		} else {
			if (!offerLink()) {
				fChangeWorkspaceSettings.dispose();
			}
			fParentComposite.layout(true, true);
		}
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
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#applyData(java.lang.Object)
	 */
	public void applyData(Object data) {
		fData= data;
		if (fChangeWorkspaceSettings != null) {
			updateLinkVisibility();
		}
 	}
	
	protected Object getData() {
		return fData;
	}
	
}

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

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/*
 * The page to configure the naming style options.
 */
public class ImportOrganizePreferencePage extends PropertyAndPreferencePage {

	public static final String PREF_ID= "org.eclipse.jdt.ui.preferences.ImportOrganizePreferencePage"; //$NON-NLS-1$
	public static final String PROP_ID= "org.eclipse.jdt.ui.propertyPages.ImportOrganizePreferencePage"; //$NON-NLS-1$

	
	private ImportOrganizeConfigurationBlock fConfigurationBlock;

	public ImportOrganizePreferencePage() {
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setDescription(PreferencesMessages.getString("ImportOrganizePreferencePage.description")); //$NON-NLS-1$
		
		// only used when page is shown programatically
		setTitle(PreferencesMessages.getString("ImportOrganizePreferencePage.title"));		 //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		fConfigurationBlock= new ImportOrganizeConfigurationBlock(getNewStatusChangedListener(), getProject());
		
		super.createControl(parent);
		WorkbenchHelp.setHelp(parent, IJavaHelpContextIds.ORGANIZE_IMPORTS_PREFERENCE_PAGE);
	}

	protected Control createPreferenceContent(Composite composite) {
		return fConfigurationBlock.createContents(composite);
	}
	
	protected boolean hasProjectSpecificOptions() {
		return fConfigurationBlock.hasProjectSpecificOptions();
	}
	
	protected void openWorkspacePreferences() {
		ImportOrganizePreferencePage page= new ImportOrganizePreferencePage();
		PreferencePageSupport.showPreferencePage(getShell(), PREF_ID, page);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.DialogPage#dispose()
	 */
	public void dispose() {
		if (fConfigurationBlock != null) {
			fConfigurationBlock.dispose();
		}
		super.dispose();
	}
	
	/*
	 * @see org.eclipse.jface.preference.IPreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		super.performDefaults();
		if (fConfigurationBlock != null) {
			fConfigurationBlock.performDefaults();
		}
	}

	/*
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	public boolean performOk() {
		boolean enabled= !isProjectPreferencePage() || useProjectSettings();
		if (fConfigurationBlock != null && !fConfigurationBlock.performOk(enabled)) {
			return false;
		}	
		return super.performOk();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.preferences.PropertyAndPreferencePage#setElement(org.eclipse.core.runtime.IAdaptable)
	 */
	public void setElement(IAdaptable element) {
		super.setElement(element);
		setDescription(null); // no description for property page
	}
	

}




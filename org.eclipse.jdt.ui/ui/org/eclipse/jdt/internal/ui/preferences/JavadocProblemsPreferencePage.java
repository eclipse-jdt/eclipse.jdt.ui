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

/**
 * Page used to configure both workspace and project specific compiler settings
 */
public class JavadocProblemsPreferencePage extends PropertyAndPreferencePage {

	public static final String PREF_ID= "org.eclipse.jdt.ui.preferences.JavadocProblemsPreferencePage"; //$NON-NLS-1$
	public static final String PROP_ID= "org.eclipse.jdt.ui.propertyPages.JavadocProblemsPropertyPage"; //$NON-NLS-1$

	private JavadocProblemsConfigurationBlock fConfigurationBlock;

	public JavadocProblemsPreferencePage() {
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		//setDescription(PreferencesMessages.getString("JavadocProblemsPreferencePage.description")); //$NON-NLS-1$
		
		// only used when page is shown programatically
		setTitle(PreferencesMessages.getString("JavadocProblemsPreferencePage.title"));		 //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		fConfigurationBlock= new JavadocProblemsConfigurationBlock(getNewStatusChangedListener(), getProject());
		
		super.createControl(parent);
		if (isProjectPreferencePage()) {
			WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.COMPILER_PROPERTY_PAGE);
		} else {
			WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.COMPILER_PREFERENCE_PAGE);
		}
	}

	protected Control createPreferenceContent(Composite composite) {
		return fConfigurationBlock.createContents(composite);
	}
	
	protected boolean hasProjectSpecificOptions() {
		return fConfigurationBlock.hasProjectSpecificOptions();
	}
	
	protected void openWorkspacePreferences() {
		JavadocProblemsPreferencePage page= new JavadocProblemsPreferencePage();
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

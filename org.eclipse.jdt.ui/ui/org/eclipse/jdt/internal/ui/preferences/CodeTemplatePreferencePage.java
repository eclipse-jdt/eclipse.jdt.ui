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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.text.templates.persistence.TemplatePersistenceData;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;

/*
 * The page to configure the code templates.
 */
public class CodeTemplatePreferencePage extends PropertyAndPreferencePage {

	public static final String PREF_ID= "org.eclipse.jdt.ui.preferences.CodeTemplatePreferencePage"; //$NON-NLS-1$
	public static final String PROP_ID= "org.eclipse.jdt.ui.propertyPages.CodeTemplatePreferencePage"; //$NON-NLS-1$
	
	private CodeTemplateBlock fCodeTemplateConfigurationBlock;

	public CodeTemplatePreferencePage() {
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		//setDescription(PreferencesMessages.getString("CodeTemplatesPreferencePage.description")); //$NON-NLS-1$
		
		// only used when page is shown programatically
		setTitle(PreferencesMessages.getString("CodeTemplatesPreferencePage.title"));		 //$NON-NLS-1$
	}

	/*
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		fCodeTemplateConfigurationBlock= new CodeTemplateBlock(getProject());
		
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.CODE_MANIPULATION_PREFERENCE_PAGE);
	}	

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.preferences.PropertyAndPreferencePage#createPreferenceContent(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createPreferenceContent(Composite composite) {
		return fCodeTemplateConfigurationBlock.createContents(composite);
	}
	
	/*
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		if (fCodeTemplateConfigurationBlock != null) {
			return fCodeTemplateConfigurationBlock.performOk(useProjectSettings());
		}
		return true;
	}
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		super.performDefaults();
		if (fCodeTemplateConfigurationBlock != null) {
			fCodeTemplateConfigurationBlock.performDefaults();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener#statusChanged(org.eclipse.core.runtime.IStatus)
	 */
	public void statusChanged(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferencePage#performCancel()
	 */
	public boolean performCancel() {
		if (fCodeTemplateConfigurationBlock != null) {
			fCodeTemplateConfigurationBlock.performCancel();
		}
		return super.performCancel();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.preferences.PropertyAndPreferencePage#hasProjectSpecificOptions()
	 */
	protected boolean hasProjectSpecificOptions() {
		return fCodeTemplateConfigurationBlock.hasProjectSpecificOptions();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.preferences.PropertyAndPreferencePage#openWorkspacePreferences()
	 */
	protected void openWorkspacePreferences() {
		CodeTemplatePreferencePage page= new CodeTemplatePreferencePage();
		PreferencePageSupport.showPreferencePage(getShell(), PREF_ID, page);
	}

	/*
	 * @see org.eclipse.jface.preference.PreferencePage#applyData(java.lang.Object)
	 */
	public void applyData(Object data) {
		if (data instanceof String) {
			final String name= (String) data;
			final TemplatePersistenceData[] templates= fCodeTemplateConfigurationBlock.fTemplateStore.getTemplateData();
			TemplatePersistenceData template= null;
			for (int index= 0; index < templates.length; index++) {
				template= templates[index];
				if (template.getTemplate().getName().equals(name)) {
					fCodeTemplateConfigurationBlock.postSetSelection(template);
				}
			}
		}
	}
}
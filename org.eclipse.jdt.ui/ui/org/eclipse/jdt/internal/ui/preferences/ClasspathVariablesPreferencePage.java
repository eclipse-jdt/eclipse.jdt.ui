/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.VariableBlock;

public class ClasspathVariablesPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String ID= "org.eclipse.jdt.ui.preferences.ClasspathVariablesPreferencePage"; //$NON-NLS-1$

	private VariableBlock fVariableBlock;
	
	/**
	 * Constructor for ClasspathVariablesPreferencePage
	 */
	public ClasspathVariablesPreferencePage() {
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		fVariableBlock= new VariableBlock(true, null);
		
		// title only used when page is shown programatically
		setTitle(PreferencesMessages.getString("ClasspathVariablesPreferencePage.title")); //$NON-NLS-1$
		setDescription(PreferencesMessages.getString("ClasspathVariablesPreferencePage.description")); //$NON-NLS-1$
	}

	/*
	 * @see PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		WorkbenchHelp.setHelp(parent, IJavaHelpContextIds.CP_VARIABLES_PREFERENCE_PAGE);
		return fVariableBlock.createContents(parent);
	}
	
	/*
	 * @see IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		fVariableBlock.performDefaults();
		super.performDefaults();
	}

	/*
	 * @see PreferencePage#performOk()
	 */
	public boolean performOk() {
		JavaPlugin.getDefault().savePluginPreferences();
		return fVariableBlock.performOk();
	}
	
}

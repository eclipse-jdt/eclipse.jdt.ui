/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.VariableBlock;

public class ClasspathVariablesPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private VariableBlock fVariableBlock;
	
	/**
	 * Constructor for ClasspathVariablesPreferencePage
	 */
	public ClasspathVariablesPreferencePage() {
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		IStatusChangeListener listener= new IStatusChangeListener() {
			public void statusChanged(IStatus status) {
				updateStatus(status);
			}
		};
		fVariableBlock= new VariableBlock(true, null);
		setDescription(JavaUIMessages.getString("ClasspathVariablesPreferencePage.description")); //$NON-NLS-1$
	}

	/**
	 * @see PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		WorkbenchHelp.setHelp(parent, IJavaHelpContextIds.CP_VARIABLES_PREFERENCE_PAGE);
		return fVariableBlock.createContents(parent);
	}
	
	/**
	 * @see IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
	/**
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		fVariableBlock.performDefaults();
		super.performDefaults();
	}

	/**
	 * @see PreferencePage#performOk()
	 */
	public boolean performOk() {
		JavaPlugin.getDefault().savePluginPreferences();
		return fVariableBlock.performOk();
	}
	
	private void updateStatus(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}		
	
	
	/**
	 * Initializes the default values of this page in the preference bundle.
	 * Will be called on startup of the JavaPlugin
	 */
	public static void initDefaults(IPreferenceStore prefs) {
	}	
}

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

/*
 * The page to configure the compiler options.
 */
public class CompilerPreferencePage extends PreferencePage implements IWorkbenchPreferencePage, IStatusChangeListener {

	private CompilerConfigurationBlock fConfigurationBlock;

	public CompilerPreferencePage() {
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setDescription(JavaUIMessages.getString("CompilerPreferencePage.description")); //$NON-NLS-1$
		fConfigurationBlock= new CompilerConfigurationBlock(this, null);
	}

	public static void initDefaults(IPreferenceStore store) {
	}
		
	/*
	 * @see IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */	
	public void init(IWorkbench workbench) {
	}

	/*
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		// added for 1GEUGE6: ITPJUI:WIN2000 - Help is the same on all preference pages
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.COMPILER_PREFERENCE_PAGE);
	}	

	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		return fConfigurationBlock.createContents(parent);
	}

	/*
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		if (!fConfigurationBlock.performOk(true)) {
			return false;
		}	
		return super.performOk();
	}
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		fConfigurationBlock.performDefaults();
		super.performDefaults();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener#IStatusChangeListener()
	 */
	public void IStatusChangeListener() {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener#statusChanged(org.eclipse.core.runtime.IStatus)
	 */
	public void statusChanged(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);		
	}

}
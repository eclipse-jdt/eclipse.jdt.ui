/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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
import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.PageBook;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.preferences.formatter.CodingStyleConfigurationBlock;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;

/*
 * The page to configure the code formatter options.
 */
public class CodeFormatterPreferencePage extends PreferencePage implements IWorkbenchPreferencePage, IStatusChangeListener {

	private CodeFormatterConfigurationBlock fOldConfigurationBlock;
	private CodingStyleConfigurationBlock fNewConfigurationBlock;
	
	private PageBook fPagebook;
	
	private Control fControlNew;
	private Control fControlOld;

	public CodeFormatterPreferencePage() {
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setDescription(PreferencesMessages.getString("CodeFormatterPreferencePage.description")); //$NON-NLS-1$
		
		// only used when page is shown programatically
		setTitle(PreferencesMessages.getString("CodeFormatterPreferencePage.title"));		 //$NON-NLS-1$
		
		fOldConfigurationBlock= new CodeFormatterConfigurationBlock(this, null);
		fNewConfigurationBlock= new CodingStyleConfigurationBlock();
	}
	
	private boolean useNewFormatter() {
		return getPreferenceStore().getBoolean(WorkInProgressPreferencePage.PREF_FORMATTER);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		if (visible)  {
			updateVisibility();
		}
		super.setVisible(visible);
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
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.CODEFORMATTER_PREFERENCE_PAGE);
	}
	

	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		
		fPagebook= new PageBook(parent, SWT.NONE);
		fControlNew= fNewConfigurationBlock.createContents(fPagebook);
		fControlOld= fOldConfigurationBlock.createContents(fPagebook); 
		updateVisibility();
		
		Dialog.applyDialogFont(fControlNew);
		Dialog.applyDialogFont(fControlOld);
		return fPagebook;
	}
	
	private void updateVisibility() {
		if (useNewFormatter()) {
			fPagebook.showPage(fControlNew);
		} else {
			fPagebook.showPage(fControlOld);
		}
	}

	/*
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		if (useNewFormatter()) {
			fNewConfigurationBlock.performOk();
			return true;
		} else {
			if (!fOldConfigurationBlock.performOk(true)) {
				return false;
			}	
		}
		return super.performOk();
	}
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		if (useNewFormatter()) {
			// not supported
		} else {
			fOldConfigurationBlock.performDefaults();
		}
		super.performDefaults();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener#statusChanged(org.eclipse.core.runtime.IStatus)
	 */
	public void statusChanged(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);		
	}

}




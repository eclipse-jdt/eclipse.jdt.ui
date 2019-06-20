/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.VariableBlock;

public class ClasspathVariablesPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String ID= JavaUI.ID_CLASSPATH_VARIABLES_PREFERENCE_PAGE;

	public static final String DATA_SELECT_VARIABLE= "ClasspathVariablesPreferencePage.select_var"; //$NON-NLS-1$

	private VariableBlock fVariableBlock;
	private String fStoredSettings;

	/**
	 * Constructor for ClasspathVariablesPreferencePage
	 */
	public ClasspathVariablesPreferencePage() {
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		fVariableBlock= new VariableBlock(true, null);
		fStoredSettings= null;

		// title only used when page is shown programatically
		setTitle(PreferencesMessages.ClasspathVariablesPreferencePage_title);
		setDescription(PreferencesMessages.ClasspathVariablesPreferencePage_description);
		noDefaultAndApplyButton();
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.CP_VARIABLES_PREFERENCE_PAGE);
	}

	/*
	 * @see PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		Control result= fVariableBlock.createContents(parent);
		Dialog.applyDialogFont(result);
		return result;
	}

	/*
	 * @see IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	@Override
	public void init(IWorkbench workbench) {
	}

	/*
	 * @see PreferencePage#performDefaults()
	 */
	@Override
	protected void performDefaults() {
		// not used (constructor calls noDefaultAndApplyButton())
//		fVariableBlock.performDefaults();
		super.performDefaults();
	}

	/*
	 * @see PreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		JavaPlugin.flushInstanceScope();
		return fVariableBlock.performOk();
	}

	@Override
	public void setVisible(boolean visible) {
		// check if the stored settings have changed
		if (visible) {
			if (fStoredSettings != null && !fStoredSettings.equals(getCurrentSettings())) {
				fVariableBlock.refresh(null);
			}
		} else {
			if (fVariableBlock.hasChanges()) {
				String title= PreferencesMessages.ClasspathVariablesPreferencePage_savechanges_title;
				String message= PreferencesMessages.ClasspathVariablesPreferencePage_savechanges_message;
				if (MessageDialog.openQuestion(getShell(), title, message)) {
					performOk();
				}
				fVariableBlock.setChanges(false); // forget
			}
			fStoredSettings= getCurrentSettings();
		}
		super.setVisible(visible);
	}

	private String getCurrentSettings() {
		StringBuilder buf= new StringBuilder();
		for (String curr : JavaCore.getClasspathVariableNames()) {
			buf.append(curr).append('\0');
			IPath val= JavaCore.getClasspathVariable(curr);
			if (val != null) {
				buf.append(val.toString());
			}
			buf.append('\0');
		}
		return buf.toString();
	}

	@Override
	public void applyData(Object data) {
		if (data instanceof Map && fVariableBlock != null) {
			Object id= ((Map<?, ?>) data).get(DATA_SELECT_VARIABLE);
			if (id instanceof String) {
				fVariableBlock.setSelection((String) id);
			}
		}
		super.applyData(data);
	}

}

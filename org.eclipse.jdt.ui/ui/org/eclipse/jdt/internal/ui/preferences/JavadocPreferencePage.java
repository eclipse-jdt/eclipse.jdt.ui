/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids <sdavids@gmx.de> bug 38692
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import java.io.File;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;

public class JavadocPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String ID= "org.eclipse.jdt.ui.preferences.JavadocPreferencePage"; //$NON-NLS-1$
	
	
	private StringButtonDialogField fJavadocSelection;
	private Composite fComposite;

	private static final String PREF_JAVADOC_COMMAND= PreferenceConstants.JAVADOC_COMMAND;

	private class JDocDialogFieldAdapter implements IDialogFieldListener, IStringButtonAdapter {
		/*
		 * @see IDialogFieldListener#dialogFieldChanged(DialogField)
		 */
		public void dialogFieldChanged(DialogField field) {
			doValidation();
		}

		/*
		 * @see IStringButtonAdapter#changeControlPressed(DialogField)
		 */
		public void changeControlPressed(DialogField field) {
			handleFileBrowseButtonPressed(fJavadocSelection.getTextControl(fComposite), null, PreferencesMessages.getString("JavadocPreferencePage.browsedialog.title")); //$NON-NLS-1$

		}

	}

	/**
	 * Returns the configured Javadoc command or the empty string.
	 */
	public static String getJavaDocCommand() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		String cmd= store.getString(PREF_JAVADOC_COMMAND);
		if (cmd.length() == 0 && store.getDefaultString(PREF_JAVADOC_COMMAND).length() == 0) {
			initJavadocCommandDefault(store);
			cmd= store.getString(PREF_JAVADOC_COMMAND);
		}
		return cmd;
	}

	public JavadocPreferencePage() {
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setTitle(PreferencesMessages.getString("JavadocPreferencePage.title"));  //$NON-NLS-1$
		
		//setDescription("Javadoc command"); //$NON-NLS-1$
	}
	
	/*
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.JAVADOC_PREFERENCE_PAGE);
	}	
	
	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		initializeDialogUnits(parent);
		
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 3;

		fComposite= new Composite(parent, SWT.NONE);
		fComposite.setLayout(layout);

		DialogField javaDocCommentLabel= new DialogField();
		javaDocCommentLabel.setLabelText(PreferencesMessages.getString("JavadocPreferencePage.description")); //$NON-NLS-1$
		javaDocCommentLabel.doFillIntoGrid(fComposite, 3);
		LayoutUtil.setWidthHint(javaDocCommentLabel.getLabelControl(null), convertWidthInCharsToPixels(80));

		JDocDialogFieldAdapter adapter= new JDocDialogFieldAdapter();

		fJavadocSelection= new StringButtonDialogField(adapter);
		fJavadocSelection.setDialogFieldListener(adapter);
		fJavadocSelection.setLabelText(PreferencesMessages.getString("JavadocPreferencePage.command.label")); //$NON-NLS-1$
		fJavadocSelection.setButtonLabel(PreferencesMessages.getString("JavadocPreferencePage.command.button")); //$NON-NLS-1$
		fJavadocSelection.doFillIntoGrid(fComposite, 3);
		LayoutUtil.setHorizontalGrabbing(fJavadocSelection.getTextControl(null));
		LayoutUtil.setWidthHint(fJavadocSelection.getTextControl(null), convertWidthInCharsToPixels(10));

		initFields();

		Dialog.applyDialogFont(fComposite);
		return fComposite;
	}

	/*
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
	private static void initJavadocCommandDefault(IPreferenceStore store) {
		File file= findJavaDocCommand();
		if (file != null) {
			store.setDefault(PREF_JAVADOC_COMMAND, file.getPath());
		}	
	}
	

	private static File findJavaDocCommand() {
		IVMInstall install= JavaRuntime.getDefaultVMInstall();
		if (install != null) {
			File res= getCommand(install);
			if (res != null) {
				return res;
			}
		}
		
		IVMInstallType[] jreTypes= JavaRuntime.getVMInstallTypes();
		for (int i= 0; i < jreTypes.length; i++) {
			IVMInstallType jreType= jreTypes[i];
			IVMInstall[] installs= jreType.getVMInstalls();
			for (int k= 0; k < installs.length; k++) {
				File res= getCommand(installs[i]);
				if (res != null) {
					return res;
				}
			}
		}
		return null;
	}

	private static File getCommand(IVMInstall install) {
		File installLocation= install.getInstallLocation();
		if (installLocation != null) {
			File javaDocCommand= new File(installLocation, "bin/javadoc"); //$NON-NLS-1$
			if (javaDocCommand.isFile()) {
				return javaDocCommand;
			}
			javaDocCommand= new File(installLocation, "bin/javadoc.exe"); //$NON-NLS-1$
			if (javaDocCommand.isFile()) {
				return javaDocCommand;
			}
		}
		return null;
	}
	
	private void initFields() {
		fJavadocSelection.setTextWithoutUpdate(getJavaDocCommand());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	public boolean performOk() {
		getPreferenceStore().setValue(PREF_JAVADOC_COMMAND, fJavadocSelection.getText());
		JavaPlugin.getDefault().savePluginPreferences();
		return super.performOk();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		IPreferenceStore store= getPreferenceStore();
		initJavadocCommandDefault(store);

		fJavadocSelection.setText(store.getDefaultString(PREF_JAVADOC_COMMAND));
		super.performDefaults();
	}

	private void doValidation() {
		StatusInfo status= new StatusInfo();
		
		String text= fJavadocSelection.getText();
		if (text.length() > 0) {
			File file= new File(text);
			if (!file.isFile()) {
				status.setError(PreferencesMessages.getString("JavadocPreferencePage.error.notexists"));	 //$NON-NLS-1$
			}
		} else {
			//bug 38692
			status.setInfo(PreferencesMessages.getString("JavadocPreferencePage.info.notset")); //$NON-NLS-1$
		}
		updateStatus(status);
	}

	protected void updateStatus(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}

	protected void handleFileBrowseButtonPressed(Text text, String[] extensions, String title) {

		FileDialog dialog= new FileDialog(text.getShell());
		dialog.setText(title);
		dialog.setFilterExtensions(extensions);
		String dirName= text.getText();
		if (!dirName.equals("")) { //$NON-NLS-1$
			File path= new File(dirName);
			if (path.exists())
				dialog.setFilterPath(dirName);

		}
		String selectedDirectory= dialog.open();
		if (selectedDirectory != null)
			text.setText(selectedDirectory);
	}

}

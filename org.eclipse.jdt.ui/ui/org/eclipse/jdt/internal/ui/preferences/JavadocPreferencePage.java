/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import java.io.File;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;

/**
 * @version 	1.0
 * @author
 */
public class JavadocPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private StringButtonDialogField fJavadocSelection;
	private Composite fComposite;

	private StatusInfo fJavadocCommandStatus;

	private static final String SETTINGS_JAVADOCPREFERENCES_COMMAND= "command";

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
			handleFileBrowseButtonPressed(fJavadocSelection.getTextControl(fComposite), null, "Javadoc Command Selection");

		}

	}

	public static String getJavaDocCommand() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return store.getString(SETTINGS_JAVADOCPREFERENCES_COMMAND);
	}

	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		MGridLayout layout= new MGridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 3;

		fComposite= new Composite(parent, SWT.NONE);
		fComposite.setLayout(layout);

		DialogField javaDocCommentLabel= new DialogField();
		javaDocCommentLabel.setLabelText("Location of the Javadoc command must be an absolute path.");
		javaDocCommentLabel.doFillIntoGrid(fComposite, 3);

		fJavadocSelection= new StringButtonDialogField(new JDocDialogFieldAdapter());
		fJavadocSelection.setLabelText("J&avadoc command:");
		fJavadocSelection.setButtonLabel("Bro&wse...");
		fJavadocSelection.doFillIntoGrid(fComposite, 3);

		initFields();

		return fComposite;
	}

	/*
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	public static void initDefaults(IPreferenceStore store) {
		File file= findJavaDocCommand();
		if (file != null)
			store.setDefault(SETTINGS_JAVADOCPREFERENCES_COMMAND, file.getPath());
		else
			store.setDefault(SETTINGS_JAVADOCPREFERENCES_COMMAND, "");
	}

	private static File findJavaDocCommand() {
		IVMInstallType[] jreTypes= JavaRuntime.getVMInstallTypes();
		for (int i= 0; i < jreTypes.length; i++) {
			IVMInstallType jreType= jreTypes[i];
			IVMInstall[] installs= jreType.getVMInstalls();
			for (int k= 0; k < installs.length; k++) {
				File installLocation= installs[k].getInstallLocation();
				if (installLocation != null) {
					File javaDocCommand= new File(installLocation, "bin/javadoc");
					if (javaDocCommand.isFile()) {
						return javaDocCommand;
					}
					javaDocCommand= new File(installLocation, "bin/javadoc.exe");
					if (javaDocCommand.isFile()) {
						return javaDocCommand;
					}
				}
			}
		}
		return null;
	}

	private void initFields() {
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();

		String command= prefs.getString(SETTINGS_JAVADOCPREFERENCES_COMMAND);
		fJavadocSelection.setText(command);
	}

	public boolean performOk() {
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		prefs.setValue(SETTINGS_JAVADOCPREFERENCES_COMMAND, fJavadocSelection.getText());
		return super.performOk();
	}

	protected void performDefaults() {
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();

		String str= prefs.getDefaultString(SETTINGS_JAVADOCPREFERENCES_COMMAND);
		File jdocCommand= findJavaDocCommand();
		if (jdocCommand != null) {
			str= jdocCommand.getPath();
			prefs.setDefault(SETTINGS_JAVADOCPREFERENCES_COMMAND, str);
		}
		fJavadocSelection.setText(str);

		super.performDefaults();
	}

	private void doValidation() {
		String text= fJavadocSelection.getText();
		File file= new File(text);
		if (!file.isFile()) {
			fJavadocCommandStatus= new StatusInfo();
			fJavadocCommandStatus.setError("Javadoc command does not exist.");
			updateStatus(fJavadocCommandStatus);
		} else
			updateStatus(new StatusInfo());
	}

	private void updateStatus(IStatus status) {
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
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;

import org.eclipse.ui.dialogs.PropertyPage;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.corext.javadoc.JavaDocLocations;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;

/**
 * Property page used to set the project's Javadoc location for sources
 */
public class JavadocConfigurationPropertyPage extends PropertyPage {

	private URL fJavaDocLocation;

	private StringButtonDialogField fJavaDocField;
	private IStatus fJavaDocStatus;

	public JavadocConfigurationPropertyPage() {
		setDescription("Specify the location (URL) of the project's Javadoc documentation.\nExample is 'file://c:/myworkspace/myproject/doc/'. This location is used by the Javadoc export wizard as default value and by the 'Open External Javadoc' action.");
	}


	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite topComp= new Composite(parent, SWT.NONE);
		GridLayout topLayout= new GridLayout();
		topLayout.numColumns= 3;
		topComp.setLayout(topLayout);

		JDocConfigurationAdapter adapter= new JDocConfigurationAdapter();

		fJavaDocField= new StringButtonDialogField(adapter);
		fJavaDocField.setDialogFieldListener(adapter);
		fJavaDocField.setLabelText("&Javadoc Location: ");
		fJavaDocField.setButtonLabel("Bro&wse...");

		fJavaDocField.doFillIntoGrid(topComp, 3);

		LayoutUtil.setWidthHint(fJavaDocField.getTextControl(null), convertWidthInCharsToPixels(40));
		LayoutUtil.setHorizontalGrabbing(fJavaDocField.getTextControl(null));

		setValues();

		return topComp;
	}

	//Sets the default by getting the stored URL setting if it exists
	//otherwise the text box is left empty.
	private void setValues() {
		IJavaProject project= getJavaProject();
		URL location= null;
		if (project != null) {
			location= JavaDocLocations.getProjectJavadocLocation(project);
		}
		if (location != null)
			fJavaDocField.setText(location.toExternalForm());
		else
			fJavaDocField.setText("");

	}

	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		fJavaDocField.setText("");
		super.performDefaults();
	}

	private void jdocChangeControlPressed(DialogField field) {
		if (field == fJavaDocField) {
			URL jdocURL= chooseJavaDocLocation();
			if (jdocURL != null) {
				fJavaDocField.setText(jdocURL.toExternalForm());
			}
		}
	}

	private void jdocDialogFieldChanged(DialogField field) {
		if (field == fJavaDocField) {
			fJavaDocStatus= updateJavaDocLocationStatus();
			StatusUtil.applyToStatusLine(this, fJavaDocStatus);
		}
	}

	private IStatus updateJavaDocLocationStatus() {
		StatusInfo status= new StatusInfo();
		fJavaDocLocation= null;
		String jdocLocation= fJavaDocField.getText();
		if (jdocLocation.length() > 0) {
			try {
				URL url= new URL(jdocLocation);
				if ("file".equals(url.getProtocol())) {
					String temp= url.getFile();
					if (temp == null) {
						status.setError("Not a valid URL.");
						return status;
					} else {
						File dir= new File(url.getFile());
						if (!dir.isDirectory()) {
							status.setError("Location does not exist.");
							return status;
						}
						File packagesFile= new File(dir, "package-list");
						if (!packagesFile.exists()) {
							status.setWarning("Location does not contain file 'package-list'.");
							// only a warning, go on
						}						
					}
				}
				fJavaDocLocation= url;
			} catch (MalformedURLException e) {
				status.setError(e.getLocalizedMessage());
				return status;
			}
		}
		return status;
	}

	private URL chooseJavaDocLocation() {
		String initPath= "";
		if (fJavaDocLocation != null && "file".equals(fJavaDocLocation.getProtocol())) {
			initPath= (new File(fJavaDocLocation.getFile())).getPath();
		}
		DirectoryDialog dialog= new DirectoryDialog(getShell());
		dialog.setText("Javadoc Location Selection");
		dialog.setMessage("&Select project's Javadoc location:");
		dialog.setFilterPath(initPath);
		String res= dialog.open();
		if (res != null) {
			try {
				return (new File(res)).toURL();
			} catch (MalformedURLException e) {
				// should not happen
				JavaPlugin.log(e);
			}
		}
		return null;
	}

	/**
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		IJavaProject jproject= getJavaProject();
		if (jproject != null) {
			JavaDocLocations.setProjectJavadocLocation(jproject, fJavaDocLocation);
		}
		return true;
	}

	private IJavaProject getJavaProject() {
		IAdaptable adaptable= getElement();
		IJavaElement elem= (IJavaElement) adaptable.getAdapter(IJavaElement.class);
		if (elem instanceof IJavaProject) {
			return (IJavaProject) elem;
		} else {
			return null;
		}
	}

	private class JDocConfigurationAdapter implements IStringButtonAdapter, IDialogFieldListener {

		// -------- IStringButtonAdapter --------
		public void changeControlPressed(DialogField field) {
			jdocChangeControlPressed(field);
		}

		// ---------- IDialogFieldListener --------
		public void dialogFieldChanged(DialogField field) {
			jdocDialogFieldChanged(field);
		}
	}
}
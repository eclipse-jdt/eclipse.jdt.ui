/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IAdaptable;
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
import org.eclipse.jdt.internal.ui.JavaUIMessages;
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
		setDescription(JavaUIMessages.getString("JavadocConfigurationPropertyPage.description")); //$NON-NLS-1$
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
		fJavaDocField.setLabelText(JavaUIMessages.getString("JavadocConfigurationPropertyPage.location.label")); //$NON-NLS-1$
		fJavaDocField.setButtonLabel(JavaUIMessages.getString("JavadocConfigurationPropertyPage.location.button")); //$NON-NLS-1$

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
			fJavaDocField.setText(""); //$NON-NLS-1$

	}

	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		fJavaDocField.setText(""); //$NON-NLS-1$
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
				if ("file".equals(url.getProtocol())) { //$NON-NLS-1$
					if (url.getFile() == null) {
						status.setError(JavaUIMessages.getString("JavadocConfigurationPropertyPage.error.notafolder")); //$NON-NLS-1$
						return status;
					} else {
						File dir= new File(url.getFile());
						if (!dir.isDirectory()) {
							status.setError(JavaUIMessages.getString("JavadocConfigurationPropertyPage.error.notafolder")); //$NON-NLS-1$
							return status;
						}
						File packagesFile= new File(dir, "package-list"); //$NON-NLS-1$
						if (!packagesFile.exists()) {
							status.setWarning(JavaUIMessages.getString("JavadocConfigurationPropertyPage.warning.packagelistnotfound")); //$NON-NLS-1$
							// only a warning, go on
						}						
					}
				}
				fJavaDocLocation= url;
			} catch (MalformedURLException e) {
				status.setError(JavaUIMessages.getFormattedString("JavadocConfigurationPropertyPage.error.invalidurl", e.getLocalizedMessage())); //$NON-NLS-1$
				return status;
			}
		}
		return status;
	}

	private URL chooseJavaDocLocation() {
		String initPath= ""; //$NON-NLS-1$
		if (fJavaDocLocation != null && "file".equals(fJavaDocLocation.getProtocol())) { //$NON-NLS-1$
			initPath= (new File(fJavaDocLocation.getFile())).getPath();
		}
		DirectoryDialog dialog= new DirectoryDialog(getShell());
		dialog.setText(JavaUIMessages.getString("JavadocConfigurationPropertyPage.javadocLocationDialog.label")); //$NON-NLS-1$
		dialog.setMessage(JavaUIMessages.getString("JavadocConfigurationPropertyPage.javadocLocationDialog.message")); //$NON-NLS-1$
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
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
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
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

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

	private IJavaElement fElem;

	public JavadocConfigurationPropertyPage() {
		
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		IJavaElement elem= getJavaElement();
		if(elem instanceof IPackageFragmentRoot)
			setDescription(JavaUIMessages.getString("JavadocConfigurationPropertyPage.IsPackageFragmentRoot.description")); //$NON-NLS-1$
		else if(elem instanceof IJavaProject) 
			setDescription(JavaUIMessages.getString("JavadocConfigurationPropertyPage.IsJavaProject.description"));  //$NON-NLS-1$
		else setDescription(JavaUIMessages.getString("JavadocConfigurationPropertyPage.IsIncorrectElement.description")); //$NON-NLS-1$

		super.createControl(parent);
	}


	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {

		Composite topComp= new Composite(parent, SWT.NONE);
		GridLayout topLayout= new GridLayout();
		topLayout.numColumns= 3;
		topLayout.marginWidth= 0;
		topComp.setLayout(topLayout);

		IJavaElement elem= getJavaElement();
		if (elem == null) {
			return topComp;
		}


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
		
				// try to find it as Java element (needed for external jars)
		IJavaElement elem= getJavaElement();
		
		String initialValue= "";//$NON-NLS-1$
	
		if(elem != null) {
				try {
					URL location = JavaDocLocations.getJavadocBaseLocation(elem);
					if (location != null)
						initialValue=  location.toExternalForm();
				} catch(JavaModelException e) {
					JavaPlugin.log(e);
				}
		}	
		fJavaDocField.setText(initialValue);

	}

	private IJavaElement getJavaElement() {
		if (fElem == null) {
			IAdaptable adaptable = getElement();
			fElem = (IJavaElement) adaptable.getAdapter(IJavaElement.class);
			if (fElem == null) {

				IResource resource = (IResource) adaptable.getAdapter(IResource.class);
				//special case when the .jar is a file
				try {
					if (resource instanceof IFile) {
						IProject proj = resource.getProject();
						if (proj.hasNature(JavaCore.NATURE_ID)) {
							IJavaProject jproject = JavaCore.create(proj);
							IPackageFragmentRoot root= jproject.findPackageFragmentRoot(resource.getFullPath());
							if (root != null && root.isArchive()) {
								fElem= root;
							}
							
						}
					}
				} catch (CoreException e) {
					JavaPlugin.log(e);
				}
			}
		}
		return fElem;
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
		IJavaElement elem= getJavaElement();
		if (elem != null) {
			IPath path= elem.getPath();
			if(elem instanceof IJavaProject)
				JavaDocLocations.setProjectJavadocLocation((IJavaProject)elem, fJavaDocLocation);
			else	JavaDocLocations.setLibraryJavadocLocation(path, fJavaDocLocation);
			return true;
		}
		return false;
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

	/* (non-Javadoc)
	 * @see IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		if (visible) {
			fJavaDocField.postSetFocusOnDialogField(getShell().getDisplay());
		}
		super.setVisible(visible);
	}

}
package org.eclipse.jdt.internal.ui.preferences;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.javadoc.JavaDocLocations;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.actions.OpenBrowserUtil;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;

public class JavadocConfigurationBlock {
	
	private StringButtonDialogField fJavaDocField;
	private Button fValidateButton;
	private IJavaElement fElem;
	private Shell fShell;
	private IStatusChangeListener fContext;
	private URL fJavaDocLocation;
	
	public JavadocConfigurationBlock(IJavaElement element, Shell shell,  IStatusChangeListener context) {
		fShell= shell;
		fElem= element;	
		fContext= context;	
	}
	
	public Control createContents(Composite parent) {
		final Composite topComp= new Composite(parent, SWT.NONE);
		GridLayout topLayout= new GridLayout();
		topLayout.numColumns= 3;
		topLayout.marginWidth= 0;
		topLayout.marginHeight= 0;
		topComp.setLayout(topLayout);

		if (fElem == null) {
			return topComp;
		}


		JDocConfigurationAdapter adapter= new JDocConfigurationAdapter();

		fJavaDocField= new StringButtonDialogField(adapter);
		fJavaDocField.setDialogFieldListener(adapter);
		fJavaDocField.setLabelText(JavaUIMessages.getString("JavadocConfigurationBlock.location.label")); //$NON-NLS-1$
		fJavaDocField.setButtonLabel(JavaUIMessages.getString("JavadocConfigurationBlock.location.button")); //$NON-NLS-1$

		fJavaDocField.doFillIntoGrid(topComp, 3);

		PixelConverter converter= new PixelConverter(topComp);
		LayoutUtil.setWidthHint(fJavaDocField.getTextControl(null), converter.convertWidthInCharsToPixels(40));
		LayoutUtil.setHorizontalGrabbing(fJavaDocField.getTextControl(null));

		// Fillers;
		new Label(topComp, SWT.LEFT);
		new Label(topComp, SWT.LEFT);
		
		fValidateButton= new Button(topComp, SWT.PUSH);
		fValidateButton.setText(JavaUIMessages.getString("JavadocConfigurationBlock.ValidateButton.label")); //$NON-NLS-1$
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_END);
		fValidateButton.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(fValidateButton);
		
		fValidateButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
				  EntryValidator validator= new EntryValidator();
				  BusyIndicator.showWhile(topComp.getDisplay(), validator);
			}
		});
		setValues();
		
		return topComp;
	}
	
	//Sets the default by getting the stored URL setting if it exists
	//otherwise the text box is left empty.
	private void setValues() {

		// try to find it as Java element (needed for external jars)

		String initialValue = ""; //$NON-NLS-1$

		if (fElem != null) {
			try {
				fJavaDocLocation = JavaDocLocations.getJavadocBaseLocation(fElem);
				if (fJavaDocLocation != null)
					initialValue = fJavaDocLocation.toExternalForm();
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		fJavaDocField.setText(initialValue);

	}
		
	public void setFocus() {
		fJavaDocField.postSetFocusOnDialogField(fShell.getDisplay());
	}
	
	public void performDefaults() {
		setValues();
	}
	
	public boolean performOk() {
		if (fElem != null) {
			IPath path = fElem.getPath();
			if (fElem instanceof IJavaProject)
				JavaDocLocations.setProjectJavadocLocation(
					(IJavaProject) fElem,
					fJavaDocLocation);
			else
				JavaDocLocations.setLibraryJavadocLocation(path, fJavaDocLocation);
			return true;
		}
		return false;
	}
	
	private class EntryValidator implements Runnable {

		private String fInvalidMessage= JavaUIMessages.getString("JavadocConfigurationBlock.InvalidLocation.message"); //$NON-NLS-1$
		private String fValidMessage= JavaUIMessages.getString("JavadocConfigurationBlock.ValidLocation.message"); //$NON-NLS-1$
		private String fTitle=  JavaUIMessages.getString("JavadocConfigurationBlock.MessageDialog.title"); //$NON-NLS-1$
		public void run() {

			Path path = new Path(fJavaDocField.getText());
			IPath index = path.append("index.html"); //$NON-NLS-1$
			IPath packagelist = path.append("package-list"); //$NON-NLS-1$
			String message = JavaUIMessages.getString("JavadocConfigurationBlock.UnableToValidateLocation.message"); //$NON-NLS-1$
			try {

				URL indexURL = new URL(index.toString());
				URL packagelistURL = new URL(packagelist.toString());
				String protocol = indexURL.getProtocol();

				if (protocol.equals("http")) { //$NON-NLS-1$
					validateURL(indexURL, packagelistURL);
				} else if (protocol.equals("file")) { //$NON-NLS-1$
					validateFile(indexURL, packagelistURL);
				} else {
					MessageDialog.openInformation(fShell, fTitle, message); //$NON-NLS-1$
				}
			} catch (MalformedURLException e) {
				MessageDialog.openInformation(fShell, fTitle, message); //$NON-NLS-1$
			}

		}
		
		public void validateFile(URL indexURL, URL packagelisURL) {
			
			File indexFile = new File(indexURL.getFile());
			if (indexFile.exists()) {
				File packaglistFile = new File(packagelisURL.getFile());
				if (packaglistFile.exists()) {
					if (MessageDialog.openConfirm(fShell, fTitle, fValidMessage)) { //$NON-NLS-1$
						spawnInBrowser(indexURL);
					}
					return;
				}
			}	
			MessageDialog.openInformation(fShell, fTitle, fInvalidMessage); //$NON-NLS-1$
		}

		public void spawnInBrowser(URL url) {
			OpenBrowserUtil.open(url, fShell, fTitle);
		}

		private void validateURL(URL indexURL, URL packagelistURL) {

			InputStream in1= null;
			InputStream in2= null;
			try {
				in1= indexURL.openConnection().getInputStream();
				in2= packagelistURL.openConnection().getInputStream();

				if (MessageDialog.openConfirm(fShell, fTitle, fValidMessage))
					spawnInBrowser(indexURL);

			} catch (IOException e) {
				MessageDialog.openInformation(fShell, fTitle, fInvalidMessage);
			} finally {
				if (in1 != null) { try { in1.close(); } catch (IOException e) {} }
				if (in2 != null) { try { in2.close(); } catch (IOException e) {} }
			}				
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
			IStatus status= updateJavaDocLocationStatus();
			fValidateButton.setEnabled(!status.matches(IStatus.ERROR) && fJavaDocField.getText().length() > 0);
			fContext.statusChanged(status);
		}
	}

	private URL chooseJavaDocLocation() {
		String initPath= ""; //$NON-NLS-1$
		if (fJavaDocLocation != null && "file".equals(fJavaDocLocation.getProtocol())) { //$NON-NLS-1$
			initPath= (new File(fJavaDocLocation.getFile())).getPath();
		}
		DirectoryDialog dialog= new DirectoryDialog(fShell);
		dialog.setText(JavaUIMessages.getString("JavadocConfigurationBlock.javadocLocationDialog.label")); //$NON-NLS-1$
		dialog.setMessage(JavaUIMessages.getString("JavadocConfigurationBlock.javadocLocationDialog.message")); //$NON-NLS-1$
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

	private IStatus updateJavaDocLocationStatus() {
		StatusInfo status= new StatusInfo();
		fJavaDocLocation= null;
		String jdocLocation= fJavaDocField.getText();
		if (jdocLocation.length() > 0) {
			try {
				URL url= new URL(jdocLocation);
				if ("file".equals(url.getProtocol())) { //$NON-NLS-1$
					if (url.getFile() == null) {
						status.setError(JavaUIMessages.getString("JavadocConfigurationBlock.error.notafolder")); //$NON-NLS-1$
						return status;
					} else {
						File dir= new File(url.getFile());
						if (!dir.isDirectory()) {
							status.setError(JavaUIMessages.getString("JavadocConfigurationBlock.error.notafolder")); //$NON-NLS-1$
							return status;
						}
						File packagesFile= new File(dir, "package-list"); //$NON-NLS-1$
						if (!packagesFile.exists()) {
							status.setWarning(JavaUIMessages.getString("JavadocConfigurationBlock.warning.packagelistnotfound")); //$NON-NLS-1$
							// only a warning, go on
						}						
					}
				}
				fJavaDocLocation= url;
			} catch (MalformedURLException e) {
				status.setError(JavaUIMessages.getString("JavadocConfigurationBlock.MalformedURL.error"));  //$NON-NLS-1$
				return status;
			}
		} else status.setWarning(JavaUIMessages.getString("JavadocConfigurationBlock.EmptyJavadocLocation.warning")); //$NON-NLS-1$
		return status;
	}


}

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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.OpenBrowserUtil;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

public class JavadocConfigurationBlock {
	
	private StringDialogField fJavaDocField;
	private URL fInitialURL;
	private SelectionButtonDialogField fValidateButton;
	private SelectionButtonDialogField fBrowseFolder;
	private Shell fShell;
	private IStatusChangeListener fContext;
	private URL fJavaDocLocation;
	
	public JavadocConfigurationBlock(Shell shell,  IStatusChangeListener context, URL initURL) {
		fShell= shell;
		fContext= context;
		fInitialURL= initURL;
	}
	
	public Control createContents(Composite parent) {
		PixelConverter converter= new PixelConverter(parent);
		
		Composite topComp= new Composite(parent, SWT.NONE);
		GridLayout topLayout= new GridLayout();
		topLayout.numColumns= 3;
		topLayout.marginWidth= 0;
		topLayout.marginHeight= 0;
		topComp.setLayout(topLayout);

		JDocConfigurationAdapter adapter= new JDocConfigurationAdapter();

		fJavaDocField= new StringDialogField();
		fJavaDocField.setDialogFieldListener(adapter);
		fJavaDocField.setLabelText(PreferencesMessages.getString("JavadocConfigurationBlock.location.label")); //$NON-NLS-1$
		fJavaDocField.doFillIntoGrid(topComp, 2);
		LayoutUtil.setWidthHint(fJavaDocField.getTextControl(null), converter.convertWidthInCharsToPixels(50));
		LayoutUtil.setHorizontalGrabbing(fJavaDocField.getTextControl(null));

		fBrowseFolder= new SelectionButtonDialogField(SWT.PUSH);
		fBrowseFolder.setDialogFieldListener(adapter);		
		fBrowseFolder.setLabelText(PreferencesMessages.getString("JavadocConfigurationBlock.browse.button")); //$NON-NLS-1$
		fBrowseFolder.doFillIntoGrid(topComp, 1);
		
		DialogField.createEmptySpace(topComp, 2);	
		
		fValidateButton= new SelectionButtonDialogField(SWT.PUSH);
		fValidateButton.setDialogFieldListener(adapter);		
		fValidateButton.setLabelText(PreferencesMessages.getString("JavadocConfigurationBlock.validate.button")); //$NON-NLS-1$
		fValidateButton.doFillIntoGrid(topComp, 1);

		setValues();
		
		return topComp;
	}
	
	//Sets the default by getting the stored URL setting if it exists
	//otherwise the text box is left empty.
	private void setValues() {
		String initialValue = fInitialURL != null ? fInitialURL.toExternalForm() : ""; //$NON-NLS-1$
		fJavaDocField.setText(initialValue);
	}
		
	public void setFocus() {
		fJavaDocField.postSetFocusOnDialogField(fShell.getDisplay());
	}
	
	public void performDefaults() {
		setValues();
	}
	
	public URL getJavadocLocation() {
		return fJavaDocLocation;
	}
		
	private class EntryValidator implements Runnable {

		private String fInvalidMessage= PreferencesMessages.getString("JavadocConfigurationBlock.InvalidLocation.message"); //$NON-NLS-1$
		private String fValidMessage= PreferencesMessages.getString("JavadocConfigurationBlock.ValidLocation.message"); //$NON-NLS-1$
		private String fTitle=  PreferencesMessages.getString("JavadocConfigurationBlock.MessageDialog.title"); //$NON-NLS-1$
		private String fUnable= PreferencesMessages.getString("JavadocConfigurationBlock.UnableToValidateLocation.message"); //$NON-NLS-1$
		public void run() {

			URL location= getJavadocLocation();
			if (location == null) {
				MessageDialog.openInformation(fShell, fTitle, fInvalidMessage); //$NON-NLS-1$
				return;
			}

			try {
				String protocol = location.getProtocol();
				if (protocol.equals("http")) { //$NON-NLS-1$
					validateURL(location);
				} else if (protocol.equals("file")) { //$NON-NLS-1$
					validateFile(location);
				} else if (protocol.equals("jar")) { //$NON-NLS-1$
					validateArchive(location);				
				} else {
					MessageDialog.openInformation(fShell, fTitle, fUnable); //$NON-NLS-1$
				}
			} catch (MalformedURLException e) {
				MessageDialog.openInformation(fShell, fTitle, fUnable); //$NON-NLS-1$
			}

		}
		
		public void spawnInBrowser(URL url) {
			OpenBrowserUtil.open(url, fShell, fTitle);
		}

		private void validateFile(URL location) throws MalformedURLException {
			File folder = new File(location.getFile());
			if (folder.isDirectory()) {
				File indexFile= new File(folder, "index.html"); //$NON-NLS-1$
				if (indexFile.isFile()) {				
					File packageList= new File(folder, "package-list"); //$NON-NLS-1$
					if (packageList.exists()) {
						if (MessageDialog.openConfirm(fShell, fTitle, fValidMessage)) { //$NON-NLS-1$
							spawnInBrowser(indexFile.toURL());
						}
						return;					
					}
				}
			}
			MessageDialog.openInformation(fShell, fTitle, fInvalidMessage); //$NON-NLS-1$
		}
		
		private void validateArchive(URL location) throws MalformedURLException {
//			String file= location.getFile();
//			int idx= file.indexOf('!');
//			if (idx != -1) {
//				URL url= new URL(file.substring(0, idx));
//				if (url.getProtocol())
//				
//				
//				
//				
//			}
//			
//			
//			
//			
//			File folder = new File(location.getFile());
//			if (folder.isDirectory()) {
//				File indexFile= new File(folder, "index.html"); //$NON-NLS-1$
//				if (indexFile.isFile()) {				
//					File packageList= new File(folder, "package-list"); //$NON-NLS-1$
//					if (packageList.exists()) {
//						if (MessageDialog.openConfirm(fShell, fTitle, fValidMessage)) { //$NON-NLS-1$
//							spawnInBrowser(indexFile.toURL());
//						}
//						return;					
//					}
//				}
//			}
			MessageDialog.openInformation(fShell, fTitle, fInvalidMessage); //$NON-NLS-1$
		}		

		private void validateURL(URL location) throws MalformedURLException {
			IPath path= new Path(location.toExternalForm());
			IPath index = path.append("index.html"); //$NON-NLS-1$
			IPath packagelist = path.append("package-list"); //$NON-NLS-1$
			URL indexURL = new URL(index.toString());
			URL packagelistURL = new URL(packagelist.toString());

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
	
	private class JDocConfigurationAdapter implements IDialogFieldListener {

		// ---------- IDialogFieldListener --------
		public void dialogFieldChanged(DialogField field) {
			jdocDialogFieldChanged(field);
		}
	}


	private void jdocDialogFieldChanged(DialogField field) {
		if (field == fJavaDocField) {
			IStatus status= updateJavaDocLocationStatus();
			fValidateButton.setEnabled(!status.matches(IStatus.ERROR) && fJavaDocField.getText().length() > 0);
			fContext.statusChanged(status);
		} else if (field == fValidateButton) {
			EntryValidator validator= new EntryValidator();
			BusyIndicator.showWhile(fShell.getDisplay(), validator);
		} else if (field == fBrowseFolder) {
			URL jdocURL= chooseJavaDocFolder();
			if (jdocURL != null) {
				fJavaDocField.setText(jdocURL.toExternalForm());
			}			
		}
	}

	private URL chooseJavaDocFolder() {
		String initPath= ""; //$NON-NLS-1$
		if (fJavaDocLocation != null && "file".equals(fJavaDocLocation.getProtocol())) { //$NON-NLS-1$
			initPath= (new File(fJavaDocLocation.getFile())).getPath();
		}
		DirectoryDialog dialog= new DirectoryDialog(fShell);
		dialog.setText(PreferencesMessages.getString("JavadocConfigurationBlock.javadocFolderDialog.label")); //$NON-NLS-1$
		dialog.setMessage(PreferencesMessages.getString("JavadocConfigurationBlock.javadocFolderDialog.message")); //$NON-NLS-1$
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
	
	/*
	private URL chooseJavaDocArchive() {
		IPath initPath= Path.EMPTY; //$NON-NLS-1$
		if (fJavaDocLocation != null && "file".equals(fJavaDocLocation.getProtocol())) { //$NON-NLS-1$
			initPath= new Path((new File(fJavaDocLocation.getFile())).getPath());
		}		
					
		if (ArchiveFileFilter.isArchivePath(initPath)) {
			initPath= initPath.removeLastSegments(1);
		}
	
		FileDialog dialog= new FileDialog(fShell);
		dialog.setText(PreferencesMessages.getString("JavadocConfigurationBlock.javadocArchiveDialog.label")); //$NON-NLS-1$
		dialog.setFilterExtensions(new String[] {"*.jar;*.zip"}); //$NON-NLS-1$
		dialog.setFilterPath(initPath.toOSString());
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
	*/
	private IStatus updateJavaDocLocationStatus() {
		StatusInfo status= new StatusInfo();
		fJavaDocLocation= null;
		String jdocLocation= fJavaDocField.getText();
		if (jdocLocation.length() > 0) {
			try {
				URL url= new URL(jdocLocation);
				if ("file".equals(url.getProtocol())) { //$NON-NLS-1$
					if (url.getFile() == null) {
						status.setError(PreferencesMessages.getString("JavadocConfigurationBlock.error.notafolder")); //$NON-NLS-1$
						return status;
					} else {
						File dir= new File(url.getFile());
						if (!dir.isDirectory()) {
							status.setError(PreferencesMessages.getString("JavadocConfigurationBlock.error.notafolder")); //$NON-NLS-1$
							return status;
						}
						File packagesFile= new File(dir, "package-list"); //$NON-NLS-1$
						if (!packagesFile.exists()) {
							status.setWarning(PreferencesMessages.getString("JavadocConfigurationBlock.warning.packagelistnotfound")); //$NON-NLS-1$
							// only a warning, go on
						}						
					}
				}
				fJavaDocLocation= url;
			} catch (MalformedURLException e) {
				status.setError(PreferencesMessages.getString("JavadocConfigurationBlock.MalformedURL.error"));  //$NON-NLS-1$
				return status;
			}
		} 
		//else status.setWarning(PreferencesMessages.getString("JavadocConfigurationBlock.EmptyJavadocLocation.warning")); //$NON-NLS-1$
		return status;
	}


}

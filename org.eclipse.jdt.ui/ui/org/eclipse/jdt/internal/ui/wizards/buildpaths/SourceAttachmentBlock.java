/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipFile;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.ISelectionValidator;
import org.eclipse.jdt.internal.ui.dialogs.IStatusInfoChangeListener;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.TypedElementSelectionValidator;
import org.eclipse.jdt.internal.ui.preferences.ZipContentProvider;
import org.eclipse.jdt.internal.ui.preferences.ZipLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.ResourceFilter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;


public class SourceAttachmentBlock {
	
	private static final String FILENAME= "SourceAttachmentBlock.filename";
	private static final String PREFIX= "SourceAttachmentBlock.prefix";
	
	private static final String JAVADOC= "SourceAttachmentBlock.javadoc";
	
	private static final String ERR_FILENAME_NOTEXISTS= FILENAME + ".error.notexists";
	private static final String ERR_FILENAME_NOTVALID= FILENAME + ".error.notvalid";
	
	private static final String ERR_JDOCLOCATION_MALFORMED= JAVADOC + ".error.malformed";
	private static final String ERR_JDOCLOCATION_NOTAFOLDER= JAVADOC + ".error.notafolder";
	
	private static final String ERR_JDOCLOCATION_IDXNOTFOUND= JAVADOC + ".warning.filesnotfound";
	
	private static final String DIALOG_INTJARDIALOG= "SourceAttachmentBlock.intjardialog";
	private static final String DIALOG_PREFIXDIALOG= "SourceAttachmentBlock.prefixdialog";
	private static final String DIALOG_EXTJARDIALOG= "SourceAttachmentBlock.extjardialog";
	private static final String DIALOG_JDOCDIALOG= "SourceAttachmentBlock.jdocdialog";	
	
	private IStatusInfoChangeListener fContext;
	
	private StringButtonDialogField fFileNameField;
	private SelectionButtonDialogField fInternalButtonField;
	private StringButtonDialogField fPrefixField;
	
	private StringButtonDialogField fJavaDocField;
	
	private StatusInfo fNameStatusInfo, fJavaDocStatusInfo;
	
	private IPath fFileName;
	private IPath fPrefix;
	
	private URL fJavaDocLocation;
	
	private IProject fCurrProject;
	private IWorkspaceRoot fRoot;
	
	private Control fSWTWidget;
	
	
	public SourceAttachmentBlock(IProject project, IStatusInfoChangeListener context, IPath filename, IPath prefix, URL jdocLocation) {
		fContext= context;
		fCurrProject= project;
		fRoot= fCurrProject.getWorkspace().getRoot();
		
		SourceAttachmentAdapter adapter= new SourceAttachmentAdapter();
		
		fFileNameField= new StringButtonDialogField(adapter);
		fFileNameField.setDialogFieldListener(adapter);
		fFileNameField.setLabelText(JavaPlugin.getResourceString(FILENAME + ".label"));
		fFileNameField.setButtonLabel(JavaPlugin.getResourceString(FILENAME + ".external.button"));
		
		fInternalButtonField= new SelectionButtonDialogField(SWT.PUSH);
		fInternalButtonField.setDialogFieldListener(adapter);
		fInternalButtonField.setLabelText(JavaPlugin.getResourceString(FILENAME + ".internal.button"));
		
		fPrefixField= new StringButtonDialogField(adapter);
		fPrefixField.setDialogFieldListener(adapter);
		fPrefixField.setLabelText(JavaPlugin.getResourceString(PREFIX + ".label"));
		fPrefixField.setButtonLabel(JavaPlugin.getResourceString(PREFIX + ".button"));		

		fJavaDocField= new StringButtonDialogField(adapter);
		fJavaDocField.setDialogFieldListener(adapter);
		fJavaDocField.setLabelText(JavaPlugin.getResourceString(JAVADOC + ".label"));
		fJavaDocField.setButtonLabel(JavaPlugin.getResourceString(JAVADOC + ".button"));		
		
		fNameStatusInfo= new StatusInfo();
		fJavaDocStatusInfo= new StatusInfo();
		
		init(filename, prefix, jdocLocation);
	}
	
	/*
	 * Initializes the fields on the page
	 */
	private void init(IPath filename, IPath prefix, URL jdocLocation) {
		if (filename == null) {
			fFileNameField.setText("");
		} else {
			fFileNameField.setText(filename.toString());
		}
			
		if (prefix == null) {
			fPrefixField.setText("");
		} else {
			fPrefixField.setText(prefix.toString());
		}
		
		if (jdocLocation == null) {
			fJavaDocField.setText("");
		} else {
			fJavaDocField.setText(jdocLocation.toExternalForm());
		}			
	}
	
	/**
	 * Gets the source attachment path chosen by the user
	 */
	public IPath getSourceAttachmentPath() {
		return fFileName;
	}

	/**
	 * Gets the source attachment root chosen by the user
	 */
	public IPath getSourceAttachmentRootPath() {
		if (fFileName != null) {
			return fPrefix;
		} else {
			return null;
		}
	}
	
	/**
	 * Gets the Java Doc location chosen by the user
	 */
	public URL getJavaDocLocation() {
		return fJavaDocLocation;
	}	
	
	/**
	 * Creates the control
	 */
	public Control createControl(Composite parent) {
		fSWTWidget= parent;
		
		Composite composite= new Composite(parent, SWT.NONE);	
		
		MGridLayout layout= new MGridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.minimumWidth= 450;
		layout.minimumHeight= 0;
		layout.numColumns= 3;		
		composite.setLayout(layout);
		
		fFileNameField.doFillIntoGrid(composite, 3);
		DialogField.createEmptySpace(composite, 2);	
		fInternalButtonField.doFillIntoGrid(composite, 1);		
		
		DialogField.createEmptySpace(composite, 1);
		Label prefixDescription= new Label(composite, SWT.LEFT + SWT.WRAP);
		prefixDescription.setText(JavaPlugin.getResourceString(PREFIX + ".description"));
		DialogField.createEmptySpace(composite, 1);
		
		fPrefixField.doFillIntoGrid(composite, 3);
		fPrefixField.getTextControl(null).setEditable(false);

		DialogField.createEmptySpace(composite, 1);
		Label jdocDescription= new Label(composite, SWT.LEFT + SWT.WRAP);
		jdocDescription.setText(JavaPlugin.getResourceString(JAVADOC + ".description"));
		DialogField.createEmptySpace(composite, 1);
			
		fJavaDocField.doFillIntoGrid(composite, 3);
		
		fFileNameField.postSetFocusOnDialogField(parent.getDisplay());
		return composite;
	}

	private void doStatusLineUpdate() {
		StatusInfo status= fNameStatusInfo.getMoreSevere(fJavaDocStatusInfo);
		fContext.statusInfoChanged(status);
	}
		
		
	private class SourceAttachmentAdapter implements IStringButtonAdapter, IDialogFieldListener {
		
		// -------- IStringButtonAdapter --------
		
		public void changeControlPressed(DialogField field) {
			if (field == fFileNameField) {
				IPath jarFilePath= chooseExtJarFile(fFileNameField.getText());
				if (jarFilePath != null) {
					fFileName= jarFilePath;
					fFileNameField.setText(fFileName.toString());
				}
			} else if (field == fPrefixField) {
				IPath prefixPath= choosePrefix(fPrefixField.getText());
				if (prefixPath != null) {
					fPrefix= prefixPath;
					fPrefixField.setText(fPrefix.toString());
				}
			} else if (field == fJavaDocField) {
				URL jdocURL= chooseJavaDocLocation();
				if (jdocURL != null) {
					fJavaDocLocation= jdocURL;
					fJavaDocField.setText(fJavaDocLocation.toExternalForm());
				}
			}			
		}
		
		// ---------- IDialogFieldListener --------
	
		public void dialogFieldChanged(DialogField field) {
			if (field == fFileNameField) {
				updateFileName();
				updatePrefixEnableState();
				doStatusLineUpdate();
			} else if (field == fPrefixField) {
				fPrefix= new Path(fPrefixField.getText());
			} else if (field == fInternalButtonField) {
				IPath jarFilePath= chooseInternalJarFile(fFileNameField.getText());
				if (jarFilePath != null) {
					fFileName= jarFilePath;
					fFileNameField.setText(fFileName.toString());
				}
			} else if (field == fJavaDocField) {
				updateJavaDocLocation();
				doStatusLineUpdate();
			}
		}
	}
	
	private void updateFileName() {
		fNameStatusInfo.setOK();
		String fileName= fFileNameField.getText();
		if ("".equals(fileName)) {
			fFileName= null;
			fPrefixField.setText("");
			return;
		} else {
			fFileName= null;
			IWorkspace ws= fCurrProject.getWorkspace();
			if (!Path.EMPTY.isValidPath(fileName)) {
				fNameStatusInfo.setError(JavaPlugin.getResourceString(ERR_FILENAME_NOTVALID));
				return;
			}
			IPath fileNamePath= new Path(fileName);
			File file= findFile(fileNamePath);
			if (file == null) {
				fNameStatusInfo.setError(JavaPlugin.getResourceString(ERR_FILENAME_NOTEXISTS));
				return;
			}
			fFileName= fileNamePath;
		}
	}
	
	private void updatePrefixEnableState() {
		fPrefixField.enableButton(fFileName != null);
	}
	
	
	private void updateJavaDocLocation() {
		fJavaDocStatusInfo.setOK();
		fJavaDocLocation= null;
		String jdocLocation= fJavaDocField.getText();
		if (!"".equals(jdocLocation)) {
			try {
				URL url= new URL(jdocLocation);
				if ("file".equals(url.getProtocol())) {
					File dir= new File(url.getFile());
					if (!dir.isDirectory()) {
						fJavaDocStatusInfo.setError(JavaPlugin.getResourceString(ERR_JDOCLOCATION_NOTAFOLDER));
						return;
					}
					/*else {
						File indexFile= new File(dir, "index.html");
						File packagesFile= new File(dir, "package-list");
						if (!packagesFile.exists() || !indexFile.exists()) {
							fJavaDocStatusInfo.setWarning(JavaPlugin.getResourceString(ERR_JDOCLOCATION_IDXNOTFOUND));
							// only a warning, go on
						}
					}*/	
				}
				fJavaDocLocation= url;
			} catch (MalformedURLException e) {
				fJavaDocStatusInfo.setError(JavaPlugin.getFormattedString(ERR_JDOCLOCATION_MALFORMED, e.getLocalizedMessage()));
				return;
			}
		}
	}
	
	/*
	 * Open a dialog to choose a jar from the file system
	 */
	private IPath chooseExtJarFile(String initPath) {
		FileDialog dialog= new FileDialog(getShell());
		dialog.setText(JavaPlugin.getResourceString(DIALOG_EXTJARDIALOG + ".text"));
		dialog.setFilterExtensions(new String[] {"*.jar;*.zip"});
		dialog.setFilterPath(initPath);
		String res= dialog.open();
		if (res != null) {
			return new Path(res);
		}
		return null;
	}

	/*
	 * Open a dialog to choose an internal jar
	 */	
	private IPath chooseInternalJarFile(String initSelection) {
		Class[] acceptedClasses= new Class[] { IFile.class };
		ISelectionValidator validator= new TypedElementSelectionValidator(acceptedClasses, false);
	
		ViewerFilter filter= new ResourceFilter(new String[] { ".jar", ".zip" }, null);

		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new WorkbenchContentProvider();

		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), lp, cp, false, true);
		dialog.setValidator(validator);
		dialog.addFilter(filter);
		dialog.setTitle(JavaPlugin.getResourceString(DIALOG_INTJARDIALOG + ".title"));
		dialog.setMessage(JavaPlugin.getResourceString(DIALOG_INTJARDIALOG + ".message"));
		
		IWorkspaceRoot root= fCurrProject.getWorkspace().getRoot();
		IResource initSel= root.findMember(new Path(initSelection));
		if (initSel == null) {
			initSel= fCurrProject;
		}
		if (dialog.open(root, initSel) == dialog.OK) {
			IFile file= (IFile) dialog.getPrimaryResult();
			return file.getFullPath();
		}
		return null;
	}

	/*
	 * Open a dialog to choose path in a zip file
	 */		
	private IPath choosePrefix(String initSelection) {
		if (fFileName == null) {
			return null;
		}
		File file= findFile(fFileName);
		if (file != null) {
			try {
				ZipFile zipFile= new ZipFile(file);			
				ZipContentProvider contentProvider= new ZipContentProvider(initSelection, zipFile);
				ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), new ZipLabelProvider(), contentProvider, false, true); 
				dialog.setTitle(JavaPlugin.getResourceString(DIALOG_PREFIXDIALOG + ".title"));
				dialog.setMessage(JavaPlugin.getResourceString(DIALOG_PREFIXDIALOG + ".message"));
				if (dialog.open(zipFile, contentProvider.getSelectedNode()) == dialog.OK) {
					Object obj= dialog.getPrimaryResult();
					return new Path(obj.toString());
				}
			} catch (IOException e) {
				MessageDialog.openError(getShell(), "Error", "IOException when opening ZipFile " + file.getPath());
			}				
		
		}
		return null;
	}
	
	/*
	 * Open a dialog to choose a root in the file system
	 */	
	private URL chooseJavaDocLocation() {
		String initPath= "";
		if (fJavaDocLocation != null && "file".equals(fJavaDocLocation.getProtocol())) {
			initPath= (new File(fJavaDocLocation.getFile())).getPath();
		}
		DirectoryDialog dialog= new DirectoryDialog(getShell());
		dialog.setText(JavaPlugin.getResourceString(DIALOG_JDOCDIALOG + ".text"));
		dialog.setFilterPath(initPath);
		String res= dialog.open();
		if (res != null) {
			try {
				return (new File(res)).toURL();
			} catch (MalformedURLException e) {
				// should not happen
				JavaPlugin.getDefault().log(e);
			}
		}
		return null;		
		
	}
		
	private Shell getShell() {
		if (fSWTWidget != null) {
			return fSWTWidget.getShell();
		}
		return JavaPlugin.getActiveWorkbenchShell();			
	}
	
	/*
	 * Finds the file externally or internally
	 * Returns null if no file found
	 */
	private File findFile(IPath fileName) {
		File file= fileName.toFile();
		if (file.isFile()) {
			return file;
		}
		IResource res= fRoot.findMember(fileName);
		if (res instanceof IFile) {
			return res.getLocation().toFile();
		}
		return null;
	}
	
}
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.io.File;import java.io.IOException;import java.net.MalformedURLException;import java.net.URL;import java.util.ArrayList;import java.util.zip.ZipFile;import org.eclipse.swt.SWT;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.DirectoryDialog;import org.eclipse.swt.widgets.FileDialog;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Shell;import org.eclipse.core.resources.IFile;import org.eclipse.core.resources.IProject;import org.eclipse.core.resources.IResource;import org.eclipse.core.resources.IWorkspace;import org.eclipse.core.resources.IWorkspaceRoot;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Path;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.jface.viewers.ILabelProvider;import org.eclipse.jface.viewers.ITreeContentProvider;import org.eclipse.jface.viewers.ViewerFilter;import org.eclipse.ui.model.WorkbenchContentProvider;import org.eclipse.ui.model.WorkbenchLabelProvider;import org.eclipse.jdt.core.IClasspathEntry;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.dialogs.ElementTreeSelectionDialog;import org.eclipse.jdt.internal.ui.dialogs.ISelectionValidator;import org.eclipse.jdt.internal.ui.dialogs.IStatusChangeListener;import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;import org.eclipse.jdt.internal.ui.dialogs.StatusTool;import org.eclipse.jdt.internal.ui.dialogs.TypedElementSelectionValidator;import org.eclipse.jdt.internal.ui.preferences.ZipContentProvider;import org.eclipse.jdt.internal.ui.preferences.ZipLabelProvider;import org.eclipse.jdt.internal.ui.viewsupport.ResourceFilter;import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;


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
	
	private IStatusChangeListener fContext;
	
	private StringButtonDialogField fFileNameField;
	private SelectionButtonDialogField fInternalButtonField;
	
	private boolean fIsVariableEntry;
	private VariableSelectionBlock fVariableSelectionBlock;
	
	private IStatus fNameStatus;
	private IStatus fNameExistsStatus;
	private IStatus fJavaDocStatus;
	
	private StringButtonDialogField fPrefixVarField;
	
	private StringButtonDialogField fPrefixField;
	private StringButtonDialogField fJavaDocField;
		
	private IPath fJARPath;
	
	private IPath fFileName;
	private IPath fPrefix;
	private URL fJavaDocLocation;
	
	private IProject fCurrProject;
	private IWorkspaceRoot fRoot;
	
	private Control fSWTWidget;
	
	public SourceAttachmentBlock(IProject project, IStatusChangeListener context, IClasspathEntry oldEntry) {
		fContext= context;
		fCurrProject= project;
				
		fRoot= fCurrProject.getWorkspace().getRoot();		
		fIsVariableEntry= (oldEntry.getEntryKind() == IClasspathEntry.CPE_VARIABLE);
		
		fNameStatus= new StatusInfo();
		fNameExistsStatus= new StatusInfo();
		fJavaDocStatus= new StatusInfo();		
		
		IPath attachPath= (oldEntry != null) ? oldEntry.getSourceAttachmentPath() : new Path("");
		IPath attachRoot= (oldEntry != null) ? oldEntry.getSourceAttachmentRootPath() : new Path("");
		fJARPath= (oldEntry != null) ? oldEntry.getPath() : new Path("");
		
		SourceAttachmentAdapter adapter= new SourceAttachmentAdapter();

		fPrefixField= new StringButtonDialogField(adapter);
		fPrefixField.setDialogFieldListener(adapter);
		fPrefixField.setLabelText(JavaPlugin.getResourceString(PREFIX + ".label"));
		fPrefixField.setButtonLabel(JavaPlugin.getResourceString(PREFIX + ".button"));		

		fJavaDocField= new StringButtonDialogField(adapter);
		fJavaDocField.setDialogFieldListener(adapter);
		fJavaDocField.setLabelText(JavaPlugin.getResourceString(JAVADOC + ".label"));
		fJavaDocField.setButtonLabel(JavaPlugin.getResourceString(JAVADOC + ".button"));		
		
		if (fIsVariableEntry) {
			IStatusChangeListener listener= new IStatusChangeListener() {
				public void statusChanged(IStatus status) {
					variableUpdated(status);
				}
			};
			String initSelection= (oldEntry != null) ? oldEntry.getPath().segment(0) : null;
			fVariableSelectionBlock= new VariableSelectionBlock(listener, new ArrayList(), attachPath, initSelection, true);
			variableUpdated(fNameStatus);
			
			fPrefixVarField= new StringButtonDialogField(adapter); 
			
		} else {		
			fFileNameField= new StringButtonDialogField(adapter);
			fFileNameField.setDialogFieldListener(adapter);
			fFileNameField.setLabelText(JavaPlugin.getResourceString(FILENAME + ".label"));
			fFileNameField.setButtonLabel(JavaPlugin.getResourceString(FILENAME + ".external.button"));
		
			fInternalButtonField= new SelectionButtonDialogField(SWT.PUSH);
			fInternalButtonField.setDialogFieldListener(adapter);
			fInternalButtonField.setLabelText(JavaPlugin.getResourceString(FILENAME + ".internal.button"));
		
			if (attachPath != null) {
				fFileNameField.setText(attachPath.toString());
			} else {
				fFileNameField.setText("");
			}
		}
	
		if (attachRoot != null) {
			fPrefixField.setText(attachRoot.toString());
		} else {
			fPrefixField.setText("");
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
	
	///**
	// * Gets the Java Doc location chosen by the user
	// */
	//public URL getJavaDocLocation() {
	//	return fJavaDocLocation;
	//}	
	

	
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
		
		if (fVariableSelectionBlock != null) {
			fVariableSelectionBlock.doFillIntoGrid(composite, 3);
			fVariableSelectionBlock.setFocus(parent.getDisplay());
		} else {
			fFileNameField.doFillIntoGrid(composite, 3);
			DialogField.createEmptySpace(composite, 2);	
			fInternalButtonField.doFillIntoGrid(composite, 1);
			fFileNameField.postSetFocusOnDialogField(parent.getDisplay());
		}		
		
		DialogField.createEmptySpace(composite, 1);
		Label prefixDescription= new Label(composite, SWT.LEFT + SWT.WRAP);
		prefixDescription.setText(JavaPlugin.getResourceString(PREFIX + ".description"));
		DialogField.createEmptySpace(composite, 1);
		
		fPrefixField.doFillIntoGrid(composite, 3);

		//DialogField.createEmptySpace(composite, 1);
		//Label jdocDescription= new Label(composite, SWT.LEFT + SWT.WRAP);
		//jdocDescription.setText(JavaPlugin.getResourceString(JAVADOC + ".description"));
		//DialogField.createEmptySpace(composite, 1);
			
		//fJavaDocField.doFillIntoGrid(composite, 3);
		return composite;
	}
	
		
	private class SourceAttachmentAdapter implements IStringButtonAdapter, IDialogFieldListener {
		
		// -------- IStringButtonAdapter --------
		
		public void changeControlPressed(DialogField field) {
			if (field == fFileNameField) {
				IPath jarFilePath= chooseExtJarFile();
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
				fNameStatus= updateFileNameStatus();
				fNameExistsStatus= updateFileExistsStatus();
				doStatusLineUpdate();
			} else if (field == fInternalButtonField) {
				IPath jarFilePath= chooseInternalJarFile(fFileNameField.getText());
				if (jarFilePath != null) {
					fFileName= jarFilePath;
					fFileNameField.setText(fFileName.toString());
				}
			} else if (field == fPrefixField) {
				fPrefix= new Path(fPrefixField.getText());
			} else if (field == fJavaDocField) {
				fJavaDocStatus= updateJavaDocLocationStatus();
				doStatusLineUpdate();
			}
		}
	}

	private IPath getResolvedPath() {
		if (fFileName != null) {
			if (fIsVariableEntry) {
				return fVariableSelectionBlock.getResolvedPath();
			} else {
				return fFileName;
			}
		}
		return null;
	}
	
	private void doStatusLineUpdate() {
		IStatus status= StatusTool.getMostSevere(new IStatus[] { fNameStatus, fNameExistsStatus, fJavaDocStatus });
		fContext.statusChanged(status);
	}		
	
	private void variableUpdated(IStatus varStatus) {
		fNameStatus= varStatus;
		if (fVariableSelectionBlock != null) {
			if (fNameStatus.isOK()) {
				fFileName= fVariableSelectionBlock.getVariablePath();
				fNameExistsStatus= updateFileExistsStatus();
				doStatusLineUpdate();
			} else {
				fFileName= null;
			}
		}
	}
	
	private IStatus updateFileNameStatus() {
		StatusInfo status= new StatusInfo();
		fFileName= null;
		String fileName= fFileNameField.getText();
		if ("".equals(fileName)) {
			return status;
		} else {
			IWorkspace ws= fCurrProject.getWorkspace();
			if (!Path.EMPTY.isValidPath(fileName)) {
				status.setError(JavaPlugin.getResourceString(ERR_FILENAME_NOTVALID));
				return status;
			}
			fFileName= new Path(fileName);
		}
		return status;
	}
	
	private IStatus updateFileExistsStatus() {
		StatusInfo status= new StatusInfo();
		IWorkspace ws= fCurrProject.getWorkspace();
		IPath fileNamePath= getResolvedPath();
		if (fileNamePath != null) {
			File file= findFile(fileNamePath);
			if (file == null) {
				status.setError(JavaPlugin.getResourceString(ERR_FILENAME_NOTEXISTS));
			}
		}
		fPrefixField.enableButton(status.isOK() && fileNamePath != null);
		return status;
	}	
	
	
	private IStatus updateJavaDocLocationStatus() {
		StatusInfo status= new StatusInfo();
		fJavaDocLocation= null;
		String jdocLocation= fJavaDocField.getText();
		if (!"".equals(jdocLocation)) {
			try {
				URL url= new URL(jdocLocation);
				if ("file".equals(url.getProtocol())) {
					File dir= new File(url.getFile());
					if (!dir.isDirectory()) {
						status.setError(JavaPlugin.getResourceString(ERR_JDOCLOCATION_NOTAFOLDER));
						return status;
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
				status.setError(JavaPlugin.getFormattedString(ERR_JDOCLOCATION_MALFORMED, e.getLocalizedMessage()));
				return status;
			}
		}
		return status;
	}
	
	/*
	 * Open a dialog to choose a jar from the file system
	 */
	private IPath chooseExtJarFile() {
		IPath prevPath= new Path(fFileNameField.getText());
		if (prevPath.isEmpty()) {
			prevPath= fJARPath;
		}
		String initPath= prevPath.removeLastSegments(1).toOSString();
		
		FileDialog dialog= new FileDialog(getShell());
		dialog.setText(JavaPlugin.getResourceString(DIALOG_EXTJARDIALOG + ".text"));
		dialog.setFilterExtensions(new String[] {"*.jar;*.zip"});
		dialog.setFilterPath(initPath);
		String res= dialog.open();
		if (res != null) {
			return new Path(res).makeAbsolute();
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
			initSel= root.findMember(fJARPath);
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
		if (fIsVariableEntry) {
			return chooseVariablePrefix(initSelection);
		}
		
		IPath resolvedPath= getResolvedPath();
		if (resolvedPath == null) {
			return null;
		}
		File file= findFile(resolvedPath);
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
	
	private IPath chooseVariablePrefix(String initSelection) {
		ChooseVariableDialog dialog= new ChooseVariableDialog(getShell(), initSelection);
		if (dialog.open() == dialog.OK) {
			return new Path(dialog.getSelectedVariable());
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
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.io.File;import java.io.IOException;import java.net.MalformedURLException;import java.net.URL;import java.util.zip.ZipFile;import org.eclipse.swt.SWT;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.DirectoryDialog;import org.eclipse.swt.widgets.FileDialog;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Shell;import org.eclipse.core.resources.IFile;import org.eclipse.core.resources.IProject;import org.eclipse.core.resources.IResource;import org.eclipse.core.resources.IWorkspaceRoot;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Path;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.jface.viewers.ILabelProvider;import org.eclipse.jface.viewers.ITreeContentProvider;import org.eclipse.jface.viewers.ViewerFilter;import org.eclipse.ui.help.DialogPageContextComputer;import org.eclipse.ui.help.WorkbenchHelp;import org.eclipse.ui.model.WorkbenchContentProvider;import org.eclipse.ui.model.WorkbenchLabelProvider;import org.eclipse.jdt.core.IClasspathEntry;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.dialogs.ElementTreeSelectionDialog;import org.eclipse.jdt.internal.ui.dialogs.ISelectionValidator;import org.eclipse.jdt.internal.ui.dialogs.IStatusChangeListener;import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;import org.eclipse.jdt.internal.ui.dialogs.StatusTool;import org.eclipse.jdt.internal.ui.dialogs.TypedElementSelectionValidator;import org.eclipse.jdt.internal.ui.preferences.ZipContentProvider;import org.eclipse.jdt.internal.ui.preferences.ZipLabelProvider;import org.eclipse.jdt.internal.ui.viewsupport.ResourceFilter;import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;import org.eclipse.jdt.internal.ui.wizards.swt.MGridData;import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;


public class SourceAttachmentBlock {
	
	private static final String PAGE_NAME= "SourceAttachmentBlock";
	
	private static final String FILENAME= PAGE_NAME + ".filename";
	private static final String PREFIX= PAGE_NAME + ".prefix";
	private static final String JAVADOC= PAGE_NAME + ".javadoc";
		
	private static final String ERR_FILENAME_NOTVALID= FILENAME + ".error.notvalid";
	private static final String ERR_FILENAME_DEVICEINPATH= FILENAME + ".error.deviceinpath";
	private static final String ERR_FILENAME_VARIABLENOTEXISTS= FILENAME + ".error.varnotexists";
	private static final String ERR_FILENAME_NOTEXISTS= FILENAME + ".error.filenotexists";

	private static final String ERR_PREFIX_NOTVALID= PREFIX + ".error.notvalid";
	private static final String ERR_PREFIX_DEVICEINPATH= PREFIX + ".error.deviceinpath";
	private static final String ERR_PREFIX_VARIABLENOTEXISTS= PREFIX + ".error.varnotexists";
	
	private static final String ERR_JDOCLOCATION_MALFORMED= JAVADOC + ".error.malformed";
	private static final String ERR_JDOCLOCATION_NOTAFOLDER= JAVADOC + ".error.notafolder";
	
	private static final String ERR_JDOCLOCATION_IDXNOTFOUND= JAVADOC + ".warning.filesnotfound";
	
	private static final String DIALOG_INTJARDIALOG= PAGE_NAME + ".intjardialog";
	private static final String DIALOG_PREFIXDIALOG= PAGE_NAME + ".prefixdialog";
	private static final String DIALOG_EXTJARDIALOG= PAGE_NAME + ".extjardialog";
	private static final String DIALOG_JDOCDIALOG= PAGE_NAME + ".jdocdialog";	
	
	private IStatusChangeListener fContext;
	
	private StringButtonDialogField fFileNameField;
	private SelectionButtonDialogField fInternalButtonField;
	
	private StringButtonDialogField fPrefixField;
	private StringButtonDialogField fJavaDocField;
	
	private boolean fIsVariableEntry;
	
	private IStatus fNameStatus;
	private IStatus fPrefixStatus;
	private IStatus fJavaDocStatus;
		
	private IPath fJARPath;
	
	private IPath fFileName;
	private File fResolvedFile;
	private IPath fPrefix;
	private URL fJavaDocLocation;
	
	private String fFilePathVariable;
	private String fPrefixVariable;
	
	private IProject fCurrProject;
	private IWorkspaceRoot fRoot;
	
	private Control fSWTWidget;
	private Label fFullPathResolvedLabel;
	private Label fPrefixResolvedLabel;
	
	public SourceAttachmentBlock(IProject project, IStatusChangeListener context, IClasspathEntry oldEntry) {
		fContext= context;
		fCurrProject= project;
				
		fRoot= fCurrProject.getWorkspace().getRoot();		
		fIsVariableEntry= (oldEntry.getEntryKind() == IClasspathEntry.CPE_VARIABLE);
		
		fNameStatus= new StatusInfo();
		fPrefixStatus= new StatusInfo();
		fJavaDocStatus= new StatusInfo();		
		
		fJARPath= (oldEntry != null) ? oldEntry.getPath() : new Path("");
		
		SourceAttachmentAdapter adapter= new SourceAttachmentAdapter();
		
		if (fIsVariableEntry) {
			fFileNameField= new VariablePathDialogField(adapter);
			fFileNameField.setDialogFieldListener(adapter);
			fFileNameField.setLabelText(JavaPlugin.getResourceString(FILENAME + ".varlabel"));
			fFileNameField.setButtonLabel(JavaPlugin.getResourceString(FILENAME + ".external.varbutton"));
			((VariablePathDialogField)fFileNameField).setVariableButtonLabel(JavaPlugin.getResourceString(FILENAME + ".variable.button"));
		
			fPrefixField= new VariablePathDialogField(adapter);
			fPrefixField.setDialogFieldListener(adapter);
			fPrefixField.setLabelText(JavaPlugin.getResourceString(PREFIX + ".varlabel"));
			fPrefixField.setButtonLabel(JavaPlugin.getResourceString(PREFIX + ".varbutton"));			
			((VariablePathDialogField)fPrefixField).setVariableButtonLabel(JavaPlugin.getResourceString(PREFIX + ".variable.button"));
			
		} else {
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
		}	
			
		fJavaDocField= new StringButtonDialogField(adapter);
		fJavaDocField.setDialogFieldListener(adapter);
		fJavaDocField.setLabelText(JavaPlugin.getResourceString(JAVADOC + ".label"));
		fJavaDocField.setButtonLabel(JavaPlugin.getResourceString(JAVADOC + ".button"));		
			
		if (oldEntry != null && oldEntry.getSourceAttachmentPath() != null) {
			fFileNameField.setText(oldEntry.getSourceAttachmentPath().toString());
		} else {
			fFileNameField.setText("");
		}	
				
		if (oldEntry != null && oldEntry.getSourceAttachmentRootPath() != null) {
			fPrefixField.setText(oldEntry.getSourceAttachmentRootPath().toString());
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
	

	private Label createHelpText(Composite composite, int style, String label, String text) {
		if (label == null) {
			DialogField.createEmptySpace(composite, 1);
		} else {
			Label labelControl= new Label(composite, SWT.LEFT);
			labelControl.setText(label);
		}
			
		Label helpTextLabel= new Label(composite, style);
		helpTextLabel.setText(text);
		MGridData gd= new MGridData(MGridData.HORIZONTAL_ALIGN_FILL);
		//gd.horizontalSpan= 2;
		helpTextLabel.setLayoutData(gd);
		DialogField.createEmptySpace(composite, 2);
		return helpTextLabel;
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
		layout.numColumns= 4;		
		composite.setLayout(layout);
		
		if (fIsVariableEntry) {
			createHelpText(composite, SWT.LEFT + SWT.WRAP, null, JavaPlugin.getResourceString(FILENAME + ".description"));
		}			
		
		fFileNameField.doFillIntoGrid(composite, 4);
		if (!fIsVariableEntry) {
			DialogField.createEmptySpace(composite, 3);	
			fInternalButtonField.doFillIntoGrid(composite, 1);
		} else {
			String label= JavaPlugin.getResourceString(FILENAME + ".resolvedpath");
			fFullPathResolvedLabel= createHelpText(composite, SWT.LEFT, null, getResolvedLabelString(fFileName));
		}
		
		// label
		createHelpText(composite, SWT.LEFT + SWT.WRAP, null, JavaPlugin.getResourceString(PREFIX + ".description"));
	
		fPrefixField.doFillIntoGrid(composite, 4);
		if (fIsVariableEntry) {
			String label= JavaPlugin.getResourceString(PREFIX + ".resolvedpath");
			fPrefixResolvedLabel= createHelpText(composite, SWT.LEFT, null, getResolvedLabelString(fPrefix));
		}
		
		fFileNameField.postSetFocusOnDialogField(parent.getDisplay());
		
		//DialogField.createEmptySpace(composite, 1);
		//Label jdocDescription= new Label(composite, SWT.LEFT + SWT.WRAP);
		//jdocDescription.setText(JavaPlugin.getResourceString(JAVADOC + ".description"));
		//DialogField.createEmptySpace(composite, 1);
			
		//fJavaDocField.doFillIntoGrid(composite, 3);
		
		WorkbenchHelp.setHelp(composite, new Object[] { IJavaHelpContextIds.SOURCE_ATTACHMENT_BLOCK });
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
				IPath prefixPath= choosePrefix();
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
			} else if (field == fInternalButtonField) {
				IPath jarFilePath= chooseInternalJarFile(fFileNameField.getText());
				if (jarFilePath != null) {
					fFileName= jarFilePath;
					fFileNameField.setText(fFileName.toString());
				}
				return;
			} else if (field == fPrefixField) {
				fPrefixStatus= updatePrefixStatus();
			} else if (field == fJavaDocField) {
				fJavaDocStatus= updateJavaDocLocationStatus();
			}
			doStatusLineUpdate();
		}
	}
		
	private void doStatusLineUpdate() {
		boolean canBrowseFileName= !fIsVariableEntry || (fResolvedFile == null && fFilePathVariable != null);
		boolean canBrowsePrefix= (fResolvedFile != null) && (!fIsVariableEntry ||  fPrefixVariable != null);
	
		fPrefixField.enableButton(canBrowsePrefix);
		fFileNameField.enableButton(canBrowseFileName);

		if (fPrefixResolvedLabel != null) {
			fPrefixResolvedLabel.setText(getResolvedLabelString(fPrefix));
		}
		if (fFullPathResolvedLabel != null) {
			fFullPathResolvedLabel.setText(getResolvedLabelString(fFileName));
		}			
		
		IStatus status= StatusTool.getMostSevere(new IStatus[] { fNameStatus, fPrefixStatus, fJavaDocStatus });
		fContext.statusChanged(status);
	}
	
	private String getResolvedLabelString(IPath path) {
		IPath resolvedPath= getResolvedPath(path);
		if (resolvedPath != null) {
			return resolvedPath.toString();
		}
		return "";
	}	
	
	private IPath getResolvedPath(IPath path) {
		if (path != null) {
			String varName= path.segment(0);
			if (varName != null) {
				IPath varPath= JavaCore.getClasspathVariable(varName);
				if (varPath != null) {
					return varPath.append(path.removeFirstSegments(1));
				}
			}
		}
		return null;
	}
		
	private IStatus updatePrefixStatus() {
		StatusInfo status= new StatusInfo();
		fPrefix= null;
		fPrefixVariable= null;
		
		String prefix= fPrefixField.getText();
		if (prefix.length() == 0) {
			return status;
		} else {
			if (!Path.EMPTY.isValidPath(prefix)) {
				status.setError(JavaPlugin.getResourceString(ERR_PREFIX_NOTVALID));
				return status;
			}
			IPath path= new Path(prefix);
			if (path.getDevice() != null) {
				status.setError(JavaPlugin.getResourceString(ERR_PREFIX_DEVICEINPATH));
				return status;
			} 			
			if (fIsVariableEntry) {
				IPath resolvedPath= getResolvedPath(path);
				if (path == null) {
					status.setError(JavaPlugin.getResourceString(ERR_PREFIX_VARIABLENOTEXISTS));
					return status;
				}
				fPrefixVariable= path.segment(0);
			}
			fPrefix= path;
		}
		return status;
	}
	
	private IStatus updateFileNameStatus() {
		StatusInfo status= new StatusInfo();
		fFileName= null;
		fResolvedFile= null;
		fFilePathVariable= null;
		
		String fileName= fFileNameField.getText();
		if (fileName.length() == 0) {
			return status;
		} else {
			if (!Path.EMPTY.isValidPath(fileName)) {
				status.setError(JavaPlugin.getResourceString(ERR_FILENAME_NOTVALID));
				return status;
			}
			IPath filePath= new Path(fileName);
			IPath resolvedPath;
			if (fIsVariableEntry) {
				if (filePath.getDevice() != null) {
					status.setError(JavaPlugin.getResourceString(ERR_FILENAME_DEVICEINPATH));
					return status;
				} 
				resolvedPath= getResolvedPath(filePath);
				if (resolvedPath == null) {
					status.setError(JavaPlugin.getResourceString(ERR_FILENAME_VARIABLENOTEXISTS));
					return status;
				}
				if (fFullPathResolvedLabel != null) {
					fFullPathResolvedLabel.setText(resolvedPath.toString());
				}				
				fFilePathVariable= filePath.segment(0);		
			} else {
				resolvedPath= filePath;
			}
			fFileName= filePath;
			
			File resolvedFile= findFile(resolvedPath);
			if (resolvedFile == null) {
				status.setError(JavaPlugin.getResourceString(ERR_FILENAME_NOTEXISTS));
				return status;
			}
			
			fResolvedFile= resolvedFile;
			
		}
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
		if (fIsVariableEntry) {
			prevPath= getResolvedPath(prevPath);
		}
		String ext= prevPath.getFileExtension();
		if ("jar".equals(ext) || "zip".equals(ext)) {
			prevPath= prevPath.removeLastSegments(1);
		}
	
		FileDialog dialog= new FileDialog(getShell());
		dialog.setText(JavaPlugin.getResourceString(DIALOG_EXTJARDIALOG + ".text"));
		dialog.setFilterExtensions(new String[] {"*.jar;*.zip"});
		dialog.setFilterPath(prevPath.toOSString());
		String res= dialog.open();
		if (res != null) {
			IPath returnPath= new Path(res).makeAbsolute();
			if (fIsVariableEntry) {
				returnPath= modifyPath(returnPath, fFilePathVariable);
			}
			return returnPath;
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
	private IPath choosePrefix() {
		if (fResolvedFile != null) {
			IPath currPath= new Path(fPrefixField.getText());
			String initSelection= null;
			if (fIsVariableEntry) {
				IPath resolvedPath= getResolvedPath(currPath);
				if (resolvedPath != null) {
					initSelection= resolvedPath.toString();
				}
			} else {
				initSelection= currPath.toString();
			}
			try {
				ZipFile zipFile= new ZipFile(fResolvedFile);			
				ZipContentProvider contentProvider= new ZipContentProvider(initSelection, zipFile);
				ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), new ZipLabelProvider(), contentProvider, false, true); 
				dialog.setTitle(JavaPlugin.getResourceString(DIALOG_PREFIXDIALOG + ".title"));
				dialog.setMessage(JavaPlugin.getResourceString(DIALOG_PREFIXDIALOG + ".message"));
				if (dialog.open(zipFile, contentProvider.getSelectedNode()) == dialog.OK) {
					Object obj= dialog.getPrimaryResult();
					IPath path= new Path(obj.toString());
					if (fIsVariableEntry) {
						path= modifyPath(path, fPrefixVariable);
					}
					return path;
				}
			} catch (IOException e) {
				String title= JavaPlugin.getResourceString(DIALOG_PREFIXDIALOG + ".error.title");
				String message= JavaPlugin.getFormattedString(DIALOG_PREFIXDIALOG + ".error.message", fResolvedFile.getPath());
				MessageDialog.openError(getShell(), title, message);
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
	
	private IPath modifyPath(IPath path, String varName) {
		IPath varPath= JavaCore.getClasspathVariable(varName);
		if (varPath != null) {
			if (varPath.isPrefixOf(path)) {
				path= path.removeFirstSegments(varPath.segmentCount());
			} else {
				path= new Path(path.lastSegment());
			}
		} else {
			path= new Path(path.lastSegment());
		}
		return new Path(varName).append(path);
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
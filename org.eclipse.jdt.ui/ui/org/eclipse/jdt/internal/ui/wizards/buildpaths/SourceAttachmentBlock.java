/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.zip.ZipFile;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.ISelectionValidator;
import org.eclipse.jdt.internal.ui.dialogs.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusTool;
import org.eclipse.jdt.internal.ui.dialogs.TypedElementSelectionValidator;
import org.eclipse.jdt.internal.ui.preferences.ZipContentProvider;
import org.eclipse.jdt.internal.ui.preferences.ZipLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.ResourceFilter;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridData;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;


public class SourceAttachmentBlock {
	
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
	
	private IWorkspaceRoot fRoot;
	
	private Control fSWTWidget;
	private CLabel fFullPathResolvedLabel;
	private CLabel fPrefixResolvedLabel;
	
	public SourceAttachmentBlock(IWorkspaceRoot root, IStatusChangeListener context, IClasspathEntry oldEntry) {
		fContext= context;
				
		fRoot= root;		
		fIsVariableEntry= (oldEntry.getEntryKind() == IClasspathEntry.CPE_VARIABLE);
		
		fNameStatus= new StatusInfo();
		fPrefixStatus= new StatusInfo();
		fJavaDocStatus= new StatusInfo();		
		
		fJARPath= (oldEntry != null) ? oldEntry.getPath() : Path.EMPTY;
		
		SourceAttachmentAdapter adapter= new SourceAttachmentAdapter();
		
		if (fIsVariableEntry) {
			fFileNameField= new VariablePathDialogField(adapter);
			fFileNameField.setDialogFieldListener(adapter);
			fFileNameField.setLabelText(NewWizardMessages.getString("SourceAttachmentBlock.filename.varlabel")); //$NON-NLS-1$
			fFileNameField.setButtonLabel(NewWizardMessages.getString("SourceAttachmentBlock.filename.external.varbutton")); //$NON-NLS-1$
			((VariablePathDialogField)fFileNameField).setVariableButtonLabel(NewWizardMessages.getString("SourceAttachmentBlock.filename.variable.button")); //$NON-NLS-1$
		
			fPrefixField= new VariablePathDialogField(adapter);
			fPrefixField.setDialogFieldListener(adapter);
			fPrefixField.setLabelText(NewWizardMessages.getString("SourceAttachmentBlock.prefix.varlabel")); //$NON-NLS-1$
			fPrefixField.setButtonLabel(NewWizardMessages.getString("SourceAttachmentBlock.prefix.varbutton"));			 //$NON-NLS-1$
			((VariablePathDialogField)fPrefixField).setVariableButtonLabel(NewWizardMessages.getString("SourceAttachmentBlock.prefix.variable.button")); //$NON-NLS-1$
			
		} else {
			fFileNameField= new StringButtonDialogField(adapter);
			fFileNameField.setDialogFieldListener(adapter);
			fFileNameField.setLabelText(NewWizardMessages.getString("SourceAttachmentBlock.filename.label")); //$NON-NLS-1$
			fFileNameField.setButtonLabel(NewWizardMessages.getString("SourceAttachmentBlock.filename.external.button")); //$NON-NLS-1$
		
			fInternalButtonField= new SelectionButtonDialogField(SWT.PUSH);
			fInternalButtonField.setDialogFieldListener(adapter);
			fInternalButtonField.setLabelText(NewWizardMessages.getString("SourceAttachmentBlock.filename.internal.button")); //$NON-NLS-1$
			
			fPrefixField= new StringButtonDialogField(adapter);
			fPrefixField.setDialogFieldListener(adapter);
			fPrefixField.setLabelText(NewWizardMessages.getString("SourceAttachmentBlock.prefix.label")); //$NON-NLS-1$
			fPrefixField.setButtonLabel(NewWizardMessages.getString("SourceAttachmentBlock.prefix.button")); //$NON-NLS-1$
		}	
			
		fJavaDocField= new StringButtonDialogField(adapter);
		fJavaDocField.setDialogFieldListener(adapter);
		fJavaDocField.setLabelText(NewWizardMessages.getString("SourceAttachmentBlock.javadoc.label")); //$NON-NLS-1$
		fJavaDocField.setButtonLabel(NewWizardMessages.getString("SourceAttachmentBlock.javadoc.button"));		 //$NON-NLS-1$
			
		if (oldEntry != null && oldEntry.getSourceAttachmentPath() != null) {
			fFileNameField.setText(oldEntry.getSourceAttachmentPath().toString());
		} else {
			fFileNameField.setText(""); //$NON-NLS-1$
		}	
				
		if (oldEntry != null && oldEntry.getSourceAttachmentRootPath() != null) {
			fPrefixField.setText(oldEntry.getSourceAttachmentRootPath().toString());
		} else {
			fPrefixField.setText(""); //$NON-NLS-1$
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
	

	private Label createHelpText(Composite composite, int style, boolean indented, String text) {
		if (indented) {
			DialogField.createEmptySpace(composite, 1);
		}
			
		Label helpTextLabel= new Label(composite, style);
		helpTextLabel.setText(text);
		MGridData gd= new MGridData(MGridData.HORIZONTAL_ALIGN_FILL);
		helpTextLabel.setLayoutData(gd);
		if (indented) {
			DialogField.createEmptySpace(composite, 2);
			gd.horizontalSpan= 1;
		} else {
			gd.horizontalSpan= 4;
		}
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
		
		createHelpText(composite, SWT.LEFT, false, NewWizardMessages.getFormattedString("SourceAttachmentBlock.message", fJARPath.lastSegment())); //$NON-NLS-1$
		
		if (fIsVariableEntry) {
			createHelpText(composite, SWT.LEFT + SWT.WRAP, true, NewWizardMessages.getString("SourceAttachmentBlock.filename.description")); //$NON-NLS-1$
		}			
		
		fFileNameField.doFillIntoGrid(composite, 4);
		if (!fIsVariableEntry) {
			DialogField.createEmptySpace(composite, 3);	
			fInternalButtonField.doFillIntoGrid(composite, 1);
		} else {
			DialogField.createEmptySpace(composite, 1);	
			fFullPathResolvedLabel= new CLabel(composite, SWT.LEFT);
			fFullPathResolvedLabel.setText(getResolvedLabelString(fFileNameField.getText()));
			fFullPathResolvedLabel.setLayoutData(new MGridData(MGridData.HORIZONTAL_ALIGN_FILL));
			DialogField.createEmptySpace(composite, 2);			
		}
		
		// label
		createHelpText(composite, SWT.LEFT + SWT.WRAP, true, NewWizardMessages.getString("SourceAttachmentBlock.prefix.description")); //$NON-NLS-1$
	
		fPrefixField.doFillIntoGrid(composite, 4);
		if (fIsVariableEntry) {
			DialogField.createEmptySpace(composite, 1);	
			fPrefixResolvedLabel= new CLabel(composite, SWT.LEFT);
			fPrefixResolvedLabel.setText(getResolvedLabelString(fPrefixField.getText()));
			fPrefixResolvedLabel.setLayoutData(new MGridData(MGridData.HORIZONTAL_ALIGN_FILL));
			DialogField.createEmptySpace(composite, 2);
		}
		
		fFileNameField.postSetFocusOnDialogField(parent.getDisplay());
		
		//DialogField.createEmptySpace(composite, 1);
		//Label jdocDescription= new Label(composite, SWT.LEFT + SWT.WRAP);
		//jdocDescription.setText(NewWizardMessages.getString(JAVADOC + ".description"));
		//DialogField.createEmptySpace(composite, 1);
			
		//fJavaDocField.doFillIntoGrid(composite, 3);
		
		WorkbenchHelp.setHelp(composite, new Object[] { IJavaHelpContextIds.SOURCE_ATTACHMENT_BLOCK });
		return composite;
	}
	
		
	private class SourceAttachmentAdapter implements IStringButtonAdapter, IDialogFieldListener {
		
		// -------- IStringButtonAdapter --------
		public void changeControlPressed(DialogField field) {
			attachmentChangeControlPressed(field);			
		}
		
		// ---------- IDialogFieldListener --------
		public void dialogFieldChanged(DialogField field) {
			attachmentDialogFieldChanged(field);
		}
	}
	
	private void attachmentChangeControlPressed(DialogField field) {
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

	private void attachmentDialogFieldChanged(DialogField field) {
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
		
	private void doStatusLineUpdate() {
		fPrefixField.enableButton(canBrowsePrefix());
		fFileNameField.enableButton(canBrowseFileName());

		if (fPrefixResolvedLabel != null) {
			fPrefixResolvedLabel.setText(getResolvedLabelString(fPrefixField.getText()));
		}
		if (fFullPathResolvedLabel != null) {
			fFullPathResolvedLabel.setText(getResolvedLabelString(fFileNameField.getText()));
		}			
		
		IStatus status= StatusTool.getMostSevere(new IStatus[] { fNameStatus, fPrefixStatus, fJavaDocStatus });
		fContext.statusChanged(status);
	}
	
	private boolean canBrowseFileName() {
		if (!fIsVariableEntry) {
			return true;
		}
		if (fFilePathVariable == null) {
			return false;
		}
		return JavaCore.getClasspathVariable(fFilePathVariable).toFile().isDirectory();	
	}
	
	private boolean canBrowsePrefix() {
		return (fResolvedFile != null) && (!fIsVariableEntry || fPrefixVariable != null);
	}	
	
	
	
	private String getResolvedLabelString(String path) {
		IPath resolvedPath= getResolvedPath(new Path(path));
		if (resolvedPath != null) {
			return resolvedPath.toOSString();
		}
		return ""; //$NON-NLS-1$
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
			// no source attachment path
			return status;
		} else {
			if (!Path.EMPTY.isValidPath(prefix)) {
				status.setError(NewWizardMessages.getString("SourceAttachmentBlock.prefix.error.notvalid")); //$NON-NLS-1$
				return status;
			}
			IPath path= new Path(prefix);
			if (fIsVariableEntry) {
				IPath resolvedPath= getResolvedPath(path);
				if (resolvedPath == null) {
					status.setError(NewWizardMessages.getString("SourceAttachmentBlock.prefix.error.varnotexists")); //$NON-NLS-1$
					return status;
				}
				if (resolvedPath.getDevice() != null) {
					status.setError(NewWizardMessages.getString("SourceAttachmentBlock.prefix.error.deviceinvar")); //$NON-NLS-1$
					return status;
				}				
				fPrefixVariable= path.segment(0);
			} else {
				if (path.getDevice() != null) {
					status.setError(NewWizardMessages.getString("SourceAttachmentBlock.prefix.error.deviceinpath")); //$NON-NLS-1$
					return status;
				}
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
			// no source attachment path
			return status;
		} else {
			if (!Path.EMPTY.isValidPath(fileName)) {
				status.setError(NewWizardMessages.getString("SourceAttachmentBlock.filename.error.notvalid")); //$NON-NLS-1$
				return status;
			}
			IPath filePath= new Path(fileName);
			IPath resolvedPath;
			if (fIsVariableEntry) {
				if (filePath.getDevice() != null) {
					status.setError(NewWizardMessages.getString("SourceAttachmentBlock.filename.error.deviceinpath")); //$NON-NLS-1$
					return status;
				} 
				resolvedPath= getResolvedPath(filePath);
				if (resolvedPath == null) {
					status.setError(NewWizardMessages.getString("SourceAttachmentBlock.filename.error.varnotexists")); //$NON-NLS-1$
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
			if (resolvedPath.isEmpty()) {
				status.setWarning(NewWizardMessages.getString("SourceAttachmentBlock.filename.warning.varempty")); //$NON-NLS-1$
				return status;
			}
	
			File resolvedFile= resolvedPath.toFile();
			if (!resolvedFile.isFile()) {
				String message= NewWizardMessages.getFormattedString("SourceAttachmentBlock.filename.error.filenotexists", resolvedPath.toOSString()); //$NON-NLS-1$
				if (fIsVariableEntry) {
					status.setWarning(message);
				} else {
					status.setError(message);
				}
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
		if (!"".equals(jdocLocation)) { //$NON-NLS-1$
			try {
				URL url= new URL(jdocLocation);
				if ("file".equals(url.getProtocol())) { //$NON-NLS-1$
					File dir= new File(url.getFile());
					if (!dir.isDirectory()) {
						status.setError(NewWizardMessages.getString("SourceAttachmentBlock.javadoc.error.notafolder")); //$NON-NLS-1$
						return status;
					}
					/*else {
						File indexFile= new File(dir, "index.html");
						File packagesFile= new File(dir, "package-list");
						if (!packagesFile.exists() || !indexFile.exists()) {
							fJavaDocStatusInfo.setWarning(NewWizardMessages.getString(ERR_JDOCLOCATION_IDXNOTFOUND));
							// only a warning, go on
						}
					}*/	
				}
				fJavaDocLocation= url;
			} catch (MalformedURLException e) {
				status.setError(NewWizardMessages.getFormattedString("SourceAttachmentBlock.javadoc.error.malformed", e.getLocalizedMessage())); //$NON-NLS-1$
				return status;
			}
		}
		return status;
	}
	
	/*
	 * Opens a dialog to choose a jar from the file system.
	 */
	private IPath chooseExtJarFile() {
		IPath currPath= new Path(fFileNameField.getText());
		
		if (currPath.isEmpty()) {
			currPath= fJARPath;
		}
		IPath resolvedPath= currPath;
		if (fIsVariableEntry) {
			resolvedPath= getResolvedPath(currPath);
			if (resolvedPath == null) {
				resolvedPath= Path.EMPTY;
			}
		}
		String ext= resolvedPath.getFileExtension();
		if ("jar".equals(ext) || "zip".equals(ext)) { //$NON-NLS-2$ //$NON-NLS-1$
			resolvedPath= resolvedPath.removeLastSegments(1);
		}
	
		FileDialog dialog= new FileDialog(getShell());
		dialog.setText(NewWizardMessages.getString("SourceAttachmentBlock.extjardialog.text")); //$NON-NLS-1$
		dialog.setFilterExtensions(new String[] {"*.jar;*.zip"}); //$NON-NLS-1$
		dialog.setFilterPath(resolvedPath.toOSString());
		String res= dialog.open();
		if (res != null) {
			IPath returnPath= new Path(res).makeAbsolute();
			if (fIsVariableEntry) {
				returnPath= modifyPath(returnPath, currPath.segment(0));
			}
			return returnPath;
		}
		return null;
	}

	/*
	 * Opens a dialog to choose an internal jar.
	 */	
	private IPath chooseInternalJarFile(String initSelection) {
		Class[] acceptedClasses= new Class[] { IFile.class };
		ISelectionValidator validator= new TypedElementSelectionValidator(acceptedClasses, false);
	
		ViewerFilter filter= new ResourceFilter(new String[] { "jar", "zip" }, false, null); //$NON-NLS-2$ //$NON-NLS-1$

		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new WorkbenchContentProvider();

		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), lp, cp, false, true);
		dialog.setValidator(validator);
		dialog.addFilter(filter);
		dialog.setTitle(NewWizardMessages.getString("SourceAttachmentBlock.intjardialog.title")); //$NON-NLS-1$
		dialog.setMessage(NewWizardMessages.getString("SourceAttachmentBlock.intjardialog.message")); //$NON-NLS-1$
		
		IResource initSel= fRoot.findMember(new Path(initSelection));
		if (initSel == null) {
			initSel= fRoot.findMember(fJARPath);
		}
		if (dialog.open(fRoot, initSel) == dialog.OK) {
			IFile file= (IFile) dialog.getPrimaryResult();
			return file.getFullPath();
		}
		return null;
	}
	
	/*
	 * Opens a dialog to choose path in a zip file.
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
				ZipContentProvider contentProvider= new ZipContentProvider(zipFile);
				ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), new ZipLabelProvider(), contentProvider, false, true); 
				dialog.setTitle(NewWizardMessages.getString("SourceAttachmentBlock.prefixdialog.title")); //$NON-NLS-1$
				dialog.setMessage(NewWizardMessages.getString("SourceAttachmentBlock.prefixdialog.message")); //$NON-NLS-1$
				if (dialog.open(zipFile, contentProvider.getSelectedNode(initSelection)) == dialog.OK) {
					Object obj= dialog.getPrimaryResult();
					IPath path= new Path(obj.toString());
					if (fIsVariableEntry) {
						path= modifyPath(path, currPath.segment(0));
					}
					return path;
				}
			} catch (IOException e) {
				String title= NewWizardMessages.getString("SourceAttachmentBlock.prefixdialog.error.title"); //$NON-NLS-1$
				String message= NewWizardMessages.getFormattedString("SourceAttachmentBlock.prefixdialog.error.message", fResolvedFile.getPath()); //$NON-NLS-1$
				MessageDialog.openError(getShell(), title, message);
				JavaPlugin.log(e);
			}				
		
		}
		return null;
	}
	
	/*
	 * Opens a dialog to choose a root in the file system.
	 */	
	private URL chooseJavaDocLocation() {
		String initPath= ""; //$NON-NLS-1$
		if (fJavaDocLocation != null && "file".equals(fJavaDocLocation.getProtocol())) { //$NON-NLS-1$
			initPath= (new File(fJavaDocLocation.getFile())).getPath();
		}
		DirectoryDialog dialog= new DirectoryDialog(getShell());
		dialog.setText(NewWizardMessages.getString("SourceAttachmentBlock.jdocdialog.text")); //$NON-NLS-1$
		dialog.setMessage(NewWizardMessages.getString("SourceAttachmentBlock.jdocdialog.message")); //$NON-NLS-1$
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
		
	private Shell getShell() {
		if (fSWTWidget != null) {
			return fSWTWidget.getShell();
		}
		return JavaPlugin.getActiveWorkbenchShell();			
	}
	
	/**
	 * Takes a path and replaces the beginning with a variable name
	 * (if the beginning matches with the variables value)
	 */
	private IPath modifyPath(IPath path, String varName) {
		if (varName == null || path == null) {
			return null;
		}
		if (path.isEmpty()) {
			return new Path(varName);
		}
		
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
	
	
	/**
	 * Creates a runnable that sets the source attachment by modifying the project's classpath.
	 */
	public IRunnableWithProgress getRunnable(final IJavaProject jproject, final Shell shell) {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {				
				try {
					IClasspathEntry newEntry;
					if (fIsVariableEntry) {
						newEntry= JavaCore.newVariableEntry(fJARPath, getSourceAttachmentPath(), getSourceAttachmentRootPath());
					} else {
						newEntry= JavaCore.newLibraryEntry(fJARPath, getSourceAttachmentPath(), getSourceAttachmentRootPath());
					}
					IClasspathEntry[] entries= modifyClasspath(jproject, newEntry, shell);		
					if (entries != null) {
						jproject.setRawClasspath(entries, monitor);
					}
				} catch (JavaModelException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
	}

	private IClasspathEntry[] modifyClasspath(IJavaProject jproject, IClasspathEntry newEntry, Shell shell) throws JavaModelException{
		IClasspathEntry[] oldClasspath= jproject.getRawClasspath();
		int nEntries= oldClasspath.length;
		ArrayList newEntries= new ArrayList(nEntries + 1);
		int entryKind= newEntry.getEntryKind();
		IPath jarPath= newEntry.getPath();
		boolean added= false;
		for (int i= 0; i < nEntries; i++) {
			IClasspathEntry curr= oldClasspath[i];
			if (curr.getEntryKind() == entryKind && curr.getPath().equals(jarPath)) {
				newEntries.add(newEntry);
				added= true;
			} else {
				newEntries.add(curr);
			}
		}
		if (!added) {
			if (newEntry.getSourceAttachmentPath() == null && !putJarOnClasspathDialog(shell)) {
				return null;
			}
		}
		return (IClasspathEntry[]) newEntries.toArray(new IClasspathEntry[newEntries.size()]);
	}	
				
	private boolean putJarOnClasspathDialog(Shell shell) {
		final boolean[] result= new boolean[1];
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				String title= NewWizardMessages.getString("SourceAttachmentBlock.putoncpdialog.title"); //$NON-NLS-1$
				String message= NewWizardMessages.getString("SourceAttachmentBlock.putoncpdialog.message"); //$NON-NLS-1$
				result[0]= MessageDialog.openQuestion(JavaPlugin.getActiveWorkbenchShell(), title, message);
			}
		});
		return result[0];
	}
		
}
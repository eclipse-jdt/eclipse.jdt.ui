/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.TypedElementSelectionValidator;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;

/**
 * UI to set the source attachment archive and root.
 * Same implementation for both setting attachments for libraries from
 * variable entries and for normal (internal or external) jar.
 */
public class SourceAttachmentBlock {
	
	private IStatusChangeListener fContext;
	
	private StringButtonDialogField fFileNameField;
	private SelectionButtonDialogField fInternalButtonField;
	
	private StringButtonDialogField fPrefixField;
	
	private boolean fIsVariableEntry;
	
	private IStatus fNameStatus;
	private IStatus fPrefixStatus;
		
	private IPath fJARPath;
	
	/**
	 * The file to which the archive path points to.
	 * Only set when the file exists.
	 */
	private File fResolvedFile; 

	/**
	 * The path to which the archive variable points.
	 * Null if invalid path or not resolvable. Must not exist.
	 */	
	private IPath fFileVariablePath;
		
	private IWorkspaceRoot fRoot;
	
	private Control fSWTWidget;
	private CLabel fFullPathResolvedLabel;
	private CLabel fPrefixResolvedLabel;
	
	private IClasspathEntry fOldEntry;
	
	public SourceAttachmentBlock(IWorkspaceRoot root, IStatusChangeListener context, IClasspathEntry oldEntry) {
		fContext= context;
				
		fRoot= root;
		fOldEntry= oldEntry;
		
		// fIsVariableEntry specifies if the UI is for a variable entry
		fIsVariableEntry= (oldEntry.getEntryKind() == IClasspathEntry.CPE_VARIABLE);
		
		fNameStatus= new StatusInfo();
		fPrefixStatus= new StatusInfo();
		
		fJARPath= (oldEntry != null) ? oldEntry.getPath() : Path.EMPTY;
		
		SourceAttachmentAdapter adapter= new SourceAttachmentAdapter();
		
		// create the dialog fields (no widgets yet)
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
	
		// set the old settings
		setDefaults();

	}
	
	public void setDefaults() {
		if (fOldEntry != null && fOldEntry.getSourceAttachmentPath() != null) {
			fFileNameField.setText(fOldEntry.getSourceAttachmentPath().toString());
		} else {
			fFileNameField.setText(""); //$NON-NLS-1$
		}	
				
		if (fOldEntry != null && fOldEntry.getSourceAttachmentRootPath() != null) {
			fPrefixField.setText(fOldEntry.getSourceAttachmentRootPath().toString());
		} else {
			fPrefixField.setText(""); //$NON-NLS-1$
		}
	}
	
	/**
	 * Gets the source attachment path chosen by the user
	 */
	public IPath getSourceAttachmentPath() {
		if (fFileNameField.getText().length() == 0) {
			return null;
		}
		return new Path(fFileNameField.getText());
	}

	/**
	 * Gets the source attachment root chosen by the user
	 */
	public IPath getSourceAttachmentRootPath() {
		if (getSourceAttachmentPath() == null) {
			return null;
		} else {
			return new Path(fPrefixField.getText());
		}
	}
	
		
	/**
	 * Creates the control
	 */
	public Control createControl(Composite parent) {
		PixelConverter converter= new PixelConverter(parent);
		
		fSWTWidget= parent;
		
		Composite composite= new Composite(parent, SWT.NONE);	
		
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 4;		
		composite.setLayout(layout);
		
		int widthHint= converter.convertWidthInCharsToPixels(fIsVariableEntry ? 50 : 60);
		
		
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 4;
		
		Label message= new Label(composite, SWT.LEFT);
		message.setLayoutData(gd);
		message.setText(NewWizardMessages.getFormattedString("SourceAttachmentBlock.message", fJARPath.lastSegment())); //$NON-NLS-1$
				
		if (fIsVariableEntry) {
			DialogField.createEmptySpace(composite, 1);
			gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
			gd.widthHint= widthHint;
			gd.horizontalSpan= 2;
			Label desc= new Label(composite, SWT.LEFT + SWT.WRAP);
			desc.setText(NewWizardMessages.getString("SourceAttachmentBlock.filename.description")); //$NON-NLS-1$
			desc.setLayoutData(gd);
			DialogField.createEmptySpace(composite, 1);
		}
		// archive name field
		fFileNameField.doFillIntoGrid(composite, 4);
		LayoutUtil.setWidthHint(fFileNameField.getTextControl(null), widthHint);
		LayoutUtil.setHorizontalGrabbing(fFileNameField.getTextControl(null));
		
		if (!fIsVariableEntry) {
			// aditional 'browse workspace' button for normal jars
			DialogField.createEmptySpace(composite, 3);	
			fInternalButtonField.doFillIntoGrid(composite, 1);
		} else {
			// label that shows the resolved path for variable jars
			DialogField.createEmptySpace(composite, 1);	
			fFullPathResolvedLabel= new CLabel(composite, SWT.LEFT);
			fFullPathResolvedLabel.setText(getResolvedLabelString(fFileNameField.getText(), true));
			fFullPathResolvedLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
			DialogField.createEmptySpace(composite, 2);			
		}
		
		// prefix description
		DialogField.createEmptySpace(composite, 1);
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.widthHint= widthHint;
		gd.horizontalSpan= 2;
		Label desc= new Label(composite, SWT.LEFT + SWT.WRAP);
		desc.setText(NewWizardMessages.getString("SourceAttachmentBlock.prefix.description")); //$NON-NLS-1$
		desc.setLayoutData(gd);
		DialogField.createEmptySpace(composite, 1);		
		
		// root path field	
		fPrefixField.doFillIntoGrid(composite, 4);
		LayoutUtil.setWidthHint(fPrefixField.getTextControl(null), widthHint);
		
		if (fIsVariableEntry) {
			// label that shows the resolved path for variable jars
			DialogField.createEmptySpace(composite, 1);	
			fPrefixResolvedLabel= new CLabel(composite, SWT.LEFT);
			fPrefixResolvedLabel.setText(getResolvedLabelString(fPrefixField.getText(), false));
			fPrefixResolvedLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
			DialogField.createEmptySpace(composite, 2);
		}
		
		fFileNameField.postSetFocusOnDialogField(parent.getDisplay());
				
		WorkbenchHelp.setHelp(composite, IJavaHelpContextIds.SOURCE_ATTACHMENT_BLOCK);
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
				fFileNameField.setText(jarFilePath.toString());
			}
		} else if (field == fPrefixField) {
			IPath prefixPath= choosePrefix();
			if (prefixPath != null) {
				fPrefixField.setText(prefixPath.toString());
			}
		}		
	}
	
	// ---------- IDialogFieldListener --------

	private void attachmentDialogFieldChanged(DialogField field) {
		if (field == fFileNameField) {
			fNameStatus= updateFileNameStatus();
		} else if (field == fInternalButtonField) {
			IPath jarFilePath= chooseInternalJarFile();
			if (jarFilePath != null) {
				fFileNameField.setText(jarFilePath.toString());
			}
			return;
		} else if (field == fPrefixField) {
			fPrefixStatus= updatePrefixStatus();
		} 
		doStatusLineUpdate();
	}	
		
	private void doStatusLineUpdate() {
		fPrefixField.enableButton(canBrowsePrefix());
		fFileNameField.enableButton(canBrowseFileName());
		
		// set the resolved path for variable jars
		if (fFullPathResolvedLabel != null) {
			fFullPathResolvedLabel.setText(getResolvedLabelString(fFileNameField.getText(), true));
		}
		if (fPrefixResolvedLabel != null) {
			fPrefixResolvedLabel.setText(getResolvedLabelString(fPrefixField.getText(), false));
		}
		
		IStatus status= StatusUtil.getMostSevere(new IStatus[] { fNameStatus, fPrefixStatus });
		fContext.statusChanged(status);
	}
	
	private boolean canBrowseFileName() {
		if (!fIsVariableEntry) {
			return true;
		}
		// to browse with a variable JAR, the variable name must point to a directory
		if (fFileVariablePath != null) {
			return fFileVariablePath.toFile().isDirectory();
		}
		return false;
	}
	
	private boolean canBrowsePrefix() {
		// can browse when the archive name is poiting to a existing file
		// and (if variable) the prefix variable name is existing
		if (fResolvedFile != null) {
			if (fIsVariableEntry) {
				// prefix has valid format, is resolvable and not empty
				return fPrefixStatus.isOK() && fPrefixField.getText().length() > 0;
			}
			return true;
		}
		return false;
	}	
	
	private String getResolvedLabelString(String path, boolean osPath) {
		IPath resolvedPath= getResolvedPath(new Path(path));
		if (resolvedPath != null) {
			if (osPath) {
				return resolvedPath.toOSString();
			} else {
				return resolvedPath.toString();
			}
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
			} else {
				if (path.getDevice() != null) {
					status.setError(NewWizardMessages.getString("SourceAttachmentBlock.prefix.error.deviceinpath")); //$NON-NLS-1$
					return status;
				}
			}				
		}
		return status;
	}
	
	private IStatus updateFileNameStatus() {
		StatusInfo status= new StatusInfo();
		fResolvedFile= null;
		fFileVariablePath= null;
		
		String fileName= fFileNameField.getText();
		if (fileName.length() == 0) {
			// no source attachment
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
				String varName= filePath.segment(0);
				if (varName == null) {
					status.setError(NewWizardMessages.getString("SourceAttachmentBlock.filename.error.notvalid")); //$NON-NLS-1$
					return status;
				}
				fFileVariablePath= JavaCore.getClasspathVariable(varName);
				if (fFileVariablePath == null) {
					status.setError(NewWizardMessages.getString("SourceAttachmentBlock.filename.error.varnotexists")); //$NON-NLS-1$
					return status;
				}
				resolvedPath= fFileVariablePath.append(filePath.removeFirstSegments(1));
				
				if (resolvedPath.isEmpty()) {
					status.setWarning(NewWizardMessages.getString("SourceAttachmentBlock.filename.warning.varempty")); //$NON-NLS-1$
					return status;
				}
				File file= resolvedPath.toFile();
				if (!file.isFile()) {				
					String message= NewWizardMessages.getFormattedString("SourceAttachmentBlock.filename.error.filenotexists", resolvedPath.toOSString()); //$NON-NLS-1$
					status.setWarning(message);
					return status;
				}						
				fResolvedFile= file;
				
			} else {
				File file= filePath.toFile();
				IResource res= fRoot.findMember(filePath);
				if (res != null) {
					file= res.getLocation().toFile();
				}
				if (!file.isFile()) {
					String message=  NewWizardMessages.getFormattedString("SourceAttachmentBlock.filename.error.filenotexists", filePath.toString()); //$NON-NLS-1$
					status.setError(message);
					return status;
				}
				fResolvedFile= file;
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
		if (fIsVariableEntry) {
			IPath resolvedPath= getResolvedPath(currPath);
			File initialSelection= resolvedPath != null ? resolvedPath.toFile() : null;
			
			String currVariable= currPath.segment(0);
			JARFileSelectionDialog dialog= new JARFileSelectionDialog(getShell(), false);
			dialog.setTitle(NewWizardMessages.getString("SourceAttachmentBlock.extvardialog.title")); //$NON-NLS-1$
			dialog.setMessage(NewWizardMessages.getString("SourceAttachmentBlock.extvardialog.description")); //$NON-NLS-1$
			dialog.setInput(fFileVariablePath.toFile());
			dialog.setInitialSelection(initialSelection);
			if (dialog.open() == dialog.OK) {
				File result= (File) dialog.getResult()[0];
				IPath returnPath= new Path(result.getPath()).makeAbsolute();
				return modifyPath(returnPath, currVariable);
			}
		} else {
			
			if (ArchiveFileFilter.isArchivePath(currPath)) {
				currPath= currPath.removeLastSegments(1);
			}
		
			FileDialog dialog= new FileDialog(getShell());
			dialog.setText(NewWizardMessages.getString("SourceAttachmentBlock.extjardialog.text")); //$NON-NLS-1$
			dialog.setFilterExtensions(new String[] {"*.jar;*.zip"}); //$NON-NLS-1$
			dialog.setFilterPath(currPath.toOSString());
			String res= dialog.open();
			if (res != null) {
				return new Path(res).makeAbsolute();
			}
		}
		return null;
	}

	/*
	 * Opens a dialog to choose an internal jar.
	 */	
	private IPath chooseInternalJarFile() {
		String initSelection= fFileNameField.getText();
		
		Class[] acceptedClasses= new Class[] { IFile.class };
		TypedElementSelectionValidator validator= new TypedElementSelectionValidator(acceptedClasses, false);
	
		ViewerFilter filter= new ArchiveFileFilter(null);

		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new WorkbenchContentProvider();

		IResource initSel= null;
		if (initSelection.length() > 0) {
			initSel= fRoot.findMember(new Path(initSelection));
		}
		if (initSel == null) {
			initSel= fRoot.findMember(fJARPath);
		}

		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), lp, cp);
		dialog.setAllowMultiple(false);
		dialog.setValidator(validator);
		dialog.addFilter(filter);
		dialog.setTitle(NewWizardMessages.getString("SourceAttachmentBlock.intjardialog.title")); //$NON-NLS-1$
		dialog.setMessage(NewWizardMessages.getString("SourceAttachmentBlock.intjardialog.message")); //$NON-NLS-1$
		dialog.setInput(fRoot);
		dialog.setInitialSelection(initSel);
		if (dialog.open() == dialog.OK) {
			IFile file= (IFile) dialog.getFirstResult();
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
				ZipContentProvider contentProvider= new ZipContentProvider();
				contentProvider.setInitialInput(zipFile);
				ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), new ZipLabelProvider(), contentProvider); 
				dialog.setAllowMultiple(false);
				dialog.setTitle(NewWizardMessages.getString("SourceAttachmentBlock.prefixdialog.title")); //$NON-NLS-1$
				dialog.setMessage(NewWizardMessages.getString("SourceAttachmentBlock.prefixdialog.message")); //$NON-NLS-1$
				dialog.setInput(zipFile);
				dialog.setInitialSelection(contentProvider.getSelectedNode(initSelection));
				if (dialog.open() == dialog.OK) {
					Object obj= dialog.getFirstResult();
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
					boolean isExported= fOldEntry != null ? fOldEntry.isExported() : false;
					IClasspathEntry newEntry;
					if (fIsVariableEntry) {
						newEntry= JavaCore.newVariableEntry(fJARPath, getSourceAttachmentPath(), getSourceAttachmentRootPath(), isExported);
					} else {
						newEntry= JavaCore.newLibraryEntry(fJARPath, getSourceAttachmentPath(), getSourceAttachmentRootPath(), isExported);
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
		boolean found= false;
		for (int i= 0; i < nEntries; i++) {
			IClasspathEntry curr= oldClasspath[i];
			if (curr.getEntryKind() == entryKind && curr.getPath().equals(jarPath)) {
				// add modified entry
				newEntries.add(newEntry);
				found= true;
			} else {
				newEntries.add(curr);
			}
		}
		if (!found) {
			if (newEntry.getSourceAttachmentPath() == null || !putJarOnClasspathDialog(shell)) {
				return null;
			}
			// add new
			newEntries.add(newEntry);			
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
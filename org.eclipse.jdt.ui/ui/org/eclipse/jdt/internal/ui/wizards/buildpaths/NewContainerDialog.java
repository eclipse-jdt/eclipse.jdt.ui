/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;

public class NewContainerDialog extends StatusDialog {

	private static final String ERR_INVALIDPATH= "NewContainerDialog.error.invalidpath";
	private static final String ERR_ENTERPATH= "NewContainerDialog.error.enterpath";
	private static final String ERR_ALREADYEXISTS= "NewContainerDialog.error.pathexists";
	
	private StringDialogField fContainerDialogField;
	private StatusInfo fContainerFieldStatus;
	
	private IFolder fFolder;
	private List fExistingFolders;
	private IProject fCurrProject;
		
	public NewContainerDialog(Shell parent, String title, IProject project, List existingFolders) {
		super(parent);
		setTitle(title);
		
		fContainerFieldStatus= new StatusInfo();
		
		SourceContainerAdapter adapter= new SourceContainerAdapter();
		fContainerDialogField= new StringDialogField();
		fContainerDialogField.setDialogFieldListener(adapter);
		
		fFolder= null;
		fExistingFolders= existingFolders;
		fCurrProject= project;
		
		fContainerDialogField.setText("");
	}
	
	public void setMessage(String message) {
		fContainerDialogField.setLabelText(message);
	}
	
		
	protected Control createDialogArea(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		MGridLayout layout= new MGridLayout();
		layout.minimumWidth= 380;
		layout.marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.numColumns= 1;
		composite.setLayout(layout);
		
		fContainerDialogField.doFillIntoGrid(composite, 2);
				
		fContainerDialogField.postSetFocusOnDialogField(parent.getDisplay());
		return composite;
	}

		
	// -------- SourceContainerAdapter --------

	private class SourceContainerAdapter implements IDialogFieldListener {
		
		// -------- IDialogFieldListener
		
		public void dialogFieldChanged(DialogField field) {
			doStatusLineUpdate();
		}
	}
	
	protected void doStatusLineUpdate() {
		checkIfPathValid();
		updateStatus(fContainerFieldStatus);
	}		
	
	protected void checkIfPathValid() {
		fFolder= null;
		
		String pathStr= fContainerDialogField.getText();
		if ("".equals(pathStr)) {
			fContainerFieldStatus.setError(JavaPlugin.getResourceString(ERR_ENTERPATH));
			return;
		}
		IPath path= fCurrProject.getFullPath().append(pathStr);
		IWorkspace workspace= fCurrProject.getWorkspace();
		
		IStatus pathValidation= workspace.validatePath(path.toString(), IResource.FOLDER);
		if (!pathValidation.isOK()) {
			fContainerFieldStatus.setError(JavaPlugin.getFormattedString(ERR_INVALIDPATH, pathValidation.getMessage()));
			return;
		}
		IFolder folder= fCurrProject.getFolder(pathStr);
		if (fExistingFolders.contains(folder)) {
			fContainerFieldStatus.setError(JavaPlugin.getResourceString(ERR_ALREADYEXISTS));
			return;
		}
		fContainerFieldStatus.setOK();
		fFolder= folder;
	}
		
	public IFolder getFolder() {
		return fFolder;
	}
		
	
}
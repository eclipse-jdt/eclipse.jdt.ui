/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceSorter;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.TypedElementSelectionValidator;
import org.eclipse.jdt.internal.ui.wizards.TypedViewerFilter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;

public class OutputLocationDialog extends StatusDialog {
	
	private StringButtonDialogField fContainerDialogField;
	private SelectionButtonDialogField fUseDefault;
	private SelectionButtonDialogField fUseSpecific;
	private StatusInfo fContainerFieldStatus;
	
	private IProject fCurrProject;
	private IPath fOutputLocation;
		
	public OutputLocationDialog(Shell parent, CPListElement entryToEdit) {
		super(parent);
		setTitle(NewWizardMessages.getString("OutputLocationDialog.title")); //$NON-NLS-1$
		fContainerFieldStatus= new StatusInfo();
	
		OutputLocationAdapter adapter= new OutputLocationAdapter();

		fUseDefault= new SelectionButtonDialogField(SWT.RADIO);
		fUseDefault.setLabelText(NewWizardMessages.getString("OutputLocationDialog.usedefault.label")); //$NON-NLS-1$
		fUseDefault.setDialogFieldListener(adapter);		

		String label= NewWizardMessages.getFormattedString("OutputLocationDialog.usespecific.label", entryToEdit.getPath().segment(0)); //$NON-NLS-1$
		fUseSpecific= new SelectionButtonDialogField(SWT.RADIO);
		fUseSpecific.setLabelText(label);
		fUseSpecific.setDialogFieldListener(adapter);		
		
		fContainerDialogField= new StringButtonDialogField(adapter);
		fContainerDialogField.setButtonLabel(NewWizardMessages.getString("OutputLocationDialog.location.button")); //$NON-NLS-1$
		fContainerDialogField.setDialogFieldListener(adapter);
		
		fUseSpecific.attachDialogField(fContainerDialogField);
		
		fCurrProject= entryToEdit.getJavaProject().getProject();
		
		IPath outputLocation= (IPath) entryToEdit.getAttribute(CPListElement.OUTPUT);
		if (outputLocation == null) {
			fUseDefault.setSelection(true);
		} else {
			fUseSpecific.setSelection(true);
			fContainerDialogField.setText(outputLocation.removeFirstSegments(1).makeRelative().toString());
		}
	}
	
	
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite)super.createDialogArea(parent);
		
		int widthHint= convertWidthInCharsToPixels(60);
		int indent= convertWidthInCharsToPixels(4);
		
		Composite inner= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		inner.setLayout(layout);
		
		fUseDefault.doFillIntoGrid(inner, 2);
		fUseSpecific.doFillIntoGrid(inner, 2);
		
		Text textControl= fContainerDialogField.getTextControl(inner);
		GridData textData= new GridData();
		textData.widthHint= widthHint;
		textData.grabExcessHorizontalSpace= true;
		textData.horizontalIndent= indent;
		textControl.setLayoutData(textData);
		
		Button buttonControl= fContainerDialogField.getChangeControl(inner);
		GridData buttonData= new GridData();
		buttonData.widthHint= SWTUtil.getButtonWidthHint(buttonControl);
		buttonControl.setLayoutData(buttonData);
		
		applyDialogFont(composite);		
		return composite;
	}

		
	// -------- OutputLocationAdapter --------

	private class OutputLocationAdapter implements IDialogFieldListener, IStringButtonAdapter {
		
		// -------- IDialogFieldListener
		
		public void dialogFieldChanged(DialogField field) {
			doStatusLineUpdate();
		}

		public void changeControlPressed(DialogField field) {
			doChangeControlPressed();
		}
	}
	
	protected void doChangeControlPressed() {
		IContainer container= chooseOutputLocation();
		if (container != null) {
			fContainerDialogField.setText(container.getProjectRelativePath().toString());
		}
	}
	
	
	protected void doStatusLineUpdate() {
		checkIfPathValid();
		updateStatus(fContainerFieldStatus);
	}		
	
	protected void checkIfPathValid() {
		fOutputLocation= null;
		fContainerFieldStatus.setOK();

		if (fUseDefault.isSelected()) {
			return;
		}
				
		String pathStr= fContainerDialogField.getText();
		if (pathStr.length() == 0) {
			fContainerFieldStatus.setOK();
			return;
		}
		IPath projectPath= fCurrProject.getFullPath();
				
		IPath path= projectPath.append(pathStr);
		
		IWorkspace workspace= fCurrProject.getWorkspace();		
		IStatus pathValidation= workspace.validatePath(path.toString(), IResource.PROJECT | IResource.FOLDER);
		if (!pathValidation.isOK()) {
			fContainerFieldStatus.setError(NewWizardMessages.getFormattedString("OutputLocationDialog.error.invalidpath", pathValidation.getMessage())); //$NON-NLS-1$
			return;
		}
		
		IWorkspaceRoot root= workspace.getRoot();
		IResource res= root.findMember(path);
		if (res != null) {
			// if exists, must be a folder or project
			if (res.getType() == IResource.FILE) {
				fContainerFieldStatus.setError(NewWizardMessages.getString("OutputLocationDialog.error.existingisfile")); //$NON-NLS-1$
				return;
			}
		}
		fOutputLocation= path;
	}
	
		
	public IPath getOutputLocation() {
		return fOutputLocation;
	}
		
	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		WorkbenchHelp.setHelp(newShell, IJavaHelpContextIds.OUTPUT_LOCATION_DIALOG);
	}
	
	// ---------- util method ------------

	private IContainer chooseOutputLocation() {
		IWorkspaceRoot root= fCurrProject.getWorkspace().getRoot();
		Class[] acceptedClasses= new Class[] { IProject.class, IFolder.class };
		ISelectionStatusValidator validator= new TypedElementSelectionValidator(acceptedClasses, false);
		IProject[] allProjects= root.getProjects();
		ArrayList rejectedElements= new ArrayList(allProjects.length);
		for (int i= 0; i < allProjects.length; i++) {
			if (!allProjects[i].equals(fCurrProject)) {
				rejectedElements.add(allProjects[i]);
			}
		}
		ViewerFilter filter= new TypedViewerFilter(acceptedClasses, rejectedElements.toArray());

		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new WorkbenchContentProvider();

		IResource initSelection= null;
		if (fOutputLocation != null) {
			initSelection= root.findMember(fOutputLocation);
		}

		FolderSelectionDialog dialog= new FolderSelectionDialog(getShell(), lp, cp);
		dialog.setTitle(NewWizardMessages.getString("OutputLocationDialog.ChooseOutputFolder.title")); //$NON-NLS-1$
		dialog.setValidator(validator);
		dialog.setMessage(NewWizardMessages.getString("OutputLocationDialog.ChooseOutputFolder.description")); //$NON-NLS-1$
		dialog.addFilter(filter);
		dialog.setInput(root);
		dialog.setInitialSelection(initSelection);
		dialog.setSorter(new ResourceSorter(ResourceSorter.NAME));

		if (dialog.open() == Window.OK) {
			return (IContainer)dialog.getFirstResult();
		}
		return null;
	}
	


}

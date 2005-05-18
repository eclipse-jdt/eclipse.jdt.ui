/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.TypedElementSelectionValidator;
import org.eclipse.jdt.internal.ui.wizards.TypedViewerFilter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

/**
 * 
 */
public class NativeLibrariesDialog extends StatusDialog {

	private class NativeLibrariesAdapter implements IDialogFieldListener {
		public void dialogFieldChanged(DialogField field) {
			doFieldChanged(field);
		}
	}
	
	private StringDialogField fPathField;
	private SelectionButtonDialogField fBrowseWorkspace;
	private SelectionButtonDialogField fBrowseExternal;
	private final CPListElement fEntry;
	
	public NativeLibrariesDialog(Shell parent, CPListElement selElement) {
		super(parent);
		setTitle(NewWizardMessages.NativeLibrariesDialog_title);
		
		setShellStyle(getShellStyle() | SWT.RESIZE);
		
		fEntry= selElement;
		
		NativeLibrariesAdapter adapter= new NativeLibrariesAdapter();
		
		fPathField= new StringDialogField();
		fPathField.setLabelText(NewWizardMessages.NativeLibrariesDialog_location_label);
		fPathField.setDialogFieldListener(adapter);
		
		fBrowseWorkspace= new SelectionButtonDialogField(SWT.PUSH);
		fBrowseWorkspace.setLabelText(NewWizardMessages.NativeLibrariesDialog_workspace_browse);
		fBrowseWorkspace.setDialogFieldListener(adapter);
		
		fBrowseExternal= new SelectionButtonDialogField(SWT.PUSH);
		fBrowseExternal.setLabelText(NewWizardMessages.NativeLibrariesDialog_external_browse);
		fBrowseExternal.setDialogFieldListener(adapter);
	
		String val= (String) selElement.getAttribute(CPListElement.NATIVE_LIB_PATH);
		if (val != null) {
			fPathField.setText(Path.fromPortableString(val).toString());
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite) super.createDialogArea(parent);
		
		Composite inner= new Composite(composite, SWT.NONE);
		inner.setFont(composite.getFont());
		inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		int nColumns= 3;
		
		GridLayout layout= new GridLayout(nColumns, false);
		layout.marginWidth= 0;
		layout.marginWidth= 0;
		inner.setLayout(layout);

		Label desc= new Label(inner, SWT.WRAP);
		desc.setFont(inner.getFont());
		desc.setText(MessageFormat.format(NewWizardMessages.NativeLibrariesDialog_description, new String[] { fEntry.getPath().lastSegment() }));
		GridData gridData= new GridData(GridData.FILL, GridData.CENTER, false, false, 3, 1);
		gridData.widthHint= convertWidthInCharsToPixels(80);
		desc.setLayoutData(gridData);
		
		fPathField.doFillIntoGrid(inner, 2);
		LayoutUtil.setHorizontalGrabbing(fPathField.getTextControl(null));
		LayoutUtil.setWidthHint(fPathField.getTextControl(null), convertWidthInCharsToPixels(50));
		
		fBrowseExternal.doFillIntoGrid(inner, 1);
		
		DialogField.createEmptySpace(inner, 2);
		fBrowseWorkspace.doFillIntoGrid(inner, 1);
		
		fPathField.setFocus();
		
		return composite;
	}
	
	public String getNativeLibraryPath() {
		String val= fPathField.getText();
		if (val.length() == 0) {
			return null;
		}
		return new Path(val).toPortableString();
	}
	
	final void doFieldChanged(DialogField field) {
		if (field == fBrowseExternal) {
			String res= chooseExternal();
			if (res != null) {
				fPathField.setText(res);
			}
		} else if (field == fBrowseWorkspace) {
			String res= chooseInternal();
			if (res != null) {
				fPathField.setText(res);
			}
		} else if (field == fPathField) {
			updateStatus(validatePath());
		}
	}

	private IStatus validatePath() {
		StatusInfo status= new StatusInfo();
		String val= fPathField.getText();
		if (val.length() == 0) {
			return status;
		}
		Path path= new Path(val);
		if (path.isAbsolute()) {
			if (!path.toFile().isDirectory()) {
				status.setWarning(NewWizardMessages.NativeLibrariesDialog_error_external_not_existing); 
				return status;
			}
		} else {
			if (!(ResourcesPlugin.getWorkspace().getRoot().findMember(path) instanceof IContainer)) {
				status.setWarning(NewWizardMessages.NativeLibrariesDialog_error_internal_not_existing); 
				return status;
			}
		}
		return status;
	}

	private String chooseExternal() {
		IPath currPath= new Path(fPathField.getText());
		if (currPath.isEmpty()) {
			currPath= fEntry.getPath();
		} else {
			currPath= currPath.removeLastSegments(1);
		}
	
		DirectoryDialog dialog= new DirectoryDialog(getShell());
		dialog.setMessage(NewWizardMessages.NativeLibrariesDialog_external_message);
		dialog.setText(NewWizardMessages.NativeLibrariesDialog_extfiledialog_text);
		dialog.setFilterPath(currPath.toOSString());
		String res= dialog.open();
		if (res != null) {
			return res;
		}
		return null;
	}
	
	/*
	 * Opens a dialog to choose an internal file.
	 */	
	private String chooseInternal() {
		String initSelection= fPathField.getText();
		
		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new WorkbenchContentProvider();
		Class[] acceptedClasses= new Class[] { IProject.class, IFolder.class };
		TypedElementSelectionValidator validator= new TypedElementSelectionValidator(acceptedClasses, true);
		ViewerFilter filter= new TypedViewerFilter(acceptedClasses);

		IResource initSel= null;
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		if (initSelection.length() > 0) {
			initSel= root.findMember(new Path(initSelection));
		}
		if (initSel == null) {
			initSel= root.findMember(fEntry.getPath());
		}

		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), lp, cp);
		dialog.setAllowMultiple(false);
		dialog.setValidator(validator);
		dialog.addFilter(filter);
		dialog.setTitle(NewWizardMessages.NativeLibrariesDialog_intfiledialog_title); 
		dialog.setMessage(NewWizardMessages.NativeLibrariesDialog_intfiledialog_message); 
		dialog.setInput(root);
		dialog.setInitialSelection(initSel);
		if (dialog.open() == Window.OK) {
			IResource res= (IResource) dialog.getFirstResult();
			return res.getFullPath().makeRelative().toString();
		}
		return null;
	}

}

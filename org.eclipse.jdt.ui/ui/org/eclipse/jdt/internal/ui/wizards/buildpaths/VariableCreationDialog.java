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

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.IUIConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

public class VariableCreationDialog extends StatusDialog {
		
	private IDialogSettings fDialogSettings;
	
	private StringDialogField fNameField;
	private StatusInfo fNameStatus;	
	
	private StringButtonDialogField fPathField;
	private StatusInfo fPathStatus;
	private SelectionButtonDialogField fDirButton;
		
	private CPVariableElement fElement;
	
	private List fExistingNames;
		
	public VariableCreationDialog(Shell parent, CPVariableElement element, List existingNames) {
		super(parent);
		if (element == null) {
			setTitle(NewWizardMessages.getString("VariableCreationDialog.titlenew")); //$NON-NLS-1$
		} else {
			setTitle(NewWizardMessages.getString("VariableCreationDialog.titleedit")); //$NON-NLS-1$
		}
		
		fDialogSettings= JavaPlugin.getDefault().getDialogSettings();
		
		fElement= element;
		
		fNameStatus= new StatusInfo();
		fPathStatus= new StatusInfo();
		
		NewVariableAdapter adapter= new NewVariableAdapter();
		fNameField= new StringDialogField();
		fNameField.setDialogFieldListener(adapter);
		fNameField.setLabelText(NewWizardMessages.getString("VariableCreationDialog.name.label")); //$NON-NLS-1$

		fPathField= new StringButtonDialogField(adapter);
		fPathField.setDialogFieldListener(adapter);
		fPathField.setLabelText(NewWizardMessages.getString("VariableCreationDialog.path.label")); //$NON-NLS-1$
		fPathField.setButtonLabel(NewWizardMessages.getString("VariableCreationDialog.path.file.button")); //$NON-NLS-1$
		
		fDirButton= new SelectionButtonDialogField(SWT.PUSH);
		fDirButton.setDialogFieldListener(adapter);
		fDirButton.setLabelText(NewWizardMessages.getString("VariableCreationDialog.path.dir.button")); //$NON-NLS-1$
		
		fExistingNames= existingNames;
		
		if (element != null) {
			fNameField.setText(element.getName());
			fPathField.setText(element.getPath().toString());
			fExistingNames.remove(element.getName());
		} else {
			fNameField.setText(""); //$NON-NLS-1$
			fPathField.setText(""); //$NON-NLS-1$
		}
	}
	
	/*
	 * @see Windows#configureShell
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		WorkbenchHelp.setHelp(newShell, IJavaHelpContextIds.VARIABLE_CREATION_DIALOG);
	}
	

	public CPVariableElement getClasspathElement() {
		return new CPVariableElement(fNameField.getText(), new Path(fPathField.getText()), false);
	}

	/*
	 * @see Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */	
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite)super.createDialogArea(parent);
		
		PixelConverter converter= new PixelConverter(parent);
		
		Composite inner= new Composite(composite, SWT.NONE);
		
		GridLayout layout= new GridLayout();
		layout.marginWidth= 0;
		layout.numColumns= 3;
		inner.setLayout(layout);
		
		int fieldWidthHint= converter.convertWidthInCharsToPixels(50);
		
		fNameField.doFillIntoGrid(inner, 2);
		LayoutUtil.setWidthHint(fNameField.getTextControl(null), fieldWidthHint);
		LayoutUtil.setHorizontalGrabbing(fNameField.getTextControl(null));
		
		DialogField.createEmptySpace(inner, 1);
		
		fPathField.doFillIntoGrid(inner, 3);
		LayoutUtil.setWidthHint(fPathField.getTextControl(null), fieldWidthHint);
		
		DialogField.createEmptySpace(inner, 2);
		fDirButton.doFillIntoGrid(inner, 1);
		
		DialogField focusField= (fElement == null) ? fNameField : fPathField;
		focusField.postSetFocusOnDialogField(parent.getDisplay());
		applyDialogFont(composite);		
		return composite;
	}

		
	// -------- NewVariableAdapter --------

	private class NewVariableAdapter implements IDialogFieldListener, IStringButtonAdapter {
		
		// -------- IDialogFieldListener
		public void dialogFieldChanged(DialogField field) {
			doFieldUpdated(field);
		}
		
		// -------- IStringButtonAdapter
		public void changeControlPressed(DialogField field) {
			doChangeControlPressed(field);
		}
	}
	
	private void doChangeControlPressed(DialogField field) {
		if (field == fPathField) {
			IPath path= chooseExtJarFile();
			if (path != null) {
				fPathField.setText(path.toString());
			}
		}
	}
	
	private void doFieldUpdated(DialogField field) {	
		if (field == fNameField) {
			fNameStatus= nameUpdated();
		} else if (field == fPathField) {
			fPathStatus= pathUpdated();
		} else if (field == fDirButton) {
			IPath path= chooseExtDirectory();
			if (path != null) {
				fPathField.setText(path.toString());
			}
		}		
		updateStatus(StatusUtil.getMoreSevere(fPathStatus, fNameStatus));
	}		
	
	private StatusInfo nameUpdated() {
		StatusInfo status= new StatusInfo();
		String name= fNameField.getText();
		if (name.length() == 0) {
			status.setError(NewWizardMessages.getString("VariableCreationDialog.error.entername")); //$NON-NLS-1$
			return status;
		}
		if (name.trim().length() != name.length()) {
			status.setError(NewWizardMessages.getString("VariableCreationDialog.error.whitespace")); //$NON-NLS-1$
		} else if (!Path.ROOT.isValidSegment(name)) {
			status.setError(NewWizardMessages.getString("VariableCreationDialog.error.invalidname")); //$NON-NLS-1$
		} else if (nameConflict(name)) {
			status.setError(NewWizardMessages.getString("VariableCreationDialog.error.nameexists")); //$NON-NLS-1$
		}
		return status;
	}
	
	private boolean nameConflict(String name) {
		if (fElement != null && fElement.getName().equals(name)) {
			return false;
		}
		for (int i= 0; i < fExistingNames.size(); i++) {
			CPVariableElement elem= (CPVariableElement)fExistingNames.get(i);
			if (name.equals(elem.getName())){
				return true;
			}
		}
		return false;
	}
	
	
	private StatusInfo pathUpdated() {
		StatusInfo status= new StatusInfo();
		
		String path= fPathField.getText();
		if (path.length() > 0) { // empty path is ok
			if (!Path.ROOT.isValidPath(path)) {
				status.setError(NewWizardMessages.getString("VariableCreationDialog.error.invalidpath")); //$NON-NLS-1$
			} else if (!new File(path).exists()) {
				status.setWarning(NewWizardMessages.getString("VariableCreationDialog.warning.pathnotexists")); //$NON-NLS-1$
			}
		}
		return status;
	}
	
	
	private String getInitPath() {
		String initPath= fPathField.getText();
		if (initPath.length() == 0) {		
			initPath= fDialogSettings.get(IUIConstants.DIALOGSTORE_LASTEXTJAR);
			if (initPath == null) {
				initPath= ""; //$NON-NLS-1$
			}
		} else {
			IPath entryPath= new Path(initPath);
			if (ArchiveFileFilter.isArchivePath(entryPath)) {
				entryPath.removeLastSegments(1);
			}
			initPath= entryPath.toOSString();
		}
		return initPath;
	}		
	
	
	/*
	 * Open a dialog to choose a jar from the file system
	 */
	private IPath chooseExtJarFile() {
		String initPath= getInitPath();
		
		FileDialog dialog= new FileDialog(getShell());
		dialog.setText(NewWizardMessages.getString("VariableCreationDialog.extjardialog.text")); //$NON-NLS-1$
		dialog.setFilterExtensions(new String[] {"*.jar;*.zip"}); //$NON-NLS-1$
		dialog.setFilterPath(initPath);
		String res= dialog.open();
		if (res != null) {
			fDialogSettings.put(IUIConstants.DIALOGSTORE_LASTEXTJAR, dialog.getFilterPath());
			return Path.fromOSString(res).makeAbsolute();
		}
		return null;
	}
	
	private IPath chooseExtDirectory() {
		String initPath= getInitPath();
		
		DirectoryDialog dialog= new DirectoryDialog(getShell());
		dialog.setText(NewWizardMessages.getString("VariableCreationDialog.extdirdialog.text")); //$NON-NLS-1$
		dialog.setMessage(NewWizardMessages.getString("VariableCreationDialog.extdirdialog.message")); //$NON-NLS-1$
		dialog.setFilterPath(initPath);
		String res= dialog.open();
		if (res != null) {
			fDialogSettings.put(IUIConstants.DIALOGSTORE_LASTEXTJAR, dialog.getFilterPath());
			return Path.fromOSString(res);
		}
		return null;		
	}
	
		
	
}

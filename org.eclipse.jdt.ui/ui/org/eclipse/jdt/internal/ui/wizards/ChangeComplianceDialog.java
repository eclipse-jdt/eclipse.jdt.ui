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
package org.eclipse.jdt.internal.ui.wizards;

import java.util.Hashtable;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.util.CoreUtility;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;

/**
 * 
 */
public class ChangeComplianceDialog extends MessageDialog {

	private SelectionButtonDialogField fChangeProject;
	private SelectionButtonDialogField fChangeWorkspace;
	private final IJavaProject fProject;

	public ChangeComplianceDialog(Shell parentShell, IJavaProject project) {
		super(parentShell, getTitle(), null, getMessage(), QUESTION, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL }, 0);
		fProject= project;
		fChangeProject= new SelectionButtonDialogField(SWT.RADIO);
		fChangeProject.setLabelText(NewWizardMessages.getString("ChangeComplianceDialog.project.selection"));  //$NON-NLS-1$
		
		fChangeWorkspace= new SelectionButtonDialogField(SWT.RADIO);
		fChangeWorkspace.setLabelText(NewWizardMessages.getString("ChangeComplianceDialog.workspace.selection"));  //$NON-NLS-1$
	}
	
	private static String getTitle() {
		return NewWizardMessages.getString("ChangeComplianceDialog.title");  //$NON-NLS-1$
	}
	
	private static String getMessage() {
		return NewWizardMessages.getString("ChangeComplianceDialog.message");  //$NON-NLS-1$
	}
	

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.MessageDialog#createCustomArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createCustomArea(Composite parent) {
		GridLayout layout= new GridLayout();
		
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(layout);
		
		fChangeProject.doFillIntoGrid(composite, 1);
		fChangeWorkspace.doFillIntoGrid(composite, 1);
		return composite;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#open()
	 */
	public int open() {
		int res= super.open();
		if (res == OK) {
			if (fChangeProject.isSelected()) {
				Map map= fProject.getOptions(false);
				JavaModelUtil.set50CompilanceOptions(map);
				fProject.setOptions(map);
			} else {
				Hashtable map= JavaCore.getOptions();
				JavaModelUtil.set50CompilanceOptions(map);
				JavaCore.setOptions(map);
			}
			CoreUtility.startBuildInBackground(fProject.getProject());
		}
		return res;
	}
	
	

}

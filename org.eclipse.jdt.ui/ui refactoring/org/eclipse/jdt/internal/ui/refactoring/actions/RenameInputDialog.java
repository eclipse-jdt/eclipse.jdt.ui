/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdatingRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

class RenameInputDialog extends InputDialog{
		
	private final IRenameRefactoring fRefactoring;
	
	public RenameInputDialog(Shell parentShell, String dialogTitle, String dialogMessage, String initialValue, IInputValidator validator, IRenameRefactoring refactoring) {
		super(parentShell, dialogTitle, dialogMessage, initialValue, validator);
		Assert.isNotNull(refactoring);
		fRefactoring= refactoring;
	}

	/* (non-javadoc)
	 * @see Dialog#createDialogArea(Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite)super.createDialogArea(parent);
		
		if (! (fRefactoring instanceof IReferenceUpdatingRefactoring))
			return composite;
		
		final IReferenceUpdatingRefactoring ref= (IReferenceUpdatingRefactoring)fRefactoring;		
		if (! ref.canEnableUpdateReferences())	
			return composite;
		
		final Button checkBox= new Button(composite, SWT.CHECK);
		checkBox.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		checkBox.setText(RefactoringMessages.getString("RenameInputDialog.update_references"));		 //$NON-NLS-1$
		checkBox.setSelection(ref.getUpdateReferences());
		ref.setUpdateReferences(checkBox.getSelection());
			checkBox.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					ref.setUpdateReferences(checkBox.getSelection());
				}
		});		
		return composite;
	}

}
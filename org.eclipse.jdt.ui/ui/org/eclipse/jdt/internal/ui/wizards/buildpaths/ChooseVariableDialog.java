/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;

import org.eclipse.jdt.internal.ui.dialogs.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;public class ChooseVariableDialog extends StatusDialog implements IStatusChangeListener, IDoubleClickListener {

	private VariableBlock fVariableBlock;
			
	public ChooseVariableDialog(Shell parent, String lastVariableSelection) {
		super(parent);
		setTitle(NewWizardMessages.getString("ChooseVariableDialog.variabledialog.title")); //$NON-NLS-1$
		fVariableBlock= new VariableBlock(this, true, lastVariableSelection);
	}
			
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite)super.createDialogArea(parent);
		fVariableBlock.createContents(composite);
		fVariableBlock.addDoubleClickListener(this);
		return composite;
	}
	
	protected void okPressed() {
		fVariableBlock.performOk();
		super.okPressed();
	}
	
	public String getSelectedVariable() {
		return fVariableBlock.getSelectedVariable();
	}
	
	/**
 	 * @see IStatusChangeListener#statusChanged(IStatus)
 	 */
	public void statusChanged(IStatus status) {
		updateStatus(status);
		
	}
	
	/**
 	 * @see IDoubleClickListener#doubleClick(DoubleClickEvent)
 	 */
	public void doubleClick(DoubleClickEvent event) {
		if (getStatus().isOK()) {
			okPressed();
		}
	}

}
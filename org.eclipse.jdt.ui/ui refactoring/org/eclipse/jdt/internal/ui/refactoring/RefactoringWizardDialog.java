/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;

/**
 * A dialog to host refactoring wizards.
 */
public class RefactoringWizardDialog extends WizardDialog {

	/**
	 * Creates a new refactoring wizard dialag with the given wizard.
	 */
	public RefactoringWizardDialog(Shell parent, IWizard wizard) {
		super(parent, wizard);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		setMinimumPageSize(600, 400);
	}	
}
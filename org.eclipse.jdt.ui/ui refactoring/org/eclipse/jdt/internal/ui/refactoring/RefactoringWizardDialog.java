/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * A dialog to host refactoring wizards.
 */
public class RefactoringWizardDialog extends WizardDialog {

	private static final String DIALOG_SETTINGS= "RefactoringWizard";
	private static final String WIDTH= "width";
	private static final String HEIGHT= "height";

	private IDialogSettings fSettings;

	/**
	 * Creates a new refactoring wizard dialag with the given wizard.
	 */
	public RefactoringWizardDialog(Shell parent, IWizard wizard) {
		super(parent, wizard);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings();
		fSettings= settings.getSection(DIALOG_SETTINGS);
		if (fSettings == null) {
			fSettings= new DialogSettings(DIALOG_SETTINGS);
			settings.addSection(fSettings);
			fSettings.put(WIDTH, 600);
			fSettings.put(HEIGHT, 400);
		}
		int width= 600;
		int height= 400;
		try {
			width= fSettings.getInt(WIDTH);
			height= fSettings.getInt(HEIGHT);
		} catch (NumberFormatException e) {
		}
		setMinimumPageSize(width, height);
	}
	
	/*
	 * @see WizardDialog#finishPressed()
	 */
	protected void finishPressed() {
		IWizardPage page= getCurrentPage();
		Control control= page.getControl().getParent();
		Point size = control.getSize();
		fSettings.put(WIDTH, size.x);
		fSettings.put(HEIGHT, size.y);
		super.finishPressed();
	}	
}
/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.jface.dialogs.ErrorDialog;

import org.eclipse.core.resources.IWorkspace;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class NewClassCreationWizard extends NewElementWizard {

	private NewClassCreationWizardPage fPage;
	private static final String WIZARD_TITLE= "NewClassCreationWizard.title";

	public NewClassCreationWizard() {
		super();
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWCLASS);
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
		setWindowTitle(JavaPlugin.getResourceString(WIZARD_TITLE));
	}

	/**
	 * @see Wizard#createPages
	 */	
	public void addPages() {
		super.addPages();
		IWorkspace workspace= JavaPlugin.getWorkspace();
		fPage= new NewClassCreationWizardPage(workspace.getRoot());
		addPage(fPage);
		fPage.init(getSelection());
	}	

	/**
	 * @see Wizard#performFinish
	 */		
	public boolean performFinish() {
		if (fPage.finishPage()) {
			ICompilationUnit cu= fPage.getNewType().getCompilationUnit();
			if (cu.isWorkingCopy()) {
				cu= (ICompilationUnit)cu.getOriginalElement();
			}	
			revealSelection(cu);
			try {
				openResource(cu.getUnderlyingResource());
			} catch (JavaModelException e) {
				ErrorDialog.openError(getShell(), "Error", null, e.getStatus());
			}
			return true;
		}
		return false;		
	}	
	
}
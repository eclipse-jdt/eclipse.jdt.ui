/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.jface.dialogs.ErrorDialog;

import org.eclipse.core.resources.IResource;import org.eclipse.core.resources.IWorkspace;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class NewInterfaceCreationWizard extends NewElementWizard {

	private static final String WIZARD_TITLE= "NewInterfaceCreationWizard.title";

	private NewInterfaceCreationWizardPage fPage;
	
	public NewInterfaceCreationWizard() {
		super();
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWINT);
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
		setWindowTitle(JavaPlugin.getResourceString(WIZARD_TITLE));
	}

	/**
	 * @see Wizard#addPages
	 */	
	public void addPages() {
		super.addPages();		
		IWorkspace workspace= JavaPlugin.getWorkspace();
		fPage= new NewInterfaceCreationWizardPage(workspace.getRoot());
		addPage(fPage);
		fPage.init(getSelection());	
	}

	/**
	 * @see Wizard#performFinish
	 */		
	public boolean performFinish() {
		if (finishPage(fPage)) {
			ICompilationUnit cu= fPage.getCreatedType().getCompilationUnit();
			if (cu.isWorkingCopy()) {
				cu= (ICompilationUnit)cu.getOriginalElement();
			}	

			try {
				IResource resource= cu.getUnderlyingResource();
				selectAndReveal(resource);
				openResource(resource);
			} catch (JavaModelException e) {
				JavaPlugin.log(e.getStatus());
				ErrorDialog.openError(getShell(), "Error", null, e.getStatus());
			}
			return true;
		}
		return false;
	}	
	
}
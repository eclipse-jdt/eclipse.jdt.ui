/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;

import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;


public class NewPackageRootCreationWizard extends NewElementWizard {

	private NewPackageRootCreationWizardPage fPage;

	public NewPackageRootCreationWizard() {
		super();
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWSRCFOLDR);
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
		setWindowTitle(NewWizardMessages.getString("NewPackageRootCreationWizard.title")); //$NON-NLS-1$
	}

	/**
	 * @see Wizard#addPages
	 */	
	public void addPages() {
		super.addPages();
		IWorkspace workspace= JavaPlugin.getWorkspace();
		fPage= new NewPackageRootCreationWizardPage(workspace.getRoot());
		addPage(fPage);
		fPage.init(getSelection());
	}			

	/**
	 * @see Wizard#performFinish
	 */		
	public boolean performFinish() {
		if (finishPage(fPage.getRunnable())) {
			IPackageFragmentRoot root= fPage.getNewPackageFragmentRoot();
			try {
				IResource resource= root.getUnderlyingResource();
				selectAndReveal(resource);
				openResource(resource);
			} catch (JavaModelException e) {
				JavaPlugin.log(e.getStatus());
				// let pass, only reveal and open will fail
			}
			return true;
		}
		return false;		
	}	
	
}
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class NewClassCreationWizard extends NewElementWizard {

	private NewClassCreationWizardPage fPage;

	public NewClassCreationWizard() {
		super();
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWCLASS);
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
		setWindowTitle(NewWizardMessages.getString("NewClassCreationWizard.title")); //$NON-NLS-1$
	}

	/*
	 * @see Wizard#createPages
	 */	
	public void addPages() {
		super.addPages();
		IWorkspace workspace= JavaPlugin.getWorkspace();
		fPage= new NewClassCreationWizardPage(workspace.getRoot());
		addPage(fPage);
		fPage.init(getSelection());
	}	

	/*
	 * @see Wizard#performFinish
	 */		
	public boolean performFinish() {
		if (finishPage(fPage.getRunnable())) {
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
				// let pass, only reveal and open will fail
			}
			return true;
		}
		return false;		
	}	
	
}
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.ui.wizards.NewInterfaceWizardPage;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class NewInterfaceCreationWizard extends NewElementWizard {

	private NewInterfaceWizardPage fPage;
	
	public NewInterfaceCreationWizard() {
		super();
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWINT);
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
		setWindowTitle(NewWizardMessages.getString("NewInterfaceCreationWizard.title")); //$NON-NLS-1$
	}

	/*
	 * @see Wizard#addPages
	 */	
	public void addPages() {
		super.addPages();		
		fPage= new NewInterfaceWizardPage();
		addPage(fPage);
		fPage.init(getSelection());	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.wizards.NewElementWizard#finishPage(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void finishPage(IProgressMonitor monitor) throws InterruptedException, CoreException {
		fPage.createType(monitor); // use the full progress monitor
		ICompilationUnit cu= JavaModelUtil.toOriginal(fPage.getCreatedType().getCompilationUnit());
		if (cu != null) {
			IResource resource= cu.getResource();
			selectAndReveal(resource);
			openResource((IFile) resource);
		}	
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#performFinish()
	 */
	public boolean performFinish() {
		warnAboutTypeCommentDeprecation();
		return super.performFinish();
	}
	
	
}
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
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.ui.wizards.NewPackageWizardPage;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class NewPackageCreationWizard extends NewElementWizard {

	private NewPackageWizardPage fPage;

	public NewPackageCreationWizard() {
		super();
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWPACK);
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
		setWindowTitle(NewWizardMessages.getString("NewPackageCreationWizard.title")); //$NON-NLS-1$
	}

	/*
	 * @see Wizard#addPages
	 */	
	public void addPages() {
		super.addPages();
		fPage= new NewPackageWizardPage();
		addPage(fPage);
		fPage.init(getSelection());
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.wizards.NewElementWizard#finishPage(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void finishPage(IProgressMonitor monitor) throws InterruptedException, CoreException {
		fPage.createPackage(monitor); // use the full progress monitor
		selectAndReveal(fPage.getModifiedResource());
	}
	
}

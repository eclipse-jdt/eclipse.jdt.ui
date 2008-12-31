/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.ui.wizards.NewPackageWizardPage;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class NewPackageCreationWizard extends NewElementWizard {

	private NewPackageWizardPage fPage;

	public NewPackageCreationWizard(NewPackageWizardPage page) {
		super();
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWPACK);
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
		setWindowTitle(NewWizardMessages.NewPackageCreationWizard_title);

		fPage= page;
	}

	public NewPackageCreationWizard() {
		this(null);
	}

	/*
	 * @see Wizard#addPages
	 */
	public void addPages() {
		super.addPages();
		if (fPage == null) {
			fPage= new NewPackageWizardPage();
			fPage.init(getSelection());
		}
		addPage(fPage);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.wizards.NewElementWizard#finishPage(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void finishPage(IProgressMonitor monitor) throws InterruptedException, CoreException {
		fPage.createPackage(monitor); // use the full progress monitor
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#performFinish()
	 */
	public boolean performFinish() {
		boolean res= super.performFinish();
		if (res) {
			selectAndReveal(fPage.getModifiedResource());
		}
		return res;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.wizards.NewElementWizard#getCreatedElement()
	 */
	public IJavaElement getCreatedElement() {
		return fPage.getNewPackageFragment();
	}

}

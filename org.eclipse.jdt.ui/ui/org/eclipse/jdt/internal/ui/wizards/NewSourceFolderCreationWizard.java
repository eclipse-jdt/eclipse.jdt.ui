/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;


public class NewSourceFolderCreationWizard extends NewElementWizard {

	private NewSourceFolderWizardPage fPage;

	public NewSourceFolderCreationWizard() {
		super();
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWSRCFOLDR);
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
		setWindowTitle(NewWizardMessages.NewSourceFolderCreationWizard_title);
	}

	/*
	 * @see Wizard#addPages
	 */
	@Override
	public void addPages() {
		super.addPages();
		fPage= new NewSourceFolderWizardPage();
		addPage(fPage);
		fPage.init(getSelection());
	}

	@Override
	protected void finishPage(IProgressMonitor monitor) throws InterruptedException, CoreException {
		fPage.createPackageFragmentRoot(monitor); // use the full progress monitor
	}

	@Override
	public boolean performFinish() {
		boolean res= super.performFinish();
		if (res) {
			IResource resource= fPage.getCorrespondingResource();
			selectAndReveal(resource);
			if (resource instanceof IFile && resource.getName().equals(JavaModelUtil.MODULE_INFO_JAVA)) {
				openResource((IFile) resource);
			}
		}
		return res;
	}

	@Override
	public IJavaElement getCreatedElement() {
		return fPage.getNewPackageFragmentRoot();
	}

}

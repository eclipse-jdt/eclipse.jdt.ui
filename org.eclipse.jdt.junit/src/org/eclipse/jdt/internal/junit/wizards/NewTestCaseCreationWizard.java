/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.wizards;

import java.net.MalformedURLException;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.junit.wizards.NewTestCaseWizardPageOne;
import org.eclipse.jdt.junit.wizards.NewTestCaseWizardPageTwo;

import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;

/**
 * A wizard for creating test cases.
 */
public class NewTestCaseCreationWizard extends JUnitWizard {

	private NewTestCaseWizardPageOne fPage1;
	private NewTestCaseWizardPageTwo fPage2;

	public NewTestCaseCreationWizard() {
		super();
		setWindowTitle(WizardMessages.Wizard_title_new_testcase); 
		initDialogSettings();
	}

	protected void initializeDefaultPageImageDescriptor() {
		try {
			ImageDescriptor id= ImageDescriptor.createFromURL(JUnitPlugin.makeIconFileURL("wizban/newtest_wiz.gif")); //$NON-NLS-1$
			setDefaultPageImageDescriptor(id);
		} catch (MalformedURLException e) {
			// Should not happen.  Ignore.
		}
	}

	/*
	 * @see Wizard#createPages
	 */	
	public void addPages() {
		super.addPages();
		fPage2= new NewTestCaseWizardPageTwo();
		fPage1= new NewTestCaseWizardPageOne(fPage2);
		addPage(fPage1);
		fPage1.init(getSelection());
		addPage(fPage2);
	}	
	
	/*
	 * @see Wizard#performFinish
	 */		
	public boolean performFinish() {
		if (finishPage(fPage1.getRunnable())) {
			IType newClass= fPage1.getCreatedType();
		
			IResource resource= newClass.getCompilationUnit().getResource();
			if (resource != null) {
				selectAndReveal(resource);
				openResource(resource);
			}
			return true;
		}
		return false;		
	}
}

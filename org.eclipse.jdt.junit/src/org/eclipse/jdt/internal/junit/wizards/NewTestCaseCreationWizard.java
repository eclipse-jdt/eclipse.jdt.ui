/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.wizards;

import java.net.MalformedURLException;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * A wizard for creating test cases.
 */
public class NewTestCaseCreationWizard extends JUnitWizard {

	private NewTestCaseCreationWizardPage fPage;
	private NewTestCaseCreationWizardPage2 fPage2;

	public NewTestCaseCreationWizard() {
		super();
		setWindowTitle(WizardMessages.getString("Wizard.title.new.testcase")); //$NON-NLS-1$
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
		fPage= new NewTestCaseCreationWizardPage();
		fPage2= new NewTestCaseCreationWizardPage2(fPage);
		addPage(fPage);
		fPage.init(getSelection(), fPage2);
		addPage(fPage2);
	}	
	
	/*
	 * @see Wizard#performFinish
	 */		
	public boolean performFinish() {
		if (finishPage(fPage.getRunnable())) {
			IType newClass= fPage.getCreatedType();

			ICompilationUnit cu= newClass.getCompilationUnit();				

			if (cu.isWorkingCopy()) {
				cu= (ICompilationUnit)cu.getOriginalElement();
			}	
			IResource resource= cu.getResource();
			if (resource != null) {
				selectAndReveal(resource);
				openResource(resource);
			}
			fPage.saveWidgetValues();
			fPage2.saveWidgetValues();
			
			return true;
		}
		return false;		
	}
}

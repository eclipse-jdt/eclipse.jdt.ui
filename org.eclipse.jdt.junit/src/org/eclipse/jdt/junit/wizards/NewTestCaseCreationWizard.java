/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.junit.wizards;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;

/**
 * A wizard for creating test cases.
 */
public class NewTestCaseCreationWizard extends JUnitWizard {

	private NewTestCaseCreationWizardPage fPage;
	private NewTestCaseCreationWizardPage2 fPage2;
	
	public NewTestCaseCreationWizard() {
		super();
		//setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWCLASS);
		//setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
		setWindowTitle("JUnit TestCase");
	}

	/*
	 * @see Wizard#createPages
	 */	
	public void addPages() {
		super.addPages();
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		fPage= new NewTestCaseCreationWizardPage();
		fPage2= new NewTestCaseCreationWizardPage2(fPage);
		addPage(fPage);
		fPage.init(getSelection(),fPage2);
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
				//added here
			}	
			try {
				IResource resource= cu.getUnderlyingResource();
				selectAndReveal(resource);
				openResource(resource);
			} catch (JavaModelException e) {
				// let pass, only reveal and open will fail
				JUnitPlugin.log(e);
			}
			return true;
		}
		return false;		
	}
}
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
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;

/**
 * A wizard for creating test cases.
 */
public class NewTestCaseCreationWizard extends JUnitWizard {

	private NewTestCaseCreationWizardPage fPage;
	private NewTestCaseCreationWizardPage2 fPage2;

	private static String DIALOG_SETTINGS_KEY= "NewTestCaseCreationWizardPage"; //$NON-NLS-1$
	
	public NewTestCaseCreationWizard() {
		super();
		//setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWCLASS);
		//setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
		setWindowTitle(Messages.getString("Wizard.title.new")); //$NON-NLS-1$
		IDialogSettings pluginSettings= JUnitPlugin.getDefault().getDialogSettings();
		IDialogSettings wizardSettings= pluginSettings.getSection(DIALOG_SETTINGS_KEY);
		if (wizardSettings == null) {
			wizardSettings= new DialogSettings(DIALOG_SETTINGS_KEY);
			pluginSettings.addSection(wizardSettings);
		}
		setDialogSettings(wizardSettings);
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
			}	
			try {
				IResource resource= cu.getUnderlyingResource();
				selectAndReveal(resource);
				openResource(resource);
			} catch (JavaModelException e) {
				JUnitPlugin.log(e);
				// let pass, only reveal and open will fail
			}
			fPage.saveWidgetValues();
			
			return true;
		}
		return false;		
	}
}
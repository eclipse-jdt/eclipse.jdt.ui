/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.core.resources.IWorkspaceRoot;

import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPage;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.launcher.VMWizardPage;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class NewProjectCreationWizard extends NewElementWizard {

	public static final String NEW_PROJECT_WIZARD_ID= "org.eclipse.jdt.ui.wizards.NewProjectCreationWizard";
		
	private static final String WIZARD_TITLE= "NewProjectCreationWizard.title";
	private static final String PREFIX_OP_ERROR= "NewProjectCreationWizard.op_error.";
	
	private static final String WZ_TITLE= "NewProjectCreationWizard.MainPage.title";
	private static final String WZ_DESC= "NewProjectCreationWizard.MainPage.description";

	private NewJavaProjectWizardPage fJavaPage;
	private WizardNewProjectCreationPage fMainPage;
	private VMWizardPage fVMPage;

	public NewProjectCreationWizard() {
		super();
		
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWJPRJ);
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
		setWindowTitle(JavaPlugin.getResourceString(WIZARD_TITLE));
	}

	/**
	 * @see Wizard#addPages
	 */	
	public void addPages() {
		super.addPages();
		fMainPage= new WizardNewProjectCreationPage("id");
		fMainPage.setTitle(JavaPlugin.getResourceString(WZ_TITLE));
		fMainPage.setDescription(JavaPlugin.getResourceString(WZ_DESC));
		addPage(fMainPage);
		IWorkspaceRoot root= JavaPlugin.getWorkspace().getRoot();
		fJavaPage= new NewJavaProjectWizardPage(root, fMainPage);
		addPage(fJavaPage);
	}		
	

	/**
	 * @see Wizard#performFinish
	 */		
	public boolean performFinish() {
		IRunnableWithProgress op= new WorkspaceModifyDelegatingOperation(fJavaPage.getRunnable());
		try {
			getContainer().run(true, true, op);
		} catch (InvocationTargetException e) {
			if (!ExceptionHandler.handle(e.getTargetException(), getShell(), JavaPlugin.getResourceBundle(), PREFIX_OP_ERROR)) {
				MessageDialog.openError(getShell(), "Error", e.getTargetException().getMessage());
			}
			return false;
		} catch  (InterruptedException e) {
			return false;
		}
		revealSelection(fJavaPage.getNewJavaProject());
		return true;
	}
		
}
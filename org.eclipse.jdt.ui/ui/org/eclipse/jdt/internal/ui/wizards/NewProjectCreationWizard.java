/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPage;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class NewProjectCreationWizard extends NewElementWizard implements IExecutableExtension {

	public static final String NEW_PROJECT_WIZARD_ID= "org.eclipse.jdt.ui.wizards.NewProjectCreationWizard"; //$NON-NLS-1$
		
	private NewJavaProjectWizardPage fJavaPage;
	private WizardNewProjectCreationPage fMainPage;
	private IConfigurationElement fConfigElement;

	public NewProjectCreationWizard() {
		super();
		
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWJPRJ);
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
		setWindowTitle(NewWizardMessages.getString("NewProjectCreationWizard.title")); //$NON-NLS-1$
	}

	/**
	 * @see Wizard#addPages
	 */	
	public void addPages() {
		super.addPages();
		fMainPage= new WizardNewProjectCreationPage("id"); //$NON-NLS-1$
		fMainPage.setTitle(NewWizardMessages.getString("NewProjectCreationWizard.MainPage.title")); //$NON-NLS-1$
		fMainPage.setDescription(NewWizardMessages.getString("NewProjectCreationWizard.MainPage.description")); //$NON-NLS-1$
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
			getContainer().run(false, true, op);
		} catch (InvocationTargetException e) {
			String title= NewWizardMessages.getString("NewProjectCreationWizard.op_error.title"); //$NON-NLS-1$
			String message= NewWizardMessages.getString("NewProjectCreationWizard.op_error.message");			 //$NON-NLS-1$
			ExceptionHandler.handle(e, getShell(), title, message);
			return false;
		} catch  (InterruptedException e) {
			return false;
		}
		BasicNewProjectResourceWizard.updatePerspective(fConfigElement);
		selectAndReveal(fJavaPage.getNewJavaProject().getProject());
		return true;
	}
		
	/**
	 * Stores the configuration element for the wizard.  The config element will be used
	 * in <code>performFinish</code> to set the result perspective.
	 */
	public void setInitializationData(IConfigurationElement cfig, String propertyName, Object data) {
		fConfigElement= cfig;
	}
}
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.exampleprojects;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.ui.INewWizard;
import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPage;

public class ExampleProjectCreationWizard extends BasicNewResourceWizard implements INewWizard, IExecutableExtension {

	private WizardNewProjectCreationPage fMainPage;
	private IConfigurationElement fConfigElement;
	
	private NewJavaProjectWizardPage fJavaProjectPage;

	public ExampleProjectCreationWizard() {
		super();
		setDialogSettings(ExampleProjectsPlugin.getDefault().getDialogSettings());
		setWindowTitle(ExampleProjectMessages.getString("ExampleProjectCreationWizard.title"));		 //$NON-NLS-1$
		setNeedsProgressMonitor(true);
	}
	
	/*
	 * @see BasicNewResourceWizard#initializeDefaultPageImageDescriptor
	 */
	protected void initializeDefaultPageImageDescriptor() {
		ImageDescriptor desc= ExampleProjectsPlugin.getDefault().getImageDescriptor("wizban/newjprjex_wiz.gif"); //$NON-NLS-1$
		setDefaultPageImageDescriptor(desc);
	}

	/**
	 * @see Wizard#addPages
	 */	
	public void addPages() {
		super.addPages();
		fMainPage= new WizardNewProjectCreationPage("id"); //$NON-NLS-1$
		fMainPage.setTitle(getConfigurationText("pagetitle")); //$NON-NLS-1$
		fMainPage.setDescription(getConfigurationText("pagedescription")); //$NON-NLS-1$

		addPage(fMainPage);
		
		IWorkspaceRoot root= ExampleProjectsPlugin.getWorkspace().getRoot();
		fJavaProjectPage= new NewJavaProjectWizardPage(root, fMainPage);
		
	}
	
	protected String getConfigurationText(String tag) {
		if (fConfigElement != null) {
			IConfigurationElement[] children = fConfigElement.getChildren(tag);
			if (children.length >= 1) {
				return children[0].getValue();
			}
		}
		return '!' + tag + '!';
	}
	
	/**
	 * @see Wizard#performFinish
	 */		
	public boolean performFinish() {
		IProject projectHandle= fMainPage.getProjectHandle();
		Shell shell= fMainPage.getControl().getShell();
		ExampleProjectCreationOperation runnable= new ExampleProjectCreationOperation(shell, projectHandle, fConfigElement, fJavaProjectPage);
		
		IRunnableWithProgress op= new WorkspaceModifyDelegatingOperation(runnable);
		try {
			getContainer().run(false, true, op);
		} catch (InvocationTargetException e) {
			handleException(e.getTargetException());
			return false;
		} catch  (InterruptedException e) {
			return false;
		}
		BasicNewProjectResourceWizard.updatePerspective(fConfigElement);
		selectAndReveal(runnable.getResourceToReveal());
		return true;
	}
	
	private void handleException(Throwable target) {
		String title= ExampleProjectMessages.getString("ExampleProjectCreationWizard.op_error.title"); //$NON-NLS-1$
		String message= ExampleProjectMessages.getString("ExampleProjectCreationWizard.op_error.message"); //$NON-NLS-1$
		if (target instanceof CoreException) {
			IStatus status= ((CoreException)target).getStatus();
			ErrorDialog.openError(getShell(), title, message, status);
			ExampleProjectsPlugin.log(status);
		} else {
			MessageDialog.openError(getShell(), title, target.getMessage());
			ExampleProjectsPlugin.log(target);
		}
	}
		
	/**
	 * Stores the configuration element for the wizard.  The config element will be used
	 * in <code>performFinish</code> to set the result perspective.
	 */
	public void setInitializationData(IConfigurationElement cfig, String propertyName, Object data) {
		fConfigElement= cfig;
	}
}
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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.ICapabilityInstallWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class JavaCapabilityWizard extends Wizard implements ICapabilityInstallWizard {

	private JavaCapabilityConfigurationPage fJavaPage;
	private IProject fProject;

	public JavaCapabilityWizard() {
		super();
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWJPRJ);
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
		setWindowTitle(NewWizardMessages.getString("JavaCapabilityWizard.title")); //$NON-NLS-1$
	}

	/*
	 * @see ICapabilityWizard#init(IWorkbench, IStructuredSelection, IProject)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection, IProject project) {
		fProject= project;
	}

	/*
	 * @see Wizard#addPages
	 */	
	public void addPages() {
		super.addPages();
		fJavaPage= new JavaCapabilityConfigurationPage();
		fJavaPage.init(JavaCore.create(fProject), null, null, false);
		addPage(fJavaPage);
	}		
	

	/*
	 * @see Wizard#performFinish
	 */		
	public boolean performFinish() {
		IRunnableWithProgress op= new WorkspaceModifyDelegatingOperation(fJavaPage.getRunnable());
		try {
			getContainer().run(true, true, op);
		} catch (InvocationTargetException e) {
			String title= NewWizardMessages.getString("JavaCapabilityWizard.op_error.title"); //$NON-NLS-1$
			String message= NewWizardMessages.getString("JavaCapabilityWizard.op_error.message");			 //$NON-NLS-1$
			ExceptionHandler.handle(e, getShell(), title, message);
			return false;
		} catch  (InterruptedException e) {
			return false;
		}
		return true;
	}
}

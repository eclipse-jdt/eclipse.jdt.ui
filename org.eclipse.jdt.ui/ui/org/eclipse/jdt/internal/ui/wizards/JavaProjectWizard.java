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
package org.eclipse.jdt.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class JavaProjectWizard extends NewElementWizard implements IExecutableExtension {
    
    private JavaProjectWizardFirstPage fFirstPage;
    private JavaProjectWizardSecondPage fSecondPage;
    
    private IConfigurationElement fConfigElement;
    protected boolean fIsAutoBuilding;
    
    public JavaProjectWizard() {
        setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWJPRJ);
        setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
        setWindowTitle(NewWizardMessages.getString("JavaProjectWizard.title")); //$NON-NLS-1$
        fIsAutoBuilding= enableAutoBuild(false);
    }

    /*
     * @see Wizard#addPages
     */	
    public void addPages() {
        super.addPages();
        fFirstPage= new JavaProjectWizardFirstPage();
        addPage(fFirstPage);
        fSecondPage= new JavaProjectWizardSecondPage(fFirstPage);
        addPage(fSecondPage);
    }		
    
    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.ui.wizards.NewElementWizard#finishPage(org.eclipse.core.runtime.IProgressMonitor)
     */
    protected void finishPage(IProgressMonitor monitor) throws InterruptedException, CoreException {
    	fSecondPage.performFinish(monitor); // use the full progress monitor
    }
    
    private boolean is15Classpath(IJavaProject javaProject) {
    	try {
    		return javaProject.findType("java.lang.Enum") != null; //$NON-NLS-1$
    	} catch (JavaModelException e) {
    		// ignore
    		return false;
    	}
    }
    
    private void checkCompliance() {
    	IJavaProject javaProject= fSecondPage.getJavaProject();
    	String projectCompatibility= javaProject.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true);
    	boolean is15Configured= JavaCore.VERSION_1_5.equals(projectCompatibility);
    	if (is15Classpath(javaProject) && !is15Configured) {
    		new ChangeComplianceDialog(getShell(), javaProject).open();
    	}
    }
    
    
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#performFinish()
	 */
	public boolean performFinish() {
		boolean res= super.performFinish();
		if (res) {
			checkCompliance();
			BasicNewProjectResourceWizard.updatePerspective(fConfigElement);
	 		selectAndReveal(fSecondPage.getJavaProject().getProject());
            enableAutoBuild(fIsAutoBuilding);
		}
		return res;
	}
    
    protected void handleFinishException(Shell shell, InvocationTargetException e) {
        String title= NewWizardMessages.getString("JavaProjectWizard.op_error.title"); //$NON-NLS-1$
        String message= NewWizardMessages.getString("JavaProjectWizard.op_error_create.message");			 //$NON-NLS-1$
        ExceptionHandler.handle(e, getShell(), title, message);
    }	
    
    /*
     * Stores the configuration element for the wizard.  The config element will be used
     * in <code>performFinish</code> to set the result perspective.
     */
    public void setInitializationData(IConfigurationElement cfig, String propertyName, Object data) {
        fConfigElement= cfig;
    }
    
    /* (non-Javadoc)
     * @see IWizard#performCancel()
     */
    public boolean performCancel() {
        fSecondPage.performCancel();
        enableAutoBuild(fIsAutoBuilding);
        return super.performCancel();
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.wizard.IWizard#canFinish()
     */
    public boolean canFinish() {
        return super.canFinish();
    }
    
    /**
     * Set the autobuild to the value of the parameter and
     * return the old one.
     * 
     * @param state the value to be set for autobuilding.
     * @return the old value of the autobuild state
     */
    private boolean enableAutoBuild(boolean state) {
        try {
            IWorkspace workspace= ResourcesPlugin.getWorkspace();
            IWorkspaceDescription desc= workspace.getDescription();
            boolean isAutoBuilding= desc.isAutoBuilding();
            if (isAutoBuilding != state)
                desc.setAutoBuilding(state);
            workspace.setDescription(desc);
            return isAutoBuilding;
        } catch (CoreException e) {
            JavaPlugin.log(e);
        }
        return true;
    }
}

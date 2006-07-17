/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;
import org.eclipse.jdt.internal.corext.buildpath.IClasspathInformationProvider;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier.IClasspathModifierListener;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;

//TODO: Use global history
public class ResetAllAction extends Action implements IClasspathModifierAction {
	
	private final IClasspathModifierListener fListener;
	private final HintTextGroup fProvider;
	private final IRunnableContext fContext;
	private IJavaProject fJavaProject;
	private List fEntries;
	private IPath fOutputLocation;

	public ResetAllAction(IJavaProject javaProject, IClasspathModifierListener listener, HintTextGroup provider, IRunnableContext context) {
		
		fJavaProject= javaProject;
		fListener= listener;
		fProvider= provider;
		fContext= context;
		
		setImageDescriptor(JavaPluginImages.DESC_ELCL_CLEAR);
		setDisabledImageDescriptor(JavaPluginImages.DESC_DLCL_CLEAR);
		setText(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_ClearAll_label);
		setToolTipText(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_ClearAll_tooltip);
		setEnabled(false);
	}
	

	public void setBreakPoint(IJavaProject javaProject) throws JavaModelException {
		fJavaProject= javaProject;
		
		fEntries= ClasspathModifier.getExistingEntries(javaProject);
		fOutputLocation= fJavaProject.getOutputLocation();
		setEnabled(true);
    }
	
	/**
	 * {@inheritDoc}
	 */
	public void run() {

		try {
	        final IRunnableWithProgress runnable= new IRunnableWithProgress() {
	        	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

        			monitor.beginTask("", 3); //$NON-NLS-1$
	        		try {
	        			if (!hasChange(fJavaProject))
	        				return;
	        			
	        			ClasspathModifier.commitClassPath(fEntries, fJavaProject, fListener, monitor);
	        			fJavaProject.setOutputLocation(fOutputLocation, monitor);
	        			
	                    fProvider.deleteCreatedResources();
	                    
	            		selectAndReveal(new StructuredSelection(fJavaProject));
	                } catch (JavaModelException e) {
	                    showExceptionDialog(e);
	                } finally {
	                	monitor.done();
	                }
	        	}
	        };
	        fContext.run(false, false, runnable);
        } catch (InvocationTargetException e) {
			if (e.getCause() instanceof CoreException) {
				showExceptionDialog((CoreException)e.getCause());
			} else {
				JavaPlugin.log(e);
			}
        } catch (InterruptedException e) {
        }
	}
	
	//TODO: Remove, action should be disabled if not hasChange
	private boolean hasChange(IJavaProject project) throws JavaModelException {
		if (!project.getOutputLocation().equals(fOutputLocation))
            return true;
      
		IClasspathEntry[] currentEntries= project.getRawClasspath();
        if (currentEntries.length != fEntries.size())
            return true;
        
        int i= 0;
        for (Iterator iterator= fEntries.iterator(); iterator.hasNext();) {
	        CPListElement oldEntrie= (CPListElement)iterator.next();
	        if (!oldEntrie.getClasspathEntry().equals(currentEntries[i]))
	        	return true;
	        i++;
        }
        return false;
	}
	
	private void showExceptionDialog(CoreException exception) {
		showError(exception, getShell(), NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_ClearAll_tooltip, exception.getMessage());
	}

	private void showError(CoreException e, Shell shell, String title, String message) {
		IStatus status= e.getStatus();
		if (status != null) {
			ErrorDialog.openError(shell, message, title, status);
		} else {
			MessageDialog.openError(shell, title, message);
		}
	}
	
	private Shell getShell() {
		return JavaPlugin.getActiveWorkbenchShell();
    }

	protected void selectAndReveal(ISelection selection) {
        fProvider.handleResetAll();	
	}

	/**
     * {@inheritDoc}
     */
    public String getDescription(int type) {
    	return NewWizardMessages.PackageExplorerActionGroup_FormText_Default_ResetAll;
    }

	/**
     * {@inheritDoc}
     */
    public String getName() {
	    return NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_Reset_tooltip;
    }

	/**
     * {@inheritDoc}
     */
    public int getTypeId() {
	    return IClasspathInformationProvider.RESET_ALL;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getId() {
     	return Integer.toString(getTypeId());
    }

}

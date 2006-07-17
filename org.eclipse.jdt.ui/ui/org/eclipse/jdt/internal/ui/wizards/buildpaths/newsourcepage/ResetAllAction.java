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

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier.IClasspathModifierListener;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;

//TODO: Use global history
public class ResetAllAction extends BuildpathModifierAction {
	
	private final IClasspathModifierListener fListener;
	private final HintTextGroup fProvider;
	private final IRunnableContext fContext;
	private IJavaProject fJavaProject;
	private List fEntries;
	private IPath fOutputLocation;

	public ResetAllAction(IClasspathModifierListener listener, HintTextGroup provider, IRunnableContext context) {
		super(null, BuildpathModifierAction.RESET_ALL);
		
		fListener= listener;
		fProvider= provider;
		fContext= context;
		
		setImageDescriptor(JavaPluginImages.DESC_ELCL_CLEAR);
		setDisabledImageDescriptor(JavaPluginImages.DESC_DLCL_CLEAR);
		setText(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_ClearAll_label);
		setToolTipText(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_ClearAll_tooltip);
		setEnabled(false);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getDetailedDescription() {
		return NewWizardMessages.PackageExplorerActionGroup_FormText_Default_ResetAll;
	}

	public void setBreakPoint(IJavaProject javaProject) throws JavaModelException {
		fJavaProject= javaProject;
		
		fEntries= ClasspathModifier.getExistingEntries(javaProject);
		fOutputLocation= fJavaProject.getOutputLocation();
		setEnabled(canHandle(null));
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
	                    showExceptionDialog(e, NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_ClearAll_tooltip);
	                } finally {
	                	monitor.done();
	                }
	        	}
	        };
	        fContext.run(false, false, runnable);
        } catch (InvocationTargetException e) {
			if (e.getCause() instanceof CoreException) {
				showExceptionDialog((CoreException)e.getCause(), NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_ClearAll_tooltip);
			} else {
				JavaPlugin.log(e);
			}
        } catch (InterruptedException e) {
        }
	}
	

	/**
     * {@inheritDoc}
     */
    protected boolean canHandle(IStructuredSelection elements) {
    	if (fJavaProject == null)
    		return false;
    	
	    return true;
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

	protected void selectAndReveal(ISelection selection) {
        fProvider.handleResetAll();	
	}
}

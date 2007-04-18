/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.workingsets.JavaWorkingSetUpdater;
import org.eclipse.jdt.internal.ui.workingsets.ViewActionGroup;

public class JavaProjectWizard extends NewElementWizard implements IExecutableExtension {
    
    private JavaProjectWizardFirstPage fFirstPage;
    private JavaProjectWizardSecondPage fSecondPage;
    
    private IConfigurationElement fConfigElement;
    
    public JavaProjectWizard() {
        setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWJPRJ);
        setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
        setWindowTitle(NewWizardMessages.JavaProjectWizard_title); 
    }

    /*
     * @see Wizard#addPages
     */	
    public void addPages() {
        super.addPages();
        fFirstPage= new JavaProjectWizardFirstPage();
        fFirstPage.setWorkingSets(getWorkingSets(getSelection()));
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
       
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#performFinish()
	 */
	public boolean performFinish() {
		boolean res= super.performFinish();
		if (res) {
			final IJavaElement newElement= getCreatedElement();
			
			IWorkingSet[] workingSets= fFirstPage.getWorkingSets();
			for (int i= 0; i < workingSets.length; i++) {
				IWorkingSet workingSet= workingSets[i];
				IAdaptable[] adaptedNewElements= workingSet.adaptElements(new IAdaptable[] {newElement});
				if (adaptedNewElements.length == 1) {
					IAdaptable[] elements= workingSet.getElements();
					IAdaptable[] newElements= new IAdaptable[elements.length + 1];
					System.arraycopy(elements, 0, newElements, 0, elements.length);
					newElements[newElements.length - 1]= adaptedNewElements[0];
					workingSet.setElements(newElements);
				}
			}
			
			BasicNewProjectResourceWizard.updatePerspective(fConfigElement);
			selectAndReveal(fSecondPage.getJavaProject().getProject());				
			
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					PackageExplorerPart activePackageExplorer= getActivePackageExplorer();
					if (activePackageExplorer != null) {
						activePackageExplorer.tryToReveal(newElement);
					}
				}
			});
		}
		return res;
	}

	protected void handleFinishException(Shell shell, InvocationTargetException e) {
        String title= NewWizardMessages.JavaProjectWizard_op_error_title; 
        String message= NewWizardMessages.JavaProjectWizard_op_error_create_message;			 
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
        return super.performCancel();
    }
    
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.wizards.NewElementWizard#getCreatedElement()
	 */
	public IJavaElement getCreatedElement() {
		return JavaCore.create(fFirstPage.getProjectHandle());
	}
	
	private IWorkingSet[] getWorkingSets(IStructuredSelection selection) {
		IWorkingSet selected= getSelectedWorkingSet(selection);
		if (selected != null)
			return new IWorkingSet[] {selected};
		
		PackageExplorerPart explorerPart= getActivePackageExplorer();
		if (explorerPart == null)
			return null;
		
		if (explorerPart.getRootMode() == ViewActionGroup.SHOW_PROJECTS) {				
			//Get active filter
			IWorkingSet filterWorkingSet= explorerPart.getFilterWorkingSet();
			if (filterWorkingSet == null)
				return null;
			
			if (!isValidWorkingSet(filterWorkingSet))
				return null;
			
			return new IWorkingSet[] {filterWorkingSet};
		} else if (explorerPart.getRootMode() == ViewActionGroup.SHOW_WORKING_SETS) {
			//If we have been gone into a working set return the working set
			Object input= explorerPart.getViewPartInput();
			if (!(input instanceof IWorkingSet))
				return null;
			
			IWorkingSet workingSet= (IWorkingSet)input;
			if (!isValidWorkingSet(workingSet))
				return null;
			
			return new IWorkingSet[] {workingSet};
		}
		
		return null;
	}

	private IWorkingSet getSelectedWorkingSet(IStructuredSelection selection) {
		if (!(selection instanceof ITreeSelection))
			return null;
		
		ITreeSelection treeSelection= (ITreeSelection)selection;
		List elements= treeSelection.toList();
		if (elements.size() != 1)
			return null;
		
		Object element= elements.get(0);
		TreePath[] paths= treeSelection.getPathsFor(element);
		if (paths.length != 1)
			return null;

		TreePath path= paths[0];
		if (path.getSegmentCount() == 0)
			return null;

		Object candidate= path.getSegment(0);
		if (!(candidate instanceof IWorkingSet))
			return null;
			
		IWorkingSet result= (IWorkingSet)candidate;
		if (!isValidWorkingSet(result))
			return null;
		
		return result;
	}

	private PackageExplorerPart getActivePackageExplorer() {
		PackageExplorerPart explorerPart= PackageExplorerPart.getFromActivePerspective();
		if (explorerPart == null)
			return null;
		
		IWorkbenchPage activePage= explorerPart.getViewSite().getWorkbenchWindow().getActivePage();
		if (activePage == null)
			return null;
		
		if (activePage.getActivePart() != explorerPart)
			return null;
		
		return explorerPart;
	}

	private boolean isValidWorkingSet(IWorkingSet workingSet) {
		String id= workingSet.getId();	
		if (!JavaWorkingSetUpdater.ID.equals(id) && !"org.eclipse.ui.resourceWorkingSetPage".equals(id)) //$NON-NLS-1$
			return false;
		
		if (workingSet.isAggregateWorkingSet())
			return false;
		
		return true;
	}
	
}

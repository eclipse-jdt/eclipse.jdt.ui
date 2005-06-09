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

package org.eclipse.jdt.internal.corext.buildpath;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.DialogPackageExplorerActionGroup;


/**
 * Operation to include objects of type  
 * <code>IResource</code> or <code>IJavaElement</code>.
 * 
 * @see org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier#include(List, IJavaProject, IProgressMonitor)
 * @see org.eclipse.jdt.internal.corext.buildpath.UnincludeOperation
 */
public class IncludeOperation extends ClasspathModifierOperation {
    
    /**
     * Constructor
     * 
     * @param listener a <code>IClasspathModifierListener</code> that is notified about 
     * changes on classpath entries or <code>null</code> if no such notification is 
     * necessary.
     * @param informationProvider a provider to offer information to the operation
     * 
     * @see IClasspathInformationProvider
     * @see ClasspathModifier
     */
    public IncludeOperation(IClasspathModifierListener listener, IClasspathInformationProvider informationProvider) {
        super(listener, informationProvider, NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_Include_tooltip, IClasspathInformationProvider.INCLUDE); 
    }
    
    /**
     * Method which runs the actions with a progress monitor.<br>
     * 
     * This operation does not require any queries from the provider.
     * 
     * @param monitor a progress monitor, can be <code>null</code>
     */
    public void run(IProgressMonitor monitor) throws InvocationTargetException {
        List result= null;
        fException= null;
        try {
            List elements= getSelectedElements();
            IJavaProject project= fInformationProvider.getJavaProject();
            result= include(elements, project, monitor);
        } catch (CoreException e) {
            fException= e;
            result= null;
        }
        super.handleResult(result, monitor);
    }
    
    /**
	 * Find out whether this operation can be executed on 
	 * the provided list of elements.
	 * 
	 * @param elements a list of elements
	 * @param types an array of types for each element, that is, 
	 * the type at position 'i' belongs to the selected element 
	 * at position 'i' 
	 * 
	 * @return <code>true</code> if the operation can be 
	 * executed on the provided list of elements, <code>
	 * false</code> otherwise.
	 * @throws JavaModelException 
	 */
	public boolean isValid(List elements, int[] types) throws JavaModelException {
	    if (elements.size() == 0)
	        return false;
	    IJavaProject project= fInformationProvider.getJavaProject();
	    for(int i= 0; i < elements.size(); i++) {
	        Object element= elements.get(i);
	        switch (types[i]) {
	            case DialogPackageExplorerActionGroup.PACKAGE_FRAGMENT: break; // ok
	            case DialogPackageExplorerActionGroup.FOLDER: if (!isValidFolder((IResource)element, project)) return false; break;
	            case DialogPackageExplorerActionGroup.EXCLUDED_FOLDER: break; // ok
	            case DialogPackageExplorerActionGroup.EXCLUDED_FILE: break; // ok
	            case DialogPackageExplorerActionGroup.COMPILATION_UNIT: break; // ok
	            default: return false; // all others are not ok
	        }
	        
	    }
	    return true;
	}
    
    /**
     * Find out whether the folder can be included or not.
     * 
     * @param resource the resource to be checked
     * @param project the Java project
     * @return <code>true</code> if the folder can be included, <code>
     * false</code> otherwise
     * @throws JavaModelException
     */
	private boolean isValidFolder(IResource resource, IJavaProject project) throws JavaModelException {
		if (project.isOnClasspath(project) && resource.getProjectRelativePath().segmentCount() == 1) {
			IPackageFragmentRoot root1= ClasspathModifier.getFragmentRoot(resource, project, null);
			IPackageFragmentRoot root2= ClasspathModifier.getFragmentRoot(project.getResource(), project, null);
			if (root1 != null && root1.equals(root2)) {
				return true;
			}
		}
		return false;
	}
    
    /**
     * Get a description for this operation. The description depends on 
     * the provided type parameter, which must be a constant of 
     * <code>DialogPackageExplorerActionGroup</code>. If the type is 
     * <code>DialogPackageExplorerActionGroup.MULTI</code>, then the 
     * description will be very general to describe the situation of 
     * all the different selected objects as good as possible.
     * 
     * @param type the type of the selected object, must be a constant of 
     * <code>DialogPackageExplorerActionGroup</code>.
     * @return a string describing the operation
     */
    public String getDescription(int type) {
        if (type == DialogPackageExplorerActionGroup.PACKAGE_FRAGMENT)
            return NewWizardMessages.PackageExplorerActionGroup_FormText_Include;
        if (type == DialogPackageExplorerActionGroup.COMPILATION_UNIT)
            return NewWizardMessages.PackageExplorerActionGroup_FormText_Include;
        if (type == DialogPackageExplorerActionGroup.FOLDER)
            return NewWizardMessages.PackageExplorerActionGroup_FormText_Include;
        if (type == DialogPackageExplorerActionGroup.EXCLUDED_FOLDER)
            return NewWizardMessages.PackageExplorerActionGroup_FormText_Include;
        if (type == DialogPackageExplorerActionGroup.EXCLUDED_FILE)
            return NewWizardMessages.PackageExplorerActionGroup_FormText_Include;
        return NewWizardMessages.PackageExplorerActionGroup_FormText_Default_Include; 
    }
}

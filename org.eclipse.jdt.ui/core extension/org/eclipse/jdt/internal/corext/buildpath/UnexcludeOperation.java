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

package org.eclipse.jdt.internal.corext.buildpath;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.DialogPackageExplorerActionGroup;


/**
 * Operation to unexclude an <code>IResource</code>. This is the 
 * reverse action to exclude.
 * 
 * @see org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier#unExclude(List, IJavaProject, IProgressMonitor)
 * @see org.eclipse.jdt.internal.corext.buildpath.ExcludeOperation
 */
public class UnexcludeOperation extends ClasspathModifierOperation {
    
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
    public UnexcludeOperation(IClasspathModifierListener listener, IClasspathInformationProvider informationProvider) {
        super(listener, informationProvider, IClasspathInformationProvider.UNEXCLUDE);
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
        try {
            List resources= fInformationProvider.getSelection();
            IJavaProject project= fInformationProvider.getJavaProject();
            result= unExclude(resources, project, monitor);
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
            switch(types[i]) {
                case DialogPackageExplorerActionGroup.FOLDER: if (!isValidFolder((IResource)element, project)) return false; break;
                case DialogPackageExplorerActionGroup.EXCLUDED_FOLDER: if (!isValidExcludedFolder((IResource)element, project)) return false; break;
                case DialogPackageExplorerActionGroup.EXCLUDED_FILE: if (!isValidExcludedFile((IFile)element, project)) return false; break;
                default: return false;
            }
        }
        return true;
    }
    
    /**
     * Find out whether the folder can be unexcluded or not.
     * 
     * @param resource the resource to be checked
     * @param project the Java project
     * @return <code>true</code> if the folder can be unexcluded, <code>
     * false</code> otherwise
     * @throws JavaModelException
     */
    private boolean isValidFolder(IResource resource, IJavaProject project) throws JavaModelException {
        return ClasspathModifier.isExcluded(resource, project);
    }
    
    /**
     * Find out whether the excluded folder can be unexcluded or not.
     * 
     * @param resource the resource to be checked
     * @param project the Java project
     * @return <code>true</code> if the folder can be unexcluded, <code>
     * false</code> otherwise
     * @throws JavaModelException
     */
    private boolean isValidExcludedFolder(IResource resource, IJavaProject project) throws JavaModelException {
        if (ClasspathModifier.isExcluded(resource, project)) {
            return true;
        }
        return false;
    }
    
    /**
     * Find out whether the file can be excluded or not.
     * 
     * @param file the file to be checked
     * @param project the Java project
     * @return <code>true</code> if the file can be unexcluded, <code>
     * false</code> otherwise
     * @throws JavaModelException
     */
    private boolean isValidExcludedFile(IFile file, IJavaProject project) throws JavaModelException {
        return ClasspathModifier.isExcluded(file, project);
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
        IResource resource= (IResource)fInformationProvider.getSelection().get(0);
        if (type == DialogPackageExplorerActionGroup.FOLDER)
            return NewWizardMessages.getFormattedString("PackageExplorerActionGroup.FormText.UnexcludeFolder", resource.getName()); //$NON-NLS-1$
        if (type == DialogPackageExplorerActionGroup.EXCLUDED_FILE)
            return NewWizardMessages.getFormattedString("PackageExplorerActionGroup.FormText.UnexcludeFile", resource.getName()); //$NON-NLS-1$
        return NewWizardMessages.getFormattedString("PackageExplorerActionGroup.FormText.Default.Unexclude", resource.getName()); //$NON-NLS-1$
    }
}

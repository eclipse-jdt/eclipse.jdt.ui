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

import org.eclipse.core.resources.IFolder;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.DialogPackageExplorerActionGroup;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IOutputFolderQuery;


/**
 * Operation to add an object (of type <code>IFolder</code> or <code>
 * IJavaElement</code> to the classpath.
 * 
 * @see org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier#addToClasspath(List, IJavaProject, IOutputFolderQuery, IProgressMonitor)
 * @see org.eclipse.jdt.internal.corext.buildpath.RemoveFromClasspathOperation
 */
public class AddToClasspathOperation extends ClasspathModifierOperation {
    
    /**
     * Constructor
     * 
     * @param listener a <code>IClasspathModifierListener</code> that is notified about 
     * changes on classpath entries or <code>null</code> if no such notification is 
     * necessary.
     * @param informationProvider a provider to offer information to the action
     * 
     * @see IClasspathInformationProvider
     * @see ClasspathModifier
     */
    public AddToClasspathOperation(IClasspathModifierListener listener, IClasspathInformationProvider informationProvider) {
        super(listener, informationProvider, NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.AddToCP"), IClasspathInformationProvider.ADD_TO_BP); //$NON-NLS-1$
    }
    
    /**
     * Method which runs the actions with a progress monitor.<br>
     * 
     * This operation requires the following query from the provider:
     * <li>IOutputFolderQuery</li>
     * 
     * @param monitor a progress monitor, can be <code>null</code>
     */
    public void run(IProgressMonitor monitor) throws InvocationTargetException {
        List result= null;
        try {
            List elements= fInformationProvider.getSelection();
            IJavaProject project= fInformationProvider.getJavaProject();
            IOutputFolderQuery query= fInformationProvider.getOutputFolderQuery();
            result= addToClasspath(elements, project, query, monitor);
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
            switch(types[i]) {
                case DialogPackageExplorerActionGroup.JAVA_PROJECT: if (!isValidProject(project)) return false; break;
                case DialogPackageExplorerActionGroup.PACKAGE_FRAGMENT: break; // is ok
                case DialogPackageExplorerActionGroup.INCLUDED_FOLDER: break; // is ok
                case DialogPackageExplorerActionGroup.FOLDER: break; // is ok
                case DialogPackageExplorerActionGroup.EXCLUDED_FOLDER: break; // is ok
                default: return false; // all others are not ok
            }
            
        }
        return true;
    }
    
    /**
     * Find out whether the project can be added to the build path or not.
     * 
     * @param project the Java project to be checked
     * @return <code>true</code> if the project can be added to the build path, 
     * <code>false</code> otherwise
     * @throws JavaModelException
     */
    private boolean isValidProject(IJavaProject project) throws JavaModelException {
        if (ClasspathModifier.getClasspathEntryFor(project.getPath(), project) == null)
            return true;
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
        Object obj= fInformationProvider.getSelection().get(0);
        if (type == DialogPackageExplorerActionGroup.JAVA_PROJECT)
            return NewWizardMessages.getFormattedString("PackageExplorerActionGroup.FormText.ProjectToBuildpath", ((IJavaProject)obj).getElementName()); //$NON-NLS-1$
        if (type == DialogPackageExplorerActionGroup.PACKAGE_FRAGMENT)
            return NewWizardMessages.getFormattedString("PackageExplorerActionGroup.FormText.toBuildpath", new String[] {"package", ((IJavaElement)obj).getElementName()}); //$NON-NLS-1$ //$NON-NLS-2$
        if (type == DialogPackageExplorerActionGroup.MODIFIED_FRAGMENT_ROOT)
            return NewWizardMessages.getFormattedString("PackageExplorerActionGroup.FormText.toBuildpath", new String[] {"package", ((IJavaElement)obj).getElementName()}); //$NON-NLS-1$ //$NON-NLS-2$
        if (type == DialogPackageExplorerActionGroup.FOLDER)
            return NewWizardMessages.getFormattedString("PackageExplorerActionGroup.FormText.toBuildpath", new String[] {"folder", ((IFolder)obj).getName()}); //$NON-NLS-1$ //$NON-NLS-2$
        if (type == DialogPackageExplorerActionGroup.EXCLUDED_FOLDER)
            return NewWizardMessages.getFormattedString("PackageExplorerActionGroup.FormText.toBuildpath", new String[] {"folder", ((IFolder)obj).getName()}); //$NON-NLS-1$ //$NON-NLS-2$
        return NewWizardMessages.getString("PackageExplorerActionGroup.FormText.Default.toBuildpath"); //$NON-NLS-1$
    }
}

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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElementAttribute;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.DialogPackageExplorerActionGroup;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IInclusionExclusionQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IOutputLocationQuery;

/**
 * Operation to edit the inclusion / exclusion filters of an
 * <code>IJavaElement</code> or the output folder of type 
 * <code>CPListElementAttribute</code>.
 * 
 * @see org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier#editFilters(IJavaElement, IJavaProject, IInclusionExclusionQuery, IProgressMonitor)
 */
public class EditOperation extends ClasspathModifierOperation {
    
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
    public EditOperation(IClasspathModifierListener listener, IClasspathInformationProvider informationProvider) {
        super(listener, informationProvider, IClasspathInformationProvider.EDIT);
    }
    
    /**
     * Method which runs the actions with a progress monitor.<br>
     * 
     * This operation requires the following queries:
     * <li>IInclusionExclusionQuery</li>
     * <li>IOutputLocationQuery</li>
     * 
     * @param monitor a progress monitor, can be <code>null</code>
     */
    public void run(IProgressMonitor monitor) throws InvocationTargetException {
        Object result= null;
        try {
            Object selection= fInformationProvider.getSelection().get(0);
            IJavaProject project= fInformationProvider.getJavaProject();
            if (selection instanceof IJavaElement) {
                IJavaElement javaElement= (IJavaElement)selection;
                IInclusionExclusionQuery query= fInformationProvider.getInclusionExclusionQuery();
                result= editFilters(javaElement, project, query, monitor);
            } else {
                CPListElement selElement= ((CPListElementAttribute)selection).getParent();
                IOutputLocationQuery query= fInformationProvider.getOutputLocationQuery();
                result= editOutputFolder(selElement, project, query, monitor);
            }
        } catch (CoreException e) {
            fException= e;
            result= null;
        }
        List resultList= new ArrayList();
        if (result != null)
            resultList.add(result);
        super.handleResult(resultList, monitor);
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
        if (elements.size() == 0 || elements.size() > 1)
            return false;
        IJavaProject project= fInformationProvider.getJavaProject();
        Object element= elements.get(0);

        if (element instanceof IJavaProject) {
            if (!project.isOnClasspath(project.getUnderlyingResource()))
                return false;
        }
        else if (!(element instanceof IPackageFragmentRoot || element instanceof CPListElementAttribute))
            return false;
        return true;
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
        if (type == DialogPackageExplorerActionGroup.JAVA_PROJECT)
            return NewWizardMessages.getString("PackageExplorerActionGroup.FormText.Edit"); //$NON-NLS-1$
        if (type == DialogPackageExplorerActionGroup.PACKAGE_FRAGMENT_ROOT)
            NewWizardMessages.getString("PackageExplorerActionGroup.FormText.Edit"); //$NON-NLS-1$
        if (type == DialogPackageExplorerActionGroup.MODIFIED_FRAGMENT_ROOT)
            NewWizardMessages.getString("PackageExplorerActionGroup.FormText.Edit"); //$NON-NLS-1$
        if (type == DialogPackageExplorerActionGroup.OUTPUT)
            return NewWizardMessages.getString("PackageExplorerActionGroup.FormText.EditOutputFolder"); //$NON-NLS-1$
        if (type == DialogPackageExplorerActionGroup.DEFAULT_OUTPUT)
            return NewWizardMessages.getString("PackageExplorerActionGroup.FormText.EditOutputFolder"); //$NON-NLS-1$
        return NewWizardMessages.getString("PackageExplorerActionGroup.FormText.Default.Exclude"); //$NON-NLS-1$
    }
}

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

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.DialogPackageExplorerActionGroup;

/**
 * Operation to reset either the inclusion/exclusion filters on a source folder or 
 * the output folder of a source folder (depends on the selected element provided by 
 * the <code>IClasspathInformationProvider</code>).
 * 
 * @see org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier#reset(List, IJavaProject, IProgressMonitor)
 */
public class ResetOperation extends ClasspathModifierOperation {
    
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
    public ResetOperation(IClasspathModifierListener listener, IClasspathInformationProvider informationProvider) {
        super(listener, informationProvider, NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.Reset"), IClasspathInformationProvider.RESET); //$NON-NLS-1$
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
            List selection= fInformationProvider.getSelection();
            IJavaProject project= fInformationProvider.getJavaProject();
            result= reset(selection, project, monitor);
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
        boolean hasResetableFragmentRoot= false;
        boolean hasOutputFolder= false;
        boolean hasResetableProject= false;
        /*
         * This computation is special compared to the other ones in operations:
         * As soon as there is at least one element which allows resetting, reseting 
         * is allowed (note: NOT ALL have to allow this!).
         * 
         * Of course, resetting is still not allowed if there is at least one element 
         * which does not support resetting at all!
         */
        for(int i= 0; i < elements.size(); i++) {
            switch(types[i]) {
                case DialogPackageExplorerActionGroup.JAVA_PROJECT: hasResetableProject= isValidProject(project); break; // as standalone selection, this is not ok --> check at end
                case DialogPackageExplorerActionGroup.PACKAGE_FRAGMENT_ROOT: break; // as standalone selection, this is not ok --> check at end
                case DialogPackageExplorerActionGroup.MODIFIED_FRAGMENT_ROOT: hasResetableFragmentRoot= true; break; // is ok
                case DialogPackageExplorerActionGroup.OUTPUT: hasOutputFolder= true; break; // is ok
                case DialogPackageExplorerActionGroup.DEFAULT_OUTPUT: break; // as standalone selection, this is not ok --> check at end
                default: return false; // all others are not ok
            }
            
        }
        return hasResetableFragmentRoot || hasOutputFolder || hasResetableProject;
    }
    
    /**
     * Find out whether the filters on the project can be reset or not.
     * 
     * @param project the Java project
     * @return <code>true</code> if this operation can be executed on the project, 
     * <code>false</code> otherwise
     * @throws JavaModelException
     */
    private boolean isValidProject(IJavaProject project) throws JavaModelException {
        if (project.isOnClasspath(project.getUnderlyingResource())) {
            IClasspathEntry entry= ClasspathModifier.getClasspathEntryFor(project.getPath(), project);
            if (entry.getInclusionPatterns().length != 0 || entry.getExclusionPatterns().length != 0)
                return true;
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
        if (type == DialogPackageExplorerActionGroup.OUTPUT ||
                type == (DialogPackageExplorerActionGroup.OUTPUT | DialogPackageExplorerActionGroup.MULTI))
            return NewWizardMessages.getString("PackageExplorerActionGroup.FormText.SetOutputToDefault");  //$NON-NLS-1$
        if (type == DialogPackageExplorerActionGroup.MODIFIED_FRAGMENT_ROOT || 
                type == (DialogPackageExplorerActionGroup.MODIFIED_FRAGMENT_ROOT | DialogPackageExplorerActionGroup.MULTI))
            return NewWizardMessages.getString("PackageExplorerActionGroup.FormText.ResetFilters"); //$NON-NLS-1$
        return NewWizardMessages.getString("PackageExplorerActionGroup.FormText.Default.Reset"); //$NON-NLS-1$
    }
}

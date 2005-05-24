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

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.DialogPackageExplorerActionGroup;


/**
 * Operation to uninclude objects of type <code>IJavaElement</code> 
 * (this is the reverse action to include).
 * 
 * @see org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier#unInclude(List, IJavaProject, IProgressMonitor)
 * @see org.eclipse.jdt.internal.corext.buildpath.IncludeOperation
 */
public class UnincludeOperation extends ClasspathModifierOperation {
    
    /**
     * Constructor
     * 
     * @param listener a <code>IClasspathModifierListener</code> that is notified about 
     * changes on classpath entries or <code>null</code> if no such notification is 
     * necessary.
     * @param informationProvider a provider to offer up-to-date information to the operation
     * 
     * @see IClasspathModifierListener
     * @see IClasspathInformationProvider
     * @see ClasspathModifier
     */
    public UnincludeOperation(IClasspathModifierListener listener, IClasspathInformationProvider informationProvider) {
        super(listener, informationProvider, NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_Uninclude_tooltip, IClasspathInformationProvider.UNINCLUDE); 
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
            result= unInclude(elements, project, monitor);
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
        if (types.length == 0)
            return false;
        for(int i= 0; i < types.length; i++) {
			int type= types[i];
            if (type != DialogPackageExplorerActionGroup.INCLUDED_FILE && type != DialogPackageExplorerActionGroup.INCLUDED_FOLDER)
                return false;
        }
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
        if (type == DialogPackageExplorerActionGroup.PACKAGE_FRAGMENT ||
                type == DialogPackageExplorerActionGroup.INCLUDED_FOLDER)
            return NewWizardMessages.PackageExplorerActionGroup_FormText_UnincludeFolder; 
        if (type == DialogPackageExplorerActionGroup.COMPILATION_UNIT ||
                type == DialogPackageExplorerActionGroup.INCLUDED_FILE)
            return NewWizardMessages.PackageExplorerActionGroup_FormText_UnincludeFile; 
        return NewWizardMessages.PackageExplorerActionGroup_FormText_Default_Uninclude; 
    }
}

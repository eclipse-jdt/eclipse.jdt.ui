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

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IAddLibrariesQuery;

/**
 * Operation to add libraries to the buildpath
 * 
 * @see org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier#addLibraries(IAddLibrariesQuery, IJavaProject, IProgressMonitor)
 */
public class AddLibraryOperation extends ClasspathModifierOperation {

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
    public AddLibraryOperation(IClasspathModifierListener listener, IClasspathInformationProvider informationProvider) {
        super(listener, informationProvider, NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.AddLibCP.tooltip"), IClasspathInformationProvider.ADD_TO_BP); //$NON-NLS-1$
    }
    
    /**
     * Method which runs the actions with a progress monitor.<br>
     * 
     * This operation requires the following query from the provider:
     * <li>IAddLibrariesQuery</li>
     * 
     * @param monitor a progress monitor, can be <code>null</code>
     */
    public void run(IProgressMonitor monitor) throws InvocationTargetException {
        List result= null;
        try {
            IJavaProject project= fInformationProvider.getJavaProject();
            IAddLibrariesQuery query= fInformationProvider.getLibrariesQuery();
            result= addLibraries(query, project, monitor);
        } catch (CoreException e) {
            fException= e;
            result= null;
        }
       super.handleResult(result, monitor);
    }

    /**
     * This particular operation is always valid.
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
        return true;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.corext.buildpath.ClasspathModifierOperation#getDescription(int)
     */
    public String getDescription(int type) {
        return NewWizardMessages.getString("PackageExplorerActionGroup.FormText.Default.toBuildpath.library"); //$NON-NLS-1$
    }

}

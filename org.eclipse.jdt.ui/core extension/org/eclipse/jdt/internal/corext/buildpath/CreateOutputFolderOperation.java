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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IOutputLocationQuery;

/**
 * Operation to create an output folder
 * 
 * @see org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier#createOutputFolder(IPackageFragmentRoot, IOutputLocationQuery, IJavaProject, IProgressMonitor)
 */
public class CreateOutputFolderOperation extends ClasspathModifierOperation {
    
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
    public CreateOutputFolderOperation(IClasspathModifierListener listener, IClasspathInformationProvider informationProvider) {
        super(listener, informationProvider, IClasspathInformationProvider.CREATE_OUTPUT);
    }
    
    
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        Object result= null;
        IPath oldOutputLocation= null;
        try {
            IPackageFragmentRoot root= (IPackageFragmentRoot)fInformationProvider.getSelection();
            IJavaProject project= fInformationProvider.getJavaProject();
            oldOutputLocation= project.getOutputLocation();
            ClasspathModifierQueries.IOutputLocationQuery query= fInformationProvider.getOutputLocationQuery();
            result= createOutputFolder(root, query, project, monitor);
        } catch (CoreException e) {
            fException= e;
            result= null;
        }
        
        super.handleResult(result, oldOutputLocation, monitor);
    }

}

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

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;


/**
 * Operation to uninclude (this is the reverse action to include) a 
 * <code>IJavaElement</code>.
 * 
 * @see org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier#unInclude(IJavaElement, IJavaProject, IProgressMonitor)
 * @see org.eclipse.jdt.internal.corext.buildpath.IncludeOperation
 */
public class UnincludeOperation extends ClasspathModifierOperation {
    
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
    public UnincludeOperation(IClasspathModifierListener listener, IClasspathInformationProvider informationProvider) {
        super(listener, informationProvider, IClasspathInformationProvider.UNINCLUDE);
    }
    
    /**
     * Method which runs the actions with a progress monitor
     * 
     * @param monitor a progress monitor, can be <code>null</code>
     */
    public void run(IProgressMonitor monitor) throws InvocationTargetException {
        Object result= null;
        IPath oldOutputLocation= null;
        try {
            IJavaElement javaElement= (IJavaElement) fInformationProvider.getSelection();
            IJavaProject project= fInformationProvider.getJavaProject();
            oldOutputLocation= project.getOutputLocation();
            result= unInclude(javaElement, project, monitor);
        } catch (CoreException e) {
            fException= e;
            result= null;
        }
        super.handleResult(result, oldOutputLocation, monitor);
    }
}

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

import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElementAttribute;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IInclusionExclusionQuery;

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
     * Method which runs the actions with a progress monitor
     * 
     * @param monitor a progress monitor, can be <code>null</code>
     */
    public void run(IProgressMonitor monitor) throws InvocationTargetException {
        Object result= null;
        IPath oldOutputLocation= null;
        try {
            Object selection= fInformationProvider.getSelection();
            IJavaProject project= fInformationProvider.getJavaProject();
            oldOutputLocation= project.getOutputLocation();
            if (selection instanceof IJavaElement) {
                IJavaElement javaElement= (IJavaElement)selection;
                ClasspathModifierQueries.IInclusionExclusionQuery query= fInformationProvider.getInclusionExclusionQuery();
                result= editFilters(javaElement, project, query, monitor);
            } else {
                CPListElement selElement= ((CPListElementAttribute)selection).getParent();
                ClasspathModifierQueries.IOutputLocationQuery query= fInformationProvider.getOutputLocationQuery();
                result= editOutputFolder(selElement, project, query, monitor);
            }
        } catch (CoreException e) {
            fException= e;
            result= null;
        }
        super.handleResult(result, oldOutputLocation, monitor);
    }
}

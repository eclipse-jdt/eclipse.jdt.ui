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

import org.eclipse.jface.operation.IRunnableWithProgress;


/**
 * Abstract class which represents classpath modifier operation, this is, 
 * Operation that call methods on <code>ClasspathModifier</code>.
 */
public abstract class ClasspathModifierOperation extends ClasspathModifier implements IRunnableWithProgress {
    protected IClasspathInformationProvider fInformationProvider;
    protected CoreException fException;
    private int fType;
    
    /**
     * Constructor
     * 
     * @param listener a <code>IClasspathModifierListener</code> that is notified about 
     * changes on classpath entries or <code>null</code> if no such notification is 
     * necessary.
     * @param informationProvider a provider to offer information to the operation
     * @param type the type of the operation, that is a constant of <code>
     * IClasspathInformationProvider</code>
     * 
     * @see IClasspathInformationProvider
     * @see ClasspathModifier
     */
    public ClasspathModifierOperation(IClasspathModifierListener listener, IClasspathInformationProvider informationProvider, int type) {
        super(listener);
        fInformationProvider= informationProvider;
        fException= null;
        fType= type;
    }
    
    protected void handleResult(Object result, IPath oldOutputLocation, IProgressMonitor monitor) throws InvocationTargetException{
        /*
         * if (fMonitor != null && fException != null) then
         * the action was called with the run method of 
         * the IRunnableWithProgress which will throw an 
         * InvocationTargetException in the case that an 
         * exception ocurred. Then error handling is 
         * done by the client which called run(monitor).
         * 
         * Otherwise we pass the information back to the 
         * information provider.
         */
        if (monitor == null || fException == null)
            fInformationProvider.handleResult(result, oldOutputLocation, fException, fType);
        else
            throw new InvocationTargetException(fException);
        fException= null;
    }
    
    /**
     * Method which runs the actions with a progress monitor
     * 
     * @param monitor a progress monitor, can be <code>null</code>
     */
    public abstract void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException;
    
    /**
     * Get the type converted into a string.
     * 
     * @return the ID (that is the type) of this operation as string.
     */
    public String getId() {
        return Integer.toString(fType);
    }
}

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

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jdt.core.JavaModelException;


/**
 * Abstract class which represents classpath modifier operation, this is, 
 * Operation that call methods on <code>ClasspathModifier</code>.
 */
public abstract class ClasspathModifierOperation extends ClasspathModifier implements IRunnableWithProgress {
    protected IClasspathInformationProvider fInformationProvider;
    protected CoreException fException;
    private int fType;
    /**
     * A human readable name for this operation
     */
    private String fName; 
    
    /**
     * Constructor
     * 
     * @param listener a <code>IClasspathModifierListener</code> that is notified about 
     * changes on classpath entries or <code>null</code> if no such notification is 
     * necessary.
     * @param informationProvider a provider to offer information to the operation
     * @param name a human readable name for this operation
     * @param type the type of the operation, that is a constant of <code>
     * IClasspathInformationProvider</code>
     * 
     * @see IClasspathInformationProvider
     * @see ClasspathModifier
     */
    public ClasspathModifierOperation(IClasspathModifierListener listener, IClasspathInformationProvider informationProvider, String name, int type) {
        super(listener);
        fInformationProvider= informationProvider;
        fException= null;
        fName= name;
        fType= type;
    }
    
    protected void handleResult(List result, IProgressMonitor monitor) throws InvocationTargetException{
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
            fInformationProvider.handleResult(result, fException, fType);
        else
            throw new InvocationTargetException(fException);
        fException= null;
    }
    
    /**
     * Method which runs the actions with a progress monitor.<br>
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
     * 
     * @throws JavaModelException
     */
    public abstract boolean isValid(List elements, int[] types) throws JavaModelException;
    
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
     * @return a string describing the operation.
     */
    public abstract String getDescription(int type);
    
    public String getName() {
        return fName;
    }
    
    public List getSelectedElements() {
        return ((IStructuredSelection)fInformationProvider.getSelection()).toList();
    }
}

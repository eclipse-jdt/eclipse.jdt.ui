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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

/**
 * Operation to reset all changes and go back to the initial project state.
 */
public class ResetAllOperation extends ClasspathModifierOperation {
    private IClasspathEntry[] fEntries;
    private IPath fOutputLocation;
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
    public ResetAllOperation(IClasspathModifierListener listener, IClasspathInformationProvider informationProvider) {
        super(listener, informationProvider, NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.UndoAll.tooltip"), IClasspathInformationProvider.RESET_ALL); //$NON-NLS-1$
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
            fInformationProvider.getJavaProject().setRawClasspath(fEntries, fOutputLocation, monitor);
            fInformationProvider.deleteCreatedResources();
            fEntries= null;
            fOutputLocation= null;
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
        IJavaProject project= fInformationProvider.getJavaProject();
        if (project == null)
            return false;
        if (fEntries == null) {
            fEntries= project.getRawClasspath();
            fOutputLocation= project.getOutputLocation();
        }
        if (!project.getOutputLocation().equals(fOutputLocation))
            return true;
        IClasspathEntry[] currentEntries= project.getRawClasspath();
        if (currentEntries.length != fEntries.length)
            return true;
        for(int i= 0; i < fEntries.length; i++) {
            if (!fEntries[i].equals(currentEntries[i]))
                return true;
        }
        return false;
    }
    
    /**
     * Get a description for this operation. In this particual case 
     * the description is independent of the selection and it's 
     * provided type.
     * 
     * @param type the type of the selected object, must be a constant of 
     * <code>DialogPackageExplorerActionGroup</code>.
     * @return a string describing the operation
     */
    public String getDescription(int type) {
        return NewWizardMessages.getString("PackageExplorerActionGroup.FormText.Default.ResetAll"); //$NON-NLS-1$
    }
}

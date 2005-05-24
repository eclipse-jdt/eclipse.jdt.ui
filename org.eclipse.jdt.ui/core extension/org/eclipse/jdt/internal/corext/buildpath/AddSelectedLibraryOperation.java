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

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.DialogPackageExplorerActionGroup;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.OutputFolderQuery;


/**
 * Operation to add objects (of type <code>IFile</code> or <code>
 * IFolder</code> as library entries to the classpath.
 * 
 * @see org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier#addToClasspath(List, IJavaProject, OutputFolderQuery, IProgressMonitor)
 * @see org.eclipse.jdt.internal.corext.buildpath.RemoveFromClasspathOperation
 */
public class AddSelectedLibraryOperation extends ClasspathModifierOperation {
    
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
    public AddSelectedLibraryOperation(IClasspathModifierListener listener, IClasspathInformationProvider informationProvider) {
        super(listener, informationProvider, NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_AddSelLibToCP_tooltip, IClasspathInformationProvider.ADD_SEL_LIB_TO_BP); 
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
        fException= null;
        try {
            List elements= getSelectedElements();
            IJavaProject project= fInformationProvider.getJavaProject();
            result= addLibraryEntries(elements, project, monitor);
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
        for (int i= 0; i < elements.size(); i++) {
        	if (types[i] != DialogPackageExplorerActionGroup.ARCHIVE) {
        		return false;
        	}            
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
        Object obj= getSelectedElements().get(0);
        String name= escapeSpecialChars(((IFile) obj).getName());
        
        if (type == DialogPackageExplorerActionGroup.ARCHIVE)
            return Messages.format(NewWizardMessages.PackageExplorerActionGroup_FormText_ArchiveToBuildpath, name); 
        return NewWizardMessages.PackageExplorerActionGroup_FormText_Default_toBuildpath; 
    }
}

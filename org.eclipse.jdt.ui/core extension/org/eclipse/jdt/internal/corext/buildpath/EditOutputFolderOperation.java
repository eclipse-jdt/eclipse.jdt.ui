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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElementAttribute;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IOutputLocationQuery;

/**
 * Operation to edit the output folder property of a source folder.
 * 
 * @see org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier#editOutputFolder(CPListElement, IJavaProject, IOutputLocationQuery, IProgressMonitor)
 */
public class EditOutputFolderOperation extends ClasspathModifierOperation {
    
    private boolean fShowOutputFolders;
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
    public EditOutputFolderOperation(IClasspathModifierListener listener, IClasspathInformationProvider informationProvider) {
        super(listener, informationProvider, NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.EditOutput.tooltip"), IClasspathInformationProvider.EDIT_OUTPUT); //$NON-NLS-1$
        fShowOutputFolders= false;
    }
    
    /**
     * Method which runs the actions with a progress monitor.<br>
     * 
     * This operation requires the following query:
     * <li>IOutputLocationQuery</li>
     * 
     * @param monitor a progress monitor, can be <code>null</code>
     */
    public void run(IProgressMonitor monitor) throws InvocationTargetException {
        Object result= null;
        try {
            Object selection= getSelectedElements().get(0);
            IJavaProject project= fInformationProvider.getJavaProject();
            CPListElement selElement;
            if (selection instanceof IJavaElement) {
                IJavaElement javaElement= (IJavaElement)selection;
                IClasspathEntry entry= ClasspathModifier.getClasspathEntryFor(javaElement.getPath(), project, IClasspathEntry.CPE_SOURCE);
                selElement= CPListElement.createFromExisting(entry, project);
            } else
                selElement= ((CPListElementAttribute)selection).getParent();
            IOutputLocationQuery query= fInformationProvider.getOutputLocationQuery();
            result= editOutputFolder(selElement, project, query, monitor);
        } catch (CoreException e) {
            fException= e;
            result= null;
        }
        List resultList= new ArrayList();
        if (result != null)
            resultList.add(result);
        super.handleResult(resultList, monitor);
    }
    
    /**
     * Method that is called whenever setting of 
     * output folders is allowed or forbidden (for example 
     * on changing a checkbox with this setting). Note that 
     * the validity state of this operation depends directly 
     * on this setting. Editing of an output folder is only 
     * possible, if output folders are shown, otherwise the 
     * operation is invalid.
     * 
     * @param show <code>true</code> if output 
     * folders should be shown, <code>false</code> otherwise.
     * 
     * @see #isValid(List, int[])
     */
    public void showOutputFolders(boolean show) {
        fShowOutputFolders= show;
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
        if (elements.size() == 0 || elements.size() > 1 || !fShowOutputFolders)
            return false;
        IJavaProject project= fInformationProvider.getJavaProject();
        Object element= elements.get(0);

        if (element instanceof IJavaProject) {
            if (!project.isOnClasspath(project.getUnderlyingResource()))
                return false;
        }
        else if (!(element instanceof IPackageFragmentRoot || element instanceof CPListElementAttribute))
            return false;
        if(element instanceof IPackageFragmentRoot && ((IPackageFragmentRoot)element).isArchive())
            return false;
        return true;
    }
    
    /**
     * Get a description for this operation. The description depends on 
     * the provided type parameter, which must be a constant of 
     * <code>DialogPackageExplorerActionGroup</code>. In this case, the 
     * type does not matter.
     * 
     * @param type the type of the selected object, must be a constant of 
     * <code>DialogPackageExplorerActionGroup</code>.
     * @return a string describing the operation
     */
    public String getDescription(int type) {
        return NewWizardMessages.getString("PackageExplorerActionGroup.FormText.EditOutputFolder"); //$NON-NLS-1$
    }
}

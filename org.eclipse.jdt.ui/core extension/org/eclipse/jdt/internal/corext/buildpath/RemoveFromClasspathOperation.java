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
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.DialogPackageExplorerActionGroup;


/**
 * Operation to remove a source folder (of type <code>
 * IPackageFragmentRoot</code> from the classpath.
 * 
 * @see org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier#removeFromClasspath(List, IJavaProject, IProgressMonitor)
 * @see org.eclipse.jdt.internal.corext.buildpath.AddToClasspathOperation
 */
public class RemoveFromClasspathOperation extends ClasspathModifierOperation {
    
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
    public RemoveFromClasspathOperation(IClasspathModifierListener listener, IClasspathInformationProvider informationProvider) {
        super(listener, informationProvider, IClasspathInformationProvider.REMOVE_FROM_BP);
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
        try {
            IJavaProject project= fInformationProvider.getJavaProject();
            List elements= fInformationProvider.getSelection();
            result= removeFromClasspath(elements, project, monitor);
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
        IJavaProject project= fInformationProvider.getJavaProject();
        Iterator iterator= elements.iterator();
        while(iterator.hasNext()) {
            Object element= iterator.next();
            if (!(element instanceof IPackageFragmentRoot || element instanceof IJavaProject))
                return false;
            if (element instanceof IJavaProject) {
                if (ClasspathModifier.getClasspathEntryFor(project.getPath(), project) == null)
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
        IJavaElement elem= (IJavaElement)fInformationProvider.getSelection().get(0);
        if (type == DialogPackageExplorerActionGroup.JAVA_PROJECT)
            return NewWizardMessages.getFormattedString("PackageExplorerActionGroup.FormText.ProjectFromBuildpath", elem.getElementName()); //$NON-NLS-1$
        if (type == DialogPackageExplorerActionGroup.PACKAGE_FRAGMENT_ROOT ||
                type == DialogPackageExplorerActionGroup.MODIFIED_FRAGMENT_ROOT)
            return NewWizardMessages.getFormattedString("PackageExplorerActionGroup.FormText.fromBuildpath", elem.getElementName()); //$NON-NLS-1$
        return NewWizardMessages.getString("PackageExplorerActionGroup.FormText.Default.FromBuildpath"); //$NON-NLS-1$
    }
}

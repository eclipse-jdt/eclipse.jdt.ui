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

package org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifierOperation;

/**
 * Action which is used when operations on the classpath 
 * are executed.
 */
public class ClasspathModifierAction extends Action {
    private ClasspathModifierOperation fOperation;
    
    /**
     * Constructor to create a classpath modifier action.
     * 
     * @param operation the operation to execute inside the action
     * @param imageDescriptor the image descriptor for the icon
     * @param disabledImageDescriptor the image descriptor for the disabled icon
     * @param text the text to be set for the action and for the tool tip
     * 
     * @see ClasspathModifierOperation
     */
    public ClasspathModifierAction(ClasspathModifierOperation operation, ImageDescriptor imageDescriptor, ImageDescriptor disabledImageDescriptor, String text) {
        setImageDescriptor(imageDescriptor);
        setDisabledImageDescriptor(disabledImageDescriptor);
        setText(text);
        setToolTipText(text);
        fOperation= operation;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.action.Action#run()
     */
    public void run() {
        try {
            fOperation.run(null);
        } catch (InvocationTargetException e) {
            // nothing to do
        } catch (InterruptedException e) {
            // nothing to do
        }
        // Remark: there is nothing to do because the operation that is executed 
        // ensures that the object receiving the result should do the exception handling
        // because it needs to implement interface IClasspathInformationProvider
    }
    
    /**
     * Find out whether this operation can be executed on 
     * the provided list of elements.
     * 
     * @param selectedElements a list of elements
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
    public boolean isValid(List selectedElements, int[] types) throws JavaModelException {
        return fOperation.isValid(selectedElements, types);
    }
    
    /**
     * Getter for the operation.
     * 
     * @return the operation that is executed within this action
     * 
     * @see ClasspathModifierOperation
     */
    public ClasspathModifierOperation getOperation() {
        return fOperation;
    }
    
    /**
     * Get a short and general description of this operation. The 
     * description is general in the sense that it does not 
     * consider the cirumstances under which this method has 
     * been called. To get a more specified description, <code>
     * getDescription(int)</code> can be called.
     * 
     * @return a very general description
     * @see #getDescription(int)
     */
    public String getDescription() {
        return fOperation.getDescription(DialogPackageExplorerActionGroup.MULTI);
    }
    
    /**
     * Get the description suitable to the provided type
     * 
     * @param type the type of the selected element(s), must be a constant of 
     * <code>DialogPackageActionGroup</code>.
     * @return a short description of the operation.
     * 
     * @see ClasspathModifierOperation#getDescription(int)
     * @see DialogPackageExplorerActionGroup
     */
    public String getDescription(int type) {
        return fOperation.getDescription(type);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.action.Action#getId()
     */
    public String getId() {
        return fOperation.getId();
    }
}

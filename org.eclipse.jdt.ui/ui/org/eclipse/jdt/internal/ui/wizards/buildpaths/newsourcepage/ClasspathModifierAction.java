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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifierOperation;

/**
 * Action which is used when modifications on the classpath 
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
        // because it has to implement interface IClasspathInformationProvider
    }
    
    /**
     * Getter for the operation.
     * 
     * @return the operation that is executed within this action
     */
    public ClasspathModifierOperation getOperation() {
        return fOperation;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.action.Action#getId()
     */
    public String getId() {
        return fOperation.getId();
    }
}

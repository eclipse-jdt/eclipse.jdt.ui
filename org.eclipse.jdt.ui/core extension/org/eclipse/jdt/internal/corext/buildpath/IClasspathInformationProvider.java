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

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries;


/**
 * Interface representing a selection provider for
 * operations. The interface allows the operation to get 
 * information about the current state and to callback on 
 * the provider if the result of an operation needs to be handled.
 * 
 * @see org.eclipse.jdt.internal.corext.buildpath.ClasspathModifierOperation
 * @see org.eclipse.jdt.internal.corext.buildpath.CreateFolderOperation
 * @see org.eclipse.jdt.internal.corext.buildpath.AddToClasspathOperation
 * @see org.eclipse.jdt.internal.corext.buildpath.RemoveFromClasspathOperation
 * @see org.eclipse.jdt.internal.corext.buildpath.IncludeOperation
 * @see org.eclipse.jdt.internal.corext.buildpath.UnincludeOperation
 * @see org.eclipse.jdt.internal.corext.buildpath.ExcludeOperation
 * @see org.eclipse.jdt.internal.corext.buildpath.UnexcludeOperation
 * @see org.eclipse.jdt.internal.corext.buildpath.EditOperation
 * @see org.eclipse.jdt.internal.corext.buildpath.ResetOperation
 */
public interface IClasspathInformationProvider {
    public static final int CREATE_FOLDER= 0x00;
    public static final int ADD_TO_BP= 0x01;
    public static final int REMOVE_FROM_BP= 0x02;
    public static final int INCLUDE= 0x03;
    public static final int UNINCLUDE= 0x04;
    public static final int EXCLUDE= 0x05;
    public static final int UNEXCLUDE= 0x06;
    public static final int EDIT= 0x07;
    public static final int RESET= 0x08;
    public static final int CREATE_OUTPUT= 0x09;
    
    /**
     * Method to invoce the <code>IClasspathInformationProvider</code> to 
     * process the result of the corresponding operation.
     * 
     * @param result the result object of an operation, can be <code>null</code> 
     * @param exception an exception object in case that an exception occurred, 
     * <code>null</code> otherwise. Note: clients should check the exception
     * object before processing the result because otherwise, the result might be
     * incorrect
     * @param operationType constant to specify which kind of operation was executed;
     * corresponds to one of the following constants of <code>IClasspathInformationProvider</code>:
     * <li>CREATE_FOLDER</li>
     * <li>EDIT_FILTERS</li>
     * <li>ADD_TO_CP</li>
     * <li>REMOVE_FROM_CP</li>
     * <li>INCLUDE</li>
     * <li>EXCLUDE</li>
     * <li>UNINCLUDE</li>
     * <li>UNEXCLUDE</li>
     * <li>RESET_FILTERS</li>
     * <li>CREATE_OUTPUT</li>
     * <li>EDIT_OUTPUT</li>
     * <li>REMOVE_FROM_CP</li>
     * <li>RESET_OUTPUT</li>
     */
    public void handleResult(Object result, IPath oldOutputLocation, CoreException exception, int operationType);
    
    /**
     * Method to retrieve the current selection of the provider, this is 
     * the object on which the operation should be executed on.
     * 
     * For example: if a tree item is selected and an operation should be 
     * executed on behalf of this item, then <code>getSelection()</code> 
     * should return this item. 
     * 
     * @return the current selection of the provider, must not be 
     * <code>null</code>
     */
    public Object getSelection();
    
    /**
     * Method to retrieve the java project from the provider.
     * 
     * @return the current java project, must not be <code>null</code>
     */
    public IJavaProject getJavaProject();
    
    /**
     * Method to retrieve an <code>IOutputFolderQuery</code> from 
     * the provider.
     * 
     * @return an <code>IOutputFolderQuery</code>, must not be 
     * <code>null</code>
     * 
     * @see ClasspathModifierQueries#getDefaultFolderQuery(Shell, IPath)
     */
    public ClasspathModifierQueries.IOutputFolderQuery getOutputFolderQuery();
    
    /**
     * Method to retrieve an <code>IInclusionExclusionQuery</code> from 
     * the provider.
     * 
     * @return an <code>IInclusionExclusionQuery</code>, must not be 
     * <code>null</code>
     * 
     * @see ClasspathModifierQueries#getDefaultInclusionExclusionQuery(Shell)
     */
    public ClasspathModifierQueries.IInclusionExclusionQuery getInclusionExclusionQuery();
    
    /**
     * Method to retrieve an <code>IOutputLocationQuery</code> from 
     * the provider.
     * 
     * @return an <code>IOutputLocationQuery</code>, must not be 
     * <code>null</code>
     * 
     * @see ClasspathModifierQueries#getDefaultOutputLocationQuery(Shell, IPath, List)
     */
    public ClasspathModifierQueries.IOutputLocationQuery getOutputLocationQuery();
    
    /**
     * Method to retrieve an <code>IFolderCreationQuery</code> from 
     * the provider.
     * 
     * @return an <code>IFolderCreationQuery</code>, must not be 
     * <code>null</code>
     * 
     * @see ClasspathModifierQueries#getDefaultFolderCreationQuery(Shell, Object, int)
     */
    public ClasspathModifierQueries.IFolderCreationQuery getFolderCreationQuery();
}

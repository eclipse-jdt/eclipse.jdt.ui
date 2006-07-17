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

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IAddArchivesQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IAddLibrariesQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.ICreateFolderQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IInclusionExclusionQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.ILinkToQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IOutputLocationQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IRemoveLinkedFolderQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.OutputFolderQuery;


/**
 * Interface representing a information provider for
 * operations. The interface allows the operation to get 
 * information about the current state and to callback on 
 * the provider if the result of an operation needs to be handled.
 * 
 */
public interface IClasspathInformationProvider {
    public static final int ADD_SEL_SF_TO_BP= 0x00;
    public static final int REMOVE_FROM_BP= 0x01;
    public static final int EXCLUDE= 0x02;
    public static final int UNEXCLUDE= 0x03;
    public static final int EDIT_FILTERS= 0x04;
    public static final int CREATE_LINK= 0x05;
    public static final int RESET_ALL= 0x06;
    public static final int EDIT_OUTPUT= 0x07;
    public static final int CREATE_OUTPUT= 0x08;
    public static final int RESET= 0x09;
    public static final int INCLUDE= 0xA;
    public static final int UNINCLUDE= 0xB;
    public static final int CREATE_FOLDER= 0xC;
    public static final int ADD_JAR_TO_BP= 0xD;
    public static final int ADD_LIB_TO_BP= 0xE;
    public static final int ADD_SEL_LIB_TO_BP= 0xF;
    
    /**
     * Method to invoce the <code>IClasspathInformationProvider</code> to 
     * process the result of the corresponding operation. Normally, operations 
     * call this method at the end of their computation an pass the result 
     * back to the provider.
     * 
     * @param resultElements the result list of an operation, can be empty
     * @param exception an exception object in case that an exception occurred, 
     * <code>null</code> otherwise. Note: clients should check the exception
     * object before processing the result because otherwise, the result might be
     * incorrect
     * @param operationType constant to specify which kind of operation was executed;
     * corresponds to one of the following constants of <code>IClasspathInformationProvider</code>:
     * <li>CREATE_FOLDER</li>
     * <li>ADD_TO_BP</li>
     * <li>REMOVE_FROM_BP</li>
     * <li>INCLUDE</li>
     * <li>UNINCLUDE</li>
     * <li>EXCLUDE</li>
     * <li>UNEXCLUDE</li>
     * <li>EDIT</li>
     * <li>RESET</li>
     * <li>CREATE_OUTPUT</li>
     */
    public void handleResult(List resultElements, CoreException exception, int operationType);
    
    /**
     * Method to retrieve the current list of selected elements of the provider, this is 
     * the objects on which the operation should be executed on.
     * 
     * For example: if a tree item is selected and an operation should be 
     * executed on behalf of this item, then <code>getSelection()</code> 
     * should return this item. 
     * 
     * @return the current list of selected elements from the provider, must not be 
     * <code>null</code>
     */
    public IStructuredSelection getSelection();
    
    /**
     * Method to retrieve the Java project from the provider.
     * 
     * @return the current Java project, must not be <code>null</code>
     */
    public IJavaProject getJavaProject();
    
    /**
     * Method to retrieve an <code>IOutputFolderQuery</code> from 
     * the provider.
     * 
     * @return an <code>IOutputFolderQuery</code>, must not be 
     * <code>null</code>
     * @throws JavaModelException
     * 
     * @see ClasspathModifierQueries#getDefaultFolderQuery(Shell, IPath)
     */
    public OutputFolderQuery getOutputFolderQuery() throws JavaModelException;
    
    /**
     * Method to retrieve an <code>IInclusionExclusionQuery</code> from 
     * the provider.
     * 
     * @return an <code>IInclusionExclusionQuery</code>, must not be 
     * <code>null</code>
     * @throws JavaModelException
     * 
     * @see ClasspathModifierQueries#getDefaultInclusionExclusionQuery(Shell)
     */
    public IInclusionExclusionQuery getInclusionExclusionQuery() throws JavaModelException;
    
    /**
     * Method to retrieve an <code>IOutputLocationQuery</code> from 
     * the provider.
     * 
     * @return an <code>IOutputLocationQuery</code>, must not be 
     * <code>null</code>
     * @throws JavaModelException
     * 
     * @see ClasspathModifierQueries#getDefaultOutputLocationQuery(Shell, IPath, List)
     */
    public IOutputLocationQuery getOutputLocationQuery() throws JavaModelException;
    
    /**
     * Method to retrieve an <code>ILinkToQuery</code> from 
     * the provider.
     * 
     * @return an <code>ILinkToQuery</code>, must not be 
     * <code>null</code>
     * @throws JavaModelException
     * 
     * @see ClasspathModifierQueries#getDefaultCreateFolderQuery(Shell, IJavaProject)
     */
    public ILinkToQuery getLinkFolderQuery() throws JavaModelException;
    
    /**
     * Method to retrieve an <code>IRemoveLinkedFolderQuery</code> from 
     * the provider.
     * 
     * @return an <code>IRemoveLinkedFolderQuery</code>, must not be 
     * <code>null</code>
     * @throws JavaModelException
     * 
     * @see ClasspathModifierQueries#getDefaultRemoveLinkedFolderQuery(Shell)
     */
    public IRemoveLinkedFolderQuery getRemoveLinkedFolderQuery() throws JavaModelException;
    
    /**
     * Method to retrieve an <code>IAddArchivesQuery</code> from 
     * the provider.
     * 
     * @return an <code>IAddArchivesQuery</code>, must not be 
     * <code>null</code>
     * @throws JavaModelException
     * 
     * @see ClasspathModifierQueries#getDefaultArchivesQuery(Shell)
     */
    public IAddArchivesQuery getExternalArchivesQuery() throws JavaModelException;
    
    /**
     * Method to retrieve an <code>IAddLibrariesQuery</code> from 
     * the provider.
     * 
     * @return an <code>IAddLibrariesQuery</code>, must not be 
     * <code>null</code>
     * @throws JavaModelException
     * 
     * @see ClasspathModifierQueries#getDefaultLibrariesQuery(Shell)
     */
    public IAddLibrariesQuery getLibrariesQuery() throws JavaModelException;

    /**
     * Method to retrieve an <code>ICreateFolderQuery</code> from 
     * the provider.
     * 
     * @return an <code>IFolderCreationQuery</code>, must not be 
     * <code>null</code>
     * @throws JavaModelException
     * 
     * @see ClasspathModifierQueries#getDefaultCreateFolderQuery(Shell, IJavaProject)
     */
	public ICreateFolderQuery getCreateFolderQuery() throws JavaModelException;
    
    /**
     * Delete all newly created folders and files.
     * Resources that existed before will not be 
     * deleted. It is assumed that the implementor of 
     * this interface knows which resources have been 
     * created and therefore is also able to remove 
     * all of them.
     */
    public void deleteCreatedResources();
}

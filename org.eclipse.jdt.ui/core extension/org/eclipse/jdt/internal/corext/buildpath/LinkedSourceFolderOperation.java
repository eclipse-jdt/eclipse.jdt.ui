/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.DialogPackageExplorerActionGroup;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.ILinkToQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.GenerateBuildPathActionGroup.CreateLinkedSourceFolderAction;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

/**
 * Operation create a link to a source folder.
 * 
 * @see org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier#createLinkedSourceFolder(ILinkToQuery, IJavaProject, IProgressMonitor)
 */
public class LinkedSourceFolderOperation extends ClasspathModifierOperation {

    private IClasspathModifierListener fListener;
	private IClasspathInformationProvider fCPInformationProvider;
	private final StringDialogField fOutputLocation;

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
    public LinkedSourceFolderOperation(IClasspathModifierListener listener, IClasspathInformationProvider informationProvider, StringDialogField outputLocation) {
        super(listener, informationProvider, NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_Link_tooltip, IClasspathInformationProvider.CREATE_LINK); 
		fListener= listener;
		fCPInformationProvider= informationProvider;
		fOutputLocation= outputLocation;
    }
    
    /**
     * Method which runs the actions with a progress monitor.<br>
     * 
     * This operation requires the following query from the provider:
     * <li>ILinkToQuery</li>
     * 
     * @param monitor a progress monitor, can be <code>null</code>
     */
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
    	CreateLinkedSourceFolderAction action= new CreateLinkedSourceFolderAction(new Path(fOutputLocation.getText()).makeAbsolute());
		action.selectionChanged(new StructuredSelection(fCPInformationProvider.getJavaProject()));
		action.run();
		IPackageFragmentRoot createdElement= (IPackageFragmentRoot)action.getCreatedElement();
		if (createdElement == null) {
			//Wizard was cancled.
			return;
		}
		try {
			IResource correspondingResource= createdElement.getCorrespondingResource();
			List result= new ArrayList();
			result.add(correspondingResource);
			if (fListener != null) {
				List entries= action.getCPListElements();
				fListener.classpathEntryChanged(entries);
			}
	        fCPInformationProvider.handleResult(result, null, IClasspathInformationProvider.CREATE_LINK);   
		} catch (JavaModelException e) {
			if (monitor == null) {
				fCPInformationProvider.handleResult(Collections.EMPTY_LIST, e, IClasspathInformationProvider.CREATE_LINK);
			} else {
				throw new InvocationTargetException(e);
			}
		}
    }

    /**
     * This particular operation is always valid.
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
        return types.length == 1 && types[0] == DialogPackageExplorerActionGroup.JAVA_PROJECT;
    }

    /**
     * Get a description for this operation.
     * 
     * @param type the type of the selected object, must be a constant of 
     * <code>DialogPackageExplorerActionGroup</code>.
     * @return a string describing the operation
     */
    public String getDescription(int type) {
        return NewWizardMessages.PackageExplorerActionGroup_FormText_createLinkedFolder; 
    }

}

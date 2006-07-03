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

import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.DialogPackageExplorerActionGroup;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.GenerateBuildPathActionGroup.CreateLocalSourceFolderAction;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

public class CreateFolderOperation extends ClasspathModifierOperation {
	
	private final IClasspathModifierListener fListener;
	private final IClasspathInformationProvider fCPInformationProvider;
	private final StringDialogField fOutputLocation;

    /**
     * Creates a new <code>AddFolderOperation</code>.
     * 
     * @param listener a <code>IClasspathModifierListener</code> that is notified about 
     * changes on classpath entries or <code>null</code> if no such notification is 
     * necessary.
     * @param informationProvider a provider to offer information to the action
     * 
     * @see IClasspathInformationProvider
     * @see ClasspathModifier
     */
	public CreateFolderOperation(IClasspathModifierListener listener, IClasspathInformationProvider informationProvider, StringDialogField outputLocation) {
		super(listener, informationProvider, NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_AddLibCP_tooltip, IClasspathInformationProvider.CREATE_FOLDER);
		fListener= listener;
		fCPInformationProvider= informationProvider;
		fOutputLocation= outputLocation;
	}

	/**
	 * {@inheritDoc}
	 */
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		CreateLocalSourceFolderAction action= new CreateLocalSourceFolderAction(new Path(fOutputLocation.getText()).makeAbsolute());
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
	        fCPInformationProvider.handleResult(result, null, IClasspathInformationProvider.CREATE_FOLDER);   
		} catch (JavaModelException e) {
			if (monitor == null) {
				fCPInformationProvider.handleResult(Collections.EMPTY_LIST, e, IClasspathInformationProvider.CREATE_FOLDER);
			} else {
				throw new InvocationTargetException(e);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isValid(List elements, int[] types) throws JavaModelException {
		return types.length == 1 && types[0] == DialogPackageExplorerActionGroup.JAVA_PROJECT;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDescription(int type) {
		return NewWizardMessages.PackageExplorerActionGroup_FormText_createNewSourceFolder; 
	}
}

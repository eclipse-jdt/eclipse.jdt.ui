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
package org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jdt.internal.corext.buildpath.IClasspathInformationProvider;

import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

public class EditOutputFolderAction2 extends EditOutputFolderAction implements IClasspathModifierAction {
	
	private final HintTextGroup fProvider;
	private boolean fShowOutputFolders;

	public EditOutputFolderAction2(NewSourceContainerWorkbookPage listener, HintTextGroup provider, IRunnableContext context) {
		super(null, context, listener);
		
		fProvider= provider;
		fShowOutputFolders= false;
    }

	/**
	 * {@inheritDoc}
	 */
	protected void selectAndReveal(ISelection selection) {
		fProvider.handleEditOutputFolder(((StructuredSelection)selection).toList());	 
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDescription(int type) {
		return NewWizardMessages.PackageExplorerActionGroup_FormText_EditOutputFolder;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getTypeId() {
		return IClasspathInformationProvider.EDIT_OUTPUT;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getId() {
	 	return Integer.toString(getTypeId());
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean canHandle(IStructuredSelection elements) {
		if (!fShowOutputFolders)
			return false;
		
	    return super.canHandle(elements);
	}

	public void showOutputFolders(boolean showOutputFolders) {
		fShowOutputFolders= showOutputFolders;
    }

}

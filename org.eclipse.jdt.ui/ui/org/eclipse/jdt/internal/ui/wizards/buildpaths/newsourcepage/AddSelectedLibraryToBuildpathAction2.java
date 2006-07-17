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

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;
import org.eclipse.jdt.internal.corext.buildpath.IClasspathInformationProvider;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier.IClasspathModifierListener;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

//Warning: This is unused and untested code!
public class AddSelectedLibraryToBuildpathAction2 extends AddSelectedLibraryToBuildpathAction implements IClasspathModifierAction {

	private final HintTextGroup fInformationProvider;

	public AddSelectedLibraryToBuildpathAction2(IClasspathModifierListener listener, HintTextGroup provider, IRunnableContext context) {
		super(null, context, listener);
				
		fInformationProvider= provider;
	}

	/**
	 * {@inheritDoc}
	 */
	protected void selectAndReveal(ISelection selection) {
	    fInformationProvider.handleAddToCP(((StructuredSelection)selection).toList());
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getDescription(int type) {
		Object obj= getSelectedElements().get(0);
        String name= ClasspathModifier.escapeSpecialChars(((IFile) obj).getName());
        
        if (type == DialogPackageExplorerActionGroup.ARCHIVE)
            return Messages.format(NewWizardMessages.PackageExplorerActionGroup_FormText_ArchiveToBuildpath, name); 
        return NewWizardMessages.PackageExplorerActionGroup_FormText_Default_toBuildpath; 
	}

	/**
	 * {@inheritDoc}
	 */
	public int getTypeId() {
		return IClasspathInformationProvider.ADD_SEL_LIB_TO_BP;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getId() {
	    return Integer.toString(getTypeId());
	}

}
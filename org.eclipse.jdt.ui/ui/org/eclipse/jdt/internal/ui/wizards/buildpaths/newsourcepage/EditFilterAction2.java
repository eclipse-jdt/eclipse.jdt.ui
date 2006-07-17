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
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jdt.internal.corext.buildpath.IClasspathInformationProvider;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier.IClasspathModifierListener;

import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

public class EditFilterAction2 extends EditFilterAction implements IClasspathModifierAction {
	
	private final HintTextGroup fProvider;

	public EditFilterAction2(IClasspathModifierListener listener, HintTextGroup provider, IRunnableContext context) {
		super(null, context, listener);
		
		fProvider= provider;
    }

	/**
	 * {@inheritDoc}
	 */
	protected void selectAndReveal(ISelection selection) {
		fProvider.defaultHandle(((StructuredSelection)selection).toList(), false);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDescription(int type) {
		if (type == DialogPackageExplorerActionGroup.JAVA_PROJECT)
			return NewWizardMessages.PackageExplorerActionGroup_FormText_Edit; 
		if (type == DialogPackageExplorerActionGroup.PACKAGE_FRAGMENT_ROOT)
			return NewWizardMessages.PackageExplorerActionGroup_FormText_Edit; 
		if (type == DialogPackageExplorerActionGroup.MODIFIED_FRAGMENT_ROOT)
			return NewWizardMessages.PackageExplorerActionGroup_FormText_Edit; 
		return NewWizardMessages.PackageExplorerActionGroup_FormText_Default_Edit;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_Edit_tooltip;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getTypeId() {
		return IClasspathInformationProvider.EDIT_FILTERS;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getId() {
	    return Integer.toString(getTypeId());
	}

}

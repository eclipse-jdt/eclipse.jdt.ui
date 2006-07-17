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
//Warning: This is unused and untested code!
public class ResetAction2 extends ResetAction implements IClasspathModifierAction {

	private final HintTextGroup fInformationProvider;

	public ResetAction2(IClasspathModifierListener listener, HintTextGroup provider, IRunnableContext context) {
		super(null, context, listener);
				
		fInformationProvider= provider;
	}

	/**
	 * {@inheritDoc}
	 */
	protected void selectAndReveal(ISelection selection) {
	    fInformationProvider.defaultHandle(((StructuredSelection)selection).toList(), false);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getDescription(int type) {
		if (type == DialogPackageExplorerActionGroup.OUTPUT ||
                type == (DialogPackageExplorerActionGroup.OUTPUT | DialogPackageExplorerActionGroup.MULTI))
            return NewWizardMessages.PackageExplorerActionGroup_FormText_SetOutputToDefault;  
        if (type == DialogPackageExplorerActionGroup.MODIFIED_FRAGMENT_ROOT || 
                type == (DialogPackageExplorerActionGroup.MODIFIED_FRAGMENT_ROOT | DialogPackageExplorerActionGroup.MULTI))
            return NewWizardMessages.PackageExplorerActionGroup_FormText_ResetFilters; 
        return NewWizardMessages.PackageExplorerActionGroup_FormText_Default_Reset;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_Reset_tooltip;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getTypeId() {
		return IClasspathInformationProvider.RESET;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getId() {
	    return Integer.toString(getTypeId());
	}

}

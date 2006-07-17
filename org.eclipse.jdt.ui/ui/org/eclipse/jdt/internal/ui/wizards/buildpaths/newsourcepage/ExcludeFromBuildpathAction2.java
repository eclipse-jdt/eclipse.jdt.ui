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

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;
import org.eclipse.jdt.internal.corext.buildpath.IClasspathInformationProvider;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier.IClasspathModifierListener;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

public class ExcludeFromBuildpathAction2 extends ExcludeFromBuildpathAction implements IClasspathModifierAction {

	private final HintTextGroup fProvider;

	public ExcludeFromBuildpathAction2(IClasspathModifierListener listener, HintTextGroup provider, IRunnableContext context) {
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
		IJavaElement elem= (IJavaElement) getSelectedElements().get(0);
        String name= ClasspathModifier.escapeSpecialChars(elem.getElementName());
        if (type == DialogPackageExplorerActionGroup.PACKAGE_FRAGMENT)
            return Messages.format(NewWizardMessages.PackageExplorerActionGroup_FormText_ExcludePackage, name); 
        if (type == DialogPackageExplorerActionGroup.INCLUDED_FOLDER)
            return Messages.format(NewWizardMessages.PackageExplorerActionGroup_FormText_ExcludePackage, name); 
        if (type == DialogPackageExplorerActionGroup.COMPILATION_UNIT)
            return Messages.format(NewWizardMessages.PackageExplorerActionGroup_FormText_ExcludeFile, name); 
        if (type == DialogPackageExplorerActionGroup.INCLUDED_FILE)
            return Messages.format(NewWizardMessages.PackageExplorerActionGroup_FormText_ExcludeFile, name); 
        return NewWizardMessages.PackageExplorerActionGroup_FormText_Default_Exclude;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_Exclude_tooltip;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getTypeId() {
		return IClasspathInformationProvider.EXCLUDE;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getId() {
	    return Integer.toString(getTypeId());
	}

}

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

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;
import org.eclipse.jdt.internal.corext.buildpath.IClasspathInformationProvider;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier.IClasspathModifierListener;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

public class AddFolderToBuildpathAction2 extends AddFolderToBuildpathAction implements IClasspathModifierAction {

	private final HintTextGroup fInformationProvider;

	public AddFolderToBuildpathAction2(IClasspathModifierListener listener, HintTextGroup provider, IRunnableContext context) {
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
     * Get the description suitable to the provided type
     * 
     * @param type the type of the selected element(s), must be a constant of 
     * <code>DialogPackageActionGroup</code>.
     * @return a short description of the operation.
     * 
     * @see DialogPackageExplorerActionGroup
     */
    public String getDescription(int type) {
        Object obj= getSelectedElements().get(0);
        if (obj instanceof IJavaElement) {
        	String name= ClasspathModifier.escapeSpecialChars(((IJavaElement)obj).getElementName());
        	if (type == DialogPackageExplorerActionGroup.JAVA_PROJECT)
        		return Messages.format(NewWizardMessages.PackageExplorerActionGroup_FormText_ProjectToBuildpath, name);
        	if (type == DialogPackageExplorerActionGroup.PACKAGE_FRAGMENT)
        		return Messages.format(NewWizardMessages.PackageExplorerActionGroup_FormText_PackageToBuildpath, name);
        	if (type == DialogPackageExplorerActionGroup.MODIFIED_FRAGMENT_ROOT)
        		return Messages.format(NewWizardMessages.PackageExplorerActionGroup_FormText_PackageToBuildpath, name);
        } else if (obj instanceof IResource) {
        	String name= ClasspathModifier.escapeSpecialChars(((IResource)obj).getName());
        	if (type == DialogPackageExplorerActionGroup.FOLDER)
        		return Messages.format(NewWizardMessages.PackageExplorerActionGroup_FormText_FolderToBuildpath, name);
        	if (type == DialogPackageExplorerActionGroup.EXCLUDED_FOLDER)
        		return Messages.format(NewWizardMessages.PackageExplorerActionGroup_FormText_FolderToBuildpath, name);
        }
        return NewWizardMessages.PackageExplorerActionGroup_FormText_Default_toBuildpath;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.action.Action#getId()
     */
    public String getId() {
        return Integer.toString(getTypeId());
    }
    
    /**
     * Get the action's name.
     * 
     * @return a human readable name for the operation/action executed
     */
    public String getName() {
        return NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_AddSelSFToCP_tooltip;
    }

    /**
     * {@inheritDoc}
     */
    public int getTypeId() {
        return IClasspathInformationProvider.ADD_SEL_SF_TO_BP;
    }
}
/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import java.util.List;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.DeleteResourceAction;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.internal.corext.refactoring.reorg.DeleteRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;


public class DeleteAction extends SelectionDispatchAction{

	private boolean fSuggestGetterSetterDeletion;

	public DeleteAction(IWorkbenchSite site) {
		super(site);
		setText(ReorgMessages.getString("DeleteAction.3")); //$NON-NLS-1$
		setDescription(ReorgMessages.getString("DeleteAction.4")); //$NON-NLS-1$
		fSuggestGetterSetterDeletion= true;//default
		ISharedImages workbenchImages= JavaPlugin.getDefault().getWorkbench().getSharedImages();
		setDisabledImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE_DISABLED));
		setImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));		
		setHoverImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE_HOVER));

		update(getSelection());
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.DELETE_ACTION);
	}
	
	public void setSuggestGetterSetterDeletion(boolean suggest){
		fSuggestGetterSetterDeletion= suggest;
	}

	/*
	 * @see SelectionDispatchAction#selectionChanged(IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		if (canDelegateToWorkbenchAction(selection)){
			setEnabled(createWorkbenchAction(selection).isEnabled());
			return;
		}
		try {
			List elements= selection.toList();
			IResource[] resources= ReorgUtils.getResources(elements);
			IJavaElement[] javaElements= ReorgUtils.getJavaElements(elements);
			if (elements.size() != resources.length + javaElements.length)
				setEnabled(false);
			else
				setEnabled(canEnable(resources, javaElements));
		} catch (JavaModelException e) {
			//no ui here - this happens on selection changes
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e);
			setEnabled(false);
		}
	}
	
	private boolean canDelegateToWorkbenchAction(IStructuredSelection selection) {
		return ReorgUtils.containsOnlyProjects(selection.toList());
	}

	private static IAction createWorkbenchAction(IStructuredSelection selection) {
		DeleteResourceAction action= new DeleteResourceAction(JavaPlugin.getActiveWorkbenchShell());
		action.selectionChanged(selection);
		return action;
	}

	private static boolean canEnable(IResource[] resources, IJavaElement[] javaElements) throws JavaModelException{
		return DeleteRefactoring.isAvailable(resources, javaElements);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		if (canDelegateToWorkbenchAction(selection)){
			createWorkbenchAction(selection).run();
			return;
		}
		try {
			List elements= selection.toList();
			IResource[] resources= ReorgUtils.getResources(elements);
			IJavaElement[] javaElements= ReorgUtils.getJavaElements(elements);
			if (canEnable(resources, javaElements)) 
				startRefactoring(resources, javaElements);
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), RefactoringMessages.getString("OpenRefactoringWizardAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		}		
	}

	private void startRefactoring(IResource[] resources, IJavaElement[] javaElements) throws JavaModelException{
		DeleteRefactoring refactoring= createRefactoring(resources, javaElements);
		RefactoringWizard wizard= createWizard(refactoring);
		/*
		 * We want to get the shell from the refactoring dialog but it's not known at this point, 
		 * so we pass the wizard and then, once the dialog is open, we will have access to its shell.
		 */
		refactoring.setQueries(new ReorgQueries(wizard));
		if (refactoring != null)
			new RefactoringStarter().activate(refactoring, wizard, getShell(), RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), false); //$NON-NLS-1$
	}

	private static RefactoringWizard createWizard(DeleteRefactoring refactoring){
		return new DeleteWizard(refactoring);
	}
		
	private DeleteRefactoring createRefactoring(IResource[] resources, IJavaElement[] javaElements) throws JavaModelException {
		DeleteRefactoring ref= DeleteRefactoring.create(resources, javaElements);
		ref.setSuggestGetterSetterDeletion(fSuggestGetterSetterDeletion);
		return ref;
	}
}

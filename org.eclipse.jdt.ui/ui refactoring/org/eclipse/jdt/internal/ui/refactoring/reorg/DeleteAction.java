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

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.DeleteResourceAction;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaDeleteProcessor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.UserInterfaceStarter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.ltk.core.refactoring.participants.DeleteRefactoring;

public class DeleteAction extends SelectionDispatchAction {

	public DeleteAction(IWorkbenchSite site) {
		super(site);
		setText(ReorgMessages.getString("DeleteAction.3")); //$NON-NLS-1$
		setDescription(ReorgMessages.getString("DeleteAction.4")); //$NON-NLS-1$
		ISharedImages workbenchImages= JavaPlugin.getDefault().getWorkbench().getSharedImages();
		setDisabledImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE_DISABLED));
		setImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
		setHoverImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE_HOVER));

		update(getSelection());
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.DELETE_ACTION);
	}

	/*
	 * @see SelectionDispatchAction#selectionChanged(IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		if (canDelegateToWorkbenchAction(selection)) {
			setEnabled(createWorkbenchAction(selection).isEnabled());
			return;
		}
		try {
			Object[] elements= selection.toArray();
			JavaDeleteProcessor processor= new JavaDeleteProcessor(elements);
			setEnabled(processor.isAvailable());
		} catch (CoreException e) {
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

	private IAction createWorkbenchAction(IStructuredSelection selection) {
		DeleteResourceAction action= new DeleteResourceAction(getShell());
		action.selectionChanged(selection);
		return action;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		if (canDelegateToWorkbenchAction(selection)) {
			createWorkbenchAction(selection).run();
			return;
		}
		try {
			Object[] elements= selection.toArray();
			DeleteRefactoring ref= createRefactoring(elements);
			UserInterfaceStarter starter= DeleteUserInterfaceManager.getDefault().getStarter(ref);
			starter.activate(ref, getShell(), false);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), RefactoringMessages.getString("OpenRefactoringWizardAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private DeleteRefactoring createRefactoring(Object[] elements) throws CoreException {
		JavaDeleteProcessor processor= new JavaDeleteProcessor(elements);
		return new DeleteRefactoring(processor);
	}
}

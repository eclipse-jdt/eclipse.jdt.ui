/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.MoveProjectAction;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaMoveProcessor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class ReorgMoveAction extends SelectionDispatchAction {
	public ReorgMoveAction(IWorkbenchSite site) {
		super(site);
		setText(ReorgMessages.getString("ReorgMoveAction.3")); //$NON-NLS-1$
		setDescription(ReorgMessages.getString("ReorgMoveAction.4")); //$NON-NLS-1$
		update(getSelection());
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.MOVE_ACTION);
	}

	public void selectionChanged(IStructuredSelection selection) {
		if (canDelegateToWorkbenchAction(selection)) {
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

	public void selectionChanged(ITextSelection selection) {
		setEnabled(true);
	}

	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	public void selectionChanged(JavaTextSelection selection) {
		try {
			setEnabled(canEnable(selection));
		} catch (JavaModelException e) {
			setEnabled(false);
		}
	}

	private boolean canEnable(JavaTextSelection selection) throws JavaModelException {
		IJavaElement element= selection.resolveEnclosingElement();
		if (element == null)
			return false;
		return JavaMoveProcessor.isAvailable(new IResource[0], new IJavaElement[] { element});
	}

	private boolean canEnable(IResource[] resources, IJavaElement[] javaElements) throws JavaModelException {
		return JavaMoveProcessor.isAvailable(resources, javaElements);
	}

	private MoveProjectAction createWorkbenchAction(IStructuredSelection selection) {
		MoveProjectAction action= new MoveProjectAction(getShell());
		action.selectionChanged(selection);
		return action;
	}

	private boolean canDelegateToWorkbenchAction(IStructuredSelection selection) {
		return ReorgUtils.containsOnlyProjects(selection.toList());
	}

	public void run(IStructuredSelection selection) {
		if (canDelegateToWorkbenchAction(selection)) {
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
			ExceptionHandler.handle(e, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), //$NON-NLS-1$
				RefactoringMessages.getString("OpenRefactoringWizardAction.exception"));  //$NON-NLS-1$
		}
	}

	private void startRefactoring(IResource[] resources, IJavaElement[] javaElements) throws JavaModelException {
		JavaMoveProcessor processor= createMoveProcessor(resources, javaElements);
		MoveRefactoring refactoring= new MoveRefactoring(processor);
		RefactoringWizard wizard= createWizard(refactoring);
		/*
		 * We want to get the shell from the refactoring dialog but it's not
		 * known at this point, so we pass the wizard and then, once the dialog
		 * is open, we will have access to its shell.
		 */
		processor.setCreateTargetQueries(new CreateTargetQueries(wizard));
		processor.setReorgQueries(new ReorgQueries(wizard));
		new RefactoringStarter().activate(refactoring, wizard, getShell(), 
			RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), //$NON-NLS-1$ 
			true);
	}

	private RefactoringWizard createWizard(MoveRefactoring refactoring) {
		return new ReorgMoveWizard(refactoring);
	}

	private JavaMoveProcessor createMoveProcessor(IResource[] resources, IJavaElement[] javaElements) throws JavaModelException {
		return JavaMoveProcessor.create(resources, javaElements);
	}
}

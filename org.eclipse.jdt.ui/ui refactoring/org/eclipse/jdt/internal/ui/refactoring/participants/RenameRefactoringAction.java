/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.participants;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.internal.corext.refactoring.participants.RenameRefactoring;

import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;


public class RenameRefactoringAction extends SelectionDispatchAction {

	public RenameRefactoringAction(IWorkbenchSite site) {
		super(site);
		setText("Rename with Participants...");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(selection.size() == 1);
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		Object element= selection.getFirstElement();
		try {
			RenameRefactoring refactoring= new RenameRefactoring(element);
			if (refactoring.isAvailable()) {
				RefactoringWizard wizard= new RenameRefactoringWizard2(
					refactoring, "Rename Refactoring", "message", "...", "...");
				new RefactoringStarter().activate(refactoring, wizard, getShell(), "Rename", false);
			} else {
				MessageDialog.openInformation(getShell(), 
					"Rename Refactoring", 
					"No refactoring available to process the selected element.");
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

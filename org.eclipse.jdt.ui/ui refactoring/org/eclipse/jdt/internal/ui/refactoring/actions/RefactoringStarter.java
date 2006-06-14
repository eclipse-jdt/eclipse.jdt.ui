/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;

import org.eclipse.jdt.internal.ui.refactoring.RefactoringSaveHelper;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;

/**
 * A helper class to activate the UI of a refactoring
 */
public class RefactoringStarter {
	
	private RefactoringSaveHelper fSaveHelper= new RefactoringSaveHelper();
	private RefactoringStatus fStatus;

	public boolean activate(Refactoring refactoring, RefactoringWizard wizard, Shell parent, String dialogTitle, boolean mustSaveEditors) throws JavaModelException {
		if (! canActivate(mustSaveEditors, parent))
			return false;

		try {
			RefactoringWizardOpenOperation op= new RefactoringWizardOpenOperation(wizard);
			int result= op.run(parent, dialogTitle);
			fStatus= op.getInitialConditionCheckingStatus();
			if (result == IDialogConstants.CANCEL_ID || result == RefactoringWizardOpenOperation.INITIAL_CONDITION_CHECKING_FAILED) {
				fSaveHelper.triggerBuild();
				return false;
			} else {
				return true;
			}
		} catch (InterruptedException e) {
			return false; // User action got cancelled
		}
	}
	
	public RefactoringStatus getInitialConditionCheckingStatus() {
		return fStatus;
	}
		
	private boolean canActivate(boolean mustSaveEditors, Shell shell) {
		return ! mustSaveEditors || fSaveHelper.saveEditors(shell);
	}
}
